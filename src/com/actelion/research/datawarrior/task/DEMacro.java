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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.io.BOMSkipper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

public class DEMacro implements CompoundTableConstants {
	private static final String MACRO_START = "<macro name=\"";
	private static final String MACRO_END = "</macro";
	private static final String TASK_START = "<task name=\"";
	private static final String TASK_END = "</task";
	private static final String AUTOSTART = " auto-start=\"true\"";
	private String			mName;
	private ArrayList<Task> mTaskList;
	private ArrayList<DEMacroListener> mListenerList;
	private ArrayList<Loop> mLoopList;
	private DEMacro			mParentMacro;
	private int				mParentIndex;
	private boolean			mIsAutoStarting;

	public static String extractMacroName(String headerLine) {
		if (!headerLine.startsWith(MACRO_START))
			return null;

		int index = headerLine.indexOf('"', MACRO_START.length());
		if (index == -1)
			return null;

		return headerLine.substring(MACRO_START.length(), index);
		}

	public static boolean isAutoStarting(String headerLine) {
		return headerLine.startsWith(MACRO_START) && headerLine.indexOf(AUTOSTART) != -1;
		}

	/**
	 * Creates an empty DEMacro with the given name.
	 * If macroList is given than the name is may be slightly adapted to make it unique.
	 * @param name
	 * @param macroList
	 */
	public DEMacro(String name, ArrayList<DEMacro> macroList) {
		mTaskList = new ArrayList<Task>();
		mListenerList = new ArrayList<DEMacroListener>();
		mName = getUniqueName(name, macroList);
		}

	/**
	 * Creates a new DEMacro as exact copy of the specified sourceMacro.
	 * If macroList is given than the name is may be slightly adapted to make it unique.
	 * @param name
	 * @param macroList
	 * @param sourceMacro macro to be cloned
	 */
	public DEMacro(String name, ArrayList<DEMacro> macroList, DEMacro sourceMacro) {
		mTaskList = new ArrayList<Task>();
		mListenerList = new ArrayList<DEMacroListener>();
		mName = getUniqueName(name, macroList);
		if (sourceMacro != null) {
			for (int i=0; i<sourceMacro.getTaskCount(); i++) {
				Task sourceTask = sourceMacro.getTask(i);
				mTaskList.add(new Task(sourceTask.getCode(), new Properties(sourceTask.getConfiguration())));
				}
			}
		}

	/**
	 * Creates a new DEMacro from a datawarrior macro file (.dwam).
	 * @param file
	 * @throws IOException
	 */
	public DEMacro(File file, ArrayList<DEMacro> macroList) throws IOException {
		mTaskList = new ArrayList<Task>();
		mListenerList = new ArrayList<DEMacroListener>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		BOMSkipper.skip(reader);
		String headerLine = reader.readLine();
		mName = extractMacroName(headerLine);
		if (mName != null) {
			mName = getUniqueName(mName, macroList);
			mIsAutoStarting = isAutoStarting(headerLine);
			readMacro(reader);
			}
		reader.close();
		}

	public DEMacro(String name, BufferedReader reader) {
		mTaskList = new ArrayList<Task>();
		mListenerList = new ArrayList<DEMacroListener>();
		mName = name;
		try {
			readMacro(reader);
			}
		catch (IOException ioe) {}
		}

	private String getUniqueName(String name, ArrayList<DEMacro> macroList) {
		if (macroList == null)
			return name;

		while (true) {
			boolean nameIsUnique = true;
			for (DEMacro macro:macroList) {
				if (name.equals(macro.getName())) {
					nameIsUnique = false;
					break;
					}
				}

			if (nameIsUnique)
				return name;

			int index = name.lastIndexOf(' ');
			try {
				int number = Integer.parseInt(name.substring(index+1));
				name = name.substring(0, index+1) + (number+1);
				}
			catch (NumberFormatException nfe) {
				name = name + " 2";
				}
			}
		}

	public void addMacroListener(DEMacroListener l) {
		mListenerList.add(l);
		}

	public void removeMacroListener(DEMacroListener l) {
		mListenerList.remove(l);
		}

	private void fireContentChanged() {
		for (DEMacroListener l:mListenerList)
			l.macroContentChanged(this);
		}

	private void fireNameChanged() {
		for (DEMacroListener l:mListenerList)
			l.macroNameChanged(this);
		}

	public String getName() {
		return mName;
		}

	public void setName(String name, ArrayList<DEMacro> macroList) {
		if (!mName.equals(name)) {
			mName = getUniqueName(name, macroList);
			fireNameChanged();
			}
		}

	public boolean isEmpty() {
		return mTaskList.size() == 0;
		}

	public void clear() {
		mTaskList.clear();
		fireContentChanged();
		}

	public void addTask(String taskCode, Properties configuration) {
		mTaskList.add(new Task(taskCode, configuration));
		fireContentChanged();
		}

	public void removeTask(int index) {
		mTaskList.remove(index);
		fireContentChanged();
		}

	public Task getTask(int index) {
		return mTaskList.get(index);
		}

	public int getTaskCount() {
		return mTaskList.size();
		}

	public String getTaskCode(int index) {
		return mTaskList.get(index).getCode();
		}

	public Properties getTaskConfiguration(int index) {
		return mTaskList.get(index).getConfiguration();
		}

	public void setTaskConfiguration(int index, Properties configuration) {
		mTaskList.get(index).setConfiguration(configuration);
		}

	/**
	 * Updates the order of old task according to index list.
	 * May introduce a new task, if the associated old index is -1.
	 * @param oldIndex
	 * @return
	 */
	public ArrayList<Task> changeTaskOrder(int[] oldIndex) {
		ArrayList<Task> oldTaskList = mTaskList;
		mTaskList = new ArrayList<Task>();
		for (int i:oldIndex) {
			if (i == -1)
				mTaskList.add(new Task());
			else
				mTaskList.add(oldTaskList.get(i));
			}
		return mTaskList;
		}

	public void writeMacro(BufferedWriter writer) throws IOException {
		writer.write(MACRO_START+mName+"\"");
		if (mIsAutoStarting)
			writer.write(AUTOSTART);
		writer.write(">");
		writer.newLine();
		for (Task task:mTaskList) {
			writer.write(TASK_START+task.getCode()+"\">");
			writer.newLine();
			if (task.configuration != null) {
				for (String key:task.configuration.stringPropertyNames()) {
					writer.write(key+"="+encode(task.configuration.getProperty(key)));
					writer.newLine();
					}
				}
			writer.write(TASK_END+">");
			writer.newLine();
			}
		writer.write(MACRO_END+">");
		writer.newLine();
		}

	private String decode(String s) {
		return s.replace(NEWLINE_STRING, "\n");
		}

	private String encode(String s) {
		return s.replaceAll(NEWLINE_REGEX, NEWLINE_STRING);
		}

	public boolean isAutoStarting() {
		return mIsAutoStarting;
		}

	public void setAutoStarting(boolean b) {
		mIsAutoStarting = b;
		}

	public void readMacro(BufferedReader reader) throws IOException {
		mTaskList.clear();
		String theLine = reader.readLine();
		while (theLine != null && theLine.startsWith(TASK_START)) {
			String taskCode = theLine.substring(12, theLine.indexOf('\"', 12));

			Properties configuration = new Properties();
			theLine = reader.readLine();
			while (theLine != null && !theLine.startsWith(TASK_END)) {
				int index = theLine.indexOf("=");
				if (index != -1)
					configuration.setProperty(theLine.substring(0, index), decode(theLine.substring(index+1)));
				theLine = reader.readLine();
				}

			mTaskList.add(new Task(taskCode, configuration));
			theLine = reader.readLine();
			if (theLine == null || theLine.startsWith(MACRO_END))
				break;
			}
		fireContentChanged();
		}

	public DEMacro getParentMacro() {
		return mParentMacro;
		}

	public int getParentIndex() {
		return mParentIndex;
		}

	public void setParentMacro(DEMacro parentMacro, int parentIndex) {
		mParentMacro = parentMacro;
		mParentIndex = parentIndex;
		}

	/**
	 * Defines a sequence of tasks to be repeated for a couple of times.
	 * @param firstTask
	 * @param lastTask
	 * @param count how many times the task sequence should be run
	 */
	public void defineLoop(int firstTask, int lastTask, int count) {
		if (mLoopList == null)
			mLoopList = new ArrayList<Loop>();
		mLoopList.add(new Loop(firstTask, lastTask, count-1));
		}

	/**
	 * If currentTask refers to the last task of an active loop, then the index
	 * of the first task of that loop is returned and the loop counter decremented.
	 * @param currentTask
	 * @return the index of the loop's first task or -1, if not at the end of a loop
	 */
	public int getLoopStart(int currentTask) {
		if (mLoopList != null && mLoopList.size() != 0) {
			int loop = mLoopList.size()-1;
			Loop currentLoop = mLoopList.get(loop);
			if (currentTask == currentLoop.lastTask) {
				int firstTask = currentLoop.firstTask;
				if (--currentLoop.count == 0)
					mLoopList.remove(loop);
				return firstTask;
				}
			}
		return -1;
		}

	public class Task {
		private String code;
		private Properties configuration;

		public Task() {
			this(null, null);
			}

		public Task(String code, Properties configuration) {
			this.code = code;
			this.configuration = configuration;
			}

		public String getCode() {
			return code;
			}

		public Properties getConfiguration() {
			return configuration;
			}

		public void setCode(String code) {
			this.code = code;
			}

		public void setConfiguration(Properties configuration) {
			this.configuration = configuration;
			}
		}

	private class Loop {
		private int firstTask,lastTask,count;

		public Loop(int firstTask, int lastTask, int count) {
			this.firstTask = firstTask;
			this.lastTask = lastTask;
			this.count = count;
			}
		}
	}
