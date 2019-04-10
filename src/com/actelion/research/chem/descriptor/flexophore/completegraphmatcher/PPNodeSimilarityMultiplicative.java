package com.actelion.research.chem.descriptor.flexophore.completegraphmatcher;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.descriptor.flexophore.PPNode;
import com.actelion.research.forcefield.interaction.ClassInteractionTable;

/**
 * 
 * MultiplicativeNodeSimilarity
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jan 7, 2013 MvK Start implementation
 */
public class PPNodeSimilarityMultiplicative implements IPPNodeSimilarity {
	
	private static final int SIZE_SIM_MATRIX = 20;
	
	private static PPNodeSimilarityMultiplicative INSTANCE = null;
	
	private ClassInteractionTable classInteractionTable;

	private Matrix maSimilarity;
	
	/**
	 * This constructor is used for parallel mode.
	 */
	public PPNodeSimilarityMultiplicative(int versionInteractionTable){

		maSimilarity = new Matrix(SIZE_SIM_MATRIX, SIZE_SIM_MATRIX);

		if(classInteractionTable ==null) {
			synchronized(this) {
				classInteractionTable = ClassInteractionTable.getInstance(versionInteractionTable);
			}
		}
	}
	
	/**
	 * Use this as constructor for serial mode.
	 * @return
	 */
	public static PPNodeSimilarityMultiplicative getInstance(int versionInteractionTable){
		if(INSTANCE == null) {
			synchronized(PPNodeSimilarityMultiplicative.class) {
				INSTANCE = new PPNodeSimilarityMultiplicative(versionInteractionTable);
			}
		}
		return INSTANCE;
	}

	/**
	 * 
	 * @param query
	 * @param base
	 * @return
	 * @throws Exception
	 */
	public double getSimilarity(PPNode query, PPNode base) {
		
		maSimilarity.set(0);
		
		for (int i = 0; i < query.getInteractionTypeCount(); i++) {
			
			int interactionIdQuery = query.getInteractionId(i);
			
			for (int j = 0; j < base.getInteractionTypeCount(); j++) {

				int interactionIdBase = base.getInteractionId(j);

				double similarity = 1.0 - classInteractionTable.getDistance(interactionIdQuery, interactionIdBase);

				maSimilarity.set(i,j,similarity);
			}
		}
		
		double sim = 1.0;
		
		if(base.getInteractionTypeCount() > query.getInteractionTypeCount()) {
			for (int i = 0; i < base.getInteractionTypeCount(); i++) {
				sim *= maSimilarity.getMax(i);
			}
		} else {
			for (int i = 0; i < query.getInteractionTypeCount(); i++) {
				sim *= maSimilarity.getMaxRow(i);
			}
		}

		
		return sim;
	}

	
}
