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

package com.actelion.research.table.filter;

import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;
import javax.swing.text.Keymap;

import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

public class JTextFilterPanel extends JFilterPanel implements ActionListener,ItemListener,KeyListener {
	private static final long serialVersionUID = 0x20061013;

	private static final String	cOptionContains = "#contains#";
	private static final String	cOptionStartsWith = "#startsWith#";
	private static final String	cOptionEquals = "#equals#";
	private static final String	cOptionRegEx = "#regEx#";
	private static final String	cOptionCaseSensitive = "#caseSensitive#";
	private static final int cOptionIndexRegEx = 3;

	private JComboBox		mComboBox;
	private JTextField		mTextField;
	private JCheckBox		mCheckBox;

	public JTextFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, -1, -1);
		}

	public JTextFilterPanel(CompoundTableModel tableModel, int columnIndex, int exclusionFlag) {
		super(tableModel, columnIndex, exclusionFlag, false);

		JPanel contentPanel = new JPanel();
		double[][] size = { {4, TableLayout.PREFERRED, 4, TableLayout.FILL, 4},
							{TableLayout.PREFERRED, TableLayout.PREFERRED} };
		contentPanel.setLayout(new TableLayout(size));
		contentPanel.setOpaque(false);

		mComboBox = new JComboBox();
		mComboBox.addItem("contains");
		mComboBox.addItem("starts with");
		mComboBox.addItem("equals");
		mComboBox.addItem("matches regex");
		mComboBox.addItemListener(this);
		contentPanel.add(mComboBox, "1,0");

		mTextField = new JTextField(4);
		mTextField.getKeymap().removeKeyStrokeBinding(KeyStroke.getKeyStroke(KeyEvent.VK_H, Event.CTRL_MASK));
		mTextField.addKeyListener(this);
 		contentPanel.add(mTextField, "3,0");

		mCheckBox = new JCheckBox("case sensitive");
		mCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		mCheckBox.addActionListener(this);
		contentPanel.add(mCheckBox, "1,1,3,1");

		add(contentPanel, BorderLayout.CENTER);

		mIsUserChange = true;
		}

	@Override
	public void enableItems(boolean b) {
		mComboBox.setEnabled(b);
		mTextField.setEnabled(b);
		mCheckBox.setEnabled(b && mComboBox.getSelectedIndex() != cOptionIndexRegEx);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBox) {
			updateExclusion(mIsUserChange);
			return;
			}

		super.actionPerformed(e);
		}

	@Override
	public void keyPressed(KeyEvent arg0) {}

	@Override
	public void keyTyped(KeyEvent arg0) {}

	@Override
	public void keyReleased(KeyEvent arg0) {
		updateExclusion(true);
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBox && e.getStateChange() == ItemEvent.SELECTED) {
			updateExclusion(mIsUserChange);
			mCheckBox.setEnabled(mComboBox.getSelectedIndex() != cOptionIndexRegEx);
			}
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (mColumnIndex != JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS)
			super.compoundTableChanged(e);

		mIsUserChange = false;

		if (mColumnIndex == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS) {
			if (e.getType() == CompoundTableEvent.cAddRows
			 || e.getType() == CompoundTableEvent.cRemoveColumns
			 || (e.getType() == CompoundTableEvent.cChangeColumnData
			  && mTableModel.getColumnSpecialType(e.getColumn()) == null))
				updateExclusionLater();
			}
		else if (mColumnIndex == e.getColumn()) {
			if (e.getType() == CompoundTableEvent.cAddRows
			 || e.getType() == CompoundTableEvent.cChangeColumnData)
				updateExclusionLater();
			}

		mIsUserChange = true;
		}

	@Override
	public void updateExclusion(boolean isUserChange) {
		if (!isEnabled())
			return;

		int type = 0;
		switch (mComboBox.getSelectedIndex()) {
		case 0:
			type = CompoundTableModel.cTextExclusionTypeContains;
			break;
		case 1:
			type = CompoundTableModel.cTextExclusionTypeStartsWith;
			break;
		case 2:
			type = CompoundTableModel.cTextExclusionTypeEquals;
			break;
		case 3:
			type = CompoundTableModel.cTextExclusionTypeRegEx;
			break;
			}

		if (isActive()) {
			mTableModel.setStringExclusion(mColumnIndex, mExclusionFlag, mTextField.getText(),
										   type, mCheckBox.isSelected(), isInverse());

			if (isUserChange)
				fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			}
		}

	@Override
	public String getInnerSettings() {
		String text = mTextField.getText();
		if (text.length() != 0 || mComboBox.getSelectedIndex() >= 2) {
			String settings = (mComboBox.getSelectedIndex() == 0) ? cOptionContains
							: (mComboBox.getSelectedIndex() == 1) ? cOptionStartsWith
							: (mComboBox.getSelectedIndex() == 2) ? cOptionEquals : cOptionRegEx;

			if (mCheckBox.isSelected())
				settings = attachSetting(settings, cOptionCaseSensitive);

			settings = attachSetting(settings, text);
			return settings;
			}

		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings != null) {
			int index = -1;
			int type = -1;
			if (settings.startsWith(cOptionContains)) {
				index = 0;
				type = CompoundTableModel.cTextExclusionTypeContains;
				settings = settings.substring(cOptionContains.length()+1);
				}
			else if (settings.startsWith(cOptionStartsWith)) {
				index = 1;
				type = CompoundTableModel.cTextExclusionTypeStartsWith;
				settings = settings.substring(cOptionStartsWith.length()+1);
				}
			else if (settings.startsWith(cOptionEquals)) {
				index = 2;
				type = CompoundTableModel.cTextExclusionTypeEquals;
				settings = settings.substring(cOptionEquals.length()+1);
				}
			else if (settings.startsWith(cOptionRegEx)) {
				index = 3;
				type = CompoundTableModel.cTextExclusionTypeRegEx;
				settings = settings.substring(cOptionRegEx.length()+1);
				}
			if (index != -1) {
				boolean caseSensitive = settings.startsWith(cOptionCaseSensitive);
				if (caseSensitive)
					settings = settings.substring(cOptionCaseSensitive.length()+1);

				mTextField.setText(settings);
				mCheckBox.setSelected(caseSensitive);
				if (mComboBox.getSelectedIndex() != index)
					mComboBox.setSelectedIndex(index);
				else if (isActive())
					mTableModel.setStringExclusion(mColumnIndex, mExclusionFlag,
												   settings, type, caseSensitive, isInverse());
				}
			mCheckBox.setEnabled(isEnabled() && mComboBox.getSelectedIndex() != cOptionIndexRegEx);
			}
		}

	@Override
	public void innerReset() {
		if (mTextField.getText().length() != 0
		 || mComboBox.getSelectedIndex() == 2		// equals
		 || mComboBox.getSelectedIndex() == 3) {	// regex
			mTextField.setText("");
			mCheckBox.setSelected(false);
			if (mComboBox.getSelectedIndex() == 0)
				updateExclusion(false);
			else
				mComboBox.setSelectedIndex(0);	// causes updateExclusion() through itemStateChanged()
			}
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_TEXT;
		}
	}
