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

package com.actelion.research.datawarrior.task.chem;

import java.sql.SQLException;
import java.util.Properties;

import com.actelion.research.chem.*;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundRecord;
import info.clearthought.layout.TableLayout;

import javax.swing.*;

public class DETaskAddLargestFragment extends DETaskAbstractAddChemProperty {
	public static final String TASK_NAME = "Add Largest Fragment";

	private static final String PROPERTY_NEUTRALIZE = "neutralize";

	private JCheckBox mCheckBoxNeutralize;
	private boolean mNeutralizeFragment;

    public DETaskAddLargestFragment(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		}

	@Override
	protected int getNewColumnCount() {
		int count = 1;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (getTableModel().getParentColumn(column) == getStructureColumn() && isCoordinateColumn(column))
			  count++;
		return count;
		}

	@Override
	protected String getNewColumnName(int column) {
		// is done by setNewColumnProperties()
		return "";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED} };

		mCheckBoxNeutralize = new JCheckBox("Neutralize charges");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxNeutralize, "0,0");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEUTRALIZE, mCheckBoxNeutralize.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxNeutralize.setSelected(true);
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getStructureColumn());

		getTableModel().setColumnName("Largest Fragment of " + sourceColumnName, firstNewColumn);
		getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType,
				CompoundTableConstants.cColumnTypeIDCode);

		int count = 1;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (getTableModel().getParentColumn(column) == getStructureColumn() && isCoordinateColumn(column)) {
				getTableModel().setColumnName("fragmentCoordinates"+count, firstNewColumn+count);
				getTableModel().setColumnProperty(firstNewColumn+count,
						CompoundTableConstants.cColumnPropertySpecialType, getTableModel().getColumnSpecialType(column));
				getTableModel().setColumnProperty(firstNewColumn+count,
						CompoundTableConstants.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(firstNewColumn));
				count++;
				}
			}
		}

	private boolean isCoordinateColumn(int column) {
		return CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(column))
			|| CompoundTableConstants.cColumnType3DCoordinates.equals(getTableModel().getColumnSpecialType(column));
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mNeutralizeFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE));
		return super.preprocessRows(configuration);
    	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		byte[] idcode = (byte[])record.getData(getStructureColumn());
		if (idcode != null) {
			int count = 0;
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
				if (getTableModel().getParentColumn(column) == getStructureColumn()) {
					if (record.getData(column) != null && isCoordinateColumn(column)) {
						count++;
						boolean is2D = CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(column));
						StereoMolecule mol = new IDCodeParser(is2D).getCompactMolecule(idcode, (byte[])record.getData(column));
						mol.stripSmallFragments();
						new MoleculeNeutralizer().neutralizeChargedMolecule(mol);
						Canonizer canonizer = new Canonizer(mol);
						getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
						getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstNewColumn+count);
						}
					}
				}
			if (count == 0) {
				StereoMolecule mol = getChemicalStructure(row, containerMol);
				if (mol != null) {
					mol.stripSmallFragments();
					Canonizer canonizer = new Canonizer(mol);
					getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
					}
				}
			}
		}
	}
