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

import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.datawarrior.DEFrame;


public class DEMacroRecorder implements ProgressController,Runnable {
	public static final String RECORDING_MESSAGE = "Recording Macro...";
	private static volatile DEMacroRecorder sRecorder = null;

	private volatile boolean	mIsRecording;
	private volatile Thread		mMacroThread;
	private volatile DEFrame	mFrontFrame;
	private volatile DEMacro	mRunningMacro;
	private volatile StandardTaskFactory	mTaskFactory;
	private DEFrame				mRecordingMacroOwner,mFrontFrameWhenRecordingStopped;
	private DEMacro				mRecordingMacro;

	/**
	 * If the macro recorder is currently recording a macro, then this method records a the given task.
	 * This method can savely be called from any thread. It waits until the task is recorded.
	 * @param task
	 * @param configuration
	 */
	public static void record(final AbstractTask task, final Properties configuration) {
		if (sRecorder != null && sRecorder.mIsRecording) {
			if (SwingUtilities.isEventDispatchThread()) {
				sRecorder.recordTask(task, configuration);
				}
			else {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							sRecorder.recordTask(task, configuration);
							}
						} );
					}
				catch (Exception e) {}
				}
			}
		}

	public static DEMacroRecorder getInstance() {
		if (sRecorder == null)
			sRecorder = new DEMacroRecorder();

		return sRecorder;
		}

	private DEMacroRecorder() {
		}

	public void startRecording(DEMacro macro, DEFrame macroOwner) {
		mRecordingMacro = macro;
		mRecordingMacroOwner = macroOwner;
		mIsRecording = true;
		for (DEFrame frame:macroOwner.getApplication().getFrameList())
			frame.updateMacroStatus();
		}

	public void continueRecording() {
		mIsRecording = true;
		for (DEFrame frame:mRecordingMacroOwner.getApplication().getFrameList())
			frame.updateMacroStatus();
		}

	public boolean canContinueRecording(DEFrame frontFrame) {
		return !mIsRecording
			&& mRecordingMacro != null
			&& mFrontFrameWhenRecordingStopped == frontFrame;
		}

	public boolean isRecording() {
		return mIsRecording;
		}

	/**
	 * @return the owner of the currently recording macro or null
	 */
	public DEFrame getRecordingMacroOwner() {
		return mIsRecording ? mRecordingMacroOwner : null;
		}

	public void stopRecording() {
		mIsRecording = false;
		mFrontFrameWhenRecordingStopped = mRecordingMacroOwner.getApplication().getActiveFrame();
		for (DEFrame frame:mRecordingMacroOwner.getApplication().getFrameList())
			frame.updateMacroStatus();
		}

	public boolean isRecording(DEMacro macro) {
		return (mIsRecording && mRecordingMacro == macro);
		}

	public boolean isRunningMacro() {
		return (mMacroThread != null);
		}

	private void recordTask(AbstractTask task, Properties configuration) {
		String taskCode = task.getTaskCode();
		int previous = mRecordingMacro.getTaskCount()-1;
		if (previous != -1
		 && taskCode.equals(mRecordingMacro.getTaskCode(previous))
		 && task.isRedundant(mRecordingMacro.getTaskConfiguration(previous), configuration))
			mRecordingMacro.setTaskConfiguration(previous, configuration);
		else
			mRecordingMacro.addTask(taskCode, configuration);
		mRecordingMacroOwner.setDirty(true);
		}

	public void runMacro(DEMacro macro, DEFrame frontFrame) {
		if (!macro.isEmpty()) {
			if (mMacroThread == null) {
				mRunningMacro = macro;
				mFrontFrame = frontFrame;
				mTaskFactory = frontFrame.getApplication().getTaskFactory();
	
				for (DEFrame frame:frontFrame.getApplication().getFrameList())
					frame.getMainFrame().getMainPane().getMacroProgressPanel().initializeThreadMustDie();
		
				mMacroThread = new Thread(this, "DataWarriorMacro");
				mMacroThread.setPriority(Thread.MIN_PRIORITY);
				mMacroThread.start();
				}
			}
		}

	@Override
	public void run() {
		startProgress("Running Macro...", 0, 0);

		try {
			for (int i=0; i<mRunningMacro.getTaskCount(); i++) {
				if (threadMustDie())
					break;
	
				AbstractTask cf = mTaskFactory.createTask(mFrontFrame, mRunningMacro.getTaskCode(i));
				if (cf != null) {
		    		if (cf instanceof DETaskRepeatNextTask && i<mRunningMacro.getTaskCount()-1) {
		    			Properties config = mRunningMacro.getTaskConfiguration(i);
		    			int count = ((DETaskRepeatNextTask)cf).getRepetitions(config);
		    			int lastTask = ((DETaskRepeatNextTask)cf).repeatAllTasks(config) ? mRunningMacro.getTaskCount()-1 : i+1;
		    			mRunningMacro.defineLoop(i+1, lastTask, count);
		    			continue;
	    				}
	
		    		// if the task is a macro itself, then spawn a daughter macro
		    		if (cf instanceof GenericTaskRunMacro) {
						DEMacro daughterMacro = ((GenericTaskRunMacro)cf).getMacro(mRunningMacro.getTaskConfiguration(i));
						if (daughterMacro == null)
							continue;	// just skip the internal macro
	
						daughterMacro.setParentMacro(mRunningMacro, i);
						mRunningMacro = daughterMacro;
						i = -1;
						continue;	// start with the first task of the daughter macro
						}
	
					cf.execute(mRunningMacro.getTaskConfiguration(i), this);
					if (cf.getNewFrontFrame() != null) {
						mFrontFrame = cf.getNewFrontFrame();
						try {
		                    SwingUtilities.invokeAndWait(new Runnable() {
		                    	@Override
		                    	public void run() {
		        					mFrontFrame.toFront();
		                    		}
		                    	});
							}
	                    catch (Exception e) {}
						}
					}
	
				// if we have finished a daughter macro, then continue with the parent one
				if (i == mRunningMacro.getTaskCount()-1) {
					if (mRunningMacro.getParentMacro() != null) {
						i = mRunningMacro.getParentIndex();
						mRunningMacro = mRunningMacro.getParentMacro();
						}
					}
	
				int gotoIndex = mRunningMacro.getLoopStart(i);
				if (gotoIndex != -1)
					i = gotoIndex-1;
				}
			}
		catch (final Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, e.toString(), "Unexpected Error", JOptionPane.ERROR_MESSAGE);
					}
				});
			}

		stopProgress();

		mMacroThread = null;
		mRunningMacro = null;
		}

	@Override
	public void startProgress(final String text, final int min, final int max) {
		if (SwingUtilities.isEventDispatchThread()) {
			for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
				frame.getMainFrame().getMainPane().getMacroProgressPanel().startProgress(text, min, max);
			}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
						frame.getMainFrame().getMainPane().getMacroProgressPanel().startProgress(text, min, max);
					}
				});
			}
		}

	@Override
	public void updateProgress(final int value) {
		if (SwingUtilities.isEventDispatchThread()) {
			for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
				frame.getMainFrame().getMainPane().getMacroProgressPanel().updateProgress(value);
			}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
						frame.getMainFrame().getMainPane().getMacroProgressPanel().updateProgress(value);
					}
				});
			}
		}

	@Override
	public void stopProgress() {
		if (SwingUtilities.isEventDispatchThread()) {
			for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
				frame.getMainFrame().getMainPane().getMacroProgressPanel().stopProgress();
			}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
						frame.getMainFrame().getMainPane().getMacroProgressPanel().stopProgress();
					}
				});
			}
		}

	@Override
	public void showErrorMessage(final String message) {
		if (SwingUtilities.isEventDispatchThread()) {
			mFrontFrame.getApplication().getActiveFrame().getMainFrame().getMainPane().getMacroProgressPanel().showErrorMessage(message);
			}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					mFrontFrame.getApplication().getActiveFrame().getMainFrame().getMainPane().getMacroProgressPanel().showErrorMessage(message);
					}
				});
			}
		}

	@Override
	public boolean threadMustDie() {
		for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
			if (frame.getMainFrame().getMainPane().getMacroProgressPanel().threadMustDie())
				return true;
		return false;
		}
	}
