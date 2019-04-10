/*
 * Project: DD_jfx
 * @(#)MoleculeDataFormats.java
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
 * Author: Christian Rufener
 */

package com.actelion.research.jfx.dataformat;

import com.actelion.research.chem.dnd.MoleculeFlavors;
import javafx.scene.input.DataFormat;

import java.awt.datatransfer.SystemFlavorMap;

/**
 * Created with IntelliJ IDEA.
 * User: rufenec
 * Date: 9/9/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
@Deprecated
public class MoleculeDataFormats
{
    public static final DataFormat DF_SERIALIZEDMOLECULE    = new DataFormat(SystemFlavorMap.encodeDataFlavor(MoleculeFlavors.DF_SERIALIZEDOBJECT));
    public static final DataFormat DF_MDLMOLFILE            = new DataFormat(SystemFlavorMap.encodeDataFlavor(MoleculeFlavors.DF_MDLMOLFILE));
    public static final DataFormat DF_MDLMOLFILEV3          = new DataFormat(SystemFlavorMap.encodeDataFlavor(MoleculeFlavors.DF_MDLMOLFILEV3));
    public static final DataFormat DF_SMILES                = new DataFormat(SystemFlavorMap.encodeDataFlavor(MoleculeFlavors.DF_SMILES));

    public static final DataFormat DF_SERIALIZEDREACTANT    = new DataFormat("com.actelion.research.mercury.model.Reactant");

    public static final DataFormat[] DATA_FORMATS = {
            DF_SERIALIZEDMOLECULE,
            DF_MDLMOLFILEV3,
            DF_MDLMOLFILE,
            DF_SERIALIZEDREACTANT,
            DataFormat.PLAIN_TEXT,
            DF_SMILES
    };
}
