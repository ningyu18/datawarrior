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
 * Bond stretching energy term class. This energy term represents the
 * bond stretching energy associated with two bonded atoms A1--A2.
 */
public class BondStretch extends AbstractTerm {
    public final int a1;
    public final int a2;
    public final double kb; // Force constant.
    public final double r0; // Ideal bond length.

    
    /**
     * Creates a new bond stretch given a force field and two bonded
     * atoms.
     *  @param table The tables parameter object.
     *  @param mol The molecule.
     *  @param a1 Atom 1 index.
     *  @param a2 Atom 2 index.
     */
    public BondStretch(Tables table, FFMolecule mol, int a1, int a2) {
    	super(mol, new int[] {a1, a2});
        this.a1 = a1;
        this.a2 = a2;

        r0 = table.bond.r0(mol, a1, a2);
        kb = table.bond.kb(mol, a1, a2);
    }

    @Override
    public double getFGValue(Coordinates[] gradient) {
    	Coordinates cc1 = mol.getCoordinates(a1);
    	Coordinates cc2 = mol.getCoordinates(a2);
    	
    	Coordinates v = cc1.subC(cc2);
    	final double c1 = Constants.MDYNE_A_TO_KCAL_MOL; //143.9325;
        final double cs = -2.0;
        final double c3 = 7.0 / 12.0;
        final double dist = v.dist();
        final double diff = (dist - r0)*(dist - r0);
        double e = (0.5*c1*kb*diff * (1.0 + cs*(dist - r0) + c3*cs*cs*diff));
	    if(gradient!=null) {
	
	        double distTerm = dist - r0;
	        double dE_dr = c1*kb*distTerm * (1.0 + 1.5*cs*distTerm + 2.0*c3*cs*cs*distTerm*distTerm);
	
	        if (dist > 0.0) {
	        	v.scale(dE_dr/dist);
	        	gradient[a1].add(v);
	        	gradient[a2].sub(v);

	        }
	    }
	    return e;
    }

    /**
     * Finds all bond stretch energy terms in the current molecule.
     *  @param t The tables parameter object.
     *  @param mol The molecule.
     *  @return The bond stretch energy terms for this molecule.
     */
    public static List<BondStretch> findIn(Tables t, FFMolecule mol) {
        List<BondStretch> bstretches = new ArrayList<BondStretch>();

        for (int a1=0; a1<mol.getNMovables(); a1++)
        	for(int i=0; i<mol.getAllConnAtoms(a1); i++) {
        		int a2 = mol.getConnAtom(a1, i);
        		if(a2>a1) {
        			bstretches.add(new BondStretch(t, mol, a1, a2));
        		}
        	}
        return bstretches;
    }
    
	@Override
	public final boolean isBonded() {
		return false;
	}

}
