package com.actelion.research.datawarrior;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.gui.viewer2d.ActionProvider;
import com.actelion.research.gui.viewer2d.MoleculeViewer;
import org.openmolecules.render.MoleculeRaytraceDialog;

import javax.vecmath.Matrix3d;
import java.awt.*;


public class MolViewerActionRaytrace implements ActionProvider<MoleculeViewer> {
	private Frame mParentFrame;

	public MolViewerActionRaytrace(Frame parent) {
		mParentFrame = parent;
		}

	@Override
	public String getActionName() {
		return "Photo-Realistic Image...";
		}

	@Override
	public void performAction(MoleculeViewer viewer) {
		showRaytraceDialog(mParentFrame, viewer);
		}

	public void showRaytraceDialog(Frame parent, MoleculeViewer viewer) {
		Coordinates cr = viewer.getVisualizer3D().getCenterOfRotation();
		Matrix3d r = viewer.getVisualizer3D().getRotationMatrix();
		double[] t = viewer.getVisualizer3D().getTranslation();
		StereoMolecule mol = viewer.getMolecule().toStereoMolecule();

		// Rather than enlarging the entire image (and distorting the perspective with it)
		// we move the molecule towards the camera to get a similar enlargement.
		float zoomPercent = (float)viewer.getVisualizer3D().getZoomPercent();

		/* this creates a similar view by adjusting the sunflow camera distance
		float dy = SunflowMoleculeRenderer.DEFAULT_CAMERA_DISTANCE * (1f - 28f / zoomPercent);	// zoomPercent=28f leads to similarly sized molecule in sunflow

		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			float x = mol.getAtomX(atom) - (float)cr.x;
			float y = mol.getAtomY(atom) + (float)cr.y;
			float z = mol.getAtomZ(atom) + (float)cr.z;
			mol.setAtomX(atom,  x*r.m00 - y*r.m01 - z*r.m02 + t[0]);
			mol.setAtomY(atom,  x*r.m20 - y*r.m21 - z*r.m22 - dy);
			mol.setAtomZ(atom, -x*r.m10 + y*r.m11 + z*r.m12 - t[1]);
			}

		new MoleculeRaytraceDialog(parent, mol);
		*/

		// this creates a similar view by adjusting the sunflow field of view parameter, which result in a closer perspective to JMol
		float fov = 4f * 100f / zoomPercent;

		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			double x = mol.getAtomX(atom) - cr.x;
			double y = mol.getAtomY(atom) + cr.y;
			double z = mol.getAtomZ(atom) + cr.z;
			mol.setAtomX(atom,  x*r.m00 - y*r.m01 - z*r.m02 + t[0]);
			mol.setAtomY(atom,  x*r.m20 - y*r.m21 - z*r.m22);
			mol.setAtomZ(atom, -x*r.m10 + y*r.m11 + z*r.m12 - t[1]);
			}

		new MoleculeRaytraceDialog(parent, new Conformer(mol), 40f, 0f, 1f, fov);
		}
	}
