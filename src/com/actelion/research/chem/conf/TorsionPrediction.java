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

import com.actelion.research.chem.StereoMolecule;

public class TorsionPrediction {
	private short[] mTorsion,mFrequency;
	private short[][] mTorsionRange;

	public TorsionPrediction(StereoMolecule mol, int[] torsionAtom) {
		int atom1 = torsionAtom[1];
		int atom2 = torsionAtom[2];
		int conns1 = mol.getConnAtoms(atom1) - 1;
		int conns2 = mol.getConnAtoms(atom2) - 1;

		if (conns1 > 3 || conns2 > 3) {	// assume octahedral neighbors
			mTorsion = new short[4];
			mTorsion[0] = 45;
			mTorsion[1] = 135;
			mTorsion[2] = 225;
			mTorsion[2] = 315;
			mTorsionRange = new short[4][2];
			mTorsionRange[0][0] = 30;
			mTorsionRange[0][1] = 60;
			mTorsionRange[1][0] = 120;
			mTorsionRange[1][1] = 150;
			mTorsionRange[2][0] = 210;
			mTorsionRange[2][1] = 240;
			mTorsionRange[2][0] = 300;
			mTorsionRange[2][1] = 330;
			mFrequency = new short[4];
			mFrequency[0] = 25;
			mFrequency[1] = 25;
			mFrequency[2] = 25;
			mFrequency[3] = 25;
			}
		// sp3 - sp3
		else if ((mol.getAtomPi(atom1) == 0 || mol.getAtomicNo(atom1) > 9)
		 && (mol.getAtomPi(atom2) == 0 || mol.getAtomicNo(atom2) > 9)) {
			if ((conns1 == 3 && conns2 == 3)
			 || (conns1 == 3 && conns2 == 2)
			 || (conns1 == 3 && conns2 == 1)
			 || (conns1 == 2 && conns2 == 3)
			 || (conns1 == 1 && conns2 == 3)
			 || (conns1 == 2 && conns2 == 2 && (torsionAtom[0] != -1 || torsionAtom[3] != -1))) {
				mTorsion = new short[3];
				mTorsion[0] = 60;
				mTorsion[1] = 180;
				mTorsion[2] = 300;
				mTorsionRange = new short[3][2];
				mTorsionRange[0][0] = 45;
				mTorsionRange[0][1] = 75;
				mTorsionRange[1][0] = 165;
				mTorsionRange[1][1] = 195;
				mTorsionRange[2][0] = 285;
				mTorsionRange[2][1] = 315;
				mFrequency = new short[3];
				mFrequency[0] = 33;
				mFrequency[1] = 33;
				mFrequency[2] = 33;
				}
			else if ((conns1 == 1 && conns2 == 2 && torsionAtom[3] == -1)
				  || (conns1 == 2 && conns2 == 1 && torsionAtom[0] == -1)) {
				mTorsion = new short[3];
				mTorsion[0] = 60;
				mTorsion[1] = 180;
				mTorsion[2] = 300;
				mTorsionRange = new short[3][2];
				mTorsionRange[0][0] = 45;
				mTorsionRange[0][1] = 75;
				mTorsionRange[1][0] = 165;
				mTorsionRange[1][1] = 195;
				mTorsionRange[2][0] = 285;
				mTorsionRange[2][1] = 315;
				mFrequency = new short[3];
				mFrequency[0] = 40;
				mFrequency[1] = 20;
				mFrequency[2] = 40;
				}
			else if ((conns1 == 1 && conns2 == 1)
				  || (conns1 == 1 && conns2 == 2 && torsionAtom[3] != -1)
				  || (conns1 == 2 && conns2 == 1 && torsionAtom[0] != -1)
				  || (conns1 == 2 && conns2 == 2 && torsionAtom[0] == -1 && torsionAtom[3] == -1)) {
				mTorsion = new short[3];
				mTorsion[0] = 60;
				mTorsion[1] = 180;
				mTorsion[2] = 300;
				mTorsionRange = new short[3][2];
				mTorsionRange[0][0] = 45;
				mTorsionRange[0][1] = 75;
				mTorsionRange[1][0] = 165;
				mTorsionRange[1][1] = 195;
				mTorsionRange[2][0] = 285;
				mTorsionRange[2][1] = 315;
				mFrequency = new short[3];
				mFrequency[0] = 25;
				mFrequency[1] = 50;
				mFrequency[2] = 25;
				}
			}
		else if (((mol.getAtomPi(atom1) == 0 || mol.getAtomicNo(atom1) > 9) && mol.getAtomPi(atom2) == 1)
			  || ((mol.getAtomPi(atom2) == 0 || mol.getAtomicNo(atom2) > 9) && mol.getAtomPi(atom1) == 1)) {
			if (conns1 == 3 || conns2 == 3) {
				mTorsion = new short[6];
				mTorsion[0] = 0;
				mTorsion[1] = 60;
				mTorsion[2] = 120;
				mTorsion[3] = 180;
				mTorsion[4] = 240;
				mTorsion[5] = 300;
				mTorsionRange = new short[6][2];
				mTorsionRange[0][0] = -15;
				mTorsionRange[0][1] = 15;
				mTorsionRange[1][0] = 45;
				mTorsionRange[1][1] = 75;
				mTorsionRange[2][0] = 105;
				mTorsionRange[2][1] = 135;
				mTorsionRange[3][0] = 165;
				mTorsionRange[3][1] = 195;
				mTorsionRange[4][0] = 225;
				mTorsionRange[4][1] = 255;
				mTorsionRange[5][0] = 285;
				mTorsionRange[5][1] = 315;
				mFrequency = new short[6];
				mFrequency[0] = 16;
				mFrequency[1] = 16;
				mFrequency[2] = 16;
				mFrequency[3] = 16;
				mFrequency[4] = 16;
				mFrequency[5] = 16;
				}
			else if (conns1 == 1 && conns2 == 1) {
				mTorsion = new short[2];
				mTorsion[0] = 120;
				mTorsion[1] = 240;
				mTorsionRange = new short[2][2];
				mTorsionRange[0][0] = 105;
				mTorsionRange[0][1] = 135;
				mTorsionRange[1][0] = 225;
				mTorsionRange[1][1] = 255;
				mFrequency = new short[2];
				mFrequency[0] = 50;
				mFrequency[1] = 50;
				}
			else if ((mol.getAtomPi(atom1) == 1 && conns1 == 2 && conns2 == 1)
				  || (mol.getAtomPi(atom2) == 1 && conns2 == 2 && conns1 == 1)) {
				mTorsion = new short[2];
				mTorsion[0] = 90;
				mTorsion[1] = 270;
				mTorsionRange = new short[2][2];
				mTorsionRange[0][0] = 75;
				mTorsionRange[0][1] = 105;
				mTorsionRange[1][0] = 255;
				mTorsionRange[1][1] = 285;
				mFrequency = new short[2];
				mFrequency[0] = 50;
				mFrequency[1] = 50;
				}
			else if ((mol.getAtomPi(atom1) == 1 && conns1 == 1 && conns2 == 2 && torsionAtom[3] == -1)
				  || (mol.getAtomPi(atom2) == 1 && conns2 == 1 && conns1 == 2 && torsionAtom[0] == -1)) {
					mTorsion = new short[3];
					mTorsion[0] = 0;
					mTorsion[1] = 120;
					mTorsion[2] = 240;
					mTorsionRange = new short[3][2];
					mTorsionRange[0][0] = -15;
					mTorsionRange[0][1] = 15;
					mTorsionRange[1][0] = 105;
					mTorsionRange[1][1] = 135;
					mTorsionRange[2][0] = 225;
					mTorsionRange[2][1] = 255;
					mFrequency = new short[3];
					mFrequency[0] = 60;
					mFrequency[1] = 20;
					mFrequency[2] = 20;
					}
			else if ((mol.getAtomPi(atom1) == 1 && conns1 == 1 && conns2 == 2 && torsionAtom[3] != -1)
				  || (mol.getAtomPi(atom2) == 1 && conns2 == 1 && conns1 == 2 && torsionAtom[0] != -1)) {
					mTorsion = new short[3];
					mTorsion[0] = 0;
					mTorsion[1] = 120;
					mTorsion[2] = 240;
					mTorsionRange = new short[3][2];
					mTorsionRange[0][0] = -15;
					mTorsionRange[0][1] = 15;
					mTorsionRange[1][0] = 105;
					mTorsionRange[1][1] = 135;
					mTorsionRange[2][0] = 225;
					mTorsionRange[2][1] = 255;
					mFrequency = new short[3];
					mFrequency[0] = 20;
					mFrequency[1] = 40;	// in reality the double bond is syn to the hydrogen or a hetero atom
					mFrequency[2] = 40;
					}
			else if (conns1 == 2 && conns2 == 2) {
				if (torsionAtom[0] == -1 || torsionAtom[3] == -1) {
					mTorsion = new short[2];
					mTorsion[0] = 0;
					mTorsion[1] = 180;
					mTorsionRange = new short[2][2];
					mTorsionRange[0][0] = -15;
					mTorsionRange[0][1] = 15;
					mTorsionRange[1][0] = 165;
					mTorsionRange[1][1] = 195;
					mFrequency = new short[2];
					mFrequency[0] = 50;
					mFrequency[1] = 50;
					}
				else {
					mTorsion = new short[2];
					mTorsion[0] = 90;	// this should be 60 or 120 depending on sp3 reference neighbor
					mTorsion[1] = 270;	// this should be 240 or 300 depending on sp3 reference neighbor
					mTorsionRange = new short[2][2];
					mTorsionRange[0][0] = 75;
					mTorsionRange[0][1] = 105;
					mTorsionRange[1][0] = 255;
					mTorsionRange[1][1] = 285;
					mFrequency = new short[2];
					mFrequency[0] = 50;
					mFrequency[1] = 50;
					}
				}
			}
		else if (mol.getAtomPi(atom1) == 1 && mol.getAtomPi(atom2) == 1) {
			if (conns1 == 1 && conns2 == 1) {
				mTorsion = new short[2];
				mTorsion[0] = 0;
				mTorsion[1] = 180;
				mTorsionRange = new short[2][2];
				mTorsionRange[0][0] = -15;
				mTorsionRange[0][1] = 15;
				mTorsionRange[1][0] = 165;
				mTorsionRange[1][1] = 195;
				mFrequency = new short[2];
				mFrequency[0] = 10;
				mFrequency[1] = 90;
				}
			else {
				mTorsion = new short[6];
				mTorsion[0] = 0;
				mTorsion[1] = 50;
				mTorsion[2] = 130;
				mTorsion[3] = 180;
				mTorsion[4] = 230;
				mTorsion[5] = 310;
				mTorsionRange = new short[6][2];
				mTorsionRange[0][0] = -15;
				mTorsionRange[0][1] = 15;
				mTorsionRange[1][0] = 35;
				mTorsionRange[1][1] = 65;
				mTorsionRange[2][0] = 115;
				mTorsionRange[2][1] = 145;
				mTorsionRange[3][0] = 165;
				mTorsionRange[3][1] = 195;
				mTorsionRange[4][0] = 215;
				mTorsionRange[4][1] = 245;
				mTorsionRange[5][0] = 295;
				mTorsionRange[5][1] = 325;
				mFrequency = new short[6];
				mFrequency[0] = 40;
				mFrequency[1] = 5;
				mFrequency[2] = 5;
				mFrequency[3] = 40;
				mFrequency[4] = 5;
				mFrequency[5] = 5;
				}
			}
		else {
			mTorsion = new short[1];
			mTorsion[0] = 180;
			mTorsionRange = new short[1][2];
			mTorsionRange[0][0] = 165;
			mTorsionRange[0][1] = 195;
			mFrequency = new short[1];
			mFrequency[0] = 100;
			}
		}

	public short[] getTorsions() {
		return mTorsion;
		}

	public short[] getTorsionFrequencies() {
		return mFrequency;
		}

	public short[][] getTorsionRanges() {
		return mTorsionRange;
		}
	}
