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

package com.actelion.research.table.filter;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.gui.hidpi.HiDPIToggleButton;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListener;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public abstract class JFilterPanel extends JPanel
		implements ActionListener,CompoundTableListener {
	private static final long serialVersionUID = 0x20110325;

	public static final int FILTER_TYPE_TEXT = 0;
	public static final int FILTER_TYPE_DOUBLE = 1;
	public static final int FILTER_TYPE_CATEGORY = 2;
	public static final int FILTER_TYPE_STRUCTURE = 3;
	public static final int FILTER_TYPE_SSS_LIST = 4;
	public static final int FILTER_TYPE_SIM_LIST = 5;
	public static final int FILTER_TYPE_REACTION = 6;
	public static final int FILTER_TYPE_ROWLIST = 7;
	public static final int FILTER_TYPE_CATEGORY_BROWSER = 8;

	public static final int PSEUDO_COLUMN_ROW_LIST = -4;
	public static final int PSEUDO_COLUMN_ALL_COLUMNS = -5;	// currently only used by JTextFilterPanel

	public static final String ALL_COLUMN_TEXT = "<All Columns>";
	public static final String ALL_COLUMN_CODE = "#allColumns#";

	static private final String INVERSE_CODE = "#inverse#";
	static private final String DISABLED_CODE = "#disabled#";

	protected CompoundTableModel	mTableModel;
	protected int					mColumnIndex,mExclusionFlag;
	protected boolean				mIsUserChange;	// Is set from derived classes once the UI is complete
													// and is temporarily disabled, whenever a programmatic change occurs.
	private JLabel					mColumnNameLabel;
	private JPanel					mTitlePanel;
	private HiDPIIconButton		    mAnimationButton;
	private HiDPIToggleButton		mButtonInverse,mButtonDisabled;
	private JPopupMenu				mAnimationPopup;
	private boolean					mIsActive,mIsInverse,mSuppressErrorMessages;
	private Animator				mAnimator;
	private JTextField				mTextFieldFrameDelay;
	private JDialog					mOptionsDialog;
	private ArrayList<FilterListener> mListenerList;

	/**
	 * @param tableModel
	 * @param column
	 * @param exclusionFlag if -1 then this filter is dead, i.e. it doesn't influence row visibility
	 * @param showAnimationOptions
	 */
	public JFilterPanel(CompoundTableModel tableModel, int column, int exclusionFlag, boolean showAnimationOptions) {
		mExclusionFlag = exclusionFlag;
		mIsActive = (exclusionFlag != -1);
		mTableModel = tableModel;
		mColumnIndex = column;

		setOpaque(false);
		setLayout(new BorderLayout());

		mTitlePanel = new JPanel();
		double[][] size = { {4, TableLayout.FILL, 4, TableLayout.PREFERRED, TableLayout.PREFERRED, 8, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 4} };
		mTitlePanel.setLayout(new TableLayout(size));
		mTitlePanel.setOpaque(false);
		mColumnNameLabel = new JLabel(getTitle()) {
			private static final long serialVersionUID = 0x20080128;
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = Math.min(size.width, HiDPIHelper.scale(72));
				return size;
				}
			};
		mColumnNameLabel.setMaximumSize(new Dimension(HiDPIHelper.scale(50),mColumnNameLabel.getMaximumSize().height));
		mTitlePanel.add(mColumnNameLabel, "1,0");

		JPanel lbp = new JPanel();
		mButtonInverse = new HiDPIToggleButton("yy16.png", "iyy16.png", "Invert filter", "inverse");
		mButtonInverse.addActionListener(this);
		lbp.add(mButtonInverse);
		mButtonDisabled = new HiDPIToggleButton("disabled16.png", "idisabled16.png", "Disable filter", "disable");
		mButtonDisabled.addActionListener(this);
		lbp.add(mButtonDisabled);
		mTitlePanel.add(lbp, "4,0");

		if (isActive()) {
			JPanel rbp = new JPanel();
			rbp.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 5));  // 5 is the default vgap matching that of lbp
			JButton cb = new HiDPIIconButton("closeButton.png", null, "close", 0, "square");
			cb.addActionListener(this);
			rbp.add(cb);
			mTitlePanel.add(rbp, "6,0");
			}

		add(mTitlePanel, BorderLayout.NORTH);

		if (showAnimationOptions) {
			mAnimator = new Animator(500);
			showAnimationControls();
			}
		}

	public String getTitle() {
		return (mColumnIndex == PSEUDO_COLUMN_ROW_LIST) ? "List membership"
			 : (mColumnIndex == PSEUDO_COLUMN_ALL_COLUMNS) ? ALL_COLUMN_TEXT
			 : (mColumnIndex >= 0) ? mTableModel.getColumnTitle(mColumnIndex) : "";
		}

	public void addFilterListener(FilterListener l) {
		if (mListenerList == null)
			mListenerList = new ArrayList<FilterListener>();
		mListenerList.add(l);
		}

	public void removeFilterListener(FilterListener l) {
		if (mListenerList != null)
			mListenerList.remove(l);
		if (mListenerList.size() == 0)
			mListenerList = null;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	/**
	 * Informs all listeners of a user induced filter change.
	 * Events are sent only, if mIsUserChange is true.
	 * To make this possible, any derived class
	 * is responsible to set mIsUserChange to true after the construction.
	 * Any derived class must also temporarily set mIsUserChange to false,
	 * if something that was not caused by a direct user interaction
	 * results in an updateExclusion() call or any direct tableModel call,
	 * which changes row exclusion.
	 * @param type
	 * @param isAdjusting
	 */
	public void fireFilterChanged(int type, boolean isAdjusting) {
		if (mListenerList != null && mIsUserChange)
			for (FilterListener l:mListenerList)
				l.filterChanged(new FilterEvent(this, type, isAdjusting));
		}

	/**
	 * Determines whether the filter is actively contributing to row visibility.
	 * Filters that are just used to configure a macro task or already removed filters are inactive.
	 * @return whether the filter is active
	 */
	public boolean isActive() {
		return mIsActive;
		}

	/**
	 * This is called to check whether an inversion can be performed.
	 * For disabled filters the state of the inversion checkbox is irrelevant
	 * and interactively toggling its state should not have an effect.
	 * @return true if filter is enabled and can be interactively inverted
	 */
//	public abstract boolean isFilterEnabled();

	/**
	 * If a CompoundTableEvent informs about a change that needs to update the filter settings,
	 * then this update should be delayed to not interfere with the completion of the
	 * original change through all listeners. Use this method to do so...
	 * This is should only be called, if the update request is indirect (not direct user change)
	 */
	public void updateExclusionLater() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateExclusion(false);
				}
			});
		}

	/**
	 * This causes the derived filter to update the exclusion with all settings on the tableModel.
	 * @param isUserChange whether the update request is caused by a direct user change of a filter
	 */
	public abstract void updateExclusion(boolean isUserChange);

	/**
	 * Enables or disables all components of the derived filter panel
	 * @param enabled
	 */
	public abstract void enableItems(boolean enabled);

	/**
	 * Override this if the filter cannot be enabled under certain circumstances.
	 * If the filter cannot be enabled, display a message, why.
	 * @return
	 */
	public boolean canEnable(boolean allowShowMessage) {
		return true;
		}

	/**
	 * This must be overwritten by filter panels which are not
	 * directly associated with a not changing column index.
	 */
	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			mColumnIndex = e.getMapping()[mColumnIndex];
			if (mColumnIndex == -1)
				removePanel();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			if (mColumnIndex == e.getColumn())
				mColumnNameLabel.setText(mTableModel.getColumnTitle(mColumnIndex));
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			fireFilterChanged(FilterEvent.FILTER_CLOSED, false);
			removePanel();
			return;
			}
		if (e.getActionCommand().equals("options")) {
			showOptionDialog(createAnimationOptionPanel());
			return;
			}
		if (e.getActionCommand().equals("optionsCancel")) {
			mOptionsDialog.setVisible(false);
			mOptionsDialog.dispose();
			return;
			}
		if (e.getActionCommand().equals("optionsOK")) {
			try {
				setFrameDelay(Long.parseLong(mTextFieldFrameDelay.getText()));
				mOptionsDialog.setVisible(false);
				mOptionsDialog.dispose();
				}
			catch (NumberFormatException nfe) {
				}
			return;
			}
		if (e.getActionCommand().equals("start")) {
			startAnimation();
			fireFilterChanged(FilterEvent.FILTER_ANIMATION_STARTED, false);
			return;
			}
		if (e.getActionCommand().equals("stop")) {
			stopAnimation();
			fireFilterChanged(FilterEvent.FILTER_ANIMATION_STOPPED, false);
			return;
			}
		if (e.getSource() == mButtonInverse) {
			setInverse(mButtonInverse.isSelected());
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			return;
			}
		if (e.getSource() == mButtonDisabled) {
			setEnabled(!mButtonDisabled.isSelected());
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			return;
			}
		}

	private void showOptionDialog(JPanel content) {
		Component frame = this;
		while (!(frame instanceof Frame))
			frame = frame.getParent();
		
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.setActionCommand("optionsCancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.setActionCommand("optionsOK");
		bok.addActionListener(this);
		ibp.add(bok);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		buttonPanel.add(ibp, BorderLayout.EAST);

		mOptionsDialog = new JDialog((Frame)frame, "Set Animation Options", true);
		mOptionsDialog.getContentPane().setLayout(new BorderLayout());
		mOptionsDialog.getContentPane().add(content, BorderLayout.CENTER);
		mOptionsDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		mOptionsDialog.getRootPane().setDefaultButton(bok);
		mOptionsDialog.pack();
		mOptionsDialog.setLocationRelativeTo(frame);
		mOptionsDialog.setVisible(true);
		}

	protected JPanel createAnimationOptionPanel() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Frame delay (ms):"), "1,1");

		mTextFieldFrameDelay = new JTextField(""+mAnimator.getFrameDelay(), 6);
		p.add(mTextFieldFrameDelay, "3,1");

		return p;
		}

	public boolean isInverse() {
		return mIsInverse;
		}

	public void removePanel() {
		if (mAnimator != null)
			resetAnimation();

		if (mExclusionFlag != -1)
			mTableModel.freeRowFlag(mExclusionFlag);
		mExclusionFlag = -1;
		Container theParent = getParent();
		theParent.remove(this);
		theParent.getParent().validate();
		theParent.getParent().repaint();
		}

	public int getColumnIndex() {
		return mColumnIndex;
		}

	public void setColumnIndex(int index) {
		mColumnIndex = index;
		}

	protected void setText(String text, Color color) {
		mColumnNameLabel.setForeground(color);
		mColumnNameLabel.setText(text);
		}

	/**
	 * Returns a unique filter type ID: one of FILTER_TYPE_...
	 * @return
	 */
	public abstract int getFilterType();

	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	public String getAnimationSettings() {
		return (mAnimator != null) ? (mAnimator.isAnimating() ? "state=running" : "state=stopped")
									+ " delay="+mAnimator.getFrameDelay() : null;
		}

	@Override
	public void setEnabled(boolean b) {
		if (isEnabled() != b
		 && (!b || canEnable(mSuppressErrorMessages))) {
			super.setEnabled(b);
			mButtonDisabled.setSelected(!b);
			if (isActive()) {
				if (!b) {
					mTableModel.freeRowFlag(mExclusionFlag);
					mExclusionFlag = -1;
					}
				else {
					mExclusionFlag = mTableModel.getUnusedRowFlag(true);
					if (mExclusionFlag == -1) {
						mButtonDisabled.setSelected(true);
						b = false;
						}
					else {
						updateExclusion(mIsUserChange);
						}
					}
				}
			enableItems(b);
			mButtonInverse.setEnabled(b);
			if (mAnimationButton != null)
				mAnimationButton.setEnabled(b);
			if (mAnimator != null)
				mAnimator.setSuspended(!b);
			}
		}

	public void setInverse(boolean b) {
		if (mIsInverse != b) {
			mIsInverse = b;
			mButtonInverse.setSelected(b);
			if (isEnabled() && isActive()) {
				mTableModel.invertExclusion(mExclusionFlag);
				}
			}
		}

	public final String getSettings() {
		StringBuilder sb = new StringBuilder();
		if (isInverse())
			sb.append(INVERSE_CODE);
		if (!isEnabled()) {
			if (sb.length() != 0)
				sb.append('\t');
			sb.append(DISABLED_CODE);
			}
		String innerSettings = getInnerSettings();
		if (innerSettings != null && innerSettings.length() != 0) {
			if (sb.length() != 0)
				sb.append('\t');
			sb.append(innerSettings);
			}
		return (sb.length() == 0) ? null : sb.toString();
		}

	public void applySettings(String settings, boolean suppressErrorMessages) {
		mIsUserChange = false;
		mSuppressErrorMessages = suppressErrorMessages;

		boolean inverse = false;
		if (settings != null && settings.startsWith(INVERSE_CODE)) {
			inverse = true;
			settings = (settings.length() == INVERSE_CODE.length()) ? null : settings.substring(INVERSE_CODE.length()+1);
			}
		boolean enabled = true;
		if (settings != null && (settings.startsWith(DISABLED_CODE) || settings.startsWith("<disabled>"))) {
			// || settings.startsWith("<disabled>") to be compatible with earlier "<disabled>" option as inner settings
			enabled = false;
			settings = (settings.length() == DISABLED_CODE.length()) ? null : settings.substring(DISABLED_CODE.length()+1);
			}

		setEnabled(enabled);
		setInverse(inverse);

		if (settings != null && settings.length() != 0)
			applyInnerSettings(settings);

		mSuppressErrorMessages = true;
		mIsUserChange = true;
		}

	public abstract void applyInnerSettings(String settings);
	public abstract String getInnerSettings();

	public final void applyAnimationSettings(String settings) {
		mIsUserChange = false;
		resetAnimation();
		if (settings != null) {
			boolean running = false;
			for (String setting:settings.split(" ")) {
				int index = setting.indexOf('=');
				if (index != -1) {
					String key = setting.substring(0, index);
					String value = setting.substring(index+1);
					if (key.equals("state"))
						running = value.equals("running");
					else if (key.equals("delay"))
						try { setFrameDelay(Long.parseLong(value)); } catch (NumberFormatException e) {}
					else
						applyAnimationSetting(key, value);
					}
				}
			if (running)
				startAnimation();
			}
		mIsUserChange = true;
		}


	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	protected void applyAnimationSetting(String key, String value) {}

	protected String attachSetting(String settingList, String setting) {
		if (settingList == null)
			return setting;

		return settingList + "\t" + setting;
		}

	/**
	 * Subclass should disable filtering or change to a setting that includes all records
	 * or if this is not possible then remove entire filter.
	 * This is only called, if the filter is active.
	 */
	public void reset() {
		mIsUserChange = false;
		if (mAnimator != null)
			resetAnimation();

		innerReset();

		setInverse(false);
		mIsUserChange = true;
		}

	/**
	 * Only called, if the filter is active.
	 */
	public abstract void innerReset();

	public void showAnimationControls() {
		JPanel mbp = new JPanel();
		mAnimationButton = new HiDPIIconButton("gear14.png", null, "showPopup");
		mAnimationButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (mAnimationButton.isEnabled() && mAnimationPopup == null)
					showAnimationPopup(e);
				}
			});
		mbp.add(mAnimationButton);
		mTitlePanel.add(mbp, "3,0");
		}

	private void showAnimationPopup(MouseEvent e) {
		JButton button = (JButton)e.getSource();
		mAnimationPopup = new JPopupMenu() {
			private static final long serialVersionUID = 1L;

			@Override
			public void setVisible(boolean b) {
				super.setVisible(b);
				mAnimationPopup = null;
				}
			};
		mAnimationPopup.add(createPopupItem("Start Animation", "start"));
		mAnimationPopup.add(createPopupItem("Stop Animation", "stop"));
		mAnimationPopup.addSeparator();
		mAnimationPopup.add(createPopupItem("Animation Options...", "options"));
		mAnimationPopup.show(button.getParent(),
							 button.getBounds().x,
							 button.getBounds().y + button.getBounds().height);
		}

	private JMenuItem createPopupItem(String title, String command) {
		JMenuItem item = new JMenuItem(title);
		item.setActionCommand(command);
		item.addActionListener(this);
		return item;
		}

	/**
	 * Changes the delay that the animation timer waits between two subsequent calls to animate().
	 */
	public void setFrameDelay(long frameDelay) {
		if (isActive())
			mAnimator.setFrameDelay(frameDelay);
		}

	/**
	 * Stops the animation timer and sets the current frame to 0.
	 */
	public void resetAnimation() {
		if (isActive())
			mAnimator.reset();
		}

	/**
	 * Starts or continues the animation with the current frame rate and number.
	 */
	public void startAnimation() {
		if (isActive())
			mAnimator.start();
		}

	/**
	 * Stops the animation timer without changing the current frame number.
	 */
	public void stopAnimation() {
		if (isActive())
			mAnimator.stop();
		}

	/**
	 * Override this if the derived filter supports animations.
	 * When an animation is running this method is called repeatedly
	 * after waiting for the frame delay with an increasing frame number.
	 * The filter should react by updating its filter settings to reflect
	 * the current frame number.
	 * @param frame
	 */
	protected void animate(int frame) {}

	private class Animator implements Runnable {
		private volatile long mStartMillis,mFrameDelay;
		private volatile boolean mIsSuspended;
		private Thread mThread;

		public Animator(long frameDelay) {
			mFrameDelay = frameDelay;
			}

		public boolean isAnimating() {
			return mThread != null;
			}

		public long getFrameDelay() {
			return mFrameDelay;
			}

		public void setFrameDelay(long frameDelay) {
			mFrameDelay = frameDelay;
			}

		public void reset() {
			mThread = null;
			}

		public void start() {
			if (mThread == null) {
				mThread = new Thread(this);
				mThread.start();
				mStartMillis = System.currentTimeMillis();
				}
			}

		public void stop() {
			mThread = null;
			}

		public void setSuspended(boolean b) {
			mIsSuspended = b;
			}

		@Override
		public void run() {
			while (Thread.currentThread() == mThread) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						int mRecentFrame = -1;
	
						@Override
						public void run() {
							if (mThread != null) {
								long totalDelay = System.currentTimeMillis() - mStartMillis;
								int currentFrame = (int)(totalDelay / mFrameDelay);
								if (mRecentFrame < currentFrame) {
									mRecentFrame = currentFrame;
									mIsUserChange = false;
									animate(currentFrame);
									mIsUserChange = true;
									}
								}
							}
						});
					}
				catch (InvocationTargetException ite) {}
				catch (InterruptedException ie) {}

				try {
					do {
						Thread.sleep(mFrameDelay - (System.currentTimeMillis() - mStartMillis) % mFrameDelay);
						} while (mIsSuspended);
					}
				catch (InterruptedException ie) {}
				}
			}
		}
	}
