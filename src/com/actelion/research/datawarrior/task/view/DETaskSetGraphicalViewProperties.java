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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.DEColorPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.*;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Properties;


public class DETaskSetGraphicalViewProperties extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Graphical View Options";

	private static final String PROPERTY_FONT_SIZE = "fontSize";
	private static final String PROPERTY_HIDE_GRID = "hideGrid";	// allowed: x,y,true,false
	private static final String PROPERTY_HIDE_LEGEND = "hideLegend";
	private static final String PROPERTY_HIDE_SCALE = "hideScale";	// allowed: x,y,true,false
	private static final String PROPERTY_SHOW_EMPTY = "showEmpty";
	private static final String PROPERTY_GLOBAL_EXCLUSION = "globalExclusion";
	private static final String PROPERTY_FAST_RENDERING = "fastRendering";
	private static final String PROPERTY_DEFAULT_DATA_COLOR = "defaultDataColor";
	private static final String PROPERTY_MISSING_DATA_COLOR = "missingDataColor";
	private static final String PROPERTY_BACKGROUND_COLOR = "backgroundColor";
	private static final String PROPERTY_LABEL_BACKGROUND_COLOR = "labelBackgroundColor";
	private static final String PROPERTY_GRAPH_FACE_COLOR = "graphFaceColor";
	private static final String PROPERTY_TITLE_BACKGROUND_COLOR = "titleBGColor";

	private JComboBox		mComboBoxScaleMode,mComboBoxGridMode;
	private JSlider			mSliderScaleFontSize;
	private JCheckBox	    mCheckBoxHideLegend,mCheckBoxShowNaN,mCheckBoxGlobalExclusion,mCheckBoxFastRendering;
	private DEColorPanel    mDefaultDataColorPanel,mMissingDataColorPanel,mBackgroundColorPanel,mLabelBackgroundColorPanel,
							mGraphFaceColorPanel,mTitleBackgroundPanel;

	/**
	 * @param owner
	 * @param mainPane
	 * @param view null or view that is interactively updated
	 */
	public DETaskSetGraphicalViewProperties(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view);
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Graphical view properties can only be applied to 2D- and 3D-Views.";
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel scalePanel = new JPanel();
		double[][] sizeScalePanel = { {8, TableLayout.FILL, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, TableLayout.FILL, 8},
									  {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8,
										  TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8 } };
		scalePanel.setLayout(new TableLayout(sizeScalePanel));

		scalePanel.add(new JLabel("Show scales:"), "2,1");
		mComboBoxScaleMode = new JComboBox((!hasInteractiveView() || getInteractiveVisualization() instanceof JVisualization2D) ?
							JVisualization2D.SCALE_MODE_TEXT : JVisualization3D.SCALE_MODE_TEXT);
		mComboBoxScaleMode.addActionListener(this);
		scalePanel.add(mComboBoxScaleMode, "4,1");

		scalePanel.add(new JLabel("Show grid:"), "2,3");
		mComboBoxGridMode = new JComboBox((!hasInteractiveView() || getInteractiveVisualization() instanceof JVisualization2D) ?
				JVisualization2D.GRID_MODE_TEXT : JVisualization3D.GRID_MODE_TEXT);
		mComboBoxGridMode.addActionListener(this);
		scalePanel.add(mComboBoxGridMode, "4,3");

		mSliderScaleFontSize = new JSlider(JSlider.HORIZONTAL, 0, 150, 50);
		mSliderScaleFontSize.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderScaleFontSize.getPreferredSize().height));
		mSliderScaleFontSize.addChangeListener(this);
		scalePanel.add(new JLabel("Scale font size:"), "2,5");
		scalePanel.add(mSliderScaleFontSize, "4,5");

		mCheckBoxHideLegend = new JCheckBox("Hide legend", false);
		mCheckBoxHideLegend.addActionListener(this);
		scalePanel.add(mCheckBoxHideLegend, "4,7");

		JPanel staticColorPanel = new JPanel();
		double[][] sizeStaticColorPanel = { {8, TableLayout.FILL, TableLayout.PREFERRED, 8, 64, 12, TableLayout.PREFERRED, TableLayout.FILL, 8},
											{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED,
											 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
		staticColorPanel.setLayout(new TableLayout(sizeStaticColorPanel));

		int colorIndex = 0;
		mDefaultDataColorPanel = addColorChooser("Default marker color:", "changeDDC", 1+2*colorIndex++,
				VisualizationColor.cDefaultDataColor, staticColorPanel);
		mMissingDataColorPanel = addColorChooser("Missing data color:", "changeMDC", 1+2*colorIndex++,
				VisualizationColor.cMissingDataColor, staticColorPanel);
		mBackgroundColorPanel = addColorChooser("Background color:", "changeBGC", 1+2*colorIndex++,
				JVisualization.DEFAULT_LABEL_BACKGROUND, staticColorPanel);
		mLabelBackgroundColorPanel = addColorChooser("Label Background:", "changeLBG", 1+2*colorIndex++,
				Color.WHITE, staticColorPanel);
		if (!hasInteractiveView() || getInteractiveVisualization().isSplitViewConfigured())
			mTitleBackgroundPanel = addColorChooser("Splitting title area:", "changeTAC", 1+2*colorIndex++,
					JVisualization.DEFAULT_TITLE_BACKGROUND, staticColorPanel);
		if (!hasInteractiveView() || getInteractiveVisualization() instanceof JVisualization3D)
			mGraphFaceColorPanel = addColorChooser("Graph face color:", "changeGFC", 1+2*colorIndex++,
					new Color(0x00FFFFFF & JVisualization3D.DEFAULT_GRAPH_FACE_COLOR), staticColorPanel);

		JPanel rowHidingPanel = new JPanel();
		double[][] sizeRowHidingPanel = { {8, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, 8},
										  {8, TableLayout.PREFERRED, TableLayout.PREFERRED, 8 } };
		rowHidingPanel.setLayout(new TableLayout(sizeRowHidingPanel));
		mCheckBoxShowNaN = new JCheckBox("Show empty values", false);
		mCheckBoxShowNaN.addActionListener(this);
		rowHidingPanel.add(mCheckBoxShowNaN, "2,1");
		mCheckBoxGlobalExclusion = new JCheckBox("Hide invisible rows in other views", true);
		mCheckBoxGlobalExclusion.addActionListener(this);
		rowHidingPanel.add(mCheckBoxGlobalExclusion, "2,2");

		JPanel renderPanel = new JPanel();
		double[][] sizeRenderPanel = { {8, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, 8},
									   {8, TableLayout.PREFERRED, 8 } };
		renderPanel.setLayout(new TableLayout(sizeRenderPanel));
		mCheckBoxFastRendering = new JCheckBox("Fast rendering / lower quality", false);
		mCheckBoxFastRendering.addActionListener(this);
		renderPanel.add(mCheckBoxFastRendering, "2,1");

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setToolTipText("");
		tabbedPane.add(scalePanel, "Scales");
		tabbedPane.add(staticColorPanel, "Colors");
		tabbedPane.add(rowHidingPanel, "Row hiding");
		tabbedPane.add(renderPanel, "Rendering");
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		return tabbedPane;
		}

	private DEColorPanel addColorChooser(String text, String actionCommand, int position, Color color, JPanel backgroundPanel) {
		backgroundPanel.add(new JLabel(text), "2,"+position);
		DEColorPanel colorPanel = new DEColorPanel(color);
		backgroundPanel.add(colorPanel, "4,"+position);
		JButton button = new JButton("Change");
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
		backgroundPanel.add(button, "6,"+position);
		return colorPanel;
		}

	@Override
	public void setDialogToDefault() {
		mComboBoxScaleMode.setSelectedIndex(0);
		mComboBoxGridMode.setSelectedIndex(0);
		mSliderScaleFontSize.setValue(50);
		mCheckBoxHideLegend.setSelected(false);
		mCheckBoxShowNaN.setSelected(false);
		mCheckBoxGlobalExclusion.setSelected(true);
		mCheckBoxFastRendering.setSelected(false);;

		if (mDefaultDataColorPanel != null)
			mDefaultDataColorPanel.setColor(VisualizationColor.cDefaultDataColor);
		if (mMissingDataColorPanel != null)
			mMissingDataColorPanel.setColor(VisualizationColor.cMissingDataColor);
		if (mBackgroundColorPanel != null)
			mBackgroundColorPanel.setColor(Color.WHITE);
		if (mLabelBackgroundColorPanel != null)
			mLabelBackgroundColorPanel.setColor(JVisualization.DEFAULT_LABEL_BACKGROUND);
		if (mGraphFaceColorPanel != null)
			mGraphFaceColorPanel.setColor(new Color(0x00FFFFFF & JVisualization3D.DEFAULT_GRAPH_FACE_COLOR));
		if (mTitleBackgroundPanel != null)
			mTitleBackgroundPanel.setColor(JVisualization.DEFAULT_TITLE_BACKGROUND);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		double fontSize = 1.0;
		try { fontSize = Double.parseDouble(configuration.getProperty(PROPERTY_FONT_SIZE, "1.0")); } catch (NumberFormatException nfe) {}
		int scaleMode = findListIndex(configuration.getProperty(PROPERTY_HIDE_SCALE), JVisualization.SCALE_MODE_CODE, 0);
		mComboBoxScaleMode.setSelectedIndex(scaleMode<mComboBoxScaleMode.getItemCount() ? scaleMode : 0);
		int gridMode = findListIndex(configuration.getProperty(PROPERTY_HIDE_GRID), JVisualization.GRID_MODE_CODE, 0);
		mComboBoxGridMode.setSelectedIndex(gridMode<mComboBoxGridMode.getItemCount() ? gridMode : 0);
		mSliderScaleFontSize.setValue(50+(int)(50.0*Math.log(fontSize)));
		mCheckBoxHideLegend.setSelected("true".equals(configuration.getProperty(PROPERTY_HIDE_LEGEND)));
		mCheckBoxShowNaN.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_EMPTY)));
		mCheckBoxGlobalExclusion.setSelected(!"false".equals(configuration.getProperty(PROPERTY_GLOBAL_EXCLUSION)));
		mCheckBoxFastRendering.setSelected("true".equals(configuration.getProperty(PROPERTY_FAST_RENDERING)));;

		if (mDefaultDataColorPanel != null)
			try { mDefaultDataColorPanel.setColor(Color.decode(configuration.getProperty(PROPERTY_DEFAULT_DATA_COLOR))); } catch (NumberFormatException nfe) {}
		if (mMissingDataColorPanel != null)
			try { mMissingDataColorPanel.setColor(Color.decode(configuration.getProperty(PROPERTY_MISSING_DATA_COLOR))); } catch (NumberFormatException nfe) {}
		if (mBackgroundColorPanel != null)
			try { mBackgroundColorPanel.setColor(Color.decode(configuration.getProperty(PROPERTY_BACKGROUND_COLOR))); } catch (NumberFormatException nfe) {}
		if (mLabelBackgroundColorPanel != null)
			try { mLabelBackgroundColorPanel.setColor(Color.decode(configuration.getProperty(PROPERTY_LABEL_BACKGROUND_COLOR))); } catch (NumberFormatException nfe) {}
		if (mTitleBackgroundPanel != null) {
			String colorText = configuration.getProperty(PROPERTY_TITLE_BACKGROUND_COLOR);
			try { mTitleBackgroundPanel.setColor((colorText == null) ? JVisualization.DEFAULT_TITLE_BACKGROUND : Color.decode(colorText)); } catch (NumberFormatException nfe) {}
			}
		if (mGraphFaceColorPanel != null) {
			String colorText = configuration.getProperty(PROPERTY_GRAPH_FACE_COLOR);
			try { mGraphFaceColorPanel.setColor((colorText == null) ? new Color(0x00FFFFFF & JVisualization3D.DEFAULT_GRAPH_FACE_COLOR) : Color.decode(colorText)); } catch (NumberFormatException nfe) {}
			}
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		float size = (float)Math.exp((double)(mSliderScaleFontSize.getValue()-50)/50.0);
		configuration.setProperty(PROPERTY_FONT_SIZE, ""+size);
		configuration.setProperty(PROPERTY_HIDE_SCALE, JVisualization.SCALE_MODE_CODE[mComboBoxScaleMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_HIDE_GRID, JVisualization.GRID_MODE_CODE[mComboBoxGridMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_HIDE_LEGEND, mCheckBoxHideLegend.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_SHOW_EMPTY, mCheckBoxShowNaN.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_GLOBAL_EXCLUSION, mCheckBoxGlobalExclusion.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_FAST_RENDERING, mCheckBoxFastRendering.isSelected() ? "true" : "false");

		if (mDefaultDataColorPanel != null)
			configuration.setProperty(PROPERTY_DEFAULT_DATA_COLOR, ""+mDefaultDataColorPanel.getColor().getRGB());
		if (mMissingDataColorPanel != null)
			configuration.setProperty(PROPERTY_MISSING_DATA_COLOR, ""+mMissingDataColorPanel.getColor().getRGB());
		if (mBackgroundColorPanel != null)
			configuration.setProperty(PROPERTY_BACKGROUND_COLOR, ""+mBackgroundColorPanel.getColor().getRGB());
		if (mLabelBackgroundColorPanel != null)
			configuration.setProperty(PROPERTY_LABEL_BACKGROUND_COLOR, ""+mLabelBackgroundColorPanel.getColor().getRGB());
		if (mTitleBackgroundPanel != null)
			configuration.setProperty(PROPERTY_TITLE_BACKGROUND_COLOR, ""+mTitleBackgroundPanel.getColor().getRGB());
		if (mGraphFaceColorPanel != null)
			configuration.setProperty(PROPERTY_GRAPH_FACE_COLOR, ""+mGraphFaceColorPanel.getColor().getRGB());
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_FONT_SIZE, ""+ getInteractiveVisualization().getFontSize());
		configuration.setProperty(PROPERTY_HIDE_SCALE, JVisualization.SCALE_MODE_CODE[getInteractiveVisualization().getScaleMode()]);
		configuration.setProperty(PROPERTY_HIDE_GRID, JVisualization.GRID_MODE_CODE[getInteractiveVisualization().getGridMode()]);
		configuration.setProperty(PROPERTY_HIDE_LEGEND, getInteractiveVisualization().isLegendSuppressed() ? "true" : "false");
		configuration.setProperty(PROPERTY_SHOW_EMPTY, getInteractiveVisualization().getShowNaNValues() ? "true" : "false");
		configuration.setProperty(PROPERTY_GLOBAL_EXCLUSION, getInteractiveVisualization().getAffectGlobalExclusion() ? "true" : "false");
		configuration.setProperty(PROPERTY_FAST_RENDERING, getInteractiveVisualization().isFastRendering() ? "true" : "false");

		configuration.setProperty(PROPERTY_DEFAULT_DATA_COLOR, ""+ getInteractiveVisualization().getMarkerColor().getDefaultDataColor().getRGB());
		configuration.setProperty(PROPERTY_MISSING_DATA_COLOR, ""+ getInteractiveVisualization().getMarkerColor().getMissingDataColor().getRGB());
		configuration.setProperty(PROPERTY_BACKGROUND_COLOR, ""+ getInteractiveVisualization().getViewBackground().getRGB());
		configuration.setProperty(PROPERTY_LABEL_BACKGROUND_COLOR, ""+ getInteractiveVisualization().getLabelBackground().getRGB());
		configuration.setProperty(PROPERTY_TITLE_BACKGROUND_COLOR, ""+ getInteractiveVisualization().getTitleBackground().getRGB());
		if (getInteractiveVisualization() instanceof JVisualization3D)
			configuration.setProperty(PROPERTY_GRAPH_FACE_COLOR, ""+((JVisualization3D) getInteractiveVisualization()).getGraphFaceColor().getRGB());
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		JVisualization v = ((VisualizationPanel)view).getVisualization();
		int scaleMode = findListIndex(configuration.getProperty(PROPERTY_HIDE_SCALE), JVisualization.SCALE_MODE_CODE, 0);
		if ((v instanceof JVisualization2D && scaleMode<JVisualization2D.SCALE_MODE_TEXT.length)
		 || (v instanceof JVisualization3D && scaleMode<JVisualization3D.SCALE_MODE_TEXT.length))
			v.setScaleMode(scaleMode);
		int gridMode = findListIndex(configuration.getProperty(PROPERTY_HIDE_GRID), JVisualization.GRID_MODE_CODE, 0);
		if ((v instanceof JVisualization2D && gridMode<JVisualization2D.GRID_MODE_TEXT.length)
		 || (v instanceof JVisualization3D && gridMode<JVisualization3D.GRID_MODE_TEXT.length))
			v.setGridMode(gridMode);
		try {
			v.setFontSize(Float.parseFloat(configuration.getProperty(PROPERTY_FONT_SIZE, "1.0")), isAdjusting);
			}
		catch (NumberFormatException nfe) {}
		v.setSuppressLegend("true".equals(configuration.getProperty(PROPERTY_HIDE_LEGEND)));
		v.setShowNaNValues("true".equals(configuration.getProperty(PROPERTY_SHOW_EMPTY)));
		v.setAffectGlobalExclusion(!"false".equals(configuration.getProperty(PROPERTY_GLOBAL_EXCLUSION)));
		v.setFastRendering("true".equals(configuration.getProperty(PROPERTY_FAST_RENDERING)));

		String ddc = configuration.getProperty(PROPERTY_DEFAULT_DATA_COLOR);
		if (ddc != null)
			try { v.getMarkerColor().setDefaultDataColor(Color.decode(ddc)); } catch (NumberFormatException nfe) {}
		String mdc = configuration.getProperty(PROPERTY_MISSING_DATA_COLOR);
		if (mdc != null)
			try { v.getMarkerColor().setMissingDataColor(Color.decode(mdc)); } catch (NumberFormatException nfe) {}
		String bgc = configuration.getProperty(PROPERTY_BACKGROUND_COLOR);
		if (bgc != null)
			try { v.setViewBackground(Color.decode(bgc)); } catch (NumberFormatException nfe) {}
		String lbg = configuration.getProperty(PROPERTY_LABEL_BACKGROUND_COLOR);
		if (lbg != null)
			try { v.setLabelBackground(Color.decode(lbg)); } catch (NumberFormatException nfe) {}
		String tbc = configuration.getProperty(PROPERTY_TITLE_BACKGROUND_COLOR);
		if (tbc != null)
			try { v.setTitleBackground(Color.decode(tbc)); } catch (NumberFormatException nfe) {}
		String gfc = configuration.getProperty(PROPERTY_GRAPH_FACE_COLOR);
		if (gfc != null && v instanceof JVisualization3D)
			try { ((JVisualization3D)v).setGraphFaceColor(Color.decode(gfc)); } catch (NumberFormatException nfe) {}
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "changeDDC") {
			Color newColor = JColorChooser.showDialog(getDialog(), "Select Default Marker Color", mDefaultDataColorPanel.getColor());
			if (newColor == null || newColor.equals(mDefaultDataColorPanel.getColor()))
				return;
			mDefaultDataColorPanel.setColor(newColor);
			}
		else if (e.getActionCommand() == "changeMDC") {
			Color newColor = JColorChooser.showDialog(getDialog(), "Select Missing Data Color", mMissingDataColorPanel.getColor());
			if (newColor == null || newColor.equals(mMissingDataColorPanel.getColor()))
				return;
			mMissingDataColorPanel.setColor(newColor);
			}
		else if (e.getActionCommand() == "changeBGC") {
			Color newColor = JColorChooser.showDialog(getDialog(), "Select Background Color", mBackgroundColorPanel.getColor());
			if (newColor == null || newColor.equals(mBackgroundColorPanel.getColor()))
				return;
			mBackgroundColorPanel.setColor(newColor);
			}
		else if (e.getActionCommand() == "changeLBG") {
			Color newColor = JColorChooser.showDialog(getDialog(), "Select Label Background Color", mLabelBackgroundColorPanel.getColor());
			if (newColor == null || newColor.equals(mLabelBackgroundColorPanel.getColor()))
				return;
			mLabelBackgroundColorPanel.setColor(newColor);
			}
		else if (e.getActionCommand() == "changeGFC") {
			Color newColor = JColorChooser.showDialog(getDialog(), "Select Graph Face Color", mGraphFaceColorPanel.getColor());
			if (newColor == null || newColor.equals(mGraphFaceColorPanel.getColor()))
				return;
			mGraphFaceColorPanel.setColor(newColor);
			}
		else if (e.getActionCommand() == "changeTAC") {
			Color newColor = JColorChooser.showDialog(getDialog(), "Select Splitting Title Area Color", mTitleBackgroundPanel.getColor());
			if (newColor == null || newColor.equals(mTitleBackgroundPanel.getColor()))
				return;
			mTitleBackgroundPanel.setColor(newColor);
			}

		super.actionPerformed(e);
		}

	@Override
	public void enableItems() {
		}
	}
