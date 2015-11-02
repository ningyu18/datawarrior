/*
 * @(#)ConformationSelfOrganizer.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDescriptor;

public class ConformationSelfOrganizer {
	private static final int    INITIAL_POOL_SIZE = 4;
	private static final int    MAX_CONFORMER_TRIES = 5;
	private static final int    MAX_BREAKOUT_ROUNDS = 3;
	private static final int    PREPARATION_CYCLES = 40;		// without ambiguous rules (i.e. torsion)
	private static final int    PRE_OPTIMIZATION_CYCLES = 20;	// with ambiguous rules before breakout
	private static final int    BREAKOUT_CYCLES = 20;
	private static final int    OPTIMIZATION_CYCLES = 100;
	private static final int    MINIMIZATION_CYCLES = 20;
	private static final float	STANDARD_CYCLE_FACTOR = 1.0f;
	private static final float	MINIMIZATION_REDUCTION = 20.0f;
	private static final float	ATOM_BREAKOUT_STRAIN = 0.5f;
	private static final float	BREAKOUT_DISTANCE = 5f;

private static final boolean WRITE_DW_FILE = false;
private BufferedWriter mDWWriter;
private int mDWCycle;
private float[] mDWStrain; 	// TODO get rid of this

	private StereoMolecule		mMol;
    private Random				mRandom;
    private int					mMaxConformers;
    private boolean				mPoolIsClosed;
	private ArrayList<ConformationRule> mRuleList;
	private ArrayList<SelfOrganizedConformer> mConformerList;
	private int[]				mRuleCount;
	private boolean[]			mSkipRule;
	private int[]				mRotatableBondForDescriptor;

	/**
	 * Generates a new ConformationSelfOrganizer from the given molecule.
	 * Explicit hydrogens are removed from the molecule, unless the
	 * keepHydrogen flag is set.<br>
	 * One conformer can be generated with the getOneConformer()
	 * or getOneConformerInPlace(). <br>
	 * Multiple different conformers can be generated with initializeConformers()
	 * and getNextConformer(). In this case conformers are considered different
	 * if at least one dihedral angle of a rotatable bond is substantially different.
	 * If atoms of the molecule are marked, these are not considered part of the molecule,
	 * when the rotatable bonds for the difference check are located.
	 * @param mol
	 */
	public ConformationSelfOrganizer(final StereoMolecule mol, boolean keepHydrogen) {

/*// winkel zwischen zwei vektoren:
final float[] v1 = { Math.sqrt(3.0)/2.0, 0.5, 0 };
final float[] v2 = { -1, 0, 1 };
float cosa = (v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2])
			/ (Math.sqrt(v1[0]*v1[0]+v1[1]*v1[1]+v1[2]*v1[2])
			 * Math.sqrt(v2[0]*v2[0]+v2[1]*v2[1]+v2[2]*v2[2]));
float a = Math.acos(cosa);
System.out.println("angle:"+a+"  in degrees:"+(a*180/Math.PI));
*/
		mMol = mol;
		if (!keepHydrogen)
		    mMol.removeExplicitHydrogens();
		mMol.ensureHelperArrays(Molecule.cHelperParities);

		mRuleList = new ArrayList<ConformationRule>();
		mSkipRule = new boolean[ConformationRule.RULE_NAME.length];
		mRuleCount = new int[ConformationRule.RULE_NAME.length];

		DistanceRule.calculateRules(mRuleList, mol);
		mRuleCount[ConformationRule.RULE_TYPE_DISTANCE] = mRuleList.size();

		mRuleCount[ConformationRule.RULE_TYPE_PLANE] = -mRuleList.size();
		PlaneRule.calculateRules(mRuleList, mol);
		mRuleCount[ConformationRule.RULE_TYPE_PLANE] += mRuleList.size();

		mRuleCount[ConformationRule.RULE_TYPE_LINE] = -mRuleList.size();
		StraightLineRule.calculateRules(mRuleList, mol);
		mRuleCount[ConformationRule.RULE_TYPE_LINE] += mRuleList.size();

		mRuleCount[ConformationRule.RULE_TYPE_STEREO] = -mRuleList.size();
		StereoRule.calculateRules(mRuleList, mol);
		mRuleCount[ConformationRule.RULE_TYPE_STEREO] += mRuleList.size();

		mRuleCount[ConformationRule.RULE_TYPE_TORSION] = -mRuleList.size();
		TorsionRule.calculateRules(mRuleList, mol);
		mRuleCount[ConformationRule.RULE_TYPE_TORSION] += mRuleList.size();

//		listRules();
		}

	/**
	 * @return returns the molecule that was passed to the constructor.
	 */
	public StereoMolecule getMolecule() {
		return mMol;
		}

/*	private void listRules() {
		System.out.println("---------------------------------------------------------------------");
		for (int i=0; i<mMol.getAllAtoms(); i++) {
			System.out.print(""+i+" "+Molecule.cAtomLabel[mMol.getAtomicNo(i)]);
			for (int j=0; j<mMol.getAllConnAtoms(i); j++) {
				int connBond = mMol.getConnBond(i, j);
				if (mMol.isAromaticBond(connBond))
					System.out.print(" .");
				else if (mMol.getBondOrder(connBond) == 1)
					System.out.print(" -");
				else if (mMol.getBondOrder(connBond) == 2)
					System.out.print(" =");
				System.out.print(""+mMol.getConnAtom(i, j));
				}
			if (mMol.getAtomParity(i) != 0)
				System.out.print(" parity:"+mMol.getAtomParity(i));
			System.out.println();
			}
		System.out.print("CanRanks:");
		mMol.ensureHelperArrays(Molecule.cHelperBitSymmetrySimple);
		for (int i=0; i<mMol.getAtoms(); i++) {
			System.out.print(" "+i+":"+mMol.getSymmetryRank(i));
			if (mMol.getAtomParity(i) != 0)
				System.out.print(" absTHP:"+mMol.getAbsoluteAtomParity(i));
			}
		System.out.println();
    	BondLengthSet bondLengthSet = new BondLengthSet(mMol);
		BondAngleSet bondAngleSet = new BondAngleSet(mMol, bondLengthSet);
		System.out.println("Angles:");
		for (int i=0; i<mMol.getAtoms(); i++) {
		for (int j=1; j<mMol.getAllConnAtoms(i); j++) {
		for (int k=0; k<j; k++) {
		System.out.print(bondAngleSet.getConnAngle(i, j, k)+" ");
		}}
		System.out.println();
		}

		for (ConformationRule rule:mRuleList)
			System.out.println(rule.toString());
		}*/

	/**
	 * This convenience method returns the StereoMolecule that has been passed
	 * to the constructor after modifying its atom coordinates
	 * to reflect the conformer internally created by generateOneConformer().
	 * @return 
	 */
	public StereoMolecule generateOneConformerInPlace(long randomSeed) {
		SelfOrganizedConformer conformer = generateOneConformer(randomSeed);
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			mMol.setAtomX(atom, conformer.x[atom]);
			mMol.setAtomY(atom, conformer.y[atom]);
			mMol.setAtomZ(atom, conformer.z[atom]);
			}
		return mMol;
		}

	/**
	 * Generates the coordinates for one conformer in the calling thread.
	 * This is done by trying MAX_CONFORMER_TRIES times to create a random
	 * conformer that meets MAX_ATOM_STRAIN and MAX_TOTAL_STRAIN criteria.
	 * If one is found it is returned. Otherwise the conformer with the lowest
	 * total strain is returned.
     * @param randomSeed 0 or specific seed
	 */
	public SelfOrganizedConformer generateOneConformer(long randomSeed) {
        mRandom = (randomSeed == 0) ? new Random() : new Random(randomSeed);

        SelfOrganizedConformer conformer = new SelfOrganizedConformer(mMol);

		if (WRITE_DW_FILE) {
			try {
				writeDWFileStart();
				mDWCycle = 0;
				tryGenerateConformer(conformer);
				writeDWFileEnd();
				mDWWriter.close();
				return conformer;
				}
			catch (IOException e) {
				e.printStackTrace();
				return null;
				}
			}

		SelfOrganizedConformer bestConformer = null;
		for (int i=0; i<MAX_CONFORMER_TRIES; i++) {
			if (tryGenerateConformer(conformer))
				return conformer;	// sufficiently low strain, we take this and don't try generating better ones

			if (bestConformer == null) {
				bestConformer = conformer;
				conformer = new SelfOrganizedConformer(mMol);
				}
			else if (bestConformer.isWorseThan(conformer)) {
				SelfOrganizedConformer tempConformer = bestConformer;
				bestConformer = conformer;
				conformer = tempConformer;
				}
			}

		return bestConformer;
		}

	/**
	 * Needs to be called, before getting individual conformers of the same molecule by
	 * getNextConformer(). Depending on the flexibility of the molecule, this method creates
	 * a small pool of random conformers, of which getNextConformer() always picks that
	 * conformer that is an optimum concerning atom strain and diversity to the already picked ones.
	 * If the pool is getting low on conformers, then new ones are generated on-the-fly.
	 * @param randomSeed use a value != 0 for a reproducible random number sequence
	 * @param maxConformers -1 to automatically generate maximum on degrees of freedom
	 */
	public void initializeConformers(long randomSeed, int maxConformers) {
        mRandom = (randomSeed == 0) ? new Random() : new Random(randomSeed);

        mConformerList = new ArrayList<SelfOrganizedConformer>();
        mPoolIsClosed = false;

		int freeBondCount = 0;
		int ringBondCount = 0;
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (!mMol.isAromaticBond(bond)
			 && mMol.getBondOrder(bond) == 1
			 && mMol.getConnAtoms(mMol.getBondAtom(0, bond)) > 1
			 && mMol.getConnAtoms(mMol.getBondAtom(1, bond)) > 1) {
				if (!mMol.isRingBond(bond))
					freeBondCount++;
				else if (mMol.getBondRingSize(bond) > 4)
					ringBondCount++;
				}
			}

		mMaxConformers = (maxConformers == -1) ? (1 << (1+freeBondCount+ringBondCount/2)) : maxConformers;
		increaseConformerPool(Math.min(INITIAL_POOL_SIZE, mMaxConformers));
		}

	private void increaseConformerPool(int newConformerCount) {
		SelfOrganizedConformer bestRefusedConformer = null;
		SelfOrganizedConformer conformer = null;
		int finalPoolSize = mConformerList.size() + newConformerCount;
		int tryCount = newConformerCount * MAX_CONFORMER_TRIES;
		for (int i=0; i<tryCount && mConformerList.size()<finalPoolSize; i++) {
			if (conformer == null)
				conformer = new SelfOrganizedConformer(mMol);
			if (tryGenerateConformer(conformer)) {
				if (mRotatableBondForDescriptor == null)
					mRotatableBondForDescriptor = TorsionDescriptor.getRotatableBonds(getMolecule());
				conformer.calculateDescriptor(mRotatableBondForDescriptor);
				boolean isNew = true;
				for (SelfOrganizedConformer c:mConformerList) {
					if (conformer.equals(c)) {
						isNew = false;
						break;
						}
					}
				if (isNew)
					mConformerList.add(conformer);
				conformer = null;
				}
			else if (mConformerList.isEmpty()) {
				if (bestRefusedConformer == null) {
					bestRefusedConformer = conformer;
					conformer = null;
					}
				else if (bestRefusedConformer.isWorseThan(conformer)) {
					SelfOrganizedConformer tempConformer = bestRefusedConformer;
					bestRefusedConformer = conformer;
					conformer = tempConformer;
					}
				}
			}
		if (mConformerList.isEmpty())
			mConformerList.add(bestRefusedConformer);
		if (mConformerList.size() < finalPoolSize
		 || mConformerList.size() == mMaxConformers)
			mPoolIsClosed = true;
		}

	/**
	 * Picks a new conformer from the conformer pool created by initializeConformers().
	 * Low strain conformers and conformers being most different from already selected ones
	 * are selected first. If the pool is getting short on conformers, new conformers are
	 * created as long as molecule flexibility allows. If a representative set of low strain
	 * molecules have been picked, this method returns null, provided that at least one conformer
	 * was returned.
	 * @return
	 */
	public SelfOrganizedConformer getNextConformer() {
		if (mConformerList == null)
			return null;

		if (!mPoolIsClosed)
			increaseConformerPool(1);

		SelfOrganizedConformer bestConformer = null;
		for (SelfOrganizedConformer conformer:mConformerList) {
			if (!conformer.isUsed()
			 && (bestConformer == null || bestConformer.isWorseThan(conformer))) {
				bestConformer = conformer;
				}
			}

		if (bestConformer != null)
			bestConformer.setUsed(true);
		else
			mConformerList = null;

		return bestConformer;
		}

	private void writeDWFileStart() throws IOException {
        mDWWriter = new BufferedWriter(new FileWriter("/home/thomas/data/ccdc/conformationSamplerDebug.dwar"));
        mDWWriter.write("<column properties>");
        mDWWriter.newLine();
        mDWWriter.write("<columnName=\"Structure\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnProperty=\"specialType\tidcode\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnName=\"before\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnProperty=\"specialType\tidcoordinates3D\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnProperty=\"parent\tStructure\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnName=\"after\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnProperty=\"specialType\tidcoordinates3D\">");
        mDWWriter.newLine();
        mDWWriter.write("<columnProperty=\"parent\tStructure\">");
        mDWWriter.newLine();
        mDWWriter.write("</column properties>");
        mDWWriter.newLine();
        mDWWriter.write("Structure\tbefore\tafter\tcycle\truleName\truleAtoms\truleDetail");
        for (int i=0; i<ConformationRule.RULE_NAME.length; i++)
            mDWWriter.write("\t"+ConformationRule.RULE_NAME[i]);
        mDWWriter.write("\ttotalStrain\tstrainGain");
        mDWWriter.newLine();
        }


	private void writeDWFileEnd() throws IOException {
		mDWWriter.write("<datawarrior properties>");
        mDWWriter.newLine();
		mDWWriter.write("<axisColumn_2D View_0=\"cycle\">");
        mDWWriter.newLine();
		mDWWriter.write("<axisColumn_2D View_1=\"totalStrain\">");
        mDWWriter.newLine();
		mDWWriter.write("<chartType_2D View=\"scatter\">");
        mDWWriter.newLine();
		mDWWriter.write("<colorColumn_2D View=\"ruleName\">");
        mDWWriter.newLine();
		mDWWriter.write("<colorCount_2D View=\"3\">");
        mDWWriter.newLine();
		mDWWriter.write("<colorListMode_2D View=\"Categories\">");
        mDWWriter.newLine();
		mDWWriter.write("<color_2D View_0=\"-11992833\">");
        mDWWriter.newLine();
		mDWWriter.write("<color_2D View_1=\"-65494\">");
        mDWWriter.newLine();
		mDWWriter.write("<color_2D View_2=\"-16732826\">");
        mDWWriter.newLine();
		mDWWriter.write("<detailView=\"height[Data]=0.4;height[before]=0.3;height[after]=0.3\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainSplitting=\"0.71712\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainView=\"2D View\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewCount=\"2\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewDockInfo0=\"root\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewDockInfo1=\"Table	center\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewName0=\"Table\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewName1=\"2D View\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewType0=\"tableView\">");
        mDWWriter.newLine();
		mDWWriter.write("<mainViewType1=\"2Dview\">");
        mDWWriter.newLine();
		mDWWriter.write("<rightSplitting=\"0\">");
        mDWWriter.newLine();
		mDWWriter.write("<rowHeight_Table=\"80\">");
        mDWWriter.newLine();
        mDWWriter.write("<filter0=\"#category#\truleName\">");
        mDWWriter.newLine();
        mDWWriter.write("<connectionColumn_2D View=\"<connectAll>\">");
        mDWWriter.newLine();
        mDWWriter.write("<connectionLineWidth_2D View=\"0.17640000581741333\">");
        mDWWriter.newLine();
   		mDWWriter.write("<logarithmicView=\"totalStrain\">");
        mDWWriter.newLine();
		mDWWriter.write("<markersize_2D View=\"0.1936\">");
        mDWWriter.newLine();
		mDWWriter.write("<sizeAdaption_2D View=\"false\">");
        mDWWriter.newLine();
		mDWWriter.write("</datawarrior properties>");
        mDWWriter.newLine();
		}

	/**
	 * Tries to create one random conformer in current thread by once going through
	 * sequence of preparation, break-out, optimization and fine-tuning phases.
	 * Stops successfully early, if none of the atoms' strain exceeds MAX_ATOM_STRAIN
	 * and the conformer's total strain is below MAX_TOTAL_STRAIN.
	 * Returns false, if after going though all phases still one of these two conditions
	 * is not met.
	 * @param conformer receives coordinates of the conformer
	 * @param threadData carries all thread specific data
	 * @return true if conformer with satisfactory low strain could be generated
	 */
	private boolean tryGenerateConformer(SelfOrganizedConformer conformer) {
		if (mMol.getAllAtoms() < 2)
			return true;

		jumbleAtoms(conformer);

		mSkipRule[ConformationRule.RULE_TYPE_TORSION] = true;

		optimize(conformer, PREPARATION_CYCLES, STANDARD_CYCLE_FACTOR, 1f);

		boolean done = false;

		if (mRuleCount[ConformationRule.RULE_TYPE_TORSION] != 0) {
			mSkipRule[ConformationRule.RULE_TYPE_TORSION] = false;
			done = optimize(conformer, PRE_OPTIMIZATION_CYCLES, STANDARD_CYCLE_FACTOR, 1f);
			}

		for (int i=0; !done && i<MAX_BREAKOUT_ROUNDS; i++) {
			if (jumbleStrainedAtoms(conformer) == 0)
				break;

			done = optimize(conformer, BREAKOUT_CYCLES, STANDARD_CYCLE_FACTOR, 1f);
			}

		if (!done)
			done = optimize(conformer, OPTIMIZATION_CYCLES, STANDARD_CYCLE_FACTOR, 1f);

		if (!done)
			done = optimize(conformer, MINIMIZATION_CYCLES, STANDARD_CYCLE_FACTOR, MINIMIZATION_REDUCTION);

		return done;
		}

	public boolean optimize(SelfOrganizedConformer conformer, int cycles, float startFactor, float factorReduction) {
		int atomsSquare = mMol.getAllAtoms() * mMol.getAllAtoms();

		float k = (float)Math.log(factorReduction)/(float)cycles;

		for (int outerCycle=0; outerCycle<cycles; outerCycle++) {
			float cycleFactor = startFactor * (float)Math.exp(-k*outerCycle);

			for (int innerCycle=0; innerCycle<atomsSquare; innerCycle++) {
				ConformationRule rule = mRuleList.get((int)(mRandom.nextFloat() * mRuleList.size()));

				if (rule.isEnabled() && !mSkipRule[rule.getRuleType()]) {
Conformer oldConformer = (mDWWriter == null) ? null : new Conformer(conformer);

//System.out.println("#1 rule:"+rule.toString());
					boolean conformerChanged = rule.apply(conformer, cycleFactor);
//System.out.println("atom 2  x:"+conformer.x[2]+" y:"+conformer.y[2]+" z:"+conformer.z[2]);
//System.out.println("atom 5  x:"+conformer.x[5]+" y:"+conformer.y[5]+" z:"+conformer.z[5]);
//System.out.println("atom 10 x:"+conformer.x[10]+" y:"+conformer.y[10]+" z:"+conformer.z[10]);
//System.out.println("#2");

					if (conformerChanged)
						conformer.invalidateStrain();

try { if (mDWWriter != null && conformerChanged) writeStrains(oldConformer, conformer, rule); } catch (Exception e) { e.printStackTrace(); }
					}
				}

			if (conformer.isAcceptable(mRuleList))
				return true;
			}

		return false;
		}

	private void writeStrains(Conformer oldConformer, SelfOrganizedConformer newConformer, ConformationRule rule) throws Exception {
		newConformer.calculateStrain(mRuleList);
		float[] strain = new float[ConformationRule.RULE_NAME.length];
		float strainSum = 0f;
		for (int i=0; i<ConformationRule.RULE_NAME.length; i++) {
			strain[i] = newConformer.getRuleStrain(i);
			strainSum += strain[i];
			}

		float oldStrainSum = 0f;
		if (mDWStrain != null)
			for (int i=0; i<mDWStrain.length; i++)
				oldStrainSum += mDWStrain[i];

        String ruleName = ConformationRule.RULE_NAME[rule.getRuleType()];

		StereoMolecule mol = mMol.getCompactCopy();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			if (mol.getAtomicNo(atom) == 1)
				mol.setAtomicNo(atom, 9);
			}

		String atoms = "";
	    if (rule != null) {
	    	int[] atomList = rule.getAtomList();
	        for (int i=0; i<atomList.length; i++) {
	            if (i!=0) atoms = atoms + ",";
	            atoms = atoms+atomList[i];
	            if (atomList[i] != -1)
	            	mol.setAtomicNo(atomList[i], 5);
	        	}
	    	}

		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, oldConformer.x[atom]);
			mol.setAtomY(atom, oldConformer.y[atom]);
			mol.setAtomZ(atom, oldConformer.z[atom]);
			}
		Canonizer oldCanonizer = new Canonizer(mol);
		String idcode = oldCanonizer.getIDCode();
		String oldCoords = oldCanonizer.getEncodedCoordinates();

		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, newConformer.x[atom]);
			mol.setAtomY(atom, newConformer.y[atom]);
			mol.setAtomZ(atom, newConformer.z[atom]);
			}
		Canonizer newCanonizer = new Canonizer(mol);
		String newCoords = newCanonizer.getEncodedCoordinates();

        mDWWriter.write(idcode+"\t"+oldCoords+"\t"+newCoords+"\t"+mDWCycle+"\t"+ruleName+"\t"+atoms+"\t"+rule.toString());
        for (float s:strain)
        	mDWWriter.write("\t"+s);
        mDWWriter.write("\t"+strainSum+"\t"+(oldStrainSum-strainSum));
        mDWWriter.newLine();
	    mDWStrain = strain;
	    mDWCycle++;
	    }

	private void jumbleAtoms(SelfOrganizedConformer conformer) {
		float boxSize = 1.0f + 3.0f * (float)Math.sqrt(mMol.getAllAtoms());
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			conformer.x[atom] = boxSize * mRandom.nextFloat() - boxSize / 2;
			conformer.y[atom] = boxSize * mRandom.nextFloat() - boxSize / 2;
			conformer.z[atom] = boxSize * mRandom.nextFloat() - boxSize / 2;
			}

		conformer.invalidateStrain();
		}

	private int jumbleStrainedAtoms(SelfOrganizedConformer conformer) {
		conformer.calculateStrain(mRuleList);

		int atomCount = 0;
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			if (conformer.getAtomStrain(atom) > ATOM_BREAKOUT_STRAIN) {
				conformer.x[atom] += BREAKOUT_DISTANCE * mRandom.nextFloat() - BREAKOUT_DISTANCE / 2;
				conformer.y[atom] += BREAKOUT_DISTANCE * mRandom.nextFloat() - BREAKOUT_DISTANCE / 2;
				conformer.z[atom] += BREAKOUT_DISTANCE * mRandom.nextFloat() - BREAKOUT_DISTANCE / 2;
				atomCount++;
				}
			}

		if (atomCount != 0)
			conformer.invalidateStrain();

		return atomCount;
		}

	public boolean disableCollidingTorsionRules(SelfOrganizedConformer conformer) {
		boolean found = false;
		conformer.calculateStrain(mRuleList);
		StereoMolecule mol = mMol;
		boolean[] isInvolvedAtom = new boolean[mol.getAllAtoms()];
		for (ConformationRule rule:mRuleList) {
			if (rule instanceof TorsionRule) {
				if (((TorsionRule)rule).disableIfColliding(conformer)) {
					int[] atom = rule.getAtomList();
					for (int i=1; i<=2; i++) {
					    for (int j=0; j<mol.getAllConnAtoms(atom[i]); j++) {
					    	int connAtom = mol.getConnAtom(atom[i], j);
					        if (connAtom != atom[3-i])
					        	isInvolvedAtom[connAtom] = true;
					    	}
						}
					found = true;
					}
				}
			}
		if (found) {
			for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
				if (isInvolvedAtom[atom]) {
					conformer.x[atom] += 0.6f * mRandom.nextFloat() - 0.3f;
					conformer.y[atom] += 0.6f * mRandom.nextFloat() - 0.3f;
					conformer.z[atom] += 0.6f * mRandom.nextFloat() - 0.3f;
					}
				}
			}
		return found;
		}

	public void disableTorsionRules() {
		for (ConformationRule rule:mRuleList)
			if (rule instanceof TorsionRule)
				rule.setEnabled(false);
		}

	public boolean disablePlaneRules() {
		boolean found = false;
		for (ConformationRule rule:mRuleList) {
			if (rule instanceof PlaneRule) {
				rule.setEnabled(false);
				found = true;
				}
			}
		return found;
		}

	public void enableTorsionRules() {
		for (ConformationRule rule:mRuleList)
			if (rule instanceof TorsionRule)
				rule.setEnabled(true);
		}
	}
