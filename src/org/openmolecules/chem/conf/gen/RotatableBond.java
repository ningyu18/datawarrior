/*
 * @(#)RotatableBond.java
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

package org.openmolecules.chem.conf.gen;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.chem.conf.TorsionDetail;
import com.actelion.research.chem.conf.TorsionPrediction;

/**
 * A RotatableBond knows the two rigid fragments within a molecule
 * that are connected by this bond. It also knows about possible torsion
 * states with associated likelyhoods, which are taken from COD statistics
 * and modified to account for collisions due to bulky groups in the molecule.
 * It knows the smaller half of the molecule and rotate the smaller half to
 * a given torsion angle.
 */
public class RotatableBond {
	private static final float ANGLE_TOLERANCE = 0.001f;	// limit for considering bonds as parallel

	private static final float ACCEPTABLE_CENTER_STRAIN = 0.05f;	// if we have less strain than this we don't check whether range edges improve strain
	private static final float NECESSARY_EDGE_STRAIN_IMPROVEMENT = 0.02f;	// necessary strain improvement to use edge torsion rather than center torsion
	private static final float MAXIMUM_CENTER_STRAIN = 0.2f;		// if center torsion is above this limit we refuse that torsion

	private Rigid3DFragment mFragment1,mFragment2;
	private int mRotationCenter,mBond,mFragmentNo1,mFragmentNo2;
	private boolean mBondAtomsInFragmentOrder;
	private short[] mTorsion;
	private short[] mFrequency;
	private short[][] mTorsionRange;
	private float[] mLikelyhood; // considering directly connected rigid fragments (frequency and collision strain)
	private int[] mTorsionAtom,mRearAtom,mSmallerSideAtomList;

	public RotatableBond(StereoMolecule mol, int bond, int[] fragmentNo, int[] disconnectedFragmentNo,
						 int disconnectedFragmentSize, Rigid3DFragment[] fragment) {
		mBond = bond;
		mTorsionAtom = new int[4];
		mRearAtom = new int[2];
		TorsionDetail detail = new TorsionDetail();
		if (TorsionDB.getTorsionID(mol, bond, mTorsionAtom, detail) != null) {
			mRearAtom[0] = detail.getRearAtom(0);
			mRearAtom[1] = detail.getRearAtom(1);
			}
		else {
			predictAtomSequence(mol);
			}

		mFragmentNo1 = fragmentNo[mTorsionAtom[1]];
		mFragmentNo2 = fragmentNo[mTorsionAtom[2]];
		mFragment1 = fragment[mFragmentNo1];
		mFragment2 = fragment[mFragmentNo2];

		mBondAtomsInFragmentOrder = (fragmentNo[mol.getBondAtom(0, bond)] == mFragmentNo1);

		mTorsion = TorsionDB.getTorsions(detail.getID());
		if (mTorsion == null) {
			TorsionPrediction prediction  = new TorsionPrediction(mol, mTorsionAtom);
			mTorsion = prediction.getTorsions();
			mFrequency = prediction.getTorsionFrequencies();
			mTorsionRange = prediction.getTorsionRanges();
			}
		else {
			mFrequency = TorsionDB.getTorsionFrequencies(detail.getID());
			mTorsionRange = TorsionDB.getTorsionRanges(detail.getID());
			}
		removeEquivalentTorsions(mol);
		mLikelyhood = new float[mTorsion.length];

		findSmallerSideAtomList(mol, disconnectedFragmentNo, disconnectedFragmentSize);
		}

	public Rigid3DFragment getFragment(int i) {
		return (i == 0) ? mFragment1 : mFragment2;
		}

	public int getFragmentNo(int i) {
		return (i == 0) ? mFragmentNo1 : mFragmentNo2;
		}

	private void predictAtomSequence(StereoMolecule mol) {
        for (int i=0; i<2; i++) {
    		int centralAtom = mol.getBondAtom(i, mBond);
        	int rearAtom = mol.getBondAtom(1-i, mBond);

        	// walk along sp-chains to first sp2 or sp3 atom
        	while (mol.getAtomPi(centralAtom) == 2
        		&& mol.getConnAtoms(centralAtom) == 2
        		&& mol.getAtomicNo(centralAtom) < 10) {
        		for (int j=0; j<2; j++) {
        			int connAtom = mol.getConnAtom(centralAtom, j);
        			if (connAtom != rearAtom) {
        				rearAtom = centralAtom;
        				centralAtom = connAtom;
        				break;
        				}
        			}
        		}

        	mTorsionAtom[i+1] = centralAtom;
           	mRearAtom[i] = rearAtom;
        	}

    	// A TorsionPrediction does not distinguish hetero atoms from carbons a positions 0 and 3.
        // Therefore we can treat two sp2 neighbors as equivalent when predicting torsions.
        if (mol.getAtomPi(mTorsionAtom[1]) == 0 && mol.getConnAtoms(mTorsionAtom[1]) == 3) {
			mTorsionAtom[0] = -1;
        	}
        else {
			for (int i=0; i<mol.getConnAtoms(mTorsionAtom[1]); i++) {
				int connAtom = mol.getConnAtom(mTorsionAtom[1], i);
				if (connAtom != mTorsionAtom[2]) {
					mTorsionAtom[0] = connAtom;
					break;
					}
				}
        	}
        if (mol.getAtomPi(mTorsionAtom[2]) == 0 && mol.getConnAtoms(mTorsionAtom[2]) == 3) {
			mTorsionAtom[3] = -1;
        	}
        else {
			for (int i=0; i<mol.getConnAtoms(mTorsionAtom[2]); i++) {
				int connAtom = mol.getConnAtom(mTorsionAtom[2], i);
				if (connAtom != mTorsionAtom[1]) {
					mTorsionAtom[3] = connAtom;
					break;
					}
				}
        	}
		}

	private void findSmallerSideAtomList(StereoMolecule mol, int[] disconnectedFragmentNo, int disconnectedFragmentSize) {
		boolean[] isMember = new boolean[mol.getAllAtoms()];
		int memberCount = mol.getSubstituent(mRearAtom[0], mTorsionAtom[1], isMember, null, null);

		int alkyneAtoms = 0;	// if we have an extended linear sp-atom strain
		if (mRearAtom[0] != mTorsionAtom[2])
			alkyneAtoms = mol.getPathLength(mRearAtom[0], mTorsionAtom[2]);

		boolean invert = false;
		if (memberCount > disconnectedFragmentSize-alkyneAtoms-memberCount) {
			memberCount = disconnectedFragmentSize-alkyneAtoms-memberCount;
			invert = true;
			}

		// if invert, then flag all linear alkyne atoms to be avoided
		if (invert && alkyneAtoms != 0) {
			int spAtom = mRearAtom[0];
			int backAtom = mTorsionAtom[1];
        	while (mol.getAtomPi(spAtom) == 2
           		&& mol.getConnAtoms(spAtom) == 2
           		&& mol.getAtomicNo(spAtom) < 10) {
        		isMember[spAtom] = true;
           		for (int j=0; j<2; j++) {
           			int connAtom = mol.getConnAtom(spAtom, j);
           			if (connAtom != backAtom) {
           				backAtom = spAtom;
           				spAtom = connAtom;
           				break;
           				}
           			}
           		}
			}

		int memberNo = 0;
		int fragmentNo = disconnectedFragmentNo[mTorsionAtom[1]];
		mSmallerSideAtomList = new int[memberCount];
		for (int atom=0; atom<mol.getAllAtoms(); atom++)
			if (disconnectedFragmentNo[atom] == fragmentNo && (isMember[atom] ^ invert))
				mSmallerSideAtomList[memberNo++] = atom;

		mRotationCenter = mTorsionAtom[invert ? 2 : 1];
		}

	public int getTorsionCount() {
		return mTorsion.length;
		}

	/**
	 * Calculates a random torsion index giving torsions with higher likelyhoods
	 * (i.e. frequencies and collision strains) a higher chance to be selected.
	 * With a progress value of 0.0 selection likelyhoods are proportional to
	 * the torsion frequencies. With increasing progress value the higher frequent
	 * torsions are less and less preferred until 1.0 without any preference.
	 * @param random
	 * @param progress 0...1 
	 */
	public int getLikelyRandomTorsionIndex(float random, float progress) {
		float sum = 0;
		for (int t=0; t<mTorsion.length; t++) {
			float contribution = (1f-progress)*mLikelyhood[t] + progress/mTorsion.length;
			sum += contribution;
			if (random <= sum)
				return t;
			}
		return mTorsion.length-1;  // should never reach this
		}

	/**
	 * @return the i'th torsion angle in degrees
	 */
	public float getTorsion(int t) {
		return mTorsion[t];
		}

	/**
	 * @return the likelyhood of torsion i among all torsions of this bond
	 */
	public float getTorsionLikelyhood(int t) {
		return mLikelyhood[t];
		}

	/**
	 * @return count of atoms of the smaller half of the molecule excluding anchor atom
	 */
	public int getSmallerSideAtomCount() {
		return mSmallerSideAtomList.length;
		}

	/**
	 * Checks both rigid fragments that are connected by this bond, whether they have
	 * been attached to the conformer yet, i.e. whether their local coordinates have been
	 * copied to conformer and transformed according to the connecting torsion.
	 * If one was already attached, then the other's coordinates are transformed according
	 * to the torsion and copied to the conformer. A likelyhood is calculated for every torsion
	 * value based on its frequency and the collision strain of the two fragments' atoms.
	 * If both fragments were not attached yet, then the larger one's coordinates are
	 * copied and the smaller one's coordinates are translated and then copied.
	 * Unlikely torsions, i.e. where collisions strain outweighs frequency, are removed from torsion list.
	 */
	public void connectFragments(Conformer conformer, boolean[] isAttached, int[] fragmentPermutation) {
		if (!isAttached[mFragmentNo1] && !isAttached[mFragmentNo2]) {
			Rigid3DFragment largerFragment = (mFragment1.getCoreSize() > mFragment2.getCoreSize()) ? mFragment1 : mFragment2;
            int largerFragmentNo = (mFragment1.getCoreSize() > mFragment2.getCoreSize()) ? mFragmentNo1 : mFragmentNo2;
			isAttached[largerFragmentNo] = true;
			int fragmentConformer = (fragmentPermutation == null) ? 0 : fragmentPermutation[largerFragmentNo];
			for (int i=0; i<largerFragment.getExtendedSize(); i++) {
				int atom = largerFragment.extendedToOriginalAtom(i);
				conformer.x[atom] = largerFragment.getExtendedAtomX(fragmentConformer, i);
				conformer.y[atom] = largerFragment.getExtendedAtomY(fragmentConformer, i);
				conformer.z[atom] = largerFragment.getExtendedAtomZ(fragmentConformer, i);
				}
			}

		assert(isAttached[mFragmentNo1] ^ isAttached[mFragmentNo2]);

		int rootAtom,rearAtom,fragmentNo,bondAtomIndex;
		Rigid3DFragment fragment = null;
		if (isAttached[mFragmentNo1]) {
            fragmentNo = mFragmentNo2;
			fragment = mFragment2;
			bondAtomIndex = mBondAtomsInFragmentOrder ? 1 : 0;
			}
		else {
            fragmentNo = mFragmentNo1;
			fragment = mFragment1;
			bondAtomIndex = mBondAtomsInFragmentOrder ? 0 : 1;
			}

		rootAtom = conformer.getMolecule().getBondAtom(bondAtomIndex, mBond);
		rearAtom = conformer.getMolecule().getBondAtom(1-bondAtomIndex, mBond);

		int fragmentConformer = (fragmentPermutation == null) ? 0 : fragmentPermutation[fragmentNo];

		int fRootAtom = fragment.originalToExtendedAtom(rootAtom);
		int fRearAtom = fragment.originalToExtendedAtom(rearAtom);

		float frootx = fragment.getExtendedAtomX(fragmentConformer, fRootAtom);
		float frooty = fragment.getExtendedAtomY(fragmentConformer, fRootAtom);
		float frootz = fragment.getExtendedAtomZ(fragmentConformer, fRootAtom);
		float rootx = conformer.x[rootAtom];
		float rooty = conformer.y[rootAtom];
		float rootz = conformer.z[rootAtom];

		float[] fuv = getUnitVector(frootx-fragment.getExtendedAtomX(fragmentConformer, fRearAtom),
									frooty-fragment.getExtendedAtomY(fragmentConformer, fRearAtom),
									frootz-fragment.getExtendedAtomZ(fragmentConformer, fRearAtom));
		float[] uv  = getUnitVector(rootx-conformer.x[rearAtom],
									rooty-conformer.y[rearAtom],
									rootz-conformer.z[rearAtom]);
		double alpha = getAngle(uv, fuv);

		float fx[] = new float[fragment.getExtendedSize()];
		float fy[] = new float[fragment.getExtendedSize()];
		float fz[] = new float[fragment.getExtendedSize()];

		if (alpha < ANGLE_TOLERANCE || alpha > Math.PI - ANGLE_TOLERANCE) {   // special cases
			for (int i=0; i<fragment.getExtendedSize(); i++) {
				int atom = fragment.extendedToOriginalAtom(i);
				if (atom != rootAtom && atom != rearAtom) {
					fx[i] = fragment.getExtendedAtomX(fragmentConformer, i) - frootx;
					fy[i] = fragment.getExtendedAtomY(fragmentConformer, i) - frooty;
					fz[i] = fragment.getExtendedAtomZ(fragmentConformer, i) - frootz;
	
					if (alpha > Math.PI / 2) {
						fx[i] = -fx[i];
						fy[i] = -fy[i];
						fz[i] = -fz[i];
						}
					}
				}
			}
		else {
			float[] cp  = getCrossProduct(uv, fuv);
			float[][] m = getRotationMatrix(getUnitVector(cp[0], cp[1], cp[2]), alpha);
	
			for (int i=0; i<fragment.getExtendedSize(); i++) {
				int atom = fragment.extendedToOriginalAtom(i);
				if (atom != rootAtom && atom != rearAtom) {
					float x = fragment.getExtendedAtomX(fragmentConformer, i) - frootx;
					float y = fragment.getExtendedAtomY(fragmentConformer, i) - frooty;
					float z = fragment.getExtendedAtomZ(fragmentConformer, i) - frootz;
	
					fx[i] = x*m[0][0]+y*m[1][0]+z*m[2][0];
					fy[i] = x*m[0][1]+y*m[1][1]+z*m[2][1];
					fz[i] = x*m[0][2]+y*m[1][2]+z*m[2][2];
					}
				}
			}
		isAttached[fragmentNo] = true;

		// we need to restore valid coordinates for rootAtom and neighbors
		// to correctly calculate torsion from atom sequence
		for (int i=0; i<conformer.getMolecule().getConnAtoms(rootAtom); i++) {
			int connAtom = conformer.getMolecule().getConnAtom(rootAtom, i);
			if (connAtom != rearAtom) {
				int fAtom = fragment.originalToExtendedAtom(connAtom);
				conformer.x[connAtom] = fx[fAtom] + rootx;
				conformer.y[connAtom] = fy[fAtom] + rooty;
				conformer.z[connAtom] = fz[fAtom] + rootz;
				}
			}
		double startTorsion = TorsionDB.calculateTorsionExtended(conformer, mTorsionAtom);

		short currentTorsion = -1;

//System.out.print("connectFragments() original torsions:"); for (int t=0; t<mTorsion.length; t++) System.out.print(mTorsion[t]+" "); System.out.println();
		for (int t=0; t<mTorsion.length; t++) {
			currentTorsion = mTorsion[t];
			float[][] m = getRotationMatrix(uv, Math.PI * mTorsion[t] / 180 - startTorsion);
			for (int i=0; i<fragment.getExtendedSize(); i++) {
				int atom = fragment.extendedToOriginalAtom(i);
				if (atom != rootAtom && atom != rearAtom) {
					conformer.x[atom] = fx[i]*m[0][0]+fy[i]*m[1][0]+fz[i]*m[2][0] + rootx;
					conformer.y[atom] = fx[i]*m[0][1]+fy[i]*m[1][1]+fz[i]*m[2][1] + rooty;
					conformer.z[atom] = fx[i]*m[0][2]+fy[i]*m[1][2]+fz[i]*m[2][2] + rootz;
					}
				}

			float strain = calculateCollisionStrain(conformer);
			// if the strain is above a certain limit, we investigate whether we should use the
			// limits of the torsion range rather than the central torsion value, which has the
			// highest frequency.
			// If we use torsion range limits, then we need to consider half of the frequency
			// for each of the lower and higher limits.
			if (strain < ACCEPTABLE_CENTER_STRAIN) {
				float relativeStrain = strain / MAXIMUM_CENTER_STRAIN;
				mLikelyhood[t] = mFrequency[t] * (1f - relativeStrain * relativeStrain);
				}
			else {
				boolean foundAlternative = false;
				boolean isFirstAlternative = true;
				for (int r=0; r<2; r++) {
					currentTorsion = mTorsionRange[t][r];
					m = getRotationMatrix(uv, Math.PI * mTorsionRange[t][r] / 180 - startTorsion);
					for (int i=0; i<fragment.getExtendedSize(); i++) {
						int atom = fragment.extendedToOriginalAtom(i);
						if (atom != rootAtom && atom != rearAtom) {
							conformer.x[atom] = fx[i]*m[0][0]+fy[i]*m[1][0]+fz[i]*m[2][0] + rootx;
							conformer.y[atom] = fx[i]*m[0][1]+fy[i]*m[1][1]+fz[i]*m[2][1] + rooty;
							conformer.z[atom] = fx[i]*m[0][2]+fy[i]*m[1][2]+fz[i]*m[2][2] + rootz;
							}
						}

					float rangeStrain = calculateCollisionStrain(conformer);
					if (strain - rangeStrain > NECESSARY_EDGE_STRAIN_IMPROVEMENT) {
						if (isFirstAlternative) {
							mTorsion[t] = mTorsionRange[t][r];
							mFrequency[t] = (short)((mFrequency[t]+1) / 2);
							float relativeStrain = rangeStrain / MAXIMUM_CENTER_STRAIN;
							mLikelyhood[t] = mFrequency[t] * (1f - relativeStrain * relativeStrain);
							isFirstAlternative = false;
							}
						else {
							float relativeStrain = rangeStrain / MAXIMUM_CENTER_STRAIN;
							insertTorsion(t+1, mTorsionRange[t][r], (short)((mFrequency[t]+1) / 2),
									mFrequency[t] * (1f - relativeStrain * relativeStrain));
							t++;
							}
						foundAlternative = true;
						}
					}
				if (!foundAlternative && strain < MAXIMUM_CENTER_STRAIN) {
					float relativeStrain = strain / MAXIMUM_CENTER_STRAIN;
					mLikelyhood[t] = mFrequency[t] * (1f - relativeStrain * relativeStrain);
					}
				}
			}

		float totalLikelyhood = 0f;
		float maxLikelyHood = -Float.MAX_VALUE;
		int maxTorsionIndex = -1;
		for (int t=0; t<mTorsion.length; t++) {
			if (maxLikelyHood < mLikelyhood[t]) {
				maxLikelyHood = mLikelyhood[t];
				maxTorsionIndex = t;
				}
			if (mLikelyhood[t] > 0f)
				totalLikelyhood += mLikelyhood[t];
			}

		// make sure, we have at least one torsion with positive likelyhood, because only those are considered later
		if (maxLikelyHood <= 0f)
			mLikelyhood[maxTorsionIndex] = 1.0f;
		else
			for (int t=0; t<mTorsion.length; t++)
				mLikelyhood[t] /= totalLikelyhood;

		maxTorsionIndex = removeUnlikelyTorsions(maxTorsionIndex);

//System.out.print("connectFragments() applied torsions:"); for (int t=0; t<mTorsion.length; t++) System.out.print(mTorsion[t]+" "); System.out.println();
//System.out.print("connectFragments() applied likelyhoods:"); for (int t=0; t<mTorsion.length; t++) System.out.print(mLikelyhood[t]+" "); System.out.println();
//System.out.println();

		if (currentTorsion != mTorsion[maxTorsionIndex]) {
			float[][] m = getRotationMatrix(uv, Math.PI * mTorsion[maxTorsionIndex] / 180 - startTorsion);
			for (int i=0; i<fragment.getExtendedSize(); i++) {
				int atom = fragment.extendedToOriginalAtom(i);
				if (atom != rootAtom && atom != rearAtom) {
					conformer.x[atom] = fx[i]*m[0][0]+fy[i]*m[1][0]+fz[i]*m[2][0] + rootx;
					conformer.y[atom] = fx[i]*m[0][1]+fy[i]*m[1][1]+fz[i]*m[2][1] + rooty;
					conformer.z[atom] = fx[i]*m[0][2]+fy[i]*m[1][2]+fz[i]*m[2][2] + rootz;
					}
				}
			}
		conformer.setBondTorsion(mBond, mTorsion[maxTorsionIndex]);
		}

	private float calculateCollisionStrain(Conformer conformer) {
		float panalty = 0f;
		int bondAtom1 = conformer.getMolecule().getBondAtom(0, mBond);
		int bondAtom2 = conformer.getMolecule().getBondAtom(1, mBond);
		for (int i=0; i<mFragment1.getCoreSize(); i++) {
			int atom1 = mFragment1.coreToOriginalAtom(i);
			if (atom1 != bondAtom1 && atom1 != bondAtom2) {
				float vdwr1 = ConformerGenerator.getToleratedVDWRadius(conformer.getMolecule().getAtomicNo(atom1));
				for (int j=0; j<mFragment2.getCoreSize(); j++) {
					int atom2 = mFragment2.coreToOriginalAtom(j);
					if (atom2 != bondAtom1 && atom2 != bondAtom2) {
						float minDistance = vdwr1 + ConformerGenerator.getToleratedVDWRadius(
								conformer.getMolecule().getAtomicNo(atom2));
						float dx = Math.abs(conformer.x[atom1] - conformer.x[atom2]);
						if (dx < minDistance) {
							float dy = Math.abs(conformer.y[atom1] - conformer.y[atom2]);
							if (dy < minDistance) {
								float dz = Math.abs(conformer.z[atom1] - conformer.z[atom2]);
								if (dz < minDistance) {
									float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
									if (distance < minDistance) {
										float p = (minDistance - distance) / minDistance;
										panalty += p*p;
										}
									}
								}
							}
						}
					}
				}
			}
		return panalty;
		}

	/**
	 * Removes all torsions with non-positive likelyhoods.
	 * Sorts torsions, frequencies and likelyhoods by descending likelyhoods.
	 * Translates originalTorsionIndex to new sorted and resized torsion values.
	 * @param adapted torsion index
	 */
	private int removeUnlikelyTorsions(int originalTorsionIndex) {
		int newTorsionIndex = -1;
		int count = 0;
		for (int t=0; t<mTorsion.length; t++)
			if (mLikelyhood[t] > 0f)
				count++;

		short[] newTorsion = new short[count];
		short[] newFrequency = new short[count];
		float[] newLikelyhood = new float[count];
		for (int i=0; i<count; i++) {
			float maxLikelyhood = 0f;
			int maxIndex = -1;
			for (int t=0; t<mTorsion.length; t++) {
				if (maxLikelyhood < mLikelyhood[t]) {
					maxLikelyhood = mLikelyhood[t];
					maxIndex = t;
					}
				}
			newTorsion[i] = mTorsion[maxIndex];
			newFrequency[i] = mFrequency[maxIndex];
			newLikelyhood[i] = mLikelyhood[maxIndex];

			if (maxIndex == originalTorsionIndex)
				newTorsionIndex = i;

			mLikelyhood[maxIndex] = 0f;
			}
		mTorsion = newTorsion;
		mFrequency = newFrequency;
		mLikelyhood = newLikelyhood;

		return newTorsionIndex;
		}

	private void insertTorsion(int index, short torsion, short frequency, float likelyhood) {
		short[] newTorsion = new short[mTorsion.length+1];
		short[][] newRange = new short[mTorsion.length+1][];
		short[] newFrequency = new short[mTorsion.length+1];
		float[] newLikelyhood = new float[mTorsion.length+1];
		int oldIndex = 0;
		for (int i=0; i<=mTorsion.length; i++) {
			if (i == index) {
				newTorsion[i] = torsion;
				newRange[i] = new short[2];
				newRange[i][0] = torsion;
				newRange[i][1] = torsion;
				newFrequency[i] = frequency;
				newLikelyhood[i] = likelyhood;
				}
			else {
				newTorsion[i] = mTorsion[oldIndex];
				newRange[i] = mTorsionRange[oldIndex];
				newFrequency[i] = mFrequency[oldIndex];
				newLikelyhood[i] = mLikelyhood[oldIndex];
				oldIndex++;
				}
			}

		mTorsion = newTorsion;
		mTorsionRange = newRange;
		mFrequency = newFrequency;
		mLikelyhood = newLikelyhood;
		}

	private static float[] getUnitVector(float x, float y, float z) {
		float l = (float)Math.sqrt(x*x+y*y+z*z);
		float[] uv = new float[3];
		uv[0] = x/l;
		uv[1] = y/l;
		uv[2] = z/l;
		return uv;
		}

	private static float[] getCrossProduct(float[] v1, float[] v2) {
		float[] cp = new float[3];
		cp[0] = v1[1]*v2[2] - v1[2]*v2[1]; 
		cp[1] = v1[2]*v2[0] - v1[0]*v2[2];
		cp[2] = v1[0]*v2[1] - v1[1]*v2[0];
		return cp;
		}

	private static double getAngle(float[] v1, float[] v2) {
		return Math.acos(v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2]);
		}

	private static float[][] getRotationMatrix(float[] uv, double alpha) {
		float sinAlpha = (float)Math.sin(alpha);
		float cosAlpha = (float)Math.cos(alpha);
		float invcosAlpha = 1f-cosAlpha;

		// rotation matrix is: m11 m12 m13
		//					 m21 m22 m23
		//					 m31 m32 m33
		float[][] m = new float[3][3];
		m[0][0] = uv[0]*uv[0]*invcosAlpha+cosAlpha;
		m[1][1] = uv[1]*uv[1]*invcosAlpha+cosAlpha;
		m[2][2] = uv[2]*uv[2]*invcosAlpha+cosAlpha;
		m[0][1] = uv[0]*uv[1]*invcosAlpha-uv[2]*sinAlpha;
		m[1][2] = uv[1]*uv[2]*invcosAlpha-uv[0]*sinAlpha;
		m[2][0] = uv[2]*uv[0]*invcosAlpha-uv[1]*sinAlpha;
		m[0][2] = uv[0]*uv[2]*invcosAlpha+uv[1]*sinAlpha;
		m[1][0] = uv[1]*uv[0]*invcosAlpha+uv[2]*sinAlpha;
		m[2][1] = uv[2]*uv[1]*invcosAlpha+uv[0]*sinAlpha;
		return m;
		}

	/**
	 * Rotate the smaller side of the molecule around this bond
	 * to reach the torsion angle defined by torsionIndex.
	 * @param conformer
	 * @param torsionIndex
	 */
	public void rotateToIndex(Conformer conformer, int torsionIndex) {
		rotateTo(conformer, mTorsion[torsionIndex]);
		}

	/**
	 * Rotate the smaller side of the molecule around this bond
	 * to reach the defined torsion angle.
	 * @param conformer
	 * @param torsion in degrees (0 ... 359)
	 */
	public void rotateTo(Conformer conformer, short torsion) {
		if (torsion != conformer.getBondTorsion(mBond)) {
			int deltaTorsion = torsion - conformer.getBondTorsion(mBond);
			rotateSmallerSide(conformer, Math.PI * deltaTorsion / 180.0);
			conformer.setBondTorsion(mBond, torsion);
			}
		}

	/**
	 * For terminal fragments with D2 or D3 symmetry we may remove parts
	 * of the torsion list, because we would get equivalent conformers.
	 */
	private void removeEquivalentTorsions(StereoMolecule mol) {
		int symmetryCount1 = (mFragment1.getConnectionPointCount() != 1) ? 1
				: countSymmetricalTerminalNeighbors(mol, mTorsionAtom[1], mRearAtom[0]);
		int symmetryCount2 = (mFragment2.getConnectionPointCount() != 1) ? 1
				: countSymmetricalTerminalNeighbors(mol, mTorsionAtom[2], mRearAtom[1]);

		if (symmetryCount1 == 1 && symmetryCount2 == 1)
        	return;

		int symmetryCount = (symmetryCount1 == symmetryCount2) ?
        		symmetryCount1 : symmetryCount1 * symmetryCount2;

		assert(mTorsion.length % symmetryCount == 0);
		int count = mTorsion.length / symmetryCount;

		short[] newTorsion = new short[count];
		short[] newFrequency = new short[count];
		short[][] newRange = new short[count][];
        for (int i=0; i<count; i++) {
        	newTorsion[i] = mTorsion[i];
        	newFrequency[i] = (short)(mFrequency[i] * symmetryCount);
        	newRange[i] = mTorsionRange[i];
        	}

        mTorsion = newTorsion;
        mFrequency = newFrequency;
        mTorsionRange = newRange;
		}

    /**
     * Checks whether all neighbors of atom (not considering rearAtom)
     * have the same symmetry rank. Implicit hydrogens are considered.
     * For sp2 atoms this requires 2 equally ranked neighbors, for sp3
     * atoms there must be three.
     * @param mol
     * @param atom
     * @param rearAtom connected to atom and not considered
     * @return
     */
    private int countSymmetricalTerminalNeighbors(StereoMolecule mol, int atom, int rearAtom) {
		if (mol.getAtomPi(atom) == 2)
			return 1;
		if ((mol.getAtomPi(atom) == 1 || mol.isFlatNitrogen(atom)) && mol.getConnAtoms(atom) != 3)
			return 1;
		if (mol.getAtomPi(atom) == 0 && mol.getConnAtoms(atom) != 4)
			return 1;

		int rank = -2;
		for (int i=0; i<mol.getConnAtoms(atom); i++) {
			int connAtom = mol.getConnAtom(atom, i);
			if (connAtom != rearAtom) {
				if (rank == -2)
					rank = mol.getSymmetryRank(connAtom);
				else if (rank != mol.getSymmetryRank(connAtom))
					return 1;
				}
			}

		return mol.getConnAtoms(atom)-1;
    	}

	/**
	 * Rotates all atoms in atomList around the axis leading from atom1 through atom2
	 * by angle. The coordinates of atom1 and atom2 are not touched.
	 * @param mol
	 * @param atom1
	 * @param atom2
	 * @param Alpha
	 * @param atomList
	 */
	private void rotateSmallerSide(Conformer conformer, double alpha) {
		float x0 = conformer.x[mTorsionAtom[2]];
		float y0 = conformer.y[mTorsionAtom[2]];
		float z0 = conformer.z[mTorsionAtom[2]];
		float[] n = getUnitVector(x0 - conformer.x[mTorsionAtom[1]],
								  y0 - conformer.y[mTorsionAtom[1]],
								  z0 - conformer.z[mTorsionAtom[1]]);
		float[][] m = getRotationMatrix(n, (mRotationCenter == mTorsionAtom[1]) ? alpha : -alpha);

		for (int atom:mSmallerSideAtomList) {
			if (atom != mRotationCenter) {
				float x = conformer.x[atom] - x0;
				float y = conformer.y[atom] - y0;
				float z = conformer.z[atom] - z0;
				conformer.x[atom] = x*m[0][0]+y*m[0][1]+z*m[0][2] + x0;
				conformer.y[atom] = x*m[1][0]+y*m[1][1]+z*m[1][2] + y0;
				conformer.z[atom] = x*m[2][0]+y*m[2][1]+z*m[2][2] + z0;
				}
			}
		}
	}