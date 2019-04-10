package com.actelion.research.chem.descriptor.flexophore.generator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.calc.MatrixFunctions;
import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.descriptor.flexophore.MolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistViz;
import com.actelion.research.chem.descriptor.flexophore.PPNodeViz;

/**
 * 
 * CGMult
 * Object containing pharmacophore nodes and a list of distance tables. 
 * The distnace tables represent the distances from the different conformations.
 * 
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2004 MvK: Start implementation
 * 02.03.2016 MvK: updates. the percentage of the distance distributions are calculated from the distance histograms.
 * This percentage is taken for the MolDistHist.
 * 29.06.2016 number of bins increase from 40 to 50, histogram range increased from 20 to 25 Angstroem.
 * 11.08.2016 number of bins increase from 50 to 80, histogram range increased from 25 to 40 Angstroem.
 * 29.05.2017 Added +0.5 in the percentage calculation of the histogram standartization.
 */
public class CGMult {
	
	public static final String SEPERATOR_NO_ATOMS = ";";
	private static final DecimalFormat FORMAT_DISTANES = new DecimalFormat("0.00000");

	/**
	 * Defines the resolution for the range.
	 */
	public static final int BINS_HISTOGRAM = 80;

	/**
	 * Range histogram in Angstrom.
	 */
	public static final int RANGE_HISTOGRAM = 40;

	private static final int MAX_ALLOWED_DIFF_COUNT_DIST_HIST = 5;

	
	private List<PPNodeViz> liPPNode;
	
	private List<double[][]> liDistTable;   
	
	private FFMolecule ffMol;

	/**
	 * A deep copy from FFMolecule is taken.
	 * @param cg the nodes are taken.
	 * @param ff FFMolecule for visualization.
	 */
	public CGMult(CompleteGraph cg, FFMolecule ff) {
		init();
		
		for (int i = 0; i < cg.getAllNodes(); i++) {
			liPPNode.add(new PPNodeViz(cg.getPPNode(i)));	
		}
		
		liDistTable  = new ArrayList<double[][]>();

		liDistTable.add(cg.getEdges());
		
		ffMol = new FFMolecule(ff);
	}

	/**
	 * Checks for the same number of nodes and for the same atom types in each node.
	 * Only the distances (edges) are added to the CGMult instance. 
	 * @param cg complete graph with nodes in the same order as the nodes in CGMult.
	 * @throws Exception
	 */
	public void add(CompleteGraph cg) throws Exception {
		
		if(getAllNodes() != cg.getAllNodes()) {
			throw new Exception("Number of atoms differs: " + getAllNodes() + " and " + cg.getAllNodes() + ".");
		}
		
		for (int i = 0; i < getAllNodes(); i++) {
			if(!getPPNode(i).equalAtoms(cg.getPPNode(i))) {
				throw new Exception("Node type differs.");
			} 
			
		}
		
		liDistTable.add(cg.getEdges());
		
	}
	
	/**
	 * Generates the MolDistHist (Flexophore) descriptor from the conformations.
	 * The Alkane clusters are summarized.
	 * The index tables are not created!
	 * @return
	 * @deprecated
	 */
	public MolDistHist getMolDistHistWithSummarizeAlkaneClusters() {
		
		MolDistHistViz mdhv = getMolDistHistVizWithSummarizedAlkaneClusters();
		
		return mdhv.getMolDistHist();
	}

	public MolDistHist getMolDistHist() {

		MolDistHistViz mdhv = getMolDistHistViz();

		return mdhv.getMolDistHist();
	}

	/**
	 * the only difference to getMolDistHistWithSummarizeAlkaneClusters() is the usage of PPNodeViz for the MolDistHist object.
	 * In contrary to getMolDistHistVizFineGranulated() alkene clusters are summarised.
	 * @return
	 * @deprecated
	 */
	public MolDistHistViz getMolDistHistVizWithSummarizedAlkaneClusters() {
		
		MolDistHistViz mdhv = getMolDistHistVizFineGranulated();
		
		mdhv = MolDistHistViz.summarizeAlkaneCluster(mdhv, MolDistHist.MAX_DIST_CLUSTER);
		
		if(!mdhv.check()){
			return null;
		}
		
		return mdhv;
	}

	public MolDistHistViz getMolDistHistViz() {

		MolDistHistViz mdhv = getMolDistHistVizFineGranulated();

		if(!mdhv.check()){
			return null;
		}

		return mdhv;
	}

	/**
	 * The distance histograms contain the percentage of the the input histograms. So, they are independent from the
	 * number of conformations.
	 * @return
	 */
	public MolDistHistViz getMolDistHistVizFineGranulated() {

		final int numNodes = getAllNodes();

		MolDistHistViz mdhv = new MolDistHistViz(numNodes, ffMol);
		
		final double minRangeDistanceHistogram =0;

		final double maxRangeDistanceHistogram = RANGE_HISTOGRAM;

		final int bins = BINS_HISTOGRAM;
		
		for (int i = 0; i < numNodes; i++) {
			mdhv.addNode(getPPNode(i));
		}

		int countValuesInHistogram0 = 0;

		for (int indexNode1 = 0; indexNode1 < numNodes; indexNode1++) {

			for (int indexNode2 = indexNode1+1; indexNode2 < numNodes; indexNode2++) {

				Matrix maDist = new Matrix(1, liDistTable.size());

				int cc = 0;
				
				for (int i = 0; i < liDistTable.size(); i++) {
					double[][] arrDistTbl = liDistTable.get(i);

					double dist = arrDistTbl[indexNode1][indexNode2];

					if(dist >= maxRangeDistanceHistogram){

						throw new RuntimeException("Distance between two pppoints higher than histogram limit of " + maxRangeDistanceHistogram + " Angstroem.");

					}

					maDist.set(0, cc++, dist);
				}

				Matrix maBins = MatrixFunctions.getHistogramBins(minRangeDistanceHistogram,maxRangeDistanceHistogram, bins);

				Matrix maHist =  MatrixFunctions.getHistogram(maDist, maBins);

				double [] arrHist = maHist.getRow(2);

				int countValuesInHistogram = 0;

				for (int i = 0; i < arrHist.length; i++) {
					countValuesInHistogram += arrHist[i];
				}

				if(countValuesInHistogram0==0){

					countValuesInHistogram0 = countValuesInHistogram;

				} else if(Math.abs(countValuesInHistogram0 - countValuesInHistogram) > MAX_ALLOWED_DIFF_COUNT_DIST_HIST) {

					throw new RuntimeException("Flexophore distance histogram counts differ.");

				}

				// Here, the percentage values for the histograms are calculated.
				byte [] arrHistPercent = new byte [maHist.getColDim()];

				for (int i = 0; i < arrHist.length; i++) {

					arrHistPercent[i]= (byte)  (((arrHist[i] / countValuesInHistogram) * 100.0) + 0.5);
				}

				mdhv.setDistHist(indexNode1, indexNode2, arrHistPercent);
			}
		}
		
		mdhv.setDistanceTables(liDistTable);

		mdhv.realize();
		
		if(!mdhv.check()){
			return null;
		}
		
		return mdhv;
	}

	
	
	public int getNumConformations(){
		return liDistTable.size();
	}
	
	public final PPNodeViz getPPNode(int i) {
		return liPPNode.get(i);
	}
	public int getAllNodes(){
		return liPPNode.size();
	}
	
	
	private void init(){
		liPPNode = new ArrayList<PPNodeViz>();
		liDistTable = new ArrayList<double[][]>();
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer();

		str.append(getAllNodes() + SEPERATOR_NO_ATOMS);
		
		for (int i = 0; i < getAllNodes(); i++) 
			str.append(getPPNode(i).toString() + " "); 
		
		
		for (int i = 0; i < liDistTable.size(); i++)
			str.append(toStringDistances(i));
		
		return str.toString();
	}
	
	public String toStringDistances(int iConformation) {
		StringBuffer str = new StringBuffer();
		int nNodes = getAllNodes();
		double [][] edges = (double[][])liDistTable.get(iConformation);
		
		for (int i = 0; i < nNodes; i++) {
			for (int j = i + 1; j < nNodes; j++) {
				str.append(FORMAT_DISTANES.format(edges[i][j]) + " "); 
			}
		}
		return str.toString();
	}

	
}
