package org.openmolecules.fx.viewer3d;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

public class Rod extends RotatableGroup {
	private static final float RADIUS = 0.01f;

	public Rod(Point3D p1, Point3D p2, Color color) {
		PhongMaterial material = createMaterial(color, 1.0);

		float distance = (float)p1.distance(p2);

		Cylinder cylinder = new Cylinder(RADIUS, distance);
		cylinder.setMaterial(material);
		getChildren().add(cylinder);

		Point3D center = p1.midpoint(p2);
		setTranslateX(center.getX());
		setTranslateY(center.getY());
		setTranslateZ(center.getZ());

		double angle1 = -180/Math.PI*getAngleXY(p1, p2);
		double angle2 = -180/Math.PI*Math.asin((p1.getZ()-p2.getZ())/distance);

		Transform r = new Rotate(angle1, Rotate.Z_AXIS);
		r = r.createConcatenation(new Rotate(angle2, Rotate.X_AXIS));
		getTransforms().add(r);
	}
}
