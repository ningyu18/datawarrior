/*
 * @(#)StraightLineRule.java
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
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;

public class StraightLineRule extends ConformationRule {

	/**
	 * Test code...
	 * @param args
	 */
	public static void main(String args[]) {
		final int LINE_ATOMS = 7;
		final int FIRST_LINE_ATOM = 4;
		final int LAST_LINE_ATOM = FIRST_LINE_ATOM+LINE_ATOMS-1;
		int[] atom = new int[LINE_ATOMS];
		for (int i=0; i<LINE_ATOMS; i++)
			atom[i] = FIRST_LINE_ATOM+i;
		new StraightLineRule(atom);
		StereoMolecule mol = new IDCodeParser(true).getCompactMolecule("fkA@@@DjYfYhIbnRtZjjjjjXAPbIbDUD@");
		SelfOrganizedConformer conformer = new SelfOrganizedConformer(mol);
		for (int i=0; i<LINE_ATOMS; i++) {	// straight line
			conformer.x[FIRST_LINE_ATOM+i] = i;
			conformer.y[FIRST_LINE_ATOM+i] = i;
			conformer.z[FIRST_LINE_ATOM+i] = i;
			}
		conformer.y[FIRST_LINE_ATOM+1] += 1;
		conformer.y[FIRST_LINE_ATOM+2] -= 1;
		conformer.y[FIRST_LINE_ATOM+4] -= 1;
		conformer.y[FIRST_LINE_ATOM+5] += 1;

		System.out.println("------------------- you may copy and paste to DataWarrior ---------------------");
		System.out.println("p\tx\ty\tz\ttime");
		for (int i=FIRST_LINE_ATOM; i<=LAST_LINE_ATOM; i++)
			System.out.println("p"+i+"\t"+conformer.x[i]+"\t"+conformer.y[i]+"\t"+conformer.z[i]+"\tbefore");
		new StraightLineRule(atom).apply(conformer, 1f);
		for (int i=FIRST_LINE_ATOM; i<=LAST_LINE_ATOM; i++)
			System.out.println("p"+i+"\t"+conformer.x[i]+"\t"+conformer.y[i]+"\t"+conformer.z[i]+"\tafter");

		for (int i=0; i<LINE_ATOMS; i++) {	// straight line
			conformer.x[FIRST_LINE_ATOM+i] = i;
			conformer.y[FIRST_LINE_ATOM+i] = 0;
			conformer.z[FIRST_LINE_ATOM+i] = 0;
			}
		conformer.y[FIRST_LINE_ATOM+1] -= 1;
		conformer.y[FIRST_LINE_ATOM+2] += 1;
		conformer.y[FIRST_LINE_ATOM+4] += 1;
		conformer.y[FIRST_LINE_ATOM+5] -= 1;
		conformer.z[FIRST_LINE_ATOM+0] -= 1;
		conformer.z[FIRST_LINE_ATOM+2] += 1;
		conformer.z[FIRST_LINE_ATOM+4] += 1;
		conformer.z[FIRST_LINE_ATOM+6] -= 1;

		System.out.println("------------------- you may copy and paste to DataWarrior ---------------------");
		System.out.println("p\tx\ty\tz\ttime");
		for (int i=FIRST_LINE_ATOM; i<=LAST_LINE_ATOM; i++)
			System.out.println("p"+i+"\t"+conformer.x[i]+"\t"+conformer.y[i]+"\t"+conformer.z[i]+"\tbefore");
		new StraightLineRule(atom).apply(conformer, 1f);
		for (int i=FIRST_LINE_ATOM; i<=LAST_LINE_ATOM; i++)
			System.out.println("p"+i+"\t"+conformer.x[i]+"\t"+conformer.y[i]+"\t"+conformer.z[i]+"\tafter");
		}

	public StraightLineRule(int[] atom) {
		super(atom);
		}

    public static void calculateRules(ArrayList<ConformationRule> ruleList, StereoMolecule mol) {
	    boolean[] atomHandled = new boolean[mol.getAllAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (!atomHandled[atom] && mol.getAtomPi(atom) == 2 && mol.getAtomicNo(atom) <= 8) {
				int[] atomList = getLineFragmentAtoms(atom, mol);
				for (int i=0; i<atomList.length; i++)
					atomHandled[atomList[i]] = true;

				ruleList.add(new StraightLineRule(atomList));
				}
			}
		}

    /**
     * Compiles a list atom indexes of a straight line atom strand in strand order.
     * It also sets the atomHandled flags for all strand atoms.
     * @param seedAtom
     * @param atomHandled
     * @param mol
     * @return
     */
	private static int[] getLineFragmentAtoms(int seedAtom, StereoMolecule mol) {
		// crawl to the end of the chain
		int backAtom = mol.getConnAtom(seedAtom, 0);
		while (mol.getAllConnAtoms(seedAtom) != 1 && mol.getAtomPi(seedAtom) == 2) {	// while we are not at the chain end
			int tempAtom = backAtom;
			backAtom = seedAtom;
			seedAtom = (mol.getConnAtom(seedAtom, 0) == tempAtom) ?
					mol.getConnAtom(seedAtom, 1) : mol.getConnAtom(seedAtom, 0);
			}

		// count number of atoms in straight atom strand
		int rearAtom = seedAtom;	// invert direction for counting
		int headAtom = backAtom;
		int count = 2;
		while (mol.getAllConnAtoms(headAtom) != 1 && mol.getAtomPi(headAtom) == 2) {
			int tempAtom = rearAtom;
			rearAtom = headAtom;
			headAtom = (mol.getConnAtom(headAtom, 0) == tempAtom) ?
					mol.getConnAtom(headAtom, 1) : mol.getConnAtom(headAtom, 0);
			count++;
			}

		int[] atomList = new int[count];
		atomList[0] = seedAtom;
		atomList[1] = backAtom;
		int index = 1;
		while (mol.getAllConnAtoms(atomList[index]) != 1 && mol.getAtomPi(atomList[index]) == 2) {
			atomList[index+1] = (mol.getConnAtom(atomList[index], 0) == atomList[index-1]) ?
					mol.getConnAtom(atomList[index], 1) : mol.getConnAtom(atomList[index], 0);
			index++;
			}

		return atomList;
		}

	@Override
	public boolean apply(Conformer conformer, float cycleFactor) {
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
		int maxIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] > S[maxIndex])
				maxIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted line
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][maxIndex];

		float[] lambda = new float[mAtom.length];
		for (int i=0; i<mAtom.length; i++)
			lambda[i] = n[0]*A[i][0]+n[1]*A[i][1]+n[2]*A[i][2];

		// in early cycles check and correct order of atoms in strand
		if (cycleFactor == 1.0f) {
			boolean isInconsistent = false;
			boolean isIncreasing = (lambda[0] < lambda[1]);
			for (int i=2; i<mAtom.length; i++) {
				if (isIncreasing != (lambda[i-1] < lambda[i])) {
					isInconsistent = true;
					break;
					}
				}

			if (isInconsistent)
				return false;
/*	re-ordering atoms is problematic if we have multiple straight chains connecting in one point
 * 
 * 			if (isInconsistent) {	// re-arrange lambda values to get atoms back in order
				float s1 = 0f;
				float s2 = 0f;
				float min = Float.MAX_VALUE;
				float max = Float.MIN_VALUE;
				for (int i=0; i<mAtom.length; i++) {
					s1 += lambda[i] * (mAtom.length - 1 - i);
					s2 += lambda[i] * i;
					min = Math.min(min, lambda[i]);
					max = Math.max(max, lambda[i]);
					}
				isIncreasing = (s2 > s1);
				float delta = isIncreasing ? max-min : min-max;
				lambda[0] = isIncreasing ? min : max;
				for (int i=1; i<mAtom.length; i++)
					lambda[i] = lambda[i-1] + delta;
				}	*/
			}

		// for a point P on the fitted line is: P = COG + lamda * N
		for (int i=0; i<mAtom.length; i++) {
				// calculate lambda that gives the closest point to current atom location
			conformer.x[mAtom[i]] += cycleFactor*(lambda[i]*n[0]-A[i][0]);
			conformer.y[mAtom[i]] += cycleFactor*(lambda[i]*n[1]-A[i][1]);
			conformer.z[mAtom[i]] += cycleFactor*(lambda[i]*n[2]-A[i][2]);
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
		int maxIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] > S[maxIndex])
				maxIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted line
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][maxIndex];

		float totalStrain = 0;
		for (int i=0; i<mAtom.length; i++) {
				// calculate lamda that gives the closest point to current atom location
			float lamda = n[0]*A[i][0]+n[1]*A[i][1]+n[2]*A[i][2];
			float dx = lamda*n[0]-A[i][0];
			float dy = lamda*n[1]-A[i][1];
			float dz = lamda*n[2]-A[i][2];
			float panalty = dx*dx+dy*dy+dz*dz;
			atomStrain[mAtom[i]] += panalty;
			totalStrain += panalty;
			}

		return totalStrain;
		}

	@Override
	public int getRuleType() {
		return RULE_TYPE_LINE;
		}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("line rule:");
		super.addAtomList(sb);
		return sb.toString();
		}
	}
