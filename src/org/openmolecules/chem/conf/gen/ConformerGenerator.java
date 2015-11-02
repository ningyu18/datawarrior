/*
 * @(#)ConformerGenerator.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property
 * of openmolecules.org. The intellectual and technical concepts contained
 * herein are proprietary to openmolecules.org.
 * Actelion Pharmaceuticals Ltd. is granted a non-exclusive, not transferable
 * and timely unlimited usage license.
 *
 * @author Thomas Sander
 */

package org.openmolecules.chem.conf.gen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;

import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;
import org.openmolecules.chem.conf.so.SelfOrganizedConformer;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.util.IntArrayComparator;

/**
 * This class generates 3D-conformers of a given molecule using the following strategy:
 * <li>All rotatable, non-ring bonds are determined.
 * <li>The fragments between rotatable bonds are considered rigid.
 * <li>For every fragment the relative atom coordinates are determined using a self organization based algorithm.
 * <li>For every rotatable bond a list of preferred torsion angles is determined based on from a CSD statistics of similar bond environments.
 * <li>For individual torsion values likelihoods are estimated based on frequency and atom collisions of vicinal fragments.
 * <li>A dedicated (systematic, biased or random) torsion set strategy delivers collision-free torsion sets, i.e. conformers.
 */
public class ConformerGenerator {
	public static final int STRATEGY_LIKELY_SYSTEMATIC = 1;
	public static final int STRATEGY_PURE_RANDOM = 2;
	public static final int STRATEGY_LIKELY_RANDOM = 3;
	public static final int STRATEGY_ADAPTIVE_RANDOM = 4;

	protected static final float VDW_TOLERANCE_HYDROGEN = 0.85f;  // factor on VDW radii for minimum tolerated non bound atom distances
	protected static final float VDW_TOLERANCE_OTHER = 0.80f;  // factor on VDW radii for minimum tolerated non bound atom distances

	private StereoMolecule		mMolecule;
	private TreeMap<int[],Conformer> mBaseConformerMap;
	private RotatableBond[]		mRotatableBond;
	private Rigid3DFragment[]	mRigidFragment;
	private ConformationSelfOrganizer mNoRotatableBondSampler;
	private TorsionSetStrategy	mTorsionSetStrategy;
	private long				mRandomSeed;
	private int					mDisconnectedFragmentCount,mConformerCount;
	private float				mContribution,mCollisionIntensitySum;
	private int[]				mFragmentNo,mDisconnectedFragmentNo,mDisconnectedFragmentSize;
	private boolean[][]			mSkipCollisionCheck;

public String mDiagnosticCollisionString,mDiagnosticTorsionString;	// TODO get rid of this
public int[] mDiagnosticCollisionAtoms;	// TODO get rid of this
private boolean mWriteDWFragmentsFile = false;
public void setWriteFragmentFile() {
 mWriteDWFragmentsFile = true;
 }

	/**
	 * Adds explicit hydrogen atoms where they are implicit by filling valences
	 * and adapting for atom charges. New hydrogen atoms receive new 2D-coordinates
	 * by equally locating them between those two neighbors with the widest angle between
	 * their bonds. Any stereo configurations deducible from 2D-coordinates are retained.
	 * @param mol
	 */
	public static void addHydrogenAtoms(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int[] implicitHydrogen = new int[mol.getAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++)
			implicitHydrogen[atom] = mol.getImplicitHydrogens(atom);
		float avbl = mol.getAverageBondLength();
		for (int atom=0; atom<implicitHydrogen.length; atom++) {
			int firstNewHydrogen = mol.getAllAtoms();
			for (int i=0; i<implicitHydrogen[atom]; i++)
				mol.addBond(atom, mol.addAtom(1), Molecule.cBondTypeSingle);
			if (implicitHydrogen[atom] != 0)
				setHydrogenLocations(mol, atom, firstNewHydrogen, implicitHydrogen[atom], avbl);
			}
		}

	/**
	 * Finds the widest open angle between all connected non-stereo bonds of atom, divides this angle
	 * into hydrogenCount+1 equals parts and sets atom coordinates of hydrogenCount new hydrogen atoms
	 * such, that they equally occupy the space. Helper arrays are assumed to have the state before
	 * adding any new hydrogen atoms.
	 * @param mol
	 * @param atom
	 * @param firstNewHydrogen atom index of first new hydrogen
	 * @param newHydrogenCount new hydrogen atoms added to atom
	 * @param avbl
	 */
	private static void setHydrogenLocations(StereoMolecule mol, int atom, int firstNewHydrogen, int newHydrogenCount, float avbl) {
		int stereoBondCount = 0;
		for (int i=0; i<mol.getAllConnAtoms(atom); i++)
			if (mol.isStereoBond(mol.getConnBond(atom,i)))
				stereoBondCount++;
		
		float[] angle = null;
		if (stereoBondCount < mol.getAllConnAtoms(atom)) {
			angle = new float[mol.getAllConnAtoms(atom)-stereoBondCount];
			int bond = 0;
			for (int i=0; i<mol.getAllConnAtoms(atom); i++)
				if (!mol.isStereoBond(mol.getConnBond(atom,i)))
					angle[bond++] = mol.getBondAngle(atom, mol.getConnAtom(atom,i));
	
			Arrays.sort(angle);
			}

		float angleIncrement = 2f*(float)Math.PI/newHydrogenCount;
		float startAngle = 0f;

		if (angle != null) {
			float biggestAngleDif = 0f;
			for (int i=0; i<angle.length; i++) {
				float a1 = (i == 0) ? angle[angle.length-1] - (float)Math.PI*2f : angle[i-1];
				float a2 = angle[i];
				if (biggestAngleDif < a2 - a1) {
					biggestAngleDif = a2 - a1;
					startAngle = a1;
					}
				}
			angleIncrement = biggestAngleDif / (newHydrogenCount + 1);
			}

		for (int i=0; i<newHydrogenCount; i++) {
			startAngle += angleIncrement;
			mol.setAtomX(firstNewHydrogen+i, mol.getAtomX(atom) + avbl * (float)Math.sin(startAngle));
			mol.setAtomY(firstNewHydrogen+i, mol.getAtomY(atom) + avbl * (float)Math.cos(startAngle));
			}
		}

	public static float getToleratedVDWRadius(int atomicNo) {
		return VDWRadii.VDW_RADIUS[atomicNo] * (atomicNo == 1 ? VDW_TOLERANCE_HYDROGEN : VDW_TOLERANCE_OTHER);
		}

	/**
	 * Instantiates a ConformerGenerator for creating not reproducible conformers.
	 */
	public ConformerGenerator() {
		this(0L);
		}

	/**
	 * @param seed != 0L if conformers shall be created in a reproducible way
	 */
	public ConformerGenerator(long seed) {
		TorsionDB.initialize(TorsionDB.MODE_ANGLES | TorsionDB.MODE_RANGES | TorsionDB.MODE_FREQUENCIES);
		mRandomSeed = seed;
		Rigid3DFragment.setRandomSeed(seed);
		}

	/**
	 * Fills all free valences of mol with explicit hydrogens and tries to
	 * create a reasonable conformer by starting with the most likely torsion set.
	 * If there are collisions, then less likely torsions are tried to find
	 * a collision free conformer. If it succeeds, mol receives the modified
	 * atom coordinates and mol is returned. If the conformer generation fails,
	 * then null is returned. The torsion strategy used is STRATEGY_ADAPTIVE_RANDOM.
	 * New 3D-coordinates correctly reflect E/Z and R/S bond/atom parities.
	 * This is a convenience method that does not require any initialization.
	 * @param mol null in case of a structure problem or the molecule that gets new 3D coordinates in place
	 */
	public StereoMolecule getOneConformer(StereoMolecule mol) {
try {	// TODO remove try catch
		if (!initialize(mol))
			return null;

		if (mRotatableBond != null) {
			mTorsionSetStrategy = new TorsionSetStrategyAdaptiveRandom(mRotatableBond, mRigidFragment, true, true, mRandomSeed);
			mBaseConformerMap = new TreeMap<int[],Conformer>(new IntArrayComparator());
			return getNextConformer(mol);
			}
		else {
			ConformationSelfOrganizer sampler = new ConformationSelfOrganizer(mol, true);
			Conformer conformer = sampler.generateOneConformer(mRandomSeed);
			separateDisconnectedFragments(conformer);
			conformer.copyTo(mol);
			return mol;
			}
} catch (Exception e) { e.printStackTrace(); return null; }
		}

	/**
	 * Adds implicit hydrogens to the molecule and determines all rotatable bonds,
	 * which are not part of a ring. Generates rigid fragments between rotatable bonds.
	 * The base conformer is constructed by connecting all rigid fragments using the
	 * most frequent torsions. Atoms of different fragments in the base conformer may
	 * collide. In order to obtain collision free conformers choose a TorsionStrategy
	 * and call getNextConformer() at least once.
	 * @param mol
	 * @param multipleFragmentConformations true to consider multiple conformers of each rigid fragment 
	 */
	private boolean initialize(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.getOccupiedValence(atom) > mol.getMaxValence(atom))
				return false;

		addHydrogenAtoms(mol);

		// we need to protect explicit hydrogens in the fragments that we create from mol
		mol.setHydrogenProtection(true);

		// we need symmetry ranks for detecting equivalent torsions
		mol.ensureHelperArrays(Molecule.cHelperSymmetrySimple);

		mMolecule = mol;
		mConformerCount = 0;

		// check, whether we have disconnected fragments
		mDisconnectedFragmentNo = new int[mol.getAllAtoms()];
		mDisconnectedFragmentCount = mol.getFragmentNumbers(mDisconnectedFragmentNo, false);
		mDisconnectedFragmentSize = new int[mDisconnectedFragmentCount];
		for (int atom=0; atom<mol.getAllAtoms(); atom++)
			mDisconnectedFragmentSize[mDisconnectedFragmentNo[atom]]++;

		boolean[] isRotatableBond = new boolean[mol.getAllBonds()];
		int count = TorsionDB.findRotatableBonds(mol, true, isRotatableBond);
		if (count == 0)
			return true;

		locateInitialFragments(isRotatableBond);

if (mWriteDWFragmentsFile) {
 try {
  BufferedWriter writer = new BufferedWriter(new FileWriter("../../../data/ccdc/ConformationGeneratorFragmentsDebug.dwar"));
  writer.write("<column properties>");
  writer.newLine();
  writer.write("<columnName=\"Structure\">");
  writer.newLine();
  writer.write("<columnProperty=\"specialType\tidcode\">");
  writer.newLine();
  writer.write("<columnName=\"coords\">");
  writer.newLine();
  writer.write("<columnProperty=\"specialType\tidcoordinates3D\">");
  writer.newLine();
  writer.write("<columnProperty=\"parent\tStructure\">");
  writer.newLine();
  writer.write("</column properties>");
  writer.newLine();
  writer.write("Structure\tcoords");
  writer.newLine();
  for (Rigid3DFragment f:mRigidFragment) {
   Canonizer canonizer = new Canonizer(f.getFragment());
   String idcode = canonizer.getIDCode();
   String coords = canonizer.getEncodedCoordinates();
   writer.write(idcode+"\t"+coords+"\t");
   writer.newLine();
   }
  writer.close();
  }
 catch (IOException ioe) {}
 }

		mRotatableBond = new RotatableBond[count];
		int rotatableBond = 0;
		for (int bond=0; bond<mol.getBonds(); bond++)
			if (isRotatableBond[bond])
				mRotatableBond[rotatableBond++] = new RotatableBond(mol, bond, mFragmentNo,
						mDisconnectedFragmentNo, mDisconnectedFragmentSize[mDisconnectedFragmentNo[mol.getBondAtom(0, bond)]], mRigidFragment);

		// sort by descending atom count of smaller side, i.e. we want those bond dividing into equal parts first!
		Arrays.sort(mRotatableBond, new Comparator<RotatableBond>() {
			@Override
			public int compare(RotatableBond b1, RotatableBond b2) {
				int c1 = b1.getSmallerSideAtomCount();
				int c2 = b2.getSmallerSideAtomCount();
				return (c1 == c2) ? 0 : (c1 < c2) ? 1 : -1;
				}
			});

		// TODO this is actually only used with random conformers. Using a conformer mode
		// may save some time, if systematic conformers are created.
		initializeCollisionCheck();

		return true;
		}

	private Conformer getBaseConformer(int[] fragmentPermutation) {
		Conformer baseConformer = mBaseConformerMap.get(fragmentPermutation);
		if (baseConformer != null)
			return baseConformer;

		baseConformer = new Conformer(mMolecule);
		boolean[] isAttached = new boolean[mRigidFragment.length];
		for (RotatableBond rb:mRotatableBond)
			rb.connectFragments(baseConformer, isAttached, fragmentPermutation);

		// for separated fragments without connection points we need to get coordinates
		for (int i=0; i<mRigidFragment.length; i++) {
			if (!isAttached[i]) {
				for (int j=0; j<mRigidFragment[i].getCoreSize(); j++) {
					baseConformer.x[mRigidFragment[i].coreToOriginalAtom(j)] = mRigidFragment[i].getCoreAtomX(fragmentPermutation[i], j);
					baseConformer.y[mRigidFragment[i].coreToOriginalAtom(j)] = mRigidFragment[i].getCoreAtomY(fragmentPermutation[i], j);
					baseConformer.z[mRigidFragment[i].coreToOriginalAtom(j)] = mRigidFragment[i].getCoreAtomZ(fragmentPermutation[i], j);
					}
				}
			}

		mBaseConformerMap.put(fragmentPermutation, baseConformer);

		return baseConformer;
		}

	/**
	 * Creates the next random, likely or systematic new(!) conformer of the molecule
	 * that was passed when calling initializeConformers(). A new conformer is one,
	 * whose combination of torsion angles was not used in a previous conformer
	 * created by this function since the last call of initializeConformers().
	 * Parameter mol may be null or recycle the original molecule to receive new 3D coordinates.
	 * If it is null, then a fresh copy of the original molecule with new atom coordinates is returned.
	 * Every call of this method creates a new collision-free conformer until the employed torsion set
	 * strategy decides that it cannot generate any more suitable torsion set.
	 * do not succeed anymore in creating a new and collision-free conformer and null is returned.
	 * Typically one stops calling getRandomConformer() after receiving a null response.
	 * @param mol null or molecule used during initialization or a copy of it
	 * @return conformer or null, if all/maximum torsion permutations have been tried
	 */
	public StereoMolecule getNextConformer(StereoMolecule mol) {
		if (mRotatableBond == null) {
			if (mNoRotatableBondSampler == null)
				return null;

			SelfOrganizedConformer conformer = mNoRotatableBondSampler.getNextConformer();
			if (conformer != null) {
				separateDisconnectedFragments(conformer);

				if (mol == null)
					mol = conformer.toMolecule();
				else
					conformer.copyTo(mol);

				return mol;
				}

			mNoRotatableBondSampler = null;
			return null;	// no rotatable bonds: return the one and only rigid fragment
			}

		if (mBaseConformerMap == null)
			return null;

		TorsionSet torsionSet = mTorsionSetStrategy.getNextTorsionSet(null, 0f);

		while (torsionSet != null) {
/*
System.out.println("---- new torsion and conformer index set: -----");
for (int i=0; i<mRotatableBond.length; i++) System.out.println("rb:"+i+" index:"+torsionSet.getTorsionIndexes()[i]
+" torsion:"+mRotatableBond[i].getTorsion(torsionSet.getTorsionIndexes()[i])
+" likelyhood:"+mRotatableBond[i].getTorsionLikelyhood(torsionSet.getTorsionIndexes()[i]));
for (int i=0; i<mRigidFragment.length; i++) System.out.println("rf:"+i+" index:"+torsionSet.getConformerIndexes()[i]
+" likelyhood:"+mRigidFragment[i].getConformerLikelyhood(torsionSet.getConformerIndexes()[i]));
*/
			Conformer conformer = new Conformer(getBaseConformer(torsionSet.getConformerIndexes()));

			for (int j=mRotatableBond.length-1; j>=0; j--)
				mRotatableBond[j].rotateToIndex(conformer, torsionSet.getTorsionIndexes()[j]);

if (mWriteDWFragmentsFile) {
 mDiagnosticTorsionString = ""+torsionSet.getTorsionIndexes()[0];
 for (int i=1; i<torsionSet.getTorsionIndexes().length; i++)
  mDiagnosticTorsionString = mDiagnosticTorsionString + ":" + torsionSet.getTorsionIndexes()[i];
 mDiagnosticTorsionString = mDiagnosticTorsionString + "<->" + torsionSet.getConformerIndexes()[0];
 for (int i=1; i<torsionSet.getConformerIndexes().length; i++)
  mDiagnosticTorsionString = mDiagnosticTorsionString + ":" + torsionSet.getConformerIndexes()[i];
 }

			float[][] collisionIntensityMatrix = checkCollision(conformer);
			if (collisionIntensityMatrix != null) {
//System.out.println("COLLIDES!");
				torsionSet = mTorsionSetStrategy.getNextTorsionSet(collisionIntensityMatrix, mCollisionIntensitySum);
				if (torsionSet != null || mConformerCount != 0)
					continue;

				// we didn't get any torsion set that didn't collide; take the best we had
				torsionSet = mTorsionSetStrategy.getBestCollidingTorsionIndexes();
				conformer = new Conformer(getBaseConformer(torsionSet.getConformerIndexes()));

				for (int j=mRotatableBond.length-1; j>=0; j--)
					mRotatableBond[j].rotateToIndex(conformer, torsionSet.getTorsionIndexes()[j]);
				}

//System.out.println("passed collision check!");
			separateDisconnectedFragments(conformer);
			if (mol == null)
				mol = conformer.toMolecule();
			else
				conformer.copyTo(mol);
			mContribution = mTorsionSetStrategy.getContribution(torsionSet);

			mConformerCount++;
			return mol;
			}

		return null;
		}

	/**
	 * With best current knowledge about colliding torsion combinations
	 * and based on the individual frequencies of currently active torsions
	 * this method returns the conformers's overall contribution to the
	 * total set of non colliding conformers.
	 * @return this conformer's contribution to all conformers
	 */
	public float getPreviousConformerContribution() {
		return mRotatableBond == null ? 1f : mContribution;
		}

	/**
	 * Needs to be called, before getting individual conformers of the same molecule by
	 * getNextConformer(). Open valences of the passed molecule are filled with hydrogen atoms.
	 * The passed molecule may repeatedly be used as container for a new conformer's atom
	 * coordinates, if it is passed as parameter to getNextConformer().
	 * @param mol will be saturated with hydrogen atoms
	 * @return false if there is a structure problem
	 */
	public boolean initializeConformers(StereoMolecule mol, int strategy) {
		if (!initialize(mol))
			return false;

		if (mRotatableBond == null) {
			mNoRotatableBondSampler = new ConformationSelfOrganizer(mol, true);
			mNoRotatableBondSampler.initializeConformers(mRandomSeed, -1);
			}
		else {
			switch(strategy) {
			case STRATEGY_PURE_RANDOM:
				mTorsionSetStrategy = new TorsionSetStrategyRandom(mRotatableBond, mRigidFragment, false, mRandomSeed);
				break;
			case STRATEGY_LIKELY_RANDOM:
				mTorsionSetStrategy = new TorsionSetStrategyRandom(mRotatableBond, mRigidFragment, true, mRandomSeed);
				break;
			case STRATEGY_ADAPTIVE_RANDOM:
				mTorsionSetStrategy = new TorsionSetStrategyAdaptiveRandom(mRotatableBond, mRigidFragment, true, true, mRandomSeed);
				break;
			case STRATEGY_LIKELY_SYSTEMATIC:
				mTorsionSetStrategy = new TorsionSetStrategyLikelySystematic(mRotatableBond, mRigidFragment);
				break;
				}
			mBaseConformerMap = new TreeMap<int[],Conformer>(new IntArrayComparator());
			}

		return true;
		}

	/**
	 * Moves disconnected fragments along Z-axis such that there is an
	 * empty z-space SEPARATION_DISTANCE thick between the fragments.
	 * @param mol
	 */
	private void separateDisconnectedFragments(Conformer conformer) {
		final float SEPARATION_DISTANCE = 3f;
		if (mDisconnectedFragmentCount > 1) {
			float[] meanX = new float[mDisconnectedFragmentCount];
			float[] meanY = new float[mDisconnectedFragmentCount];
			float[] minZ = new float[mDisconnectedFragmentCount];
			float[] maxZ = new float[mDisconnectedFragmentCount];
			for (int i=0; i<mDisconnectedFragmentCount; i++) {
				minZ[i] =  1000000000f;
				maxZ[i] = -1000000000f;
				}
			for (int atom=0; atom<conformer.z.length; atom++) {
				meanX[mDisconnectedFragmentNo[atom]] += conformer.x[atom];
				meanY[mDisconnectedFragmentNo[atom]] += conformer.y[atom];
				if (minZ[mDisconnectedFragmentNo[atom]] > conformer.z[atom])
					minZ[mDisconnectedFragmentNo[atom]] = conformer.z[atom];
				if (maxZ[mDisconnectedFragmentNo[atom]] < conformer.z[atom])
					maxZ[mDisconnectedFragmentNo[atom]] = conformer.z[atom];
				}
			for (int i=0; i<mDisconnectedFragmentCount; i++) {
				meanX[i] /= mDisconnectedFragmentSize[i];
				meanY[i] /= mDisconnectedFragmentSize[i];
				}
			float[] shiftX = new float[mDisconnectedFragmentCount];
			float[] shiftY = new float[mDisconnectedFragmentCount];
			float[] shiftZ = new float[mDisconnectedFragmentCount];
			for (int i=1; i<mDisconnectedFragmentCount; i++) {
				shiftX[i] = meanX[0] - meanX[i];
				shiftY[i] = meanY[0] - meanY[i];
				shiftZ[i] = shiftZ[i-1] + maxZ[i-1] - minZ[i] + SEPARATION_DISTANCE;
				}
			for (int atom=0; atom<conformer.z.length; atom++) {
				if (mDisconnectedFragmentNo[atom] != 0) {
					conformer.x[atom] += shiftX[mDisconnectedFragmentNo[atom]];
					conformer.y[atom] += shiftY[mDisconnectedFragmentNo[atom]];
					conformer.z[atom] += shiftZ[mDisconnectedFragmentNo[atom]];
					}
				}
			}
		}

	private void locateInitialFragments(boolean[] isRotatableBond) {
		mFragmentNo = new int[mMolecule.getAllAtoms()];
		int fragmentCount = mMolecule.getFragmentNumbers(mFragmentNo, isRotatableBond);
		mRigidFragment = new Rigid3DFragment[fragmentCount];
		for (int i=0; i<fragmentCount; i++)
			mRigidFragment[i] = new Rigid3DFragment(mMolecule, mFragmentNo, i);
		}

	/**
	 * Creates a atom matrix flagging all atom pairs, which should be skipped
	 * during collision check, because they are members of the same fragment,
	 * they are members of two adjacent fragments or because the number of
	 * bonds between them is smaller than 3.
	 * @param mol
	 * @param isRotatableBond
	 * @param fragmentCount
	 */
	private void initializeCollisionCheck() {
		mSkipCollisionCheck = new boolean[mMolecule.getAllAtoms()][];
		for (int atom=1; atom<mMolecule.getAllAtoms(); atom++)
			mSkipCollisionCheck[atom] = new boolean[atom];

		// skip collision check for two atoms in adjacent fragments
		for (RotatableBond rb:mRotatableBond) {
			Rigid3DFragment f1 = rb.getFragment(0);
			Rigid3DFragment f2 = rb.getFragment(1);
			for (int i=0; i<f1.getCoreSize(); i++) {
				int atom1 = f1.coreToOriginalAtom(i);
				for (int j=0; j<f2.getCoreSize(); j++)
					skipCollisionCheck(atom1, f2.coreToOriginalAtom(j));
				}
			}

		// skip collision check for two atoms of the same fragment
		for (Rigid3DFragment rf:mRigidFragment)
			for (int i=1; i<rf.getExtendedSize(); i++)
				for (int j=0; j<i; j++)
					skipCollisionCheck(rf.extendedToOriginalAtom(i), rf.extendedToOriginalAtom(j));

		// skip collision check for atom pairs with 2 bonds in between
		for (int atom=0; atom<mMolecule.getAtoms(); atom++)
			for (int i=1; i<mMolecule.getAllConnAtoms(atom); i++)
				for (int j=0; j<i; j++)
					skipCollisionCheck(mMolecule.getConnAtom(atom, i), mMolecule.getConnAtom(atom, j));

		// skip collision check for any two atoms that belong to different disconnected fragments
		if (mDisconnectedFragmentNo != null)
			for (int atom1=1; atom1<mMolecule.getAllAtoms(); atom1++)
				for (int atom2=0; atom2<atom1; atom2++)
					if (mDisconnectedFragmentNo[atom1] != mDisconnectedFragmentNo[atom2])
						mSkipCollisionCheck[atom1][atom2] = true;
		}

	private void skipCollisionCheck(int atom1, int atom2) {
		if (atom1 < atom2)
			mSkipCollisionCheck[atom2][atom1] = true;
		else
			mSkipCollisionCheck[atom1][atom2] = true;
		}

	private float[][] checkCollision(Conformer conformer) {
mDiagnosticCollisionString = "";
mDiagnosticCollisionAtoms = null;
		mCollisionIntensitySum = 0;
		float[][] collisionIntensityMatrix = null;
		StereoMolecule mol = conformer.getMolecule();
		for (int atom1=1; atom1<mol.getAllAtoms(); atom1++) {
			float vdwr1 = getToleratedVDWRadius(mol.getAtomicNo(atom1));
			for (int atom2=0; atom2<atom1; atom2++) {
				if (!mSkipCollisionCheck[atom1][atom2]) {
					float minDistance = vdwr1+getToleratedVDWRadius(mol.getAtomicNo(atom2));
					float dx = Math.abs(conformer.x[atom1] - conformer.x[atom2]);
					if (dx < minDistance) {
						float dy = Math.abs(conformer.y[atom1] - conformer.y[atom2]);
						if (dy < minDistance) {
							float dz = Math.abs(conformer.z[atom1] - conformer.z[atom2]);
							if (dz < minDistance) {
								float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
								if (distance < minDistance) {
									float collisionIntensity = (minDistance - distance) * (minDistance - distance);
									mCollisionIntensitySum += collisionIntensity;
//System.out.println("a1:"+atom1+" f1:"+mFragmentNo[atom1]+" a2:"+atom2+" f2:"+mFragmentNo[atom2]+" distance:"+distance+" min:"+minDistance);
if (mWriteDWFragmentsFile) {
 if (mDiagnosticCollisionString.length() != 0) mDiagnosticCollisionString = mDiagnosticCollisionString + "<NL>";
  mDiagnosticCollisionString = mDiagnosticCollisionString+"a1:"+atom1+" f1:"+mFragmentNo[atom1]+" a2:"+atom2+" f2:"+mFragmentNo[atom2]+" distance:"+distance+" min:"+minDistance;
 if (mDiagnosticCollisionAtoms == null) {
  mDiagnosticCollisionAtoms = new int[2];
  mDiagnosticCollisionAtoms[0] = atom1;
  mDiagnosticCollisionAtoms[1] = atom2;
 }
} else {
									if (collisionIntensityMatrix == null)
										collisionIntensityMatrix = new float[mRigidFragment.length][];
									int f1 = mFragmentNo[atom1];
									int f2 = mFragmentNo[atom2];
									if (f1 < f2) {
										if (collisionIntensityMatrix[f2] == null)
											collisionIntensityMatrix[f2] = new float[f2];
										collisionIntensityMatrix[f2][f1] += collisionIntensity;
										}
									else {
										if (collisionIntensityMatrix[f1] == null)
											collisionIntensityMatrix[f1] = new float[f1];
										collisionIntensityMatrix[f1][f2] += collisionIntensity;
										}
									continue;
}
									}
								}
							}
						}
					}
				}
			}
		return collisionIntensityMatrix;
		}
	}
