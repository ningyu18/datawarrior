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

import com.actelion.research.forcefield.FastMath;


/**
 * AbstractEvaluableOptimizer is used to describe any optimization done on
 * a IEvaluable class
 * 
 */
public abstract class AbstractOptimizer {

	protected double minRMS = 0.001;
	protected int maxIterations = 2000;
	protected int maxTime = 10000;
	protected double RMS;
	protected int iteration;
	protected double energy;

	/**
	 * Function used to optimize the forcefield
	 * @return the final energy
	 */
	public abstract double optimize(AbstractEvaluable eval);

	/**
	 * @return
	 */
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * @return
	 */
	public double getMinRMS() {
		return minRMS;
	}

	/**
	 * @return
	 */
	public double getRMS() {
		return RMS;
	}

	/**
	 * @param d
	 */
	public void setMaxIterations(int d) {
		maxIterations = d;
	}

	/**
	 * @param d
	 */
	public void setMinRMS(double d) {
		minRMS = d;
	}

	/**
	 * @return
	 */
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * @param i
	 */
	public void setMaxTime(int i) {
		maxTime = i;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public double getEnergy() {
		return energy;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}

	public void setRMS(double rMS) {
		RMS = rMS;
	}

	// ------------- Tools ---------------------
	public final static double getNormSq(double[] vector) {
		double res = 0;
		for (int i = 0; i < vector.length; i++) res += vector[i] * vector[i];
		return res;
	}

	public final static double getNorm(double[] vector) {return FastMath.sqrt(getNormSq(vector));}
	public final static double getRMS(double[] vector) {return FastMath.sqrt(getNormSq(vector) / vector.length);}	

	

}