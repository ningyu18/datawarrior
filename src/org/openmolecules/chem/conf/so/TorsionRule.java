/*
 * @(#)TorsionRule.java
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
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.chem.conf.TorsionDetail;
import com.actelion.research.chem.conf.TorsionPrediction;

public class TorsionRule extends ConformationRule {
	final static float COLLIDING_ATOM_STRAIN = 0.05f;

	private int		mSmallerSubstituentIndex;
	private int[]	mAtomToRotate;
	private short[]	mTorsion,mFrequency;

	public TorsionRule(short[] torsion, short[] frequency, int[] torsionAtom, int[] atomToRotate, int smallerSubstituentIndex) {
		super(torsionAtom);
		mTorsion = torsion;
		mFrequency = frequency;
		mAtomToRotate = atomToRotate;
		mSmallerSubstituentIndex = smallerSubstituentIndex;
		}

	@Override
	public int getRuleType() {
		return RULE_TYPE_TORSION;
		}

    public static void calculateRules(ArrayList<ConformationRule> ruleList, StereoMolecule mol) {
		TorsionDB.initialize(TorsionDB.MODE_ANGLES | TorsionDB.MODE_FREQUENCIES);

		boolean[] isRotatableBond = new boolean[mol.getAllBonds()];
		TorsionDB.findRotatableBonds(mol, false, isRotatableBond);
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (isRotatableBond[bond]) {
				int[] torsionAtom = new int[4];
				TorsionDetail torsionDetail = new TorsionDetail();
			    String torsionID = TorsionDB.getTorsionID(mol, bond, torsionAtom, torsionDetail);
			    if (torsionID != null) {
			        short[] torsion = TorsionDB.getTorsions(torsionID);
			        short[] frequency = TorsionDB.getTorsionFrequencies(torsionID);

if (torsion == null && !mol.isFragment()) System.out.println("missing torsionID:"+torsionID+" predicting...");

					if (torsion == null) {
						TorsionPrediction prediction = new TorsionPrediction(mol, torsionAtom);
						torsion = prediction.getTorsions();
						frequency = prediction.getTorsionFrequencies();
						}

                    if (torsion != null) {
/*
System.out.print("torsionID:"+torsionID+" torsions:"+torsion[0]);
for (int i=1; i<torsion.length; i++)
System.out.print(","+torsion[i]);
System.out.println();
*/
					    int[] atomToRotate = null;
					    int smallerSubstituentIndex = 0;
					    if (!mol.isRingBond(bond)) {
				    		boolean[][] isMemberAtom = new boolean[2][mol.getAllAtoms()];
				    		int[] count = new int[2];
					    	for (int i=0; i<2; i++)
					    		count[i] = mol.getSubstituent(torsionDetail.getRearAtom(i),
					    				torsionDetail.getCentralAtom(i), isMemberAtom[i], null, null);

					    	smallerSubstituentIndex = (count[0] < count[1]) ? 0 : 1;
					    	atomToRotate = new int[count[smallerSubstituentIndex]];
					    	int index = 0;
					    	for (int a=0; a<mol.getAllAtoms(); a++)
					    		if (isMemberAtom[smallerSubstituentIndex][a])
					    			atomToRotate[index++] = a;
					        }
					    ruleList.add(new TorsionRule(torsion, frequency, torsionAtom, atomToRotate, smallerSubstituentIndex));
                        }
				    }
				}
			}

		// In the CSD torsion table atom sequences cover non-H atoms only.
		// Often this is not a problem, because in sp3 chains distance rules cause
		// hydrogen gauche positioning. If we have an -OH or -NH2 connected to a pi-system,
		// the hydrogen needs to be in the pi-plane. We add artificial torsion rules
		// to rotate the hydrogen of ?=?-X-H into the pi-plane.
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (mol.getBondType(bond) == Molecule.cBondTypeSingle	// does not include up/down-bonds
			 && !mol.isRingBond(bond)) {
				for (int i=0; i<2; i++) {
					int heteroAtom = mol.getBondAtom(i, bond);
					int atomicNo = mol.getAtomicNo(heteroAtom);
					if (atomicNo > 6
					 && mol.getConnAtoms(heteroAtom) == 1) {
						int hCount = mol.getAllConnAtoms(heteroAtom) - 1;
						if (hCount != 0) {	// hetero atom with hydrogen(s) and one non-H neighbor
							int piAtom = mol.getBondAtom(1-i, bond);
							if (mol.getAtomPi(piAtom) == 1) {
								int[] torsionAtom = new int[4];
								torsionAtom[0] = mol.getConnAtom(piAtom, (mol.getConnAtom(piAtom, 0) == heteroAtom) ? 1 : 0);
								torsionAtom[1] = piAtom;
								torsionAtom[2] = heteroAtom;
								torsionAtom[3] = mol.getConnAtom(heteroAtom, 1);	// first H-neighbor

								int[] atomToRotate = new int[hCount];
								for (int j=0; j<hCount; j++)
									atomToRotate[j] = mol.getConnAtom(heteroAtom, mol.getConnAtoms(heteroAtom) + j);

								short[] torsion = new short[2];
								torsion[0] = 0;
								torsion[1] = 180;

								short[] frequency = new short[2];
								frequency[0] = 50;
								frequency[1] = 50;

							    ruleList.add(new TorsionRule(torsion, frequency, torsionAtom, atomToRotate, 1));
								}
							}
						}
					}
				}
			}
    	}

	@Override
	public boolean apply(Conformer conformer, float cycleFactor) {
	    double currentTorsion = TorsionDB.calculateTorsionExtended(conformer, mAtom);
	    if (currentTorsion < 0)
	        currentTorsion += 2 * Math.PI;

	    double optTorsion = Double.NaN;
	    double minDif = Math.PI;
	    for (int i=0; i<mTorsion.length; i++) {
	        double t = Math.PI * mTorsion[i] / 180.0;
	        double dif = Math.abs(currentTorsion - t);
	        if (dif > Math.PI)
	            dif = 2*Math.PI - dif;
	        dif /= Math.sqrt(mFrequency[i]);	// normalize by frequency
	        if (minDif > dif) {
	            minDif = dif;
	            optTorsion = t;
	            }
	        }

	    Coordinates unit = new Coordinates(conformer.x[mAtom[2]] - conformer.x[mAtom[1]],
	    								   conformer.y[mAtom[2]] - conformer.y[mAtom[1]],
	    								   conformer.z[mAtom[2]] - conformer.z[mAtom[1]]).unit();
	    double angleCorrection = optTorsion - currentTorsion;
	    if (Math.abs(angleCorrection) > Math.PI)
	        angleCorrection = (angleCorrection < 0) ? 2*Math.PI + angleCorrection : angleCorrection - 2*Math.PI;

	    if (Math.abs(angleCorrection) < 0.001 * Math.PI)
	    	return false;
	    
	    angleCorrection *= cycleFactor;

	    StereoMolecule mol = conformer.getMolecule();

	    if (mAtomToRotate != null) {	// rotate smaller side of the molecule
    	    double rotation = (mSmallerSubstituentIndex == 0) ? -angleCorrection : angleCorrection;
            for (int a:mAtomToRotate)
            	rotateAtom(conformer, a, mAtom[1], unit, rotation);
	        }
	    else {	// rotate first and second atom shell from bond atoms; reduce angle for second shell atoms and if one side is more rigid
	    	int bond = mol.getBond(mAtom[1], mAtom[2]);
	    	boolean isFiveMemberedRing = (bond != -1 && mol.getBondRingSize(bond) <= 5);
	    	for (int i=1; i<=2; i++) {
		    	float factor = (i==1 ? -2f : 2f) * mol.getAtomRingBondCount(mAtom[i]);
	    	    for (int j=0; j<mol.getAllConnAtoms(mAtom[i]); j++) {
	    	    	int firstConn = mol.getConnAtom(mAtom[i], j);
	    	        if (firstConn != mAtom[3-i]) {
	    	            rotateGroup(conformer, firstConn, mAtom[i], unit, angleCorrection / factor);
	    	            if (!isFiveMemberedRing) {
		    	    	    for (int k=0; k<mol.getConnAtoms(firstConn); k++) {
		    	    	    	int secondConn = mol.getConnAtom(firstConn, k);
		    	    	        if (secondConn != firstConn && mol.getConnAtoms(secondConn) != 1) {
		    	    	            rotateGroup(conformer, secondConn, mAtom[i], unit, angleCorrection / (4f*factor));
		    	    	        	}
		    	    	    	}
	    	    	    	}
	    	        	}
	    	    	}
	    		}
	        }

/*
if (mol.getConnAtoms(mAtom[1])+mol.getConnAtoms(mAtom[2])==4) {
double after = TorsionDB.calculateTorsion(conformer, mAtom);
if (after < 0) after += 2 * Math.PI;
float improvement = Math.abs(Molecule.getAngleDif((float)currentTorsion, (float)optTorsion))-Math.abs(Molecule.getAngleDif((float)after, (float)optTorsion));
System.out.println((mAtomToRotate==null?"ring":"!ring")+" before:"+currentTorsion+" after:"+after+" wanted:"+optTorsion+" correction:"+angleCorrection+" factor:"+cycleFactor+" atoms:"+mAtom[0]+","+mAtom[1]+","+mAtom[2]+","+mAtom[3]+" gain:"+improvement+(improvement<0f?" WORSE!!!":""));
}
*/
	    return true;
		}

	/**
	 * Rotate atom and all of its exclusive neighbor atoms.
	 * @param conformer
	 * @param atom
	 * @param refAtom
	 * @param unit
	 * @param theta
	 */
	private void rotateGroup(Conformer conformer, int atom, int refAtom, Coordinates unit, double theta) {
		rotateAtom(conformer, atom, refAtom, unit, theta);
	    StereoMolecule mol = conformer.getMolecule();
        for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
        	int connAtom = mol.getConnAtom(atom, i);
        	if (mol.getAllConnAtoms(connAtom) == 1)
        		rotateAtom(conformer, connAtom, refAtom, unit, theta);
        	}
		}

	private void rotateAtom(Conformer conformer, int atom, int refAtom, Coordinates unit, double theta) {
        double x = unit.x;
        double y = unit.y;
        double z = unit.z;
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        double t = 1-c;
        double mx = conformer.x[atom] - conformer.x[refAtom];
        double my = conformer.y[atom] - conformer.y[refAtom];
        double mz = conformer.z[atom] - conformer.z[refAtom];
        conformer.x[atom] = conformer.x[refAtom] + (float)((t*x*x+c)*mx + (t*x*y+s*z)*my + (t*x*z-s*y)*mz);
        conformer.y[atom] = conformer.y[refAtom] + (float)((t*x*y-s*z)*mx + (t*y*y+c)*my + (t*y*z+s*x)*mz);
        conformer.z[atom] = conformer.z[refAtom] + (float)((t*x*z+s*y)*mx + (t*z*y-s*x)*my + (t*z*z+c)*mz);
	    }

	@Override
	public float addStrain(Conformer conformer, float[] atomStrain) {
	    double torsion = 180 * TorsionDB.calculateTorsionExtended(conformer, mAtom) / Math.PI;
	    if (torsion < 0)
	        torsion += 360;
	    double minDif = Integer.MAX_VALUE;
	    for (int i=0; i<mTorsion.length; i++) {
	        double dif = Math.abs(torsion - mTorsion[i]);
	        if (dif > 180)
	            dif = 360 - dif;
	        if (minDif > dif)
	            minDif = dif;
	        }
        if (minDif > 60)
            minDif = 60;
	    float totalStrain = 0;
	    StereoMolecule mol = conformer.getMolecule();
	    double penalty = minDif*minDif/14400;	// make a 60 degree dif penalty the same as 0.5 Angstrom distance penalty
	    for (int i=0; i<mol.getAllConnAtoms(mAtom[1]); i++) {
	        if (mol.getConnAtom(mAtom[1], i) != mAtom[2]) {
	        	atomStrain[mol.getConnAtom(mAtom[1], i)] += penalty;
				totalStrain += penalty;
	        	}
	    	}
        for (int i=0; i<mol.getAllConnAtoms(mAtom[2]); i++) {
            if (mol.getConnAtom(mAtom[2], i) != mAtom[1]) {
            	atomStrain[mol.getConnAtom(mAtom[2], i)] += penalty;
				totalStrain += penalty;
		    	}
			}
		return totalStrain;
		}

	public boolean disableIfColliding(SelfOrganizedConformer conformer) {
	    StereoMolecule mol = conformer.getMolecule();
		float maxAtomStrain = 0;
		for (int i=1; i<=2; i++) {
		    for (int j=0; j<mol.getAllConnAtoms(mAtom[i]); j++) {
		    	int connAtom = mol.getConnAtom(mAtom[i], j);
		        if (connAtom != mAtom[3-i] && conformer.getAtomStrain(connAtom) > maxAtomStrain)
		        	maxAtomStrain = conformer.getAtomStrain(connAtom);
		    	}
			}
		if (maxAtomStrain < COLLIDING_ATOM_STRAIN)
			return false;

		mIsEnabled = false;
		return true;
		}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("torsion rule:");
		super.addAtomList(sb);
        sb.append(" torsions:"+mTorsion[0]);
        for (int i=1; i<mTorsion.length; i++)
            sb.append(","+mTorsion[i]);
		return sb.toString();
		}
	}

final class Coordinates {
	public float x,y,z;
	
	public Coordinates(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		}
	
	public final Coordinates unit() {
		float d = (float)Math.sqrt(x*x + y*y + z*z);
		if (d==0) {
			System.err.println("Cannot call unit() on a null vector");
			return new Coordinates(1,0,0);
			}

		return new Coordinates(x/d, y/d, z/d);		
		}
	}
