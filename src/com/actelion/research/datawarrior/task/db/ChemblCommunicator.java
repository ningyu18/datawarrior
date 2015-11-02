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

import java.util.TreeMap;

import org.openmolecules.chembl.ChemblServerConstants;
import org.openmolecules.comm.ClientCommunicator;

import com.actelion.research.calc.ProgressController;

public class ChemblCommunicator extends ClientCommunicator implements ChemblServerConstants {
	private ProgressController mProgressController;
	private static String sServerURL = SERVER_URL;

	public ChemblCommunicator(ProgressController task) {
		super(false);
		mProgressController = task;
		}

	public byte[][][] getTargetTable() {
		return (byte[][][])getResponse(REQUEST_GET_TARGET_LIST);
		}

	public byte[][][] getProteinClassDictionary() {
		return (byte[][][])getResponse(REQUEST_GET_PROTEIN_CLASS_DICTIONARY);
		}

	public byte[][][] search(TreeMap<String,Object> query) {
		return (byte[][][])getResponse(REQUEST_RUN_QUERY, KEY_QUERY, encode(query));
		}

	public byte[][][] getAssayDetailTable(byte[][] assayID) {
		return (byte[][][])getResponse(REQUEST_GET_ASSAY_DETAILS, KEY_ID_LIST, encode(assayID));
		}

	/**
	 * This runs a skelSpheres search against many(!) given molecules
	 * @param idcode
	 * @return
	 */
	public byte[][][] findActiveCompoundsSkelSpheres(byte[][] idcode) {
		return (byte[][][])getResponse(REQUEST_FIND_ACTIVES_SKELSPHERES, KEY_IDCODE_LIST, encode(idcode));
		}

	/**
	 * This runs a flexophore and skelSPheres search against one(!) given molecule
	 * @param idcode
	 * @return
	 */
	public byte[][][] findActiveCompoundsFlexophore(byte[] idcode) {
		return (byte[][][])getResponse(REQUEST_FIND_ACTIVES_FLEXOPHORE, KEY_IDCODE, encode(idcode));
		}

	@Override
	public String getServerURL() {
		return sServerURL;
		}

	public static void setServerURL(String serverURL) {
		sServerURL = serverURL;
		}

	@Override
	public void showBusyMessage(String message) {
		if (message.length() == 0)
			System.out.println("Done");
		else
			System.out.println("Busy: "+message);
		}

	@Override
	public void showErrorMessage(String message) {
		if (mProgressController != null)
			mProgressController.showErrorMessage(message);
		else
			System.out.println("Error: "+message);
		}
	}
