/*
 * @(#)SelfOrganizedConformer.java
 *
 * Copyright 2014 openmolecules.org, Inc. All Rights Reserved.
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

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDescriptor;

public class SelfOrganizedConformer extends Conformer {
	private static final float	MAX_ATOM_STRAIN = 0.01f;

	// adjust such that a molecule strain of atom count times MAX_AVERARAGE_ATOM_STRAIN reduces frequency by factor 100
	private static final float	MAX_AVERARAGE_ATOM_STRAIN = 0.001f;

	private float	mMaxAtomStrain,mTotalStrain;
	private float[]	mAtomStrain,mRuleStrain;
	private boolean	mIsUsed;
	private TorsionDescriptor mTorsionDescriptor;

	public SelfOrganizedConformer(StereoMolecule mol) {
		super(mol);
		}

	/**
	 * Checks whether the total strain of this Conformer is larger than that of conformer,
	 * assuming that the calulated strain values are up-to-date.
	 * @param conformer
	 * @return
	 */
	public boolean isWorseThan(SelfOrganizedConformer conformer) {
		return mTotalStrain > conformer.mTotalStrain;
		}

	public void calculateStrain(ArrayList<ConformationRule> ruleList) {
		if (mAtomStrain != null)
			return;

		mAtomStrain = new float[getMolecule().getAllAtoms()];
		mRuleStrain = new float[ConformationRule.RULE_NAME.length];

		for (ConformationRule rule:ruleList)
			if (rule.isEnabled())
				mRuleStrain[rule.getRuleType()] += rule.addStrain(this, mAtomStrain);

		mMaxAtomStrain = 0f;
		mTotalStrain = 0f;
		for (int atom=0; atom<getMolecule().getAllAtoms(); atom++) {
			mTotalStrain += mAtomStrain[atom];
			if (mMaxAtomStrain < mAtomStrain[atom])
				mMaxAtomStrain = mAtomStrain[atom];
			}
		}

	public float getAtomStrain(int atom) {
		return mAtomStrain[atom];
		}

	public float getRuleStrain(int rule) {
		return mRuleStrain[rule];
		}

	public float getTotalStrain() {
		return mTotalStrain;
		}

	/**
	 * Tries to estimate the relative likelyhood of this conformer from atom strains
	 * considering an unstrained conformer to have a likelyhood of 1.0.
	 * @return conformer likelyhood
	 */
	public float getLikelyhood() {
		return (float)Math.pow(100, -mTotalStrain / (SelfOrganizedConformer.MAX_AVERARAGE_ATOM_STRAIN*getMolecule().getAllAtoms()));
		}

	protected boolean isAcceptable(ArrayList<ConformationRule> ruleList) {
		calculateStrain(ruleList);
		return (mMaxAtomStrain < MAX_ATOM_STRAIN
			 && mTotalStrain < MAX_AVERARAGE_ATOM_STRAIN * getMolecule().getAllAtoms());
		}

	public void invalidateStrain() {
		mAtomStrain = null;
		mRuleStrain = null;
		}

	/**
	 * Calculates the torsion descriptor for the current coordinates.
	 * Use calculateRotatableBondsForDescriptor() once and pass it
	 * for every new conformer to this method.
	 * @param rotatableBond set of rotatable bonds to be considered
	 */
	public void calculateDescriptor(int[] rotatableBond) {
		mTorsionDescriptor = new TorsionDescriptor(this, rotatableBond);
		}

	/**
	 * Returns true, if none of the torsion angles between both conformers
	 * are more different than TorsionDescriptor.TORSION_EQUIVALENCE_TOLERANCE;
	 * Calling this method requires that calculateDescriptor() has been called earlier.
	 * @param td
	 * @return true if all torsions are similar
	 */
	public boolean equals(SelfOrganizedConformer conformer) {
		return mTorsionDescriptor.equals(conformer.mTorsionDescriptor);
		}

	public boolean isUsed() {
		return mIsUsed;
		}

	public void setUsed(boolean isUsed) {
		mIsUsed = isUsed;
		}
	}
