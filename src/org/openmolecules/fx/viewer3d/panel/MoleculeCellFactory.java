package org.openmolecules.fx.viewer3d.panel;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

/**
 * Created by thomas on 20.10.16.
 */
public class MoleculeCellFactory implements Callback<ListView<MoleculeModel>, ListCell<MoleculeModel>> {
	private BooleanProperty mShowStructure;
	public MoleculeCellFactory(BooleanProperty showStructure) {
		mShowStructure = showStructure;
	}

	@Override
	public ListCell<MoleculeModel> call(ListView<MoleculeModel> listview) {
		return new MoleculeCell(mShowStructure);
	}
}
