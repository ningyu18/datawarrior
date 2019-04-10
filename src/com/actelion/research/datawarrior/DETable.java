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

package com.actelion.research.datawarrior;

import com.actelion.research.datawarrior.task.table.DETaskJumpToCurrentRow;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.gui.table.JTableWithRowNumbers;
import com.actelion.research.table.*;
import com.actelion.research.table.model.*;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public class DETable extends JTableWithRowNumbers implements ActionListener,CompoundTableListener {
	private static final long serialVersionUID = 0x20060904;

	public static final int DEFAULT_FONT_SIZE = 13;
	private static final Color LOOKUP_COLOR = new Color(99, 99, 156);
	private static final Color EMBEDDED_DETAIL_COLOR = new Color(108, 156, 99);
	private static final Color REFERENCED_DETAIL_COLOR = new Color(156, 99, 99);

	private Frame           mParentFrame;
	private DEMainPane      mMainPane;
	private JScrollPane		mScrollPane;
	private CompoundRecord mCurrentRecord;
	private TreeMap<String,Integer> mNonExpandedColumnSizes,mHiddenColumnSizes;
	private TreeMap<String,TableCellRenderer> mHiddenCellRendererMap;
	private JButton mJumpButton,mExpandButton;

	public DETable(Frame parentFrame, DEMainPane mainPane, TableColumnModel cm, ListSelectionModel sm) {
		super(mainPane.getTableModel(), cm, sm);
		mParentFrame = parentFrame;
		mMainPane = mainPane;
		mainPane.getTableModel().addCompoundTableListener(this);

		// to eliminate the disabled default action of the JTable when typing menu-V
		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		// to eliminate the default action of the JTable when typing menu-C that the higher level DataWarrior menu can take over (and include the header line when copying)
		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		putClientProperty("Quaqua.Table.style", "striped");
		putClientProperty(org.jvnet.lafwidget.LafWidget.ANIMATION_KIND, org.jvnet.lafwidget.utils.LafConstants.AnimationKind.NONE);
		mCurrentRecord = mainPane.getTableModel().getActiveRow();

		mHiddenCellRendererMap = new TreeMap<String,TableCellRenderer>();
		mHiddenColumnSizes = new TreeMap<String,Integer>();
		setFontSize(DEFAULT_FONT_SIZE);

		// JTable() calls setModel() causing updating table renderers, which sets the correct row height.
		// Then it calls initializeLocalVars() to set the row height back to 16. Thus, we need to correct again...
		updateRowHeight();

		SwingUtilities.invokeLater(new Runnable() { @Override public void run() { createTopLeftButtons(); } } );
		}

	private void updateRowHeight() {
		int rowHeight = 16;
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++)
			rowHeight = Math.max(rowHeight, getPreferredRowHeight(viewColumn));
		setRowHeight(HiDPIHelper.scale(rowHeight));
		}

	private int getPreferredRowHeight(int viewColumn) {
		int column = convertTotalColumnIndexFromView(viewColumn);
		if (((CompoundTableModel)getModel()).getColumnSpecialType(column) != null)
			return 80;
		return ((CompoundTableModel)getModel()).isMultiLineColumn(column) ? 40 : 16;
		}

	@Override
	public int getFontSize() {
		return Math.round(super.getFontSize() / HiDPIHelper.getUIScaleFactor());
		}

	@Override
	public void setFontSize(int fontSize) {
		super.setFontSize(HiDPIHelper.scale(fontSize));
		getTableHeader().resizeAndRepaint();
		}

	@Override
	public void updateUI() {
		super.updateUI();
		SwingUtilities.invokeLater(new Runnable() { @Override public void run() { updateRowHeaderMinWidth(); } } );
		}

	public void updateTableRenderers(boolean initializeColumnWidths) {
		int rowHeight = 16;
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++)
			rowHeight = Math.max(rowHeight, updateTableRenderer(viewColumn, initializeColumnWidths));
		setRowHeight(HiDPIHelper.scale(rowHeight));
		}

	@Override
	public void setRowHeight(int height) {
		super.setRowHeight(height);
		}

	private void createTopLeftButtons() {
		JPanel bp = new JPanel();
		bp.setBackground(UIManager.getColor("TableHeader.background"));
		double[][] size = {{2, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 2},{TableLayout.FILL,TableLayout.PREFERRED,TableLayout.FILL}};
		bp.setLayout(new TableLayout(size));

		mJumpButton = new HiDPIIconButton("jumpTo.png", "Jump to current row", "jump");
		mJumpButton.addActionListener(this);
		bp.add(mJumpButton, "1,1");

		mExpandButton = new HiDPIIconButton("expand.png", "Expand all columns", "expand");
		mExpandButton.addActionListener(this);
		bp.add(mExpandButton, "3,1");

		updateRowHeaderMinWidth();

		((JScrollPane)getParent().getParent()).setCorner(JScrollPane.UPPER_LEFT_CORNER, bp);
		}

	private void updateRowHeaderMinWidth() {
		if (mJumpButton != null)
			setRowHeaderMinWidth(mJumpButton.getPreferredSize().width + mExpandButton.getPreferredSize().width + 8);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("jump")) {
			new DETaskJumpToCurrentRow(mParentFrame, mMainPane).defineAndRun();
			return;
			}
		if (e.getActionCommand().equals("expand")) {
			CompoundTableModel tableModel = (CompoundTableModel)getModel();
			if (mNonExpandedColumnSizes == null) {
				mNonExpandedColumnSizes = new TreeMap<String,Integer>();
				for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
					int column = convertTotalColumnIndexFromView(viewColumn);
					String key = tableModel.getColumnTitleNoAlias(column);
					int oldWidth = getColumnModel().getColumn(viewColumn).getPreferredWidth();
					mNonExpandedColumnSizes.put(key, new Integer(oldWidth));
					String name = tableModel.getColumnTitle(column);
					int width = getFont().getSize() + SwingUtilities.computeStringWidth(getFontMetrics(getFont()), name);
					getColumnModel().getColumn(viewColumn).setPreferredWidth(Math.max(oldWidth, width));
					}
				}
			else {
				for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
					int column = convertTotalColumnIndexFromView(viewColumn);
					String name = tableModel.getColumnTitleNoAlias(column);
					Integer width = mNonExpandedColumnSizes.get(name);
					if (width != null)
						getColumnModel().getColumn(viewColumn).setPreferredWidth(width);
					}
				mNonExpandedColumnSizes = null;
				}
			return;
			}
		}

	/**
	 * @param viewColumn
	 * @return preferred row height (not corrected for HiDPI displays)
	 */
	private int updateTableRenderer(int viewColumn, boolean initializeColumnWidths) {
		int column = convertTotalColumnIndexFromView(viewColumn);
		TableColumn tc = getColumnModel().getColumn(viewColumn);

		String specialType = ((CompoundTableModel)getModel()).getColumnSpecialType(column);
		if (specialType != null) {
			TableCellRenderer renderer = tc.getCellRenderer();
			if (!(renderer instanceof CompoundTableChemistryCellRenderer)) {
				renderer = new CompoundTableChemistryCellRenderer();
				((CompoundTableChemistryCellRenderer)renderer).setAlternateRowBackground(true);
				tc.setCellRenderer(renderer);
				}

			boolean isReaction = specialType.equals(CompoundTableModel.cColumnTypeRXNCode);
			((CompoundTableChemistryCellRenderer)renderer).setReaction(isReaction);

			if (initializeColumnWidths)
		   		tc.setPreferredWidth(HiDPIHelper.scale(isReaction ? 200 : 100));

			return 80;
			}

	 // use one of the following to test the original Substance renderers
	 /*
	 if (((CompoundTableModel)getModel()).isColumnTypeDouble(column)) {
		 SubstanceDefaultTableCellRenderer.DoubleRenderer renderer = new SubstanceDefaultTableCellRenderer.DoubleRenderer() {
			 public Component getTableCellRendererComponent(JTable table, Object value,
	 				boolean isSelected, boolean hasFocus, int row, int column) {
			 	double d = Double.NaN;
			 	try { d = Double.parseDouble((String)value); }
			 	catch (NumberFormatException nfe) {}
			 	return super.getTableCellRendererComponent(table, new Double(d), isSelected, hasFocus, row, column);
			 	}
		 	};
	 	getColumnModel().getColumn(viewColumn).setCellRenderer(renderer);
	 	return 40;
	 }*/
	 /*if (((CompoundTableModel)getModel()).isColumnTypeDouble(column)) {
		 SubstanceDefaultTableCellRenderer renderer = new SubstanceDefaultTableCellRenderer();
	 	getColumnModel().getColumn(viewColumn).setCellRenderer(renderer);
	 	return 40;
	 }*/
	 /*if (((CompoundTableModel)getModel()).isColumnTypeDouble(column)) {
	 	TableCellRenderer renderer = mTable.getDefaultRenderer(mTable.getColumnClass(viewColumn));
	 	getColumnModel().getColumn(viewColumn).setCellRenderer(renderer);
	 	return 40;
	 }*/

		if (((CompoundTableModel)getModel()).isMultiLineColumn(column)) {
//				 SubstanceMultiLineCellRenderer.MultiLineRenderer renderer = new SubstanceMultiLineCellRenderer.MultiLineRenderer();
			if (!(tc.getCellRenderer() instanceof MultiLineCellRenderer)) {
				MultiLineCellRenderer renderer = new MultiLineCellRenderer();
				renderer.setAlternateRowBackground(true);
				tc.setCellRenderer(renderer);
				}

			if (initializeColumnWidths)
				tc.setPreferredWidth(HiDPIHelper.scale(80));

			return 40;
			}

//	 		getColumnModel().getColumn(viewColumn).setCellRenderer(null);

	 		// use a MultiLineCellRenderer to allow text wrapping
//			 SubstanceMultiLineCellRenderer.MultiLineRenderer renderer = new SubstanceMultiLineCellRenderer.MultiLineRenderer();

		if (!(tc.getCellRenderer() instanceof MultiLineCellRenderer)) {
			MultiLineCellRenderer renderer = new MultiLineCellRenderer();
			renderer.setLineWrap(false);
			renderer.setAlternateRowBackground(true);
			tc.setCellRenderer(renderer);
			}

		if (initializeColumnWidths)
			tc.setPreferredWidth(HiDPIHelper.scale(80));

		return 16;
		}

	public void cleanup() {
		mHiddenCellRendererMap.clear();
		getModel().removeTableModelListener(this);
		((CompoundTableModel)getModel()).removeCompoundTableListener(this);
		}

	public void paint(Graphics g) {
			// Deleting large numbers of records at the end of an even larger table
			// causes sometimes paint() calls from which g.getClipBounds() and this.getBounds()
			// reflect the situation before the actual deletion while rowAtPoint() accesses the
			// new situation and thus returns -1. The standard implementation of TableUIs in various
			// LAFs repaints all(!!!) table rows. This may take minutes if we have some 100.000 rows
			// and must be prevented by the following.
		Rectangle clip = g.getClipBounds();
		Point upperLeft = clip.getLocation();
		Point lowerRight = new Point(clip.x + clip.width - 1, clip.y
				+ clip.height - 1);
		int rMin = rowAtPoint(upperLeft);
		int rMax = rowAtPoint(lowerRight);
		if (rMin == -1 && rMax == -1)
			return;
		
		super.paint(g);

		if (getColumnCount() != 0 && getRowCount() != 0) {
			int firstRow = 0;
			int lastRow = getRowCount()-1;
			int firstColumn = 0;
			int lastColumn = getColumnCount()-1;

			if (mScrollPane != null) {
				Rectangle viewRect = mScrollPane.getViewport().getViewRect();
				int rowAtTop = rowAtPoint(new Point(0, viewRect.y));
				int rowAtBottom = rowAtPoint(new Point(0, viewRect.y + viewRect.height));
				firstRow = Math.max(firstRow, rowAtTop);
				lastRow = (rowAtBottom == -1) ? lastRow : Math.min(lastRow, rowAtBottom);
				int columnAtLeft = columnAtPoint(new Point(viewRect.x, 0));
				int columnAtRight = columnAtPoint(new Point(viewRect.x + viewRect.width, 0));
				firstColumn = Math.max(firstColumn, columnAtLeft);
				lastColumn = (columnAtRight == -1) ? lastColumn : Math.min(lastColumn, columnAtRight);
				}

					// draw red frame of current record
			if (mCurrentRecord != null) {
				for (int row=firstRow; row<=lastRow; row++) {
					if (((CompoundTableModel)getModel()).getRecord(row) == mCurrentRecord) {
						int tableWidth = getWidth();
						Rectangle cellRect = getCellRect(row, 0, false);
						g.setColor(Color.red);
						g.drawRect(0, cellRect.y-1, tableWidth-1, cellRect.height+1);
						g.drawRect(1, cellRect.y, tableWidth-3, cellRect.height-1);
						break;
						}
					}
				}

			for (int column=firstColumn; column<=lastColumn; column++) {
				CompoundTableModel tableModel = (CompoundTableModel)getModel();
				int modelColumn = convertTotalColumnIndexFromView(column);
				int detailCount = tableModel.getColumnDetailCount(modelColumn);
				int lookupCount = tableModel.getColumnLookupCount(modelColumn);
				if (detailCount != 0 || lookupCount != 0) {
					int fontSize = HiDPIHelper.scale(7);
					g.setFont(g.getFont().deriveFont(0, fontSize));
					for (int row=firstRow; row<=lastRow; row++) {
						Color color = LOOKUP_COLOR;	// lowest priority
						int count = 0;

						String[][] detail = tableModel.getRecord(row).getDetailReferences(modelColumn);
						if (detail != null) {
							for (int i=0; i<detail.length; i++) {
								if (detail[i] != null) {
									count += detail[i].length;
									if (!tableModel.getColumnDetailSource(modelColumn, i).equals(CompoundTableDetailHandler.EMBEDDED))
										color = REFERENCED_DETAIL_COLOR;
									else if (color == LOOKUP_COLOR)
										color = EMBEDDED_DETAIL_COLOR;
									}
								}
							}

						// if we have a lookup key we have potentially one detail per lookup-URL
						String[] key = tableModel.separateUniqueEntries(tableModel.encodeData(tableModel.getRecord(row), modelColumn));
						if (key != null)
							count += lookupCount * key.length;

						if (count != 0) {
							String detailString = ""+count;
							int stringWidth = g.getFontMetrics().stringWidth(detailString);
							int drawWidth = Math.max(stringWidth, HiDPIHelper.scale(12));
							Rectangle cellRect = getCellRect(row, column, false);
							g.setColor(color);
							g.fillRect(cellRect.x+cellRect.width-drawWidth, cellRect.y, drawWidth, HiDPIHelper.scale(9));
							g.setColor(Color.white);
							g.drawString(detailString, cellRect.x+cellRect.width-(drawWidth+stringWidth)/2, cellRect.y+fontSize);
							}
						}
					}
				}
			}
		}

	public void addNotify() {
		super.addNotify();

		if (getParent().getParent() instanceof JScrollPane)
			mScrollPane = (JScrollPane) getParent().getParent();
		}

	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(this.getColumnModel()) {
			private static final long serialVersionUID = 0x20110105;
			public String getToolTipText(MouseEvent e) {
				int visColumn = getTableHeader().columnAtPoint(e.getPoint());
				if (visColumn != -1) {
					CompoundTableModel model = (CompoundTableModel)getModel();
					int column = convertTotalColumnIndexFromView(visColumn);
					StringBuilder html = new StringBuilder("<html><b>"+model.getColumnTitleNoAlias(column)+"</b>");
					String alias = model.getColumnAlias(column);
					if (alias != null)
						html.append("<br><i>alias:</i> "+alias);
					String description = model.getColumnDescription(column);
					if (description != null)
						html.append("<br><i>description:</i> "+description);
					html.append("<br><i>perceived data type:</i> ");
					if (CompoundTableModel.cColumnTypeIDCode.equals(model.getColumnSpecialType(column)))
						html.append("chemical structure");
					else if (CompoundTableModel.cColumnTypeRXNCode.equals(model.getColumnSpecialType(column)))
						html.append("chemical reaction");
					else if (model.isColumnTypeDouble(column))
						html.append("numerical");
					else if (model.isColumnTypeString(column))
						html.append("text");
					if (model.isColumnTypeCategory(column)) {
						html.append(" categories:"+model.getCategoryCount(column));
						if (model.isColumnTypeRangeCategory(column))
							html.append(" (ranges)");
						}
					if (model.isColumnTypeDate(column))
						html.append(" (date values)");
					if (model.getCategoryCustomOrder(column) != null)
						html.append("<br>Column has custom order of categories.");
					if (model.isLogarithmicViewMode(column))
						html.append("<br>Values are interpreted logarithmically.");
					if (model.isColumnDataComplete(column))
						html.append("<br>Column does not contain empty values.");
					else
						html.append("<br>Column contains empty values.");
					if (model.isColumnDataUnique(column))
						html.append("<br>Column contains single, unique values.");
					else
						html.append("<br>Column contains duplicate values.");
					if (model.isMultiEntryColumn(column))
						html.append("<br>Some cells contain multiple values.");
					if (model.isMultiLineColumn(column))
						html.append("<br>Some cells contain multiple lines.");
					if (model.getColumnDetailCount(column) != 0)
						html.append("<br>Column has "+model.getColumnDetailCount(column)+" associated details.");
					if (model.getColumnSummaryMode(column) != CompoundTableModel.cSummaryModeNormal) {
						html.append("<br>Of multiple numerical values only the <b>");
						html.append(model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMean ? " mean"
								  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMedian ? " median"
								  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMinimum ? " min"
							  	  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMaximum ? " max"
								  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeSum ? " sum" : " ???");
						html.append("</b> value is shown.");
						}
					if (model.getColumnSignificantDigits(column) != 0)
						html.append("<br>Numerical values are rounded to "+model.getColumnSignificantDigits(column)+" significant digits.");

					html.append("</html>");
					return html.toString();
					}
				return null;
				}
			};
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			mHiddenCellRendererMap.clear();
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			Iterator<String> iterator = mHiddenCellRendererMap.keySet().iterator();
			while (iterator.hasNext())
				if (((CompoundTableModel)getModel()).findColumn(iterator.next()) == -1)
					iterator.remove();
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			mCurrentRecord = ((CompoundTableModel)getModel()).getActiveRow();
			repaint();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnDetailSource) {
			repaint();
			}
		}

	public void tableChanged(TableModelEvent e) {
		ArrayList<DEColumnProperty> columnPropertyList = null;
		int[] rowSelection = null;

		// insertion, deletion or renaming of a column
		if (e.getFirstRow() == TableModelEvent.HEADER_ROW
				&& e.getColumn() != TableModelEvent.ALL_COLUMNS) {
			columnPropertyList = getColumnPropertyList(e.getType() != TableModelEvent.UPDATE);
			rowSelection = getRowSelection();
			}

		super.tableChanged(e);

		// re-apply column visibility
		if (e.getFirstRow() == TableModelEvent.HEADER_ROW
				&& e.getColumn() != TableModelEvent.ALL_COLUMNS) {
			for (int column = 0; column < ((CompoundTableModel) getModel()).getTotalColumnCount(); column++)
				if (mHiddenCellRendererMap.keySet().contains(((CompoundTableModel) getModel()).getColumnTitleNoAlias(column)))
					setColumnVisibility(column, false);
			}

		if (e.getFirstRow() == TableModelEvent.HEADER_ROW)
			updateTableRenderers(true);

		if (columnPropertyList != null)
			applyColumnProperties(columnPropertyList);

		if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
			if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
				getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnModel().getColumnCount() - 1);
				}
			else {
				restoreRowSelection(rowSelection);
				((CompoundTableModel)getModel()).invalidateSelectionModel();
				}
			}
		}

	public void setColumnVisibility(int column, boolean b) {
		int displayableColumn = ((CompoundTableModel)getModel()).convertToDisplayableColumnIndex(column);
		int viewColumn = convertColumnIndexToView(displayableColumn);
		if (b && viewColumn == -1) {
			int targetColumn = 0;
			for (int i=0; i<getColumnCount(); i++)
				if (convertColumnIndexToModel(i) < displayableColumn)
					targetColumn++;

			addColumn(new TableColumn(displayableColumn));
			viewColumn = convertColumnIndexToView(displayableColumn);
			if (viewColumn != targetColumn) {
				moveColumn(viewColumn, targetColumn);
				viewColumn = targetColumn;
				}

			String columnName = ((CompoundTableModel)getModel()).getColumnTitleNoAlias(column);
			getColumnModel().getColumn(viewColumn).setPreferredWidth(mHiddenColumnSizes.get(columnName));
			getColumnModel().getColumn(viewColumn).setCellRenderer(mHiddenCellRendererMap.get(columnName));
			mHiddenCellRendererMap.remove(columnName);
			}
		if (!b && viewColumn != -1) {
			String columnName = ((CompoundTableModel)getModel()).getColumnTitleNoAlias(column);
			TableColumn tc = getColumnModel().getColumn(viewColumn);
			mHiddenColumnSizes.put(columnName, new Integer(tc.getPreferredWidth()));
			mHiddenCellRendererMap.put(columnName, tc.getCellRenderer());
			removeColumn(tc);
			}
		}

	public ArrayList<DEColumnProperty> getColumnPropertyList(boolean columnIndicesDirty) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		ArrayList<DEColumnProperty> columnPropertyList = new ArrayList<DEColumnProperty>();
		if (columnIndicesDirty) {
			// If INSERT or DELETE, visible column titles are kept,
			// but column order and count is may change.
			for (int column=0; column<getColumnCount(); column++) {
				String visName = (String)getColumnModel().getColumn(column).getHeaderValue();
				String name = tableModel.getColumnTitleNoAlias(tableModel.findColumn(visName));
				if (name != null)
					columnPropertyList.add(new DEColumnProperty(name, getColumnModel().getColumn(column),
							getColumnModel().getSelectionModel().isSelectedIndex(column)));
				}
			}
		else {
			// If e.g. UPDATE on header, visible column title may change,
			// but column order and count is kept.
			for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
				int column = convertTotalColumnIndexFromView(viewColumn);
				String name = tableModel.getColumnTitleNoAlias(column);
				columnPropertyList.add(new DEColumnProperty(name, getColumnModel().getColumn(viewColumn),
						getColumnModel().getSelectionModel().isSelectedIndex(column)));
				}
			}
		return columnPropertyList;
		}

	/**
	 * Determines whether visible columns are in native order, i.e. in
	 * the same order as in the underlying CompoundTableModel.
	 * @return TAB delimited String of column names or null if native order
	 */
	public String getColumnOrder() {
		int previousColumn = -1;
		boolean inNativeOrder = true;
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
			int column = convertTotalColumnIndexFromView(viewColumn);
			if (column < previousColumn) {
				inNativeOrder = false;
				break;
				}
			previousColumn = column;
			}
		if (inNativeOrder)
			return null;

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		StringBuffer buf = new StringBuffer();
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
			int column = convertTotalColumnIndexFromView(viewColumn);
			if (buf.length() != 0)
				buf.append('\t');
			buf.append(tableModel.getColumnTitleNoAlias(column));
			}
		return buf.toString();
		}

	/**
	 * Rearranges visible columns to match given column order.
	 * @param columnOrder TAB delimited String of column names
	 */
	public void setColumnOrder(String columnOrder) {
		setColumnOrder(columnOrder.split("\\t"));
		}
		
	private void setColumnOrder(String[] columnTitle) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int newIndex = 0;
		for (String title:columnTitle) {
			int viewColumn = convertTotalColumnIndexToView(tableModel.findColumn(title));
			if (viewColumn != -1)
				getColumnModel().moveColumn(viewColumn, newIndex++);
			}
		}

	/**
	 * Restores column order and column widths from a columnPropertyList.
	 * @param columnPropertyList
	 */
	private void applyColumnProperties(ArrayList<DEColumnProperty> columnPropertyList) {
		for (DEColumnProperty columnProperty:columnPropertyList)
			columnProperty.apply(this);

		String[] columnTitle = new String[columnPropertyList.size()];
		for (int i=0; i<columnPropertyList.size(); i++)
			columnTitle[i] = columnPropertyList.get(i).getColumnName();
		setColumnOrder(columnTitle);
		}

	/**
	 * Converts from CompoundTableModel's total column index to JTable's view column index.
	 * If the column is not displayable or if it is set to hidden in the view -1 is returned.
	 * @param column total column index
	 * @return view column index or -1, if view doesn't display that column
	 */
	public int convertTotalColumnIndexToView(int column) {
		int modelColumn = ((CompoundTableModel)getModel()).convertToDisplayableColumnIndex(column);
		return (modelColumn == -1) ? -1 : convertColumnIndexToView(modelColumn);
		}

	/**
	 * Converts from JTable's view column index to CompoundTableModel's total column index.
	 * @param viewColumn view column index
	 * @return total column index
	 */
	public int convertTotalColumnIndexFromView(int viewColumn) {
		return (viewColumn == -1) ? -1
			 : ((CompoundTableModel)getModel()).convertFromDisplayableColumnIndex(convertColumnIndexToModel(viewColumn));
		}

	public void rowNumberClicked(int row) {
		mCurrentRecord = ((CompoundTableModel)getModel()).getRecord(row);
		((CompoundTableModel)getModel()).setActiveRow(mCurrentRecord);
		repaint();
		}

	private int[] getRowSelection() {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int rowCount = tableModel.getTotalRowCount();
		int[] selection = new int[(rowCount+31)/32];
		for (int row=0; row<rowCount; row++)
			if (tableModel.getTotalRecord(row).isSelected())
				selection[row >> 5] |= (1 << (row % 32));
		return selection;
		}

	private void restoreRowSelection(int[] selection) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int rowCount = tableModel.getTotalRowCount();
		for (int row=0; row<rowCount; row++)
			if ((selection[row >> 5] & (1 << (row % 32))) != 0)
				tableModel.getTotalRecord(row).setSelection(true);
		}
	}

class DEColumnProperty {
	private String columnName;
	TableCellRenderer cellRenderer;
	int width;
	boolean isSelected;

	public DEColumnProperty(String columnName, TableColumn column, boolean isSelected) {
		this.columnName = columnName;
		cellRenderer = column.getCellRenderer();
		width = column.getWidth();
		this.isSelected = isSelected;
		}

	public String getColumnName() {
		return columnName;
		}

	public void apply(DETable table) {
		int column = ((CompoundTableModel)table.getModel()).findColumn(columnName);
		int viewColumn = table.convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			TableColumn tableColumn = table.getColumnModel().getColumn(viewColumn);
			tableColumn.setCellRenderer(cellRenderer);
			tableColumn.setPreferredWidth(width);
			table.getColumnModel().getSelectionModel().addSelectionInterval(viewColumn, viewColumn);
			}
		}
	}
