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

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.form.JHTMLDetailView;
import com.actelion.research.gui.form.JImageDetailView;
import com.actelion.research.gui.form.JResultDetailView;
import com.actelion.research.gui.form.JSVGDetailView;
import com.actelion.research.gui.viewer2d.MoleculeViewer;
import com.actelion.research.table.*;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.VisualizationColor;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

public class DEDetailPane extends JMultiPanelView implements HighlightListener,CompoundTableListener,CompoundTableColorHandler.ColorListener {
	private static final long serialVersionUID = 0x20060904;

	private static final String STRUCTURE = "Structure";
	private static final String STRUCTURE_3D = "3D-Structure";
	private static final String RECORD_DATA = "Data";
	private static final String IMAGE = "Image";
	protected static final String RESULT_DETAIL = "Detail";

	private CompoundTableModel mTableModel;
	private CompoundRecord mCurrentRecord;
	private DetailTableModel	mDetailModel;
	private JDetailTable		mDetailTable;
	private ArrayList<DetailViewInfo> mDetailViewList;

	public DEDetailPane(CompoundTableModel tableModel) {
		super();
		mTableModel = tableModel;
		mTableModel.addCompoundTableListener(this);
		mTableModel.addHighlightListener(this);

		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(100, 100));

		mDetailModel = new DetailTableModel(mTableModel);
  		mDetailTable = new JDetailTable(mDetailModel);
		Font tableFont = UIManager.getFont("Table.font");
  		mDetailTable.setFont(tableFont.deriveFont(Font.PLAIN, tableFont.getSize() * 11 / 12));
		mDetailTable.putClientProperty("Quaqua.Table.style", "striped");

		// to eliminate the disabled default action of the JTable when typing menu-V
		mDetailTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mDetailTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, RECORD_DATA);

		mDetailViewList = new ArrayList<DetailViewInfo>();
		}

	public void setColorHandler(CompoundTableColorHandler colorHandler) {
		mDetailTable.setColorHandler(colorHandler);
		colorHandler.addColorListener(this);
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns) {
			int firstNewView = mDetailViewList.size();
			addColumnDetailViews(e.getColumn());
			
			for (int i=firstNewView; i<mDetailViewList.size(); i++)
				updateDetailView(mDetailViewList.get(i));
			}
		else if (e.getType() == CompoundTableEvent.cNewTable) {
			mCurrentRecord = null;

			for (DetailViewInfo viewInfo:mDetailViewList)
				remove(viewInfo.view);
			mDetailViewList.clear();

			addColumnDetailViews(0);
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			for (DetailViewInfo viewInfo:mDetailViewList) {
				if (e.getColumn() == viewInfo.column) {
					String title = createDetailTitle(viewInfo);
					if (title != null) {
						for (int i=0; i<getViewCount(); i++) {
							if (getView(i) == viewInfo.view) {
								setTitle(i, title);
								break;
								}
							}
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			for (DetailViewInfo viewInfo:mDetailViewList)
				if (e.getColumn() == viewInfo.column)
					updateDetailView(viewInfo);
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			for (int i=mDetailViewList.size()-1; i>=0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				viewInfo.column = e.getMapping()[viewInfo.column];

					// for STRUCTURE(_3D) types viewInfo.detail contains the coordinate column
				if (viewInfo.type.equals(STRUCTURE)) {
					if (viewInfo.detail != -1)  // 2D-coords may be generated on-the-fly
						viewInfo.detail = e.getMapping()[viewInfo.detail];
					}
				if (viewInfo.type.equals(STRUCTURE_3D)) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1)  // remove view if 3D-coords missing
						viewInfo.column = -1;
					}

				if (viewInfo.column == -1) {
					remove(viewInfo.view);
					mDetailViewList.remove(i);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumnDetails) {
			for (int i=mDetailViewList.size()-1; i>=0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				if (viewInfo.type.equals(RESULT_DETAIL) && e.getColumn() == viewInfo.column) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1) {
						remove(viewInfo.view);
						mDetailViewList.remove(i);
						}
					}
				}
			}
/* don't need to track this anymore, because detail source is now columnName&detailIndex rather than previous detail source string
 * 
 * 		else if (e.getType() == CompoundTableEvent.cChangeColumnDetailSource) {
			for (DetailViewInfo viewInfo:mDetailViewList) {
				if (viewInfo.type.equals(RESULT_DETAIL)) {
					int column = e.getSpecifier();
					int detail = e.getMapping()[0];
					if (column == viewInfo.column
					 && detail == viewInfo.detail) {
						String source = mTableModel.getColumnDetailSource(column, detail);
						((JResultDetailView)viewInfo.view).setDetailSource(source);
						}
					}
				}
			}*/
		}

	public void colorChanged(int column, int type, VisualizationColor color) {
		for (DetailViewInfo viewInfo:mDetailViewList) {
			if (viewInfo.column == column) {
				if (viewInfo.type.equals(STRUCTURE)) {
					updateDetailView(viewInfo);
					}
				}
			}
		mDetailTable.repaint();
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	protected void addColumnDetailViews(int firstColumn) {
		for (int column=firstColumn; column<mTableModel.getTotalColumnCount(); column++) {
			String columnName = mTableModel.getColumnTitleNoAlias(column);
			String specialType = mTableModel.getColumnSpecialType(column);
			if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
				int coordinateColumn = mTableModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
				JStructureView view = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
//				view.setBackground(Color.white);
				view.setClipboardHandler(new ClipboardHandler());
				addColumnDetailView(view, column, coordinateColumn, STRUCTURE, mTableModel.getColumnTitle(column));
				continue;
				}
			if (CompoundTableModel.cColumnType3DCoordinates.equals(specialType)) {
				final MoleculeViewer view = new MoleculeViewer();
				Component parent = DEDetailPane.this;
				while (!(parent instanceof Frame))
					parent = parent.getParent();
				view.addActionProvider(new MolViewerActionCopy((Frame)parent));
				view.addActionProvider(new MolViewerActionRaytrace((Frame)parent));
				addColumnDetailView(view, mTableModel.getParentColumn(column), column, STRUCTURE_3D,
									mTableModel.getColumnTitle(column));
				continue;
				}
			if (columnName.equalsIgnoreCase("imagefilename")
			 || columnName.equals("#Image#")
			 || mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath) != null) {
				boolean useThumbNail = (mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyUseThumbNail) != null);
				String imagePath = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath);
				if (imagePath == null)
					imagePath = FileHelper.getCurrentDirectory() + File.separator + "images" + File.separator;
				JImagePanel view = new JImagePanel(imagePath, useThumbNail);
				String viewName = (columnName.equalsIgnoreCase("imagefilename")
								|| columnName.equals("#Image#")) ? IMAGE : mTableModel.getColumnTitle(column);
				addColumnDetailView(view, column, -1, IMAGE, viewName);
				continue;
				}
			for (int detail=0; detail<mTableModel.getColumnDetailCount(column); detail++) {
				String mimetype = mTableModel.getColumnDetailType(column, detail);
				JResultDetailView view = createResultDetailView(column, detail, mimetype);

				if (view != null) {
					addColumnDetailView(view, column, detail, RESULT_DETAIL, mTableModel.getColumnDetailName(column, detail)
							+" ("+mTableModel.getColumnTitle(column)+")");
					}
				}
			}
		}

	private String createDetailTitle(DetailViewInfo info) {
		if (info.type.equals(STRUCTURE))
			return mTableModel.getColumnTitle(info.column);
		if (info.type.equals(STRUCTURE_3D))
			return mTableModel.getColumnTitle(info.detail);
		if (info.type.equals(IMAGE)) {
			String columnName = mTableModel.getColumnTitleNoAlias(info.column);
			return (columnName.equalsIgnoreCase("imagefilename")
				 || columnName.equals("#Image#")) ? IMAGE : mTableModel.getColumnTitle(info.column);
			}
		if (info.type.equals(RESULT_DETAIL))
			return mTableModel.getColumnDetailName(info.column, info.detail)+" ("+mTableModel.getColumnTitle(info.column)+")";
		return null;
		}

	protected JResultDetailView createResultDetailView(int column, int detail, String mimetype) {
		CompoundTableDetailSpecification spec = new CompoundTableDetailSpecification(getTableModel(), column, detail);

		if (mimetype.equals(JHTMLDetailView.TYPE_TEXT_PLAIN)
		 || mimetype.equals(JHTMLDetailView.TYPE_TEXT_HTML))
			 return new JHTMLDetailView(mTableModel.getDetailHandler(),	mTableModel.getDetailHandler(), spec, mimetype);

		if (mimetype.equals(JImageDetailView.TYPE_IMAGE_JPEG)
		 || mimetype.equals(JImageDetailView.TYPE_IMAGE_GIF)
		 || mimetype.equals(JImageDetailView.TYPE_IMAGE_PNG))
			return new JImageDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec);

		if (mimetype.equals(JSVGDetailView.TYPE_IMAGE_SVG))
			return new JSVGDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec);

		return null;
		}

	protected void addColumnDetailView(JComponent view, int column, int detail, String type, String title) {
		mDetailViewList.add(new DetailViewInfo(view, column, detail, type));
		add(view, title);
		}

	public void highlightChanged(CompoundRecord record) {
		if (record != null) {
			mCurrentRecord = record;
	
			for (DetailViewInfo viewInfo:mDetailViewList)
				updateDetailView(viewInfo);
	
			mDetailModel.detailChanged(record);
			}
		}

	private void updateDetailView(DetailViewInfo viewInfo) {
		if (viewInfo.type.equals(STRUCTURE)) {
			StereoMolecule mol = null;
			StereoMolecule displayMol = null;
			if (mCurrentRecord != null) {
				byte[] idcode = (byte[])mCurrentRecord.getData(viewInfo.column);
				if (idcode != null) {
					int coordinateColumn = (viewInfo.column == -1) ? -1
							: mTableModel.getChildColumn(viewInfo.column, CompoundTableModel.cColumnType2DCoordinates);
					if ((coordinateColumn != -1 && mCurrentRecord.getData(coordinateColumn) != null)
					 || new IDCodeParser().getAtomCount(idcode, 0) <= CompoundTableChemistryCellRenderer.ON_THE_FLY_COORD_MAX_ATOMS) {
						mol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
						displayMol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
						}
					}
				}
			((JStructureView)viewInfo.view).structureChanged(mol, displayMol);
			}
		else if (viewInfo.type.equals(STRUCTURE_3D)) {
			StereoMolecule mol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.detail, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (mol == null || mol.getAllBonds() == 0)
				((MoleculeViewer)viewInfo.view).setMolecule((StereoMolecule)null);
			else {
				((MoleculeViewer)viewInfo.view).setMolecule(mol);
				((MoleculeViewer)viewInfo.view).resetView();
				((MoleculeViewer)viewInfo.view).repaint();
				}
			}
		else if (viewInfo.type.equals(IMAGE)) {
			((JImagePanel)viewInfo.view).setFileName((mCurrentRecord == null) ? null
							: mTableModel.encodeData(mCurrentRecord, viewInfo.column));
			}
		else if (viewInfo.type.equals(RESULT_DETAIL)) {
			if (mCurrentRecord == null) {
				((JResultDetailView)viewInfo.view).setReferences(null);
				}
			else {
				String[][] reference = mCurrentRecord.getDetailReferences(viewInfo.column);
				((JResultDetailView)viewInfo.view).setReferences(reference == null
											|| reference.length<=viewInfo.detail ?
							null : reference[viewInfo.detail]);
				}
			}
		}
	}

class DetailViewInfo {
	public JComponent view;
	public int column,detail;
	public String type;

	public DetailViewInfo(JComponent view, int column, int detail, String type) {
		this.view = view;
		this.column = column;   // is idcode column in case of STRUCTURE(_3D)
		this.detail = detail;   // is coordinate column in case of STRUCTURE(_3D)
		this.type = type;
		}
	}
