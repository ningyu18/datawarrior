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


import java.util.Arrays;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.TermList;
import com.actelion.research.forcefield.transformation.ChainOfTransformations;
import com.actelion.research.util.ArrayUtils;


/**
 * Generic Transformation
 * 
 */
public abstract class AbstractEvaluableTransformation extends AbstractEvaluable {
	protected ForceField forcefield;
	protected ChainOfTransformations chain;
	protected Coordinates[] initial;
		
	public AbstractEvaluableTransformation(ForceField forcefield, ChainOfTransformations chain) {
		this(forcefield, chain, forcefield.getMolecule().getCoordinates());
	}
	
	public AbstractEvaluableTransformation(ForceField forcefield, ChainOfTransformations chain, Coordinates[] initial) {
		this.forcefield = forcefield;
		this.chain = chain;
		this.initial = initial;
	}
	
	protected AbstractEvaluableTransformation(ForceField forcefield) {
		this.forcefield = forcefield;
		this.initial = forcefield.getMolecule().getCoordinates();
	}
	
	@Override
	public void setState(double[] v) {
		chain.setMultivariate(v);
		chain.transform(forcefield.getMolecule());
	}
	
	@Override
	public double[] getState() {
		return chain.getMultivariate();
	}
	
	@Override
	public double[] getState(double[] populate) {
		return chain.getMultivariate(populate);
	}
	
	@Override
	public double getFGValue(double[] grad) {		
		FFMolecule mol = forcefield.getMolecule();
		
		//Compute the Gradient in the cartesian referential			
		Coordinates[] g = new Coordinates[mol.getNMovables()];
		for(int i=0; i<g.length; i++) g[i] = new Coordinates(); 
		double e = forcefield.getTerms().getFGValue(g);
	
		if(grad!=null) {
			//Intializes the gradient
			double[] v = grad;			
			Arrays.fill(v, 0);
			
			//Compute the Gradient in the new referential
			for(int var=0; var<v.length; var++) {
				Coordinates[] dX = chain.getDTransformation(var); 
				for(int i=0; i<g.length; i++) {
					v[var] += g[i].dot(dX[i]);	
				}
			}
		}
		
		return e;		
	}
	
		
	/**
	 * Clone the function but shares the forcefield and the initial values
	 * @see java.lang.Object#clone()
	 */
	@Override
	public abstract AbstractEvaluableTransformation clone();
	
	/**
	 * Hard clone (function, initial values, forcefield and molecule)
	 * @return
	 */
	public AbstractEvaluableTransformation hardClone() {
		AbstractEvaluableTransformation res = (AbstractEvaluableTransformation) clone();
		res.forcefield = forcefield.clone();
		res.initial = (Coordinates[]) ArrayUtils.copy(initial);
		return res;
	}
	
	public ChainOfTransformations getChain() {
		return chain;
	}
	
	public void setChain(ChainOfTransformations transformations) {
		chain = transformations;
	}
	
	/**
	 * @return
	 */
	public ForceField getForcefield() {
		return forcefield;
	}

	/**
	 * @return
	 */
	public Coordinates[] getInitial() {
		return initial;
	}
	
	/**
	 * Remove useless terms from the Forcefield, ie terms that are
	 * within the same groups of atoms
	 */
	public int removeUselessTerms() {
		int n = 0;
		int[] groups = chain.getGroups();
		TermList tl = forcefield.getTerms();
		loop: for (int i = 0; i < tl.size(); i++) {
			AbstractTerm t = tl.get(i);
			assert t!=null;
			int[] atoms = t.getAtoms();
			if(atoms.length<=1) continue loop;
			
			if(atoms[0]>=groups.length) {
				//System.out.println("Keep intermolecular term "+t);
				continue loop; //Intermolecular interaction
			}
			for (int j = 1; j < atoms.length; j++) {
				if(atoms[j]>=groups.length) {
					//System.out.println("Keep intermolecular term "+t);
					continue loop; //Intermolecular interaction
				}
				if(groups[atoms[j]]!=groups[atoms[0]]) {
					//System.out.println("Keep <>group term "+t);
					continue loop;
				}
			}
			//All the atoms of this term are in the same group
			//this term is therefore useless and me remove it
			tl.remove(i--);
			n++;
		}
		return n;
	}



}