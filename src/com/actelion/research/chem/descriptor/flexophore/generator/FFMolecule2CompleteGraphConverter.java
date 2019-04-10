package com.actelion.research.chem.descriptor.flexophore.generator;

import com.actelion.research.chem.*;
import com.actelion.research.chem.descriptor.flexophore.PPNodeViz;

/**
 * FFMolecule2CompleteGraphConverter
 * <p>Copyright: Actelion Pharmaceuticals Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * Created by korffmo1 on 27.06.16.
 */
public class FFMolecule2CompleteGraphConverter {
    /**
     * Generates a complete graph from a summarised molecule.
     * The atoms which are flagged by FFMoleculeFunctions.FLAG_CENTER_ATOM will be used for the summary.
     * @param ff
     * @return
     */
    public static CompleteGraph convert(FFMolecule ff) {
        CompleteGraph cg = new CompleteGraph();

        for (int i = 0; i < ff.getAllAtoms(); i++) {

            if(ff.isAtomFlag(i, FFMoleculeFunctions.FLAG_CENTER_ATOM)) {

                if(ff.getAllConnAtoms(i)>0) {
                    StereoMolecule ext = ff.toStereoMolecule();
                    Canonizer can = new Canonizer(ext);
                    String e = "Center atom " + i + " in molecule " + can.getIDCode() + " has " + ff.getAllConnAtoms(i) + " neighbors.";
                    throw new RuntimeException(e);
                }

                Coordinates coord = new Coordinates(ff.getAtomX(i), ff.getAtomY(i), ff.getAtomZ(i));

                // The index of the original atom is taken from the group
                String sIndOriginalAtom = ff.getAtomChainId(i);

                int interactionType = ff.getAtomInteractionClass(i);

                // System.out.println("interactionType " + interactionType);

                if(interactionType > -1) {
                    PPNodeViz node = new PPNodeViz(coord, interactionType, Integer.parseInt(sIndOriginalAtom));

                    cg.addNode(node);
                }
            }
        }

        cg.calculateDistances();

        return cg;

    }
}
