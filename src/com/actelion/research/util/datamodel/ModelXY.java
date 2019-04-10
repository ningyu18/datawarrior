
package com.actelion.research.util.datamodel;

import com.actelion.research.calc.Matrix;


/**
 * 
 * 
 * ModelDataXY
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Aug 4, 2011 MvK: Start implementation
 */
public class ModelXY {
		
	public Matrix X;
	
	public Matrix Y;
		
	public ModelXY() {
		
	}

	
	
	
	/**
	 * @param x
	 * @param y
	 */
	public ModelXY(Matrix x, Matrix y) {
		super();
		X = x;
		Y = y;
	}




	/**
	 * Deep copy constructor
	 * @param dataXY
	 */
	public ModelXY(ModelXY dataXY) {
		
		X = new Matrix(dataXY.X.rows(), dataXY.X.cols());
		
		Y = new Matrix(dataXY.Y.rows(), dataXY.Y.cols());
				
		X.copy(dataXY.X);
		
		Y.copy(dataXY.Y);
				
	}
	
	/**
	 * 
	 * @return rows in X.
	 */
	public int size(){
		return X.rows();
	}
	
	
	
}