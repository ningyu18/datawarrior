/*
 * Project: DD_jfx
 * @(#)ClipboardHelper.java
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

package com.actelion.research.jfx.gui.misc;

import com.actelion.research.chem.*;
import com.actelion.research.jfx.dataformat.MoleculeDataFormats;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by baerr on 7/28/14.
 */
public class ClipboardHelper {
    private ClipboardHelper() {
    }

    public static ClipboardContent writeContent(StereoMolecule mol) {
        return writeContent(mol, null);
    }

    public static ClipboardContent writeContent(StereoMolecule mol, ClipboardContent content) {
        if(content==null)
            content = new ClipboardContent();

        // JavaFx/awt clipboard
        content.put(MoleculeDataFormats.DF_SERIALIZEDMOLECULE, mol);

        MolfileV3Creator molfileV3Creator = new MolfileV3Creator(mol);
        content.put(MoleculeDataFormats.DF_MDLMOLFILEV3, molfileV3Creator.getMolfile());

        MolfileCreator molfileCreator = new MolfileCreator(mol);
        content.put(MoleculeDataFormats.DF_MDLMOLFILE, molfileCreator.getMolfile());

        Canonizer can = new Canonizer(mol);
        String idcode = String.format("%s %s", can.getIDCode(), can.getEncodedCoordinates(true));
        content.putString(idcode);

        SmilesCreator smilesCreator = new SmilesCreator();
        content.put(MoleculeDataFormats.DF_SMILES, smilesCreator.generateSmiles(mol));

        return content;
    }

    public static void copy(StereoMolecule mol) {
        // JavaFx clipboard
//        final Clipboard clipboard = Clipboard.getSystemClipboard();
//        clipboard.setContent(writeContent(mol));

        // Native clipboard
//        ClipboardHandler handler = new ClipboardHandler();
//        handler.copyMolecule(mol);
    }

    public static StereoMolecule readContent(Clipboard clipboard) {
        StereoMolecule mol = null;
        List<DataFormat> formats = getAcceptedFormats(clipboard);

        int i = -1;
        while (mol == null && ++i < formats.size()) {
            DataFormat format = formats.get(i);

            if (format.equals(MoleculeDataFormats.DF_SERIALIZEDMOLECULE)) {
                System.out.println("Put molecule using " + format);
                try {
                    mol = (StereoMolecule) clipboard.getContent(format);
                } catch (Exception e) {
                    System.err.println("Cannot parse serialized data for molecule");
                }
            } else if (format.equals(MoleculeDataFormats.DF_MDLMOLFILEV3) || format.equals(MoleculeDataFormats.DF_MDLMOLFILE)) {
                System.out.println("Put molecule using " + format);
                try {
                    MolfileParser p = new MolfileParser();
                    p.parse(mol, clipboard.getContent(format).toString());
                } catch (Exception e) {
                    System.err.println("Cannot parse molfile/molfilev3 data for molecule");
                }
            } else if (format.equals(DataFormat.PLAIN_TEXT)) {
                System.out.println("Put molecule using " + format);
                try {
                    IDCodeParser p = new IDCodeParser(true);
                    p.parse(mol, clipboard.getString());
                } catch (Exception e) {
                    System.err.println("Cannot parse idcode data for molecule");
                }
            } else if (format.equals(MoleculeDataFormats.DF_SMILES)) {
                System.out.println("Put molecule using " + format);
                try {
                    SmilesParser p = new SmilesParser();
                    p.parse(mol, clipboard.getContent(format).toString());
                } catch (Exception e) {
                    System.err.println("Cannot parse smiles data for molecule");
                }
            }
        }
        return mol;
    }

    public static StereoMolecule paste() {
        StereoMolecule mol = null;

        // Native clipboard
//        ClipboardHandler handler = new ClipboardHandler();
//        mol = handler.pasteMolecule();
        if(mol!=null) {
            System.out.println("Got molecule from native clipboard");
        } else {
            // JavaFx clipboard
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            mol = readContent(clipboard);
            if(mol!=null) {
                System.out.println("Got molecule from standard clipboard");
            }
        }

        return mol;
    }

    public static List<DataFormat> getAcceptedFormats(Clipboard clipboard)
    {
        Set<DataFormat> formats = clipboard.getContentTypes();
        List<DataFormat> res = new ArrayList<DataFormat>();
        for (DataFormat dataFormat : MoleculeDataFormats.DATA_FORMATS) {
            for (DataFormat f : formats) {
                if (f.equals(dataFormat)) {
                    res.add(f);
                    break;
                }
            }
        }
        return res;
    }
}
