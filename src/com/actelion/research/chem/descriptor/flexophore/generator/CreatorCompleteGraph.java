package com.actelion.research.chem.descriptor.flexophore.generator;

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.AdvancedTools;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.chem.descriptor.flexophore.ExceptionConformationGenerationFailed;
import com.actelion.research.chem.redgraph.SubGraphExtractor;
import com.actelion.research.chem.redgraph.SubGraphIndices;
import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;
import com.actelion.research.forcefield.mm2.MM2Parameters;
import com.actelion.research.util.Formatter;
import org.openmolecules.chem.conf.gen.ConformerGenerator;

import java.util.Date;
import java.util.List;

/**
 * CreatorCompleteGraph
 * <p>Copyright: Actelion Pharmaceuticals Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * Created by korffmo1 on 19.02.16.
 */
public class CreatorCompleteGraph {

    private static final boolean DEBUG = DescriptorHandlerFlexophore.DEBUG;

    private static final long SEED = 123456789;

    // private static final int CONFORMATIONS = 250;

    // Maximum number of tries to generate conformers with the torsion rule based conformer generator from Thomas Sander
    private static final int MAX_NUM_TRIES = 10000;

    private static final int CONF_GEN_TS = 0;

    private static final int CONF_GEN_JF_FORCE_FIELD01 = 1;

    private static final int CONF_GEN_JF_FORCE_FIELD_SINGLE_CONFORMATION = 2;

    private static CreatorCompleteGraph INSTANCE;

    /**
     * We exclude C atoms that are close to a hetero atom.
     * These hetero atoms have their own specific atom type.
     */
    private static final InteractionType [] ARR_EXCLUDED_INTERACTION_TYPES = {
            new InteractionType(39, "6*CALKANE", "Inspecific single C atom."),
            new InteractionType(125, "6*CALKYNE_#NC", "C atom close to N"),
            new InteractionType(126, "6*CCARBONYL", "C atom close to O"),
            new InteractionType(127, "6*CCARBONYL_=NCC", "C atom close to O"),
            new InteractionType(128, "6*CCARBONYL_=NCN", "C atom close to O"),
            new InteractionType(129, "6*CCARBONYL_=NCO", "C atom close to O"),
            new InteractionType(130, "6*CCARBONYL_=NNN", "C atom close to O"),
            new InteractionType(131, "6*CCARBONYL_=OCC", "C atom close to O"),
            new InteractionType(132, "6*CCARBONYL_=OCN", "C atom close to O"),
            new InteractionType(133, "6*CCARBONYL_=OCO", "C atom close to O"),
            new InteractionType(134, "6*CCARBONYL_=OCS", "C atom close to O"),
            new InteractionType(135, "6*CCARBONYL_=ONN", "C atom close to O"),
            new InteractionType(136, "6*CCARBONYL_=ONO", "C atom close to O"),
            new InteractionType(137, "6*CCARBONYL_H1=NC", "C atom close to O"),
            new InteractionType(138, "6*CCARBONYL_H1=OC", "C atom close to O")
    };

    private SubGraphExtractor subGraphExtractor;

    private boolean [] arrExcludedInteractionTypesMap;

    private ClassInteractionStatistics classInteractionStatistics;

    private ConformerGenerator conformerGenerator;

    private int conformationMode;

    private MoleculeStandardizer moleculeStandardizer;

    private long seed;

    // for debugging
    private boolean onlyOneConformer;

    private int ccCounterCallsInitializeConformerGenerator;

    public CreatorCompleteGraph() {

        seed = SEED;

        subGraphExtractor = new SubGraphExtractor();

        classInteractionStatistics = ClassInteractionStatistics.getInstance();

        arrExcludedInteractionTypesMap = new boolean[classInteractionStatistics.getNClasses()];


        for (InteractionType exIntType : ARR_EXCLUDED_INTERACTION_TYPES) {

            arrExcludedInteractionTypesMap[exIntType.type] = true;

        }

        conformerGenerator = new ConformerGenerator(seed);

        moleculeStandardizer = new MoleculeStandardizer();

        conformationMode = CONF_GEN_TS;

        // System.out.println("CreatorCompleteGraph conformationMode " + conformationMode);

    }

    public void setConformationMode(int conformationMode) {
        this.conformationMode = conformationMode;
    }

    public CGMult createCGMult(StereoMolecule molOrig) throws Exception {

        CGMult cgMult = null;

        switch (conformationMode) {
            case CONF_GEN_TS:
                cgMult = createCGMultTSConfGen(molOrig);
                break;
            case CONF_GEN_JF_FORCE_FIELD01:
                cgMult = createCGMultJFConfGenForceField01(molOrig);
                break;
            case CONF_GEN_JF_FORCE_FIELD_SINGLE_CONFORMATION:
                cgMult = createCGMultJFConfGenForceFieldSingleConformation(molOrig);
                break;
            default:
                throw new RuntimeException("Invalid conformation mode");

        }

        return cgMult;
    }


    /**
     * Conformation generator of Thomas Sander
     * @param molOrig
     * @return
     * @throws Exception
     */
    public CGMult createCGMultTSConfGen(StereoMolecule molOrig) throws Exception {

        int nConformations = DescriptorHandlerFlexophore.NUM_CONFORMATIONS;

        StereoMolecule molStand = moleculeStandardizer.getStandardized(molOrig);

        molStand.ensureHelperArrays(Molecule.cHelperRings);


        // Commented out 27.06.2016
        // We calculate the first 3D structure with J.Freyss force field. The 1. structure is for
        // visualization and is smoother than with the self organising structure calculator from TS.
        // List<FFMolecule> li = AdvancedTools.convertMolecule2DTo3D(molStart);
        // FFMolecule ffMolInPlace = li.get(0);

        FFMolecule ffMolInPlace = new FFMolecule(molStand);

        FFMoleculeFunctions.removeHydrogensAndElectronPairs(ffMolInPlace);

        MM2Parameters.setAtomTypes(ffMolInPlace);

        classInteractionStatistics.setClassIdsForMolecule(ffMolInPlace);

        StereoMolecule molInPlace = ffMolInPlace.toStereoMolecule();

        molInPlace.ensureHelperArrays(Molecule.cHelperRings);

        List<SubGraphIndices> liFragment = subGraphExtractor.extract(molInPlace);

        if(DEBUG) {

            injectNewSeed();
        }

        conformerGenerator.initializeConformers(molInPlace, ConformerGenerator.STRATEGY_LIKELY_RANDOM, MAX_NUM_TRIES, false);


        int nAtoms = molInPlace.getAtoms();

        int ccConformationsGenerated = 0;

        boolean conformerGenerated = generateConformerAndSetCoordinates(conformerGenerator, nAtoms, ffMolInPlace);

        if(!conformerGenerated){

            throw new ExceptionConformationGenerationFailed("Impossible to generate one conformer!");

        }

        ccConformationsGenerated++;



        FFMolecule ffMolCenterSmooth = calcCenter(ffMolInPlace, liFragment);

        CompleteGraph cgInit = FFMolecule2CompleteGraphConverter.convert(ffMolCenterSmooth);

        CGMult cgMult = new CGMult(cgInit, ffMolCenterSmooth);

        for (int i = 1; i < nConformations; i++) {

            conformerGenerated = generateConformerAndSetCoordinates(conformerGenerator, nAtoms, ffMolInPlace);

            if(!conformerGenerated) {

                break;

            }

            FFMolecule ffMolCenter = calcCenter(ffMolInPlace, liFragment);

            CompleteGraph cg = FFMolecule2CompleteGraphConverter.convert(ffMolCenter);

            // Check for exceeding maximum distance in histogram
            if(i == 1) {

                double [][] arrDistance = cg.getEdges();

                for (int j = 0; j < arrDistance.length; j++) {

                    for (int k = j+1; k < arrDistance.length; k++) {

                        double dist = arrDistance[i][j];

                        if(dist > CGMult.RANGE_HISTOGRAM) {

                            throw new RuntimeException("Distance " + Formatter.format1(dist) + " between two pharmacophore points exceeded maximum histogram range.");

                        }
                    }
                }
            }

//            System.out.println(cg.toStringDistances());
//
//            System.out.println();

            cgMult.add(cg);

            ccConformationsGenerated++;
        }

        int nPotentialConformers = conformerGenerator.getPotentialConformerCount();

        onlyOneConformer = false;

        if((nPotentialConformers > 1) && (ccConformationsGenerated==1)){

            if(DEBUG) {
                System.out.println("CreatorCompleteGraph: only one conformer generated.");

                System.out.println("Seed " + seed);

                System.out.println("Potential conformer count " + nPotentialConformers);

                Canonizer can = new Canonizer(molInPlace);

                System.out.println(can.getIDCode());
            }

            onlyOneConformer = true;

        }

        return cgMult;

    }

    public void injectNewSeed(){

        seed = new Date().getTime();

        conformerGenerator = new ConformerGenerator(seed);

        ccCounterCallsInitializeConformerGenerator = 0;
    }


    public boolean isOnlyOneConformer() {
        return onlyOneConformer;
    }

    /**
     * Force field from Joel Freyss, derived from Tinker
     * @param molOrig
     * @return
     * @throws Exception
     */
    private CGMult createCGMultJFConfGenForceField01(StereoMolecule molOrig) throws Exception {

        // In this molecule the conformations will change.
        StereoMolecule molStart = molOrig.getCompactCopy();

        molStart.ensureHelperArrays(Molecule.cHelperRings);

        List<FFMolecule> li = AdvancedTools.convertMolecule2DTo3D(molStart);

        FFMolecule ffMolInPlace = li.get(0);

        MM2Parameters.setAtomTypes(ffMolInPlace);

        classInteractionStatistics.setClassIdsForMolecule(ffMolInPlace);

        List<FFMolecule> liFFConf = TorsionCalculator.createConformations(ffMolInPlace, DescriptorHandlerFlexophore.NUM_CONFORMATIONS);

        FFMoleculeFunctions.removeHydrogensAndElectronPairs(ffMolInPlace);

        StereoMolecule molInPlace = ffMolInPlace.toStereoMolecule();

        molInPlace.ensureHelperArrays(Molecule.cHelperRings);

        List<SubGraphIndices> liFragment = subGraphExtractor.extract(molInPlace);

        FFMolecule ffMolCenterSmooth = calcCenter(ffMolInPlace, liFragment);

        CGMult cgMult = new CGMult(FFMolecule2CompleteGraphConverter.convert(ffMolCenterSmooth), ffMolCenterSmooth);

        for (FFMolecule ffConf : liFFConf) {

            FFMoleculeFunctions.removeHydrogensAndElectronPairs(ffConf);

            MM2Parameters.setAtomTypes(ffConf);

            classInteractionStatistics.setClassIdsForMolecule(ffConf);

            FFMolecule ffMolCenter = calcCenter(ffConf, liFragment);

            CompleteGraph cg = FFMolecule2CompleteGraphConverter.convert(ffMolCenter);

            cgMult.add(cg);

        }

        return cgMult;

    }

    private CGMult createCGMultJFConfGenForceFieldSingleConformation(StereoMolecule molOrig) throws Exception {

        // In this molecule the conformations will change.
        StereoMolecule molStart = molOrig.getCompactCopy();

        molStart.ensureHelperArrays(Molecule.cHelperRings);

        List<FFMolecule> li = AdvancedTools.convertMolecule2DTo3D(molStart);

        FFMolecule ffMolInPlace = li.get(0);

        MM2Parameters.setAtomTypes(ffMolInPlace);

        classInteractionStatistics.setClassIdsForMolecule(ffMolInPlace);

        FFMoleculeFunctions.removeHydrogensAndElectronPairs(ffMolInPlace);

        StereoMolecule molInPlace = ffMolInPlace.toStereoMolecule();

        molInPlace.ensureHelperArrays(Molecule.cHelperRings);

        List<SubGraphIndices> liFragment = subGraphExtractor.extract(molInPlace);

        FFMolecule ffMolCenterSmooth = calcCenter(ffMolInPlace, liFragment);

        CGMult cgMult = new CGMult(FFMolecule2CompleteGraphConverter.convert(ffMolCenterSmooth), ffMolCenterSmooth);

        return cgMult;

    }


    /**
     * 08.03.2017 Method set to public for debugging purposes.
     * @param conformerGenerator
     * @param nAtoms
     * @param ffMolInPlace
     * @return
     */
    public static boolean generateConformerAndSetCoordinates(ConformerGenerator conformerGenerator, int nAtoms, FFMolecule ffMolInPlace){

        boolean nextConformerAvailable = false;

        Conformer conformer = conformerGenerator.getNextConformer();

        if(conformer != null){

            // System.out.println(ConformerUtils.toStringDistances(ConformerUtils.getDistanceMatrix(conformer)));

            for (int j = 0; j < nAtoms; j++) {

                double x = conformer.getX(j);
                double y = conformer.getY(j);
                double z = conformer.getZ(j);

                ffMolInPlace.getCoordinates(j).x = x;
                ffMolInPlace.getCoordinates(j).y = y;
                ffMolInPlace.getCoordinates(j).z = z;

            }
            nextConformerAvailable = true;
        }

        return nextConformerAvailable;
    }

    private FFMolecule calcCenter(FFMolecule ffMol, List<SubGraphIndices> liFragment) {

        FFMolecule ffMolCenter = new FFMolecule(ffMol);


        int ccIncluded = 0;
        int ccExcluded = 0;
        int ccPPPoints = 0;
        int ccSkippedPPPoints = 0;

        for (SubGraphIndices fragment : liFragment) {

            boolean ppPoint = false;

            int [] arrAtomIndexList = fragment.getAtomIndices();

            // Calculate center coordinates.
            Coordinates coordCenter = FFMoleculeFunctions.getCenterGravity(ffMolCenter, arrAtomIndexList);

            for (int at = 0; at < arrAtomIndexList.length; at++) {

                // Atom type.
                int interactionType = ffMolCenter.getAtomInteractionClass(arrAtomIndexList[at]);

                if(interactionType == -1){

                    continue;

                }

                if(arrExcludedInteractionTypesMap[interactionType]){

//                    System.out.println("CreatorCompleteGraph interaction type skipped");
//
//                    StereoMolecule mol = ffMolCenter.toStereoMolecule();
//
//                    mol.ensureHelperArrays(Molecule.cHelperRings);
//
//                    Canonizer can = new Canonizer(mol);
//
//                    System.out.println(can.getIDCode());

                    ccExcluded++;

                    continue;

                }

                ppPoint = true;

                // MM2 interaction type
                int iMM2Type = ffMolCenter.getMM2AtomType(arrAtomIndexList[at]);

                int iAtomicNo = ffMolCenter.getAtomicNo(arrAtomIndexList[at]);

                ffMolCenter.setAtomFlag(arrAtomIndexList[at], FFMolecule.FLAG1, true);

                int indexOriginalAtom = arrAtomIndexList[at];

                int indexAtm = ffMolCenter.addAtom(iAtomicNo);

                ffMolCenter.setAtomInteractionClass(indexAtm, interactionType);

                ffMolCenter.setMM2AtomType(indexAtm, iMM2Type);

                String sOrigIndex = Integer.toString(indexOriginalAtom);

                ffMolCenter.setAtomChainId(indexAtm, sOrigIndex);

                // Set the center coordinates
                ffMolCenter.setCoordinates(indexAtm, coordCenter);

                ffMolCenter.setAtomFlag(indexAtm, FFMoleculeFunctions.FLAG_CENTER_ATOM, true);

                ffMolCenter.setPPP(indexAtm, arrAtomIndexList);

                ccIncluded++;
            }

            if(ppPoint) {

                ccPPPoints++;

            } else {

                ccSkippedPPPoints++;
            }
        }

//        System.out.println("CreatorCompleteGraph ppPoints " + ccPPPoints);
//
//        System.out.println("CreatorCompleteGraph skipped ppPoints " + ccSkippedPPPoints);
//
//        System.out.println("CreatorCompleteGraph included atoms " + ccIncluded);
//
//        System.out.println("CreatorCompleteGraph excluded atoms " + ccExcluded);






        return ffMolCenter;
    }

    public static int getConformations2Generate(StereoMolecule mol, int maxNumConf) {

        int conf = 0;

        int rot = mol.getRotatableBondCount();

        double dConf = Math.pow(3,rot);

        if(dConf > maxNumConf){

            conf = maxNumConf;

        } else {

            conf = (int)dConf;

        }

        return conf;

    }

    private static class InteractionType {

        int type;

        String description;

        String comment;

        public InteractionType(int type, String description, String comment) {
            this.type = type;
            this.description = description;
            this.comment = comment;
        }
    }

    public static CreatorCompleteGraph getInstance(){

        if(INSTANCE==null){

            INSTANCE = new CreatorCompleteGraph();

        }

        return INSTANCE;
    }

}
