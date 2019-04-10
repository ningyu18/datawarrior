package org.openmolecules.fx.viewer3d;

import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;

public class NonRotatingLabel extends Label implements TransformationListener {
	private static final String FONT_NAME = "Tahoma";
	private static final int FONT_SIZE = 40;
	private static final double SCALE = 0.012;

	private static Font sFont;

	private Parent mParent;
	private Point3D mP1,mP2;
	private double mWidth, mHeight;

	/**
	 * Create a correctly sized and positioned label.
	 * @parem parent
	 * @param text
	 * @param p1
	 * @param p2
	 * @param color
	 */
	private NonRotatingLabel(Parent parent, String text, Point3D p1, Point3D p2, Color color) {
		super(text);

		mParent = parent;
		mP1 = p1;
		mP2 = p2;

		if (sFont == null)
			sFont = Font.font(FONT_NAME, FONT_SIZE);

		Text t = new Text(text);
		t.setFont(sFont);
		Bounds bounds = t.getLayoutBounds();
		mWidth = bounds.getWidth() * SCALE;
		mHeight = bounds.getHeight() * SCALE;

		if (color == null)
			color = Color.AQUA;

		setFont(sFont);
		setTextFill(color);

		getTransforms().add(Transform.scale(SCALE, SCALE, 0, 0));

		updatePosition();
		}

	public static NonRotatingLabel create(Parent parent, String text, Point3D p1, Point3D p2, Color color) {
		NonRotatingLabel label = new NonRotatingLabel(parent, text, p1, p2, color);

		while (parent.getParent() != null) {
			if (parent instanceof RotatableGroup)
				((RotatableGroup) parent).addRotationListener(label);
			parent = parent.getParent();
			}

		((Group)parent).getChildren().add(label);
		return label;
		}

	public void remove(Parent parent) {
		while (parent.getParent() != null) {
			if (parent instanceof RotatableGroup)
				((RotatableGroup) parent).removeRotationListener(this);
			parent = parent.getParent();
			}

		((Group)parent).getChildren().remove(this);
		}

	private void updatePosition() {
		Point3D p = mParent.localToScene(mP1.midpoint(mP2));
		setTranslateX(p.getX() - mWidth/2);
		setTranslateY(p.getY() - mHeight/2);
		setTranslateZ(p.getZ() - 0.5);
		}

	@Override
	public void transformationChanged() {
		updatePosition();
		}
	}
