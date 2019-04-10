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

package mmff;

import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.RingCollection;

/**
 * MMFF molecule is a wrapper class for the ExtendedMolecule. It holds some
 * additional data such as a cache of the atom types, whether the molecule is
 * valid for MMFF and the ring mmff aromaticity property.
 */
public final class Molecule {
    protected ExtendedMolecule mol;

    private RingBoolean[] ring_arom;
    private int[] atom_types;

    public Molecule(ExtendedMolecule mol) throws BadAtomTypeException,
                                                 BadRingAromException
    {
        this.mol = mol;
        mol.ensureHelperArrays(ExtendedMolecule.cHelperRings);

        RingCollection rings = mol.getRingSet();
        ring_arom = new RingBoolean[rings.getSize()];
        for (int i=0; i<ring_arom.length; i++)
            ring_arom[i] = RingBoolean.NOT_SET;

        boolean allset = false, changed = true;
        while (!allset && changed) {
            allset = true;
            changed = false;
            for (int r=0; r<rings.getSize(); r++) {
                if (ring_arom[r] == RingBoolean.NOT_SET) {
                    ring_arom[r] = mmff.type.Atom.ringIsMMFFAromatic(this, r);

                    if (ring_arom[r] != RingBoolean.NOT_SET)
                        changed = true;
                }

                if (ring_arom[r] == RingBoolean.NOT_SET)
                    allset = false;
            }
        }

        if (!allset)
            throw new BadRingAromException();

        // Assign the atom types to the atom type cache.
        atom_types = new int[mol.getAllAtoms()];
        for (int i=0; i<atom_types.length; i++) {
            atom_types[i] = -1;
            atom_types[i] = mmff.type.Atom.getType(this, i);

            if (atom_types[i] == 0)
                throw new BadAtomTypeException("Couldn't assign an atom type "
                        +"to atom "+i+" ("+getAtomLabel(i)+")");
        }
    }

    /**
     * Get the MMFF atom type of an atom. This returns the cached value.
     *  @param a The atom index in the molecule.
     *  @return The MMFF atom type.
     */
    public int getAtomType(int a) {
        return atom_types[a];
    }

    /**
     * Determine if a ring is aromatic according to MMFF criteria. Only
     * designed to work with rings of size 5 and 6. Returns the cached value.
     *  @param r The ring index in the molecule.
     *  @return True if the ring is aromatic, false otherwise.
     */
    public boolean ringIsMMFFAromatic(int r) {
        return ring_arom[r] == RingBoolean.TRUE ? true : false;
    }

    /**
     * Returns true if the given ring has had its MMFF aromaticity flag set.
     *  @param r The ring index in the molecule.
     *  @return True if the ring has had its flag set, false otherwise.
     */
    public boolean isSetRingMMFFAromaticity(int r) {
        return ring_arom[r] == RingBoolean.NOT_SET ? false : true;
    }

    /* Wrapper functions that map directly to the underlying ExtendedMolecule.
     *----------------------------------------------------------------------*/
    public int            getAllAtoms()               { return mol.getAllAtoms(); }
    public int            getAllBonds()               { return mol.getAllBonds(); }
    public int            getAllConnAtoms(int a)      { return mol.getAllConnAtoms(a); }
    public int            getAtomCharge(int a)        { return mol.getAtomCharge(a); }
    public String         getAtomLabel(int a)         { return mol.getAtomLabel(a); }
    public double         getAtomX(int a)             { return mol.getAtomX(a); }
    public double         getAtomY(int a)             { return mol.getAtomY(a); }
    public double         getAtomZ(int a)             { return mol.getAtomZ(a); }
    public int            getAtomRingSize(int a)      { return mol.getAtomRingSize(a); }
    public int            getAtomicNo(int a)          { return mol.getAtomicNo(a); }
    public int            getBond(int a1, int a2)     { return mol.getBond(a1, a2); }
    public int            getBondAtom(int i, int b)   { return mol.getBondAtom(i, b); }
    public int            getBondOrder(int b)         { return mol.getBondOrder(b); }
    public int            getConnAtom(int a, int i)   { return mol.getConnAtom(a, i); }
    public int            getConnAtoms(int a)         { return mol.getConnAtoms(a); }
    public int            getImplicitHydrogens(int a) { return mol.getImplicitHydrogens(a); }
    public String         getName()                   { return mol.getName(); }
    public int            getOccupiedValence(int a)   { return mol.getOccupiedValence(a); }
    public RingCollection getRingSet()                { return mol.getRingSet(); }
    public boolean        isAromaticAtom(int a)       { return mol.isAromaticAtom(a); }
    public boolean        isAromaticBond(int b)       { return mol.isAromaticBond(b); }
    public boolean        isRingAtom(int a)           { return mol.isRingAtom(a); }
    public void           setAtomX(int a, double x)   {        mol.setAtomX(a, x); }
    public void           setAtomY(int a, double y)   {        mol.setAtomY(a, y); }
    public void           setAtomZ(int a, double z)   {        mol.setAtomZ(a, z); }
    public void           setAtomX(int a, float  x)   {        mol.setAtomX(a, x); }
    public void           setAtomY(int a, float  y)   {        mol.setAtomY(a, y); }
    public void           setAtomZ(int a, float  z)   {        mol.setAtomZ(a, z); }
}
