package org.openmolecules.fx.viewer3d;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.KeyCode;

public class V3DKeyHandler {
	public static boolean sControlIsDown;
	private Group mAxis;

	public V3DKeyHandler(final V3DScene scene) {
		final PerspectiveCamera camera = (PerspectiveCamera)scene.getCamera();

		scene.setOnKeyPressed(ke -> {
				switch (ke.getCode()) {
				case CONTROL:
					sControlIsDown = true;
					break;
				case Z:
					camera.setTranslateZ(-V3DScene.CAMERA_INITIAL_DISTANCE);
					break;
				case X:
					if (mAxis != null)
						mAxis.setVisible(!mAxis.isVisible());
					break;
				case Y:
					double fieldOfView = camera.getFieldOfView();
					double screenSize = camera.isVerticalFieldOfView() ? scene.getHeight() : scene.getWidth();
					double sizeAtZ0 = -camera.getTranslateZ() * Math.tan(Math.PI*fieldOfView/90);
					System.out.println("sizeAtZ0:"+sizeAtZ0);
					break;
				default:
					int index = ke.getCode().ordinal() - KeyCode.DIGIT1.ordinal();
					if (index >= 0 && index < 9) {
						int molIndex = 0;
						for (Node node:scene.getWorld().getChildren()) {
							if (node instanceof V3DMolecule) {
								if (index == molIndex) {
									V3DMolecule fxmol = (V3DMolecule)node;
									fxmol.setVisible(!fxmol.isVisible());
									break;
									}
								molIndex++;
								}
							}
						}
					break;
					}
				} );
		scene.setOnKeyReleased(ke -> {
				if (ke.getCode() == KeyCode.CONTROL) {
					sControlIsDown = false;
					}
				} );
		}

	public void setAxis(Group axis) {
		mAxis = axis;
		}
	}
