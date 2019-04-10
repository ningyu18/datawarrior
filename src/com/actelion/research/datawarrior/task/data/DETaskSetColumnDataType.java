package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetColumnDataType extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Set Column Data Type";

	private static final String PROPERTY_DATA_TYPE = "type";

	private JComboBox mComboBoxDataType;
	private int mDataType;

	public DETaskSetColumnDataType(Frame owner, CompoundTableModel tableModel) {
		super(owner, tableModel, false);
		}

	/**
	 * Constructor for interactively define and run the task without showing a configuration dialog.
	 * @param owner
	 * @param tableModel
	 * @param column valid column
	 * @param dataType valid summaryMode
	 */
	public DETaskSetColumnDataType(Frame owner, CompoundTableModel tableModel, int column, int dataType) {
		this(owner, tableModel, createColumnList(column), dataType);
		}

	/**
	 * Constructor for interactively define and run the task without showing a configuration dialog.
	 * @param owner
	 * @param tableModel
	 * @param columnList valid column list
	 * @param dataType valid summaryMode
	 */
	public DETaskSetColumnDataType(Frame owner, CompoundTableModel tableModel, int[] columnList, int dataType) {
		super(owner, tableModel, false, columnList);
		mDataType = dataType;
		}

	private static int[] createColumnList(int column) {
		int[] columnList = new int[1];
		columnList[0] = column;
		return columnList;
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().getColumnSpecialType(column) == null;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.put(PROPERTY_DATA_TYPE, CompoundTableConstants.cDataTypeCode[mDataType]);

		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_DATA_TYPE, CompoundTableConstants.cDataTypeCode[mComboBoxDataType.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxDataType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_DATA_TYPE),
				CompoundTableConstants.cDataTypeCode, CompoundTableConstants.cDataTypeAutomatic));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxDataType.setSelectedIndex(CompoundTableConstants.cDataTypeAutomatic);
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 8, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 16} };

		mComboBoxDataType = new JComboBox(CompoundTableConstants.cDataTypeText);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Data type:"), "0,0");
		ip.add(mComboBoxDataType, "2,0");
		return ip;
		}

	@Override
	public void runTask(Properties configuration) {
		int dataType = findListIndex(configuration.getProperty(PROPERTY_DATA_TYPE),
				CompoundTableConstants.cDataTypeCode, CompoundTableConstants.cDataTypeAutomatic);

		int[] columnList = getColumnList(configuration);

		for (int column:columnList)
			getTableModel().setExplicitDataType(column, dataType);
		}
	}
