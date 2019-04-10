package com.actelion.research.datawarrior;

import com.actelion.research.chem.conf.Conformer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneWithSidePane;
import org.openmolecules.mesh.MoleculeSurfaceMesh;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class FXConformerPanel extends JFXPanel {
	private V3DScene mScene;

	public FXConformerPanel() {
		Platform.runLater(() -> {
			V3DSceneWithSidePane sceneWithSidePanel = new V3DSceneWithSidePane(512, 384);
			sceneWithSidePanel.getMoleculePanel().setShowStructure(false);
			mScene = sceneWithSidePanel.getScene3D();
			mScene.setIndividualRotationModus(true);

			String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
			Scene scene = new Scene(sceneWithSidePanel, 512, 384, true, SceneAntialiasing.BALANCED);
			scene.getStylesheets().add(css);
			mScene.widthProperty().bind(scene.widthProperty());
			mScene.heightProperty().bind(scene.heightProperty());
			setScene(scene);
		} );
	}

	public void clear() {
		Platform.runLater(() -> mScene.clearAll() );
	}

	public void setConformerSplitting(double value) {
		Platform.runLater(() -> {
			int count = 0;
			double molSize = 0;
			for (Node node:mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					molSize = Math.max(molSize, 2 * Math.sqrt(((V3DMolecule)node).getConformer().getSize()));
					count++;
				}
			}

			double maxLineWidth = 0.01 + molSize * Math.max(2.0, Math.round(1.2 * Math.sqrt(count)));
			int maxPerLine = (value == 0) ? count : (int)(1.0 + (maxLineWidth - molSize) / (value * molSize));
			int lineCount = (count == 0) ? 0 : 1 + (count - 1) / maxPerLine;
			int countPerLine = (count == 0) ? 0 : 1 + (count - 1) / lineCount;

			double x = 0.5;
			double y = 0.5;
			for (Node node:mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					node.setTranslateX((x-(double)countPerLine/2) * molSize * value);
					node.setTranslateY((y-(double)lineCount/2) * molSize);
					x += 1;
					if (x > countPerLine) {
						x = 0.5;
						y += 1.0;
					}
				}
			}
		} );
	}

	public void setConollySurfaceMode(int mode) {
		Platform.runLater(() -> {
		for (Node node:mScene.getWorld().getChildren())
			if (node instanceof V3DMolecule)
				((V3DMolecule)node).setSurfaceMode(MoleculeSurfaceMesh.CONNOLLY, mode);
		} );
	}

	public void addMolecule(Conformer conformer, Color color) {
		Platform.runLater(() -> {
			V3DMolecule fxmol = new V3DMolecule(conformer);
			if (color != null) {
				fxmol.setColor(color, true);
				fxmol.setSurfaceColorMode(MoleculeSurfaceMesh.CONNOLLY, SurfaceMesh.SURFACE_COLOR_INHERIT);
				mScene.addMolecule(fxmol);
			}
		} );
	}

	public ArrayList<Conformer> getConformers() {
		final ArrayList<Conformer> conformerList = new ArrayList<>();


		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			for (Node node:mScene.getWorld().getChildren())
				if (node instanceof V3DMolecule)
					conformerList.add(((V3DMolecule)node).getConformer());
			latch.countDown();
		} );

		try { latch.await(); } catch (InterruptedException ie) {}
		return conformerList;
	}
}
