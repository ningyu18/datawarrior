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

package com.actelion.research.datawarrior.task;

import java.util.TreeSet;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.chem.DECoreBasedSAR;
import com.actelion.research.datawarrior.task.chem.DETaskAdd2DCoordinates;
import com.actelion.research.datawarrior.task.chem.DETaskAdd3DCoordinates;
import com.actelion.research.datawarrior.task.chem.DETaskAddCIPInfo;
import com.actelion.research.datawarrior.task.chem.DETaskAddFormula;
import com.actelion.research.datawarrior.task.chem.DETaskAddLargestFragment;
import com.actelion.research.datawarrior.task.chem.DETaskAddSmiles;
import com.actelion.research.datawarrior.task.chem.DETaskAnalyseActivityCliffs;
import com.actelion.research.datawarrior.task.chem.DETaskAnalyseScaffolds;
import com.actelion.research.datawarrior.task.chem.DETaskAutomaticSAR;
import com.actelion.research.datawarrior.task.chem.DETaskCalculateChemicalProperties;
import com.actelion.research.datawarrior.task.chem.DETaskCalculateDescriptor;
import com.actelion.research.datawarrior.task.chem.DETaskClassifyReactions;
import com.actelion.research.datawarrior.task.chem.DETaskClusterCompounds;
import com.actelion.research.datawarrior.task.chem.DETaskFindSimilarCompoundsInFile;
import com.actelion.research.datawarrior.task.chem.DETestCompareDescriptorSimilarityDistribution;
import com.actelion.research.datawarrior.task.chem.DETestExtractPairwiseCompoundSimilarities;
import com.actelion.research.datawarrior.task.data.DETaskAddEmptyColumns;
import com.actelion.research.datawarrior.task.data.DETaskAddEmptyRows;
import com.actelion.research.datawarrior.task.data.DETaskAddRecordNumbers;
import com.actelion.research.datawarrior.task.data.DETaskCalculateColumn;
import com.actelion.research.datawarrior.task.data.DETaskCalculateSOM;
import com.actelion.research.datawarrior.task.data.DETaskCreateBins;
import com.actelion.research.datawarrior.task.data.DETaskDeleteColumns;
import com.actelion.research.datawarrior.task.data.DETaskDeleteInvisibleRows;
import com.actelion.research.datawarrior.task.data.DETaskDeleteRedundantRows;
import com.actelion.research.datawarrior.task.data.DETaskDeleteSelectedRows;
import com.actelion.research.datawarrior.task.data.DETaskMergeColumns;
import com.actelion.research.datawarrior.task.data.DETaskPCA;
import com.actelion.research.datawarrior.task.data.DETaskSearchAndReplace;
import com.actelion.research.datawarrior.task.data.DETaskSetCategoryCustomOrder;
import com.actelion.research.datawarrior.task.data.DETaskSetCurrentRow;
import com.actelion.research.datawarrior.task.data.DETaskSetLogarithmicMode;
import com.actelion.research.datawarrior.task.data.DETaskSetValueRange;
import com.actelion.research.datawarrior.task.data.DETaskSortRows;
import com.actelion.research.datawarrior.task.db.DETaskChemblQuery;
import com.actelion.research.datawarrior.task.db.DETaskRetrieveWikipediaCompounds;
import com.actelion.research.datawarrior.task.elib.DETaskCreateEvolutionaryLibrary;
import com.actelion.research.datawarrior.task.file.DETaskAnalyseSOMFile;
import com.actelion.research.datawarrior.task.file.DETaskApplySOMFile;
import com.actelion.research.datawarrior.task.file.DETaskApplyTemplateFromFile;
import com.actelion.research.datawarrior.task.file.DETaskCloseWindow;
import com.actelion.research.datawarrior.task.file.DETaskExportMacro;
import com.actelion.research.datawarrior.task.file.DETaskImportMacro;
import com.actelion.research.datawarrior.task.file.DETaskMergeFile;
import com.actelion.research.datawarrior.task.file.DETaskNewFile;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromList;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromPivoting;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromReversePivoting;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromSelection;
import com.actelion.research.datawarrior.task.file.DETaskOpenFile;
import com.actelion.research.datawarrior.task.file.DETaskRunMacroFromFile;
import com.actelion.research.datawarrior.task.file.DETaskSaveFile;
import com.actelion.research.datawarrior.task.file.DETaskSaveFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveSDFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveTemplateFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveTextFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveVisibleRowsAs;
import com.actelion.research.datawarrior.task.filter.DETaskAddNewFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeCategoryBrowser;
import com.actelion.research.datawarrior.task.filter.DETaskChangeCategoryFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeListFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeRangeFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeSimilarStructureListFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeStructureFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeSubstructureListFilter;
import com.actelion.research.datawarrior.task.filter.DETaskChangeTextFilter;
import com.actelion.research.datawarrior.task.filter.DETaskCloseFilter;
import com.actelion.research.datawarrior.task.filter.DETaskDisableAllFilters;
import com.actelion.research.datawarrior.task.filter.DETaskEnableAllFilters;
import com.actelion.research.datawarrior.task.filter.DETaskResetAllFilters;
import com.actelion.research.datawarrior.task.list.DETaskAddSelectionToList;
import com.actelion.research.datawarrior.task.list.DETaskCombineTwoRowLists;
import com.actelion.research.datawarrior.task.list.DETaskDeleteAllRowLists;
import com.actelion.research.datawarrior.task.list.DETaskDeleteRowList;
import com.actelion.research.datawarrior.task.list.DETaskDeselectRowsFromList;
import com.actelion.research.datawarrior.task.list.DETaskExportHitlist;
import com.actelion.research.datawarrior.task.list.DETaskImportHitlist;
import com.actelion.research.datawarrior.task.list.DETaskNewColumnWithListNames;
import com.actelion.research.datawarrior.task.list.DETaskNewRowList;
import com.actelion.research.datawarrior.task.list.DETaskRemoveSelectionFromList;
import com.actelion.research.datawarrior.task.list.DETaskSelectRowsFromList;
import com.actelion.research.datawarrior.task.view.DETaskAssignOrZoomAxes;
import com.actelion.research.datawarrior.task.view.DETaskCloseView;
import com.actelion.research.datawarrior.task.view.DETaskCopyStatisticalValues;
import com.actelion.research.datawarrior.task.view.DETaskCopyView;
import com.actelion.research.datawarrior.task.view.DETaskDuplicateView;
import com.actelion.research.datawarrior.task.view.DETaskNewView;
import com.actelion.research.datawarrior.task.view.DETaskRelocateView;
import com.actelion.research.datawarrior.task.view.DETaskRenameView;
import com.actelion.research.datawarrior.task.view.DETaskSelectView;
import com.actelion.research.datawarrior.task.view.DETaskSeparateCases;
import com.actelion.research.datawarrior.task.view.DETaskSetConnectionLines;
import com.actelion.research.datawarrior.task.view.DETaskSetFocus;
import com.actelion.research.datawarrior.task.view.DETaskSetGeneralViewProperties;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerBackgroundColor;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerColor;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerJittering;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerShape;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerSize;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerTransparency;
import com.actelion.research.datawarrior.task.view.DETaskSetMultiValueMarker;
import com.actelion.research.datawarrior.task.view.DETaskSetPreferredChartType;
import com.actelion.research.datawarrior.task.view.DETaskSetStatisticalViewOptions;
import com.actelion.research.datawarrior.task.view.DETaskSetTextBackgroundColor;
import com.actelion.research.datawarrior.task.view.DETaskSetTextColor;
import com.actelion.research.datawarrior.task.view.DETaskShowLabels;
import com.actelion.research.datawarrior.task.view.DETaskSynchronizeView;

public class StandardTaskFactory {
	protected TreeSet<TaskSpecification> mTaskDictionary;

	/**
	 * Creates a task one of these purposes:<br>
	 * - provide a configuration UI for any data set (not necessarily the current table model)<br>
	 * - to execute the new task with a earlier created configuration
	 * @param frame
	 * @param taskCode
	 * @return
	 */
	public AbstractTask createTask(DEFrame frame, String taskCode) {
		DataWarrior application = frame.getApplication();
		DEMainPane mainPane = frame.getMainFrame().getMainPane();
		DEPruningPanel pruningPanel = frame.getMainFrame().getPruningPanel();
		return codeMatches(taskCode, DETaskAdd2DCoordinates.TASK_NAME) ? new DETaskAdd2DCoordinates(frame)
			 : codeMatches(taskCode, DETaskAdd3DCoordinates.TASK_NAME) ? new DETaskAdd3DCoordinates(frame)
			 : codeMatches(taskCode, DETaskAddEmptyColumns.TASK_NAME) ? new DETaskAddEmptyColumns(application)
			 : codeMatches(taskCode, DETaskAddEmptyRows.TASK_NAME) ? new DETaskAddEmptyRows(frame)
			 : codeMatches(taskCode, DETaskAddCIPInfo.TASK_NAME) ? new DETaskAddCIPInfo(frame, false)
			 : codeMatches(taskCode, DETaskAddFormula.TASK_NAME) ? new DETaskAddFormula(frame)
			 : codeMatches(taskCode, DETaskAddLargestFragment.TASK_NAME) ? new DETaskAddLargestFragment(frame)
			 : codeMatches(taskCode, DETaskAddNewFilter.TASK_NAME) ? new DETaskAddNewFilter(frame, pruningPanel)
			 : codeMatches(taskCode, DETaskAddRecordNumbers.TASK_NAME) ? new DETaskAddRecordNumbers(frame, false)
			 : codeMatches(taskCode, DETaskAddSelectionToList.TASK_NAME) ? new DETaskAddSelectionToList(frame, -1)
			 : codeMatches(taskCode, DETaskAddSmiles.TASK_NAME) ? new DETaskAddSmiles(frame)
			 : codeMatches(taskCode, DETaskAnalyseActivityCliffs.TASK_NAME) ? new DETaskAnalyseActivityCliffs(frame, application, false)
			 : codeMatches(taskCode, DETaskAnalyseScaffolds.TASK_NAME) ? new DETaskAnalyseScaffolds(frame, false)
			 : codeMatches(taskCode, DETaskAnalyseSOMFile.TASK_NAME) ? new DETaskAnalyseSOMFile(application)
			 : codeMatches(taskCode, DETaskApplySOMFile.TASK_NAME) ? new DETaskApplySOMFile(frame)
			 : codeMatches(taskCode, DETaskApplyTemplateFromFile.TASK_NAME) ? new DETaskApplyTemplateFromFile(application)
			 : codeMatches(taskCode, DETaskAssignOrZoomAxes.TASK_NAME) ? new DETaskAssignOrZoomAxes(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskAutomaticSAR.TASK_NAME) ? new DETaskAutomaticSAR(frame)
			 : codeMatches(taskCode, DETaskCalculateChemicalProperties.TASK_NAME) ? new DETaskCalculateChemicalProperties(frame, false)
			 : codeMatches(taskCode, DETaskCalculateColumn.TASK_NAME) ? new DETaskCalculateColumn(frame)
			 : codeMatches(taskCode, DETaskCalculateDescriptor.TASK_NAME) ? new DETaskCalculateDescriptor(frame, null)
			 : codeMatches(taskCode, DETaskCalculateSOM.TASK_NAME) ? new DETaskCalculateSOM(frame, false)
			 : codeMatches(taskCode, DETaskChangeCategoryBrowser.TASK_NAME) ? new DETaskChangeCategoryBrowser(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeCategoryFilter.TASK_NAME) ? new DETaskChangeCategoryFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeListFilter.TASK_NAME) ? new DETaskChangeListFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeRangeFilter.TASK_NAME) ? new DETaskChangeRangeFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeSimilarStructureListFilter.TASK_NAME) ? new DETaskChangeSimilarStructureListFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeStructureFilter.TASK_NAME) ? new DETaskChangeStructureFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeSubstructureListFilter.TASK_NAME) ? new DETaskChangeSubstructureListFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChangeTextFilter.TASK_NAME) ? new DETaskChangeTextFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskChemblQuery.TASK_NAME) ? new DETaskChemblQuery(frame, application)
			 : codeMatches(taskCode, DETaskClassifyReactions.TASK_NAME) ? new DETaskClassifyReactions(frame, false)
			 : codeMatches(taskCode, DETaskCloseFilter.TASK_NAME) ? new DETaskCloseFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskCloseView.TASK_NAME) ? new DETaskCloseView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskCloseWindow.TASK_NAME) ? new DETaskCloseWindow(frame, application, null)
			 : codeMatches(taskCode, DETaskClusterCompounds.TASK_NAME) ? new DETaskClusterCompounds(frame, false)
			 : codeMatches(taskCode, DETaskCombineTwoRowLists.TASK_NAME) ? new DETaskCombineTwoRowLists(frame, false)
			 : codeMatches(taskCode, DETaskCopy.TASK_NAME) ? new DETaskCopy(frame)
			 : codeMatches(taskCode, DETaskCopyStatisticalValues.TASK_NAME) ? new DETaskCopyStatisticalValues(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskCopyView.TASK_NAME) ? new DETaskCopyView(frame, mainPane, null)
			 : codeMatches(taskCode, DECoreBasedSAR.TASK_NAME) ? new DECoreBasedSAR(frame)
			 : codeMatches(taskCode, DETaskCreateBins.TASK_NAME) ? new DETaskCreateBins(frame, false)
			 : codeMatches(taskCode, DETaskCreateEvolutionaryLibrary.TASK_NAME) ? new DETaskCreateEvolutionaryLibrary(frame, application)
			 : codeMatches(taskCode, DETaskDeleteAllRowLists.TASK_NAME) ? new DETaskDeleteAllRowLists(frame)
			 : codeMatches(taskCode, DETaskDeleteColumns.TASK_NAME) ? new DETaskDeleteColumns(frame, mainPane.getTableModel(), false)
			 : codeMatches(taskCode, DETaskDeleteInvisibleRows.TASK_NAME) ? new DETaskDeleteInvisibleRows(frame)
			 : codeMatches(taskCode, DETaskDeleteRedundantRows.TASK_NAME[0]) ? new DETaskDeleteRedundantRows(frame, 0, false)
			 : codeMatches(taskCode, DETaskDeleteRedundantRows.TASK_NAME[1]) ? new DETaskDeleteRedundantRows(frame, 1, false)
			 : codeMatches(taskCode, DETaskDeleteRedundantRows.TASK_NAME[2]) ? new DETaskDeleteRedundantRows(frame, 2, false)
			 : codeMatches(taskCode, DETaskDeleteRowList.TASK_NAME) ? new DETaskDeleteRowList(frame, -1)
			 : codeMatches(taskCode, DETaskDeleteSelectedRows.TASK_NAME) ? new DETaskDeleteSelectedRows(frame)
			 : codeMatches(taskCode, DETaskDeselectRowsFromList.TASK_NAME) ? new DETaskDeselectRowsFromList(frame, -1)
			 : codeMatches(taskCode, DETaskDisableAllFilters.TASK_NAME) ? new DETaskDisableAllFilters(frame)
			 : codeMatches(taskCode, DETaskDuplicateView.TASK_NAME) ? new DETaskDuplicateView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskEnableAllFilters.TASK_NAME) ? new DETaskEnableAllFilters(frame)
			 : codeMatches(taskCode, DETaskExportHitlist.TASK_NAME) ? new DETaskExportHitlist(frame, false)
			 : codeMatches(taskCode, DETaskExportMacro.TASK_NAME) ? new DETaskExportMacro(frame, null)
			 : codeMatches(taskCode, DETaskImportHitlist.TASK_NAME) ? new DETaskImportHitlist(frame)
			 : codeMatches(taskCode, DETaskImportMacro.TASK_NAME) ? new DETaskImportMacro(frame)
			 : codeMatches(taskCode, DETaskInvertSelection.TASK_NAME) ? new DETaskInvertSelection(frame)
			 : codeMatches(taskCode, DETaskFindSimilarCompoundsInFile.TASK_NAME) ? new DETaskFindSimilarCompoundsInFile(frame, false)
			 : codeMatches(taskCode, DETaskMergeColumns.TASK_NAME) ? new DETaskMergeColumns(frame, false)
			 : codeMatches(taskCode, DETaskMergeFile.TASK_NAME) ? new DETaskMergeFile(frame)
			 : codeMatches(taskCode, DETaskNewColumnWithListNames.TASK_NAME) ? new DETaskNewColumnWithListNames(frame)
			 : codeMatches(taskCode, DETaskNewFile.TASK_NAME) ? new DETaskNewFile(application)
			 : codeMatches(taskCode, DETaskNewFileFromList.TASK_NAME) ? new DETaskNewFileFromList(frame, application, -1)
			 : codeMatches(taskCode, DETaskNewFileFromPivoting.TASK_NAME) ? new DETaskNewFileFromPivoting(frame, application)
			 : codeMatches(taskCode, DETaskNewFileFromReversePivoting.TASK_NAME) ? new DETaskNewFileFromReversePivoting(frame, application)
			 : codeMatches(taskCode, DETaskNewFileFromSelection.TASK_NAME) ? new DETaskNewFileFromSelection(frame, application)
			 : codeMatches(taskCode, DETaskNewRowList.TASK_NAME) ? new DETaskNewRowList(frame, -1)
			 : codeMatches(taskCode, DETaskNewView.TASK_NAME) ? new DETaskNewView(frame, mainPane, -1, null, -1)
			 : codeMatches(taskCode, DETaskOpenFile.TASK_NAME) ? new DETaskOpenFile(application)
			 : codeMatches(taskCode, DETaskPCA.TASK_NAME) ? new DETaskPCA(frame, false)
			 : codeMatches(taskCode, DETaskPaste.TASK_NAME) ? new DETaskPaste(frame, application)
			 : codeMatches(taskCode, DETaskRelocateView.TASK_NAME) ? new DETaskRelocateView(frame, mainPane, null, null, -1)
			 : codeMatches(taskCode, DETaskRemoveSelectionFromList.TASK_NAME) ? new DETaskRemoveSelectionFromList(frame, -1)
			 : codeMatches(taskCode, DETaskRenameView.TASK_NAME) ? new DETaskRenameView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskRepeatNextTask.TASK_NAME) ? new DETaskRepeatNextTask(frame)
			 : codeMatches(taskCode, DETaskRetrieveWikipediaCompounds.TASK_NAME) ? new DETaskRetrieveWikipediaCompounds(frame, application, false)
			 : codeMatches(taskCode, DETaskResetAllFilters.TASK_NAME) ? new DETaskResetAllFilters(frame)
			 : codeMatches(taskCode, DETaskRunMacro.TASK_NAME) ? new DETaskRunMacro(frame, null)
			 : codeMatches(taskCode, DETaskRunMacroFromFile.TASK_NAME) ? new DETaskRunMacroFromFile(application)
			 : codeMatches(taskCode, DETaskSaveFile.TASK_NAME) ? new DETaskSaveFile(frame)
			 : codeMatches(taskCode, DETaskSaveFileAs.TASK_NAME) ? new DETaskSaveFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveSDFileAs.TASK_NAME) ? new DETaskSaveSDFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveTemplateFileAs.TASK_NAME) ? new DETaskSaveTemplateFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveTextFileAs.TASK_NAME) ? new DETaskSaveTextFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveVisibleRowsAs.TASK_NAME) ? new DETaskSaveVisibleRowsAs(frame)
			 : codeMatches(taskCode, DETaskSearchAndReplace.TASK_NAME) ? new DETaskSearchAndReplace(frame)
			 : codeMatches(taskCode, DETaskSelectAll.TASK_NAME) ? new DETaskSelectAll(frame)
			 : codeMatches(taskCode, DETaskSelectDiverse.TASK_NAME) ? new DETaskSelectDiverse(frame, false)
			 : codeMatches(taskCode, DETaskSelectRowsFromList.TASK_NAME) ? new DETaskSelectRowsFromList(frame, -1)
			 : codeMatches(taskCode, DETaskSelectView.TASK_NAME) ? new DETaskSelectView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSelectWindow.TASK_NAME) ? new DETaskSelectWindow(frame, application, null)
			 : codeMatches(taskCode, DETaskSeparateCases.TASK_NAME) ? new DETaskSeparateCases(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetCategoryCustomOrder.TASK_NAME) ? new DETaskSetCategoryCustomOrder(frame, frame.getTableModel(), -1)
			 : codeMatches(taskCode, DETaskSetConnectionLines.TASK_NAME) ? new DETaskSetConnectionLines(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetCurrentRow.TASK_NAME) ? new DETaskSetCurrentRow(frame)
			 : codeMatches(taskCode, DETaskSetFocus.TASK_NAME) ? new DETaskSetFocus(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetGeneralViewProperties.TASK_NAME) ? new DETaskSetGeneralViewProperties(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetLogarithmicMode.TASK_NAME) ? new DETaskSetLogarithmicMode(frame, -1, false)
			 : codeMatches(taskCode, DETaskSetMarkerBackgroundColor.TASK_NAME) ? new DETaskSetMarkerBackgroundColor(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerColor.TASK_NAME) ? new DETaskSetMarkerColor(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerJittering.TASK_NAME) ? new DETaskSetMarkerJittering(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerShape.TASK_NAME) ? new DETaskSetMarkerShape(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerSize.TASK_NAME) ? new DETaskSetMarkerSize(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerTransparency.TASK_NAME) ? new DETaskSetMarkerTransparency(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMultiValueMarker.TASK_NAME) ? new DETaskSetMultiValueMarker(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetPreferredChartType.TASK_NAME) ? new DETaskSetPreferredChartType(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetStatisticalViewOptions.TASK_NAME) ? new DETaskSetStatisticalViewOptions(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetTextBackgroundColor.TASK_NAME) ? new DETaskSetTextBackgroundColor(frame, mainPane, null, -1)
			 : codeMatches(taskCode, DETaskSetTextColor.TASK_NAME) ? new DETaskSetTextColor(frame, mainPane, null, -1)
			 : codeMatches(taskCode, DETaskSetValueRange.TASK_NAME) ? new DETaskSetValueRange(frame, -1)
			 : codeMatches(taskCode, DETaskShowLabels.TASK_NAME) ? new DETaskShowLabels(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskShowMessage.TASK_NAME) ? new DETaskShowMessage(frame)
			 : codeMatches(taskCode, DETaskSortRows.TASK_NAME) ? new DETaskSortRows(frame, frame.getTableModel(), -1, false)
			 : codeMatches(taskCode, DETaskSynchronizeView.TASK_NAME) ? new DETaskSynchronizeView(frame, mainPane, null, null)
			 : codeMatches(taskCode, DETaskWait.TASK_NAME) ? new DETaskWait(frame)
			 : null;
		}

	public TreeSet<TaskSpecification> getTaskDictionary() {
		if (mTaskDictionary == null) {
			mTaskDictionary = new TreeSet<TaskSpecification>();
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAdd2DCoordinates.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAdd3DCoordinates.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddEmptyColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddEmptyRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAddCIPInfo.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAddFormula.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAddLargestFragment.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskAddNewFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddRecordNumbers.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskAddSelectionToList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAddSmiles.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAnalyseActivityCliffs.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAnalyseScaffolds.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAnalyseSOMFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskApplySOMFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskApplyTemplateFromFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskAssignOrZoomAxes.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAutomaticSAR.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCalculateChemicalProperties.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskCalculateColumn.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCalculateDescriptor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskCalculateSOM.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeCategoryBrowser.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeCategoryFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeListFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeRangeFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeStructureFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeSimilarStructureListFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeSubstructureListFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskChangeTextFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATABASE, DETaskChemblQuery.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskClassifyReactions.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskCloseFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskCloseView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskCloseWindow.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskClusterCompounds.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCombineTwoRowLists.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskCopy.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskCopyStatisticalValues.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskCopyView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DECoreBasedSAR.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskCreateBins.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCreateEvolutionaryLibrary.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskDeleteAllRowLists.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteInvisibleRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteRedundantRows.TASK_NAME[0]));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteRedundantRows.TASK_NAME[1]));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteRedundantRows.TASK_NAME[2]));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskDeleteRowList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteSelectedRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskDeselectRowsFromList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskDisableAllFilters.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskDuplicateView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskEnableAllFilters.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskExportHitlist.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskExportMacro.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskFindSimilarCompoundsInFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskImportHitlist.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskImportMacro.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskInvertSelection.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskMergeColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskMergeFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskNewColumnWithListNames.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskNewFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskNewFileFromList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskNewFileFromPivoting.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskNewFileFromReversePivoting.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskNewFileFromSelection.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskNewRowList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskNewView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskOpenFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskPCA.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskPaste.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskRelocateView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskRemoveSelectionFromList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskRenameView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskRepeatNextTask.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATABASE, DETaskRetrieveWikipediaCompounds.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskResetAllFilters.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskRunMacro.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskRunMacroFromFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSaveFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSaveFileAs.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSaveSDFileAs.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSaveTemplateFileAs.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSaveTextFileAs.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSaveVisibleRowsAs.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskSearchAndReplace.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskSelectAll.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskSelectDiverse.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskSelectRowsFromList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSelectView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSelectWindow.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSeparateCases.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetCategoryCustomOrder.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetConnectionLines.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskSetCurrentRow.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetFocus.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetGeneralViewProperties.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetLogarithmicMode.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerBackgroundColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerJittering.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerSize.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerTransparency.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMultiValueMarker.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetPreferredChartType.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetStatisticalViewOptions.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetTextBackgroundColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetTextColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetValueRange.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskShowLabels.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskShowMessage.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSortRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSynchronizeView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskWait.TASK_NAME));
			
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TEST, DETestCompareDescriptorSimilarityDistribution.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TEST, DETestExtractPairwiseCompoundSimilarities.TASK_NAME));
			}

		return mTaskDictionary;
		}

	protected boolean codeMatches(String taskCode, String taskName) {
		return taskCode.equals(ConfigurableTask.constructTaskCodeFromName(taskName));
		}
	}
