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

package com.actelion.research.forcefield.mmff;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FastMath;

/**
 * Torsional Angle energy term class. This energy term represents the
 * energy associated with the torsional angle formed by four atoms
 * A1..A4:
 *
 *      A1
 *        \
 *         A2--A3
 *               \
 *                A4
 *
 */
public class TorsionAngle extends AbstractTerm {
    public final int a1;
    public final int a2;
    public final int a3;
    public final int a4;

    public final double v1;
    public final double v2;
    public final double v3;

    /**
     * Construct a new torsion angle energy term.
     *  @param table The tables parameter object.
     *  @param mol The molecule.
     *  @param a1 Index of atom 1 in mol.
     *  @param a2 Index of atom 2 in mol.
     *  @param a3 Index of atom 3 in mol.
     *  @param a4 Index of atom 4 in mol.
     */
    public TorsionAngle(Tables table, FFMolecule mol, int a1, int a2, int a3, int a4) {
    	super(mol, new int[] {a1, a2, a3, a4});

        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.a4 = a4;

        com.actelion.research.forcefield.mmff.table.Torsion.Kb kbs = table.torsion.getForceConstants(mol, a1, a2, a3, a4);
        v1 = kbs.v1;
        v2 = kbs.v2;
        v3 = kbs.v3;
    }

    
    @Override
    public double getFGValue(Coordinates[] gradient) {
    	Coordinates cc1 = mol.getCoordinates(a1);
    	Coordinates cc2 = mol.getCoordinates(a2);
    	Coordinates cc3 = mol.getCoordinates(a3);
    	Coordinates cc4 = mol.getCoordinates(a4);
    	
    	Coordinates r1 = cc2.subC(cc1);
    	Coordinates r2 = cc2.subC(cc3);
    	Coordinates r3 = cc3.subC(cc2);
    	Coordinates r4 = cc3.subC(cc4);

    	Coordinates t1 = r1.cross(r2);
    	Coordinates t2 = r3.cross(r4);
        double cosPhi = t1.cosAngle(t2);

        double cos2Phi = 2.0 * cosPhi * cosPhi - 1.0;
        double cos3Phi = cosPhi * (2.0 * cos2Phi - 1.0);

        double e = 0.5 * (v1*(1.0 + cosPhi)
                    + v2*(1.0 - cos2Phi)
                    + v3*(1.0 + cos3Phi));

	    if(gradient!=null) {
	    	r1.negate();
	    	r2.negate();
	    	r3.negate();
	    	r4.negate();
	    	
	    	double d1 = t1.dist();
	    	double d2 = t2.dist();
	
//	        if (d1 > 0.00001 || d2 < 0.00001) {
	        if (d1 > 0.00001 && d2 > 0.00001) {
		
	        	t1.unit();
	        	t2.unit();
		
		        cosPhi = t1.dot(t2);
		        double sinPhiSq = 1.0 - cosPhi * cosPhi;
		        double sinPhi = ((sinPhiSq > 0.0) ? FastMath.sqrt(sinPhiSq) : 0.0);
		        double sin2Phi = 2.0 * sinPhi * cosPhi;
		        double sin3Phi = 3.0 * sinPhi - 4.0 * sinPhi * sinPhiSq;
		        double dE_dPhi = 0.5 * (-(v1) * sinPhi + 2.0 * v2 * sin2Phi
		                - 3.0 * v3 * sin3Phi);
		        double sinTerm = -dE_dPhi * (Math.abs(sinPhi) < 0.00001
		                ? (1.0 / cosPhi) : (1.0 / sinPhi));
		
		        double[] dCos_dT = new double[]{
		            1.0 / d1 * (t2.x - cosPhi * t1.x),
		            1.0 / d1 * (t2.y - cosPhi * t1.y),
		            1.0 / d1 * (t2.z - cosPhi * t1.z),
		            1.0 / d2 * (t1.x - cosPhi * t2.x),
		            1.0 / d2 * (t1.y - cosPhi * t2.y),
		            1.0 / d2 * (t1.z - cosPhi * t2.z)
		        };
		
		        gradient[a1].x += sinTerm * (dCos_dT[2] * r2.y - dCos_dT[1] * r2.z);
		        gradient[a1].y += sinTerm * (dCos_dT[0] * r2.z - dCos_dT[2] * r2.x);
		        gradient[a1].z += sinTerm * (dCos_dT[1] * r2.x - dCos_dT[0] * r2.y);
		

		        gradient[a2].x += sinTerm * (dCos_dT[1] * (r2.z - r1.z)
		                + dCos_dT[2] * (r1.y - r2.y)
		                + dCos_dT[4] * (-r4.z)
		                + dCos_dT[5] * (r4.y));
		        gradient[a2].y += sinTerm * (dCos_dT[0] * (r1.z - r2.z)
		                + dCos_dT[2] * (r2.x - r1.x)
		                + dCos_dT[3] * (r4.z)
		                + dCos_dT[5] * (-r4.x));
		        gradient[a2].z += sinTerm * (dCos_dT[0] * (r2.y - r1.y)
		                + dCos_dT[1] * (r1.x - r2.x)
		                + dCos_dT[3] * (-r4.y)
		                + dCos_dT[4] * (r4.x));
		
		        gradient[a3].x += sinTerm * (dCos_dT[1] * (r1.z)
		                + dCos_dT[2] * (-r1.y)
		                + dCos_dT[4] * (r4.z - r3.z)
		                + dCos_dT[5] * (r3.y - r4.y));
		        gradient[a3].y += sinTerm * (dCos_dT[0] * (-r1.z)
		                + dCos_dT[2] * (r1.x)
		                + dCos_dT[3] * (r3.z - r4.z)
		                + dCos_dT[5] * (r4.x - r3.x));
		        gradient[a3].z += sinTerm * (dCos_dT[0] * (r1.y)
		                + dCos_dT[1] * (-r1.x)
		                + dCos_dT[3] * (r4.y - r3.y)
		                + dCos_dT[4] * (r3.x - r4.x));
		
		        gradient[a4].x += sinTerm * (dCos_dT[4] * r3.z - dCos_dT[5] * r3.y);
		        gradient[a4].y += sinTerm * (dCos_dT[5] * r3.x - dCos_dT[3] * r3.z);
		        gradient[a4].z += sinTerm * (dCos_dT[3] * r3.y - dCos_dT[4] * r3.x);
	        }
	    }
	    return e;
    }
    /**
     * Checks that at least one of the constants is non-zero.
     *  @return True if any constant is non-zero, false otherwise.
     */
    public boolean nonZero() {
        return Math.abs(v1) > 0.001
            || Math.abs(v2) > 0.001
            || Math.abs(v3) > 0.001;
    }

    /**
     * Helper function that builds a list of TorsionAngles for a molecule.
     *  @param t The tables object.
     *  @param mol The molecule to generate torsions for.
     *  @return Am array of TorsionAngle.
     */
    public static List<TorsionAngle> findIn(Tables t, FFMolecule mol) {
        ArrayList<TorsionAngle> tors = new ArrayList<TorsionAngle>();

        for (int a1=0; a1<mol.getAllAtoms(); a1++) {
            for (int j=0; j<mol.getAllConnAtoms(a1); j++) {
                int a2 = mol.getConnAtom(a1, j);
                for (int k=0; k<mol.getAllConnAtoms(a2); k++) {
                    int a3 = mol.getConnAtom(a2, k);

                    if (a1 == a3)
                        continue;

                    for (int l=0; l<mol.getAllConnAtoms(a3); l++) {
                        int a4 = mol.getConnAtom(a3, l);

                        if (a2 == a4 || a1 == a4)
                            continue;

                        if(a1<mol.getNMovables() || a2<mol.getNMovables() || a3<mol.getNMovables() || a4<mol.getNMovables()) {

	                        if (a4 > a1) {
	                            TorsionAngle tor = new TorsionAngle(t, mol, a1, a2, a3, a4);
	
	                            if (tor.nonZero())
	                                tors.add(tor);
	                        }
                        }
                    }
                }
            }
        }

        return tors;
    }
    
	@Override
	public final boolean isBonded() {
		return false;
	}

}
