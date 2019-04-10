package com.actelion.research.datawarrior;

import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.viewer2d.ActionProvider;
import com.actelion.research.gui.viewer2d.MoleculeViewer;

import java.awt.*;


public class MolViewerActionCopy implements ActionProvider<MoleculeViewer> {
	private Frame mParentFrame;

	public MolViewerActionCopy(Frame parent) {
		mParentFrame = parent;
		}

	@Override
	public String getActionName() {
		return "Copy Conformer";
		}

	@Override
	public void performAction(MoleculeViewer viewer) {
		new ClipboardHandler().copyMolecule(viewer.getMolecule().toStereoMolecule());
		}
	}
