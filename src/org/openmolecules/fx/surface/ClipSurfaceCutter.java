package org.openmolecules.fx.surface;

import org.openmolecules.fx.viewer3d.V3DMolecule;

/**
 * Created by thomas on 23.04.16.
 */
public class ClipSurfaceCutter extends SurfaceCutter {
	private double mNearClip, mFarClip;
	private V3DMolecule mFXMol;

	public ClipSurfaceCutter(double nearSlab, double farSlab, V3DMolecule fxmol) {
		mNearClip = nearSlab;
		mFarClip = farSlab;
		mFXMol = fxmol;
	}

	@Override
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		double szi = mFXMol.localToScene(xi, yi, zi).getZ();
		double szo = mFXMol.localToScene(xo, yo, zo).getZ();
		double f = (szo < mNearClip) ? (mNearClip - szo) / (szi - szo) : (szo - mFarClip) / (szo - szi);
		location[0] += (float)(xo + f * (xi - xo));
		location[1] += (float)(yo + f * (yi - yo));
		location[2] += (float)(zo + f * (zi - zo));
	}

	@Override
	protected boolean pointIsOutside(float x, float y, float z) {
		double sz = mFXMol.localToScene(x, y, z).getZ();
		return (sz < mNearClip) || (sz > mFarClip);
	}
}
