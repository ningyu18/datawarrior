package com.actelion.research.calc.linearregression.pls;

import com.actelion.research.calc.Matrix;
import com.actelion.research.util.datamodel.ModelXY;

/**
 * RegressionModelCalculator
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Aug 14, 2015 MvK Start implementation
 */
public class RegressionModelCalculator {

	
	private boolean centerData;
	
	private Matrix B;
	
	private Matrix Xvar;
	
	private Matrix YHat;
	
	private Matrix XtrainPreprocessed;
	
	private Matrix YtrainPreprocessed;
	
	/**
	 * 
	 */
	public RegressionModelCalculator() {
		centerData = false;
	}
	
	
	
	/**
	 * @param centerData the centerData to set
	 */
	public void setCenterData(boolean centerData) {
		this.centerData = centerData;
	}



	public ModelError calculateModel(ModelXY dataXYTrain, int factors){
		
		XtrainPreprocessed = dataXYTrain.X;
		
		YtrainPreprocessed = dataXYTrain.Y;

		
		if(centerData){
			
			XtrainPreprocessed = dataXYTrain.X.getCenteredMatrix();
			
			YtrainPreprocessed = dataXYTrain.Y.getCenteredMatrix();
			
			// System.out.println("Calculate PLS with centered data.");
			
		} else {
			// System.out.println("Calculate PLS with raw data.");
		}
		
		SimPLS simPLS = new SimPLS();
		
		simPLS.simPlsSave(XtrainPreprocessed, YtrainPreprocessed, factors);
		
		Matrix R = simPLS.getR();

		if(R.cols() == 1 && R.rows() == 1 && R.get(0,0)==0){
			System.out.println("RegressionModelCalculator R = 0.");
		}

		Matrix Q = simPLS.getQ();

		B = R.multiply(false, true, Q);
		
		Xvar = XtrainPreprocessed.getVarianceCols();
								
        YHat = SimPLS.InvLinReg_Yhat(B, XtrainPreprocessed, dataXYTrain.X, YtrainPreprocessed);
        
    	ModelError modelError = ModelError.calculateError(dataXYTrain.Y, YHat);
    	    	
        return modelError;
	}

	public Matrix calculateYHat(Matrix Xtest){
		
		Matrix YHatTest = SimPLS.InvLinReg_Yhat(B, XtrainPreprocessed, Xtest, YtrainPreprocessed);
		
		return YHatTest;
	}
	
	public ModelError calculateModelErrorTest(Matrix Xtest, Matrix Ytest){
		
		Matrix YHatTest = SimPLS.InvLinReg_Yhat(B, XtrainPreprocessed, Xtest, YtrainPreprocessed);
		
		return ModelError.calculateError(Ytest, YHatTest);
	}
	
	
	/**
	 * @return the b
	 */
	public Matrix getB() {
		return B;
	}



	/**
	 * @return the xvar
	 */
	public Matrix getXvar() {
		return Xvar;
	}



	/**
	 * @return the yHat
	 */
	public Matrix getYHat() {
		return YHat;
	}

	
	
}
