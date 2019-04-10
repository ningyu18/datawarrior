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

/**
 * AbstractEvaluable is the interface used to describe a function in the system.
 * It has a state (defined by a multivariate) and can return its value and gradient
 * 
 * @author freyssj
 */
public abstract class AbstractEvaluable implements Cloneable {
	
	/**
	 * Sets the current state
	 * @param var
	 */
	public abstract void setState(double[] var);
	
	/**
	 * Gets the current state
	 * @return
	 */
	public abstract double[] getState();
	
	/**
	 * Gets the current state using the given vector (no new creation)
	 * @return
	 */
	public abstract double[] getState(double[] populate);
	
	/**
	 * Returns the energy and the gradient (if not null) at the current state 
	 * @param grad - null to calculate the energy only, or it will be populated with the gradient
	 * @return
	 */
	public abstract double getFGValue(double[] gradient);
	
	@Override
	public abstract AbstractEvaluable clone();
	
}
