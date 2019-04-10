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
 * 
 */
public class OptimizerConjugateGradient extends AbstractOptimizer {
	@Override
	public double optimize(AbstractEvaluable eval) {
				
		long s = System.currentTimeMillis();		
		int errs = 0; 		
		int iter;
		double[] initial = eval.getState();
		int N = initial.length;
		double[] grad = new double[N];
		double[] gradprevious = new double[N];
		double[] d = new double[N];
		double[] dprevious = new double[N];
		double f = eval.getFGValue(grad);
		for(iter=0; iter<maxIterations && errs<3 && getRMS(grad)>minRMS && System.currentTimeMillis()-s<getMaxTime(); iter++) {
			
			System.arraycopy(grad, 0, gradprevious, 0, N);
			System.arraycopy(d, 0, dprevious, 0, N);

			for (int i = 0; i < grad.length; i++) d[i]=-grad[i];
			double normSq = getNormSq(grad);
			if(normSq==0) break;
			for(int i=0; i<grad.length; i++) {
				double beta = grad[i] * (grad[i]-gradprevious[i])/normSq;
				d[i] += dprevious[i]*beta;
			}					   				
			
			Object[] res = LineSearch.minimizeEnergyAroundDirection(eval, f, grad, d, FastMath.sqrt(normSq));
			if(res[2]==Boolean.FALSE) {
				errs++;
			}
			f = ((Double)res[0]).doubleValue();
			grad = (double[]) res[1]; 
			
		}	
		return f;
	}
	
}
