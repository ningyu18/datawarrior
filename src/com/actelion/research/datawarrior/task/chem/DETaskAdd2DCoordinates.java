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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.ScaffoldHelper;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.CompoundCollectionModel;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.CompoundTableModel;


public class DETaskAdd2DCoordinates extends DETaskAbstractAddChemProperty implements ActionListener,Runnable {
	public static final String TASK_NAME = "Generate 2D-Atom-Coordinates";
	private static Properties sRecentConfiguration;

	private static final String PROPERTY_SCAFFOLD_LIST = "scaffolds";
	private static final String PROPERTY_AUTOMATIC = "automatic";
	private static final String PROPERTY_SCAFFOLD_MODE = "scaffoldMode";

	private static final int SCAFFOLD_CENTRAL_RING = 0;
	private static final int SCAFFOLD_MURCKO = 1;

	private static final String[] SCAFFOLD_TEXT = { "Most central ring system", "Murcko scaffolds" };
	private static final String[] SCAFFOLD_CODE = { "centralRing", "murcko" };

	private CompoundCollectionPane<String>	mStructurePane;
	private JCheckBox						mCheckBoxAutomatic;
	private JComboBox						mComboBoxScaffoldMode;
	private ArrayList<Scaffold>				mScaffoldList;
	private int								mCoordinateColumn,mFFPColumn,mScaffoldMode,mIDCodeErrors;
	private SSSearcher						mSearcher;
	private SSSearcherWithIndex				mSearcherWithIndex;
	private StereoMolecule					mScaffoldContainer;

	public DETaskAdd2DCoordinates(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
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
		return "/html/help/chemistry.html#Add2DCoords";
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		double[][] size = { {480},
							{TableLayout.PREFERRED, 4, 96, 24, TableLayout.PREFERRED, 4, TableLayout.PREFERRED} };
		
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Enforce atom coordinates for these scaffolds:"), "0,0");

		mStructurePane = new CompoundCollectionPane<String>(new DefaultCompoundCollectionModel.IDCode(), false);
		mStructurePane.setEditable(true);
		mStructurePane.setClipboardHandler(new ClipboardHandler());
		mStructurePane.setShowValidationError(true);
		mStructurePane.setCreateFragments(true);
		ep.add(mStructurePane, "0,2");

		mCheckBoxAutomatic = new JCheckBox("Automatically detect scaffolds and unify their orientation");
		mCheckBoxAutomatic.addActionListener(this);
		ep.add(mCheckBoxAutomatic, "0,4");

		JPanel tp = new JPanel();
		tp.add(new JLabel("Scaffold detection method: "));
		mComboBoxScaffoldMode = new JComboBox(SCAFFOLD_TEXT);
		tp.add(mComboBoxScaffoldMode);
		ep.add(tp, "0,6");

		return ep;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxAutomatic) {
			mComboBoxScaffoldMode.setEnabled(mCheckBoxAutomatic.isSelected());
			return;
			}

		super.actionPerformed(e);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		CompoundCollectionModel<String> model = mStructurePane.getModel();
		if (model.getSize() != 0) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<model.getSize(); i++) {
				sb.append(model.getCompound(i));
				sb.append('\t');
				}
			configuration.put(PROPERTY_SCAFFOLD_LIST, sb.toString());
			}
		configuration.setProperty(PROPERTY_AUTOMATIC, mCheckBoxAutomatic.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_SCAFFOLD_MODE, SCAFFOLD_CODE[mComboBoxScaffoldMode.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mStructurePane.getModel().clear();
		String scaffolds = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffolds.length() != 0) {
			String[] idcodeList = scaffolds.split("\\t");
			for (String idcode:idcodeList)
				mStructurePane.getModel().addCompound(idcode);
			}

		mCheckBoxAutomatic.setSelected(!"false".equals(configuration.getProperty(PROPERTY_AUTOMATIC)));
		mComboBoxScaffoldMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CENTRAL_RING));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mStructurePane.getModel().clear();
		mCheckBoxAutomatic.setSelected(true);
		mComboBoxScaffoldMode.setSelectedIndex(SCAFFOLD_CENTRAL_RING);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		return true;
		}

	@Override
	protected int getNewColumnCount() {
		return (mCoordinateColumn == -1) ? 1 : 0;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnProperty(firstNewColumn,
				CompoundTableModel.cColumnPropertySpecialType,
				CompoundTableModel.cColumnType2DCoordinates);
		getTableModel().setColumnProperty(firstNewColumn,
				CompoundTableModel.cColumnPropertyParentColumn,
				getTableModel().getColumnTitleNoAlias(getStructureColumn()));
		}

	@Override
	protected String getNewColumnName(int column) {
		return "2D-"+getTableModel().getColumnTitle(getStructureColumn());
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mIDCodeErrors = 0;

		int idcodeColumn = getStructureColumn();
		mCoordinateColumn = getTableModel().getChildColumn(idcodeColumn, CompoundTableConstants.cColumnType2DCoordinates);

		mScaffoldList = new ArrayList<Scaffold>();
		String scaffolds = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffolds.length() != 0) {
			String[] idcodeList = scaffolds.split("\\t");
			for (int i=0; i<idcodeList.length; i++)
				mScaffoldList.add(new Scaffold(new IDCodeParser().getCompactMolecule(idcodeList[i])));
			}

		if ("true".equals(configuration.getProperty(PROPERTY_AUTOMATIC))) {
			mScaffoldContainer = new StereoMolecule();
			mScaffoldMode = findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CENTRAL_RING);
			}

		if (!mScaffoldList.isEmpty() || mScaffoldContainer != null) {
			mFFPColumn = getTableModel().getChildColumn(idcodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			if (mFFPColumn == -1) {
				mSearcher = new SSSearcher();
				}
			else {
				mSearcherWithIndex = new SSSearcherWithIndex();
				for (Scaffold s:mScaffoldList)
					s.calculateFFP();
				}
			}

		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		int coordinateColumn = (mCoordinateColumn != -1) ? mCoordinateColumn : firstNewColumn;
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null && mol.getAllAtoms() != 0) {
			boolean found = false;
			if (!mScaffoldList.isEmpty()) {
				if (mFFPColumn == -1) {
					mSearcher.setMolecule(mol);
					for (Scaffold s:mScaffoldList) {
						mSearcher.setFragment(s.mol);
						if (mSearcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) != 0) {
							found = true;
							updateCoords(mol, s.mol, mSearcher.getMatchList().get(0));
							break;
							}
						}
					}
				else {
					mSearcherWithIndex.setMolecule(mol, (int[])getTableModel().getTotalRecord(row).getData(mFFPColumn));
					for (Scaffold s:mScaffoldList) {
						mSearcherWithIndex.setFragment(s.mol, s.ffp);
						if (mSearcherWithIndex.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) != 0) {
							updateCoords(mol, s.mol, mSearcherWithIndex.getMatchList().get(0));
							found = true;
							break;
							}
						}
					}
				}

			if (mScaffoldContainer != null && !found) {
				StereoMolecule mol2 = mol.getCompactCopy();
				int[] atomMap = mol2.stripSmallFragments();
				boolean[] isCoreAtom = (mScaffoldMode == SCAFFOLD_MURCKO) ?
						ScaffoldHelper.findMurckoScaffold(mol2) : ScaffoldHelper.findMostCentralRingSystem(mol2);
				if (isCoreAtom != null) {
					int[] coreAtom = new int[mol2.getAllAtoms()];
					mol2.copyMoleculeByAtoms(mScaffoldContainer, isCoreAtom, true, coreAtom);
					new CoordinateInventor().invent(mScaffoldContainer);
					Scaffold scaffold = new Scaffold(mScaffoldContainer.getCompactCopy());
					scaffold.calculateFFP();
					mScaffoldList.add(scaffold);

					int[] matchMask = new int[mScaffoldContainer.getAtoms()];
					if (atomMap == null) {
						for (int atom=0; atom<mol.getAllAtoms(); atom++) {
							int scaffoldAtom = coreAtom[atom];
							if (scaffoldAtom != -1)
								matchMask[scaffoldAtom] = atom;
							}
						}
					else {
						for (int atom=0; atom<mol.getAllAtoms(); atom++) {
							if (atomMap[atom] != -1) {
								int scaffoldAtom = coreAtom[atomMap[atom]];
								if (scaffoldAtom != -1)
									matchMask[scaffoldAtom] = atom;
								}
							}
						}
					updateCoords(mol, mScaffoldContainer, matchMask);
					found = true;
					}
				}

			if (!found) {
				mol.ensureHelperArrays(Molecule.cHelperParities);
				new CoordinateInventor().invent(mol);
				mol.setStereoBondsFromParity();
				}

			Canonizer canonizer = new Canonizer(mol);
			if (!canonizer.getIDCode().equals(getTableModel().getTotalValueAt(row, getStructureColumn()))) {
				mIDCodeErrors++;
				System.out.println("WARNING: idcodes after 2D-coordinate generation differ!!!");
				System.out.println("old: "+getTableModel().getTotalValueAt(row, getStructureColumn()));
				System.out.println("new: "+canonizer.getIDCode());
				}
			else {	// don't change coordinates if idcode from new coordinates doesn't match old one
				getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(true), row, coordinateColumn);
				}
			}
		else {
			getTableModel().setTotalValueAt(null, row, coordinateColumn);
			}
		}

	private void updateCoords(StereoMolecule mol, StereoMolecule scaffold, int[] matchMask) {
		mol.ensureHelperArrays(Molecule.cHelperParities);
		for (int atom=0; atom<scaffold.getAllAtoms(); atom++) {
			mol.setAtomX(matchMask[atom], scaffold.getAtomX(atom));
			mol.setAtomY(matchMask[atom], scaffold.getAtomY(atom));
			mol.setAtomMarker(matchMask[atom], true);
			}
		new CoordinateInventor(CoordinateInventor.MODE_PREFER_MARKED_ATOM_COORDS).invent(mol);
		mol.setStereoBondsFromParity();
		}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (mCoordinateColumn != -1)	// we use an existing column
			getTableModel().finalizeChangeChemistryColumn(getTableModel().getParentColumn(mCoordinateColumn),
					0, getTableModel().getTotalRowCount(), false);

		if (isInteractive() && mIDCodeErrors != 0)
			showInteractiveTaskMessage("Coordinates were not changed for "+mIDCodeErrors
					+" structures, because original stereo configurations would have been changed.", JOptionPane.INFORMATION_MESSAGE);
		}

	private class Scaffold {
		StereoMolecule mol;
		int[] ffp;

		private Scaffold(StereoMolecule mol) {
			this.mol = mol;
			}

		private void calculateFFP() {
			ffp = mSearcherWithIndex.createIndex(mol);
			}
		}
	}
