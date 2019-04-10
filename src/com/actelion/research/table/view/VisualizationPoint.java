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

import com.actelion.research.table.model.CompoundRecord;


public class VisualizationPoint {
	public float screenX, screenY, width, height;
	protected int chartGroupIndex, hvIndex;
	protected byte colorIndex, shape, exclusionFlags;
	protected CompoundRecord record;
	protected VisualizationLabelPosition labelPosition;

	protected VisualizationPoint(CompoundRecord r) {
		record = r;
		colorIndex = 0;
		shape = 0;
		hvIndex = 0;
	}

	public VisualizationLabelPosition findLabel(int x, int y) {
		VisualizationLabelPosition lp = labelPosition;
		while (lp != null) {
			if (lp.containsOnScreen(x, y))
				return lp;
			lp = lp.getNext();
		}
		return null;
	}

	public VisualizationLabelPosition getOrCreateLabelPosition(int column) {
		VisualizationLabelPosition lp = labelPosition;
		while (lp != null && lp.getColumn() != column)
			lp = lp.getNext();
		if (lp != null)
			return lp;
		return labelPosition = new VisualizationLabelPosition(column, labelPosition);
	}

	/**
	 * After column deletion this method remaps column indexes of cached label position
	 * and it removes cached label positions of labels that have no columns anymore.
	 * @param oldToNewColumn
	 */
	public void remapLabelPositionColumns(int[] oldToNewColumn) {
		if (labelPosition != null) {
			labelPosition.setColumn(oldToNewColumn[labelPosition.getColumn()]);
			if (labelPosition.getColumn() == -1) {
				labelPosition = labelPosition.getNext();
				remapLabelPositionColumns(oldToNewColumn);
				return;
				}
			VisualizationLabelPosition lp=labelPosition;
			while (lp.getNext() != null) {
				lp.getNext().setColumn(oldToNewColumn[lp.getNext().getColumn()]);
				if (lp.getNext().getColumn() == -1)
					lp.skipNext();
				else
					lp=lp.getNext();
				}
			}
		}

	/**
	 * This method removes all cached non-custom label positions.
	 */
	public void removeNonCustomLabelPositions() {
		while (labelPosition != null && !labelPosition.isCustom())
			labelPosition = labelPosition.getNext();

		if (labelPosition != null) {
			VisualizationLabelPosition lp = labelPosition;
			while (lp.getNext() != null) {
				if (lp.getNext().isCustom())
					lp = lp.getNext();
				else
					lp.skipNext();
				}
			}
		}

	/**
	 * This method removes the cached label position of the given column
	 * @param column
	 */
	public void removeNonCustomLabelPosition(int column) {
		if (labelPosition != null) {
			if (labelPosition.getColumn() == column) {
				if (!labelPosition.isCustom())
					labelPosition = labelPosition.getNext();
				return;
				}
			for (VisualizationLabelPosition lp = labelPosition; lp.getNext() != null; lp = lp.getNext()) {
				if (lp.getNext().getColumn() == column) {
					if (!lp.getNext().isCustom())
						lp.skipNext();
					return;
					}
				}
			}
		}
	}