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
 *
 * @author Joel Freyss
 */
package com.actelion.research.forcefield.optimizer;


/**
 * Adapted from the Tinker code (search.f file)
 *  
 */
class LineSearch {

	
	/**
	 * Minimize the function according to the line search algorithm. (ie find lamda so that newpos = pos+lambda*dir minimizes the energy)
	 * This function expects the initial FGValue, the direction of search and the initial move
	 * 
	 *  http://www.mcs.anl.gov/~anitescu/CLASSES/2012/LECTURES/S310-2012-lect4.pdf
	 *  
	 * @param f0
	 * @param function
	 * @param grad
	 * @param dir
	 * @param fMove
	 * @return
	 */
	public static final Object[] minimizeEnergyAroundDirection(final AbstractEvaluable function, double f0, final double[] grad, final double[] dir, final double fMove) {
		final double CAPPA = .9;
		final double STPMIN = 1e-6;
		final double STPMAX = .1;
		double fA=0, fB=0, fC=0;
		double slopeA=0, slopeB=0, slopeC=0;
		double cube = 0;
		
		//Compute length of Gradient
		final double len = grad.length;
		final double sNorm = AbstractOptimizer.getNorm(dir);
				
		//Normalize the search vector and find the projected gradient
		double slope = 0;
		for(int i=0; i<len; i++) {
			dir[i] /= sNorm;
			slope += dir[i]*grad[i];
		}
			
		double step = Math.min(2.0*Math.abs(fMove/slope), sNorm);
		if(step>STPMAX) step = STPMAX;
		else if(step<STPMIN) step = STPMIN;
		
		double lambda = 0;
		double[] initial = function.getState();
		double[] v = new double[initial.length];
		try {
			for(int reSearch=0; reSearch<2; reSearch++) {
				fB = f0;
				slopeB = slope;
	
				//Quadratic interpolation: f(r) = a.r^2 + b.r + c  ( a = (gA-gB)/(2A-2B), b = gA - (gA-gB)/(A-B)) 
				//
				//    A
				//     \          /B
				//       -       /
				//         -- --
				//
				//    0            lambda
				for(int counter = 0; counter<20; counter++) {
					fA = fB;
					slopeA = slopeB;
					
					//evaluate at lambda+step
					lambda += step;
					
					move(function, initial, dir, lambda, v);
					fB = function.getFGValue(grad);
					slopeB = 0; for(int i=0; i<len; i++) slopeB += dir[i]*grad[i];
					
					if(Math.abs(slopeB/slope)<=CAPPA && fB<=fA) { //success
						return new Object[]{fB, grad, Boolean.TRUE};
					}				
					
					//go to cubic interpolation if gradient changes sign or function increases
					if(fB>=fA || slopeB*slopeA<0) break;
					
					//Adapt step
					if(slopeB>slopeA) {
						double parab = (fA - fB) / (slopeB - slopeA);
						if(parab>2*step) step = 2 * step;
						else if(parab<2*step) step = step / 2;
						else step = parab;
					} else {
						step*=2;
					}
					if(step>STPMAX) step = STPMAX;
					else if(step<STPMIN) step = STPMIN;
				} // end-while

				fC = fB;
				//Cubic interpolation (http://www.mathworks.com/access/helpdesk_r13/help/toolbox/optim/tutori5b.html)
				for(int counter = 0; counter<5; counter++) {//Cubic interpolation
					double sss = 3*(fB-fA)/step - slopeA - slopeB;
		
					double ttt = sss*sss - slopeA*slopeB;
					if(ttt<0) return new Object[]{fB, grad, Boolean.FALSE}; //Interpolation error, stop here

					ttt = Math.sqrt(ttt);
					cube = step * (slopeB + ttt + sss) / (slopeB - slopeA + 2 * ttt);

//					if(cube<0 || cube>step) cube = step/2;
					if(cube<0 || cube>step) {
						return new Object[]{fC, grad, Boolean.FALSE}; //Interpolation error, stop here
					}

					
					lambda -= cube;	
					
					//Get new function and gradient
					move(function, initial, dir, lambda, v);
					fC = function.getFGValue(grad);
					slopeC = 0; for(int i=0; i<len; i++) slopeC += dir[i]*grad[i];
	
	
					if(Math.abs(slopeC/slope)<=CAPPA) {
						//Success
						return new Object[]{fC, grad, Boolean.TRUE};
					}
					if(fC<=fA || fC<=fA) {
						double cubStep = Math.min(Math.abs(cube), Math.abs(step-cube));
						if(cubStep>=STPMIN) {
							if( (slopeA*slopeB<0 && slopeA*slopeC<0) || (slopeA*slopeB>=0 && (slopeA*slopeC<0 || fA<fC)) ) {
								//C becomes the right limit -> B
								fB = fC;
								slopeB = slopeC;
								step -= cube;
							} else {
								//C becomes the left limit -> A
								fA = fC;
								slopeA = slopeC;
								step = cube;
								lambda += cube;					
							}
						} else {
	//						if((fC>fA && fC>fB) || cubStep<STPMIN) break;
							break;
						}
					}
				}  
				
				//Cubic Interpolation has failed, reset to best current point
				double fL, sgL;
				if(fA<=fB && fA<=fC) { //A is min
					fL = fA;
					sgL = slopeA;
					lambda += cube-step;					
				} else if(fB<=fA && fB<=fC) { //B is min
					fL = fB;
					sgL = slopeB;
					lambda += cube;					
				} else /*fC<=fA && fC<=fB*/ { //C is min
					fL = fC;
					sgL = slopeC;
				}

			
				//try to restart from best point with smaller stepsize
				if(fL>f0) {
					move(function, initial, dir, lambda, v);
					f0 = function.getFGValue(grad);
					System.err.println("ERR fL>f0");
					return new Object[]{f0, grad, Boolean.FALSE};
				}
				f0 = fL;
				if(sgL>0) {
					lambda = 0;
					for(int i=0; i<dir.length; i++) dir[i] = -dir[i];
					slope -= -sgL;
				}  else {
					slope = sgL;					
				}
				step = Math.max( STPMIN, Math.max(cube, step-cube) / 10);				
			}
			
			//Already restarted, return best current point					
			move(function, initial, dir, lambda, v);
			f0 = function.getFGValue(grad);
			return new Object[]{f0, grad, Boolean.FALSE};

		} catch(Exception e) {
			e.printStackTrace();
			function.setState(initial);
			f0 = function.getFGValue(grad);
			return new Object[]{new Double(f0), grad, Boolean.FALSE};				
		}
	}	

	private final static void move(AbstractEvaluable eval, double[] initial, double[] dir, double lambda, double[] v) {
		for (int i = 0; i < v.length; i++) {
			v[i] = initial[i] + lambda * dir[i]; 
		}
		eval.setState(v);
	}
	
}
