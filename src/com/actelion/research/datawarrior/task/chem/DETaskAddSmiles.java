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

import java.util.Properties;

import com.actelion.research.chem.SmilesCreator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;


public class DETaskAddSmiles extends DETaskAbstractAddChemProperty implements Runnable {
	public static final String TASK_NAME = "Add Smiles Codes";
    private static Properties sRecentConfiguration;

	public DETaskAddSmiles(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, true, true);
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
    protected int getNewColumnCount() {
		return 1;
		}

	@Override
    protected String getNewColumnName(int column) {
		return "Smiles";
		}

	@Override
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
    	return new SmilesCreator().generateSmiles(mol);
		}
	}
