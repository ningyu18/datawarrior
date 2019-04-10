package com.actelion.research.forcefield.mmff;

import java.util.List;

import com.actelion.research.chem.FFMolecule;

public class MMFFParameters {
	
	public static void setAtomTypes(FFMolecule mol) {
		
		setAromRings(mol);
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)<=0) continue;
			mol.setMMFFAtomType(i, -1);
			int type = com.actelion.research.forcefield.mmff.type.Atom.getType(mol, i);
			mol.setMMFFAtomType(i, type);
			assert type>=0;
			
			if (!mol.isAtomFlag(i, FFMolecule.RIGID) && type <= 0) {
				throw new BadAtomTypeException("Couldn't assign an atom type to flexible atom " + i + "> atomicNo=" + mol.getAtomicNo(i)+" connected="+mol.getAllConnAtoms(i)+ " "+mol.getAtomicNo(mol.getConnAtom(i, 0))+ " set to "+type);
			}
		}
	}
	
	private static void setAromRings(FFMolecule mol) {
		List<int[]> rings = mol.getAllRings();
		Boolean[] ringAtoms = new Boolean[rings.size()];
        mol.setMmffRingAtoms(ringAtoms);

        boolean allset = false, changed = true;
        while (!allset && changed) {
            allset = true;
            changed = false;
            for (int r=0; r<rings.size(); r++) {
                if (ringAtoms[r] == null) {
                    ringAtoms[r] = ringIsMMFFAromatic(mol, r);
                    if (ringAtoms[r] != null) {
                    	changed = true;
                    }
                }

                if (ringAtoms[r] == null) {
                	allset = false;
                }
            }
        }
        assert allset;
	}
	
	 /**
     * Determine if a ring is aromatic according to MMFF criteria. Only
     * designed to work with rings of size 5 and 6.
     *  @param mol The molecule that the ring is in.
     *  @param r The ring.
     *  @return True if the ring is aromatic, false otherwise.
     */
    public static Boolean ringIsMMFFAromatic(FFMolecule mol, int r) {
        List<int[]> rings = mol.getAllRings();

        if (!mol.isAromaticRing(r)) return Boolean.FALSE;

        int[] ring = rings.get(r);
        int ringSize = ring.length;
        if (ringSize == 6) {
            for (int a : rings.get(r)) {
                if (mol.getOccupiedValence(a) + mol.getImplicitHydrogens(a)
                        != (degree(mol, a)+1))
                    return Boolean.FALSE;
            }
            for (int ai = 0; ai < ringSize; ai++) {
            	int b = mol.getBond(ring[ai], ring[(ai+1)%ringSize]);
                int [] c = { -1, -1 };
                if (mol.getBondOrder(b) == 1) {
                    for (int j = 0; j <= 1; j++) {
                        int atom = mol.getBondAtom(j, b);
                        for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
                            int nbr = mol.getConnAtom(atom, i);
                            if (!mol.isAtomMember(r, nbr)
                                    && mol.getBondOrder(mol.getBond(atom, nbr)) == 2) {
                                c[j] = nbr;
                                break;
                            }
                        }
                    }
                    if (c[0] > -1 && c[1] > -1) {
                        for (int ri = 0; ri < rings.size(); ri++) {
                            if (mol.isAtomMember(ri, c[0])
                                    && mol.isAtomMember(ri, c[1])
                                    && !mol.isSetRingMMFFAromaticity(ri))
                                return null;

                            if (mol.isAtomMember(ri, c[0])
                                    && mol.isAtomMember(ri, c[1])
                                    && !mol.ringIsMMFFAromatic(ri))
                                return Boolean.FALSE;
                        }
                    }
                }
            }
        }

        if (ringSize == 5) {
            int passes = 1;

            for (int a : ring) {
                if (mol.getOccupiedValence(a) + mol.getImplicitHydrogens(a)
                        == degree(mol, a) && passes > 0) {
                    passes--;
                    continue;
                }
                if (mol.getOccupiedValence(a) + mol.getImplicitHydrogens(a)
                        != (degree(mol, a)+1)) {
                    return Boolean.FALSE;
                }
            }
        }

        return Boolean.TRUE;
    }
    

    
    /**
     * Returns the total number of atoms connected to an atom, including
     * implicit and explicit hydrogens.
     *  @param mol The molecule that the atom is in.
     *  @param atom The atom to count the neighbours of.
     *  @return The number of neighbours connected to this atom.
     */
    public static int degree(FFMolecule mol, int atom) {
        return mol.getAllConnAtoms(atom) + mol.getImplicitHydrogens(atom);
    }

}
