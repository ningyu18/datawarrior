package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.openmolecules.chem.conf.gen.ConformerGenerator;

import java.util.ArrayList;

public class V3DScene extends SubScene {
	private ClipboardHandler mClipboardHandler;
	private Group mRoot;                  	// not rotatable, contains light and camera
	private RotatableGroup mUniverse;		// rotatable, not movable, root in center of scene, contains all visible objects
	private RotatableGroup mWorld;		    // rotatable & movable, contains all visible objects
	private V3DMouseHandler mMouseHandler;
	private V3DKeyHandler mKeyHandler;
	private V3DSceneListener mSceneListener;
	private boolean mIsIndividualRotationModus;

	public static final Color SELECTION_COLOR = Color.TURQUOISE;
	protected static final double CAMERA_INITIAL_DISTANCE = 45;
	protected static final double CAMERA_FIELD_OF_VIEW = 30.0;	// default field of view
	protected static final double CAMERA_NEAR_CLIP = 10.0;
	protected static final double CAMERA_FAR_CLIP = 250.0;
	protected static final double CAMERA_MIN_CLIP_THICKNESS = 2;


	public V3DScene(Group root, double width, double height) {
		super(root , width, height, true, SceneAntialiasing.BALANCED);
		mRoot = root;

		mUniverse = new RotatableGroup();
		mWorld = new RotatableGroup();

		mRoot.getChildren().add(mUniverse);
		mUniverse.getChildren().add(mWorld);
		mRoot.setDepthTest(DepthTest.ENABLE);

		// gradients work well in a Scene, but don't seem to work in SubScenes
//		Stop[] stops = new Stop[] { new Stop(0, Color.MIDNIGHTBLUE), new Stop(1, Color.MIDNIGHTBLUE.darker().darker().darker())};
//		LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
		setFill(Color.MIDNIGHTBLUE.darker().darker());

		buildLight();
		buildCamera();

		mMouseHandler = new V3DMouseHandler(this);
		mKeyHandler = new V3DKeyHandler(this);
		mClipboardHandler = new ClipboardHandler();
		}

	public void setSceneListener(V3DSceneListener sl) {
		mSceneListener = sl;
		}

	public boolean isIndividualRotationModus() {
		return mIsIndividualRotationModus;
		}

	public void setIndividualRotationModus(boolean b) {
		mIsIndividualRotationModus = b;
		}

	public void cut(V3DMolecule fxmol) {
		copy(fxmol);
		delete(fxmol);
		}

	public void copy(V3DMolecule fxmol) {
		mClipboardHandler.copyMolecule(fxmol.getConformer().toMolecule(null));
		}

	public void copy2D(V3DMolecule fxmol) {
		Conformer conformer = fxmol.getConformer();
		StereoMolecule mol = conformer.getMolecule().getCompactCopy();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			Point2D p = fxmol.localToScreen(conformer.getX(atom), conformer.getY(atom), conformer.getZ(atom));
			mol.setAtomX(atom, p.getX());
			mol.setAtomY(atom, p.getY());
			mol.setAtomZ(atom, 0);
			}
		mClipboardHandler.copyMolecule(mol);
		}

	public void paste() {
		StereoMolecule mol = mClipboardHandler.pasteMolecule(false);
		if (mol == null) {   // TODO interactive message
			System.out.println("No molecule on clipboard!");
			return;
			}

		boolean is3D = false;
		for (int atom=1; atom<mol.getAllAtoms(); atom++) {
			if (Math.abs(mol.getAtomZ(atom) - mol.getAtomZ(0)) > 0.1) {
				is3D = true;
				break;
				}
			}

		Conformer conformer = null;
		if (is3D) {
			conformer = new Conformer(mol);
			}
		else {
			conformer = new ConformerGenerator().getOneConformer(mol);
			conformer.toMolecule(mol);	// copy atom coordinates to molecule as well
			}
		if (conformer == null) {    // TODO interactive message
			System.out.println("Conformer generation failed!");
			return;
			}

		V3DMolecule fxmol = new V3DMolecule(conformer);
		fxmol.activateEvents();
		addMolecule(fxmol);
		}

	public void delete(V3DMolecule fxmol) {
		fxmol.removeMeasurements();
		fxmol.deactivateEvents();
		mWorld.getChildren().remove(fxmol);
		if (mSceneListener != null)
			mSceneListener.removeMolecule(fxmol);
		}

	public void deleteInvisibleMolecules() {
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule && !node.isVisible())
				list.add((V3DMolecule) node);
		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void deleteAllMolecules() {
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule)
				list.add((V3DMolecule) node);
		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void setAllVisible(boolean visible) {
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule)
				node.setVisible(visible);
		}

	public void clearAll() {
		for (Node node:mWorld.getChildren()) {
			if (node instanceof V3DMolecule) {
				((V3DMolecule) node).removeMeasurements();
				if (mSceneListener != null)
					mSceneListener.removeMolecule((V3DMolecule) node);
			}
		}
		mWorld.getChildren().clear();	// this does not remove the measurements
	}

	public void addAxis(Group axis) {
		mWorld.getChildren().add(axis);
		mKeyHandler.setAxis(axis);
		}

	public void addMolecule(V3DMolecule fxmol) {
		mWorld.getChildren().add(fxmol);
		if (mSceneListener != null)
			mSceneListener.addMolecule(fxmol);
		}

/*	public double getDistanceToScreenFactor(double z) {
		PerspectiveCamera camera = (PerspectiveCamera)getCamera();
		double fieldOfView = camera.getFieldOfView();
		double screenSize = camera.isVerticalFieldOfView() ? getHeight() : getWidth();
		double sizeAtZ0 = -camera.getTranslateZ() * Math.tan(Math.PI*fieldOfView/90);
		return 20;	// TODO calculate something reasonable
		}*/

	public RotatableGroup getUniverse() {
		return mUniverse;
		}

	public RotatableGroup getWorld() {
		return mWorld;
		}

	public double getFieldOfView() {
		return ((PerspectiveCamera)getCamera()).getFieldOfView();
		}

	/**
	 * @param polygon
	 * @param mode 0: normal, 1:add, 2:subtract
	 * @param paneOnScreen top let point of parent pane on screen
	 */
	public void select(Polygon polygon, int mode, Point2D paneOnScreen) {
		for (Node node:mWorld.getChildren())
			if (node instanceof V3DMolecule)
				((V3DMolecule) node).select(polygon, mode, paneOnScreen);
		}

	/**
	 * @param mol3D
	 * @param mode 0: normal, 1:add, 2:subtract
	 */
	public void selectMolecule(V3DMolecule mol3D, int mode) {
		if (mode == 0) {
			for (Node node : mWorld.getChildren())
				if (node instanceof V3DMolecule)
					((V3DMolecule)node).select(node == mol3D);
			}
		else {
			if (mol3D != null)
				mol3D.select(mode == 1);
			}
		}

	public void moveCamera(double dz) {
		getCamera().setTranslateZ(getCamera().getTranslateZ() + dz);
		}

	private void buildLight() {
		AmbientLight light1=new AmbientLight(new Color(0.3, 0.3, 0.3, 1.0));
		light1.getScope().addAll(mWorld);

		PointLight light2=new PointLight(new Color(0.8, 0.8, 0.8, 1.0));
		light2.setTranslateX(-100);
		light2.setTranslateY(-100);
		light2.setTranslateZ(-200);
		light2.getScope().addAll(mWorld);

		Group lightGroup = new Group();
		lightGroup.getChildren().addAll(light1, light2);
		mRoot.getChildren().addAll(lightGroup);
		}

	private void buildCamera() {
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(-CAMERA_INITIAL_DISTANCE);
		setCamera(camera);
		mRoot.getChildren().add(camera);
		}
	}
