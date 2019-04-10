package org.openmolecules.fx.viewer3d;

import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class V3DSceneWithSelection extends Pane {
	private V3DScene mScene3D;
	private Polygon mSelection;
	private int mSelectionMode;	// 1:adding, 2:subtracting

	public V3DSceneWithSelection(V3DScene scene3D) {
		mScene3D = scene3D;

		setOnMousePressed(me -> {
			mSelection = null;
			if (me.getButton() == MouseButton.PRIMARY) {
				mSelectionMode = me.isShiftDown() ? 1 : me.isControlDown() ? 2 : 0;
				startSelection(me.getX(), me.getY());
			}
		} );
		setOnMouseDragged(me -> {
			if (mSelection != null) {
				appendToSelection(me.getX(), me.getY());
			}
		} );
		setOnMouseReleased(me -> {
			if (mSelection != null) {
				finishSelection();
			}
		} );
		getChildren().addAll(scene3D);
	}

	private void startSelection(double x, double y) {
		mSelection = new Polygon();
		mSelection.setStroke(V3DScene.SELECTION_COLOR);
		mSelection.setFill(Color.TRANSPARENT);
		mSelection.getPoints().add(x);
		mSelection.getPoints().add(y);
		getChildren().add(mSelection);
	}

	private void appendToSelection(double x, double y) {
		int index = mSelection.getPoints().size()-2;
		double xOld = mSelection.getPoints().get(index);
		double yOld = mSelection.getPoints().get(index+1);
		if (Math.abs(xOld - x) > 2 || Math.abs(yOld - y) > 2) {
			mSelection.getPoints().add(x);
			mSelection.getPoints().add(y);
		}
	}

	private void finishSelection() {
		getChildren().remove(mSelection);
		if (mSelection.getPoints().size() > 2)
			mScene3D.select(mSelection, mSelectionMode, localToScreen(0,0));
		mSelection = null;
	}

	public V3DScene getScene3D() {
		return mScene3D;
	}
}
