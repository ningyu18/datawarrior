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

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.optimizer.EvaluableConformation;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.forcefield.optimizer.OptimizerLBFGS;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;
import org.openmolecules.chem.conf.so.SelfOrganizedConformer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.SynchronousQueue;


public class DETaskAdd3DCoordinates extends DETaskAbstractAddChemProperty implements Runnable {
	public static final String TASK_NAME = "Generate Conformers";

	private static final String PROPERTY_ALGORITHM = "algorithm";
	private static final String PROPERTY_TORSION_SOURCE = "torsionSource";
	private static final String PROPERTY_MINIMIZE = "minimize";
	private static final String PROPERTY_FILE_NAME = "fileName";
	private static final String PROPERTY_FILE_TYPE = "fileType";
	private static final String PROPERTY_MAX_CONFORMERS = "maxConformers";
	private static final String PROPERTY_LARGEST_FRAGMENT = "largestFragment";
	private static final String PROPERTY_NEUTRALIZE_FRAGMENT = "neutralize";
	private static final String PROPERTY_PROTONATION_PH = "protonationPH";
	private static final String PROPERTY_PROTONATION_SPAN = "protonationSpan";
	private static final String PROPERTY_STEREO_ISOMER_LIMIT = "stereoIsomerLimit";

	private static final String[] TORSION_SOURCE_TEXT = { "From crystallographic database", "Use 60 degree steps" };
	private static final String[] TORSION_SOURCE_CODE = { "crystallDB", "6steps" };
	private static final int TORSION_SOURCE_CRYSTAL_DATA = 0;
	private static final int TORSION_SOURCE_6_STEPS = 1;
	private static final int DEFAULT_TORSION_SOURCE = TORSION_SOURCE_CRYSTAL_DATA;

	private static final String[] MINIMIZE_TEXT = { "MMFF94s+ forcefield", "MMFF94s forcefield", "Idorsia forcefield", "Don't minimize" };
	private static final String[] MINIMIZE_CODE = { "mmff94+", "mmff94", "actelion", "none" };
	private static final String[] MINIMIZE_TITLE = { "mmff94s+", "mmff94s", "Idorsia-FF", "not minimized" };
	private static final int MINIMIZE_MMFF94sPlus = 0;
	private static final int MINIMIZE_MMFF94s = 1;
	private static final int MINIMIZE_IDORSIA_FORCEFIELD = 2;
	private static final int MINIMIZE_NONE = 3;
	private static final int DEFAULT_MINIMIZATION = MINIMIZE_MMFF94sPlus;
	private static final String DEFAULT_MAX_CONFORMERS = "16";
	private static final int MAX_CONFORMERS = 1024;
	private static final String DEFAULT_MAX_STEREO_ISOMERS = "64";

	private static final int LOW_ENERGY_RANDOM = 0;
	private static final int PURE_RANDOM = 1;
	private static final int ADAPTIVE_RANDOM = 2;
	private static final int SYSTEMATIC = 3;
	private static final int SELF_ORGANIZED = 4;
	private static final int ACTELION3D = 5;
	private static final int DEFAULT_ALGORITHM = LOW_ENERGY_RANDOM;
	private static final int FILE_TYPE_NONE = -1;

	private static final String[] ALGORITHM_TEXT = { "Random, low energy bias", "Pure random", "Adaptive collision avoidance, low energy bias", "Systematic, low energy bias", "Self-organized" };
	private static final String[] ALGORITHM_CODE = { "lowEnergyRandom", "pureRandom", "adaptiveRandom", "systematic", "selfOrganized", "actelion3d" };
	private static final boolean[] ALGORITHM_NEEDS_TORSIONS = { true, true, true, true, false, false };
	private static final String ALGORITHM_TEXT_ACTELION3D = "Actelion3D";

	private static final String[] FILE_TYPE_TEXT = { "DataWarrior", "SD-File Version 2", "SD-File Version 3" };
	private static final String[] FILE_TYPE_CODE = { "dwar", "sdf2", "sdf3" };
	private static final int[] FILE_TYPE = { FileHelper.cFileTypeDataWarrior, FileHelper.cFileTypeSDV2, FileHelper.cFileTypeSDV3 };

/*	private static final String[] ALGORITHM_TEXT = { "Actelion3D" };
	private static final String[] ALGORITHM_CODE = { "actelion3d" };
	private static final int ACTELION3D = 0;
	private static final int DEFAULT_ALGORITHM = ACTELION3D;
*/
	private JComboBox			mComboBoxAlgorithm,mComboBoxTorsionSource,mComboBoxMinimize,mComboBoxFileType;
	private JCheckBox			mCheckBoxExportFile,mCheckBoxLargestFragment,mCheckBoxNeutralize,mCheckBoxSkip,mCheckBoxProtonate;
	private JFilePathLabel		mLabelFileName;
	private JTextField			mTextFieldMaxCount,mTextFieldSkip,mTextFieldPH,mTextFieldPHSpan;
	private JButton				mButtonEdit;
	private volatile boolean	mCheckOverwrite;
	private volatile boolean	mLargestFragmentOnly,mNeutralizeLargestFragment,mMarvinAvailable;
	private volatile float		mPH1,mPH2;
	private volatile int		mAlgorithm,mMinimization,mTorsionSource,mMinimizationErrors,mFileType,mMaxConformers,
								mStereoIsomerLimit,mIdentifierColumn;
	private volatile BufferedWriter mFileWriter;
	private volatile Map<String,Object> mMMFFOptions;
	private volatile SynchronousQueue<String> mRowQueue;

	public DETaskAdd3DCoordinates(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		mCheckOverwrite = true;
		try {
			Class.forName("chemaxon.marvin.calculations.pKaPlugin");
			mMarvinAvailable = true;
			}
		catch (ClassNotFoundException cnfe) {
			mMarvinAvailable = false;
			}
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/conformers.html#Generate";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 24, TableLayout.PREFERRED,
							8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Algorithm:"), "0,0");
		mComboBoxAlgorithm = new JComboBox(ALGORITHM_TEXT);
		mComboBoxAlgorithm.addActionListener(this);
		if (System.getProperty("development") != null)
			mComboBoxAlgorithm.addItem(ALGORITHM_TEXT_ACTELION3D);
		ep.add(mComboBoxAlgorithm, "2,0,4,0");
		ep.add(new JLabel("Initial torsions:"), "0,2");
		mComboBoxTorsionSource = new JComboBox(TORSION_SOURCE_TEXT);
		ep.add(mComboBoxTorsionSource, "2,2,4,2");
		ep.add(new JLabel("Minimize energy:"), "0,4");
		mComboBoxMinimize = new JComboBox(MINIMIZE_TEXT);
		ep.add(mComboBoxMinimize, "2,4,4,4");

		mCheckBoxExportFile = new JCheckBox("Write into file:");
		ep.add(mCheckBoxExportFile, "0,6");
		mCheckBoxExportFile.addActionListener(this);

		mLabelFileName = new JFilePathLabel(!isInteractive());
		ep.add(mLabelFileName, "2,6,3,6");

		mButtonEdit = new JButton("Edit");
		mButtonEdit.addActionListener(this);
		ep.add(mButtonEdit, "4,6");

		ep.add(new JLabel("File type:"), "0,8");
		mComboBoxFileType = new JComboBox(FILE_TYPE_TEXT);
		mComboBoxFileType.addActionListener(this);
		ep.add(mComboBoxFileType, "2,8");

		ep.add(new JLabel("Max. conformer count:"), "0,10");
		mTextFieldMaxCount = new JTextField();
		ep.add(mTextFieldMaxCount, "2,10");
		ep.add(new JLabel(" per stereo isomer"), "3,10,4,10");

		mCheckBoxLargestFragment = new JCheckBox("Remove small fragments");
		mCheckBoxLargestFragment.addActionListener(this);
		ep.add(mCheckBoxLargestFragment, "2,12,4,12");

		mCheckBoxNeutralize = new JCheckBox("Neutralize remaining fragment");
		mCheckBoxNeutralize.addActionListener(this);
		ep.add(mCheckBoxNeutralize, "2,14,4,14");

		mCheckBoxSkip = new JCheckBox("Skip compounds with more than ");
		mCheckBoxSkip.addActionListener(this);
		mTextFieldSkip = new JTextField(2);
		JPanel skipPanel = new JPanel();
		skipPanel.add(mCheckBoxSkip);
		skipPanel.add(mTextFieldSkip);
		skipPanel.add(new JLabel(" stereo isomers"));
		ep.add(skipPanel, "0,16,4,16");

		mCheckBoxProtonate = new JCheckBox("Create proper protonation state(s) for pH=");
		mCheckBoxProtonate.addActionListener(this);
		mTextFieldPH = new JTextField(2);
		mTextFieldPHSpan = new JTextField(2);
		JPanel protonationPanel = new JPanel();
		protonationPanel.add(mCheckBoxProtonate);
		protonationPanel.add(mTextFieldPH);
		protonationPanel.add(new JLabel(" +-"));
		protonationPanel.add(mTextFieldPHSpan);
		ep.add(protonationPanel, "0,18,4,18");

		return ep;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxAlgorithm) {
			mComboBoxTorsionSource.setEnabled(ALGORITHM_NEEDS_TORSIONS[mComboBoxAlgorithm.getSelectedIndex()]);
			return;
			}
		if (e.getSource() == mButtonEdit) {
			String filename = new FileHelper(getParentFrame()).selectFileToSave(
					"Write Conformers To File", FILE_TYPE[mComboBoxFileType.getSelectedIndex()], "conformers");
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			}
		if (e.getSource() == mCheckBoxExportFile) {
			if (mCheckBoxExportFile.isSelected()) {
				if (mLabelFileName.getPath() == null) {
					String filename = new FileHelper(getParentFrame()).selectFileToSave(
							"Write Conformers To File", FILE_TYPE[mComboBoxFileType.getSelectedIndex()], "conformers");
					if (filename != null) {
						mLabelFileName.setPath(filename);
						mCheckOverwrite = false;
						}
					else {
						mCheckBoxExportFile.setSelected(false);
						mLabelFileName.setPath(null);
						}
					}
				}

			enableItems();
			return;
			}
		if (e.getSource() == mCheckBoxLargestFragment) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxNeutralize) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxSkip) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxProtonate) {
			enableItems();
			}
		if (e.getSource() == mComboBoxFileType) {
			String filePath = mLabelFileName.getPath();
			if (filePath != null) {
				mLabelFileName.setPath(FileHelper.removeExtension(filePath)
						+ FileHelper.getExtension(FILE_TYPE[mComboBoxFileType.getSelectedIndex()]));
				}
			return;
			}

		super.actionPerformed(e);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_ALGORITHM, ALGORITHM_CODE[mComboBoxAlgorithm.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_TORSION_SOURCE, TORSION_SOURCE_CODE[mComboBoxTorsionSource.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_MINIMIZE, MINIMIZE_CODE[mComboBoxMinimize.getSelectedIndex()]);

		if (mCheckBoxExportFile.isSelected()) {
			configuration.setProperty(PROPERTY_FILE_NAME, mLabelFileName.getPath());
			configuration.setProperty(PROPERTY_FILE_TYPE, FILE_TYPE_CODE[mComboBoxFileType.getSelectedIndex()]);
			configuration.setProperty(PROPERTY_MAX_CONFORMERS, mTextFieldMaxCount.getText());
			configuration.setProperty(PROPERTY_LARGEST_FRAGMENT, mCheckBoxLargestFragment.isSelected()?"true":"false");
			configuration.setProperty(PROPERTY_NEUTRALIZE_FRAGMENT, mCheckBoxNeutralize.isSelected()?"true":"false");
			if (mCheckBoxSkip.isSelected()) {
				configuration.setProperty(PROPERTY_STEREO_ISOMER_LIMIT, mTextFieldSkip.getText());
				}
			if (mCheckBoxProtonate.isSelected()) {
				configuration.setProperty(PROPERTY_PROTONATION_PH, mTextFieldPH.getText());
				if (mTextFieldPHSpan.getText().length() != 0 && !mTextFieldPHSpan.getText().equals("0"))
					configuration.setProperty(PROPERTY_PROTONATION_SPAN, mTextFieldPHSpan.getText());
				}
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxAlgorithm.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM));
		mComboBoxTorsionSource.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_TORSION_SOURCE), TORSION_SOURCE_CODE, DEFAULT_TORSION_SOURCE));
		mComboBoxMinimize.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MINIMIZE), MINIMIZE_CODE, DEFAULT_MINIMIZATION));

		String value = configuration.getProperty(PROPERTY_FILE_NAME);
		mCheckBoxExportFile.setSelected(value != null);
		mLabelFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);

		mComboBoxFileType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0));
		mTextFieldMaxCount.setText(configuration.getProperty(PROPERTY_MAX_CONFORMERS, DEFAULT_MAX_CONFORMERS));
		mCheckBoxLargestFragment.setSelected(!"false".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT)));
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE_FRAGMENT)));

		value = configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT);
		mCheckBoxSkip.setSelected(value != null);
		mTextFieldSkip.setText(value != null ? value : DEFAULT_MAX_STEREO_ISOMERS);

		mCheckBoxProtonate.setSelected(configuration.getProperty(PROPERTY_PROTONATION_PH) != null);
		mTextFieldPH.setText(configuration.getProperty(PROPERTY_PROTONATION_PH, "7.4"));
		mTextFieldPHSpan.setText(configuration.getProperty(PROPERTY_PROTONATION_SPAN, "0"));

		enableItems();
		mComboBoxTorsionSource.setEnabled(ALGORITHM_NEEDS_TORSIONS[mComboBoxAlgorithm.getSelectedIndex()]);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxAlgorithm.setSelectedIndex(DEFAULT_ALGORITHM);
		mComboBoxTorsionSource.setSelectedIndex(DEFAULT_TORSION_SOURCE);
		mComboBoxMinimize.setSelectedIndex(DEFAULT_MINIMIZATION);
		mCheckBoxExportFile.setSelected(false);
		mTextFieldMaxCount.setText(DEFAULT_MAX_CONFORMERS);
		mCheckBoxLargestFragment.setSelected(true);
		mCheckBoxNeutralize.setSelected(false);
		mCheckBoxSkip.setSelected(true);
		mTextFieldSkip.setText(DEFAULT_MAX_STEREO_ISOMERS);
		mCheckBoxProtonate.setSelected(false);
		mTextFieldPH.setText("7.4");
		mTextFieldPHSpan.setText("0");

		enableItems();
		mComboBoxTorsionSource.setEnabled(ALGORITHM_NEEDS_TORSIONS[mComboBoxAlgorithm.getSelectedIndex()]);
		}

	private void enableItems() {
		boolean isEnabled = mCheckBoxExportFile.isSelected();
		mLabelFileName.setEnabled(isEnabled);
		mComboBoxFileType.setEnabled(isEnabled);
		mTextFieldMaxCount.setEnabled(isEnabled);
		mButtonEdit.setEnabled(isEnabled);
		mCheckBoxLargestFragment.setEnabled(isEnabled);
		mCheckBoxNeutralize.setEnabled(isEnabled && mCheckBoxLargestFragment.isSelected() && (!mMarvinAvailable || !mCheckBoxProtonate.isSelected()));
		mCheckBoxSkip.setEnabled(isEnabled);
		mTextFieldSkip.setEnabled(mCheckBoxSkip.isEnabled() && mCheckBoxSkip.isSelected());
		mCheckBoxProtonate.setEnabled(isEnabled && mMarvinAvailable && (!mCheckBoxLargestFragment.isSelected() || !mCheckBoxNeutralize.isSelected()));
		mTextFieldPH.setEnabled(mCheckBoxProtonate.isEnabled() && mCheckBoxProtonate.isSelected());
		mTextFieldPHSpan.setEnabled(mCheckBoxProtonate.isEnabled() && mCheckBoxProtonate.isSelected());
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		String fileName = configuration.getProperty(PROPERTY_FILE_NAME);
		if (fileName != null) {
			if (!isFileAndPathValid(fileName, true, mCheckOverwrite))
				return false;
			int fileType = FILE_TYPE[findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0)];
			String extension = FileHelper.getExtension(fileType);
			if (!fileName.endsWith(extension)) {
				showErrorMessage("Wrong file extension for file type '"+FILE_TYPE_TEXT[fileType]+"'.");
				return false;
				}
			try {
				int count = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_CONFORMERS));
				if (count < 1 || count > MAX_CONFORMERS) {
					showErrorMessage("The maximum conformer count must be between 1 and "+MAX_CONFORMERS);
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("The maximum conformer count is not numerical");
				return false;
				}
			String stereoIsomerLimit = configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT);
			if (stereoIsomerLimit != null) {
				try {
					int count = Integer.parseInt(configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT));
					if (count < 1) {
						showErrorMessage("The stereo isomer limit must not be lower than 1");
						return false;
						}
					}
				catch (NumberFormatException nfe) {
					showErrorMessage("The stereo isomer limit is not numerical");
					return false;
					}
				}
			String pH = configuration.getProperty(PROPERTY_PROTONATION_PH);
			if (pH != null) {
				if (pH.length() == 0) {
					showErrorMessage("No pH-value given.");
					return false;
					}
				try {
					float value = Float.parseFloat(pH);
					if (value < 0 || value > 14) {
						showErrorMessage("The pH value must be between 0 and 14.");
						return false;
						}
					String span = configuration.getProperty(PROPERTY_PROTONATION_SPAN);
					if (span != null) {
						try {
							float d = Float.parseFloat(span);
							if (value-d < 0 || value+d > 14) {
								showErrorMessage("The pH range must be between 0 and 14.");
								return false;
								}
							}
						catch (NumberFormatException nfe) {
							showErrorMessage("The pH range value is neither empty not numerical");
							return false;
							}
						}
					}
				catch (NumberFormatException nfe) {
					showErrorMessage("The pH value is not numerical");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	protected int getNewColumnCount() {
		return (mFileType != FILE_TYPE_NONE) ? 0 : (mMinimization == MINIMIZE_MMFF94s || mMinimization == MINIMIZE_MMFF94sPlus) ? 2 : 1;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		if (mFileType == FILE_TYPE_NONE) {
			getTableModel().setColumnProperty(firstNewColumn,
					CompoundTableModel.cColumnPropertySpecialType,
					CompoundTableModel.cColumnType3DCoordinates);
			getTableModel().setColumnProperty(firstNewColumn,
					CompoundTableModel.cColumnPropertyParentColumn,
					getTableModel().getColumnTitleNoAlias(getStructureColumn()));
			}
		}

	@Override
	protected String getNewColumnName(int column) {
		switch (column) {
		case 0:
			String title = "3D-"+getTableModel().getColumnTitle(getStructureColumn());
			switch (mAlgorithm) {
			case ADAPTIVE_RANDOM:
				return title+" (adaptive torsions, "+MINIMIZE_TITLE[mMinimization]+")";
			case SYSTEMATIC:
				return title+" (systematic torsions, "+MINIMIZE_TITLE[mMinimization]+")";
			case LOW_ENERGY_RANDOM:
				return title+" (low-energy random, "+MINIMIZE_TITLE[mMinimization]+")";
			case PURE_RANDOM:
				return title+" (pure random, "+MINIMIZE_TITLE[mMinimization]+")";
			case SELF_ORGANIZED:
				return title+" (self-organized, "+MINIMIZE_TITLE[mMinimization]+")";
			case ACTELION3D:
				return title+" (Actelion3D, "+MINIMIZE_TITLE[mMinimization]+")";
			default:	// should not happen
				return CompoundTableModel.cColumnType3DCoordinates;
				}
		case 1:
			return "MMFF94 Energy";
		default:
			return "Unknown";
			}
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mAlgorithm = findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM);
		mTorsionSource = findListIndex(configuration.getProperty(PROPERTY_TORSION_SOURCE), TORSION_SOURCE_CODE, DEFAULT_TORSION_SOURCE);
		mMinimization = findListIndex(configuration.getProperty(PROPERTY_MINIMIZE), MINIMIZE_CODE, DEFAULT_MINIMIZATION);
		if (mMinimization == MINIMIZE_MMFF94s) {
			mmff.ForceField.initialize(mmff.ForceField.MMFF94S);
			mMMFFOptions = new HashMap<String, Object>();
			}
		else if (mMinimization == MINIMIZE_MMFF94sPlus) {
			mmff.ForceField.initialize(mmff.ForceField.MMFF94SPLUS);
			mMMFFOptions = new HashMap<String, Object>();
			}
		mMinimizationErrors = 0;

		mStereoIsomerLimit = 0;
		String limit = configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT);
		if (limit != null)
			mStereoIsomerLimit = Integer.parseInt(limit);

		mPH1 = Float.NaN;
		String ph = mMarvinAvailable ? configuration.getProperty(PROPERTY_PROTONATION_PH) : null;
		if (ph != null) {
			mPH1 = Float.parseFloat(ph);
			mPH2 = mPH1;
			String span = configuration.getProperty(PROPERTY_PROTONATION_SPAN);
			if (span != null) {
				float d = Float.parseFloat(span);
				mPH2 = mPH1 + d;
				mPH1 -= d;
				}
			}

		mFileType = FILE_TYPE_NONE;	// default
		String fileName = configuration.getProperty(PROPERTY_FILE_NAME);
		if (fileName != null) {
			mFileType = FILE_TYPE[findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0)];
			mMaxConformers = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_CONFORMERS));
			mLargestFragmentOnly = !"false".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT));
			mNeutralizeLargestFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE_FRAGMENT));

			String columnName = getTableModel().getColumnProperty(getStructureColumn(), CompoundTableConstants.cColumnPropertyIdentifierColumn);
			mIdentifierColumn = (columnName == null) ? -1 : getTableModel().findColumn(columnName);

			try {
				mFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(fileName)), "UTF-8"));

				if (mFileType == FileHelper.cFileTypeDataWarrior)
					writeDataWarriorHeader();
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				return false;
				}

			mRowQueue = new SynchronousQueue<String>();
			new Thread(new Runnable() {
				@Override
				public void run() {
					consumeRows();
					}
				} ).start();
			}

		return true;
		}

	private void consumeRows() {
		String row;
		try {
			while ((row = mRowQueue.take()).length() != 0) {
				try {
					mFileWriter.write(row);
					} catch (IOException ioe) { break; }
				}
			} catch (InterruptedException ie) {}

		try {
			if (mFileType == FileHelper.cFileTypeDataWarrior)
				writeDataWarriorFooter();

			mFileWriter.close();
			}
		catch (IOException ioe) {
			showErrorMessage(ioe.toString());
			}
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule mol) throws Exception {
		if (mFileType != FILE_TYPE_NONE) {
			addConformersToQueue(row, mol);
			}
		else {
			addConformerToTable(row, firstNewColumn, mol);
			}
		}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (mRowQueue != null)
			try { mRowQueue.put(""); } catch (InterruptedException ie) {}  // to release consuming thread

		if (mMinimizationErrors != 0 && isInteractive())
			showInteractiveTaskMessage("Forcefield minimization failed in "+mMinimizationErrors+" cases.", JOptionPane.INFORMATION_MESSAGE);
		}

	private void addConformerToTable(int row, int firstNewColumn, StereoMolecule mol) throws Exception {
		mol = getChemicalStructure(row, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

		boolean isOneStereoIsomer = !hasMultipleStereoIsomers(mol);
		FFMolecule ffmol = null;
		ConformerGenerator cg;

		switch (mAlgorithm) {
		case ADAPTIVE_RANDOM:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
			mol = cg.getNextConformer(mol);
			break;
		case SYSTEMATIC:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
			mol = cg.getNextConformer(mol);
			break;
		case LOW_ENERGY_RANDOM:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
			mol = cg.getNextConformer(mol);
			break;
		case PURE_RANDOM:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
			mol = cg.getNextConformer(mol);
			break;
		case SELF_ORGANIZED:
			//from here ConformationSampler based
			ConformationSelfOrganizer sampler = new ConformationSelfOrganizer(mol, false);
			sampler.generateOneConformerInPlace(0);
			break;
		case ACTELION3D:
			// from here AdvancedTools based
			try {
				List<FFMolecule> isomerList = TorsionCalculator.createAllConformations(new FFMolecule(mol));
				if (isomerList.size() != 0)
					ffmol = isomerList.get(0);
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			break;
			}


		if (mol != null && mol.getAllAtoms() != 0) {
//			String rawCoords = (mMinimization != MINIMIZE_MMFF94) ? null : new Canonizer(mol).getEncodedCoordinates(true);

			MinimizationResult result = new MinimizationResult();
			minimize(mol, ffmol, result);

			centerConformer(mol);
			Canonizer canonizer = new Canonizer(mol);
			if (isOneStereoIsomer	// a final conformer is one stereo isomer
			 && !canonizer.getIDCode().equals(getTableModel().getTotalValueAt(row, getStructureColumn()))) {
				System.out.println("WARNING: idcodes after 3D-coordinate generation differ!!!");
				System.out.println("old: "+getTableModel().getTotalValueAt(row, getStructureColumn()));
				System.out.println("new: "+canonizer.getIDCode());
				}
			getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(true), row, firstNewColumn);
			if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
				String energyText = (result.errorMessage != null) ? result.errorMessage : DoubleFormat.toString(result.energy);
				getTableModel().setTotalValueAt(energyText, row, firstNewColumn+1);
				}
			}
		else {
			getTableModel().setTotalValueAt(null, row, firstNewColumn);
			if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
				getTableModel().setTotalValueAt(null, row, firstNewColumn+1);
				}
			}
		}

	private void addConformersToQueue(int row, StereoMolecule mol) throws Exception {
		mol = getChemicalStructure(row, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

		if (mLargestFragmentOnly) {
			mol.stripSmallFragments();
			if (mNeutralizeLargestFragment)
				new MoleculeNeutralizer().neutralizeChargedMolecule(mol);
			}

		// don't create any protonation states
		if (Float.isNaN(mPH1)) {
			StringBuilder builder = new StringBuilder();
			addConformersToQueue2(row, -1, mol, builder);
			if (builder.length() != 0)
				mRowQueue.put(builder.toString());

			return;
			}

		double[] basicpKa = new double[3];
		double[] acidicpKa = new double[3];
		int[] basicAtom = new int[3];
		int[] acidicAtom = new int[3];

		for (int i=0; i<3; i++) {
			basicpKa[i] = Double.NaN;
			acidicpKa[i] = Double.NaN;
			}

		new PKaPredictor().getProtonationStates(mol, basicpKa, acidicpKa, basicAtom, acidicAtom);

		ArrayList<PKa> pKaList = new ArrayList<PKa>();
		for (int i=0; i<3 && !Double.isNaN(basicpKa[i]); i++)
			pKaList.add(new PKa(basicAtom[i], basicpKa[i], true));
		for (int i=0; i<3 && !Double.isNaN(acidicpKa[i]); i++)
			pKaList.add(new PKa(acidicAtom[i], acidicpKa[i], false));

		PKa[] pKa = pKaList.toArray(new PKa[0]);
		Arrays.sort(pKa);

		// determine indexes of pKa values that are within the pH-range
		int i1 = 0;
		while (i1 < pKa.length && pKa[i1].pKa < mPH1)
			i1++;
		int i2 = i1;
		while (i2 < pKa.length && pKa[i2].pKa <= mPH2)
			i2++;

		for (int i=0; i<i1; i++)
			mol.setAtomCharge(pKa[i].atom, pKa[i].isBasic ? 0 : -1);
		for (int i=i1; i<pKa.length; i++)
			mol.setAtomCharge(pKa[i].atom, pKa[i].isBasic ? 1 : 0);

		StringBuilder builder = new StringBuilder();
		addConformersToQueue2(row, 0, new StereoMolecule(mol), builder);
		for (int i=i1; i<i2; i++) {
			mol.setAtomCharge(pKa[i].atom, pKa[i].isBasic ? 0 : -1);
			addConformersToQueue2(row, i-i1+1, new StereoMolecule(mol), builder);
			}
		if (builder.length() != 0)
			mRowQueue.put(builder.toString());
		}

	private class PKa implements Comparable<PKa> {
		int atom;
		double pKa;
		boolean isBasic;

		public PKa(int atom, double pKa, boolean isBasic) {
			this.atom = atom;
			this.pKa = pKa;
			this.isBasic = isBasic;
			}

		@Override public int compareTo(PKa o) {
			return this.pKa > o.pKa ? 1 : this.pKa == o.pKa ? 0 : -1;
			}
		}

	private void addConformersToQueue2(int row, int protonationState, StereoMolecule mol, StringBuilder builder) throws Exception {
		StereoIsomerEnumerator sie = new StereoIsomerEnumerator(mol, true);
		int isomerCount = sie.getStereoIsomerCount();
		boolean createEnantiomers = sie.isSkippingEnantiomers();

		if (isomerCount <= mStereoIsomerLimit)
			for (int i=0; i<isomerCount; i++)
				addConformersToQueue3(row, protonationState, i, sie, createEnantiomers, builder);
		}

	private void addConformersToQueue3(int row, int protonationState, int stereoIsomer, StereoIsomerEnumerator stereoIsomerEnumerator, boolean createEnantiomers, StringBuilder builder) throws Exception {
		ConformerGenerator cg = null;
		ConformationSelfOrganizer cs = null;
		List<FFMolecule> isomerList = null;
		int[] rotatableBond = null;
		ArrayList<TorsionDescriptor> torsionDescriptorList = null;

		int maxTorsionSets = (int)Math.max(2 * mMaxConformers, (1000 * Math.sqrt(mMaxConformers)));

		StereoMolecule mol = stereoIsomerEnumerator.getStereoIsomer(stereoIsomer);

		for (int i=0; i<mMaxConformers; i++) {
			FFMolecule ffmol = null;

			switch (mAlgorithm) {
			case ADAPTIVE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformer(mol);
				break;
			case SYSTEMATIC:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformer(mol);
				break;
			case LOW_ENERGY_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformer(mol);
				break;
			case PURE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformer(mol);
				break;
			case SELF_ORGANIZED:
				if (cs == null) {
					ConformerGenerator.addHydrogenAtoms(mol);
					cs = new ConformationSelfOrganizer(mol, true);
					cs.initializeConformers(0, mMaxConformers);
					}
				SelfOrganizedConformer soc = cs.getNextConformer();
				if (soc == null)
					mol = null;
				else
					soc.toMolecule(mol);
				break;
			case ACTELION3D:
				try {
					if (isomerList == null) {
						ConformerGenerator.addHydrogenAtoms(mol);
						isomerList = TorsionCalculator.createAllConformations(new FFMolecule(mol));
						}
					if (isomerList.size() > i)
						ffmol = isomerList.get(i);
					else
						mol = null;
					}
				catch (Exception e) {
					e.printStackTrace();
					}
				break;
				}

			if (mol == null)
				break;

			MinimizationResult result = new MinimizationResult();
			minimize(mol, ffmol, result);

			if (!stereoIsomerEnumerator.isCorrectStereoIsomer(mol, stereoIsomer))
				continue;

			// if we minimize, we check again, whether the minimized conformer is a very similar sibling in the list
			boolean isRedundantConformer = false;
			if (mMinimization != MINIMIZE_NONE) {
				if (rotatableBond == null)
					rotatableBond = TorsionDescriptor.getRotatableBonds(mol);
				if (torsionDescriptorList == null)
					torsionDescriptorList = new ArrayList<TorsionDescriptor>();

				TorsionDescriptor ntd = new TorsionDescriptor(mol, rotatableBond);
				for (TorsionDescriptor td:torsionDescriptorList) {
					if (td.equals(ntd)) {
						isRedundantConformer = true;
						break;
						}
					}
				if (!isRedundantConformer)
					torsionDescriptorList.add(ntd);
				}

			if (!isRedundantConformer) {
				String id = (mIdentifierColumn == -1) ? Integer.toString(row+1) : getTableModel().getTotalValueAt(row, mIdentifierColumn);

				centerConformer(mol);

				int realStereoIsomer = stereoIsomer * (createEnantiomers ? 2 : 1);

				if (mFileType == FileHelper.cFileTypeDataWarrior) {
					Canonizer canonizer = new Canonizer(mol);
					buildDataWarriorRecord(builder, canonizer.getIDCode(), canonizer.getEncodedCoordinates(),
							id, protonationState, realStereoIsomer, result);
					}
				else {
					buildSDFRecord(builder, mol, id, protonationState, realStereoIsomer, result);
					}

				if (createEnantiomers) {
					for (int atom=0; atom<mol.getAllAtoms(); atom++)
						mol.setAtomZ(atom, -mol.getAtomZ(atom));

					if (mFileType == FileHelper.cFileTypeDataWarrior) {
						Canonizer canonizer = new Canonizer(mol);
						buildDataWarriorRecord(builder, canonizer.getIDCode(), canonizer.getEncodedCoordinates(),
								id, protonationState, realStereoIsomer+1, result);
						}
					else {
						buildSDFRecord(builder, mol, id, protonationState, realStereoIsomer+1, result);
						}
					}
				}
			}
		}

	/**
	 * Minimizes the molecule with the method defined in mMinimization.
	 * The starting conformer is taken from ffmol, if it is not null.
	 * When this method finishes, then the minimized atom coodinates are in mol,
	 * even if mMinimization == MINIMIZE_NONE.
	 * @param mol receives minimized coodinates; taken as start conformer if ffmol == null
	 * @param ffmol if not null this is taken as start conformer
	 * @param result receives energy and possibly error message
	 */
	private void minimize(StereoMolecule mol, FFMolecule ffmol, MinimizationResult result) {
		if (ffmol != null && mMinimization != MINIMIZE_IDORSIA_FORCEFIELD)
			copyFFMolCoordsToMol(mol, ffmol);

		try {
			if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
				String tableSet = mMinimization == MINIMIZE_MMFF94sPlus ? mmff.ForceField.MMFF94SPLUS : mmff.ForceField.MMFF94S;
				int[] fragmentNo = new int[mol.getAllAtoms()];
				int fragmentCount = mol.getFragmentNumbers(fragmentNo, false, true);
				if (fragmentCount == 1) {
					mmff.ForceField ff = new mmff.ForceField(mol, tableSet, mMMFFOptions);
					int error = ff.minimise(10000, 0.0001, 1.0e-6);
					if (error != 0)
						throw new Exception("MMFF94 error code "+error);
					result.energy = (float)ff.getTotalEnergy();
					}
				else {
					int maxAtoms = 0;
	
					StereoMolecule[] fragment = mol.getFragments(fragmentNo, fragmentCount);
					for (StereoMolecule f:fragment) {
						if (f.getAllAtoms() > 2) {
							mmff.ForceField ff = new mmff.ForceField(f, tableSet, mMMFFOptions);
							int error = ff.minimise(10000, 0.0001, 1.0e-6);
							if (error != 0)
								throw new Exception("MMFF94 error code "+error);
	
							if (maxAtoms < f.getAllAtoms()) {	// we take the energy value from the largest fragment
								maxAtoms = f.getAllAtoms();
								result.energy = (float)ff.getTotalEnergy();
								}
							}
						}
					int[] atom = new int[fragmentCount];
					for (int i=0; i<fragmentNo.length; i++) {
						int f = fragmentNo[i];
						mol.setAtomX(i, fragment[f].getAtomX(atom[f]));
						mol.setAtomY(i, fragment[f].getAtomY(atom[f]));
						mol.setAtomZ(i, fragment[f].getAtomZ(atom[f]));
						atom[f]++;
						}
					}
				}
			else if (mMinimization == MINIMIZE_IDORSIA_FORCEFIELD) {
				if (ffmol == null)
					ffmol = new FFMolecule(mol);	 

				ForceField f = new ForceField(ffmol);
				new OptimizerLBFGS().optimize(new EvaluableConformation(f));	//optimize torsions -> 6+nRot degrees of freedom, no change of angles and bond distances
				result.energy = (float)new OptimizerLBFGS().optimize(new EvaluableForceField(f));	//optimize cartesians -> 3n degrees of freedem

				// EvaluableForcefield -> optimize everything in a cartesian referential
				// EvaluableConformation -> optimize the torsions in the torsion referential
				// EvaluableDockFlex -> optimize the torsions + translation/rotation in the torsion referential
				// EvaluableDockRigid -> optimize the translation/rotation in the cartesian referential

				// AlgoLBFGS -> faster algo
				// AlgoConjugateGradient -> very slow, not used anymore
				// AlgoMD -> test of molecular dynamic, not a optimization
				}
			}
		catch (Exception e) {
			result.energy = Double.NaN;
			result.errorMessage = e.toString();

			if (mMinimizationErrors == 0)
				e.printStackTrace();
			mMinimizationErrors++;
			}

		if (ffmol != null && mMinimization == MINIMIZE_IDORSIA_FORCEFIELD)
			copyFFMolCoordsToMol(mol, ffmol);
		}

	private void copyFFMolCoordsToMol(StereoMolecule mol, FFMolecule ffmol) {
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, ffmol.getAtomX(atom));
			mol.setAtomY(atom, ffmol.getAtomY(atom));
			mol.setAtomZ(atom, ffmol.getAtomZ(atom));
			}
		}

	private boolean hasMultipleStereoIsomers(StereoMolecule mol) {
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.getAtomParity(atom) == Molecule.cAtomParityUnknown
			 || (mol.isAtomStereoCenter(atom) && mol.getAtomESRType(atom) != Molecule.cESRTypeAbs))
				return true;
		for (int bond=0; bond<mol.getBonds(); bond++)
			if (mol.getBondParity(bond) == Molecule.cBondParityUnknown)
				return true;

		return false;
		}

	private void centerConformer(StereoMolecule mol) {
		double x = 0;
		double y = 0;
		double z = 0;
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			x += mol.getAtomX(atom);
			y += mol.getAtomY(atom);
			z += mol.getAtomZ(atom);
			}
		x /= mol.getAllAtoms();
		y /= mol.getAllAtoms();
		z /= mol.getAllAtoms();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, mol.getAtomX(atom) - x);
			mol.setAtomY(atom, mol.getAtomY(atom) - y);
			mol.setAtomZ(atom, mol.getAtomZ(atom) - z);
			}
		}

	private void writeDataWarriorHeader() throws IOException {
		mFileWriter.write("<datawarrior-fileinfo>\n");
		mFileWriter.write("<version=\"3.1\">\n");
		mFileWriter.write("</datawarrior-fileinfo>\n");
		mFileWriter.write("<column properties>\n");
		mFileWriter.write("<columnName=\"Structure\">\n");
		mFileWriter.write("<columnProperty=\"specialType\tidcode\">\n");
		mFileWriter.write("<columnName=\"idcoordinates3D\">\n");
		mFileWriter.write("<columnProperty=\"specialType\tidcoordinates3D\">\n");
		mFileWriter.write("<columnProperty=\"parent\tStructure\">\n");
		mFileWriter.write("</column properties>\n");
		mFileWriter.write("Structure\tidcoordinates3D\tID");
		if (!Float.isNaN(mPH1))
			mFileWriter.write("\tProtonation State");
		mFileWriter.write("\tStereo Isomer");
		if (mMinimization != MINIMIZE_NONE)
			mFileWriter.write("\tEnergy\tMinimization Error");
		mFileWriter.write("\n");
		}

	private void writeDataWarriorFooter() throws IOException {
		mFileWriter.write("<datawarrior properties>\n");
		mFileWriter.write("<columnWidth_Table_Energy=\"75\">\n");
		mFileWriter.write("<columnWidth_Table_ID=\"75\">\n");
		mFileWriter.write("<columnWidth_Table_Minimization Error=\"75\">\n");
		mFileWriter.write("<columnWidth_Table_Structure=\"132\">\n");
		mFileWriter.write("<detailView=\"height[Data]=0.22;height[Structure]=0.30;height[3D-Structure]=0.48\">\n");
		mFileWriter.write("<filter0=\"#double#\tEnergy\">\n");
		mFileWriter.write("<mainSplitting=\"0.72\">\n");
		mFileWriter.write("<mainView=\"Structures\">\n");
		mFileWriter.write("<mainViewCount=\"2\">\n");
		mFileWriter.write("<mainViewDockInfo0=\"root\">\n");
		mFileWriter.write("<mainViewDockInfo1=\"Table\tright\t0.50\">\n");
		mFileWriter.write("<mainViewName0=\"Table\">\n");
		mFileWriter.write("<mainViewName1=\"Structures\">\n");
		mFileWriter.write("<mainViewType0=\"tableView\">\n");
		mFileWriter.write("<mainViewType1=\"structureView\">\n");
		mFileWriter.write("<rightSplitting=\"0.16\">\n");
		mFileWriter.write("<rowHeight_Table=\"80\">\n");
		mFileWriter.write("<structureGridColumn_Structures=\"Structure\">\n");
		mFileWriter.write("<structureGridColumns_Structures=\"6\">\n");
		mFileWriter.write("</datawarrior properties>\n");
		}

	private void buildDataWarriorRecord(StringBuilder builder, String idcode, String coords, String id,
										int protonationState, int stereoIsomer, MinimizationResult result) throws IOException {
		builder.append(idcode+"\t"+coords+"\t"+id);
		if (!Float.isNaN(mPH1)) {
			builder.append('\t');
			builder.append(Integer.toString(protonationState + 1));
			}
		builder.append('\t');
		builder.append(Integer.toString(stereoIsomer+1));
		if (mMinimization != MINIMIZE_NONE) {
			builder.append('\t');
			if (!Double.isNaN(result.energy))
				builder.append(DoubleFormat.toString(result.energy));
			builder.append('\t');
			if (result.errorMessage != null)
				builder.append(result.errorMessage);
			}
		builder.append('\n');
		}

	private void buildSDFRecord(StringBuilder builder, StereoMolecule mol, String id,
								int protonationState, int stereoIsomer, MinimizationResult result) throws IOException {
		if (mFileType == FileHelper.cFileTypeSDV2)
			new MolfileCreator(mol, true, builder);
		else
			new MolfileV3Creator(mol, true, builder);

		builder.append(">  <ID>\n");
		builder.append(id);
		builder.append("\n\n");

		if (protonationState != -1) {
			builder.append(">  <Protonation State>\n");
			builder.append(protonationState+1);
			builder.append("\n\n");
			}

		if (stereoIsomer != -1) {
			builder.append(">  <Stereo Isomer>\n");
			builder.append(stereoIsomer+1);
			builder.append("\n\n");
			}

		if (mMinimization != MINIMIZE_NONE) {
			builder.append(">  <Energy>\n");
			builder.append(DoubleFormat.toString(result.energy));
			builder.append("\n\n");

			builder.append(">  <Error>\n");
			builder.append(result.errorMessage == null ? "" : result.errorMessage);
			builder.append("\n\n");
			}

		builder.append("$$$$\n");
		}

	private class MinimizationResult {
		double energy;
		String errorMessage;

		public MinimizationResult() {
			energy = Double.NaN;
			errorMessage = null;
			}
		}
	}
