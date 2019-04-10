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

import com.actelion.research.forcefield.*;
import com.actelion.research.forcefield.transformation.*;


/**
 * Transformation used to optimize the docking of the ligand inside a protein:
 * trans/rot (degreesOfFreedom = 6)
 * 
 */
public class EvaluableDockRigid extends AbstractEvaluableTransformation {

	private EvaluableDockRigid(EvaluableDockRigid e) {
		super(e.forcefield, (ChainOfTransformations) e.chain.clone(), e.initial);		
	}
	public EvaluableDockRigid(ForceField forcefield) {
		super(forcefield, new ChainOfTransformations(new AbstractTransform[]{new TransRotTransform(forcefield.getMolecule())}, forcefield.getMolecule()));
	}
	
	
	/**
	 * @see com.actelion.research.forcefield.optimizer.AbstractEvaluable#clone()
	 */
	@Override
	public EvaluableDockRigid clone() {
		return new EvaluableDockRigid(this);
	}

	
}