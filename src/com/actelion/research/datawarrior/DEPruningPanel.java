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

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.filter.*;
import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.VerticalFlowLayout;
import com.actelion.research.table.*;
import com.actelion.research.table.filter.*;
import com.actelion.research.table.model.*;
import com.actelion.research.util.ColorHelper;

import javax.swing.*;
import java.awt.*;

public class DEPruningPanel extends JScrollPane
                implements CompoundTableListener,CompoundTableListListener,FilterListener {
    private static final long serialVersionUID = 0x20060904;

    private JPanel              mContent;
	private Frame               mOwner;
	private DEParentPane		mParentPane;
    private CompoundTableModel  mTableModel;
	private String              mRecentErrorMessage;
	private long                mRecentErrorMillis;

    public DEPruningPanel(Frame owner, DEParentPane parentPane, CompoundTableModel tableModel) {
		mContent = new JPanel() {
		    private static final long serialVersionUID = 0x20100729;

		    @Override
		    public void paintComponent(Graphics g) {
		    	super.paintComponent(g);

			    Color color = LookAndFeelHelper.isDarkLookAndFeel() ?
					    ColorHelper.brighter(getBackground(), 0.85f) : ColorHelper.darker(getBackground(), 0.85f);
		    	g.setColor(color);
			    Rectangle r = new Rectangle();
		    	for (int i=1; i<getComponentCount(); i++) {
		    		getComponent(i).getBounds(r);
		    		g.drawLine(r.x+2, r.y-3, r.x+r.width-3, r.y-3);
		    		g.drawLine(r.x+2, r.y-2, r.x+r.width-3, r.y-2);
		    		}
		    	}

		    @Override
		    public void remove(Component comp) {
		    	super.remove(comp);
		    	mParentPane.fireRuntimePropertyChanged(
		    			new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_REMOVE_FILTER, -1));
		    	}
			};
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(100, 100));
		setBorder(BorderFactory.createEmptyBorder());
        mContent.setLayout(new VerticalFlowLayout());
		setViewportView(mContent);

		mOwner = owner;
		mParentPane = parentPane;
        mTableModel = tableModel;
        tableModel.addCompoundTableListener(this);
        tableModel.getListHandler().addCompoundTableListListener(this);
        }

    public DEParentPane getParentPane() {
    	return mParentPane;
    	}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

    public void addDefaultFilters() {
		try {
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnTypeCategory(column)) {
                    addCategoryBrowser(mTableModel);
                    break;
                    }
                }

			for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
				addDefaultFilter(i);

			if (mTableModel.getListHandler().getListCount() > 0) {
				JFilterPanel filter = new JHitlistFilterPanel(mTableModel, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}

			validate();
			repaint();
			}
		catch (FilterException fpe) {
			showErrorMessage(fpe.getMessage());
			}
		}

	/**
	 * We actually only show the message, if the same message has not been shown during the recent 5 seconds
	 */
	private void showErrorMessage(String message) {
		if (System.currentTimeMillis() - mRecentErrorMillis > 5000
		 || !message.equals(mRecentErrorMessage)) {
			JOptionPane.showMessageDialog(mOwner, message);
			mRecentErrorMessage = message;
			}
		mRecentErrorMillis = System.currentTimeMillis();
		}

    private int allocateFilterFlag() throws FilterException {
		int flag = mTableModel.getUnusedRowFlag(true);

		if (flag == -1)
			throw new FilterException("Maximum number of open filters reached.");

		return flag;
    	}

    public void addDefaultFilter(int column) throws FilterException {
		if (mTableModel.isColumnTypeDouble(column)) {
			if (mTableModel.isColumnDataComplete(column)
			 && mTableModel.getMinimumValue(column) != mTableModel.getMaximumValue(column)) {
				JFilterPanel filter = new JRangeFilterPanel(mTableModel, column, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}
			}
		else if (mTableModel.isColumnTypeRangeCategory(column)) {
			if (mTableModel.getCategoryCount(column) <= 9) {
				JFilterPanel filter = new JCategoryFilterPanel(mTableModel, column, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}
			else {
				JFilterPanel filter = new JRangeFilterPanel(mTableModel, column, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}
			}
		else if (mTableModel.isColumnTypeCategory(column)
			  && mTableModel.getCategoryCount(column) <= JCategoryFilterPanel.cPreferredCheckboxCount) {
			JFilterPanel filter = new JCategoryFilterPanel(mTableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			}
        else if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) {
            if (mTableModel.hasDescriptorColumn(column)) {
                JStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, mTableModel, column, allocateFilterFlag(), null);
				filter.addFilterListener(this);
                mContent.add(filter);
                }
			}
        else if (CompoundTableModel.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column))) {
            if (mTableModel.hasDescriptorColumn(column)) {
				JSingleStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, mTableModel, column, allocateFilterFlag(), null);
				filter.addFilterListener(this);
                mContent.add(filter);
                }
            }
        else if (mTableModel.isColumnTypeString(column)) {
            JFilterPanel filter = new JTextFilterPanel(mTableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
            mContent.add(filter);
            }
		}

	public void ensureStructureFilter(int idcodeColumn) {
        if (!structureFilterExists(idcodeColumn)) {
        	try {
	        	JStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, mTableModel, idcodeColumn, allocateFilterFlag(), null);
				filter.addFilterListener(this);
	        	mContent.add(filter);
				validate();
				repaint();
        		}
    		catch (FilterException fpe) {
			    showErrorMessage(fpe.getMessage());
    			}

        	}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			Component[] filter = mContent.getComponents();
			for (int i=0; i<filter.length; i++)
				((JFilterPanel)filter[i]).removePanel();
			if (e.getSpecifier() == CompoundTableEvent.cSpecifierDefaultRuntimeProperties)
				addDefaultFilters();
			validate();
			repaint();
            return;
			}

        Component[] filter = mContent.getComponents();
        for (int i=0; i<filter.length; i++)
            ((JFilterPanel)filter[i]).compoundTableChanged(e);

        if (e.getType() == CompoundTableEvent.cAddColumns) {
		    assert(e.getSource() == mTableModel);
			try {
				for (int column=e.getColumn(); column<mTableModel.getTotalColumnCount(); column++)
					addDefaultFilter(column);
				}
			catch (FilterException fpe) {
				showErrorMessage(fpe.getMessage());
				}
			validate();
			}
        }

    public void listChanged(CompoundTableListEvent e) {
        CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();

        if (e.getType() == CompoundTableListEvent.cDelete) {
            if (hitlistHandler.getListCount() == 0) {
                Component[] filter = mContent.getComponents();
                for (int i=0; i<filter.length; i++)
                    if (filter[i] instanceof JHitlistFilterPanel)
                        ((JFilterPanel)filter[i]).removePanel();
                }
            }

        boolean hitlistFiltersVisible = false;
        Component[] filter = mContent.getComponents();
        for (int i=0; i<filter.length; i++) {
            if (filter[i] instanceof JHitlistFilterPanel) {
                ((JHitlistFilterPanel)filter[i]).listChanged(e);
                hitlistFiltersVisible = true;
                }
            }

        if (e.getType() == CompoundTableListEvent.cAdd) {
            if (!hitlistFiltersVisible) {
                try {
                	JHitlistFilterPanel f = new JHitlistFilterPanel(mTableModel, allocateFilterFlag());
    				f.addFilterListener(this);
                    mContent.add(f);
                    }
                catch (FilterException fpe) {}
                validate();
                }
            }
        }

    public JCategoryBrowser addCategoryBrowser(CompoundTableModel tableModel) throws FilterException {
        JCategoryBrowser filter = new JCategoryBrowser(mOwner, tableModel, allocateFilterFlag());
		filter.addFilterListener(this);
        mContent.add(filter);
        validate();
        repaint();
        return filter;
        }

    public JTextFilterPanel addTextFilter(CompoundTableModel tableModel, int column) throws FilterException {
		if (column == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS || tableModel.getColumnSpecialType(column) == null) {
			JTextFilterPanel filter = new JTextFilterPanel(tableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			validate();
			repaint();
			return filter;
			}
		return null;
		}

	public JRangeFilterPanel addDoubleFilter(CompoundTableModel tableModel, int column) throws FilterException {
		if (tableModel.isColumnTypeDouble(column)) {
			JRangeFilterPanel filter = new JRangeFilterPanel(tableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			validate();
			repaint();
			return filter;
			}
		return null;
		}

	public JCategoryFilterPanel addCategoryFilter(CompoundTableModel tableModel, int column) throws FilterException {
		if (tableModel.isColumnTypeCategory(column)) {
			JCategoryFilterPanel filter = new JCategoryFilterPanel(tableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			validate();
			repaint();
			return filter;
			}
		return null;
		}

	public JSingleStructureFilterPanel addStructureFilter(CompoundTableModel tableModel, int column, StereoMolecule mol) throws FilterException {
		JSingleStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, tableModel, column, allocateFilterFlag(), mol);
		filter.addFilterListener(this);
		mContent.add(filter);
		validate();
		repaint();
		return filter;
		}

	public JMultiStructureFilterPanel addStructureListFilter(CompoundTableModel tableModel, int column, boolean isSSS) throws FilterException {
		JMultiStructureFilterPanel filter = new JMultiStructureFilterPanel(mOwner, tableModel, column, allocateFilterFlag(), isSSS);
		mContent.add(filter);
		filter.addFilterListener(this);
		validate();
		repaint();
		return filter;
		}

    public JReactionFilterPanel addReactionFilter(CompoundTableModel tableModel, int column, Reaction rxn) throws FilterException {
        JReactionFilterPanel filter = new JReactionFilterPanel(mOwner, tableModel, column, allocateFilterFlag(), rxn);
		filter.addFilterListener(this);
        mContent.add(filter);
        validate();
        repaint();
        return filter;
        }

	public JHitlistFilterPanel addHitlistFilter(CompoundTableModel tableModel) throws FilterException {
		JHitlistFilterPanel filter = new JHitlistFilterPanel(tableModel, allocateFilterFlag());
		filter.addFilterListener(this);
		mContent.add(filter);
		validate();
		repaint();
		return filter;
		}

	public void disableAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++)
			((JFilterPanel)filter[i]).setEnabled(false);
		}

	public void enableAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++)
			((JFilterPanel)filter[i]).setEnabled(true);
		}

	public void removeAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++)
			((JFilterPanel)filter[i]).removePanel();
		}

	public void resetAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++)
			((JFilterPanel)filter[i]).reset();
		}

	public int getFilterCount() {
		return mContent.getComponentCount();
		}

	public JFilterPanel getFilter(int index) {
		return (JFilterPanel)mContent.getComponent(index);
		}

	public int getFilterDuplicateIndex(JFilterPanel filterPanel, int column) {
		int index = 0;
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++) {
			if (filter[i] == filterPanel)
				return index;
			if (filterPanel.getClass() == filter[i].getClass()
			 && filterPanel.getColumnIndex() == ((JFilterPanel)filter[i]).getColumnIndex())
				index++;
			}
		return index;
		}

	/**
	 *
	 * @param filterType
	 * @param column -1 if this filter type doesn't need to match the column
	 * @param duplicateIndex
	 * @return
	 */
	public JFilterPanel getFilter(int filterType, int column, int duplicateIndex) {
		int index = 0;
		Component[] component = mContent.getComponents();
		for (int i=0; i<component.length; i++) {
			JFilterPanel filter = (JFilterPanel)component[i];
			if (filterType == filter.getFilterType()
			 && (column == -1 || column == filter.getColumnIndex())) {
				if (index == duplicateIndex)
					return filter;
				index++;
				}
			}
		return null;
		}

	private boolean structureFilterExists(int idcodeColumn) {
        Component[] filter = mContent.getComponents();
        for (int i=0; i<filter.length; i++)
            if (filter[i] instanceof JStructureFilterPanel
             && idcodeColumn == ((JFilterPanel)filter[i]).getColumnIndex())
                return true;
        return false;
        }

    public class FilterException extends Exception {
        private static final long serialVersionUID = 0x20110325;

        public FilterException(String msg) {
    		super(msg);
    		}
    	}

	@Override
	public void filterChanged(FilterEvent e) {
		if (DEMacroRecorder.getInstance().isRecording()) {
			AbstractTask task = null;
			JFilterPanel filter = e.getSource();
			if (e.getType() == FilterEvent.FILTER_CLOSED)
				task = new DETaskCloseFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JCategoryBrowser)
				task = new DETaskChangeCategoryBrowser(mOwner, this, filter);
			else if (e.getSource() instanceof JCategoryFilterPanel)
				task = new DETaskChangeCategoryFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JHitlistFilterPanel)
				task = new DETaskChangeListFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JRangeFilterPanel)
				task = new DETaskChangeRangeFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JMultiStructureFilterPanel) {
				if (((JMultiStructureFilterPanel)e.getSource()).supportsSSS())
					task = new DETaskChangeSubstructureListFilter(mOwner, this, filter);
				else
					task = new DETaskChangeSimilarStructureListFilter(mOwner, this, filter);
				}
			else if (e.getSource() instanceof JSingleStructureFilterPanel)
				task = new DETaskChangeStructureFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JTextFilterPanel)
				task = new DETaskChangeTextFilter(mOwner, this, filter);

			if (task != null)
				DEMacroRecorder.record(task, task.getPredefinedConfiguration());
			}
		}
	}

