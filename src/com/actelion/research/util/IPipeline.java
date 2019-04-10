package com.actelion.research.util;

/**
 * 
 * 
 * IPipeline
 * Interface to connect several Runable in a pipeline
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Mar 27, 2012 MvK: Start implementation
 * Oct 9 2012 MvK: wereAllDataFetched() added.
 */
public interface IPipeline<T> {
	
	public boolean isAllDataIn();
	
	/**
	 * Has to be true when all data were fetched.
	 * @return
	 */
	public boolean wereAllDataFetched();
	
	public void setAllDataIn(boolean allDataIn);
	
	
}
