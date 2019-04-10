package org.openmolecules.fx.viewer3d;

import javafx.scene.Group;
import org.controlsfx.control.HiddenSidesPane;
import org.openmolecules.fx.viewer3d.panel.SidePanel;

/**
 * Created by thomas on 25.09.16.
 */
public class V3DSceneWithSidePane extends HiddenSidesPane {
	private V3DScene mScene3D;
	private SidePanel mMoleculePanel;

	public V3DSceneWithSidePane() {
		this(1024, 768);
	}

	public V3DSceneWithSidePane(int width, int height) {
		mScene3D = new V3DScene(new Group(), width, height);

		mMoleculePanel = new SidePanel(mScene3D, this);
		mMoleculePanel.getStyleClass().add("side-panel");
		setLeft(mMoleculePanel);
//		setContent(mScene3D);
		setContent(new V3DSceneWithSelection(mScene3D));
	}

	public V3DScene getScene3D() {
		return mScene3D;
	}

	public SidePanel getMoleculePanel() {
		return mMoleculePanel;
	}
}
