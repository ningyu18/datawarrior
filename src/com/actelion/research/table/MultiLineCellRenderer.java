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

package com.actelion.research.table;

import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.util.ColorHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MultiLineCellRenderer extends JTextArea implements ColorizedCellRenderer,TableCellRenderer {
    static final long serialVersionUID = 0x20070312;

    private boolean mAlternateBackground;
    private VisualizationColor mForegroundColor,mBackgroundColor;

    public MultiLineCellRenderer() {
		setLineWrap(true);
		setWrapStyleWord(true);
	    setOpaque(false);
		}

	public void setAlternateRowBackground(boolean b) {
		mAlternateBackground = b;
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

	@Override
	public void paintComponent(Graphics g) {
		// Substance Graphite LaF does not consider the defined background
		if (LookAndFeelHelper.isNewSubstance()) {
			Rectangle r = new Rectangle(new java.awt.Point(0,0), getSize());
			g.setColor(getBackground());
			((Graphics2D) g).fill(r);
			setOpaque(false);    // if the panel is opaque (e.g. after LaF change) new substance may crash
			super.paintComponent(g);
			}
		else {
			super.paintComponent(g);
			}
		}

    public Component getTableCellRendererComponent(JTable table, Object value,
							boolean isSelected, boolean hasFocus, int row, int column) {
	    if (LookAndFeelHelper.isAqua()
			    // Quaqua does not use the defined background color if CellRenderer is translucent
	     || (LookAndFeelHelper.isQuaQua()
		  && mBackgroundColor != null
		  && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned))
		    setOpaque(true);
		else
		    setOpaque(false);

		if (isSelected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
			}
		else {
            if (mForegroundColor != null && mForegroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	setForeground(mForegroundColor.getColorForForeground(record));
            	}
            else
            	setForeground(UIManager.getColor("Table.foreground"));

            if (mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	setBackground(mBackgroundColor.getColorForBackground(record));
            	}
            else {
            	if (!LookAndFeelHelper.isQuaQua()) {	// simulate the quaqua table style "striped"
		            Color bg = UIManager.getColor("Table.background");
		            setBackground(!mAlternateBackground || (row & 1) == 0 ? bg : ColorHelper.darker(bg, 0.94f));
            		}
            	}
			}

		setFont(table.getFont());
		if (hasFocus) {
			setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
			if (table.isCellEditable(row, column)) {
				setForeground( UIManager.getColor("Table.focusCellForeground") );
				setBackground( UIManager.getColor("Table.focusCellBackground") );
				}
			}
		else {
			setBorder(new EmptyBorder(1, 2, 1, 2));
			}

		setText((value == null) ? "" : value.toString());
		return this;
		}
	}
