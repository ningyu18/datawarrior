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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.TreeMap;


public class DETaskAddRecordNumbers extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Add Row Numbers";

	private static final String PROPERTY_COLUMN_NAME = "columnName";
	private static final String PROPERTY_FIRST_NUMBER = "firstNumber";
	private static final String PROPERTY_VISIBLE_ONLY = "visibleOnly";
	private static final String PROPERTY_CATEGORY = "category";
	private static final String PROPERTY_CATEGORY_MODE = "categoryMode";

	private static final String CATEGORY_MODE_INDEPENDENT = "independent";
	private static final String CATEGORY_MODE_SAME = "same";

	private DEFrame				mSourceFrame;
    private JTextField          mTextFieldColumnName,mTextFieldFirstNo;
    private JCheckBox           mCheckBoxVisibleOnly,mCheckBoxUseSameForSame,mCheckBoxCountWithinCategory;
    private JComboBox           mComboBoxCategory;

	public DETaskAddRecordNumbers(DEFrame sourceFrame) {
		super(sourceFrame, true);
		mSourceFrame = sourceFrame;
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
        mTextFieldColumnName = new JTextField("Row No", 12);
        mTextFieldFirstNo = new JTextField("1", 12);
        mCheckBoxVisibleOnly = new JCheckBox("Visible rows only", true);
        mCheckBoxUseSameForSame = new JCheckBox("Use same number within same category", false);
        mCheckBoxUseSameForSame.addActionListener(this);
        mComboBoxCategory = new JComboBox();
        mComboBoxCategory.setEnabled(false);
        mComboBoxCategory.setEditable(!isInteractive());
		mCheckBoxCountWithinCategory = new JCheckBox("Use independent row numbers in each category", false);
		mCheckBoxCountWithinCategory.addActionListener(this);
        CompoundTableModel tableModel = mSourceFrame.getTableModel();
        for (int column=0; column<tableModel.getTotalColumnCount(); column++)
            if (tableModel.isColumnTypeCategory(column))
                mComboBoxCategory.addItem(tableModel.getColumnTitle(column));
        if (isInteractive() && mComboBoxCategory.getItemCount() == 0)
        	mCheckBoxUseSameForSame.setEnabled(false);

        JPanel gp = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16,
							 TableLayout.PREFERRED, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16} };
        gp.setLayout(new TableLayout(size));
        gp.add(new JLabel("Title of new column:", JLabel.RIGHT), "1,1");
        gp.add(new JLabel("First number to use:", JLabel.RIGHT), "1,3");
        gp.add(mTextFieldColumnName, "3,1");
        gp.add(mTextFieldFirstNo, "3,3");
        gp.add(mCheckBoxVisibleOnly, "1,5");
        gp.add(mCheckBoxUseSameForSame, "1,7,3,7");
		gp.add(mCheckBoxCountWithinCategory, "1,8,3,8");
		gp.add(new JLabel("Category:", JLabel.RIGHT), "1,10");
		gp.add(mComboBoxCategory, "3,10");

        return gp;
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	String value = mTextFieldColumnName.getText();
    	if (value.length() != 0)
    		configuration.setProperty(PROPERTY_COLUMN_NAME, value);

    	value = mTextFieldFirstNo.getText();
    	if (value.length() != 0) {
	    	try {
	    		configuration.setProperty(PROPERTY_FIRST_NUMBER, ""+Integer.parseInt(value));
	    		}
	    	catch (NumberFormatException nfe) {}
    		}

   		configuration.setProperty(PROPERTY_VISIBLE_ONLY, mCheckBoxVisibleOnly.isSelected() ? "true" : "false");

   		if (mCheckBoxUseSameForSame.isSelected()
		 || mCheckBoxCountWithinCategory.isSelected()) {
			configuration.setProperty(PROPERTY_CATEGORY_MODE, mCheckBoxUseSameForSame.isSelected() ?
										CATEGORY_MODE_SAME : CATEGORY_MODE_INDEPENDENT);
			configuration.setProperty(PROPERTY_CATEGORY, (String) mComboBoxCategory.getSelectedItem());
			}

		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN_NAME);
		mTextFieldColumnName.setText(value == null ? "Record No" : value);

		value = configuration.getProperty(PROPERTY_FIRST_NUMBER);
		mTextFieldFirstNo.setText(value == null ? "1" : value);

		mCheckBoxVisibleOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY)));

		value = configuration.getProperty(PROPERTY_CATEGORY);
		if (value != null) {
			mComboBoxCategory.setSelectedItem(value);
			if (CATEGORY_MODE_INDEPENDENT.equals(configuration.getProperty(PROPERTY_CATEGORY_MODE))) {
				mCheckBoxUseSameForSame.setSelected(false);
				mCheckBoxCountWithinCategory.setSelected(true);
				}
			else {
				mCheckBoxUseSameForSame.setSelected(true);
				mCheckBoxCountWithinCategory.setSelected(false);
				}
			}
		else {
			mCheckBoxUseSameForSame.setSelected(false);
			mCheckBoxCountWithinCategory.setSelected(false);
			}

		enableItems();
		}

	private void enableItems() {
		mComboBoxCategory.setEnabled(mCheckBoxUseSameForSame.isSelected() || mCheckBoxCountWithinCategory.isSelected());
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			String category = configuration.getProperty(PROPERTY_CATEGORY);
			if (category != null) {
				int column = mSourceFrame.getTableModel().findColumn(category);
				if (column == -1) {
					showErrorMessage("Category column '"+category+"' was not found.");
					return false;
					}
				if (!mSourceFrame.getTableModel().isColumnTypeCategory(column)) {
					showErrorMessage("Column '"+category+"' does not contain categories.");
					return false;
					}		
				}
			}

		return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldColumnName.setText("Row No");
		mTextFieldFirstNo.setText("1");
		mCheckBoxVisibleOnly.setSelected(false);
		mCheckBoxUseSameForSame.setSelected(false);
		mCheckBoxCountWithinCategory.setSelected(false);
        mComboBoxCategory.setEnabled(false);
		}

	@Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mCheckBoxUseSameForSame) {
			if (mCheckBoxUseSameForSame.isSelected())
				mCheckBoxCountWithinCategory.setSelected(false);
			enableItems();
            return;
            }
		if (e.getSource() == mCheckBoxCountWithinCategory) {
			if (mCheckBoxCountWithinCategory.isSelected())
				mCheckBoxUseSameForSame.setSelected(false);
			enableItems();
			return;
			}
		}

	@Override
	public void runTask(Properties configuration) {
        CompoundTableModel tableModel = mSourceFrame.getTableModel();

        String[] columnName = new String[1];
		columnName[0] = configuration.getProperty(PROPERTY_COLUMN_NAME, "Row No");

		int recordNoColumn = tableModel.addNewColumns(columnName);

        int categoryColumn = tableModel.findColumn(configuration.getProperty(PROPERTY_CATEGORY));
        TreeMap<String,Integer> map = (categoryColumn == -1) ? null : new TreeMap<String,Integer>();

        boolean visibleOnly = "true".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY));

		boolean sameInCategory = false;
		boolean independentCategories = false;
		if (categoryColumn != -1) {
			if (CATEGORY_MODE_INDEPENDENT.equals(configuration.getProperty(PROPERTY_CATEGORY_MODE)))
				independentCategories = true;
			else
				sameInCategory = true;
			}

        int rowCount = visibleOnly ? tableModel.getRowCount() : tableModel.getTotalRowCount();
		String value = configuration.getProperty(PROPERTY_FIRST_NUMBER);
		int firstNo = (value == null) ? 1 : Integer.parseInt(value);
		StringBuilder sb = new StringBuilder();
        for (int row=0; row<rowCount; row++) {
            CompoundRecord record = visibleOnly ? tableModel.getRecord(row) : tableModel.getTotalRecord(row);
			String data = null;

			if (sameInCategory || independentCategories) {
				String[] entries = mSourceFrame.getTableModel().separateEntries(tableModel.encodeData(record, categoryColumn));
				sb.setLength(0);
				for (String entry:entries) {
					Integer index = map.get(entry);
					if (sameInCategory) {
						if (index == null) {
							index = new Integer(map.size());
							map.put(entry, index);
							}
						}
					else {
						if (index == null)
							index = new Integer(0);
						else
							index = new Integer(index+1);
						map.put(entry, index);
						}

					if (sb.length() != 0)
						sb.append(CompoundTableModel.cEntrySeparator);
					sb.append(Integer.toString(firstNo+index));
					}
				data = sb.toString();
				}
			else {
				data = Integer.toString(firstNo+row);
				}

            record.setData(tableModel.decodeData(data, recordNoColumn), recordNoColumn);
            }

        tableModel.finalizeNewColumns(recordNoColumn, null);
		}

    private String getCategoryNumbers(int firstNo, boolean isSame, TreeMap<String,Integer> map, String categories, StringBuilder sb) {
        String[] entries = mSourceFrame.getTableModel().separateEntries(categories);
        sb.setLength(0);
        for (String entry:entries) {
            Integer index = map.get(entry);
            if (index == null)
                index = map.put(entry, map.size());

            if (sb.length() != 0)
                sb.append(CompoundTableModel.cEntrySeparator);
			sb.append(Integer.toString(firstNo+index));
            }
        return sb.toString();
        }
	}

