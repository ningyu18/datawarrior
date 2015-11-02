/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
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

package com.actelion.research.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;

public class CompoundTableChemistryCellRenderer extends ChemistryCellRenderer implements ColorizedCellRenderer {
	public static final int ON_THE_FLY_COORD_MAX_ATOMS = 255;
	private static final String[] ON_THE_FLY_COORD_ERROR_MESSAGE = { "Calculate 2D-atom", "coordinates to render", "large molecules!" };

	private VisualizationColor mForegroundColor,mBackgroundColor;
	private boolean	mIsReaction;

	public void setReaction(boolean isReaction) {
		mIsReaction = isReaction;
		}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row, int col) {
		JPanel renderPanel = null;

		if (value != null && value instanceof String) {
			String s = (String)value;
			if (s.length() != 0) {
				if (mIsReaction) {
					value = ReactionEncoder.decode(s, true);
					}
				else if (s.indexOf(' ') == -1 && s.indexOf('\n') == -1) {
					CompoundTableModel tableModel = (CompoundTableModel)table.getModel();
					int idcodeColumn = tableModel.convertFromDisplayableColumnIndex(table.convertColumnIndexToModel(col));
					int coordsColumn = tableModel.getChildColumn(idcodeColumn, CompoundTableModel.cColumnType2DCoordinates);
					CompoundRecord record = tableModel.getRecord(row);
					byte[] idcode = (byte[])record.getData(idcodeColumn);
					byte[] coords = (coordsColumn == -1) ? null : (byte[])record.getData(coordsColumn);
					if (idcode != null && coords == null && new IDCodeParser().getAtomCount(idcode, 0) > ON_THE_FLY_COORD_MAX_ATOMS) {
						renderPanel = getErrorRendererComponent();
						}
					else {
						StereoMolecule mol = new StereoMolecule();
						new IDCodeParser(true).parse(mol, idcode, coords);
						tableModel.colorizeAtoms(record, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_ALL, mol);
						value = mol;
						}
					}
				}
			}

		if (renderPanel == null)
			renderPanel = (JPanel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		if (!isSelected) {
			if (mForegroundColor != null && mForegroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
				CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
				renderPanel.setForeground(mForegroundColor.getDarkerColor(record));
				}

			// Quaqua does not use the defined background color if CellRenderer is translucent
			if (UIManager.getLookAndFeel().getName().startsWith("Quaqua"))
				renderPanel.setOpaque(mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned);

			if (mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
				CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
				renderPanel.setBackground(mBackgroundColor.getLighterColor(record));
				}
			}

		return renderPanel;
		}

	private JPanel getErrorRendererComponent() {
		return new JPanel() {
			private static final long serialVersionUID = 20150417L;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Dimension size = getSize();
				showOnTheFlyAtomCoordsExceededMessage(g, new Rectangle(0, 0, size.width, size.height));
				}
			};
		}

	public static void showOnTheFlyAtomCoordsExceededMessage(Graphics g, Rectangle bounds) {
		if (bounds.width != 0 && bounds.height != 0) {

			int d = g.getFontMetrics().getHeight();
			int w = g.getFontMetrics().stringWidth(ON_THE_FLY_COORD_ERROR_MESSAGE[0]);
			for (int i=1; i<ON_THE_FLY_COORD_ERROR_MESSAGE.length; i++)
				w = Math.max(w, g.getFontMetrics().stringWidth(ON_THE_FLY_COORD_ERROR_MESSAGE[i]));

			int maxWidth = bounds.width * 9 / 10;

			int od = d;
			if ((d > 12 || w > maxWidth) && d > 6)	// reduce
				d = Math.min(12, Math.max(6, d*maxWidth/w));
			else if (d < 12)
				d = Math.min(12, d*maxWidth/w);

			int x = bounds.x + bounds.width / 2;
			int y = bounds.y + (bounds.height - d*(ON_THE_FLY_COORD_ERROR_MESSAGE.length-1))/2 + d/3;

			if (od != d)
				g.setFont(new Font("Helvetica", Font.PLAIN, d));

			g.setColor(Color.RED);

			for (String msg:ON_THE_FLY_COORD_ERROR_MESSAGE) {
				g.drawString(msg, x-g.getFontMetrics().stringWidth(msg)/2, y);
				y += d;
				}
			}
		}

	public void setColorHandler(VisualizationColor vc, int type) {
		switch (type) {
		case CompoundTableColorHandler.FOREGROUND:
			mForegroundColor = vc;
			break;
		case CompoundTableColorHandler.BACKGROUND:
			mBackgroundColor = vc;
			break;
			}
		}
	}
