package com.actelion.research.calc.linearregression.pls;

import com.actelion.research.calc.Matrix;
import com.actelion.research.calc.MatrixFunctions;
import com.actelion.research.util.Formatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ModelError
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Aug 14, 2015 MvK Start implementation
 */
public class ModelError {

	// Average error
	public double error;
	
	public double errMax;
	
	public double errMin;
	
	public double corrSquared;
	
	/**
	 * 
	 */
	public ModelError() {

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModelError [error=");
		builder.append(Formatter.format3(error));
		builder.append(", errMax=");
		builder.append(Formatter.format3(errMax));
		builder.append(", errMin=");
		builder.append(Formatter.format3(errMin));
		builder.append(", corrSquared=");
		builder.append(Formatter.format3(corrSquared));
		builder.append("]");
		return builder.toString();
	}
	
	public static ModelError calculateError(Matrix Y, Matrix YHat){
		
    	ModelError modelError = new ModelError();
    	
    	modelError.error=0;
        
    	modelError.errMax = 0;
        
    	modelError.errMin = Integer.MAX_VALUE;
    	
        for (int i = 0; i < YHat.cols(); i++) {
        	
            for (int j = 0; j < YHat.rows(); j++) {
            	
            	double e = Y.get(j, i) - YHat.get(j, i);
            	
            	modelError.errMax = Math.max(modelError.errMax, e);
            	
            	modelError.errMin = Math.min(modelError.errMin, e);
            	
            	modelError.error += Math.abs(e);
    		}
            
		}
        
        modelError.error = modelError.error / (YHat.rows()*YHat.cols());
        
        double corr = MatrixFunctions.getCorrPearson(YHat, Y);
        
        modelError.corrSquared = corr*corr; 
        
        return modelError;

	}
	
	public static List<Double> getError(List<ModelError> liME){
		
		List<Double> li = new ArrayList<Double>();
		
		for (ModelError modelError : liME) {
			li.add(modelError.error);
		}
		
		
		return li;
	}
	
	public static Comparator<ModelError> getComparatorError(){
		
		return new Comparator<ModelError>() {
			
			@Override
			public int compare(ModelError o1, ModelError o2) {
				int cmp = 0;
				
				if(o1.error > o2.error){
					cmp=1;
				}else if(o1.error < o2.error){
					cmp=-1;
				}
							
				return cmp;
			}
		};
	}
	

}
