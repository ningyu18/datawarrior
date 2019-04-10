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
 * Angle bending energy term class. This energy term represents the
 * angle bending energy associated with three bonded atoms A1--A2--A3
 * with an angle at A2.
 */
public class AngleBend extends AbstractTerm {
    public final int a1;
    public final int a2; // Central atom.
    public final int a3;
    public final boolean isLinear;

    public final double ka;     // Force constant.
    public final double theta0; // Ideal angle.

    /**
     * Construct a new angle bend energy term.
     *  @param table The tables parameter object.
     *  @param mol The molecule.
     *  @param a1 Index of atom 1 in mol.
     *  @param a2 Index of atom 2 (the central atom) in mol.
     *  @param a3 Index of atom 3 in mol.
     */
    public AngleBend(Tables table, FFMolecule mol, int a1, int a2, int a3) {
    	super(mol, new int[] {a1,a2,a3});
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        isLinear = table.atom.linear(mol.getMMFFAtomType(a2));

        theta0 = table.angle.theta(mol, a1, a2, a3);
        ka = table.angle.ka(mol, a1, a2, a3);
    }

    @Override
    public double getFGValue(Coordinates[] gradient) {
    	
    	Coordinates cc1 = mol.getCoordinates(a1);
    	Coordinates cc2 = mol.getCoordinates(a2);
    	Coordinates cc3 = mol.getCoordinates(a3);
    	
    	Coordinates cc21 = cc1.subC(cc2);
    	Coordinates cc23 = cc3.subC(cc2);
    	double theta = cc21.getAngle(cc23);
    	
    	
        double angle = Math.toDegrees(theta) - theta0;

        final double cb = -0.006981317;
        final double c2 = Constants.MDYNE_A_TO_KCAL_MOL * Constants.DEG2RAD
            * Constants.DEG2RAD;

        
        double e;
        if (isLinear) { // isLinear is a property of the central atom and can be found in the prop table.
        	e = Constants.MDYNE_A_TO_KCAL_MOL*ka*(1.0 + FastMath.cos(theta));
        } else {
            e = 0.5*c2*ka*angle*angle*(1.0 + cb*angle);
        }
        
        if(gradient!=null) {
	        double dist0 = cc21.dist();
	        double dist1 = cc23.dist();
		    
	        if(dist0>0 && dist1>0) {
	        	cc21.scale(1/dist0);
	        	cc23.scale(1/dist1);
	
	        	double cosTheta = cc21.cosAngle(cc23);
		        
	
	
		        double sinThetaSq = 1.0 - cosTheta*cosTheta;
		        double sinTheta = 1.0e-8;
		        if (sinThetaSq > 0.0)
		            sinTheta = FastMath.sqrt(sinThetaSq);
		
		        double angleTerm = Constants.RAD2DEG * FastMath.acos(cosTheta) - theta0;
		
		        double dE_dTheta = Constants.RAD2DEG*c2*ka*angleTerm
		            * (1.0 + 1.5*cb*angleTerm);
		        if (isLinear)
		            dE_dTheta = -Constants.MDYNE_A_TO_KCAL_MOL * ka * sinTheta;
			        
		        
		        Coordinates dCos_dS1 = new Coordinates(
		        		1.0/dist0*(cc23.x - cosTheta*cc21.x),
			            1.0/dist0*(cc23.y - cosTheta*cc21.y),
			            1.0/dist0*(cc23.z - cosTheta*cc21.z));
		        Coordinates dCos_dS2 = new Coordinates(
		        		1.0/dist1*(cc21.x - cosTheta*cc23.x),
			            1.0/dist1*(cc21.y - cosTheta*cc23.y),
			            1.0/dist1*(cc21.z - cosTheta*cc23.z));
			        
		        
		        Coordinates v2 = dCos_dS1.addC(dCos_dS2);
		        v2.scale(dE_dTheta/sinTheta);
		        dCos_dS1.scale(-dE_dTheta/sinTheta);
		        dCos_dS2.scale(-dE_dTheta/sinTheta);
		        		        
		        gradient[a1].add(dCos_dS1);
		        gradient[a2].add(v2);
		        gradient[a3].add(dCos_dS2);
	        }
	    }
        
        return e;
    }
    

    /**
     * Helper function that builds a list of AngleBends for a molecule.
     *  @param t The tables object.
     *  @param mol The molecule to generate angles for.
     *  @return Am array of AngleBends.
     */
    public static List<AngleBend> findIn(Tables t, FFMolecule mol) {
        ArrayList<AngleBend> angles = new ArrayList<AngleBend>();

        for (int atom=0; atom<mol.getAllAtoms(); atom++) {
            if (mol.getAllConnAtoms(atom) > 1) {
                for (int i=0; i<mol.getAllConnAtoms(atom); i++) {
                    int nbr_i = mol.getConnAtom(atom, i);
                    for (int k=i+1; k<mol.getAllConnAtoms(atom); k++) {
                        int nbr_k = mol.getConnAtom(atom, k);
                        
                        if(nbr_i<mol.getNMovables() || atom<mol.getNMovables() || nbr_k<mol.getNMovables()) {                        
                        	angles.add(new AngleBend(t, mol, nbr_i, atom, nbr_k));
                        }
                    }
                }
            }
        }

        return angles;
    }
    
    public double getPreferredAngle() {
    	return theta0;
    }
    
	@Override
	public final boolean isBonded() {
		return false;
	}

}
