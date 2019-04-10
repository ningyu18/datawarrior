package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.conf.Conformer;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.openmolecules.chem.conf.render.MoleculeArchitect;
import org.openmolecules.chem.conf.render.MoleculeBuilder;

import java.util.TreeMap;

public class V3DMoleculeBuilder implements MoleculeBuilder {
	public static final int DEFAULT_MODE = MoleculeArchitect.CONSTRUCTION_MODE_STICKS;

	public static PhongMaterial sTransparentMaterial;
	private static final double STICK_MODE_ATOM_PICK_RADIUS = 0.35;
	private static final double WIRE_MODE_ATOM_PICK_RADIUS = 0.25;
	private static TreeMap<Integer,PhongMaterial> sMaterialMap;
	private MoleculeArchitect mArchitect;
	private V3DMolecule mV3DMolecule;
	private int mSphereDivisions,mCylinderDivisions;

	public V3DMoleculeBuilder(V3DMolecule v3DMolecule) {
		mArchitect = new MoleculeArchitect(this);
		mArchitect.setConstructionMode(v3DMolecule.getConstructionMode());
		calculateDivisions();
		mV3DMolecule = v3DMolecule;
		if (sMaterialMap == null) {
			sMaterialMap = new TreeMap<Integer, PhongMaterial>();
			sTransparentMaterial = new PhongMaterial();
			sTransparentMaterial.setDiffuseColor(Color.rgb(0, 0, 0, 0.0));
			}
		}

	public void setConstructionMode(int mode) {
		mArchitect.setConstructionMode(mode);
		calculateDivisions();
		}

	public void setHydrogenMode(int mode) {
		mArchitect.setHydrogenMode(mode);
		}

/*	public void centerMolecule(Conformer conformer) {
		mArchitect.centerMolecule(conformer);
		}*/

	public void buildMolecule(Conformer conformer) {
		mArchitect.buildMolecule(conformer);

		if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS)	// no atom picking for wires
			for (int i=0; i<conformer.getSize(); i++)
				addTransparentSphere(i, conformer.getCoordinates(i), mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS ?
						STICK_MODE_ATOM_PICK_RADIUS : WIRE_MODE_ATOM_PICK_RADIUS);
		}

	private void addTransparentSphere(int atom, Coordinates c, double radius) {
		Sphere sphere = new Sphere(radius, mSphereDivisions);
		sphere.setMaterial(sTransparentMaterial);
		sphere.setTranslateX(c.x);
		sphere.setTranslateY(c.y);
		sphere.setTranslateZ(c.z);
		sphere.setUserData(new NodeDetail(sTransparentMaterial, atom, -1, false));
		mV3DMolecule.getChildren().add(sphere);
		}

	@Override
	public void init() {}

	@Override
	public void addSphere(int atom, int bond, Coordinates c, double radius, int argb) {
		boolean isOverridable = (argb == MoleculeArchitect.ATOM_ARGB[1]
							  || argb == MoleculeArchitect.ATOM_ARGB[6]);
		PhongMaterial material = getMaterial(argb);
		Sphere sphere = new Sphere(radius, mSphereDivisions);
		sphere.setMaterial(material);
		sphere.setTranslateX(c.x);
		sphere.setTranslateY(c.y);
		sphere.setTranslateZ(c.z);
		sphere.setUserData(new NodeDetail(material, atom, bond, isOverridable));
		mV3DMolecule.getChildren().add(sphere);
		}

	@Override
	public void addCylinder(int bond, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
		boolean isOverridable = (argb == MoleculeArchitect.ATOM_ARGB[1]
							  || argb == MoleculeArchitect.ATOM_ARGB[6]
							  || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR);
		PhongMaterial material = getMaterial(argb);
		Cylinder cylinder = new Cylinder(radius, length, mCylinderDivisions);
		cylinder.setMaterial(material);
		cylinder.setTranslateX(center.x);
		cylinder.setTranslateY(center.y);
		cylinder.setTranslateZ(center.z);

		Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
		Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
		cylinder.getTransforms().add(r2);
		cylinder.getTransforms().add(r1);
		cylinder.setUserData(new NodeDetail(material, -1, bond, isOverridable));
		mV3DMolecule.getChildren().add(cylinder);
		}

	@Override
	public void done() {}

	private void calculateDivisions() {
		mSphereDivisions = (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALLS) ? 64
				   : (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS) ? 32
				   : (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS) ? 16 : 8;
		mCylinderDivisions = (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALLS) ? 10
				: (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS) ? 10
				: (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS) ? 10 : 5;
		}

	private PhongMaterial getMaterial(int argb) {
		PhongMaterial material = sMaterialMap.get(argb);
		if (material == null) {
			Color color = Color.rgb((argb & 0x00FF0000) >> 16, (argb & 0x0000FF00) >> 8, argb & 0x000000FF);
			material = new PhongMaterial();
			material.setDiffuseColor(color.darker());
			material.setSpecularColor(color);
			sMaterialMap.put(argb, material);
			}
		return material;
		}
	}
