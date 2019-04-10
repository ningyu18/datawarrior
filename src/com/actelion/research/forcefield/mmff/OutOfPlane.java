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
 * Out of plane energy term class. This energy term represents the
 * out of plane energy associated with four atoms:
 *
 *             A1
 *            /
 *      A2--AC
 *            \
 *             A3
 *
 * Where AC is the central atom and A1, A2 and A3 are each connected to
 * AC.
 */
public class OutOfPlane extends AbstractTerm {
    int ac;
    int a1;
    int a2;
    int a3;
    double koop;

    /**
     * Construct a new out of plane energy term.
     *  @param table The tables parameter object.
     *  @param mol The molecule.
     *  @param a1 Index of atom 1 in mol.
     *  @param a2 Index of atom 2 (the central atom) in mol.
     *  @param a3 Index of atom 3 in mol.
     */
    public OutOfPlane(Tables table, FFMolecule mol, int ac, int a1, int a2, int a3) {
    	super(mol, new int[] {ac, a1, a2, a3});
        this.ac = ac; // j
        this.a1 = a1; // i
        this.a2 = a2; // k
        this.a3 = a3; // l

        koop = table.oop.getKoop(mol, ac, a1, a2, a3);
    }


    @Override
    public double getFGValue(Coordinates[] gradient) {
    	
    	Coordinates cc1 = mol.getCoordinates(a1);
    	Coordinates cc2 = mol.getCoordinates(a2);
    	Coordinates cc3 = mol.getCoordinates(a3);
    	Coordinates ccc = mol.getCoordinates(ac);
    	
    	Coordinates rji = cc1.subC(ccc); 
    	Coordinates rjk = cc2.subC(ccc); 
    	Coordinates rjl = cc3.subC(ccc);
    	Coordinates n = rji.cross(rjk);
    	
    	if(n.distSq()==0) {
    		return 0;
    	}
    	double d = n.dist();
    	if(d>0) n.scale(1/d);
    	


        double chi = Constants.RAD2DEG * FastMath.asin(n.dot(rjl.unitC()));
        double c2 = Constants.MDYNE_A_TO_KCAL_MOL * Constants.DEG2RAD
            * Constants.DEG2RAD;
        double e = 0.5 * c2 * koop * chi * chi;
        
	    if(gradient!=null) {

	        final double dji = rji.dist();
	        final double djk = rjk.dist();
	        final double djl = rjl.dist();
		        
	        if(dji>0 && djk>0 && djl>0) {
		        rji.scale(1/dji);
		        rjk.scale(1/djk);
		        rjl.scale(1/djl);
		        
		        n.negate();
		
		
		        double sinChi = rjl.dot(n);
		        double cosChiSq = 1.0 - sinChi*sinChi;
		        double cosChi = Math.max(cosChiSq > 0.0
		                ? FastMath.sqrt(cosChiSq) : 0.0, 1.0e-8);
		        chi = Constants.RAD2DEG * FastMath.asin(sinChi);
		        double cosTheta = rji.dot(rjk);
		        double sinThetaSq = Math.max(1.0 - cosTheta * cosTheta, 1.0e-8);
		        double sinTheta = Math.max(sinThetaSq > 0.0
		                ? FastMath.sqrt(sinThetaSq) : 0.0, 1.0e-8);
		        double dE_dChi = Constants.RAD2DEG * c2 * koop * chi;
		
		        Coordinates t1 = rjl.cross(rjk);
		        Coordinates t2 = rji.cross(rjl);
		        Coordinates t3 = rjk.cross(rji);
		
		        double term1 = cosChi * sinTheta;
		        double term2 = sinChi / (cosChi * sinThetaSq);
		
		        Coordinates tg1 = new Coordinates(
		            (t1.x/term1 - (rji.x - rjk.x*cosTheta) * term2) / dji,
		            (t1.y/term1 - (rji.y - rjk.y*cosTheta) * term2) / dji,
		            (t1.z/term1 - (rji.z - rjk.z*cosTheta) * term2) / dji);
		        Coordinates tg2 = new Coordinates(
		            (t2.x/term1 - (rjk.x - rji.x*cosTheta) * term2) / djk,
		            (t2.y/term1 - (rjk.y - rji.y*cosTheta) * term2) / djk,
		            (t2.z/term1 - (rjk.z - rji.z*cosTheta) * term2) / djk);
		        Coordinates tg3 = new Coordinates(
		            (t3.x/term1 - rjl.x*sinChi/cosChi) / djl,
		            (t3.y/term1 - rjl.y*sinChi/cosChi) / djl,
		            (t3.z/term1 - rjl.z*sinChi/cosChi) / djl);
		        tg1.scale(dE_dChi);
		        tg2.scale(dE_dChi);
		        tg3.scale(dE_dChi);
		        
		        Coordinates sum = tg1.addC(tg2);
		        sum.add(tg3);
		        sum.scale(-1);
	
		        gradient[ac].add(sum);
		        gradient[a1].add(tg1);
		        gradient[a2].add(tg2);
		        gradient[a3].add(tg3);
	        }
	        
	    }
	    return e;
    }

    /**
     * Finds all out of plane angles in the current molecule.
     *  @param t The tables parameter object.
     *  @param mol The molecule to search in.
     *  @return An array of OutOfPlane angles.
     */
    public static List<OutOfPlane> findIn(Tables t, FFMolecule mol) {
        ArrayList<OutOfPlane> oops = new ArrayList<OutOfPlane>();

        for (int ac=0; ac<mol.getAllAtoms(); ac++) {
            if (mol.getAllConnAtoms(ac) != 3) continue;

            int a1 = mol.getConnAtom(ac, 0);
            int a2 = mol.getConnAtom(ac, 1);
            int a3 = mol.getConnAtom(ac, 2);
            if(a1<mol.getNMovables() || a2<mol.getNMovables() || a3<mol.getNMovables() || ac<mol.getNMovables()) {
	            oops.add(new OutOfPlane(t, mol, ac, a1, a2, a3));
	            oops.add(new OutOfPlane(t, mol, ac, a1, a3, a2));
	            oops.add(new OutOfPlane(t, mol, ac, a2, a3, a1));
            }
        }

        return oops;
    }
    
	@Override
	public final boolean isBonded() {
		return false;
	}


}
