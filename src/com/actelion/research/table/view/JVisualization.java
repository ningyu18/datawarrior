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

package com.actelion.research.table.view;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.graph.RadialVisualizationNode;
import com.actelion.research.table.view.graph.TreeVisualizationNode;
import com.actelion.research.table.view.graph.VisualizationNode;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TTestImpl;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;

public abstract class JVisualization extends JComponent
		implements CompoundTableListener,FocusableView,HighlightListener,MarkerLabelDisplayer,ListSelectionListener,MouseListener,
			MouseMotionListener,Printable,CompoundTableListListener,VisualizationColorListener {
	private static final long serialVersionUID = 0x20100610;

	// taken from class com.actelion.research.gui.form.FormObjectBorder
	public static final Color DEFAULT_TITLE_BACKGROUND = new Color(224, 224, 255);
	public static final Color DEFAULT_LABEL_BACKGROUND = new Color(224, 224, 224);
	public static final float DEFAULT_LABEL_TRANSPARENCY = 0.25f;
	protected static final Rectangle2D.Double DEPICTOR_RECT = new Rectangle2D.Double(0, 0, 32000, 32000);

	public static final int cColumnUnassigned = -1;
	public static final int cConnectionColumnConnectAll = -2;
	public static final int cConnectionColumnMeanAndMedian = -3;
	public static final int cConnectionColumnConnectCases = -4;

	public static final int cChartTypeScatterPlot = 0;
	public static final int cChartTypeWhiskerPlot = 1;
	public static final int cChartTypeBoxPlot = 2;
	public static final int cChartTypeBars = 3;
	public static final int cChartTypePies = 4;

	public static final String[] CHART_TYPE_NAME = { "Scatter Plot", "Whisker Plot", "Box Plot", "Bar Chart", "Pie Chart" };
	public static final String[] CHART_TYPE_CODE = { "scatter", "whiskers", "boxes", "bars", "pies" };

	public static final int cChartModeCount = 0;
	public static final int cChartModePercent = 1;
	public static final int cChartModeMean = 2;
	public static final int cChartModeMin = 3;
	public static final int cChartModeMax = 4;
	public static final int cChartModeSum = 5;

	public static final String[] CHART_MODE_NAME = { "Row Count", "Row Percentage", "Mean Value", "Minimum Value", "Maximum Value", "Sum of Values" };
	public static final String[] CHART_MODE_CODE = { "count", "percent", "mean", "min", "max", "sum" };

	public static final String[] SCALE_MODE_CODE = { "false", "true", "y", "x" };
	public static final int cScaleModeShown = 0;
	public static final int cScaleModeHidden = 1;
	public static final int cScaleModeHideY = 2;
	public static final int cScaleModeHideX = 3;

	public static final String[] GRID_MODE_CODE = { "false", "true", "vertical", "horizontal" };
	public static final int cGridModeShown = 0;
	public static final int cGridModeHidden = 1;
	public static final int cGridModeShowVertical = 2;
	public static final int cGridModeShowHorizontal = 3;

	public static final int cTreeViewModeNone = 0;
	public static final int cTreeViewModeHTree = 1;
	public static final int cTreeViewModeVTree = 2;
	public static final int cTreeViewModeRadial = 3;

	public static final String[] TREE_VIEW_MODE_NAME = { "<none>", "Horizontal Tree", "Vertical Tree", "Radial Graph" };
	public static final String[] TREE_VIEW_MODE_CODE = { "none", "hTree", "vTree", "radial" };

	public static final int cMaxChartCategoryCount = 256;			// this is for one axis
	public static final int cMaxTotalChartCategoryCount = 32768;	// this is the product of all axis
	public static final float cBarSpacingFactor = 1.08f;

	public static final int cMaxCaseSeparationCategoryCount = 128;	// this is for one axis
	public static final int cMaxSplitViewCount = 10000;

	protected static final String[] CHART_MODE_AXIS_TEXT = CHART_MODE_CODE;

	public static final String[] BOXPLOT_MEAN_MODE_TEXT = { "No Indicator", "Median Line", "Mean Line", "Mean & Median Lines", "Mean & Median Triangles" };
	public static final String[] BOXPLOT_MEAN_MODE_CODE = { "none", "median", "mean", "both", "triangles" };
	public static final int BOXPLOT_DEFAULT_MEAN_MODE = 3;
	protected static final int cBoxplotMeanModeMedian = 1;
	protected static final int cBoxplotMeanModeMean = 2;
	protected static final int cBoxplotMeanModeLines = 3;
	protected static final int cBoxplotMeanModeTriangles = 4;

/*	protected static final int AXIS_TYPE_UNASSIGNED = 0;
	protected static final int AXIS_TYPE_TEXT_CATEGORY = 1;
	protected static final int AXIS_TYPE_DOUBLE_CATEGORY = 2;
	protected static final int AXIS_TYPE_DOUBLE_VALUE = 3;
*/
	protected static final float SCALE_LIGHT = 0.4f;
	protected static final float SCALE_MEDIUM = 0.7f;
	protected static final float SCALE_STRONG = 1.0f;

	private static final int DRAG_MODE_NONE = 0;
	private static final int DRAG_MODE_LASSO_SELECT = 1;
	private static final int DRAG_MODE_RECT_SELECT = 2;
	private static final int DRAG_MODE_TRANSLATE = 3;
	private static final int DRAG_MODE_MOVE_LABEL = 4;

	private static final byte EXCLUSION_FLAG_ZOOM_0 = 0x01;
	private static final byte EXCLUSION_FLAG_NAN_0 = 0x08;
	private static final byte EXCLUSION_FLAGS_NAN = (byte)(7 * EXCLUSION_FLAG_NAN_0);
	private static final byte EXCLUSION_FLAG_DETAIL_GRAPH = 0x40;
	private static final byte EXCLUSION_FLAG_OTHER_NAN = (byte)0x80;	// used if a column's NAN values affect visibility, but column not assigned to axis

	private static final long RIGHT_MOUSE_POPUP_DELAY = 800;

	public static final String CUSTOM_LABEL_POSITION_START_VIEW_TAG = "<view name=\"";
	public static final String CUSTOM_LABEL_POSITION_END_VIEW_TAG = "</view>";

	protected CompoundTableModel	mTableModel;
	protected float[]				mAxisVisMin,mAxisVisMax;
	protected float[][]				mAxisSimilarity;
	protected int[]					mAxisIndex,mLabelColumn,mSplittingColumn,mCategoryVisibleCount;
	protected boolean[]				mIsCategoryAxis;
	protected CategoryViewInfo		mChartInfo;
	protected VisualizationPoint[]	mPoint;
	protected VisualizationPoint 	mHighlightedPoint,mActivePoint;
	protected VisualizationLabelPosition mHighlightedLabelPosition;
	protected VisualizationColor	mMarkerColor;
	protected VisualizationSplitter	mSplitter;
	protected VisualizationPoint[]  mConnectionLinePoint;
	protected TreeMap<byte[],VisualizationPoint>mConnectionLineMap;
	private TreeMap<VisualizationPoint,LineConnection[]> mReverseConnectionMap;
	protected VisualizationNode[][]	mTreeNodeList;
	protected ArrayList<JVisualizationLegend> mLegendList;
	protected float					mAbsoluteMarkerSize,mRelativeMarkerSize,mMarkerLabelSize,mMarkerJittering,mRelativeFontSize,
									mAbsoluteConnectionLineWidth,mRelativeConnectionLineWidth,mCaseSeparationValue,mMarkerSizeZoomAdaption,
									mSplittingAspectRatio,mLabelBackgroundTransparency;
	protected volatile boolean		mOffImageValid;
	protected boolean				mCoordinatesValid,mMouseIsDown,mTouchFunctionActive,mLocalAffectsGlobalExclusion,
									mAddingToSelection,mMarkerSizeInversion,mSuppressLegend,mSuspendGlobalExclusion,
									mShowNaNValues, mShowMeanAndMedianValues,mBoxplotShowPValue,mBoxplotShowFoldChange,
									mLabelsInTreeViewOnly,mTreeViewShowAll,mTreeViewIsDynamic,mTreeViewIsInverted,
									mIsFastRendering,mMarkerSizeProportional,mShowLabelBackground,mIsConnectionLineInverted,
									mShowEmptyInSplitView,mShowStandardDeviation,mShowConfidenceInterval,mShowValueCount;
	private boolean[]				mAxisVisRangeIsLogarithmic;
	protected int					mDataPoints,mMarkerSizeColumn,mMarkerShapeColumn,mFontHeight,mBoxplotMeanMode,mLabelList,
									mMouseX1,mMouseY1,mMouseX2,mMouseY2,mDimensions,mConnectionColumn,mConnectionOrderColumn,
									mChartColumn,mChartMode,mChartType,mPreferredChartType,mPValueColumn,mTreeViewRadius,
									mFocusList,mCaseSeparationColumn,mCaseSeparationCategoryCount,mScaleMode, mUseAsFilterFlagNo,
									mTreeViewMode,mActiveExclusionFlags,mHVCount,mHVExclusionTag,mVisibleCategoryExclusionTag,
									mMarkerJitteringAxes,mGridMode;
	protected String				mPValueRefCategory,mWarningMessage;
	protected Random				mRandom;
	protected StereoMolecule		mLabelMolecule;
	protected Color					mLabelBackground;

	private Thread					mPopupThread;
	private CompoundListSelectionModel mSelectionModel;
	private int						mDragMode,mLocalExclusionFlagNo,mPreviousLocalExclusionFlagNo;
	private float[]					mPruningBarLow,mPruningBarHigh,mSimilarityMarkerSize;
	private int[][]					mVisibleCategoryFromCategory;
	private int[]					mCategoryMin,mCategoryMax,mCombinedCategoryCount;
	private Color					mViewBackground,mTitleBackground;
	private boolean					mApplyLocalExclusionScheduled,mSplitViewCountExceeded;
	private Polygon			 		mLassoRegion;
	private DetailPopupProvider		mDetailPopupProvider;

	public JVisualization(CompoundTableModel tableModel,
						  CompoundListSelectionModel selectionModel,
						  int dimensions) {
		mTableModel = tableModel;
		mSelectionModel = selectionModel;
		mDimensions = dimensions;

		addMouseListener(this);
		addMouseMotionListener(this);

		tableModel.addHighlightListener(this);
		selectionModel.addListSelectionListener(this);

		mPruningBarLow = new float[mDimensions];
		mPruningBarHigh = new float[mDimensions];

		mAxisVisMin = new float[mDimensions];
		mAxisVisMax = new float[mDimensions];
		mAxisVisRangeIsLogarithmic = new boolean[mDimensions];
		mAxisIndex = new int[mDimensions];
		mIsCategoryAxis = new boolean[mDimensions];
		mSplittingColumn = new int[mDimensions];
		mPreviousLocalExclusionFlagNo = -1;
		mLocalExclusionFlagNo = -1;
		mLocalAffectsGlobalExclusion = true;	// default
		mSuspendGlobalExclusion = true;
		mMarkerSizeZoomAdaption = 1.0f;

		mMarkerColor = new VisualizationColor(mTableModel, this);

		mLabelColumn = new int[MarkerLabelDisplayer.cPositionCode.length];

		mPreferredChartType = cChartTypeScatterPlot;
		mChartMode = cChartModeCount;
		mChartColumn = cColumnUnassigned;

		mCategoryMin = new int[mDimensions];
		mCategoryMax = new int[mDimensions];
		mCategoryVisibleCount = new int[mDimensions];
		mAxisSimilarity = new float[mDimensions][];

		mRandom = new Random();
		setToolTipText("");	// to switch on tool-tips
		}

	protected void initialize() {
		mRelativeFontSize = 1.0f;
		mRelativeMarkerSize = 1.0f;
		mMarkerSizeInversion = false;
		mMarkerSizeProportional = false;
		mMarkerSizeColumn = cColumnUnassigned;
		mMarkerShapeColumn = cColumnUnassigned;
		mMarkerJittering = 0.0f;
		mMarkerJitteringAxes = (mDimensions == 3) ? 7 : 3;
		mConnectionColumn = cColumnUnassigned;
		mConnectionOrderColumn = cColumnUnassigned;
		mRelativeConnectionLineWidth = 1.0f;
		mSplittingColumn[0] = cColumnUnassigned;
		mSplittingColumn[1] = cColumnUnassigned;
		mSplittingAspectRatio = 1.0f;
		mCaseSeparationColumn = cColumnUnassigned;
		mCaseSeparationValue = 0.5f;
		mPValueColumn = cColumnUnassigned;
		mBoxplotMeanMode = BOXPLOT_DEFAULT_MEAN_MODE;
		mSplitter = null;
		mShowEmptyInSplitView = true;
		mVisibleCategoryExclusionTag = -1;
		mHVExclusionTag = -1;
		mHighlightedPoint = null;
		mActivePoint = null;
		mCoordinatesValid = false;
		mLegendList = new ArrayList<JVisualizationLegend>();
		mSuppressLegend = false;
		mScaleMode = cScaleModeShown;
		mGridMode = cGridModeShown;
		mFocusList = cFocusNone;
		mLabelList = cLabelsOnAllRows;
		mShowLabelBackground = false;
		mLabelsInTreeViewOnly = false;
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++) {
			mLabelColumn[i] = cColumnUnassigned;
			}
		mMarkerLabelSize = 1.0f;
		mLabelBackgroundTransparency = DEFAULT_LABEL_TRANSPARENCY;
		for (int axis=0; axis<mDimensions; axis++) {
			mAxisIndex[axis] = cColumnUnassigned;
			initializeAxis(axis);
			}
		mShowNaNValues = true;
		mTitleBackground = DEFAULT_TITLE_BACKGROUND;
		mLabelBackground = DEFAULT_LABEL_BACKGROUND;
		mTreeViewMode = cTreeViewModeNone;
		mTreeViewRadius = 5;
		mTreeViewShowAll = true;
		mUseAsFilterFlagNo = -1;
		determineChartType();
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	public String getAxisTitle(int column) {
		return mTableModel.getColumnTitleExtended(column);
		}

/*	protected int getAxisType(int axis) {
		if (mAxisIndex[axis] == -1)
			return AXIS_TYPE_UNASSIGNED;
		return mIsCategoryAxis[axis] ? AXIS_TYPE_TEXT_CATEGORY : AXIS_TYPE_DOUBLE_VALUE;

		if (mAxisIndex[axis] != -1) {
			if (!mTableModel.isColumnTypeDouble(mAxisIndex[axis])
			 && !mTableModel.isDescriptorColumn(mAxisIndex[axis]))
				return AXIS_TYPE_TEXT_CATEGORY;
			if ((mChartType == cChartTypeBars
			  || mChartType == cChartTypePies
			  || ((mChartType == cChartTypeBoxPlot
				|| mChartType == cChartTypeWhiskerPlot)
			   && axis != mChartInfo.barAxis)
			  || (mChartType == cChartTypeScatterPlot
			   && mCaseSeparationColumn != cColumnUnassigned))
			  && mTableModel.isColumnTypeCategory(mAxisIndex[axis]))
				return AXIS_TYPE_DOUBLE_CATEGORY;
			else
				return AXIS_TYPE_DOUBLE_VALUE;
			}
		else if (mChartType == cChartTypeBars && mChartInfo.barAxis == axis) {
			return AXIS_TYPE_DOUBLE_VALUE;
			}
		return AXIS_TYPE_UNASSIGNED;
		}*/

	public void cleanup() {
		mTableModel.removeHighlightListener(this);
		mSelectionModel.removeListSelectionListener(this);
		if (mLocalExclusionFlagNo != -1)
			mTableModel.freeRowFlag(mLocalExclusionFlagNo);
		mLocalExclusionFlagNo = -1;
		}

	public int getDimensionCount() {
		return mDimensions;
		}

	/**
	 * Build up the VisualizationPoint array mPoint of all rows.
	 * Datapoint initialization needs to be done when a new view is created
	 * or when rows are removed or added.
	 * @param recycleExisting true if some rows stay the same
	 * @param afterDeletion true if rows where deleted
	 */
	public void initializeDataPoints(boolean recycleExisting, boolean afterDeletion) {
		mDataPoints = mTableModel.getTotalRowCount();

		VisualizationPoint[] existing = mPoint;
		mPoint = new VisualizationPoint[mDataPoints];

		if (!recycleExisting) {
			for (int i=0; i<mDataPoints; i++) {
				CompoundRecord record = mTableModel.getTotalRecord(i);
				mPoint[record.getID()] = createVisualizationPoint(record);
				}
			}
		else if (afterDeletion) {
			int index = 0;
			for (int i=0; i<existing.length; i++)
				if (existing[i].record.getID() != -1)
					mPoint[index++] = existing[i];
			}
		else {
			for (int i=0; i<existing.length; i++)
				mPoint[i] = existing[i];
			for (int i=existing.length; i<mDataPoints; i++)
				mPoint[i] = createVisualizationPoint(mTableModel.getTotalRecord(i));
			}

		updateActiveRow();
		}

	protected Point getLabelConnectionPoint(float px, float py, int lx1, int ly1, int lx2, int ly2) {
		if (px >= lx1 && px <= lx2 && py >= ly1 && py <= ly2)
			return null;
		Point p = new Point();
		p.x = (px <= lx1) ? lx1 : (px >= lx2) ? lx2 : (lx1+lx2)/2;
		p.y = (py <= ly1) ? ly1 : (py >= ly2) ? ly2 : (ly1+ly2)/2;
		return p;
		}

	protected void drawSelectionOutline(Graphics g) {
		g.setColor(VisualizationColor.cSelectedColor);
		if (mDragMode == DRAG_MODE_RECT_SELECT)
			g.drawRect((mMouseX1<mMouseX2) ? mMouseX1 : mMouseX2,
					   (mMouseY1<mMouseY2) ? mMouseY1 : mMouseY2,
					   Math.abs(mMouseX2-mMouseX1),
					   Math.abs(mMouseY2-mMouseY1));

		if (mDragMode == DRAG_MODE_LASSO_SELECT)
			g.drawPolygon(mLassoRegion);
		}

	protected abstract VisualizationPoint createVisualizationPoint(CompoundRecord record);
	protected abstract void updateHighlightedLabelPosition();
	public abstract int getAvailableShapeCount();
	public abstract int print(Graphics g, PageFormat f, int pageIndex);
	public abstract void paintHighResolution(Graphics2D g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting);

	// methods needed by JVisualizationLegend
	protected abstract int getStringWidth(String s);
	protected abstract void setFontHeight(int h);
	protected abstract void setColor(Color c);
	protected abstract void drawLine(int x1, int y1, int x2, int y2);
	protected abstract void drawRect(int x, int y, int w, int h);
	protected abstract void fillRect(int x, int y, int w, int h);
	protected abstract void drawMarker(Color color, int shape, int size, int x, int y);
	protected abstract void drawString(String s, int x, int y);
	protected abstract void drawMolecule(StereoMolecule mol, Color color, Rectangle2D.Double rect, int mode, int maxAVBL);

	protected void setActivePoint(VisualizationPoint vp) {
		mActivePoint = vp;

		if (mTreeViewMode != cTreeViewModeNone)
			updateTreeViewGraph();

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] != cColumnUnassigned && mTableModel.isDescriptorColumn(mAxisIndex[axis]))
				setSimilarityValues(axis, -1);

		updateSimilarityMarkerSizes(-1);

		repaint();
		}

	private void setHighlightedPoint(VisualizationPoint vp) {
		mHighlightedPoint = vp;
		repaint();
		}

	/**
	 * Determines whether radial chart view is selected and if conditions are such
	 * that a radial chart is shown when a current record is defined, i.e. we have
	 * connection lines defined by both a referencing and a referenced column.
	 * @return true radial chart view can be displayed
	 */
	public int getTreeViewMode() {
		if (mTreeViewMode != cTreeViewModeNone
		 && mConnectionColumn != cColumnUnassigned
		 && mConnectionColumn != cConnectionColumnConnectAll
		 && mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn)) != -1)
			return mTreeViewMode;
		return cTreeViewModeNone;
		}

	public int getTreeViewRadius() {
		return mTreeViewRadius;
		}

	public void setTreeViewMode(int mode, int radius, boolean showAll, boolean isDynamic, boolean isInverted) {
		if (mTreeViewMode != mode
		 || (mTreeViewMode != cTreeViewModeNone
		  && (mTreeViewRadius != radius
		   || mTreeViewShowAll != showAll
		   || mTreeViewIsDynamic != isDynamic
		   || mTreeViewIsInverted != isInverted))) {
			mTreeViewMode = mode;
			mTreeViewRadius = radius;
			mTreeViewShowAll = showAll;
			mTreeViewIsDynamic = isDynamic;
			mTreeViewIsInverted = isInverted;
			updateTreeViewGraph();
			}
		}

	public boolean isTreeViewDynamic() {
		return mTreeViewIsDynamic;
		}

	public boolean isTreeViewInverted() {
		return mTreeViewIsInverted;
	}

	public boolean isTreeViewShowAll() {
		return mTreeViewShowAll;
		}

	public boolean isTreeViewModeEnabled() {
		return getTreeViewMode() != cTreeViewModeNone;
		}

	public boolean isMarkerLabelsInTreeViewOnly() {
		return mLabelsInTreeViewOnly;
		}

	/**
	 * Determines whether currently a tree view is displayed, which includes
	 * empty tree views, if no root is chosen and not all rows are shown.
	 * @return
	 */
	protected boolean isTreeViewGraph() {
		return mTreeViewMode != cTreeViewModeNone
			&& (mActivePoint != null || !mTreeViewShowAll)
		   	&& mConnectionColumn != cColumnUnassigned
		   	&& mConnectionColumn != cConnectionColumnConnectAll
		   	&& mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn)) != -1;
		}

	protected int compareConnectionLinePoints(VisualizationPoint p1, VisualizationPoint p2) {
		if (p1.hvIndex != p2.hvIndex)
			return (p1.hvIndex < p2.hvIndex) ? -1 : 1;
		if (isCaseSeparationDone()) {
			float v1 = p1.record.getDouble(mCaseSeparationColumn);
			float v2 = p2.record.getDouble(mCaseSeparationColumn);
			if (v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		if (mConnectionColumn > cColumnUnassigned) {
			float v1 = p1.record.getDouble(mConnectionColumn);
			float v2 = p2.record.getDouble(mConnectionColumn);
			if (v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		int connectionOrderColumn = (mConnectionOrderColumn == cColumnUnassigned) ? mAxisIndex[0] : mConnectionOrderColumn;
		if (connectionOrderColumn != cColumnUnassigned) {
			float v1 = p1.record.getDouble(connectionOrderColumn);
			float v2 = p2.record.getDouble(connectionOrderColumn);
			if (v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		return 0;
		}

	protected boolean isConnectionLinePossible(VisualizationPoint p1, VisualizationPoint p2) {
		if (p1.hvIndex != p2.hvIndex
		 || (isCaseSeparationDone()
		  && p1.record.getDouble(mCaseSeparationColumn) != p2.record.getDouble(mCaseSeparationColumn))
		 || (mConnectionColumn != JVisualization.cConnectionColumnConnectAll
		  && p1.record.getDouble(mConnectionColumn) != p2.record.getDouble(mConnectionColumn)))
		  	return false;
		return true;
		}

	protected int getNextChangedConnectionLinePointIndex(int index1) {
		int index2 = index1+1;

		while (index2<mConnectionLinePoint.length
			&& compareConnectionLinePoints(mConnectionLinePoint[index1], mConnectionLinePoint[index2]) == 0)
			index2++;

		return index2;
		}

	private void updateTreeViewGraph() {
		boolean oldWasNull = (mTreeNodeList == null);
		mTreeNodeList = null;

		if (isTreeViewGraph()) {
			int referencedColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));
			int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
			float min = 0;
			float max = 0;
			float dif = 0;
			if (strengthColumn != -1) {
				min = mTableModel.getMinimumValue(strengthColumn);
				max = mTableModel.getMaximumValue(strengthColumn);
				if (max == min) {
					strengthColumn = -1;
					}
				else {
					min -= 0.2 * (max - min);
					dif = max - min;
					}
				}

			if (mConnectionLineMap == null)
				mConnectionLineMap = createReferenceMap(mConnectionColumn, referencedColumn);

	   		for (int i=0; i<mDataPoints; i++)
	   			mPoint[i].exclusionFlags |= EXCLUSION_FLAG_DETAIL_GRAPH;

			if (mActivePoint == null || (mTreeViewIsDynamic && !isVisibleInModel(mActivePoint))) {
				mTreeNodeList = new VisualizationNode[0][];
				}
			else {
		   		mActivePoint.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;

				VisualizationNode[] rootShell = new VisualizationNode[1];
				rootShell[0] = createVisualizationNode(mActivePoint, null, 1.0f);
	
				ArrayList<VisualizationNode[]> shellList = new ArrayList<VisualizationNode[]>();
				shellList.add(rootShell);

				boolean isReverseTree = mTreeViewIsInverted
						&& CompoundTableConstants.cColumnPropertyReferenceTypeTopDown.equals(
						mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType));
				if (mReverseConnectionMap == null && isReverseTree)
					mReverseConnectionMap = createReverseConnectionMap(mConnectionColumn);

				// create array lists for every shell
				for (int shell=1; shell<=mTreeViewRadius; shell++) {
					ArrayList<VisualizationNode> vpList = new ArrayList<VisualizationNode>();
					for (VisualizationNode parent:shellList.get(shell-1)) {
						if (isReverseTree) {
							LineConnection[] connection = mReverseConnectionMap.get(parent.getVisualizationPoint());
							if (connection != null) {
								int firstChildIndex = vpList.size();
								for (int i=0; i<connection.length; i++) {
									VisualizationPoint vp = connection[i].target;
									if (vp != null && (!mTreeViewIsDynamic || isVisibleInModel(vp))) {
										float strength = connection[i].strength;
										VisualizationNode childNode = null;

										// if we have a strength value and the child is already connected compare strength values
										if ((vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0) {
											for (int j=0; j<firstChildIndex; j++) {
												if (vpList.get(j).getVisualizationPoint() == vp) {
													if (vpList.get(j).getStrength() < strength) {
														childNode = vpList.get(j);
														vpList.remove(childNode);
														firstChildIndex--;
														childNode.setParentNode(parent);
														childNode.setStrength(strength);
														}
													break;
													}
												}
											if (childNode == null)
												continue;
											}
										else {
											vp.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
											childNode = createVisualizationNode(vp, parent, strength);
											}

										int insertIndex = firstChildIndex;
										while (insertIndex < vpList.size() && childNode.getStrength() <= vpList.get(insertIndex).getStrength())
											insertIndex++;

										vpList.add(insertIndex, childNode);
										}
									}
								}
							}
						else {
							byte[] data = (byte[])parent.getVisualizationPoint().record.getData(mConnectionColumn);
							if (data != null) {
								String[] entry = mTableModel.separateEntries(new String(data));
								String[] strength = null;
								if (strengthColumn != cColumnUnassigned) {
									byte[] strengthData = (byte[])parent.getVisualizationPoint().record.getData(strengthColumn);
									if (strengthData != null) {
										strength = mTableModel.separateEntries(new String(strengthData));
										if (strength.length != entry.length)
											strength = null;
										}
									}
								int firstChildIndex = vpList.size();
								for (int i=0; i<entry.length; i++) {
									String ref = entry[i];
									VisualizationPoint vp = mConnectionLineMap.get(ref.getBytes());
									if (vp != null && (!mTreeViewIsDynamic || isVisibleInModel(vp))) {
										// if we don't have connection strength information and the child is already connected to another parent
										if (strengthColumn == cColumnUnassigned && (vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0)
											continue;

										float strengthValue = 1.0f;
										if (strength != null) {
											try {
												float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[i], strengthColumn)));
												strengthValue = Float.isNaN(value) ? 0.0f : (float) ((value - min) / dif);
												}
											catch (NumberFormatException nfe) {}
											}

										VisualizationNode childNode = null;

										// if we have a strength value and the child is already connected compare strength values
										if ((vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0) {
											for (int j = 0; j < firstChildIndex; j++) {
												if (vpList.get(j).getVisualizationPoint() == vp) {
													if (vpList.get(j).getStrength() < strengthValue) {
														childNode = vpList.get(j);
														vpList.remove(childNode);
														firstChildIndex--;
														childNode.setParentNode(parent);
														childNode.setStrength(strengthValue);
														}
													break;
													}
												}
											if (childNode == null)
												continue;
											}
										else {
											vp.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
											childNode = createVisualizationNode(vp, parent, strengthValue);
											}

										int insertIndex = firstChildIndex;
										while (insertIndex < vpList.size() && childNode.getStrength() <= vpList.get(insertIndex).getStrength())
											insertIndex++;

										vpList.add(insertIndex, childNode);
										}
									}
								}
							}
						}
	
					if (vpList.size() == 0)
						break;
	
					shellList.add(vpList.toArray(new VisualizationNode[0]));
					}
	
				mTreeNodeList = shellList.toArray(new VisualizationNode[0][]);
				}
			}

		if (!oldWasNull && mTreeNodeList == null) {
	   		for (int i=0; i<mDataPoints; i++)
	   			mPoint[i].exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
			}

		if (!oldWasNull || mTreeNodeList != null) {
			// if we have a tree without any nodes (no chosen root) we don't want to hide invisible rows from other views
			mActiveExclusionFlags = (mTreeNodeList != null && mTreeNodeList.length == 0) ? 0 : EXCLUSION_FLAG_DETAIL_GRAPH;
			applyLocalExclusion(false);
			invalidateOffImage(true);
			}
		}

	private VisualizationNode createVisualizationNode(VisualizationPoint vp, VisualizationNode parent, float strength) {
		if (mTreeViewMode == cTreeViewModeHTree || mTreeViewMode == cTreeViewModeVTree)
			return new TreeVisualizationNode(vp, (TreeVisualizationNode)parent, strength);
		if (mTreeViewMode == cTreeViewModeRadial)
			return new RadialVisualizationNode(vp, (RadialVisualizationNode)parent, strength);
		return null;
		}

	protected void invalidateOffImage(boolean invalidateCoordinates) {
		if (invalidateCoordinates)
			mCoordinatesValid = false;
		mOffImageValid = false;
		repaint();
		}

	protected void calculateLegend(Rectangle bounds, int fontHeight) {
		mLegendList.clear();
		if (!mSuppressLegend) {
			int initialHeight = bounds.height;
			addLegends(bounds, fontHeight);

			// if we have less than 20% of the height then stepwise remove large legends to remedy
			int removalCount = 0;
			while (bounds.height < initialHeight / 5) {
				int largestLegendIndex = -1;
				int largestLegendHeight = 0;
				for (int i=0; i<mLegendList.size(); i++) {
					JVisualizationLegend legend = mLegendList.get(i);
					if (largestLegendHeight < legend.getHeight()) {
						largestLegendHeight = legend.getHeight();
						largestLegendIndex = i;
						}
					}
				bounds.height += largestLegendHeight;
				mLegendList.remove(largestLegendIndex);
				for (int i=largestLegendIndex; i<mLegendList.size(); i++)
					mLegendList.get(i).moveVertically(-largestLegendHeight);
				removalCount++;
				}
			if (removalCount != 0)
				mWarningMessage = "Reduce font size to show "+removalCount+" suppressed legend(s)!";
			}
		}

	protected void addLegends(Rectangle bounds, int fontHeight) {
		if (mMarkerSizeColumn != cColumnUnassigned
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			JVisualizationLegend sizeLegend = new JVisualizationLegend(this, mTableModel,
													mMarkerSizeColumn,
													null,
													JVisualizationLegend.cLegendTypeSize);
			sizeLegend.calculate(bounds, fontHeight);
			bounds.height -= sizeLegend.getHeight();
			mLegendList.add(sizeLegend);
			}

		boolean shareShapeAndColors = false;

		if (mMarkerShapeColumn != cColumnUnassigned
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies
			// if the marker color and marker shape encode the same categories, the use only one legend
		 && (mMarkerColor.getColorColumn() != mMarkerShapeColumn
		  || mMarkerColor.getColorListMode() != VisualizationColor.cColorListModeCategories)) {
			JVisualizationLegend shapeLegend = new JVisualizationLegend(this, mTableModel,
													mMarkerShapeColumn,
													null,
													JVisualizationLegend.cLegendTypeShapeCategory);
			shapeLegend.calculate(bounds, fontHeight);
			bounds.height -= shapeLegend.getHeight();
			mLegendList.add(shapeLegend);
			}

		if (mMarkerColor.getColorColumn() != cColumnUnassigned) {
			JVisualizationLegend colorLegend = new JVisualizationLegend(this, mTableModel,
													mMarkerColor.getColorColumn(),
													mMarkerColor,
													mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
													  JVisualizationLegend.cLegendTypeColorCategory
													: JVisualizationLegend.cLegendTypeColorDouble);
			colorLegend.calculate(bounds, fontHeight);
			bounds.height -= colorLegend.getHeight();
			mLegendList.add(colorLegend);
			}
		}

	protected void paintLegend(Rectangle bounds, boolean transparentBG) {
		for (JVisualizationLegend legend:mLegendList)
			legend.paint(bounds, transparentBG);
		}

	protected void paintTouchIcon(Graphics g) {
		if (mTouchFunctionActive) {
			g.setColor(Color.red);
			g.drawString("touch", 10, 20);
			}
		}

	public void setDetailPopupProvider(DetailPopupProvider p) {
		mDetailPopupProvider = p;
		}

	/**
	 * This returns the <b>indended</b> case separation column that was defined with
	 * setCaseSeparation(). Whether the view really separates cases by applying a case
	 * specific shift to markers, bars or boxes, depends on other view settings.
	 * This shift is not applied, if<br>
	 * - the categories are already separated, because the same column is also assigned to an axis.<br>
	 * - none of the axes is unassigned or assigned to another category column with a low number of categories.<br>
	 * Call isCaseSeparationDone() to find out, whether the view applies a case specific shift.
	 * @return
	 */
	public int getCaseSeparationColumn() {
		return mCaseSeparationColumn;
		}

	public float getCaseSeparationValue() {
		return mCaseSeparationValue;
		}

	/**
	 * Checks, whether a case separation column was defined and the view applies
	 * a case specific shift to all markers, bars or boxes.
	 * Case separation is not done, if the case separation column is also assigned to an axis.
	 * Case separation is not possible if we have no category axis or unassigned axis.
	 * @return true, if the view applies a case specific shift to markers, bars or boxes
	 */
	public boolean isCaseSeparationDone() {
		if (mCaseSeparationColumn == cColumnUnassigned)
			return false;

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] == mCaseSeparationColumn)
				return false;	// don't separate cases, if we have them separated on one axis anyway

		boolean categoryCountExceeded = false;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mAxisIndex[axis] == cColumnUnassigned)
				return true;
			if (mTableModel.isColumnTypeCategory(mAxisIndex[axis])) {
				if (mTableModel.getCategoryCount(mAxisIndex[axis]) <= cMaxCaseSeparationCategoryCount)
					return true;
				else
					categoryCountExceeded = true;
				}
			}

		if (categoryCountExceeded)
			mWarningMessage = "Too many categories on axis for case separation!";

		return false;
		}

	protected int getCaseSeparationAxis() {
		if (!isCaseSeparationDone())
			return -1;

		int preferredAxis = -1;
		int preferredCategoryCount = Integer.MAX_VALUE;
		for (int axis=0; axis<mDimensions; axis++) {
			if ((mChartType != cChartTypeBoxPlot
			  && mChartType != cChartTypeWhiskerPlot)
			 || ((BoxPlotViewInfo)mChartInfo).barAxis != axis) {
				int column = mAxisIndex[axis];
				if (column == cColumnUnassigned) {
					if (preferredCategoryCount > 1) {
						preferredAxis = axis;
						preferredCategoryCount = 1;
						}
					}
				else if (mTableModel.isColumnTypeCategory(column)
					  && mTableModel.getCategoryCount(column) <= cMaxCaseSeparationCategoryCount
					  && preferredCategoryCount > mTableModel.getCategoryCount(column)) {
					preferredAxis = axis;
					preferredCategoryCount = mTableModel.getCategoryCount(column);
					}
				}
			}
		return preferredAxis;
		}

	@Override
	public int getFocusList() {
		return mFocusList;
		}

	protected int getFocusFlag() {
		return (mFocusList == cFocusNone) ? -1
			 : (mFocusList == cFocusOnSelection) ? CompoundRecord.cFlagSelected
			 : mTableModel.getListHandler().getListFlagNo(mFocusList);
		}

	protected int getLabelFlag() {
		return (mLabelList == cLabelsOnAllRows) ? getFocusFlag()
				: (mLabelList == cFocusOnSelection) ? CompoundRecord.cFlagSelected
				: mTableModel.getListHandler().getListFlagNo(mLabelList);
		}

	public float getFontSize() {
		return mRelativeFontSize;
		}

	public int getMarkerShapeColumn() {
		return mMarkerShapeColumn;
		}

	public float getJittering() {
		return mMarkerJittering;
		}

	public int getJitterAxes() {
		return mMarkerJitteringAxes;
	}

	public float getMarkerSize() {
		return mRelativeMarkerSize;
		}

	public int getMarkerSizeColumn() {
		return mMarkerSizeColumn;
		}

	public boolean getMarkerSizeInversion() {
		return mMarkerSizeInversion;
		}

	public boolean getMarkerSizeProportional() {
		return mMarkerSizeProportional;
		}

	public boolean isMarkerSizeZoomAdapted() {
		return !Float.isNaN(mMarkerSizeZoomAdaption);
		}

	public float getMarkerLabelSize() {
		return mMarkerLabelSize;
		}

	/**
	 * Calculates the font size for drawing a marker label. Usually this depends on
	 * default font size, general font size factor, marker label font size factor.
	 * However, if we have a central label instead of a marker, and if the marker size column
	 * is set, then the central label's font size is defined by the general marker label size setting.
	 * @param vp
	 * @param position cMidCenter or other
	 * @return
	 */
	protected float getLabelFontSize(VisualizationPoint vp, int position, boolean isTreeView) {
		if (mMarkerSizeColumn != cColumnUnassigned
		 && position == cMidCenter
		 && !isTreeView) {
			float fontsize = getMarkerSize(vp) / 2.0f;
			float factor = 0.5f*(mMarkerLabelSize+1.0f); // reduce effect by factor 2.0
			return Math.max(3f, mRelativeFontSize*factor*fontsize);
			}

		return mMarkerLabelSize * mFontHeight;
		}

	/**
	 * Calculates the average bond length used for molecule scaling, if a label
	 * displays the chemical structure. This method is based on getLabelFontSize()
	 * and, thus, used the same logic.
	 * @param vp
	 * @param position cMidCenter or other
	 * @return
	 */
	protected float getLabelAVBL(VisualizationPoint vp, int position, boolean isTreeView) {
		return getLabelFontSize(vp, position, isTreeView) / 2f;
		}

	/**
	 * Calculates the absolute marker size of the visualization point, which depends
	 * on a view size specific base value (mAbsoluteMarkerSize), a user changeable factor
	 * (mRelativeMarkerSize) and optionally another factor derived from a column value
	 * defined to influence the marker size. It also includes the retina factor, but not
	 * the anti-aliasing factor.
	 * @param vp
	 * @return
	 */
	protected float getMarkerSize(VisualizationPoint vp) {
		if (mMarkerSizeColumn == cColumnUnassigned) {
			float size = Float.isNaN(mMarkerSizeZoomAdaption) ? mAbsoluteMarkerSize : mAbsoluteMarkerSize * mMarkerSizeZoomAdaption;
			return validateSizeWithConnections(size);
			}

		if (mTableModel.isDescriptorColumn(mMarkerSizeColumn)) {
			return getMarkerSizeFromDescriptorSimilarity(mSimilarityMarkerSize == null ? 0.2f : mSimilarityMarkerSize[vp.record.getID()]);
			}

		if (CompoundTableListHandler.isListColumn(mMarkerSizeColumn))
			return getMarkerSizeFromHitlistMembership(vp.record.isFlagSet(
				mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.getListFromColumn(mMarkerSizeColumn))));

		return getMarkerSizeFromValue(vp.record.getDouble(mMarkerSizeColumn));
		}

	protected float getMarkerSizeFromHitlistMembership(boolean isMember) {
		float size = (isMember ^ mMarkerSizeInversion) ?
				mAbsoluteMarkerSize * 1.2f : mAbsoluteMarkerSize * 0.6f;
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			size *= mMarkerSizeZoomAdaption;
		return validateSizeWithConnections(size);
		}

	protected float getMarkerSizeFromDescriptorSimilarity(float similarity) {
		float size = 2f * mAbsoluteMarkerSize * similarity;
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			size *= mMarkerSizeZoomAdaption;
		return validateSizeWithConnections(size);
		}

	protected float getMarkerSizeFromValue(float value) {
		float factor = getMarkerSizeVPFactor(value, mMarkerSizeColumn);
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			factor *= mMarkerSizeZoomAdaption;
		float size = (int)(factor*mAbsoluteMarkerSize);
		return validateSizeWithConnections(size);
		}

	/**
	 * Calculates the marker size factor from the absolute(!) row value normalized
	 * by the maximum of all absolute values. Returned is 2*sqrt(value/max)) assuming that
	 * the factor is used on two marker dimensions to make the marker area proportional
	 * to the value.
	 * @param value
	 * @param valueColumn
	 * @return 0.0 -> 2.0
	 */
	protected float getMarkerSizeVPFactor(float value, int valueColumn) {
		if (mMarkerSizeProportional) {
			float min = mTableModel.getMinimumValue(valueColumn);
			float max = mTableModel.getMaximumValue(valueColumn);
			if (mTableModel.isLogarithmicViewMode(valueColumn)) {
				max = (float)Math.pow(10, max);
				value = (float)Math.pow(10, value);
				}
			else {
				max = Math.max(Math.abs(min), Math.abs(max));
				value = Math.abs(value);
				}
			return 2f*(float)Math.sqrt((0.01+(mMarkerSizeInversion?max-value:value) / max) / 1.01f);
			}
		else {
			float min = mTableModel.getMinimumValue(valueColumn);
			float max = mTableModel.getMaximumValue(valueColumn);
			return 2f*(float)Math.sqrt((0.04+(mMarkerSizeInversion?max-value:value-min) / (max-min)) / 1.04f);
			}
		}

	/**
	 * Make marker size not smaller than 1.5 unless<br>
	 * - marker sizes are not modulated by a column value<br>
	 * - and connection lines are shown that are thicker than the marker<br>
	 * This allows reduce marker size to 0, if connection lines are shown.
	 * @param size updated size
	 * @return
	 */
	private float validateSizeWithConnections(float size) {
		if (size < 1.5) {
			if (mMarkerSizeColumn != cColumnUnassigned
			 || mConnectionColumn == cColumnUnassigned
			 || mAbsoluteConnectionLineWidth < size)
				size = 1.5f;
			}

		return size;
		}

	public void colorChanged(VisualizationColor source) {
		if (source == mMarkerColor) {
			updateColorIndices();
			}
		}

	protected boolean isLegendLayoutValid(JVisualizationLegend legend, VisualizationColor color) {
		if (legend == null || color.getColorColumn() == cColumnUnassigned)
			return legend == null && color.getColorColumn() == cColumnUnassigned;

		return legend.layoutIsValid(color.getColorListMode() == VisualizationColor.cColorListModeCategories, color.getColorListSizeWithoutDefaults());
		}

	public VisualizationColor getMarkerColor() {
		return mMarkerColor;
		}

	private void updateColorIndices() {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = VisualizationColor.cDefaultDataColorIndex;
			}
		else if (CompoundTableListHandler.isListColumn(mMarkerColor.getColorColumn())) {
			int listIndex = CompoundTableListHandler.getListFromColumn(mMarkerColor.getColorColumn());
			int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = (byte)(mPoint[i].record.isFlagSet(flagNo) ?
						VisualizationColor.cSpecialColorCount : VisualizationColor.cSpecialColorCount + 1);
			}
		else if (mTableModel.isDescriptorColumn(mMarkerColor.getColorColumn())) {
			setSimilarityColors(-1);
			}
		else if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = (byte)(VisualizationColor.cSpecialColorCount
					+ mTableModel.getCategoryIndex(mMarkerColor.getColorColumn(), mPoint[i].record));
			}
		else if (mTableModel.isColumnTypeDouble(mMarkerColor.getColorColumn())) {
			float min = Float.isNaN(mMarkerColor.getColorMin()) ?
									mTableModel.getMinimumValue(mMarkerColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mMarkerColor.getColorColumn())) ?
							   (float)Math.log10(mMarkerColor.getColorMin()) : mMarkerColor.getColorMin();
			float max = Float.isNaN(mMarkerColor.getColorMax()) ?
									mTableModel.getMaximumValue(mMarkerColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mMarkerColor.getColorColumn())) ?
							   (float)Math.log10(mMarkerColor.getColorMax()) : mMarkerColor.getColorMax();

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as indented
			if (min >= max)  
				if (!Float.isNaN(mMarkerColor.getColorMax()))
					min = Float.MIN_VALUE;

			for (int i=0; i<mDataPoints; i++) {
				float value = mPoint[i].record.getDouble(mMarkerColor.getColorColumn());
				if (Float.isNaN(value))
					mPoint[i].colorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (value <= min)
					mPoint[i].colorIndex = (byte)VisualizationColor.cSpecialColorCount;
				else if (value >= max)
					mPoint[i].colorIndex = (byte)(mMarkerColor.getColorList().length-1);
				else
					mPoint[i].colorIndex = (byte)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mMarkerColor.getColorList().length-VisualizationColor.cSpecialColorCount-1)
						* (value - min) / (max - min));
				}
			}

		invalidateOffImage(true);
		}

	/**
	 * @param axis
	 * @param rowID if != -1 then update this row only
	 */
	private void setSimilarityValues(int axis, int rowID) {
		int column = mAxisIndex[axis];
		if (mActivePoint == null) {
			mAxisSimilarity[axis] = null;
			}
		else if (rowID != -1) {
			for (int i=0; i<mDataPoints; i++) {
				if (mPoint[i].record.getID() == rowID) {
					mAxisSimilarity[axis][rowID] =	(float)mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
					break;
					}
				}
			}
		else {
			if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(column))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				mAxisSimilarity[axis] = null;
				Object descriptor = mActivePoint.record.getData(column);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					String idcode = new String((byte[])mActivePoint.record.getData(mTableModel.getParentColumn(column)));
					mAxisSimilarity[axis] = createSimilarityListSMP(idcode, descriptor, column);
					}
				}
			else {
				mAxisSimilarity[axis] = new float[mDataPoints];
				for (int i=0; i<mDataPoints; i++)
					mAxisSimilarity[axis][mPoint[i].record.getID()] =
						(float)mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
				}
			}
		invalidateOffImage(true);
		}

	/**
	 * @param rowID if != -1 then update this row only
	 */
	private void updateSimilarityMarkerSizes(int rowID) {
		if (mActivePoint == null
		 || !mTableModel.isDescriptorColumn(mMarkerSizeColumn)) {
			mSimilarityMarkerSize = null;
			}
		else if (rowID != -1) {
			for (int i=0; i<mDataPoints; i++) {
				if (mPoint[i].record.getID() == rowID) {
					mSimilarityMarkerSize[rowID] =	(float)mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, mMarkerSizeColumn);
					break;
					}
				}
			}
		else {
			if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(mMarkerSizeColumn))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				mSimilarityMarkerSize = null;
				Object descriptor = mActivePoint.record.getData(mMarkerSizeColumn);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					String idcode = new String((byte[])mActivePoint.record.getData(mTableModel.getParentColumn(mMarkerSizeColumn)));
					mSimilarityMarkerSize = createSimilarityListSMP(idcode, descriptor, mMarkerSizeColumn);
					}
				}
			else {
				mSimilarityMarkerSize = new float[mDataPoints];
				for (int i=0; i<mDataPoints; i++)
					mSimilarityMarkerSize[mPoint[i].record.getID()] =
						(float)mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, mMarkerSizeColumn);
				}
			}
		}

	/**
	 * @param rowID if != -1 then update this row only
	 */
	private void setSimilarityColors(int rowID) {
		if (mActivePoint == null) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = VisualizationColor.cDefaultDataColorIndex;
			}
		else {
			float min = Float.isNaN(mMarkerColor.getColorMin()) ? 0.0f : mMarkerColor.getColorMin();
			float max = Float.isNaN(mMarkerColor.getColorMax()) ? 1.0f : mMarkerColor.getColorMax();
			if (min >= max) {
				min = 0.0f;
				max = 1.0f;
				}

			int column = mMarkerColor.getColorColumn();
			float[] flexophoreSimilarity = null;
			if (rowID == -1
			 && DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(column))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				Object descriptor = mActivePoint.record.getData(column);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					String idcode = new String((byte[])mActivePoint.record.getData(mTableModel.getParentColumn(column)));
					flexophoreSimilarity = createSimilarityListSMP(idcode, descriptor, column);
					if (flexophoreSimilarity == null) {	// cancelled
						mMarkerColor.setColor(cColumnUnassigned);
						return;
						}
					}
				}

			for (int i=0; i<mDataPoints; i++) {
				if (rowID == -1 || mActivePoint.record.getID() == rowID) {
					float similarity = (flexophoreSimilarity != null) ? flexophoreSimilarity[i]
									  : mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
					if (Float.isNaN(similarity))
						mPoint[i].colorIndex = VisualizationColor.cMissingDataColorIndex;
					else if (similarity <= min)
						mPoint[i].colorIndex = (byte)VisualizationColor.cSpecialColorCount;
					else if (similarity >= max)
						mPoint[i].colorIndex = (byte)(mMarkerColor.getColorList().length-1);
					else
						mPoint[i].colorIndex = (byte)(0.5 + VisualizationColor.cSpecialColorCount
							+ (float)(mMarkerColor.getColorList().length - VisualizationColor.cSpecialColorCount - 1)
							* (similarity - min) / (max - min));
					}
				}
			}
		}

	private float[] createSimilarityListSMP(String idcode, Object descriptor, int descriptorColumn) {
		float[] similarity = mTableModel.getSimilarityListFromCache(idcode, descriptorColumn);
		if (similarity != null)
			return similarity;

		Component c = this;
		while (!(c instanceof Frame))
			c = c.getParent();

		JProgressDialog progressDialog = new JProgressDialog((Frame)c) {
			private static final long serialVersionUID = 0x20110325;

			public void stopProgress() {
				super.stopProgress();
				close();
				}
			};

	   	mTableModel.createSimilarityListSMP(null, descriptor, idcode, descriptorColumn, progressDialog, false);

   		progressDialog.setVisible(true);
   		similarity = mTableModel.getSimilarityListSMP();

		return similarity;
 		}

	/**
	 * Calculates for the given category column, which of its categories have at least one visible
	 * member in this view. To do so, this method updates mVisibleCategoryFromCategory[column].
	 * @param column valid category column
	 * @return
	 */
	protected int getVisibleCategoryCount(int column) {
		if (CompoundTableListHandler.isListColumn(column))
			return 2;	// don't handle lists here

		if (mVisibleCategoryFromCategory == null)
			mVisibleCategoryFromCategory = new int[mTableModel.getTotalColumnCount()][];

		// If the tableModel's row visibility was changed since the last generation of visible categories
		// then we have to freshly generate.
		if (mVisibleCategoryExclusionTag != mTableModel.getExclusionTag()) {
			mVisibleCategoryExclusionTag = mTableModel.getExclusionTag();
			mVisibleCategoryFromCategory[column] = null;
			}

		if (mVisibleCategoryFromCategory[column] == null) {
			int categoryCount = mTableModel.getCategoryCount(column);
			int count = 0;
			boolean[] isVisibleCategory = new boolean[categoryCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisible(mPoint[i])) {
					int category = mTableModel.getCategoryIndex(column, mPoint[i].record);
					if (!isVisibleCategory[category]) {
						isVisibleCategory[category] = true;
						if (++count == categoryCount)
							break;
						}
					}
				}

			mVisibleCategoryFromCategory[column] = new int[categoryCount];
			int visibleCategory = 0;
			for (int i=0; i<categoryCount; i++)
				mVisibleCategoryFromCategory[column][i] = isVisibleCategory[i] ? visibleCategory++ : -1;

			return count;
			}

		int count = 0;
		for (int i=0; i<mVisibleCategoryFromCategory[column].length; i++)
			if (mVisibleCategoryFromCategory[column][i] != -1)
				count++;
		return count;
		}

	/**
	 * Creates a list of those categories within the given column,
	 * which have at least one visible member in this view.
	 * @param column
	 * @return
	 */
	protected String[] getVisibleCategoryList(int column) {
		String[] category = mTableModel.getCategoryList(column);
		String[] visibleCategory = new String[getVisibleCategoryCount(column)];
		for (int i=0; i<mVisibleCategoryFromCategory[column].length; i++)
			if (mVisibleCategoryFromCategory[column][i] != -1)
				visibleCategory[mVisibleCategoryFromCategory[column][i]] = category[i];
		return visibleCategory;
		}

	private void invalidateSplittingIndices() {
		mHVExclusionTag = -1;
		invalidateOffImage(true);
		}

	/**
	 * Updates all rows assignments (hv-indexes) to split views, if they are invalid.
	 * @return true, if these assignments were updated.
	 */
	protected boolean validateSplittingIndices() {
		if (mHVExclusionTag == mTableModel.getExclusionTag())
			return false;

		mHVExclusionTag = mTableModel.getExclusionTag();

		int count1 = (mSplittingColumn[0] == cColumnUnassigned) ? 1 : mShowEmptyInSplitView ?
				mTableModel.getCategoryCount(mSplittingColumn[0]) : getVisibleCategoryCount(mSplittingColumn[0]);
		int count2 = (mSplittingColumn[1] == cColumnUnassigned) ? 1 : mShowEmptyInSplitView ?
				mTableModel.getCategoryCount(mSplittingColumn[1]) : getVisibleCategoryCount(mSplittingColumn[1]);
		mHVCount = Math.max(0, count1 * count2);

		mSplitViewCountExceeded = (mHVCount > cMaxSplitViewCount);
		if (mSplitViewCountExceeded) {
			mHVCount = 1;
			mWarningMessage = "The view is not split into individual views, because there would be too many of them!";
			}

		if (mHVCount == 1) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].hvIndex = 0;
			}
		else if (mSplittingColumn[1] == cColumnUnassigned) {
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[0])) {
				int flagNo = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.getListFromColumn(mSplittingColumn[0]));
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = mPoint[i].record.isFlagSet(flagNo) ? 0 : 1;
				}
			else if (mShowEmptyInSplitView) {
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = mTableModel.getCategoryIndex(mSplittingColumn[0], mPoint[i].record);
				}
			else {
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = mVisibleCategoryFromCategory[mSplittingColumn[0]][mTableModel.getCategoryIndex(mSplittingColumn[0], mPoint[i].record)];
				}
			}
		else {
			int flagNo1 = -1;
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[0]))
				flagNo1 = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.getListFromColumn(mSplittingColumn[0]));

			int flagNo2 = -1;
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[1]))
				flagNo2 = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.getListFromColumn(mSplittingColumn[1]));

			for (int i=0; i<mDataPoints; i++) {
				CompoundRecord record = mPoint[i].record;
				int index1 = (flagNo1 != -1) ? (record.isFlagSet(flagNo1) ? 0 : 1)
							: mShowEmptyInSplitView ? mTableModel.getCategoryIndex(mSplittingColumn[0], record)
							: mVisibleCategoryFromCategory[mSplittingColumn[0]][mTableModel.getCategoryIndex(mSplittingColumn[0], record)];
				if (index1 == -1) {
					mPoint[i].hvIndex = -1;
					continue;
					}
				int index2 = (flagNo2 != -1) ? (record.isFlagSet(flagNo2) ? 0 : 1)
							: mShowEmptyInSplitView ? mTableModel.getCategoryIndex(mSplittingColumn[1], record)
							: mVisibleCategoryFromCategory[mSplittingColumn[1]][mTableModel.getCategoryIndex(mSplittingColumn[1], record)];
				if (index2 == -1) {
					mPoint[i].hvIndex = -1;
					continue;
				}
				mPoint[i].hvIndex = index1 + index2 * count1;
				}
			}

		return true;
		}

	public Color getTitleBackground() {
		return mTitleBackground;
		}

	public void setTitleBackground(Color c) {
		if (!mTitleBackground.equals(c)) {
			mTitleBackground = c;
			invalidateOffImage(false);
			}
		}

	public Color getViewBackground() {
		return (mViewBackground == null) ? Color.WHITE : mViewBackground;
		}

	/**
	 * Defines the background color
	 * @param c (null for WHITE)
	 * @return whether there was a change
	 */
	public boolean setViewBackground(Color c) {
		if (Color.WHITE.equals(c))
			c = null;
		if ((c != null && !c.equals(mViewBackground))
		 || (c == null && mViewBackground != null)) {
			mViewBackground = c;
			invalidateOffImage(false);
			return true;
			}
		return false;
		}

	public Color getLabelBackground() {
		return mLabelBackground;
		}

	/**
	 * Defines whether to show background area and which color of the rectangular areas behind labels
	 * @param c color of background area
	 * @return whether there was a change
	 */
	public boolean setLabelBackground(Color c) {
		if (!c.equals(mLabelBackground)) {
			mLabelBackground = c;
			if (showAnyLabels())
				invalidateOffImage(false);
			return true;
			}
		return false;
		}

	@Override
	public boolean isShowLabelBackground() {
		return mShowLabelBackground;
		}

	/**
	 * Defines whether to show a rectangular background area behind any labels
	 * @param b whether to show a rectangular background
	 */
	public void setShowLabelBackground(boolean b) {
		if (mShowLabelBackground != b) {
			mShowLabelBackground = b;
			if (showAnyLabels())
				invalidateOffImage(false);
			}
		}

	@Override
	public float getLabelTransparency() {
		return mLabelBackgroundTransparency;
		}

	@Override
	public void setLabelTransparency(float transparency, boolean isAdjusting) {
		if (mLabelBackgroundTransparency != transparency) {
			mLabelBackgroundTransparency = transparency;
			if (showAnyLabels())
				invalidateOffImage(false);
			}
		}

	/**
	 * Generates a neutral grey with given contrast to the current background.
	 * @param contrast 0.0 (not visible) to 1.0 (full contrast)
	 * @return
	 */
	protected Color getContrastGrey(float contrast) {
		return getContrastGrey(contrast, mViewBackground);
		}

	/**
	 * Generates a neutral grey with given contrast to given color.
	 * @param contrast 0.0 (not visible) to 1.0 (full contrast)
	 * @return
	 */
	protected Color getContrastGrey(float contrast, Color color) {
		float brightness = ColorHelper.perceivedBrightness(color);
		float range = (brightness > 0.5) ? brightness : 1f-brightness;

		// enhance contrast for middle bright backgrounds
		contrast = (float)Math.pow(contrast, range);

		return (brightness > 0.5) ?
				  Color.getHSBColor(0.0f, 0.0f, brightness - range*contrast)
				: Color.getHSBColor(0.0f, 0.0f, brightness + range*contrast);
		}

	@Override
	public void setFocusHitlist(int listIndex) {
		if (mFocusList != listIndex) {
			mFocusList = listIndex;
			invalidateOffImage(mChartType != cChartTypeScatterPlot);	// no of colors in mChartInfo needs to be updated
			}
		}

	/**
	 * This is the user defined relative font size factor applied to marker and scale labels.
	 * Note: Marker labels are also affected by setMarkerLabelSize().
	 * @param size
	 * @param isAdjusting
	 */
	public void setFontSize(float size, boolean isAdjusting) {
		if (mRelativeFontSize != size) {
			mRelativeFontSize = size;
			invalidateOffImage(true);
			}
		}

	/**
	 * This defines the amount of random displacement on one,two or three axes
	 * @param jittering relative displacement amount
	 * @param axes combination of 1,2,4 for x,y,z axes
	 * @param isAdjusting
	 */
	public void setJittering(float jittering, int axes, boolean isAdjusting) {
		if (axes == 0)
			axes = (mDimensions == 3) ? 7 : 3;
		if (mMarkerJittering != jittering || mMarkerJitteringAxes != axes) {
			mMarkerJittering = jittering;
			mMarkerJitteringAxes = axes;
			invalidateOffImage(true);
			}
		}

	public void setCaseSeparation(int column, float value, boolean isAdjusting) {
		if (column >= 0
		 && (!mTableModel.isColumnTypeCategory(column)
		  || mTableModel.getCategoryCount(column) > cMaxCaseSeparationCategoryCount))
			column = cColumnUnassigned;

		if (mCaseSeparationColumn != column
		 || (mCaseSeparationColumn != cColumnUnassigned && mCaseSeparationValue != value)) {
			mCaseSeparationColumn = column;
			mCaseSeparationValue = value;
		   	validateExclusion(determineChartType());
			invalidateOffImage(true);
			}
		}

	/**
	 * Returns whether the user selected one or two category columns for view splitting.
	 * If columns are selected, but the vast number of categories is preventing splitting,
	 * then this returns true!
	 * @return whether the view is configured for view splitting
	 */
	public boolean isSplitViewConfigured() {
		return mSplittingColumn[0] != cColumnUnassigned;
		}

	/**
	 * Returns whether the user selected one or two category columns for view splitting
	 * and if the number of view categories don't exceed the maximum for rendering.
	 * @return whether split views are rendered
	 */
	protected boolean isSplitView() {
		return mSplittingColumn[0] != cColumnUnassigned && !mSplitViewCountExceeded;
		}

	public boolean isShowEmptyInSplitView() {
		return mShowEmptyInSplitView;
		}

	public int[] getSplittingColumns() {
		return mSplittingColumn;
		}

	public float getSplittingAspectRatio() {
		return mSplittingAspectRatio;
		}

	public void setSplittingColumns(int column1, int column2, float aspectRatio, boolean showEmptyViews) {
		if (column1 == cColumnUnassigned
		 && column2 != cColumnUnassigned) {
			column1 = column2;
			column2 = cColumnUnassigned;
			}

		if (mSplittingColumn[0] != column1
		 || mSplittingColumn[1] != column2
		 || (mSplittingColumn[0] != cColumnUnassigned && mSplittingAspectRatio != aspectRatio)
		 || (mSplittingColumn[0] != cColumnUnassigned && mShowEmptyInSplitView != showEmptyViews)) {
			if ((column1 == cColumnUnassigned
			  || mTableModel.isColumnTypeCategory(column1))
			 && (column2 == cColumnUnassigned
			  || mTableModel.isColumnTypeCategory(column2))) {
				mSplittingColumn[0] = column1;
				mSplittingColumn[1] = column2;
				mSplittingAspectRatio = aspectRatio;
				mShowEmptyInSplitView = showEmptyViews;
				invalidateSplittingIndices();
				}
			}
		}

	public boolean supportsShowMeanAndMedian() {
		if (mChartType == cChartTypeBars
		 || mChartType == cChartTypePies)
			return mChartMode != cChartModePercent
				|| mChartMode != cChartModeCount;
		return mChartType == cChartTypeBoxPlot
			|| mChartType == cChartTypeWhiskerPlot;
		}

	public boolean supportsShowStdDevAndErrorMergin() {
		if (mChartType == cChartTypeBars
		 || mChartType == cChartTypePies)
			return mChartMode != cChartModePercent
				|| mChartMode != cChartModeCount;
		return mChartType == cChartTypeBoxPlot
			|| mChartType == cChartTypeWhiskerPlot;
		}

	public boolean supportsShowValueCount() {
		return mChartType == cChartTypeBars
			|| mChartType == cChartTypePies
			|| mChartType == cChartTypeBoxPlot
			|| mChartType == cChartTypeWhiskerPlot;
		}

	/** Determine current mean/median setting for box and whisker plots
	 * @return mode or BOXPLOT_DEFAULT_MEAN_MODE if not box/whisker plot
	 */
	public int getBoxplotMeanMode() {
		return (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) ? mBoxplotMeanMode : BOXPLOT_DEFAULT_MEAN_MODE;
		}

	/**
	 * Defines whether mean and/or median are have indicators in box/whisker plots.
	 * @param mode
	 */
	public void setBoxplotMeanMode(int mode) {
		if (mBoxplotMeanMode != mode) {
			mBoxplotMeanMode = mode;
			invalidateOffImage(false);
			}
		}

	/** Determines whether mean and/or median values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot. 
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowMeanAndMedianValues() {
		return supportsShowMeanAndMedian()
			&& mShowMeanAndMedianValues;
		}

	/**
	 * Defines whether mean and/or median are shown in box/whisker plots.
	 * @param b
	 */
	public void setShowMeanAndMedianValues(boolean b) {
		if (mShowMeanAndMedianValues != b) {
			mShowMeanAndMedianValues = b;
			if (supportsShowMeanAndMedian())
				invalidateOffImage(true);
			}
		}

	/**
	 * Determines whether the standard deviation is shown in a current box or whisker plot.
	 * @return true if current graph box or whisker plot and standard deviation is shown
	 */
	public boolean isShowStandardDeviation() {
		return supportsShowStdDevAndErrorMergin()
			  && mShowStandardDeviation;
		}

	/**
	 * Sets whether the standard deviation is shown in box/whisker plots.
	 * @param b
	 */
	public void setShowStandardDeviation(boolean b) {
		if (mShowStandardDeviation != b) {
			mShowStandardDeviation = b;
			if (supportsShowStdDevAndErrorMergin())
				invalidateOffImage(true);
			}
		}

	/**
	 * Determines whether the 95% confidence interval is shown in a current box or whisker plot.
	 * @return true if current graph box or whisker plot and standard deviation is shown
	 */
	public boolean isShowConfidenceInterval() {
		return supportsShowStdDevAndErrorMergin()
			  && mShowConfidenceInterval;
		}

	/**
	 * Sets whether the 95% confidence interval is shown in box/whisker plots.
	 * @param b
	 */
	public void setShowConfidenceInterval(boolean b) {
		if (mShowConfidenceInterval != b) {
			mShowConfidenceInterval = b;
			if (supportsShowStdDevAndErrorMergin())
				invalidateOffImage(true);
			}
		}

	/**
	 * Determines whether the value count N is shown in a current box or whisker plot.
	 * @return true if current graph box or whisker plot and standard deviation is shown
	 */
	public boolean isShowValueCount() {
		return supportsShowValueCount()
			&& mShowValueCount;
		}

	/**
	 * Sets whether the value count N is shown in box/whisker plots.
	 * @param b
	 */
	public void setShowValueCount(boolean b) {
		if (mShowValueCount != b) {
			mShowValueCount = b;
			if (supportsShowValueCount())
				invalidateOffImage(true);
			}
		}

	/** Determines whether p-values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot
	 *  or if no proper p-value column is assigned.
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowPValue() {
		return (mChartType == cChartTypeBoxPlot
	   		 || mChartType == cChartTypeWhiskerPlot)
	   		&& mBoxplotShowPValue
   			&& isValidPValueColumn(mPValueColumn);
   		}

	/**
	 * Defines whether p-values are shown in box/whisker plots.
	 * For showing p-values one also needs to call setPValueColumn()
	 * @param b
	 */
	public void setShowPValue(boolean b) {
		if (mBoxplotShowPValue != b) {
			mBoxplotShowPValue = b;
			if (isValidPValueColumn(mPValueColumn))
				invalidateOffImage(true);	// to recalculate p-values
			}
		}

	/** Determines whether fold-change values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot
	 *  or if no proper p-value column is assigned.
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowFoldChange() {
		return (mChartType == cChartTypeBoxPlot
	   		 || mChartType == cChartTypeWhiskerPlot)
	   		&& mBoxplotShowFoldChange
   			&& isValidPValueColumn(mPValueColumn);
   		}

	/**
	 * Defines whether fold-change values are shown in box/whisker plots.
	 * For showing fold-change values one also needs to call setPValueColumn()
	 * @param b
	 */
	public void setShowFoldChange(boolean b) {
		if (mBoxplotShowFoldChange != b) {
			mBoxplotShowFoldChange = b;
			if (isValidPValueColumn(mPValueColumn))
				invalidateOffImage(true);	// to recalculate fold change
			}
		}

	/**
	 * Returns the currently applied p-value column.
	 * @return
	 */
	public int getPValueColumn() {
		return isValidPValueColumn(mPValueColumn) ? mPValueColumn : cColumnUnassigned;
		}

	/**
	 * Returns the currently applied reference category for p-value calculation.
	 * @return
	 */
	public String getPValueRefCategory() {
		return (getPValueColumn() == cColumnUnassigned) ? null : mPValueRefCategory;
		}

	public boolean isValidPValueColumn(int column) {
		if (column == cColumnUnassigned)
			return false;

		if (mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot) {
			if (column == getCaseSeparationColumn())
				return true;

			if (isSplitView()
			 && (column == mSplittingColumn[0]
			  || column == mSplittingColumn[1]))
				return true;

			for (int axis=0; axis<mDimensions; axis++)
				if (column == mAxisIndex[axis]
				 && mCategoryVisibleCount[axis] >= 2
				 && axis != mChartInfo.barAxis)
					return true;
			}

		return false;
		}

	public void setPValueColumn(int column, String refCategory) {
		if (column == cColumnUnassigned)
			refCategory = null;
		else if (refCategory == null)
			column = cColumnUnassigned;

		if (column != mPValueColumn
		 || (refCategory != null && !refCategory.equals(mPValueRefCategory))) {
			mPValueColumn = column;
			mPValueRefCategory = refCategory;
			invalidateOffImage(true);
			}
		}

	/**
	 * In fast rendering mode anti-aliasing is switched off
	 * @return whether we are in fast render mode
	 */
	public boolean isFastRendering() {
		return mIsFastRendering;
		}

	/**
	 * In fast rendering mode anti-aliasing is switched off
	 * @param v
	 */
	public void setFastRendering(boolean v) {
		if (mIsFastRendering != v) {
			mIsFastRendering = v;
			invalidateOffImage(false);
			}
		}

	/**
	 * Determines the type of the drawn chart and visible ranges on all axes.
	 * This depends on the preferred chart type, on the columns being assigned
	 * to the axes, the number of existing categories within these columns,
	 * and the current pruning bar settings.
	 * If any of these change, then this method needs to be called.
	 * @return local exclusion update needs (EXCLUSION_FLAG_ZOOM_0 left shifted according to axis)
	 */
	private int determineChartType() {
		boolean[] wasCatagoryAxis = new boolean[mDimensions];
		for (int axis=0; axis<mDimensions; axis++)
			wasCatagoryAxis[axis] = mIsCategoryAxis[axis];

		mChartType = cChartTypeScatterPlot;	// scatter plot is the default that is always possible
		for (int axis=0; axis<mDimensions; axis++)
			mIsCategoryAxis[axis] = (mAxisIndex[axis] != cColumnUnassigned
								  && mTableModel.isColumnTypeCategory(mAxisIndex[axis])
								  && !mTableModel.isColumnTypeDouble(mAxisIndex[axis]));

		if (mPreferredChartType == cChartTypeScatterPlot) {
			int csAxis = getCaseSeparationAxis();
			if (csAxis != -1)
				mIsCategoryAxis[csAxis] = true;
			}
		else if (mPreferredChartType == cChartTypeBoxPlot
			  || mPreferredChartType == cChartTypeWhiskerPlot) {
			int boxPlotDoubleAxis = determineBoxPlotDoubleAxis();
			if (boxPlotDoubleAxis != -1) {
				mChartType = mPreferredChartType;
				for (int axis=0; axis<mDimensions; axis++)
					mIsCategoryAxis[axis] = (axis != boxPlotDoubleAxis);
				}
			}
		else if (mPreferredChartType == cChartTypeBars
			  || mPreferredChartType == cChartTypePies) {
			boolean allQualify = true;
			for (int axis=0; axis<mDimensions; axis++)
				if (!qualifiesAsChartCategory(axis))
					allQualify = false;

			if (allQualify) {
				int categoryCount = 1;
				for (int axis=0; axis<mDimensions; axis++)
					if (mAxisIndex[axis] != cColumnUnassigned)
						categoryCount *= mTableModel.getCategoryCount(mAxisIndex[axis]);

				if (categoryCount <= cMaxTotalChartCategoryCount) {
					mChartType = mPreferredChartType;
					for (int axis=0; axis<mDimensions; axis++)
						mIsCategoryAxis[axis] = (mAxisIndex[axis] != cColumnUnassigned);
					}
				}
			}

		if (mTreeNodeList == null) {			// no tree view
			mActiveExclusionFlags = 0xFF;	// masks out NaN flags for axes that show categories
			for (int axis=0; axis<mDimensions; axis++)
				if (mIsCategoryAxis[axis])
					mActiveExclusionFlags &= ~(byte)(EXCLUSION_FLAG_NAN_0 << axis);

			updateBarAndPieNaNExclusion();
			}
		else if (mTreeNodeList.length == 0) {	// we have an empty tree view
			mActiveExclusionFlags = 0;
			}
		else {									// tree view with at least one node
			mActiveExclusionFlags = EXCLUSION_FLAG_DETAIL_GRAPH;
			}

		int localExclusionNeeds = 0;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mIsCategoryAxis[axis] != wasCatagoryAxis[axis])
				localExclusionNeeds |= (EXCLUSION_FLAG_ZOOM_0 << axis);

			calculateVisibleRange(axis);
			}

		return localExclusionNeeds;
		}

	protected void determineWarningMessage() {
		if (mChartType != mPreferredChartType) {
			String preferred = mPreferredChartType == cChartTypeBars ? "bar chart"
							 : mPreferredChartType == cChartTypePies ? "pie chart"
							 : mPreferredChartType == cChartTypeBoxPlot ? "box plot"
							 : mPreferredChartType == cChartTypeWhiskerPlot ? "whisker plot" : "scatter plot";

			if (mPreferredChartType == cChartTypeBars
			 || mPreferredChartType == cChartTypePies) {
				for (int axis=0; axis<mDimensions; axis++) {
					if (!qualifiesAsChartCategory(axis)) {
						mWarningMessage = "A " + preferred + " is not shown, because an axis is assigned to a column that does not contain categories!";
						return;
						}
					}
				mWarningMessage = "A " + preferred + " is not shown, because the total number of displayed categories would be too high!";
				}
			else {
				mWarningMessage = "A " + preferred + " is not shown, because the current column-to-axes assignment is not compatible!";
				}
			}
		}

	private void updateBarAndPieNaNExclusion() {
		boolean excludeNaN = (mChartType == cChartTypeBars
						   || mChartType == cChartTypePies)
						  && (mChartMode != cChartModeCount
						  && mChartMode != cChartModePercent)
						  && mChartColumn != cColumnUnassigned
						  && !mTableModel.isDescriptorColumn(mChartColumn);
			
		for (int i=0; i<mDataPoints; i++) {
			if (excludeNaN && Float.isNaN(mPoint[i].record.getDouble(mChartColumn)))
				mPoint[i].exclusionFlags |= EXCLUSION_FLAG_DETAIL_GRAPH;
			else
				mPoint[i].exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
			}
		}

	/**
	 * Updates the NaN and zoom flags of local exclusion for all axes where needed
	 * and applies the local to the global exclusion, if local NaN or zoom flags were updated.
	 * @param localExclusionNeeds EXCLUSION_FLAG_NAN_0 and EXCLUSION_FLAG_ZOOM_0 left shifted according to axis
	 */
	private void validateExclusion(int localExclusionNeeds) {
		boolean found = false;
		for (int axis=0; axis<mDimensions; axis++) {
			if ((localExclusionNeeds & (EXCLUSION_FLAG_NAN_0 << axis)) != 0) {
				found = true;
				initializeLocalExclusion(axis);
				}
			if (mAxisIndex[axis] != cColumnUnassigned
			 && (localExclusionNeeds & (EXCLUSION_FLAG_ZOOM_0 << axis)) != 0) {
				found = true;
				updateLocalZoomExclusion(axis);
				}
			}
		if (found)
		   	applyLocalExclusion(false);
		}

	/**
	 * Determines based on table model information and current axis assignment,
	 * which axis is the preferred double value axis.
	 * @return 0,1,-1 for x-axis, y-axis, none
	 */
	protected int determineBoxPlotDoubleAxis() {
		for (int i=0; i<mDimensions; i++)
			if (mAxisIndex[i] != cColumnUnassigned
			 && (mTableModel.isDescriptorColumn(mAxisIndex[i]) || mTableModel.isColumnTypeDouble(mAxisIndex[i]))
			 && !mTableModel.isColumnTypeCategory(mAxisIndex[i])
			 && qualifyOtherAsChartCategory(i))
				return i;

		int minCategoryCount = Integer.MAX_VALUE;
		int minCountAxis = -1;
		for (int i=0; i<mDimensions; i++) {
			if (mAxisIndex[i] != cColumnUnassigned
			 && mTableModel.isColumnTypeDouble(mAxisIndex[i])
			 && qualifyOtherAsChartCategory(i)) {
				int categoryCount = 1;
				for (int axis=0; axis<mDimensions; axis++)
					if (axis != i && mAxisIndex[axis] != cColumnUnassigned)
						categoryCount = mTableModel.getCategoryCount(mAxisIndex[axis]);

				if (minCategoryCount > categoryCount) {
					minCategoryCount = categoryCount;
					minCountAxis = i;
					}
				}
			}

		return minCountAxis;
		}

	private boolean qualifyOtherAsChartCategory(int axis) {
		for (int i=0; i<mDimensions; i++)
			if (axis != i && !qualifiesAsChartCategory(i))
				return false;
		return true;
		}

	protected boolean qualifiesAsChartCategory(int axis) {
		return (mAxisIndex[axis] == cColumnUnassigned
			 || (mTableModel.isColumnTypeCategory(mAxisIndex[axis])
			  && mTableModel.getCategoryCount(mAxisIndex[axis]) <= cMaxChartCategoryCount));
		}

	/**
	 * Calculates the visible range of the axis based on pruning bar settings,
	 * current graph type, logarithmic view mode.
	 * @param axis
	 * @return whether the visible range has been changed
	 */
	private boolean calculateVisibleRange(int axis) {
		float visMin = -1.0f;
		float visMax =  1.0f;
		boolean visRangeIsLog = false;
		int column = mAxisIndex[axis];
		if (column != cColumnUnassigned) {
			if (mIsCategoryAxis[axis]) {
				int maxCategory = mTableModel.getCategoryCount(column) - 1;
				visMin = Math.round(mPruningBarLow[axis] * maxCategory) - 0.5f;
				visMax = Math.round(mPruningBarHigh[axis] * maxCategory) + 0.5f;
				}
			else if (mTableModel.isDescriptorColumn(column)) {
				visMin = mPruningBarLow[axis];
				visMax = mPruningBarHigh[axis];
				}
			else {
				float dataMin = mTableModel.getMinimumValue(column);
				float dataMax = mTableModel.getMaximumValue(column);
				float dataRange = dataMax - dataMin;
				visMin = (mPruningBarLow[axis] == 0.0f) ? dataMin : dataMin + mPruningBarLow[axis] * dataRange;
				visMax = (mPruningBarHigh[axis] == 1.0f) ? dataMax : dataMin + mPruningBarHigh[axis] * dataRange;
				visRangeIsLog = mTableModel.isLogarithmicViewMode(column);
				}
			}

		if (visMin != mAxisVisMin[axis]
		 || visMax != mAxisVisMax[axis]) {
			mAxisVisMin[axis] = visMin;
			mAxisVisMax[axis] = visMax;
			mAxisVisRangeIsLogarithmic[axis] = visRangeIsLog;
			return true;
			}

		return false;
		}

	public float getVisibleMin(int axis) {
		return mAxisVisMin[axis];
		}

	public float getVisibleMax(int axis) {
		return mAxisVisMax[axis];
		}

	/**
	 * Based on the currently selected visible range, the displayed chart type and axis usage,
	 * this method calculates the pruning bar value from 0.0 to 1.0 to reflect visible range low value.
	 * @param axis
	 * @return low value of visible range normalized to span 0.0 to 1.0
	 */
	public float getPruningBarLow(int axis) {
		int column = mAxisIndex[axis];
		if (column == cColumnUnassigned)
			return 0.0f;
		if (mTableModel.isDescriptorColumn(column))
			return mAxisVisMin[axis];
		if (mIsCategoryAxis[axis])
			return (float)Math.round(mAxisVisMin[axis] + 0.5f) / (float)mTableModel.getCategoryCount(column);

		float min = mTableModel.getMinimumValue(column);
		float max = mTableModel.getMaximumValue(column);
		float visMin = mAxisVisMin[axis];

		if (mTableModel.isLogarithmicViewMode(column) != mAxisVisRangeIsLogarithmic[axis]) {
			if (mAxisVisRangeIsLogarithmic[axis])
				visMin = (float)Math.pow(10, visMin);
			else
				visMin = (float)Math.log10(visMin);
			}

		return Math.min(1f, Math.max(0f, (visMin - min)
			 / (max - min)));
		}

	/**
	 * Based on the currently selected visible range, the displayed chart type and axis usage,
	 * this method calculates the pruning bar value from 0.0 to 1.0 to reflect visible range high value.
	 * @param axis
	 * @return high value of visible range normalized to span 0.0 to 1.0
	 */
	public float getPruningBarHigh(int axis) {
		int column = mAxisIndex[axis];
		if (column == cColumnUnassigned)
			return 1.0f;
		if (mTableModel.isDescriptorColumn(column))
			return mAxisVisMax[axis];
		if (mIsCategoryAxis[axis])
			return (float)Math.round(mAxisVisMax[axis] + 0.5f) / (float)mTableModel.getCategoryCount(column);

		float min = mTableModel.getMinimumValue(column);
		float max = mTableModel.getMaximumValue(column);
		float visMax = mAxisVisMax[axis];

		if (mTableModel.isLogarithmicViewMode(column) != mAxisVisRangeIsLogarithmic[axis]) {
			if (mAxisVisRangeIsLogarithmic[axis])
				visMax = (float)Math.pow(10, visMax);
			else
				visMax = (float)Math.log10(visMax);
			}

		return Math.min(1f, Math.max(0f, (visMax - min) / (max - min)));
		}

	private float calculateZoomState() {
		float zoomState = 1.0f;
		int axisCount = 0;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mAxisIndex[axis] != cColumnUnassigned) {
				zoomState *= Math.max(0.001f, mPruningBarHigh[axis] - mPruningBarLow[axis]);
				axisCount++;
				}
			}
		zoomState = (float)Math.pow(zoomState, 0.75f / (float)axisCount);
		return Math.min(100f, 1f / zoomState);
		}

	// TODO remove mCategoryMin,mCategoryMax,mCategoryVisibleCount
	protected void calculateCategoryCounts(int boxPlotDoubleAxis) {
		for (int axis=0; axis<mDimensions; axis++) {
			int column = mAxisIndex[axis];
			if (column == cColumnUnassigned || axis == boxPlotDoubleAxis) {
				mCategoryMin[axis] = 0;
				mCategoryMax[axis] = 1;
				mCategoryVisibleCount[axis] = 1;
				}
			else {
				mCategoryMin[axis] = Math.round(mAxisVisMin[axis] + 0.5f);
				mCategoryMax[axis] = Math.round(mAxisVisMax[axis] + 0.5f);
				mCategoryVisibleCount[axis] = mCategoryMax[axis] - mCategoryMin[axis];
				}
			}

		if (isCaseSeparationDone())
			mCaseSeparationCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
		else
			mCaseSeparationCategoryCount = 1;

		mCombinedCategoryCount = new int[1+mDimensions];
		mCombinedCategoryCount[0] = mCaseSeparationCategoryCount;
		for (int axis=0; axis<mDimensions; axis++)
			mCombinedCategoryCount[axis+1] = mCombinedCategoryCount[axis] * mCategoryVisibleCount[axis];
		}

	/**
	 * Based on axis column assignments and on hvIndices of VisualizationPoints
	 * this method assigns all visible VisualizationPoints to bars/pies and to color categories
	 * within these bars/pies. It also calculates relative bar/pie sizes.
	 */
	protected void calculateBarsOrPies() {
		calculateCategoryCounts(-1);

		Color[] colorList = mMarkerColor.getColorList();
		int focusFlagNo = getFocusFlag();
		int basicColorCount = colorList.length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);

		int catCount = mCaseSeparationCategoryCount;
		for (int count:mCategoryVisibleCount)
			catCount *= count;

		mChartInfo = new CategoryViewInfo(mHVCount, catCount, colorCount);

		mChartInfo.barAxis = 0;
		for (int axis=1; axis<mDimensions; axis++)
			if (mAxisIndex[axis] == cColumnUnassigned)
				mChartInfo.barAxis = axis;
			else if (mAxisIndex[mChartInfo.barAxis] != cColumnUnassigned
				  && mCategoryVisibleCount[axis] <= mCategoryVisibleCount[mChartInfo.barAxis])
				mChartInfo.barAxis = axis;

		for (int i=0; i<colorList.length; i++)
			mChartInfo.color[i] = colorList[i];
		mChartInfo.color[colorList.length] = VisualizationColor.cSelectedColor;
		mChartInfo.color[colorList.length+1] = VisualizationColor.cUseAsFilterColor;
		if (focusFlagNo != -1) {
			for (int i=0; i<colorList.length; i++)
				mChartInfo.color[i+basicColorCount] = VisualizationColor.grayOutColor(colorList[i]);
			mChartInfo.color[colorList.length+basicColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cSelectedColor);
			mChartInfo.color[colorList.length+1+basicColorCount] = VisualizationColor.cUseAsFilterColor;
			}

		if (mChartMode != cChartModeCount
		 && mChartMode != cChartModePercent)
			mChartInfo.mean = new float[mHVCount][catCount];

		int visibleCount = 0;
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int colorIndex = (mUseAsFilterFlagNo != -1 && !mPoint[i].record.isFlagSet(mUseAsFilterFlagNo)) ? colorList.length+1
							   : (mPoint[i].record.isSelected() && mFocusList != cFocusOnSelection) ?
									   colorList.length : mPoint[i].colorIndex;
				if (focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo))
					colorIndex += basicColorCount;

				int cat = getChartCategoryIndex(mPoint[i]);
				mChartInfo.pointsInCategory[mPoint[i].hvIndex][cat]++;
				mChartInfo.pointsInColorCategory[mPoint[i].hvIndex][cat][colorIndex]++;
				visibleCount++;
				switch (mChartMode) {
				case cChartModeCount:
				case cChartModePercent:
					mChartInfo.barValue[mPoint[i].hvIndex][cat]++;
					break;
				case cChartModeMin:
				case cChartModeMax:
					float value = mPoint[i].record.getDouble(mChartColumn);
					if (mChartInfo.pointsInCategory[mPoint[i].hvIndex][cat] == 1)
						mChartInfo.barValue[mPoint[i].hvIndex][cat] = value;
					else if (mChartMode == cChartModeMin)
						mChartInfo.barValue[mPoint[i].hvIndex][cat] = Math.min(mChartInfo.barValue[mPoint[i].hvIndex][cat], value);
					else
						mChartInfo.barValue[mPoint[i].hvIndex][cat] = Math.max(mChartInfo.barValue[mPoint[i].hvIndex][cat], value);
					break;
				case cChartModeMean:
					mChartInfo.barValue[mPoint[i].hvIndex][cat] += mPoint[i].record.getDouble(mChartColumn);
					break;
				case cChartModeSum:
					if (mTableModel.isLogarithmicViewMode(mChartColumn))
						mChartInfo.barValue[mPoint[i].hvIndex][cat] += Math.pow(10, mPoint[i].record.getDouble(mChartColumn));
					else
						mChartInfo.barValue[mPoint[i].hvIndex][cat] += mPoint[i].record.getDouble(mChartColumn);
					break;
					}
				if (mChartInfo.mean != null)
					mChartInfo.mean[mPoint[i].hvIndex][cat] += mPoint[i].record.getDouble(mChartColumn);
				}
			}

		if (mChartMode == cChartModePercent)
			for (int i=0; i<mHVCount; i++)
				for (int j=0; j<catCount; j++)
	   				mChartInfo.barValue[i][j] *= 100f / visibleCount;
		if (mChartMode == cChartModeMean)
			for (int i=0; i<mHVCount; i++)
				for (int j=0; j<catCount; j++)
					mChartInfo.barValue[i][j] /= mChartInfo.pointsInCategory[i][j];
		if (mChartMode == cChartModeSum && mTableModel.isLogarithmicViewMode(mChartColumn))
			for (int i=0; i<mHVCount; i++)
				for (int j=0; j<catCount; j++)
					mChartInfo.barValue[i][j] = (float)Math.log10(mChartInfo.barValue[i][j]);

		if (mChartInfo.mean != null)
			for (int i=0; i<mHVCount; i++)
				for (int j = 0; j < catCount; j++)
					if (mChartInfo.pointsInCategory[i][j] != 0)
						mChartInfo.mean[i][j] /= mChartInfo.pointsInCategory[i][j];

		if (mChartInfo.mean != null)
			// calculate standard deviation and error margin using the values in mChartColumn
			calculateStdDevAndErrorMargin(catCount, mChartInfo.pointsInCategory, -1, mChartColumn);

		int[][][] count = new int[mHVCount][catCount][colorCount];
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				for (int color=1; color<colorCount; color++)
					count[hv][cat][color] = count[hv][cat][color-1]+mChartInfo.pointsInColorCategory[hv][cat][color-1];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int colorIndex = (mUseAsFilterFlagNo != -1 && !mPoint[i].record.isFlagSet(mUseAsFilterFlagNo)) ? colorList.length+1
							   : (mPoint[i].record.isSelected() && mFocusList != cFocusOnSelection) ?
									   colorList.length : mPoint[i].colorIndex;
				if (focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo))
					colorIndex += basicColorCount;

				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				mPoint[i].chartGroupIndex = count[hv][cat][colorIndex];
				count[hv][cat][colorIndex]++;
				}
			}

		float dataMin = Float.POSITIVE_INFINITY;
		float dataMax = Float.NEGATIVE_INFINITY;
		for (int i=0; i<mHVCount; i++) {
			for (int j=0; j<catCount; j++) {
				if (mChartInfo.pointsInCategory[i][j] != 0) {
					if (dataMin > mChartInfo.barValue[i][j])
						dataMin = mChartInfo.barValue[i][j];
					if (dataMax < mChartInfo.barValue[i][j])
						dataMax = mChartInfo.barValue[i][j];
					}
				}
			}
		mChartInfo.barOrPieDataAvailable = (dataMin != Float.POSITIVE_INFINITY);

		if (mChartInfo.barOrPieDataAvailable) {
			switch (mChartMode) {
			case cChartModeCount:
			case cChartModePercent:
				mChartInfo.axisMin = 0.0f;
				mChartInfo.axisMax = dataMax * cBarSpacingFactor;
				mChartInfo.barBase = 0.0f;
				break;
			default:
				if (mTableModel.isLogarithmicViewMode(mChartColumn)) {
					float dataRange = dataMax - dataMin;
					mChartInfo.axisMin = dataMin - dataRange * (cBarSpacingFactor - 1.0f);
					mChartInfo.axisMax = dataMax + dataRange * (cBarSpacingFactor - 1.0f);
					mChartInfo.barBase = mChartInfo.axisMin;
					}
				else {
					if (dataMin >= 0.0) {
						mChartInfo.axisMin = 0.0f;
						mChartInfo.axisMax = dataMax * cBarSpacingFactor;
						}
					else if (dataMax <= 0.0) {
						mChartInfo.axisMin = dataMin * cBarSpacingFactor;
						mChartInfo.axisMax = 0.0f;
						}
					else {
						float dataRange = dataMax - dataMin;
						float spacing = (cBarSpacingFactor - 1.0f) * dataRange / 2.0f;
						mChartInfo.axisMax = dataMax + spacing;
						mChartInfo.axisMin = dataMin - spacing;
						}
					mChartInfo.barBase = 0.0f;
					}
				break;
				}
			}
		else {
			mChartInfo.axisMin = 0.0f;
			mChartInfo.axisMax = cBarSpacingFactor;
			mChartInfo.barBase = 0.0f;
			}
//System.out.println("calculateBarsOrPies() dataMin:"+mChartInfo.dataMin+" dataMax:"+mChartInfo.dataMax+" axisMin:"+mChartInfo.axisMin+" axisMax:"+mChartInfo.axisMax+" barBase:"+mChartInfo.barBase);
		}

	/**
	 * @return true if bars of a bar chart are based on the left/bottom edge
	 */
	protected boolean isLeftBarChart() {
		if (mChartInfo.axisMin == 0)
			return true;
		int chartColumn = getChartColumn();
		return (chartColumn != -1 && mTableModel.isLogarithmicViewMode(chartColumn));
		}

	/**
	 * @return true if bars of a bar chart are based on the right/top edge
	 */
	protected boolean isRightBarChart() {
		return mChartInfo.axisMax == 0;
	}

	/**
	 * @return true if bars of a bar chart are centered in view
	 */
	protected boolean isCenteredBarChart() {
		if (mChartInfo.axisMin == 0 || mChartInfo.axisMax == 0)
			return false;
		int chartColumn = getChartColumn();
		return (chartColumn != -1 && !mTableModel.isLogarithmicViewMode(chartColumn));
	}

	/**
	 * Allocates stdDev and errorMargin of mChartInfo and calculates the values.
	 * Needs a valid mean array and vCount, which includes all outliers.
	 * @param catCount
	 * @param vCount
	 * @param axis -1 if column is given
	 * @param column -1 if axis is given
	 */
	private void calculateStdDevAndErrorMargin(int catCount, int[][] vCount, int axis, int column) {
		mChartInfo.stdDev = new float[mHVCount][catCount];
		mChartInfo.errorMargin = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				float d = getValue(mPoint[i].record, axis, column) - mChartInfo.mean[hv][cat];
				mChartInfo.stdDev[hv][cat] += d*d;
				}
			}
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (vCount[hv][cat] <= 1) {
					mChartInfo.stdDev[hv][cat] = Float.POSITIVE_INFINITY;
					mChartInfo.errorMargin[hv][cat] = Float.POSITIVE_INFINITY;
					}
				else {
					mChartInfo.stdDev[hv][cat] = (float)Math.sqrt(mChartInfo.stdDev[hv][cat] /= (vCount[hv][cat] - 1));
					mChartInfo.errorMargin[hv][cat] = 1.96f * mChartInfo.stdDev[hv][cat] / (float)Math.sqrt(vCount[hv][cat]);
					}
				}
			}
		}

	/**
	 * Based on axis column assignments and on hvIndices of VisualizationPoints
	 * this method assigns all visible VisualizationPoints to boxes and to color categories
	 * within these boxes. It also calculates statistical parameters of all boxes.
	 */
	protected void calculateBoxPlot() {
		int doubleAxis = determineBoxPlotDoubleAxis();
		calculateCategoryCounts(doubleAxis);

		Color[] colorList = mMarkerColor.getColorList();
		int focusFlagNo = getFocusFlag();
		int basicColorCount = colorList.length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);

		int catCount = mCaseSeparationCategoryCount;
		for (int axis=0; axis<mDimensions; axis++)
			if (axis != doubleAxis)
				catCount *= mCategoryVisibleCount[axis]; 

		BoxPlotViewInfo boxPlotInfo = new BoxPlotViewInfo(mHVCount, catCount, colorCount);
		mChartInfo = boxPlotInfo;
		boxPlotInfo.barAxis = doubleAxis;

		// create array with all visible values separated by hv and cat
		int[][] vCount = new int[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = getChartCategoryIndex(mPoint[i]);
				int hv = mPoint[i].hvIndex;
				vCount[hv][cat]++;
				}
			}
		double[][][] value = new double[mHVCount][catCount][];
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (vCount[hv][cat] != 0)
					value[hv][cat] = new double[vCount[hv][cat]];

		// fill in values
		mChartInfo.mean = new float[mHVCount][catCount];
		vCount = new int[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				float d = getAxisValue(mPoint[i].record, boxPlotInfo.barAxis);
				boxPlotInfo.mean[hv][cat] += d;
				value[hv][cat][vCount[hv][cat]] = d;
				vCount[hv][cat]++;
				}
			}

		// calculate mean
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (vCount[hv][cat] != 0) {
					boxPlotInfo.mean[hv][cat] /= vCount[hv][cat];
					}
				}
			}

		// calculate standard deviation and error margin using the values applied to barAxis
		calculateStdDevAndErrorMargin(catCount, vCount, boxPlotInfo.barAxis, -1);

		boxPlotInfo.boxQ1 = new float[mHVCount][catCount];
		boxPlotInfo.median = new float[mHVCount][catCount];
		boxPlotInfo.boxQ3 = new float[mHVCount][catCount];
		boxPlotInfo.boxLAV = new float[mHVCount][catCount];
		boxPlotInfo.boxUAV = new float[mHVCount][catCount];

		// calculate statistical parameters from sorted values
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (vCount[hv][cat] != 0) {
					Arrays.sort(value[hv][cat]);
					boxPlotInfo.boxQ1[hv][cat] = (float)getQuartile(value[hv][cat], 1);
					boxPlotInfo.median[hv][cat] = (float)getQuartile(value[hv][cat], 2);
					boxPlotInfo.boxQ3[hv][cat] = (float)getQuartile(value[hv][cat], 3);
	
					// set lower and upper adjacent values
					float iqr = boxPlotInfo.boxQ3[hv][cat] - boxPlotInfo.boxQ1[hv][cat];
					float lowerLimit = boxPlotInfo.boxQ1[hv][cat] - 1.5f * iqr;
					float upperLimit = boxPlotInfo.boxQ3[hv][cat] + 1.5f * iqr;
					int i = 0;
					while (value[hv][cat][i] < lowerLimit)
						i++;
					boxPlotInfo.boxLAV[hv][cat] = (float)value[hv][cat][i];
					i = value[hv][cat].length - 1;
					while (value[hv][cat][i] > upperLimit)
						i--;
					boxPlotInfo.boxUAV[hv][cat] = (float)value[hv][cat][i];

					if (mChartType == cChartTypeWhiskerPlot)
						boxPlotInfo.pointsInCategory[hv][cat] = vCount[hv][cat];
					}
				}
			}

		int pValueColumn = getPValueColumn();
		if (pValueColumn != cColumnUnassigned) {
			int categoryIndex = getCategoryIndex(pValueColumn, mPValueRefCategory);
			if (categoryIndex != -1) {
				if (mBoxplotShowFoldChange)
					boxPlotInfo.foldChange = new float[mHVCount][catCount];
				if (mBoxplotShowPValue)
					boxPlotInfo.pValue = new float[mHVCount][catCount];
				int[] individualIndex = new int[1+mDimensions];
				for (int hv=0; hv<mHVCount; hv++) {
					for (int cat=0; cat<catCount; cat++) {
						if (vCount[hv][cat] != 0) {
							int refHV = getReferenceHV(hv, pValueColumn, categoryIndex);
							int refCat = getReferenceCat(cat, pValueColumn, categoryIndex, individualIndex);
							if ((refHV != hv || refCat != cat) && vCount[refHV][refCat] != 0) {
								if (mBoxplotShowFoldChange) {
									if (mTableModel.isLogarithmicViewMode(mAxisIndex[boxPlotInfo.barAxis]))
										boxPlotInfo.foldChange[hv][cat] = 3.321928094887363f * (boxPlotInfo.mean[hv][cat] - boxPlotInfo.mean[refHV][refCat]);	// this is the log2(fc)
									else
										boxPlotInfo.foldChange[hv][cat] = boxPlotInfo.mean[hv][cat] / boxPlotInfo.mean[refHV][refCat];
									}
								if (mBoxplotShowPValue) {
									try {
										boxPlotInfo.pValue[hv][cat] = (float) new TTestImpl().tTest(value[hv][cat], value[refHV][refCat]);
										}
									catch (IllegalArgumentException e) {
										boxPlotInfo.pValue[hv][cat] = Float.NaN;
										}
									catch (MathException e) {
										boxPlotInfo.pValue[hv][cat] = Float.NaN;
										}
									}
								}
							else {
								if (mBoxplotShowFoldChange)
									boxPlotInfo.foldChange[hv][cat] = Float.NaN;
								if (mBoxplotShowPValue)
									boxPlotInfo.pValue[hv][cat] = Float.NaN;
								}
							}
						}
					}
				}
			}

		// for this chart type we need the statistical parameters, but no colored bar
		if (mChartType == cChartTypeWhiskerPlot)
			return;

		// create color list
		for (int i=0; i<colorList.length; i++)
			boxPlotInfo.color[i] = colorList[i];
		boxPlotInfo.color[colorList.length] = VisualizationColor.cSelectedColor;
		boxPlotInfo.color[colorList.length+1] = VisualizationColor.cUseAsFilterColor;
		if (focusFlagNo != -1) {
			for (int i=0; i<colorList.length; i++)
				boxPlotInfo.color[i+basicColorCount] = VisualizationColor.grayOutColor(colorList[i]);
			boxPlotInfo.color[colorList.length+basicColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cSelectedColor);
			boxPlotInfo.color[colorList.length+1+basicColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cUseAsFilterColor);
			}

		boxPlotInfo.outlierCount = new int[mHVCount][catCount];

		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = getChartCategoryIndex(mPoint[i]);
				int hv = mPoint[i].hvIndex;
				if (boxPlotInfo.isOutsideValue(hv, cat, getAxisValue(mPoint[i].record, boxPlotInfo.barAxis))) {
					boxPlotInfo.outlierCount[hv][cat]++;
					}
				else {
					int colorIndex = (mUseAsFilterFlagNo != -1 && !mPoint[i].record.isFlagSet(mUseAsFilterFlagNo)) ? colorList.length+1
								   : (mPoint[i].record.isSelected() && mFocusList != cFocusOnSelection) ?
										   colorList.length : mPoint[i].colorIndex;
					if (focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo))
						colorIndex += basicColorCount;
	
					boxPlotInfo.pointsInCategory[hv][cat]++;
					boxPlotInfo.pointsInColorCategory[hv][cat][colorIndex]++;
					}
				}
			}

		int[][][] count = new int[mHVCount][catCount][colorCount];
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				for (int color=1; color<colorCount; color++)
					count[hv][cat][color] = count[hv][cat][color-1]+boxPlotInfo.pointsInColorCategory[hv][cat][color-1];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisible(mPoint[i])) {
				float v = getAxisValue(mPoint[i].record, boxPlotInfo.barAxis);
				if (Float.isNaN(v)) {
					mPoint[i].chartGroupIndex = -1;
					}
				else {
					int hv = mPoint[i].hvIndex;
					int cat = getChartCategoryIndex(mPoint[i]);
					if (boxPlotInfo.isOutsideValue(hv, cat, v)) {
						mPoint[i].chartGroupIndex = -1;
						}
					else {
						CompoundRecord record = mPoint[i].record;
						int colorIndex = (mUseAsFilterFlagNo != -1 && !mPoint[i].record.isFlagSet(mUseAsFilterFlagNo)) ? colorList.length+1
									   : (record.isSelected() && mFocusList != cFocusOnSelection) ?
											   colorList.length : mPoint[i].colorIndex;
						if (focusFlagNo != -1 && !record.isFlagSet(focusFlagNo))
							colorIndex += basicColorCount;
		
						mPoint[i].chartGroupIndex = count[hv][cat][colorIndex];
						count[hv][cat][colorIndex]++;
						}
					}
				}
			}
		}

	public String getStatisticalValues() {
		if (mChartType == cChartTypeScatterPlot)
			return "Incompatible chart type.";

		String[][] categoryList = new String[6][];
		int[] categoryColumn = new int[6];
		int categoryColumnCount = 0;

		if (isSplitView()) {
			for (int i=0; i<2; i++) {
				if (mSplittingColumn[i] != cColumnUnassigned) {
					categoryColumn[categoryColumnCount] = mSplittingColumn[i];
					categoryList[categoryColumnCount] = mTableModel.getCategoryList(mSplittingColumn[i]);
					categoryColumnCount++;
					}
				}
			}
		for (int axis=0; axis<mDimensions; axis++) {
			if (mIsCategoryAxis[axis]) {
				categoryColumn[categoryColumnCount] = mAxisIndex[axis];
				categoryList[categoryColumnCount] = new String[mCategoryVisibleCount[axis]];
				if (mAxisIndex[axis] == cColumnUnassigned) { // box/whisker plot with unassigned category axis
					categoryList[categoryColumnCount][0] = "<All Rows>";
					}
				else {
					String[] list = mTableModel.getCategoryList(categoryColumn[categoryColumnCount]);
					for (int j=0; j<mCategoryVisibleCount[axis]; j++)
						categoryList[categoryColumnCount][j] = list[mCategoryMin[axis]+j];
					}
				categoryColumnCount++;
				}
			}
		if (isCaseSeparationDone()) {
			categoryColumn[categoryColumnCount] = mCaseSeparationColumn;
			categoryList[categoryColumnCount] = mTableModel.getCategoryList(mCaseSeparationColumn);
			categoryColumnCount++;
			}

		boolean includePValue = false;
		boolean includeFoldChange = false;
		int pValueColumn = getPValueColumn();
		int referenceCategoryIndex = -1;
		if (pValueColumn != cColumnUnassigned) {
			referenceCategoryIndex = getCategoryIndex(pValueColumn, mPValueRefCategory);
			if (referenceCategoryIndex != -1) {
				includePValue = mBoxplotShowPValue;
				includeFoldChange = mBoxplotShowFoldChange;
				}
			}

		StringWriter stringWriter = new StringWriter(1024);
		BufferedWriter writer = new BufferedWriter(stringWriter);

		try {
			// construct the title line
			for (int i=0; i<categoryColumnCount; i++) {
				String columnTitle = (categoryColumn[i] == -1) ? "Category" : mTableModel.getColumnTitleWithSpecialType(categoryColumn[i]);
				writer.append(columnTitle+"\t");
				}

			if (mChartType != cChartTypeBoxPlot)
				writer.append("Rows in Category");

			if ((mChartType == cChartTypeBars
			  || mChartType == cChartTypePies)
			 && mChartMode != cChartModeCount) {
				String name = mTableModel.getColumnTitleWithSpecialType(mChartColumn);
				writer.append((mChartMode == cChartModePercent) ? "\tPercent of Rows"
							: (mChartMode == cChartModeMean) ? "\tMean of "+name
							: (mChartMode == cChartModeSum) ? "\tSum of "+name
							: (mChartMode == cChartModeMin) ? "\tMinimum of "+name
							: (mChartMode == cChartModeMax) ? "\tMaximum of "+name : "");
				if (mChartMode != cChartModePercent) {
					boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mChartColumn);
					writer.append(isLogarithmic ? "\tStandard Deviation (geom.)" : "\tStandard Deviation");
					writer.append("\tConfidence Interval (95%)");
					}
				}

			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) {
				boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mAxisIndex[mChartInfo.barAxis]);
				if (mChartType == cChartTypeBoxPlot) {
					writer.append("Total Count");
					writer.append("\tOutlier Count");
					}
				writer.append(isLogarithmic ? "\tMean (geom.)" : "\tMean Value");
				writer.append("\t1st Quartile");
				writer.append("\tMedian");
				writer.append("\t3rd Quartile");
				writer.append("\tLower Adjacent Limit");
				writer.append("\tUpper Adjacent Limit");
				writer.append(isLogarithmic ? "\tStandard Deviation (geom.)" : "\tStandard Deviation");
				writer.append("\tConfidence Interval (95%)");
/*				if (includeFoldChange || includePValue)		don't use additional column
					writer.append("\tIs Reference Group");	*/
				if (includeFoldChange)
					writer.append(isLogarithmic ? "\tlog2(Fold Change)" : "\tFold Change");
				if (includePValue)
					writer.append("\tp-Value");
				}
			writer.newLine();
		
			int[] categoryIndex = new int[6];
			while (categoryIndex[0] < categoryList[0].length) {
				int columnIndex = 0;
				int hv = 0;
				if (isSplitView()) {
					if (mSplittingColumn[0] != cColumnUnassigned)
						hv += categoryIndex[columnIndex++];
					if (mSplittingColumn[1] != cColumnUnassigned)
						hv += categoryIndex[columnIndex++] * categoryList[0].length;
					}

				int cat = 0;
				for (int axis=0; axis<mDimensions; axis++)
					if (mIsCategoryAxis[axis])
						cat += categoryIndex[columnIndex++] * mCombinedCategoryCount[axis];

				if (isCaseSeparationDone())
					cat += categoryIndex[columnIndex++];

				if (mChartInfo.pointsInCategory[hv][cat] != 0) {
					for (int i=0; i<categoryColumnCount; i++) {
						writer.append(categoryList[i][categoryIndex[i]]);
						if ((includeFoldChange || includePValue)
						 && pValueColumn==categoryColumn[i]
						 && mPValueRefCategory.equals(categoryList[i][categoryIndex[i]]))
							writer.append(" (ref)");
						writer.append("\t");
						}
			
					if (mChartType != cChartTypeBoxPlot)
						writer.append(""+mChartInfo.pointsInCategory[hv][cat]);

					if ((mChartType == cChartTypeBars
					  || mChartType == cChartTypePies)
					 && mChartMode != cChartModeCount) {
						writer.append("\t" + formatValue(mChartInfo.barValue[hv][cat], mChartColumn));
						if (mChartMode != cChartModePercent) {
							writer.append("\t"+formatValue(mChartInfo.stdDev[hv][cat], mChartColumn));
							writer.append("\t"+formatValue(mChartInfo.mean[hv][cat]-mChartInfo.errorMargin[hv][cat], mChartColumn)
										  +"-"+formatValue(mChartInfo.mean[hv][cat]+mChartInfo.errorMargin[hv][cat], mChartColumn));
							}
						}

					if (mChartType == cChartTypeBoxPlot
					 || mChartType == cChartTypeWhiskerPlot) {
						BoxPlotViewInfo vi = (BoxPlotViewInfo)mChartInfo;
						if (mChartType == cChartTypeBoxPlot) {
							writer.append(""+(mChartInfo.pointsInCategory[hv][cat]+vi.outlierCount[hv][cat]));
							writer.append("\t"+vi.outlierCount[hv][cat]);
							}
						int column = mAxisIndex[((BoxPlotViewInfo)mChartInfo).barAxis];
						writer.append("\t"+formatValue(vi.mean[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxQ1[hv][cat], column));
						writer.append("\t"+formatValue(vi.median[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxQ3[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxLAV[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxUAV[hv][cat], column));
						writer.append("\t"+formatValue(vi.stdDev[hv][cat], column));
						writer.append("\t"+formatValue(vi.mean[hv][cat]-vi.errorMargin[hv][cat], column)
									  +"-"+formatValue(vi.mean[hv][cat]+vi.errorMargin[hv][cat], column));
/*						if (includeFoldChange || includePValue) {		don't use additional column
							int refHV = getReferenceHV(hv, pValueColumn, referenceCategoryIndex);
							int refCat = getReferenceCat(cat, pValueColumn, referenceCategoryIndex, new int[1+mDimensions]);
							writer.append("\t"+((hv==refHV && cat==refCat) ? "yes" : "no"));
							}	*/
						if (includeFoldChange) {
							writer.append("\t");
							if (!Float.isNaN(vi.foldChange[hv][cat]))
								writer.append(new DecimalFormat("#.#####").format(vi.foldChange[hv][cat]));
							}
						if (includePValue) {
							writer.append("\t");
							if (!Float.isNaN(vi.pValue[hv][cat]))
								writer.append(new DecimalFormat("#.#####").format(vi.pValue[hv][cat]));
							}
						}
					writer.newLine();
					}
	
				// update category indices for next row
				for (int i=categoryColumnCount-1; i>=0; i--) {
					if (++categoryIndex[i] < categoryList[i].length || i == 0)
						break;

					categoryIndex[i] = 0;
					}
				}
			writer.close();
			}
		catch (IOException ioe) {}

		return stringWriter.toString();
		}

	/**
	 * Formats a numerical value for displaying it.
	 * This includes proper rounding and potential de-logarithmization of the original value.
	 * @param value is the logarithm, if the column is in logarithmic view mode
	 * @param column the column this value refers to
	 * @return
	 */
	protected String formatValue(float value, int column) {
		if (mTableModel.isLogarithmicViewMode(column) && !Float.isNaN(value) && !Float.isInfinite(value))
			value = (float)Math.pow(10, value);
		return DoubleFormat.toString(value);
		}

	/**
	 * If axis != -1, then this method returns getAxisValue(record, axis), which is
	 * the correct value to apply, when positioning a VisualizationPoint on an axis.
	 * Otherwise, this method returns record.getDouble(column).
	 * @param record
	 * @param axis -1 if column is given
	 * @param column -1 if axis is given
	 * @return
	 */
	private float getValue(CompoundRecord record, int axis, int column) {
		return (column != -1) ? record.getDouble(column) : getAxisValue(record, axis);
		}

	/**
	 * Returns the correct value to apply, when positioning a VisualizationPoint
	 * on an axis. This method resolves whether we have a dynamic value (e.g. from
	 * a descriptor similarity calculation) or a static value from the CompoundRecord.
	 * With ambiguous column types (category and double) it also considers, whether
	 * to use the category index or the double value.
	 * @param record
	 * @param axis
	 * @return
	 */
	protected float getAxisValue(CompoundRecord record, int axis) {
		int column = mAxisIndex[axis];
		return mTableModel.isDescriptorColumn(column) ?
				(mAxisSimilarity[axis] == null ? 0.5f : mAxisSimilarity[axis][record.getID()])
			  : (mIsCategoryAxis[axis]) ? mTableModel.getCategoryIndex(column, record) : record.getDouble(column);
		}

	protected TreeMap<byte[],VisualizationPoint> createReferenceMap(int referencingColumn, int referencedColumn) {
		// create list of referencing keys
/*		TreeSet<byte[]> set = new TreeSet<byte[]>(new ByteArrayComparator());
		for (VisualizationPoint vp:mPoint) {
			byte[] data = (byte[])vp.record.getData(referencingColumn);
			if (data != null)
				for (String ref:mTableModel.separateEntries(new String(data)))
					set.add(ref.getBytes());
			}*/

		// create map of existing and referenced VisualizationPoints
		TreeMap<byte[],VisualizationPoint> map = new TreeMap<byte[],VisualizationPoint>(new ByteArrayComparator());
		for (VisualizationPoint vp:mPoint) {
			byte[] key = (byte[])vp.record.getData(referencedColumn);
//			if (set.contains(key))
			if (key.length != 0)
				map.put(key, vp);
			}

		return map;
		}

	private TreeMap<VisualizationPoint,LineConnection[]> createReverseConnectionMap(int referencingColumn) {
		TreeMap<VisualizationPoint,LineConnection[]> map = new TreeMap<VisualizationPoint,LineConnection[]>(new VisualizationPointComparator());

		int keyColumn = mTableModel.findColumn(mTableModel.getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));
		int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
		float min = 0;
		float max = 0;
		float dif = 0;
		if (strengthColumn != -1) {
			min = mTableModel.getMinimumValue(strengthColumn);
			max = mTableModel.getMaximumValue(strengthColumn);
			if (max == min) {
				strengthColumn = -1;
				}
			else {
				min -= 0.2 * (max - min);
				dif = max - min;
				}
			}

		for (VisualizationPoint vp:mPoint) {
			byte[] key = (byte[])vp.record.getData(keyColumn);
			if (key == null)
				continue;

			byte[] data = (byte[])vp.record.getData(referencingColumn);
			if (data != null) {
				String[] keyEntry = mTableModel.separateEntries(new String(data));
				float[] strength = null;
				data = (byte[])vp.record.getData(strengthColumn);
				if (data != null) {
					String[] strengthEntry = mTableModel.separateEntries(new String(data));
					if (strengthEntry.length == keyEntry.length) {
						strength = new float[strengthEntry.length];
						for (int i=0; i<strength.length; i++) {
							try {
								float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strengthEntry[i], strengthColumn)));
								strength[i] = Float.isNaN(value) ? 0.0f : (value - min) / dif;
								}
							catch (NumberFormatException nfe) {}
							}
						}
					}

				for (int i=0; i<keyEntry.length; i++) {
					VisualizationPoint vp1 = mConnectionLineMap.get(key);
					VisualizationPoint vp2 = mConnectionLineMap.get(keyEntry[i].getBytes());
					if (vp2 != null) {
						LineConnection[] connection = map.get(vp2);
						if (connection == null) {
							connection = new LineConnection[1];
							connection[0] = new LineConnection(vp1, strength == null ? 1.0f : strength[i]);
							}
						else {
							LineConnection[] old = connection;
							connection = new LineConnection[old.length+1];
							for (int j=0; j<old.length; j++)
								connection[j] = old[j];
							connection[old.length] = new LineConnection(vp1, strength == null ? 1.0f : strength[i]);
							}
						map.put(vp2, connection);
						}
					}
				}
			}
		return map;
		}

	public boolean isUsedAsFilter() {
		return mUseAsFilterFlagNo != -1;
	}

	public void setUseAsFilter(boolean b) {
		if (b != (mUseAsFilterFlagNo != -1)) {
			if (!b) {
				mTableModel.setRowFlagToDirty(mUseAsFilterFlagNo);
				mTableModel.freeRowFlag(mUseAsFilterFlagNo);
				mUseAsFilterFlagNo = -1;
				}
			else {
				mUseAsFilterFlagNo = mTableModel.getUnusedRowFlag(true);
				}
			}
		}

	public boolean getShowNaNValues() {
		return mShowNaNValues;
		}

	public void setShowNaNValues(boolean b) {
		if (mShowNaNValues != b) {
			mShowNaNValues = b;
			applyLocalExclusion(false);
			invalidateOffImage(true);
			}
		}

	public int getGridMode() {
		return mGridMode;
		}

	public void setGridMode(int gridMode) {
		if (mGridMode != gridMode) {
			mGridMode = gridMode;
			invalidateOffImage(false);
			}
		}

	public boolean isLegendSuppressed() {
		return mSuppressLegend;
	}

	public void setSuppressLegend(boolean hideLegend) {
		if (mSuppressLegend != hideLegend) {
			mSuppressLegend = hideLegend;
			invalidateOffImage(true);
		}
	}

	public int getScaleMode() {
		return mScaleMode;
		}

	public void setScaleMode(int scaleMode) {
		if (mScaleMode != scaleMode) {
			mScaleMode = scaleMode;
			invalidateOffImage(true);
			}
		}

	public int getConnectionColumn() {
		// filter out connection types being incompatible with chart type
		if (mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot) {
			if (mConnectionColumn == cConnectionColumnConnectCases
			 || (mCaseSeparationCategoryCount != 1 && mConnectionColumn == mCaseSeparationColumn))
				return mConnectionColumn;
			for (int i=0; i<mDimensions; i++)
				if (mConnectionColumn == mAxisIndex[i])
					return mConnectionColumn;
			return cColumnUnassigned;
			}
		else {
			return mConnectionColumn != cConnectionColumnConnectCases ? mConnectionColumn : cColumnUnassigned;
			}
		}

	public int getConnectionOrderColumn() {
		return (mConnectionColumn == cColumnUnassigned
			 || mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) ?
					 cColumnUnassigned : mConnectionOrderColumn;
		}

	public void setConnectionColumns(int column, int orderColumn) {
		if (column != cColumnUnassigned
		 && column != cConnectionColumnConnectAll
		 && column != cConnectionColumnConnectCases
		 && !mTableModel.isColumnTypeCategory(column)
		 && mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn) == null)
			column = cColumnUnassigned;

		if (column == cColumnUnassigned
		 || column == cConnectionColumnConnectCases
		 ||	(column >= 0 && mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn) != null))
			orderColumn = cColumnUnassigned;

		if (mConnectionColumn != column || mConnectionOrderColumn != orderColumn) {
			invalidateConnectionLines();
			mConnectionColumn = column;
			mConnectionOrderColumn = orderColumn;
			invalidateOffImage(false);
			updateTreeViewGraph();
			}
		}

	public void setConnectionLineWidth(float width, boolean isAdjusting) {
		width = Math.min(4f, width);
		if (mRelativeConnectionLineWidth != width) {
			mRelativeConnectionLineWidth = width;
			invalidateOffImage(false);
			}
		}

	public float getConnectionLineWidth() {
		return mRelativeConnectionLineWidth;
		}

	public boolean isConnectionLineInverted() {
		return (mIsConnectionLineInverted
			 && mConnectionColumn != cColumnUnassigned
			 && mConnectionColumn != cConnectionColumnConnectAll
			 && mConnectionColumn != cConnectionColumnConnectCases
			 && mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn) != null
			 && CompoundTableConstants.cColumnPropertyReferenceTypeTopDown.equals(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType)));
		}

	public void setConnectionLineInversion(boolean isInverted) {
		if (mIsConnectionLineInverted != isInverted) {
			mIsConnectionLineInverted = isInverted;
			if (mConnectionColumn != cColumnUnassigned
				&& mConnectionColumn != cConnectionColumnConnectAll
				&& mConnectionColumn != cConnectionColumnConnectCases
				&& mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn) != null
				&& CompoundTableConstants.cColumnPropertyReferenceTypeTopDown.equals(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType)))
				invalidateOffImage(false);
			}
		}

	private void invalidateConnectionLines() {
		mConnectionLinePoint = null;
		mConnectionLineMap = null;
		mReverseConnectionMap = null;
		}

	public void setMarkerSize(float size, boolean isAdjusting) {
		if (mRelativeMarkerSize != size) {
			mRelativeMarkerSize = size;

			// if no connection lines are drawn then keep line width synchronized with marker size for potential use
			if (mConnectionColumn == cColumnUnassigned)
				mRelativeConnectionLineWidth = mRelativeMarkerSize;

			invalidateOffImage(true);
			}
		}

	public void setMarkerSizeColumn(int column) {
		if (mMarkerSizeColumn != column) {
			mMarkerSizeColumn = column;
			updateSimilarityMarkerSizes(-1);
			invalidateOffImage(true);
			}
		}

	public void setMarkerSizeInversion(boolean inversion) {
		if (mMarkerSizeInversion != inversion) {
			mMarkerSizeInversion = inversion;
			invalidateOffImage(false);
			}
		}

	public void setMarkerSizeProportional(boolean proportional) {
		if (mMarkerSizeProportional != proportional) {
			mMarkerSizeProportional = proportional;
			invalidateOffImage(false);
			}
		}

	public void setMarkerSizeZoomAdaption(boolean adapt) {
		if (Float.isNaN(mMarkerSizeZoomAdaption) == adapt) {
			if (adapt)
				mMarkerSizeZoomAdaption = calculateZoomState();
			else
				mMarkerSizeZoomAdaption = Float.NaN;
			invalidateOffImage(false);
			}
		}

	public void setMarkerLabelSize(float size, boolean isAdjusting) {
		if (mMarkerLabelSize != size) {
			mMarkerLabelSize = size;
			if (showAnyLabels()) {
				invalidateOffImage(false);
				}
			}
		}

	public void setMarkerShapeColumn(int column) {
		if (column >= 0
		 && (!mTableModel.isColumnTypeCategory(column)
		  || mTableModel.getCategoryCount(column) > getAvailableShapeCount()))
			column = cColumnUnassigned;

		if (mMarkerShapeColumn != column) {
			mMarkerShapeColumn = column;
			updateShapeIndices();
			}
		}
	
	private void updateShapeIndices() {
		if (mMarkerShapeColumn == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = 0;
		else if (CompoundTableListHandler.isListColumn(mMarkerShapeColumn)) {
			int flagNo = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.getListFromColumn(mMarkerShapeColumn));
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
			}
		else {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = (byte)mTableModel.getCategoryIndex(mMarkerShapeColumn, mPoint[i].record);
			}

		invalidateOffImage(true);
		}

	public void setMarkerLabelsInTreeViewOnly(boolean inTreeViewOnly) {
		mLabelsInTreeViewOnly = inTreeViewOnly;
		invalidateOffImage(false);
		}

	public void setMarkerLabels(int[] columnAtPosition) {
		boolean[] columnToClear = new boolean[mTableModel.getTotalColumnCount()];
		for (int i=0; i<mLabelColumn.length; i++)
			if (mLabelColumn[i] != cColumnUnassigned)
				columnToClear[mLabelColumn[i]] = true;

		mLabelColumn = columnAtPosition;

		for (int i=0; i<mLabelColumn.length; i++)
			if (mLabelColumn[i] != cColumnUnassigned)
				columnToClear[mLabelColumn[i]] = false;

		for (int column=0; column<columnToClear.length; column++)
			if (columnToClear[column])
				clearNonCustomLabelPositions(column);

		invalidateOffImage(false);
		}

	public int getMarkerLabelList() {
		return mLabelList;
		}

	public void setMarkerLabelList(int listNo) {
		if (mLabelList != listNo) {
			mLabelList = listNo;
			invalidateOffImage(false);
			}
		}

	protected boolean showAnyLabels() {
		if (!mLabelsInTreeViewOnly || isTreeViewGraph())
			for (int i=0; i<mLabelColumn.length; i++)
				if (mLabelColumn[i] != cColumnUnassigned)
					return true;

		return false;
		}

	public int getMarkerLabelColumn(int position) {
		return mLabelColumn[position];
		}

	public int getColumnIndex(int axis) {
		return mAxisIndex[axis];
		}

	/**
	 * Assigns the axis to the specified column or cColumnUnassigned.
	 * The chart type is updated and the visible range set to the maximum.
	 * Local record hiding of this axis is initialized and applied to the global
	 * exclusion.
	 * @param axis
	 * @param index
	 */
	public void setColumnIndex(int axis, int index) {
		if (mAxisIndex[axis] != index) {
			mAxisIndex[axis] = index;
			clearAllLabelPositions();
			initializeAxis(axis);
			int exclusionNeeeds = (EXCLUSION_FLAG_NAN_0 << axis) | determineChartType();
			validateExclusion(exclusionNeeeds);
			}
		}

	public abstract int[] getSupportedChartTypes();

	public int getChartType() {
		return mChartType;
		}

	/**
	 * @return -1 or active chart column, i.e. only if bar/pie and not count/percent mode
	 */
	public int getChartColumn() {
		if (mChartType != cChartTypeBars && mChartType != cChartTypePies)
			return -1;
		if (mChartMode == cChartModeCount || mChartMode == cChartModePercent)
			return -1;
		return mChartColumn;
		}

	/**
	 * @return defined chart type, which may be different from the actually shown one
	 */
	public int getPreferredChartType() {
		return mPreferredChartType;
		}

	/**
	 * @return defined chart mode even if we don't show bar/pie chart
	 */
	public int getPreferredChartMode() {
		return mChartMode;
		}

	/**
	 * @return defined chart column even if we don't show bar/pie chart or if they are in count/percent mode
	 */
	public int getPreferredChartColumn() {
		return mChartColumn;
		}

	public void setPreferredChartType(int type, int mode, int column) {
		if (mode == -1)
			mode = cChartModeCount;
		if (mode != cChartModeCount && mode != cChartModePercent && column == cColumnUnassigned)
			mode = cChartModeCount;
		if (mPreferredChartType != type
		 || mChartColumn != column
		 || mChartMode != mode) {
			mChartColumn = column;
			mPreferredChartType = type;
			mChartMode = mode;
			int exclusionNeeeds = determineChartType();
			validateExclusion(exclusionNeeeds);
			updateTreeViewGraph();
			invalidateOffImage(true);
			}
		}

	private int getReferenceHV(int hv, int categoryColumn, int categoryIndex) {
		if (!isSplitView())
			return 0;

		if (mSplittingColumn[1] == cColumnUnassigned)
			return (mSplittingColumn[0] == categoryColumn) ? categoryIndex : hv;

		int categoryCount = mTableModel.getCategoryCount(mSplittingColumn[0]);
		if (mSplittingColumn[0] == categoryColumn) {
			int index2 = hv / categoryCount;
			return categoryIndex + index2 * categoryCount;
			}
		else {
			int index1 = hv % categoryCount;
			return index1 + categoryIndex * categoryCount;
			}
		}

	private int getReferenceCat(int cat, int categoryColumn, int categoryIndex, int[] individualIndex) {
		for (int i=mDimensions; i>0; i--) {
			individualIndex[i] = cat / mCombinedCategoryCount[i-1];
			cat -= individualIndex[i] * mCombinedCategoryCount[i-1];
			}
		individualIndex[0] = cat;
		
		if (mCaseSeparationCategoryCount != 1 && categoryColumn == mCaseSeparationColumn) {
			individualIndex[0] = categoryIndex;
			}
		else {
			for (int axis=0; axis<mDimensions; axis++) {
				if (categoryColumn == mAxisIndex[axis]) {
					individualIndex[axis+1] = categoryIndex;
					break;
					}
				}
			}

		int index = individualIndex[0];
		for (int axis=0; axis<mDimensions; axis++)
			index += individualIndex[axis+1] * mCombinedCategoryCount[axis];

		return index;
		}

	/**
	 * Returns the category index on the axis, i.e. the visible(!) category list
	 * index of the visualization point. If the row belong to a category being
	 * zoomed out of the view, then -1 is returned.
	 * @param axis
	 * @param vp
	 * @return visible category index or -1
	 */
	protected int getCategoryIndex(int axis, VisualizationPoint vp) {
		if (mAxisIndex[axis] == cColumnUnassigned)
			return 0;

		int index = mTableModel.getCategoryIndex(mAxisIndex[axis], vp.record);

		return (index >= mCategoryMin[axis] && index < mCategoryMax[axis]) ?
			index - mCategoryMin[axis] : -1;
		}

	/**
	 * Returns the index of value in the column's visible(!!!) category list.
	 * If the column is shown on an axis and if this axis is zoomed in, then
	 * this category index differs from the one of the CompoundTableModel.
	 * @param column
	 * @param value
	 * @return category index or -1 if the category is scrolled out of view
	 */
	private int getCategoryIndex(int column, String value) {
		if (column == mCaseSeparationColumn)
			return mTableModel.getCategoryIndex(column, value);

		if (column == mSplittingColumn[0]
		 || column == mSplittingColumn[1])
			return mSplitViewCountExceeded ? 0 : mTableModel.getCategoryIndex(column, value);

		int axis = -1;
		for (int i=0; i<mDimensions; i++) {
			if (mAxisIndex[i] == column) {
				axis = i;
				break;
				}
			}

		int index = mTableModel.getCategoryIndex(column, value);

		return (index >= mCategoryMin[axis] && index < mCategoryMax[axis]) ?
			index - mCategoryMin[axis] : -1;
		}

	/**
	 * This method requires a valid chart type, which may be achieved by calling determineChartBasics()
	 * @param axis
	 * @return whether the data of the column assigned to this axis is considered category data
	 */
	public boolean isCategoryAxis(int axis) {
		return mIsCategoryAxis[axis];
		}

	/**
	 * Calculates one combined category index for VisualizationPoint
	 * that includes individual categories from case separation and axis
	 * categories. If the vp is zoomed out of the view, then -1 is returned.
	 * @param vp
	 * @return
	 */
	protected int getChartCategoryIndex(VisualizationPoint vp) {
		int index = (mCombinedCategoryCount[0] == 1) ?
				0 : mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);

		for (int axis=0; axis<mDimensions; axis++) {
			if ((mChartType != cChartTypeBoxPlot
   			  && mChartType != cChartTypeWhiskerPlot)
		   	 || axis != mChartInfo.barAxis) {
				int axisIndex = getCategoryIndex(axis, vp);
				if (axisIndex == -1)
					return -1;

				index += axisIndex * mCombinedCategoryCount[axis];
				}
			}

		return index;
		}

	/**
	 *
	 * @param value
	 * @param no 1(lower), 2(mean), or 3(upper)
	 * @return
	 */
	private double getQuartile(double[] value, int no) {
		int length = value.length;
		if (length == 1)
			return value[0];

		int index;
		switch (no) {
		case 1:
			index = length / 4;
			if ((length & 1) == 0) {
				return ((length & 2) == 2) ? value[index] : (value[index-1] + value[index]) / 2;
				}
			else if ((length & 3) == 1) {
				return (value[index-1] + 3*value[index]) / 4;
				}
			else {
				return (3*value[index] + value[index+1]) / 4;
				}
		case 2:
			index = length / 2;
			return ((length & 1) == 1) ? value[index] : (value[index-1] + value[index]) / 2;
		case 3:
			index = length / 2 + length / 4;
			if ((length & 1) == 0) {
				return ((length & 2) == 2) ? value[index] : (value[index-1] + value[index]) / 2;
				}
			else if ((length & 3) == 1) {
				return (3*value[index] + value[index+1]) / 4;
				}
			else {
				return (value[index] + 3*value[index+1]) / 4;
				}
			}
		return 0;
		}

	public void mouseClicked(MouseEvent e) {
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			VisualizationPoint marker = findMarker(e.getX(), e.getY());
			if (mActivePoint != marker) {
				// don't allow root de-selection if we are in a dedicated tree view
				boolean isPureTreeView = isTreeViewGraph() && !mTreeViewShowAll;
				if (marker != null || !isPureTreeView)
					mTableModel.setActiveRow(marker==null? null : marker.record);
				}
			}
		}

	/**
	 * This is the default implementation of locating a marker from screen coordinates.
	 * It assumes a rectangular marker shape and relies on the getDistanceToMarker().
	 * For complex marker shapes overwrite this method or getDistanceToMarker().
	 * @param x
	 * @param y
	 * @return
	 */
	public VisualizationPoint findMarker(int x, int y) {
		// inverted order to prefer markers that are in the front
		boolean searchLabels = showAnyLabels();
		mHighlightedLabelPosition = null;
		VisualizationPoint p = null;
		float minDistance = Float.MAX_VALUE;
		for (int i=mDataPoints-1; i>=0; i--) {
			if (isVisible(mPoint[i])) {
				float dvp = getDistanceToMarker(mPoint[i], x, y);
				if (dvp == 0)
					return mPoint[i];
				if (searchLabels) {
					mHighlightedLabelPosition = mPoint[i].findLabel(x, y);
					if (mHighlightedLabelPosition != null)
						return mPoint[i];
					}
				if (dvp < 4 && minDistance > dvp) {
					p = mPoint[i];
					minDistance = dvp;
					}
				}
			}

		return p;
		}

	/**
	 * This method assumes a rectangular marker shape and uses the
	 * VisualizationPoint's width and height values.
	 * May be overwritten to support complex marker shapes.
	 * @param vp
	 * @param x
	 * @param y
	 * @return
	 */
	public float getDistanceToMarker(VisualizationPoint vp, int x, int y) {
		float dx = Math.abs(vp.screenX - x) - vp.width / 2f;
		float dy = Math.abs(vp.screenY - y) - vp.height / 2f;
		return Math.max(0, Math.max(dx, dy));
		}

	public void mousePressed(MouseEvent e) {
		mMouseX1 = mMouseX2 = e.getX();
		mMouseY1 = mMouseY2 = e.getY();
		mMouseIsDown = true;

		if (System.getProperty("touch") != null) {
			new Thread() {
				public void run() {
					try {
						Thread.sleep(1000);
						}
					catch (InterruptedException ie) {}

					if (Math.abs(mMouseX2 - mMouseX1) < 5
					 && Math.abs(mMouseY2 - mMouseY1) < 5
					 && mMouseIsDown) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								activateTouchFunction();
								}
							});
						}
					}
				}.start();
			}

		mDragMode = DRAG_MODE_NONE;
		if (e.isPopupTrigger()) {
			if (delayPopupMenu())
				startPopupTimer();
			else
				showPopupMenu();
			}
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == 0) {
			mAddingToSelection = e.isShiftDown();
			if (e.isControlDown()) {
				mDragMode = DRAG_MODE_TRANSLATE;
				}
			else if (e.isAltDown()) {
				mDragMode = DRAG_MODE_RECT_SELECT;
				}
			else if (mHighlightedLabelPosition != null) {
				mDragMode = DRAG_MODE_MOVE_LABEL;
				}
			else {
				mDragMode = DRAG_MODE_LASSO_SELECT;
				mLassoRegion = new Polygon();
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				}
			}
		}

	public void mouseReleased(MouseEvent e) {
		mMouseIsDown = false;
		if (e.isPopupTrigger()) {
			if (showDelayedPopupMenu())
				showPopupMenu();
			}
		else if (mPopupThread != null) {
			mPopupThread = null;
			if (showDelayedPopupMenu())
				showPopupMenu();
			}

		if (mDragMode == DRAG_MODE_RECT_SELECT) {
			int mouseX1,mouseX2,mouseY1,mouseY2;

			if (mMouseX1 < mMouseX2) {
				mouseX1 = mMouseX1;
				mouseX2 = mMouseX2;
				}
			else {
				mouseX1 = mMouseX2;
				mouseX2 = mMouseX1;
				}

			if (mMouseY1 < mMouseY2) {
				mouseY1 = mMouseY1;
				mouseY2 = mMouseY2;
				}
			else {
				mouseY1 = mMouseY2;
				mouseY2 = mMouseY1;
				}

			boolean isCustomFilter = (mUseAsFilterFlagNo != -1);
			for (int i=0; i<mDataPoints; i++) {
				boolean isSelected = mPoint[i].screenX>=mouseX1
								  && mPoint[i].screenX<=mouseX2
								  && mPoint[i].screenY>=mouseY1
								  && mPoint[i].screenY<=mouseY2
								  && isVisible(mPoint[i]);
				if (isCustomFilter) {
					if (isSelected)
						mPoint[i].record.clearFlag(mUseAsFilterFlagNo);
					else if (!mAddingToSelection)
						mPoint[i].record.setFlag(mUseAsFilterFlagNo);
					}
				else {
					if (isSelected)
						mPoint[i].record.setSelection(true);
					else if (!mAddingToSelection)
						mPoint[i].record.setSelection(false);
					}
				}

			if (isCustomFilter)
				mTableModel.updateExternalExclusion(mUseAsFilterFlagNo, false, true);
			else
				mSelectionModel.invalidate();
			}
		else if (mDragMode == DRAG_MODE_LASSO_SELECT) {
			boolean isCustomFilter = (mUseAsFilterFlagNo != -1);
			for (int i=0; i<mDataPoints; i++) {
				boolean isSelected = mLassoRegion.contains(mPoint[i].screenX, mPoint[i].screenY)
						&& isVisible(mPoint[i]);
				if (isCustomFilter) {
					if (isSelected)
						mPoint[i].record.clearFlag(mUseAsFilterFlagNo);
					else if (!mAddingToSelection)
						mPoint[i].record.setFlag(mUseAsFilterFlagNo);
					}
				else {
					if (isSelected)
						mPoint[i].record.setSelection(true);
					else if (!mAddingToSelection)
						mPoint[i].record.setSelection(false);
					}
				}

			if (isCustomFilter)
				mTableModel.updateExternalExclusion(mUseAsFilterFlagNo, false, true);
			else
				mSelectionModel.invalidate();
			}
		else if (mDragMode == DRAG_MODE_MOVE_LABEL) {
			updateHighlightedLabelPosition();
			invalidateOffImage(false);
			}

		if (mTouchFunctionActive) {
			mTouchFunctionActive = false;
			repaint();
			}

		mDragMode = DRAG_MODE_NONE;
		}

	private void activateTouchFunction() {
		if (!showPopupMenu()) {
			mTouchFunctionActive = true;
			repaint();
			}
		}

	protected boolean isTouchFunctionActive() {
		return mTouchFunctionActive;
		}

	protected boolean showPopupMenu() {
		if (mDetailPopupProvider != null) {
			CompoundRecord record = (mHighlightedPoint == null) ? null : mHighlightedPoint.record;
			JPopupMenu popup = mDetailPopupProvider.createPopupMenu(record, (VisualizationPanel)getParent(), -1);
			if (popup != null) {
				popup.show(this, mMouseX1, mMouseY1);
				return true;
				}
			}

		return false;
		}

	/**
	 * May be overridden to delay popup menus in case a preferred alternative action
	 * may happen, e.g. dragging to rotate the view. After a short delay
	 * showDelayedPopupMenu() is called.
	 * @return true if popup menu shall be shown
	 */
	public boolean delayPopupMenu() {
		return false;
		}

	private void startPopupTimer() {
		mPopupThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(RIGHT_MOUSE_POPUP_DELAY);
					if (showDelayedPopupMenu())
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								if (mPopupThread != null)
									showPopupMenu();
							}
						});
					} catch (InterruptedException ie) {}
				}
			} );
		mPopupThread.start();
		}

	/**
	 * If delayPopupMenu() is overridden, this should also be overridden to decide,
	 * whether the delayed popup shall be shown or whether the alternative action took place.
	 * @return
	 */
	public boolean showDelayedPopupMenu() {
		return true;
		}

	public void mouseEntered(MouseEvent e) {
		mMouseIsDown = false;
		}

	public void mouseExited(MouseEvent e) {
		mMouseIsDown = false;
		}

	public void mouseMoved(MouseEvent e) {
		VisualizationLabelPosition oldLabel = mHighlightedLabelPosition;
		VisualizationPoint marker = findMarker(e.getX(), e.getY());
		if (oldLabel != mHighlightedLabelPosition)
			repaint();
		if (mHighlightedPoint != marker)
			mTableModel.setHighlightedRow((marker == null) ? null : marker.record);
		}

	public void mouseDragged(MouseEvent e) {
		if (mDragMode == DRAG_MODE_MOVE_LABEL) {
			mHighlightedLabelPosition.translate(e.getX() - mMouseX2, e.getY() - mMouseY2);
			repaint();
			}

		mMouseX2 = e.getX();
		mMouseY2 = e.getY();

		if (mDragMode == DRAG_MODE_RECT_SELECT) {
			repaint();
			}
		else if (mDragMode == DRAG_MODE_LASSO_SELECT) {
			if ((Math.abs(mMouseX2 - mLassoRegion.xpoints[mLassoRegion.npoints-1]) > 3)
			 || (Math.abs(mMouseY2 - mLassoRegion.ypoints[mLassoRegion.npoints-1]) > 3)) {
				mLassoRegion.npoints--;
				mLassoRegion.addPoint(mMouseX2, mMouseY2);
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				}

			repaint();
			}
		}

	@Override
	public Point getToolTipLocation(MouseEvent e) {
		VisualizationPoint vp = findMarker(e.getX(), e.getY());
		return (vp != null) ? new Point((int)vp.screenX, (int)vp.screenY) : null;
		}

	@Override
	public String getToolTipText(MouseEvent e) {
		VisualizationPoint vp = findMarker(e.getX(), e.getY());
		if (vp == null)
			return null;
		TreeSet<Integer> columnSet = new TreeSet<Integer>();
		StringBuilder sb = new StringBuilder();
		for (int axis=0; axis<mDimensions; axis++)
			addTooltipRow(vp.record, mAxisIndex[axis], mAxisSimilarity[axis], columnSet, sb);

		addMarkerTooltips(vp, columnSet, sb);

		if (sb.length() == 0)
			return null;

		sb.append("</html>");
		return sb.toString();
		}

	protected void addMarkerTooltips(VisualizationPoint vp, TreeSet<Integer> columnSet, StringBuilder sb) {
		addTooltipRow(vp.record, mMarkerColor.getColorColumn(), null, columnSet, sb);
		addTooltipRow(vp.record, mMarkerSizeColumn, null, columnSet, sb);
		addTooltipRow(vp.record, mMarkerShapeColumn, null, columnSet, sb);
		addTooltipRow(vp.record, getChartColumn(), null, columnSet, sb);
		}

	protected void addTooltipRow(CompoundRecord record, int column, float[] similarity, TreeSet<Integer> columnSet, StringBuilder sb) {
		if (column != cColumnUnassigned && !columnSet.contains(column)) {
			columnSet.add(column);
			String title = null;
			String value = null;
			if (CompoundTableListHandler.isListColumn(column)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(column);
				int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
				title = record.isFlagSet(flagNo) ? "Member of '" : "Not member of '";
				value = mTableModel.getListHandler().getListName(listIndex);
				}
			else {
				title = getAxisTitle(column)+": ";
				if (mTableModel.isDescriptorColumn(column)) {
					if (similarity != null)
						value = DoubleFormat.toString(similarity[record.getID()]);
					else
						value = DoubleFormat.toString((mActivePoint == null) ? Double.NaN
								: mTableModel.getDescriptorSimilarity(mActivePoint.record, record, column));
					}
				else if (mTableModel.getColumnSpecialType(column) != null) {
					int idColumn = mTableModel.findColumn(mTableModel.getColumnProperty(column,
							CompoundTableConstants.cColumnPropertyIdentifierColumn));
					if (idColumn != -1)
						value = mTableModel.encodeData(record, idColumn);
					}
			   	else {
			   		value = mTableModel.encodeData(record, column);
			   		}
		   		}
			if (value != null) {
				sb.append((sb.length() == 0) ? "<html>" : "<br>");
		   		sb.append(title);
		   		sb.append(value);
				}
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		boolean needsUpdate = false;
		int exclusionNeeds = 0;

		if (e.getType() == CompoundTableEvent.cChangeExcluded) {
			mVisibleCategoryFromCategory = null;
			if ((mSplittingColumn[0] >=0 || mSplittingColumn[1] >=0) && !mShowEmptyInSplitView)
				invalidateSplittingIndices();
			if (mTreeViewIsDynamic && mTreeNodeList != null) {
				updateTreeViewGraph();
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getColumn();
			if (mVisibleCategoryFromCategory != null)
				mVisibleCategoryFromCategory[column] = null;

			for (int axis=0; axis<mDimensions; axis++) {
				if (column == mAxisIndex[axis]) {
					if (mTableModel.isDescriptorColumn(column))
			   			setSimilarityValues(axis, e.getSpecifier());

				   	exclusionNeeds = ((EXCLUSION_FLAG_NAN_0 | EXCLUSION_FLAG_ZOOM_0) << axis);
					needsUpdate = true;
					}
				}
			if (mMarkerSizeColumn == column) {
		   		updateSimilarityMarkerSizes(e.getSpecifier());
				needsUpdate = true;
				}
			if (mMarkerShapeColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column)
				 || mTableModel.getCategoryCount(column) > getAvailableShapeCount())
				 	mMarkerShapeColumn = cColumnUnassigned;
	   			updateShapeIndices();
		   		needsUpdate = true;
				}
			if (mCaseSeparationColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column)
				 || mTableModel.getCategoryCount(column) > cMaxCaseSeparationCategoryCount)
					mCaseSeparationColumn = cColumnUnassigned;
				needsUpdate = true;
				}
			if (mSplittingColumn[0] == column
			 || mSplittingColumn[1] == column) {
				if (!mTableModel.isColumnTypeCategory(column)) {
					if (mSplittingColumn[0] == column) {
						mSplittingColumn[0] = mSplittingColumn[1];
						mSplittingColumn[1] = cColumnUnassigned;
						}
					else {
						mSplittingColumn[1] = cColumnUnassigned;
						}
					}
				
				invalidateSplittingIndices();
				}
			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] == column)
					needsUpdate = true;
				}
			if (mConnectionColumn == column || mConnectionOrderColumn == column) {
				invalidateConnectionLines();
				needsUpdate = true;
				}
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			initializeDataPoints(true, e.getType() == CompoundTableEvent.cDeleteRows);
			mVisibleCategoryFromCategory = null;
			for (int axis=0; axis<mDimensions; axis++) {
				int column = mAxisIndex[axis];
				if (column != cColumnUnassigned) {
				   	if (mTableModel.isDescriptorColumn(column))
				   		setSimilarityValues(axis, -1);

				   	exclusionNeeds |= ((EXCLUSION_FLAG_NAN_0 | EXCLUSION_FLAG_ZOOM_0) << axis);
					}
				}

	   		updateSimilarityMarkerSizes(-1);
		   	if (mMarkerShapeColumn >= 0) {	// if not is unassigned or list
				if (!mTableModel.isColumnTypeCategory(mMarkerShapeColumn))
					mMarkerShapeColumn = cColumnUnassigned;
				updateShapeIndices();
				}
			if (mCaseSeparationColumn >= 0) {
				if (!mTableModel.isColumnTypeCategory(mCaseSeparationColumn)
				 || mTableModel.getCategoryCount(mCaseSeparationColumn) > cMaxCaseSeparationCategoryCount)
					mCaseSeparationColumn = cColumnUnassigned;
				needsUpdate = true;
				}
			if (mSplittingColumn[0] >= 0
			 || mSplittingColumn[1] >= 0) {  // if not is unassigned or list
				if (mSplittingColumn[0] >= 0 && !mTableModel.isColumnTypeCategory(mSplittingColumn[0])) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				if (mSplittingColumn[1] >= 0 && !mTableModel.isColumnTypeCategory(mSplittingColumn[1])) {
					mSplittingColumn[1] = cColumnUnassigned;
					}
				invalidateSplittingIndices();
				}
			invalidateConnectionLines();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cAddColumns) {
			if (mVisibleCategoryFromCategory != null) {
				int[][] oldVisibleCategoryFromCategory = mVisibleCategoryFromCategory;
				mVisibleCategoryFromCategory = new int[mTableModel.getTotalColumnCount()][];
				for (int i=0; i<oldVisibleCategoryFromCategory.length; i++)
					mVisibleCategoryFromCategory[i] = oldVisibleCategoryFromCategory[i];
				}
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mVisibleCategoryFromCategory != null) {
				int[][] oldVisibleCategoryFromCategory = mVisibleCategoryFromCategory;
				mVisibleCategoryFromCategory = new int[mTableModel.getTotalColumnCount()][];
				for (int i=0; i<columnMapping.length; i++)
					if (columnMapping[i] != -1)
						mVisibleCategoryFromCategory[columnMapping[i]] = oldVisibleCategoryFromCategory[i];
				}
			for (int i=mLegendList.size()-1; i>=0; i--) {
				int column = mLegendList.get(i).getColumn();
				if (column >= 0 && columnMapping[column] == cColumnUnassigned)
					needsUpdate = true;
				}
			if (mMarkerSizeColumn >= 0) {
				mMarkerSizeColumn = columnMapping[mMarkerSizeColumn];
				}
			if (mMarkerShapeColumn >= 0) {
				mMarkerShapeColumn = columnMapping[mMarkerShapeColumn];
				}
			if (mCaseSeparationColumn >= 0) {
				mCaseSeparationColumn = columnMapping[mCaseSeparationColumn];
				if (mCaseSeparationColumn == cColumnUnassigned)
					needsUpdate = true;
				}
			if (mSplittingColumn[0] >= 0) {
				mSplittingColumn[0] = columnMapping[mSplittingColumn[0]];
				boolean updateSplitting = (mSplittingColumn[0] == cColumnUnassigned);
				if (mSplittingColumn[1] >= 0) {
					mSplittingColumn[1] = columnMapping[mSplittingColumn[1]];
					if (mSplittingColumn[1] == cColumnUnassigned)
						updateSplitting = true;
					}
				if (mSplittingColumn[0] == cColumnUnassigned && mSplittingColumn[1] != -1) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				if (updateSplitting) {
					invalidateSplittingIndices();
					needsUpdate = true;
					}
				}
			if (mConnectionColumn >= 0) {
				mConnectionColumn = columnMapping[mConnectionColumn];
				if (mConnectionColumn == cColumnUnassigned) {
					invalidateConnectionLines();
					needsUpdate = true;
					}
				}
			if (mConnectionOrderColumn != cColumnUnassigned) {
				mConnectionOrderColumn = columnMapping[mConnectionOrderColumn];
				if (mConnectionOrderColumn == cColumnUnassigned) {
					needsUpdate = true;
					}
				}
			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] >= 0) {
					mLabelColumn[i] = columnMapping[mLabelColumn[i]];
					if (mLabelColumn[i] == cColumnUnassigned)
						needsUpdate = true;
					}
				}
			for (int i=0; i<mDataPoints; i++) {
				mPoint[i].remapLabelPositionColumns(columnMapping);
				}
			for (int axis=0; axis<mDimensions; axis++) {
				if (mAxisIndex[axis] != cColumnUnassigned) {
					mAxisIndex[axis] = columnMapping[mAxisIndex[axis]];
					if (mAxisIndex[axis] == cColumnUnassigned) {
						mAxisIndex[axis] = cColumnUnassigned;
						needsUpdate = true;
						initializeAxis(axis);
						exclusionNeeds = (EXCLUSION_FLAG_NAN_0 << axis);
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			updateActiveRow();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			invalidateOffImage(false);
			}

		for (JVisualizationLegend legend:mLegendList)
			legend.compoundTableChanged(e);

		mMarkerColor.compoundTableChanged(e);

	   	validateExclusion(exclusionNeeds | determineChartType());

		if (needsUpdate)
			invalidateOffImage(true);
		}

	/**
	 * Removes all cached label positions for the given column.
	 * This does not include manually assigned label positions.
	 * @param column
	 */
	public void clearNonCustomLabelPositions(int column) {
		for (int i=0; i<mDataPoints; i++)
			mPoint[i].removeNonCustomLabelPosition(column);
		}

	/**
	 * Removes all cached label positions with no exception.
	 */
	public void clearAllLabelPositions() {
		for (int i=0; i<mDataPoints; i++)
			mPoint[i].labelPosition = null;
		}

	/**
	 * Updates the column entry of all cached label positions from oldColumn to newColumn.
	 * This may include or exclude manually assigned label positions.
	 * @param oldToNewColumn
	 */
	public void remapLabelPositionColumns(int[] oldToNewColumn) {
		for (int i=0; i<mDataPoints; i++)
			mPoint[i].remapLabelPositionColumns(oldToNewColumn);
		}

	public void listChanged(CompoundTableListEvent e) {
		if (e.getType() == CompoundTableListEvent.cDelete) {
			if (mFocusList != cFocusNone) {
				if (mFocusList == e.getListIndex())
					setFocusHitlist(cFocusNone);
				else if (mFocusList > e.getListIndex())
					mFocusList--;
				}
			if (mLabelList != cLabelsOnAllRows) {
				if (mLabelList == e.getListIndex())
					setFocusHitlist(cLabelsOnAllRows);
				else if (mLabelList > e.getListIndex())
					mLabelList--;
				}
			if (CompoundTableListHandler.isListColumn(mMarkerSizeColumn)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mMarkerSizeColumn);
				if (e.getListIndex() == listIndex) {
					mMarkerSizeColumn = cColumnUnassigned;
					invalidateOffImage(false);
					}
				else if (listIndex > e.getListIndex()) {
					mMarkerSizeColumn = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (CompoundTableListHandler.isListColumn(mMarkerShapeColumn)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mMarkerShapeColumn);
				if (e.getListIndex() == listIndex) {
					mMarkerShapeColumn = cColumnUnassigned;
					for (int i=0; i<mDataPoints; i++)
						mPoint[i].shape = 0;
					invalidateOffImage(true);
					}
				else if (listIndex > e.getListIndex()) {
					mMarkerShapeColumn = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (CompoundTableListHandler.isListColumn(mCaseSeparationColumn)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mCaseSeparationColumn);
				if (e.getListIndex() == listIndex) {
					mCaseSeparationColumn = cColumnUnassigned;
					invalidateOffImage(true);
					}
				else if (listIndex > e.getListIndex()) {
					mCaseSeparationColumn = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			boolean splittingChanged = false;
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[0])) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mSplittingColumn[0]);
				if (e.getListIndex() == listIndex) {
					mSplittingColumn[0] = cColumnUnassigned;
					splittingChanged = true;
					}
				else if (listIndex > e.getListIndex()) {
					mSplittingColumn[0] = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[1])) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mSplittingColumn[1]);
				if (e.getListIndex() == listIndex) {
					mSplittingColumn[1] = cColumnUnassigned;
					splittingChanged = true;
					}
				else if (listIndex > e.getListIndex()) {
					mSplittingColumn[1] = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (splittingChanged) {
				if (mSplittingColumn[0] == cColumnUnassigned
				 || mSplittingColumn[1] != cColumnUnassigned) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				invalidateSplittingIndices();
				}
			}
		else if (e.getType() == CompoundTableListEvent.cChange) {
			if (mFocusList == e.getListIndex()) {
				invalidateOffImage(false);
				}
			if (mLabelList == e.getListIndex()) {
				invalidateOffImage(false);
				}
			if (CompoundTableListHandler.isListColumn(mMarkerSizeColumn)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mMarkerSizeColumn);
				if (e.getListIndex() == listIndex) {
					invalidateOffImage(false);
					}
				}
			if (CompoundTableListHandler.isListColumn(mMarkerShapeColumn)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mMarkerShapeColumn);
				if (e.getListIndex() == listIndex) {
					int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
					for (int i=0; i<mDataPoints; i++)
						mPoint[i].shape = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
					invalidateOffImage(false);
					}
				}
			if (CompoundTableListHandler.isListColumn(mCaseSeparationColumn)) {
				int listIndex = CompoundTableListHandler.getListFromColumn(mCaseSeparationColumn);
				if (e.getListIndex() == listIndex)
					invalidateOffImage(true);
				}
			if ((CompoundTableListHandler.isListColumn(mSplittingColumn[0])
			  && e.getListIndex() == CompoundTableListHandler.getListFromColumn(mSplittingColumn[0]))
			 || (CompoundTableListHandler.isListColumn(mSplittingColumn[1])
			  && e.getListIndex() == CompoundTableListHandler.getListFromColumn(mSplittingColumn[1]))) {
				invalidateSplittingIndices();
				}
			}

		mMarkerColor.listChanged(e);
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			invalidateOffImage(mChartType == cChartTypeBoxPlot
							|| mChartType == cChartTypeBars
							|| mChartType == cChartTypePies);
			}
		}

	@Override
	public void highlightChanged(CompoundRecord record) {
		if (record != null
		 && (mHighlightedPoint == null || mHighlightedPoint.record != record)) {
			for (int i=0; i<mPoint.length; i++) {
				if (mPoint[i].record == record) {
					setHighlightedPoint(mPoint[i]);
					break;
					}
				}
			}
		else if (record == null && mHighlightedPoint != null) {
			setHighlightedPoint(null);
			}
		}

	public void updateVisibleRange(int axis, float low, float high, boolean isAdjusting) {
		if (axis < mDimensions && mAxisIndex[axis] != cColumnUnassigned) {
			mPruningBarLow[axis] = low;
			mPruningBarHigh[axis] = high;
			if (calculateVisibleRange(axis)) {
				updateLocalZoomExclusion(axis);
				applyLocalExclusion(isAdjusting);
				if (!Float.isNaN(mMarkerSizeZoomAdaption))
					mMarkerSizeZoomAdaption = calculateZoomState();
				invalidateOffImage(true);
				}
			}
		}

	/**
	 * This is used by find marker, assuming that a marker covers a
	 * rectangular area. If the marker's area is not rectangular,
	 * then findMarker() should be overridden for a smooth handling.
	 * @param p
	 * @return
	 */
	protected float getMarkerWidth(VisualizationPoint p) {
		return mAbsoluteMarkerSize;
		}

	/**
	 * This is used by find marker, assuming that a marker covers a
	 * rectangular area. If the marker's area is not rectangular,
	 * then findMarker() should be overridden for a smooth handling.
	 * @param p
	 * @return
	 */
	protected float getMarkerHeight(VisualizationPoint p) {
		return mAbsoluteMarkerSize;
		}

	/**
	 * Resets the visible range of the axis to the tablemodel's
	 * min and max values and repaints.
	 * @param axis
	 */
	public void initializeAxis(int axis) {
		mPruningBarLow[axis] = 0.0f;
		mPruningBarHigh[axis] = 1.0f;

		mAxisSimilarity[axis] = null;

		int column = mAxisIndex[axis];

		if (column != cColumnUnassigned
		 && mTableModel.isDescriptorColumn(column))
			setSimilarityValues(axis, -1);

		invalidateOffImage(true);
		}

	/**
	 * @return whether this is a non excluded point in the table model neglecting this views filter flag
	 */
	protected boolean isVisibleInModel(VisualizationPoint point) {
		return (mUseAsFilterFlagNo == -1) ? mTableModel.isVisible(point.record)
				: mTableModel.isVisibleNeglecting(point.record, mUseAsFilterFlagNo);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered visible, if the showNaNValue option is on.
	 * @param point
	 * @return
	 */
	protected boolean isVisible(VisualizationPoint point) {
		return (point.exclusionFlags & (mShowNaNValues ? ~EXCLUSION_FLAGS_NAN : mActiveExclusionFlags)) == 0
			&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered invisible, even if the showNaNValue option is on.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleExcludeNaN(VisualizationPoint point) {
		return (point.exclusionFlags & mActiveExclusionFlags) == 0
				&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered visible, even if the showNaNValue option is off.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleIncludeNaN(VisualizationPoint point) {
		return (point.exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0
				&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether this visualization point is not zoomed out of this view
	 * and not invisible because of another view, but contains a NaN value
	 * on at least one axis that shows floating point values.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleAndNaN(VisualizationPoint point) {
		return (point.exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0
			&& (point.exclusionFlags & EXCLUSION_FLAGS_NAN & mActiveExclusionFlags) != 0
			&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether there is at least one axis showing floating point values
	 * (not as categories) where this point has a NaN value in the associated column.
	 * @param point
	 * @return
	 */
	protected boolean isNaN(VisualizationPoint point) {
		return (point.exclusionFlags & EXCLUSION_FLAGS_NAN & mActiveExclusionFlags) != 0;
		}

	/**
	 * Sets axis related zoom flags to false and sets NaN flags depending on
	 * whether the value is NaN regardless whether the axis is showing floats or categories.
	 * @param axis
	 */
	private void initializeLocalExclusion(int axis) {
			// flags 0-2: set if invisible due to view zooming
			// flags 3-5: set if invisible because of empty data (applies only for non-category )
			// flags 6  : set if invisible because point is not part of currently shown detail graph
		int column = mAxisIndex[axis];
		byte nanFlag = (byte)(EXCLUSION_FLAG_NAN_0 << axis);
		byte bothFlags = (byte)((EXCLUSION_FLAG_ZOOM_0 | EXCLUSION_FLAG_NAN_0) << axis);

			// reset all flags and then
			// flag all records with empty data
		for (int i=0; i<mDataPoints; i++) {
			mPoint[i].exclusionFlags &= ~bothFlags;
			if (column != -1
			 && !mTableModel.isDescriptorColumn(column)
			 && Float.isNaN(mPoint[i].record.getDouble(column)))
				mPoint[i].exclusionFlags |= nanFlag;
			}
		}

	/**
	 * Updates the local exclusion flags of non-NAN row values to
	 * reflect whether the value lies between the visible range of the axis.
	 * Needs to be called after determineChartType().
	 * @param axis
	 */
	private void updateLocalZoomExclusion(int axis) {
		byte zoomFlag = (byte)(EXCLUSION_FLAG_ZOOM_0 << axis);
		byte nanFlag = (byte)(EXCLUSION_FLAG_NAN_0 << axis);
		for (int i=0; i<mDataPoints; i++) {
			if (mIsCategoryAxis[axis] || (mPoint[i].exclusionFlags & nanFlag) == 0) {
				float theDouble = getAxisValue(mPoint[i].record, axis);
				if (theDouble < mAxisVisMin[axis]
				 || theDouble > mAxisVisMax[axis])
					mPoint[i].exclusionFlags |= zoomFlag;
				else
					mPoint[i].exclusionFlags &= ~zoomFlag;
				}
			else {
				mPoint[i].exclusionFlags &= ~zoomFlag;
				}
			}
		}

	private void updateGlobalExclusion() {
		if (mLocalAffectsGlobalExclusion && !mSuspendGlobalExclusion) {
			if (mLocalExclusionFlagNo == -1)
				mLocalExclusionFlagNo = mTableModel.getUnusedRowFlag(true);
			}
		else {
			mLocalExclusionFlagNo = -1;
			}
		applyLocalExclusion(false);
		}

	/**
	 * Returns whether rows zoomed out of view or invisible rows because of NaN values
	 * are also excluded from other views. The default is true.
	 * @return whether this view's local exclusion also affects the global exclusion
	 */
	public boolean getAffectGlobalExclusion() {
		return mLocalAffectsGlobalExclusion;
		}

	/**
	 * Defines whether local exclusion is affecting global exclusion,
	 * i.e. whether rows zoomed out of view or invisible rows because of NaN values
	 * are also excluded from other views. The default is true.
	 * @param v
	 */
	public void setAffectGlobalExclusion(boolean v) {
		if (mLocalAffectsGlobalExclusion != v) {
	   		mLocalAffectsGlobalExclusion = v;
			updateGlobalExclusion();
			}
		}

	/**
	 * Used to temporarily suspend the global record exclusion from the local one.
	 * This is called when the view gets hidden or shown again.
	 * @param suspend
	 */
	public void setSuspendGlobalExclusion(boolean suspend) {
		if (mSuspendGlobalExclusion != suspend) {
			mSuspendGlobalExclusion = suspend;
			updateGlobalExclusion();
			}
		}

	/**
	 * Set table model row exclusion flags according to local zooming/NAN-values
	 * and trigger a TableModelEvent in case the global record visibility changes.
	 * @param isAdjusting
	 */
	private void applyLocalExclusion(final boolean isAdjusting) {
			// set "current view exclusion flags" in CompoundTableModel
		if (!mApplyLocalExclusionScheduled) {
			mApplyLocalExclusionScheduled = true;

			// in case applyLocalExclusion() is called in the cascade caused by a compoundTableChanged()
			// (e.g. with delete columns), then we must wait until all views have updated accordingly,
			// before interfering by spawning another compoundTableChanged() cascade...
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					mApplyLocalExclusionScheduled = false;

					if (mLocalExclusionFlagNo != -1) {
						boolean excludedRecordsFound = false;
						long mask = mTableModel.convertRowFlagToMask(mLocalExclusionFlagNo);
						for (int i=0; i<mDataPoints; i++) {
							if ((mPoint[i].exclusionFlags & mActiveExclusionFlags) == 0
							 || (mShowNaNValues
							  && (mPoint[i].exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0)) {
								mPoint[i].record.clearFlags(mask);
								}
							else {
								mPoint[i].record.setFlags(mask);
								excludedRecordsFound = true;
								}
							}
	
						mTableModel.updateExternalExclusion(mLocalExclusionFlagNo, isAdjusting, excludedRecordsFound);
						}
					else if (mPreviousLocalExclusionFlagNo != -1) {
						mTableModel.freeRowFlag(mPreviousLocalExclusionFlagNo);
						}

					mPreviousLocalExclusionFlagNo = mLocalExclusionFlagNo;
					}
				});
			}
		}

	protected String createDateLabel(int theMarker, int exponent) {
		long time = theMarker;
		while (exponent < 0) {
			if (time % 10 != 0)
				return null;
			time /= 10;
			exponent++;
			}
		while (exponent > 0) {
			time *= 10;
			exponent--;
			}
		return DateFormat.getDateInstance().format(new Date(86400000*time+43200000));
		}

	protected void updateActiveRow() {
		CompoundRecord newActiveRow = mTableModel.getActiveRow();
		if (newActiveRow != null
		 && (mActivePoint == null || mActivePoint.record != newActiveRow)) {
			for (int i=0; i<mPoint.length; i++) {
				if (mPoint[i].record == newActiveRow) {
					setActivePoint(mPoint[i]);
					break;
					}
				}
			}
		else if (newActiveRow == null && mActivePoint != null) {
			setActivePoint(null);
			}
		}

	@Override
	public boolean supportsMarkerLabelTable() {
		return false;
		}

	@Override
	public boolean supportsMidPositionLabels() {
		return true;
		}

	public int getMarkerLabelTableEntryCount() {
		return 0;
		}

	@Override
	public boolean supportsLabelsByList() {
		return true;
		}

	@Override
	public boolean supportsLabelBackground() {
		return true;
		}

	@Override
	public boolean supportsLabelBackgroundTransparency() {
		return mDimensions == 2;
		}

	public boolean hasCustomPositionLabels() {
		for (int i=0; i<mDataPoints; i++) {
			VisualizationLabelPosition lp = mPoint[i].labelPosition;
			while (lp != null) {
				if (lp.isCustom())
					return true;
				lp = lp.getNext();
				}
			}
		return false;
		}

	public void readCustomLabelPositions(ArrayList<String> lineList) {
		float[] value = new float[3];
		for (String line:lineList) {
			String[] entry = line.split("\\t");
			if (entry.length == mDimensions+2) {
				int column = mTableModel.findColumn(entry[1]);
				try {
					int row = Integer.parseInt(entry[0]);
					for (int i=0; i<mDimensions; i++)
						value[i] = Float.parseFloat(entry[i+2]);
					VisualizationLabelPosition lp = mPoint[row].getOrCreateLabelPosition(column);
					lp.setXYZ(value[0], value[1], value[2]);
					}
				catch (NumberFormatException nfe) {}
				}
			}
		}

	public void writeCustomLabelPositions(BufferedWriter writer) throws IOException {
		int[] idToRow = new int[mTableModel.getTotalRowCount()];
		for (int row=0; row<mTableModel.getTotalRowCount(); row++)
			idToRow[mTableModel.getTotalRecord(row).getID()] = row;

		for (int i=0; i<mDataPoints; i++) {
			VisualizationLabelPosition lp = mPoint[i].labelPosition;
			while (lp != null) {
				if (lp.isCustom()) {
					writer.write(Integer.toString(idToRow[mPoint[i].record.getID()]));
					writer.write('\t');
					writer.write(mTableModel.getColumnTitleNoAlias(lp.getColumn()));
					writer.write('\t');
					writer.write(DoubleFormat.toString(lp.getX()));
					writer.write('\t');
					writer.write(DoubleFormat.toString(lp.getY()));
					if (mDimensions == 3) {
						writer.write('\t');
						writer.write(DoubleFormat.toString(lp.getZ()));
						}
					writer.newLine();
					}
				lp = lp.getNext();
				}
			}
		writer.write(CUSTOM_LABEL_POSITION_END_VIEW_TAG);
		writer.newLine();
		}

	public class DoubleDimension {
		double width,height;
		}

	public class LineConnection {
		VisualizationPoint target;
		float strength;

		public LineConnection(VisualizationPoint target, float strength) {
			this.target = target;
			this.strength = strength;
			}
		}
	}

class VisualizationPointComparator implements Comparator<VisualizationPoint> {
	public int compare(VisualizationPoint o1, VisualizationPoint o2) {
		int id1 = o1.record.getID();
		int id2 = o2.record.getID();
		return (id1 < id2) ? -1 : (id1 == id2) ? 0 : 1;
		}
	}
