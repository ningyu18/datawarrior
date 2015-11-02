/*
 * @(#)PlaneRule.java
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

import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;

public class PlaneRule extends ConformationRule {
	private int[] mPlaneAtom;	// these are the atoms that define the plane

	public PlaneRule(int[] atom, StereoMolecule mol) {
		super(atom);
		int count = 0;
		for (int i=0; i<atom.length; i++)
			if (mol.getConnAtoms(atom[i]) != 1)
				count++;
		if (count > 2) {
			mPlaneAtom = new int[count];
			count = 0;
			for (int i=0; i<atom.length; i++)
				if (mol.getConnAtoms(atom[i]) != 1)
					mPlaneAtom[count++] = atom[i];
			}
		else {
			mPlaneAtom = atom;
			}
		}

	@Override
	public int getRuleType() {
		return RULE_TYPE_PLANE;
		}

    public static void calculateRules(ArrayList<ConformationRule> ruleList, StereoMolecule mol) {
		boolean[] isFlatBond = new boolean[mol.getBonds()];
		int[] atomicNo = new int[2];
		for (int bond=0; bond<mol.getBonds(); bond++) {
			int bondAtom1 = mol.getBondAtom(0, bond);
			int bondAtom2 = mol.getBondAtom(1, bond);
		    atomicNo[0] = mol.getAtomicNo(bondAtom1);
		    atomicNo[1] = mol.getAtomicNo(bondAtom2);
			isFlatBond[bond] = (mol.isAromaticBond(bond)
							|| (mol.getBondOrder(bond) == 2
							 && atomicNo[0] <= 8 && atomicNo[1] <= 8
							 && mol.getAtomPi(bondAtom1) == 1
							 && mol.getAtomPi(bondAtom2) == 1
							 && mol.getAllConnAtoms(bondAtom1) > 1
							 && mol.getAllConnAtoms(bondAtom2) > 1));
			if (!isFlatBond[bond]) {
					// check if bond is an amide or ester bond
				if (mol.getBondOrder(bond) == 1) {
					for (int i=0; i<2; i++) {
						if ((atomicNo[i] == 7 || atomicNo[i] == 8) && atomicNo[1-i] == 6) {
							int carbon = mol.getBondAtom(1-i, bond);
							for (int j=0; j<mol.getConnAtoms(carbon); j++) {
								if (mol.getConnBondOrder(carbon, j) == 2
								 && mol.getAtomicNo(mol.getConnAtom(carbon, j)) == 8) {
									isFlatBond[bond] = true;
									break;
									}
								}
							}
						}
					}
				}
			}

		boolean[] isFlatBondAtom = new boolean[mol.getAtoms()];
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (isFlatBond[bond]) {
				isFlatBondAtom[mol.getBondAtom(0, bond)] = true;
				isFlatBondAtom[mol.getBondAtom(1, bond)] = true;
				}
			}

		int[] fragmentAtom = new int[mol.getAllAtoms()];
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (isFlatBond[bond]) {
				fragmentAtom[0] = mol.getBondAtom(0, bond);
				int count = getFlatFragmentAtoms(fragmentAtom, isFlatBond, mol);
				int[] atomList = new int[count];
				for (int i=0; i<count; i++)
					atomList[i] = fragmentAtom[i];
				ruleList.add(new PlaneRule(atomList, mol));
				}
			}

		// this covers flat atoms that are not part of flat bonds, e.g. C in C=S, B if not B-, etc.
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (!isFlatBondAtom[atom]) {
				if ((mol.getAtomicNo(atom) == 5 && mol.getAtomCharge(atom) == 0 && mol.getAllConnAtoms(atom) <= 3)
				 || (mol.getAtomicNo(atom) <= 8 && mol.getAtomPi(atom) == 1 && mol.getAllConnAtoms(atom) > 1)
				 || (mol.isFlatNitrogen(atom) && mol.getAtomPi(atom) != 2 && mol.getAllConnAtoms(atom) > 1)) {
					int[] atomList = new int[1+mol.getAllConnAtoms(atom)];
					for (int i=0; i<mol.getAllConnAtoms(atom); i++)
						atomList[i] = mol.getConnAtom(atom, i);
					atomList[mol.getAllConnAtoms(atom)] = atom;
					ruleList.add(new PlaneRule(atomList, mol));
					}
				}
			}
    	}

	private static int getFlatFragmentAtoms(int[] fragmentAtom, boolean[] isFlatBond, StereoMolecule mol) {
		// locate all atoms connected directly via flat bonds
		boolean[] isFragmentMember = new boolean[mol.getAllAtoms()];
		isFragmentMember[fragmentAtom[0]] = true;
		int current = 0;
		int highest = 0;
	 	while (current <= highest && mol.getAtomPi(fragmentAtom[current]) < 2) {
			for (int i=0; i<mol.getConnAtoms(fragmentAtom[current]); i++) {
				int candidateAtom = mol.getConnAtom(fragmentAtom[current], i);
				int candidateBond = mol.getConnBond(fragmentAtom[current], i);
				if (isFlatBond[candidateBond]) {
					if (!isFragmentMember[candidateAtom]) {
						fragmentAtom[++highest] = candidateAtom;
						isFragmentMember[candidateAtom] = true;
						}
					isFlatBond[candidateBond] = false;
					}
				}
			current++;
			}
	
			// attach first sphere of atoms connected via non-flat bonds
		for (int i=highest; i>=0; i--) {
			for (int j=0; j<mol.getAllConnAtoms(fragmentAtom[i]); j++) {
				int connAtom = mol.getConnAtom(fragmentAtom[i], j);
				if (!isFragmentMember[connAtom]) {
					fragmentAtom[++highest] = connAtom;
					isFragmentMember[connAtom] = true;
					}
				}	
			}
	
		return highest+1;
		}

	@Override
	public boolean apply(Conformer conformer, float cycleFactor) {
		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<mPlaneAtom.length; i++) {
			cog[0] += conformer.x[mPlaneAtom[i]];
			cog[1] += conformer.y[mPlaneAtom[i]];
			cog[2] += conformer.z[mPlaneAtom[i]];
			}
		for (int j=0; j<3; j++)
			cog[j] /= mPlaneAtom.length;

		float[][] A = new float[mPlaneAtom.length][3];
		for (int i=0; i<mPlaneAtom.length; i++) {
			A[i][0] = conformer.x[mPlaneAtom[i]] - cog[0];
			A[i][1] = conformer.y[mPlaneAtom[i]] - cog[1];
			A[i][2] = conformer.z[mPlaneAtom[i]] - cog[2];
			}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<mPlaneAtom.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += A[i][j] * A[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int minIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] < S[minIndex])
				minIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted plane
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][minIndex];

		for (int i=0; i<mAtom.length; i++) {
			float distance = -(n[0]*(conformer.x[mAtom[i]] - cog[0])
							 + n[1]*(conformer.y[mAtom[i]] - cog[1])
							 + n[2]*(conformer.z[mAtom[i]] - cog[2]));
			moveGroup(conformer, mAtom[i], mAtom, 0.5f*distance*cycleFactor*n[0],
												  0.5f*distance*cycleFactor*n[1],
												  0.5f*distance*cycleFactor*n[2]);
//			moveAtom(conformer, mAtom[i], 0.5f*distance*cycleFactor*n[0],
//										  0.5f*distance*cycleFactor*n[1],
//										  0.5f*distance*cycleFactor*n[2]);
			}

		return true;
		}

	@Override
	public float addStrain(Conformer conformer, float[] atomStrain) {
		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<mAtom.length; i++) {
			cog[0] += conformer.x[mAtom[i]];
			cog[1] += conformer.y[mAtom[i]];
			cog[2] += conformer.z[mAtom[i]];
			}
		for (int j=0; j<3; j++)
			cog[j] /= mAtom.length;

		float[][] A = new float[mAtom.length][3];
		for (int i=0; i<mAtom.length; i++) {
			A[i][0] = conformer.x[mAtom[i]] - cog[0];
			A[i][1] = conformer.y[mAtom[i]] - cog[1];
			A[i][2] = conformer.z[mAtom[i]] - cog[2];
			}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<mAtom.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += A[i][j] * A[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int minIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] < S[minIndex])
				minIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted plane
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][minIndex];

		float totalStrain = 0;
		for (int i=0; i<mAtom.length; i++) {
			float distance = -(n[0]*A[i][0] + n[1]*A[i][1] + n[2]*A[i][2]);
			float panalty = distance*distance;
			atomStrain[mAtom[i]] += panalty;
			totalStrain += panalty;
			}

		return totalStrain;
		}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("plane rule:");
		super.addAtomList(sb);
		return sb.toString();
		}
	}
