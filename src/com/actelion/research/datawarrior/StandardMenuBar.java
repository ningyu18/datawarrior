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

package com.actelion.research.datawarrior;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.action.*;
import com.actelion.research.datawarrior.help.DEHelpFrame;
import com.actelion.research.datawarrior.task.*;
import com.actelion.research.datawarrior.task.chem.*;
import com.actelion.research.datawarrior.task.chem.clib.DETaskCreateCombinatorialLibrary;
import com.actelion.research.datawarrior.task.chem.elib.DETaskCreateEvolutionaryLibrary;
import com.actelion.research.datawarrior.task.data.*;
import com.actelion.research.datawarrior.task.data.fuzzy.DETaskCalculateFuzzyScore;
import com.actelion.research.datawarrior.task.db.*;
import com.actelion.research.datawarrior.task.file.*;
import com.actelion.research.datawarrior.task.filter.DETaskAddNewFilter;
import com.actelion.research.datawarrior.task.filter.DETaskDisableAllFilters;
import com.actelion.research.datawarrior.task.filter.DETaskEnableAllFilters;
import com.actelion.research.datawarrior.task.filter.DETaskResetAllFilters;
import com.actelion.research.datawarrior.task.list.*;
import com.actelion.research.datawarrior.task.macro.DETaskExitProgram;
import com.actelion.research.datawarrior.task.macro.DETaskRunMacro;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JScrollableMenu;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.JCompoundTableForm;
import com.actelion.research.util.Platform;
import org.openmolecules.datawarrior.plugin.IPluginTask;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.prefs.Preferences;

public class StandardMenuBar extends JMenuBar implements ActionListener,
		CompoundTableListener,CompoundTableListListener,ItemListener {
	static final long serialVersionUID = 0x20060728;

	public static final boolean SUPPRESS_CHEMISTRY = false;

	public static final String USER_MACRO_DIR = "$HOME/datawarrior/macro";

	public static final String PREFERENCES_KEY_RECENT_FILE = "recentFile";
	public static final int MAX_RECENT_FILE_COUNT = 16;

	private static final String OPEN_FILE = "open_";
	private static final String NEW_FROM_LIST = "newFromList_";
	private static final String SET_RANGE = "range_";
	private static final String LIST_ADD = "add_";
	private static final String LIST_REMOVE = "remove_";
	private static final String LIST_SELECT = "select_";
	private static final String LIST_DESELECT = "deselect_";
	private static final String LIST_DELETE = "delete_";
	private static final String LOOK_AND_FEEL = "laf_";
	private static final String EXPORT_MACRO = "export_";
	private static final String RUN_GLOBAL_MACRO = "runGlobal_";
	private static final String RUN_INTERNAL_MACRO = "runInternal_";

	final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private DataWarrior			mApplication;
	private DEFrame				mParentFrame;
	private DEParentPane		mParentPane;
	private DEMainPane			mMainPane;
	private CompoundTableModel mTableModel;
	private PageFormat mPageFormat;

	private JMenu jMenuFileOpenSpecial,jMenuFileOpenRecent,jMenuFileSaveSpecial,jMenuEditPasteSpecial,jMenuDataRemoveRows,
				  jMenuDataSelfOrganizingMap,jMenuDataSetRange,jMenuDataViewLogarithmic,jMenuChemAddDescriptor,
				  jMenuListCreate,jMenuMacroExport,jMenuMacroRun,jMenuHelpLaF;

	private JMenuItem jMenuFileNew,jMenuFileNewFromSelection,jMenuFileNewFromPivoting,jMenuFileNewFromReversePivoting,
					  jMenuFileOpen,jMenuFileOpenMacro,jMenuFileOpenTemplate,jMenuFileOpenMDLReactions,jMenuFileMerge,
					  jMenuFileAppend,jMenuFileClose,jMenuFileCloseAll,jMenuFileSave,jMenuFileSaveAs,jMenuFileSaveText,
					  jMenuFileSaveSDF,jMenuFileSaveTemplate,jMenuFileSaveVisibleAs,jMenuFilePageFormat,
					  jMenuFilePreview,jMenuFilePrint,jMenuFileExit,jMenuEditCut,jMenuEditCopy,jMenuEditPaste,
					  jMenuEditPasteWithHeader,jMenuEditPasteWithoutHeader,jMenuEditDelete,
					  jMenuEditSelectAll,jMenuEditInvertSelection,jMenuEditSearchAndReplace,jMenuEditDisableFilters,
					  jMenuEditEnableFilters,jMenuEditResetFilters,
					  jMenuEditNewFilter,jMenuDataRemoveColumns,jMenuDataRemoveSelected,jMenuDataRemoveInvisible,
			jMenuDataRemoveDuplicate,jMenuDataRemoveUnique,jMenuDataMergeColumns, jMenuDataMergeDuplicate,
					  jMenuDataAddEmptyColumns,jMenuDataAddEmptyRows,jMenuDataAddRowNumbers,jMenuDataAddCalculatedValues,
					  jMenuDataAddBinnedColumn,jMenuDataAddFuzzyScore,jMenuDataAddPrincipalComponents,jMenuDataSOMCreate,
					  jMenuDataSOMApply,jMenuDataSOMAnalyse, jMenuDataGiniScore,jMenuDataArrangeGraph,jMenuDataCorrelationMatrix,
					  jMenuChemCCLibrary,jMenuChemEALibrary,jMenuChemEnumerateMarkush,jMenuChemAddProperties,jMenuChemAddFormula,
					  jMenuChemAddSmiles,jMenuChemCreate2DCoords,jMenuChemCreate3DCoords,jMenuChemAddLargestFragment,
					  jMenuChemAddStructureFromName,jMenuChemAutomaticSARTable,jMenuChemCoreBasedSARTable,jMenuChemInteractiveSARTable,
					  jMenuChemAnalyzeScaffolds,jMenuChemAnalyzeCliffs,jMenuChemMatchFile,jMenuChemSelectDiverse,
					  jMenuChemCluster,jMenuChemExtractPKATree,jMenuChemUndocumented,jMenuChemPredictPKa,
					  jMenuChemCreateGenericTautomers,jMenuChemCompareDescriptorSimilarityDistribution,
					  jMenuChemExtractPairwiseCompoundSimilarities,jMenuChemExtractPairwiseStuff,jMenuChemCountAtomTypes,
					  jMenuChemRunSurfacePLS,jMenuChemClassifyReactions,jMenuDBWikipedia,jMenuDBReadChEMBL,jMenuDBFindChEMBLActives,
					  jMenuDBSearchCOD,jMenuDBRetrieveDataFromURL,
					  jMenuListCreateSelected,jMenuListCreateVisible,jMenuListCreateHidden,jMenuListCreateClipboard,
					  jMenuListCreateMerge,jMenuListDeleteAll,jMenuListNewColumn,jMenuListListsFromColumn,jMenuListImport,
					  jMenuListExport,jMenuMacroImport,jMenuMacroStartRecording,jMenuMacroContinueRecording,
					  jMenuMacroStopRecording,jMenuHelpHelp,jMenuHelpAbout,jMenuHelpCheckForUpdate;

	private DEScrollableMenu jMenuFileNewFromList,jMenuListAddSelectedTo,jMenuListRemoveSelectedFrom,jMenuListSelectFrom,jMenuListDeselectFrom,jMenuListDelete;
	private JCheckBoxMenuItem jMenuHelpAutomaticUpdateCheck;

	public StandardMenuBar(DEFrame parentFrame) {
		mApplication = parentFrame.getApplication();
		mParentFrame = parentFrame;
		mParentPane = parentFrame.getMainFrame();
		mMainPane = parentFrame.getMainFrame().getMainPane();
		mTableModel = (CompoundTableModel)mParentPane.getTableModel();
		mTableModel.addCompoundTableListener(this);
		mTableModel.getListHandler().addCompoundTableListListener(this);
		buildMenu();
		}

	public DEFrame getParentFrame() {
		return mParentFrame;
		}

	protected void buildMenu() {
		add(buildFileMenu());
		add(buildEditMenu());
		add(buildDataMenu());
		if (!SUPPRESS_CHEMISTRY)
			add(buildChemistryMenu());
		add(buildDatabaseMenu());
		add(buildListMenu());
		add(buildMacroMenu());
		add(buildHelpMenu());
		}

	protected JMenu buildFileMenu() {
		jMenuFileNew = new JMenuItem();
		jMenuFileNewFromSelection = new JMenuItem();
		jMenuFileNewFromList = new DEScrollableMenu();
		jMenuFileNewFromPivoting = new JMenuItem();
		jMenuFileNewFromReversePivoting = new JMenuItem();
		jMenuFileOpen = new JMenuItem();
		jMenuFileOpenRecent = new JMenu();
		jMenuFileOpenSpecial = new JMenu();
		jMenuFileOpenMacro = new JMenuItem();
		jMenuFileOpenTemplate = new JMenuItem();
		jMenuFileOpenMDLReactions = new JMenuItem();
		jMenuFileMerge = new JMenuItem();
		jMenuFileAppend = new JMenuItem();
		jMenuFileClose = new JMenuItem();
		jMenuFileCloseAll = new JMenuItem();
		jMenuFileSave = new JMenuItem();
		jMenuFileSaveAs = new JMenuItem();
		jMenuFileSaveSpecial = new JMenu();
		jMenuFileSaveText = new JMenuItem();
		jMenuFileSaveSDF = new JMenuItem();
		jMenuFileSaveTemplate = new JMenuItem();
		jMenuFileSaveVisibleAs = new JMenuItem();
		jMenuFilePageFormat = new JMenuItem();
		jMenuFilePreview = new JMenuItem();
		jMenuFilePrint = new JMenuItem();
		jMenuFileExit = new JMenuItem();

		jMenuFileNew.setText("New...");
		jMenuFileNew.setAccelerator(KeyStroke.getKeyStroke('N', MENU_MASK));
		jMenuFileNew.addActionListener(this);
		jMenuFileNewFromSelection.setText("New From Selection");
		jMenuFileNewFromSelection.setAccelerator(KeyStroke.getKeyStroke('N', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileNewFromSelection.addActionListener(this);
		jMenuFileNewFromList.setText("New From List");
		jMenuFileNewFromPivoting.setText("New From Pivoting...");
		jMenuFileNewFromPivoting.addActionListener(this);
		jMenuFileNewFromReversePivoting.setText("New From Reverse Pivoting...");
		jMenuFileNewFromReversePivoting.addActionListener(this);
		jMenuFileOpen.setText("Open...");
		jMenuFileOpen.setAccelerator(KeyStroke.getKeyStroke('O', MENU_MASK));
		jMenuFileOpen.addActionListener(this);
		jMenuFileOpenRecent.setText("Open Recent");
		jMenuFileOpenSpecial.setText("Open Special");
		jMenuFileOpenMacro.setText("Run Macro...");
		jMenuFileOpenMacro.addActionListener(this);
		jMenuFileOpenTemplate.setText("Apply Template...");
		jMenuFileOpenTemplate.addActionListener(this);
		jMenuFileOpenMDLReactions.setText("IsisBase Reactions...");
		jMenuFileOpenMDLReactions.addActionListener(this);
		jMenuFileMerge.setText("Merge File...");
		jMenuFileMerge.addActionListener(this);
		jMenuFileAppend.setText("Append File...");
		jMenuFileAppend.addActionListener(this);
		jMenuFileClose.setText("Close");
		jMenuFileClose.setAccelerator(KeyStroke.getKeyStroke('W', MENU_MASK));
		jMenuFileClose.addActionListener(this);
		jMenuFileCloseAll.setText("Close All");
		jMenuFileCloseAll.setAccelerator(KeyStroke.getKeyStroke('W', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileCloseAll.addActionListener(this);
		jMenuFileSave.setText("Save");
		jMenuFileSave.setAccelerator(KeyStroke.getKeyStroke('S', MENU_MASK));
		jMenuFileSave.addActionListener(this);
		jMenuFileSaveAs.setText("Save As...");
		jMenuFileSaveAs.setAccelerator(KeyStroke.getKeyStroke('S', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileSaveAs.addActionListener(this);
		jMenuFileSaveSpecial.setText("Save Special");
		jMenuFileSaveText.setText("Textfile...");
		jMenuFileSaveText.addActionListener(this);
		jMenuFileSaveSDF.setText("SD-File...");
		jMenuFileSaveSDF.addActionListener(this);
		jMenuFileSaveTemplate.setText("Template...");
		jMenuFileSaveTemplate.addActionListener(this);
		jMenuFileSaveVisibleAs.setText("Save Visible As...");
		jMenuFileSaveVisibleAs.addActionListener(this);
		jMenuFilePageFormat.setText("Page Format...");
		jMenuFilePageFormat.addActionListener(this);
		jMenuFilePreview.setText("Print Preview");
		jMenuFilePreview.addActionListener(this);
		jMenuFilePrint.setText("Print...");
		jMenuFilePrint.addActionListener(this);
		if (!mApplication.isMacintosh()) {
			jMenuFileExit.setText("Exit");
			jMenuFileExit.setAccelerator(KeyStroke.getKeyStroke('X', MENU_MASK));
			jMenuFileExit.addActionListener(this);
			}

		JMenu jMenuFile = new JMenu();
		jMenuFile.setText("File");
		jMenuFile.add(jMenuFileNew);
		jMenuFile.add(jMenuFileNewFromSelection);
		jMenuFile.add(jMenuFileNewFromList);
		jMenuFile.add(jMenuFileNewFromPivoting);
		jMenuFile.add(jMenuFileNewFromReversePivoting);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileOpen);
		jMenuFile.add(jMenuFileOpenRecent);
		updateRecentFileMenu();
		jMenuFileOpenSpecial.add(jMenuFileOpenTemplate);
		jMenuFileOpenSpecial.add(jMenuFileOpenMacro);
		addActelionOpenFileMenuOptions(jMenuFileOpenSpecial);
		jMenuFileOpenSpecial.add(jMenuFileOpenMDLReactions);
		jMenuFile.add(jMenuFileOpenSpecial);
		jMenuFile.addSeparator();
		addResourceFileMenus(jMenuFile);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileMerge);
		jMenuFile.add(jMenuFileAppend);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileClose);
		jMenuFile.add(jMenuFileCloseAll);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileSave);
		jMenuFile.add(jMenuFileSaveAs);
		jMenuFileSaveSpecial.add(jMenuFileSaveText);
		jMenuFileSaveSpecial.add(jMenuFileSaveSDF);
		addActelionSaveFileMenuOptions(jMenuFileSaveSpecial);
		jMenuFileSaveSpecial.add(jMenuFileSaveTemplate);
		jMenuFile.add(jMenuFileSaveSpecial);
 		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileSaveVisibleAs);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFilePageFormat);
//		jMenuFile.add(jMenuFilePreview);
		jMenuFile.add(jMenuFilePrint);
		if (!mApplication.isMacintosh()) {
			jMenuFile.addSeparator();
			jMenuFile.add(jMenuFileExit);
			}
		return jMenuFile;
		}

	protected void addActelionOpenFileMenuOptions(JMenu jMenuFileOpenSpecial) {}	// override to add Actelion specific items
	protected void addActelionSaveFileMenuOptions(JMenu jMenuFileOpenSpecial) {}	// override to add Actelion specific items

	protected JMenu buildEditMenu() {
		jMenuEditCut = new JMenuItem();
		jMenuEditCopy = new JMenuItem();
		jMenuEditPaste = new JMenuItem();
		jMenuEditPasteSpecial = new JMenu();
		jMenuEditPasteWithHeader = new JMenuItem();
		jMenuEditPasteWithoutHeader = new JMenuItem();
		jMenuEditDelete = new JMenuItem();
		jMenuEditSelectAll = new JMenuItem();
		jMenuEditInvertSelection = new JMenuItem();
		jMenuEditSearchAndReplace = new JMenuItem();
		jMenuEditDisableFilters = new JMenuItem();
		jMenuEditEnableFilters = new JMenuItem();
		jMenuEditResetFilters = new JMenuItem();
		jMenuEditNewFilter = new JMenuItem();

		jMenuEditCut.setText("Cut");
		jMenuEditCut.setAccelerator(KeyStroke.getKeyStroke('X', MENU_MASK));
		jMenuEditCut.setEnabled(false);
		jMenuEditCopy.setText("Copy");
		jMenuEditCopy.setAccelerator(KeyStroke.getKeyStroke('C', MENU_MASK));
		jMenuEditCopy.addActionListener(this);
		jMenuEditPaste.setText("Paste");
		jMenuEditPaste.setAccelerator(KeyStroke.getKeyStroke('V', MENU_MASK));
		jMenuEditPaste.addActionListener(this);
		jMenuEditPasteSpecial.setText("Paste Special");
		jMenuEditPasteWithHeader.setText("Paste With Header Row");
		jMenuEditPasteWithHeader.addActionListener(this);
		jMenuEditPasteWithoutHeader.setText("Paste Without Header Row");
		jMenuEditPasteWithoutHeader.addActionListener(this);
		jMenuEditDelete.setText("Delete");
		jMenuEditDelete.setEnabled(false);
		jMenuEditSelectAll.setText("Select All");
		jMenuEditSelectAll.setAccelerator(KeyStroke.getKeyStroke('A', MENU_MASK));
		jMenuEditSelectAll.addActionListener(this);
		jMenuEditInvertSelection.setText("Invert Selection");
		jMenuEditInvertSelection.addActionListener(this);
		jMenuEditSearchAndReplace.setText("Find And Replace...");
		jMenuEditSearchAndReplace.addActionListener(this);
		jMenuEditSearchAndReplace.setAccelerator(KeyStroke.getKeyStroke('H', MENU_MASK));
		jMenuEditNewFilter.setText("New Filter...");
		jMenuEditNewFilter.addActionListener(this);
		jMenuEditDisableFilters.setText("Disable All Filters");
		jMenuEditDisableFilters.addActionListener(this);
		jMenuEditEnableFilters.setText("Enable All Filters");
		jMenuEditEnableFilters.addActionListener(this);
		jMenuEditResetFilters.setText("Reset All Filters...");
		jMenuEditResetFilters.addActionListener(this);

		JMenu jMenuEdit = new JMenu();
		jMenuEdit.setText("Edit");
		jMenuEdit.add(jMenuEditCut);
		jMenuEdit.add(jMenuEditCopy);
		jMenuEdit.add(jMenuEditPaste);
		jMenuEditPasteSpecial.add(jMenuEditPasteWithHeader);
		jMenuEditPasteSpecial.add(jMenuEditPasteWithoutHeader);
		jMenuEdit.add(jMenuEditPasteSpecial);
		jMenuEdit.add(jMenuEditDelete);
 		jMenuEdit.addSeparator();
		jMenuEdit.add(jMenuEditSelectAll);
		jMenuEdit.add(jMenuEditInvertSelection);
 		jMenuEdit.addSeparator();
 		jMenuEdit.add(jMenuEditSearchAndReplace);
 		jMenuEdit.addSeparator();
 		jMenuEdit.add(jMenuEditNewFilter);
 		jMenuEdit.add(jMenuEditDisableFilters);
 		jMenuEdit.add(jMenuEditEnableFilters);
 		jMenuEdit.add(jMenuEditResetFilters);
 		return jMenuEdit;
		}

	protected JMenu buildDataMenu() {
		jMenuDataRemoveColumns = new JMenuItem();
		jMenuDataRemoveRows = new JMenu();
		jMenuDataRemoveSelected = new JMenuItem();
		jMenuDataRemoveInvisible = new JMenuItem();
		jMenuDataRemoveDuplicate = new JMenuItem();
		jMenuDataRemoveUnique = new JMenuItem();
		jMenuDataMergeColumns = new JMenuItem();
		jMenuDataMergeDuplicate = new JMenuItem();
		jMenuDataAddEmptyColumns = new JMenuItem();
		jMenuDataAddEmptyRows = new JMenuItem();
		jMenuDataAddRowNumbers = new JMenuItem();
		jMenuDataAddBinnedColumn = new JMenuItem();
		jMenuDataAddFuzzyScore = new JMenuItem();
		jMenuDataAddCalculatedValues = new JMenuItem();
		jMenuDataAddPrincipalComponents = new JMenuItem();
		jMenuDataSelfOrganizingMap = new JMenu();
		jMenuDataSOMCreate = new JMenuItem();
		jMenuDataSOMApply = new JMenuItem();
		jMenuDataSOMAnalyse = new JMenuItem();
		jMenuDataGiniScore = new JMenuItem();
		jMenuDataArrangeGraph = new JMenuItem();
		jMenuDataSetRange = new JScrollableMenu();
		jMenuDataViewLogarithmic = new JScrollableMenu();
		jMenuDataCorrelationMatrix = new JMenuItem();

		jMenuDataRemoveColumns.setText("Delete Columns...");
		jMenuDataRemoveColumns.addActionListener(this);
		jMenuDataRemoveRows.setText("Delete Rows");
		jMenuDataRemoveSelected.setText("Selected Rows");
		jMenuDataRemoveSelected.addActionListener(this);
		jMenuDataRemoveInvisible.setText("Invisible Rows");
		jMenuDataRemoveInvisible.addActionListener(this);
		jMenuDataRemoveDuplicate.setText("Duplicate Rows...");
		jMenuDataRemoveDuplicate.addActionListener(this);
		jMenuDataRemoveUnique.setText("Unique Rows...");
		jMenuDataRemoveUnique.addActionListener(this);
		jMenuDataMergeColumns.setText("Merge Colums...");
		jMenuDataMergeColumns.addActionListener(this);
		jMenuDataMergeDuplicate.setText("Merge Rows...");
		jMenuDataMergeDuplicate.addActionListener(this);
		jMenuDataAddEmptyColumns.setText("Add Empty Columns...");
		jMenuDataAddEmptyColumns.addActionListener(this);
		jMenuDataAddEmptyRows.setText("Add Empty Rows...");
		jMenuDataAddEmptyRows.addActionListener(this);
		jMenuDataAddRowNumbers.setText("Add Row Numbers...");
		jMenuDataAddRowNumbers.addActionListener(this);
		jMenuDataAddBinnedColumn.setText("Add Bins From Numbers...");
		jMenuDataAddBinnedColumn.addActionListener(this);
		jMenuDataAddFuzzyScore.setText("Calculate Fuzzy Score...");
		jMenuDataAddFuzzyScore.addActionListener(this);
		jMenuDataGiniScore.setText("Calculate Selectivity Score...");
		jMenuDataGiniScore.addActionListener(this);
		jMenuDataAddCalculatedValues.setText("Add Calculated Values...");
		jMenuDataAddCalculatedValues.addActionListener(this);
		jMenuDataAddPrincipalComponents.setText("Calculate Principal Components...");
		jMenuDataAddPrincipalComponents.addActionListener(this);
		jMenuDataSelfOrganizingMap.setText("Self Organizing Map");
		jMenuDataSOMCreate.setText("Create...");
		jMenuDataSOMCreate.addActionListener(this);
		jMenuDataSOMApply.setText("Open And Apply...");
		jMenuDataSOMApply.addActionListener(this);
		jMenuDataSOMAnalyse.setText("Analyse...");
		jMenuDataSOMAnalyse.addActionListener(this);
		jMenuDataArrangeGraph.setText("Arrange Graph Nodes...");
		jMenuDataArrangeGraph.addActionListener(this);
		jMenuDataViewLogarithmic.setText("Treat Logarithmically");
		jMenuDataSetRange.setText("Set Value Range");
		jMenuDataCorrelationMatrix.setText("Show Correlation Matrix...");
		jMenuDataCorrelationMatrix.addActionListener(this);

		JMenu jMenuData = new JMenu();
		jMenuData.setText("Data");
		jMenuData.add(jMenuDataRemoveColumns);
		jMenuData.add(jMenuDataRemoveRows);
		jMenuDataRemoveRows.add(jMenuDataRemoveSelected);
		jMenuDataRemoveRows.add(jMenuDataRemoveInvisible);
		jMenuDataRemoveRows.add(jMenuDataRemoveDuplicate);
		jMenuDataRemoveRows.add(jMenuDataRemoveUnique);
 		jMenuData.addSeparator();
 		jMenuData.add(jMenuDataMergeColumns);
		jMenuData.add(jMenuDataMergeDuplicate);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataAddEmptyColumns);
		jMenuData.add(jMenuDataAddEmptyRows);
		jMenuData.add(jMenuDataAddRowNumbers);
		jMenuData.add(jMenuDataAddBinnedColumn);
		jMenuData.add(jMenuDataAddCalculatedValues);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataGiniScore);
		jMenuData.add(jMenuDataAddFuzzyScore);
		jMenuData.add(jMenuDataAddPrincipalComponents);
		jMenuData.add(jMenuDataSelfOrganizingMap);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMCreate);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMApply);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMAnalyse);
		if (System.getProperty("development") != null) {
			jMenuData.addSeparator();
			jMenuData.add(jMenuDataArrangeGraph);
			}
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataSetRange);
		jMenuData.add(jMenuDataViewLogarithmic);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataCorrelationMatrix);
		return jMenuData;
		}

	protected JMenu buildChemistryMenu() {
		jMenuChemCCLibrary = new JMenuItem();
		jMenuChemEALibrary = new JMenuItem();
		jMenuChemEnumerateMarkush = new JMenuItem();
		jMenuChemAddProperties = new JMenuItem();
		jMenuChemAddDescriptor = new JMenu();
		jMenuChemAddFormula = new JMenuItem();
		jMenuChemAddSmiles = new JMenuItem();
		jMenuChemAddLargestFragment = new JMenuItem();
		jMenuChemAddStructureFromName = new JMenuItem();
		jMenuChemCreate2DCoords = new JMenuItem();
		jMenuChemCreate3DCoords = new JMenuItem();
		jMenuChemAutomaticSARTable = new JMenuItem();
		jMenuChemCoreBasedSARTable = new JMenuItem();
		jMenuChemInteractiveSARTable = new JMenuItem();
		jMenuChemAnalyzeScaffolds = new JMenuItem();
		jMenuChemAnalyzeCliffs = new JMenuItem();
		jMenuChemMatchFile = new JMenuItem();
		jMenuChemSelectDiverse = new JMenuItem();
		jMenuChemCluster = new JMenuItem();
		jMenuChemExtractPKATree = new JMenuItem();
		jMenuChemUndocumented = new JMenuItem();
		jMenuChemPredictPKa = new JMenuItem();
		jMenuChemCreateGenericTautomers = new JMenuItem();
		jMenuChemCompareDescriptorSimilarityDistribution = new JMenuItem();
		jMenuChemExtractPairwiseCompoundSimilarities = new JMenuItem();
		jMenuChemExtractPairwiseStuff = new JMenuItem();
		jMenuChemCountAtomTypes = new JMenuItem();
		jMenuChemRunSurfacePLS = new JMenuItem();
		jMenuChemClassifyReactions = new JMenuItem();

		jMenuChemCCLibrary.setText("Create Combinatorial Library...");
		jMenuChemCCLibrary.addActionListener(this);
		jMenuChemEALibrary.setText("Create Evolutionary Library...");
		jMenuChemEALibrary.addActionListener(this);
		jMenuChemEnumerateMarkush.setText("Enumerate Markush Structure...");
		jMenuChemEnumerateMarkush.addActionListener(this);
		jMenuChemAddProperties.setText("Add Compound Properties...");
		jMenuChemAddProperties.addActionListener(this);
		jMenuChemAddDescriptor.setText("Add Descriptor");
		jMenuChemAddFormula.setText("Add Molecular Formula...");
		jMenuChemAddFormula.addActionListener(this);
		jMenuChemAddSmiles.setText("Add Smiles...");
		jMenuChemAddSmiles.addActionListener(this);
		jMenuChemAddLargestFragment.setText("Add Largest Fragment...");
		jMenuChemAddLargestFragment.addActionListener(this);
		jMenuChemAddStructureFromName.setText("Add Structures From Name");
		jMenuChemAddStructureFromName.addActionListener(this);
		jMenuChemCreate2DCoords.setText("Generate 2D Atom Coordinates...");
		jMenuChemCreate2DCoords.addActionListener(this);
		jMenuChemCreate3DCoords.setText("Generate Conformers...");
		jMenuChemCreate3DCoords.addActionListener(this);
		jMenuChemAutomaticSARTable.setText("Automatic SAR Analysis...");
		jMenuChemAutomaticSARTable.addActionListener(this);
		jMenuChemCoreBasedSARTable.setText("Core based SAR Analysis...");
		jMenuChemCoreBasedSARTable.addActionListener(this);
		jMenuChemInteractiveSARTable.setText("Interactive SAR Analysis...");
		jMenuChemInteractiveSARTable.addActionListener(this);
		jMenuChemAnalyzeScaffolds.setText("Analyse Scaffolds...");
		jMenuChemAnalyzeScaffolds.addActionListener(this);
		jMenuChemAnalyzeCliffs.setText("Analyse Similarity/Activity Cliffs...");
		jMenuChemAnalyzeCliffs.addActionListener(this);
		jMenuChemMatchFile.setText("Find Similar Compounds In File...");
		jMenuChemMatchFile.addActionListener(this);
		jMenuChemSelectDiverse.setText("Select Diverse Set...");
		jMenuChemSelectDiverse.addActionListener(this);
		jMenuChemCluster.setText("Cluster Compounds...");
		jMenuChemCluster.addActionListener(this);
		jMenuChemExtractPKATree.setText("Extract pKa-Tree");
		jMenuChemExtractPKATree.addActionListener(this);
		jMenuChemUndocumented.setText("Do Undocumented Stuff");
		jMenuChemUndocumented.addActionListener(this);
		jMenuChemPredictPKa.setText("Predict pKa");
		jMenuChemPredictPKa.addActionListener(this);
		jMenuChemCreateGenericTautomers.setText("Create Generic Tautomers");
		jMenuChemCreateGenericTautomers.addActionListener(this);
		jMenuChemCompareDescriptorSimilarityDistribution.setText("Compare Descriptor Similarity Distribution");
		jMenuChemCompareDescriptorSimilarityDistribution.addActionListener(this);
		jMenuChemExtractPairwiseCompoundSimilarities.setText("Extract Pairwise Compound Similarities");
		jMenuChemExtractPairwiseCompoundSimilarities.addActionListener(this);
		jMenuChemExtractPairwiseStuff.setText("Extract Pairwise Similarities And Distances");
		jMenuChemExtractPairwiseStuff.addActionListener(this);
		jMenuChemCountAtomTypes.setText("Count Surface Atom Types");
		jMenuChemCountAtomTypes.addActionListener(this);
		jMenuChemRunSurfacePLS.setText("Run Surface Parameter PLS");
		jMenuChemRunSurfacePLS.addActionListener(this);
		jMenuChemClassifyReactions.setText("Classify Reactions");
		jMenuChemClassifyReactions.addActionListener(this);

		JMenu jMenuChem = new JMenu();
		jMenuChem.setText("Chemistry");
		jMenuChem.add(jMenuChemCCLibrary);
		jMenuChem.add(jMenuChemEALibrary);
//		jMenuChem.add(jMenuChemEnumerateMarkush);
 		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAddProperties);
		jMenuChem.add(jMenuChemAddDescriptor);
		for (int i = 0; i< DescriptorConstants.DESCRIPTOR_LIST.length; i++) {
			JMenuItem item = new JMenuItem();
			item.setText(DescriptorConstants.DESCRIPTOR_LIST[i].shortName);
			item.addActionListener(this);
			jMenuChemAddDescriptor.add(item);
			}
		jMenuChem.add(jMenuChemAddFormula);
		jMenuChem.add(jMenuChemAddSmiles);
		jMenuChem.add(jMenuChemAddLargestFragment);
		jMenuChem.add(jMenuChemAddStructureFromName);
		addActelionChemistryMenuOptions(jMenuChem);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemCreate2DCoords);
		jMenuChem.add(jMenuChemCreate3DCoords);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAutomaticSARTable);
		jMenuChem.add(jMenuChemCoreBasedSARTable);
		if (mApplication.isIdorsia() || System.getProperty("development") != null)
			jMenuChem.add(jMenuChemInteractiveSARTable);
		jMenuChem.add(jMenuChemAnalyzeScaffolds);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAnalyzeCliffs);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemMatchFile);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemSelectDiverse);
		jMenuChem.add(jMenuChemCluster);
		if (System.getProperty("development") != null) {
			jMenuChem.addSeparator();
//			jMenuChem.add(jMenuChemExtractPKATree);
			jMenuChem.add(jMenuChemCompareDescriptorSimilarityDistribution);
			jMenuChem.add(jMenuChemExtractPairwiseCompoundSimilarities);
			jMenuChem.add(jMenuChemExtractPairwiseStuff);
			jMenuChem.addSeparator();
			jMenuChem.add(jMenuChemCountAtomTypes);
			jMenuChem.add(jMenuChemRunSurfacePLS);
			jMenuChem.addSeparator();
			jMenuChem.add(jMenuChemClassifyReactions);
//		jMenuChem.add(jMenuChemPredictPKa);
//		jMenuChem.add(jMenuChemCreateGenericTautomers);
			}
		
		return jMenuChem;
		}

	protected void addActelionChemistryMenuOptions(JMenu jMenuChem) {}	// override to add Actelion specific items

	protected JMenu buildDatabaseMenu() {
		JMenu jMenuDB = new JMenu();
		jMenuDB.setText("Database");

		jMenuDBWikipedia = new JMenuItem();
		jMenuDBWikipedia.setText("Retrieve Wikipedia Molecules");
		jMenuDBWikipedia.addActionListener(this);
		jMenuDB.add(jMenuDBWikipedia);

		jMenuDB.addSeparator();

		jMenuDBReadChEMBL = new JMenuItem();
		jMenuDBReadChEMBL.setText("Search ChEMBL Database...");
		jMenuDBReadChEMBL.addActionListener(this);
		jMenuDB.add(jMenuDBReadChEMBL);

		jMenuDBFindChEMBLActives = new JMenuItem();
		jMenuDBFindChEMBLActives.setText("Get Similar Compounds From ChEMBL Actives");
		jMenuDBFindChEMBLActives.addActionListener(this);
		jMenuDB.add(jMenuDBFindChEMBLActives);

		jMenuDB.addSeparator();

		jMenuDBSearchCOD = new JMenuItem();
		jMenuDBSearchCOD.setText("Search Crystallography Open Database...");
		jMenuDBSearchCOD.addActionListener(this);
		jMenuDB.add(jMenuDBSearchCOD);

		jMenuDB.addSeparator();

		jMenuDBRetrieveDataFromURL = new JMenuItem();
		jMenuDBRetrieveDataFromURL.setText("Retrieve Data From Custom URL...");
		jMenuDBRetrieveDataFromURL.addActionListener(this);
		jMenuDB.add(jMenuDBRetrieveDataFromURL);

		addPluginItems(jMenuDB);

		return jMenuDB;
		}

	protected JMenu buildListMenu() {
		jMenuListCreate = new JMenu();
		jMenuListCreateSelected = new JMenuItem();
		jMenuListCreateVisible = new JMenuItem();
		jMenuListCreateHidden = new JMenuItem();
		jMenuListCreateClipboard = new JMenuItem();
		jMenuListCreateMerge = new JMenuItem();
		jMenuListAddSelectedTo = new DEScrollableMenu();
		jMenuListRemoveSelectedFrom = new DEScrollableMenu();
		jMenuListSelectFrom = new DEScrollableMenu();
		jMenuListDeselectFrom = new DEScrollableMenu();
		jMenuListDelete = new DEScrollableMenu();
		jMenuListDeleteAll = new JMenuItem();
		jMenuListNewColumn = new JMenuItem();
		jMenuListListsFromColumn = new JMenuItem();
		jMenuListImport = new JMenuItem();
		jMenuListExport = new JMenuItem();

		jMenuListCreate.setText("Create Row List From");
		jMenuListCreateSelected.setText("Selected Rows...");
		jMenuListCreateSelected.addActionListener(this);
		jMenuListCreateVisible.setText("Visible Rows...");
		jMenuListCreateVisible.addActionListener(this);
		jMenuListCreateHidden.setText("Hidden Rows...");
		jMenuListCreateHidden.addActionListener(this);
		jMenuListCreateClipboard.setText("Clipboard...");
		jMenuListCreateClipboard.addActionListener(this);
		jMenuListCreateMerge.setText("Existing Row Lists...");
		jMenuListCreateMerge.addActionListener(this);
		jMenuListAddSelectedTo.setText("Add Selected To");
		jMenuListRemoveSelectedFrom.setText("Remove Selected From");
		jMenuListSelectFrom.setText("Select Rows From");
		jMenuListDeselectFrom.setText("Deselect Rows From");
		jMenuListDelete.setText("Delete Row List");
		jMenuListDeleteAll.setText("Delete All Row Lists");
		jMenuListDeleteAll.addActionListener(this);
		jMenuListNewColumn.setText("Add Column From Row Lists...");
		jMenuListNewColumn.addActionListener(this);
		jMenuListListsFromColumn.setText("Create Row Lists From Category Column...");
		jMenuListListsFromColumn.addActionListener(this);
		jMenuListExport.setText("Export Row List...");
		jMenuListExport.addActionListener(this);
		jMenuListImport.setText("Import Row List...");
		jMenuListImport.addActionListener(this);
		jMenuListCreate.add(jMenuListCreateSelected);
		jMenuListCreate.add(jMenuListCreateVisible);
		jMenuListCreate.add(jMenuListCreateHidden);
 		jMenuListCreate.addSeparator();
		jMenuListCreate.add(jMenuListCreateClipboard);
 		jMenuListCreate.addSeparator();
		jMenuListCreate.add(jMenuListCreateMerge);
		JMenu jMenuList = new JMenu();
		jMenuList.setText("List");
		jMenuList.add(jMenuListCreate);
 		jMenuList.addSeparator();
 		jMenuList.add(jMenuListAddSelectedTo);
 		jMenuList.add(jMenuListRemoveSelectedFrom);
 		jMenuList.addSeparator();
 		jMenuList.add(jMenuListSelectFrom);
 		jMenuList.add(jMenuListDeselectFrom);
 		jMenuList.addSeparator();
		jMenuList.add(jMenuListDelete);
		jMenuList.add(jMenuListDeleteAll);
		jMenuList.addSeparator();
		jMenuList.add(jMenuListNewColumn);
		jMenuList.add(jMenuListListsFromColumn);
		jMenuList.addSeparator();
		jMenuList.add(jMenuListImport);
		jMenuList.add(jMenuListExport);
		return jMenuList;
		}

	protected JMenu buildMacroMenu() {
		jMenuMacroImport = new JMenuItem();
		jMenuMacroExport = new JMenu();
		jMenuMacroRun = new JMenu();
		jMenuMacroStartRecording = new JMenuItem();
		jMenuMacroContinueRecording = new JMenuItem();
		jMenuMacroStopRecording = new JMenuItem();

		jMenuMacroImport.setText("Import Macro...");
		jMenuMacroImport.addActionListener(this);
		jMenuMacroExport.setText("Export Macro");
		jMenuMacroStartRecording.setText("Start Recording...");
		jMenuMacroStartRecording.addActionListener(this);
		jMenuMacroContinueRecording.setText("Continue Recording");
		jMenuMacroContinueRecording.addActionListener(this);
		jMenuMacroStopRecording.setText("Stop Recording");
		jMenuMacroStopRecording.addActionListener(this);
		jMenuMacroRun.setText("Run Macro");
		addMenuItem(jMenuMacroExport, "<no macros defined>", null);
		addMacroFileItems(jMenuMacroRun);
		JMenu jMenuMacro = new JMenu();
		jMenuMacro.setText("Macro");
		jMenuMacro.add(jMenuMacroImport);
		jMenuMacro.add(jMenuMacroExport);
 		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroStartRecording);
		jMenuMacro.add(jMenuMacroContinueRecording);
		jMenuMacro.add(jMenuMacroStopRecording);
 		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroRun);
		enableMacroItems();
		return jMenuMacro;
		}

	protected JMenu buildHelpMenu() {
		jMenuHelpLaF = new JMenu();
		jMenuHelpLaF.setText("Look & Feel");
		for (int i=0; i<mApplication.getAvailableLAFNames().length; i++) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem();
			item.setActionCommand(LOOK_AND_FEEL+mApplication.getAvailableLAFClassNames()[i]);
			item.setText(mApplication.getAvailableLAFNames()[i]);
			item.setSelected(mApplication.getAvailableLAFClassNames()[i].equals(
					UIManager.getLookAndFeel().getClass().getCanonicalName()));
			item.addActionListener(this);
			jMenuHelpLaF.add(item);
			}

		jMenuHelpHelp = new JMenuItem();
		jMenuHelpAbout = new JMenuItem();
		jMenuHelpAutomaticUpdateCheck = new JCheckBoxMenuItem();
		jMenuHelpCheckForUpdate = new JMenuItem();
		if (!mApplication.isMacintosh()) {
			jMenuHelpAbout.setText("About...");
			jMenuHelpAbout.addActionListener(this);
			}
		jMenuHelpHelp.setText("Help...");
		jMenuHelpHelp.addActionListener(this);

		JMenu jMenuHelp = new JMenu();
		if (!mApplication.isMacintosh()) {
			jMenuHelp.add(jMenuHelpAbout);
			jMenuHelp.addSeparator();
			}
		jMenuHelp.setText("Help");
		jMenuHelp.add(jMenuHelpHelp);
		jMenuHelp.addSeparator();
		jMenuHelp.add(jMenuHelpLaF);

		if (!mApplication.isIdorsia()) {
			Preferences prefs = Preferences.userRoot().node(DataWarrior.PREFERENCES_ROOT);
			boolean check = prefs.getBoolean(DataWarrior.PREFERENCES_KEY_AUTO_UPDATE_CHECK, true);

			jMenuHelpAutomaticUpdateCheck.setText("Automatically Check For Updates");
			jMenuHelpAutomaticUpdateCheck.setSelected(check);
			jMenuHelpAutomaticUpdateCheck.addActionListener(this);
			jMenuHelpCheckForUpdate.setText("Check For Update Now...");
			jMenuHelpCheckForUpdate.addActionListener(this);

			jMenuHelp.addSeparator();
			jMenuHelp.add(jMenuHelpAutomaticUpdateCheck);
			jMenuHelp.add(jMenuHelpCheckForUpdate);
			}

		return jMenuHelp;
		}

	private void ensurePageFormat(PrinterJob job) {
		if (mPageFormat == null) {
			mPageFormat = job.defaultPage();
			Paper paper = mPageFormat.getPaper();
			paper.setImageableArea(60, 30, paper.getWidth() - 90, paper.getHeight() - 60);
			mPageFormat.setPaper(paper);
			}
		}

	private void menuFilePageFormat() {
		PrinterJob job = PrinterJob.getPrinterJob();
		ensurePageFormat(job);
		mPageFormat = job.pageDialog(mPageFormat);
		}

	private void menuFilePreview() {
		
		}

	public void menuFilePrint() {
		if (mMainPane.getSelectedDockable() == null) {
			JOptionPane.showMessageDialog(mParentFrame, "Sorry, an empty view cannot be printed");
			return;
			}

		PrinterJob job = PrinterJob.getPrinterJob();
		ensurePageFormat(job);

		try {
			Component component = mMainPane.getSelectedDockable().getContent();
			if (component instanceof DEFormView) {
				JCompoundTableForm form = ((DEFormView)component).getCompoundTableForm();
				if (!new DEPrintFormDialog(mParentFrame, mTableModel, form).isOK())
					return;
				form.setPageFormat(mPageFormat);
				job.setPageable(form);
				}
			else { // assume Printable
				job.setPrintable((Printable)component, mPageFormat);
				}

			job.setJobName("DataWarrior:"+mParentFrame.getTitle());
			if (job.printDialog()) {
				try {
					job.print();
					}
				catch (PrinterException e) {
					JOptionPane.showMessageDialog(mParentFrame, e);
					}
				}
			}
		catch (ClassCastException e) {
			JOptionPane.showMessageDialog(mParentFrame, "Sorry, the current view cannot be printed");
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			jMenuFileNewFromList.removeAll();
			jMenuListSelectFrom.removeAll();
			jMenuListDeselectFrom.removeAll();
			jMenuListAddSelectedTo.removeAll();
			jMenuListRemoveSelectedFrom.removeAll();
			jMenuListDelete.removeAll();
			}
		if (e.getType() == CompoundTableEvent.cNewTable
		 || e.getType() == CompoundTableEvent.cAddColumns
		 || e.getType() == CompoundTableEvent.cRemoveColumns
		 || e.getType() == CompoundTableEvent.cChangeColumnName
		 || e.getType() == CompoundTableEvent.cChangeColumnData
		 || e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows) {
			jMenuDataViewLogarithmic.removeAll();
			jMenuDataSetRange.removeAll();
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isLogarithmicViewMode(column)
				 || (mTableModel.isColumnTypeDouble(column)
				  && !mTableModel.isColumnTypeDate(column)
				  && mTableModel.getMinimumValue(column) > 0)) {
					JCheckBoxMenuItem item = new StayOpenCheckBoxMenuItem(mTableModel.getColumnTitle(column),
																   mTableModel.isLogarithmicViewMode(column));
					item.addItemListener(this);
					jMenuDataViewLogarithmic.add(item);
					}
				if (mTableModel.isColumnTypeDouble(column)
				 && !mTableModel.isColumnTypeDate(column)) {
					addMenuItem(jMenuDataSetRange, mTableModel.getColumnTitle(column)+"...", SET_RANGE+mTableModel.getColumnTitle(column));
					}
				} jMenuDataViewLogarithmic.updateUI();
			}
		if (e.getType() == CompoundTableEvent.cNewTable
		 || (e.getType() == CompoundTableEvent.cChangeExtensionData
		  && e.getSpecifier() == DECompoundTableExtensionHandler.ID_MACRO)) {
			jMenuMacroExport.removeAll();
			jMenuMacroRun.removeAll();
			@SuppressWarnings("unchecked")
			ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
			if (macroList == null || macroList.size() == 0) {
				addMenuItem(jMenuMacroExport, "<no macros defined>", null);
				}
			else {
				for (DEMacro macro:macroList) {
					addMenuItem(jMenuMacroExport, macro.getName()+"...", EXPORT_MACRO+macro.getName());
					addMenuItem(jMenuMacroRun, macro.getName(), RUN_INTERNAL_MACRO+macro.getName());
					}
				}
			addMacroFileItems(jMenuMacroRun);
			}
		}

	public void listChanged(CompoundTableListEvent e) {
		jMenuFileNewFromList.removeAll();
		jMenuListSelectFrom.removeAll();
		jMenuListDeselectFrom.removeAll();
		jMenuListAddSelectedTo.removeAll();
		jMenuListRemoveSelectedFrom.removeAll();
		jMenuListDelete.removeAll();
		CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();

		for (int hitlist = 0; hitlist<hitlistHandler.getListCount(); hitlist++) {
			addMenuItem(jMenuFileNewFromList, hitlistHandler.getListName(hitlist), NEW_FROM_LIST+hitlist);
			addMenuItem(jMenuListAddSelectedTo, hitlistHandler.getListName(hitlist), LIST_ADD+hitlist);
			addMenuItem(jMenuListRemoveSelectedFrom, hitlistHandler.getListName(hitlist), LIST_REMOVE+hitlist);
			addMenuItem(jMenuListSelectFrom, hitlistHandler.getListName(hitlist), LIST_SELECT+hitlist);
			addMenuItem(jMenuListDeselectFrom, hitlistHandler.getListName(hitlist), LIST_DESELECT+hitlist);
			addMenuItem(jMenuListDelete, hitlistHandler.getListName(hitlist), LIST_DELETE+hitlist);
			}
		}

	/**
	 * @param menu
	 * @param text
	 * @param actionCommand if null, then show menu item as disabled
	 */
	private void addMenuItem(JMenu menu, String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		if (actionCommand != null) {
			item.setActionCommand(actionCommand);
			item.addActionListener(this);
			}
		menu.add(item);
		}

	public void actionPerformed(ActionEvent e) {
		try {
			Object source = e.getSource();
			String actionCommand = e.getActionCommand();
			if (source == jMenuFilePageFormat)
				menuFilePageFormat();
			else if (source == jMenuFilePreview)
				menuFilePreview();
			else if (source == jMenuFilePrint)
				menuFilePrint();
			else if (source == jMenuFileExit)
				new DETaskExitProgram(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuHelpAbout)
				new DEAboutDialog(mParentFrame);
			else if (source == jMenuHelpHelp)
				new DEHelpFrame(mParentFrame);
			else if (source == jMenuFileNew)
				new DETaskNewFile(mApplication).defineAndRun();
			else if (source == jMenuFileNewFromSelection)
				new DETaskNewFileFromSelection(mParentFrame, mApplication).defineAndRun();
			else if (actionCommand.startsWith(NEW_FROM_LIST))
				new DETaskNewFileFromList(mParentFrame, mApplication, Integer.parseInt(actionCommand.substring(NEW_FROM_LIST.length()))).defineAndRun();
			else if (source == jMenuFileNewFromPivoting)
				new DETaskNewFileFromPivoting(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuFileNewFromReversePivoting)
				new DETaskNewFileFromReversePivoting(mParentFrame, mApplication).defineAndRun();
			else if (actionCommand.startsWith(OPEN_FILE))	// these are the reference,sample,etc-files
				new DETaskOpenFile(mApplication, actionCommand.substring(OPEN_FILE.length())).defineAndRun();
			else if (source == jMenuFileOpen)
				new DETaskOpenFile(mApplication).defineAndRun();
			else if (source == jMenuFileOpenTemplate)
				new DETaskApplyTemplateFromFile(mApplication).defineAndRun();
			else if (source == jMenuFileOpenMacro)
				new DETaskRunMacroFromFile(mApplication).defineAndRun();
			else if (source == jMenuFileOpenMDLReactions)
				new DENativeMDLReactionReader(mParentFrame, mApplication).read();
			else if (source == jMenuFileMerge)
				new DETaskMergeFile(mParentFrame, true).defineAndRun();
			else if (source == jMenuFileAppend) {
				if (mParentFrame.getTableModel().getTotalRowCount() == 0)
					JOptionPane.showMessageDialog(mParentFrame, "You cannot append a file to an empty table. Use 'Open File...' instead.");
				else {
					File file = FileHelper.getFile(mParentFrame, "Append DataWarrior-, SD- or Text-File", FileHelper.cFileTypeDataWarriorCompatibleData);
					if (file != null)
						new DEFileLoader(mParentFrame, null).appendFile(file);
					}
				}
			else if (source == jMenuFileClose)
				new DETaskCloseWindow(mParentFrame, mApplication, mParentFrame).defineAndRun();
			else if (source == jMenuFileCloseAll)
				mApplication.closeAllFramesSafely(true);
			else if (source == jMenuFileSave) {
				if (mTableModel.getFile() == null)
					new DETaskSaveFileAs(mParentFrame).defineAndRun();
				else
					new DETaskSaveFile(mParentFrame).defineAndRun();
				}
			else if (source == jMenuFileSaveAs)
				new DETaskSaveFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveText)
				new DETaskSaveTextFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveSDF)
				new DETaskSaveSDFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveTemplate)
				new DETaskSaveTemplateFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveVisibleAs)
				new DETaskSaveVisibleRowsAs(mParentFrame).defineAndRun();
			else if (source == jMenuEditCopy)
				new DETaskCopy(mParentFrame).defineAndRun();
			else if (source == jMenuEditPaste)
				new DETaskPaste(mParentFrame, mApplication, DETaskPaste.HEADER_ANALYZE).defineAndRun();
			else if (source == jMenuEditPasteWithHeader)
				new DETaskPaste(mParentFrame, mApplication, DETaskPaste.HEADER_WITH).defineAndRun();
			else if (source == jMenuEditPasteWithoutHeader)
				new DETaskPaste(mParentFrame, mApplication, DETaskPaste.HEADER_WITHOUT).defineAndRun();
			else if (source == jMenuEditSelectAll)
				new DETaskSelectAll(mParentFrame).defineAndRun();
			else if (source == jMenuEditInvertSelection)
				new DETaskInvertSelection(mParentFrame).defineAndRun();
			else if (source == jMenuEditSearchAndReplace)
				new DETaskFindAndReplace(mParentFrame).defineAndRun();
			else if (source == jMenuEditNewFilter)
				new DETaskAddNewFilter(mParentFrame, mParentPane.getPruningPanel()).defineAndRun();
			else if (source == jMenuEditDisableFilters)
				new DETaskDisableAllFilters(mParentFrame).defineAndRun();
			else if (source == jMenuEditEnableFilters)
				new DETaskEnableAllFilters(mParentFrame).defineAndRun();
			else if (source == jMenuEditResetFilters) {
				if (JOptionPane.showConfirmDialog(mParentFrame,
												  "Do you really want to clear all filter settings?",
												  "Reset All Filters?",
												  JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
					new DETaskResetAllFilters(mParentFrame).defineAndRun();
				}
			else if (source == jMenuDataRemoveColumns)
				new DETaskDeleteColumns(mParentFrame, mTableModel, null).defineAndRun();
			else if (source == jMenuDataRemoveSelected)
				new DETaskDeleteSelectedRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveInvisible)
				new DETaskDeleteInvisibleRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveDuplicate)
				new DETaskDeleteDuplicateRows(mParentFrame, DETaskDeleteDuplicateRows.MODE_REMOVE_DUPLICATE).defineAndRun();
			else if (source == jMenuDataRemoveUnique)
				new DETaskDeleteDuplicateRows(mParentFrame, DETaskDeleteDuplicateRows.MODE_REMOVE_UNIQUE).defineAndRun();
			else if (source == jMenuDataMergeColumns)
				new DETaskMergeColumns(mParentFrame).defineAndRun();
			else if (source == jMenuDataMergeDuplicate)
				new DETaskDeleteDuplicateRows(mParentFrame, DETaskDeleteDuplicateRows.MODE_MERGE_DUPLICATE).defineAndRun();
			else if (source == jMenuDataAddRowNumbers)
				new DETaskAddRecordNumbers(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddEmptyColumns)
				new DETaskAddEmptyColumns(mApplication).defineAndRun();
			else if (source == jMenuDataAddEmptyRows)
				new DETaskAddEmptyRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddBinnedColumn)
				new DETaskAddBinsFromNumbers(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddFuzzyScore)
				new DETaskCalculateFuzzyScore(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddCalculatedValues)
				new DETaskAddCalculatedValues(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddPrincipalComponents)
				new DETaskPCA(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataSOMCreate)
				new DETaskCalculateSOM(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataSOMApply)
				new DETaskApplySOMFile(mApplication).defineAndRun();
			else if (source == jMenuDataSOMAnalyse)
				new DETaskAnalyseSOMFile(mApplication).defineAndRun();
			else if (source == jMenuDataGiniScore)
				new DETaskCalculateSelectivityScore(mParentFrame, mTableModel).defineAndRun();
			else if (source == jMenuDataArrangeGraph)
				new DETaskArrangeGraphNodes(mParentFrame).defineAndRun();
			else if (source == jMenuDataCorrelationMatrix)
				new DECorrelationDialog(mParentFrame, mTableModel);
			else if (source == jMenuChemCCLibrary)
				new DETaskCreateCombinatorialLibrary(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemEALibrary)
				new DETaskCreateEvolutionaryLibrary(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemEnumerateMarkush)
				new DEMarkushDialog(mParentFrame, mApplication);
			else if (source == jMenuChemAddProperties)
				new DETaskCalculateChemicalProperties(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddFormula)
				new DETaskAddFormula(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddSmiles)
				new DETaskAddSmiles(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddLargestFragment)
				new DETaskAddLargestFragment(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddStructureFromName)
				new DETaskAddStructureFromName(mParentFrame).defineAndRun();
			else if (source == jMenuChemCreate2DCoords)
				new DETaskAdd2DCoordinates(mParentFrame).defineAndRun();
			else if (source == jMenuChemCreate3DCoords)
				new DETaskAdd3DCoordinates(mParentFrame).defineAndRun();
			else if (source == jMenuChemAutomaticSARTable)
				new DETaskAutomaticSAR(mParentFrame).defineAndRun();
			else if (source == jMenuChemCoreBasedSARTable)
				new DECoreBasedSAR(mParentFrame).defineAndRun();
			else if (source == jMenuChemInteractiveSARTable) {
JOptionPane.showMessageDialog(mParentFrame, "This functionality is not final yet.\nSuggestions and sample data are welcome.");
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new DEInteractiveSARDialog(mParentFrame, mTableModel, idcodeColumn);
				}
			else if (source == jMenuChemAnalyzeScaffolds)
				new DETaskAnalyseScaffolds(mParentFrame).defineAndRun();
			else if (source == jMenuChemAnalyzeCliffs)
				new DETaskAnalyseActivityCliffs(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemMatchFile)
				new DETaskFindSimilarCompoundsInFile(mParentFrame).defineAndRun();
			else if (source == jMenuChemSelectDiverse)
				new DETaskSelectDiverse(mParentFrame).defineAndRun();
			else if (source == jMenuChemCluster)
				new DETaskClusterCompounds(mParentFrame).defineAndRun();
/*		  else if (source == jMenuChemExtractPKATree)
				new PKATreeExtractor(mParentFrame, new PKADataWarriorAdapter(mTableModel)).extract();	*/
			else if (source == jMenuChemCompareDescriptorSimilarityDistribution)
				new DETestCompareDescriptorSimilarityDistribution(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemExtractPairwiseCompoundSimilarities)
				new DETestExtractPairwiseCompoundSimilarities(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemExtractPairwiseStuff)
				new DETestExtractPairwiseStuff(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemCountAtomTypes)
				new DETestCountAtomTypes(mParentFrame).defineAndRun();
			else if (source == jMenuChemRunSurfacePLS)
				new DETestRunSurfacePLS(mParentFrame).defineAndRun();
			else if (source == jMenuChemClassifyReactions)
				new DETaskClassifyReactions(mParentFrame).defineAndRun();

//			  int idcodeColumn = getStructureColumn(true);
//			  if (idcodeColumn != -1)
//					new UndocumentedStuff(mParentFrame, mMainPane, mTableModel, idcodeColumn).doStuff();

/*			else if (source == jMenuChemPredictPKa) {
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new UndocumentedStuff(mParentFrame, mMainPane, mTableModel, idcodeColumn).predictPKaValues();
				}*/
			else if (source == jMenuChemCreateGenericTautomers) {
				int idcodeColumn = getStructureColumn(false);
				if (idcodeColumn != -1)
					new DEGenericTautomerCreator(mParentFrame, mTableModel).create(idcodeColumn);
				}
			else if (source == jMenuDBWikipedia)
				new DETaskRetrieveWikipediaCompounds(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBReadChEMBL)
				new DETaskChemblQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBFindChEMBLActives)
				new DETaskFindSimilarActiveCompounds(mParentFrame).defineAndRun();
			else if (source == jMenuDBSearchCOD)
				new DETaskCODQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBRetrieveDataFromURL)
				new DETaskRetrieveDataFromURL(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuListCreateSelected) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_SELECTED))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_SELECTED).defineAndRun();
				}
			else if (source == jMenuListCreateVisible) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_VISIBLE))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_VISIBLE).defineAndRun();
				}
			else if (source == jMenuListCreateHidden) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_HIDDEN))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_HIDDEN).defineAndRun();
				}
			else if (source == jMenuListCreateClipboard)
				new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_CLIPBOARD).defineAndRun();
			else if (source == jMenuListCreateMerge)
				new DETaskCombineTwoRowLists(mParentFrame).defineAndRun();
			else if (actionCommand.startsWith(LIST_ADD))
				new DETaskAddSelectionToList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_ADD.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_REMOVE))
				new DETaskRemoveSelectionFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_REMOVE.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_SELECT))
				new DETaskSelectRowsFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_SELECT.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_DESELECT))
				new DETaskDeselectRowsFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_DESELECT.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_DELETE))
				new DETaskDeleteRowList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_DELETE.length()))).defineAndRun();
			else if (source == jMenuListDeleteAll) {
				String[] names = mTableModel.getListHandler().getListNames();
				if (names == null) {
					JOptionPane.showMessageDialog(mParentFrame, "There are no row lists to be removed.");
					}
				else {
					int doDelete = JOptionPane.showConfirmDialog(this,
									"Do you really want to delete all row lists?",
									"Delete All Row Lists?",
									JOptionPane.OK_CANCEL_OPTION);
					if (doDelete == JOptionPane.OK_OPTION)
						new DETaskDeleteAllRowLists(mParentFrame).defineAndRun();
					}
				}
			else if (source == jMenuListNewColumn)
				new DETaskNewColumnWithListNames(mParentFrame).defineAndRun();
			else if (source == jMenuListListsFromColumn)
				new DETaskCreateListsFromCategories(mParentFrame).defineAndRun();
			else if (source == jMenuListImport)
				new DETaskImportHitlist(mApplication).defineAndRun();
			else if (source == jMenuListExport)
				new DETaskExportHitlist(mParentFrame, true).defineAndRun();
			else if (source == jMenuMacroImport) {
				new DETaskImportMacro(mApplication).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(EXPORT_MACRO)) {
				String macroName = e.getActionCommand().substring(EXPORT_MACRO.length());
				new DETaskExportMacro(mParentFrame, macroName).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(RUN_GLOBAL_MACRO)) {
				new DETaskRunMacroFromFile(mApplication, e.getActionCommand().substring(RUN_GLOBAL_MACRO.length())).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(RUN_INTERNAL_MACRO)) {
				new DETaskRunMacro(mParentFrame, e.getActionCommand().substring(RUN_INTERNAL_MACRO.length())).defineAndRun();
				}
			else if (source == jMenuMacroStartRecording)
				new DEDialogRecordMacro(mParentFrame);
			else if (source == jMenuMacroContinueRecording) {
				DEMacroRecorder.getInstance().continueRecording();
				enableMacroItems();
				}
			else if (source == jMenuMacroStopRecording) {
				DEMacroRecorder.getInstance().stopRecording();
				enableMacroItems();
				}
			else if (actionCommand.startsWith(LOOK_AND_FEEL)) {
				for (int i=0; i<jMenuHelpLaF.getItemCount(); i++) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem)jMenuHelpLaF.getItem(i);
					if (item != source)
						item.setSelected(false);
					}
				String lafName = actionCommand.substring(LOOK_AND_FEEL.length());
				mApplication.updateLookAndFeel(lafName);
				DEHelpFrame.updateLookAndFeel();
				}
			else if (source == jMenuHelpAutomaticUpdateCheck) {
				Preferences prefs = Preferences.userRoot().node(DataWarrior.PREFERENCES_ROOT);
				prefs.putBoolean(DataWarrior.PREFERENCES_KEY_AUTO_UPDATE_CHECK, jMenuHelpAutomaticUpdateCheck.isSelected());
				}
			else if (source == jMenuHelpCheckForUpdate) {
				mApplication.checkVersion(true);
				}
			else if (actionCommand.startsWith(SET_RANGE)) {
				int column = mTableModel.findColumn(actionCommand.substring(SET_RANGE.length()));
				new DETaskSetValueRange(mParentFrame, column).defineAndRun();
				}
			else if (DescriptorHelper.isDescriptorShortName(actionCommand)) {
				new DETaskCalculateDescriptor(mParentFrame, actionCommand).defineAndRun();
				}
			else
				JOptionPane.showMessageDialog(mParentFrame, "This option is not supported yet.");
			}
		catch (OutOfMemoryError ex) {
			JOptionPane.showMessageDialog(mParentFrame, ex);
			}
		}

	private boolean checkAndAllowEmptyList(int listMode) {
		boolean isEmpty = (listMode == DETaskNewRowList.MODE_SELECTED) ?
				mMainPane.getTable().getSelectionModel().isSelectionEmpty()
						: (listMode == DETaskNewRowList.MODE_VISIBLE) ?
				(mTableModel.getRowCount() == 0)
						: (listMode == DETaskNewRowList.MODE_HIDDEN) ?
				(mTableModel.getRowCount() == mTableModel.getTotalRowCount())
						: false;	// should not happen

		if (!isEmpty)
			return true;

		String message = (listMode == DETaskNewRowList.MODE_SELECTED) ?
				"The selection is empty."
						: (listMode == DETaskNewRowList.MODE_VISIBLE) ?
				"There are no visible rows."
						:
				"There are now hidden rows.";

		int doDelete = JOptionPane.showConfirmDialog(mParentFrame,
						message+"\nDo you really want to create an empty list?",
						"Create Empty List?",
						JOptionPane.OK_CANCEL_OPTION);
		return (doDelete == JOptionPane.OK_OPTION);
		}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem)e.getSource();
			int[] columnList = new int[1];
			columnList[0] = mTableModel.findColumn(menuItem.getText());
			new DETaskSetLogarithmicMode(mParentFrame, mTableModel, columnList, menuItem.isSelected()).defineAndRun();
			}
		}

	private int getStructureColumn(boolean requireFingerprint) {
		int idcodeColumn = -1;

		int[] idcodeColumnList = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumnList == null) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your columns contains chemical structures.");
			}
		else if (idcodeColumnList.length == 1) {
			idcodeColumn = idcodeColumnList[0];
			}
		else {
			String[] columnNameList = new String[idcodeColumnList.length];
			for (int i=0; i<idcodeColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(idcodeColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(mParentFrame,
								"Please select a column with chemical structures!",
								"Select Structure Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			idcodeColumn = mTableModel.findColumn(columnName);
			}

		if (idcodeColumn != -1 && requireFingerprint) {
			int fingerprintColumn = mTableModel.getChildColumn(idcodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			if (fingerprintColumn == -1) {
				JOptionPane.showMessageDialog(mParentFrame, "Please create first a chemical fingerprint for the selected structure column.");
				idcodeColumn = -1;
				}
			if (!mTableModel.isDescriptorAvailable(fingerprintColumn)) {
				JOptionPane.showMessageDialog(mParentFrame, "Please wait until the chemical fingerprint creation is completed.");
				idcodeColumn = -1;
				}
			}
		
		return idcodeColumn;
		}

	private int getReactionColumn() {
		int reactionColumn = -1;

		int[] reactionColumnList = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeRXNCode);
		if (reactionColumnList == null) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your columns contains chemical reaction.");
			}
		else if (reactionColumnList.length == 1) {
			reactionColumn = reactionColumnList[0];
			}
		else {
			String[] columnNameList = new String[reactionColumnList.length];
			for (int i=0; i<reactionColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(reactionColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(mParentFrame,
								"Please select a column with chemical reactions!",
								"Select Reaction Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			reactionColumn = mTableModel.findColumn(columnName);
			}

		return reactionColumn;
		}

	public void updateRecentFileMenu() {
		jMenuFileOpenRecent.removeAll();
		try {
			Preferences prefs = Preferences.userRoot().node(DataWarrior.PREFERENCES_ROOT);

			for (int i=1; i<=MAX_RECENT_FILE_COUNT; i++) {
				String path = prefs.get(PREFERENCES_KEY_RECENT_FILE + i, "");
				if (path == null || path.length() == 0)
					break;

				File file = new File(path);
				if (file.exists())
					addMenuItem(jMenuFileOpenRecent, file.getName(), OPEN_FILE+path);
				}
			}
		catch (Exception e) {}
		}

	private void addResourceFileMenus(JMenu parentMenu) {
		// alternative to get location of datawarrior.jar:
		//   getClass().getProtectionDomain().getCodeSource().getLocation();

		for (String resDir:DataWarrior.RESOURCE_DIR) {
			File directory = DataWarrior.resolveResourcePath(resDir);
			if (directory != null)
				addResourceFileMenu(parentMenu, "Open "+resDir+" File", DataWarrior.makePathVariable(resDir), directory);
			}

		String dirlist = System.getProperty("datapath");
		if (dirlist != null)
			parentMenu.addSeparator();

		while (dirlist != null) {
			int index = dirlist.indexOf(File.pathSeparatorChar);
			String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
			dirlist = (index == -1) ? null : dirlist.substring(index+1);
			File directory = new File(DataWarrior.resolveVariables(dirname));
			if (directory.exists())
				addResourceFileMenu(parentMenu, "Open User File <"+directory.getName()+">", dirname, directory);
			}
		}

	/**
	 * @param parentMenu
	 * @param itemString
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param directory
	 */
	private void addResourceFileMenu(JMenu parentMenu, String itemString, String dirPath, File directory) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return false;
				return (file.getName().toLowerCase().endsWith(".dwar"));
				}
			};
		File[] file = directory.listFiles(filter);
		if (file != null && file.length != 0) {
			JMenu menu = new JScrollableMenu(itemString);
			Arrays.sort(file, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
			});
			for (int i=0; i<file.length; i++) {
				addMenuItem(menu, file[i].getName(), OPEN_FILE+dirPath+File.separator+file[i].getName());
				}
			parentMenu.add(menu);
			}
		}

	private void addPluginItems(JMenu parentMenu) {
		ArrayList<IPluginTask> pluginList = mApplication.getPluginRegistry().getPluginTasks();
		if (pluginList == null || pluginList.size() == 0)
			return;

		parentMenu.addSeparator();

		for (final IPluginTask pluginTask:pluginList) {
			JMenuItem item = new JMenuItem(pluginTask.getTaskName()+"...");
			item.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
					new DETaskPluginTask(mParentFrame, pluginTask).defineAndRun();
/*					final Properties configuration = pluginTask.showDialog(mParentFrame);
					if (configuration != null) {
						final JProgressDialog progressDialog = new JProgressDialog(mParentFrame);
						final PluginHelper pluginHelper = new PluginHelper(mApplication, progressDialog);
						new Thread() {
										@Override public void run() {
								progressDialog.startProgress("Starting Task Action...", 0, 0);
								pluginTask.run(configuration, pluginHelper);
								progressDialog.close(pluginHelper.getNewFrame());
								}
										}.start();
						progressDialog.setVisible(true);
						}
*/					}

							});
			parentMenu.add(item);
			}
		}

	private void addMacroFileItems(JMenu parentMenu) {
		File directory = DataWarrior.resolveResourcePath(DataWarrior.MACRO_DIR);
		if (directory != null)
			addMacroFileItems(parentMenu, DataWarrior.makePathVariable(DataWarrior.MACRO_DIR), directory);

		directory = new File(System.getProperty("user.home")+File.separator+"datawarrior"+File.separator+"macro");
		if (directory != null)
			addMacroFileItems(parentMenu, USER_MACRO_DIR, directory);

		if (Platform.isWindows()) {
			String userMacroPath = "C:\\Users\\".concat(System.getProperty("user.name")).concat("\\AppData\\Roaming\\DataWarrior\\Macro");
			directory = new File(userMacroPath);
			if (directory.exists())
				addMacroFileItems(parentMenu, userMacroPath, directory);
			}

		String dirlist = System.getProperty("macropath");
		while (dirlist != null) {
			int index = dirlist.indexOf(File.pathSeparatorChar);
			String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
			dirlist = (index == -1) ? null : dirlist.substring(index+1);
			directory = new File(DataWarrior.resolveVariables(dirname));
			if (directory.exists())
				addMacroFileItems(parentMenu, dirname, directory);
			}

		if (parentMenu.getItemCount() == 0) {
			JMenuItem item = new JMenuItem("<no macros defined>");
			item.setEnabled(false);
			parentMenu.add(item);
			}
		}

	/**
	 * @param parentMenu
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param directory
	 */
	private void addMacroFileItems(JMenu parentMenu, String dirPath, File directory) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return false;
				return (file.getName().toLowerCase().endsWith(".dwam"));
				}
			};
		File[] file = directory.listFiles(filter);
		if (file != null && file.length != 0) {
			if (parentMenu.getItemCount() != 0)
				parentMenu.addSeparator();
			Arrays.sort(file);
			for (int i=0; i<file.length; i++) {
				try {
					String macroName = new DEMacro(file[i], null).getName();
					addMenuItem(parentMenu, macroName, RUN_GLOBAL_MACRO + dirPath + File.separator + file[i].getName());
					}
				catch (IOException ioe) {}
				}
			}
		}

	public void enableMacroItems() {
		boolean isRecording = DEMacroRecorder.getInstance().isRecording();
		jMenuMacroStartRecording.setEnabled(!isRecording);
		jMenuMacroStopRecording.setEnabled(isRecording);
		jMenuMacroContinueRecording.setEnabled(DEMacroRecorder.getInstance().canContinueRecording(mParentFrame));
		}
	}
