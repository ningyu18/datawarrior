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

package com.actelion.research.datawarrior.task.db;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerFFP512;
import com.actelion.research.database.OsirisConnectionHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;

public abstract class DETaskStructureQuery extends ConfigurableTask implements ActionListener,ProgressController,Runnable {
    public static final int SEARCH_TYPE_SSS = StructureSearchSpecification.TYPE_SUBSTRUCTURE;
    public static final int SEARCH_TYPE_SIMILARITY = StructureSearchSpecification.TYPE_SIMILARITY;
    public static final int SEARCH_TYPE_EXACT = StructureSearchSpecification.TYPE_EXACT_STRICT;
    public static final int SEARCH_TYPE_NO_STEREO = StructureSearchSpecification.TYPE_EXACT_NO_STEREO | StructureSearchSpecification.MODE_LARGEST_FRAGMENT_ONLY;
    public static final int SEARCH_TYPE_TAUTOMER = StructureSearchSpecification.TYPE_TAUTOMER | StructureSearchSpecification.MODE_LARGEST_FRAGMENT_ONLY;

    public static final int QUERY_SOURCE_DRAWN = 0;
    public static final int QUERY_SOURCE_SELECTED = 1;

    protected static final String PROPERTY_QUERY_SOURCE = "querySource";
    protected static final String PROPERTY_SEARCH_TYPE = "searchType";
    protected static final String PROPERTY_IDCODE = "idcode";
    protected static final String PROPERTY_IDCOORDS = "idcoordinates";
    protected static final String PROPERTY_SIMILARITY = "similarity";
    protected static final String PROPERTY_DESCRIPTOR_NAME = "descriptorName";	// not used yet; for future use

    private static final String[] SEARCH_TYPE_TEXT = {"superstructures of", "similar structures to", "equal structures to" };
    private static final String[] SEARCH_TYPE_TEXT_EXT = {"superstructures of", "similar structures to", "equal structures to", "stereo isomers of", "tautomers of" };
    private static final String[] SEARCH_TYPE_CODE = {"sss", "similarity", "exact", "noStereo", "tautomer" };
    private static final String[] QUERY_SOURCE = {"the structure drawn below", "any selected structure"};
    protected static final String[] QUERY_SOURCE_CODE = {"drawn", "selected"};

    private static final int[] SEARCH_TYPE = { SEARCH_TYPE_SSS, SEARCH_TYPE_SIMILARITY, SEARCH_TYPE_EXACT, SEARCH_TYPE_NO_STEREO, SEARCH_TYPE_TAUTOMER };

	protected DEFrame				mTargetFrame;
	protected Connection          	mConnection;
	protected ArrayList<Object[]>	mResultList;
    protected CompoundTableModel	mSourceTableModel;

	private DataWarrior				mApplication;
    private JComboBox   		    mComboBoxSearchType,mComboBoxQuerySource;
    private JEditableStructureView  mStructureView;
    private JSlider         		mSimilaritySlider;
    private boolean					mDisableEvents,mIsMultiStructureQueryPossible;
    private int						mIDCodeColumn;

	public static boolean isIndexVersionOK(Connection connection) throws SQLException {
		boolean indexVersionOK = true;

		Statement stmt = connection.createStatement();
		ResultSet rset = stmt.executeQuery ("SELECT idcode FROM actcart.actstructkeys ORDER BY id");
		for (int i=0; i<SSSearcherWithIndex.getNoOfKeys(); i++) {
			if (!rset.next()) {
				indexVersionOK = false;
				break;
				}
			if (!rset.getString(1).equals(SSSearcherWithIndex.cKeyIDCode[i])) {
				indexVersionOK = false;
				break;
				}
			}

		rset.close();
		stmt.close();

		return indexVersionOK;
		}
	
    public DETaskStructureQuery(DEFrame owner, DataWarrior application, boolean dataFromOSIRIS) {
    	super(owner, true);

    	mApplication = application;
		mSourceTableModel = owner.getTableModel();

        CompoundTableModel tableModel = ((DEFrame)owner).getTableModel();
        mIsMultiStructureQueryPossible = false;
        for (int column=0; column<tableModel.getTotalColumnCount(); column++) {
            if (CompoundTableModel.cColumnTypeIDCode.equals(tableModel.getColumnSpecialType(column))) {
                mIsMultiStructureQueryPossible = true;
                break;
                }
            }
        if (mIsMultiStructureQueryPossible) {
            boolean selectionFound = false;
            for (int row=0; row<tableModel.getRowCount(); row++) {
                if (tableModel.isSelected(row)) {
                    selectionFound = true;
                    break;
                    }
                }
            if (!selectionFound)
                mIsMultiStructureQueryPossible = false;
            }

		if (dataFromOSIRIS) {
			try {
				mConnection = OsirisConnectionHelper.getConnection(owner);
				if (mConnection == null)	// user cancelled login dialog
					return;
				}
			catch (Exception e) {
	    		JOptionPane.showMessageDialog(owner, e);
		    	return;
			    }
			}

	    }

	protected abstract String getDocumentTitle();
	protected abstract String getSQL();
	protected abstract String[] getColumnNames();
    protected abstract int getIdentifierColumn();

	/**
	 * This creates an initial configuration including common structure search options.
	 * Derived classes are expected to add additional search options.
	 */
	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	boolean drawnStructureAvailable = false;
    	if (mStructureView != null && mStructureView.isEnabled() && mStructureView.getMolecule().getAllAtoms() != 0) {
    		Canonizer canonizer = new Canonizer(mStructureView.getMolecule());
    		configuration.setProperty(PROPERTY_IDCODE, canonizer.getIDCode());
    		configuration.setProperty(PROPERTY_IDCOORDS, canonizer.getEncodedCoordinates());
    		drawnStructureAvailable = true;
    		}

    	if (drawnStructureAvailable || mComboBoxQuerySource.getSelectedIndex() == QUERY_SOURCE_SELECTED) {
	    	if (mComboBoxSearchType != null)
	    		configuration.setProperty(PROPERTY_SEARCH_TYPE, SEARCH_TYPE_CODE[mComboBoxSearchType.getSelectedIndex()]);
	
	    	if (mComboBoxQuerySource != null)
	    		configuration.setProperty(PROPERTY_QUERY_SOURCE, QUERY_SOURCE_CODE[mComboBoxQuerySource.getSelectedIndex()]);
	
	    	if (mSimilaritySlider != null && mSimilaritySlider.isEnabled())
	        	configuration.setProperty(PROPERTY_SIMILARITY, ""+mSimilaritySlider.getValue());
    		}

    	return configuration;
    	}

	@Override
    public void setDialogConfiguration(Properties configuration) {
    	mDisableEvents = true;

    	if (mStructureView != null)
    		mStructureView.setIDCode(configuration.getProperty(PROPERTY_IDCODE), configuration.getProperty(PROPERTY_IDCOORDS));

    	if (mComboBoxSearchType != null) {
	    	String value = configuration.getProperty(PROPERTY_SEARCH_TYPE);
	    	for (int i=0; i<SEARCH_TYPE_CODE.length; i++)
	    		if (SEARCH_TYPE_CODE[i].equals(value))
	    			mComboBoxSearchType.setSelectedIndex(i);
    		}

    	if (mComboBoxQuerySource != null) {
    		String value = configuration.getProperty(PROPERTY_QUERY_SOURCE);

    		// don't use "any selected structure" if we have no selection
    		if (QUERY_SOURCE_CODE[QUERY_SOURCE_SELECTED].equals(value)
			 && isInteractive()
    		 && !mSourceTableModel.hasSelectedRows()) {
    			mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);
    			}
    		else {
		    	for (int i=0; i<QUERY_SOURCE_CODE.length; i++)
		    		if (QUERY_SOURCE_CODE[i].equals(value))
		    			mComboBoxQuerySource.setSelectedIndex(i);
    			}
    		}

    	if (mSimilaritySlider != null) {
	    	String value = configuration.getProperty(PROPERTY_SIMILARITY);
	    	if (value != null)
	    		mSimilaritySlider.setValue(Integer.parseInt(value));
    		}

    	mDisableEvents = false;

    	enableStructureItems();
    	}

	@Override
    public void setDialogConfigurationToDefault() {
    	mDisableEvents = true;

    	if (mStructureView != null)
    		mStructureView.setIDCode(null, null);

    	if (mComboBoxSearchType != null)
   			mComboBoxSearchType.setSelectedIndex(findArrayIndex(SEARCH_TYPE_SSS, SEARCH_TYPE));

    	if (mComboBoxQuerySource != null)
    		mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);

    	if (mSimilaritySlider != null)
    		mSimilaritySlider.setValue(80);

    	mDisableEvents = false;

    	enableStructureItems();
    	}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive && QUERY_SOURCE_CODE[QUERY_SOURCE_SELECTED].equals(configuration.getProperty(PROPERTY_QUERY_SOURCE))) {
	        int[] idcodeColumnList = mSourceTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
	        if (idcodeColumnList == null) {
	        	showErrorMessage("None of your columns contains chemical structures.");
	        	return false;
	            }

	        boolean selectionFound = false;
	        for (int row=0; row<mSourceTableModel.getRowCount(); row++) {
				if (mSourceTableModel.getRecord(row).isSelected()) {
					selectionFound = true;
					break;
					}
				}
	        if (!selectionFound) {
	        	showErrorMessage("No rows are selected.");
	        	return false;
	            }
			}

		return true;
		}

    /**
     * If the dialog was not shown yet, then the behaviour is undefined.
     * @return query structure defined in the dialog. One of QUERY_SOURCE_...
     */
    protected StereoMolecule getDialogQueryStructure() {
    	if (mStructureView != null)
    		return mStructureView.getMolecule();
    	return null;
    	}

    /**
     * If the dialog was not shown yet, then the behaviour is undefined.
     * @return query source defined in the dialog. One of QUERY_SOURCE_...
     */
    protected int getDialogQuerySource() {
    	if (mComboBoxQuerySource != null)
    		return mComboBoxQuerySource.getSelectedIndex();
    	return -1;
    	}

    /**
     * If no configuration was set then the behaviour is undefined.
     * @return query idcode of current configuration.
     */
    protected String getQueryIDCode(Properties configuration) {
    	return configuration.getProperty(PROPERTY_IDCODE);
    	}

    /**
     * If no configuration was set then the behaviour is undefined.
     * @return short name of descriptor to be used for similarity search
     */
    protected String getDescriptorShortName(Properties configuration) {
    	return configuration.getProperty(PROPERTY_DESCRIPTOR_NAME, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
    	}

    /**
     * If no configuration was set then the behaviour is undefined.
     * @return similarity value of current configuration (0.0 <= value <= 1.0).
     */
    protected float getSimilarityValue(Properties configuration) {
    	String value = configuration.getProperty(PROPERTY_SIMILARITY);
    	if (value != null)
    		try { return Float.parseFloat(value) / 100; } catch (NumberFormatException e) {}
    	return Float.NaN;
    	}

    /**
     * If no configuration was set then the behaviour is undefined.
     * @return search type of current configuration. One of SEARCH_TYPE_...
     */
    protected int getSearchType(Properties configuration) {
    	String value = configuration.getProperty(PROPERTY_SEARCH_TYPE);
    	for (int i=0; i<SEARCH_TYPE_CODE.length; i++)
    		if (SEARCH_TYPE_CODE[i].equals(value))
    			return SEARCH_TYPE[i];
    	return -1;	// should not happen
    	}

    /**
     * If no configuration was set then the behaviour is undefined.
     * @return query source of current configuration. One of QUERY_SOURCE_...
     */
    protected int getQuerySource(Properties configuration) {
    	String value = configuration.getProperty(PROPERTY_QUERY_SOURCE);
    	for (int i=0; i<QUERY_SOURCE_CODE.length; i++)
    		if (QUERY_SOURCE_CODE[i].equals(value))
    			return i;
    	return QUERY_SOURCE_DRAWN;
    	}

	/**
	 * Creates a valid StructureSearchSpecification from the configuration
	 * of the currently executing task, provided that the configuration
	 * contains the necessary information.
	 * @return valid SSSpec or null
	 */
	protected StructureSearchSpecification getStructureSearchSpecification(Properties configuration) {
		int searchType = getSearchType(configuration);

		byte[][] idcode = null;
		Object[] descriptor = null;
		if (getQuerySource(configuration) == QUERY_SOURCE_DRAWN) {
			String idcodeString = getQueryIDCode(configuration);
			if (idcodeString == null)
				return null;

			idcode = new byte[1][];
			idcode[0] = idcodeString.getBytes();
			}
		else {
			idcode = getSelectedIDCodes();
			if (searchType == SEARCH_TYPE_SIMILARITY) {
				descriptor = getSelectedDescriptors(getDescriptorShortName(configuration));
				if (!DescriptorConstants.DESCRIPTOR_FFP512.shortName.equals(getDescriptorShortName(configuration)))
					searchType |= StructureSearchSpecification.MODE_LARGEST_FRAGMENT_ONLY;
				}
			}

		StructureSearchSpecification spec = new StructureSearchSpecification(searchType, idcode,
				descriptor, getDescriptorShortName(configuration), getSimilarityValue(configuration));
		
		String errorMessage = spec.validate();
		if (errorMessage == null)
			return spec;

		showErrorMessage(errorMessage);
		return null;
		}

    public void actionPerformed(ActionEvent e) {
    	if (mDisableEvents)
    		return;

    	if (e.getSource() == mComboBoxSearchType) {
            boolean isSSS = (SEARCH_TYPE[mComboBoxSearchType.getSelectedIndex()]==SEARCH_TYPE_SSS);
            if (mStructureView.getMolecule().isFragment() ^ isSSS) {
	            mStructureView.getMolecule().setFragment(isSSS);
	            mStructureView.structureChanged();
            	}
            if (isSSS && mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_SELECTED) {
            	mDisableEvents = true;
            	mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);
            	mDisableEvents = false;
            	}
            enableStructureItems();
            return;
            }
        else if (e.getSource() == mComboBoxQuerySource) {
            boolean isMultiQuery = (mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_SELECTED);
            if (isMultiQuery && SEARCH_TYPE[mComboBoxSearchType.getSelectedIndex()]==SEARCH_TYPE_SSS) {
            	mDisableEvents = true;
            	mComboBoxSearchType.setSelectedIndex(findArrayIndex(SEARCH_TYPE_SIMILARITY, SEARCH_TYPE));
            	mDisableEvents = false;

	            mStructureView.getMolecule().setFragment(false);
	            mStructureView.structureChanged();
            	}
            enableStructureItems();
            return;
            }
		}

    private int findArrayIndex(int value, int[] array) {
    	for (int i=0; i<array.length; i++)
    		if (value == array[i])
    			return i;
    	return -1;
    	}

    @Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		startProgress("Retrieving Chemical Data...", 0, 0);

		try {
		    retrieveRecords();
			}
		catch (Exception e) {
		    e.printStackTrace();
		    showErrorMessage(e.toString());
			mResultList = null;
			return;
			}
		if (mResultList.size() == 0) {
			showErrorMessage("Your query did not retrieve any records.");
			mResultList = null;
			return;
			}

		startProgress("Checking Index Version...", 0, 0);

		if (!threadMustDie()) {
			mTargetFrame = mApplication.getEmptyFrame(getDocumentTitle());
			CompoundTableModel tableModel = mTargetFrame.getTableModel();
            String[] columnName = getColumnNames();
		    int columnCount = columnName.length + 3;
			tableModel.initializeTable(mResultList.size(), columnCount);
            tableModel.prepareStructureColumns(0, "Structure", true, true);
            if (getIdentifierColumn() != -1)
            	tableModel.setColumnProperty(0, CompoundTableModel.cColumnPropertyIdentifierColumn, columnName[getIdentifierColumn()]);
			for (int i=3; i<columnCount; i++)
				tableModel.setColumnName(columnName[i-3], i);

			startProgress("Populating Table...", 0, mResultList.size());
			for (int row=0; row<mResultList.size(); row++) {
				updateProgress(row);

			    Object[] result = mResultList.get(row);
				for (int column=0; column<columnCount; column++) {
					tableModel.setTotalDataAt(result[column], row, column);
					}
				}
			}

		mResultList = null;

        if (mTargetFrame != null) {
        	setColumnProperties();
        	int rtpKind = useDefaultRuntimeProperties() ? CompoundTableEvent.cSpecifierDefaultRuntimeProperties
        												: CompoundTableEvent.cSpecifierNoRuntimeProperties;
        	mTargetFrame.getTableModel().finalizeTable(rtpKind, this);
        	}

        if (mTargetFrame != null) {
        	try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
			        	mTargetFrame.setTitle(getDocumentTitle());
				        setRuntimeProperties();
						}
					} );
        		}
        	catch (Exception e) {}
        	}
	    }

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	protected void retrieveRecords() throws Exception {
	    Statement stmt = mConnection.createStatement();
		ResultSet rset = stmt.executeQuery(getSQL());
		mResultList = new ArrayList<Object[]>();
		while (rset.next()) {
		    int noOfColumns = rset.getMetaData().getColumnCount();
			Object[] newResult = new Object[noOfColumns];
			boolean found = false;
			for (int i=0; i<noOfColumns; i++) {
			    // columns: 0->idcodeBytes; 1->coordinateBytes, 2->indexInts; 3...->valueBytes
			    if (i == 2) {	// index
			        newResult[i] = DescriptorHandlerFFP512.getDefaultInstance().decode(rset.getString(i+1));
			    	}
				else {
					String s = rset.getString(i+1);
					if (s != null && s.length() != 0) {
						newResult[i] = s.getBytes();
				        found = true;
						}
					}
				}
			if (found)
			    mResultList.add(newResult);
			}
		rset.close();
		stmt.close();
		}

	/**
	 * This merges all rows with the same entry, i.e. idcode, in column 0.
	 * Column 1 and 2 are considered to contain idcoords and FragFp. All other
	 * columns will contain as many lines of content as there were source rows
	 * that shared the same entry in column 0.
	 * Optionally one may flag other columns to show the first row's value rather
	 * than every source row's values.
	 * @param skipColumn null or flags to define which columns shall be skipped
	 * @param otherGroupColumn null or column(s) that cause row splitting if its content differs 
	 */
    protected void groupByStructure(boolean[] skipColumn, int[] otherGroupColumn) {
        TreeMap<String,Object[]> map = new TreeMap<String,Object[]>();
        ArrayList<Object[]> newResultList = new ArrayList<Object[]>();
        for (Object[] result:mResultList) {
            String key = new String((byte[])result[0]);
            if (otherGroupColumn != null) {
            	for (int column:otherGroupColumn) {
            		key = key.concat("#");
            		if (result[column] != null && ((byte[])result[column]).length != 0)
            			key = key.concat(new String((byte[])result[column]));
            		}
            	}
            Object[] newResult = map.get(key);
            if (newResult == null) {
                map.put(key, result);
            	newResultList.add(result);
            	}
            else {
                for (int i=3; i<result.length; i++) {
                	boolean isExtendedKey = false;
                    if (otherGroupColumn != null) {
                    	for (int column:otherGroupColumn) {
                    		if (i == column) {
                    			isExtendedKey = true;
                    			break;
                    			}
                    		}
                    	}
                	
                	if (!isExtendedKey && (skipColumn == null || !skipColumn[i])) {
	                    byte[] bytes1 = (byte[])newResult[i];
	                    byte[] bytes2 = (byte[])result[i];
	                    int bytes1count = (bytes1 == null) ? 0 : bytes1.length;
	                    int bytes2count = (bytes2 == null) ? 0 : bytes2.length;
	
	                    byte[] bytes = new byte[bytes1count+bytes2count+1];
	                    int byteIndex = 0;
	                    for (int j=0; j<bytes1count; j++)
	                        bytes[byteIndex++] = bytes1[j];
	                    bytes[byteIndex++] = '\n';
	                    for (int j=0; j<bytes2count; j++)
	                        bytes[byteIndex++] = bytes2[j];
	                    newResult[i] = bytes;
                		}
                	}
            	}
        	}

        // get rid of empty content concatenated by '\n'
        for (Object[] result:mResultList) {
            for (int i=3; i<result.length; i++) {
                byte[] bytes = (byte[])result[i];
                boolean found = false;
                if (bytes != null) {
                    for (int j=0; j<bytes.length; j++) {
                        if (bytes[j] != '\n') {
                            found = true;
                            break;
                            }
                        }
                    }
                if (!found)
                    result[i] = null;
                }
            }

        mResultList = newResultList;
    	}

    /**
     * Returns true if current dataset contains structures and more than one row is selected.
     * @return
     */
    protected boolean isMultiStructureQueryPossible() {
    	return mIsMultiStructureQueryPossible;
    	}

    protected JComponent createComboBoxSearchType() {
    	return createComboBoxSearchType(false);
    	}

    protected JComponent createComboBoxSearchType(boolean includeExtended) {
        mComboBoxSearchType = new JComboBox(includeExtended ? SEARCH_TYPE_TEXT_EXT : SEARCH_TYPE_TEXT);
        mComboBoxSearchType.addActionListener(this);
        return mComboBoxSearchType;
    	}

    protected JComponent createComboBoxQuerySource() {
        mComboBoxQuerySource = new JComboBox(QUERY_SOURCE);
        mComboBoxQuerySource.addActionListener(this);
        mComboBoxQuerySource.setEnabled(mIsMultiStructureQueryPossible);
        return mComboBoxQuerySource;
    	}

    protected JComponent createStructureView() {
        StereoMolecule mol = new StereoMolecule();
        mol.setFragment(true);
        mStructureView = new JEditableStructureView(mol);
        mStructureView.setMinimumSize(new Dimension(100, 100));
        mStructureView.setPreferredSize(new Dimension(100, 100));
        mStructureView.setBorder(BorderFactory.createTitledBorder("Structure"));
        return mStructureView;
    	}

    protected JComponent createSimilaritySlider() {
        Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
        labels.put(new Integer(80), new JLabel("80%"));
        labels.put(new Integer(90), new JLabel("90%"));
        labels.put(new Integer(100), new JLabel("100%"));
        mSimilaritySlider = new JSlider(JSlider.VERTICAL, 80, 100, 90);
        mSimilaritySlider.setMinorTickSpacing(1);
        mSimilaritySlider.setMajorTickSpacing(10);
        mSimilaritySlider.setLabelTable(labels);
        mSimilaritySlider.setPaintLabels(true);
        mSimilaritySlider.setPaintTicks(true);
        int width = mSimilaritySlider.getPreferredSize().width;
        mSimilaritySlider.setMinimumSize(new Dimension(width, 116));
        mSimilaritySlider.setPreferredSize(new Dimension(width, 116));
        mSimilaritySlider.setEnabled(false);
        JPanel spanel = new JPanel();
        spanel.add(mSimilaritySlider);
        spanel.setBorder(BorderFactory.createTitledBorder("Similarity"));
        return spanel;
    	}

    private void enableStructureItems() {
        boolean isSSS = (SEARCH_TYPE[mComboBoxSearchType.getSelectedIndex()]==SEARCH_TYPE_SSS);
        boolean isSimilaritySearch = (SEARCH_TYPE[mComboBoxSearchType.getSelectedIndex()]==SEARCH_TYPE_SIMILARITY);

        mSimilaritySlider.setEnabled(isSimilaritySearch);
        if (mComboBoxQuerySource != null)
        	mComboBoxQuerySource.setEnabled(isMultiStructureQueryPossible() && !isSSS);
        mStructureView.setEnabled(mComboBoxQuerySource == null || mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_DRAWN);
    	}

    /**
     * Creates an array of all selected idcodes. May ask user
     * to select one of multiple structure columns.
     * If no rows are selected or not structure column exists,
     * then an empty array is returned.
     * @return array of selected idcodes
     */
    protected byte[][] getSelectedIDCodes() {
    	// TODO use selected column in configuration
    	if (SwingUtilities.isEventDispatchThread()) {
            mIDCodeColumn = selectIDCodeColumn();
    		}
    	else {
	    	mIDCodeColumn = -1;
	    	try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
		                mIDCodeColumn = selectIDCodeColumn();
						}
					} );
	    		}
	    	catch (Exception e) {}
    		}

    	TreeSet<byte[]> idcodeSet = new TreeSet<byte[]>(new ByteArrayComparator());
        for (int row=0; row<mSourceTableModel.getRowCount(); row++)
            if (mSourceTableModel.isSelected(row))
            	idcodeSet.add((byte[])mSourceTableModel.getRecord(row).getData(mIDCodeColumn));
        return idcodeSet.toArray(new byte[0][]);
    	}

    /**
     * May only be called after calling getSelectedIDCodes()!!!
     * @return descriptors of selected rows
     */
    protected Object[] getSelectedDescriptors(String descriptorShortName) {
    	if (mIDCodeColumn == -1)
    		return null;

    	int descriptorColumn = mSourceTableModel.getChildColumn(mIDCodeColumn, descriptorShortName);
    	if (descriptorColumn == -1)
    		return null;

    	TreeMap<byte[],Object> map = new TreeMap<byte[],Object>(new ByteArrayComparator());
        for (int row=0; row<mSourceTableModel.getRowCount(); row++) {
            if (mSourceTableModel.isSelected(row)) {
            	CompoundRecord record = mSourceTableModel.getRecord(row);
            	map.put((byte[])record.getData(mIDCodeColumn), record.getData(descriptorColumn));
            	}
        	}
        return map.values().toArray(new Object[0]);
    	}

    protected String[] toStringArray(byte[][] byteArray) {
    	String[] stringArray = new String[byteArray.length];
    	for (int i=0; i<byteArray.length; i++)
    		stringArray[i] = new String(byteArray[i]);
    	return stringArray;
    	}

    private int selectIDCodeColumn() {
        int idcodeColumn = -1;
        ArrayList<String> structureColumnList = null;
        for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
            if (CompoundTableModel.cColumnTypeIDCode.equals(mSourceTableModel.getColumnSpecialType(column))) {
                if (idcodeColumn == -1) {
                    idcodeColumn = column;
                    }
                else if (structureColumnList == null) {
                    structureColumnList = new ArrayList<String>();
                    structureColumnList.add(mSourceTableModel.getColumnTitle(idcodeColumn));
                    structureColumnList.add(mSourceTableModel.getColumnTitle(column));
                    }
                else {
                    structureColumnList.add(mSourceTableModel.getColumnTitle(column));
                    }
                }
            }

        if (structureColumnList != null) {
            String idcodeColumnName = (String)JOptionPane.showInputDialog(getParentFrame(),
                                        "Please select the column containing the query structures!",
                                        "Select Structure Column",
                                        JOptionPane.QUESTION_MESSAGE,
                                        null,
                                        structureColumnList.toArray(),
                                        structureColumnList.get(0));
            idcodeColumn = mSourceTableModel.findColumn(idcodeColumnName);
            }
        
        return idcodeColumn;
        }

    /**
     * May be overridden to define special columns before finalizing the tableModel.
     * This method is called from the worker thread.
     */
    protected void setColumnProperties() {}

    /**
     * Override this and return false, if you don't want any views and filters created
     * @return
     */
    protected boolean useDefaultRuntimeProperties() {
    	return true;
    	}

    /**
     * May be overridden to define view settings after finalizing the tableModel.
     * Overriding methods will be called from the event dispatcher thread after
     * the table model is finalized.
     */
    protected void setRuntimeProperties() {}
    }
