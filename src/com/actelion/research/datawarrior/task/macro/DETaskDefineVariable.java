package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * Created by thomas on 12/8/16.
 */
public class DETaskDefineVariable extends ConfigurableTask {
	public static final String TASK_NAME = "Define Variable";

	private static final String PROPERTY_NAME = "name";
	private static final String PROPERTY_VALUE = "value";

	private JTextField mTextFieldName,mTextFieldValue;

	public DETaskDefineVariable(Frame parent) {
		super(parent, false);
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Variable name:"), "1,1");
		mTextFieldName = new JTextField(20);
		content.add(mTextFieldName, "3,1");

		content.add(new JLabel("Variable value:"), "1,3");
		mTextFieldValue = new JTextField(20);
		content.add(mTextFieldValue, "3,3");

		content.add(new JLabel("(Keep value empty to ask when macro is running)"), "1,5,3,5");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_NAME, mTextFieldName.getText());
		if (mTextFieldValue.getText().length() != 0)
			configuration.setProperty(PROPERTY_VALUE, mTextFieldValue.getText());
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldName.setText(configuration.getProperty(PROPERTY_NAME, ""));
		mTextFieldValue.setText(configuration.getProperty(PROPERTY_VALUE, ""));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldName.setText("");
		mTextFieldValue.setText("");
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (configuration.getProperty(PROPERTY_NAME, "").length() == 0) {
			showErrorMessage("No variable name defined.");
			return false;
		}

		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		String name = configuration.getProperty(PROPERTY_NAME);
		String value = configuration.getProperty(PROPERTY_VALUE, "");
		if (value.length() == 0) {
			value = JOptionPane.showInputDialog(getParentFrame(), "Please define the value of variable '" + name + "'",
					"Define Variable", JOptionPane.QUESTION_MESSAGE);
			if (value == null)
				((DEMacroRecorder)getProgressController()).stopMacro();
			}

		if (getProgressController() instanceof DEMacroRecorder)	// the progress controller of a running macro should always be a DEMacroRecorder
			((DEMacroRecorder)getProgressController()).setVariable(name, value);
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
