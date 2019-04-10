package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskJumpToCurrentRow extends AbstractViewTask {
	public static final String TASK_NAME = "Jump To Current Row";

	private DETable mTable;

	public DETaskJumpToCurrentRow(Frame parent, DEMainPane mainPane) {
		super(parent, mainPane, mainPane.getTableView());
		mTable = mainPane.getTableView().getTable();
		}

	@Override
	public boolean isConfigurable() {
		return ((CompoundTableModel)mTable.getModel()).getActiveRow() != null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return true;
	}

	@Override
	public JComponent createInnerDialogContent() {
		return null;
	}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof DETableView) ? null : "'Jump to current row' needs a table view.";
		}

	@Override
	public void runTask(Properties configuration) {
		int row = ((CompoundTableModel)mTable.getModel()).getActiveRowIndex();
		if (row != -1)
			mTable.scrollRectToVisible(new Rectangle(0, row * mTable.getRowHeight(), mTable.getWidth(), mTable.getRowHeight()));
		return;
		}
	}
