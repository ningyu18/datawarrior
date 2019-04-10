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
 * Feb 14, 2017 MvK: Start implementation
 */
public class ModelMedianLong {
	
	public long lowerQuartile;
	
	public long median;
	
	public long upperQuartile;
	
	public int id;
	
	public int size;
	
	public long range(){
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
