package com.actelion.research.calc.statistics.median;



/**
 * 
 * 
 * ModelMedianInteger
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Feb 9, 2012 MvK: Start implementation
 */
public class ModelMedianInteger {
	
	public int lowerQuartile;
	
	public int median;
	
	public int upperQuartile;
	
	public int id;
	
	public int size;
	
	public int range(){
		return upperQuartile-lowerQuartile;
	}
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder(); 
		
		sb.append(lowerQuartile);
		sb.append("\t");
		sb.append(median);
		sb.append("\t");
		sb.append(upperQuartile);
		
		return sb.toString();
	}

}
