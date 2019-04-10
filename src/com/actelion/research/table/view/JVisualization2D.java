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

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.calc.INumericalDataColumn;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.DepictorTransformation;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.category.CategoryList;
import com.actelion.research.table.category.CategoryMolecule;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.graph.RadialGraphOptimizer;
import com.actelion.research.table.view.graph.TreeGraphOptimizer;
import com.actelion.research.table.view.graph.VisualizationNode;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.awt.print.PageFormat;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

import static com.actelion.research.table.view.VisualizationColor.cUseAsFilterColor;

public class JVisualization2D extends JVisualization {
	private static final long serialVersionUID = 0x00000001;

	public static final int cAvailableShapeCount = 7;
	public static final int BACKGROUND_VISIBLE_RECORDS = -1;
	public static final int BACKGROUND_ALL_RECORDS = -2;

	private static final float cMarkerSize = 0.028f;
	private static final float cConnectionLineWidth = 0.005f;
	private static final float cMaxPieSize = 1.0f;
	private static final float cMaxBarReduction = 0.66f;
	private static final float ARROW_TIP_SIZE = 0.6f;
	private static final float OUTLINE_LIMIT = 3;

	private static final int cPrintScaling = 16;

	private static final float MARKER_OUTLINE = 0.7f;
	private static final float NAN_WIDTH = 2.0f;
	private static final float NAN_SPACING = 0.5f;
	private static final int DIGITS = 4;    // significant digits for in-view values

	// if the delay between recent repaint() and paintComponent() is larger than this, we assume a busy EDT and paint with low detail
	private static final long MAX_REPAINT_DELAY_FOR_FULL_DETAILS = 100;

	// if paintComponent() with full detail takes more than this, the next paintComponent() will skip detail if EDT is busy or paintComponent()s are believed to be adjusting
	private static final long MAX_FULL_DETAIL_PAINT_TIME = 80;

	// delay between finishing last paintComponent() till start of next one. Delays smaller than this indicate adjusting=true, i.e. more paints to expect
	private static final long MAX_MILLIS_BETWEEN_LOW_DETAIL_PAINTS = 500;

	// delay between low detail paint and next automatic full detail paint, unless another low detail paint comes in between
	private static final long SLEEP_MILLIS_UNTIL_FULL_DETAIL_PAINT = 500;

	public static final String[] SCALE_MODE_TEXT = { "On both axes", "Hide all scales", "On X-axis only", "On Y-axis only" };
	public static final String[] GRID_MODE_TEXT = { "Show full grid", "Hide any grid", "Vertical lines only", "Horizontal lines only" };

	public static final String[] CURVE_MODE_TEXT = { "<none>", "Vertical Line", "Horizontal Line", "Fitted Line" };
	public static final String[] CURVE_MODE_CODE = { "none", "abscissa", "ordinate", "fitted" };
	private static final int cCurveModeNone = 0;
	private static final int cCurveModeMask = 7;
	private static final int cCurveModeAbscissa = 1;
	private static final int cCurveModeOrdinate = 2;
	private static final int cCurveModeBothAxes = 3;
	private static final int cCurveStandardDeviation = 8;
	private static final int cCurveSplitByCategory = 16;

	private static final int[] SUPPORTED_CHART_TYPE = { cChartTypeScatterPlot, cChartTypeWhiskerPlot, cChartTypeBoxPlot, cChartTypeBars, cChartTypePies };

	private static final int cScaleTextNormal = 1;
	private static final int cScaleTextAlternating = 2;
	private static final int cScaleTextInclined = 3;

	public static final int cMultiValueMarkerModeNone = 0;
	private static final int cMultiValueMarkerModePies = 1;
	private static final int cMultiValueMarkerModeBars = 2;
	public static final String[] MULTI_VALUE_MARKER_MODE_TEXT = { "<none>", "Pie Pieces", "Bars" };
	public static final String[] MULTI_VALUE_MARKER_MODE_CODE = { "none", "pies", "bars" };

	private static final String[] SPLIT_VIEW_EXCEEDED_MESSAGE = {"Split view count exceeded!", "Use filters to hide rows or", "don't configure view splitting."};

	private volatile boolean mSkipPaintDetails;
	private volatile Thread	mFullDetailPaintThread;

	private Graphics2D		mG;
	private Composite		mLabelBackgroundComposite;
	private Stroke          mThinLineStroke,mNormalLineStroke,mFatLineStroke,mConnectionStroke;
	private float[]			mCorrelationCoefficient;
	private float			mFontScaling,mMarkerTransparency;
	private int				mBorder,mCurveInfo,mBackgroundHCount,mBackgroundVCount,
							mBackgroundColorRadius,mBackgroundColorFading,mBackgroundColorConsidered,
							mConnectionFromIndex1,mConnectionFromIndex2,mShownCorrelationType,mMultiValueMarkerMode;
	private long			mPreviousPaintEnd,mPreviousFullDetailPaintMillis,mMostRecentRepaintMillis;
	private boolean			mBackgroundValid,mIsHighResolution,mMayNeedStatisticsLabelAdaption,mScaleTitleCentered;
	private int[]			mScaleSize,mScaleTextMode,mScaleDepictorOffset,mSplittingMolIndex,mNaNSize,mMultiValueMarkerColumns;
	protected VisualizationColor	mBackgroundColor;
	private Color[]			mMultiValueMarkerColor;
	private Color[][][]		mBackground;
	private Depictor2D[][]	mScaleDepictor,mSplittingDepictor;
	private VolatileImage	mOffImage;
	private BufferedImage   mBackgroundImage;		// primary data
	private byte[]			mBackgroundImageData;	// cached if delivered or constructed from mBackgroundImage if needed
	private byte[]			mSVGBackgroundData;		// alternative to mBackgroundImage
	private Graphics2D		mOffG;
	private ArrayList<ScaleLine>[]	mScaleLineList;

	@SuppressWarnings("unchecked")
	public JVisualization2D(CompoundTableModel tableModel,
							CompoundListSelectionModel selectionModel) {
		super(tableModel, selectionModel, 2);
		mPoint = new VisualizationPoint2D[0];
		mNaNSize = new int[2];
		mScaleSize = new int[2];
		mScaleTextMode = new int[2];
		mScaleDepictor = new Depictor2D[2][];
		mScaleDepictorOffset = new int[2];
		mScaleLineList = new ArrayList[2];
		mScaleLineList[0] = new ArrayList<ScaleLine>();
		mScaleLineList[1] = new ArrayList<ScaleLine>();
		mSplittingDepictor = new Depictor2D[2][];
		mSplittingMolIndex = new int[2];
		mSplittingMolIndex[0] = cColumnUnassigned;
		mSplittingMolIndex[1] = cColumnUnassigned;

		mBackgroundColor = new VisualizationColor(mTableModel, this);

		initialize();
		}

	protected void initialize() {
		super.initialize();
		mBackgroundColorConsidered = BACKGROUND_VISIBLE_RECORDS;
		mMarkerShapeColumn = cColumnUnassigned;
		mChartColumn = cColumnUnassigned;
		mChartMode = cChartModeCount;
		mBackgroundColorRadius = 10;
		mBackgroundColorFading = 10;
		mBackgroundImage = null;
		mBackgroundImageData = null;
		mMultiValueMarkerColumns = null;
		mMultiValueMarkerMode = cMultiValueMarkerModePies;
		mShownCorrelationType = CorrelationCalculator.TYPE_NONE;
		mScaleTitleCentered = true;
		}

	@Override
	public void repaint() {
		super.repaint();
		mMostRecentRepaintMillis = System.currentTimeMillis();
		}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!mIsFastRendering)
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		mIsHighResolution = false;

		mWarningMessage = null;
		determineWarningMessage();

		int width = getWidth();
		int height = getHeight();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		float retinaFactor = HiDPIHelper.getRetinaScaleFactor();
		if (mOffImage == null
		 || mOffImage.getWidth(null) != width*retinaFactor
		 || mOffImage.getHeight(null) != height*retinaFactor) {
			mOffImage = gc.createCompatibleVolatileImage(Math.round(width*retinaFactor), Math.round(height*retinaFactor), Transparency.OPAQUE);
			mCoordinatesValid = false;
			}

		if (!mCoordinatesValid)
			mOffImageValid = false;

		if (!mOffImageValid) {
			do  {
				if (mOffImage.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE)
					mOffImage = gc.createCompatibleVolatileImage(Math.round(width*retinaFactor), Math.round(height*retinaFactor), Transparency.OPAQUE);

				mOffG = null;
				try {
					mOffG = mOffImage.createGraphics();

					final long start = System.currentTimeMillis();
					if (!mSkipPaintDetails) {	// if we are not already in skip detail mode
						if (start - mMostRecentRepaintMillis > MAX_REPAINT_DELAY_FOR_FULL_DETAILS
						  || (start - mPreviousPaintEnd < MAX_MILLIS_BETWEEN_LOW_DETAIL_PAINTS
						   && mPreviousFullDetailPaintMillis > MAX_FULL_DETAIL_PAINT_TIME)) {
							mSkipPaintDetails = true;
							mFullDetailPaintThread = null;
							}
//						System.out.println(this.toString()+"; millis since repaint:"+(start - mMostRecentRepaintMillis)+" since recent paint end:"
//								+(start - mPreviousPaintEnd)+" recent full paint millis:"+mPreviousFullDetailPaintMillis+" skipping details:"+mSkipPaintDetails);
						}

					if (retinaFactor != 1f)
						mOffG.scale(retinaFactor, retinaFactor);
					if (!mIsFastRendering && !mSkipPaintDetails)
						mOffG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//					mOffG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	// no sub-pixel accuracy looks cleaner
		
					mOffG.setColor(getViewBackground());
					mOffG.fillRect(0, 0, width, height);
					Insets insets = getInsets();
					Rectangle bounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
		
					mCorrelationCoefficient = null;

					mFontHeight = (int)(mRelativeFontSize * Math.sqrt(bounds.width*bounds.height) / 50f);

					mG = mOffG;

					paintContent(bounds, false);

					if (mWarningMessage != null) {
						setColor(Color.RED);
						setFontHeight(mFontHeight);
						drawString(mWarningMessage, width/2 - getStringWidth(mWarningMessage)/2, mFontHeight);
						}

					if (mSkipPaintDetails) {
						mFullDetailPaintThread = new Thread(new Runnable() {
							@Override
							public void run() {
								try { Thread.sleep(SLEEP_MILLIS_UNTIL_FULL_DETAIL_PAINT); } catch (InterruptedException ie) {}
								if (Thread.currentThread() == mFullDetailPaintThread) {
									mSkipPaintDetails = false;
									mOffImageValid = false;
									repaint();
									}
								}
							});
						mFullDetailPaintThread.start();
						}

					mPreviousPaintEnd = System.currentTimeMillis();
					if (!mSkipPaintDetails)
						mPreviousFullDetailPaintMillis = System.currentTimeMillis() - start;
					}
				finally {	// It's always best to dispose of your Graphics objects.
					mOffG.dispose();
					}
				} while (mOffImage.contentsLost());

			mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, width, height, this);
		if (mActivePoint != null
		 && isVisible(mActivePoint))
			markReference((Graphics2D)g);

		if (mHighlightedPoint != null)
			markHighlighted((Graphics2D)g);

		drawSelectionOutline(g);

		if (mSplittingColumn[0] != cColumnUnassigned && !isSplitView()) {
			g.setColor(Color.RED.darker());
			int fontSize = 3*mFontHeight;
			g.setFont(getFont().deriveFont(Font.PLAIN, fontSize));
			for (int i=0; i<SPLIT_VIEW_EXCEEDED_MESSAGE.length; i++) {
				int w = (int)g.getFontMetrics().getStringBounds(SPLIT_VIEW_EXCEEDED_MESSAGE[i], g).getWidth();
				g.drawString(SPLIT_VIEW_EXCEEDED_MESSAGE[i], (width-w)/2, height/2+(i*2-SPLIT_VIEW_EXCEEDED_MESSAGE.length-1)*fontSize);
				}
			}
		}

	@Override
	public int print(Graphics g, PageFormat f, int pageIndex) {
		if (pageIndex != 0)
			return NO_SUCH_PAGE;

		Rectangle bounds = new Rectangle((int)(cPrintScaling * f.getImageableX()),
										 (int)(cPrintScaling * f.getImageableY()),
										 (int)(cPrintScaling * f.getImageableWidth()),
										 (int)(cPrintScaling * f.getImageableHeight()));

		paintHighResolution((Graphics2D)g, bounds, cPrintScaling, false, true);

		return PAGE_EXISTS;
		}

	@Override
	public void paintHighResolution(Graphics2D g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting) {
			// font sizes are optimized for screen resolution are need to be scaled by fontScaling
		mIsHighResolution = true;
		mFontScaling = fontScaling;

		mCoordinatesValid = false;
		mBackgroundValid = false;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	no sub-pixel accuracy looks cleaner

					// font size limitations used to cause different view layouts when resizing a view, TLS 20-Dez-2013
//		mFontHeight = (int)(mRelativeFontSize * Math.max(Math.min(bounds.width/60, 14*fontScaling), 7*fontScaling));
		mFontHeight = (int)(mRelativeFontSize * Math.sqrt(bounds.width*bounds.height) / 60f);

		mG = g;

		if (isPrinting)
				// fontScaling was also used to inflate bounds to gain resolution
				// and has to be compensated by inverted scaling of the g2D
			g.scale(1.0/fontScaling, 1.0/fontScaling);

		if (!transparentBG) {
			setColor(getViewBackground());
			fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}

		if (bounds.width > 0 && bounds.height > 0)
			paintContent(bounds, transparentBG);

		mCoordinatesValid = false;
		mBackgroundValid = false;
		}

	/**
	 * If we need space for statistics labels, then reduce the area that we have for the bar
	 * If we show a scale reflecting the bar values, then we need to transform scale labels
	 * accordingly.
	 * @param baseRect
	 */
	private void adaptDoubleScalesForStatisticalLabels(final Rectangle baseRect) {
		if (mMayNeedStatisticsLabelAdaption) {  // used to adapt only once and not for every split view
			if (mChartType == cChartTypeBars) {
				for (int axis=0; axis<2; axis++) {
					if (axis == mChartInfo.barAxis) {
						float labelSize = calculateStatisticsLabelSize(axis);
						if (labelSize != 0f) {
							if (mAxisIndex[axis] == cColumnUnassigned) {
								float cellHeight = (float)(axis == 1 ? baseRect.height : baseRect.width) / (float)mCategoryVisibleCount[axis];
								float reduction = Math.min(cMaxBarReduction * cellHeight, labelSize);
								float barAreaHeight = (cellHeight - reduction) * cBarSpacingFactor;
								float shift = isRightBarChart() ? 1f / cBarSpacingFactor - 1f + reduction / cellHeight // bar at right edge and label left or below
										: isCenteredBarChart() ? (0.5f / cBarSpacingFactor - 0.5f) * barAreaHeight / cellHeight // bar middle centered (we move it to touch left edge, label right
										: 0f; // bar base touches left edge and the label is right

								for (ScaleLine sl : mScaleLineList[axis])
									sl.position = shift + sl.position * barAreaHeight / cellHeight;
								}
							else {
								if (isCenteredBarChart()) {
									float cellHeight = (float)(axis == 1 ? baseRect.height : baseRect.width) / (float)mCategoryVisibleCount[axis];
									float reduction = Math.min(cMaxBarReduction * cellHeight, labelSize);
									float barAreaHeight = (cellHeight - reduction) * cBarSpacingFactor;
									float barBaseFraction = mChartInfo.axisMin / (mChartInfo.axisMax - mChartInfo.axisMin);
									float shift = (barBaseFraction + (0.5f / cBarSpacingFactor - 0.5f - barBaseFraction)
												* barAreaHeight / cellHeight) / (float)mCategoryVisibleCount[axis];
									for (ScaleLine sl : mScaleLineList[axis])
										sl.position += shift;
									}
								}
							}
						}
					}
				}
			else if (mChartType == cChartTypePies) {
				if (mAxisIndex[1] != cColumnUnassigned) {
					float labelSize = Math.min(baseRect.height/2, calculateStatisticsLabelSize(1));
					if (labelSize != 0f) {
						float shift = labelSize / (float) baseRect.height;
						for (ScaleLine sl : mScaleLineList[1])
							sl.position = shift + sl.position * (1f - shift);
						}
					}
				}
			mMayNeedStatisticsLabelAdaption = false;
			}
		}

	/**
	 * @param barAxis
	 * @return size needed to display statistics labels: height or width
	 */
	private float calculateStatisticsLabelSize(int barAxis) {
		int labelCount = getLabelCount();
		if (labelCount == 0)
			return 0f;

		float scaledFontHeight = scaleIfSplitView(mFontHeight);
		return scaledFontHeight * (barAxis == 1 ? labelCount : 0.6f*getLabelWidth());
		}

	/**
	 * @return rough estimate of how many characters the longest label will be
	 */
	private int getLabelWidth() {
		if (mChartMode != cChartModeCount && mChartMode != cChartModePercent) {
			if (mShowConfidenceInterval)
				return 8+2*DIGITS;
			if (mShowMeanAndMedianValues)
				return 7+DIGITS;
			if (mShowStandardDeviation)
				return 4+DIGITS;
			}
		return 3+(int)Math.log10(mTableModel.getTotalRowCount());  // N=nnn
		}

	/**
	 * @return number of labels, i.e. label line count
	 */
	private int getLabelCount() {
		int count = mShowValueCount ? 1 : 0;
		if (mChartMode != cChartModeCount && mChartMode != cChartModePercent) {
			if (mShowConfidenceInterval)
				count++;
			if (mShowMeanAndMedianValues)
				count++;
			if (mShowStandardDeviation)
				count++;
			}
		return count;
		}

	private void paintContent(final Rectangle bounds, boolean transparentBG) {
		if (validateSplittingIndices())
			mBackgroundValid = false;

		mChartInfo = null;
		switch (mChartType) {
			case cChartTypeBoxPlot:
			case cChartTypeWhiskerPlot:
				calculateBoxPlot();
				break;
			case cChartTypeBars:
			case cChartTypePies:
				calculateBarsOrPies();
				break;
			}

		calculateMarkerSize(bounds);    // marker sizes are needed for size legend
		calculateLegend(bounds, (int)scaleIfSplitView(mFontHeight));

		float thinLineWidth = scaleIfSplitView(mFontHeight)/16f;
		if (mIsFastRendering) {
			mThinLineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mNormalLineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mFatLineStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mConnectionStroke = new BasicStroke(mAbsoluteConnectionLineWidth, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			}
		else {
			mThinLineStroke = new BasicStroke(thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mNormalLineStroke = new BasicStroke(1.4f * thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mFatLineStroke = new BasicStroke(2f * thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mConnectionStroke = new BasicStroke(mAbsoluteConnectionLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			}

		mLabelBackgroundComposite = (showAnyLabels() && mShowLabelBackground && mLabelBackgroundTransparency != 0) ?
				AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mLabelBackgroundTransparency)) : null;

		if (isSplitView()) {
			int scaledFontHeight = (int) scaleIfSplitView(mFontHeight);
			if (mLegendList.size() != 0)
				bounds.height -= scaledFontHeight / 2;
			compileSplittingHeaderMolecules();
			int count1 = mShowEmptyInSplitView ? mTableModel.getCategoryCount(mSplittingColumn[0]) : getVisibleCategoryCount(mSplittingColumn[0]);
			int count2 = (mSplittingColumn[1] == cColumnUnassigned) ? -1
					: mShowEmptyInSplitView ? mTableModel.getCategoryCount(mSplittingColumn[1]) : getVisibleCategoryCount(mSplittingColumn[1]);
			boolean largeHeader = (mSplittingDepictor[0] != null
					|| mSplittingDepictor[1] != null);
			mSplitter = new VisualizationSplitter(bounds, count1, count2, scaledFontHeight, largeHeader, mSplittingAspectRatio);
			}

		if (isSplitView()) {
			int scaledFontHeight = (int) scaleIfSplitView(mFontHeight);
			float titleBrightness = ColorHelper.perceivedBrightness(getTitleBackground());
			float backgroundBrightness = ColorHelper.perceivedBrightness(getViewBackground());
			Color borderColor = (backgroundBrightness > titleBrightness) ? getTitleBackground().darker().darker()
																		 : getTitleBackground().brighter().brighter();
			mSplitter.paintGrid(mG, borderColor, getTitleBackground());
			for (int hv=0; hv<mHVCount; hv++)
				paintGraph(mSplitter.getGraphBounds(hv), hv, transparentBG);

			mG.setColor(getContrastGrey(SCALE_STRONG, getTitleBackground()));
			mG.setFont(getFont().deriveFont(Font.BOLD, scaledFontHeight));

			String[][] categoryTitle = new String[2][];
			categoryTitle[0] = mShowEmptyInSplitView ? mTableModel.getCategoryList(mSplittingColumn[0])
													 : getVisibleCategoryList(mSplittingColumn[0]);
			if (mSplittingColumn[1] != cColumnUnassigned)
				categoryTitle[1] = mShowEmptyInSplitView ? mTableModel.getCategoryList(mSplittingColumn[1])
						 								 : getVisibleCategoryList(mSplittingColumn[1]);

			for (int hv=0; hv<mHVCount; hv++) {
				Rectangle titleArea = mSplitter.getTitleBounds(hv);
				mG.setClip(titleArea);

				int molWidth = Math.min(titleArea.width*2/5, titleArea.height*3/2);
				int cat1Index = (mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv);
				String shortTitle1 = categoryTitle[0][cat1Index];
				String title1 = mTableModel.getColumnTitle(mSplittingColumn[0])+" = "+shortTitle1;
				int title1Width = mSplittingDepictor[0] == null ?
								  mG.getFontMetrics().stringWidth(title1)
								: molWidth;
				String shortTitle2 = null;
				String title2 = null;
				int title2Width = 0;
				int totalWidth = title1Width;
				if (mSplittingColumn[1] != cColumnUnassigned) {
					shortTitle2 = categoryTitle[1][mSplitter.getVIndex(hv)];
					title2 = mTableModel.getColumnTitle(mSplittingColumn[1])+" = "+shortTitle2;
					title2Width = mSplittingDepictor[1] == null ?
								  mG.getFontMetrics().stringWidth(title2)
								: molWidth;
					totalWidth += title2Width + mG.getFontMetrics().stringWidth("|");
					}

				int textY = titleArea.y+(1+titleArea.height-scaledFontHeight)/2+mG.getFontMetrics().getAscent();

				if (totalWidth > titleArea.width) {
					title1 = shortTitle1;
					title1Width = mSplittingDepictor[0] == null ?
								  mG.getFontMetrics().stringWidth(shortTitle1)
								: molWidth;
					totalWidth = title1Width;
					if (mSplittingColumn[1] != cColumnUnassigned) {
						title2 = shortTitle2;
						title2Width = mSplittingDepictor[1] == null ?
									  mG.getFontMetrics().stringWidth(title2)
									: molWidth;
						totalWidth += title2Width + mG.getFontMetrics().stringWidth("|");
						}
					}

				int x1 = titleArea.x+(titleArea.width-totalWidth)/2;
				if (mSplittingDepictor[0] == null)
					mG.drawString(title1, x1, textY);
				else if (mSplittingDepictor[0][cat1Index] != null) {
					Rectangle.Double r = new Rectangle.Double(x1, titleArea.y, molWidth, titleArea.height);
					int maxAVBL = Depictor2D.cOptAvBondLen;
					if (mIsHighResolution)
						maxAVBL *= mFontScaling;
					mSplittingDepictor[0][cat1Index].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
					mSplittingDepictor[0][cat1Index].paint(mG);
					}

				if (mSplittingColumn[1] != cColumnUnassigned) {
					mG.drawString("|", titleArea.x+(titleArea.width-totalWidth)/2+title1Width,
								  textY);

					int x2 = titleArea.x+(totalWidth+titleArea.width)/2-title2Width;
					if (mSplittingDepictor[1] == null)
						mG.drawString(title2, x2, textY);
					else if (mSplittingDepictor[1][mSplitter.getVIndex(hv)] != null) {
						Rectangle.Double r = new Rectangle.Double(x2, titleArea.y, molWidth, titleArea.height);
						int maxAVBL = Depictor2D.cOptAvBondLen;
						if (mIsHighResolution)
							maxAVBL *= mFontScaling;
						mSplittingDepictor[1][mSplitter.getVIndex(hv)].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
						mSplittingDepictor[1][mSplitter.getVIndex(hv)].paint(mG);
						}
					}
				}
			setFontHeightAndScaleToSplitView(mFontHeight);	// set font back to plain
			mG.setClip(null);
			}
		else {
			mSplitter = null;
			paintGraph(bounds, 0, transparentBG);
			}

		Rectangle baseGraphRect = getGraphBounds(isSplitView() ? mSplitter.getGraphBounds(0) : bounds);

		if (baseGraphRect.width <= 0 || baseGraphRect.height <= 0)
			return;

		switch (mChartType) {
		case cChartTypeBars:
			paintBarChart(mG, baseGraphRect);
			break;
		case cChartTypePies:
			paintPieChart(mG, baseGraphRect);
			break;
		case cChartTypeScatterPlot:
			paintMarkers(baseGraphRect);
			break;
		case cChartTypeBoxPlot:
		case cChartTypeWhiskerPlot:
			paintMarkers(baseGraphRect);
			paintBoxOrWhiskerPlot(mG, baseGraphRect);
			break;
			}

		paintLegend(bounds, transparentBG);
		}

	/**
	 * Returns the bounds of the graph area, provided that the given point
	 * is part of it. If we have split views, then the graph area of that view
	 * is returned, which contains the the given point.
	 * If point(x,y) is outside of the/an graph area, then null is returned.
	 * Scale, legend and border area is not part of the graph bounds.
	 * @param screenX
	 * @param screenY
	 * @return graph bounds or null (does not include retinal factor)
	 */
	public Rectangle getGraphBounds(int screenX, int screenY) {
		int width = getWidth();
		int height = getHeight();
		Insets insets = getInsets();
		Rectangle allBounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
		if (isSplitView()) {
			for (int hv=0; hv<mHVCount; hv++) {
				Rectangle bounds = getGraphBounds(mSplitter.getGraphBounds(hv));
				Rectangle tempBounds = new Rectangle(bounds);
				tempBounds.grow(2,2);	// to get away with uncertaintees of rounded input coordinates
				if (tempBounds.contains(screenX, screenY))
					return bounds;
				}
			return null;
			}
		else {
			for (JVisualizationLegend legend:mLegendList)
				allBounds.height -= legend.getHeight();
			Rectangle bounds = getGraphBounds(allBounds);
			Rectangle tempBounds = new Rectangle(bounds);
			tempBounds.grow(2,2);	// to get away with uncertaintees of rounded input coordinates
			return tempBounds.contains(screenX, screenY) ? bounds : null;
			}
		}

	private Rectangle getGraphBounds(Rectangle bounds) {
		float scaledFontHeight = scaleIfSplitView(mFontHeight);
		Rectangle graphBounds = new Rectangle(
				bounds.x + mBorder + mNaNSize[0] + mScaleSize[1],
				bounds.y + mBorder,
				bounds.width - mNaNSize[0] - mScaleSize[1] - 2 * mBorder,
				bounds.height - mNaNSize[1] - mScaleSize[0] - 2 * mBorder);

		if (mTreeNodeList == null) {
			if (mScaleTitleCentered) {
				if (showScale(0)) {
					graphBounds.width -= ARROW_TIP_SIZE * scaledFontHeight;    // arrow triangle on x-axis
					graphBounds.height -= 2*scaledFontHeight;
					}
				if (showScale(1)) {
					graphBounds.x += 2*scaledFontHeight;
					graphBounds.width -= 2*scaledFontHeight;
					graphBounds.y += ARROW_TIP_SIZE * scaledFontHeight;
					graphBounds.height -= ARROW_TIP_SIZE * scaledFontHeight;
					}
				}
			else {
				if (showScale(0)) {
					graphBounds.width -= ARROW_TIP_SIZE * scaledFontHeight;    // arrow triangle on x-axis
					graphBounds.height -= scaledFontHeight;
					}
				if (showScale(1)) {
					graphBounds.y += scaledFontHeight;
					graphBounds.height -= scaledFontHeight;
					}
				}
			}

		return graphBounds;
		}

	private void paintGraph(Rectangle bounds, int hvIndex, boolean transparentBG) {
		setFontHeightAndScaleToSplitView(mFontHeight);  // calculateCoordinates() requires a proper getStringWidth()
		if (!mCoordinatesValid)
			calculateCoordinates(mG, bounds);

		Rectangle graphRect = getGraphBounds(bounds);

		// this needs to be done before the scales are actually painted
		adaptDoubleScalesForStatisticalLabels(graphRect);

		if (hasColorBackground()) {
			if (mSplitter != null
			 && (mSplitter.getHCount() != mBackgroundHCount
			  || mSplitter.getVCount() != mBackgroundVCount))
				mBackgroundValid = false;

			if (!mBackgroundValid)
				calculateBackground(graphRect, transparentBG);
			}

		if (mShowNaNValues)
			drawNaNArea(mG, graphRect);

		if (mBackgroundImage != null
		 || hasColorBackground())
			drawBackground(mG, graphRect, hvIndex);

		if (mTreeNodeList == null) {
			if (mGridMode != cGridModeHidden || mScaleMode != cScaleModeHidden)
				drawGrid(mG, graphRect);	// draws grid and scale labels
			if (mScaleMode != cScaleModeHidden)
				drawAxes(mG, graphRect);
			}
		}

	private boolean hasColorBackground() {
		return mChartType != cChartTypeBars
	   		&& mChartType != cChartTypeBoxPlot
	   		&& mBackgroundColor.getColorColumn() != cColumnUnassigned;
		}

	private void paintMarkers(Rectangle baseGraphRect) {
		if (mTreeNodeList != null && mTreeNodeList.length == 0)
			return;	// We have a detail graph view without root node (no active row chosen)

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency));
			mG.setComposite(composite);
			}

		boolean showAnyLabels = showAnyLabels();
		boolean drawConnectionLinesInFocus = (mChartType == cChartTypeScatterPlot || mTreeNodeList != null) && drawConnectionLines();

		if (mConnectionColumn != cColumnUnassigned
		 && mRelativeMarkerSize == 0.0
		 && !showAnyLabels) {
			// don't draw markers if we have connection lines and marker size is zero
			if (drawConnectionLinesInFocus)
				drawConnectionLines(true, true);
			}
		else {
			int focusFlagNo = getFocusFlag();
			int firstFocusIndex = 0;
			if (focusFlagNo != -1) {
				int index2 = mDataPoints-1;
				while (firstFocusIndex<index2) {
					if (mPoint[firstFocusIndex].record.isFlagSet(focusFlagNo)) {
						while (mPoint[index2].record.isFlagSet(focusFlagNo)
							&& index2 > firstFocusIndex)
							index2--;
						if (index2 == firstFocusIndex)
							break;
						VisualizationPoint temp = mPoint[firstFocusIndex];
						mPoint[firstFocusIndex] = mPoint[index2];
						mPoint[index2] = temp;
						}
					firstFocusIndex++;
					}
				}

			boolean isTreeView = isTreeViewGraph();
			boolean isDarkBackground = (ColorHelper.perceivedBrightness(getViewBackground()) <= 0.5);
			MultiValueBars mvbi = (mMultiValueMarkerMode == cMultiValueMarkerModeBars && mMultiValueMarkerColumns != null) ?
					new MultiValueBars() : null;

			int labelFlagNo = getLabelFlag();

			MarkerLabelInfo[] labelInfo = null;
			if (showAnyLabels) {
				labelInfo = new MarkerLabelInfo[mLabelColumn.length];
				for (int i=0; i<mLabelColumn.length; i++)
					if (mLabelColumn[i] != -1)
						labelInfo[i] = new MarkerLabelInfo();
				}

			for (int i=0; i<mDataPoints; i++) {
				if (drawConnectionLinesInFocus && i == firstFocusIndex)
					drawConnectionLines(true, true);

				boolean drawLabels = false;
				if (isVisible(mPoint[i])
				 && (mChartType == cChartTypeScatterPlot
				  || mChartType == cChartTypeWhiskerPlot
				  || mPoint[i].chartGroupIndex == -1
				  || mTreeNodeList != null)) {
					VisualizationPoint vp = mPoint[i];
					vp.width = vp.height = (int)getMarkerSize(vp);
					boolean inFocus = (focusFlagNo == -1 || vp.record.isFlagSet(focusFlagNo));

					Color color = (mUseAsFilterFlagNo != -1 && !vp.record.isFlagSet(mUseAsFilterFlagNo)) ? cUseAsFilterColor
							: (vp.record.isSelected() && mFocusList != cFocusOnSelection) ?
									VisualizationColor.cSelectedColor : mMarkerColor.getColorList()[vp.colorIndex];

					Color markerColor = inFocus ? color : VisualizationColor.lowContrastColor(color, getViewBackground());
					Color outlineColor = isDarkBackground ? markerColor.brighter() : markerColor.darker();

					drawLabels = showAnyLabels && (labelFlagNo == cLabelsOnAllRows || vp.record.isFlagSet(labelFlagNo));
					if (drawLabels) {
						for (int j = 0; j<mLabelColumn.length; j++) {
							if (mLabelColumn[j] != -1) {
								calculateMarkerLabel(vp, j, isTreeView, baseGraphRect, labelInfo[j]);
								drawMarkerLabelLine(vp, labelInfo[j], outlineColor);
								}
							}
						}

					if (vp.width != 0
					 && (mLabelColumn[MarkerLabelDisplayer.cMidCenter] == cColumnUnassigned || !drawLabels)) {
						if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null) {
							if (mMultiValueMarkerMode == cMultiValueMarkerModeBars)
								drawMultiValueBars(color, inFocus, isDarkBackground, vp.width, mvbi, vp);
							else
								drawMultiValuePies(color, inFocus, isDarkBackground, vp.width, vp);
							}
						else {
							int shape = (mMarkerShapeColumn != cColumnUnassigned) ? vp.shape : mIsFastRendering ? 1 : 0;
							drawMarker(markerColor, outlineColor, shape, vp.width, vp.screenX, vp.screenY);
							}
						}

					if (drawLabels)
						drawMarkerLabels(labelInfo, markerColor, outlineColor, isTreeView);
					}

				if (!drawLabels)
					mPoint[i].removeNonCustomLabelPositions();
				}
			}

		if (original != null)
			mG.setComposite(original);

		if (mChartType == cChartTypeScatterPlot && mTreeNodeList == null) {
			if (mCurveInfo != cCurveModeNone)
				drawCurves(baseGraphRect);

			if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE)
				drawCorrelationCoefficient(baseGraphRect);
			}
		}

	private void drawMarkerLabels(MarkerLabelInfo[] labelInfo, Color markerColor, Color outlineColor, boolean isTreeView) {
		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mMarkerLabelSize * mFontHeight);

		boolean isDarkBackground = (ColorHelper.perceivedBrightness(mLabelBackground) <= 0.5);
		Color labelColor = isDarkBackground ? markerColor : markerColor.darker();

		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned
				&& (!mLabelsInTreeViewOnly || isTreeView))
			drawMarkerLabel(labelInfo[MarkerLabelDisplayer.cMidCenter], labelColor, outlineColor);

		for (int i=0; i<labelInfo.length; i++)
			if (i != MarkerLabelDisplayer.cMidCenter
					&& mLabelColumn[i] != cColumnUnassigned
					&& (!mLabelsInTreeViewOnly || isTreeView))
				drawMarkerLabel(labelInfo[i], labelColor, outlineColor);

		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mFontHeight);
		}

	/**
	 * Calculates the marker label specified by position considering vp.width and vp.height
	 * for exact label location. If position is midCenter and therefore replaces
	 * the original marker and, thus, redefines marker dimensions as being mid-center label size,
	 * then vp.width and vp.height are updated in place to hold the label size instead of marker size.
	 * @param vp
	 * @param position
	 * @param isTreeView
	 * @param baseGraphRect
	 */
	private void calculateMarkerLabel(VisualizationPoint vp, int position, boolean isTreeView, Rectangle baseGraphRect, MarkerLabelInfo mli) {
		int column = mLabelColumn[position];
		boolean isMolecule = CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column));
		int x = Math.round(vp.screenX);
		int y = Math.round(vp.screenY);
		int w,h;
		float zoom = Float.isNaN(mMarkerSizeZoomAdaption) ? 1f : mMarkerSizeZoomAdaption;
		mli.fontSize = zoom * getLabelFontSize(vp, position, isTreeView);
		mli.label = null;
		mli.depictor = null;
		Rectangle2D.Double molRect = null;

		// in case we have an empty label replacing the marker
		if (position == MarkerLabelDisplayer.cMidCenter) {
			vp.width = 0;
			vp.height = 0;
			}

		if (isMolecule) {
			if (mLabelMolecule == null)
				mLabelMolecule = new StereoMolecule();
			StereoMolecule mol = mTableModel.getChemicalStructure(vp.record, column, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, mLabelMolecule);
			if (mol == null)
				return;

			mli.depictor = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
			mli.depictor.validateView(mG, DEPICTOR_RECT,
					Depictor2D.cModeInflateToHighResAVBL+Math.max(1, (int)(256*zoom*scaleIfSplitView(getLabelAVBL(vp, position, isTreeView)))));
			molRect = mli.depictor.getBoundingRect();
			w = (int)molRect.width;
			h = (int)molRect.height;
			}
		else {
			mli.label = mTableModel.getValue(vp.record, column);
			if (mli.label.length() == 0) {
				mli.label = null;
				return;
				}

			setFontHeightAndScaleToSplitView(mli.fontSize);
			w = mG.getFontMetrics().stringWidth(mli.label);
			h = mG.getFontMetrics().getHeight();
			}

		VisualizationLabelPosition labelPosition = vp.getOrCreateLabelPosition(column);
		if (labelPosition.isCustom()) {
			float dataX = labelPosition.getX();
			float dataY = labelPosition.getY();
			float relX = (mAxisVisMax[0] == mAxisVisMin[0]) ? 0.5f : (dataX - mAxisVisMin[0]) / (mAxisVisMax[0] - mAxisVisMin[0]);
			float relY = (mAxisVisMax[1] == mAxisVisMin[1]) ? 0.5f : (dataY - mAxisVisMin[1]) / (mAxisVisMax[1] - mAxisVisMin[1]);
			x = baseGraphRect.x + Math.round(relX * baseGraphRect.width);
			y = baseGraphRect.y + baseGraphRect.height - Math.round(relY * baseGraphRect.height);

			// when zooming show labels of visible markers that would be zoomed out of the view at the view edge
			if (x < baseGraphRect.x)
				x = baseGraphRect.x;
			else if (x > baseGraphRect.x + baseGraphRect.width)
				x = baseGraphRect.x + baseGraphRect.width;
			if (y < baseGraphRect.y)
				y = baseGraphRect.y;
			else if (y > baseGraphRect.y + baseGraphRect.height)
				y = baseGraphRect.x + baseGraphRect.height;

			x -= w/2;
			y -= h/2;

			if (mHVCount != 1) {
				x += mSplitter.getHIndex(vp.hvIndex) * mSplitter.getGridWidth();
				y += mSplitter.getVIndex(vp.hvIndex) * mSplitter.getGridHeight();
				}
			}
		else {
			switch (position) {
				case MarkerLabelDisplayer.cTopLeft:
					x -= vp.width/2 + w;
					y -= vp.height/2 + h;
					break;
				case MarkerLabelDisplayer.cTopCenter:
					x -= w/2;
					y -= vp.height/2 + h;
					break;
				case MarkerLabelDisplayer.cTopRight:
					x += vp.width/2;
					y -= vp.height/2 + h;
					break;
				case MarkerLabelDisplayer.cMidLeft:
					x -= vp.width*2/3 + w;
					y -= h/2;
					break;
				case MarkerLabelDisplayer.cMidCenter:
					vp.width = w;
					vp.height = h;
					x -= w/2;
					y -= h/2;
					break;
				case MarkerLabelDisplayer.cMidRight:
					x += vp.width*2/3;
					y -= h/2;
					break;
				case MarkerLabelDisplayer.cBottomLeft:
					x -= vp.width/2 + w;
					y += vp.height/2;
					break;
				case MarkerLabelDisplayer.cBottomCenter:
					x -= w/2;
					y += vp.height/2;
					break;
				case MarkerLabelDisplayer.cBottomRight:
					x += vp.width/2;
					y += vp.height/2;
					break;
				}
			}

		int border = 0;

		if (mShowLabelBackground)
			border = (int)(0.15f * mli.fontSize);

		mli.x1 = x - border;
		mli.y1 = y - border;
		mli.x2 = x + w + 2 * border;
		mli.y2 = y + h + 2 * border;

		if (!mIsHighResolution && position != MarkerLabelDisplayer.cMidCenter)
			labelPosition.setScreenLocation(mli.x1, mli.y1, mli.x2, mli.y2, 0);

		if (isMolecule)
			mli.depictor.applyTransformation(new DepictorTransformation(1.0f, x - molRect.x, y - molRect.y));
		else {
			mli.x = x;
			mli.y = y;
			}
		}

	private void drawMarkerLabelLine(VisualizationPoint vp, MarkerLabelInfo mli, Color color) {
		if (mli.label != null || mli.depictor != null) {
			Point connectionPoint = getLabelConnectionPoint(vp.screenX, vp.screenY, mli.x1, mli.y1, mli.x2, mli.y2);
			if (connectionPoint != null) {
				mG.setColor(color);
				mG.setStroke(mNormalLineStroke);
				mG.drawLine(Math.round(vp.screenX), Math.round(vp.screenY), connectionPoint.x, connectionPoint.y);
				}
			}
		}

	private void drawMarkerLabel(MarkerLabelInfo mli, Color labelColor, Color outlineColor) {
		if (mli.label != null || mli.depictor != null) {
			if (mShowLabelBackground) {
				Composite original = null;
				if (mLabelBackgroundComposite != null) {
					original = mG.getComposite();
					mG.setComposite(mLabelBackgroundComposite);
					}

				mG.setColor(mLabelBackground);
				mG.fillRect(mli.x1, mli.y1, mli.x2-mli.x1, mli.y2-mli.y1);
				mG.setColor(outlineColor);
				mG.setStroke(mThinLineStroke);
				mG.drawRect(mli.x1, mli.y1, mli.x2-mli.x1, mli.y2-mli.y1);

				if (original != null)
					mG.setComposite(original);
				}

			// For custom located labels we may need to draw a line from marker to label edge

			if (mli.depictor != null) {
				mli.depictor.setOverruleColor(labelColor, getViewBackground());
				mli.depictor.paint(mG);
				}
			else {
				mG.setColor(labelColor);
				setFontHeightAndScaleToSplitView(mli.fontSize);
				mG.drawString(mli.label, mli.x, mli.y + mG.getFontMetrics().getAscent());
				}
			}
		}

	protected void drawMarker(Color color, int shape, int size, int x, int y) {
		drawMarker(color, getContrastGrey(MARKER_OUTLINE), shape, size, x, y);
		}

	private boolean renderFaster() {
		return mSkipPaintDetails;
		}

	private void drawMarker(Color color, Color outlineColor, int shape, float size, float x, float y) {
		float halfSize = size/2;
		float sx,sy;
		GeneralPath polygon;

		mG.setColor(size > OUTLINE_LIMIT ? color : outlineColor);
		mG.setStroke(mThinLineStroke);
		switch (shape) {
		case 0:
			mG.fill(new Ellipse2D.Float(x-halfSize, y-halfSize, size, size));
			if (!renderFaster() && size > OUTLINE_LIMIT) {
				mG.setColor(outlineColor);
				mG.draw(new Ellipse2D.Float(x-halfSize, y-halfSize, size, size));
				}
			break;
		case 1:
			if (!mIsFastRendering) {
				mG.fill(new Rectangle2D.Float(x - halfSize, y - halfSize, size, size));
				if (!renderFaster() && size > OUTLINE_LIMIT) {
					mG.setColor(outlineColor);
					mG.draw(new Rectangle2D.Float(x - halfSize, y - halfSize, size, size));
					}
				}
			else {
				int x1 = Math.round(x - halfSize);
				int y1 = Math.round(y - halfSize);
				int s = Math.round(size);
				mG.fillRect(x1, y1, s, s);
				if (!renderFaster() && size > OUTLINE_LIMIT) {
					mG.setColor(outlineColor);
					mG.drawRect(x1, y1, s, s);
					}
				}
			break;
		case 2:
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
			polygon.moveTo(x-halfSize, y+size/3);
			polygon.lineTo(x+halfSize, y+size/3);
			polygon.lineTo(x, y-2*size/3);
			polygon.closePath();
			mG.fill(polygon);
			if (!renderFaster() && size > OUTLINE_LIMIT) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
				}
			break;
		case 3:
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
			polygon.moveTo(x-halfSize, y);
			polygon.lineTo(x, y+halfSize);
			polygon.lineTo(x+halfSize, y);
			polygon.lineTo(x, y-halfSize);
			polygon.closePath();
			mG.fill(polygon);
			if (!renderFaster() && size > OUTLINE_LIMIT) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
				}
			break;
		case 4:
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
			polygon.moveTo(x-halfSize, y-size/3);
			polygon.lineTo(x+halfSize, y-size/3);
			polygon.lineTo(x, y+2*size/3);
			polygon.closePath();
			mG.fill(polygon);
			if (!renderFaster() && size > OUTLINE_LIMIT) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
			}
			break;
		case 5:
			sx = size/4;
			sy = sx+halfSize;
			mG.fill(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
			if (!renderFaster() && size > OUTLINE_LIMIT) {
				mG.setColor(outlineColor);
				mG.draw(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
				}
			break;
		case 6:
			sy = size/4;
			sx = sy+halfSize;
			mG.fill(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
			if (!renderFaster() && size > OUTLINE_LIMIT) {
				mG.setColor(outlineColor);
				mG.draw(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
				}
			break;
			}
		}

	private void drawMultiValueBars(Color color, boolean inFocus, boolean isDarkBackground, float size, MultiValueBars info, VisualizationPoint vp) {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned
		 && color != VisualizationColor.cSelectedColor
		 && color != VisualizationColor.cUseAsFilterColor) {
			if (mMultiValueMarkerColor == null || mMultiValueMarkerColor.length != mMultiValueMarkerColumns.length)
				mMultiValueMarkerColor = VisualizationColor.createDiverseColorList(mMultiValueMarkerColumns.length);
			color = null;
			}

		info.calculate(size, vp);
		int x = info.firstBarX;
		mG.setColor(getContrastGrey(1f));
		mG.drawLine(x-info.barWidth/2, info.zeroY, x+mMultiValueMarkerColumns.length*info.barWidth+info.barWidth/2, info.zeroY);
		for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
			if (!Float.isNaN(info.relValue[i])) {
				Color barColor = (color != null) ? color : mMultiValueMarkerColor[i];
				Color fillColor = inFocus ? barColor : VisualizationColor.lowContrastColor(barColor, getViewBackground());
				mG.setColor(fillColor);
				mG.fillRect(x, info.barY[i], info.barWidth, info.barHeight[i]);
				}
			x += info.barWidth;
			}
		}

	private void drawMultiValuePies(Color color, boolean inFocus, boolean isDarkBackground, float size, VisualizationPoint vp) {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned
		 && color != VisualizationColor.cSelectedColor
		 && color != VisualizationColor.cUseAsFilterColor) {
			if (mMultiValueMarkerColor == null || mMultiValueMarkerColor.length != mMultiValueMarkerColumns.length)
				mMultiValueMarkerColor = VisualizationColor.createDiverseColorList(mMultiValueMarkerColumns.length);
			color = null;
			}

		size *= 0.5f  * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
			// one sqrt because of area, 2nd sqrt to grow under-proportional with number of pie pieces

		float angleIncrement = 360f / mMultiValueMarkerColumns.length;
		float angle = 90f - angleIncrement;
		for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
			float r = size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]);
			Color piePieceColor = (color != null) ? color : mMultiValueMarkerColor[i];
			Color fillColor = inFocus ? piePieceColor : VisualizationColor.lowContrastColor(piePieceColor, getViewBackground());
			mG.setColor(fillColor);
			mG.fillArc(Math.round(vp.screenX-r), Math.round(vp.screenY-r), Math.round(2*r-1), Math.round(2*r-1), Math.round(angle), Math.round(angleIncrement));
			Color lineColor = isDarkBackground ? fillColor.brighter() : fillColor.darker();
			mG.setColor(lineColor);
			mG.drawArc(Math.round(vp.screenX-r), Math.round(vp.screenY-r), Math.round(2*r-1), Math.round(2*r-1), Math.round(angle), Math.round(angleIncrement));
			angle -= angleIncrement;
			}
		}

	/**
	 * If no connection lines need to be drawn, then this method does nothing and returns false.
	 * Otherwise, if no focus is set, then this method draws all connection lines and returns false.
	 * Otherwise, this method draws those lines connecting markers, which are not in focus and
	 * returns true to indicate that connection line drawing is not completed yet. In this case
	 * drawConnectionLines(true, true) needs to be called after drawing those markers that are
	 * not in focus.
	 * @return true if drawConnectionLines(true, true) needs to be called later
	 */
	private boolean drawConnectionLines() {
		if (mPoint.length == 0)
			return false;

		if (mConnectionColumn == cColumnUnassigned
		 || mConnectionColumn == cConnectionColumnConnectCases)
			return false;

		if (!mIsHighResolution && mAbsoluteConnectionLineWidth < 0.5f)
			return false;

		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);
		if (value == null)
			return drawCategoryConnectionLines();

		int referencedColumn = mTableModel.findColumn(value);
		if (referencedColumn != -1)
			return drawReferenceConnectionLines(referencedColumn);

		return false;
		}

	/**
	 * 
	 * @param considerFocus
	 * @param inFocus
	 */
	private void drawConnectionLines(boolean considerFocus, boolean inFocus) {
		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);

		if (value == null)
			drawCategoryConnectionLines(considerFocus, inFocus);
		else
			drawReferenceConnectionLines(considerFocus, inFocus);
		}

	private boolean drawCategoryConnectionLines() {
		int connectionOrderColumn = (mConnectionOrderColumn == cColumnUnassigned) ?
										mAxisIndex[0] : mConnectionOrderColumn;
		if (connectionOrderColumn == cColumnUnassigned)
			return false;

		if (mConnectionLinePoint == null || mConnectionLinePoint.length != mPoint.length)
			mConnectionLinePoint = new VisualizationPoint[mPoint.length];
		for (int i=0; i<mPoint.length; i++)
			mConnectionLinePoint[i] = mPoint[i];

		Arrays.sort(mConnectionLinePoint, new Comparator<VisualizationPoint>() {
			public int compare(VisualizationPoint p1, VisualizationPoint p2) {
				return compareConnectionLinePoints(p1, p2);
				}
			} );

		mConnectionFromIndex1 = 0;
		while (mConnectionFromIndex1<mConnectionLinePoint.length
			&& !isVisibleExcludeNaN(mConnectionLinePoint[mConnectionFromIndex1]))
			mConnectionFromIndex1++;

		if (mConnectionFromIndex1 == mConnectionLinePoint.length)
			return false;

		mConnectionFromIndex2 = getNextChangedConnectionLinePointIndex(mConnectionFromIndex1);
		if (mConnectionFromIndex2 == mConnectionLinePoint.length)
			return false;

		drawCategoryConnectionLines(mFocusList != cFocusNone, false);
		return (mFocusList != cFocusNone);
		}

	private void drawCategoryConnectionLines(boolean considerFocus, boolean inFocus) {
		long focusMask = (mFocusList == cFocusNone) ? 0
					   : (mFocusList == cFocusOnSelection) ? CompoundRecord.cFlagMaskSelected
					   : mTableModel.getListHandler().getListMask(mFocusList);

		int fromIndex1 = mConnectionFromIndex1;
		int fromIndex2 = mConnectionFromIndex2;

		mG.setStroke(mConnectionStroke);

		while (true) {
			int toIndex1 = fromIndex2;

			while (toIndex1<mConnectionLinePoint.length
				&& !isVisibleExcludeNaN(mConnectionLinePoint[toIndex1]))
				toIndex1++;

			if (toIndex1 == mConnectionLinePoint.length)
				return;

			int toIndex2 = getNextChangedConnectionLinePointIndex(toIndex1);

			if (isConnectionLinePossible(mConnectionLinePoint[fromIndex1], mConnectionLinePoint[toIndex1]))
				for (int i=fromIndex1; i<fromIndex2; i++)
					if (isVisibleExcludeNaN(mConnectionLinePoint[i]))
						for (int j=toIndex1; j<toIndex2; j++)
							if (isVisibleExcludeNaN(mConnectionLinePoint[j])
							 && (!considerFocus
							  || (inFocus
								^ (mConnectionLinePoint[j].record.getFlags() & focusMask) == 0)))
								drawConnectionLine(mConnectionLinePoint[i], mConnectionLinePoint[j], considerFocus && !inFocus, 0.0f, false);

			fromIndex1 = toIndex1;
			fromIndex2 = toIndex2;
			}
		}

	private boolean drawReferenceConnectionLines(int referencedColumn) {
		if (mConnectionLineMap == null)
			mConnectionLineMap = createReferenceMap(mConnectionColumn, referencedColumn);

		drawReferenceConnectionLines(mFocusList != cFocusNone, false);

		return (mFocusList != cFocusNone);
		}

	private void drawReferenceConnectionLines(boolean considerFocus, boolean inFocus) {
		long focusMask = (mFocusList == cFocusNone) ? 0
					   : (mFocusList == cFocusOnSelection) ? CompoundRecord.cFlagMaskSelected
					   : mTableModel.getListHandler().getListMask(mFocusList);
		int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn,
				CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
		boolean isRedundant = CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(
				mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType));

		mG.setStroke(mConnectionStroke);
		Composite original = (strengthColumn == -1) ? null : mG.getComposite();

		if (mTreeNodeList != null) {
			for (int layer=1; layer<mTreeNodeList.length; layer++) {
				for (VisualizationNode node:mTreeNodeList[layer]) {
					VisualizationPoint vp1 = node.getVisualizationPoint();
					VisualizationPoint vp2 = node.getParentNode().getVisualizationPoint();
					float strength = node.getStrength();
					if (isVisible(vp1)
					 && isVisible(vp2)
					 && (!considerFocus
					  || (inFocus
						^ (vp1.record.getFlags() & vp2.record.getFlags() & focusMask) == 0))) {
						if (strength > 0f)
							drawConnectionLine(vp1, vp2, considerFocus && !inFocus, 1f-strength, !isRedundant);
						}
					}
				}
			}
		else {
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

			for (VisualizationPoint vp1:mPoint) {
				if (isVisible(vp1)) {
					byte[] data = (byte[])vp1.record.getData(mConnectionColumn);
					if (data != null) {
						String[] entry = mTableModel.separateEntries(new String(data));
	
						String[] strength = null;
						if (strengthColumn != -1) {
							byte[] strengthData = (byte[])vp1.record.getData(strengthColumn);
							if (strengthData != null) {
								strength = mTableModel.separateEntries(new String(strengthData));
								if (strength.length != entry.length)
									strength = null;
								}
							}
	
						int index = 0;
						for (String ref:entry) {
							VisualizationPoint vp2 = mConnectionLineMap.get(ref.getBytes());
							if (vp2 != null && isVisible(vp2)
							 && (!isRedundant || (vp1.record.getID() < vp2.record.getID()))
							 && (!considerFocus
							  || (inFocus
							   ^ (vp1.record.getFlags() & vp2.record.getFlags() & focusMask) == 0))) {
								float transparency = 0.0f;
								if (strength != null) {
									try {
										float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[index++], strengthColumn)));
										transparency = Float.isNaN(value) ? 1.0f : (float)((max-value) / dif);
										}
									catch (NumberFormatException nfe) {}
									}
								if (transparency != 1.0f) {
									drawConnectionLine(vp1, vp2, considerFocus && !inFocus, transparency, !isRedundant);
									}
								}
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);
		}

	/**
	 * Draws a connection line between the given points. 
	 * If transparency is different from 0.0, then this method sets a Composite on mG. In this case the calling
	 * method needs to save and restore the old Composite before/after calling this method.
	 * If fast render mode is active, transparency is simulated by adapting the line color to the current background.
	 * @param p1
	 * @param p2
	 * @param outOfFocus
	 * @param transparency if 0.0 then the current composite is not touched and the line drawn with the current g2d transparency
	 */
	private void drawConnectionLine(VisualizationPoint p1, VisualizationPoint p2, boolean outOfFocus, float transparency, boolean showArrow) {
		Color color = ColorHelper.intermediateColor(mMarkerColor.getColorList()[p1.colorIndex],
													mMarkerColor.getColorList()[p2.colorIndex], 0.5f);
		if (transparency != 0.0f) {
			if (!mIsFastRendering || mIsHighResolution)
				mG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - transparency));
			else
				color = ColorHelper.intermediateColor(color, getViewBackground(), transparency);
			}
		if (outOfFocus)
			color = VisualizationColor.lowContrastColor(color, getViewBackground());
		mG.setColor(color);
		mG.draw(new Line2D.Float(p1.screenX, p1.screenY, p2.screenX, p2.screenY));
		if (showArrow) {
			float dx = p2.screenX - p1.screenX;
			float dy = p2.screenY - p1.screenY;
			float len = (float)Math.sqrt(dx*dx+dy*dy);
			float s = 2.5f * mAbsoluteConnectionLineWidth;
			if (len > 2*s) {
				float xm = p1.screenX + dx / 2;
				float ym = p1.screenY + dy / 2;
				float ndx = dx * s / len;
				float ndy = dy * s / len;

				if (mIsConnectionLineInverted) {
					GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);
					polygon.moveTo(xm + ndx + ndy, ym + ndy - ndx);
					polygon.lineTo(Math.round(xm + ndx - ndy), ym + ndy + ndx);
					polygon.lineTo(Math.round(xm - ndx), ym - ndy);
					polygon.closePath();
					mG.fill(polygon);
					}
				else {
					GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);
					polygon.moveTo(xm - ndx + ndy, ym - ndy - ndx);
					polygon.lineTo(Math.round(xm - ndx - ndy), ym - ndy + ndx);
					polygon.lineTo(Math.round(xm + ndx), ym + ndy);
					polygon.closePath();
					mG.fill(polygon);
					}
				}
			}
		}

	private void paintBarChart(Graphics2D g, Rectangle baseRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float axisSize = mChartInfo.axisMax - mChartInfo.axisMin;

		float cellWidth = (mChartInfo.barAxis == 1) ?
				(float)baseRect.width / (float)mCategoryVisibleCount[0]
			  : (float)baseRect.height / (float)mCategoryVisibleCount[1];
		float cellHeight = (mChartInfo.barAxis == 1) ?
				(float)baseRect.height / (float)mCategoryVisibleCount[1]
			  : (float)baseRect.width / (float)mCategoryVisibleCount[0];


		// if we need space for statistics labels, then reduce the area that we have for the bar
		float labelSize = calculateStatisticsLabelSize(mChartInfo.barAxis);

		// barAreaHeight is the maximum bar length multiplied by padding factor cBarSpacingFactor
		float barAreaHeight = (labelSize == 0f) ? cellHeight
				: (cellHeight - Math.min(cMaxBarReduction * cellHeight, labelSize)) * cBarSpacingFactor;

		mChartInfo.barWidth = Math.min(0.2f * cellHeight, 0.5f * cellWidth / mCaseSeparationCategoryCount);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int catCount = mCategoryVisibleCount[0]*mCategoryVisibleCount[1]*mCaseSeparationCategoryCount; 

		float barBaseOffset = (mChartInfo.barBase - mChartInfo.axisMin) * barAreaHeight / axisSize;

		if (labelSize != 0f) {
			if (isRightBarChart())
				barBaseOffset = cellHeight;
			if (isCenteredBarChart())   // middle-centered bar area
				barBaseOffset -= barAreaHeight * (1f - 1f / cBarSpacingFactor) / 2f;
			}

		mChartInfo.innerDistance = new float[mHVCount][catCount];
		float[][] barPosition = new float[mHVCount][catCount];
		float[][][] barColorEdge = new float[mHVCount][catCount][colorCount+1];
		float csWidth = (mChartInfo.barAxis == 1 ? cellWidth : -cellWidth)
					   * mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (isSplitView()) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<mCategoryVisibleCount[0]; i++) {
				for (int j=0; j<mCategoryVisibleCount[1]; j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j*mCategoryVisibleCount[0])*mCaseSeparationCategoryCount+k;
						if (mChartInfo.pointsInCategory[hv][cat] > 0) {
							mChartInfo.innerDistance[hv][cat] = barAreaHeight * Math.abs(mChartInfo.barValue[hv][cat] - mChartInfo.barBase)
													   / (axisSize * (float)mChartInfo.pointsInCategory[hv][cat]);
							barPosition[hv][cat] = (mChartInfo.barAxis == 1) ?
									  baseRect.x + hOffset + i*cellWidth + cellWidth/2
									: baseRect.y + vOffset + baseRect.height - j*cellWidth - cellWidth/2;
	
							if (mCaseSeparationCategoryCount != 1)
								barPosition[hv][cat] += csOffset + k*csWidth;

							// right bound bars have negative values and the barBase set to 0f (==axisMax).
							// Move them left by one bar length and extend them to the right (done by positive inner distance).
							float barOffset = isRightBarChart() || (isCenteredBarChart() && mChartInfo.barValue[hv][cat] < 0f) ?
									barAreaHeight * (mChartInfo.barValue[hv][cat] - mChartInfo.barBase) / axisSize : 0f;
							barColorEdge[hv][cat][0] = (mChartInfo.barAxis == 1) ?
									baseRect.y + vOffset - barBaseOffset - barOffset + baseRect.height - cellHeight * j
								  : baseRect.x + hOffset + barBaseOffset + barOffset + cellHeight * i;
		
							for (int l=0; l<colorCount; l++)
								barColorEdge[hv][cat][l+1] = (mChartInfo.barAxis == 1) ?
										  barColorEdge[hv][cat][l] - mChartInfo.innerDistance[hv][cat]
										  * (float)mChartInfo.pointsInColorCategory[hv][cat][l]
										: barColorEdge[hv][cat][l] + mChartInfo.innerDistance[hv][cat]
										  * (float)mChartInfo.pointsInColorCategory[hv][cat][l];
							}
						}
					}
				}
			}

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			mG.setComposite(composite);
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				for (int k=0; k<colorCount; k++) {
					if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
						g.setColor(mChartInfo.color[k]);
						if (mChartInfo.barAxis == 1)
							g.fillRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
		  							   Math.round(barColorEdge[hv][cat][k+1]),
									   Math.round(mChartInfo.barWidth),
									   Math.round(barColorEdge[hv][cat][k])-Math.round(barColorEdge[hv][cat][k+1]));
						else
							g.fillRect(Math.round(barColorEdge[hv][cat][k]),
		  							   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
									   Math.round(barColorEdge[hv][cat][k+1]-Math.round(barColorEdge[hv][cat][k])),
									   Math.round(mChartInfo.barWidth));
						}
					}
				if (mChartInfo.pointsInCategory[hv][cat] > 0) {
					g.setColor(getContrastGrey(MARKER_OUTLINE));
					if (mChartInfo.barAxis == 1)
						g.drawRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
  								   Math.round(barColorEdge[hv][cat][colorCount]),
								   Math.round(mChartInfo.barWidth),
								   Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][colorCount]));
					else
						g.drawRect(Math.round(barColorEdge[hv][cat][0]),
  								   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
								   Math.round(barColorEdge[hv][cat][colorCount])-Math.round(barColorEdge[hv][cat][0]),
								   Math.round(mChartInfo.barWidth));
					}
				}
			}

		if (labelSize != 0f) {
			boolean labelIsLeftOrBelow = isRightBarChart();
			String[] lineText = new String[5];
			g.setColor(getContrastGrey(SCALE_STRONG));
			int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
			for (int hv=0; hv<mHVCount; hv++) {
				for (int cat=0; cat<catCount; cat++) {
					if (mChartInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = compileBarAndPieStatisticsLines(hv, cat, lineText);

						if (mChartInfo.barAxis == 1) {
							float x0 = barPosition[hv][cat];
							float y0 = labelIsLeftOrBelow ?
									   barColorEdge[hv][cat][0] + scaledFontHeight
									 : barColorEdge[hv][cat][colorCount] - ((float)lineCount - 0.5f) * scaledFontHeight;
							for (int line=0; line<lineCount; line++) {
								float x = Math.round(x0 - mG.getFontMetrics().stringWidth(lineText[line])/2);
								float y = y0 + line*scaledFontHeight;
								mG.drawString(lineText[line], Math.round(x), Math.round(y));
								}
							}
						else {
							float x0 = labelIsLeftOrBelow ?
									   barColorEdge[hv][cat][0] - scaledFontHeight/2
									 : barColorEdge[hv][cat][colorCount] + scaledFontHeight/2;
							float y0 = barPosition[hv][cat] - ((lineCount-1)*scaledFontHeight)/2 + mG.getFontMetrics().getAscent()/2;
							for (int line=0; line<lineCount; line++) {
								float x = x0 - (labelIsLeftOrBelow ? mG.getFontMetrics().stringWidth(lineText[line]) : 0f);
								float y = y0 + line*scaledFontHeight;
								mG.drawString(lineText[line], Math.round(x), Math.round(y));
								}
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);

		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				if (mChartInfo.barAxis == 1) {
					mPoint[i].screenX = Math.round(barPosition[hv][cat]);
					mPoint[i].screenY = Math.round(barColorEdge[hv][cat][0]
									  - mChartInfo.innerDistance[hv][cat]*(0.5f+(float)mPoint[i].chartGroupIndex));
					mPoint[i].width = Math.round(mChartInfo.barWidth);
					mPoint[i].height = Math.round(mChartInfo.innerDistance[hv][cat]);
					}
				else {
					mPoint[i].screenX = Math.round(barColorEdge[hv][cat][0]
									  + mChartInfo.innerDistance[hv][cat]*(0.5f+(float)mPoint[i].chartGroupIndex));
					mPoint[i].screenY = Math.round(barPosition[hv][cat]);
					mPoint[i].width = Math.round(mChartInfo.innerDistance[hv][cat]);
					mPoint[i].height = Math.round(mChartInfo.barWidth);
					}
				}
			}
		}

	private int compileBarAndPieStatisticsLines(int hv, int cat, String[] lineText) {
		boolean usesCounts = (mChartMode == cChartModeCount || mChartMode == cChartModePercent);
		boolean isLogarithmic = usesCounts ? false : mTableModel.isLogarithmicViewMode(mChartColumn);

		int lineCount = 0;
		if (!usesCounts && mShowMeanAndMedianValues) {
			float meanValue = isLogarithmic ? (float) Math.pow(10, mChartInfo.mean[hv][cat]) : mChartInfo.mean[hv][cat];
			lineText[lineCount++] = "mean=" + DoubleFormat.toString(meanValue, DIGITS);
			}
		if (!usesCounts && mShowStandardDeviation) {
			if (Float.isInfinite(mChartInfo.stdDev[hv][cat])) {
				lineText[lineCount++] = "\u03C3=Infinity";
				}
			else {
				double stdDev = isLogarithmic ? Math.pow(10, mChartInfo.stdDev[hv][cat]) : mChartInfo.stdDev[hv][cat];
				lineText[lineCount++] = "\u03C3=".concat(DoubleFormat.toString(stdDev, DIGITS));
				}
			}
		if (!usesCounts && mShowConfidenceInterval) {
			if (Float.isInfinite(mChartInfo.errorMargin[hv][cat])) {
				lineText[lineCount++] = "CI95: Infinity";
				}
			else {
				double ll = mChartInfo.barValue[hv][cat] - mChartInfo.errorMargin[hv][cat];
				double hl = mChartInfo.barValue[hv][cat] + mChartInfo.errorMargin[hv][cat];
				if (isLogarithmic) {
					ll = Math.pow(10, ll);
					hl = Math.pow(10, hl);
					}
				lineText[lineCount++] = "CI95: ".concat(DoubleFormat.toString(ll, DIGITS)).concat("-").concat(DoubleFormat.toString(hl, DIGITS));
				}
			}
		if (mShowValueCount) {
			lineText[lineCount++] = "N=" + mChartInfo.pointsInCategory[hv][cat];
			}
		return lineCount;
		}

	private void paintPieChart(Graphics2D g, Rectangle baseRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float labelHeight = Math.min(baseRect.height/2, calculateStatisticsLabelSize(1));  // we need the height of the label set
		float cellWidth = (float)baseRect.width / (float)mCategoryVisibleCount[0];
		float cellHeight = (float)(baseRect.height - labelHeight) / (float)mCategoryVisibleCount[1];
		float cellSize = Math.min(cellWidth, cellHeight);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int catCount = mCaseSeparationCategoryCount*mCategoryVisibleCount[0]*mCategoryVisibleCount[1]; 

		mChartInfo.pieSize = new float[mHVCount][catCount];
		mChartInfo.pieX = new float[mHVCount][catCount];
		mChartInfo.pieY = new float[mHVCount][catCount];
		float[][][] pieColorEdge = new float[mHVCount][catCount][colorCount+1];
		int preferredCSAxis = (cellWidth > cellHeight) ? 0 : 1;
		float csWidth = (preferredCSAxis == 0 ? cellWidth : -cellHeight)
						* mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (isSplitView()) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<mCategoryVisibleCount[0]; i++) {
				for (int j=0; j<mCategoryVisibleCount[1]; j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j*mCategoryVisibleCount[0])*mCaseSeparationCategoryCount+k;
						if (mChartInfo.pointsInCategory[hv][cat] > 0) {
							float relSize = Math.abs(mChartInfo.barValue[hv][cat] - mChartInfo.barBase)
											 / (mChartInfo.axisMax - mChartInfo.axisMin);
							mChartInfo.pieSize[hv][cat] = cMaxPieSize * cellSize * mRelativeMarkerSize
													  * (float)Math.sqrt(relSize);
							mChartInfo.pieX[hv][cat] = baseRect.x + hOffset + i*cellWidth + cellWidth/2;
							mChartInfo.pieY[hv][cat] = baseRect.y + vOffset + baseRect.height
									- labelHeight - j*cellHeight - cellHeight/2;
	
							if (mCaseSeparationCategoryCount != 1) {
								if (preferredCSAxis == 0)
									mChartInfo.pieX[hv][cat] += csOffset + k*csWidth;
								else
									mChartInfo.pieY[hv][cat] += csOffset + k*csWidth;
								}
	
							for (int l=0; l<colorCount; l++)
								pieColorEdge[hv][cat][l+1] = pieColorEdge[hv][cat][l] + 360.0f
										  * (float)mChartInfo.pointsInColorCategory[hv][cat][l]
										  / (float)mChartInfo.pointsInCategory[hv][cat];
							}
						}
					}
				}
			}

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			mG.setComposite(composite);
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (mChartInfo.pointsInCategory[hv][cat] > 0) {
					int r = Math.round(mChartInfo.pieSize[hv][cat]/2);
					int x = Math.round(mChartInfo.pieX[hv][cat]);
					int y = Math.round(mChartInfo.pieY[hv][cat]);
					if (mChartInfo.pointsInCategory[hv][cat] == 1) {
						for (int k=0; k<colorCount; k++) {
							if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mChartInfo.color[k]);
								break;
								}
							}
						g.fillOval(x-r, y-r, 2*r, 2*r);
						}
					else {
						for (int k=0; k<colorCount; k++) {
							if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mChartInfo.color[k]);
								g.fillArc(x-r, y-r, 2*r, 2*r,
										  Math.round(pieColorEdge[hv][cat][k]),
										  Math.round(pieColorEdge[hv][cat][k+1])-Math.round(pieColorEdge[hv][cat][k]));
								}
							}
						}
					g.setColor(getContrastGrey(MARKER_OUTLINE));
					g.drawOval(x-r, y-r, 2*r, 2*r);
					}
				}
			}

		boolean showLabels = mShowValueCount
				|| ((mChartMode != cChartModeCount && mChartMode != cChartModePercent)
				&& (mShowMeanAndMedianValues || mShowStandardDeviation || mShowConfidenceInterval));

		if (showLabels) {
			String[] lineText = new String[5];
			g.setColor(getContrastGrey(SCALE_STRONG));
			int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
			for (int hv=0; hv<mHVCount; hv++) {
				for (int cat=0; cat<catCount; cat++) {
					if (mChartInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = compileBarAndPieStatisticsLines(hv, cat, lineText);

						float r = mChartInfo.pieSize[hv][cat]/2;
						float x0 = mChartInfo.pieX[hv][cat];
						float y0 = mChartInfo.pieY[hv][cat] + r + mG.getFontMetrics().getAscent() + scaledFontHeight/2;

						for (int line=0; line<lineCount; line++) {
							float x = Math.round(x0 - mG.getFontMetrics().stringWidth(lineText[line])/2);
							float y = y0 + line*scaledFontHeight;
							mG.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);

			// calculate coordinates for selection
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				float angle = (0.5f +(float)mPoint[i].chartGroupIndex)
							 * 2.0f * (float)Math.PI / (float)mChartInfo.pointsInCategory[hv][cat];
				mPoint[i].screenX = Math.round(mChartInfo.pieX[hv][cat]+mChartInfo.pieSize[hv][cat]/2.0f*(float)Math.cos(angle));
				mPoint[i].screenY = Math.round(mChartInfo.pieY[hv][cat]-mChartInfo.pieSize[hv][cat]/2.0f*(float)Math.sin(angle));
				}
			}
		}

	private void paintBoxOrWhiskerPlot(Graphics2D g, Rectangle baseRect) {
		BoxPlotViewInfo boxPlotInfo = (BoxPlotViewInfo)mChartInfo;

		float cellWidth = (boxPlotInfo.barAxis == 1) ?
				(float)baseRect.width / (float)mCategoryVisibleCount[0]
			  : (float)baseRect.height / (float)mCategoryVisibleCount[1];
		float cellHeight = (boxPlotInfo.barAxis == 1) ?
				(float)baseRect.height
			  : (float)baseRect.width;
		float valueRange = (boxPlotInfo.barAxis == 1) ?
				mAxisVisMax[1]-mAxisVisMin[1]
			  : mAxisVisMax[0]-mAxisVisMin[0];

		mChartInfo.barWidth = Math.min(0.2f * cellHeight, 0.5f * cellWidth / mCaseSeparationCategoryCount);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int axisCatCount = mCategoryVisibleCount[boxPlotInfo.barAxis == 1 ? 0 : 1];
		int catCount = axisCatCount * mCaseSeparationCategoryCount;

		boxPlotInfo.innerDistance = new float[mHVCount][catCount];
		float[][] barPosition = new float[mHVCount][catCount];
		float[][][] barColorEdge = new float[mHVCount][catCount][colorCount+1];
		float[][] boxLAV = new float[mHVCount][catCount];
		float[][] boxUAV = new float[mHVCount][catCount];
		float[][] mean = new float[mHVCount][catCount];
		float[][] median = new float[mHVCount][catCount];
		float csWidth = (mChartInfo.barAxis == 1 ? cellWidth : -cellWidth)
					   * mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (isSplitView()) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<axisCatCount; i++) {
				for (int j=0; j<mCaseSeparationCategoryCount; j++) {
					int cat = i*mCaseSeparationCategoryCount + j;
					if (boxPlotInfo.pointsInCategory[hv][cat] != 0) {
						boxPlotInfo.innerDistance[hv][cat] = (boxPlotInfo.boxQ3[hv][cat] - boxPlotInfo.boxQ1[hv][cat])
														  * cellHeight / valueRange / (float)boxPlotInfo.pointsInCategory[hv][cat];
	
						int offset = 0;
						float visMin = 0;
						float factor = 0;
						float innerDistance = boxPlotInfo.innerDistance[hv][cat];
						if (boxPlotInfo.barAxis == 1) {
							barPosition[hv][cat] = baseRect.x + hOffset + i*cellWidth + cellWidth/2;
	
							offset = baseRect.y + vOffset + baseRect.height;
							visMin = mAxisVisMin[1];
							factor =  - (float)baseRect.height / valueRange;
							innerDistance = -innerDistance;
							}
						else {
							barPosition[hv][cat] = baseRect.y + vOffset + baseRect.height - i*cellWidth - cellWidth/2;
	
							offset = baseRect.x + hOffset;
							visMin = mAxisVisMin[0];
							factor =  (float)baseRect.width / valueRange;
							}

						if (mCaseSeparationCategoryCount != 1)
							barPosition[hv][cat] += csOffset + j*csWidth;

						barColorEdge[hv][cat][0] = offset + factor * (boxPlotInfo.boxQ1[hv][cat] - visMin);
	
						for (int k=0; k<colorCount; k++)
							barColorEdge[hv][cat][k+1] = barColorEdge[hv][cat][k] + innerDistance
													   * (float)boxPlotInfo.pointsInColorCategory[hv][cat][k];

	
						boxLAV[hv][cat] = offset + factor * (boxPlotInfo.boxLAV[hv][cat] - visMin);
						boxUAV[hv][cat] = offset + factor * (boxPlotInfo.boxUAV[hv][cat] - visMin);
						mean[hv][cat] = offset + factor * (boxPlotInfo.mean[hv][cat] - visMin);
						median[hv][cat] = offset + factor * (boxPlotInfo.median[hv][cat] - visMin);
						}
					}
				}
			}

		// draw connection lines
		if (mConnectionColumn == cConnectionColumnConnectCases
		 || mConnectionColumn == mAxisIndex[1-boxPlotInfo.barAxis]) {
			g.setStroke(mConnectionStroke);
			g.setColor(mBoxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int j=0; j<mCaseSeparationCategoryCount; j++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (mCaseSeparationCategoryCount != 1
					 && mMarkerColor.getColorColumn() == mCaseSeparationColumn) {
						if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(mMarkerColor.getColor(j));
							}
						else {
							for (int k=0; k<colorCount; k++) {
								if (boxPlotInfo.pointsInColorCategory[hv][j][k] != 0) {
									g.setColor(boxPlotInfo.color[k]);
									break;
									}
								}
							}
						}

					for (int i=0; i<axisCatCount; i++) {
						int cat = i*mCaseSeparationCategoryCount + j;
				
						if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
							int value = Math.round(mBoxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(boxPlotInfo.barAxis == 1 ? barPosition[hv][cat] : value);
							int newY = Math.round(boxPlotInfo.barAxis == 1 ? value : barPosition[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
								}
							oldX = newX;
							oldY = newY;
							}
						}
					}
				}
			}
		else if (mCaseSeparationCategoryCount != 1 && mConnectionColumn == mCaseSeparationColumn) {
			g.setStroke(mConnectionStroke);
			g.setColor(mBoxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int i=0; i<axisCatCount; i++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (mMarkerColor.getColorColumn() == mAxisIndex[1-boxPlotInfo.barAxis]) {
						if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(mMarkerColor.getColor(i));
							}
						else {
							for (int k=0; k<colorCount; k++) {
								if (boxPlotInfo.pointsInColorCategory[hv][i*mCaseSeparationCategoryCount][k] != 0) {
									g.setColor(boxPlotInfo.color[k]);
									break;
									}
								}
							}
						}

					for (int j=0; j<mCaseSeparationCategoryCount; j++) {
						int cat = i*mCaseSeparationCategoryCount + j;
				
						if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
							int value = Math.round(mBoxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(boxPlotInfo.barAxis == 1 ? barPosition[hv][cat] : value);
							int newY = Math.round(boxPlotInfo.barAxis == 1 ? value : barPosition[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
								}
							oldX = newX;
							oldY = newY;
							}
						}
					}
				}
			}

		Composite original = null;
		Composite composite = null;
		if (mChartType == cChartTypeBoxPlot && mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			}

		float lineLengthAV = mChartInfo.barWidth / 3;

		float lineWidth = Math.min(scaleIfSplitView(mFontHeight)/8f, mChartInfo.barWidth/8f);

		Stroke lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Stroke dashedStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
										   lineWidth, new float[] {3*lineWidth}, 0f);

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
					if (mChartType == cChartTypeBoxPlot) {
						if (composite != null)
							mG.setComposite(composite);
	
						for (int k=0; k<colorCount; k++) {
							if (boxPlotInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(boxPlotInfo.color[k]);
								if (boxPlotInfo.barAxis == 1)
									g.fillRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
				  							   Math.round(barColorEdge[hv][cat][k+1]),
											   Math.round(mChartInfo.barWidth),
											   Math.round(barColorEdge[hv][cat][k])-Math.round(barColorEdge[hv][cat][k+1]));
								else
									g.fillRect(Math.round(barColorEdge[hv][cat][k]),
				  							   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
											   Math.round(barColorEdge[hv][cat][k+1]-Math.round(barColorEdge[hv][cat][k])),
											   Math.round(mChartInfo.barWidth));
								}
							}
						if (original != null)
							mG.setComposite(original);
						}

					// If we show no markers in a whisker plot, and if every whisker belongs to one category
					// of that column that is assigned for marker coloring, then we draw the whisker itself
					// in the color assigned to that category.
					if (mChartType == cChartTypeWhiskerPlot
				   	 && mRelativeMarkerSize == 0.0) {
						if (mMarkerColor.getColorColumn() == mAxisIndex[boxPlotInfo.barAxis == 1 ? 0 : 1])
							g.setColor(mMarkerColor.getColor(cat / mCaseSeparationCategoryCount));
						else if (mCaseSeparationCategoryCount != 1
							  && mMarkerColor.getColorColumn() == mCaseSeparationColumn)
							g.setColor(mMarkerColor.getColor(cat % mCaseSeparationCategoryCount));
						else
							g.setColor(getContrastGrey(SCALE_STRONG));
						}
					else {
						g.setColor(getContrastGrey(SCALE_STRONG));
						}

					if (boxPlotInfo.barAxis == 1) {
						mG.setStroke(lineStroke);
						if (mChartType == cChartTypeBoxPlot) {
							g.drawRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
	  								   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(mChartInfo.barWidth),
									   Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][colorCount]));
							}
						if (boxLAV[hv][cat] > barColorEdge[hv][cat][0])
							g.drawLine(Math.round(barPosition[hv][cat]-lineLengthAV),
									   Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]+lineLengthAV),
									   Math.round(boxLAV[hv][cat]));
						if (boxUAV[hv][cat] < barColorEdge[hv][cat][colorCount])
							g.drawLine(Math.round(barPosition[hv][cat]-lineLengthAV),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]+lineLengthAV),
									   Math.round(boxUAV[hv][cat]));

						mG.setStroke(dashedStroke);
						if (mChartType == cChartTypeWhiskerPlot) {
							g.drawLine(Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(boxLAV[hv][cat]));
							}
						else {
							if (boxLAV[hv][cat] > barColorEdge[hv][cat][0])
								g.drawLine(Math.round(barPosition[hv][cat]),
										   Math.round(boxLAV[hv][cat]),
										   Math.round(barPosition[hv][cat]),
										   Math.round(barColorEdge[hv][cat][0]));
							if (boxUAV[hv][cat] < barColorEdge[hv][cat][colorCount])
								g.drawLine(Math.round(barPosition[hv][cat]),
										   Math.round(boxUAV[hv][cat]),
										   Math.round(barPosition[hv][cat]),
										   Math.round(barColorEdge[hv][cat][colorCount]));
							}
						}
					else {
						mG.setStroke(lineStroke);
						if (mChartType == cChartTypeBoxPlot) {
							g.drawRect(Math.round(barColorEdge[hv][cat][0]),
	  								   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
									   Math.round(barColorEdge[hv][cat][colorCount])-Math.round(barColorEdge[hv][cat][0]),
									   Math.round(mChartInfo.barWidth));
							}
						mG.setStroke(lineStroke);
						g.drawLine(Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV));
						g.drawLine(Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV));

						mG.setStroke(dashedStroke);
						if (mChartType == cChartTypeWhiskerPlot) {
							g.drawLine(Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]));
							}
						else {
							mG.setStroke(dashedStroke);
							g.drawLine(Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][0]),
									   Math.round(barPosition[hv][cat]));
							g.drawLine(Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(barPosition[hv][cat]));
							}
						}

					mG.setStroke(lineStroke);
					drawBoxMeanIndicators(g, median[hv][cat], mean[hv][cat], barPosition[hv][cat], 2*lineWidth);
					}
				}
			}

		if (mShowMeanAndMedianValues
		 || mShowStandardDeviation
		 || mShowConfidenceInterval
		 || mShowValueCount
		 || boxPlotInfo.foldChange != null
		 || boxPlotInfo.pValue != null) {
			String[] lineText = new String[7];
			g.setColor(getContrastGrey(SCALE_STRONG));
			int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
			boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mAxisIndex[boxPlotInfo.barAxis]);
			for (int hv=0; hv<mHVCount; hv++) {
				int hOffset = 0;
				int vOffset = 0;
				if (isSplitView()) {
					hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					}
				for (int cat=0; cat<catCount; cat++) {
					if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = 0;
						if (mShowMeanAndMedianValues) {
							float meanValue = isLogarithmic ? (float)Math.pow(10, boxPlotInfo.mean[hv][cat]) : boxPlotInfo.mean[hv][cat];
							float medianValue = isLogarithmic ? (float)Math.pow(10, boxPlotInfo.median[hv][cat]) : boxPlotInfo.median[hv][cat];
							switch (mBoxplotMeanMode) {
							case cBoxplotMeanModeMedian:
								lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue, DIGITS);
								break;
							case cBoxplotMeanModeMean:
								lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue, DIGITS);
								break;
							case cBoxplotMeanModeLines:
							case cBoxplotMeanModeTriangles:
								lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue, DIGITS);
								lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue, DIGITS);
								break;
								}
							}
						if (mShowStandardDeviation) {
							if (Float.isInfinite(boxPlotInfo.stdDev[hv][cat])) {
								lineText[lineCount++] = "\u03C3=Infinity";
								}
							else {
								double stdDev = isLogarithmic ? Math.pow(10, boxPlotInfo.stdDev[hv][cat]) : boxPlotInfo.stdDev[hv][cat];
								lineText[lineCount++] = "\u03C3=" + DoubleFormat.toString(stdDev, DIGITS);
								}
							}
						if (mShowConfidenceInterval) {
							if (Float.isInfinite(boxPlotInfo.errorMargin[hv][cat])) {
								lineText[lineCount++] = "CI95: Infinity";
								}
							else {
								double ll = boxPlotInfo.mean[hv][cat] - boxPlotInfo.errorMargin[hv][cat];
								double hl = boxPlotInfo.mean[hv][cat] + boxPlotInfo.errorMargin[hv][cat];
								if (isLogarithmic) {
									ll = Math.pow(10, ll);
									hl = Math.pow(10, hl);
									}
								lineText[lineCount++] = "CI95: ".concat(DoubleFormat.toString(ll, DIGITS)).concat("-").concat(DoubleFormat.toString(hl, DIGITS));
								}
							}
						if (mShowValueCount) {
							lineText[lineCount++] = "N="+boxPlotInfo.pointsInCategory[hv][cat];
							}
						if (boxPlotInfo.foldChange != null && !Float.isNaN(boxPlotInfo.foldChange[hv][cat])) {
							String label = isLogarithmic ? "log2fc=" : "fc=";
							lineText[lineCount++] = label+new DecimalFormat("#.###").format(boxPlotInfo.foldChange[hv][cat]);
							}
						if (boxPlotInfo.pValue != null && !Float.isNaN(boxPlotInfo.pValue[hv][cat])) {
							lineText[lineCount++] = "p="+new DecimalFormat("#.####").format(boxPlotInfo.pValue[hv][cat]);
							}

						// calculate the needed space of the text area incl. border of scaledFontHeight/2
						int textWidth = 0;
						int textHeight = (1+lineCount) * scaledFontHeight;
						for (int line=0; line<lineCount; line++) {
							int textLineWidth = mG.getFontMetrics().stringWidth(lineText[line]);
							if (textWidth < textLineWidth)
								textWidth = textLineWidth;
							}
						textWidth += scaledFontHeight;

						for (int line=0; line<lineCount; line++) {
							int textLineWidth = mG.getFontMetrics().stringWidth(lineText[line]);
							float x,y;
							if (boxPlotInfo.barAxis == 1) {
								x = barPosition[hv][cat] - textLineWidth/2;
								if (baseRect.y+baseRect.height - boxLAV[hv][cat] < textHeight
								 && boxUAV[hv][cat] - baseRect.y > textHeight)
									y = boxUAV[hv][cat]-textHeight+scaledFontHeight*3/2+line*scaledFontHeight;
								else
									y = Math.min(boxLAV[hv][cat]+scaledFontHeight*3/2, baseRect.y+baseRect.height+vOffset+scaledFontHeight/2-lineCount*scaledFontHeight)+line*scaledFontHeight;
								}
							else {
								if (baseRect.x+baseRect.width - boxUAV[hv][cat] < textWidth
								 && boxLAV[hv][cat] - baseRect.x > textWidth)
									x = boxLAV[hv][cat] - textLineWidth - scaledFontHeight/2;
								else
									x = Math.min(boxUAV[hv][cat]+scaledFontHeight/2, baseRect.x+baseRect.width+hOffset-textLineWidth);
								y = barPosition[hv][cat]+mG.getFontMetrics().getAscent()/2-((lineCount-1)*scaledFontHeight)/2+line*scaledFontHeight;
								}
							mG.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);

		// in case of box-plot calculate screen positions of all non-outliers
		if (mChartType != cChartTypeWhiskerPlot) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int chartGroupIndex = mPoint[i].chartGroupIndex;
					if (chartGroupIndex != -1) {
						int hv = mPoint[i].hvIndex;
						int cat = getChartCategoryIndex(mPoint[i]);
						if (boxPlotInfo.barAxis == 1) {
							mPoint[i].screenX = Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2)+Math.round(mChartInfo.barWidth/2);
							mPoint[i].screenY = Math.round(barColorEdge[hv][cat][0]-boxPlotInfo.innerDistance[hv][cat]*(1+chartGroupIndex))
											  + Math.round(boxPlotInfo.innerDistance[hv][cat]/2);
							mPoint[i].width = Math.round(boxPlotInfo.barWidth);
							mPoint[i].height = Math.round(boxPlotInfo.innerDistance[hv][cat]);
							}
						else {
							mPoint[i].screenX = Math.round(barColorEdge[hv][cat][0]+boxPlotInfo.innerDistance[hv][cat]*chartGroupIndex)
											  + Math.round(boxPlotInfo.innerDistance[hv][cat]/2);
							mPoint[i].screenY = Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2)+Math.round(mChartInfo.barWidth/2);
							mPoint[i].width = Math.round(boxPlotInfo.innerDistance[hv][cat]);
							mPoint[i].height = Math.round(boxPlotInfo.barWidth);
							}
						}
					}
				}
			}
		}

	private void drawBoxMeanIndicators(Graphics2D g, float median, float mean, float bar, float lineWidth) {
		switch (mBoxplotMeanMode) {
		case cBoxplotMeanModeMedian:
			drawIndicatorLine(g, median, bar, lineWidth, getContrastGrey(SCALE_STRONG));
			break;
		case cBoxplotMeanModeMean:
			drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
			break;
		case cBoxplotMeanModeLines:
			drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
			drawIndicatorLine(g, median, bar, lineWidth, getContrastGrey(SCALE_STRONG));
			break;
		case cBoxplotMeanModeTriangles:
			float width = mChartInfo.barWidth / 4;
			float space = width / 3;
			float tip = space + 1.5f * width;

			if (mChartInfo.barAxis == 1) {
				drawIndicatorTriangle(g, bar+tip, median, bar+space, median-width, bar+space, median+width, Color.BLACK);
				drawIndicatorTriangle(g, bar-tip, mean, bar-space, mean-width, bar-space, mean+width, Color.RED);
				}
			else {
				drawIndicatorTriangle(g, median, bar+tip, median-width, bar+space, median+width, bar+space, Color.BLACK);
				drawIndicatorTriangle(g, mean, bar-tip, mean-width, bar-space, mean+width, bar-space, Color.RED);
				}
			break;
			}
		}

	private void drawIndicatorLine(Graphics2D g, float position, float bar, float lineWidth, Color color) {
		g.setColor(color);
		if (mChartInfo.barAxis == 1) {
			g.fillRect(Math.round(bar-mChartInfo.barWidth/2),
					   Math.round(position-lineWidth/2),
					   Math.round(mChartInfo.barWidth),
					   Math.round(lineWidth));
			}
		else {
			g.fillRect(Math.round(position-lineWidth/2),
					   Math.round(bar-mChartInfo.barWidth/2),
					   Math.round(lineWidth),
					   Math.round(mChartInfo.barWidth));
			}
		}

	private void drawIndicatorTriangle(Graphics2D g, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);

		polygon.moveTo(Math.round(x1), Math.round(y1));
		polygon.lineTo(Math.round(x2), Math.round(y2));
		polygon.lineTo(Math.round(x3), Math.round(y3));
		polygon.closePath();

		g.setColor(color);
		g.fill(polygon);
		g.setColor(getContrastGrey(SCALE_STRONG));
		g.draw(polygon);
		}

	private void markHighlighted(Graphics2D g) {
		if (isVisible(mHighlightedPoint)) {
			g.setColor(getContrastGrey(SCALE_STRONG));
			markMarker(g, (VisualizationPoint2D)mHighlightedPoint, false);
			if (mHighlightedLabelPosition != null)
				g.drawRect(mHighlightedLabelPosition.getScreenX(),
						   mHighlightedLabelPosition.getScreenY(),
						   mHighlightedLabelPosition.getScreenWidth(),
						   mHighlightedLabelPosition._getScreenHeight());
			}
		}

	@Override
	protected void updateHighlightedLabelPosition() {
		int newX = mHighlightedLabelPosition.getLabelCenterOnScreenX();
		int newY = mHighlightedLabelPosition.getLabelCenterOnScreenY();
		Rectangle bounds = getGraphBounds(Math.round(mHighlightedPoint.screenX), Math.round(mHighlightedPoint.screenY));
		if (bounds != null && bounds.contains(newX, newY)) { // don't allow dragging into another split view
			int sx1 = bounds.x;
			int sx2 = bounds.x + bounds.width;
			int sy1 = bounds.y;
			int sy2 = bounds.y + bounds.height;
			float rx = (float) (mHighlightedLabelPosition.getLabelCenterOnScreenX() - sx1) / (float) (sx2 - sx1);
			float x = mAxisVisMin[0] + rx * (mAxisVisMax[0] - mAxisVisMin[0]);
			float ry = (float) (mHighlightedLabelPosition.getLabelCenterOnScreenY() - sy1) / (float) (sy2 - sy1);
			float y = mAxisVisMin[1] + (1f - ry) * (mAxisVisMax[1] - mAxisVisMin[1]);
			mHighlightedLabelPosition.updatePosition(x, y, 0);
			}
		}

	private void markReference(Graphics2D g) {
		g.setColor(Color.red);
		markMarker(g, (VisualizationPoint2D)mActivePoint, true);
		}

	private void drawCurves(Rectangle baseGraphRect) {
		mG.setColor(getContrastGrey(SCALE_STRONG));
		switch (mCurveInfo & cCurveModeMask) {
		case cCurveModeAbscissa:
			drawVerticalMeanLine(baseGraphRect);
			break;
		case cCurveModeOrdinate:
			drawHorizontalMeanLine(baseGraphRect);
			break;
		case cCurveModeBothAxes:
			drawFittedMeanLine(baseGraphRect);
			break;
			}
		}

	@Override
	public String getStatisticalValues() {
		if (mChartType != cChartTypeScatterPlot)
			return super.getStatisticalValues();

		StringWriter stringWriter = new StringWriter(1024);
		BufferedWriter writer = new BufferedWriter(stringWriter);

		try {
			if ((mCurveInfo & cCurveModeMask) == cCurveModeAbscissa)
				getMeanLineStatistics(writer, 0);
			if ((mCurveInfo & cCurveModeMask) == cCurveModeOrdinate)
				getMeanLineStatistics(writer, 1);
			if ((mCurveInfo & cCurveModeMask) == cCurveModeBothAxes)
				getFittedLineStatistics(writer);

			if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE && mCorrelationCoefficient != null) {
				writer.write("Correlation coefficient"+ " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+"):");
				writer.newLine();
				if (mCorrelationCoefficient.length == 1) {
					writer.write(DoubleFormat.toString(mCorrelationCoefficient[0], 3));
					writer.newLine();
					}
				else {
					String[] splittingCategory0 = isSplitView() ? mTableModel.getCategoryList(mSplittingColumn[0]) : null;
					String[] splittingCategory1 = (isSplitView() && mSplittingColumn[1] != cColumnUnassigned) ?
							mTableModel.getCategoryList(mSplittingColumn[1]) : null;
					if (isSplitView())
						writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
					if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
						writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
					writer.write("r");
					writer.newLine();
					for (int hv=0; hv<mHVCount; hv++) {
						if (isSplitView())
							writer.write(splittingCategory0[(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
						if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
							writer.write(splittingCategory1[mSplitter.getVIndex(hv)]+"\t");
						writer.write(DoubleFormat.toString(mCorrelationCoefficient[hv], 3));
						writer.newLine();
						}
					}
				}

			writer.close();
			}
		catch (IOException ioe) {}

		return stringWriter.toString();
		}

	private void getMeanLineStatistics(BufferedWriter writer, int axis) throws IOException {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				xmean[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, axis);
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					xmean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat])
											   		* (getAxisValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		String[] splittingCategory0 = isSplitView() ? mTableModel.getCategoryList(mSplittingColumn[0]) : null;
		String[] splittingCategory1 = (isSplitView() && mSplittingColumn[1] != cColumnUnassigned) ?
						mTableModel.getCategoryList(mSplittingColumn[1]) : null;
		String[] colorCategory = (catCount == 1) ? null : mTableModel.getCategoryList(mMarkerColor.getColorColumn());

//		writer.write((axis == 0) ? "Vertical Mean Line:" : "Horizontal Mean Line:");	// without this line we can paste the data into DataWarrior
//		writer.newLine();

		if (isSplitView())
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (catCount != 1)
			writer.write(mTableModel.getColumnTitle(mMarkerColor.getColorColumn())+"\t");
		writer.write("Value Count\tMean Value");
		if ((mCurveInfo & cCurveStandardDeviation) != 0)
			writer.write("\tStandard Deviation");
		writer.newLine();

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (isSplitView())
					writer.write(splittingCategory0[(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
				if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
					writer.write(splittingCategory1[mSplitter.getVIndex(hv)]+"\t");
				if (catCount != 1)
					writer.write(colorCategory[cat]+"\t");
				writer.write(count[hv][cat]+"\t");
				writer.write(count[hv][cat] == 0 ? "" : formatValue(xmean[hv][cat], mAxisIndex[axis]));
				if ((mCurveInfo & cCurveStandardDeviation) != 0) {
					stdDev[hv][cat] /= (count[hv][cat]-1);
					stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
					writer.write("\t"+formatValue(stdDev[hv][cat], mAxisIndex[axis]));
					}
				writer.newLine();
				}
			}
		writer.newLine();
		}

	private void getFittedLineStatistics(BufferedWriter writer) throws IOException {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				sx[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0);
				sy[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 1);
				sx2[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0) * getAxisValue(mPoint[i].record, 0);
				sxy[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0) * getAxisValue(mPoint[i].record, 1);
				count[mPoint[i].hvIndex][cat]++;
				}
			}
		float[][] m = null;
		float[][] b = null;
		m = new float[mHVCount][catCount];
		b = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				m[hv][cat] = (count[hv][cat]*sxy[hv][cat]-sx[hv][cat]*sy[hv][cat])/(count[hv][cat]*sx2[hv][cat]-sx[hv][cat]*sx[hv][cat]);
				b[hv][cat] = sy[hv][cat]/count[hv][cat]-m[hv][cat]*sx[hv][cat]/count[hv][cat];
				}
			}

		float[][] stdDev = null;
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					float b2 = getAxisValue(mPoint[i].record, 1) + getAxisValue(mPoint[i].record, 0)/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, 0)-xs)*(getAxisValue(mPoint[i].record, 0)-xs);
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, 1)-ys)*(getAxisValue(mPoint[i].record, 1)-ys);
					}
				}
			}

		String[] splittingCategory0 = isSplitView() ? mTableModel.getCategoryList(mSplittingColumn[0]) : null;
		String[] splittingCategory1 = (isSplitView() && mSplittingColumn[1] != cColumnUnassigned) ?
						mTableModel.getCategoryList(mSplittingColumn[1]) : null;
		String[] colorCategory = (catCount == 1) ? null : mTableModel.getCategoryList(mMarkerColor.getColorColumn());

//		writer.write("Fitted Straight Line:");	// without this line we can paste the data into DataWarrior
//		writer.newLine();

		if (mTableModel.isLogarithmicViewMode(mAxisIndex[0]) || mTableModel.isLogarithmicViewMode(mAxisIndex[1])) {
			writer.write("Gradient m and standard deviation are based on logarithmic values.");
			writer.newLine();
			}
		if (isSplitView())
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (catCount != 1)
			writer.write(mTableModel.getColumnTitle(mMarkerColor.getColorColumn())+"\t");
		writer.write("Value Count\tGradient m\tIntercept b");
		if ((mCurveInfo & cCurveStandardDeviation) != 0)
			writer.write("\tStandard Deviation");
		writer.newLine();

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (isSplitView())
					writer.write(splittingCategory0[(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
				if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
					writer.write(splittingCategory1[mSplitter.getVIndex(hv)]+"\t");
				if (catCount != 1)
					writer.write(colorCategory[cat]+"\t");
				writer.write(count[hv][cat]+"\t");
				if (count[hv][cat] < 2) {
					writer.write("\t");
					if ((mCurveInfo & cCurveStandardDeviation) != 0)
						writer.write("\t");
					}
				else {
					if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat])
						writer.write("Infinity\t-Infinity");
					else if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat])
						writer.write("0.0\t"+formatValue(sy[hv][cat] / count[hv][cat], mAxisIndex[1]));
					else
						writer.write(DoubleFormat.toString(m[hv][cat])+"\t"+formatValue(b[hv][cat], mAxisIndex[1]));
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
						writer.write("\t"+DoubleFormat.toString(stdDev[hv][cat]));
						}
					}
				writer.newLine();
				}
			}
		writer.newLine();
		}

	private void drawVerticalMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				xmean[mPoint[i].hvIndex][cat] += mPoint[i].screenX;
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					xmean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat])
											   		* (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					int vOffset = 0;
					if (isSplitView()) {
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
						}
					int ymin = baseGraphRect.y + vOffset;
					int ymax = ymin + baseGraphRect.height;
	
					if (catCount != 1)
						mG.setColor(mMarkerColor.getColor(cat));
					drawVerticalLine(xmean[hv][cat], ymin, ymax, false);
	
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						int xmin = baseGraphRect.x + hOffset;
						int xmax = xmin + baseGraphRect.width;
	
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
	
		   				if (xmean[hv][cat]-stdDev[hv][cat] > xmin)
							drawVerticalLine(xmean[hv][cat]-stdDev[hv][cat], ymin, ymax, true);
						if (xmean[hv][cat]+stdDev[hv][cat] < xmax)
							drawVerticalLine(xmean[hv][cat]+stdDev[hv][cat], ymin, ymax, true);
						}
					}
				}
			}
		}

	private void drawVerticalLine(float x, float ymin, float ymax, boolean isStdDevLine) {
//		if (mIsHighResolution) {
			mG.setStroke(isStdDevLine ? mNormalLineStroke : mFatLineStroke);
			mG.draw(new Line2D.Float(x, ymin, x, ymax));
/*			}
		else {
			if (isStdDevLine) {
				mG.drawLine((int)(x-0.5), (int)ymin, (int)(x-0.5), (int)ymax);
				mG.drawLine((int)(x+0.5), (int)ymin, (int)(x+0.5), (int)ymax);
				}
			else {
				mG.drawLine((int)(x-1.0), (int)ymin, (int)(x-1.0), (int)ymax);
				mG.drawLine((int)x, (int)ymin, (int)x, (int)ymax);
				mG.drawLine((int)(x+1.0), (int)ymin, (int)(x+1.0), (int)ymax);
				}
			}*/
		}

	private void drawHorizontalMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] ymean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				ymean[mPoint[i].hvIndex][cat] += mPoint[i].screenY;
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					ymean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat])
													* (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat]);
					}
				}
			}
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					int vOffset = 0;
					if (isSplitView()) {
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
						}
					int xmin = baseGraphRect.x + hOffset;
					int xmax = xmin + baseGraphRect.width;
	
					if (catCount != 1)
						mG.setColor(mMarkerColor.getColor(cat));
					drawHorizontalLine(xmin, xmax, ymean[hv][cat], false);
	
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						int ymin = baseGraphRect.y + vOffset;
						int ymax = ymin + baseGraphRect.height;
	
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
	
						if (ymean[hv][cat]-stdDev[hv][cat] > ymin)
							drawHorizontalLine(xmin, xmax, ymean[hv][cat]-stdDev[hv][cat], true);
						if (ymean[hv][cat]+stdDev[hv][cat] < ymax)
							drawHorizontalLine(xmin, xmax, ymean[hv][cat]+stdDev[hv][cat], true);
						}
					}
				}
			}
		}

	private void drawHorizontalLine(float xmin, float xmax, float y, boolean isStdDevLine) {
//		if (mIsHighResolution) {
			mG.setStroke(isStdDevLine ? mNormalLineStroke : mFatLineStroke);
			mG.draw(new Line2D.Float(xmin, y, xmax, y));
/*			}
		else {
			if (isStdDevLine) {
				mG.drawLine((int)xmin, (int)(y-0.5), (int)xmax, (int)(y-0.5));
				mG.drawLine((int)xmin, (int)(y+0.5), (int)xmax, (int)(y+0.5));
				}
			else {
				mG.drawLine((int)xmin, (int)(y-1.0), (int)xmax, (int)(y-1.0));
				mG.drawLine((int)xmin, (int)y, (int)xmax, (int)y);
				mG.drawLine((int)xmin, (int)(y+1.0), (int)xmax, (int)(y+1.0));
				}
			}*/
		}

	private void drawFittedMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				sx[mPoint[i].hvIndex][cat] += mPoint[i].screenX;
				sy[mPoint[i].hvIndex][cat] += mPoint[i].screenY;
				sx2[mPoint[i].hvIndex][cat] += mPoint[i].screenX * mPoint[i].screenX;
				sxy[mPoint[i].hvIndex][cat] += mPoint[i].screenX * mPoint[i].screenY;
				count[mPoint[i].hvIndex][cat]++;
				}
			}
		float[][] m = new float[mHVCount][catCount];
		float[][] b = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				m[hv][cat] = (count[hv][cat]*sxy[hv][cat]-sx[hv][cat]*sy[hv][cat])/(count[hv][cat]*sx2[hv][cat]-sx[hv][cat]*sx[hv][cat]);
				b[hv][cat] = sy[hv][cat]/count[hv][cat]-m[hv][cat]*sx[hv][cat]/count[hv][cat];
				}
			}

		float[][] stdDev = null;
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					float b2 = mPoint[i].screenY + mPoint[i].screenX/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX-xs)*(mPoint[i].screenX-xs);
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY-ys)*(mPoint[i].screenY-ys);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] < 2) {
					continue;
					}
				if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat]) {
					float x = sx[hv][cat] / count[hv][cat];
					float ymin = baseGraphRect.y;
					if (isSplitView())
						ymin += mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					drawVerticalLine(x, ymin, ymin+baseGraphRect.height, false);
					continue;
					}
				if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat]) {
					float y = sy[hv][cat] / count[hv][cat];
					float xmin = baseGraphRect.x;
					if (isSplitView())
						xmin += mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					drawHorizontalLine(xmin, xmin+baseGraphRect.width, y, false);
					continue;
					}

				if (catCount != 1)
					mG.setColor(mMarkerColor.getColor(cat));
				drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat], false);
		
				if ((mCurveInfo & cCurveStandardDeviation) != 0) {
					stdDev[hv][cat] /= (count[hv][cat]-1);
					stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
					float db = (float)Math.sqrt(stdDev[hv][cat]*stdDev[hv][cat]*(1+m[hv][cat]*m[hv][cat]));
					drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat]+db, true);
					drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat]-db, true);
					}
				}
			}
		}

	private void drawInclinedLine(Rectangle baseGraphRect, int hv, float m, float b, boolean isStdDevLine) {
		int hOffset = 0;
		int vOffset = 0;
		if (isSplitView()) {
			hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
			vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
			}

		int xmin = baseGraphRect.x+hOffset;
		int xmax = xmin+baseGraphRect.width;
		int ymin = baseGraphRect.y+vOffset;
		int ymax = ymin+baseGraphRect.height;

		float sxtop = (ymin-b)/m;
		float sxbottom = (ymax-b)/m;
		float syleft = m*xmin+b;
		float syright = m*(xmax)+b;
		float[] x = new float[2];
		float[] y = new float[2];
		if (syleft >= ymin && syleft <= ymax) {
			x[0] = xmin;
			y[0] = syleft;
			}
		else if (m < 0) {
			if (sxbottom < xmin || sxbottom > xmax)
				return;
			x[0] = sxbottom;
			y[0] = ymax;
			}
		else {
			if (sxtop < xmin || sxtop > xmax)
				return;
			x[0] = sxtop;
			y[0] = ymin;
			}
		if (syright >= ymin && syright <= ymax) {
			x[1] = xmax;
			y[1] = syright;
			}
		else if (m < 0) {
			if (sxtop < xmin || sxtop > xmax)
				return;
			x[1] = sxtop;
			y[1] = ymin;
			}
		else {
			if (sxbottom < xmin || sxbottom > xmax)
				return;
			x[1] = sxbottom;
			y[1] = ymax;
			}
		mG.setStroke(isStdDevLine ? mNormalLineStroke : mFatLineStroke);
		mG.draw(new Line2D.Float(x[0], y[0], x[1], y[1]));
		}

	private void drawCorrelationCoefficient(Rectangle baseGraphRect) {
		if (mAxisIndex[0] == cColumnUnassigned || mAxisIndex[1] == cColumnUnassigned)
			return;

		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		setFontHeight(scaledFontHeight);
		mG.setColor(getContrastGrey(SCALE_STRONG));

		mCorrelationCoefficient = new float[mHVCount];
		if (mHVCount == 1) {
			float r = (float)CorrelationCalculator.calculateCorrelation(
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isVisibleExcludeNaN(mPoint[row]) ? getAxisValue(mPoint[row].record, 0) : Float.NaN;
							}
						},
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isVisibleExcludeNaN(mPoint[row]) ? getAxisValue(mPoint[row].record, 1) : Float.NaN;
							}
						},
					mShownCorrelationType);
			String s = "r="+DoubleFormat.toString(r, 3)
					 + " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+")";
			mG.drawString(s, baseGraphRect.x+baseGraphRect.width-mG.getFontMetrics().stringWidth(s),
							 baseGraphRect.y+baseGraphRect.height-scaledFontHeight/2);
			mCorrelationCoefficient[0] = r;
			}
		else {
			int[] count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++)
				if (isVisibleExcludeNaN(mPoint[i]))
					count[mPoint[i].hvIndex]++;
			float[][][] value = new float[mHVCount][2][];
			for (int hv=0; hv<mHVCount; hv++) {
				value[hv][0] = new float[count[hv]];
				value[hv][1] = new float[count[hv]];
				}
			count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					value[mPoint[i].hvIndex][0][count[mPoint[i].hvIndex]] = getAxisValue(mPoint[i].record, 0);
					value[mPoint[i].hvIndex][1][count[mPoint[i].hvIndex]] = getAxisValue(mPoint[i].record, 1);
					count[mPoint[i].hvIndex]++;
					}
				}

			for (int hv=0; hv<mHVCount; hv++) {
				if (count[hv] >= 2) {
					final float[][] _value = value[hv];
					float r = (float)CorrelationCalculator.calculateCorrelation(
							new INumericalDataColumn() {
								public int getValueCount() {
									return _value[0].length;
									}
								public double getValueAt(int row) {
									return _value[0][row];
									}
								},
							new INumericalDataColumn() {
								public int getValueCount() {
									return _value[1].length;
									}
								public double getValueAt(int row) {
									return _value[1][row];
									}
								},
							mShownCorrelationType);

					int hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					int vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					String s = "r="+DoubleFormat.toString(r, 3)
							 + " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+")";
					mG.drawString(s, hOffset+baseGraphRect.x+baseGraphRect.width-mG.getFontMetrics().stringWidth(s),
									 vOffset+baseGraphRect.y+baseGraphRect.height-scaledFontHeight/2);
					mCorrelationCoefficient[hv] = r;
					}
				}
			}
		}

	@Override
	protected void setActivePoint(VisualizationPoint newReference) {
		super.setActivePoint(newReference);

		if (mBackgroundColor.getColorColumn() != cColumnUnassigned) {
			if (mTableModel.isDescriptorColumn(mBackgroundColor.getColorColumn())) {
				setBackgroundSimilarityColors();
				mBackgroundValid = false;
				mOffImageValid = false;
				}
			}
		}

	private void markMarker(Graphics2D g, VisualizationPoint2D vp, boolean boldLine) {
		final float GAP = Math.round(scaleIfSplitView(mFontHeight) / 8f);
		float sizeX,sizeY,hSizeX,hSizeY;
		GeneralPath polygon;

		g.setStroke(boldLine ? mFatLineStroke : mThinLineStroke);

		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != -1
		 && (!mLabelsInTreeViewOnly || isTreeViewGraph())) {
			sizeX = vp.width + 2f*GAP;
			sizeY = vp.height + 2f*GAP;
			hSizeX = sizeX/2;
			hSizeY = sizeY/2;
			g.draw(new Rectangle2D.Float(vp.screenX-hSizeX, vp.screenY-hSizeY, sizeX, sizeY));
			}
		else if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && vp.chartGroupIndex != -1)) {
			int hv = vp.hvIndex;
			int cat = getChartCategoryIndex(vp);
			if (cat != -1 && mChartInfo.innerDistance != null) {
				if (mChartInfo.barAxis == 1) {
					sizeX = mChartInfo.barWidth + 2f*GAP;
					sizeY = mChartInfo.innerDistance[hv][cat] + 2f*GAP;
					}
				else {
					sizeX = mChartInfo.innerDistance[hv][cat] + 2f*GAP;
					sizeY = mChartInfo.barWidth + 2f*GAP;
					}

				hSizeX = sizeX/2;
				hSizeY = sizeY/2;
				g.draw(new Rectangle2D.Float(vp.screenX-hSizeX, vp.screenY-hSizeY, sizeX, sizeY));
				}
			}
		else if (mChartType == cChartTypePies) {
			if (!mChartInfo.barOrPieDataAvailable)
				return;

			int hv = vp.hvIndex;
			int cat = getChartCategoryIndex(vp);
			if (cat != -1) {
				float x = mChartInfo.pieX[hv][cat];
				float y = mChartInfo.pieY[hv][cat];
				float r = mChartInfo.pieSize[hv][cat]/2 + GAP;
				float dif = 360f / (float)mChartInfo.pointsInCategory[hv][cat];
				float angle = dif * vp.chartGroupIndex;
				if (mChartInfo.pointsInCategory[hv][cat] == 1)
					g.draw(new Ellipse2D.Float(x-r, y-r, 2*r, 2*r));
				else
					g.draw(new Arc2D.Float(x-r, y-r, 2*r, 2*r, angle, dif, Arc2D.PIE));
				}
			}
		else if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null) {
			if (mMultiValueMarkerMode == cMultiValueMarkerModeBars) {
				MultiValueBars mvbi = new MultiValueBars();
				mvbi.calculate(vp.width, vp);
				final int z = mMultiValueMarkerColumns.length-1;
				float x1 = mvbi.firstBarX-GAP;
				float xn = mvbi.firstBarX+mMultiValueMarkerColumns.length*mvbi.barWidth+GAP/2;
				g.draw(new Line2D.Float(x1, mvbi.barY[0]-GAP, x1, mvbi.barY[0]+mvbi.barHeight[0]+GAP/2));
				g.draw(new Line2D.Float(xn, mvbi.barY[z]-GAP, xn, mvbi.barY[z]+mvbi.barHeight[z]+GAP/2));
				float x2 = x1;
				float y1 = mvbi.barY[0]-GAP;
				float y2 = mvbi.barY[0]+mvbi.barHeight[0]+GAP/2;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					float x3 = mvbi.firstBarX+(i+1)*mvbi.barWidth-GAP;
					float x4 = x3;
					if (i == z || mvbi.barY[i]<mvbi.barY[i+1])
						x3 += 1.5f*GAP;
					if (i == z || mvbi.barY[i]+mvbi.barHeight[i]>mvbi.barY[i+1]+mvbi.barHeight[i+1])
						x4 += 1.5f*GAP;
					g.draw(new Line2D.Float(x1, y1, x3, y1));
					g.draw(new Line2D.Float(x2, y2, x4, y2));
					if (i != z) {
						float y3 = mvbi.barY[i+1]-GAP;
						float y4 = mvbi.barY[i+1]+mvbi.barHeight[i+1]+GAP/2;
						g.draw(new Line2D.Float(x3, y1, x3, y3));
						g.draw(new Line2D.Float(x4, y2, x4, y4));
						y1 = y3;
						y2 = y4;
						}
					x1 = x3;
					x2 = x4;
					}
				}
			else {
				float x = vp.screenX;
				float y = vp.screenY;
				float size = 0.5f  * vp.width * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
				float[] r = new float[mMultiValueMarkerColumns.length];
				for (int i=0; i<mMultiValueMarkerColumns.length; i++)
					r[i] = size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]);
				float angleIncrement = 360f / mMultiValueMarkerColumns.length;
				float angle = 90f - angleIncrement;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					g.draw(new Arc2D.Float(x-r[i], y-r[i], 2*r[i], 2*r[i], angle, angleIncrement, Arc2D.OPEN));

					float radAngle = (float)Math.PI * angle / 180;
					int h = (i == mMultiValueMarkerColumns.length-1) ? 0 : i+1;
					g.draw(new Line2D.Float(x+(float)Math.cos(radAngle)*r[h], y-(float)Math.sin(radAngle)*r[h],
							x+(float)Math.cos(radAngle)*r[i], y-(float)Math.sin(radAngle)*r[i]));

					angle -= angleIncrement;
					}
				}
			}
		else {
			float size = vp.width;
			float halfSize = size / 2;
			float sx,sy;

			int shape = (mMarkerShapeColumn != cColumnUnassigned) ? vp.shape : mIsFastRendering ? 1 : 0;
			switch (shape) {
			case 0:
				g.draw(new Ellipse2D.Float(vp.screenX-halfSize-GAP, vp.screenY-halfSize-GAP, size+2*GAP, size+2*GAP));
				break;
			case 1:
				g.draw(new Rectangle2D.Float(vp.screenX-halfSize-GAP, vp.screenY-halfSize-GAP, size+2*GAP, size+2*GAP));
				break;
			case 2:
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
				polygon.moveTo(vp.screenX-halfSize-1.5f*GAP, vp.screenY+size/3+GAP);
				polygon.lineTo(vp.screenX+halfSize+1.5f*GAP, vp.screenY+size/3+GAP);
				polygon.lineTo(vp.screenX, vp.screenY-2*size/3-2*GAP);
				polygon.closePath();
				g.draw(polygon);
				break;
			case 3:
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
				polygon.moveTo(vp.screenX-halfSize-1.4*GAP, vp.screenY);
				polygon.lineTo(vp.screenX, vp.screenY+halfSize+1.4*GAP);
				polygon.lineTo(vp.screenX+halfSize+1.4*GAP, vp.screenY);
				polygon.lineTo(vp.screenX, vp.screenY-halfSize-1.4*GAP);
				polygon.closePath();
				g.draw(polygon);
				break;
			case 4:
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
				polygon.moveTo(vp.screenX-halfSize-1.5f*GAP, vp.screenY-size/3-GAP);
				polygon.lineTo(vp.screenX+halfSize+1.5f*GAP, vp.screenY-size/3-GAP);
				polygon.lineTo(vp.screenX, vp.screenY+2*size/3+2*GAP);
				polygon.closePath();
				g.draw(polygon);
				break;
			case 5:
				sx = size/4+GAP;
				sy = sx+halfSize;
				g.draw(new Rectangle2D.Float(vp.screenX-sx, vp.screenY-sy, 2*sx, 2*sy));
				break;
			case 6:
				sy = size/4+GAP;
				sx = sy+halfSize;
				g.draw(new Rectangle2D.Float(vp.screenX-sx, vp.screenY-sy, 2*sx, 2*sy));
				break;
				}
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		if (e.getType() == CompoundTableEvent.cChangeExcluded) {
			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) {
				invalidateOffImage(true);
				}
			if (mBackgroundColorConsidered == BACKGROUND_VISIBLE_RECORDS)
				mBackgroundValid = false;
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			for (int axis=0; axis<2; axis++)
				mScaleDepictor[axis] = null;
			for (int i=0; i<2; i++)
				mSplittingDepictor[i] = null;
			invalidateOffImage(true);
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mMultiValueMarkerColumns != null) {
				int count = 0;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					mMultiValueMarkerColumns[i] = columnMapping[mMultiValueMarkerColumns[i]];
					if (mMultiValueMarkerColumns[i] == cColumnUnassigned)
						count++;
					}
				if (count != 0) {
					if (count == mMultiValueMarkerColumns.length) {
						mMultiValueMarkerColumns = null;
						}
					else {
						int[] newColumns = new int[mMultiValueMarkerColumns.length-count];
						int index = 0;
						for (int i=0; i<mMultiValueMarkerColumns.length; i++)
							if (mMultiValueMarkerColumns[i] != cColumnUnassigned)
								newColumns[index++] = mMultiValueMarkerColumns[i];
						mMultiValueMarkerColumns = newColumns;
						}
					invalidateOffImage(false);
					}
				}

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn != cColumnUnassigned) {
				mChartColumn = columnMapping[mChartColumn];
				if (mChartColumn == cColumnUnassigned) {
					mChartMode = cChartModeCount;
					invalidateOffImage(true);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getColumn();
			for (int axis=0; axis<2; axis++)
				if (column == mAxisIndex[axis])
					mScaleDepictor[axis] = null;
			for (int i=0; i<2; i++)
				if (column == mSplittingColumn[i])
					mSplittingDepictor[i] = null;
			if (mMultiValueMarkerColumns != null)
				for (int i=0; i<mMultiValueMarkerColumns.length; i++)
					if (column == mMultiValueMarkerColumns[i])
						invalidateOffImage(false);

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn == column) {
				invalidateOffImage(true);
				}
			}

		mBackgroundColor.compoundTableChanged(e);
		}

	public void listChanged(CompoundTableListEvent e) {
		super.listChanged(e);

		if (e.getType() == CompoundTableListEvent.cDelete) {
			if (mBackgroundColorConsidered >= 0) {	// is a list index
				if (e.getListIndex() == mBackgroundColorConsidered) {
					mBackgroundColorConsidered = BACKGROUND_VISIBLE_RECORDS;
					mBackgroundValid = false;
					invalidateOffImage(false);
					}
				else if (mBackgroundColorConsidered > e.getListIndex()) {
					mBackgroundColorConsidered--;
					}
				}
			}
		else if (e.getType() == CompoundTableListEvent.cChange) {
			if (mBackgroundColorConsidered >= 0) {	// is a list index
				if (e.getListIndex() == mBackgroundColorConsidered) {
					mBackgroundValid = false;
					invalidateOffImage(false);
					}
				}
			}

		mBackgroundColor.listChanged(e);
		}

	@Override
	public void colorChanged(VisualizationColor source) {
		if (source == mBackgroundColor) {
			updateBackgroundColorIndices();
			return;
			}

		super.colorChanged(source);
		}

	public VisualizationColor getBackgroundColor() {
		return mBackgroundColor;
		}

	public int getBackgroundColorConsidered() {
		return mBackgroundColorConsidered;
		}

	public int getBackgroundColorRadius() {
		return mBackgroundColorRadius;
		}

	public int getBackgroundColorFading() {
		return mBackgroundColorFading;
		}

	public void setBackgroundColorConsidered(int considered) {
		if (mBackgroundColorConsidered != considered) {
			mBackgroundColorConsidered = considered;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorRadius(int radius) {
		if (mBackgroundColorRadius != radius) {
			mBackgroundColorRadius = radius;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorFading(int fading) {
		if (mBackgroundColorFading != fading) {
			mBackgroundColorFading = fading;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	@Override
	protected void addMarkerTooltips(VisualizationPoint vp, TreeSet<Integer> columnSet, StringBuilder sb) {
		if (mMultiValueMarkerColumns != null) {
			for (int i=0; i<mMultiValueMarkerColumns.length; i++)
		        addTooltipRow(vp.record, mMultiValueMarkerColumns[i], null, columnSet, sb);
	        addTooltipRow(vp.record, mMarkerColor.getColorColumn(), null, columnSet, sb);
	        addTooltipRow(vp.record, mMarkerSizeColumn, null, columnSet, sb);
			}
		else {
			super.addMarkerTooltips(vp, columnSet, sb);
			}
		addTooltipRow(vp.record, mBackgroundColor.getColorColumn(), null, columnSet, sb);
		}

	@Override
	public boolean setViewBackground(Color c) {
		if (super.setViewBackground(c)) {
			mBackgroundValid = false;
			return true;
			}
		return false;
		}

	public float getMarkerTransparency() {
		return mMarkerTransparency;
		}

	/**
	 * Changes the marker transparency for non-histogram views
	 * @param transparency value from 0.0 to 1.0
	 */
	public void setMarkerTransparency(float transparency) {
		if (mMarkerTransparency != transparency) {
			mMarkerTransparency = transparency;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	private void updateBackgroundColorIndices() {
		if (mBackgroundColor.getColorColumn() == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cDefaultDataColorIndex;
		else if (CompoundTableListHandler.isListColumn(mBackgroundColor.getColorColumn())) {
			int listIndex = CompoundTableListHandler.getListFromColumn(mBackgroundColor.getColorColumn());
			int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = mPoint[i].record.isFlagSet(flagNo) ?
						VisualizationColor.cSpecialColorCount : VisualizationColor.cSpecialColorCount + 1;
			}
		else if (mTableModel.isDescriptorColumn(mBackgroundColor.getColorColumn()))
			setBackgroundSimilarityColors();
		else if (mBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cSpecialColorCount
						+ mTableModel.getCategoryIndex(mBackgroundColor.getColorColumn(), mPoint[i].record);
			}
		else if (mTableModel.isColumnTypeDouble(mBackgroundColor.getColorColumn())) {
			float min = Float.isNaN(mBackgroundColor.getColorMin()) ?
					mTableModel.getMinimumValue(mBackgroundColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn())) ?
							   (float)Math.log10(mBackgroundColor.getColorMin()) : mBackgroundColor.getColorMin();
			float max = Float.isNaN(mBackgroundColor.getColorMax()) ?
					mTableModel.getMaximumValue(mBackgroundColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn())) ?
							   (float)Math.log10(mBackgroundColor.getColorMax()) : mBackgroundColor.getColorMax();

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as indented
			if (min >= max)
				if (!Float.isNaN(mBackgroundColor.getColorMax()))
					min = Float.MIN_VALUE;

			for (int i=0; i<mDataPoints; i++) {
				float value = mPoint[i].record.getDouble(mBackgroundColor.getColorColumn());
				if (Float.isNaN(value))
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (value <= min)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (byte)VisualizationColor.cSpecialColorCount;
				else if (value >= max)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (byte)(mBackgroundColor.getColorList().length-1);
				else
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (byte)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mBackgroundColor.getColorList().length-VisualizationColor.cSpecialColorCount-1)
						* (value - min) / (max - min));
				}
			}

		mBackgroundValid = false;
		invalidateOffImage(true);
		}

	private void setBackgroundSimilarityColors() {
		if (mActivePoint == null)
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cDefaultDataColorIndex;
		else {
			for (int i=0; i<mDataPoints; i++) {
				float similarity = mTableModel.getDescriptorSimilarity(
										mActivePoint.record, mPoint[i].record, mBackgroundColor.getColorColumn());
				if (Float.isNaN(similarity))
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cMissingDataColorIndex;
				else
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (int)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mBackgroundColor.getColorList().length - VisualizationColor.cSpecialColorCount - 1)
						* similarity);
				}
			}
		}

	public byte[] getBackgroundImageData() {
		if (mBackgroundImageData == null
		 && mBackgroundImage != null) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(mBackgroundImage, "png", baos);
				return baos.toByteArray();
				}
			catch (IOException ioe) {
				return null;
				}
			}
		return mBackgroundImageData;
		}

	public BufferedImage getBackgroundImage() {
		return mBackgroundImage;
		}

	public void setBackgroundImageData(byte[] imageData) {
		if (imageData == null) {
			if (mBackgroundImage == null)
				return;

			mBackgroundImage = null;
			mBackgroundImageData = null;
			}
		else {
			try {
				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(imageData));
				mBackgroundImageData = imageData;
				}
			catch (IOException e) {}
			}

		invalidateOffImage(false);
		}

   public void setBackgroundImage(BufferedImage image) {
		if (image == null) {
			if (mBackgroundImage == null)
				return;

			mBackgroundImage = null;
			}
		else {
			mBackgroundImage = image;
			}

		mBackgroundImageData = null;
		invalidateOffImage(false);
		}

	public VisualizationPoint findMarker(int x, int y) {
		if (mChartType == cChartTypePies) {
			if (mChartInfo != null && mChartInfo.barOrPieDataAvailable) {
				int catCount = mCategoryVisibleCount[0]*mCategoryVisibleCount[1]*mCaseSeparationCategoryCount; 
				for (int hv=mHVCount-1; hv>=0; hv--) {
					for (int cat=catCount-1; cat>=0; cat--) {
						float dx = x - mChartInfo.pieX[hv][cat];
						float dy = mChartInfo.pieY[hv][cat] - y;
						float radius = Math.round(mChartInfo.pieSize[hv][cat]/2);
						if (Math.sqrt(dx*dx+dy*dy) < radius) {
							float angle = (dx==0) ? ((dy>0) ? (float)Math.PI/2 : -(float)Math.PI/2)
										 : (dx<0) ? (float)Math.PI + (float)Math.atan(dy/dx)
										 : (dy<0) ? 2*(float)Math.PI + (float)Math.atan(dy/dx) : (float)Math.atan(dy/dx);
							int index = (int)(mChartInfo.pointsInCategory[hv][cat] * angle/(2*Math.PI));
							if (index>=0 && index<mChartInfo.pointsInCategory[hv][cat]) {
								for (int i=mDataPoints-1; i>=0; i--) {
									if (mPoint[i].hvIndex == hv
									 && getChartCategoryIndex(mPoint[i]) == cat
									 && mPoint[i].chartGroupIndex == index
									 && isVisibleExcludeNaN(mPoint[i]))
										return mPoint[i];
									}
								return null;	// should never reach this
								}
							}
						}
					}
				}

			return null;
			}

		return super.findMarker(x, y);
		}

	@Override
    public float getDistanceToMarker(VisualizationPoint vp, int x, int y) {
		if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null
		 && (mChartType == cChartTypeScatterPlot
		  || mChartType == cChartTypeWhiskerPlot
		  || (mChartType == cChartTypeBoxPlot && vp.chartGroupIndex == -1))) {
			if (mMultiValueMarkerMode == cMultiValueMarkerModePies) {
				float dx = x - vp.screenX;
				float dy = y - vp.screenY;
				float a = (float)(Math.atan2(dy, dx) + Math.PI/2);	// 0 degrees is not in EAST, but in NORTH
				if (a < 0f)
					a += 2*Math.PI;
				int i = Math.min((int)(a * mMultiValueMarkerColumns.length / (2*Math.PI)), mMultiValueMarkerColumns.length-1);
				float distance = (float)Math.sqrt(dx*dx + dy*dy);
				float size = 0.5f  * vp.width * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
				float r = size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]);
				return Math.max(0f, distance-r);
				}
			else {
				float minDistance = Float.MAX_VALUE;
				float maxdx = (mMultiValueMarkerColumns.length*Math.max(2, Math.round(vp.width/(2f*(float)Math.sqrt(mMultiValueMarkerColumns.length))))+8)/2;
				float maxdy = Math.round(vp.height*2f)+4;
				if (Math.abs(x-vp.screenX) < maxdx && Math.abs(y-vp.screenY) < maxdy) {
					MultiValueBars mvbi = new MultiValueBars();
					mvbi.calculate(vp.width, vp);
					for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
						float barX = mvbi.firstBarX+i*mvbi.barWidth;
						float dx = Math.max(0, (x < barX) ? barX-x : x-(barX+mvbi.barWidth));
						float dy = Math.max(0, (y < mvbi.barY[i]) ? mvbi.barY[i]-y : y-(mvbi.barY[i]+mvbi.barHeight[i]));
						float d = Math.max(dx, dy);
						if (minDistance > d)
							minDistance = d;
						}
					}
				return minDistance;
				}
			}

		return super.getDistanceToMarker(vp, x, y);
		}

	@Override
	protected float getMarkerWidth(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1)) {
			if (mChartInfo == null) {
				return 0;
				}
			else if (mChartInfo.barAxis == 1) {
				return mChartInfo.barWidth;
				}
			else {
				int cat = getChartCategoryIndex(p);
				return (cat == -1) ? 0 : mChartInfo.innerDistance[p.hvIndex][cat];
				}
			}
		else {
			return getMarkerSize(p);
			}
		}

	@Override
	protected float getMarkerHeight(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1)) {
			if (mChartInfo == null) {
				return 0;
				}
			else if (mChartInfo.barAxis == 1) {
				int cat = getChartCategoryIndex(p);
				return (cat == -1) ? 0 : mChartInfo.innerDistance[p.hvIndex][cat];
				}
			else {
				return mChartInfo.barWidth;
				}
			}
		else {
			return getMarkerSize(p);
			}
		}

	public void initializeAxis(int axis) {
		super.initializeAxis(axis);

		mBackgroundValid = false;
		mScaleDepictor[axis] = null;
		}

	private void calculateNaNArea(int width, int height) {
		int size = Math.min((int)((NAN_WIDTH+NAN_SPACING) * mAbsoluteMarkerSize), Math.min(width, height) / 5);
		for (int axis=0; axis<mDimensions; axis++)
			mNaNSize[axis] = (!mShowNaNValues
							|| mAxisIndex[axis] == cColumnUnassigned
							|| mIsCategoryAxis[axis]
							|| mTableModel.isColumnDataComplete(mAxisIndex[axis])) ? 0 : size;
		}

	private void calculateScaleDimensions(Graphics2D g, int width, int height) {
		mScaleSize[0] = 0;
		mScaleSize[1] = 0;

		if (mScaleMode == cScaleModeHidden || mTreeNodeList != null)
			return;

		int[] minScaleSize = new int[2];	// minimum space needed near graph root to not truncate labels of other axis
		int[] usedScaleSize = new int[2];	// space needed for labels on this axis

		int scaledFontSize = (int)scaleIfSplitView(mFontHeight);
		for (int axis=1; axis>=0; axis--) {	// vertical axis first
			compileScaleLabels(axis);
			if (mScaleLineList[axis].isEmpty()) {	   // empty scale
				usedScaleSize[axis] = 0;
				}
			else if (mScaleDepictor[axis] != null) {	// molecules on scale
				if (axis == 0) {
					usedScaleSize[0] = scaledFontSize+(int)Math.min(mRelativeFontSize*0.5f*width/mScaleLineList[0].size(), 0.6f*height);
					}
				else {
					usedScaleSize[1] = scaledFontSize+(int)Math.min(mRelativeFontSize*0.5f*height/mScaleLineList[1].size(), 0.6f*width);
					}
				}
			else {
				int maxLabelSize = 0;
				for (int i=0; i<mScaleLineList[axis].size(); i++) {
					String label = (String)mScaleLineList[axis].get(i).label;
					int size = getStringWidth(label);
					if (maxLabelSize < size)
						maxLabelSize = size;
					}

				if (axis == 0) {
					// assume vertical scale to take 1/6 of total width
					int firstLabelWidth = (mScaleLineList[0].size() == 0) ? 0 : getStringWidth((String)mScaleLineList[0].get(0).label);
					int gridSize = (width - Math.max(mNaNSize[1] + usedScaleSize[1], firstLabelWidth / 2)) / mScaleLineList[0].size();
					int maxSizeWithPadding = maxLabelSize + scaledFontSize / 3;
					if (maxSizeWithPadding > gridSize*2) {
						usedScaleSize[0] = (int)(0.71*(scaledFontSize+maxSizeWithPadding));
						mScaleTextMode[axis] = cScaleTextInclined;
						minScaleSize[1] = usedScaleSize[0]*4/5;
						}
					else if (maxSizeWithPadding > gridSize) {
						usedScaleSize[0] = 2*scaledFontSize;
						mScaleTextMode[axis] = cScaleTextAlternating;
						minScaleSize[1] = gridSize/4;
						}
					else {
						usedScaleSize[0] = scaledFontSize;
						mScaleTextMode[axis] = cScaleTextNormal;
						minScaleSize[1] = 0;
						}
					}
				else {
					usedScaleSize[1] = Math.max(scaledFontSize, maxLabelSize);
					mScaleTextMode[1] = cScaleTextNormal;
					minScaleSize[0] = scaledFontSize / 2;
					}
				}
			}

		for (int axis=0; axis<2; axis++) {
			if (showScale(axis))
				mScaleSize[axis] = Math.max(minScaleSize[axis]-mNaNSize[axis], usedScaleSize[axis]);
			else
				mScaleSize[axis] = Math.max(minScaleSize[axis]-mNaNSize[axis], 0);

			int allowedMax = 2*((axis == 0 ? height : width)-mNaNSize[axis])/3;
			if (mScaleSize[axis] > allowedMax)
				mScaleSize[axis] = allowedMax;
			}
		}

	private void compileSplittingHeaderMolecules() {
		for (int i=0; i<2; i++) {
			if (mSplittingMolIndex[i] != mSplittingColumn[i])
				mSplittingDepictor[i] = null;

			mSplittingDepictor[i] = null;
			if (mSplittingColumn[i] >= 0) {
				if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(mSplittingColumn[i]))
				 && mSplittingDepictor[i] == null) {
					String[] idcodeList = mShowEmptyInSplitView ? mTableModel.getCategoryList(mSplittingColumn[i])
																: getVisibleCategoryList(mSplittingColumn[i]);
					mSplittingDepictor[i] = new Depictor2D[idcodeList.length];
					mSplittingMolIndex[i] = mSplittingColumn[i];

					for (int j=0; j<idcodeList.length; j++) {
						String idcode = idcodeList[j];
						if (idcode.length() != 0) {
							int index = idcode.indexOf(' ');
							StereoMolecule mol = (index == -1) ?
										new IDCodeParser(true).getCompactMolecule(idcode)
									  : new IDCodeParser(true).getCompactMolecule(
																	idcode.substring(0, index),
																	idcode.substring(index+1));
							mSplittingDepictor[i][j] = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
							}
						}
					}
				}
			}
		}   

	private void updateScaleMolecules(int axis, int i1, int i2) {
		String[] idcodeList = mTableModel.getCategoryList(mAxisIndex[axis]);
		if (mScaleDepictor[axis] == null) {
			mScaleDepictor[axis] = new Depictor2D[Math.min(maxDisplayedCategoryLabels(axis), idcodeList.length)];
			mScaleDepictorOffset[axis] = Math.min(i1, idcodeList.length-mScaleDepictor[axis].length);
			createScaleMolecules(axis, mScaleDepictorOffset[axis], mScaleDepictorOffset[axis]+mScaleDepictor[axis].length);
			}
		else if (i1 < mScaleDepictorOffset[axis]) {
			int shift = mScaleDepictorOffset[axis]-i1;
			for (int i=mScaleDepictor[axis].length-1; i>=shift; i--)
				mScaleDepictor[axis][i] = mScaleDepictor[axis][i-shift];
			mScaleDepictorOffset[axis] = i1;
			createScaleMolecules(axis, i1, i1+Math.min(shift, mScaleDepictor[axis].length));
			}
		else if (i2 > mScaleDepictorOffset[axis]+mScaleDepictor[axis].length) {
			int shift = i2-mScaleDepictorOffset[axis]-mScaleDepictor[axis].length;
			for (int i=0; i<mScaleDepictor[axis].length-shift; i++)
				mScaleDepictor[axis][i] = mScaleDepictor[axis][i+shift];
			mScaleDepictorOffset[axis] = i2-mScaleDepictor[axis].length;
			createScaleMolecules(axis, i2-Math.min(shift, mScaleDepictor[axis].length), i2);
			}
		}

	private void createScaleMolecules(int axis, int i1, int i2) {
		CategoryList<CategoryMolecule> list = (CategoryList<CategoryMolecule>)mTableModel.getNativeCategoryList(mAxisIndex[axis]);
		for (int i=i1; i<i2; i++) {
			StereoMolecule mol = list.get(i).getMolecule();
			if (mol != null)
				mScaleDepictor[axis][i-mScaleDepictorOffset[axis]] = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
			}
		}

	protected boolean isTextCategoryAxis(int axis) {
		return mAxisIndex[axis] != cColumnUnassigned
			&& !mTableModel.isColumnTypeDouble(mAxisIndex[axis])
			&& !mTableModel.isDescriptorColumn(mAxisIndex[axis]);
		}

	private void compileScaleLabels(int axis) {
		mScaleLineList[axis].clear();
		mMayNeedStatisticsLabelAdaption = true;
		if (mAxisIndex[axis] == cColumnUnassigned) {
			if (mChartType == cChartTypeBars && mChartInfo.barAxis == axis)
				compileDoubleScaleLabels(axis);
			}
		else {
			if (mIsCategoryAxis[axis])
				compileCategoryScaleLabels(axis);
			else
				compileDoubleScaleLabels(axis);
			}
		}

	private int maxDisplayedCategoryLabels(int axis) {
		int splitCount = (mSplitter == null) ? 1
				: (axis == 0) ? mSplitter.getHCount()
				:			   mSplitter.getVCount();

		return 64 / splitCount;
		}

	private void compileCategoryScaleLabels(int axis) {
		if ((int)(mAxisVisMax[axis]-mAxisVisMin[axis]) > maxDisplayedCategoryLabels(axis))
			return;

		String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);
		int entireCategoryCount = categoryList.length;
		if (entireCategoryCount == 0)
			return;

		if (mTableModel.isColumnTypeRangeCategory(mAxisIndex[axis])
		 && (mChartType != cChartTypeBars || axis != mChartInfo.barAxis)) {
			compileRangeCategoryScaleLabels(axis);
			return;
			}

		int min = Math.round(mAxisVisMin[axis] + 0.5f);
		int max = Math.round(mAxisVisMax[axis] - 0.5f);
		if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(mAxisIndex[axis])))
			updateScaleMolecules(axis, min, max+1);

		for (int i=min; i<=max; i++) {
			float scalePosition = (mChartType == cChartTypeBars && axis == mChartInfo.barAxis) ?
				(mChartInfo.barBase - mChartInfo.axisMin) / (mChartInfo.axisMax - mChartInfo.axisMin) - 0.5f + i : i;
			float position = (scalePosition - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
			if (mScaleDepictor[axis] == null)
				mScaleLineList[axis].add(new ScaleLine(position, categoryList[i]));
			else
				mScaleLineList[axis].add(new ScaleLine(position, mScaleDepictor[axis][i-mScaleDepictorOffset[axis]]));
			}
		}

	private void compileRangeCategoryScaleLabels(int axis) {
		String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);

		int min = Math.round(mAxisVisMin[axis] + 0.5f);
		int max = Math.round(mAxisVisMax[axis] - 0.5f);

		for (int i=min; i<=max+1; i++) {
			String category = categoryList[Math.min(i, max)];
			float position = ((float)i - 0.5f - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
			String label = "???";	// should not happen
			if (category == CompoundTableConstants.cRangeNotAvailable) {
				if (i == max+1)
					continue;
				label = "none";
				position = ((float)i - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
				}
			else {
				int index = category.indexOf(CompoundTableConstants.cRangeSeparation);
				if (index != -1)
					label = (i <= max) ? categoryList[i].substring(0, index)
							: categoryList[max].substring(index + CompoundTableConstants.cRangeSeparation.length());
				}
			mScaleLineList[axis].add(new ScaleLine(position, label));
			}
		}

	private void compileDoubleScaleLabels(int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mTableModel.isLogarithmicViewMode(mChartColumn)) {
				compileLogarithmicScaleLabels(axis);
				return;
				}

			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else if (mTableModel.isDescriptorColumn(mAxisIndex[axis])) {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = 1.0f;
			}
		else {
			if (mTableModel.isLogarithmicViewMode(mAxisIndex[axis])) {
				compileLogarithmicScaleLabels(axis);
				return;
				}

			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
						- mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int exponent = 0;
		while (axisLength >= 50.0) {
			axisStart /= 10;
			axisLength /= 10;
			exponent++;
			}
		while (axisLength < 5.0) {
			axisStart *= 10;
			axisLength *= 10.0;
			exponent--;
			}

		int gridSpacing = (int)(axisLength / 10);
		if (gridSpacing < 1)
			gridSpacing = 1;
		else if (gridSpacing < 2)
			gridSpacing = 2;
		else
			gridSpacing = 5;

		int theMarker = (axisStart < 0) ?
			  (int)(axisStart - 0.0000001 - (axisStart % gridSpacing))
			: (int)(axisStart + 0.0000001 + gridSpacing - (axisStart % gridSpacing));
		while ((float)theMarker < (axisStart + axisLength)) {
			float position = (float)(theMarker-axisStart) / axisLength;

			if (mAxisIndex[axis] != -1 && mTableModel.isColumnTypeDate(mAxisIndex[axis])) {
				String label = createDateLabel(theMarker, exponent);
				if (label != null)
					mScaleLineList[axis].add(new ScaleLine(position, label));
				}
			else
				mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toShortString(theMarker, exponent)));

			theMarker += gridSpacing;
			}
		}

	private void compileLogarithmicScaleLabels(int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - axisStart;
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
						 - mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int intMin = (int)Math.floor(axisStart);
		int intMax = (int)Math.floor(axisStart+axisLength);
		
		if (axisLength > 5.4) {
			int step = 1 + (int)axisLength/10;
			for (int i=intMin; i<=intMax; i+=step)
				addLogarithmicScaleLabel(axis, i);
			}
		else if (axisLength > 3.6) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.47712125472f);
				}
			}
		else if (axisLength > 1.8) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.301029996f);
				addLogarithmicScaleLabel(axis, i + 0.698970004f);
				}
			}
		else if (axisLength > 1.0) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.176091259f);
				addLogarithmicScaleLabel(axis, i + 0.301029996f);
				addLogarithmicScaleLabel(axis, i + 0.477121255f);
				addLogarithmicScaleLabel(axis, i + 0.698970004f);
				addLogarithmicScaleLabel(axis, i + 0.84509804f);
				}
			}
		else {
			float start = (float)Math.pow(10, axisStart);
			float length = (float)Math.pow(10, axisStart+axisLength) - start;

			int exponent = 0;
			while (length >= 50.0) {
				start /= 10;
				length /= 10;
				exponent++;
				}
			while (length < 5.0) {
				start *= 10;
				length *= 10.0;
				exponent--;
				}

			int gridSpacing = (int)(length / 10);
			if (gridSpacing < 1)
				gridSpacing = 1;
			else if (gridSpacing < 2)
				gridSpacing = 2;
			else
				gridSpacing = 5;

			int theMarker = (start < 0) ?
				  (int)(start - 0.0000001 - (start % gridSpacing))
				: (int)(start + 0.0000001 + gridSpacing - (start % gridSpacing));
			while ((float)theMarker < (start + length)) {
				float log = (float)Math.log10(theMarker) + exponent;
				float position = (float)(log-axisStart) / axisLength;
				mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toShortString(theMarker, exponent)));
				theMarker += gridSpacing;
				}
			}
		}

	private void addLogarithmicScaleLabel(int axis, float value) {
		float min = (mAxisIndex[axis] == -1) ? mChartInfo.axisMin : mAxisVisMin[axis];
		float max = (mAxisIndex[axis] == -1) ? mChartInfo.axisMax : mAxisVisMax[axis];
		if (value >= min && value <= max) {
			float position = (value-min) / (max - min);
			mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toString(Math.pow(10, value), 3, true)));
			}
		}

	/**
	 * Needs to be called before validateLegend(), because the size legend depends on it.
	 */
	private void calculateMarkerSize(Rectangle bounds) {
		if (mChartType != cChartTypeBars && mChartType != cChartTypePies) {

			// With smaller views due to splitting we reduce size less than proportionally,
			// because individual views are much less crowded and relatively larger markers seem more natural.
			float splittingFactor = (float)Math.pow(mHVCount, 0.33);

			if (mChartType == cChartTypeBoxPlot || mChartType == cChartTypeWhiskerPlot) {
				float cellWidth = (mIsCategoryAxis[0]) ?
						Math.min((float)bounds.width / (float)mCategoryVisibleCount[0], (float)bounds.height / 3.0f)
					  : Math.min((float)bounds.height / (float)mCategoryVisibleCount[1], (float)bounds.width / 3.0f);
				mAbsoluteMarkerSize = mRelativeMarkerSize * cellWidth / (2.0f * splittingFactor * (float)Math.sqrt(mCaseSeparationCategoryCount));
				}
			else {
				mAbsoluteMarkerSize = mRelativeMarkerSize * cMarkerSize * (float)Math.sqrt(bounds.width * bounds.height) / splittingFactor;
				}

			mAbsoluteConnectionLineWidth = mRelativeConnectionLineWidth * cConnectionLineWidth
		 								 * (float)Math.sqrt(bounds.width * bounds.height) / splittingFactor;
			if (!Float.isNaN(mMarkerSizeZoomAdaption))
				mAbsoluteConnectionLineWidth *= mMarkerSizeZoomAdaption;
			}
		}

	private void calculateCoordinates(Graphics2D g, Rectangle bounds) {
		int size = Math.min(bounds.width, bounds.height);
		mBorder = size/40;

		calculateNaNArea(bounds.width, bounds.height);
		calculateScaleDimensions(g, bounds.width, bounds.height);

		if (mChartType == cChartTypeScatterPlot
		 || mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot
		 || mTreeNodeList != null) {
			Rectangle graphRect = getGraphBounds(bounds);

			float jitterMaxX = 0;
			float jitterMaxY = 0;
			if ((mMarkerJitteringAxes & 1) != 0)
				jitterMaxX = mMarkerJittering * graphRect.width / (mIsCategoryAxis[0] ? mAxisVisMax[0] - mAxisVisMin[0] : 5);
			if ((mMarkerJitteringAxes & 2) != 0)
				jitterMaxY = mMarkerJittering * graphRect.height / (mIsCategoryAxis[1] ? mAxisVisMax[1] - mAxisVisMin[1] : 5);

			if (mTreeNodeList != null) {
				if (mTreeNodeList.length != 0)
					calculateTreeCoordinates(graphRect);
				mBackgroundValid = false;

/*				if (mMarkerJittering > 0.0) {	// don't jitter trees
					for (int i=0; i<mDataPoints; i++) {
						mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
						mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;
						}
					}*/
				}
			else {
				float csCategoryWidth = 0;
				float csOffset = 0;
				int csAxis = getCaseSeparationAxis();
				if (csAxis != -1) {
					float width = csAxis == 0 ? graphRect.width : graphRect.height;
					float categoryWidth = (mAxisIndex[csAxis] == cColumnUnassigned) ? width
										 : width / (mAxisVisMax[csAxis]-mAxisVisMin[csAxis]);	// mCategoryCount[csAxis]; 	mCategoryCount is undefined for scatter plots
					categoryWidth *= mCaseSeparationValue;
					float csCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
					csCategoryWidth = categoryWidth / csCategoryCount;
					csOffset = (csCategoryWidth - categoryWidth) / 2;
					}

				int xNaN = Math.round(graphRect.x - mNaNSize[0] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				int yNaN = Math.round(graphRect.y + graphRect.height + mNaNSize[1] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				if (mChartType == cChartTypeScatterPlot) {
					for (int i=0; i<mDataPoints; i++) {
						// calculating coordinates for invisible records also allows to skip coordinate recalculation
						// when the visibility changes (JVisualization3D uses the inverse approach)
						float doubleX = (mAxisIndex[0] == cColumnUnassigned) ? 0.0f : getAxisValue(mPoint[i].record, 0);
						float doubleY = (mAxisIndex[1] == cColumnUnassigned) ? 0.0f : getAxisValue(mPoint[i].record, 1);
						mPoint[i].screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + Math.round((doubleX-mAxisVisMin[0])*graphRect.width
															/ (mAxisVisMax[0]-mAxisVisMin[0]));
						mPoint[i].screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + Math.round((mAxisVisMin[1]-doubleY)*graphRect.height
															/ (mAxisVisMax[1]-mAxisVisMin[1]));
						if (jitterMaxX != 0)
							mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
						if (jitterMaxY != 0)
							mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;

						if (csAxis != -1) {
							float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, mPoint[i].record);
							if (csAxis == 0)
								mPoint[i].screenX += csShift;
							else
								mPoint[i].screenY -= csShift;
							}
						}
					}
				else {	// mChartType == cChartTypeBoxPlot or cChartTypeWhiskerPlot
					boolean xIsDoubleCategory = mChartInfo.barAxis == 1
											 && mAxisIndex[0] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[0]);
					boolean yIsDoubleCategory = mChartInfo.barAxis == 0
											 && mAxisIndex[1] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[1]);
					for (int i=0; i<mDataPoints; i++) {
						if (mChartType == cChartTypeWhiskerPlot
						 || mPoint[i].chartGroupIndex == -1) {
							if (mAxisIndex[0] == cColumnUnassigned)
								mPoint[i].screenX = graphRect.x + Math.round(graphRect.width * 0.5f);
							else if (xIsDoubleCategory)
								mPoint[i].screenX = graphRect.x + Math.round(graphRect.width
											* (0.5f + getCategoryIndex(0, mPoint[i])) / mCategoryVisibleCount[0]);
							else {
								float doubleX = getAxisValue(mPoint[i].record, 0);
								mPoint[i].screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + Math.round((doubleX-mAxisVisMin[0])*graphRect.width
															/ (mAxisVisMax[0]-mAxisVisMin[0]));							}
							if (mAxisIndex[1] == cColumnUnassigned)
								mPoint[i].screenY = graphRect.y + Math.round(graphRect.height * 0.5f);
							else if (yIsDoubleCategory)
								mPoint[i].screenY = graphRect.y + graphRect.height - Math.round(graphRect.height
											* (0.5f + getCategoryIndex(1, mPoint[i])) / mCategoryVisibleCount[1]);
							else {
								float doubleY = getAxisValue(mPoint[i].record, 1);
								mPoint[i].screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + Math.round((mAxisVisMin[1]-doubleY)*graphRect.height
															/ (mAxisVisMax[1]-mAxisVisMin[1]));
								}

							if (jitterMaxX != 0)
								mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
							if (jitterMaxY != 0)
								mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;

							if (csAxis != -1) {
								float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, mPoint[i].record);
								if (csAxis == 0)
									mPoint[i].screenX += csShift;
								else
									mPoint[i].screenY -= csShift;
								}
							}
						}
					}
				}

			addSplittingOffset();
			}

	   	mCoordinatesValid = true;
		}

	private void calculateTreeCoordinates(Rectangle graphRect) {
		if (mTreeViewMode == cTreeViewModeRadial) {
			float zoomFactor = (!mTreeViewShowAll || Float.isNaN(mMarkerSizeZoomAdaption)) ? 1f : mMarkerSizeZoomAdaption;
			float preferredMarkerDistance = 4*mAbsoluteMarkerSize*zoomFactor;
			RadialGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, preferredMarkerDistance);
			return;
			}
		if (mTreeViewMode == cTreeViewModeHTree) {
			int maxLayerDistance = graphRect.height / 4;
			int maxNeighborDistance = graphRect.width / 8;
			TreeGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, false, maxLayerDistance, maxNeighborDistance);
			}
		if (mTreeViewMode == cTreeViewModeVTree) {
			int maxLayerDistance = graphRect.width / 4;
			int maxNeighborDistance = graphRect.height / 8;
			TreeGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, true, maxLayerDistance, maxNeighborDistance);
			}
		}

	private void addSplittingOffset() {
		if (isSplitView()) {
			int gridWidth = mSplitter.getGridWidth();
			int gridHeight = mSplitter.getGridHeight();
			for (int i=0; i<mDataPoints; i++) {
				if (mChartType == cChartTypeScatterPlot
				 || mChartType == cChartTypeWhiskerPlot
				 || mPoint[i].chartGroupIndex == -1) {
					int hIndex = mSplitter.getHIndex((int)mPoint[i].hvIndex);
					int vIndex = mSplitter.getVIndex((int)mPoint[i].hvIndex);
					mPoint[i].screenX += hIndex * gridWidth;
					mPoint[i].screenY += vIndex * gridHeight;
					}
				}
			}
		}

	/**
	 * Calculates the background color array for all split views.
	 * @param graphBounds used in case of tree view only
	 */
	private void calculateBackground(Rectangle graphBounds, boolean transparentBG) {
		int backgroundSize = (int)(480.0 - 120.0 * Math.log(mBackgroundColorRadius));
		int backgroundColorRadius = 2*mBackgroundColorRadius;
		if (mIsHighResolution) {
			backgroundSize *= 2;
			backgroundColorRadius *= 2;
			}

		if (mSplitter == null) {
			mBackgroundHCount = 1;
			mBackgroundVCount = 1;
			}
		else {
			backgroundSize *= 2;
			backgroundColorRadius *= 2;
			mBackgroundHCount = mSplitter.getHCount();
			mBackgroundVCount = mSplitter.getVCount();
			}
		int backgroundWidth = backgroundSize / mBackgroundHCount;
		int backgroundHeight = backgroundSize / mBackgroundVCount;

			// add all points' RGB color components to respective grid cells
			// consider all points that are less than backgroundColorRadius away from visible area
		float[][][] backgroundR = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundG = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundB = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundC = new float[mHVCount][backgroundWidth][backgroundHeight];

		float xMin,xMax,yMin,yMax;
		if (mTreeNodeList != null) {
			xMin = graphBounds.x;
			yMin = graphBounds.y + graphBounds.height;
			xMax = graphBounds.x + graphBounds.width;
			yMax = graphBounds.y;
			}
		else {
			if (mAxisIndex[0] == cColumnUnassigned) {
				xMin = mAxisVisMin[0];
				xMax = mAxisVisMax[0];
				}
			else if (mIsCategoryAxis[0]) {
				xMin = -0.5f;
				xMax = -0.5f + mTableModel.getCategoryCount(mAxisIndex[0]);
				}
			else {
				xMin = mTableModel.getMinimumValue(mAxisIndex[0]);
				xMax = mTableModel.getMaximumValue(mAxisIndex[0]);
				}
	
			if (mAxisIndex[1] == cColumnUnassigned) {
				yMin = mAxisVisMin[1];
				yMax = mAxisVisMax[1];
				}
			else if (mIsCategoryAxis[1]) {
				yMin = -0.5f;
				yMax = -0.5f + mTableModel.getCategoryCount(mAxisIndex[1]);
				}
			else {
				yMin = mTableModel.getMinimumValue(mAxisIndex[1]);
				yMax = mTableModel.getMaximumValue(mAxisIndex[1]);
				}
			}

		Color neutralColor = transparentBG ? Color.BLACK : getViewBackground();
		int neutralR = neutralColor.getRed();
		int neutralG = neutralColor.getGreen();
		int neutralB = neutralColor.getBlue();

		float rangeX = xMax - xMin;
		float rangeY = yMax - yMin;
		boolean considerVisibleRecords = (mBackgroundColorConsidered == BACKGROUND_VISIBLE_RECORDS) || (mTreeNodeList != null);
		boolean considerAllRecords = (mBackgroundColorConsidered == BACKGROUND_ALL_RECORDS && !considerVisibleRecords);
		int listFlagNo = (considerVisibleRecords || considerAllRecords) ? -1
						: mTableModel.getListHandler().getListFlagNo(mBackgroundColorConsidered);
		for (int i=0; i<mDataPoints; i++) {
			if (considerAllRecords
			 || (considerVisibleRecords && isVisibleExcludeNaN(mPoint[i]))
			 || (!considerVisibleRecords && mPoint[i].record.isFlagSet(listFlagNo)))	{
				float valueX;
				float valueY;
				if (mTreeNodeList != null) {
					valueX = mPoint[i].screenX;
					valueY = mPoint[i].screenY;
					}
				else {
					valueX = (mAxisIndex[0] == cColumnUnassigned) ? (xMin + xMax) / 2 : getAxisValue(mPoint[i].record, 0);
					valueY = (mAxisIndex[1] == cColumnUnassigned) ? (yMin + yMax) / 2 : getAxisValue(mPoint[i].record, 1);
					}
							  
				if (Float.isNaN(valueX) || Float.isNaN(valueY))
					continue;

				int x = Math.min(backgroundWidth-1, (int)(backgroundWidth * (valueX - xMin) / rangeX));
				int y = Math.min(backgroundHeight-1, (int)(backgroundHeight * (valueY - yMin) / rangeY));

				Color c = mBackgroundColor.getColorList()[((VisualizationPoint2D)mPoint[i]).backgroundColorIndex];
				backgroundR[mPoint[i].hvIndex][x][y] += c.getRed() - neutralR;
				backgroundG[mPoint[i].hvIndex][x][y] += c.getGreen() - neutralG;
				backgroundB[mPoint[i].hvIndex][x][y] += c.getBlue() - neutralB;
				backgroundC[mPoint[i].hvIndex][x][y] += 1.0;	// simply counts individual colors added
				}
			}

			// propagate colors to grid neighbourhood via cosine function
		float[][] influence = new float[backgroundColorRadius][backgroundColorRadius];
		for (int x=0; x<backgroundColorRadius; x++) {
			for (int y=0; y<backgroundColorRadius; y++) {
				float distance = (float)Math.sqrt(x*x + y*y);
				if (distance < backgroundColorRadius)
					influence[x][y] = (float)(0.5 + Math.cos(Math.PI*distance/(float)backgroundColorRadius) / 2.0);
				}
			}
		float[][][] smoothR = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothG = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothB = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothC = new float[mHVCount][backgroundWidth][backgroundHeight];
		boolean xIsCyclic = (mAxisIndex[0] == cColumnUnassigned) ? false
									: (mTableModel.getColumnProperty(mAxisIndex[0],
										CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		boolean yIsCyclic = (mAxisIndex[1] == cColumnUnassigned) ? false
									: (mTableModel.getColumnProperty(mAxisIndex[1],
										CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		for (int x=0; x<backgroundWidth; x++) {
			int xmin = x-backgroundColorRadius+1;
			if (xmin < 0 && !xIsCyclic)
				xmin = 0;
			int xmax = x+backgroundColorRadius-1;
			if (xmax >= backgroundWidth && !xIsCyclic)
				xmax = backgroundWidth-1;

			for (int y=0; y<backgroundHeight; y++) {
				int ymin = y-backgroundColorRadius+1;
				if (ymin < 0 && !yIsCyclic)
					ymin = 0;
				int ymax = y+backgroundColorRadius-1;
				if (ymax >= backgroundHeight && !yIsCyclic)
					ymax = backgroundHeight-1;
	
				for (int hv=0; hv<mHVCount; hv++) {
					if (backgroundC[hv][x][y] > (float)0.0) {
						for (int ix=xmin; ix<=xmax; ix++) {
							int dx = Math.abs(x-ix);
	
							int destX = ix;
							if (destX < 0)
								destX += backgroundWidth;
							else if (destX >= backgroundWidth)
								destX -= backgroundWidth;
	
							for (int iy=ymin; iy<=ymax; iy++) {
								int dy = Math.abs(y-iy);
	
								int destY = iy;
								if (destY < 0)
									destY += backgroundHeight;
								else if (destY >= backgroundHeight)
									destY -= backgroundHeight;
	
								if (influence[dx][dy] > (float)0.0) {
									smoothR[hv][destX][destY] += influence[dx][dy] * backgroundR[hv][x][y];
									smoothG[hv][destX][destY] += influence[dx][dy] * backgroundG[hv][x][y];
									smoothB[hv][destX][destY] += influence[dx][dy] * backgroundB[hv][x][y];
									smoothC[hv][destX][destY] += influence[dx][dy] * backgroundC[hv][x][y];
									}
								}
							}
						}
					}
				}
			}

			// find highest sum of RGB components
		float max = (float)0.0;
		for (int hv=0; hv<mHVCount; hv++)
			for (int x=0; x<backgroundWidth; x++)
				for (int y=0; y<backgroundHeight; y++)
					if (max < smoothC[hv][x][y])
						max = smoothC[hv][x][y];

		float fading = (float)Math.exp(Math.log(1.0)-(float)mBackgroundColorFading/20*(Math.log(1.0)-Math.log(0.1)));

		mBackground = new Color[mHVCount][backgroundWidth][backgroundHeight];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int x=0; x<backgroundWidth; x++) {
				for (int y=0; y<backgroundHeight; y++) {
					if (smoothC[hv][x][y] == 0) {
						mBackground[hv][x][y] = transparentBG ? new Color(1f, 1f, 1f, 0f) : neutralColor;
						}
					else {
						float f = (float)Math.exp(fading*Math.log(smoothC[hv][x][y] / max));
						if (transparentBG) {
							mBackground[hv][x][y] = new Color((int) (smoothR[hv][x][y] / smoothC[hv][x][y]),
									(int) (smoothG[hv][x][y] / smoothC[hv][x][y]),
									(int) (smoothB[hv][x][y] / smoothC[hv][x][y]),
									(int) (f * 255));
							}
						else {
							f /= smoothC[hv][x][y];
							mBackground[hv][x][y] = new Color(neutralR + (int) (f * smoothR[hv][x][y]),
															  neutralG + (int) (f * smoothG[hv][x][y]),
															  neutralB + (int) (f * smoothB[hv][x][y]));
							}
						}
					}
				}
			}

		mBackgroundValid = true;
		}

	private void drawBackground(Graphics2D g, Rectangle graphRect, int hvIndex) {
		ViewPort port = new ViewPort();

		if (hasColorBackground()) {
			int backgroundWidth = mBackground[0].length;
			int backgroundHeight = mBackground[0][0].length;
	
			int[] x = new int[backgroundWidth+1];
			int[] y = new int[backgroundHeight+1];
	
			float factorX = (float)graphRect.width/port.getVisRangle(0);
			float factorY = (float)graphRect.height/port.getVisRangle(1);
	
			int minxi = 0;
			int maxxi = backgroundWidth;
			int minyi = 0;
			int maxyi = backgroundHeight;
			for (int i=0; i<=backgroundWidth; i++) {
				float axisX = port.min[0]+i*port.getRange(0)/backgroundWidth;
				x[i] = graphRect.x + (int)(factorX*(axisX-port.visMin[0]));
				if (x[i] <= graphRect.x) {
					x[i] = graphRect.x;
					minxi = i;
					}
				if (x[i] >= graphRect.x+graphRect.width) {
					x[i] = graphRect.x+graphRect.width;
					maxxi = i;
					break;
					}
				}
			for (int i=0; i<=backgroundHeight; i++) {
	 			float axisY = port.min[1]+i*port.getRange(1)/backgroundHeight;
				y[i] = graphRect.y+graphRect.height - (int)(factorY*(axisY-port.visMin[1]));
				if (y[i] >= graphRect.y+graphRect.height) {
					y[i] = graphRect.y+graphRect.height;
					minyi = i;
					}
				if (y[i] <= graphRect.y) {
					y[i] = graphRect.y;
					maxyi = i;
					break;
					}
				}
			for (int xi=minxi; xi<maxxi; xi++) {
				for (int yi=minyi; yi<maxyi; yi++) {
					g.setColor(mBackground[hvIndex][xi][yi]);
					g.fillRect(x[xi], y[yi+1], x[xi+1]-x[xi], y[yi]-y[yi+1]);
					}
				}
			}

		if (mBackgroundImage != null) {
			int sx1 = Math.round((float)mBackgroundImage.getWidth()*(port.visMin[0]-port.min[0])/port.getRange(0));
			int sx2 = Math.round((float)mBackgroundImage.getWidth()*(port.visMax[0]-port.min[0])/port.getRange(0));
			int sy1 = Math.round((float)mBackgroundImage.getHeight()*(port.max[1]-port.visMax[1])/port.getRange(1));
			int sy2 = Math.round((float)mBackgroundImage.getHeight()*(port.max[1]-port.visMin[1])/port.getRange(1));
			if (sx1 < sx2 && sy1 < sy2)
				g.drawImage(mBackgroundImage, graphRect.x, graphRect.y,
											  graphRect.x+graphRect.width, graphRect.y+graphRect.height,
											  sx1, sy1, sx2, sy2, null);
			}
		}

	private void drawNaNArea(Graphics2D g, Rectangle graphRect) {
		mG.setColor(getContrastGrey(0.1f));
		int xNaNSpace = Math.round(mNaNSize[0] * NAN_SPACING / (NAN_WIDTH + NAN_SPACING));
		int yNaNSpace = Math.round(mNaNSize[1] * NAN_SPACING / (NAN_WIDTH + NAN_SPACING));
		if (mNaNSize[0] != 0)
			mG.fillRect(graphRect.x - mNaNSize[0], graphRect.y, mNaNSize[0] - xNaNSpace, graphRect.height + mNaNSize[1]);
		if (mNaNSize[1] != 0)
			mG.fillRect(graphRect.x - mNaNSize[0], graphRect.y + graphRect.height + yNaNSpace, graphRect.width + mNaNSize[0], mNaNSize[1] - yNaNSpace);
		}

	private void drawAxes(Graphics2D g, Rectangle graphRect) {
		g.setStroke(mNormalLineStroke);
		g.setColor(getContrastGrey(SCALE_STRONG));

		int xmin = graphRect.x;
		int xmax = graphRect.x+graphRect.width;
		int ymin = graphRect.y;
		int ymax = graphRect.y+graphRect.height;

		int arrowSize = (int)(ARROW_TIP_SIZE*scaleIfSplitView(mFontHeight));
		int[] px = new int[3];
		int[] py = new int[3];
		if (showScale(0)
		 && (mAxisIndex[0] != cColumnUnassigned
		  || (mChartType == cChartTypeBars && mChartInfo.barAxis == 0))) {
			g.drawLine(xmin, ymax, xmax, ymax);
			px[0] = xmax;
			py[0] = ymax - arrowSize/3;
			px[1] = xmax;
			py[1] = ymax + arrowSize/3;
			px[2] = xmax + arrowSize;
			py[2] = ymax;
			g.fillPolygon(px, py, 3);

			String label = (mAxisIndex[0] != cColumnUnassigned) ? getAxisTitle(mAxisIndex[0])
					: mChartMode == cChartModeCount ? "Count"
					: mChartMode == cChartModePercent ? "Percent"
					: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")";
			if (mScaleTitleCentered) {
				g.drawString(label,
						xmax-(xmax-xmin)/2-g.getFontMetrics().stringWidth(label)/2,
						ymax+mScaleSize[0]+mNaNSize[1]+g.getFontMetrics().getAscent()+scaleIfSplitView(mFontHeight)/2);
				}
			else {
				g.drawString(label,
						xmax-g.getFontMetrics().stringWidth(label),
						ymax+mScaleSize[0]+mNaNSize[1]+g.getFontMetrics().getAscent());
				}
			}

		if (showScale(1)
		 && (mAxisIndex[1] != cColumnUnassigned
		  || (mChartType == cChartTypeBars && mChartInfo.barAxis == 1))) {
			g.drawLine(xmin, ymax, xmin, ymin);
			px[0] = xmin - arrowSize/3;
			py[0] = ymin;
			px[1] = xmin + arrowSize/3;
			py[1] = ymin;
			px[2] = xmin;
			py[2] = ymin - arrowSize;
			g.fillPolygon(px, py, 3);

			String label = (mAxisIndex[1] != cColumnUnassigned) ? getAxisTitle(mAxisIndex[1])
					: mChartMode == cChartModeCount ? "Count"
					: mChartMode == cChartModePercent ? "Percent"
					: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")";
			if (mScaleTitleCentered) {
				double labelX = xmin - mScaleSize[1] - mNaNSize[0] - 2 * scaleIfSplitView(mFontHeight);
				double labelY = ymin + (ymax - ymin) / 2;
				AffineTransform oldTransform = g.getTransform();
				g.rotate(-Math.PI / 2, labelX, labelY);
				g.drawString(label, (int)labelX - g.getFontMetrics().stringWidth(label)/2, (int)(labelY + g.getFontMetrics().getAscent()));
				g.setTransform(oldTransform);
				}
			else {
				int labelX = xmin - arrowSize - g.getFontMetrics().stringWidth(label);
				if (labelX < xmin - mBorder * 2 / 3 - mScaleSize[1] - mNaNSize[0])
					labelX = xmin + arrowSize;
				g.drawString(label, labelX, ymin - g.getFontMetrics().getDescent());
				}
			}
		}

	private void drawGrid(Graphics2D g, Rectangle graphRect) {
		g.setStroke(mThinLineStroke);
		for (int axis=0; axis<2; axis++)
			for (int i=0; i<mScaleLineList[axis].size(); i++)
				drawScaleLine(g, graphRect, axis, i);
		}

	private void drawScaleLine(Graphics2D g, Rectangle graphRect, int axis, int index) {
		ScaleLine scaleLine = mScaleLineList[axis].get(index);
		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		if (axis == 0) {	// X-axis
			int axisPosition = graphRect.x + Math.round(graphRect.width*scaleLine.position);
			int yBase = graphRect.y+graphRect.height+mNaNSize[1];

			if (mGridMode == cGridModeShown || mGridMode == cGridModeShowVertical) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(axisPosition, graphRect.y, axisPosition, yBase);
				}
			else if (showScale(axis)) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(axisPosition, graphRect.y+graphRect.height, axisPosition, graphRect.y+graphRect.height+scaledFontHeight/6);
				}

			if (scaleLine.label != null && showScale(axis)) {
				if (scaleLine.label instanceof String) {
					g.setColor(getContrastGrey(SCALE_MEDIUM));
					String label = (String)scaleLine.label;
					if (mScaleTextMode[axis] == cScaleTextInclined) {
						int labelWidth = g.getFontMetrics().stringWidth(label);
						int textX = axisPosition-(int)(0.71*labelWidth);
						int textY = yBase+(int)(0.71*(scaledFontHeight+labelWidth));
						g.rotate(-Math.PI/4, textX, textY);
						g.drawString(label, textX, textY);
						g.rotate(Math.PI/4, textX, textY);
						}
					else {
						int yShift = ((mScaleTextMode[axis] == cScaleTextAlternating && (index & 1)==1)) ?
								scaledFontHeight : 0;
						g.drawString(label, axisPosition-g.getFontMetrics().stringWidth(label)/2,
									 yBase+scaledFontHeight+yShift);
						}
					}
				else {
					Depictor2D depictor = (Depictor2D)scaleLine.label;
					depictor.setOverruleColor(getContrastGrey(SCALE_MEDIUM), null);
					drawScaleMolecule(g, graphRect, axis, scaleLine.position, depictor);
					}
				}
			}
		else {  // Y-axis
			int axisPosition = graphRect.y+graphRect.height + Math.round(-graphRect.height*scaleLine.position);
			int xBase = graphRect.x-mNaNSize[0];

			if (mGridMode == cGridModeShown || mGridMode == cGridModeShowHorizontal) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(xBase, axisPosition, graphRect.x+graphRect.width, axisPosition);
				}
			else if (showScale(axis)) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(graphRect.x-scaledFontHeight/6, axisPosition, graphRect.x, axisPosition);
				}

			if (scaleLine.label != null && showScale(axis)) {
				if (scaleLine.label instanceof String) {
					g.setColor(getContrastGrey(SCALE_MEDIUM));
					String label = (String)scaleLine.label;
					g.drawString(label, xBase-scaledFontHeight/5-g.getFontMetrics().stringWidth(label),
								 axisPosition+scaledFontHeight/3);
					}
				else {
					Depictor2D depictor = (Depictor2D)scaleLine.label;
					depictor.setOverruleColor(getContrastGrey(SCALE_STRONG), null);
					drawScaleMolecule(g, graphRect, axis, scaleLine.position, depictor);
					}
				}
			}
		}

	private void drawScaleMolecule(Graphics2D g, Rectangle graphRect, int axis, float position, Depictor2D depictor) {
		int x,y,w,h;

		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		if (axis == 0) {	// X-axis
			h = mScaleSize[axis]-scaledFontHeight;
			w = h;	// h*5/4; for rectangular label
			x = graphRect.x + (int)((float)graphRect.width * position) - w/2;
			y = graphRect.y + graphRect.height + mNaNSize[1] + scaledFontHeight/2;
			}
		else {  // Y-axis
			w = mScaleSize[axis]-scaledFontHeight;
			h = w;	// w*4/5; for rectangular label
			x = graphRect.x - mNaNSize[0] - w -scaledFontHeight/2;
			y = graphRect.y + graphRect.height - (int)((float)graphRect.height * position) - h/2;
			}

		int maxAVBL = Depictor2D.cOptAvBondLen;
		if (mIsHighResolution)
			maxAVBL *= mFontScaling;

		Font oldFont = g.getFont();
		depictor.validateView(g, new Rectangle2D.Double(x, y, w, h), Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
		depictor.paint(g);
		g.setFont(oldFont);
		}

	public int getShownCorrelationType() {
		return mShownCorrelationType;
		}

	public void setShownCorrelationType(int type) {
		if (mShownCorrelationType != type) {
			mShownCorrelationType = type;
			invalidateOffImage(false);
			}
		}
	
	public int getCurveMode() {
		return mCurveInfo & cCurveModeMask;
		}

	public boolean isShowStandardDeviationLines() {
		return (mCurveInfo & cCurveStandardDeviation) != 0;
		}

	public boolean isCurveSplitByCategory() {
		return (mCurveInfo & cCurveSplitByCategory) != 0;
		}

	public void setCurveMode(int mode, boolean drawStdDevRange, boolean splitByCategory) {
		int newInfo = mode
					+ (drawStdDevRange ? cCurveStandardDeviation : 0)
					+ (splitByCategory ? cCurveSplitByCategory : 0);
		if (mCurveInfo != newInfo) {
			mCurveInfo = newInfo;
			invalidateOffImage(false);
			}
		}

	public int[] getMultiValueMarkerColumns() {
		return mMultiValueMarkerColumns;
		}

	public int getMultiValueMarkerMode() {
		return mMultiValueMarkerMode;
		}

	public void setMultiValueMarkerColumns(int[] columns, int mode) {
		if (columns == null)
			mode = cMultiValueMarkerModeNone;
		if (mode == cMultiValueMarkerModeNone)
			columns = null;

		boolean isChange = (mMultiValueMarkerMode != mode);
		if (!isChange) {
			isChange = (columns != mMultiValueMarkerColumns);
			if (columns != null && mMultiValueMarkerColumns != null) {
				isChange = true;
				if (columns.length == mMultiValueMarkerColumns.length) {
					isChange = false;
					for (int i=0; i<columns.length; i++) {
						if (columns[i] != mMultiValueMarkerColumns[i]) {
							isChange = true;
							break;
							}
						}
					}
				}
			}
		if (isChange) {
			mMultiValueMarkerColumns = columns;
			mMultiValueMarkerMode = mode;
			invalidateOffImage(true);
			}
		}

	protected Color getMultiValueMarkerColor(int i) {
		return mMultiValueMarkerColor[i];
		}

	@Override
	protected VisualizationPoint createVisualizationPoint(CompoundRecord record) {
		return new VisualizationPoint2D(record);
		}

	protected int getStringWidth(String s) {
		// used by JVisualizationLegend
		return (int)mG.getFontMetrics().getStringBounds(s, mG).getWidth();
		}

	protected void setFontHeightAndScaleToSplitView(float h) {
		setFontHeight((int)scaleIfSplitView(h));
		}

	protected void setFontHeight(int h) {
		if (mG.getFont().getSize2D() != h)
			mG.setFont(getFont().deriveFont(Font.PLAIN, h));
		}

	/**
	 * If the view is split, then all text drawing is reduced in size
	 * depending on the number of split views. If we don't have view splitting
	 * then no scaling is done.
	 * @return value scaled down properly to be used in split view
	 */
	private float scaleIfSplitView(float value) {
		return (mHVCount == 1) ? value : (float)(value / Math.pow(mHVCount, 0.3));
		}

	protected void setColor(Color c) {
		mG.setColor(c);
		}

	protected void drawLine(int x1, int y1, int x2, int y2) {
		mG.drawLine(x1, y1, x2, y2);
		}

	protected void drawRect(int x, int y, int w, int h) {
		mG.drawRect(x, y, w-1, h-1);
		}

	protected void fillRect(int x, int y, int w, int h) {
		mG.fillRect(x, y, w, h);
		}

	protected void drawString(String s, int x, int y) {
		mG.drawString(s, x, y);
		}

	protected void drawMolecule(StereoMolecule mol, Color color, Rectangle2D.Double rect, int mode, int maxAVBL) {
		Depictor2D d = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
		d.validateView(mG, rect, mode+maxAVBL);
		d.setOverruleColor(color, null);
		d.paint(mG);
		}

	protected void paintLegend(Rectangle bounds, boolean transparentBG) {
		mG.setStroke(mThinLineStroke);
		super.paintLegend(bounds, transparentBG);
		}

	@Override
	protected void addLegends(Rectangle bounds, int fontHeight) {
		super.addLegends(bounds, fontHeight);

		if (!mSuppressLegend
		 && mMultiValueMarkerMode != cMultiValueMarkerModeNone
		 && mMultiValueMarkerColumns != null
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			JVisualizationLegend multiValueLegend = new JVisualizationLegend(this, mTableModel, cColumnUnassigned, null,
														 JVisualizationLegend.cLegendTypeMultiValueMarker);
			multiValueLegend.calculate(bounds, fontHeight);
			bounds.height -= multiValueLegend.getHeight();
			mLegendList.add(multiValueLegend);
			}

		if (!mSuppressLegend
		 && mBackgroundColor.getColorColumn() != cColumnUnassigned
		 && mChartType != cChartTypeBars) {
			JVisualizationLegend backgroundLegend = new JVisualizationLegend(this, mTableModel,
													mBackgroundColor.getColorColumn(),
													mBackgroundColor,
													mBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
													  JVisualizationLegend.cLegendTypeBackgroundColorCategory
													: JVisualizationLegend.cLegendTypeBackgroundColorDouble);
			backgroundLegend.calculate(bounds, fontHeight);
			bounds.height -= backgroundLegend.getHeight();
			mLegendList.add(backgroundLegend);
			}
		}

	public int getAvailableShapeCount() {
		return cAvailableShapeCount;
		}

	public int[] getSupportedChartTypes() {
		return SUPPORTED_CHART_TYPE;
		}

	private boolean showScale(int axis) {
		return mScaleMode == cScaleModeShown
			|| (axis == 0 && mScaleMode == cScaleModeHideY)
			|| (axis == 1 && mScaleMode == cScaleModeHideX);
		}

	class ScaleLine {
		float position;
		Object label;

		ScaleLine(float position, Object label) {
			this.position = position;
			this.label = label;
			}
		}

	class MarkerLabelInfo {
		int x,x1,x2,y,y1,y2;
		float fontSize;
		String label;
		Depictor2D depictor;
		}

	class MultiValueBars {
		private float top,bottom;	// relative usage of area above and below zero line (0...1); is the same for all markers in a view
		private float[] relValue;	// relative value of a specific marker compared to max/min (-1...1)
		int barWidth,firstBarX,zeroY;
		int[] barY,barHeight;

		MultiValueBars() {
			barY = new int[mMultiValueMarkerColumns.length];
			barHeight = new int[mMultiValueMarkerColumns.length];
			relValue = new float[mMultiValueMarkerColumns.length];
			calculateExtends();
			}

		/**
		 * The relative area above and below of the zero line, when showing
		 * multiple bars representing multiple column values of one row.
		 * @return float[2] with top and bottom area between 0.0 and 1.0 each
		 */
		private void calculateExtends() {
			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				float min = mTableModel.getMinimumValue(mMultiValueMarkerColumns[i]);
				float max = mTableModel.getMaximumValue(mMultiValueMarkerColumns[i]);
				if (min >= 0f) {
					top = 1f;
					continue;
					}
				if (max <= 0f) {
					bottom = 1f;
					continue;
					}
				float topPart = max / (max - min);
				if (top < topPart)
					top = topPart;
				if (bottom < 1f - topPart)
					bottom = 1f - topPart;
				}
			}

		private void calculate(float size, VisualizationPoint vp) {
			float factor = 0;
			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				float min = mTableModel.getMinimumValue(mMultiValueMarkerColumns[i]);
				float max = mTableModel.getMaximumValue(mMultiValueMarkerColumns[i]);
				float value = vp.record.getDouble(mMultiValueMarkerColumns[i]);
				relValue[i] = Float.isNaN(value) ? Float.NaN
							: (min >= 0f) ? value/max
							: (max <= 0f) ? value/-min
							: (max/top > -min/bottom) ? value*top/max : value*bottom/-min;
				if (!Float.isNaN(value))
					factor = Math.max(factor, Math.abs(relValue[i]));
				}

			float height = size*2f;	// value range of one bar in pixel; the histogram height is <= 2*height
			barWidth = Math.max(2, Math.round(size/(2f*(float)Math.sqrt(mMultiValueMarkerColumns.length))));

			// if we have not used all height, then we reduce barWidth and give it to height
			float widthReduction = 1f;
			int newBarWidth = Math.max(1, (int)((barWidth+1) * Math.sqrt(factor)));
			if (newBarWidth < barWidth) {
				widthReduction = (float)barWidth / (float)newBarWidth;
				barWidth = newBarWidth;
				}

			firstBarX = Math.round(vp.screenX - mMultiValueMarkerColumns.length * barWidth / 2);
			zeroY = Math.round(vp.screenY + height*factor*widthReduction*0.5f*(top-bottom));

			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				if (Float.isNaN(relValue[i])) {
					barHeight[i] = -1;
					barY[i] = zeroY+1;
					}
				else if (relValue[i] > 0) {
					barHeight[i] = Math.round(height*widthReduction*relValue[i]);
					barY[i] = zeroY-barHeight[i];
					}
				else {
					barHeight[i] = -Math.round(height*widthReduction*relValue[i]);
					barY[i] = zeroY+1;
					}
				}
			}
		}

	class ViewPort {
		float[] min,max,visMin,visMax;

		ViewPort() {
			min = new float[2];
			max = new float[2];
			visMin = new float[2];
			visMax = new float[2];
			for (int i=0; i<2; i++) {
				int column = mAxisIndex[i];
				if (column == cColumnUnassigned || mTreeNodeList != null) {
					min[i] = mAxisVisMin[i];
					max[i] = mAxisVisMax[i];
					}
				else if (mIsCategoryAxis[i]) {
					min[i] = -0.5f;
					max[i] = -0.5f + mTableModel.getCategoryCount(column);
					}
				else {
					min[i] = mTableModel.getMinimumValue(column);
					max[i] = mTableModel.getMaximumValue(column);
					}
				visMin[i] = mAxisVisMin[i];
				visMax[i] = mAxisVisMax[i];
				}
			}

		float getRange(int dimension) {
			return max[dimension] - min[dimension];
			}

		float getVisRangle(int dimension) {
			return visMax[dimension] - visMin[dimension];
			}
		}
	}
