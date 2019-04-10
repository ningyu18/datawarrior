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
import com.actelion.research.forcefield.FastMath;

/**
 * Stretch bending energy term class. This energy term represents the
 * bond stretching-angle bending energy associated with three bonded
 * atoms A1--A2--A3 with an angle at A2.
 */
public class StretchBend extends AbstractTerm {
    public final int a1;
    public final int a2; // Central atom.
    public final int a3;

    public final double theta0;
    public final double kba_ijk;
    public final double kba_kji;
    public final double r0i;
    public final double r0k;

    /**
     * Constructs a new stretch bend object. Note that it only represents
     * the stretch bend energy in the IJK direction. A separate stretch
     * bend object should be created to represent the KJI direction with
     * a1 and a3 swapped.
     *  @param mol The molecule containing the atoms.
     *  @param a1 Atom 1 (atom i).
     *  @param a2 Atom 2, the central atom (atom j).
     *  @param a3 Atom 3 (atom k).
     */
    public StretchBend(Tables table, FFMolecule mol, int a1, int a2, int a3) {
    	super(mol, new int[] {a1, a2, a3});
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;

        theta0 = table.angle.theta(mol, a1, a2, a3);
        r0i = table.bond.r0(mol, a1, a2);
        r0k = table.bond.r0(mol, a3, a2);
        kba_ijk = table.stbn.kba(mol, a1, a2, a3);
        kba_kji = table.stbn.kba(mol, a3, a2, a1);
    }

    
    @Override
    public double getFGValue(Coordinates[] gradient) {
    	Coordinates cc1 = mol.getCoordinates(a1);
    	Coordinates cc2 = mol.getCoordinates(a2);
    	Coordinates cc3 = mol.getCoordinates(a3);
    	
    	Coordinates p12 = cc1.subC(cc2);
    	Coordinates p32 = cc3.subC(cc2);
    	
    	
    	double dist1 = p12.dist();
        double dist2 = p32.dist();
        double theta = p12.getAngle(p32);
    	
        double factor = Constants.MDYNE_A_TO_KCAL_MOL * Constants.DEG2RAD
            * (Math.toDegrees(theta) - theta0);

        double e = factor*(dist1 - r0i)*kba_ijk + factor*(dist2 - r0k)*kba_kji;

        
	    if(gradient!=null && dist1>0 && dist2>0) {
	    	p12.scale(1.0/dist1);
	    	p32.scale(1.0/dist2);
	
	        final double c5 = Constants.MDYNE_A_TO_KCAL_MOL * Constants.DEG2RAD;
	        double cosTheta = p12.dot(p32);
	        double sinThetaSq = 1.0 - cosTheta*cosTheta;
	        double sinTheta = Math.max(sinThetaSq > 0.0
	                ? FastMath.sqrt(sinThetaSq) : 0.0, 1.0e-8);
	        double angleTerm = Constants.RAD2DEG * FastMath.acos(cosTheta) - theta0;
	        double distTerm = Constants.RAD2DEG
	                * (kba_ijk * (dist1 - r0i)
	                +  kba_kji * (dist2 - r0k));
	
	        double dCos_dS1 = 1.0 / dist1 * (p32.x - cosTheta * p12.x);
	        double dCos_dS2 = 1.0 / dist1 * (p32.y - cosTheta * p12.y);
	        double dCos_dS3 = 1.0 / dist1 * (p32.z - cosTheta * p12.z);
	
	        double dCos_dS4 = 1.0 / dist2 * (p12.x - cosTheta * p32.x);
	        double dCos_dS5 = 1.0 / dist2 * (p12.y - cosTheta * p32.y);
	        double dCos_dS6 = 1.0 / dist2 * (p12.z - cosTheta * p32.z);
	
	        gradient[a1].x += c5 * (p12.x * kba_ijk
	            * angleTerm + dCos_dS1 / (-sinTheta) * distTerm);
	        gradient[a1].y += c5 * (p12.y * kba_ijk
	            * angleTerm + dCos_dS2 / (-sinTheta) * distTerm);
	        gradient[a1].z += c5 * (p12.z * kba_ijk
	            * angleTerm + dCos_dS3 / (-sinTheta) * distTerm);
	
	        gradient[a2].x += c5 * ((-p12.x * kba_ijk
	            - p32.x * kba_kji) * angleTerm
	            + (-dCos_dS1 - dCos_dS4) / (-sinTheta) * distTerm);
	        gradient[a2].y += c5 * ((-p12.y * kba_ijk
	            - p32.y * kba_kji) * angleTerm
	            + (-dCos_dS2 - dCos_dS5) / (-sinTheta) * distTerm);
	        gradient[a2].z += c5 * ((-p12.z * kba_ijk
	            - p32.z * kba_kji) * angleTerm
	            + (-dCos_dS3 - dCos_dS6) / (-sinTheta) * distTerm);
	
	        gradient[a3].x += c5 * (p32.x * kba_kji
	            * angleTerm + dCos_dS4 / (-sinTheta) * distTerm);
	        gradient[a3].y += c5 * (p32.y * kba_kji
	            * angleTerm + dCos_dS5 / (-sinTheta) * distTerm);
	        gradient[a3].z += c5 * (p32.z * kba_kji
	            * angleTerm + dCos_dS6 / (-sinTheta) * distTerm);
	    }
	    return e;
    	
    }
    
    
    /**
     * Checks if a Stretch Bend is valid
     */
    public static boolean valid(Tables table, FFMolecule mol, int a1,
            int a2, int a3) {
        int a2t = mol.getMMFFAtomType(a2);

        // Fail if the central atom is linear or the two neighbours are the
        // same atom.
        if (table.atom.linear(a2t) || a1 == a3)
            return false;

        // Fail if the three atoms do not form an angle.
        if (mol.getBond(a1, a2) == -1 || mol.getBond(a2, a3) == -1)
            return false;

        return true;
    }


    /**
     * Helper function that builds a list of StretchBends for a molecule.
     *  @param t The tables object.
     *  @param mol The molecule to generate angles for.
     *  @return An array of AngleBends.
     */
    public static List<StretchBend> findIn(Tables t, FFMolecule mol) {
        ArrayList<StretchBend> stbn = new ArrayList<StretchBend>();

        for (int atom=0; atom<mol.getAllAtoms(); atom++) {
            int atomt = mol.getMMFFAtomType(atom);

            if (mol.getAllConnAtoms(atom) <= 1 && t.atom.linear(atomt))
                continue;

            for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
                int nbr_i = mol.getConnAtom(atom, i);
                for (int k=0; k<mol.getAllConnAtoms(atom); k++) {
                    int nbr_k = mol.getConnAtom(atom, k);

                    // Only add half of the bends.
                    if (nbr_i > nbr_k) continue;
                    
                    if(nbr_i<mol.getNMovables() || nbr_k<mol.getNMovables() || atom<mol.getNMovables()) {
	
	                    if (StretchBend.valid(t, mol, nbr_i, atom, nbr_k)) {
	                        StretchBend bend = new StretchBend(t, mol, nbr_i, atom, nbr_k);
	
	                        // Don't bother adding any stretch bends whose kba
	                        // would be 0.
	                        if (Math.abs(bend.kba_ijk) > 0.0001
	                                || Math.abs(bend.kba_kji) > 0.0001) {
	                            stbn.add(bend);
	                        }
	                    }
                    }
                }
            }
        }

        return stbn;
    }
    
	@Override
	public final boolean isBonded() {
		return false;
	}

}
