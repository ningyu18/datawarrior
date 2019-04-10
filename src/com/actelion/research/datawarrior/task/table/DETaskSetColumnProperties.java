package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Properties;

public class DETaskSetColumnProperties extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Set Column Properties";

	private static final String PROPERTY_PROPERTIES = "properties";

	private JTextArea mTextAreaProperties;
	private HashMap<String,String> mProperties;

	public DETaskSetColumnProperties(Frame owner, CompoundTableModel tableModel) {
		this(owner, tableModel, -1, null);
	}

	public DETaskSetColumnProperties(Frame owner, CompoundTableModel tableModel, int column, HashMap<String,String> properties) {
		super(owner, tableModel, false, column);
		mProperties = properties;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_PROPERTIES, encode(mProperties));
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 4, HiDPIHelper.scale(320) }, {8, TableLayout.PREFERRED, HiDPIHelper.scale(64), 16} };

		mTextAreaProperties = new JTextArea();
		JScrollPane sp = new JScrollPane(mTextAreaProperties, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Properties:"), "0,1");
		ip.add(sp, "2,1,2,2");
		return ip;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_PROPERTIES, mTextAreaProperties.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextAreaProperties.setText(configuration.getProperty(PROPERTY_PROPERTIES, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		columnChanged(getSelectedColumn());
		}

	@Override
	public void columnChanged(int column) {
		if (mTextAreaProperties != null)
			mTextAreaProperties.setText(column == -1 ? "" : encode(getTableModel().getColumnProperties(column)));
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	private HashMap<String,String> decode(String s) {
		if (s == null || s.length() == 0)
			return null;

		HashMap<String,String> properties = new HashMap<String,String>();
		String[] list = s.split("\\n");
		for (String p:list) {
			int index = p.indexOf('=');
			if (index > 0 && index < p.length()-1) {
				properties.put(p.substring(0, index), p.substring(index+1));
				}
			}
		return properties.size() == 0 ? null : properties;
		}

	private String encode(HashMap<String,String> properties) {
		if (properties == null || properties.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		for (String key:properties.keySet()) {
			sb.append(key);
			sb.append('=');
			sb.append(properties.get(key));
			sb.append('\n');
			}
		return sb.toString();
		}

	@Override
	public void runTask(Properties configuration) {
		String properties = configuration.getProperty(PROPERTY_PROPERTIES);
		getTableModel().setColumnProperties(getColumn(configuration), decode(properties));
		}
	}
