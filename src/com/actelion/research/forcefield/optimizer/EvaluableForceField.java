/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Joel Freyss
 */
package com.actelion.research.forcefield.optimizer;

import com.actelion.research.chem.*;
import com.actelion.research.forcefield.*;


/**
 * Transformation used to optimize the 3D cartesian coordinates of the ligand:
 * (degrees of freedom=3*nAtoms)
 * @author freyssj
 */
public class EvaluableForceField extends AbstractEvaluable {
	protected ForceField forcefield;
	
	public EvaluableForceField(EvaluableForceField e) {
		this.forcefield = e.forcefield;		
	}
	public EvaluableForceField(ForceField forcefield) {
		this.forcefield = forcefield;
	}
	
	@Override
	public void setState(double[] v) {
		for(int i=0; i<v.length; i++) {
			assert !Double.isNaN(v[i]);
		}
		FFMolecule mol = forcefield.getMolecule();
		for(int i=0, a = 0; i<v.length; i+=3, a++) {			
			if(!mol.isAtomFlag(a, FFMolecule.RIGID)) {
				mol.setAtomX(a, v[i]);
				mol.setAtomY(a, v[i+1]);
				mol.setAtomZ(a, v[i+2]);
			}
		}
	}

	@Override
	public double[] getState() {
		FFMolecule mol = forcefield.getMolecule();
		return getState(new double[mol.getNMovables()*3]);
	}
	
	@Override
	public double[] getState(double[] v) {
		FFMolecule mol = forcefield.getMolecule();
		for(int i=0, a = 0; a<mol.getNMovables(); a++) {
			Coordinates c = mol.getCoordinates(a);
			v[i++] = c.x;
			v[i++] = c.y;
			v[i++] = c.z;
		}
		return v;
	}
	
	
	private Coordinates[] g = null; 
	
	@Override
	public double getFGValue(double[] grad) {
		FFMolecule mol = forcefield.getMolecule();
		
		if(grad==null) {
			return forcefield.getTerms().getFGValue(null);
		} else {
			//Compute the Gradient in the cartesian referential, ie vector of coordinates
			if(g==null || g.length!=mol.getNMovables()) {
				g = new Coordinates[mol.getNMovables()];
			}
			double e = forcefield.getTerms().getFGValue(g);
			for(int i=0, a = 0; i<grad.length; a++) {
				grad[i++] = g[a].x;
				grad[i++] = g[a].y;
				grad[i++] = g[a].z;
			}
			
			
			return e;		
		}
	}
	
	/**
	 * @see com.actelion.research.forcefield.optimizer.AbstractEvaluable#clone()
	 */
	@Override
	public EvaluableForceField clone() {
		return new EvaluableForceField(this);
	}

		
}