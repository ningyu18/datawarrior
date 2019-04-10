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

package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.datawarrior.task.ConfigurableTask;
import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;


public class DETaskRepeatNextTask extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Repeat Next Tasks";

	private static final String PROPERTY_COUNT = "count";
	private static final String PROPERTY_ALL_TASKS = "all";

    private JTextField          mTextFieldCount;
    private JCheckBox           mCheckBoxAllTasks,mCheckBoxForever;

	public DETaskRepeatNextTask(Frame sourceFrame) {
		super(sourceFrame, true);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
        return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public JPanel createDialogContent() {
        mTextFieldCount = new JTextField("10", 4);
        mCheckBoxForever = new JCheckBox("Repeat the task(s) forever");
        mCheckBoxForever.addActionListener(this);
        mCheckBoxAllTasks = new JCheckBox("Repeat all following tasks");

        JPanel gp = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
        gp.setLayout(new TableLayout(size));
        gp.add(new JLabel("Repetition count:", JLabel.RIGHT), "1,1");
        gp.add(mTextFieldCount, "3,1");
        gp.add(mCheckBoxForever, "1,3,3,3");
        gp.add(mCheckBoxAllTasks, "1,5,3,5");

        return gp;
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	if (!mCheckBoxForever.isSelected())
    		configuration.setProperty(PROPERTY_COUNT, mTextFieldCount.getText());

   		configuration.setProperty(PROPERTY_ALL_TASKS, mCheckBoxAllTasks.isSelected() ? "true" : "false");

    	return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COUNT, "");
		mTextFieldCount.setText(value);
		mCheckBoxForever.setSelected(value.length() == 0);

		mCheckBoxAllTasks.setSelected("true".equals(configuration.getProperty(PROPERTY_ALL_TASKS)));
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		try {
			String value = configuration.getProperty(PROPERTY_COUNT);
			if (value != null) {
				int count = Integer.parseInt(value);
				if (count < 2) {
					showErrorMessage("Repetition count must be at least 2.");
					return false;
					}
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Repetition count is not numerical.");
			return false;
			}

		return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldCount.setText("10");
		mCheckBoxForever.setSelected(false);
		mCheckBoxAllTasks.setSelected(false);
		}

	@Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mCheckBoxForever) {
        	mTextFieldCount.setEnabled(!mCheckBoxForever.isSelected());
            return;
            }
		}

	@Override
	public void runTask(Properties configuration) {
		// don't do anything
		}

	/**
	 * @param configuration
	 * @return repetition count or -1 of forever
	 */
    public int getRepetitions(Properties configuration) {
    	return Integer.parseInt(configuration.getProperty(PROPERTY_COUNT, "-1"));
        }

    public boolean repeatAllTasks(Properties configuration) {
    	return "true".equals(configuration.getProperty(PROPERTY_ALL_TASKS));
    	}
	}

