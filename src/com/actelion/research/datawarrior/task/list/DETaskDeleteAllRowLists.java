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

package com.actelion.research.datawarrior.task.list;

import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.DETaskWithEmptyConfiguration;
import com.actelion.research.table.CompoundTableHitlistHandler;

public class DETaskDeleteAllRowLists extends DETaskWithEmptyConfiguration {
    public static final String TASK_NAME = "Delete All Row Lists";

    DEFrame mParentFrame;

    public DETaskDeleteAllRowLists(DEFrame parent) {
		super(parent, false);
		mParentFrame = parent;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		CompoundTableHitlistHandler hh = mParentFrame.getMainFrame().getTableModel().getHitlistHandler();
		String[] names = hh.getHitlistNames();
		if (names != null)
			for (int i=0; i<names.length; i++)
				hh.deleteHitlist(names[i]);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
