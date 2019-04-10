package org.openmolecules.fx.viewer3d.panel;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.openmolecules.fx.viewer3d.V3DMolecule;

/**
 * Created by thomas on 09/10/16.
 */
public class MoleculeModel {
	private V3DMolecule mMol3D;
	private StereoMolecule mMol2D;
	private static int sStructureCount;

	public MoleculeModel(V3DMolecule mol3D) {
		mMol2D = createMolecule2D(mol3D);
		mMol3D = mol3D;
	}

	private StereoMolecule createMolecule2D(V3DMolecule mol3D) {
		StereoMolecule mol = mol3D.getConformer().getMolecule().getCompactCopy();
		mol.ensureHelperArrays(Molecule.cHelperParities);	// create parities from 3D-coords
		new CoordinateInventor(CoordinateInventor.MODE_REMOVE_HYDROGEN).invent(mol);
		mol.setStereoBondsFromParity();

		sStructureCount++;
		if (mol.getName() == null || mol.getName().length() == 0)
			mol.setName("Structure "+sStructureCount);

		return mol;
	}

	public String getMoleculeName() {
		if (mMol3D.getConformer().getName() != null)
			return mMol3D.getConformer().getName();
		return mMol2D.getName();
	}

	public StereoMolecule getMolecule2D() {
		return mMol2D;
	}

	public V3DMolecule getMolecule3D() {
		return mMol3D;
	}
}
