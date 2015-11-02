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

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.util.Platform;


public abstract class DETaskAbstractOpenFile extends ConfigurableTask implements ActionListener {
	public static final String[] RESOURCE_DIR = { "Reference", "Example", "Tutorial" };
	public static final String MACRO_DIR = "Macro";

	protected static final String PROPERTY_FILENAME = "fileName";
	protected static final String ASK_FOR_FILE = "#ask#";
	protected static final int MDL_REACTIONS = -1;

	private JFilePathLabel	mFilePathLabel;
	private JButton			mButtonEdit;
	private JCheckBox		mCheckBoxInteractive;
	private int				mAllowedFileTypes;
	private String			mDialogTitle;
	private String			mPredefinedFilePath;
	private DEFrame			mNewFrame;

	/**
	 * Tries to find the directory with the specified name in the DataWarrior installation directory.
	 * resourceDir may contain capital letters, but is is converted to lower case before checked against
	 * installed resource directories, which are supposed to be lower case.
	 * @param resourceDir as shown to the user, e.g. "Example"
	 * @return null or full path to resource directory
	 */
	public static File resolveResourcePath(String resourceDir) {
		String dirname = "C:\\Program Files\\DataWarrior\\"+resourceDir.toLowerCase();
		File directory = new File(dirname);
		if (!directory.exists()) {
			dirname = "C:\\Program Files (x86)\\DataWarrior\\"+resourceDir.toLowerCase();
			directory = new File(dirname);
			}
		if (!directory.exists()) {
			dirname = "/Applications/DataWarrior.app/"+resourceDir.toLowerCase();
			directory = new File(dirname);
			}
		if (!directory.exists()) {
			dirname = "/opt/datawarrior/"+resourceDir.toLowerCase();
			directory = new File(dirname);
			}
		if (!directory.exists()) {
			dirname = "\\\\actelch02\\pgm\\Datawarrior\\"+resourceDir.toLowerCase();
			directory = new File(dirname);
			}
		if (!directory.exists()) {
			dirname = "/mnt/rim/Datawarrior/"+resourceDir.toLowerCase();
			directory = new File(dirname);
			}
		return directory.exists() ? directory : null;
		}

	/**
	 * Creates a path variable name from a resource directory name.
	 * @param resourceDir as shown to the user, e.g. "Example"
	 * @return path variable name, e.g. $EXAMPLE
	 */
	public static String makePathVariable(String resourceDir) {
		return "$"+resourceDir.toUpperCase();
		}

	/**
	 * If the given path starts with a valid variable name, then this
	 * is replaced by the corresponding path on the current system and all file separators
	 * are converted to the correct ones for the current platform.
	 * Valid variable names are $HOME or resource file names.
	 * @param path possibly starting with variable, e.g. "$EXAMPLE/drugs.dwar"
	 * @return untouched path or path with resolved variable, e.g. "/opt/datawarrior/example/drugs.dwar"
	 */
	@Override
	public String resolveVariables(String path) {
		path = super.resolveVariables(path);
		if (path != null && path.startsWith("$")) {
			for (String dirName:RESOURCE_DIR) {
				String varName = makePathVariable(dirName);
				if (path.startsWith(varName)) {
					File dir = resolveResourcePath(dirName);
					if (dir != null) {
						return dir.getAbsolutePath().concat(DataWarrior.correctFileSeparators(path.substring(varName.length())));
						}
					}
				}
			String varName = makePathVariable(MACRO_DIR);
			if (path.startsWith(varName)) {
				File dir = resolveResourcePath(MACRO_DIR);
				if (dir != null)
					return dir.getAbsolutePath().concat(DataWarrior.correctFileSeparators(path.substring(varName.length())));
				}
			}
		return path;
		}

	/**
	 * Creates an open-file task which only shows a configuration dialog, if the task
	 * is not invoked interactively. Otherwise a file chooser is shown to directly select
	 * the file to be opened.
	 * @param parent
	 * @param dialogTitle
	 * @param allowedFileTypes
	 * @param isInteractive
	 */
	public DETaskAbstractOpenFile(Frame parent, String dialogTitle, int allowedFileTypes) {
		super(parent, true);	// we want a progress bar
//		super(parent, !isInteractive);	// non-interactive tasks use own thread
		mDialogTitle = dialogTitle;
		mAllowedFileTypes = allowedFileTypes;
		mPredefinedFilePath = null;
		}

	/**
	 * Creates an open-file task with a file as parameter. This constructor is used when
	 * the user interactively chooses to open a specific file without file dialog.
	 * @param parent
	 * @param dialogTitle
	 * @param allowedFileTypes
	 * @param file
	 */
	public DETaskAbstractOpenFile(Frame parent, String dialogTitle, int allowedFileTypes, String filePath) {
		super(parent, true);	// we want a progress bar
//		super(parent, false);
		mDialogTitle = dialogTitle;
		mAllowedFileTypes = allowedFileTypes;
		mPredefinedFilePath = filePath;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			String fileName = mPredefinedFilePath;
			if (fileName == null) {
				File file = askForFile(null);
				if (file != null)
					fileName = file.getAbsolutePath();
				}

			Properties configuration = new Properties();
			if (fileName != null)
				configuration.setProperty(PROPERTY_FILENAME, fileName);
			return configuration;
			}

		return null;	// show a configuration dialog
		}

	@Override
	public boolean isPredefinedStatusOK(Properties configuration) {
		return configuration.getProperty(PROPERTY_FILENAME) != null;	// a null indicates that the file dialog was cancelled
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mNewFrame;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, TableLayout.FILL, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8, TableLayout.PREFERRED } };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mFilePathLabel = new JFilePathLabel(!isInteractive());
		content.add(mFilePathLabel, "1,1,2,1");

		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		content.add(mButtonEdit, "1,3");

		mCheckBoxInteractive = new JCheckBox("Choose file during macro execution");
		mCheckBoxInteractive.addActionListener(this);
		content.add(mCheckBoxInteractive, "1,5,2,5");

		JPanel moreOptions = createInnerDialogContent();
		if (moreOptions != null)
			content.add(moreOptions, "1,7,2,7");
		
		return content;
		}

	/**
	 * Override this if your subclass needs more dialog options.
	 * There should not be any border except for an 8 pixel spacing at the bottom.
	 * @return
	 */
	public JPanel createInnerDialogContent() {
		return null;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		if (ASK_FOR_FILE.equals(fileName))
			return true;
		if (isLive && !isFileAndPathValid(resolveVariables(fileName), false, false))
			return false;
		if ((FileHelper.getFileType(fileName) & mAllowedFileTypes) == 0) {
			showErrorMessage("Incompatible file type.");
			return false;
			}
		return true;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mFilePathLabel.setPath(null);
		mCheckBoxInteractive.setSelected(true);
		enableItems();
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		mFilePathLabel.setPath(fileName.equals(ASK_FOR_FILE) ? null : fileName);
		mCheckBoxInteractive.setSelected(fileName.equals(ASK_FOR_FILE));
		enableItems();
		}

	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mCheckBoxInteractive.isSelected()) {
			configuration.setProperty(PROPERTY_FILENAME, ASK_FOR_FILE);
			}
		else {
			String fileName = mFilePathLabel.getPath();
			if (fileName != null)
				configuration.setProperty(PROPERTY_FILENAME, fileName);
			}

		return configuration;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			File file = askForFile(resolveVariables(mFilePathLabel.getPath()));
			if (file != null) {
				mFilePathLabel.setPath(file.getAbsolutePath());
				fileChanged(file);
				}
			enableItems();
			return;
			}
		if (e.getSource() == mCheckBoxInteractive) {
			enableItems();
			return;
			}
		}

	private void enableItems() {
		mButtonEdit.setEnabled(!mCheckBoxInteractive.isSelected());
		mFilePathLabel.setEnabled(!mCheckBoxInteractive.isSelected());
		setOKButtonEnabled(mCheckBoxInteractive.isSelected() || mFilePathLabel.getPath() != null);
		}

	/**
	 * Override this, if additional user interface elements need to be updated from file content
	 * @param file
	 */
	protected void fileChanged(File file) {
		}

	protected File askForFile(String selectedFile) {
		return new FileHelper(getParentFrame()).selectFileToOpen(mDialogTitle, mAllowedFileTypes, selectedFile);
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);

		if (isInteractive() && ASK_FOR_FILE.equals(fileName))
			return;	// Is interactive and was cancelled. Don't create an error message.

		File file = ASK_FOR_FILE.equals(fileName) ? askForFile(null) : new File(resolveVariables(fileName));
		if (file == null) {
			showErrorMessage("No file was chosen.");
			return;
			}

		mNewFrame = openFile(file, configuration);
		}

	public abstract DEFrame openFile(File file, Properties configuration);
	}
