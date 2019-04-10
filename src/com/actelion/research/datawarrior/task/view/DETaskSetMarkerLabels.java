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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableListHandler;
import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.MarkerLabelConstants;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;


public class DETaskSetMarkerLabels extends DETaskAbstractSetViewOptions implements MarkerLabelConstants {
	public static final String TASK_NAME = "Set Marker Labels";

	private static final String PROPERTY_IN_DETAIL_GRAPH_ONLY = "inDetailGraphOnly";
	private static final String PROPERTY_LABEL_SIZE = "labelSize";
	private static final String PROPERTY_SHOW_BACKGROUND = "showBackground";
	private static final String PROPERTY_BACKGROUND_TRANSPARENCY = "backgroundTransparency";

	private static final String PROPERTY_ROWS = "rows";
	private static final String ITEM_ALL_ROWS = "<All Rows>";
	private static final String CODE_ALL_ROWS = "<all>";
	private static final String ITEM_SELECTED_ROWS = "<Selected Rows>";
	private static final String CODE_SELECTED_ROWS = "<selected>";

	private static final String TEXT_NO_LABEL = "<no label>";

	private JComboBox[]	mComboBoxPosition;
	private JComboBox	mComboBoxLabelList;
	private JSlider		mSliderSize,mSliderTransparency;
	private JLabel		mLabelTransparency;
	private JCheckBox	mCheckBoxDetailGraphOnly,mCheckBoxShowBackground;

	public DETaskSetMarkerLabels(Frame owner, DEMainPane mainPane, CompoundTableView view) {
		super(owner, mainPane, view);
		}

	private MarkerLabelDisplayer getLabelDisplayer(CompoundTableView view) {
		return (view == null) ? null
			 : (view instanceof VisualizationPanel) ? ((VisualizationPanel)view).getVisualization()
			 : (MarkerLabelDisplayer)view;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof MarkerLabelDisplayer
			 || view instanceof VisualizationPanel) ? null : "Labels can only be shown in 2D-, 3D- and structure-views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		ArrayList<String> columnNameList = new ArrayList<String>();
		columnNameList.add(TEXT_NO_LABEL);
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (columnQualifies(column))
				columnNameList.add(getTableModel().getColumnTitle(column));
		String[] columnName = columnNameList.toArray(new String[0]);

		MarkerLabelDisplayer mld = getLabelDisplayer(getInteractiveView());

		mComboBoxPosition = new JComboBox[cPositionCode.length];
		for (int i=0; i<cFirstMidPosition; i++)
			mComboBoxPosition[i] = new JComboBox(columnName);
		if (mld == null || mld.supportsMidPositionLabels())
			for (int i=cFirstMidPosition; i<MarkerLabelDisplayer.cFirstBottomPosition; i++)
				mComboBoxPosition[i] = new JComboBox(columnName);
		for (int i=cFirstBottomPosition; i<cFirstTablePosition; i++)
			mComboBoxPosition[i] = new JComboBox(columnName);
		int tableLines = (mld == null || mld.supportsMarkerLabelTable()) ? cPositionCode.length - cFirstTablePosition : 0;
		for (int i=0; i<tableLines; i++)
			mComboBoxPosition[cFirstTablePosition+i] = new JComboBox(columnName);

		for (JComboBox cb:mComboBoxPosition)
			if (cb != null)
				cb.addActionListener(this);

		boolean showBackgroundOption = (mld == null || mld.supportsLabelBackground());
		boolean showBackgroundSlider = (showBackgroundOption && (mld == null || mld.supportsLabelBackgroundTransparency()));
		boolean showListOption = (mld == null || mld.supportsLabelsByList());
		boolean showDetailGraphOption = (mld == null || mld.isTreeViewModeEnabled());

		double[] sizeY1 = {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16 };
		double[] sizeY = new double[sizeY1.length+2*tableLines+4+(showBackgroundOption?4:0)+(showListOption?2:0)+(showDetailGraphOption?2:0)];
		int index = 0;
		for (double s:sizeY1)
			sizeY[index++] = s;
		for (int i=0; i<tableLines; i++) {
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 4;
			}
		sizeY[index-1] = 16;	// correct the last vertical spacer
		int indexAfterLabels = index;
		if (showListOption) {
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 8;
			}
		if (showBackgroundOption) {
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 4;
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 8;
			}
		for (int i=0; i<2; i++) {
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 8;
			}

		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8 }, sizeY };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		cp.add(new JLabel("Top Left"), "1,1");
		cp.add(new JLabel("Top Right", JLabel.RIGHT), "5,1");
		cp.add(new JLabel("Bottom Left"), "1,9");
		cp.add(new JLabel("Bottom Right", JLabel.RIGHT), "5,9");
		for (int i=0; i<cPositionCode.length; i++) {
			if (mComboBoxPosition[i] != null) {
				mComboBoxPosition[i].addItemListener(this);
				mComboBoxPosition[i].setEditable(!hasInteractiveView());
				if (i < cFirstTablePosition) {
					cp.add(mComboBoxPosition[i], ""+(1+2*(i%3))+","+(3+2*(i/3)));
					}
				else {
					int y = 11+2*(i-cFirstTablePosition);
					cp.add(new JLabel("Table line "+(1+i-cFirstTablePosition)+":", JLabel.RIGHT), "1,"+y);
					cp.add(mComboBoxPosition[i], "3,"+y);
					}
				}
			}

		index = indexAfterLabels;

		if (showListOption) {
			mComboBoxLabelList = new JComboBox();
			mComboBoxLabelList.addItem(ITEM_ALL_ROWS);
			mComboBoxLabelList.addItem(ITEM_SELECTED_ROWS);
			for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++)
				mComboBoxLabelList.addItem(getTableModel().getColumnTitleExtended(CompoundTableListHandler.getColumnFromList(i)));
			mComboBoxLabelList.setEditable(!hasInteractiveView());
			mComboBoxLabelList.addItemListener(this);
			JPanel listPanel = new JPanel();
			listPanel.add(new JLabel("Show labels on:"));
			listPanel.add(mComboBoxLabelList);
			cp.add(listPanel, "1," + index + ",5," + index);
			index += 2;
			}

		if (showBackgroundOption) {
			mCheckBoxShowBackground = new JCheckBox("Show rectangular label background");
			mCheckBoxShowBackground.setHorizontalAlignment(SwingConstants.CENTER);
			mCheckBoxShowBackground.addActionListener(this);
			cp.add(mCheckBoxShowBackground, "1," + index + ",5," + index);
			index += 2;

			if (showBackgroundSlider) {
				JPanel sliderpanel1 = new JPanel();
				mSliderTransparency = new JSlider(JSlider.HORIZONTAL, 0, 80, 25);
				mSliderTransparency.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderTransparency.getPreferredSize().height));
				mSliderTransparency.addChangeListener(this);
				mLabelTransparency = new JLabel("Background transparency:");
				sliderpanel1.add(mLabelTransparency);
				sliderpanel1.add(mSliderTransparency);
				cp.add(sliderpanel1, "1," + index + ",5," + index);
				index += 2;
				}
			}

		JPanel sliderpanel2 = new JPanel();
		mSliderSize = new JSlider(JSlider.HORIZONTAL, 0, 150, 50);
		mSliderSize.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderSize.getPreferredSize().height));
		mSliderSize.addChangeListener(this);
		sliderpanel2.add(new JLabel("Label size:"));
		sliderpanel2.add(mSliderSize);
		cp.add(sliderpanel2, "1,"+index+",5,"+index);
		index += 2;

		if (showDetailGraphOption) {
			mCheckBoxDetailGraphOnly = new JCheckBox("Show labels in detail graph only");
			mCheckBoxDetailGraphOnly.setEnabled(mld == null || mld.isTreeViewModeEnabled());
			mCheckBoxDetailGraphOnly.setHorizontalAlignment(SwingConstants.CENTER);
			mCheckBoxDetailGraphOnly.addActionListener(this);
			cp.add(mCheckBoxDetailGraphOnly, "1," + index + ",5," + index);
			index += 2;
			}

		JPanel bp = new JPanel();
		bp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bdefault = new JButton("Add Default");
		bdefault.addActionListener(this);
		bp.add(bdefault);
		JButton bnone = new JButton("Remove All");
		bnone.addActionListener(this);
		bp.add(bnone);
		cp.add(bp, "1,"+index+",5,"+index);

		return cp;
		}

	private boolean columnQualifies(int column) {
		return getTableModel().getColumnSpecialType(column) == null
			|| (CompoundTableModel.cColumnTypeIDCode.equals(getTableModel().getColumnSpecialType(column))
			 && (!hasInteractiveView() || getInteractiveView() instanceof VisualizationPanel));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Remove All")
			removeAllLabels();

		if (e.getActionCommand() == "Add Default")
			addDefaultLabels();
	
		if (!isIgnoreEvents()) {
			for (JComboBox cb:mComboBoxPosition) {
				if (cb != null && e.getSource() == cb && !TEXT_NO_LABEL.equals(cb.getSelectedItem())) {
					setIgnoreEvents(true);
					for (JComboBox cbi:mComboBoxPosition)
						if (cbi != null && cbi != cb && cb.getSelectedItem().equals(cbi.getSelectedItem()))
							cbi.setSelectedItem(TEXT_NO_LABEL);
					setIgnoreEvents(false);
					}
				}
			
			super.actionPerformed(e);	// causes a view update
			}
		}

	private void removeAllLabels() {
		setIgnoreEvents(true);
		for (int i=0; i<mComboBoxPosition.length; i++)
			if (mComboBoxPosition[i] != null)
				mComboBoxPosition[i].setSelectedItem(TEXT_NO_LABEL);
		setIgnoreEvents(false);
		}

	private void addDefaultLabels() {
		removeAllLabels();

		setIgnoreEvents(true);
		int idCount = 0;
		int numCount = 0;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (columnQualifies(column) && getTableModel().getColumnSpecialType(column) == null) {
				if (getTableModel().isColumnTypeDouble(column)) {
					if ((!hasInteractiveView() || getLabelDisplayer(getInteractiveView()).supportsMarkerLabelTable()) && numCount < 6)
						mComboBoxPosition[cFirstTablePosition+numCount++].setSelectedItem(getTableModel().getColumnTitle(column));
					}
				else {
					if (idCount == 0) {
						mComboBoxPosition[0].setSelectedItem(getTableModel().getColumnTitle(column));
						idCount++;
						}
					else if (idCount == 1) {
						mComboBoxPosition[2].setSelectedItem(getTableModel().getColumnTitle(column));
						idCount++;
						}
					}
				}
			}
		setIgnoreEvents(false);
   		}
	
	@Override
	public void setDialogToDefault() {
		for (JComboBox cb:mComboBoxPosition)
			cb.setSelectedItem(TEXT_NO_LABEL);
		if (mComboBoxLabelList != null)
			mComboBoxLabelList.setSelectedIndex(0);
		if (mCheckBoxShowBackground != null)
			mCheckBoxShowBackground.setSelected(false);
		if (mSliderTransparency != null)
			mSliderTransparency.setValue(25);
		mSliderSize.setValue(50);
		if (mCheckBoxDetailGraphOnly != null)
			mCheckBoxDetailGraphOnly.setSelected(false);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		for (int i=0; i<cPositionCode.length; i++) {
			if (mComboBoxPosition[i] != null) {
				String columnName = configuration.getProperty(cPositionCode[i]);
				if (columnName == null)
					mComboBoxPosition[i].setSelectedItem(TEXT_NO_LABEL);
				else {
					int column = getTableModel().findColumn(columnName);
					mComboBoxPosition[i].setSelectedItem(column == -1 ? columnName : getTableModel().getColumnTitle(column));
					}
				}
			}

		if (mComboBoxLabelList != null) {
			String columnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			if (columnName.equals(CODE_ALL_ROWS)) {
				mComboBoxLabelList.setSelectedItem(ITEM_ALL_ROWS);
				}
			else if (columnName.equals(CODE_SELECTED_ROWS)) {
				mComboBoxLabelList.setSelectedItem(ITEM_SELECTED_ROWS);
				}
			else {
				int pseudoColumn = getTableModel().findColumn(columnName);
				mComboBoxLabelList.setSelectedItem(!hasInteractiveView() && pseudoColumn == -1 ? columnName : getTableModel().getColumnTitleExtended(pseudoColumn));
				}
			}

		if (mCheckBoxShowBackground != null)
			mCheckBoxShowBackground.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_BACKGROUND)));

		if (mSliderTransparency != null) {
			float transparency = 0.25f;
			try {
				transparency = Float.parseFloat(configuration.getProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "0.25"));
				}
			catch (NumberFormatException nfe) {}
			mSliderTransparency.setValue(Math.round(100f * transparency));
			}

		float size = 1.0f;
		try {
			size = Float.parseFloat(configuration.getProperty(PROPERTY_LABEL_SIZE, "1.0"));
			}
		catch (NumberFormatException nfe) {}
		mSliderSize.setValue(50+(int)(50.0*Math.log(size)));

		if (mCheckBoxDetailGraphOnly != null)
			mCheckBoxDetailGraphOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		for (int i=0; i<cFirstTablePosition; i++)
			if (mComboBoxPosition[i] != null && !mComboBoxPosition[i].getSelectedItem().equals(TEXT_NO_LABEL))
				configuration.setProperty(cPositionCode[i], ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxPosition[i].getSelectedItem()));
		int tableIndex = cFirstTablePosition;
		for (int i=cFirstTablePosition; i<cPositionCode.length; i++)
			if (mComboBoxPosition[i] != null && !mComboBoxPosition[i].getSelectedItem().equals(TEXT_NO_LABEL))
				configuration.setProperty(cPositionCode[tableIndex++], ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxPosition[i].getSelectedItem()));
		if (mComboBoxLabelList != null) {
			String item = (String) mComboBoxLabelList.getSelectedItem();
			configuration.setProperty(PROPERTY_ROWS, item.equals(ITEM_ALL_ROWS) ? CODE_ALL_ROWS
					: item.equals(ITEM_SELECTED_ROWS) ? CODE_SELECTED_ROWS
					: getTableModel().getColumnTitleNoAlias(item));
			}
		if (mCheckBoxShowBackground != null) {
			boolean showBackground = mCheckBoxShowBackground.isSelected();
			configuration.setProperty(PROPERTY_SHOW_BACKGROUND, showBackground ? "true" : "false");
			if (showBackground && mSliderTransparency != null) {
				float transparency = (float) mSliderTransparency.getValue() / 100f;
				configuration.setProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "" + transparency);
				}
			}
		float size = (float)Math.exp((double)(mSliderSize.getValue()-50)/50.0);
		configuration.setProperty(PROPERTY_LABEL_SIZE, ""+size);
		if (mCheckBoxDetailGraphOnly != null)
			configuration.setProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY, mCheckBoxDetailGraphOnly.isSelected() ? "true" : "false");
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		MarkerLabelDisplayer mld = getLabelDisplayer(getInteractiveView());
		for (int i=0; i<cPositionCode.length; i++) {
			int column = mld.getMarkerLabelColumn(i);
			if (column != JVisualization.cColumnUnassigned)
				configuration.setProperty(cPositionCode[i], ""+getTableModel().getColumnTitleNoAlias(column));
			}

		int list = mld.getMarkerLabelList();
		if (list == MarkerLabelDisplayer.cLabelsOnAllRows)
			configuration.setProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
		else if (list == MarkerLabelDisplayer.cLabelsOnSelection)
			configuration.setProperty(PROPERTY_ROWS, CODE_SELECTED_ROWS);
		else
			configuration.setProperty(PROPERTY_ROWS, getTableModel().getColumnTitleNoAlias(CompoundTableListHandler.getColumnFromList(list)));

		if (mld.supportsLabelBackground() && mld.isShowLabelBackground()) {
			configuration.setProperty(PROPERTY_SHOW_BACKGROUND, mld.isShowLabelBackground() ? "true" : "false");
			if (mld.supportsLabelBackgroundTransparency())
				configuration.setProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "" + mld.getLabelTransparency());
			}
		configuration.setProperty(PROPERTY_LABEL_SIZE, ""+mld.getMarkerLabelSize());
		configuration.setProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY, mld.isMarkerLabelsInTreeViewOnly() ? "true" : "false");
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			for (int i=0; i<cPositionCode.length; i++) {
				String columnName = configuration.getProperty(cPositionCode[i]);
				if (columnName != null) {
					int column = getTableModel().findColumn(columnName);
					if (column == -1) {
						showErrorMessage("Column '"+columnName+"' not found.");
						return false;
						}
					if (!columnQualifies(column)) {
						showErrorMessage("Column '"+columnName+"' cannot be used for displaying labels.");
						return false;
						}
					}
				}

			String pseudoColumnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			if (!CODE_ALL_ROWS.equals(pseudoColumnName)
			 && !CODE_SELECTED_ROWS.equals(pseudoColumnName)) {
				int pseudoColumn = getTableModel().findColumn(pseudoColumnName);
				if (pseudoColumn == -1) {
					showErrorMessage(pseudoColumnName+"' not found.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		if (mSliderTransparency != null) {
			mLabelTransparency.setEnabled(mCheckBoxShowBackground.isSelected());
			mSliderTransparency.setEnabled(mCheckBoxShowBackground.isSelected());
			}
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		int[] columnAtPosition = new int[cPositionCode.length];

		MarkerLabelDisplayer mld = getLabelDisplayer(view);
		for (int i=0; i<cPositionCode.length; i++) {
			String columnName = configuration.getProperty(cPositionCode[i]);
			if (columnName == null)
				columnAtPosition[i] = -1;
			else
				columnAtPosition[i] = getTableModel().findColumn(columnName);
			}
		mld.setMarkerLabels(columnAtPosition);

		String pseudoColumnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
		if (pseudoColumnName.equals(CODE_ALL_ROWS))
			mld.setMarkerLabelList(MarkerLabelDisplayer.cLabelsOnAllRows);
		else if (pseudoColumnName.equals(CODE_SELECTED_ROWS))
			mld.setMarkerLabelList(MarkerLabelDisplayer.cLabelsOnSelection);
		else
			mld.setMarkerLabelList(
					CompoundTableListHandler.getListFromColumn(getTableModel().findColumn(pseudoColumnName)));

		if (mld.supportsLabelBackground()) {
			mld.setShowLabelBackground("true".equals(configuration.getProperty(PROPERTY_SHOW_BACKGROUND)));
			if (mld.supportsLabelBackgroundTransparency()) {
				try {
					mld.setLabelTransparency(Float.parseFloat(configuration.getProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "0.25")), isAdjusting);
					}
				catch (NumberFormatException nfe) {}
				}
			}

		try {
			mld.setMarkerLabelSize(Float.parseFloat(configuration.getProperty(PROPERTY_LABEL_SIZE, "1.0")), isAdjusting);
			}
		catch (NumberFormatException nfe) {}

		mld.setMarkerLabelsInTreeViewOnly("true".equals(configuration.getProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY)));
		}
	}
