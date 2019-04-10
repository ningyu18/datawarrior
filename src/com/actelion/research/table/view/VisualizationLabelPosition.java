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

package com.actelion.research.table.view;

public class VisualizationLabelPosition {
	private float mX,mY,mZ;
	private int mColumn,mScreenX1,mScreenX2,mScreenY1,mScreenY2,mScreenZ;    // screen coords include the retina factor
	private VisualizationLabelPosition mNextInChain;

	/**
	 * Creates a new non-custom VisualizationLabelPosition
	 * @param column
	 * @param nextInChain
	 */
	public VisualizationLabelPosition(int column, VisualizationLabelPosition nextInChain) {
		mColumn = column;
		mNextInChain = nextInChain;
		mX = Float.NaN;	// not custom
	}

	public VisualizationLabelPosition getNext() {
		return mNextInChain;
	}

	public void skipNext() {
		mNextInChain = mNextInChain.mNextInChain;
	}

	public boolean isCustom() {
		return !Float.isNaN(mX);
	}

	public int getColumn() {
		return mColumn;
	}

	public void setColumn(int column) {
		mColumn = column;
	}

	public float getX() {
		return mX;
	}

	public float getY() {
		return mY;
	}

	public float getZ() {
		return mZ;
	}

	public void setXYZ(float x, float y, float z) {
		mX = x;
		mY = y;
		mZ = z;
		}

	/**
	 * @return screen x-coord of label center without retina and AA factors
	 */
	public int getLabelCenterOnScreenX() {
		return (mScreenX1 + mScreenX2) / 2;
	}

	/**
	 * @return screen y-coord of label center without retina and AA factors
	 */
	public int getLabelCenterOnScreenY() {
		return (mScreenY1 + mScreenY2) / 2;
	}

	/**
	 * @return screen x-coord of left edge of label without retina and AA factors
	 */
	public int getScreenX() {
		return mScreenX1;
		}

	/**
	 * @return screen y-coord of top edge of label without retina and AA factors
	 */
	public int getScreenY() {
		return mScreenY1;
		}

	/**
	 * @return screen x-coord of right edge of label without retina and AA factors
	 */
	public int getScreenX2() {
		return mScreenX2;
		}

	/**
	 * @return screen y-coord of bottom edge of label without retina and AA factors
	 */
	public int getScreenY2() {
		return mScreenY2;
		}

	/**
	 * @return screen width of label without retina and AA factors
	 */
	public int getScreenWidth() {
		return mScreenX2 - mScreenX1;
		}

	/**
	 * @return screen height label without retina and AA factors
	 */
	public int _getScreenHeight() {
		return mScreenY2 - mScreenY1;
		}

	/**
	 * @return screen z-coord of label center without retina and AA factors
	 */
	public int getScreenZ() {
		return mScreenZ;
	}

		/**
		 * This sets a new custom position for this label.
		 * x,y,z are data values in the column data range assigned to the axes.
		 * If the label didn't have a custom location before, it has afterwards.
		 * @param x relative x (0...1) in unzoomed view
		 * @param y relative y (0...1) in unzoomed view
		 * @param z relative z (0...1) in unzoomed view (3D-views only)
		 */
	public void updatePosition(float x, float y, float z) {
		mX = x;
		mY = y;
		mZ = z;
	}

	/**
	 * Translates this position by dx and dy on a scale without retina and AA factors
	 * @param dx
	 * @param dy
	 */
	public void translate(int dx, int dy) {
		mScreenX1 += dx;
		mScreenY1 += dy;
		mScreenX2 += dx;
		mScreenY2 += dy;
	}

	/**
	 * Set screen coordinates without retina and AA factors
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param z
	 */
	public void setScreenLocation(int x1, int y1, int x2, int y2, int z) {
		mScreenX1 = x1;
		mScreenY1 = y1;
		mScreenX2 = x2;
		mScreenY2 = y2;
		mScreenZ = z;
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean containsOnScreen(int x, int y) {
		return x >= mScreenX1 && x <= mScreenX2 && y >= mScreenY1 && y <= mScreenY2;
	}
}
