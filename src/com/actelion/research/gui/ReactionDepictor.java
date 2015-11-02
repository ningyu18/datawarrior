/*
 * Project: DD_gui
 * @(#)ReactionDepictor.java
 *
 * Copyright (c) 1997- 2015
 * Actelion Pharmaceuticals Ltd.
 * Gewerbestrasse 16
 * CH-4123 Allschwil, Switzerland
 *
 * All Rights Reserved.
 *
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.
 *
 * Author:
 */

package com.actelion.research.gui;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.share.gui.AbstractReactionDepictor;

/**
 * Created by rufenec on 30/03/15.
 */
public class ReactionDepictor extends AbstractReactionDepictor
{

    public ReactionDepictor(Reaction rxn)
    {
        super(rxn);
    }

    @Override
    public AbstractDepictor createDepictor(StereoMolecule molecule, int displaymode)
    {
        return new Depictor2D(molecule,displaymode);
    }
/*
    @Override
    protected void drawLine(AbstractDepictor.DepictorLine theLine)
    {

    }
*/

//    @Override
//    protected AbstractDepictor createDepictor(StereoMolecule mol, int width, int height, int displayMode)
//    {
//        return new Depictor2D(mol,displayMode);
//    }

//    @Override
//    protected void drawPolygon(float[] x, float[] y, int size)
//    {
//
//    }
}
