package org.openmolecules.fx.viewer3d;

/**
 * Created by thomas on 09/10/16.
 */
public interface V3DSceneListener {
//	public void updateMolecule(V3DMolecule fxmol);
	public void addMolecule(V3DMolecule fxmol);
	public void removeMolecule(V3DMolecule fxmol);
}
