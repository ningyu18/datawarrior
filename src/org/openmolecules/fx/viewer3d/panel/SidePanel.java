package org.openmolecules.fx.viewer3d.panel;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.controlsfx.control.HiddenSidesPane;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneListener;

import static javafx.collections.FXCollections.observableArrayList;

/**
 * Created by thomas on 27.09.16.
 */
public class SidePanel extends BorderPane implements V3DSceneListener {
	private static final boolean AUTO_HIDE_AT_START = false;
	private V3DScene mScene3D;
	private CheckBox mCheckBoxPin;
	private ObservableList<MoleculeModel> mCellModelList;
	private BooleanProperty mShowStructure;

	public SidePanel(final V3DScene scene3D, final HiddenSidesPane pane) {
		super();

		mScene3D = scene3D;
		mScene3D.setSceneListener(this);

		mCheckBoxPin = new CheckBox("Auto-Hide Panel");
		mCheckBoxPin.setSelected(AUTO_HIDE_AT_START);
		if (!AUTO_HIDE_AT_START)
			pane.setPinnedSide(Side.LEFT);
		mCheckBoxPin.setOnAction(event -> pane.setPinnedSide(mCheckBoxPin.isSelected() ? null : Side.LEFT) );
		mCheckBoxPin.setPadding(new Insets(8, 8, 8, 8));
		mCheckBoxPin.setStyle("-fx-text-fill: white;");

		setBottom(mCheckBoxPin);

		mCellModelList = FXCollections.observableArrayList(new Callback<MoleculeModel, Observable[]>() {
			@Override
			public Observable[] call(MoleculeModel molModel) {
				return new Observable[] { molModel.getMolecule3D().visibleProperty() };
			}});

		mShowStructure = new SimpleBooleanProperty(true);

		ListView<MoleculeModel> listView = new ListView<>(mCellModelList);
		listView.setStyle("-fx-background-color: #00000000;");
		listView.setPrefWidth(160);
		listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listView.setCellFactory(new MoleculeCellFactory(mShowStructure));
		setCenter(listView);

//		setPrefSize(160, 160);
	}

	public V3DScene getV3DScene() {
		return mScene3D;
	}

	public void addMolecule(V3DMolecule fxmol) {
		mCellModelList.add(new MoleculeModel(fxmol));
	}

	public void setShowStructure(boolean b) {
		mShowStructure.set(b);
	}

	public void removeMolecule(V3DMolecule fxmol) {
		for (int i = 0; i< mCellModelList.size(); i++) {
			if (mCellModelList.get(i).getMolecule3D() == fxmol) {
				mCellModelList.remove(i);
			}
		}
	}
}
