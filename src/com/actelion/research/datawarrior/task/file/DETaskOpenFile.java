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

package com.actelion.research.datawarrior.task.file;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableLoader;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskOpenFile extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Open File";

    private DataWarrior mApplication;

    public DETaskOpenFile(DataWarrior application) {
		super(application, "Open DataWarrior-, SD- or Text-File",
				FileHelper.cFileTypeDataWarriorCompatibleData);
		mApplication = application;
		}

    public DETaskOpenFile(DataWarrior application, String filePath) {
		super(application, "Open DataWarrior-, SD- or Text-File",
				FileHelper.cFileTypeDataWarriorCompatibleData, filePath);
		mApplication = application;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		final int filetype = FileHelper.getFileType(file.getName());
		final DEFrame emptyFrame = mApplication.getEmptyFrame(file.getName());
		CompoundTableLoader loader = new CompoundTableLoader(emptyFrame, emptyFrame.getTableModel(), this) {
			public void finalStatus(boolean success) {
				if (success && filetype == FileHelper.cFileTypeDataWarrior) {
					emptyFrame.setDirty(false);
					SwingUtilities.invokeLater(() -> runAutoStartMacros(emptyFrame));
					}
				}
			};
		loader.addDataDependentPropertyReader(CustomLabelPositionWriter.PROPERTY_NAME, new CustomLabelPositionReader(emptyFrame));
		loader.addDataDependentPropertyReader(CardViewPositionWriter.PROPERTY_NAME, new CardViewPositionReader(emptyFrame));
		loader.readFile(file, new DERuntimeProperties(emptyFrame.getMainFrame()), filetype);
		return emptyFrame;
		}

	private void runAutoStartMacros(DEFrame frame) {
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)frame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList != null) {
			for (DEMacro macro:macroList) {
				if (macro.isAutoStarting()) {
					DEMacroRecorder.getInstance().runMacro(macro, frame);
					}
				}
			}
		}
	}
