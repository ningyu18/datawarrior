/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
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

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;
import org.openmolecules.chem.conf.so.SelfOrganizedConformer;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.MolfileV3Creator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.optimizer.AlgoLBFGS;
import com.actelion.research.forcefield.optimizer.EvaluableConformation;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;


public class DETaskAdd3DCoordinates extends DETaskAbstractAddChemProperty implements Runnable {
	public static final String TASK_NAME = "Generate Conformers";
	private static Properties sRecentConfiguration;

	private static final String PROPERTY_ALGORITHM = "algorithm";
	private static final String PROPERTY_MINIMIZE = "minimize";
	private static final String PROPERTY_FILE_NAME = "fileName";
	private static final String PROPERTY_FILE_TYPE = "fileType";
	private static final String PROPERTY_MAX_CONFORMERS = "maxConformers";
	private static final String PROPERTY_LARGEST_FRAGMENT = "largestFragment";

	private static final String[] MINIMIZE_TEXT = { "MMFF94 forcefield", "Actelion forcefield", "Don't minimize" };
	private static final String[] MINIMIZE_CODE = { "mmff94", "actelion", "none" };
	private static final String[] MINIMIZE_TITLE = { "mmff94", "Actelion-FF", "not minimized" };
	private static final int MINIMIZE_MMFF94 = 0;
	private static final int MINIMIZE_ACTELION_FORCEFIELD = 1;
	private static final int MINIMIZE_NONE = 2;
	private static final int DEFAULT_MINIMIZATION = MINIMIZE_MMFF94;
	private static final String DEFAULT_MAX_CONFORMERS = "16";
	private static final int MAX_CONFORMERS = 64;

//*	 reduced options because of unclear copyright situation with CCDC concerning torsion statistics
	private static final int ADAPTIVE_RANDOM = 0;
	private static final int SYSTEMATIC = 1;
	private static final int LOW_ENERGY_RANDOM = 2;
	private static final int PURE_RANDOM = 3;
	private static final int SELF_ORGANIZED = 4;
	private static final int ACTELION3D = 5;
	private static final int DEFAULT_ALGORITHM = ADAPTIVE_RANDOM;
	private static final int FILE_TYPE_NONE = -1;

	private static final String[] ALGORITHM_TEXT = { "Adaptive collision avoidance, low energy bias", "Systematic, low energy bias", "Random, low energy bias", "Pure random", "Self-organized" };
	private static final String[] ALGORITHM_CODE = { "adaptiveRandom", "systematic", "lowEnergyRandom", "pureRandom", "selfOrganized", "actelion3d" };
	private static final String ALGORITHM_TEXT_ACTELION3D = "Actelion3D";

	private static final String[] FILE_TYPE_TEXT = { "DataWarrior", "SD-File Version 2", "SD-File Version 3" };
	private static final String[] FILE_TYPE_CODE = { "dwar", "sdf2", "sdf3" };
	private static final int[] FILE_TYPE = { FileHelper.cFileTypeDataWarrior, FileHelper.cFileTypeSDV2, FileHelper.cFileTypeSDV3 };
//	*/

/*	private static final String[] ALGORITHM_TEXT = { "Actelion3D" };
	private static final String[] ALGORITHM_CODE = { "actelion3d" };
	private static final int ACTELION3D = 0;
	private static final int DEFAULT_ALGORITHM = ACTELION3D;
*/
	private JComboBox			mComboBoxAlgorithm,mComboBoxMinimize,mComboBoxFileType;
	private JCheckBox			mCheckBoxExportFile,mCheckBoxLargestFragment;
	private JFilePathLabel		mLabelFileName;
	private JTextField			mTextFieldMaxCount;
	private JButton				mButtonEdit;
	private boolean				mCheckOverwrite;
	private volatile boolean	mLargestFragmentOnly;
	private volatile int		mAlgorithm,mMinimization,mMinimizationErrors,mFileType,mMaxConformers,mIdentifierColumn;
	private volatile BufferedWriter mFileWriter;
	private volatile Map<String,Object> mMMFFOptions;

	public DETaskAdd3DCoordinates(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);

		mCheckOverwrite = true;
		}

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
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
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 24, TableLayout.PREFERRED,
							8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Algorithm:"), "0,0");
		mComboBoxAlgorithm = new JComboBox(ALGORITHM_TEXT);
		if (System.getProperty("development") != null)
			mComboBoxAlgorithm.addItem(ALGORITHM_TEXT_ACTELION3D);
		ep.add(mComboBoxAlgorithm, "2,0,4,0");
		ep.add(new JLabel("Minimize energy:"), "0,2");
		mComboBoxMinimize = new JComboBox(MINIMIZE_TEXT);
		ep.add(mComboBoxMinimize, "2,2,4,2");

		mCheckBoxExportFile = new JCheckBox("Write into file:");
		ep.add(mCheckBoxExportFile, "0,4");
		mCheckBoxExportFile.addActionListener(this);

		mLabelFileName = new JFilePathLabel(!isInteractive());
		ep.add(mLabelFileName, "2,4,3,4");

		mButtonEdit = new JButton("Edit");
		mButtonEdit.addActionListener(this);
		ep.add(mButtonEdit, "4,4");

		ep.add(new JLabel("File type:"), "0,6");
		mComboBoxFileType = new JComboBox(FILE_TYPE_TEXT);
		mComboBoxFileType.addActionListener(this);
		ep.add(mComboBoxFileType, "2,6");

		ep.add(new JLabel("Max. conformer count:"), "0,8");
		mTextFieldMaxCount = new JTextField();
		ep.add(mTextFieldMaxCount, "2,8");

		mCheckBoxLargestFragment = new JCheckBox("Remove small fragments");
		ep.add(mCheckBoxLargestFragment, "2,10,4,10");

		return ep;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mButtonEdit) {
			String filename = new FileHelper(getParentFrame()).selectFileToSave(
					"Write Conformers To File", FILE_TYPE[mComboBoxFileType.getSelectedIndex()], "conformers");
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			}
		if (e.getSource() == mCheckBoxExportFile) {
			boolean isEnabled = false;
			if (mCheckBoxExportFile.isSelected()) {
				isEnabled = true;
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
						isEnabled = false;
						}
					}
				}

			enableItems(isEnabled);
			return;
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
		configuration.setProperty(PROPERTY_MINIMIZE, MINIMIZE_CODE[mComboBoxMinimize.getSelectedIndex()]);

		if (mCheckBoxExportFile.isSelected()) {
			configuration.setProperty(PROPERTY_FILE_NAME, mLabelFileName.getPath());
			configuration.setProperty(PROPERTY_FILE_TYPE, FILE_TYPE_CODE[mComboBoxFileType.getSelectedIndex()]);
			configuration.setProperty(PROPERTY_MAX_CONFORMERS, mTextFieldMaxCount.getText());
			configuration.setProperty(PROPERTY_LARGEST_FRAGMENT, mCheckBoxLargestFragment.isSelected()?"true":"false");
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxAlgorithm.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM));
		mComboBoxMinimize.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MINIMIZE), MINIMIZE_CODE, DEFAULT_MINIMIZATION));

		String value = configuration.getProperty(PROPERTY_FILE_NAME);
		mCheckBoxExportFile.setSelected(value != null);
		mLabelFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);

		mComboBoxFileType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0));
		mTextFieldMaxCount.setText(configuration.getProperty(PROPERTY_MAX_CONFORMERS, DEFAULT_MAX_CONFORMERS));
		mCheckBoxLargestFragment.setSelected(!"false".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT)));
		enableItems(mCheckBoxExportFile.isSelected());
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxAlgorithm.setSelectedIndex(DEFAULT_ALGORITHM);
		mComboBoxMinimize.setSelectedIndex(DEFAULT_MINIMIZATION);
		mCheckBoxExportFile.setSelected(false);
		mTextFieldMaxCount.setText(DEFAULT_MAX_CONFORMERS);
		mCheckBoxLargestFragment.setSelected(true);
		enableItems(false);
		}

	private void enableItems(boolean isEnabled) {
		mLabelFileName.setEnabled(isEnabled);
		mComboBoxFileType.setEnabled(isEnabled);
		mTextFieldMaxCount.setEnabled(isEnabled);
		mButtonEdit.setEnabled(isEnabled);
		mCheckBoxLargestFragment.setEnabled(isEnabled);
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
			}

		return true;
		}

	@Override
	protected int getNewColumnCount() {
		return (mFileType != FILE_TYPE_NONE) ? 0 : (mMinimization == MINIMIZE_MMFF94) ? 2 : 1;
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
		mMinimization = findListIndex(configuration.getProperty(PROPERTY_MINIMIZE), MINIMIZE_CODE, DEFAULT_MINIMIZATION);
		if (mMinimization == MINIMIZE_MMFF94) {
			mmff.ForceField.initialize(mmff.ForceField.MMFF94);
			mMMFFOptions = new HashMap<String, Object>();
			}
		mMinimizationErrors = 0;

		mFileType = FILE_TYPE_NONE;	// default
		String fileName = configuration.getProperty(PROPERTY_FILE_NAME);
		if (fileName != null) {
			mFileType = FILE_TYPE[findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0)];
			mMaxConformers = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_CONFORMERS));
			mLargestFragmentOnly = !"false".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT));

			String columnName = getTableModel().getColumnProperty(getStructureColumn(), CompoundTableConstants.cColumnPropertyIdentifierColumn);
			mIdentifierColumn = (columnName == null) ? -1 : getTableModel().findColumn(columnName);

			try {
				mFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolveVariables(fileName)), "UTF-8"));

				if (mFileType == FileHelper.cFileTypeDataWarrior)
					writeDataWarriorHeader();
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				return false;
				}
			}

		
		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule mol) throws Exception {
		if (mFileType != FILE_TYPE_NONE) {
			addConformersToFile(row, firstNewColumn, mol);
			}
		else {
			addConformerToTable(row, firstNewColumn, mol);
			}
		}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (mMinimizationErrors != 0 && isInteractive()) {
			showInteractiveTaskMessage("Forcefield minimization failed in "+mMinimizationErrors+" cases.", JOptionPane.INFORMATION_MESSAGE);
			}

		if (mFileType != FILE_TYPE_NONE) {
			try {
				if (mFileType == FileHelper.cFileTypeDataWarrior)
					writeDataWarriorFooter();

				mFileWriter.close();
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				}
			}
		}

	public void addConformerToTable(int row, int firstNewColumn, StereoMolecule mol) throws Exception {
		mol = getChemicalStructure(row, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

		boolean isOneStereoIsomer = !hasMultipleStereoIsomers(mol);
		FFMolecule ffmol = null;
		ConformerGenerator cg;

		switch (mAlgorithm) {
		case ADAPTIVE_RANDOM:
			mol = new ConformerGenerator().getOneConformer(mol);
			break;
		case SYSTEMATIC:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC);
			mol = cg.getNextConformer(mol);
			break;
		case LOW_ENERGY_RANDOM:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM);
			mol = cg.getNextConformer(mol);
			break;
		case PURE_RANDOM:
			cg = new ConformerGenerator();
			cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM);
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

//		String rawCoords = (mMinimization != MINIMIZE_MMFF94) ? null : new Canonizer(mol).getEncodedCoordinates(true);

		MinimizationResult result = new MinimizationResult();
		minimize(mol, ffmol, result);

		if (mol != null && mol.getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mol);
			if (isOneStereoIsomer	// a final conformer is one stereo isomer
			 && !canonizer.getIDCode().equals(getTableModel().getTotalValueAt(row, getStructureColumn()))) {
				System.out.println("WARNING: idcodes after 3D-coordinate generation differ!!!");
				System.out.println("old: "+getTableModel().getTotalValueAt(row, getStructureColumn()));
				System.out.println("new: "+canonizer.getIDCode());
				}
			getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(true), row, firstNewColumn);
			if (mMinimization == MINIMIZE_MMFF94) {
				String energyText = (result.errorMessage != null) ? result.errorMessage : DoubleFormat.toString(result.energy);
				getTableModel().setTotalValueAt(energyText, row, firstNewColumn+1);
				}
			}
		else {
			getTableModel().setTotalValueAt(null, row, firstNewColumn);
			if (mMinimization == MINIMIZE_MMFF94) {
				getTableModel().setTotalValueAt(null, row, firstNewColumn+1);
				}
			}
		}

	public void addConformersToFile(int row, int firstNewColumn, StereoMolecule mol) throws Exception {
		mol = getChemicalStructure(row, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

		ConformerGenerator cg = null;
		ConformationSelfOrganizer cs = null;
		List<FFMolecule> isomerList = null;
		int[] rotatableBond = null;
		ArrayList<TorsionDescriptor> torsionDescriptorList = null;

		StringBuilder builder = new StringBuilder();

		if (mLargestFragmentOnly)
			mol.stripSmallFragments();

		for (int i=0; i<mMaxConformers; i++) {
			FFMolecule ffmol = null;

			switch (mAlgorithm) {
			case ADAPTIVE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM);
					}
				mol = cg.getNextConformer(mol);
				break;
			case SYSTEMATIC:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC);
					}
				mol = cg.getNextConformer(mol);
				break;
			case LOW_ENERGY_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM);
					}
				mol = cg.getNextConformer(mol);
				break;
			case PURE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator();
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM);
					}
				mol = cg.getNextConformer(mol);
				break;
			case SELF_ORGANIZED:
				if (cs == null) {
					cs = new ConformationSelfOrganizer(mol, false);
					cs.initializeConformers(0, mMaxConformers);
					}
				SelfOrganizedConformer soc = cs.getNextConformer();
				if (soc == null)
					mol = null;
				else
					soc.copyTo(mol);
				break;
			case ACTELION3D:
				try {
					if (isomerList == null) {
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
	
				if (mFileType == FileHelper.cFileTypeDataWarrior) {
					Canonizer canonizer = new Canonizer(mol);
					buildDataWarriorRecord(builder, canonizer.getIDCode(), canonizer.getEncodedCoordinates(), id, result);
					}
				else {
					buildSDFRecord(builder, mol, id, result);
					}
				}
			}

		if (builder.length() != 0) {
			synchronized(mFileWriter) {
				mFileWriter.write(builder.toString());
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
		if (ffmol != null && mMinimization != MINIMIZE_ACTELION_FORCEFIELD)
			copyFFMolCoordsToMol(mol, ffmol);

		try {
			if (mMinimization == MINIMIZE_MMFF94) {
				int[] fragmentNo = new int[mol.getAllAtoms()];
				int fragmentCount = mol.getFragmentNumbers(fragmentNo, false);
				if (fragmentCount == 1) {
					mmff.ForceField ff = new mmff.ForceField(mol, mmff.ForceField.MMFF94, mMMFFOptions);
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
							mmff.ForceField ff = new mmff.ForceField(f, mmff.ForceField.MMFF94, mMMFFOptions);
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
			else if (mMinimization == MINIMIZE_ACTELION_FORCEFIELD) {
				if (ffmol == null)
					ffmol = new FFMolecule(mol);	 

				ForceField f = new ForceField(ffmol);
				new AlgoLBFGS().optimize(new EvaluableConformation(f));	//optimize torsions -> 6+nRot degrees of freedom, no change of angles and bond distances
				result.energy = (float)new AlgoLBFGS().optimize(new EvaluableForceField(f));	//optimize cartesians -> 3n degrees of freedem

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

		if (ffmol != null && mMinimization == MINIMIZE_ACTELION_FORCEFIELD)
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

	private void buildDataWarriorRecord(StringBuilder builder, String idcode, String coords, String id, MinimizationResult result) throws IOException {
		builder.append(idcode+"\t"+coords+"\t"+id);
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

	private void buildSDFRecord(StringBuilder builder, StereoMolecule mol, String id, MinimizationResult result) throws IOException {
		if (mFileType == FileHelper.cFileTypeSDV2)
			new MolfileCreator(mol, true, builder);
		else
			new MolfileV3Creator(mol, true, builder);

		builder.append(">  <ID>\n");
		builder.append(id);
		builder.append("\n\n");

		if (mMinimization != MINIMIZE_NONE) {
			builder.append(">  <Energy>\n");
			builder.append(DoubleFormat.toString(result.energy));
			builder.append("\n\n");

			builder.append(">  <Error>\n");
			builder.append(result.errorMessage);
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
