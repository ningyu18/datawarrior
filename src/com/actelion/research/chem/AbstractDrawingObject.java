/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.chem;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public abstract class AbstractDrawingObject {
	protected static final String DESCRIPTOR_START = "<DrawingObject";
	protected static final String DESCRIPTOR_END = "></DrawingObject>";
	protected static final String DESCRIPTOR_TYPE = " type=\"";

    protected static final Color SELECTION_COLOR = UIManager.getColor("TextArea.selectionBackground");

	protected Point2D.Double[]	mPoint;
	protected boolean			mIsSelected,mProtectedFromDeletion;

	protected double			mTransformationReferenceX,mTransformationReferenceY;
	protected double			mTransformationValue1[];
	protected double			mTransformationValue2[];

	abstract public void draw(Graphics g, DepictorTransformation t);
	abstract public void draw2D(Graphics2D g, DepictorTransformation t);
	abstract public void hilite(Graphics g);
	abstract public void clearHiliting();

	/**
	 * Checks, whether this drawing object contains the point at x,y
	 * @param x
	 * @param y
	 * @return
	 */
	abstract public boolean contains(double x, double y);

	abstract public boolean checkHiliting(double x, double y);
	abstract public AbstractDrawingObject clone();

	/**
	 * Creates a string encoding all properties specific to this drawing object.
	 * Its type is not part of the descriptor detail. The detail must start, but not
	 * end with a space. Example: ' size="12" x="50.0" y="80.0"'
	 * @return concatenated property list, each property with a leading space
	 */
	abstract public String getDescriptorDetail();
	abstract public String getTypeString();

        
	public static AbstractDrawingObject instantiate(String descriptor) {
		return DrawingObjectFactory.createObject(descriptor);
		}

	public void move(double dx, double dy) {
		if (mPoint != null) {
			for (int i=0; i<mPoint.length; i++) {
				mPoint[i].x += dx;
				mPoint[i].y += dy;
				}
			}
		}

	public void scale(double f) {
		if (mPoint != null) {
			for (int i=0; i<mPoint.length; i++) {
				mPoint[i].x *= f;
				mPoint[i].y *= f;
				}
			}
		}

	public boolean isDeletable() {
		return !mProtectedFromDeletion;
		}

	public void setDeletable(boolean d) {
		mProtectedFromDeletion = !d;
		}

	public boolean isSelected() {
		return mIsSelected;
		}

	public void setSelected(boolean s) {
		mIsSelected = s;
		}

	public Rectangle2D.Double getBoundingRect() {
		if (mPoint == null)
			return null;

		Rectangle2D.Double bounds = new Rectangle2D.Double();
		bounds.x = mPoint[0].x;
		bounds.y = mPoint[0].y;

		for (int i=1; i<mPoint.length; i++) {
			if (bounds.x > mPoint[i].x) {
				bounds.width += bounds.x - mPoint[i].x;
				bounds.x = mPoint[i].x;
				}
			else if (bounds.width < mPoint[i].x - bounds.x) {
				bounds.width = mPoint[i].x - bounds.x;
				}
			if (bounds.y > mPoint[i].y) {
				bounds.height += bounds.y - mPoint[i].y;
				bounds.y = mPoint[i].y;
				}
			else if (bounds.height < mPoint[i].y - bounds.y) {
				bounds.height = mPoint[i].y - bounds.y;
				}
			}

		return bounds;
		}

	public boolean isSurroundedBy(Shape shape) {
		if (mPoint == null)
			return false;

		for (int i=0; i<mPoint.length; i++)
			if (!shape.contains(mPoint[i]))
				return false;

		return true;
		}

	public void translateInit(double x, double y) {
		mTransformationReferenceX = x;
		mTransformationReferenceY = y;
		if (mPoint != null) {
			int pointCount = mPoint.length;
			mTransformationValue1 = new double[pointCount];
			mTransformationValue2 = new double[pointCount];
			for (int i=0; i<pointCount; i++) {
				mTransformationValue1[i] = mPoint[i].x;
				mTransformationValue2[i] = mPoint[i].y;
				}
			}
		}

	public void translate(double x, double y) {
			// overwrite this if only hilited parts of the object shall be moved
		if (mPoint != null) {
			for (int i=0; i<mPoint.length; i++) {
				mPoint[i].x = mTransformationValue1[i] + x - mTransformationReferenceX;
				mPoint[i].y = mTransformationValue2[i] + y - mTransformationReferenceY;
				}
			}
		}

	public void zoomAndRotateInit(double x, double y) {
		mTransformationReferenceX = x;
		mTransformationReferenceY = y;
		if (mPoint != null) {
			int pointCount = mPoint.length;
			mTransformationValue1 = new double[pointCount];
			mTransformationValue2 = new double[pointCount];
			for (int i=0; i<pointCount; i++) {
				double dx = x - mPoint[i].x;
				double dy = y - mPoint[i].y;
				mTransformationValue2[i] = Math.sqrt(dx*dx+dy*dy);	// distance to center of gravity
				mTransformationValue1[i] = Molecule.getAngle(x,y,mPoint[i].x,mPoint[i].y);
				}
			}
		}

	public void zoomAndRotate(double zoom, double angle) {
		if (mPoint != null) {
			for (int i=0; i<mPoint.length; i++) {
				double newDistance = mTransformationValue2[i] * zoom;
				double newAngle = mTransformationValue1[i] - angle;
				mPoint[i].x = mTransformationReferenceX + newDistance*Math.sin(newAngle);
				mPoint[i].y = mTransformationReferenceY + newDistance*Math.cos(newAngle);
				}
			}
		}

	public String getDescriptor() {
		return DESCRIPTOR_START+" type=\""+getTypeString()+"\""+getDescriptorDetail()+DESCRIPTOR_END;
		}

	public String toString() {
		StringBuffer objectString = new StringBuffer();
        objectString.append(getDescriptor());
		return objectString.toString();
		}

}
