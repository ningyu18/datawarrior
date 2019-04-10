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

package com.actelion.research.table;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.MolfileV3Creator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.form.ReferenceResolver;
import com.actelion.research.table.model.*;
import com.actelion.research.util.BinaryEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.*;

public class CompoundTableSaver implements CompoundTableConstants,Runnable {
	public static final String cCurrentFileVersion = "3.2";

	public static final int ID_USE_PROPERTY = -1;
	public static final int ID_BUILD_ONE = -2;

	private JTable				mTable;
	private CompoundTableModel	mTableModel;
	private JProgressDialog		mProgressDialog;
	private Frame   			mParentFrame;
	private File				mFile;
	private Writer				mDataWriter;
	private int					mDataType,mSDColumnStructure,mSDColumnIdentifier,
								mSDColumn2DCoordinates,mSDColumn3DCoordinates;
	private boolean				mVisibleOnly,mToClipboard,mEmbedDetails,mPrefer3D;
	private RuntimeProperties	mRuntimeProperties;
	private ArrayList<DataDependentPropertyWriter> mDataDependentPropertyWriterList;

	public CompoundTableSaver(Frame parent, CompoundTableModel tableModel, JTable table) {
		mTableModel = tableModel;
		mTable = table;
		mParentFrame = parent;
		}

	/**
	 * Writes the associated tableModel's data into a native file without asking any questions.
	 * Before returning this method calls finalStatus(File file) with file== null if it couldn't be successfully written.
	 * Error checking should be done before calling this function. 
	 * @param properties must be given if fileType==FileHelper.cFileTypeDataWarrior or ...Template
	 * @param file a valid file with proper write privileges
	 * @param visibleOnly if true, then only visible records are written
	 * @param embedDetails if true, referenced detail information is retrieved and embedded in the file
	 */
	public void saveNative(RuntimeProperties properties, File file, boolean visibleOnly, boolean embedDetails) {
		mRuntimeProperties = properties;
		mDataType = FileHelper.cFileTypeDataWarrior;
		mFile = file;
		mVisibleOnly = visibleOnly;
		mEmbedDetails = embedDetails;

		saveFile();
		}

	public void addDataDependentPropertyWriter(DataDependentPropertyWriter ddpw) {
		if (mDataDependentPropertyWriterList == null)
			mDataDependentPropertyWriterList = new ArrayList<>();

		mDataDependentPropertyWriterList.add(ddpw);
		}

	public void saveTemplate(RuntimeProperties properties, File file) {
		mRuntimeProperties = properties;
		mDataType = FileHelper.cFileTypeDataWarriorTemplate;
		mFile = file;
		mVisibleOnly = false;
		mEmbedDetails = false;

		saveFile();
		}

	/**
	 * Exports the associated tableModel's data into a TAB delimited text file without asking any questions.
	 * Before returning this method calls finalStatus(File file) with file=null if it couldn't be successfully written.
	 * Error checking should be done before calling this function.
	 * @param file a valid file with proper write privileges
	 */
	public void saveText(File file) {
		mRuntimeProperties = null;
		mDataType = FileHelper.cFileTypeTextTabDelimited;
		mFile = file;
		mVisibleOnly = false;
		mEmbedDetails = false;
		
		saveFile();
		}

	/**
	 * Exports the associated tableModel's data into an SD-file without asking any questions.
	 * Before returning this method calls finalStatus(File file) with file=null if it couldn't be successfully written.
	 * Error checking should be done before calling this function.
	 * @param file a valid file with proper write privileges
	 * @param fileType CompoundFileHelper.cFileTypeSDV3 or CompoundFileHelper.cFileTypeSDV2
	 * @param structureColumn column containing the idcode encoded structure
	 * @param idColumn column containing compound identifiers or ID_USE_PROPERTY or ID_BUILD_ONE
	 * @param prefer3DCoords if true and if a column with 3D-coordinates exists, then these are used
	 */
	public void saveSDFile(File file, int fileType, int structureColumn, int idColumn, boolean prefer3DCoords) {
		mRuntimeProperties = null;
		mDataType = fileType;
		mFile = file;
		mVisibleOnly = false;
		mEmbedDetails = false;

		mSDColumnStructure = structureColumn;
		mSDColumnIdentifier = idColumn;
		mSDColumn2DCoordinates = -1;
		mSDColumn3DCoordinates = -1;

		if (mSDColumnStructure != -1) {
			if (mSDColumnIdentifier == ID_USE_PROPERTY)
				mSDColumnIdentifier = mTableModel.findColumn(mTableModel.getColumnProperty(mSDColumnStructure, CompoundTableModel.cColumnPropertyIdentifierColumn));
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				String specialType = mTableModel.getColumnSpecialType(column);
				if (specialType != null
				 && mTableModel.getParentColumn(column) == mSDColumnStructure) {
					if (specialType.equals(CompoundTableModel.cColumnType2DCoordinates))
						mSDColumn2DCoordinates = column;
					else if (specialType.equals(CompoundTableModel.cColumnType3DCoordinates))
						mSDColumn3DCoordinates = column;
					}
				}
			}

		mPrefer3D = prefer3DCoords && (mSDColumn3DCoordinates != -1);

		saveFile();
		}

	public void writeFile(String filename, RuntimeProperties properties) {
		mFile = new File(filename);
		mDataType = FileHelper.cFileTypeDataWarrior;
		mVisibleOnly = false;
		mEmbedDetails = false;
		saveFile();
		}

	private void saveFile() {
		try {
			mDataWriter = new OutputStreamWriter(new FileOutputStream(mFile),"UTF-8");
			mToClipboard = false;
			processData();
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParentFrame, e);
			finalStatus(null);
			}
		}

	public void copy() {
		mDataWriter = new StringWriter(1024);
		mDataType = FileHelper.cFileTypeTextTabDelimited;
		mEmbedDetails = false;
		mVisibleOnly = false;
		mToClipboard = true;
		try {
			writeTextData();
			StringSelection theData = new StringSelection(mDataWriter.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParentFrame, e);
			}
		}

	private void processData() {
		mProgressDialog = new JProgressDialog(mParentFrame, false);

		Thread t = new Thread(this, "CompoundTableSaver");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

		mProgressDialog.setVisible(true);
		}

	private void writeTextData() throws IOException {
		synchronized(mDataWriter) {
			BufferedWriter theWriter = new BufferedWriter(mDataWriter);

			if (mDataType == FileHelper.cFileTypeDataWarrior) {
				writeFileHeader(theWriter);
				writeTableExtensions(theWriter);
				writeColumnProperties(theWriter);
				}

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType != FileHelper.cFileTypeDataWarriorTemplate)
				writeRecords(theWriter);

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType == FileHelper.cFileTypeDataWarrior) {
				writeHitlists(theWriter);
				}

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType == FileHelper.cFileTypeDataWarrior) {
				writeEmbeddedDetails(theWriter);
				}

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && (mDataType == FileHelper.cFileTypeDataWarrior
			  || mDataType == FileHelper.cFileTypeDataWarriorTemplate)
			 && mRuntimeProperties != null)
				mRuntimeProperties.write(theWriter);

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType == FileHelper.cFileTypeDataWarrior) {
				writeDataDependentProperties(theWriter);
				}

			theWriter.close();
			}
		}

	private void writeRecords(BufferedWriter theWriter) throws IOException {
		if (mToClipboard && mTable == null)	// just to make sure
			return;

		int tabs = mTableModel.getTotalColumnCount() - 1;

		// first write non-displayable columns
		if (mDataType == FileHelper.cFileTypeDataWarrior) {
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (!mTableModel.isColumnDisplayable(column)) {
					theWriter.write(mTableModel.getColumnTitleNoAlias(column));
					if (tabs-- > 0)
						theWriter.write("\t");
					}
				}
			}

		if (mToClipboard) {	// selected columns only
			int[] selectedColumn = mTable.getSelectedColumns();

			// don't write a header into the clipboard, if only one cell is selected
			if (selectedColumn.length > 1
			 || ((CompoundListSelectionModel)mTable.getSelectionModel()).getSelectionCount() > 1) {
				for (int i=0; i<selectedColumn.length; i++) {
					int column = mTableModel.convertFromDisplayableColumnIndex(
									  mTable.convertColumnIndexToModel(selectedColumn[i]));
					if (mDataType == FileHelper.cFileTypeTextTabDelimited)
						theWriter.write(mTableModel.getColumnTitleWithSpecialType(column));
					else
						theWriter.write(mTableModel.getColumnTitleNoAlias(column));
					if (i < selectedColumn.length-1)
						theWriter.write("\t");
					}
				theWriter.write("\n");
				}
			}
		else {
			// now write displayable columns in table model order
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isColumnDisplayable(column)) {
					if (mDataType == FileHelper.cFileTypeTextTabDelimited)
						theWriter.write(mTableModel.getColumnTitleNoAliasWithSpecialType(column));
					else
						theWriter.write(mTableModel.getColumnTitleNoAlias(column));
					if (tabs-- > 0)
						theWriter.write("\t");
					}
				}
			}

		if (!mToClipboard)
			theWriter.newLine();

		int rowCount = mVisibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();

		if (mProgressDialog != null)
			mProgressDialog.startProgress("Saving Records...", 0, rowCount);

		for (int row=0; row<rowCount; row++) {
			CompoundRecord record = (mVisibleOnly) ? mTableModel.getRecord(row)
					   : mTableModel.getTotalRecord(row);

			if (!mToClipboard || mTableModel.isVisibleAndSelected(record)) {
				if (mProgressDialog != null) {
					if (mProgressDialog.threadMustDie())
						break;
					mProgressDialog.updateProgress(row);
					}

				tabs = mTableModel.getTotalColumnCount() - 1;

				if (mDataType == FileHelper.cFileTypeDataWarrior) {
					// write non-displayable columns first
					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						if (!mTableModel.isColumnDisplayable(column)) {
							theWriter.write(convertNewlines(getValue(record, column)));
							if (tabs-- > 0)
								theWriter.write("\t");
							}
						}
					}

				if (mToClipboard) {	// selected columns only
					int[] selectedColumn = mTable.getSelectedColumns();
					for (int i=0; i<selectedColumn.length; i++) {
						int column = mTableModel.convertFromDisplayableColumnIndex(
									 mTable.convertColumnIndexToModel(selectedColumn[i]));
						theWriter.write(convertNewlines(getValue(record, column)));
						if (i < selectedColumn.length-1)
							theWriter.write("\t");
						}
					theWriter.write("\n");
					}
				else {
					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						if (mTableModel.isColumnDisplayable(column)) {
							theWriter.write(convertNewlines(getValue(record, column)));
							if (tabs-- > 0)
								theWriter.write("\t");
							}
						}
					}

				if (!mToClipboard)
					theWriter.newLine();
				}
			}
		}

	private String getValue(CompoundRecord record, int column) {
		return (mDataType == FileHelper.cFileTypeDataWarrior) ?
				   (String)mTableModel.encodeDataWithDetail(record, column)
			 : (mToClipboard) ? // use the display value (mean, max, sum, etc.)
				   (String)mTableModel.getValue(record, column)
				 : (String)mTableModel.encodeData(record, column);
		}

	private String convertNewlines(String value) {
		value = value.replaceAll(NEWLINE_REGEX, NEWLINE_STRING);
		value = value.replace("\t", CompoundTableLoader.TAB_STRING);
		return value;
		}

	private void writeFileHeader(BufferedWriter theWriter) throws IOException {
		theWriter.write(cNativeFileHeaderStart);
		theWriter.newLine();
		theWriter.write("<"+cNativeFileVersion+"=\""+cCurrentFileVersion+"\">");
		theWriter.newLine();
		theWriter.write("<"+cNativeFileCreated+"=\""+System.currentTimeMillis()+"\">");
		theWriter.newLine();
		int rowCount = mVisibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();
		theWriter.write("<"+cNativeFileRowCount+"=\""+rowCount+"\">");
		theWriter.newLine();
		theWriter.write(cNativeFileHeaderEnd);
		theWriter.newLine();
		}

	private void writeTableExtensions(BufferedWriter theWriter) throws IOException {
		CompoundTableExtensionHandler extensionHandler = mTableModel.getExtensionHandler();
		if (extensionHandler != null) {
			String[] nameList = mTableModel.getAvailableExtensionNames();
			if (nameList != null)
				for (String name:nameList)
					extensionHandler.writeData(name, mTableModel.getExtensionData(name), theWriter);
			}
		}

	private void writeColumnProperties(BufferedWriter theWriter) throws IOException {
		boolean found = false;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			HashMap<String,String> map = mTableModel.getColumnProperties(column);
			if (map != null && !map.isEmpty()) {
				if (!found) {
					theWriter.write(cColumnPropertyStart);
					theWriter.newLine();
					found = true;
					}
				theWriter.write("<"+cColumnName+"=\""+mTableModel.getColumnTitleNoAlias(column)+"\">");
				theWriter.newLine();
				Iterator<String> iterator = map.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					theWriter.write("<"+cColumnProperty+"=\""+key+"\t"+map.get(key)+"\">");
					theWriter.newLine();
					}
				}
			}
		if (found) {
			theWriter.write(cColumnPropertyEnd);
			theWriter.newLine();
			}
		}

	private void writeHitlists(BufferedWriter theWriter) throws IOException {
		CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();
		if (mTableModel.hasSelectedRows() || (hitlistHandler != null && hitlistHandler.getListCount() != 0)) {
			theWriter.write(cHitlistDataStart);
			theWriter.newLine();
			if (mTableModel.hasSelectedRows())
				writeOneHitlist(theWriter, CompoundRecord.cFlagSelected, CompoundTableListHandler.LISTNAME_SELECTION);
			if (hitlistHandler != null)
				for (int list = 0; list<hitlistHandler.getListCount(); list++)
					writeOneHitlist(theWriter, hitlistHandler.getListFlagNo(list), hitlistHandler.getListName(list));
			theWriter.write(cHitlistDataEnd);
			theWriter.newLine();
			}
		}

	private void writeOneHitlist(BufferedWriter theWriter, int flagNo, String listName) throws IOException {
		int rowCount = mVisibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();
		byte[] data = new byte[(5+rowCount)/6];
		int dataBit = 1;
		int dataIndex = 0;
		for (int row=0; row<rowCount; row++) {
			CompoundRecord record = mVisibleOnly ? mTableModel.getRecord(row) : mTableModel.getTotalRecord(row);
			if (record.isFlagSet(flagNo))
				data[dataIndex] |= dataBit;
			dataBit *= 2;
			if (dataBit == 64) {
				dataBit = 1;
				dataIndex++;
				}
			}
		for (int i=0; i<data.length; i++)
			data[i] += 64;
		theWriter.write("<"+cHitlistName+"=\""+listName+"\">");
		theWriter.newLine();
		for (int offset=0; offset<data.length; offset+=80) {
			String value = new String(data, offset, Math.min(80, data.length-offset));
			theWriter.write("<"+cHitlistData+"=\""+value+"\">");
			theWriter.newLine();
			}
		}

	private void writeEmbeddedDetails(BufferedWriter theWriter) throws IOException {
		HashMap<String,byte[]> detailMap = mTableModel.getDetailHandler().getEmbeddedDetailMap();

		TreeSet<String> usedIDSet = null;
		if (mVisibleOnly) {		// save only details that are referenced from visible records
			usedIDSet = new TreeSet<String>();
			for (int row=0; row<mTableModel.getRowCount(); row++) {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					String[][] key = mTableModel.getRecord(row).getDetailReferences(column);
					if (key != null)
						for (int detailIndex=0; detailIndex<key.length; detailIndex++)
							if (key[detailIndex] != null)
								for (int i=0; i<key[detailIndex].length; i++)
									usedIDSet.add(key[detailIndex][i]);
					}
				}
			}

		if (detailMap != null && detailMap.size() != 0 && (usedIDSet == null || !usedIDSet.isEmpty())) {
			theWriter.write(cDetailDataStart);
			theWriter.newLine();
			for (String id:detailMap.keySet()) {
				if (usedIDSet == null || usedIDSet.contains(id)) {
					theWriter.write("<"+cDetailID+"=\""+id+"\">");
					theWriter.newLine();
	
					byte[] detail = (byte[])detailMap.get(id);
					BinaryEncoder encoder = new BinaryEncoder(theWriter);
					encoder.initialize(8, detail.length);
					for (int i=0; i<detail.length; i++)
						encoder.write(detail[i]);
					encoder.finalize();
	
					theWriter.write("</"+cDetailID+">");
					theWriter.newLine();
					}
				}
			theWriter.write(cDetailDataEnd);
			theWriter.newLine();
			}
		}

	private void embedAllDetails() {
		int errorCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			for (int detail=0; detail<mTableModel.getColumnDetailCount(column); detail++) {
				String source = mTableModel.getColumnDetailSource(column, detail);
				if (!source.equals(CompoundTableDetailHandler.EMBEDDED)) {
					ArrayList<String> keyList = new ArrayList<String>();
					for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
						String[][] references = mTableModel.getTotalRecord(row).getDetailReferences(column);
						if (references != null && references.length>detail && references[detail] != null) {
							for (int i=0; i<references[detail].length; i++) {
								keyList.add(references[detail][i]);
								}
							}
						}

					String type = mTableModel.getColumnDetailType(column, detail);
					CompoundTableDetailSpecification sourceSpec = new CompoundTableDetailSpecification(mTableModel, column, detail);
					HashMap<String,String> oldToNewKeyMap = mTableModel.getDetailHandler().embedDetails(keyList.toArray(), sourceSpec, ReferenceResolver.MODE_DEFAULT, type, mProgressDialog);

					if (oldToNewKeyMap != null) {
						for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
							String[][] references = mTableModel.getTotalRecord(row).getDetailReferences(column);
							if (references != null && references.length>detail && references[detail] != null) {
								for (int i=0; i<references[detail].length; i++) {
									String newKey = oldToNewKeyMap.get(references[detail][i]);
									if (newKey != null)
										references[detail][i] = newKey;
									}
								}
							}
						mTableModel.setColumnDetailSource(column, detail, CompoundTableDetailHandler.EMBEDDED);
						}
					else {
						errorCount++;
						}
					}
				}
			}

		if (errorCount != 0)
			mProgressDialog.showErrorMessage("Some detail data could not be embedded and won't be saved.");
		}

	private void writeDataDependentProperties(BufferedWriter theWriter) throws IOException {
		if (mDataDependentPropertyWriterList != null) {
			for (DataDependentPropertyWriter ddpw:mDataDependentPropertyWriterList) {
				if (ddpw.hasSomethingToWrite()) {
					theWriter.write(cDataDependentPropertiesStart);
					theWriter.write(ddpw.getPropertyName());
					theWriter.write("\">");
					theWriter.newLine();
					ddpw.write(theWriter);
					theWriter.write(cDataDependentPropertiesEnd);
					theWriter.newLine();
					}
				}
			}
		}

	private void writeSDData() throws IOException {
		synchronized(mDataWriter) {
			BufferedWriter theWriter = new BufferedWriter(mDataWriter);

			if (mProgressDialog != null)
				mProgressDialog.startProgress("Saving Records...", 0, mTableModel.getTotalRowCount());

			StereoMolecule mol = new StereoMolecule();

			IDCodeParser parser2D = new IDCodeParser(true);
			IDCodeParser parser3D = new IDCodeParser(false);

			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {			
				if (mProgressDialog != null) {
					if (mProgressDialog.threadMustDie())
						break;
					mProgressDialog.updateProgress(row);
					}

				CompoundRecord record = mTableModel.getTotalRecord(row);
				if (mSDColumnStructure != -1) {
					byte[] idcode = (byte[])record.getData(mSDColumnStructure);
					byte[] coords2D = (byte[])record.getData(mSDColumn2DCoordinates);
					byte[] coords3D = (byte[])record.getData(mSDColumn3DCoordinates);

					if (idcode != null) {
						if (coords3D != null && mPrefer3D)
							parser3D.parse(mol, idcode, coords3D);
						else
							parser2D.parse(mol, idcode, coords2D);

						if (mSDColumnIdentifier != -1) {
							byte[] name = (mSDColumnIdentifier == ID_BUILD_ONE) ?
									  ("Compound ".concat(Integer.toString(row+1))).getBytes()
									: (byte[])record.getData(mSDColumnIdentifier);
							if (name != null)
								mol.setName(new String(name));
							}
						}
					else {
						mol.deleteMolecule();
						}
					}

				if (mDataType == FileHelper.cFileTypeSDV3)
					new MolfileV3Creator(mol).writeMolfile(theWriter);
				else
					new MolfileCreator(mol).writeMolfile(theWriter);

				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (mTableModel.getColumnSpecialType(column) == null) {
						theWriter.write(">  <"+mTableModel.getColumnTitle(column)+">");
						theWriter.newLine();
						theWriter.write(convertNewlines((String)mTableModel.encodeDataWithDetail(record, column)));
						theWriter.newLine();
						theWriter.newLine();
						}
					}

				theWriter.write("$$$$");
				theWriter.newLine();
				}
			theWriter.close();
			}
		}

	public void run() {
		boolean successful = true;

		if (mEmbedDetails)
			embedAllDetails();

		try {
			switch (mDataType) {
			case FileHelper.cFileTypeDataWarrior:
			case FileHelper.cFileTypeDataWarriorTemplate:
			case FileHelper.cFileTypeTextTabDelimited:
				writeTextData();
				break;
			case FileHelper.cFileTypeSDV2:
			case FileHelper.cFileTypeSDV3:
				writeSDData();
				break;
				}

			if (mDataType == FileHelper.cFileTypeDataWarrior && !mVisibleOnly) {
				mTableModel.setFile(mFile);
				mParentFrame.setTitle(mFile.getName());
				}
			}
		catch (IOException e) {
			mProgressDialog.showErrorMessage(e.toString());
			successful = false;
			}

		if (mProgressDialog.threadMustDie())	// action was cancelled
			successful = false;

		finalStatus(successful ? mFile : null);
		mProgressDialog.close(null);
		}

	/**
	 * This function serves as a callback function to report the success when the saver thread is done.
	 * Overwrite this, if you need the status after saving.
	 * @param file valid file if successful, otherwise null
	 */
	public void finalStatus(File file) {}
	}
