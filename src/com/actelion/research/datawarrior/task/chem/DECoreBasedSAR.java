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
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Properties;

public class DECoreBasedSAR extends DETaskAbstractAddChemProperty {
	public static final String TASK_NAME = "Core-Based SAR Analysis";

	private static final String PROPERTY_SCAFFOLD_LIST = "scaffoldList";
	private static final String PROPERTY_USE_EXISTING_COLUMNS = "useExistingColumns";
	private static final String PROPERTY_DISTINGUISH_STEREO_ISOMERS = "considerStereo";

	private static final String CORE_FRAGMENT_COLUMN_NAME = "Scaffold";
	private static final int cTableColumnNew = -2;

	private DefaultCompoundCollectionModel.Molecule mScaffoldModel;
	private JCheckBox			mCheckBoxDistinguishStereocenters,mCheckBoxUseExistingColumns;
	private String[]			mScaffold;
	private String[][]			mSubstituent;
	private int					mScaffoldColumn,mMultipleMatches,mNewColumnCount;
	private int[]				mSubstituentColumn;

    public DECoreBasedSAR(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
	    }

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#SARTables";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		double[][] size = { {480},
							{TableLayout.PREFERRED, 4, 96, 12, TableLayout.PREFERRED, 4, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Define scaffold structures:"), "0,0");

		mScaffoldModel = new DefaultCompoundCollectionModel.Molecule();
		CompoundCollectionPane<StereoMolecule> scaffoldPane = new CompoundCollectionPane<StereoMolecule>(mScaffoldModel, false);
		scaffoldPane.setCreateFragments(true);
		scaffoldPane.setEditable(true);
		scaffoldPane.setClipboardHandler(new ClipboardHandler());
		scaffoldPane.setShowValidationError(true);
		ep.add(scaffoldPane, "0,2");

        mCheckBoxDistinguishStereocenters = new JCheckBox("Distinguish stereoisomers", true);
		ep.add(mCheckBoxDistinguishStereocenters, "0,4");

		mCheckBoxUseExistingColumns = new JCheckBox("Use existing columns for scaffold and substituents", true);
		ep.add(mCheckBoxUseExistingColumns, "0,6");

		return ep;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		StringBuilder sb = new StringBuilder();
		for (int i=0; i<mScaffoldModel.getSize(); i++) {
			if (sb.length() != 0)
				sb.append('\t');
			sb.append(new Canonizer(mScaffoldModel.getMolecule(i)).getIDCode());
			}
		configuration.setProperty(PROPERTY_SCAFFOLD_LIST, sb.toString());

		configuration.setProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS, mCheckBoxDistinguishStereocenters.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_USE_EXISTING_COLUMNS, mCheckBoxUseExistingColumns.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		for (String idcode:configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "").split("\\t"))
			mScaffoldModel.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));

		mCheckBoxDistinguishStereocenters.setSelected("true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS)));
		mCheckBoxUseExistingColumns.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_EXISTING_COLUMNS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxUseExistingColumns.setSelected(true);
		}

	@Override
	protected int getNewColumnCount() {
		return mNewColumnCount;
		}

	@Override
	protected String getNewColumnName(int column) {
		if (column == 0 && mScaffoldColumn == cTableColumnNew)
			return CORE_FRAGMENT_COLUMN_NAME;
		int index = (mScaffoldColumn == cTableColumnNew) ? 1 : 0;
		for (int i=0; i<mSubstituentColumn.length; i++) {
			if (mSubstituentColumn[i] == cTableColumnNew) {
				if (index == column)
					return "R"+(i+1);

				index++;
				}
			}
		return null;	// should never reach this
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String scaffoldList = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffoldList.length() == 0) {
			showErrorMessage("No scaffolds defined.");
			return false;
			}
		for (String idcode:scaffoldList.split("\\t")) {
			try {
				new IDCodeParser(true).getCompactMolecule(idcode).validate();
				}
			catch (Exception e) {
				showErrorMessage("Some of the scaffold structures are not valid:\n"+e.toString());
				return false;
				}
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public boolean preprocessRows(Properties configuration) {
		boolean distinguishStereoCenters = "true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS));

		int rowCount = getTableModel().getTotalRowCount();
		mScaffold = new String[rowCount];
		mSubstituent = null;
		int notFoundCount = 0;

		String[] scaffoldIDCode = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "").split("\\t");
		for (String idcode:scaffoldIDCode) {
			StereoMolecule scaffoldMol = new IDCodeParser(true).getCompactMolecule(idcode);
			String[][] substituent = new String[scaffoldMol.getAtoms()][rowCount];

			if (processScaffold(scaffoldMol, distinguishStereoCenters, substituent)) {
				int substituentCount = 0;
				for (String[] s:substituent)
					if (s != null)
						substituentCount++;

				if (substituentCount != 0) {
					if (mSubstituent == null) {
						mSubstituent = new String[substituentCount][rowCount];
						}
					else if (mSubstituent.length < substituentCount) {
						String[][] oldSubstituent = mSubstituent;
						mSubstituent = new String[substituentCount][];
						for (int i=0; i<oldSubstituent.length; i++)
							mSubstituent[i] = oldSubstituent[i];
						for (int i=oldSubstituent.length; i<substituentCount; i++)
							mSubstituent[i] = new String[rowCount];
						}

					int substituentIndex = 0;
					for (String[] s:substituent) {
						if (s != null) {
							for (int i=0; i<s.length; i++)
								if (s[i] != null)
									mSubstituent[substituentIndex][i] = s[i];

							substituentIndex++;
							}
						}
					}
				}
			else {
				notFoundCount++;
				}
			}

		if (notFoundCount == scaffoldIDCode.length && isInteractive()) {
			final String message = "None of your scaffolds was found in in the '"
					+getTableModel().getColumnTitle(getStructureColumn())+"' column.";
			showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
			return false;
			}

		mScaffoldColumn = cTableColumnNew;
		mSubstituentColumn = new int[mSubstituent.length];
		for (int i=0; i<mSubstituent.length; i++)
			mSubstituentColumn[i] = cTableColumnNew;

		boolean useExistingColumns = "true".equals(configuration.getProperty(PROPERTY_USE_EXISTING_COLUMNS));
		if (useExistingColumns) {
			int column = getTableModel().findColumn(CORE_FRAGMENT_COLUMN_NAME);
			if (column != -1) {
	            String specialType = getTableModel().getColumnSpecialType(column);
	            if (specialType != null && specialType.equals(CompoundTableModel.cColumnTypeIDCode))
	            	mScaffoldColumn = column;
	            }
			for (int i=0; i<mSubstituent.length; i++) {
				String columnName = "R"+(i+1);
				column = getTableModel().findColumn(columnName);
				if (column != -1) {
		            String specialType = getTableModel().getColumnSpecialType(column);
		            if (specialType != null && specialType.equals(CompoundTableModel.cColumnTypeIDCode))
		            	mSubstituentColumn[i] = column;
		            }
				}
			}

		mNewColumnCount = (mScaffoldColumn == cTableColumnNew) ? 1 : 0;
		if (mSubstituentColumn != null)
			for (int i=0; i<mSubstituentColumn.length; i++)
				if (mSubstituentColumn[i] == cTableColumnNew)
					mNewColumnCount++;

		return true;
		}

	/**
	 * Processes entire table with one of the defined scaffolds:<br>
	 * For every row with no previously found scaffold it checks whether the row's structure contains scaffold as substructure.
	 * With all rows, where scaffold was found as substructure<br>
	 * - For every scaffold atom it is determined whether these bear changing substituents through matching rows.<br>
	 * - For every scaffold atom with changing substituents, the substituent of every row is created and put into substituent.<br> 
	 * - For every scaffold atom with no or a constant substituent, substituent is set to null and the constant substituent is attached to the scaffold structure.<br>
	 * - The decorated scaffold structure is written into mScaffold for these rows.<br>
	 * @param scaffoldMol
	 * @param distinguishStereoCenters
	 * @param substituent initialized as [scaffold atom count][row count]
	 * @return false if the scaffold could not be found in any row
	 */
	private boolean processScaffold(StereoMolecule scaffoldMol, boolean distinguishStereoCenters, String[][] substituent) {
		SSSearcherWithIndex searcher = new SSSearcherWithIndex();
		searcher.setFragment(scaffoldMol, null);

		scaffoldMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		String[] coreFragment = new String[getTableModel().getTotalRowCount()];

		mMultipleMatches = 0;

		startProgress("Analyzing substituents...", 0, getTableModel().getTotalRowCount());

        int coordinateColumn = getTableModel().getChildColumn(getStructureColumn(), CompoundTableModel.cColumnType2DCoordinates);
        int fingerprintColumn = getTableModel().getChildColumn(getStructureColumn(), DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		UniqueStringList coreIDCodeList = new UniqueStringList();
        ArrayList<StereoMolecule> coreFragmentList = new ArrayList<StereoMolecule>();
        ArrayList<int[]> coreParitiesList = new ArrayList<int[]>();
		StereoMolecule fragment = new StereoMolecule();

		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (threadMustDie())
				break;

			if (mScaffold[row] != null)
				continue;

			updateProgress(row+1);

			byte[] idcode = (byte[])getTableModel().getTotalRecord(row).getData(getStructureColumn());
			if (idcode != null) {
				searcher.setMolecule(idcode, (int[])getTableModel().getTotalRecord(row).getData(fingerprintColumn));
				int matchCount = searcher.findFragmentInMolecule(SSSearcher.cCountModeRigorous, SSSearcher.cDefaultMatchMode);
				if (matchCount > 0) {
					if (matchCount > 1)
						mMultipleMatches++;

					int[] matchAtom = (int[])searcher.getMatchList().get(0);

					byte[] coords = (byte[])getTableModel().getTotalRecord(row).getData(coordinateColumn);
					StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(idcode, coords);

						// store original fragment atom numbers incremented by 1 in atomMapNo
					for (int i=0; i<matchAtom.length; i++)
						if (matchAtom[i] != -1)
							mol.setAtomMapNo(matchAtom[i], i+1, false);

						// mark all atoms belonging to core fragment
					boolean[] isCoreAtom = new boolean[mol.getAllAtoms()];
					for (int i=0; i<matchAtom.length; i++)
						if (matchAtom[i] != -1)
							isCoreAtom[matchAtom[i]] = true;

					String stereoInfo = "";
					int[] coreAtomParity = null;
					if (distinguishStereoCenters) {
						mol.ensureHelperArrays(Molecule.cHelperNeighbours);
						boolean[] isExtendedCoreAtom = new boolean[mol.getAllAtoms()];	// core plus direct neighbours
						for (int i=0; i<matchAtom.length; i++) {
							int atom = matchAtom[i];
							if (atom != -1) {
								isExtendedCoreAtom[atom] = true;
								for (int j = 0; j < mol.getConnAtoms(atom); j++) {
									int connAtom = mol.getConnAtom(atom, j);
									if (!isCoreAtom[connAtom])
										isExtendedCoreAtom[connAtom] = true;
									}
								}
							}

						StereoMolecule extendedCore = new StereoMolecule();	// core plus direct neighbours
						mol.copyMoleculeByAtoms(extendedCore, isExtendedCoreAtom, true, null);

							// change atomicNo of non-core atoms to 'R1'
						for (int atom=0; atom<extendedCore.getAllAtoms(); atom++)
							if (extendedCore.getAtomMapNo(atom) == 0)
								extendedCore.setAtomicNo(atom, 142);	// 'R1'

						extendedCore.ensureHelperArrays(Molecule.cHelperParities);

						boolean stereoCenterFound = false;
						coreAtomParity = new int[matchAtom.length];
						byte[] parityByte = new byte[matchAtom.length];
						for (int atom=0; atom<extendedCore.getAllAtoms(); atom++) {
							int coreAtomNo = extendedCore.getAtomMapNo(atom) - 1;
							if (coreAtomNo != -1) {
								int atomParity = extendedCore.getAtomParity(atom);
								coreAtomParity[coreAtomNo] = atomParity;
								parityByte[coreAtomNo] = (byte)('0'+atomParity);
                                if (atomParity != Molecule.cAtomParityNone)
                                    stereoCenterFound = true;
								if (atomParity == Molecule.cAtomParity1
								 || atomParity == Molecule.cAtomParity2) {
                                    int esrType = extendedCore.getAtomESRType(atom);
                                    if (esrType != Molecule.cESRTypeAbs) {
                                        int esrEncoding = (extendedCore.getAtomESRGroup(atom) << 4)
                                                        + ((esrType == Molecule.cESRTypeAnd) ? 4 : 8);
                                        parityByte[coreAtomNo] += esrEncoding;
                                        coreAtomParity[coreAtomNo] += esrEncoding;
                                        }
                                    }
								}
							}
                        if (stereoCenterFound)
                            stereoInfo = new String(parityByte);
                        else
                            coreAtomParity = null;
						}

					StereoMolecule core = new StereoMolecule();
					mol.copyMoleculeByAtoms(core, isCoreAtom, true, null);
					core.setFragment(false);
					core.stripStereoInformation();
					coreFragment[row] = new Canonizer(core).getIDCode() + stereoInfo;

					if (coreIDCodeList.addString(coreFragment[row]) != -1) {	// new unique core fragment
						coreFragmentList.add(core);
						coreParitiesList.add(coreAtomParity);
						}

					for (int i=0; i<matchAtom.length; i++)
						if (matchAtom[i] != -1)
							mol.setAtomicNo(matchAtom[i], 0);

					int[] workAtom = new int[mol.getAllAtoms()];
					for (int i=0; i<matchAtom.length; i++) {
						if (matchAtom[i] != -1) {
							boolean[] isSubstituentAtom = new boolean[mol.getAllAtoms()];
							isSubstituentAtom[matchAtom[i]] = true;
							workAtom[0] = matchAtom[i];
							int current = 0;
							int highest = 0;
							while (current <= highest) {
								for (int j=0; j<mol.getConnAtoms(workAtom[current]); j++) {
									if (current == 0 || !isCoreAtom[workAtom[current]]) {
										int candidate = mol.getConnAtom(workAtom[current], j);
										if (!isSubstituentAtom[candidate]
										 && (current != 0 || !isCoreAtom[candidate])) {
											isSubstituentAtom[candidate] = true;
											workAtom[++highest] = candidate;
											}
										}
									}
								current++;
								}

							fragment.deleteMolecule();
							mol.copyMoleculeByAtoms(fragment, isSubstituentAtom, false, null);
							fragment.setFragment(false);

							if (!distinguishStereoCenters)
								fragment.stripStereoInformation();

								// if substituent is a ring forming bridge to the startatom
							for (int bond=fragment.getAllBonds()-1; bond>=0; bond--)
								if (fragment.getAtomicNo(fragment.getBondAtom(0, bond)) == 0
								 && fragment.getAtomicNo(fragment.getBondAtom(1, bond)) == 0)
									fragment.deleteBond(bond);

							substituent[i][row] = (highest == 0) ? null : new Canonizer(fragment).getIDCode();
							}
						}
					}
				}
			}

		if (coreIDCodeList.getSize() == 0)
			return false;

		if (threadMustDie())
			return true;

			// mark core atoms with varying substituents to require a new column
		boolean[] substituentVaries = new boolean[scaffoldMol.getAtoms()];
		String[] constantSubstituent = new String[scaffoldMol.getAtoms()];
		for (int atom=0; atom<scaffoldMol.getAtoms(); atom++) {
			constantSubstituent[atom] = substituent[atom][0];
			for (int row=1; row<getTableModel().getTotalRowCount(); row++) {
				if (substituent[atom][row] == null
				 && constantSubstituent[atom] == null)
					continue;
				if (substituent[atom][row] == null
				 || constantSubstituent[atom] == null
				 || !substituent[atom][row].equals(constantSubstituent[atom])) {
					substituentVaries[atom] = true;
					break;
					}
				}
			}

		// remove not varying substituent columns (expected outside of method)
		for (int atom=0; atom<scaffoldMol.getAtoms(); atom++)
			if (!substituentVaries[atom])
				substituent[atom] = null;

			// add R-groups to all atoms of core fragment with varying substituents
		String[] idcodeListWithRGroups = new String[coreIDCodeList.getSize()];
		for (int i=0; i<coreIDCodeList.getSize(); i++) {
		    StereoMolecule core = coreFragmentList.get(i);

				// recreate matchAtom array from stored mapping numbers
			int[] matchAtom = new int[scaffoldMol.getAtoms()];
			for (int atom=0; atom<core.getAtoms(); atom++)
				matchAtom[core.getAtomMapNo(atom) - 1] = atom;

			int substituentNo = 0;
			for (int atom=0; atom<scaffoldMol.getAtoms(); atom++) {
						//	if substituent varies => attach an R group
				if (substituentVaries[atom]) {
					int newAtom = core.addAtom((substituentNo < 3) ?
										142+substituentNo : 126+substituentNo);
					core.addBond(matchAtom[atom], newAtom, 1);
					substituentNo++;
					}
				else {	//	else => attach the non-varying substituent (if it is not null = 'unsubstituted')
					if (constantSubstituent[atom] != null) {
					    StereoMolecule theSubstituent = new IDCodeParser(true).getCompactMolecule(constantSubstituent[atom]);
						core.addSubstituent(theSubstituent, matchAtom[atom]);
						}
					}
				}

			int[] parityList = coreParitiesList.get(i);
			if (parityList != null) {
				for (int atom=0; atom<scaffoldMol.getAtoms(); atom++) {
					int parity = parityList[atom] & 3;
                    int esrType = (parityList[atom] & 0x0C);
                    int esrGroup = (parityList[atom] & 0xF0) >> 4;
                    core.setAtomParity(matchAtom[atom], parity, false);
                    if (esrType != 0) {
                        core.setAtomESR(matchAtom[atom], esrType == 4 ?
                                Molecule.cESRTypeAnd : Molecule.cESRTypeOr,
                                        esrGroup);
				        }
                    }
                }
			new CoordinateInventor().invent(core);	// creates stereo bonds from parities
            core.setStereoBondsFromParity();

			idcodeListWithRGroups[i] = new Canonizer(core).getIDCode();
			}
		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (coreFragment[row] != null) {
				int index = coreIDCodeList.getListIndex(coreFragment[row]);
				mScaffold[row] = idcodeListWithRGroups[index];
				}
			}

		return true;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		int lastNewColumn = firstNewColumn;
		if (mScaffoldColumn == cTableColumnNew)
			mScaffoldColumn = lastNewColumn++;
		if (mSubstituentColumn != null)
			for (int i=0; i<mSubstituentColumn.length; i++)
				if (mSubstituentColumn[i] == cTableColumnNew)
					mSubstituentColumn[i] = lastNewColumn++;

		for (int i=firstNewColumn; i<lastNewColumn; i++)
            getTableModel().setColumnProperty(i, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		if (mScaffold[row] != null) {
			getTableModel().setTotalValueAt(mScaffold[row], row, mScaffoldColumn);
			getTableModel().removeChildDescriptorsAndCoordinates(row, mScaffoldColumn);
			for (int i=0; i<mSubstituent.length; i++) {
				if (mSubstituent[i][row] != null) {
					getTableModel().setTotalValueAt(mSubstituent[i][row], row, mSubstituentColumn[i]);
					getTableModel().removeChildDescriptorsAndCoordinates(row, mSubstituentColumn[i]);
					}
				}
			}
		}

	@Override
	public void postprocess(int firstNewColumn) {
		if (mScaffoldColumn < firstNewColumn)
			getTableModel().finalizeChangeChemistryColumn(mScaffoldColumn, 0, getTableModel().getTotalRowCount(), false);
		for (int i=0; i<mSubstituent.length; i++)
			if (mSubstituentColumn[i] < firstNewColumn)
				getTableModel().finalizeChangeChemistryColumn(mSubstituentColumn[i], 0, getTableModel().getTotalRowCount(), false);

		if (isInteractive() && mMultipleMatches > 0) {
			final String message = "In "+mMultipleMatches+" cases a symmetric scaffold could be matched multiple times.\n"
								 + "In these cases R-groups could not be ssigned in a unique way.\n"
								 + "You may try avoiding this by specifying less symmetrical scaffold structures.";
			showInteractiveTaskMessage(message, JOptionPane.WARNING_MESSAGE);
			}
		}
	}
