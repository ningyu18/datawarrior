/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of ActelionMMFF94.
 *
 * ActelionMMFF94 is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ActelionMMFF94 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with ActelionMMFF94.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Paolo Tosco,Daniel Bergmann
 */

package com.actelion.research.forcefield.mmff.type;

import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.mmff.Tables;

/**
 * The charge class provides static functions for calculating formal and
 * partial charges on a molecule.
 */
public class Charge {
	/**
	 * Computes the MMFF formal charges for a molecules atoms.
	 *  @param mol The molecule to work on.
	 *  @return An array of formal charges indexed on the molecules atom
	 *      indices.
	 */
	public static double[] getFormalCharges(FFMolecule mol) {
		// Computes the Formal Charges

		double[] charges = new double[mol.getAllAtoms()];
		boolean[] conj;

		overatoms:
			for (int atom=0; atom<charges.length; atom++) {
				int type = mol.getMMFFAtomType(atom);
				charges[atom] = 0.0;

				switch (type) {
				case 32:
					// O2CM (Oxygen in carboxylate group)
				case 72:
					// SM (Anionic terminal Sulfur)
					for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
						int nbr = mol.getConnAtom(atom, i);
						int nbrType = mol.getMMFFAtomType(nbr);

						int nSecNtoNbr = 0;
						int nTermOStoNbr = 0;

						for (int j=0; j<mol.getAllConnAtoms(nbr); j++) {
							int nbr2 = mol.getConnAtom(nbr, j);

							// If it's Nitrogen with 2 neighbours and it is not
							// aromatic, increment the counter of secondary
							// Nitrogens.
							if (mol.getAtomicNo(nbr2) == 7
									&& degree(mol, nbr2) == 2
									&& !mol.isAromaticAtom(nbr2))
								nSecNtoNbr++;

							// If it's terminal Oxygen/Sulfur, increment the
							// terminal Oxygen/Sulfur counter.
							if ((mol.getAtomicNo(nbr2) == 8
									|| mol.getAtomicNo(nbr2) == 16)
									&& degree(mol, nbr2) == 1)
								nTermOStoNbr++;
						}

						// In case it's Sulfur with two terminal Oxygen/Sulfur
						// atoms and one secondary Nitrogen, this is a
						// deprotonated sulfonamide, so we should not consider
						// Nitrogen as a replacement for Oxygen/Sulfur in a
						// sulfone.
						if (mol.getAtomicNo(nbr) == 16 && nTermOStoNbr == 2
								&& nSecNtoNbr == 1)
							nSecNtoNbr = 0;

						// If the neighbour is Carbon.
						// O2CM (Oxygen in (thio)carboxylate group: charge is
						//      shared across 2 oxygens/sulfur atoms in
						//      (thio)carboxylate, 3 oxygen/sulfur atoms in
						//      (thio)carbonate.)
						// SM (Anionic terminal sulfur: charge is localized.)
						if (mol.getAtomicNo(nbr) == 6 && nTermOStoNbr > 0) {
							charges[atom] = nTermOStoNbr == 1 ? -1.0 :
								-(double)(nTermOStoNbr-1) / nTermOStoNbr;
							continue overatoms;
						}

						// If the neighbour is NO2 or NO3.
						// O3N (Nitrate anion Oxygen)
						if (nbrType == 45 && nTermOStoNbr == 3) {
							charges[atom] = -1.0 / 3.0;
							continue overatoms;
						}

						// OP (Oxygen in phosphine oxide)
						// O2P (One of 2 terminal O's on P)
						// O3P (One of 3 terminal O's on P)
						// O4P (One of 4 terminal O's on P)
						if (nbrType == 25 && nTermOStoNbr > 0) {
							charges[atom] = nTermOStoNbr == 1 ? 0.0 :
								-(double)(nTermOStoNbr-1) / nTermOStoNbr;
							continue overatoms;
						}

						// SO2 (Sulfone sulfur)
						// SO2N (Sulfonamide sulfur)
						// SO3 (Sulfonate group sulfur)
						// SO4 (Sulfate group sulfur)
						// SNO (Sulfur in nitrogen analog of a sulfone)
						if (nbrType == 18 && nTermOStoNbr > 0) {
							charges[atom] = nSecNtoNbr + nTermOStoNbr == 2
									? 0.0 : -(nSecNtoNbr + nTermOStoNbr - 2)
											/ (double)nTermOStoNbr;
							continue overatoms;
						}

						// SO2M (Sulfur in anionic sulfinate group)
						// SSOM (Tricoordinate sulfur in anionic thiosulfinate
						//      group)
						if (nbrType == 73 && nTermOStoNbr > 0) {
							charges[atom] = nTermOStoNbr == 1 ? 0.0 :
								-(double)(nTermOStoNbr-1) / nTermOStoNbr;
							continue overatoms;
						}

						// O4Cl (Oxygen in perchlorate anion)
						if (nbrType == 77 && nTermOStoNbr > 0) {
							charges[atom] = -1.0 / nTermOStoNbr;
							continue overatoms;
						}
					}
					break;

				case 76:
					int ring = 0;
					List<int[]> rings = mol.getAllRings();

					for (int r=0; r<rings.size(); r++)
						if (mol.isAtomMember(r, atom)) {
							ring = r;
							break;
						}

					if (ring < rings.size()) {
						int nNitrogensIn5Ring = 0;

						for (int a : rings.get(ring))
							if (a > -1 && mol.getMMFFAtomType(a) == 76)
								nNitrogensIn5Ring++;

						if (nNitrogensIn5Ring > 0) {
							charges[atom] = -1.0 / nNitrogensIn5Ring;
							continue overatoms;
						}
					}
					break;

				case 55:
				case 56:
				case 81:
					// NIM+ (Aromatic Nitrogen in imidazolium)
					// N5A+ (Positive Nitrogen in 5-ring alpha position)
					// N5B+ (Positive Nitrogen in 5-ring beta position)
					// N5+ (Positive Nitrogen in other 5-ring position)
					// Positive Nitrogen in other 5-ring position we need to
					// loop over all molecule atoms and find all those
					// nitrogens with atom type 81, 55 or 56, check whether
					// they are conjugated with ipso and keep on looping until
					// no more conjugated atoms can be found. Finally, we
					// divide the total formal charge that was found on the
					// conjugated system by the number of conjugated nitrogens
					// of types 81, 55 or 56 that were found. This is not
					// strictly what is described in the MMFF papers, but it
					// is the only way to get an integer total formal charge,
					// which makes sense to me probably such conjugated
					// systems are anyway out of the scope of MMFF, but this
					// is an attempt to correctly deal with them somehow.

					charges[atom] = mol.getAtomCharge(atom);
					int nConj = 1;
					int old_nConj = 0;
					conj = new boolean[mol.getNMovables()];
					conj[atom] = true;

					while (nConj > old_nConj) {
						old_nConj = nConj;

						for (int i=0; i<mol.getNMovables(); i++) {
							// If this atom is not marked as conj, move on.
							if (!conj[i])
								continue;

							// Loop over the neighbours.
							for (int j=0; j<mol.getAllConnAtoms(i); j++) {
								int nbr = mol.getConnAtom(i, j);
								int nbrType = mol.getMMFFAtomType(nbr);

								// If atom type is not 57 or 80, move on.
								if (nbrType != 57 && nbrType != 80)
									continue;

								// Loop over the neighbours of the neighbours.
								// Count the number of Nitrogens of type 55,
								// 56 and 81 which have not been marked as
								// conjugated yet.
								for (int k=0; k<mol.getAllConnAtoms(nbr); k++) {
									int nbr2 = mol.getConnAtom(nbr, k);
									int nbr2type = mol.getMMFFAtomType(nbr2);

									if (nbr2type != 55 && nbr2type != 56
											&& nbr2type != 81)
										continue;

									if (conj[nbr2])
										continue;

									conj[nbr2] = true;
									charges[atom] += mol.getAtomCharge(nbr2);
									nConj++;
								}
							}
						}
					}

					charges[atom] /= nConj;
					continue overatoms;

				case 61:
					for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
						int nbr = mol.getConnAtom(atom, i);

						// If it is diazonium, set a +1 formal charge on the
						// secondary Nitrogen.
						if (mol.getMMFFAtomType(nbr) == 42) {
							charges[atom] = 1.0;
							continue overatoms;
						}
					}
					continue overatoms;

				case 34:
					// NR+ (Quaternary Nitrogen)
				case 49:
					// O+ (Oxonium Oxygen)
				case 51:
					// O=+ (Oxenium Oxygen)
				case 54:
					// N+=C (Iminium Nitrogen)
					// N+=N (Positively charged Nitrogen doubly bonded to N)
				case 58:
					// NPD+ (Aromatic Nitrogen in pyridinium)
				case 92:
					// Li+ (Lithium cation)
				case 93:
					// Na+ (Sodium cation)
				case 94:
					// K+ (Potassium cation)
				case 97:
					// Cu+1 (Monopositive Copper cation)
					charges[atom] = 1.0;
					continue overatoms;

				case 87:
					// Fe+2 (Dipositive Iron cation)
				case 95:
					// Zn+2 (Dipositive Zinc cation)
				case 96:
					// Ca+2 (Dipositive Calcium cation)
				case 98:
					// Cu+2 (Dipositive Copper cation)
				case 99:
					// Mg+2 (Dipositive magnesium cation)
					charges[atom] = 2.0;
					continue overatoms;

				case 88:
					// Fe+3 (Tripositive Iron cation)
					charges[atom] = 3.0;
					continue overatoms;

				case 35:
					// OM  (Oxide Oxygen on sp3 Carbon)
					// OM2 (Oxide Oxygen on sp2 Carbon)
					// OM  (Oxide Oxygen on sp3 Nitrogen)
					// OM2 (Oxide Oxygen on sp2 Nitrogen)
				case 62:
					// NM (Anionic divalent Nitrogen)
				case 89:
					// F- (Fluoride anion)
				case 90:
					// Cl- (Chloride anion)
				case 91:
					// Br- (Bromide anion)
					charges[atom] = -1.0;
					continue overatoms;

				}
				charges[atom] = 0.0;
			}
		return charges;
	}

	/**
	 * Computes the partial MMFF charges.
	 *  @param mol The molecule to work on.
	 *  @return An array of partial charges, indexed by the atom indices
	 *      from the molecule 'mol'.
	 */
	public static double[] getCharges(Tables table, FFMolecule mol) {
		double[] fcharges = getFormalCharges(mol);
		double[] charges = new double[fcharges.length];

		// Loop over all the atoms in the molecule.
		for (int atom=0; atom<fcharges.length; atom++) {
			int type = mol.getMMFFAtomType(atom);
			double q0 = fcharges[atom];
			double v = table.chge.getFcadj(type);
			int M = table.atom.crd(type);

			double sumFormal = 0.0;
			double sumPartial = 0.0;

			if (Math.abs(v) < 0.0001)
				for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
					int nbr = mol.getConnAtom(atom, i);

					// If the neighbours have a negative formal charge, the
					// latter infulences the charge on ipso.
					if (fcharges[nbr] < 0.0)
						q0 += fcharges[nbr] / (2.0 * mol.getAllConnAtoms(nbr));
				}

			// There is a special case for anionic divalent Nitrogen with a
			// positively charged neighbour.
			if (type == 62)
				for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
					int nbr = mol.getConnAtom(atom, i);
					if (fcharges[nbr] > 0.0)
						q0 -= fcharges[nbr] / 2.0;
				}

			// Loop over the neighbours of 'atom'.
			for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
				int nbr = mol.getConnAtom(atom, i);
				int bond = mol.getBond(atom, nbr);
				int nbrType = mol.getMMFFAtomType(nbr);
				int bondType = Bond.getType(table, mol, bond);

				sumPartial += table.chge.getPartial(bondType, type, nbrType);
				sumFormal += fcharges[nbr];
			}

			charges[atom] = (1.0 - M*v)*q0 + v*sumFormal + sumPartial;
		}
		return charges;
	}


	public static int degree(FFMolecule mol, int atom) {
		return mol.getAllConnAtoms(atom) + mol.getImplicitHydrogens(atom);
	}
}
