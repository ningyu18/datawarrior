package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetColumnAlias extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Set Column Alias";

	private static final String PROPERTY_ALIAS = "alias";

	private JTextField  mTextFieldAlias;
	private String      mAlias;

	public DETaskSetColumnAlias(Frame owner, CompoundTableModel tableModel) {
		this(owner, tableModel, -1, null);
		}

	public DETaskSetColumnAlias(Frame owner, CompoundTableModel tableModel, int column, String alias) {
		super(owner, tableModel, false, column);
		mAlias = alias;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_ALIAS, mAlias);
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED}, {TableLayout.PREFERRED, 16} };

		mTextFieldAlias = new JTextField(12);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Alias name:"), "0,0");
		ip.add(mTextFieldAlias, "2,0");
		return ip;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_ALIAS, mTextFieldAlias.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextFieldAlias.setText(configuration.getProperty(PROPERTY_ALIAS, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mTextFieldAlias.setText("");
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		String alias = configuration.getProperty(PROPERTY_ALIAS);
		getTableModel().setColumnAlias(alias, getColumn(configuration));
		}
	}
