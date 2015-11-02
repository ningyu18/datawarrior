/*
 * @(#)TorsionSetStrategy.java
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

import java.util.ArrayList;
import java.util.Arrays;

import com.actelion.research.util.UniqueList;

/**
 * Knowing all rotatable bonds of an underlying molecule and knowing those rigid fragments
 * that are connected by them, the TorsionSetStrategy provides a mechanism to deliver
 * valid and unique torsion sets, each effectively defining an individual conformer.
 * While the strategies of the implementing classes differ (random, biased random, systematic, ...)
 * this parent class provides the following common functionality:<br>
 * <li>maintains a history of already delivered torsion sets.
 * <li>provides a novelty check for torsion sets suggested by derived classes.
 * <li>check new torsion sets for atom collisions.
 * <li>maintains a list of torsion set subsets that cause atom collisions.
 * <li>provides a quick check, whether a new torsion set contains a subset causing collisions.
 * <br><br>A TorsionSetStrategy is employed by a ConformerGenerator, which repeatedly calls
 * getNextTorsionIndexes(), creates the conformer, checks for atom collisions and reports back,
 * whether and how serious atom collisions occurred.
 */
public abstract class TorsionSetStrategy {
	private static final long[] BITS = { 0x00l, 0x01l, 0x03l, 0x07l, 0x0fl, 0x1fl, 0x3fl, 0x7fl };
	private static final int DEFAULT_MAX_TOTAL_COUNT = 1024;

	protected RotatableBond[] mRotatableBond;
	protected Rigid3DFragment[] mRigidFragment;
	private TorsionSet mPreviousDeliveredTorsionSet;
	private int mFragmentCount,mEncodingLongCount,
				mCollisionCount,mTotalCount,mMaxTotalCount,mPermutationCount;
	private int[][][]	mBondsBetweenFragments;
	private int[][] mConnFragmentNo;
	private int[][] mConnRotatableBondNo;
	private int[] mEncodingBitCount;
	private int[] mEncodingBitShift;
	private int[] mEncodingLongIndex;
	private int[] mGraphFragment = new int[mFragmentCount];
	private int[] mGraphBond = new int[mFragmentCount];
	private int[] mGraphParent = new int[mFragmentCount];
	private boolean[] mGraphFragmentHandled = new boolean[mFragmentCount];
	private UniqueList<TorsionSet> mTorsionSetList;
	private ArrayList<TorsionSetEliminationRule> mEliminationRuleList;

	public TorsionSetStrategy(RotatableBond[] rotatableBond, Rigid3DFragment[] fragment) {
		mRotatableBond = rotatableBond;
		mRigidFragment = fragment;

		// create arrays of neighbor fragment no's
		mFragmentCount = 0;
		for (RotatableBond rb:mRotatableBond)
			mFragmentCount = Math.max(mFragmentCount, Math.max(1+rb.getFragmentNo(0), 1+rb.getFragmentNo(1)));
		int[] count = new int[mFragmentCount];
		for (RotatableBond rb:mRotatableBond) {
			count[rb.getFragmentNo(0)]++;
			count[rb.getFragmentNo(1)]++;
			}
		mConnFragmentNo = new int[count.length][];
		mConnRotatableBondNo = new int[count.length][];
		for (int i=0; i<count.length; i++) {
			mConnFragmentNo[i] = new int[count[i]];
			mConnRotatableBondNo[i] = new int[count[i]];
			}
		Arrays.fill(count, 0);
		for (int i=0; i<mRotatableBond.length; i++) {
			int f1 = mRotatableBond[i].getFragmentNo(0);
			int f2 = mRotatableBond[i].getFragmentNo(1);
			mConnFragmentNo[f1][count[f1]] = f2;
			mConnFragmentNo[f2][count[f2]] = f1;
			mConnRotatableBondNo[f1][count[f1]] = i;
			mConnRotatableBondNo[f2][count[f2]] = i;
			count[f1]++;
			count[f2]++;
			}

		mGraphFragment = new int[mFragmentCount];
		mGraphBond = new int[mFragmentCount];
		mGraphParent = new int[mFragmentCount];
		mGraphFragmentHandled = new boolean[mFragmentCount];

		// initialize array for rotatable bond sequences from fragment pairs
		mBondsBetweenFragments = new int[mFragmentCount][][];
		for (int f1=1; f1<mFragmentCount; f1++) {
			mBondsBetweenFragments[f1] = new int[f1][];
			for (int f2=0; f2<f1; f2++)
				mBondsBetweenFragments[f1][f2] = getRotatableBondsBetween(f1, f2);
			}

		mPermutationCount = 1;
		mEncodingBitCount = new int[mRotatableBond.length+mRigidFragment.length];
		mEncodingBitShift = new int[mRotatableBond.length+mRigidFragment.length];
		mEncodingLongIndex = new int[mRotatableBond.length+mRigidFragment.length];
		int bitCount = 0;
		int longIndex = 0;
		int index = 0;
		for (int i=0; i<mRotatableBond.length; i++) {
			mPermutationCount *= mRotatableBond[i].getTorsionCount();
			int bits = 0;
			if (mRotatableBond[i].getTorsionCount() > 1) {
				bits = 0;
				int maxTorsionIndex = mRotatableBond[i].getTorsionCount()-1;
				while (maxTorsionIndex > 0) {
					maxTorsionIndex >>= 1;
					bits++;
					}
				}
			if (bitCount + bits <= 64) {
				mEncodingBitShift[index] = bitCount;
				bitCount += bits;
				}
			else {
				longIndex++;
				mEncodingBitShift[index] = 0;
				bitCount = 0;
				}
			mEncodingBitCount[index] = bits;
			mEncodingLongIndex[index] = longIndex;
			index++;
			}
		for (int i=0; i<mRigidFragment.length; i++) {
			mPermutationCount *= mRigidFragment[i].getConformerCount();
			int bits = 0;
			if (mRigidFragment[i].getConformerCount() > 1) {
				bits = 0;
				int maxTorsionIndex = mRigidFragment[i].getConformerCount()-1;
				while (maxTorsionIndex > 0) {
					maxTorsionIndex >>= 1;
					bits++;
					}
				}
			if (bitCount + bits <= 64) {
				mEncodingBitShift[index] = bitCount;
				bitCount += bits;
				}
			else {
				longIndex++;
				mEncodingBitShift[index] = 0;
				bitCount = 0;
				}
			mEncodingBitCount[index] = bits;
			mEncodingLongIndex[index] = longIndex;
			index++;
			}
		mEncodingLongCount = longIndex+1;

		mEliminationRuleList = new ArrayList<TorsionSetEliminationRule>();

		mTorsionSetList = new UniqueList<TorsionSet>();
		mPreviousDeliveredTorsionSet = null;
		mCollisionCount = 0;
		mTotalCount = 0;
		mMaxTotalCount = DEFAULT_MAX_TOTAL_COUNT;
		}

/*	public UniqueList<TorsionSet> getTorsionSetList() {
		return mTorsionSetList;
		}
*/

	/**
	 * @return pre-calculated number of all possible torsion index permutations
	 */
	public int getPermutationCount() {
		return mPermutationCount;
		}

	/**
	 * @return number of generated torsion sets that resulted in collisions
	 */
	public int getFailureCount() {
		return mCollisionCount;
		}

	/**
	 * @return number of generated torsion sets till now
	 */
	public int getTorsionSetCount() {
		return mTotalCount;
		}

	/**
	 * Creates a new TorsionSet object from a torsion index array.
	 * An An overall likelyhood of the new torsion set is calculated
	 * by multiplying individual likelyhoods of the specific torsion angles
	 * of all underlying RotatableBonds.
	 * @param torsionIndex
	 * @param conformerIndex null or conformer index for every rigid fragment
	 * @return
	 */
	protected TorsionSet createTorsionSet(int[] torsionIndex, int[] conformerIndex) {
		float likelyhood = 1f;
		for (int j=0; j<mRotatableBond.length; j++)
			likelyhood *= mRotatableBond[j].getTorsionLikelyhood(torsionIndex[j]);
		if (conformerIndex != null)
			for (int j=0; j<mRigidFragment.length; j++)
				likelyhood *= mRigidFragment[j].getConformerLikelyhood(conformerIndex[j]);
		return new TorsionSet(torsionIndex, conformerIndex, mEncodingBitShift, mEncodingLongIndex, likelyhood);
		}

/*	protected TorsionSet createTorsionSet(int[] torsionIndex, float likelyhood) {
		return new TorsionSet(torsionIndex, mEncodingBitShift, mEncodingLongIndex, likelyhood);
		}	*/

	protected boolean isNewTorsionSet(TorsionSet ts) {
		return !mTorsionSetList.contains(ts);
		}

	/**
	 * Creates the next set of torsion indexes to be tried by the ConformerGenerator.
	 * If the previous set obtained by this method resulted in an atom collision,
	 * then the collision intensity matrix among all rigid fragments and the overall
	 * collision intensity sum need to be passed.
	 * Torsion sets returned by this method are guaranteed to avoid torsion sequences that
	 * had previously caused collisions.
	 * To achieve this goal, TorsionSetStragegy repeatedly requests new TorsionSets
	 * from the strategy implementation and returns the first set that is not in conflict
	 * with the collision rules already collected or until no further set exists.
	 * @param collisionIntensityMatrix null if previous torsion indexes did not collide
	 * @param collisionIntensitySum in case of a previous collision
	 * @return torsion index set that adheres to already known collision rules
	 */
	public final TorsionSet getNextTorsionSet(float[][] collisionIntensityMatrix, float collisionIntensitySum) {
		if (mTotalCount == mMaxTotalCount)
			return null;

		if (collisionIntensityMatrix != null) {
			mPreviousDeliveredTorsionSet.setCollisionIntensitySum(collisionIntensitySum);
			reportCollision(collisionIntensityMatrix, collisionIntensitySum);
			mCollisionCount++;
			}

		TorsionSet ts = getNextTorsionSet(mPreviousDeliveredTorsionSet);

		while (ts != null && matchesEliminationRule(ts)) {
			mCollisionCount++;
			mTotalCount++;

			if (mTotalCount == mMaxTotalCount)
				return null;

			ts = getNextTorsionSet(ts);
			}

		if (ts == null)
			return null;

		mTotalCount++;
		mPreviousDeliveredTorsionSet = ts;
		mTorsionSetList.add(ts);
		return ts;
		}

	/**
	 * If no collision free torsion set can be constructed, this method is called
	 * to get the torsion set with the least atom collision strain.
	 * @return
	 */
	public TorsionSet getBestCollidingTorsionIndexes() {
		float bestCollisionIntensity = Float.MAX_VALUE;
		TorsionSet bestTorsionSet = null;
		for (int i=0; i<mTorsionSetList.size(); i++) {
			TorsionSet ts = mTorsionSetList.get(i);
			if (bestCollisionIntensity > ts.getCollisionIntensitySum()) {
				bestCollisionIntensity = ts.getCollisionIntensitySum();
				bestTorsionSet = ts;
				}
			}
		return bestTorsionSet;
		}

	/**
	 * Provide the next set of torsion angles using a specific strategy and
	 * considering, which angle combinations were already tried, which had failed, and
	 * (depending on the strategy) considering the likelyhoods of particular torsions.
	 * @param previousTorsionSet previous torsion set which may or may not be used by strategy
	 * @return
	 */
	public abstract TorsionSet getNextTorsionSet(TorsionSet previousTorsionSet);

	/**
	 * Must be called if the torsion indexes delivered with getNextTorsionIndexes()
	 * caused an atom collision.
	 * Completes a dictionary of bond sequences with specific torsions that cause
	 * atom collisions. Depending on the strategy implementation, this disctionary
	 * is taken into account, when creating new torsion sets.
	 * @param collisionIntensityMatrix every value is a sum of collision intensities among all atoms belonging to two fragments
	 * @param collisionIntensitySum larger values represent less desirable torsion index sets
	 */
	private void reportCollision(float[][] collisionIntensityMatrix, float collisionIntensitySum) {
		int[] torsionIndex = mPreviousDeliveredTorsionSet.getTorsionIndexes();
		for (int f1=1; f1<collisionIntensityMatrix.length; f1++) {
			if (collisionIntensityMatrix[f1] != null) {
				for (int f2=0; f2<f1; f2++) {
					if (collisionIntensityMatrix[f1][f2] != 0f) {
						int[] rotatableBondIndex = mBondsBetweenFragments[f1][f2];
						long[] mask = new long[mEncodingLongCount];
						long[] data = new long[mEncodingLongCount];
						for (int i=0; i<rotatableBondIndex.length; i++) {
							int rb = rotatableBondIndex[i];
							data[mEncodingLongIndex[rb]] += (torsionIndex[rb] << mEncodingBitShift[rb]);
							mask[mEncodingLongIndex[rb]] += (BITS[mEncodingBitCount[rb]] << mEncodingBitShift[rb]);
							}
						boolean isCovered = false;
						ArrayList<TorsionSetEliminationRule> obsoleteList = null;
						for (TorsionSetEliminationRule er:mEliminationRuleList) {
							if (er.isCovered(mask, data)) {
								isCovered = true;
								break;
								}
							if (er.isMoreGeneral(mask, data)) {
								if (obsoleteList == null)
									obsoleteList = new ArrayList<TorsionSetEliminationRule>();
								obsoleteList.add(er);
								}
							}
						if (!isCovered) {
							if (obsoleteList != null)
								mEliminationRuleList.removeAll(obsoleteList);
							mEliminationRuleList.add(new TorsionSetEliminationRule(mask, data, collisionIntensityMatrix[f1][f2]));
//							eliminateTorsionSets(mask, data);
// currently validity checking is done in getNextTorsionIndexes() against the list of elimination rules
							}
						}
					}
				}
			}
		}

	/**
	 * Calculates for every rotatable bond and for every rigid fragment a collision intensity sum
	 * for the given torsion/conformer state from the collision rules already known.
	 * If the given torsion or conformer index of the collidingTorsionSet is covered by a rule
	 * then the rule's collision intensity is added to all involved bond's intensity sums.
	 * Strategies may focus on those bond torsions or conformers first that have the highest
	 * collision intensity sums.
	 * @param collidingTorsionSet
	 * @return collision intensity sum for every rotatable bond and rigid fragment
	 */
	protected float[] getBondAndFragmentCollisionIntensities(TorsionSet collidingTorsionSet) {
		float[] collisionIntensity = new float[mRotatableBond.length+mRigidFragment.length];
		for (TorsionSetEliminationRule er:mEliminationRuleList) {
			if (collidingTorsionSet.matches(er)) {
				long[] mask = er.getMask();
				for (int i=0; i<collisionIntensity.length; i++)
					if ((mask[mEncodingLongIndex[i]] & (1L << mEncodingBitShift[i])) != 0L)
						collisionIntensity[i] += er.getCollisionIntensity();
				}
			}
		return collisionIntensity;
		}

	/**
	 * If the implementation of the TorsionStrategy is caching some kind of a pre-calculated
	 * list of TorsionSets, then those sets should be removed that match the elimination
	 * condition defined by mask and data, i.e. TorsionSets that return true on matches(mask, data).
	 * @param mask
	 * @param data
	 *
	public abstract void eliminateTorsionSets(long[] mask, long[] data);	*/


	/**
	 * With best current knowledge about colliding torsion combinations
	 * and based on the individual frequencies of currently active torsions
	 * this method returns the conformers's overall contribution to the
	 * total set of non colliding conformers.
	 * @return this conformer's contribution to all conformers
	 */
	public float getContribution(TorsionSet torsionSet) {
		float likelyhood = 1f;
		for (int i=0; i<mRotatableBond.length; i++)
			likelyhood *= mRotatableBond[i].getTorsionLikelyhood(torsionSet.getTorsionIndexes()[i]);
		for (int i=0; i<mRigidFragment.length; i++)
			likelyhood *= mRigidFragment[i].getConformerLikelyhood(torsionSet.getConformerIndexes()[i]);
		return likelyhood;
		}

	private boolean matchesEliminationRule(TorsionSet ts) {
		for (TorsionSetEliminationRule er:mEliminationRuleList)
			if (ts.matches(er))
				return true;
		return false;
		}

	private int[] getRotatableBondsBetween(int f1, int f2) {
		Arrays.fill(mGraphFragmentHandled, false);
		mGraphFragment[0] = f1;
		mGraphFragmentHandled[f1] = true;
		int current = 0;
		int highest = 0;
		while (current <= highest) {
			for (int i=0; i<mConnFragmentNo[mGraphFragment[current]].length; i++) {
				int candidate = mConnFragmentNo[mGraphFragment[current]][i];
				if (candidate == f2) {
					int bondCount = 1;
					int index = current;
					while (index != 0) {
						bondCount++;
						index = mGraphParent[index];
						}
					int[] rotatableBondIndex = new int[bondCount];
					rotatableBondIndex[0] = mConnRotatableBondNo[mGraphFragment[current]][i];
					bondCount = 1;
					while (current != 0) {
						rotatableBondIndex[bondCount++] = mGraphBond[current];
						current = mGraphParent[current];
						}
					return rotatableBondIndex;
					}
				if (!mGraphFragmentHandled[candidate]) {
					mGraphFragmentHandled[candidate] = true;
					highest++;
					mGraphFragment[highest] = candidate;
					mGraphBond[highest] = mConnRotatableBondNo[mGraphFragment[current]][i];
					mGraphParent[highest] = current;
					}
				}
			current++;
			}
		return null;
		}
	}
