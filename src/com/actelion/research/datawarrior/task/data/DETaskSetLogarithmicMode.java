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

package com.actelion.research.datawarrior.task.data;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.CompoundTableModel;


public class DETaskSetLogarithmicMode extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Set Logarithmic Mode";

	private static final String PROPERTY_LOGARITHMIC = "isLogarithmic";
	private static final String PROPERTY_COLUMN = "column";

    private static Properties sRecentConfiguration;

    private CompoundTableModel  	mTableModel;
    private JComboBox				mComboBoxColumn;
    private JCheckBox				mCheckBoxIsLogarithmic;
	private int						mColumn;
	private boolean					mIsLogarithmic;

    public DETaskSetLogarithmicMode(DEFrame parent) {
		super(parent, false);
		mTableModel = parent.getTableModel();
		mColumn = -1;
    	}

    public DETaskSetLogarithmicMode(DEFrame parent, int column, boolean isLogarithmic) {
		super(parent, false);
		mTableModel = parent.getTableModel();
		mColumn = column;
		mIsLogarithmic = isLogarithmic;
    	}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mColumn == -1)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(mColumn));
		configuration.setProperty(PROPERTY_LOGARITHMIC, mIsLogarithmic ? "true" : "false");
		return configuration;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxColumn) {
			int column = mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());
			if (column != -1) {
				mCheckBoxIsLogarithmic.setSelected(mTableModel.isLogarithmicViewMode(column));
				}
			return;
			}
		}

	@Override
	public boolean isConfigurable() {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(column))
				return true;

		showErrorMessage("No numerical column with all values above 0.0 found.");
		return false;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel mp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		mp.setLayout(new TableLayout(size));

		mp.add(new JLabel("Data column:"), "1,1");

		mComboBoxColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(true);
		mComboBoxColumn.addActionListener(this);
		mp.add(mComboBoxColumn, "3,1");

		mCheckBoxIsLogarithmic = new JCheckBox("Treat column data logarithmically");
		mp.add(mCheckBoxIsLogarithmic, "1,3,3,3");

		return mp;
		}

	private boolean columnQualifies(int column) {
		return mTableModel.isColumnTypeDouble(column)
			&& !mTableModel.isColumnTypeDate(column)
			&& mTableModel.getMinimumValue(column) > 0;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.put(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		configuration.put(PROPERTY_LOGARITHMIC, mCheckBoxIsLogarithmic.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		int column = mTableModel.findColumn(columnName);
		if (column != -1)
			columnName = mTableModel.getColumnTitle(column);
		mComboBoxColumn.setSelectedItem(columnName);
		mCheckBoxIsLogarithmic.setSelected("true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxColumn.setSelectedItem("");
		mCheckBoxIsLogarithmic.setSelected(false);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
			if (column == -1) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
		        return false;
				}
			if ("true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC))
			 && !columnQualifies(column)) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' is not numerical of contains values <= 0.");
		        return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
		boolean isLogarithmic = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));
		mTableModel.setLogarithmicViewMode(column, isLogarithmic);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public Properties getRecentConfiguration() {
    	return sRecentConfiguration;
    	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
    	sRecentConfiguration = configuration;
    	}
	}
