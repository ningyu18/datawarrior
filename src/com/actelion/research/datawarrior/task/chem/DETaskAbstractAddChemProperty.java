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

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class simplifies creating new tasks that calculate some stuff based on an existing
 * chemical structure column with or without the need of an associated descriptor.
 * Typically the configuration doesn't need anything except the definition of the structure
 * column used and one or more calculated columns are finally attached to the table.
 * Derived classes may(!) do some pre-processing and must(!) implement either processRow(),
 * which is expected write calculated values directly into the tablemodel, or getNewColumnValue(),
 * which should return calculated values for specific cells. A post-processing may be used to
 * add views, etc.
 * If the configuration requires more parameters than just the chemical structure, then these
 * methods need to be overriden: getExtendedDialogContent(), getDialogConfiguration(),
 * setDialogConfiguration(), setDialogConfigurationToDefault()
 * and possibly isConfigurable(), isConfigurationValid().
 */
public abstract class DETaskAbstractAddChemProperty extends ConfigurableTask implements ActionListener,Runnable {
	protected static final int DESCRIPTOR_NONE = 0;
	protected static final int DESCRIPTOR_BINARY = 1;
	protected static final int DESCRIPTOR_ANY = 2;

	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_DESCRIPTOR_SHORT_NAME = "descriptor";
	private static final String PROPERTY_NEW_COLUMN_NAME = "columnName";

	private JComboBox			mComboBoxStructureColumn,mComboBoxDescriptorColumn;
	private JTextField[]		mTextFieldColumnName;

	private volatile CompoundTableModel	mTableModel;
	private volatile int				mDescriptorClass,mStructureColumn,mDescriptorColumn;
	private volatile boolean			mUseMultipleCores,mEditableColumnNames;
	private AtomicInteger				mSMPRecordIndex,mSMPWorkingThreads,mSMPErrorCount;

	public DETaskAbstractAddChemProperty(DEFrame parent, int descriptorClass, boolean editableColumnNames, boolean useMultipleCores) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		mDescriptorClass = descriptorClass;
		mEditableColumnNames = editableColumnNames;
		mUseMultipleCores = useMultipleCores;
		}

	/**
	 * Derived classes may overwrite this if they need to preprocess all chemistry objects
	 * before actually looping over all to create the calculated value.
	 * Lengthy preprocessings should start and update progress status.
	 * If the preprocessing fails, then call showErrorMessage() return false.
	 * @param configuration
	 * @return true if the preprocessing was successful
	 */
	protected boolean preprocessRows(Properties configuration) {
		return true;
		}

	/**
	 * This method is called after the table is modified with calculated results
	 * and may be overwritten to generate new views etc.
	 * @param firstNewColumn is -1 if getNewColumnCount() returned 0
	 */
	protected void postprocess(int firstNewColumn) {
		}

	/**
	 * Derived classes may overwrite this to assign column properties to the new columns.
	 * This method is only called if getNewColumnCount() does not return 0.
	 * @param firstNewColumn
	 */
	protected void setNewColumnProperties(int firstNewColumn) {
		}

	/**
	 * If new column names can be edited by the user, then the number of new columns
	 * must be a fixed value that is returned by this method. If column names are
	 * predefined and cannot be edited by the user then the number of new columns
	 * may be determined during preprocessing. In this case this method must return
	 * return the correct number of new columns after preprocessing.<br>
	 * This number may be 0 if properties are written into existing columns rather than new ones.
	 * If existing columns are used, then processRow() and postProcess() must be overridden and
	 * finalizeChangeColumn() must be called on all updated columns of the table model (!!!)
	 * @return count of new columns or 0 (only before preprocessing)
	 */
	abstract protected int getNewColumnCount();

	abstract protected String getNewColumnName(int column);

	/**
	 * Derived classes must either override this or override processRow() instead.
	 * @param mol is guaranteed to be != null
	 * @param descriptor
	 * @param column (one of the) new column(s)
	 * @return
	 */
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		return null;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		if (getCompatibleStructureColumnList() == null) {
			String message = "Running '"+getTaskName()+"' requires the presence of chemical structures";
			switch (mDescriptorClass) {
			case DESCRIPTOR_NONE:
				showErrorMessage(message+".");
				break;
			case DESCRIPTOR_BINARY:
				showErrorMessage(message+" with a binary descriptor.");
				break;
			case DESCRIPTOR_ANY:
				showErrorMessage(message+" with any chemical descriptor.");
				break;
				}
			return false;
			}

		return true;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = null;

		if (!mEditableColumnNames && !hasExtendedDialogContent() && isInteractive()) {
			int[] structureColumn = getCompatibleStructureColumnList();
			if (structureColumn != null && structureColumn.length == 1) {
				int[] descriptorColumn = (mDescriptorClass == DESCRIPTOR_NONE) ? null
									   : getCompatibleDescriptorColumnList(structureColumn[0]);
				if (mDescriptorClass == DESCRIPTOR_NONE
				 || (descriptorColumn != null && descriptorColumn.length == 1)) {
					configuration = new Properties();
					configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, mTableModel.getColumnTitleNoAlias(structureColumn[0]));
					if (mDescriptorClass != DESCRIPTOR_NONE)
						configuration.setProperty(PROPERTY_DESCRIPTOR_SHORT_NAME, mTableModel.getDescriptorHandler(descriptorColumn[0]).getInfo().shortName);
					}
				}
			}

		return configuration;
		}

	@Override
	public JPanel createDialogContent() {
		JPanel extentionPanel = getExtendedDialogContent();

		double[] sizeY = new double[3 + (mDescriptorClass != DESCRIPTOR_NONE? 2:0)
		                              + (extentionPanel != null? 3:0)
									  + (mEditableColumnNames? 2*getNewColumnCount() : 0)];
		int index = 0;
		sizeY[index++] = 8;
		sizeY[index++] = TableLayout.PREFERRED;
		if (mDescriptorClass != DESCRIPTOR_NONE) {
			sizeY[index++] = 4;
			sizeY[index++] = TableLayout.PREFERRED;
			}
		if (extentionPanel != null) {
			sizeY[index++] = 12;
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 8;
			}
		if (mEditableColumnNames) {
			for (int i=0; i<getNewColumnCount(); i++) {
				sizeY[index++] = 4;
				sizeY[index++] = TableLayout.PREFERRED;
				}
			}
		sizeY[index++] = 8;
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8}, sizeY };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		int[] structureColumn = getCompatibleStructureColumnList();

		// create components
		mComboBoxStructureColumn = new JComboBox();
		if (structureColumn != null)
			for (int i=0; i<structureColumn.length; i++)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(structureColumn[i]));
		content.add(new JLabel("Structure column:"), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");
		mComboBoxStructureColumn.setEditable(!isInteractive());
		mComboBoxStructureColumn.addActionListener(this);

		index = 3;
		if (mDescriptorClass != DESCRIPTOR_NONE) {
			mComboBoxDescriptorColumn = new JComboBox();
			populateComboBoxDescriptor(structureColumn == null? -1 : structureColumn[0]);
			content.add(new JLabel("Descriptor:"), "1,"+index);
			content.add(mComboBoxDescriptorColumn, "3,"+index);
			index += 2;
			}

		if (extentionPanel != null) {
			content.add(extentionPanel, "1,"+index+",3,"+index);
			index += 3;
			}

		if (mEditableColumnNames) {
			mTextFieldColumnName = new JTextField[getNewColumnCount()];
			for (int i=0; i<getNewColumnCount(); i++) {
				mTextFieldColumnName[i] = new JTextField();
				content.add(new JLabel("New column name:"), "1,"+index);
				content.add(mTextFieldColumnName[i], "3,"+index);
				index += 2;
				}
			}

		return content;
		}

	/**
	 * If this task has no additional configuration items and if there is one structure column
	 * only, then the configuration dialog can be skipped in a live context. This is done by
	 * returning a valid configuration to getPredefinedConfiguration().
	 * @return true if this task has configuration items beyond the structure selection combobox
	 */
	public abstract boolean hasExtendedDialogContent();

	/**
	 * If the implementation needs additional configuration items, then override this method
	 * to return a panel containing the corresponding control elements without any border.
	 * @return null or JPanel with controls
	 */
	public JPanel getExtendedDialogContent() {
		return null;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxStructureColumn)
			populateComboBoxDescriptor(mTableModel.findColumn((String)mComboBoxStructureColumn.getSelectedItem()));
		}

	private void populateComboBoxDescriptor(int structureColumn) {
		if (mComboBoxDescriptorColumn != null) {
			mComboBoxDescriptorColumn.removeAllItems();
			if (structureColumn != -1) {
				int[] descriptorColumn = getCompatibleDescriptorColumnList(structureColumn);
				if (descriptorColumn != null)
					for (int i=0; i<descriptorColumn.length; i++)
						mComboBoxDescriptorColumn.addItem(mTableModel.getDescriptorHandler(descriptorColumn[i]).getInfo().name);
				}
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int idcodeColumn = selectStructureColumn(configuration);
			if (idcodeColumn == -1) {
				showErrorMessage("Structure column not found.");
				return false;
				}
			if (mDescriptorClass != DESCRIPTOR_NONE) {
				String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
				if (descriptorName == null) {
					showErrorMessage("Descriptor column not defined.");
					return false;
					}
				int descriptorColumn = mTableModel.getChildColumn(idcodeColumn, descriptorName);
				if (descriptorColumn == -1) {
					showErrorMessage("Descriptor '"+descriptorName+"' not found.");
					return false;
					}
				}
			}
		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1) {
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));

				if (mComboBoxDescriptorColumn != null) {
					value = configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
					if (value != null) {
						int descriptorColumn = mTableModel.getChildColumn(column, value);
						if (descriptorColumn != -1 || !isInteractive())
							mComboBoxDescriptorColumn.setSelectedItem(DescriptorHelper.shortNameToName(value));
						}
					}
				}
			else if (!isInteractive()) {
				mComboBoxStructureColumn.setSelectedItem(value);
				if (mComboBoxDescriptorColumn != null) {
					value = configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
					if (value != null) {
						mComboBoxDescriptorColumn.setSelectedItem(DescriptorHelper.shortNameToName(value));
						}
					}
				}
			}
		else if (!isInteractive()) {
			mComboBoxStructureColumn.setSelectedItem("Structure");
			}
		else if (mComboBoxStructureColumn.getItemCount() != 0) {
			mComboBoxStructureColumn.setSelectedIndex(0);
			}

		if (mEditableColumnNames) {
			for (int i=0; i<getNewColumnCount(); i++) {
				value = configuration.getProperty(PROPERTY_NEW_COLUMN_NAME+i);
				mTextFieldColumnName[i].setText(value == null ? getNewColumnName(i) : value);
				}
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxStructureColumn.setSelectedItem("Structure");
		if (mComboBoxDescriptorColumn != null
		 && mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		if (mEditableColumnNames)
			for (int i=0; i<getNewColumnCount(); i++)
				mTextFieldColumnName[i].setText(getNewColumnName(i));
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String structureColumn = (String)mComboBoxStructureColumn.getSelectedItem();
		if (structureColumn != null)
			configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, structureColumn);

		if (mComboBoxDescriptorColumn != null) {
			String descriptorName = (String)mComboBoxDescriptorColumn.getSelectedItem();
			if (descriptorName != null) {
				configuration.setProperty(PROPERTY_DESCRIPTOR_SHORT_NAME, DescriptorHelper.nameToShortName(descriptorName));
				}
			}

		if (mEditableColumnNames)
			for (int i=0; i<getNewColumnCount(); i++)
				if (mTextFieldColumnName[i].getText().length() != 0 && !mTextFieldColumnName[i].getText().equals(getNewColumnName(i)))
					configuration.setProperty(PROPERTY_NEW_COLUMN_NAME+i, mTextFieldColumnName[i].getText());

		return configuration;
		}

	@Override
	public void runTask(Properties configuration) {
		mStructureColumn = selectStructureColumn(configuration);
		String shortName = (mDescriptorClass == DESCRIPTOR_NONE) ? null : configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
		mDescriptorColumn = (shortName == null) ? -1 : mTableModel.getChildColumn(mStructureColumn, shortName);
		if (mDescriptorColumn != -1) {
			waitForDescriptor(mTableModel, mDescriptorColumn);
			if (threadMustDie())
				return;
			}

		if (!preprocessRows(configuration))
			return;
		if (threadMustDie())
			return;

		startProgress("Running '"+getTaskName()+"'...", 0, mTableModel.getTotalRowCount());

		int firstNewColumn = -1;
		if (getNewColumnCount() != 0) {	// this allows derived classes to reuse an existing column
			String[] columnName = new String[getNewColumnCount()];
			for (int i=0; i<getNewColumnCount(); i++) {
				columnName[i] = configuration.getProperty(PROPERTY_NEW_COLUMN_NAME+i);
				if (columnName[i] == null)
					columnName[i] = getNewColumnName(i);
				}

			firstNewColumn = mTableModel.addNewColumns(columnName);
			setNewColumnProperties(firstNewColumn);
			}

		if (mUseMultipleCores)
			finishTaskMultiCore(firstNewColumn);
		else
			finishTaskSingleCore(firstNewColumn);

		if (!threadMustDie())
			postprocess(firstNewColumn);
		}

	/**
	 * @param firstNewColumn -1 if getNewColumnCount() returned 0
	 */
	private void finishTaskSingleCore(final int firstNewColumn) {
		int errorCount = 0;

		StereoMolecule containerMol = new StereoMolecule();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if ((row % 16) == 15)
				updateProgress(row);

			try {
				processRow(row, firstNewColumn, containerMol);
				}
			catch (Exception e) {
				errorCount++;
				}
			}

		if (!threadMustDie() && errorCount != 0)
			showErrorCount(errorCount);

		if (getNewColumnCount() != 0)
			mTableModel.finalizeNewColumns(firstNewColumn, this);
		}

	/**
	 * @param firstNewColumn -1 if getNewColumnCount() returns 0
	 */
	private void finishTaskMultiCore(final int firstNewColumn) {
		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPRecordIndex = new AtomicInteger(mTableModel.getTotalRowCount());
		mSMPWorkingThreads = new AtomicInteger(threadCount);
		mSMPErrorCount = new AtomicInteger(0);

		Thread[] t = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			t[i] = new Thread("Abstract ChemProp Calculator "+(i+1)) {
				public void run() {
					StereoMolecule containerMol = new StereoMolecule();
					int recordIndex = mSMPRecordIndex.decrementAndGet();
					while (recordIndex >= 0 && !threadMustDie()) {
						try {
							processRow(recordIndex, firstNewColumn, containerMol);
							}
						catch (Exception e) {
							mSMPErrorCount.incrementAndGet();
							e.printStackTrace();
							}

						updateProgress(-1);
						recordIndex = mSMPRecordIndex.decrementAndGet();
						}

					if (mSMPWorkingThreads.decrementAndGet() == 0) {
						if (!threadMustDie() && mSMPErrorCount.get() != 0)
							showErrorCount(mSMPErrorCount.get());

						if (getNewColumnCount() != 0)
							mTableModel.finalizeNewColumns(firstNewColumn, DETaskAbstractAddChemProperty.this);
						}
					}
				};
			t[i].setPriority(Thread.MIN_PRIORITY);
			t[i].start();
			}

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		for (int i=0; i<threadCount; i++)
			try { t[i].join(); } catch (InterruptedException e) {}
		}

	private void showErrorCount(int errorCount) {
		String message = "The task '"+getTaskName()+"' failed on "+errorCount+" molecules.";
		if (errorCount >= mTableModel.getRowCount())
			showMessage(message, WARNING_MESSAGE);
		}

	/**
	 * Derived classes may overwrite this to directly assign values to compound table cells.
	 * The default implementation calls getNewColumnValue() for every new table cell.
	 * If one or more existing columns are updated rather than all properties written
	 * into new columns, then this method must be overridden and in postProcess()
	 * finalizeChangeColumn() must be called on all updated columns of the table model (!!!).
	 * @param row
	 * @param containerMol container molecule to be repeatedly used
	 * @param firstNewColumn
	 */
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		assert(firstNewColumn != -1);
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null)
			for (int i=0; i<getNewColumnCount(); i++)
				mTableModel.setTotalValueAt(getNewColumnValue(mol, getDescriptor(row), i), row, firstNewColumn+i);
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	/**
	 * @return column containing the idcode when the task is running
	 */
	public int getStructureColumn() {
		return mStructureColumn;
		}

	/**
	 * Uses mTableModel.getChemicalStructure() to create a non-colored molecule.
	 * @param row
	 * @param mol null or a container molecule that is filled and returned
	 * @return null or valid molecule
	 */
	public StereoMolecule getChemicalStructure(int row, StereoMolecule mol) {
		return mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, mol);
		}

	public Object getDescriptor(int row) {
		return (mDescriptorColumn == -1) ? null : mTableModel.getTotalRecord(row).getData(mDescriptorColumn);
		}

	private int selectStructureColumn(Properties configuration) {
		int[] idcodeColumn = getCompatibleStructureColumnList();
		if (idcodeColumn.length == 1)
			return idcodeColumn[0];	// there is no choice
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN));
		for (int i=0; i<idcodeColumn.length; i++)
			if (column == idcodeColumn[i])
				return column;
		return -1;
		}

	private int[] getCompatibleStructureColumnList() {
		int[] structureColumn = null;

		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumn != null) {
			int count = 0;
			for (int i=0; i<idcodeColumn.length; i++)
				if (mDescriptorClass == DESCRIPTOR_NONE || getCompatibleDescriptorColumnList(idcodeColumn[i]) != null)
					count++;

			if (count != 0) {
				structureColumn = new int[count];
				count = 0;
				for (int i=0; i<idcodeColumn.length; i++)
					if (mDescriptorClass == DESCRIPTOR_NONE || getCompatibleDescriptorColumnList(idcodeColumn[i]) != null)
						structureColumn[count++] = idcodeColumn[i];
				}
			}
		
		return structureColumn;
		}

	private int[] getCompatibleDescriptorColumnList(int parentColumn) {
		int[] descriptorColumn = null;

		int count = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getParentColumn(column) == parentColumn && isCompatibleDescriptorColumn(column))
				count++;

		if (count != 0) {
			descriptorColumn = new int[count];
			count = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getParentColumn(column) == parentColumn && isCompatibleDescriptorColumn(column))
					descriptorColumn[count++] = column;
			}

		return descriptorColumn;
		}

	private boolean isCompatibleDescriptorColumn(int column) {
		if (mTableModel.isDescriptorColumn(column)) {
			DescriptorHandler<?,?> dh = mTableModel.getDescriptorHandler(column);
			if (dh.getInfo().type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) {
				if (mDescriptorClass == DESCRIPTOR_ANY)
					return true;
				if (mDescriptorClass == DESCRIPTOR_BINARY && dh.getInfo().isBinary)
					return true;
				}
			}
		return false;
		}
	}
