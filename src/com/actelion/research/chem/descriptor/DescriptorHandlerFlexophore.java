package com.actelion.research.chem.descriptor;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.flexophore.*;
import com.actelion.research.chem.descriptor.flexophore.completegraphmatcher.ObjectiveFlexophoreHardMatchUncovered;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;
import com.actelion.research.chem.descriptor.flexophore.generator.CreatorCompleteGraph;
import com.actelion.research.util.graph.complete.CompleteGraphMatcher;

/**
 * 
 * DescriptorHandlerFlexophore
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 4.2
 * 29 Jan 2009 MvK: Start implementation 
 * 15 Oct 2012 MvK renamed DescriptorHandler3DMM2PPInteract-->DescriptorHandlerFlexophore
 * 19 Apr 2013 MvK major changes in Flexophore encoding decoding
 * 25 Apr 2013 MvK Flexophore version changed --> 3.0
 * 07 May 2013 MvK bug fixes in encoding. Flexophore version changed --> 3.1
 * 15 May 2013 MvK Bug fix for the objective function
 * 03 May 2016 MvK versioning for interaction tables from Joel introduced.
 * 10 Jun 2016 MvK DescriptorHandlerFlexophoreV4 --> DescriptorHandlerFlexophore, V4 becomes today the new Flexophore
 * 18 Jun 2016 MvK New ConformationGenerator from TS for Flexophore creation. V 4.1
 * 29 Jun 2016 MvK number of histogram bins and range increased in CGMult. V 4.2
 * 15 Jul 2016 MvK if generation of conformer failed a new seed is injected and the generation is tried again.
 * 11 Aug 2016 MvK number of bins increase from 50 to 80, histogram range increased from 25 to 40 Angstroem. --> V.4.3
 * 30 Jan 2017 MvK minor bug fix. Two constants for the number of conformations. --> V.4.4. Compatible with V.4.3
 */
public class DescriptorHandlerFlexophore implements DescriptorHandler {

	public static final boolean DEBUG = false;

	private static final int MAX_NUM_HEAVY_ATOMS = 70;

	private static final double CORRECTION_FACTOR = 0.40;

	private static final int MAX_TRIES_TO_GENERATE_CONFORMER = 25;

	private static final int MAX_TRIES_TO_GENERATE_CONFORMER_ONE_CONF = 11;

	private static DescriptorHandlerFlexophore INSTANCE;

	private static final int MIN_NUM_ATOMS = 6;

	// 250
	public static final int NUM_CONFORMATIONS = 250;

	public static final int MAX_NUM_SOLUTIONS = 1000;

	public static final MolDistHist FAILED_OBJECT = new MolDistHist();

	// Version 3.0 after definition of new interaction types by Joel.
	// 07.05.2013 Version 3.1 after bug fixes in encoding.
	// 17.09.2015 Version 3.2. Joel re-calculated interaction tables. Differences in atom types.
	// 10.06.2016 Version 4.0. Total re-implementation of the Flexophore. The pharmacophore point recognition is now
	// more generic.
	private static final String VERSION = DescriptorConstants.DESCRIPTOR_Flexophore.version;

	public static final int VERSION_INTERACTION_TABLES = 2;

	private ConcurrentLinkedQueue<CompleteGraphMatcher<IMolDistHist>> queueCGM;


	private MolDistHistEncoder molDistHistEncoder;

	private CreatorCompleteGraph creatorCompleteGraph;

	//
	// If you change this, do not forget to change the objective in CompleteGraphMatcher<IMolDistHist> getNewCompleteGraphMatcher().
	//
	private ObjectiveFlexophoreHardMatchUncovered objectiveCompleteGraphHard;

	private Exception exceptionCreateDescriptor;


	public DescriptorHandlerFlexophore() {
		init();
	}

	private void init(){

		MolDistHistViz.createIndexTables();

		queueCGM = new ConcurrentLinkedQueue<CompleteGraphMatcher<IMolDistHist>>();

		queueCGM.add(getNewCompleteGraphMatcher());

		molDistHistEncoder = new MolDistHistEncoder();

		objectiveCompleteGraphHard = new ObjectiveFlexophoreHardMatchUncovered(VERSION_INTERACTION_TABLES);

		creatorCompleteGraph = new CreatorCompleteGraph();
	}


	private CompleteGraphMatcher<IMolDistHist> getNewCompleteGraphMatcher(){

		ObjectiveFlexophoreHardMatchUncovered objective = new ObjectiveFlexophoreHardMatchUncovered(VERSION_INTERACTION_TABLES);

		CompleteGraphMatcher<IMolDistHist> cgMatcher = new CompleteGraphMatcher<IMolDistHist>(objective);

		cgMatcher.setMaxNumSolutions(MAX_NUM_SOLUTIONS);

		return cgMatcher;
	}

	public DescriptorInfo getInfo() {
		return DescriptorConstants.DESCRIPTOR_Flexophore;
	}

	public String getVersion() {
		return VERSION;
	}

	public static DescriptorHandlerFlexophore getDefaultInstance(){

		if(INSTANCE==null){
			INSTANCE = new DescriptorHandlerFlexophore();
		}

		return INSTANCE;
	}

	public String encode(Object o) {

		if(calculationFailed(o)){
			return FAILED_STRING;
		}

		MolDistHist mdh = null;

		if(o instanceof MolDistHist){

			mdh = (MolDistHist)o;

		} else if(o instanceof MolDistHistViz){

			mdh = ((MolDistHistViz)o).getMolDistHist();

		} else {
			return FAILED_STRING;
		}

		return molDistHistEncoder.encode(mdh);

	}

	public MolDistHist decode(byte[] bytes) {
		try {
			return bytes == null || bytes.length == 0 ? null : Arrays.equals(bytes, FAILED_BYTES) ? FAILED_OBJECT : molDistHistEncoder.decode(bytes);
		} catch (RuntimeException e1) {
			return FAILED_OBJECT;
		}
	}

	public MolDistHist decode(String s) {
		try {
			return s == null || s.length() == 0 ? null
					: s.equals(FAILED_STRING) ? FAILED_OBJECT
					:                           molDistHistEncoder.decode(s);
		} catch (RuntimeException e1) {
			return FAILED_OBJECT;
		}
	}

	public MolDistHist createDescriptor(Object mol) {

		StereoMolecule fragBiggest = (StereoMolecule)mol;

		fragBiggest.stripSmallFragments();

		fragBiggest.ensureHelperArrays(StereoMolecule.cHelperCIP);

		if(fragBiggest.getAtoms() < MIN_NUM_ATOMS){
			return FAILED_OBJECT;
		} else if(fragBiggest.getAtoms() > MAX_NUM_HEAVY_ATOMS){
			return FAILED_OBJECT;
		}


		MolDistHist mdh = null;

		boolean conformationGenerationFailed = true;

		int ccFailed = 0;

		exceptionCreateDescriptor = null;

		while (conformationGenerationFailed) {

			conformationGenerationFailed = true;

			try {

				CGMult cgMult = creatorCompleteGraph.createCGMult(fragBiggest);

				mdh = cgMult.getMolDistHist();

				if(creatorCompleteGraph.isOnlyOneConformer() && (ccFailed < MAX_TRIES_TO_GENERATE_CONFORMER_ONE_CONF)){

					conformationGenerationFailed = true;

				} else {

					conformationGenerationFailed = false;

				}

			} catch (ExceptionConformationGenerationFailed e) {

				exceptionCreateDescriptor = e;

			} catch (Exception e) {

				exceptionCreateDescriptor = e;

			}

			if(conformationGenerationFailed) {

				if(DEBUG) {
					System.out.println("DescriptorHandlerFlexophore Inject new seed");
				}

				creatorCompleteGraph.injectNewSeed();

				ccFailed++;
			}

			if(ccFailed==MAX_TRIES_TO_GENERATE_CONFORMER){

				try {
					if(DEBUG) {

						Canonizer can = new Canonizer(fragBiggest);

						String msg = "DescriptorHandlerFlexophore Impossible to generate conformer for\n" + can.getIDCode();

						System.err.println(msg);
					}

				} catch (Exception e) {
					e.printStackTrace();
					exceptionCreateDescriptor = e;
				}

				break;

			}

		}

		if(creatorCompleteGraph.isOnlyOneConformer() && (ccFailed > 1) && (mdh != null)){

			System.out.println("Flexophore: only one conformer generated after trying " + ccFailed + " seeds.");

		} else if((ccFailed > 1) && (mdh != null)){

			System.out.println("Flexophore: generation worked after injection of " + ccFailed + " seeds.");

		} else if (conformationGenerationFailed) {

			System.out.println("Flexophore: generation failed finally after injection of " + ccFailed + " seeds.");

		}

		if(mdh == null) {
			mdh = FAILED_OBJECT;
		} else if (mdh.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE) {

			String msg = "Flexophore exceeded maximum number of nodes.";

			exceptionCreateDescriptor = new RuntimeException(msg);

			mdh = FAILED_OBJECT;;
		}

		return mdh;
	}

	public Exception getExceptionCreateDescriptor() {
		return exceptionCreateDescriptor;
	}


	public float getSimilarity(Object query, Object base) {

		float sc=0;

		if(base == null
				|| query == null
				|| ((IMolDistHist)base).getNumPPNodes() == 0
				|| ((IMolDistHist)query).getNumPPNodes() == 0) {
			sc = 0;

		} else {

			IMolDistHist mdhvBase = (IMolDistHist)base;

			IMolDistHist mdhvQuery = (IMolDistHist)query;

			if(mdhvBase.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE){

				System.out.println("DescriptorHandlerFlexophore getSimilarity(...) mdhvBase.getNumPPNodes() " + mdhvBase.getNumPPNodes());

				return 0;
			} else if(mdhvQuery.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE){
				System.out.println("DescriptorHandlerFlexophore getSimilarity(...) mdhvQuery.getNumPPNodes() " + mdhvQuery.getNumPPNodes());
				return 0;
			}

			sc = (float)getMinimumSimilarity(mdhvBase, mdhvQuery);

		}

		return normalizeValue(sc);

		// return sc;
	}

	private double getMinimumSimilarity(IMolDistHist mdhvBase, IMolDistHist mdhvQuery){

		double sc = 0;

		if(mdhvBase.getNumPPNodes() == mdhvQuery.getNumPPNodes()){
			double s1 = getSimilarity(mdhvBase, mdhvQuery);
			double s2 = getSimilarity(mdhvQuery, mdhvBase);

			sc = Math.max(s1, s2);
		} else {
			sc = getSimilarity(mdhvBase, mdhvQuery);
		}
		return sc;
	}


	private double getSimilarity(IMolDistHist mdhvBase, IMolDistHist mdhvQuery){

		CompleteGraphMatcher<IMolDistHist> cgMatcher = queueCGM.poll();

		if(cgMatcher == null){
			cgMatcher = getNewCompleteGraphMatcher();
		}

		cgMatcher.set(mdhvBase, mdhvQuery);

		double sc = (float)cgMatcher.calculateSimilarity();

		queueCGM.add(cgMatcher);

		return sc;
	}

	public double getSimilarityNodes(PPNode query, PPNode base) {
		return objectiveCompleteGraphHard.getSimilarityNodes(query, base);
	}


	public float normalizeValue(double value) {
		return value <= 0.0f ? 0.0f
				: value >= 1.0f ? 1.0f
				: (float)(1.0-Math.pow(1-Math.pow(value, CORRECTION_FACTOR) ,1.0/CORRECTION_FACTOR));
	}

	public boolean calculationFailed(Object o) {

		if(o instanceof MolDistHist){
			return ((MolDistHist)o).getNumPPNodes() == 0;
		} else if(o instanceof MolDistHistViz){
			return ((MolDistHistViz)o).getNumPPNodes() == 0;
		}

		return true;

	}

	public DescriptorHandlerFlexophore getThreadSafeCopy() {

		DescriptorHandlerFlexophore dh = new DescriptorHandlerFlexophore();

		return dh;
	}
	public void setObjectiveQueryBiased(boolean enable){

		throw new RuntimeException("setObjectiveQueryBiased(...) is not implemented");
	}

}
