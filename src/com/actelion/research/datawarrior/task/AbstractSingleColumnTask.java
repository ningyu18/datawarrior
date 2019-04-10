package com.actelion.research.datawarrior.task;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

public abstract class AbstractSingleColumnTask extends ConfigurableTask implements ItemListener {
	private static final String PROPERTY_COLUMN = "column";

	private CompoundTableModel	mTableModel;
	private JComboBox           mComboBoxColumn;
	private int				    mColumn;

	public AbstractSingleColumnTask(Frame owner, CompoundTableModel tableModel, boolean useOwnThread) {
		super(owner, useOwnThread);
		mTableModel = tableModel;
		mColumn = -1;	// if interactive, then show dialog
		}

	/**
	 * Instantiates this task interactively with a pre-defined configuration.
	 * @param owner
	 * @param tableModel
	 * @param useOwnThread
	 * @param column
	 */
	public AbstractSingleColumnTask(Frame owner, CompoundTableModel tableModel, boolean useOwnThread, int column) {
		super(owner, useOwnThread);
		mTableModel = tableModel;
		mColumn = column;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
	}

	/**
	 * @param column total column index
	 * @return true if the column should appear in list for selection or shall be matched with condition
	 */
	public abstract boolean isCompatibleColumn(int column);

	@Override
	public Properties getPredefinedConfiguration() {
		if (mColumn == -1)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(mColumn));
		return configuration;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.FILL, 8},
				{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Column name:"), "1,1");
		mComboBoxColumn = new JComboBox();
		mComboBoxColumn.addItemListener(this);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (isCompatibleColumn(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(!isInteractive());
		content.add(mComboBoxColumn, "3,1");

		JPanel innerPanel = createInnerDialogContent();
		if (innerPanel != null)
			content.add(innerPanel, "1,3,4,3");

		return content;
		}

	/**
	 * Override this if your subclass needs more properties to be defined.
	 * This panel is expected to have some whitespace at its bottom.
	 * @return
	 */
	public JPanel createInnerDialogContent() {
		return null;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBoxColumn && e.getStateChange() == ItemEvent.SELECTED)
			columnChanged(getSelectedColumn());
		}

	public int getSelectedColumn() {
		return getTableModel().findColumn((String)mComboBoxColumn.getSelectedItem());
		}

	/**
	 * Override this if you need to update dialog items, when the column popup staet changes
	 */
	public void columnChanged(int column) {}

	@Override
	public boolean isConfigurable() {
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (isCompatibleColumn(i))
				return true;

		showErrorMessage("No compatible columns found.");
		return false;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int column = (mColumn != -1) ? mColumn : mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
		mComboBoxColumn.setSelectedItem(column != -1 ? mTableModel.getColumnTitle(column) : configuration.getProperty(PROPERTY_COLUMN));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mColumn != -1) {
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(mColumn));
			}
		else {
			for (int i = 0; i < mTableModel.getTotalColumnCount(); i++) {
				if (isCompatibleColumn(i)) {
					mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(i));
					break;
					}
				}
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, "");
		if (columnName.length() == 0) {
			showErrorMessage("No column defined.");
			return false;
			}
		if (isLive) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1) {
				showErrorMessage("Column '" + columnName + "' not found.");
				return false;
				}
			if (!isCompatibleColumn(column)) {
				showErrorMessage("Column '" + columnName + "' is not compatible.");
				return false;
				}
			}
		return true;
		}

	public int getColumn(Properties configuration) {
		return mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}

