/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 *
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.chem.conf;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;

public class TorsionRelevanceHelper {
	/**
	 * The relevance of a rotatable bond and its torsion angle for creating substantially different conformers
	 * depends on how close the bond is to the center of the molecule. Bond relevance values range from
	 * 1.0/atomCount (e.g. bond to methyl group) to 1.0 (bond dividing molecule into two equally large parts).
	 * Ring bonds are assigned a relevance value of 0.33 independent of their location.
	 * @param mol
	 * @param rotatableBond array containing bond indexes for which to calculate relevance values
	 * @return
	 */
	public static final float[] getRelevance(StereoMolecule mol, int[] rotatableBond) {
		mol.ensureHelperArrays(Molecule.cHelperRings);

		float[] bondWeight = new float[rotatableBond.length];
		for (int i=0; i<bondWeight.length; i++) {
			if (mol.isRingBond(rotatableBond[i])) {
				bondWeight[i] = 0.33f;
				}
			else {
				int atom1 = mol.getBondAtom(0, rotatableBond[i]);
				int atom2 = mol.getBondAtom(1, rotatableBond[i]);
				if (mol.getConnAtoms(atom1) == 1 || mol.getConnAtoms(atom2) == 1) {
					bondWeight[i] = 1f / mol.getAtoms();	// rotates hydrogens only
					}
				else {
					int atomCount = mol.getSubstituentSize(atom1, atom2);
					bondWeight[i] = 2f * Math.min(atomCount, mol.getAtoms() - atomCount) / mol.getAtoms();
					}
				}
			}
		return bondWeight;
	}

	/**
	 * The relevance of a rotatable bond and its torsion angle for creating substantially different conformers
	 * depends on how close the bond is to the center of the molecule. Bond relevance values range from
	 * 1.0/atomCount (e.g. bond to methyl group) to 1.0 (bond dividing molecule into two equally large parts).
	 * @param mol
	 * @param isRotatableBond if null, then the relevance is calculated for every non-H-bond
	 * @return array with bond relevance values for all rotatable or all non-H-bonds
	 */
	public static final float[] getRelevance(StereoMolecule mol, boolean[] isRotatableBond) {
		mol.ensureHelperArrays(Molecule.cHelperRings);

		float[] bondWeight = new float[mol.getBonds()];
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (isRotatableBond == null || isRotatableBond[bond]) {
				if (mol.isRingBond(bond)) {
					bondWeight[bond] = 0.33f;
					}
				else {
					int atom1 = mol.getBondAtom(0, bond);
					int atom2 = mol.getBondAtom(1, bond);
					if (mol.getConnAtoms(atom1) == 1 || mol.getConnAtoms(atom2) == 1) {
						bondWeight[bond] = 1f / mol.getAtoms();	// rotates hydrogens only
						}
					else {
						int atomCount = mol.getSubstituentSize(atom1, atom2);
						bondWeight[bond] = 2f * Math.min(atomCount, mol.getAtoms() - atomCount) / mol.getAtoms();
						}
					}
				}
			}
		return bondWeight;
		}
	}
