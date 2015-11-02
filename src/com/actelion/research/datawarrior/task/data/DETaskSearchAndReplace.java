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

package com.actelion.research.datawarrior.task.data;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableModel;


public class DETaskSearchAndReplace extends ConfigurableTask implements ActionListener,Runnable {
	public static final long serialVersionUID = 0x20130131;

	public static final String TASK_NAME = "Search And Replace";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_WHAT = "what";
	private static final String PROPERTY_WITH = "with";
	private static final String PROPERTY_IS_STRUCTURE = "isStructure";
	private static final String PROPERTY_CASE_SENSITIVE = "caseSensitive";
	private static final String PROPERTY_MODE = "mode";
	
	private static final String OPTION_ANY_COLUMN = "<any column>";
	private static final String CODE_ANY_COLUMN = "<any>";

	private static final int cModeAllRows = 0;
	private static final int cModeSelectedOnly = 1;
	private static final int cModeVisibleOnly = 2;

	public static final String[] MODE_NAME = { "All rows", "Selected rows", "Visible rows" };
	public static final String[] MODE_CODE = { "all", "selected", "visible" };

	private static Properties sRecentConfiguration;

	private DEFrame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private JPanel				mDialogPanel;
	private JTextField			mTextFieldWhat,mTextFieldWith;
	private JEditableStructureView	mStructureFieldWhat,mStructureFieldWith;
	private JComboBox			mComboBoxColumn,mComboBoxMode;
	private JCheckBox			mCheckBoxIsStructureColumn,mCheckBoxCaseSensitive;
	private JLabel				mLabelUseRGroups;
	private boolean				mIsStructureMode;

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	public DETaskSearchAndReplace(DEFrame owner) {
		super(owner, true);
		mParentFrame = owner;
		mTableModel = mParentFrame.getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#SearchReplace";
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() != 0)
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (qualifiesAsColumn(column))
					return true;

		showErrorMessage("Search and replace requires a column with\nalphanumerical content or with chemical structures.");
		return false;
		}

	@Override
	public JPanel createDialogContent() {
		mComboBoxColumn = new JComboBox();
		mComboBoxColumn.addItem(OPTION_ANY_COLUMN);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsColumn(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(!isInteractive());
		mComboBoxColumn.addActionListener(this);

		mTextFieldWhat = new JTextField(16);
		mTextFieldWith = new JTextField(16);
		mCheckBoxCaseSensitive = new JCheckBox("Case sensitive");
		if (!isInteractive()) {
			mCheckBoxIsStructureColumn = new JCheckBox("Is chemical structure");
			mCheckBoxIsStructureColumn.addActionListener(this);
			}

		mDialogPanel = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		mDialogPanel.setLayout(new TableLayout(size));

		mDialogPanel.add(new JLabel("Column:", SwingConstants.RIGHT), "1,1");
		mDialogPanel.add(mComboBoxColumn, "3,1");
		if (mCheckBoxIsStructureColumn != null)
			mDialogPanel.add(mCheckBoxIsStructureColumn, "3,3");
		mDialogPanel.add(new JLabel("Search:", SwingConstants.RIGHT), "1,5");
		mDialogPanel.add(mTextFieldWhat, "3,5");
		mDialogPanel.add(new JLabel("Replace with:", SwingConstants.RIGHT), "1,7");
		mDialogPanel.add(mTextFieldWith, "3,7");
		mDialogPanel.add(mCheckBoxCaseSensitive, "3,9");

		mComboBoxMode = new JComboBox(MODE_NAME);
		mDialogPanel.add(new JLabel("Target:", SwingConstants.RIGHT), "1,11");
		mDialogPanel.add(mComboBoxMode, "3,11");

		mIsStructureMode = false;

		return mDialogPanel;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxIsStructureColumn) {
			updateInputFields(mCheckBoxIsStructureColumn.isSelected());
			return;
			}
		if (isInteractive() && e.getSource() == mComboBoxColumn) {
			int column = mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());
			boolean isStructure = (column == -1) ?
					false : CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column));
			updateInputFields(isStructure);
			}
		}

	private void updateInputFields(boolean isStructure) {
		if (mCheckBoxIsStructureColumn != null
		 && mCheckBoxIsStructureColumn.isSelected() != isStructure) {
			mCheckBoxIsStructureColumn.removeActionListener(this);
			mCheckBoxIsStructureColumn.setSelected(isStructure);
			mCheckBoxIsStructureColumn.addActionListener(this);
			}

		if (isStructure && !mIsStructureMode) {
			if (mStructureFieldWhat == null) {
				Dimension editorSize = new Dimension(mTextFieldWhat.getPreferredSize().width, 100);
				mStructureFieldWhat = new JEditableStructureView();
				mStructureFieldWhat.getMolecule().setFragment(true);
				mStructureFieldWhat.setPreferredSize(editorSize);
				mStructureFieldWhat.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
				mStructureFieldWith = new JEditableStructureView();
				mStructureFieldWith.setPreferredSize(editorSize);
				mStructureFieldWith.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
				mLabelUseRGroups = new JLabel("Use R-groups (R1, R2, ...) to define links.", JLabel.CENTER);
				}
			String what = mTextFieldWhat.getText();
			String with = mTextFieldWith.getText();
			mDialogPanel.remove(mTextFieldWhat);
			mDialogPanel.remove(mTextFieldWith);
			mDialogPanel.remove(mCheckBoxCaseSensitive);
			mDialogPanel.add(mStructureFieldWhat, "3,5");
			mDialogPanel.add(mStructureFieldWith, "3,7");
			mDialogPanel.add(mLabelUseRGroups, "1,9,3,9");
			mDialogPanel.validate();
			getDialog().pack();
			if (what.length() != 0) {
				try {
					new IDCodeParser().parse(mStructureFieldWhat.getMolecule(), what);
					mStructureFieldWhat.structureChanged();
					}
				catch (Exception e) {}
				}
			if (with.length() != 0) {
				try {
					new IDCodeParser().parse(mStructureFieldWith.getMolecule(), with);
					mStructureFieldWith.structureChanged();
					}
				catch (Exception e) {}
				}
			mIsStructureMode = true;
			mCheckBoxCaseSensitive.setEnabled(false);
			return;
			}

		if (!isStructure && mIsStructureMode) {
			StereoMolecule whatMol = mStructureFieldWhat.getMolecule();
			StereoMolecule withMol = mStructureFieldWith.getMolecule();
			String what = (whatMol.getAllAtoms() == 0) ? "" : new Canonizer(whatMol).getIDCode();
			String with = (withMol.getAllAtoms() == 0) ? "" : new Canonizer(withMol).getIDCode();
			mDialogPanel.remove(mStructureFieldWhat);
			mDialogPanel.remove(mStructureFieldWith);
			mDialogPanel.remove(mLabelUseRGroups);
			mDialogPanel.add(mTextFieldWhat, "3,5");
			mDialogPanel.add(mTextFieldWith, "3,7");
			mDialogPanel.add(mCheckBoxCaseSensitive, "3,9");
			mDialogPanel.validate();
			getDialog().pack();
			mTextFieldWhat.setText(what);
			mTextFieldWith.setText(with);
			mIsStructureMode = false;
			mCheckBoxCaseSensitive.setEnabled(true);
			return;
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String column = OPTION_ANY_COLUMN.equals(mComboBoxColumn.getSelectedItem()) ? CODE_ANY_COLUMN
					: mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem()));
		configuration.setProperty(PROPERTY_COLUMN, column);

		if (mIsStructureMode) {
			configuration.setProperty(PROPERTY_IS_STRUCTURE, "true");

			StereoMolecule whatMol = mStructureFieldWhat.getMolecule();
			StereoMolecule withMol = mStructureFieldWith.getMolecule();
			String what = (whatMol.getAllAtoms() == 0) ? "" : new Canonizer(whatMol).getIDCode();
			String with = (withMol.getAllAtoms() == 0) ? "" : new Canonizer(withMol).getIDCode();
			if (what.length() != 0)
				configuration.setProperty(PROPERTY_WHAT, what);
	
			configuration.setProperty(PROPERTY_WITH, with);
			}
		else {
			configuration.setProperty(PROPERTY_IS_STRUCTURE, "false");

			String what = mTextFieldWhat.getText();
			if (what.length() != 0)
				configuration.setProperty(PROPERTY_WHAT, what);
	
			configuration.setProperty(PROPERTY_WITH, mTextFieldWith.getText());
			}

		if (mCheckBoxCaseSensitive.isSelected())
			configuration.setProperty(PROPERTY_CASE_SENSITIVE, "true");

		if (mComboBoxMode.getSelectedIndex() != cModeAllRows)
			configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN);
		if (value == null || value.equals(CODE_ANY_COLUMN)) {
			mComboBoxColumn.setSelectedItem(OPTION_ANY_COLUMN);
			}
		else {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsColumn(column))
				mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			}

		if ("true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE))) {
			updateInputFields(true);
			String what = configuration.getProperty(PROPERTY_WHAT, "");
			String with = configuration.getProperty(PROPERTY_WITH, "");
			if (what.length() == 0) {
				mStructureFieldWhat.getMolecule().deleteMolecule();
				}
			else {
				try {
					new IDCodeParser().parse(mStructureFieldWhat.getMolecule(), what);
					mStructureFieldWhat.structureChanged();
					}
				catch (Exception e) {}
				}
			if (with.length() == 0) {
				mStructureFieldWith.getMolecule().deleteMolecule();
				}
			else {
				try {
					new IDCodeParser().parse(mStructureFieldWith.getMolecule(), with);
					mStructureFieldWith.structureChanged();
					}
				catch (Exception e) {}
				}
			}
		else {
			updateInputFields(false);
			mTextFieldWhat.setText(configuration.getProperty(PROPERTY_WHAT, ""));
			mTextFieldWith.setText(configuration.getProperty(PROPERTY_WITH, ""));
			}

		value = configuration.getProperty(PROPERTY_CASE_SENSITIVE);
		mCheckBoxCaseSensitive.setSelected("true".equals(value));

		mComboBoxMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0));
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		boolean isStructureMode = "true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE));
		String what = configuration.getProperty(PROPERTY_WHAT, "");
		if (what.length() == 0) {
			showErrorMessage(isStructureMode ? "No structure defined." : "No search string defined.");
			return false;
			}
		if (what.contains("\t")) {
			showErrorMessage("TAB is not allowed in replace string.");
			return false;
			}
		if (isStructureMode) {
			String with = configuration.getProperty(PROPERTY_WITH, "");
			if (with.length() == 0) {
				showErrorMessage("No replacement structure defined.");
				return false;
				}
			boolean[] rGroupUsed = new boolean[16];
			int count = 0;
			try {
				StereoMolecule whatMol = new IDCodeParser().getCompactMolecule(what);
				whatMol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<whatMol.getAllAtoms(); atom++) {
					int atomicNo = whatMol.getAtomicNo(atom);
					if (atomicNo > 128 && atomicNo <= 144) {
						if (rGroupUsed[atomicNo - 129]) {
							showErrorMessage("Duplicate R-groups used in search structure.");
							return false;
							}
						if (whatMol.getConnAtoms(atom) != 1) {
							showErrorMessage("R-groups must have exactly one neighbor atom.");
							return false;
							}
						rGroupUsed[atomicNo - 129] = true;
						count++;
						}
					}
/*				if (count == 0) {
					showErrorMessage("No R-groups (R1,R2,...) defined in search structure.");
					return false;
					} we allow the replacement of separated fragments	*/
				}
			catch (Exception e) {
				showErrorMessage("Invalid idcode of search structure.");
				return false;
				}
			try {
				StereoMolecule withMol = new IDCodeParser().getCompactMolecule(with);
				withMol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<withMol.getAllAtoms(); atom++) {
					int atomicNo = withMol.getAtomicNo(atom);
					if (atomicNo > 128 && atomicNo <= 144) {
						if (!rGroupUsed[atomicNo - 129]) {
							showErrorMessage("R-groups in search and replacement structures must match.");
							return false;
							}
						if (withMol.getConnAtoms(atom) != 1) {
							showErrorMessage("R-groups must have exactly one neighbor atom.");
							return false;
							}
						rGroupUsed[atomicNo - 129] = false;
						count--;
						}
					}
				if (count != 0) {
					showErrorMessage("Different number of R-groups used in search and replacement structures.");
					return false;
					}
				}
			catch (Exception e) {
				showErrorMessage("Invalid idcode of search structure.");
				return false;
				}
			}

		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		if (columnName.equals(CODE_ANY_COLUMN) && isStructureMode) {
			showErrorMessage("Substructure replacement cannot be combined with '<any column>'.");
			return false;
			}

		if (isLive) {
			if (!columnName.equals(CODE_ANY_COLUMN)) {
				int column = mTableModel.findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				if (!qualifiesAsColumn(column)) {
					showErrorMessage("Column '"+columnName+"' is not alphanumerical.");
					return false;
					}
				if (CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) {
					if (!isStructureMode) {
						showErrorMessage("Text replacement cannot be done on '"+columnName+"', which is a structure column.");
						return false;
						}
					}
				else {
					if (isStructureMode) {
						showErrorMessage("Substructure replacement cannot be done on '"+columnName+"', which is a text column.");
						return false;
						}
					}
				}
			int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0);
			if (mode == cModeVisibleOnly && mTableModel.getRowCount() == 0) {
				showErrorMessage("There are no visible rows.");
				return false;
				}
			if (mode == cModeSelectedOnly
			 && mParentFrame.getMainFrame().getMainPane().getTable().getSelectionModel().isSelectionEmpty()) {
				showErrorMessage("There are no selected rows.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxColumn.getItemCount() != 0)
			mComboBoxColumn.setSelectedIndex(0);

		mTextFieldWhat.setText("");
		mTextFieldWith.setText("");
		mCheckBoxCaseSensitive.setSelected(false);
		mComboBoxMode.setSelectedIndex(cModeAllRows);
		}

	private boolean qualifiesAsColumn(int column) {
		return (CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))
			 || (mTableModel.getColumnSpecialType(column) == null
			  && (mTableModel.isColumnTypeString(column)
			   || mTableModel.isColumnTypeDouble(column)
			   || mTableModel.isColumnTypeCategory(column))
			  && !mTableModel.isColumnTypeRangeCategory(column)));
		}

	@Override
	public void runTask(Properties configuration) {
		String what = configuration.getProperty(PROPERTY_WHAT, "").replace("\\n", "\n");
		String with = configuration.getProperty(PROPERTY_WITH, "").replace("\\n", "\n");
		String value = configuration.getProperty(PROPERTY_COLUMN);
		int targetColumn = (value == null || value.equals(CODE_ANY_COLUMN)) ? -1 : mTableModel.findColumn(value);
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0);

		if ("true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE))) {
			replaceStructures(what, with, targetColumn, mode);
			}
		else {
			boolean isCaseSensitive = "true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE));
			replaceText(what, with, targetColumn, isCaseSensitive, mode);
			}
		}

	private void replaceStructures(String what, String with, int column, int mode) {
		StereoMolecule mol = null;
		StereoMolecule whatMol = null;
		StereoMolecule withMol = null;
		Link[] link = null;
		mol = new StereoMolecule();
		whatMol = new IDCodeParser().getCompactMolecule(what);
		withMol = new IDCodeParser().getCompactMolecule(with);
		whatMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		withMol.ensureHelperArrays(Molecule.cHelperParities);
		ArrayList<Link> linkList = new ArrayList<Link>();
		for (int atom1=0; atom1<whatMol.getAllAtoms(); atom1++) {
			int atomicNo = whatMol.getAtomicNo(atom1);
			if (atomicNo > 128 && atomicNo <= 144) {
				for (int atom2=0; atom2<withMol.getAllAtoms(); atom2++) {
					if (withMol.getAtomicNo(atom2) == atomicNo) {
						whatMol.setAtomQueryFeature(atom1, Molecule.cAtomQFAny, true);
						whatMol.setAtomMarker(atom1, true);
						int newStereoCenter = withMol.getConnAtom(atom2, 0);
						int newParity = 0;
						if (withMol.getAtomParity(newStereoCenter) == Molecule.cAtomParity1
						 || withMol.getAtomParity(newStereoCenter) == Molecule.cAtomParity2) {
							boolean inversion = false;
							for (int i=0; i<withMol.getConnAtoms(newStereoCenter); i++)
								if (atom2 > withMol.getConnAtom(newStereoCenter, i))
									inversion = !inversion;
							newParity = withMol.getAtomParity(newStereoCenter);
							if (inversion)
								newParity = (newParity == Molecule.cAtomParity1) ?
										Molecule.cAtomParity2 : Molecule.cAtomParity1;
							}
						else {
							newStereoCenter = -1;
							}
						
						linkList.add(new Link(atom1, whatMol.getConnAtom(atom1, 0), whatMol.getConnBondOrder(atom1, 0),
											  atom2, newStereoCenter, newParity,
											  newStereoCenter == -1 ? false : withMol.isAtomParityPseudo(newStereoCenter)));
						break;
						}
					}
				}
			else {
				whatMol.setAtomQueryFeature(atom1, Molecule.cAtomQFNoMoreNeighbours, true);
				}
			}
		if (linkList.size() != 0) {
			withMol.ensureHelperArrays(Molecule.cHelperBitNeighbours);
			for (int i=0; i<linkList.size(); i++) {
				Link l = linkList.get(i);
				int bond = withMol.getConnBond(l.replaceAtom, 0);
				withMol.markAtomForDeletion(l.replaceAtom);
				withMol.markBondForDeletion(bond);
				l.setNewBondOrder(withMol.getConnBondOrder(l.replaceAtom, 0));
				l.replaceAtom = withMol.getConnAtom(l.replaceAtom, 0);
				}
			int[] atomMap = withMol.deleteMarkedAtomsAndBonds();
			for (Link l:linkList) {
				l.replaceAtom = atomMap[l.replaceAtom];
				if (l.newStereoCenter != -1)
					l.newStereoCenter = atomMap[l.newStereoCenter];
				}
			link = linkList.toArray(new Link[0]);
			}

		int maxProgress = mTableModel.getTotalRowCount() / 64;
		startProgress("Replacing sub-structures...", 0, maxProgress);

		int replacements = 0;
		int fragFpColumn = -1;
		SSSearcher searcher = null;
		SSSearcherWithIndex searcherWithIndex = null;
		fragFpColumn = mTableModel.getChildColumn(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		if (fragFpColumn == -1) {
			searcher = new SSSearcher();
			searcher.setFragment(whatMol);
			}
		else {
			searcherWithIndex = new SSSearcherWithIndex();
			searcherWithIndex.setFragment(whatMol, searcherWithIndex.createIndex(whatMol));
			}

		boolean found = false;
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if ((row & 63) == 63)
				updateProgress(-1);

			if (mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), column,
					CompoundTableModel.ATOM_COLOR_MODE_NONE, mol) == null)
				continue;

			mol.ensureHelperArrays(Molecule.cHelperParities);
			ArrayList<int[]> matchList = null;
			if (fragFpColumn == -1) {
				searcher.setMolecule(mol);
				if (searcher.findFragmentInMolecule() != 0)
					matchList = searcher.getMatchList();
				}
			else {
				searcherWithIndex.setMolecule(mol, (int[])mTableModel.getTotalRecord(row).getData(fragFpColumn));
				if (searcherWithIndex.findFragmentInMolecule() != 0)
					matchList = searcherWithIndex.getMatchList();
				}
			if (matchList != null) {
				// since there is not cMatchModeSeparated yet, we have to simulate
				boolean[] atomUsed = new boolean[mol.getAllAtoms()];
				for (int[] match:matchList) {
					boolean usedAtomFound = false;
					for (int atom:match) {
						if (atomUsed[atom]) {
							usedAtomFound = true;
							break;
							}
						}
					if (!usedAtomFound) {
						for (int atom=0; atom<match.length; atom++) {
							if (!whatMol.isMarkedAtom(atom)) {
								atomUsed[match[atom]] = true;
								mol.markAtomForDeletion(match[atom]);	// don't delete link atoms
								}
							}
						for (int bond=0; bond<mol.getAllBonds(); bond++)
							if (mol.isAtomMarkedForDeletion(mol.getBondAtom(0, bond))
							 || mol.isAtomMarkedForDeletion(mol.getBondAtom(1, bond)))
								mol.markBondForDeletion(bond);
						int[] fromWithAtomToMolAtom = mol.addMolecule(withMol);
						if (link != null) {
							for (Link l:link) {
								mol.addBond(fromWithAtomToMolAtom[l.replaceAtom], match[l.searchAtom], l.newBondOrder);

								if (mol.getAtomParity(match[l.searchAtom]) != Molecule.cAtomParityNone) {
									// if we introduce a double bond then reset any parity on new neighbor atom
									int molChiralCenter = match[l.searchAtom];
									if (l.newBondOrder != 1 && l.bondOrderChanged) {
										mol.setAtomParity(molChiralCenter, Molecule.cAtomParityNone, false);
										}
									// repair parity 1 or 2 on mol side
									else if (mol.getAtomParity(molChiralCenter) == Molecule.cAtomParity1
										  || mol.getAtomParity(molChiralCenter) == Molecule.cAtomParity2) {
										int oldSubstituentIndex = match[l.searchAtomNeighbour];
										boolean inversion = false;
										// connAtoms should still be valid. 
										// The new substituent gets a higher atom index than any mol atoms.
										// We count the chiral center's neighbors with higher atom index than
										// the substituent. Every swap of two neighbors inverts the parity.
										for (int i=0; i<mol.getConnAtoms(molChiralCenter); i++)
											if (mol.getConnAtom(molChiralCenter, i) > oldSubstituentIndex)
												inversion = !inversion;
										if (inversion) {
											int parity = mol.getAtomParity(molChiralCenter) == Molecule.cAtomParity1 ?
													Molecule.cAtomParity2 : Molecule.cAtomParity1;
											mol.setAtomParity(molChiralCenter, parity, mol.isAtomParityPseudo(molChiralCenter));
											}
										}
									}

								if (l.newStereoCenter != -1) {
									mol.setAtomParity(fromWithAtomToMolAtom[l.newStereoCenter], l.newParity, l.newParityIsPseudo);
									}
								}
							}
						replacements++;
						found = true;
						}
					}
				mol.deleteMarkedAtomsAndBonds();
				mol.setParitiesValid(0);
				new CoordinateInventor().invent(mol);
				mol.setStereoBondsFromParity();
				Canonizer canonizer = new Canonizer(mol);
				mTableModel.setTotalValueAt(canonizer.getIDCode(), row, column);
				mTableModel.removeChildDescriptorsAndCoordinates(row, column);
				int coords2DColumn = mTableModel.getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
				if (coords2DColumn != -1)
					mTableModel.setTotalValueAt(canonizer.getEncodedCoordinates(), row, coords2DColumn);
				}
			}

		if (found) {
			mTableModel.finalizeChangeChemistryColumn(column, 0, mTableModel.getTotalRowCount(), false);
			}

		if (isInteractive())
			showInteractiveTaskMessage("The substructure was replaced "+replacements+" times.",
					JOptionPane.INFORMATION_MESSAGE);
		}

	private void replaceText(String what, String with, int targetColumn, boolean isCaseSensitive, int mode) {
		if (!isCaseSensitive)
			what = what.toLowerCase();

		int maxProgress = (targetColumn != -1) ? mTableModel.getTotalRowCount() / 64 : mTableModel.getColumnCount();
		startProgress("Replacing '"+what+"'", 0, maxProgress);

		int replacements = 0;
		int columns = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (targetColumn == -1 && column != 0)
				updateProgress(-1);

			if (column == targetColumn || (targetColumn == -1 && mTableModel.getColumnSpecialType(column) == null)) {

				boolean found = false;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					if (targetColumn != -1 && (row & 63) == 63)
						updateProgress(-1);

					CompoundRecord record = mTableModel.getTotalRecord(row);
					if ((mode == cModeSelectedOnly && !mTableModel.isVisibleAndSelected(record))
					 || (mode == cModeVisibleOnly && !mTableModel.isVisible(record)))
						continue;

					String value = mTableModel.getTotalValueAt(row, column);
					if (isCaseSensitive) {
						if (value.contains(what)) {
							mTableModel.setTotalValueAt(value.replace(what, with), row, column);
							replacements++;
							found = true;
							}
						}
					else {
						if (value.toLowerCase().contains(what)) {
							StringBuilder newValue = new StringBuilder();
							String lowerValue = value.toLowerCase();
							int oldValueIndex = 0;
							int index = lowerValue.indexOf(what);
							while (index != -1) {
								if (oldValueIndex < index)
									newValue.append(value.substring(oldValueIndex, index));
		
								newValue.append(with);
								oldValueIndex = index + what.length();
		
								index = lowerValue.indexOf(what, oldValueIndex);
								}
		
							if (oldValueIndex < value.length())
								newValue.append(value.substring(oldValueIndex));
		
							mTableModel.setTotalValueAt(newValue.toString(), row, column);
							replacements++;
							found = true;
							}
						}
					}

				if (found) {
					mTableModel.finalizeChangeAlphaNumericalColumn(column, 0, mTableModel.getTotalRowCount());
					columns++;
					}
				}
			}

		if (isInteractive())
			showInteractiveTaskMessage("'"+what+"' was replaced "+replacements+" times in "+columns+" columns.",
					JOptionPane.INFORMATION_MESSAGE);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	private class Link {
		int searchAtom,searchAtomNeighbour,replaceAtom,oldBondOrder,newBondOrder,newStereoCenter,newParity;
		boolean bondOrderChanged,newParityIsPseudo;

		public Link(int searchAtom, int searchAtomNeighbour, int oldBondOrder,
				int replaceAtom, int newStereoCenter, int newParity, boolean newParityIsPseudo) {
			this.searchAtom = searchAtom;
			this.searchAtomNeighbour = searchAtomNeighbour;
			this.oldBondOrder = oldBondOrder;
			this.replaceAtom = replaceAtom;
			this.newStereoCenter = newStereoCenter;
			this.newParity = newParity;
			this.newParityIsPseudo = newParityIsPseudo;
			}

		public void setNewBondOrder(int bondOrder) {
			newBondOrder = bondOrder;
			bondOrderChanged = (oldBondOrder != newBondOrder);
			}
		}
	}