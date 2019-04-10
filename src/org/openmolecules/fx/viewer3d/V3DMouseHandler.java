package org.openmolecules.fx.viewer3d;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;

public class V3DMouseHandler {
	private static final long POPUP_DELAY = 750;	// milli seconds delay right mouse click may be used for rotation
	private static final int SINGLE_MOL_KEY = 1;	// 0: Shift; 1: Ctrl; 2: none (single mol if highlighted)

	private static final long WHEEL_DELAY_LIMIT = 250;	// milli seconds; above this delay we have the smallest clip change
	private static final double WHEEL_MIN_FACTOR = 0.05;// smallest clip change = this factor * smallest mouse wheel delta
	private static final double WHEEL_MAX_FACTOR = 50;	// largest clip change * this factor * smallest clip change (when mouse wheel is rotated quickly)

	private volatile boolean mShowPopup;
	private V3DScene mScene;
	private double mMouseX,mMouseY;
	private long mRecentWheelMillis;
	private V3DMolecule mHighlightedMol,mMousePressedMol,mAffectedMol;
	private long mMousePressedMillis;


	public V3DMouseHandler(final V3DScene scene) {
		mScene = scene;

		scene.setOnScroll(se -> {
			// we modify the wheel delta depending on how quickly the wheel is rotated
			double delta = WHEEL_MIN_FACTOR * se.getDeltaY();	// delta is the smallest possible clip step
			long millis = System.currentTimeMillis();
			long delay = Math.max(1L, millis - mRecentWheelMillis);
			if (delay < WHEEL_DELAY_LIMIT)
				delta *= Math.pow(WHEEL_MAX_FACTOR, (1.0 - (double)delay / WHEEL_DELAY_LIMIT));
			mRecentWheelMillis = millis;

			if (V3DPopupMenu.sUseMouseWheelForClipping || se.isShiftDown()) {
				if (se.isControlDown()) {
					delta *= getScreenToObjectFactor (mScene.getCamera().farClipProperty().getValue());
					moveFarClip(delta, false);
				}
				else {
					delta *= getScreenToObjectFactor (mScene.getCamera().nearClipProperty().getValue());
					if (delta < 0)
						moveFarClip(moveNearClip(delta), true);
					else
						moveNearClip(moveFarClip(delta, true));
				}
			}
			else {
				trackHighlightedMol(se.getPickResult());
				trackAffectedMol(isSingleMolecule(se), true);
//				translate(0, 0, delta);     // moving the world in the universe moves the world relative to the rotation center
				if (mAffectedMol == null)
					translateCamera(-delta);    // this does not change the world rotation center
				else
					translate(0, 0, delta);
			}
		} );
		scene.setOnMousePressed(me -> {
			mMouseX = me.getScreenX();
			mMouseY = me.getScreenY();

			mAffectedMol = null;
			mMousePressedMol = mHighlightedMol;

			// setOnMouseClicked() is strangely often not triggered. Therefore we simulate...
			long millis = System.currentTimeMillis();
			boolean isDoubleClick = (millis - mMousePressedMillis < 300);
			mMousePressedMillis = millis;

			//System.out.println("mouse pressed isPrimaryButtonDown:"+me.isPrimaryButtonDown()+" isMiddleButtonDown:"+me.isMiddleButtonDown());
			if (me.getButton() == MouseButton.PRIMARY) {
				if (isDoubleClick)
					mScene.selectMolecule(mHighlightedMol, me.isShiftDown() ? 1 : me.isControlDown() ? 2 : 0);
			}

			if (me.getButton() == MouseButton.SECONDARY) {
				mShowPopup = true;
				new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(POPUP_DELAY);
						} catch (InterruptedException ie) {}
						if (mShowPopup) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									if (mShowPopup) {
										mShowPopup = false;
										Node node = me.getPickResult().getIntersectedNode();
										createPopupMenu(node, me.getScreenX(), me.getScreenY());
									}
								}
							});
						}
					}
				}.start();
			}
		} );
		scene.setOnMouseReleased(me -> {
			if (mShowPopup) {
				mShowPopup = false;
				Node node = me.getPickResult().getIntersectedNode();
				createPopupMenu(node, me.getScreenX(), me.getScreenY());
			}
//			mAffectedMol = null;
		} );
		scene.setOnMouseMoved(me -> {
			trackHighlightedMol(me.getPickResult());
		} );
		scene.setOnMouseDragged(me -> {
			trackHighlightedMol(me.getPickResult());
			trackAffectedMol(isSingleMolecule(me), false);

			double oldMouseX = mMouseX;
			double oldMouseY = mMouseY;
			mMouseX = me.getScreenX();
			mMouseY = me.getScreenY();
			double dx = (mMouseX - oldMouseX);
			double dy = (mMouseY - oldMouseY);

			if (me.isMiddleButtonDown()) {
				translate(dx, dy, 0);
			}
			else if (me.isSecondaryButtonDown()) {
				mShowPopup = false;
				rotate(dx, dy, me.isShiftDown());
			}
		} );
	}

	private void trackAffectedMol(boolean moleculeKeyIsDown, boolean isScrollWheel) {
		if (!moleculeKeyIsDown)
			mAffectedMol = null;
		else if (mAffectedMol == null) {
			if (isScrollWheel)
				mAffectedMol = mHighlightedMol;
			else
				mAffectedMol = mMousePressedMol;
			}
		}

	private boolean isSingleMolecule(MouseEvent me) {
		if (SINGLE_MOL_KEY == 0 && me.isShiftDown())
			return true;
		if (SINGLE_MOL_KEY == 1 && me.isControlDown())
			return true;
		if (SINGLE_MOL_KEY == 2 && mHighlightedMol != null)
			return true;
		return false;
	}

	private boolean isSingleMolecule(ScrollEvent me) {
		if (SINGLE_MOL_KEY == 0 && me.isShiftDown())
			return true;
		if (SINGLE_MOL_KEY == 1 && me.isControlDown())
			return true;
		if (SINGLE_MOL_KEY == 2 && mHighlightedMol != null)
			return true;
		return false;
	}

	private void trackHighlightedMol(PickResult pr) {
		Node node = pr.getIntersectedNode();

		Node molecule = node;
		while (molecule != null && !(molecule instanceof V3DMolecule))
			molecule = molecule.getParent();

		if (mHighlightedMol != null && (molecule == null || molecule != mHighlightedMol))
			mHighlightedMol.removeHilite();

		mHighlightedMol = (V3DMolecule) molecule;
	}

	/**
	 * Moves the near clip layer by delta units preventing going beyond near clip limit
	 * and far clip layer minus the minimum distance betreen those layers.
	 * @param delta
	 * @return the amount of really moved clip
	 */
	private double moveNearClip(double delta) {
		double nearClip = mScene.getCamera().nearClipProperty().getValue();
		double farClip = mScene.getCamera().farClipProperty().getValue();
		if (delta < 0) {
			if (nearClip > V3DScene.CAMERA_NEAR_CLIP) {
				if (delta < V3DScene.CAMERA_NEAR_CLIP - nearClip)
					delta = V3DScene.CAMERA_NEAR_CLIP - nearClip;
				mScene.getCamera().nearClipProperty().setValue(nearClip + delta);
				return delta;
			}
		}
		else {
			if (nearClip < farClip - V3DScene.CAMERA_MIN_CLIP_THICKNESS) {
				if (delta > farClip - V3DScene.CAMERA_MIN_CLIP_THICKNESS - nearClip)
					delta = farClip - V3DScene.CAMERA_MIN_CLIP_THICKNESS - nearClip;
				mScene.getCamera().nearClipProperty().setValue(nearClip + delta);
				return delta;
			}
		}
		return 0;
	}

	private double moveFarClip(double delta, boolean neglectRearLimit) {
		double nearClip = mScene.getCamera().nearClipProperty().getValue();
		double farClip = mScene.getCamera().farClipProperty().getValue();
		if (delta < 0) {
			if (farClip > nearClip + V3DScene.CAMERA_MIN_CLIP_THICKNESS) {
				if (delta < nearClip + V3DScene.CAMERA_MIN_CLIP_THICKNESS - farClip)
					delta = nearClip + V3DScene.CAMERA_MIN_CLIP_THICKNESS - farClip;
				mScene.getCamera().farClipProperty().setValue(farClip + delta);
				return delta;
			}
		}
		else {
			if (farClip < V3DScene.CAMERA_FAR_CLIP || neglectRearLimit) {
				if (delta > V3DScene.CAMERA_FAR_CLIP - farClip && !neglectRearLimit)
					delta = V3DScene.CAMERA_FAR_CLIP - farClip;
				mScene.getCamera().farClipProperty().setValue(farClip + delta);
				return delta;
			}
		}
		return 0;
	}

	private void translate(double dx, double dy, double dz) {
		RotatableGroup world = mScene.getWorld();

		double f = getScreenToObjectFactor (mAffectedMol != null ? mAffectedMol.getHighlightedZ() : world.getTranslateZ());

		if (mAffectedMol != null) {
			// p0 and p1 are world coordinates
			Point3D p0 = mAffectedMol.localToParent(mAffectedMol.getHighlightedPointLocal());
			Point3D p1 = world.sceneToLocal(mAffectedMol.getHighlightedPointInScene().subtract(f*dx, f*dy, f*dz));
			mAffectedMol.setTranslateX(mAffectedMol.getTranslateX() + p0.getX() - p1.getX());
			mAffectedMol.setTranslateY(mAffectedMol.getTranslateY() + p0.getY() - p1.getY());
			mAffectedMol.setTranslateZ(mAffectedMol.getTranslateZ() + p0.getZ() - p1.getZ());
		}
		else {
			Point3D p = world.sceneToLocal(world.localToScene(0,0,0).subtract(f*dx, f*dy, f*dz));
			world.setTranslateX(world.getTranslateX() - p.getX());
			world.setTranslateY(world.getTranslateY() - p.getY());
			world.setTranslateZ(world.getTranslateZ() - p.getZ());
		}
	}

	private void translateCamera(double dz) {
		double f = getScreenToObjectFactor(mScene.getWorld().getTranslateZ());
		mScene.moveCamera(2*f*dz);
	}

	private double getScreenToObjectFactor(double objectZ) {
		PerspectiveCamera camera = (PerspectiveCamera)mScene.getCamera();
		double f = -camera.getTranslateZ()
				* 2.0 * Math.tan(camera.getFieldOfView()*Math.PI/360)
				/ mScene.getWidth();

		double cameraZ = camera.getTranslateZ();
		// The following 2 compensates for the fact that
		// only about half of the field of view is visible.
		f *= 2 * (cameraZ - objectZ) / cameraZ;

		// it seems that the width/height ratio is about proportional to f
		f *= 0.5*mScene.getWidth()/mScene.getHeight();

		return f;
		}

	private void rotate(double dx, double dy, boolean aroundZAxis) {
		if (dx != 0 || dy != 0) {
			double f = 720 / (mScene.getWidth() + mScene.getHeight());	// about two rotations when dragging through full window
			double d = f * Math.sqrt(dx*dx+dy*dy);

			Point3D p1 = null;
			if (aroundZAxis) {
				Point2D origin = (mAffectedMol != null) ? mAffectedMol.localToScreen(0,0,0)
														: mScene.getUniverse().localToScreen(0,0,0);
				double x2 = mMouseX - origin.getX();
				double x1 = x2 - dx;
				double y2 = mMouseY - origin.getY();
				double y1 = y2 - dy;
				d = 180 / Math.PI * angleDif(angle(x1, y1), angle(x2, y2));
				p1 = new Point3D(0, 0, 1);
				}
			else {
				p1 = new Point3D(dy, -dx, 0);
				}
			if (mAffectedMol != null || mScene.isIndividualRotationModus()) {
				RotatableGroup world = mScene.getWorld();
				Point3D p0 = world.sceneToLocal(new Point3D(0, 0, 0));
				Point3D p2 = world.sceneToLocal(p1).subtract(p0);
				Rotate r = new Rotate(d, p2);
				if (mAffectedMol != null)
					mAffectedMol.rotate(r);
				else
					for (Node node : mScene.getWorld().getChildren())
						if (node instanceof V3DMolecule)
							((V3DMolecule) node).rotate(r);
				}
			else {
				mScene.getUniverse().rotate(new Rotate(d, p1));
				}
			}
		}

	private double angle(double dx, double dy) {
		if (dy == 0.0)
			return (dx > 0.0) ? Math.PI/2.0 : -Math.PI/2.0;

		double angle = Math.atan(dx/dy);
		if (dy < 0.0)
			return (dx < 0.0) ? angle - Math.PI : angle + Math.PI;

		return angle;
		}

	public static double angleDif(double angle1, double angle2) {
		double angleDif = angle1 - angle2;
		while (angleDif < -Math.PI)
			angleDif += 2 * Math.PI;
		while (angleDif > Math.PI)
			angleDif -= 2 * Math.PI;
		return angleDif;
		}

	private void createPopupMenu(Node node, double x, double y) {
		// if we have an active node, create a node specific popup
		Node parent = node;
		while (parent != null && !(parent instanceof V3DMolecule))
			parent = parent.getParent();

		if (parent == null)
			new V3DPopupMenu(mScene, null).show(mScene.getWorld(), x, y);
		else
			new V3DPopupMenu(mScene, (V3DMolecule)parent).show(node, x, y);
	}
}
