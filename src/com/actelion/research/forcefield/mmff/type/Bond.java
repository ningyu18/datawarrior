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
 * The bond type class provides static functions for getting the MMFF bond
 * type (either by bond or by the atoms of a bond).
 */
public class Bond {
    /**
     * Returns the MMFF bond type of a bond on a molecule. This function
     * assumes that a valid bond is passed for the molecule.
     *  @param mol The molecule that contains the bond.
     *  @param bond The bond to be typed.
     *  @return The MMFF bond type.
     */
    public static int getType(Tables table, FFMolecule mol, int bond) {
        return getType(table, mol, mol.getBondAtom(0, bond), mol.getBondAtom(1, bond));
    }

    /**
     * Returns the MMFF bond type of a bond on a molecule. This function
     * assumes that both atoms passed are valid and have a valid bond
     * between them.
     *  @param mol The molecule that contains the bond.
     *  @param atom1 The first atom of the bond.
     *  @param atom2 The second atom of the bond.
     *  @return The MMFF bond type.
     */
    public static int getType(Tables table, FFMolecule mol, int atom1, int atom2) {
        int a1t = mol.getMMFFAtomType(atom1);
        int a2t = mol.getMMFFAtomType(atom2);
        int bond = mol.getBond(atom1, atom2);
        assert a1t>=0;
        assert a2t>=0;
        assert bond>=0;
        
        		
        boolean notInAromaticRing = true;

        List<int[]> rings = mol.getAllRings();
        for (int r=0; r<rings.size() && notInAromaticRing; r++) {
            for (int ai = 0; ai < rings.get(r).length; ai++) {
            	int b = mol.getBond(rings.get(r)[ai], rings.get(r)[(ai+1)%rings.get(r).length]);
                if (b == bond && mol.ringIsMMFFAromatic(r)) {
                    notInAromaticRing = false;
                    break;
                }
            }
        }

        return (mol.getBondOrder(bond) == 1)
            && notInAromaticRing
            && (table.atom.arom(a1t) && table.atom.arom(a2t)
            || table.atom.sbmb(a1t) && table.atom.sbmb(a2t)) ? 1 : 0;
    }
}
