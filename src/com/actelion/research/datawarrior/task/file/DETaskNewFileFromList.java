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

package com.actelion.research.datawarrior.task.file;

import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.list.DETaskAbstractListTask;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;


public class DETaskNewFileFromList extends DETaskAbstractListTask {
	public static final String TASK_NAME = "New File From List";

	private static Properties sRecentConfiguration;

	private DEFrame		mSourceFrame,mTargetFrame;
	private DataWarrior	mApplication;

	/**
	 * The listIndex parameter may be used to override the configuration's list name.
	 * If listIndex is preconfigured (i.e. != -1) and defineAndRun() is called, then
	 * this task will immediately run without showing a configuration dialog.
	 * @param parent
	 * @param listIndex -1 or valid list index
	 */
	public DETaskNewFileFromList(DEFrame sourceFrame, DataWarrior application, int listIndex) {
		super(sourceFrame, listIndex);
		mSourceFrame = sourceFrame;
		mApplication = application;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
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
	public void runTask(Properties configuration) {
		DERuntimeProperties rp = new DERuntimeProperties(mSourceFrame.getMainFrame());
		rp.learn();

		CompoundTableModel sourceTableModel = mSourceFrame.getTableModel();
		CompoundTableHitlistHandler sourceHitlistHandler = sourceTableModel.getHitlistHandler();

	   	boolean[] hitlistUsed = new boolean[sourceHitlistHandler.getHitlistCount()];
	   	long hitlistMask[] = null;
	   	if (hitlistUsed.length != 0) {
	   		hitlistMask = new long[hitlistUsed.length];
	   		for (int i=0; i<hitlistUsed.length; i++)
	   			hitlistMask[i] = sourceHitlistHandler.getHitlistMask(i);
	   		}

		long mask = getTableModel().getHitlistHandler().getHitlistMask(getListIndex(configuration));

		int listMemberCount = 0;
		for (int row=0; row<sourceTableModel.getTotalRowCount(); row++) {
	   		CompoundRecord record = sourceTableModel.getTotalRecord(row);
			if ((record.getFlags() & mask) != 0) {
				listMemberCount++;
		   		for (int i=0; i<hitlistUsed.length; i++)
		   			if ((record.getFlags() & hitlistMask[i]) != 0)
		   				hitlistUsed[i] = true;
				}
			}

		mTargetFrame = mApplication.getEmptyFrame("Selection of "+mSourceFrame.getTitle());
		CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
		CompoundTableHitlistHandler targetHitlistHandler = targetTableModel.getHitlistHandler();
		targetTableModel.initializeTable(listMemberCount, sourceTableModel.getTotalColumnCount());
		for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++)
			targetTableModel.setColumnName(sourceTableModel.getColumnTitleNoAlias(column), column);

		for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++) {
			HashMap<String,String> properties = sourceTableModel.getColumnProperties(column);
			for (String key:properties.keySet())
				targetTableModel.setColumnProperty(column, key, properties.get(key));
			}

		TreeSet<String> detaiIDSet = new TreeSet<String>();
	   	for (int targetRow=0,row=0; row<sourceTableModel.getTotalRowCount(); row++) {
	   		CompoundRecord record = sourceTableModel.getTotalRecord(row);
			if ((record.getFlags() & mask) != 0) {
 				for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++) {
					targetTableModel.setTotalValueAt(sourceTableModel.encodeDataWithDetail(record, column), targetRow, column);
					String[][] key = record.getDetailReferences(column);
					if (key != null)
   						for (int detailIndex=0; detailIndex<key.length; detailIndex++)
   							if (key[detailIndex] != null)
   								for (int i=0; i<key[detailIndex].length; i++)
   									detaiIDSet.add(key[detailIndex][i]);
 					}
 				targetRow++;
				}
			}

		targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

		for (int i=0; i<hitlistUsed.length; i++) {
			if (hitlistUsed[i]) {
				int flagNo = targetHitlistHandler.getHitlistFlagNo(targetHitlistHandler.createHitlist(
						sourceHitlistHandler.getHitlistName(i), -1, CompoundTableHitlistHandler.EMPTY_LIST, -1, null));
			   	int tRow = 0;
				for (int row=0; row<sourceTableModel.getTotalRowCount(); row++) {
			   		CompoundRecord record = sourceTableModel.getTotalRecord(row);
					if ((record.getFlags() & mask) != 0) {
						if ((record.getFlags() & hitlistMask[i]) != 0)
							targetHitlistHandler.addRecordSilent(targetTableModel.getTotalRecord(tRow), flagNo);
						tRow++;
						}
			   		}
				}
			}

		HashMap<String,byte[]> detailMap = sourceTableModel.getDetailHandler().getEmbeddedDetailMap();
		if (detailMap != null)
			for (String detailID:detaiIDSet)
				targetTableModel.getDetailHandler().setEmbeddedDetail(detailID, detailMap.get(detailID));

		rp.setParentPane(mTargetFrame.getMainFrame());
		rp.apply();
		}
	}
