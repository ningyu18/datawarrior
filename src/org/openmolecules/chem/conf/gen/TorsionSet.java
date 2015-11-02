/*
 * @(#)TorsionSet.java
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

public class TorsionSet implements Comparable<TorsionSet> {
	private long[] mEncodedBits;
	private float mLikelyhood;
	private int[] mTorsionIndex,mConformerIndex;
	private float mCollisionIntensitySum;

	/**
	 * Creates a new conformer description from torsion and conformer indexes.
	 * @param torsionIndex torsion angle index for all rotatable bonds
	 * @param conformerIndex conformer index for every rigid fragment
	 * @param bitShift bit position of torsion and conformer indexes
	 * @param longIndex index on long array for shifted torsion and conformer indexes
	 * @param likelyhood all individual index likelyhoods multiplied
	 */
	public TorsionSet(int[] torsionIndex, int[] conformerIndex, int[] bitShift, int[] longIndex, float likelyhood) {
		mTorsionIndex = torsionIndex;
		mConformerIndex = conformerIndex;
		mLikelyhood = likelyhood;
		mEncodedBits = new long[1+longIndex[longIndex.length-1]];
		int i = 0;
		for (int index:torsionIndex) {
			mEncodedBits[longIndex[i]] += (index << bitShift[i]);
			i++;
			}
		for (int index:conformerIndex) {
			mEncodedBits[longIndex[i]] += (index << bitShift[i]);
			i++;
			}
		}

	public int[] getTorsionIndexes() {
		return mTorsionIndex;
		}

	public int[] getConformerIndexes() {
		return mConformerIndex;
		}

	public float getLikelyhood() {
		return mLikelyhood;
		}

	public float getCollisionIntensitySum() {
		return mCollisionIntensitySum;
		}

	public void setCollisionIntensitySum(float sum) {
		mCollisionIntensitySum = sum;
		}

	/**
	 * Checks whether all torsion indexes of a subset of all rotatable bonds
	 * match, when comparing this TorsionSet with the torsion indexes contained
	 * in data, considering only those rotatable bonds defined by mask.
	 * @param mask
	 * @param data
	 * @return
	 */
	public boolean matches(long[] mask, long[] data) {
		for (int i=0; i<mask.length; i++)
			if ((mEncodedBits[i] & mask[i]) != (data[i] & mask[i]))
				return false;

		return true;
		}

	public boolean matches(TorsionSetEliminationRule er) {
		return matches(er.getMask(), er.getData());
		}

	/**
	 * Allows to order TorsionSets in a unique way for quick uniqueness checking against a TreeSet.
	 */
	@Override
	public int compareTo(TorsionSet ts) {
		for (int i=0; i<mEncodedBits.length; i++)
			if (mEncodedBits[i] != ts.mEncodedBits[i])
				return (mEncodedBits[i] < ts.mEncodedBits[i]) ? -1 : 1;

		return 0;
		}

	@Override
	public boolean equals(Object ts) {
		return compareTo((TorsionSet)ts) == 0;
		}
	}
