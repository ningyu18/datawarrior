package com.actelion.research.calc.linearregression.pls;

import com.actelion.research.calc.Matrix;
import com.actelion.research.util.datamodel.ModelXY;

/**
 * RegressionModelCalculatorOptimumFactors
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Aug 14, 2015 MvK Start implementation
 */
public class RegressionModelCalculatorOptimumFactors {

	private static final double FRACTION_LEAVE_OUT = 0.25;
	
	private static final int LMO_REPETITIONS = 7;
	
	
	private boolean centerData;
	
	private Matrix B;
		
	private Matrix YHat;
	
	private int factorsMin;
	
	/**
	 * 
	 */
	public RegressionModelCalculatorOptimumFactors() {
		centerData = false;
	}
	
	
	
	/**
	 * @param centerData the centerData to set
	 */
	public void setCenterData(boolean centerData) {
		this.centerData = centerData;
	}


	/**
	 * Calculates the PLS regression model for the given data set. 
	 * A Leave Multiple Out estimator is used to assess the optimum number of factors.
	 * The calculation starts with factorsStart factor up to factorsEnd.
	 * @param dataXYTrain
	 * @param factorsStart
	 * @param factorsEnd
	 * @return
	 */
	public ModelError calculateModel(ModelXY dataXYTrain, int factorsStart, int factorsEnd){
		
		
		SimPLSLMOValidation simPLSLMOValidation = new SimPLSLMOValidation(dataXYTrain.X, dataXYTrain.Y);
				
		simPLSLMOValidation.setFractionLeaveOut(FRACTION_LEAVE_OUT);
		
		simPLSLMOValidation.setNumRepetitions(LMO_REPETITIONS);
		
		double errTestMin = Integer.MAX_VALUE;
		
		factorsMin = 1;
		
		for (int i = factorsStart; i < factorsEnd+1; i++) {
			
			simPLSLMOValidation.setNumFactors(i);
			
			double errTest = simPLSLMOValidation.calculateMedianTestError();
			
			if(errTest < errTestMin){
				errTestMin = errTest;
				factorsMin = i;
			}
			
		}
		
		System.out.println("Optimum number of factors " + factorsMin);
		
		RegressionModelCalculator rmc = new RegressionModelCalculator();
		
		rmc.setCenterData(centerData);
		
		ModelError modelerror = rmc.calculateModel(dataXYTrain, factorsMin);

		B = rmc.getB();
		
		YHat = rmc.getYHat();
		       
        return modelerror;
	}

	/**
	 * @return the b
	 */
	public Matrix getB() {
		return B;
	}


	/**
	 * @return the yHat
	 */
	public Matrix getYHat() {
		return YHat;
	}



	/**
	 * @return the factorsMin
	 */
	public int getFactorsMin() {
		return factorsMin;
	}

	
	
}
