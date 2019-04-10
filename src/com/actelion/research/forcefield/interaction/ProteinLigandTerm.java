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
package com.actelion.research.forcefield.interaction;

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFConfig;

/**
 * ProteinLigandTerm is used to represent the energy between 2 atoms.
 * 
 * The function can be divider into 3 parts:
 * - Linear part for the very short distances <1A (to avoid extreme derivates)
 * - Statistics derived potential when the data is enough to have a dependable function  
 * - Lennard-Jones 8-4 VDW when stats are not available
 * 
 */
public class ProteinLigandTerm extends AbstractTerm {

	//Taper to the null function close to cutoff distance
	private final static double CUTOFF = PLFunctionSplineCalculator.CUTOFF_STATS - PLFunctionSplineCalculator.DELTA_RADIUS;	
	
	public double rik2;
	private double energy;
	private double factor;

	//Statistics
	private final PLFunction f; 
	
	private ProteinLigandTerm(FFMolecule mol, int[] atoms, PLFunction f, double factor) {
		super(mol, atoms);
		this.f = f;			
		this.factor = factor;
	}
	
	public static ProteinLigandTerm create(FFMolecule mol, int a1, int a2, FFConfig config) {		
		PLFunction f = config.getClassStatistics().getFunction(mol.getAtomInteractionClass(a1), mol.getAtomInteractionClass(a2));
		if(f==null) return null;
		return new ProteinLigandTerm(mol, new int[]{a1, a2}, f, config.getProteinLigandFactor());			
	}
	
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		final Coordinates ci = getMolecule().getCoordinates(atoms[0]);		
		final Coordinates ck = getMolecule().getCoordinates(atoms[1]);				
		final Coordinates cr = ci.subC(ck);
		rik2 = cr.distSq();		

		if(rik2>CUTOFF*CUTOFF) {
			energy = 0; 
		} else {
			double de=0;
			double rik = Math.sqrt(rik2);

			double grad[] = f.getFGValue(rik);			
			
			energy = factor * grad[0];	 
			if(gradient!=null) de = factor * grad[1];				


			if(gradient!=null) {
			
				double deddt = (rik<=1? -10 : de) / rik;
				cr.scale(deddt);
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(cr);
				if(atoms[1]<gradient.length) gradient[atoms[1]].sub(cr);
			}					
		}
		
		return energy;

	}
	
	@Override
	public String toString() {
		return "PL-Term  "+atoms[0] +" - "+atoms[1]+" "+new DecimalFormat("0.000").format(Math.sqrt(rik2))+" -> "+new DecimalFormat("0.0000").format(energy);
	}

	@Override
	public boolean isExtraMolecular() {
		return true;
	}
	

}
