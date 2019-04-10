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

package com.actelion.research.datawarrior;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.data.DETaskDeleteColumns;
import com.actelion.research.datawarrior.task.data.DETaskSetCategoryCustomOrder;
import com.actelion.research.datawarrior.task.data.DETaskSetColumnDataType;
import com.actelion.research.datawarrior.task.filter.DETaskAddNewFilter;
import com.actelion.research.datawarrior.task.table.*;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.filter.JCategoryFilterPanel;
import com.actelion.research.table.filter.JFilterPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DETablePopupMenu extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 0x20060904;

    private static final String SET_COLUMN_ALIAS = "Set Column Alias...";
    private static final String SET_COLUMN_DESCRIPTION = "Set Column Description...";
	private static final String SET_COLUMN_DATA_TYPE = "Set Column Data Type To";
    private static final String SET_CATEGORY_CUSTOM_ORDER = "Set Category Custom Order...";
    private static final String NEW_STRUCTURE_FILTER = "New Structure Filter";
    private static final String NEW_SSS_LIST_FILTER = "New SSS-List Filter";
    private static final String NEW_SIM_LIST_FILTER = "New Sim-List Filter";
    private static final String NEW_REACTION_FILTER = "New Reaction Filter";
    private static final String NEW_TEXT_FILTER = "New Text Filter";
    private static final String NEW_SLIDER_FILTER = "New Slider Filter";
    private static final String NEW_CATEGORY_FILTER = "New Category Filter";
    private static final String HIDE_VALUE_COUNT = "Hide Value Count";
    private static final String SHOW_STD_DEVIATION = "Show Standard Deviation";
    private static final String SHOW_ROUNDED_VALUES = "Show Rounded Values...";
    private static final String EXCLUDE_MODIFIER_VALUES = "Exclude Values With Modifiers";
    private static final String SET_STRUCTURE_COLOR = "Set Structure Color...";
    private static final String SET_TEXT_COLOR = "Set Text Color...";
    private static final String SET_BACKGROUND_COLOR = "Set Background Color...";
    private static final String WRAP_TEXT = "Wrap Text";
    private static final String HIDE = "Hide '";
    private static final String SHOW = "Show '";
    private static final String DELETE = "Delete '";
	private static final String TYPE = "type:";
	private static final String SUMMARY = "summary:";
	private static final String HILITE = "hilite:";

    private CompoundTableModel	mTableModel;
	private int					mColumn;
	private Frame    			mParentFrame;
	private DEParentPane		mParentPane;
	private DETableView         mTableView;

	public DETablePopupMenu(Frame parent, DEParentPane parentPane, DETableView tableView, int column) {
		super();

        mParentFrame = parent;
        mParentPane = parentPane;
        mTableView = tableView;
		mTableModel = tableView.getTableModel();
		mColumn = column;

		addItem(SET_COLUMN_ALIAS);
		addItem(SET_COLUMN_DESCRIPTION);

		String specialType = mTableModel.getColumnSpecialType(column);
		if (specialType == null) {
			JMenu dataTypeMenu = new JMenu(SET_COLUMN_DATA_TYPE);
			add(dataTypeMenu);
			for (int i=0; i<CompoundTableConstants.cDataTypeText.length; i++) {
				JRadioButtonMenuItem dataTypeItem = new JRadioButtonMenuItem(CompoundTableConstants.cDataTypeText[i],
						mTableModel.getExplicitDataType(column) == i);
				dataTypeItem.addActionListener(this);
				dataTypeItem.setActionCommand(TYPE+CompoundTableConstants.cDataTypeCode[i]);
				dataTypeMenu.add(dataTypeItem);
				}
			}

		if (DETaskSetCategoryCustomOrder.columnQualifies(mTableModel, column)) {
	        addSeparator();
			addItem(SET_CATEGORY_CUSTOM_ORDER);
			}

        addSeparator();

        if (specialType != null) {
            if (specialType.equals(CompoundTableModel.cColumnTypeIDCode)) {
    			JMenu filterMenu = new JMenu("New Structure Filter");
                add(filterMenu);
                addItem(filterMenu, "Single Structure", NEW_STRUCTURE_FILTER);
                addItem(filterMenu, "Substructure List", NEW_SSS_LIST_FILTER);
                addItem(filterMenu, "Similar Structure List", NEW_SIM_LIST_FILTER);
            	}
            if (specialType.equals(CompoundTableModel.cColumnTypeRXNCode)) {
                addItem(NEW_REACTION_FILTER);
            	}
            }
        else {
            if (mTableModel.isColumnTypeString(column) && specialType == null)
                addItem(NEW_TEXT_FILTER);

            if (mTableModel.isColumnTypeDouble(column)
             && mTableModel.hasNumericalVariance(column))
                addItem(NEW_SLIDER_FILTER);

            if (mTableModel.isColumnTypeCategory(column)
       		 && mTableModel.getCategoryCount(column) < JCategoryFilterPanel.cMaxCheckboxCount)
                addItem(NEW_CATEGORY_FILTER);

            if (mTableModel.isColumnTypeRangeCategory(column))
                addItem(NEW_SLIDER_FILTER);
            }

		if (mTableModel.isColumnTypeDouble(column)) {
			addSeparator();
			JMenu summaryModeMenu = new JMenu("Show Multiple Values As");
            add(summaryModeMenu);
            for (int i=0; i<CompoundTableConstants.cSummaryModeText.length; i++) {
                JRadioButtonMenuItem summaryItem = new JRadioButtonMenuItem(CompoundTableConstants.cSummaryModeText[i],
                        mTableModel.getColumnSummaryMode(column) == i);
                summaryItem.addActionListener(this);
	            summaryItem.setActionCommand(SUMMARY+CompoundTableConstants.cSummaryModeCode[i]);
                summaryModeMenu.add(summaryItem);
                }
            summaryModeMenu.addSeparator();
            JCheckBoxMenuItem menuItem1 = new JCheckBoxMenuItem(HIDE_VALUE_COUNT);
            menuItem1.setState(mTableModel.isColumnSummaryCountHidden(column));
            menuItem1.setEnabled(mTableModel.getColumnSummaryMode(column) != CompoundTableConstants.cSummaryModeNormal);
            menuItem1.addActionListener(this);
            summaryModeMenu.add(menuItem1);
            JCheckBoxMenuItem menuItem2 = new JCheckBoxMenuItem(SHOW_STD_DEVIATION);
            menuItem2.setState(mTableModel.isColumnStdDeviationShown(column));
            menuItem2.setEnabled(mTableModel.getColumnSummaryMode(column) == CompoundTableConstants.cSummaryModeMean);
            menuItem2.addActionListener(this);
            summaryModeMenu.add(menuItem2);

            addItem(SHOW_ROUNDED_VALUES);

            if (mTableModel.isColumnWithModifiers(column)) {
                addCheckBoxMenuItem(EXCLUDE_MODIFIER_VALUES, mTableModel.getColumnModifierExclusion(column));
                }
		    }

		if (mTableModel.getColumnSpecialType(column) == null) {
            addSeparator();
            addCheckBoxMenuItem(WRAP_TEXT, mTableView.getTextWrapping(column));
		    }

		addSeparator();
        if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
			JMenu hiliteModeMenu = new JMenu("Highlight Structure By");
            add(hiliteModeMenu);
            for (int i=0; i<CompoundTableConstants.cStructureHiliteModeText.length; i++) {
                JRadioButtonMenuItem hiliteItem = new JRadioButtonMenuItem(CompoundTableConstants.cStructureHiliteModeText[i],
                        mTableModel.getStructureHiliteMode(mColumn) == i);
                hiliteItem.addActionListener(this);
	            hiliteItem.setActionCommand(HILITE+CompoundTableConstants.cHiliteModeCode[i]);
                hiliteModeMenu.add(hiliteItem);
                }
        	addItem(SET_STRUCTURE_COLOR);
        	}
        else {
        	addItem(SET_TEXT_COLOR);
        	}
		addItem(SET_BACKGROUND_COLOR);

        addSeparator();
		addItem(DELETE+mTableModel.getColumnTitle(column)+"'");

		addSeparator();
		addItem(HIDE+mTableModel.getColumnTitle(column)+"'");

        for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
            if (mTableModel.isColumnDisplayable(i)
             && !mTableView.isColumnVisible(i)) {
                addItem(SHOW+mTableModel.getColumnTitle(i)+"'");
                }
            }
  		}

	private void addItem(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(this);
        add(menuItem);
	    }

	private void addItem(JMenuItem menu, String text, String actionCommand) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(this);
        menu.add(menuItem);
	    }

	private void addCheckBoxMenuItem(String text, boolean state) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
        menuItem.setState(state);
        menuItem.addActionListener(this);
        add(menuItem);
	    }

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.startsWith(TYPE)) {
			int type = decodeItem(command.substring(TYPE.length()), CompoundTableConstants.cDataTypeCode);
			new DETaskSetColumnDataType(mParentFrame, mTableModel, mColumn, type).defineAndRun();
			return;
			}
		if (command.startsWith(SUMMARY)) {
			int summaryMode = decodeItem(command.substring(SUMMARY.length()), CompoundTableConstants.cSummaryModeCode);
            new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, summaryMode, -1, -1, -1, -1).defineAndRun();
	        return;
	        }
		if (command.startsWith(HILITE)) {
			int hiliteMode = decodeItem(command.substring(HILITE.length()), CompoundTableConstants.cHiliteModeCode);
	        new DETaskSetStructureHiliteMode(mParentFrame, mTableModel, mColumn, hiliteMode).defineAndRun();
            return;
	        }
		if (e.getActionCommand().equals(SET_COLUMN_ALIAS)) {
			String alias = (String)JOptionPane.showInputDialog(
					mParentFrame,
					"Column Alias to be used for '"+mTableModel.getColumnTitleNoAlias(mColumn)+"'",
					"Set Column Alias",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					mTableModel.getColumnTitle(mColumn));
			if (alias != null)    // if not canceled
				new DETaskSetColumnAlias(mParentFrame, mTableModel, mColumn, alias).defineAndRun();
			}
		if (e.getActionCommand().equals(SET_COLUMN_DESCRIPTION)) {
			String description = (String)JOptionPane.showInputDialog(
					mParentFrame,
					"Column Description for '"+mTableModel.getColumnTitle(mColumn)+"'",
					"Set Column Description",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					mTableModel.getColumnDescription(mColumn));
			if (description != null)	// if not canceled
				new DETaskSetColumnDescription(mParentFrame, mTableModel, mColumn, description).defineAndRun();
			}
		else if (e.getActionCommand().equals(SET_CATEGORY_CUSTOM_ORDER)) {
			new DETaskSetCategoryCustomOrder(mParentFrame, mTableModel, mColumn).defineAndRun();
			}
        else if (e.getActionCommand().startsWith("New ") && e.getActionCommand().endsWith(" Filter")) {
            DEPruningPanel pruningPanel = mTableView.getParentPane().getPruningPanel();
            if (e.getActionCommand().equals(NEW_STRUCTURE_FILTER)) {
                addDefaultDescriptor(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_STRUCTURE).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_SSS_LIST_FILTER)) {
                addDefaultDescriptor(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_SSS_LIST).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_SIM_LIST_FILTER)) {
                addDefaultDescriptor(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_SIM_LIST).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_REACTION_FILTER)) {
                addDefaultDescriptor(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_REACTION).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_TEXT_FILTER)) {
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_TEXT).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_SLIDER_FILTER)) {
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_DOUBLE).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_CATEGORY_FILTER)) {
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_CATEGORY).defineAndRun();
                }
            }
        else if (e.getActionCommand().equals(HIDE_VALUE_COUNT)) {
			new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1,
					mTableModel.isColumnSummaryCountHidden(mColumn) ? 1 : 0, -1, -1, -1).defineAndRun();
			}
        else if (e.getActionCommand().equals(SHOW_STD_DEVIATION)) {
			new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1, -1,
					mTableModel.isColumnStdDeviationShown(mColumn) ? 0 : 1, -1, -1).defineAndRun();
			}
        else if (e.getActionCommand().equals(SHOW_ROUNDED_VALUES)) {
            int oldDigits = mTableModel.getColumnSignificantDigits(mColumn);
            String selection = (String)JOptionPane.showInputDialog(
                    mParentFrame,
                    "Number of significant digits:",
                    "Display Rounded Value",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
		            DETaskSetNumericalColumnDisplayMode.ROUNDING_TEXT,
		            DETaskSetNumericalColumnDisplayMode.ROUNDING_TEXT[oldDigits]);
            if (selection != null) {// if not cancelled
                int newDigits = 0;
                while (!selection.equals(DETaskSetNumericalColumnDisplayMode.ROUNDING_TEXT[newDigits]))
                    newDigits++;

                if (newDigits != oldDigits)  // if changed
	                new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1, -1, -1, newDigits, -1).defineAndRun();
                }
            }
        else if (e.getActionCommand().equals(EXCLUDE_MODIFIER_VALUES)) {
			new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1, -1, -1, -1, mTableModel.getColumnModifierExclusion(mColumn) ? 0 : 1).defineAndRun();
            }
        else if (e.getActionCommand().equals(SET_TEXT_COLOR)
       		  || e.getActionCommand().equals(SET_STRUCTURE_COLOR)) {
        	new DETaskSetTextColor(mParentFrame, mParentPane.getMainPane(), mTableView, mColumn).defineAndRun();
        	}
        else if (e.getActionCommand().equals(SET_BACKGROUND_COLOR)) {
        	new DETaskSetTextBackgroundColor(mParentFrame, mParentPane.getMainPane(), mTableView, mColumn).defineAndRun();
        	}
        else if (e.getActionCommand().equals(WRAP_TEXT)) {
			new DETaskSetTextWrapping(mParentFrame, mTableView, convertToList(mColumn), !mTableView.getTextWrapping(mColumn)).defineAndRun();
            }
        else if (e.getActionCommand().startsWith(HIDE)) {
            new DETaskHideTableColumns(mParentFrame, mTableView, convertToList(mColumn)).defineAndRun();
            }
        else if (e.getActionCommand().startsWith(SHOW)) {
        	int column = mTableModel.findColumn(e.getActionCommand().substring(6, e.getActionCommand().length()-1));
            new DETaskShowTableColumns(mParentFrame, mTableView, convertToList(column)).defineAndRun();
            }
		else if (e.getActionCommand().startsWith(DELETE)) {
	        int doDelete = JOptionPane.showConfirmDialog(this,
                    "Do you really want to delete the column '"+mTableModel.getColumnTitle(mColumn)+"'?",
                    "Delete Column?",
                    JOptionPane.OK_CANCEL_OPTION);
	        if (doDelete == JOptionPane.OK_OPTION)
	        	new DETaskDeleteColumns(mParentFrame, mTableModel, convertToList(mColumn)).defineAndRun();
			}
		}

	private int[] convertToList(int column) {
    	int[] columnList = new int[1];
    	columnList[0] = column;
		return columnList;
		}

	/**
	 * Tries to find item in itemList. If successful it returns the list index.
	 * If item is null or item is not found, defaultIndex is returned.
	 * @param item
	 * @param itemList
	 * @return
	 */
	private int decodeItem(String item, String[] itemList) {
		for (int i=0; i<itemList.length; i++)
			if (item.equals(itemList[i]))
				return i;
		return -1;  // should never happen
		}

	private void addDefaultDescriptor(int column) {
        for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
            if (mTableModel.getParentColumn(i) == column
             && mTableModel.isDescriptorColumn(i))
                return;

        if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column)))
            mTableModel.addDescriptorColumn(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
        else if (CompoundTableModel.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column)))
            mTableModel.addDescriptorColumn(column, DescriptorConstants.DESCRIPTOR_ReactionIndex.shortName);
        }
    }
