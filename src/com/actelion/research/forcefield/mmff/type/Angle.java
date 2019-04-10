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

import java.util.HashSet;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.mmff.Tables;

/**
 * The angle type class provides static functions for getting angle and
 * stretch bend MMFF type.
 */
public final class Angle {
    /**
     * Checks if an angle is in a ring of given size. Will also check that
     * the atoms form an angle.
     *  @param mol The molecule that the angle is in.
     *  @param a1 Atom 1 of the angle.
     *  @param a2 Atom 2 of the angle.
     *  @param a3 Atom 3 of the angle.
     *  @param size The size of ring to check for.
     *  @return True if the angle is in the given size, false otherwise.
     */
    public static boolean inRingOfSize(FFMolecule mol, int a1, int a2,
            int a3, int size) {
        List<int[]> rings = mol.getAllRings();
        HashSet<Integer> angle = new HashSet<Integer>();
        angle.add(a1);
        angle.add(a2);
        angle.add(a3);

        if (mol.getBond(a1, a2) >= 0 && mol.getBond(a2, a3) >= 0)
            for (int r=0; r<rings.size(); r++)
                if (rings.get(r).length == size) {
                    HashSet<Integer> ring = new HashSet<Integer>();

                    for (int a : rings.get(r))
                        ring.add(a);

                    if (ring.containsAll(angle))
                        return true;
                }
        return false;
    }

    /**
     * Gets the angle type of an angle bend. The possible return types are
     * as follows:
     *      0: The angle i-j-k is a "normal" bond angle.
     *      1: Either bond i-j or bond j-k has a bond type of 1.
     *      2: Bonds i-j and j-k each have bond types of 1; The sum is 2.
     *      3: The angle occurs in a three member ring.
     *      4: The angle occurs in a four member ring.
     *      5: The angle is in a three member ring and the sum of the bond
     *          types is 1.
     *      6: The angle is in a three member ring and the sum of the bond
     *          types is 2.
     *      7: The angle is in a four member ring and the sum of the bond
     *          types is 1.
     *      8: The angle is in a four member ring and the sum of the bond
     *          types is 2.
     */
    public static int getType(Tables table, FFMolecule mol, int a1,
            int a2, int a3) {
        int bondsum = Bond.getType(table, mol, a1, a2)
                    + Bond.getType(table, mol, a2, a3);
        int type = bondsum;

        if (inRingOfSize(mol, a1, a2, a3, 3))
            type += bondsum > 0 ? 4 : 3;
        else if (inRingOfSize(mol, a1, a2, a3, 4))
            type += bondsum > 0 ? 6 : 4;

        return type;
    }

    /**
     * Given three atoms which form an angle, returns the MMFF
     * stretch-bend type of the angle.
     *  @param mol The molecule containing the atoms.
     *  @param a1 Atom 1 (atom i).
     *  @param a2 Atom 2, the central atom (atom j).
     *  @param a3 Atom 3 (atom k).
     *  @return The stretch-bend type.
     */
    public static int getStbnType(Tables table, FFMolecule mol, int a1,
            int a2, int a3) {

        int a1t = mol.getMMFFAtomType(a1);
        int a3t = mol.getMMFFAtomType(a3);

        int b1tf = Bond.getType(table, mol, a1, a2);
        int b2tf = Bond.getType(table, mol, a2, a3);

        int b1t = a1t <= a3t ? b1tf : b2tf;
        int b2t = a1t < a3t  ? b2tf : b1tf;

        int angt = getType(table, mol, a1, a2, a3);

        switch (angt) {
            case 1:
                return (b1t > 0 || b1t == b2t) ? 1 : 2;
            case 2:
                return 3;
            case 3:
                return 5;
            case 4:
                return 4;
            case 5:
                return (b1t > 0 || b1t == b2t) ? 6 : 7;
            case 6:
                return 8;
            case 7:
                return (b1t > 0 || b1t == b2t) ? 9 : 10;
            case 8:
                return 11;
        }
        return 0;
    }
}
