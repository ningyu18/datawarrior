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

package com.actelion.research.datawarrior.task;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.chem.*;
import com.actelion.research.datawarrior.task.chem.clib.DETaskCreateCombinatorialLibrary;
import com.actelion.research.datawarrior.task.chem.elib.DETaskCreateEvolutionaryLibrary;
import com.actelion.research.datawarrior.task.data.*;
import com.actelion.research.datawarrior.task.data.fuzzy.DETaskCalculateFuzzyScore;
import com.actelion.research.datawarrior.task.db.*;
import com.actelion.research.datawarrior.task.file.*;
import com.actelion.research.datawarrior.task.filter.*;
import com.actelion.research.datawarrior.task.list.*;
import com.actelion.research.datawarrior.task.macro.*;
import com.actelion.research.datawarrior.task.table.*;
import com.actelion.research.datawarrior.task.view.*;
import org.openmolecules.datawarrior.plugin.IPluginTask;

import java.util.ArrayList;
import java.util.TreeSet;

public class StandardTaskFactory {
	private DataWarrior mApplication;
	protected TreeSet<TaskSpecification> mTaskDictionary;

	public StandardTaskFactory(DataWarrior application) {
		mApplication = application;
		}

	/**
	 * Creates a task one of these purposes:<br>
	 * - provide a configuration UI for any data set (not necessarily the current table model)<br>
	 * - to execute the new task with an earlier created configuration
	 * @param frame
	 * @param taskName
	 * @return
	 */
	public AbstractTask createTaskFromName(DEFrame frame, String taskName) {
		return createTaskFromCode(frame, getTaskCodeFromName(taskName));
	}

	/**
	 * Creates a task one of these purposes:<br>
	 * - provide a configuration UI for any data set (not necessarily the current table model)<br>
	 * - to execute the new task with an earlier created configuration
	 * @param frame
	 * @param taskCode
	 * @return
	 */
	public AbstractTask createTaskFromCode(DEFrame frame, String taskCode) {
		DataWarrior application = frame.getApplication();
		DEMainPane mainPane = frame.getMainFrame().getMainPane();
		DEPruningPanel pruningPanel = frame.getMainFrame().getPruningPanel();
		return codeMatches(taskCode, DETaskAdd2DCoordinates.TASK_NAME) ? new DETaskAdd2DCoordinates(frame)
			 : codeMatches(taskCode, DETaskAdd3DCoordinates.TASK_NAME) ? new DETaskAdd3DCoordinates(frame)
			 : codeMatches(taskCode, DETaskAddEmptyColumns.TASK_NAME) ? new DETaskAddEmptyColumns(application)
			 : codeMatches(taskCode, DETaskAddEmptyRows.TASK_NAME) ? new DETaskAddEmptyRows(frame)
			 : codeMatches(taskCode, DETaskAddCIPInfo.TASK_NAME) ? new DETaskAddCIPInfo(frame, false)
			 : codeMatches(taskCode, DETaskAddFormula.TASK_NAME) ? new DETaskAddFormula(frame)
			 : codeMatches(taskCode, DETaskCalculateFuzzyScore.TASK_NAME) ? new DETaskCalculateFuzzyScore(frame)
			 : codeMatches(taskCode, DETaskAddLargestFragment.TASK_NAME) ? new DETaskAddLargestFragment(frame)
			 : codeMatches(taskCode, DETaskAddNewFilter.TASK_NAME) ? new DETaskAddNewFilter(frame, pruningPanel)
			 : codeMatches(taskCode, DETaskAddRecordNumbers.TASK_NAME) ? new DETaskAddRecordNumbers(frame)
			 : codeMatches(taskCode, DETaskAddSelectionToList.TASK_NAME) ? new DETaskAddSelectionToList(frame, -1)
			 : codeMatches(taskCode, DETaskAddSmiles.TASK_NAME) ? new DETaskAddSmiles(frame)
			 : codeMatches(taskCode, DETaskAnalyseActivityCliffs.TASK_NAME) ? new DETaskAnalyseActivityCliffs(frame, application)
			 : codeMatches(taskCode, DETaskAnalyseScaffolds.TASK_NAME) ? new DETaskAnalyseScaffolds(frame)
			 : codeMatches(taskCode, DETaskAnalyseSOMFile.TASK_NAME) ? new DETaskAnalyseSOMFile(application)
			 : codeMatches(taskCode, DETaskApplySOMFile.TASK_NAME) ? new DETaskApplySOMFile(application)
			 : codeMatches(taskCode, DETaskApplyTemplateFromFile.TASK_NAME) ? new DETaskApplyTemplateFromFile(application)
			 : codeMatches(taskCode, DETaskArrangeGraphNodes.TASK_NAME) ? new DETaskArrangeGraphNodes(frame)
			 : codeMatches(taskCode, DETaskAssignOrZoomAxes.TASK_NAME) ? new DETaskAssignOrZoomAxes(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskAutomaticSAR.TASK_NAME) ? new DETaskAutomaticSAR(frame)
			 : codeMatches(taskCode, DETaskCalculateChemicalProperties.TASK_NAME) ? new DETaskCalculateChemicalProperties(frame)
			 : codeMatches(taskCode, DETaskAddCalculatedValues.TASK_NAME) ? new DETaskAddCalculatedValues(frame)
			 : codeMatches(taskCode, DETaskCalculateDescriptor.TASK_NAME) ? new DETaskCalculateDescriptor(frame, null)
			 : codeMatches(taskCode, DETaskCalculateSelectivityScore.TASK_NAME) ? new DETaskCalculateSelectivityScore(frame, mainPane.getTableModel())
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
			 : codeMatches(taskCode, DETaskClassifyReactions.TASK_NAME) ? new DETaskClassifyReactions(frame)
			 : codeMatches(taskCode, DETaskCloseFilter.TASK_NAME) ? new DETaskCloseFilter(frame, pruningPanel, null)
			 : codeMatches(taskCode, DETaskCloseView.TASK_NAME) ? new DETaskCloseView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskCloseWindow.TASK_NAME) ? new DETaskCloseWindow(frame, application, null)
			 : codeMatches(taskCode, DETaskClusterCompounds.TASK_NAME) ? new DETaskClusterCompounds(frame)
			 : codeMatches(taskCode, DETaskCODQuery.TASK_NAME) ? new DETaskCODQuery(frame, application)
			 : codeMatches(taskCode, DETaskCombineTwoRowLists.TASK_NAME) ? new DETaskCombineTwoRowLists(frame)
			 : codeMatches(taskCode, DETaskCopy.TASK_NAME) ? new DETaskCopy(frame)
			 : codeMatches(taskCode, DETaskCopyStatisticalValues.TASK_NAME) ? new DETaskCopyStatisticalValues(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskCopyView.TASK_NAME) ? new DETaskCopyView(frame, mainPane, null)
			 : codeMatches(taskCode, DECoreBasedSAR.TASK_NAME) ? new DECoreBasedSAR(frame)
			 : codeMatches(taskCode, DETaskAddBinsFromNumbers.TASK_NAME) ? new DETaskAddBinsFromNumbers(frame)
			 : codeMatches(taskCode, DETaskCreateCombinatorialLibrary.TASK_NAME) ? new DETaskCreateCombinatorialLibrary(frame, application)
			 : codeMatches(taskCode, DETaskCreateEvolutionaryLibrary.TASK_NAME) ? new DETaskCreateEvolutionaryLibrary(frame, application)
			 : codeMatches(taskCode, DETaskCreateListsFromCategories.TASK_NAME) ? new DETaskCreateListsFromCategories(frame)
			 : codeMatches(taskCode, DETaskDefineVariable.TASK_NAME) ? new DETaskDefineVariable(frame)
			 : codeMatches(taskCode, DETaskDeleteAllRowLists.TASK_NAME) ? new DETaskDeleteAllRowLists(frame)
			 : codeMatches(taskCode, DETaskDeleteColumns.TASK_NAME) ? new DETaskDeleteColumns(frame, mainPane.getTableModel(), null)
			 : codeMatches(taskCode, DETaskDeleteInvisibleRows.TASK_NAME) ? new DETaskDeleteInvisibleRows(frame)
			 : codeMatches(taskCode, DETaskDeleteDuplicateRows.TASK_NAME[0]) ? new DETaskDeleteDuplicateRows(frame, 0)
			 : codeMatches(taskCode, DETaskDeleteDuplicateRows.TASK_NAME[1]) ? new DETaskDeleteDuplicateRows(frame, 1)
			 : codeMatches(taskCode, DETaskDeleteDuplicateRows.TASK_NAME[2]) ? new DETaskDeleteDuplicateRows(frame, 2)
			 : codeMatches(taskCode, DETaskDeleteRowList.TASK_NAME) ? new DETaskDeleteRowList(frame, -1)
			 : codeMatches(taskCode, DETaskDeleteSelectedRows.TASK_NAME) ? new DETaskDeleteSelectedRows(frame)
			 : codeMatches(taskCode, DETaskDeselectRowsFromList.TASK_NAME) ? new DETaskDeselectRowsFromList(frame, -1)
			 : codeMatches(taskCode, DETaskDisableAllFilters.TASK_NAME) ? new DETaskDisableAllFilters(frame)
			 : codeMatches(taskCode, DETaskDuplicateView.TASK_NAME) ? new DETaskDuplicateView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskEnableAllFilters.TASK_NAME) ? new DETaskEnableAllFilters(frame)
			 : codeMatches(taskCode, DETaskExitProgram.TASK_NAME) ? new DETaskExitProgram(frame, application)
			 : codeMatches(taskCode, DETaskExportHitlist.TASK_NAME) ? new DETaskExportHitlist(frame, false)
			 : codeMatches(taskCode, DETaskExportMacro.TASK_NAME) ? new DETaskExportMacro(frame, null)
			 : codeMatches(taskCode, DETaskFindSimilarCompoundsInFile.TASK_NAME) ? new DETaskFindSimilarCompoundsInFile(frame)
			 : codeMatches(taskCode, DETaskHideTableColumns.TASK_NAME) ? new DETaskHideTableColumns(frame, mainPane.getTableView(), null)
			 : codeMatches(taskCode, DETaskImportHitlist.TASK_NAME) ? new DETaskImportHitlist(application)
			 : codeMatches(taskCode, DETaskImportMacro.TASK_NAME) ? new DETaskImportMacro(application)
			 : codeMatches(taskCode, DETaskInvertSelection.TASK_NAME) ? new DETaskInvertSelection(frame)
			 : codeMatches(taskCode, DETaskJumpToCurrentRow.TASK_NAME) ? new DETaskJumpToCurrentRow(frame, mainPane)
			 : codeMatches(taskCode, DETaskMergeColumns.TASK_NAME) ? new DETaskMergeColumns(frame)
			 : codeMatches(taskCode, DETaskMergeFile.TASK_NAME) ? new DETaskMergeFile(frame, false)
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
			 : codeMatches(taskCode, DETaskPaste.TASK_NAME) ? new DETaskPaste(frame, application, -1)
			 : codeMatches(taskCode, DETaskRelocateView.TASK_NAME) ? new DETaskRelocateView(frame, mainPane, null, null, -1)
			 : codeMatches(taskCode, DETaskRemoveSelectionFromList.TASK_NAME) ? new DETaskRemoveSelectionFromList(frame, -1)
			 : codeMatches(taskCode, DETaskRenameView.TASK_NAME) ? new DETaskRenameView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskRepeatNextTask.TASK_NAME) ? new DETaskRepeatNextTask(frame)
			 : codeMatches(taskCode, DETaskRetrieveDataFromURL.TASK_NAME) ? new DETaskRetrieveDataFromURL(frame, application)
			 : codeMatches(taskCode, DETaskRetrieveWikipediaCompounds.TASK_NAME) ? new DETaskRetrieveWikipediaCompounds(frame, application)
			 : codeMatches(taskCode, DETaskResetAllFilters.TASK_NAME) ? new DETaskResetAllFilters(frame)
			 : codeMatches(taskCode, DETaskRunMacro.TASK_NAME) ? new DETaskRunMacro(frame, null)
			 : codeMatches(taskCode, DETaskRunMacroFromFile.TASK_NAME) ? new DETaskRunMacroFromFile(application)
			 : codeMatches(taskCode, DETaskSaveFile.TASK_NAME) ? new DETaskSaveFile(frame)
			 : codeMatches(taskCode, DETaskSaveFileAs.TASK_NAME) ? new DETaskSaveFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveSDFileAs.TASK_NAME) ? new DETaskSaveSDFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveTemplateFileAs.TASK_NAME) ? new DETaskSaveTemplateFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveTextFileAs.TASK_NAME) ? new DETaskSaveTextFileAs(frame)
			 : codeMatches(taskCode, DETaskSaveVisibleRowsAs.TASK_NAME) ? new DETaskSaveVisibleRowsAs(frame)
			 : codeMatches(taskCode, DETaskFindAndReplace.TASK_NAME) ? new DETaskFindAndReplace(frame)
			 : codeMatches(taskCode, DETaskSelectAll.TASK_NAME) ? new DETaskSelectAll(frame)
			 : codeMatches(taskCode, DETaskSelectDiverse.TASK_NAME) ? new DETaskSelectDiverse(frame)
			 : codeMatches(taskCode, DETaskSelectRowsFromList.TASK_NAME) ? new DETaskSelectRowsFromList(frame, -1)
			 : codeMatches(taskCode, DETaskSelectView.TASK_NAME) ? new DETaskSelectView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSelectWindow.TASK_NAME) ? new DETaskSelectWindow(frame, application, null)
			 : codeMatches(taskCode, DETaskSeparateCases.TASK_NAME) ? new DETaskSeparateCases(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetBackgroundImage.TASK_NAME) ? new DETaskSetBackgroundImage(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetCategoryCustomOrder.TASK_NAME) ? new DETaskSetCategoryCustomOrder(frame, frame.getTableModel(), -1)
			 : codeMatches(taskCode, DETaskSetColumnAlias.TASK_NAME) ? new DETaskSetColumnAlias(frame, frame.getTableModel())
			 : codeMatches(taskCode, DETaskSetColumnDataType.TASK_NAME) ? new DETaskSetColumnDataType(frame, frame.getTableModel())
			 : codeMatches(taskCode, DETaskSetColumnDescription.TASK_NAME) ? new DETaskSetColumnDescription(frame, frame.getTableModel())
			 : codeMatches(taskCode, DETaskSetColumnProperties.TASK_NAME) ? new DETaskSetColumnProperties(frame, frame.getTableModel())
			 : codeMatches(taskCode, DETaskSetConnectionLines.TASK_NAME) ? new DETaskSetConnectionLines(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetCurrentRow.TASK_NAME) ? new DETaskSetCurrentRow(frame)
			 : codeMatches(taskCode, DETaskSetFocus.TASK_NAME) ? new DETaskSetFocus(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetFontSize.TASK_NAME) ? new DETaskSetFontSize(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetGraphicalViewProperties.TASK_NAME) ? new DETaskSetGraphicalViewProperties(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetHorizontalStructureCount.TASK_NAME) ? new DETaskSetHorizontalStructureCount(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetLogarithmicMode.TASK_NAME) ? new DETaskSetLogarithmicMode(frame, frame.getTableModel(), null, false)
			 : codeMatches(taskCode, DETaskSetMarkerBackgroundColor.TASK_NAME) ? new DETaskSetMarkerBackgroundColor(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerColor.TASK_NAME) ? new DETaskSetMarkerColor(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerJittering.TASK_NAME) ? new DETaskSetMarkerJittering(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerLabels.TASK_NAME) ? new DETaskSetMarkerLabels(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerShape.TASK_NAME) ? new DETaskSetMarkerShape(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerSize.TASK_NAME) ? new DETaskSetMarkerSize(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMarkerTransparency.TASK_NAME) ? new DETaskSetMarkerTransparency(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetMessageMode.TASK_NAME) ? new DETaskSetMessageMode(frame)
			 : codeMatches(taskCode, DETaskSetMultiValueMarker.TASK_NAME) ? new DETaskSetMultiValueMarker(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetNumericalColumnDisplayMode.TASK_NAME) ? new DETaskSetNumericalColumnDisplayMode(frame, frame.getTableModel())
			 : codeMatches(taskCode, DETaskSetPreferredChartType.TASK_NAME) ? new DETaskSetPreferredChartType(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetRotation.TASK_NAME) ? new DETaskSetRotation(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetStatisticalViewOptions.TASK_NAME) ? new DETaskSetStatisticalViewOptions(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSetTextBackgroundColor.TASK_NAME) ? new DETaskSetTextBackgroundColor(frame, mainPane, null, -1)
			 : codeMatches(taskCode, DETaskSetStructureHiliteMode.TASK_NAME) ? new DETaskSetStructureHiliteMode(frame, frame.getTableModel())
			 : codeMatches(taskCode, DETaskSetTextColor.TASK_NAME) ? new DETaskSetTextColor(frame, mainPane, null, -1)
			 : codeMatches(taskCode, DETaskSetTextWrapping.TASK_NAME) ? new DETaskSetTextWrapping(frame, mainPane.getTableView())
			 : codeMatches(taskCode, DETaskSetValueRange.TASK_NAME) ? new DETaskSetValueRange(frame, -1)
			 : codeMatches(taskCode, DETaskShowTableColumns.TASK_NAME) ? new DETaskShowTableColumns(frame, mainPane.getTableView(), null)
			 : codeMatches(taskCode, DETaskShowMessage.TASK_NAME) ? new DETaskShowMessage(frame)
			 : codeMatches(taskCode, DETaskSortRows.TASK_NAME) ? new DETaskSortRows(frame, frame.getTableModel(), -1, false)
			 : codeMatches(taskCode, DETaskSortStructuresBySimilarity.TASK_NAME) ? new DETaskSortStructuresBySimilarity(frame, -1, null)
			 : codeMatches(taskCode, DETaskSplitView.TASK_NAME) ? new DETaskSplitView(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskSynchronizeView.TASK_NAME) ? new DETaskSynchronizeView(frame, mainPane, null, null)
			 : codeMatches(taskCode, DETaskUseAsFilter.TASK_NAME) ? new DETaskUseAsFilter(frame, mainPane, null)
			 : codeMatches(taskCode, DETaskWait.TASK_NAME) ? new DETaskWait(frame)
			 : createPluginTaskFromCode(frame, taskCode);
		}

	private AbstractTask createPluginTaskFromCode(DEFrame frame, String taskCode) {
		ArrayList<IPluginTask> pluginTaskList = frame.getApplication().getPluginRegistry().getPluginTasks();
		for (IPluginTask pluginTask:pluginTaskList)
			if (taskCode.equals(pluginTask.getTaskCode()))
				return new DETaskPluginTask(frame, pluginTask);

		return null;
		}

	public TreeSet<TaskSpecification> getTaskDictionary(DEFrame frame) {
		if (mTaskDictionary == null) {
			mTaskDictionary = new TreeSet<TaskSpecification>();
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAdd2DCoordinates.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAdd3DCoordinates.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddEmptyColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddEmptyRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAddCIPInfo.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAddFormula.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskCalculateFuzzyScore.TASK_NAME));
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
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskArrangeGraphNodes.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskAssignOrZoomAxes.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskAutomaticSAR.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCalculateChemicalProperties.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddCalculatedValues.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCalculateDescriptor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskCalculateSelectivityScore.TASK_NAME));
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
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATABASE, DETaskCODQuery.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskCombineTwoRowLists.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskCopy.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskCopyStatisticalValues.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskCopyView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DECoreBasedSAR.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskAddBinsFromNumbers.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCreateCombinatorialLibrary.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskCreateEvolutionaryLibrary.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskCreateListsFromCategories.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskDefineVariable.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskDeleteAllRowLists.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteInvisibleRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteDuplicateRows.TASK_NAME[0]));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteDuplicateRows.TASK_NAME[1]));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteDuplicateRows.TASK_NAME[2]));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskDeleteRowList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskDeleteSelectedRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskDeselectRowsFromList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskDisableAllFilters.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskDuplicateView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILTER, DETaskEnableAllFilters.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskExitProgram.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskExportHitlist.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskExportMacro.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskFindSimilarCompoundsInFile.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskHideTableColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskImportHitlist.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskImportMacro.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskInvertSelection.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskJumpToCurrentRow.TASK_NAME));
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
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATABASE, DETaskRetrieveDataFromURL.TASK_NAME));
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
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskFindAndReplace.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_EDIT, DETaskSelectAll.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskSelectDiverse.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_LIST, DETaskSelectRowsFromList.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSelectView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_FILE, DETaskSelectWindow.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSeparateCases.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetBackgroundImage.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetCategoryCustomOrder.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetColumnAlias.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetColumnDataType.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetColumnDescription.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetColumnProperties.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetConnectionLines.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskSetCurrentRow.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetFocus.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetFontSize.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetGraphicalViewProperties.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetHorizontalStructureCount.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetLogarithmicMode.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerBackgroundColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerJittering.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerLabels.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerShape.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerSize.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMarkerTransparency.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskSetMessageMode.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetMultiValueMarker.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetNumericalColumnDisplayMode.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetPreferredChartType.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetRotation.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSetStatisticalViewOptions.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetTextBackgroundColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetTextColor.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetTextWrapping.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSetValueRange.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskShowTableColumns.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskShowMessage.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATA, DETaskSortRows.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_CHEMISTRY, DETaskSortStructuresBySimilarity.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TABLE, DETaskSetStructureHiliteMode.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSplitView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskSynchronizeView.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_VIEW, DETaskUseAsFilter.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_MACRO, DETaskWait.TASK_NAME));

			ArrayList<IPluginTask> pluginTaskList = frame.getApplication().getPluginRegistry().getPluginTasks();
			for (IPluginTask pluginTask:pluginTaskList)
				mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_DATABASE, pluginTask.getTaskName()));

			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TEST, DETestCompareDescriptorSimilarityDistribution.TASK_NAME));
			mTaskDictionary.add(new TaskSpecification(TaskSpecification.CATEGORY_TEST, DETestExtractPairwiseCompoundSimilarities.TASK_NAME));
			}

		return mTaskDictionary;
		}

	/**
	 * Returns a unique task code to identify and distinguish this task
	 * from others within a task sequence file. Task codes are guaranteed
	 * to stay unchanged even if task names are changed for whatever reason.
	 * This method matches updated task names to original unchanged task codes.
	 * @return unique task name
	 */
	public String getTaskCodeFromName(String taskName) {
		if (taskName.equals(DETaskSetMarkerLabels.TASK_NAME))
			return "showLabels";
		if (taskName.equals(DETaskCalculateFuzzyScore.TASK_NAME))
			return "addFuzzyScore";
		if (taskName.equals(DETaskAddBinsFromNumbers.TASK_NAME))
			return "createBinsFromNumbers";
		if (taskName.equals(DETaskAddCalculatedValues.TASK_NAME))
			return "calculateNewColumn";
		if (taskName.equals(DETaskFindAndReplace.TASK_NAME))
			return "searchAndReplace";
		if (taskName.equals(DETaskDeleteDuplicateRows.TASK_NAME[DETaskDeleteDuplicateRows.MODE_REMOVE_DUPLICATE]))
			return "deleteRedundantRows";

		ArrayList<IPluginTask> pluginTaskList = mApplication.getPluginRegistry().getPluginTasks();
		for (IPluginTask pluginTask:pluginTaskList)
			if (taskName.equals(pluginTask.getTaskName()))
				return pluginTask.getTaskCode();

		return constructTaskCodeFromName(taskName);
		}

	private String constructTaskCodeFromName(String taskName) {
		return taskName.substring(0,1).toLowerCase() + taskName.substring(1).replaceAll("[^a-zA-Z0-9]", "");
		}

	protected boolean codeMatches(String taskCode, String taskName) {
		return taskCode.equals(getTaskCodeFromName(taskName));
		}
	}
