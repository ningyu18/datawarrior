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

package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;


public class DEHelpFrame extends JFrame implements ActionListener, HyperlinkListener {
    private static final long serialVersionUID = 0x20061025;

	private static ArrayList<DEHelpFrame> mHelpFrameList;
    private DEFrame 			mParent;
	private JEditorPane			mTextArea;		
	private JEditorPane			mContentArea;

	public static void updateLookAndFeel() {
		if (mHelpFrameList != null) {
			for (DEHelpFrame hf : mHelpFrameList) {
				SwingUtilities.updateComponentTreeUI(hf);
				hf.mTextArea.setBackground(Color.white);
				}
			}
		}

	public DEHelpFrame(DEFrame parent) {
		super("DataWarrior Help");
		mParent = parent;

		final float zoomFactor = HiDPIHelper.getUIScaleFactor();
		final int scaled250 = (int)(zoomFactor*250);

		getContentPane().setLayout(new BorderLayout());	//change this to tableLayout

		//1st jEditorPane
		mTextArea = new JEditorPane();
		mTextArea.setEditorKit(HiDPIHelper.getUIScaleFactor() == 1f ? new HTMLEditorKit() : new ScaledEditorKit());
		mTextArea.setEditable(false);
		mTextArea.addHyperlinkListener(this);
		mTextArea.setContentType("text/html");
		mTextArea.setBackground(Color.white);   // leads to gray in dark substance LaF
		try {
		    mTextArea.setPage(getClass().getResource("/html/help/basics.html"));
			}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(mParent, ioe.getMessage());
			return;
			}

		JScrollPane spTextArea = new JScrollPane(mTextArea);
		spTextArea.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spTextArea.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spTextArea.setPreferredSize(new Dimension(scaled250, scaled250));

		//2nd jEditorPane
		mContentArea = new JEditorPane();
		mContentArea.setEditorKit(HiDPIHelper.getUIScaleFactor() == 1f ? new HTMLEditorKit() : new ScaledEditorKit());
		mContentArea.setEditable(false);
		mContentArea.addHyperlinkListener(this);
		mContentArea.setContentType("text/html");
		try {
			DataWarrior app = parent.getApplication();
			mContentArea.setPage(getClass().getResource(
					app.isIdorsia() ? "/html/help/contentActelion.html" : "/html/help/content.html"));
			}
		catch (IOException e) {
			JOptionPane.showMessageDialog(mParent,e.getMessage());
			return;
			}
		JScrollPane spContentArea = new JScrollPane(mContentArea);
		spContentArea.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spContentArea.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spContentArea.setPreferredSize(new Dimension(scaled250, scaled250));
		
		JTabbedPane tabbedContentPane = new JTabbedPane();
		tabbedContentPane.addTab("Content", spContentArea);
		
//		Create a split pane with the two scroll panes in it.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedContentPane, spTextArea);
	  	splitPane.setOneTouchExpandable(true);
	  	splitPane.setDividerLocation(scaled250);

//		Provide minimum sizes for the two components in the split pane
	  	Dimension minimumSize = new Dimension((int)(zoomFactor*100), (int)(zoomFactor*50));
		tabbedContentPane.setMinimumSize(minimumSize);
		spTextArea.setMinimumSize(minimumSize);
		
		getContentPane().add(splitPane, BorderLayout.CENTER);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		if (mHelpFrameList == null)
			mHelpFrameList = new ArrayList<>();
		mHelpFrameList.add(this);

		setSize((int)(zoomFactor*980), Math.min((int)(zoomFactor*1024), (int)getGraphicsConfiguration().getBounds().getHeight()-32));
		setLocation(new Point((int)(mParent.getLocationOnScreen().getX() + mParent.getSize().getWidth()/2 - this.getSize().getWidth()/2), (int)(mParent.getLocationOnScreen().getY() + mParent.getSize().getHeight()/2 - this.getSize().getHeight()/2)));
		setResizable(true);
		setVisible(true);
		}

	@Override
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			mHelpFrameList.remove(this);
			setVisible(false);
			dispose();
			}
		}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			java.net.URL url = e.getURL();
			try {
				mTextArea.setPage(url);
				}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(mParent, ioe.getMessage(), "DataWarrior Help", JOptionPane.WARNING_MESSAGE);
				return;
				}
			}
		}
	
	public void actionPerformed(ActionEvent e){	
//		DocumentRenderer DocumentRenderer = new DocumentRenderer();
//		DocumentRenderer.print(mTextArea);
		}
	}