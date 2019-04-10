package org.openmolecules.fx.viewer3d;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.openmolecules.chem.conf.render.MoleculeArchitect;
import org.openmolecules.fx.sunflow.RayTraceDialog;
import org.openmolecules.fx.sunflow.RayTraceOptions;
import org.openmolecules.fx.surface.ClipSurfaceCutter;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.mesh.MoleculeSurfaceMesh;

import java.util.Optional;

public class V3DPopupMenu extends ContextMenu {
	private static final boolean DEFAULT_USE_WHEEL_FOR_CLIPPING = false;
	private static final double CLIP_STEEPNESS = 2.5;
	private static final double MIN_CLIP = V3DScene.CAMERA_NEAR_CLIP;
	private static final double MAX_CLIP = Math.min(100, V3DScene.CAMERA_FAR_CLIP);

	private static RayTraceOptions sPreviousMoleculeRayTraceOptions,sPreviousSceneRayTraceOptions;
	private static V3DPopupMenu sPopupMenu;
	protected static boolean sUseMouseWheelForClipping = DEFAULT_USE_WHEEL_FOR_CLIPPING;

	private V3DMolecule mMolecule;

	public V3DPopupMenu(V3DScene scene, V3DMolecule fxmol) {
		if (sPopupMenu != null && sPopupMenu.isShowing()) {
			sPopupMenu.hide();
		}

		sPopupMenu = this;

		mMolecule = fxmol;

		MenuItem itemCut = new MenuItem("Cut Molecule");
		itemCut.setDisable(fxmol == null);
		itemCut.setOnAction(e -> scene.cut(fxmol));

		MenuItem itemCopy3D = new MenuItem("Copy Molecule 3D");
		itemCopy3D.setDisable(fxmol == null);
		itemCopy3D.setOnAction(e -> scene.copy(fxmol));

		MenuItem itemCopy2D = new MenuItem("Copy Molecule 2D");
		itemCopy2D.setDisable(fxmol == null);
		itemCopy2D.setOnAction(e -> scene.copy2D(fxmol));

		MenuItem itemPaste = new MenuItem("Paste Molecule");
		itemPaste.setOnAction(e -> scene.paste());

		MenuItem itemDelete = new MenuItem("Delete Molecule");
		itemDelete.setDisable(fxmol == null);
		itemDelete.setOnAction(e -> scene.delete(fxmol));

		MenuItem itemClear = new MenuItem("Clear All");
		itemClear.setOnAction(e -> scene.clearAll());

		Menu menuClipboard = new Menu("Edit");
		menuClipboard.getItems().addAll(itemCut, itemCopy3D, itemCopy2D, itemPaste, itemDelete, new SeparatorMenuItem(), itemClear);
		getItems().add(menuClipboard);

		if (fxmol != null) {
			getItems().add(new SeparatorMenuItem());

			RadioMenuItem modeBallAndSticks = new RadioMenuItem("Ball And Sticks");
			modeBallAndSticks.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS);
			modeBallAndSticks.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS));
			RadioMenuItem modeBalls = new RadioMenuItem("Balls");
			modeBalls.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALLS);
			modeBalls.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_BALLS));
			RadioMenuItem modeSticks = new RadioMenuItem("Sticks");
			modeSticks.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS);
			modeSticks.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_STICKS));
			RadioMenuItem modeWires = new RadioMenuItem("Wires");
			modeWires.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_WIRES);
			modeWires.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_WIRES));
			Menu menuMode = new Menu("Molecule Style");
			menuMode.getItems().addAll(modeBallAndSticks, modeBalls, modeSticks, modeWires);
			getItems().add(menuMode);

			Menu menuColor = new Menu("Molecule Color");
			RadioMenuItem colorNone = new RadioMenuItem("By Atomic No");
			colorNone.setSelected(fxmol.getColor() == null);
			colorNone.setOnAction(e -> mMolecule.setColor(null, true));
			ColorPicker molColorPicker = new ColorPicker(fxmol.getColor());
			molColorPicker.setOnAction(t -> { fxmol.setColor(molColorPicker.getValue(), true); hide(); });
			CustomMenuItem colorExplicit = new CustomMenuItem(molColorPicker);
			colorExplicit.setHideOnClick(false);
			menuColor.getItems().addAll(colorNone, colorExplicit);
			getItems().add(menuColor);

			for (int i=0; i<MoleculeSurfaceMesh.SURFACE_TYPE.length; i++) {
				final int type = i;
				RadioMenuItem surfaceNone = new RadioMenuItem("None");
				surfaceNone.setSelected(fxmol.getSurfaceMode(type) == V3DMolecule.SURFACE_NONE);
				surfaceNone.setOnAction(e -> fxmol.setSurfaceMode(type, V3DMolecule.SURFACE_NONE));
				RadioMenuItem surfaceMesh = new RadioMenuItem("Triangles");
				surfaceMesh.setSelected(fxmol.getSurfaceMode(type) == V3DMolecule.SURFACE_WIRES);
				surfaceMesh.setOnAction(e -> fxmol.setSurfaceMode(type, V3DMolecule.SURFACE_WIRES));
				RadioMenuItem surfaceOpaque = new RadioMenuItem("Filled");
				surfaceOpaque.setSelected(fxmol.getSurfaceMode(type) == V3DMolecule.SURFACE_FILLED);
				surfaceOpaque.setOnAction(e -> fxmol.setSurfaceMode(type, V3DMolecule.SURFACE_FILLED));

				RadioMenuItem surfaceColorInherit = new RadioMenuItem("From Molecule");
				surfaceColorInherit.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_INHERIT);
				surfaceColorInherit.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_INHERIT));
				RadioMenuItem surfaceColorPlain = new RadioMenuItem("Plain Color");
				surfaceColorPlain.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_PLAIN);
				surfaceColorPlain.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_PLAIN));
				RadioMenuItem surfaceColorAtoms = new RadioMenuItem("By Polarity");
				surfaceColorAtoms.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_POLARITY);
				surfaceColorAtoms.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_POLARITY));
				RadioMenuItem surfaceColorNegativity = new RadioMenuItem("By Donors & Acceptors");
				surfaceColorNegativity.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_DONORS_ACCEPTORS);
				surfaceColorNegativity.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_DONORS_ACCEPTORS));
				RadioMenuItem surfaceColorByAtomicNo = new RadioMenuItem("By Atomic-No");
				surfaceColorByAtomicNo.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS);
				surfaceColorByAtomicNo.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS));

				ColorPicker colorPicker = new ColorPicker(fxmol.getSurfaceColor(type));
				colorPicker.setOnAction(t -> { fxmol.setSurfaceColor(type, colorPicker.getValue()); hide(); });
				CustomMenuItem colorItem = new CustomMenuItem(colorPicker);
				colorItem.setHideOnClick(false);

				Menu menuSurfaceColor = new Menu("Surface Color");
				menuSurfaceColor.getItems().addAll(surfaceColorInherit, surfaceColorPlain, surfaceColorAtoms, surfaceColorNegativity, surfaceColorByAtomicNo, colorItem);

				Slider sliderTransparency = createSlider(0.0, 0.9, fxmol.getSurfaceTransparency(type));
				sliderTransparency.valueProperty().addListener(newValue -> fxmol.setSurfaceTransparency(type, ((DoubleProperty) newValue).doubleValue()));

				VBox transparencyPane = new VBox();
				transparencyPane.setPadding(new Insets(4, 4, 4, 4));
				transparencyPane.getChildren().addAll(sliderTransparency);
				CustomMenuItem transparencyItem = new CustomMenuItem(transparencyPane, false);
				Menu menuTransparency = new Menu("Transparency");
				menuTransparency.getItems().addAll(transparencyItem);

				MenuItem itemRemoveInvisible = new MenuItem("Remove Invisible Part");
				itemRemoveInvisible.setOnAction(e -> {
					float nearClip = (float) (scene.getCamera().getTranslateZ() + scene.getCamera().getNearClip());
					float farClip = (float) (scene.getCamera().getTranslateZ() + scene.getCamera().getFarClip());
					fxmol.cutSurface(type, new ClipSurfaceCutter(nearClip, farClip, fxmol));
				} );

				Menu menuSurface = new Menu(MoleculeSurfaceMesh.SURFACE_TYPE[type]+" Surface");
				menuSurface.getItems().addAll(surfaceNone, surfaceMesh, surfaceOpaque, new SeparatorMenuItem(),
						menuSurfaceColor, menuTransparency, new SeparatorMenuItem(), itemRemoveInvisible);
				getItems().add(menuSurface);
			}

			RadioMenuItem measurementsNone = new RadioMenuItem("None");
			measurementsNone.setSelected(fxmol.getMeasurementMode() == V3DMolecule.MEASUREMENT.NONE);
			measurementsNone.setOnAction(e -> fxmol.setMeasurementMode(V3DMolecule.MEASUREMENT.NONE));
			RadioMenuItem measurementsDistance = new RadioMenuItem("Distance");
			measurementsDistance.setSelected(fxmol.getMeasurementMode() == V3DMolecule.MEASUREMENT.DISTANCE);
			measurementsDistance.setOnAction(e -> fxmol.setMeasurementMode(V3DMolecule.MEASUREMENT.DISTANCE));
			RadioMenuItem measurementsAngle = new RadioMenuItem("Angle");
			measurementsAngle.setSelected(fxmol.getMeasurementMode() == V3DMolecule.MEASUREMENT.ANGLE);
			measurementsAngle.setOnAction(e -> fxmol.setMeasurementMode(V3DMolecule.MEASUREMENT.ANGLE));
			RadioMenuItem measurementsDihedral = new RadioMenuItem("Torsion");
			measurementsDihedral.setSelected(fxmol.getMeasurementMode() == V3DMolecule.MEASUREMENT.TORSION);
			measurementsDihedral.setOnAction(e -> fxmol.setMeasurementMode(V3DMolecule.MEASUREMENT.TORSION));
			MenuItem measurementsRemoveAll = new MenuItem("Remove All");
			measurementsRemoveAll.setOnAction(e -> fxmol.removeMeasurements());
			Menu menuMeasurements = new Menu("Measurements");
			menuMeasurements.getItems().addAll(measurementsNone, measurementsDistance, measurementsAngle, measurementsDihedral, new SeparatorMenuItem(), measurementsRemoveAll);
			getItems().add(menuMeasurements);

			getItems().add(new SeparatorMenuItem());
			MenuItem itemHide = new MenuItem("Hide Molecule");
			itemHide.setOnAction(e -> fxmol.setVisible(false));
			getItems().add(itemHide);
		}
		else {
			getItems().add(new SeparatorMenuItem());
			MenuItem itemHideAll = new MenuItem("Hide All Molecules");
			itemHideAll.setOnAction(e -> scene.setAllVisible(false));
			getItems().add(itemHideAll);

			MenuItem itemShowAll = new MenuItem("Show All Molecules");
			itemShowAll.setOnAction(e -> scene.setAllVisible(true));
			getItems().add(itemShowAll);
		}

		getItems().add(new SeparatorMenuItem());

		Slider slider1 = createSlider(0, CLIP_STEEPNESS, clipValueToSlider(scene.getCamera().nearClipProperty().get()));
		slider1.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				double newClipStart = sliderToClipValue(newValue.doubleValue());
				double thickness = scene.getCamera().farClipProperty().getValue()
								 - scene.getCamera().nearClipProperty().getValue();
				double newFarClip = Math.min(V3DScene.CAMERA_FAR_CLIP, newClipStart + thickness);
				scene.getCamera().nearClipProperty().setValue(newClipStart);
				scene.getCamera().farClipProperty().setValue(newFarClip);
			}
		});

		double oldThickness = Math.min(MAX_CLIP, scene.getCamera().farClipProperty().get()
				- scene.getCamera().nearClipProperty().get());
		Slider slider2 = createSlider(0, CLIP_STEEPNESS, clipThicknessToSlider(oldThickness));
		slider2.valueProperty().addListener(newValue -> {
			double slider = ((DoubleProperty)newValue).doubleValue();
			if (slider > 0.99 * CLIP_STEEPNESS) {
				scene.getCamera().farClipProperty().setValue(V3DScene.CAMERA_FAR_CLIP);
			}
			else {
				double newThickness = sliderToClipThickness(slider);
				double clipStart = scene.getCamera().nearClipProperty().getValue();
				double newFarClip = Math.min(V3DScene.CAMERA_FAR_CLIP, clipStart + newThickness);
				scene.getCamera().farClipProperty().setValue(newFarClip);
			}
		});

		CheckMenuItem useWheelForClipping = new CheckMenuItem("Use Mouse Wheel For Clipping");
		useWheelForClipping.setSelected(sUseMouseWheelForClipping);
		useWheelForClipping.setOnAction(e -> sUseMouseWheelForClipping = useWheelForClipping.isSelected());

		VBox sliderPane = new VBox();
		sliderPane.setPadding(new Insets(4, 4, 4, 4));
		sliderPane.setSpacing(8);
		sliderPane.getChildren().addAll(new Label("Front of Visible Region"), slider1, new Label("Depth Of Visible Region"), slider2);
		CustomMenuItem sliderItem = new CustomMenuItem(sliderPane, false);
		Menu menuClippingPlanes = new Menu("Clipping Planes");
		menuClippingPlanes.getItems().addAll(sliderItem, useWheelForClipping);
		getItems().add(menuClippingPlanes);

		getItems().add(new SeparatorMenuItem());
		MenuItem itemRayTraceMol = new MenuItem("Of This Molecule...");
		itemRayTraceMol.setOnAction(e -> showMoleculeRayTraceDialog(scene));
		itemRayTraceMol.setDisable(fxmol == null);
		MenuItem itemRaytraceScene = new MenuItem("Of Entire Scene...");
		itemRaytraceScene.setOnAction(e -> showSceneRayTraceDialog(scene));
		Menu menuRaytrace = new Menu("Photo-Realistic Image");
		menuRaytrace.getItems().addAll(itemRayTraceMol, itemRaytraceScene);
		getItems().add(menuRaytrace);
	}

	private double clipValueToSlider(double clipValue) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = (MAX_CLIP - en * MIN_CLIP) / (1.0 - en);
		double a = MIN_CLIP - b;
//System.out.println("clipValueToSlider("+clipValue+") : "+(Math.log((clipValue - b) / a)));
		return Math.log((clipValue - b) / a);
	}

	private double sliderToClipValue(double sliderValue) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = (MAX_CLIP - en * MIN_CLIP) / (1.0 - en);
		double a = MIN_CLIP - b;
//System.out.println("sliderToClipValue("+sliderValue+") : "+(a * Math.exp(sliderValue) + b));
		return a * Math.exp(sliderValue) + b;
	}

	private double clipThicknessToSlider(double clipThickness) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = MAX_CLIP / (1.0 - en);
		double a = -b;
//System.out.println("clipThicknessToSlider("+clipThickness+") : "+(Math.log((clipThickness - b) / a)));
		return Math.log((clipThickness - b) / a);
	}

	private double sliderToClipThickness(double sliderValue) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = MAX_CLIP / (1.0 - en);
		double a = -b;
//System.out.println("sliderToClipThickness("+sliderValue+") : "+(a * Math.exp(sliderValue) + b));
		return a * Math.exp(sliderValue) + b;
	}

	/**
	 * Displays a raytrace dialog for rendering one molecule
	 */
	private void showMoleculeRayTraceDialog(V3DScene scene) {
		Optional<RayTraceOptions> result = new RayTraceDialog(scene.getScene().getWindow(), sPreviousMoleculeRayTraceOptions, mMolecule).showAndWait();
		result.ifPresent(options -> {
			sPreviousMoleculeRayTraceOptions = options;
			double cameraDistance = -scene.getCamera().getTranslateZ();
			double fieldOfView = scene.getFieldOfView();
			options.rayTraceInit(cameraDistance, fieldOfView);
			options.addMolecule(mMolecule);
			options.rayTraceStart(scene.getScene().getWindow());
		} );
	}

	private void showSceneRayTraceDialog(V3DScene scene) {
		Optional<RayTraceOptions> result = new RayTraceDialog(scene.getScene().getWindow(), sPreviousSceneRayTraceOptions, null).showAndWait();
		result.ifPresent(options -> {
			sPreviousSceneRayTraceOptions = options;
			double cameraDistance = -scene.getCamera().getTranslateZ();
			double fieldOfView = scene.getFieldOfView();
			if (((PerspectiveCamera)scene.getCamera()).isVerticalFieldOfView())
				fieldOfView *= scene.getWidth() / scene.getHeight();
			options.rayTraceInit(cameraDistance, fieldOfView);
			for (Node node:scene.getWorld().getChildren())
				if (node instanceof V3DMolecule)
					if (node.isVisible())
						options.addMolecule((V3DMolecule)node);
			options.rayTraceStart(scene.getScene().getWindow());
		} );
	}

	private Slider createSlider(double min, double max, double value) {
		Slider slider = new Slider(min, max, value);
		slider.setPrefWidth(200);
		slider.setShowTickMarks(true);
		slider.setValue(value);
		return slider;
	}
}
