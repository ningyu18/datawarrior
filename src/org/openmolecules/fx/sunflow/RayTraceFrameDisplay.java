package org.openmolecules.fx.sunflow;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.image.Color;

import java.awt.*;

public class RayTraceFrameDisplay implements Display {
	private String filename,format;
	private FXRenderFrame frame;
	private Window owner;
	private String sceneName;

	public RayTraceFrameDisplay(Window owner, String sceneName) {
		this(owner, sceneName, null, null);
	}

	public RayTraceFrameDisplay(Window owner, String sceneName, String filename, String format) {
		this.owner = owner;
		this.sceneName = sceneName;
		this.filename = filename;
		this.format = format;
		frame = null;
	}

	/**
	 * Had to encapsulate imageBegin in runLater(), because the JavaFX is very
	 * picky about GUI manipulations not in application thread. TLS 8Jan2016
	 * @param w width of the rendered image in pixels
	 * @param h height of the rendered image in pixels
	 * @param bucketSize size of the buckets in pixels
	 */
	public void imageBegin(int w, int h, int bucketSize) {
		Platform.runLater(() -> {
			if (frame == null) {
				frame = new FXRenderFrame(owner, w, h);
				frame.imagePanel.setImageName(sceneName);
				frame.imagePanel.imageBegin(w, h, bucketSize);
				frame.show();
			} else
				frame.imagePanel.imageBegin(w, h, bucketSize);
		} );
	}

	public void imagePrepare(int x, int y, int w, int h, int id) {
		Platform.runLater(() -> frame.imagePanel.imagePrepare(x, y, w, h, id));
	}

	public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
		Platform.runLater(() -> frame.imagePanel.imageUpdate(x, y, w, h, data, alpha));
	}

	public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
		Platform.runLater(() -> frame.imagePanel.imageFill(x, y, w, h, c, alpha));
	}

	public void imageEnd() {
		Platform.runLater(() -> {
			frame.imagePanel.imageEnd();
			if (filename != null)
				frame.imagePanel.save(filename, format);
		} );
	}

	@Override
	public boolean imageCancelled() {
		return frame.isClosed();
	}

	@SuppressWarnings("serial")
	private static class FXRenderFrame extends Stage {
		RayTraceImagePanel imagePanel;
		boolean frameIsClosed;

		/**
		 * Creates and shows a new frame to show an image being rendered.
		 * If image height or width is smaller than 50% of screen size,
		 * then the frame size is equal to the image size. Otherwise,
		 * the frame is smaller and the image is scaled. The mouse may be
		 * used to zoom and move the image while it is being rendered.
		 * A popup allows to copy or save the image once rendering is completed.
		 * @param owner
		 * @param width of image
		 * @param height of image
		 */
		FXRenderFrame(Window owner, int width, int height) {
			super();
			initOwner(owner);
			initModality(Modality.NONE);
			setTitle("Rendered By Sunflow v" + SunflowAPI.VERSION);

			Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
			double scale = 1.0;
			if (width > screenRes.getWidth() / 2)
				scale = screenRes.getWidth() / (2 * width);
			if (height > screenRes.getHeight() / 2)
				scale = screenRes.getHeight() / (2 * height);
			if (scale != 1.0) {
				width = (int)(scale * width);
				height = (int)(scale * height);
			}

			setWidth(width);
			setHeight(height);

			addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> frameIsClosed = true);

			imagePanel = new RayTraceImagePanel(width, height);

			Scene scene = new Scene(new Group(imagePanel), width, height);
			setScene(scene);
			sizeToScene();

			imagePanel.widthProperty().bind(scene.widthProperty());
			imagePanel.heightProperty().bind(scene.heightProperty());
		}

		public boolean isClosed() {
			return frameIsClosed;
		}
	}
}