/*
 * @(#)DistanceRule.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of openmolecules.org.  The intellectual and technical concepts contained
 * herein are proprietary to openmolecules.org.
 * Actelion Pharmaceuticals Ltd. is granted a non-exclusive, non-transferable
 * and timely unlimited usage license.
 *
 * @author Thomas Sander
 */

package org.openmolecules.chem.conf.so;

import java.util.ArrayList;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.RingCollection;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.BondAngleSet;
import com.actelion.research.chem.conf.BondLengthSet;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.util.DoubleFormat;

public class DistanceRule extends ConformationRule {
	private static final float VDW_TOLERANCE = 1.0f;

	private static final int PRIORITY_ONE_BOND = 10;
	private static final int PRIORITY_TWO_BONDS = 5;
	private static final int PRIORITY_THREE_BONDS = 3;

	private float[] mDistance;
	private int[] mNotList;
	private int mPriority;

	/**
	 * Constructor for 2 bonds in between.
	 * @param atom
	 * @param notList atoms (direct neighbors of atom[0] and atom[1]) to be excluded from movement
	 * @param distance
	 */
	public DistanceRule(int[] atom, int[] notList, float distance, int priority) {
		super(atom);
		mDistance = new float[1];
		mDistance[0] = distance;
		mNotList = notList;
		mPriority = priority;
		}

	public DistanceRule(int[] atom, float minDistance, float maxDistance, int priority) {
		super(atom);
		mDistance = new float[2];
		mDistance[0] = minDistance;
		mDistance[1] = maxDistance;
		mPriority = priority;
		}

   public DistanceRule(int[] atom, int[] notList, float minDistance, float maxDistance, int priority) {
		super(atom);
		mDistance = new float[2];
		mDistance[0] = minDistance;
		mDistance[1] = maxDistance;
		mNotList = notList;
		mPriority = priority;
		}

	@Override
	public int getRuleType() {
		return RULE_TYPE_DISTANCE;
		}

	public boolean isFixedDistance() {
		return mDistance.length == 1;
		}

	public static void calculateRules(ArrayList<ConformationRule> ruleList, StereoMolecule mol) {
		BondLengthSet bondLengthSet = new BondLengthSet(mol);
		BondAngleSet bondAngleSet = new BondAngleSet(mol, bondLengthSet);

		DistanceRule[][] rule = new DistanceRule[mol.getAllAtoms()][];
		for (int i=1; i<mol.getAllAtoms(); i++)
			rule[i] = new DistanceRule[i];

		// distances with 1 bond between both atoms
		for (int bond=0; bond<mol.getAllBonds(); bond++) {
			int[] atom = combineAtoms(mol.getBondAtom(0, bond), mol.getBondAtom(1, bond));
			setFixedDistance(rule, atom, atom, bondLengthSet.getLength(bond), PRIORITY_ONE_BOND);
			}

		for (int atom=0; atom<mol.getAtoms(); atom++) {
			for (int i=1; i<mol.getAllConnAtoms(atom); i++) {
				int connAtom1 = mol.getConnAtom(atom, i);
				int connBond1 = mol.getConnBond(atom, i);
				float bondLength1 = bondLengthSet.getLength(connBond1);

					// distances with 2 bonds between both atoms
				for (int j=0; j<i; j++) {
					int connAtom2 = mol.getConnAtom(atom, j);
					int connBond2 = mol.getConnBond(atom, j);

					float angle = bondAngleSet.getConnAngle(atom, i, j);
	
					float bondLength2 = bondLengthSet.getLength(connBond2);
					float distance = (float)Math.sqrt(bondLength1*bondLength1+bondLength2*bondLength2
												-2*bondLength1*bondLength2*Math.cos(angle));
					int[] notAtom = new int[1];
					notAtom[0] = atom;
					setFixedDistance(rule, combineAtoms(connAtom1, connAtom2), notAtom, distance, PRIORITY_TWO_BONDS);
					}
				}
			}

					// distances with 3 bonds between both atoms (special cases only)
		int[] bondRingSize = calculateBondRingSizes(mol);
		for (int bond=0; bond<mol.getAllBonds(); bond++) {
			int[] atom = new int[2];
			for (int i=0; i<2; i++)
				atom[i] = mol.getBondAtom(i, bond);

			if (mol.getAllConnAtoms(atom[0]) > 1
			 && mol.getAllConnAtoms(atom[1]) > 1) {

					// triple bonds
				switch (mol.getBondOrder(bond)) {
				case 3:
					float distance = bondLengthSet.getLength(bond);
					int[] outerAtom = new int[2];
					for (int i=0; i<2; i++) {
						for (int j=0; j<mol.getAllConnAtoms(atom[i]); j++) {
							int connBond = mol.getConnBond(atom[i], j);
							if (connBond != bond) {
								distance += bondLengthSet.getLength(connBond);
								outerAtom[i] = mol.getConnAtom(atom[i], j);
								break;
								}
							}
						}
					int[] notAtom = new int[2];
					notAtom[0] = atom[0];
					notAtom[1] = atom[1];
					setFixedDistance(rule, combineAtoms(outerAtom[0], outerAtom[1]), notAtom, distance, PRIORITY_THREE_BONDS);
					break;

				case 2:
					// strainless double bond with stereo information
					// (including symmetrical ones with parityNone)
					if (!mol.isAromaticBond(bond)
					 && mol.getAtomPi(atom[0]) == 1
					 && mol.getAtomPi(atom[1]) == 1
					 && mol.getBondParity(bond) != Molecule.cBondParityUnknown
					 && (bondRingSize[bond] == 0 || bondRingSize[bond] > 5)) {
//					 && (!mol.isRingAtom(atom[0]) || mol.getAtomRingSize(atom[0]) > 5)
//					 && (!mol.isRingAtom(atom[1]) || mol.getAtomRingSize(atom[1]) > 5)) {
						int[][] connAtom = new int[2][];
						int[][] connBond = new int[2][];
						float[][] connAngle = new float[2][];
						for (int i=0; i<2; i++) {
							connAtom[i] = new int[mol.getAllConnAtoms(atom[i])-1];
							connBond[i] = new int[mol.getAllConnAtoms(atom[i])-1];
							connAngle[i] = new float[mol.getAllConnAtoms(atom[i])-1];
	
							int doubleBondOpponentIndex = -1;
							for (int j=0; j<mol.getAllConnAtoms(atom[i]); j++) {
								if (mol.getConnAtom(atom[i], j) == atom[1-i]) {
									doubleBondOpponentIndex = j;
									break;
									}
								}
	
							int connIndex = 0;
							for (int j=0; j<mol.getAllConnAtoms(atom[i]); j++) {
								if (j != doubleBondOpponentIndex) {
									connAtom[i][connIndex] = mol.getConnAtom(atom[i], j);
									connBond[i][connIndex] = mol.getConnBond(atom[i], j);
									connAngle[i][connIndex] = bondAngleSet.getConnAngle(atom[i], doubleBondOpponentIndex, j);
									connIndex++;
									}
								}
							}
	
						for (int i=0; i<connAtom[0].length; i++) {
							for (int j=0; j<connAtom[1].length; j++) {
								boolean isE = (mol.getBondParity(bond) == Molecule.cBondParityEor1);
								if (connAtom[0].length == 2 && connAtom[0][i] > connAtom[0][1-i])
									isE = !isE;
								if (connAtom[1].length == 2 && connAtom[1][j] > connAtom[1][1-j])
									isE = !isE;
								setDoubleBondDistance(connAtom[0][i], connAtom[1][j],
													  connBond[0][i], connBond[1][j],
													  connAngle[0][i], connAngle[1][j],
													  bond, isE, bondLengthSet, rule, mol);
								}
							}
						}
					break;
				case 1:
					int[] opponentIndex = new int[2];
					for (int i=0; i<2; i++) {
						for (int j=0; j<mol.getAllConnAtoms(atom[i]); j++) {
							if (mol.getConnAtom(atom[i], j) == atom[1-i]) {
								opponentIndex[i] = j;
								break;
								}
							}
						}
					for (int i=0; i<mol.getAllConnAtoms(atom[0]); i++) {
						if (i != opponentIndex[0]) {
							for (int j=0; j<mol.getAllConnAtoms(atom[1]); j++) {
								if (j != opponentIndex[1]) {
									setSingleBondConnAtomDistance(
											mol.getConnAtom(atom[0], i), mol.getConnAtom(atom[1], j),
											mol.getConnBond(atom[0], i), mol.getConnBond(atom[1], j),
											bondAngleSet.getConnAngle(atom[0], opponentIndex[0], i),
											bondAngleSet.getConnAngle(atom[1], opponentIndex[1], j),
											bond, bondLengthSet, rule, mol);
									}
								}
							}
						}
					break;
					}
				}
			}

			// distances over 4 bonds in allenes
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.getAtomPi(atom) == 2
			 && mol.getConnAtoms(atom) == 2
			 && mol.getConnBondOrder(atom, 0) == 2
			 && mol.getConnBondOrder(atom, 1) == 2) {
				int atom1 = mol.getConnAtom(atom, 0);
				int atom2= mol.getConnAtom(atom, 1);
				for (int i=0; i<mol.getAllConnAtoms(atom1); i++) {
					int conn1 = mol.getConnAtom(atom1, i);
					if (conn1 != atom) {
						for (int j=0; j<mol.getAllConnAtoms(atom2); j++) {
							int conn2 = mol.getConnAtom(atom2, j);
							if (conn2 != atom) {
								float angle1 = bondAngleSet.getAngle(atom1, atom, conn1);
								float angle2 = bondAngleSet.getAngle(atom2, atom, conn2);
								float bondLength1 = bondLengthSet.getLength(mol.getConnBond(atom1, i));
								float bondLength2 = bondLengthSet.getLength(mol.getConnBond(atom2, j));
								float dx = bondLengthSet.getLength(mol.getConnBond(atom, 0))
										  + bondLengthSet.getLength(mol.getConnBond(atom, 1))
										  - bondLength1*(float)Math.cos(angle1)
										  - bondLength2*(float)Math.cos(angle2);
								float dy = bondLength1*(float)Math.sin(angle1);
								float dz = bondLength2*(float)Math.sin(angle2);
								int[] notAtom = new int[2];
								notAtom[0] = atom1;
								notAtom[1] = atom2;
								setFixedDistance(rule, combineAtoms(conn1, conn2), notAtom, (float)Math.sqrt(dx*dx+dy*dy+dz*dz), PRIORITY_THREE_BONDS);
								}
							}
						}
					}
				}
			}

		for (int atom=0; atom<mol.getAllAtoms(); atom++)
			calculateLongDistanceRules(rule, atom, mol, bondLengthSet);

		calculateDisconnectedDistanceRules(rule, mol);

		for (int i=1; i<mol.getAllAtoms(); i++)
			for (int j=0; j<i; j++)
				ruleList.add(rule[i][j]);
		}

	private static void calculateLongDistanceRules(DistanceRule[][] rule, int rootAtom, StereoMolecule mol, BondLengthSet bondLengthSet) {
		int[] bondCount = new int[mol.getAllAtoms()];
		int[] graphAtom = new int[mol.getAllAtoms()];
		float[] distanceToRoot = new float[mol.getAllAtoms()];

		graphAtom[0] = rootAtom;
		int current = 0;
		int highest = 0;
	 	while (current <= highest) {
			int parent = graphAtom[current];
			for (int i=0; i<mol.getAllConnAtoms(parent); i++) {
				int candidate = mol.getConnAtom(parent, i);
				if (bondCount[candidate] == 0 && candidate != rootAtom) {
					graphAtom[++highest] = candidate;
					bondCount[candidate] = bondCount[parent] + 1;

					if (bondCount[candidate] == 2)
						distanceToRoot[candidate] = (candidate < rootAtom) ?
													rule[rootAtom][candidate].mDistance[0]
												  : rule[candidate][rootAtom].mDistance[0];
						// distances with 3 or more bonds in between
					else if (bondCount[candidate] > 2) {
						distanceToRoot[candidate] = distanceToRoot[parent]
												  + bondLengthSet.getLength(mol.getConnBond(parent, i));

						if (candidate < rootAtom && rule[rootAtom][candidate] == null) {
							rule[rootAtom][candidate] = new DistanceRule(combineAtoms(rootAtom, candidate),
									getVDWRadius(rootAtom, mol) + getVDWRadius(candidate, mol), distanceToRoot[candidate], 0);

							rule[rootAtom][candidate].mDistance[0] *= VDW_TOLERANCE; // in reality non binding atoms sometimes come closer...
							}
						}
					}
				}
			current++;
			}
		}

	private static void calculateDisconnectedDistanceRules(DistanceRule[][] rule, StereoMolecule mol) {
		for (int atom1=1; atom1<mol.getAllAtoms(); atom1++) {
			for (int atom2=0; atom2<atom1; atom2++) {
				if (rule[atom1][atom2] == null) {
					rule[atom1][atom2] = new DistanceRule(combineAtoms(atom1, atom2),
							getVDWRadius(atom1, mol) + getVDWRadius(atom2, mol), Float.MAX_VALUE, -1);
					}
				}
			}
		}

	private static float getVDWRadius(int atom, StereoMolecule mol) {
		int atomicNo = mol.getAtomicNo(atom);
		return (atomicNo < VDWRadii.VDW_RADIUS.length) ? VDWRadii.VDW_RADIUS[atomicNo] : 2.0f;
		}

	private static void setDoubleBondDistance(int atom1, int atom2, int bond1, int bond2,
									   		  float angle1, float angle2, int ezBond, boolean isE,
									   		  BondLengthSet bondLengthSet, DistanceRule[][] rule,
									   		  StereoMolecule mol) {
		float s1 = bondLengthSet.getLength(ezBond)
				  - bondLengthSet.getLength(bond1) * (float)Math.cos(angle1)
				  - bondLengthSet.getLength(bond2) * (float)Math.cos(angle2);
		float s2 = bondLengthSet.getLength(bond1) * (float)Math.sin(angle1);
		if (isE)
			s2 += bondLengthSet.getLength(bond2) * Math.sin(angle2);
		else
			s2 -= bondLengthSet.getLength(bond2) * Math.sin(angle2);
		int[] notAtom = new int[2];
		notAtom[0] = mol.getBondAtom(0, ezBond);
		notAtom[1] = mol.getBondAtom(1, ezBond);
		setFixedDistance(rule, combineAtoms(atom1, atom2), notAtom, (float)Math.sqrt(s1*s1+s2*s2), PRIORITY_THREE_BONDS);
		}

	/**
	 * Adds a rules for two atoms with 3 bonds in between, where the central
	 * bond is rotatable. We assume the geometry with the central bond torsion
	 * in 60 degrees as minimum distance and with 180 degrees as maximum distance.
	 * @param atom1
	 * @param atom2
	 * @param bond1
	 * @param bond2
	 * @param angle1
	 * @param angle2
	 * @param centralBond
	 * @param bondLengthSet
	 * @param rule
	 * @param mol
	 */
	private static void setSingleBondConnAtomDistance(int atom1, int atom2, int bond1, int bond2,
													  float angle1, float angle2, int centralBond,
													  BondLengthSet bondLengthSet, DistanceRule[][] rule,
													  StereoMolecule mol) {
		if (atom1 == atom2)
			return;	// possible in 3-membered ring

		int[] atom = combineAtoms(atom1, atom2);
		if (rule[atom[0]][atom[1]] != null && rule[atom[0]][atom[1]].isFixedDistance())
			return;

		float sinTorsion = 0.866f;	// default: 60 degrees, i.e. we assume atoms come not closer than in gauche-position
		float cosTorsion = 0.5f;	// default: 60 degrees
		if (mol.isRingBond(centralBond)) {
			int ringSize = mol.getBondRingSize(centralBond);
			if (ringSize < 6) {
				sinTorsion = 0.0f;	// for strained rings we allow syn-position without strain
				cosTorsion = 1.0f;
				}
			}

		// distance along central bond, which is independent of central bond torsion
		float dx = bondLengthSet.getLength(centralBond)
				 - bondLengthSet.getLength(bond1) * (float)Math.cos(angle1)
				 - bondLengthSet.getLength(bond2) * (float)Math.cos(angle2);
		float s1 = bondLengthSet.getLength(bond1) * (float)Math.sin(angle1);
		float s2 = bondLengthSet.getLength(bond2) * (float)Math.sin(angle2);
		float dyMax = s1 + s2;
        float dyMin = s1 - s2 * cosTorsion;
		float dz = s2 * sinTorsion;
		float min = (float)Math.sqrt(dx*dx+dyMin*dyMin+dz*dz);
		float max = (float)Math.sqrt(dx*dx+dyMax*dyMax);
		int[] notAtom = new int[2];
		notAtom[0] = mol.getBondAtom(0, centralBond);
		notAtom[1] = mol.getBondAtom(1, centralBond);

		DistanceRule currentRule = rule[atom[0]][atom[1]];
		if (currentRule == null) {
			rule[atom[0]][atom[1]] = new DistanceRule(atom, notAtom, min, max, PRIORITY_THREE_BONDS);
			}
		else {
			currentRule.mDistance[0] = Math.min(currentRule.mDistance[0], min);
			currentRule.mDistance[1] = Math.min(currentRule.mDistance[1], max);
			}
		}

	/**
	 * Defines a fixed distance rule between the given atoms.
	 * If there is already a distance rule defined for these atoms,
	 * then the priority decides, which one gets precedence. If priorities are
	 * equal, then the distance value and not-lists are merged.
	 * The distance value will be a mean one and the notLists are combined.
	 * If there is a distance range already defined, then it is replaced by
	 * a new fixed distance rule.
	 * @param rule distance rule matrix between all atoms
	 * @param atom both atoms in array with first atom being the larger one
	 * @param notList
	 * @param distance
	 * @param priority
	 */
	private static void setFixedDistance(DistanceRule[][] rule, int[] atom, int[] notList, float distance, int priority) {
		if (rule[atom[0]][atom[1]] == null) {
			rule[atom[0]][atom[1]] = new DistanceRule(atom, notList, distance, priority);
			}
		else if (rule[atom[0]][atom[1]].mDistance.length == 2
			  || rule[atom[0]][atom[1]].mPriority < priority) {
			rule[atom[0]][atom[1]] = new DistanceRule(atom, notList, distance, priority);
			}
		else if (rule[atom[0]][atom[1]].mPriority == priority) {
			rule[atom[0]][atom[1]].mDistance[0] = (rule[atom[0]][atom[1]].mDistance[0] + distance) / 2f;
			rule[atom[0]][atom[1]].mNotList = mergeNotLists(rule[atom[0]][atom[1]].mNotList, notList);
			}
		}

	private static final int[] mergeNotLists(int[] nl1, int[] nl2) {
		if (nl1 == null)
			return nl2;
		if (nl2 == null)
			return nl1;
		int[] nl = new int[nl1.length+nl2.length];
		int index = 0;
		for (int atom:nl1)
			nl[index++] = atom;
		for (int atom:nl2)
			nl[index++] = atom;
		return nl;
		}

	/**
	 * puts atom1 and atom2 into an array, such that the first atom is the larger one
	 * @param atom1
	 * @param atom2
	 * @return
	 */
	private static int[] combineAtoms(int atom1, int atom2) {
		int[] atom = new int[2];
		if (atom1 > atom2) {
			atom[0] = atom1;
			atom[1] = atom2;
			}
		else {
			atom[0] = atom2;
			atom[1] = atom1;
			}
		return atom;
		}

	private static int[] calculateBondRingSizes(StereoMolecule mol) {
		int[] bondRingSize = new int[mol.getBonds()];
		RingCollection ringSet = mol.getRingSet();
		for (int ring=0; ring<ringSet.getSize(); ring++) {
			int[] ringBond = ringSet.getRingBonds(ring);
			for (int i=0; i<ringBond.length; i++) {
				if (bondRingSize[ringBond[i]] == 0
				 || bondRingSize[ringBond[i]] > ringBond.length) {
					bondRingSize[ringBond[i]] = ringBond.length;
					}
				}
			}
		return bondRingSize;
		}

	@Override
	public boolean apply(Conformer conformer, float cycleFactor) {
/*
float strainBefore = addStrain(conformer, new float[conformer.getMolecule().getAllAtoms()]);
*/
	    float dx = conformer.x[mAtom[1]] - conformer.x[mAtom[0]];
		float dy = conformer.y[mAtom[1]] - conformer.y[mAtom[0]];
		float dz = conformer.z[mAtom[1]] - conformer.z[mAtom[0]];
		float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);

		float distanceFactor = 0.0f;
		if (mDistance.length == 2) {	// is min and max
			if (distance < mDistance[0]) {
				distanceFactor = (distance-mDistance[0]) / distance;
				}
			else if (distance > mDistance[1]) {
				distanceFactor = (distance-mDistance[1]) / distance;
				}
			}
		else {	// exact distance
			if (distance < mDistance[0]) {
				distanceFactor = (distance-mDistance[0]) / distance;
				}
			else if (distance > mDistance[0]) {
				distanceFactor = (distance-mDistance[0]) / distance;
				}
			}

		if (Math.abs(distanceFactor) < 0.001)
			return false;

		float factor = cycleFactor * distanceFactor;

// for exclusively moving each of both involved atoms atom 50%
//moveAtom(conformer, mAtom[0], dx*factor/2f, dy*factor/2f, dz*factor/2f);
//moveAtom(conformer, mAtom[1], -dx*factor/2f, -dy*factor/2f, -dz*factor/2f);

		StereoMolecule mol = conformer.getMolecule();

		if (mPriority == PRIORITY_ONE_BOND) {
			if (mol.getAllConnAtoms(mAtom[0]) == 1
			 && mol.getAllConnAtoms(mAtom[1]) != 1) {
				moveAtom(conformer, mAtom[0], dx*factor, dy*factor, dz*factor);
//printStuff(conformer, distance);
				return true;
				}
			if (mol.getAllConnAtoms(mAtom[0]) != 1
			 && mol.getAllConnAtoms(mAtom[1]) == 1) {
				moveAtom(conformer, mAtom[1], -dx*factor, -dy*factor, -dz*factor);
//printStuff(conformer, distance);
				return true;
				}
			}

		factor /= 2f;
		moveGroup(conformer, mAtom[0], mNotList, dx*factor, dy*factor, dz*factor);
		moveGroup(conformer, mAtom[1], mNotList, -dx*factor, -dy*factor, -dz*factor);
/*
float strainAfter = addStrain(conformer, new float[conformer.getMolecule().getAllAtoms()]);
if (strainAfter > 0.0001) System.out.println("Strain before:"+strainBefore+" after:"+strainAfter+" distanceBefore:"+distance+" "+toString());
*/
//printStuff(conformer, distance);
		return true;
		}
/*
private void printStuff(Conformer conformer, float distanceBefore) {
 float dx = conformer.x[mAtom[1]] - conformer.x[mAtom[0]];
 float dy = conformer.y[mAtom[1]] - conformer.y[mAtom[0]];
 float dz = conformer.z[mAtom[1]] - conformer.z[mAtom[0]];
 float distanceAfter = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
 System.out.println("Distance before:"+distanceBefore+" distanceAfter:"+distanceAfter+((mDistance.length==1 && Math.abs(distanceAfter-mDistance[0])>Math.abs(distanceBefore-mDistance[0]))?"!!!!!!!!!!!!!":""));	
}*/

	@Override
	public float addStrain(Conformer conformer, float[] atomStrain) {
		float dx = conformer.x[mAtom[1]] - conformer.x[mAtom[0]];
		float dy = conformer.y[mAtom[1]] - conformer.y[mAtom[0]];
		float dz = conformer.z[mAtom[1]] - conformer.z[mAtom[0]];
		float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
		float totalStrain = 0;
		if (mDistance.length == 2) {
			if (distance < mDistance[0]) {	// van der waals radii
				float strain = (mDistance[0] - distance) / 2.0f;
				float panalty = strain*strain;
				atomStrain[mAtom[0]] += panalty;
				atomStrain[mAtom[1]] += panalty;
				totalStrain += 2*panalty;
/*TLSSystem.out.println(toString()+" distance:"+distance+" strain:"+strain+" panalty:"+panalty);*/
				}
			else if (distance > mDistance[1]) {
				float strain = (distance - mDistance[1]) / 2.0f;
				float panalty = strain*strain;
				atomStrain[mAtom[0]] += panalty;
				atomStrain[mAtom[1]] += panalty;
				totalStrain += 2*panalty;
/*TLSSystem.out.println(toString()+" distance:"+distance+" strain:"+strain+" panalty:"+panalty);*/
				}
			}
		else {
			float strain = (distance - mDistance[0]) / 2.0f;
			if (Math.abs(strain) > 0.005f) {
    			float panalty = strain*strain;
    			atomStrain[mAtom[0]] += panalty;
    			atomStrain[mAtom[1]] += panalty;
    			totalStrain += 2*panalty;
/*TLSSystem.out.println(toString()+" distance:"+distance+" strain:"+strain+" panalty:"+panalty);*/
			    }
			}
		return totalStrain;
		}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("distance rule:");
		super.addAtomList(sb);
		if (mDistance.length == 1)
			sb.append(" distance:"+DoubleFormat.toString(mDistance[0]));
		else
			sb.append(" min:"+DoubleFormat.toString(mDistance[0])+" max:"+DoubleFormat.toString(mDistance[1]));
		return sb.toString();
		}
	}
