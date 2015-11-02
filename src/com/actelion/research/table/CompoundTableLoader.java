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

package com.actelion.research.table;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.ByteArrayComparator;

public class CompoundTableLoader implements CompoundTableConstants,Runnable {
	public static final String DATASET_COLUMN_TITLE = "Dataset Name";

	public static final byte NEWLINE = '\n';			// used in String values of TableModel
	public static final byte[] NEWLINE_BYTES = NEWLINE_STRING.getBytes();

	public static final byte TAB = '\t';				// used in String values of TableModel
	public static final byte[] TAB_BYTES = TAB_STRING.getBytes();

	public static final int	NEW_COLUMN = -1;	// pseudo destination columns for appending/merging data
	public static final int	NO_COLUMN = -2;

	public static final int MERGE_MODE_IS_KEY = 0;
	public static final int MERGE_MODE_APPEND = 1;
	public static final int MERGE_MODE_KEEP = 2;
	public static final int MERGE_MODE_REPLACE = 3;
	public static final int MERGE_MODE_USE_IF_EMPTY = 4;
	public static final int MERGE_MODE_AS_PARENT = 5;	// <- from here merge mode(s) not being available for user selection

	public static final int READ_DATA = 1;		// load data into buffer
	public static final int REPLACE_DATA = 2;	// empty tablemodel and copy data from buffer to tablemodel
	public static final int APPEND_DATA = 4;	// append data from buffer to tablemodel
	public static final int MERGE_DATA = 8;		// merge data from buffer to tablemodel
	public static final int APPLY_TEMPLATE = 16;// apply the loaded template

	public static final byte[] QUOTES1_BYTES = "\\\"".getBytes();
	public static final byte[] QUOTES2_BYTES = "\"\"".getBytes();

	private static final int PROGRESS_LIMIT = 50000;
	private static final int PROGRESS_STEP = 200;

	private static final int MAX_COLUMNS_FOR_SMILES_CHECK = 64;
	private static final int MAX_ROWS_FOR_SMILES_CHECK = 16;
	private static final int MAX_TOLERATED_SMILES_FAILURES = 4;


	private static volatile IdentifierHandler sIdentifierHandler = new IdentifierHandler() {
		@Override
		public TreeMap<String,String> addDefaultColumnProperties(String columnName, TreeMap<String,String> columnProperties) {
			return columnProperties;
			}

		@Override
		public String getSubstanceIdentifierName() {
			return "Substance-ID";
			}

		@Override
		public String getBatchIdentifierName() {
			return "Batch-ID";
			}

		@Override
		public boolean isIdentifierName(String s) {
			return s.equals(getSubstanceIdentifierName()) || s.equals(getBatchIdentifierName());
			}

		@Override
		public boolean isValidSubstanceIdentifier(String s) {
			return false;
			}

		@Override
		public boolean isValidBatchIdentifier(String s) {
			return false;
			}

		@Override
		public String normalizeIdentifierName(String identifierName) {
			return identifierName;
			}
		};

	private CompoundTableModel	mTableModel;
	private Frame				mParentFrame;
	private volatile ProgressController	mProgressController;
	private volatile File		mFile;
	private volatile Reader		mDataReader;
	private volatile int		mDataType,mAction;
	private int					mOldVersionIDCodeColumn,mOldVersionCoordinateColumn,mOldVersionCoordinate3DColumn;
	private boolean				mWithHeaderLine,mAppendRest,mCoordsMayBe3D;
	private volatile boolean	mOwnsProgressController;
	private String				mNewWindowTitle,mVersion;
	private RuntimeProperties	mRuntimeProperties;
	private String[]			mFieldNames;
	private Object[][]			mFieldData;
	private volatile Thread		mThread;
	private int					mAppendDatasetColumn,mFirstNewColumn;
	private int[]				mAppendDestColumn,mMergeDestColumn,mMergeMode;
	private String				mAppendDatasetNameExisting,mAppendDatasetNameNew;
	private ArrayList<String>	mHitlists;
	private TreeMap<String,String> mColumnProperties;
	private TreeMap<String,Object> mExtensionMap;
	private HashMap<String,byte[]> mDetails;

	public static void setColumnPropertyProvider(IdentifierHandler p) {
		sIdentifierHandler = p;
		}

	/**
	 * This contructor must be invoked from the EventDispatchThread
	 * @param parent
	 * @param tableModel
	 */
	public CompoundTableLoader(Frame parent, CompoundTableModel tableModel) {
		this(parent, tableModel, null);
		}

	/**
	 * If this contructor is not invoked from the EventDispatchThread, then a valid
	 * progress controller must be specified.
	 * @param parent
	 * @param tableModel
	 * @param pc
	 */
	public CompoundTableLoader(Frame parent, CompoundTableModel tableModel, ProgressController pc) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mProgressController = pc;
		}

	public void paste() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable theData = clipboard.getContents(this);
		String s;
		try {
			s = (String)(theData.getTransferData(DataFlavor.stringFlavor));
			mWithHeaderLine = analyzeHeaderLine(new StringReader(s));
			mDataReader = new StringReader(s);
			mAction = READ_DATA | REPLACE_DATA;
			mDataType = FileHelper.cFileTypeTextTabDelimited;
			mNewWindowTitle = "Data From Clipboard";
			mRuntimeProperties = null;
			processData();
			}
		catch (Exception e) {
			mTableModel.unlock();
			e.printStackTrace();
			JOptionPane.showMessageDialog(mParentFrame, e.toString());
			}
		}

	public void readFile(URL url, RuntimeProperties properties) {
		try {
			mDataReader = new InputStreamReader(url.openStream());
			}
		catch (IOException e) {
			mTableModel.unlock();
			JOptionPane.showMessageDialog(mParentFrame, "IO-Exception during file retrieval.");
			return;
			}
		mDataType = FileHelper.cFileTypeDataWarrior;
		mAction = READ_DATA | REPLACE_DATA;
		mWithHeaderLine = true;
		mNewWindowTitle = url.toString();
		mRuntimeProperties = properties;
		processData();
		}

	public void readFile(File file, RuntimeProperties properties) {
		readFile(file, properties, FileHelper.cFileTypeDataWarrior, READ_DATA | REPLACE_DATA);
		}

	public void readTemplate(File file, RuntimeProperties properties) {
		readFile(file, properties, FileHelper.cFileTypeDataWarriorTemplate, READ_DATA | APPLY_TEMPLATE);
		}

	public void readFile(File file, RuntimeProperties properties, int dataType) {
		readFile(file, properties, dataType, READ_DATA | REPLACE_DATA);
		}

	public void readFile(File file, RuntimeProperties properties, int dataType, int action) {
		mFile = file;
		try {
			mDataReader = new InputStreamReader(new FileInputStream(mFile), "UTF-8");
			}
		catch (FileNotFoundException e) {
			mTableModel.unlock();
			JOptionPane.showMessageDialog(mParentFrame, "File not found.");
			return;
			}
		catch (UnsupportedEncodingException e) {
			mTableModel.unlock();
			JOptionPane.showMessageDialog(mParentFrame, "Unsupported encoding.");
			return;
			}
		mDataType = dataType;
		mAction = action;
		mWithHeaderLine = true;
		mNewWindowTitle = mFile.getName();
		mRuntimeProperties = properties;
		processData();
		}

	public String[] getFieldNames() {
		while (mThread != null)
			try { Thread.sleep(100); } catch (InterruptedException e) {}

		return mFieldNames;
		}

	public void appendFile(int[] destColumn, int datasetColumn, String existingSetName, String newSetName) {
		mAction = APPEND_DATA;
		mAppendDestColumn = destColumn;
		mAppendDatasetColumn = datasetColumn;
		mAppendDatasetNameExisting = existingSetName;
		mAppendDatasetNameNew = newSetName;
		processData();
		}

	/**
	 * Checks whether the columns defined in mergeMode as keys have content that uniquely
	 * identifies every row.
	 * @param mergeMode matching the total previously read column count with at least one MERGE_MODE_IS_KEY entry
	 * @param pl null or progress listener to receive messages
	 * @return true if all 
	 */
	public boolean areMergeKeysUnique(String[] keyColumnName, ProgressListener pl) {
		if (pl != null)
			pl.startProgress("Sorting new keys...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		int[] keyColumn = new int[keyColumnName.length];
		for (int i=0; i<keyColumnName.length; i++) {
			for (int j=0; j<mFieldNames.length; j++) {
				if (mFieldNames[j].equals(keyColumnName[i])) {
					keyColumn[i] = j;
					break;
					}
				}
			}

		TreeSet<byte[]> newKeySet = new TreeSet<byte[]>(new ByteArrayComparator());
		for (int row=0; row<mFieldData.length; row++) {
			if (pl != null && mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				pl.updateProgress(row);

			byte[] key = constructMergeKey((Object[])mFieldData[row], keyColumn);
			if (key != null) {
				if (newKeySet.contains(key))
					return false;

				newKeySet.add(key);
				}
			}
		return true;
		}

	/**
	 * Combines all individual byte arrays from all key columns
	 * separated by TAB codes. If none of the columns contain any data, then null is returned.
	 * @param rowData
	 * @param keyColumn
	 * @return null or row key as byte array
	 */
	private byte[] constructMergeKey(Object[] rowData, int[] keyColumn) {
		int count = keyColumn.length - 1;	// TABs needed
		for (int i=0; i<keyColumn.length; i++) {
			byte[] data = (byte[])rowData[keyColumn[i]];
			if (data != null)
				count += data.length;
			}
		if (count == keyColumn.length - 1)
			return null;

		byte[] key = new byte[count];
		int index = 0;
		for (int i=0; i<keyColumn.length; i++) {
			if (i != 0)
				key[index++] = '\t';
			byte[] data = (byte[])rowData[keyColumn[i]];
			if (data != null)
				for (byte b:data)
					key[index++] = b;
			}

		return key;
		}

	/**
	 * Merges previously read file content into the associated table model.
	 * Prior to this method either paste() or one of the readFile() methods
	 * must have been called. Then areMergeKeysUnique() must have been called
	 * and must have returned true.
	 * @param destColumn
	 * @param mergeMode
	 * @param appendRest
	 */
	public void mergeFile(int[] destColumn, int[] mergeMode, boolean appendRest) {
		mAction = MERGE_DATA;
		mMergeDestColumn = destColumn;
		mMergeMode = mergeMode;
		mAppendRest = appendRest;
		processData();
		}

	private void processData() {
		if (SwingUtilities.isEventDispatchThread()) {
			if (mProgressController == null) {
				mProgressController = new JProgressDialog(mParentFrame);
				mOwnsProgressController = true;
				}
	
			mThread = new Thread(this, "CompoundTableLoader");
			mThread.setPriority(Thread.MIN_PRIORITY);
			mThread.start();

			if (mOwnsProgressController)
				((JProgressDialog)mProgressController).setVisible(true);
			}
		else {
			run();
			}
		}

	private boolean analyzeHeaderLine(Reader reader) throws Exception {
		BufferedReader theReader = new BufferedReader(reader);

		ArrayList<String> lineList = new ArrayList<String>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				lineList.add(theLine);
				}
			}
		catch (IOException e) {}
		theReader.close();

		if (lineList.size() < 2)
			return false;

		char columnSeparator = (mDataType == FileHelper.cFileTypeTextCommaSeparated) ? ',' : '\t';
		ArrayList<String> columnNameList = new ArrayList<String>();
		String header = lineList.get(0);
		int fromIndex = 0;
		int toIndex;
		do {
			String columnName;
			toIndex = header.indexOf(columnSeparator, fromIndex);

			if (toIndex == -1) {
				columnName = header.substring(fromIndex);
				}
			else {
				columnName = header.substring(fromIndex, toIndex);
				fromIndex = toIndex+1;
				}

			if (sIdentifierHandler.isIdentifierName(columnName)
			 || columnName.equalsIgnoreCase("Substance Name")
			 || columnName.equalsIgnoreCase("smiles")
			 || columnName.equalsIgnoreCase("idcode")
			 || columnName.endsWith("[idcode]")
			 || columnName.endsWith("[rxncode]")
			 || columnName.startsWith("fingerprint"))
				return true;

			columnNameList.add(columnName);
			} while (toIndex != -1);

		boolean[] isNotNumerical = new boolean[columnNameList.size()];
		for (int row=1; row<lineList.size(); row++) {
			String theLine = lineList.get(row);
			fromIndex = 0;
			int sourceColumn = 0;
			do {
				String value;
				toIndex = theLine.indexOf(columnSeparator, fromIndex);
				if (toIndex == -1) {
					value = theLine.substring(fromIndex);
					}
				else {
					value = theLine.substring(fromIndex, toIndex);
					fromIndex = toIndex+1;
					}

				if (!isNotNumerical[sourceColumn] && value.length() != 0) {
					try {
						Double.parseDouble(value);
						}
					catch (NumberFormatException e) {
						isNotNumerical[sourceColumn] = true;
						}
					}

				sourceColumn++;
				} while (sourceColumn<columnNameList.size() && toIndex != -1);
			}

		for (int column=0; column<columnNameList.size(); column++) {
			if (!isNotNumerical[column]) {
				try {
					Double.parseDouble(columnNameList.get(column));
					}
				catch (NumberFormatException e) {
					return true;
					}
				}
			}

		return false;
		}

	private boolean readTemplateOnly() {
		mProgressController.startProgress("Reading Template...", 0, 0);

		BufferedReader theReader = new BufferedReader(mDataReader);

		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				if (theLine.equals(cPropertiesStart)) {
					mRuntimeProperties.read(theReader);
					break;
					}
				}
			theReader.close();
			}
		catch (IOException e) {}

		return true;
		}

	private boolean readTextData() {
		mProgressController.startProgress("Reading Data...", 0, 0);

		BufferedReader theReader = new BufferedReader(mDataReader);

		String header = null;
		mVersion = null;
		int rowCount = -1;
		CompoundTableExtensionHandler extensionHandler = mTableModel.getExtensionHandler();
		ArrayList<byte[]> lineList = new ArrayList<byte[]>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				if (theLine.equals(cNativeFileHeaderStart)) {
					rowCount = readFileHeader(theReader);
					if (rowCount > PROGRESS_LIMIT)
						mProgressController.startProgress("Reading Data...", 0, (rowCount > PROGRESS_LIMIT) ? rowCount : 0);
					continue;
					}

				if (extensionHandler != null) {
					String name = extensionHandler.extractExtensionName(theLine);
					if (name != null) {
						if (mExtensionMap == null)
							mExtensionMap = new TreeMap<String,Object>();
						mExtensionMap.put(name, extensionHandler.readData(name, theReader));
						continue;
						}
					}

				if (theLine.equals(cColumnPropertyStart)) {
					readColumnProperties(theReader);
					continue;
					}

				if (theLine.equals(cHitlistDataStart)) {
					readHitlistData(theReader);
					continue;
					}

				if (theLine.equals(cDetailDataStart)) {
					readDetailData(theReader);
					continue;
					}

				if (theLine.equals(cPropertiesStart)) {
					if ((mAction & APPEND_DATA) == 0
					 && (mAction & MERGE_DATA) == 0
					 && mRuntimeProperties != null)
						mRuntimeProperties.read(theReader);

					break;
					}

				if (mDataType != FileHelper.cFileTypeDataWarriorTemplate) {
					if (mWithHeaderLine && header == null) {
						header = theLine;
						}
					else if (theLine.length() != 0) {
						lineList.add(theLine.getBytes());
						if (rowCount > PROGRESS_LIMIT && lineList.size()%PROGRESS_STEP == 0)
							mProgressController.updateProgress(lineList.size());
						}
					}
				}
			theReader.close();
			}
		catch (IOException e) {}

		if (mWithHeaderLine && header == null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, "No header line found.");
					}
				} );
			return false;
			}

		if (mDataType != FileHelper.cFileTypeDataWarriorTemplate)
			processLines(header, lineList);

		return true;
		}

	private void processLines(String header, ArrayList<byte[]> lineList) {
		ArrayList<String> columnNameList = new ArrayList<String>();
		byte columnSeparator = (mDataType == FileHelper.cFileTypeTextCommaSeparated) ? (byte)',' : (byte)'\t';

		if (mWithHeaderLine) {
			int fromIndex = 0;
			int toIndex = 0;
			do {
				String columnName;
				toIndex = header.indexOf(columnSeparator, fromIndex);
				if (toIndex == -1) {
					columnName = header.substring(fromIndex);
					}
				else {
					columnName = header.substring(fromIndex, toIndex);
					fromIndex = toIndex+1;
					}

				String[] type = cParentSpecialColumnTypes;
				for (int i=0; i<type.length; i++) {
					if (columnName.endsWith("["+type[i]+"]")) {
						columnName = columnName.substring(0, columnName.length()-type[i].length()-2);
						if (mColumnProperties == null)
							mColumnProperties = new TreeMap<String,String>();
						mColumnProperties.put(columnName+"\t"+cColumnPropertySpecialType, type[i]);
						}
					}

				columnNameList.add(sIdentifierHandler.normalizeIdentifierName(columnName));
				} while (toIndex != -1);
			}

		if (mVersion == null)
			createColumnPropertiesForFilesPriorVersion270(columnNameList);

		if (!mWithHeaderLine && lineList.size() > 0) {
			byte[] lineBytes = lineList.get(0);
			columnNameList.add("Column 1");
			int no = 2;
			for (byte b:lineBytes)
				if (b == columnSeparator)
					columnNameList.add("Column "+no++);
			}

		int columnCount = columnNameList.size();

		mFieldNames = new String[columnCount];
		mFieldData = new Object[lineList.size()][columnCount];

		for (int column=0; column<columnCount; column++)
			mFieldNames[column] = columnNameList.get(column);

		boolean[] descriptorValid = new boolean[columnCount];
		DescriptorHandler<?,?>[] descriptorHandler = new DescriptorHandler[columnCount];
		for (int column=0; column<columnCount; column++) {
			descriptorHandler[column] = CompoundTableModel.getDefaultDescriptorHandler(getColumnSpecialType(mFieldNames[column]));
			descriptorValid[column] = descriptorHandler[column] != null
					&& descriptorHandler[column].getVersion().equals(
							mColumnProperties.get(mFieldNames[column] + "\t" + cColumnPropertyDescriptorVersion));
			}

		mProgressController.startProgress("Processing Records...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		for (int row=0; row<mFieldData.length; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);
			byte[] lineBytes = lineList.get(row);
			lineList.set(row, null);
			int fromIndex = 0;
			int column = 0;
			do {
				int toIndex = fromIndex;
				int spaces = 0;

				boolean isQuotedString = false;
				if (mDataType == FileHelper.cFileTypeTextCommaSeparated) {
					while (fromIndex<lineBytes.length && lineBytes[fromIndex] == (byte)' ')
						fromIndex++;

					int index2 = -1;
					if (lineBytes[fromIndex] == (byte)'\"') {
						index2 = fromIndex + 1;
						while (index2<lineBytes.length) {
							if (index2+1<lineBytes.length
							 && lineBytes[index2+1] == (byte)'\"'
							 && (lineBytes[index2] == (byte)'"' || lineBytes[index2] == (byte)'\\')) {
								index2 += 2;
								continue;
								}
							if (lineBytes[index2] == (byte)'\"') {
								int index3 = index2 + 1;
								while (index3<lineBytes.length
									&& lineBytes[index3] == (byte)' ') {
									index3++;
									spaces++;
									}
								if (index3 == lineBytes.length
								 || lineBytes[index3] == columnSeparator) {
									isQuotedString = true;
									}
								break;
								}
							index2++;
							}
						}
					if (isQuotedString) {
						fromIndex++;
						toIndex = index2;
						}
					else {
						toIndex = fromIndex;
						while (toIndex<lineBytes.length && lineBytes[toIndex] != columnSeparator)
							toIndex++;
						while (toIndex>fromIndex && lineBytes[toIndex-1] == (byte)' ') {
							toIndex--;
							spaces++;
							}
						}
					}
				else {
					while (toIndex<lineBytes.length && lineBytes[toIndex] != columnSeparator)
						toIndex++;
					}

				if (toIndex == fromIndex) {
					mFieldData[row][column] = null;
					}
				else {
					byte[] cellBytes = Arrays.copyOfRange(lineBytes, fromIndex, toIndex);
	
					if (mDataType == FileHelper.cFileTypeTextCommaSeparated) {
						if (isQuotedString)
							mFieldData[row][column] = convertDoubleQuotes(cellBytes);
						else
							mFieldData[row][column] = cellBytes;
						}
					else {
						if (descriptorHandler[column] == null)
							mFieldData[row][column] = convertNLAndTAB(cellBytes);
						else if (descriptorValid[column])
							mFieldData[row][column] = descriptorHandler[column].decode(cellBytes);
						}
					}

				fromIndex = toIndex + (isQuotedString ? 2 : 1) + spaces;
				column++;
				} while (fromIndex<lineBytes.length && column<columnCount);
			}

		if (!mWithHeaderLine)
			deduceColumnTitles();

		if (mDataType != FileHelper.cFileTypeDataWarrior)
			handleSmiles();

		addDefaultLookupColumnProperties();

		if (mVersion == null) // a version entry exists since V3.0
			handlePotentially3DCoordinates();
		}

	private void handleSmiles() {
		StereoMolecule mol = new StereoMolecule();
		int columns = Math.min(MAX_COLUMNS_FOR_SMILES_CHECK, mFieldNames.length);
		for (int column=columns-1; column>=0; column--) {
//			if (mFieldNames[column].toLowerCase().contains("smiles")) {
				if (checkForSmiles(mol, column)) {
					insertStructureColumnFromSmiles(mol, column);
					}
//				}
			}
		}

	private void insertStructureColumnFromSmiles(StereoMolecule mol, int smilesColumn) {
		int columnCount = mFieldNames.length;
		String structureColumnName = "Structure of "+mFieldNames[smilesColumn];

		String[] newFieldNames = new String[columnCount+1];
		for (int i=0; i<columnCount; i++) {
			newFieldNames[i < smilesColumn ? i : i+1] = mFieldNames[i];
			}
		newFieldNames[smilesColumn] = structureColumnName;
		mFieldNames = newFieldNames;

		for (int row=0; row<mFieldData.length; row++) {
			Object[] newFieldData = new Object[columnCount+1];
			for (int i=0; i<columnCount; i++)
				newFieldData[i < smilesColumn ? i : i+1] = mFieldData[row][i];
			if (isValidSmiles(mol, (byte[])mFieldData[row][smilesColumn]))
				newFieldData[smilesColumn] = getIDCodeFromMolecule(mol);
			mFieldData[row] = newFieldData;
			}

		if (mColumnProperties == null)
			mColumnProperties = new TreeMap<String,String>();

		mColumnProperties.put(structureColumnName+"\t"+cColumnPropertySpecialType, cColumnTypeIDCode);
		}

	/**
	 * Checks whether a column contains valid SMILES codes, which is considered true if<br>
	 * - the first MAX_ROWS_FOR_SMILES_CHECK non-null entries in the column are valid SMILES<br>
	 * - or (if the column contains less than MAX_ROWS_FOR_SMILES_CHECK rows) every row contains a valid SMILES
	 * @param column
	 * @return
	 */
	private boolean checkForSmiles(StereoMolecule mol, int column) {
		int found = 0;
		int failures = 0;
		for (int row=0; row<mFieldData.length; row++) {
			byte[] data = (byte[])mFieldData[row][column];
			if (data != null && data.length > 3) {
				if (!isValidSmiles(mol, data)) {
					failures++;
					if (failures > MAX_TOLERATED_SMILES_FAILURES)
						return false;
					}
				else {
					found++;
					if (found == MAX_ROWS_FOR_SMILES_CHECK)
						return true;
					}
				}
			}
		return (mFieldData.length != 0 && found == mFieldData.length);
		}

	private boolean isValidSmiles(StereoMolecule mol, byte[] smiles) {
		if (smiles != null && smiles.length != 0) {
			try {
				new SmilesParser().parse(mol, smiles);
				return mol.getAllAtoms() != 0;
				}
			catch (Exception e) {}
			}
		return false;
		}

	private byte[] getIDCodeFromMolecule(StereoMolecule mol) {
		try {
			mol.normalizeAmbiguousBonds();
			mol.canonizeCharge(true);
			Canonizer canonizer = new Canonizer(mol);
			canonizer.setSingleUnknownAsRacemicParity();
			return canonizer.getIDCode().getBytes();
			}
		catch (Exception e) {}

		return null;
		}

	private void deduceColumnTitles() {
		if (mFieldData.length == 0)
			return;

		for (int column=0; column<mFieldNames.length; column++) {
			int firstRow = 0;
			while (firstRow<mFieldData.length && mFieldData[firstRow][column] == null)
				firstRow++;
			if (firstRow == mFieldData.length || !(mFieldData[firstRow][column] instanceof byte[]))
				continue;

			boolean isSubstanceID = sIdentifierHandler.isValidSubstanceIdentifier(new String((byte[])mFieldData[firstRow][column]).trim());
			for (int row=firstRow+1; isSubstanceID && row<mFieldData.length; row++)
				if (mFieldData[row][column] != null)
					isSubstanceID = sIdentifierHandler.isValidSubstanceIdentifier(new String((byte[])mFieldData[row][column]).trim());

			if (isSubstanceID) {
				mFieldNames[column] = sIdentifierHandler.getSubstanceIdentifierName();
				continue;
				}

			boolean isBatchID = sIdentifierHandler.isValidBatchIdentifier(new String((byte[])mFieldData[firstRow][column]).trim());
			for (int row=firstRow+1; isBatchID && row<mFieldData.length; row++)
				if (mFieldData[row][column] != null)
					isBatchID = sIdentifierHandler.isValidBatchIdentifier(new String((byte[])mFieldData[row][column]).trim());

			if (isBatchID) {
				mFieldNames[column] = sIdentifierHandler.getBatchIdentifierName();
				continue;
				}
			}
		}

	private void addDefaultLookupColumnProperties() {
		if (sIdentifierHandler != null)
			for (int column=0; column<mFieldNames.length; column++)
				mColumnProperties = sIdentifierHandler.addDefaultColumnProperties(mFieldNames[column], mColumnProperties);
		}

	private void createColumnPropertiesForFilesPriorVersion270(ArrayList<String> columnNameList) {
			// Native DataWarrior files before V2.7.0 didn't have column properties
			// for column headers 'idcode','idcoordinates','fingerprint_Vxxx'.
			// There types were recognized by the column header only.
		mOldVersionIDCodeColumn = -1;
		mOldVersionCoordinateColumn = -1;
		for (int i=0; i<columnNameList.size(); i++) {
			String columnName = columnNameList.get(i);
			if (columnName.equals("idcode") && !columnHasProperty(columnName)) {
				columnNameList.set(i, "Structure");
				if (mColumnProperties == null)
					mColumnProperties = new TreeMap<String,String>();
				mColumnProperties.put("Structure\t"+cColumnPropertySpecialType, cColumnTypeIDCode);
				for (int j=0; j<columnNameList.size(); j++) {
					if (columnName.equals(sIdentifierHandler.getSubstanceIdentifierName())) {
						mColumnProperties.put("Structure\t"+cColumnPropertyIdentifierColumn, sIdentifierHandler.getSubstanceIdentifierName());
						break;
						}
					}
				mOldVersionIDCodeColumn = i;
				}
			if (mOldVersionIDCodeColumn != -1
			 && (columnName.equals("idcoordinates") || columnName.equals("idcoords"))
			 && !columnHasProperty(columnName)) {
				columnNameList.set(i, cColumnType2DCoordinates);
				if (mColumnProperties == null)
					mColumnProperties = new TreeMap<String,String>();
				mColumnProperties.put("cColumnType2DCoordinates\t"+cColumnPropertySpecialType, cColumnType2DCoordinates);
				mColumnProperties.put("cColumnType2DCoordinates\t"+cColumnPropertyParentColumn, "Structure");
				mOldVersionCoordinateColumn = i;
				}
			if (mOldVersionIDCodeColumn != -1
			 && columnName.startsWith("fingerprint_")
			 && !columnHasProperty(columnName)) {
				columnNameList.set(i, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				if (mColumnProperties == null)
					mColumnProperties = new TreeMap<String,String>();
				mColumnProperties.put("DescriptorConstants.DESCRIPTOR_FFP512.shortName\t"+cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				mColumnProperties.put("DescriptorConstants.DESCRIPTOR_FFP512.shortName\t"+cColumnPropertyParentColumn, "Structure");
				mColumnProperties.put("DescriptorConstants.DESCRIPTOR_FFP512.shortName\t"+cColumnPropertyDescriptorVersion, columnName.substring(12));
				}
			}
		if (mOldVersionCoordinateColumn != -1)
			mCoordsMayBe3D = true;
		}

	private boolean columnHasProperty(String columnName) {
		if (mColumnProperties != null) {
			for (String key:mColumnProperties.keySet())
				if (key.startsWith(columnName+"\t"))
					return true;
			}
		return false;
		}

	private int readFileHeader(BufferedReader theReader) {
		int rowCount = -1;
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cNativeFileHeaderEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cNativeFileVersion)) {
					mVersion = extractValue(theLine);
					continue;
					}

				if (theLine.startsWith("<"+cNativeFileRowCount)) {
					try {
						rowCount = Integer.parseInt(extractValue(theLine));
						}
					catch (NumberFormatException nfe) {}
					continue;
					}
				}
			}
		catch (Exception e) {}
		return rowCount;
		}

	private void readColumnProperties(BufferedReader theReader) {
		mColumnProperties = new TreeMap<String,String>();
		try {
			String columnName = null;
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cColumnPropertyEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cColumnName)) {
					columnName = extractValue(theLine);
					continue;
					}

				if (theLine.startsWith("<"+cColumnProperty)) {
					String keyAndValue = extractValue(theLine);

					// to support deprecated property cColumnPropertyIsIDCode => "isIDCode"
					if (keyAndValue.equals("isIDCode\ttrue")) {
						mColumnProperties.put(columnName + "\t" + cColumnPropertySpecialType, cColumnTypeIDCode);
						}
					else {
						int index = keyAndValue.indexOf('\t');
						if (index != -1)
							mColumnProperties.put(columnName + "\t" + keyAndValue.substring(0, index), keyAndValue.substring(index+1));
						}

					continue;
					}
				}
			}
		catch (Exception e) {
			mColumnProperties = null;
			}
		}

	public String getParentColumnName(String columnName) {
		return mColumnProperties == null ? null : mColumnProperties.get(columnName + "\t" + cColumnPropertyParentColumn);
		}

	public String getColumnSpecialType(String columnName) {
		return mColumnProperties == null ? null : mColumnProperties.get(columnName + "\t" + cColumnPropertySpecialType);
		}

	private void readHitlistData(BufferedReader theReader) {
		mHitlists = new ArrayList<String>();
		try {
			String hitlistName = null;
			StringBuffer hitlistData = null;
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cHitlistDataEnd)) {
					mHitlists.add(hitlistName + "\t" + hitlistData);
					break;
					}

				if (theLine.startsWith("<"+cHitlistName)) {
					if (hitlistName != null)
						mHitlists.add(hitlistName + "\t" + hitlistData);

					hitlistName = extractValue(theLine);
					hitlistData = new StringBuffer();
					continue;
					}

				if (theLine.startsWith("<"+cHitlistData)) {
					hitlistData.append(extractValue(theLine));
					continue;
					}
				}
			}
		catch (Exception e) {
			mHitlists = null;
			}
		}

	private void readDetailData(BufferedReader theReader) {
		mDetails = new HashMap<String,byte[]>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cDetailDataEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cDetailID)) {
					String detailID = extractValue(theLine);
					BinaryDecoder decoder = new BinaryDecoder(theReader);
					int size = decoder.initialize(8);
					byte[] detailData = new byte[size];
					for (int i=0; i<size; i++)
						detailData[i] = (byte)decoder.read();
					mDetails.put(detailID, detailData);
					}
				}
			}
		catch (Exception e) {
			mDetails = null;
			}
		}

	static public String extractValue(String theLine) {
		int index1 = theLine.indexOf("=\"") + 2;
		int index2 = theLine.indexOf("\"", index1);
		return theLine.substring(index1, index2);
		}

	private byte[] getBytes(String s) {
		return (s == null || s.length() == 0) ? null : s.getBytes();
		}

	private boolean readSDFile() {
		mProgressController.startProgress("Examining Records...", 0, 0);

		SDFileParser sdParser = new SDFileParser(mDataReader);
		String[] fieldNames = sdParser.getFieldNames();
		int fieldCount = fieldNames.length;

		mFieldNames = new String[fieldCount+3];
		mFieldNames[0] = "Structure";
		mFieldNames[1] = cColumnType2DCoordinates;
		mFieldNames[2] = "Molecule Name";   // use record no as default column
		for (int i=0; i<fieldCount; i++)
			mFieldNames[3+i] = sIdentifierHandler.normalizeIdentifierName(fieldNames[i]);

		ArrayList<Object[]> fieldDataList = new ArrayList<Object[]>();

		mOldVersionIDCodeColumn = 0;
		mOldVersionCoordinateColumn = 1;
		mCoordsMayBe3D = true;

		int structureIDColumn = (fieldCount != 0
	   						 && (fieldNames[0].equals("ID")
	   						  || fieldNames[0].equals("IDNUMBER")
	   						  || fieldNames[0].equals(sIdentifierHandler.getSubstanceIdentifierName())
	   						  || fieldNames[0].equals("code"))) ? 3 : -1;

		// this takes preference
		for (int i=0; i<fieldCount; i++) {
			if (fieldNames[i].equals(sIdentifierHandler.getSubstanceIdentifierName())
			 || fieldNames[i].equals("EMOL_VERSION_ID")) {
				structureIDColumn = 3 + i;
				}
			}

		sdParser = new SDFileParser(mFile, fieldNames);
		MolfileParser mfParser = new MolfileParser();
		StereoMolecule mol = new StereoMolecule();
		int recordNo = 0;
		int errors = 0;
		boolean molnameFound = false;
		boolean molnameIsDifferentFromFirstField = false;
		int recordCount = sdParser.getRowCount();

		mProgressController.startProgress("Processing Records...", 0, (recordCount != -1) ? recordCount : 0);

		while (sdParser.next()) {
			if (mProgressController.threadMustDie())
				break;
			if (recordCount != -1 && recordNo%PROGRESS_STEP == 0)
				mProgressController.updateProgress(recordNo);

			Object[] fieldData = new Object[mFieldNames.length];

			String molname = null;
			try {
				String molfile = sdParser.getNextMolFile();

				BufferedReader r = new BufferedReader(new StringReader(molfile));
				molname = r.readLine().trim();
				r.readLine();
				String comment = r.readLine();

				// exclude manually CCDC entries with atoms that are in multiple locations.
				if (comment.contains("From CSD data") && !comment.contains("No disordered atoms"))
					throw new Exception("CSD molecule with ambivalent atom location.");
					
				mfParser.parse(mol, molfile);
				if (mol.getAllAtoms() != 0) {
					mol.normalizeAmbiguousBonds();
					mol.canonizeCharge(true);
					Canonizer canonizer = new Canonizer(mol);
					canonizer.setSingleUnknownAsRacemicParity();
					byte[] idcode = getBytes(canonizer.getIDCode());
					byte[] coords = getBytes(canonizer.getEncodedCoordinates());
					fieldData[0] = idcode;
					fieldData[1] = coords;
					}
				}
			catch (Exception e) {
				errors++;
				}

			if (molname.length() != 0) {
				molnameFound = true;
				fieldData[2] = getBytes(molname);
				if (structureIDColumn != -1 && !molname.equals(removeTabs(sdParser.getFieldData(structureIDColumn - 3))))
					molnameIsDifferentFromFirstField = true;
				}

			for (int i=0; i<fieldCount; i++)
				fieldData[3+i] = getBytes(removeTabs(sdParser.getFieldData(i)));

		  /* IDCode conversion validation code
			if (mIDCode[recordNo] != null) {
				StereoMolecule testMol = new IDCodeParser().getCompactMolecule(mIDCode[recordNo], mCoordinates[recordNo]);
				Canonizer testCanonizer = new Canonizer(testMol);
				String testIDCode = testCanonizer.getIDCode();
				if (!testIDCode.equals(new String(mIDCode[recordNo]))) {
					new IDCodeParser().printContent(mIDCode[recordNo], null);
					new IDCodeParser().printContent(testIDCode.getBytes(), null);
					}
				else {
					recordNo--;
					}
				}
		   */

			fieldDataList.add(fieldData);
			recordNo++;
			}

		mColumnProperties = new TreeMap<String,String>();
		mColumnProperties.put("Structure"+"\t"+cColumnPropertySpecialType, cColumnTypeIDCode);
		mColumnProperties.put(cColumnType2DCoordinates+"\t"+cColumnPropertySpecialType, cColumnType2DCoordinates);
		mColumnProperties.put(cColumnType2DCoordinates+"\t"+cColumnPropertyParentColumn, "Structure");

		mFieldData = fieldDataList.toArray(new Object[0][]);

		if (structureIDColumn != -1) {
			mColumnProperties.put("Structure\t"+cColumnPropertyIdentifierColumn, mFieldNames[structureIDColumn]);
			}
		else if (molnameFound) {
			mColumnProperties.put("Structure\t"+cColumnPropertyIdentifierColumn, mFieldNames[2]);
			}
		else {
			mFieldNames[2] = "Structure No";
			for (int row=0; row<mFieldData.length; row++)
				mFieldData[row][2] = (""+(row+1)).getBytes();
			}

		// if the molname column is redundant, then delete it
		if (structureIDColumn != -1 && (!molnameFound || !molnameIsDifferentFromFirstField)) {
			for (int column=3; column<mFieldNames.length; column++)
				mFieldNames[column-1] = mFieldNames[column];
			mFieldNames = Arrays.copyOf(mFieldNames, mFieldNames.length-1);

			for (int row=0; row<mFieldData.length; row++) {
				for (int column=3; column<mFieldData[row].length; column++)
					mFieldData[row][column-1] = mFieldData[row][column];
				mFieldData[row] = Arrays.copyOf(mFieldData[row], mFieldData[row].length-1);
				}
			}

		if (errors > 0) {
			final String message = ""+errors+" compound structures could not be generated because of molfile parsing errors.";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}

		handlePotentially3DCoordinates();

		addDefaultLookupColumnProperties();

		return true;
		}

	private String removeTabs(String s) {
		return (s == null) ? null : s.trim().replace('\t', ' ');
		}

	/**
	 * SD-Files or native DataWarrior files before version 2.7.0 may end up with
	 * 2D- and/or 3D-coordinates in one column (cColumnType2DCoordinates).
	 * If we have a mix of 2D and 3D, we need to add a new column and separate the data.
	 * If we have 3D only, we need to change column properties accordingly.
	 */
	private void handlePotentially3DCoordinates() {
		mOldVersionCoordinate3DColumn = -1;

		if (!mCoordsMayBe3D)
			return;

		mProgressController.startProgress("Checking for 3D-coordinates...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		boolean found2D = false;
		boolean found3D = false;
		IDCodeParser parser = new IDCodeParser(false);
		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			byte[] idcode = (byte[])mFieldData[row][mOldVersionIDCodeColumn];
			byte[] coords = (byte[])mFieldData[row][mOldVersionCoordinateColumn];
			if (idcode != null && coords != null) {
				if (parser.coordinatesAre3D(idcode, coords))
					found3D = true;
				else
					found2D = true;

				if (found2D && found3D)
					break;
				}
			}

		if (!found3D)
			return;

		if (!found2D) {
			mFieldNames[mOldVersionCoordinateColumn] = cColumnType3DCoordinates;
			mColumnProperties.remove(cColumnType2DCoordinates+"\t"+cColumnPropertySpecialType);
			mColumnProperties.remove(cColumnType2DCoordinates+"\t"+cColumnPropertyParentColumn);
			mColumnProperties.put(cColumnType3DCoordinates+"\t"+cColumnPropertySpecialType, cColumnType3DCoordinates);
			mColumnProperties.put(cColumnType3DCoordinates+"\t"+cColumnPropertyParentColumn, "Structure");
			return;
			}

		mOldVersionCoordinate3DColumn = mFieldNames.length;
		mFieldNames = Arrays.copyOf(mFieldNames, mOldVersionCoordinate3DColumn+1);
		mFieldNames[mOldVersionCoordinate3DColumn] = cColumnType3DCoordinates;

		mProgressController.startProgress("Separating 2D- from 3D-coordinates...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			mFieldData[row] = Arrays.copyOf(mFieldData[row], mOldVersionCoordinate3DColumn+1);
			byte[] idcode = (byte[])mFieldData[row][mOldVersionIDCodeColumn];
			byte[] coords = (byte[])mFieldData[row][mOldVersionCoordinateColumn];
			if (idcode != null && coords != null) {
				if (parser.coordinatesAre3D(idcode, coords)) {
					mFieldData[row][mOldVersionCoordinate3DColumn] = mFieldData[row][mOldVersionCoordinateColumn];
					mFieldData[row][mOldVersionCoordinateColumn] = null;
					}
				}
			}
		mColumnProperties.put(cColumnType3DCoordinates+"\t"+cColumnPropertySpecialType, cColumnType3DCoordinates);
		mColumnProperties.put(cColumnType3DCoordinates+"\t"+cColumnPropertyParentColumn, "Structure");
		}

	private int populateTable() {
		mTableModel.initializeTable(mFieldData.length, mFieldNames.length);

		if (mExtensionMap != null)
			for (String name:mExtensionMap.keySet())
				mTableModel.setExtensionData(name, mExtensionMap.get(name));

		for (int column=0; column<mFieldNames.length; column++)
			mTableModel.setColumnName(mFieldNames[column], column);

		int rowCount = mFieldData.length;

		mProgressController.startProgress("Populating Table...", 0, (rowCount > PROGRESS_LIMIT) ? rowCount : 0);

		for (int row=0; row<rowCount; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (rowCount > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			for (int column=0; column<mFieldNames.length; column++)
				mTableModel.setTotalDataAt(mFieldData[row][column], row, column);
			}

		setColumnProperties(null);

		clearBufferedData();

		if (mDataType == FileHelper.cFileTypeDataWarrior)
			mTableModel.setFile(mFile);

		return rowCount;
		}

	private byte[] convertNLAndTAB(byte[] cellBytes) {
		return replaceSpecialSigns(replaceSpecialSigns(cellBytes, NEWLINE_BYTES, NEWLINE), TAB_BYTES, TAB);
		}

	private byte[] convertDoubleQuotes(byte[] cellBytes) {
		return replaceSpecialSigns(replaceSpecialSigns(cellBytes, QUOTES1_BYTES, (byte)'\"'), QUOTES2_BYTES, (byte)'\"');
		}

	private byte[] replaceSpecialSigns(byte[] cellBytes, byte[] what, byte with) {
		int index = 0;
		for (int i=0; i<cellBytes.length; i++) {
			boolean found = false;
			if (i <= cellBytes.length-what.length) {
				found = true;
				for (int j=0; j<what.length; j++) {
					if (cellBytes[i+j] != what[j]) {
						found = false;
						break;
						}
					}
				}
			if (found) {
				cellBytes[index++] = with;
				i += what.length-1;
				}
			else {
				cellBytes[index++] = cellBytes[i];
				}
			}

		if (index == cellBytes.length)
			return cellBytes;

		byte[] newBytes = new byte[index];
		for (int i=0; i<index; i++)
			newBytes[i] = cellBytes[i];

		return newBytes;
		}

	public void run() {
		try {
			boolean error = false;
			if ((mAction & READ_DATA) != 0)
				error = !readData();
	
			if ((mAction & REPLACE_DATA) != 0 && !error && !mProgressController.threadMustDie())
				replaceTable();
	
			if ((mAction & APPEND_DATA) != 0 && !error && !mProgressController.threadMustDie())
				appendTable();
	
			if ((mAction & MERGE_DATA) != 0 && !error && !mProgressController.threadMustDie())
				error = mergeTable();
	
			if (mOwnsProgressController) {
				((JProgressDialog)mProgressController).stopProgress();
				((JProgressDialog)mProgressController).close(mParentFrame);
				}
	
			if ((mAction & (REPLACE_DATA | APPEND_DATA | MERGE_DATA | APPLY_TEMPLATE)) != 0
			 && mRuntimeProperties != null
			 && !error)
				mRuntimeProperties.apply();
	
			finalStatus(!error);
			}
		catch (Throwable t) {
			t.printStackTrace();
			}

		mThread = null;
		}

	private boolean readData() {
			// returns true if successful
		clearBufferedData();

		try {
			switch (mDataType) {
			case FileHelper.cFileTypeDataWarriorTemplate:
				return readTemplateOnly();
			case FileHelper.cFileTypeDataWarrior:
			case FileHelper.cFileTypeTextTabDelimited:
			case FileHelper.cFileTypeTextCommaSeparated:
				return readTextData();
			case FileHelper.cFileTypeSD:
				return readSDFile();
				}
			}
		catch (OutOfMemoryError err) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, "Out of memory. Launch this application with Java option -Xms???m or -Xmx???m.");
					}
				} );
			clearBufferedData();
			return false;
			}
		return false;
		}

	private void replaceTable() {
		if (mDataType != FileHelper.cFileTypeDataWarriorTemplate) {
			mFirstNewColumn = 0;
			int rowCount = populateTable();

			if (mProgressController.threadMustDie()) {
				mTableModel.initializeTable(0, 0);
				if (mParentFrame != null)
					mParentFrame.setTitle("no data");
				}
			else {
				if (mParentFrame != null)
					mParentFrame.setTitle(mNewWindowTitle);

				mTableModel.finalizeTable(mRuntimeProperties != null
						   && mRuntimeProperties.size() != 0 ?
								   CompoundTableEvent.cSpecifierNoRuntimeProperties
								 : CompoundTableEvent.cSpecifierDefaultRuntimeProperties,
										  mProgressController);

				populateHitlists(rowCount, 0, null);
				populateDetails();
				}
			}
		}

	private void appendTable() {
		resolveDetailIDCollisions();

		mFirstNewColumn = mTableModel.getTotalColumnCount();
		int newDatasetNameColumns = (mAppendDatasetColumn == NEW_COLUMN) ? 1 : 0;
		int newColumns = newDatasetNameColumns;
		for (int i=0; i<mAppendDestColumn.length; i++)
			if (mAppendDestColumn[i] == NEW_COLUMN)
				newColumns++;

		if (newColumns != 0) {
			String[] columnName = new String[newColumns];
			if (newDatasetNameColumns != 0)
				columnName[0] = DATASET_COLUMN_TITLE;
			newColumns = newDatasetNameColumns;
			for (int i=0; i<mAppendDestColumn.length; i++)
				if (mAppendDestColumn[i] == NEW_COLUMN)
					columnName[newColumns++] = mFieldNames[i];

			int destinationColumn = mTableModel.addNewColumns(columnName);
			
			if (newDatasetNameColumns != 0) {
				mAppendDatasetColumn = destinationColumn++;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++)
					mTableModel.setTotalValueAt(mAppendDatasetNameExisting, row, mAppendDatasetColumn);
				}

			for (int i=0; i<mAppendDestColumn.length; i++)
				if (mAppendDestColumn[i] == NEW_COLUMN)
					mAppendDestColumn[i] = destinationColumn++;

			setColumnProperties(mAppendDestColumn);

			mTableModel.finalizeNewColumns(mFirstNewColumn, mProgressController);
			}

		if (mRuntimeProperties != null) // do this after finalizeNewColumns()
			mRuntimeProperties.learn(); // to also copy the new dataset filter

		int existingRowCount = mTableModel.getTotalRowCount();
		int additionalRowCount = mFieldData.length;
		mTableModel.addNewRows(additionalRowCount);

		mProgressController.startProgress("Appending rows...", 0, additionalRowCount);

		for (int row=0; row<additionalRowCount; row++) {
			int newRow = existingRowCount + row;

			if (mAppendDatasetColumn != NO_COLUMN)
				mTableModel.setTotalValueAt(mAppendDatasetNameNew, newRow, mAppendDatasetColumn);
			for (int column=0; column<mFieldNames.length; column++)
				if (mAppendDestColumn[column] != NO_COLUMN)
					mTableModel.setTotalDataAt(mFieldData[row][column], newRow, mAppendDestColumn[column]);

			mProgressController.updateProgress(row);
			}

		clearBufferedData();

		mTableModel.finalizeNewRows(existingRowCount, mProgressController);

		populateHitlists(additionalRowCount, existingRowCount, null);
		populateDetails();
		}

	private boolean mergeTable() {
		mProgressController.startProgress("Sorting current keys...", 0, mTableModel.getTotalRowCount());

		TreeMap<String,int[]> currentKeyMap = new TreeMap<String,int[]>();

		// construct key column array from mMergeMode
		int keyColumns = 0;
		for (int i=0; i<mMergeMode.length; i++)
			if (mMergeMode[i] == MERGE_MODE_IS_KEY)
				keyColumns++;
		int[] keyColumn = new int[keyColumns];
		keyColumns = 0;
		for (int i=0; i<mMergeMode.length; i++)
			if (mMergeMode[i] == MERGE_MODE_IS_KEY)
				keyColumn[keyColumns++] = i;

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mProgressController.threadMustDie())
				break;
			if (row % PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			// create combined key from all key columns
			String key = mTableModel.getTotalValueAt(row, mMergeDestColumn[keyColumn[0]]);
			for (int i=1; i<keyColumns; i++)
				key = key.concat("\t").concat(mTableModel.getTotalValueAt(row, mMergeDestColumn[keyColumn[i]]));

			if (key != null && key.length() != 0) {
				int[] rowList = currentKeyMap.get(key);
				if (rowList == null) {
					rowList = new int[1];
					rowList[0] = row;
					}
				else {
					int[] oldRowList = rowList;
					rowList = new int[oldRowList.length+1];
					int i=0;
					for (int oldRow:oldRowList)
						rowList[i++] = oldRow;
					rowList[i] = row;
					}
				currentKeyMap.put(key, rowList);
				}
			}

		if (mProgressController.threadMustDie()) {
			resolveDetailIDCollisions();
			return true;
			}

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		int[][] destRowMap = null;
		if (mHitlists != null)
			destRowMap = new int[mFieldData.length][];

		if (mRuntimeProperties != null)
			mRuntimeProperties.learn();

		int newColumns = 0;
		for (int i=0; i<mMergeDestColumn.length; i++)
			if (mMergeDestColumn[i] == NEW_COLUMN)
				newColumns++;

		mFirstNewColumn = mTableModel.getTotalColumnCount();
		if (newColumns != 0) {
			mProgressController.startProgress("Merging data...", 0, mFieldData.length);

			String[] columnName = new String[newColumns];
			newColumns = 0;
			for (int i=0; i<mMergeDestColumn.length; i++)
				if (mMergeDestColumn[i] == NEW_COLUMN)
					columnName[newColumns++] = mFieldNames[i];

			int destinationColumn = mTableModel.addNewColumns(columnName);
			for (int i=0; i<mMergeDestColumn.length; i++)
				if (mMergeDestColumn[i] == NEW_COLUMN)
					mMergeDestColumn[i] = destinationColumn++;
			}

		int mergedColumns = 0;
		for (int row=0; row<mFieldData.length; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (row % PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			byte[] key = constructMergeKey(mFieldData[row], keyColumn);
			if (key != null) {
				int[] rowList = currentKeyMap.get(new String(key));
				if (rowList != null) {
					for (int destRow:rowList) {
						// In case we have child columns with merge mode MERGE_MODE_AS_PARENT, we need to handle them first.
						for (int column=0; column<mMergeDestColumn.length; column++) {
							if (mMergeDestColumn[column] != NO_COLUMN) {
								if (mMergeMode[column] == MERGE_MODE_AS_PARENT) {
									int parentColumn = getSourceColumn(getParentColumnName(mFieldNames[column]));
									if (mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[parentColumn]) == null)
										mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);
									}
								}
							}
						for (int column=0; column<mMergeDestColumn.length; column++) {
							if (mMergeDestColumn[column] != NO_COLUMN) {
								switch (mMergeMode[column]) {
								case MERGE_MODE_APPEND:
									mTableModel.appendTotalDataAt((byte[])mFieldData[row][column], destRow, mMergeDestColumn[column]);
									break;
								case MERGE_MODE_REPLACE:
									mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);
									break;
								case MERGE_MODE_USE_IF_EMPTY:
									if (mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[column]) == null)
										mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);
									break;
								default:	// merge key don't require handling and child column merging was handled before
									break;
									}
								}
							}
						}

					if (destRowMap != null)
						destRowMap[row] = rowList;
	
					mFieldData[row] = null;
					mergedColumns++;
					}
				}
			}

		if (newColumns != 0) {
			setColumnProperties(mMergeDestColumn);

			mTableModel.finalizeNewColumns(mFirstNewColumn, mProgressController);
			}

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		final int existingRowCount = mTableModel.getTotalRowCount();
		final int additionalRowCount = mFieldData.length - mergedColumns;
		if (mAppendRest && additionalRowCount > 0) {

			mTableModel.addNewRows(additionalRowCount);

			int destRow = existingRowCount;

			mProgressController.startProgress("Appending remaining...", 0, additionalRowCount);

			for (int row=0; row<mFieldData.length; row++) {
				if (mFieldData[row] == null)
					continue;

				if (mProgressController.threadMustDie())
					break;

				for (int column=0; column<mMergeDestColumn.length; column++)
					if (mMergeDestColumn[column] != NO_COLUMN)
						mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);

				if (destRowMap != null) {
					destRowMap[row] = new int[1];
					destRowMap[row][0] = destRow;
					}

				mProgressController.updateProgress(destRow - existingRowCount);

				destRow++;
				}
			}

		clearBufferedData();

		if (mAppendRest && additionalRowCount > 0)
			mTableModel.finalizeNewRows(existingRowCount, mProgressController);

		if (destRowMap != null)
			populateHitlists(destRowMap.length, -1, destRowMap);

		populateDetails();

		return false;
		}

	/**
	 * If we append or merge we need to translate column indexes from source to destination tables.
	 * @param appendOrMergeColumn null or source to destination column mapping
	 */
	private void setColumnProperties(int[] appendOrMergeDestColumn) {
		if (mColumnProperties == null)
			return;

		for (String propertyKey:mColumnProperties.keySet()) {
			int index = propertyKey.indexOf('\t');
			String columnName = propertyKey.substring(0, index);

			int column = getSourceColumn(columnName);
			if (column != NO_COLUMN
			 && appendOrMergeDestColumn != null)
				column = appendOrMergeDestColumn[column];

			if (column != NO_COLUMN
			 && column >= mFirstNewColumn) {
				String key = propertyKey.substring(index+1);
				String value = mColumnProperties.get(propertyKey);

					// in case of merge/append column property references
					// to parent columns may need to be translated
				if (appendOrMergeDestColumn != null) {
					if (key.equals(cColumnPropertyParentColumn)) {
						int parentColumn = appendOrMergeDestColumn[getSourceColumn(value)];
						if (parentColumn == NO_COLUMN)	// visible columns that have a parent (e.g. cluster no)
							value = null;
						else
							value = mTableModel.getColumnTitleNoAlias(parentColumn);
						}
					}

				mTableModel.setColumnProperty(column, key, value);
				}
			}
		}

	private int getSourceColumn(String columnName) {
		for (int j=0; j<mFieldNames.length; j++)
			if (columnName.equals(mFieldNames[j]))
				return j;
		return NO_COLUMN;
		}

	private void populateHitlists(int rowCount, int offset, int[][] destRowMap) {
			// use either offset or destRowMap to indicate mapping of original hitlists to current rows
		if (mHitlists != null) {
			CompoundTableHitlistHandler hitlistHandler = mTableModel.getHitlistHandler();
			for (int list=0; list<mHitlists.size(); list++) {
				String listString = mHitlists.get(list);
				int index = listString.indexOf('\t');
				String name = listString.substring(0, index);
				byte[] data = new byte[listString.length()-index-1];
				for (int i=0; i<data.length; i++)
					data[i] = (byte)(listString.charAt(++index) - 64);

				String uniqueName = hitlistHandler.createHitlist(name, -1, CompoundTableHitlistHandler.EMPTY_LIST, -1, null);
				int flagNo = hitlistHandler.getHitlistFlagNo(uniqueName);
				int dataBit = 1;
				int dataIndex = 0;
				for (int row=0; row<rowCount; row++) {
					if ((data[dataIndex] & dataBit) != 0) {
						if (destRowMap == null)
							mTableModel.getTotalRecord(row+offset).setFlag(flagNo);
						else
							for (int destRow:destRowMap[row])
								mTableModel.getTotalRecord(destRow).setFlag(flagNo);
						}
					dataBit *= 2;
					if (dataBit == 64) {
						dataBit = 1;
						dataIndex++;
						}
					}
				}
			}
		}

	private void populateDetails() {
		CompoundTableDetailHandler detailHandler = mTableModel.getDetailHandler();
		if (detailHandler != null)
			detailHandler.setEmbeddedDetailMap(mDetails);
		}

	private void resolveDetailIDCollisions() {
		if (mDetails != null && mTableModel.getDetailHandler().getEmbeddedDetailCount() != 0) {
						// Existing data as well a new data have embedded details.
						// Adding an offset to the IDs of existing details ensures collision-free merging/appending.
			int highID = 0;
			Iterator<String> iterator = mDetails.keySet().iterator();
			while (iterator.hasNext()) {
				try {
					int id = Math.abs(Integer.parseInt(iterator.next()));
					if (highID < id)
						highID = id;
					}
				catch (NumberFormatException nfe) {}
				}

			if (highID != 0)
				mTableModel.addOffsetToEmbeddedDetailIDs(highID);
			}
		}

	private void clearBufferedData() {
		mFieldNames = null;
		mFieldData = null;
		mColumnProperties = null;
		mAppendDestColumn = null;
		mMergeDestColumn = null;
		}

	/**
	 * This function serves as a callback function to report the success when the loader thread is done.
	 * Overwrite this, if you need the status after loading.
	 * @param success true if file was successfully read
	 */
	public void finalStatus(boolean success) {}
	}
