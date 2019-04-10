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
 * @author Paolo Tosco, Daniel Bergmann, Joel Freyss
 */

package com.actelion.research.forcefield.mmff;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;

/**
 * Nonbonded electrostatic energy term class. This energy term represents
 * the energy from the electrostatic attraction/repulsion between any two
 * atoms A1..A2 which are in a 1,X relationship (X > 3; interactions where
 * X = 4 are scaled by 0.75). A cutoff (default: 100.0 angstrom) can be
 * set to skip computation of electrostatic interactions between atoms
 * separated by distances larger than the cutoff.
 * A constant or distance-dependent dielectric model can be used (default:
 * constant).
 */
public class Electrostatic extends AbstractTerm {

	public final int a1;
	public final int a2;
	public final Separation.Relation rel;
	public final double charge_term;
	public final boolean distModel;

	/**
	 * Construct a new electrostatic energy term.
	 *  @param mol The molecule.
	 *  @param a1 Index of atom 1 in mol.
	 *  @param a2 Index of atom 2 in mol.
	 *  @param rel The 1,X relationship between the two atoms (e.g., 1,2;
	 *      1,3; 1,4; 1,X).
	 *  @param chge1 The charge of atom 1.
	 *  @param chge2 The charge of atom 2.
	 *  @param distModel The distance model to use, true for "distance"
	 *      and false for "constant".
	 *  @param dielConst The dielectric constant.
	 */
	public Electrostatic(FFMolecule mol, int a1, int a2, Separation.Relation rel, double chge1, double chge2, boolean distModel, double dielConst) {
		super(mol, new int[] {a1, a2});

		this.a1 = a1;
		this.a2 = a2;
		this.rel = rel;
		this.distModel = distModel;
		charge_term = chge1 * chge2 / dielConst;
	}

	@Override
	public double getFGValue(Coordinates[] gradient) {
		Coordinates cc1 = mol.getCoordinates(a1);
		Coordinates cc2 = mol.getCoordinates(a2);
		Coordinates v = cc1.subC(cc2);

		double dist = v.dist();
		double corr_dist = dist + 0.05;
		double diel = 332.0716;

		if (distModel) corr_dist *= corr_dist;

		double e = diel * charge_term / corr_dist * (rel == Separation.Relation.ONE_FOUR ? 0.75 : 1.0);


		if(gradient!=null) {
			corr_dist = dist + 0.05;
			corr_dist *= (distModel ? corr_dist * corr_dist : corr_dist);

			double dE_dr = -diel * (distModel ? 2.0 : 1.0)
					* charge_term / corr_dist
					* (rel == Separation.Relation.ONE_FOUR ? 0.75 : 1.0);

			if (dist > 0.0) {
				v.scale(dE_dr/dist);
				if(a1<gradient.length) gradient[a1].add(v);
				if(a2<gradient.length) gradient[a2].sub(v);
			} else {
				Coordinates v2 = new Coordinates(1,1,1);
				v2.scale(0.02);
				if(a1<gradient.length) gradient[a1].add(v2);
				if(a2<gradient.length) gradient[a2].sub(v2);
			}
		}
		return e;
	}

	/**
	 * Finds all Electrostatic energy terms in the current molecule.
	 *  @param table The parameter tables to use.
	 *  @param mol The molecule to search for Electrostatic forces.
	 *  @param sep The separations table for molecule mol.
	 *  @param nonbondedCutoff The nonbonded energy terms cutoff distance.
	 *      Electrostatic energy terms between atoms further than this are
	 *      not added.
	 *  @param dielModel The dielectric model to use. True for 'distance'
	 *      and False for 'linear'.
	 *  @param dielConst The dielectric constant.
	 *  @return The Electrostatic energy terms for this molecule.
	 */
	public static List<Electrostatic> findIn(Tables table,
			FFMolecule mol, Separation sep, double nonbondedCutoff,
			boolean dielModel, double dielConst) {
		ArrayList<Electrostatic> eles = new ArrayList<Electrostatic>();
		double[] charges = com.actelion.research.forcefield.mmff.type.Charge.getCharges(table, mol);
		for (int i=0; i<mol.getNMovables(); i++) {
			for (int j=0; j<i; j++) {

				Separation.Relation relation = sep.get(i, j);

				if ((relation == Separation.Relation.ONE_FOUR
						|| relation == Separation.Relation.ONE_X)
						&& Math.abs(charges[i]) > 0.00001
						&& Math.abs(charges[j]) > 0.00001) {
					if (mol.getCoordinates(i).distance(mol.getCoordinates(j)) < nonbondedCutoff) {
						eles.add(new Electrostatic(mol, i, j, relation, charges[i], charges[j], dielModel, dielConst));
					}
				}
			}
		}

		return eles;
	}
}
