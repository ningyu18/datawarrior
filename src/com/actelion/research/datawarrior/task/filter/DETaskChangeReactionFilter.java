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

package com.actelion.research.datawarrior.task.filter;

import java.awt.Frame;
import java.util.Properties;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.filter.JReactionFilterPanel;
import com.actelion.research.table.filter.JSingleStructureFilterPanel;

public class DETaskChangeReactionFilter extends DEAbstractFilterTask {
	public static final String TASK_NAME = "Change Reaction Filter";

	public DETaskChangeReactionFilter(Frame parent, DEPruningPanel pruningPanel, JFilterPanel filter) {
		super(parent, pruningPanel, filter);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public JFilterPanel createFilterUI() {
		return new JReactionFilterPanel(getParentFrame(), getTableModel(), null);
	}

	@Override
	protected String getColumnQualificationError(int column) {
		return getTableModel().isColumnTypeReaction(column) ? null
				: "Reaction filters can be applied to reaction columns only.";
	}

	@Override
	public int getFilterType() {
		return JFilterPanel.FILTER_TYPE_REACTION;
	}
}
