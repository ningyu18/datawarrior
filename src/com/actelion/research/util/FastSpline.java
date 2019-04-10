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
 * @author freyssj
 */
package com.actelion.research.util;

import java.util.*;

/**
 * Represents a polynomial spline function.
 */
public final class FastSpline {
   
	public final static class Polynome  {
		private final double coeffs[]; //x0 x1 x2 x3 
		
		public Polynome(double[] coeffs) {
			this.coeffs = coeffs;			
		}
		
		public final Polynome derivative() {
			return new Polynome(new double[] {coeffs[1], 2*coeffs[2], 3*coeffs[3], 0});
		}

		public final double value(double x) {
			return coeffs[0]+x*(coeffs[1]+x*(coeffs[2]+x*coeffs[3]));			
		}
		public final double[] getCoefficients() {
			return coeffs;
		}
	}
    
	
    /** Spline segment interval delimiters (knots).   Size is n+1 for n segments. */
    private final double knots[];

    /**
     * The polynomial functions that make up the spline.  The first element
     * determines the value of the spline over the first subinterval, the
     * second over the second, etc.   Spline function values are determined by
     * evaluating these functions at <code>(x - knot[i])</code> where i is the
     * knot segment to which x belongs.
     */
    private final Polynome polynomials[];
    
    /** 
     * Number of spline segments = number of polynomials
     *  = number of partition points - 1 
     */
    private final int n;
    

    /**
     * Construct a polynomial spline function with the given segment delimiters
     * and interpolating polynomials.
     */
    public FastSpline(double knots[], Polynome polynomials[]) {
        this.n = knots.length -1;
        this.knots = new double[n + 1];
        this.polynomials = new Polynome[n];
        
        System.arraycopy(knots, 0, this.knots, 0, n + 1);
        System.arraycopy(polynomials, 0, this.polynomials, 0, n);
    }

    /**
     * Compute the value for the function.
     */
    public final double value(double v) {    	
        int i = Arrays.binarySearch(knots, v);
        if (i < 0) i = -i - 2;
        if (i < 0) i = 0; 
        return polynomials[i].value(v - knots[i]);
    }
        
    /**
     * Returns the derivative of the polynomial spline function as a PolynomialSplineFunction
     */
    public final FastSpline derivative() {
    	Polynome derivativePolynomials[] = new Polynome[n];
        for (int i = 0; i < n; i++) derivativePolynomials[i] = polynomials[i].derivative();
        return new FastSpline(knots, derivativePolynomials);
    }

}
