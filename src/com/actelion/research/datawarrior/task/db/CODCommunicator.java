/*
 * Copyright 2016, Thomas Sander, openmolecules.org
 *
 * This file is part of COD-Structure-Server.
 *
 * COD-Structure-Server is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * COD-Structure-Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with COD-Structure-Server.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.calc.ProgressController;
import org.openmolecules.cod.CODServerConstants;
import org.openmolecules.comm.ClientCommunicator;

import java.util.TreeMap;

public class CODCommunicator extends ClientCommunicator implements CODServerConstants {
	private ProgressController mProgressController;
	private static String sServerURL = SERVER_URL;

	public CODCommunicator(ProgressController task) {
		super(false);
		mProgressController = task;
		}

	@Override
	public String getServerURL() {
		return sServerURL;
		}

	public static void setServerURL(String serverURL) {
		sServerURL = serverURL;
	}

	public byte[][][] search(TreeMap<String,Object> query) {
		return (byte[][][])getResponse(REQUEST_RUN_QUERY, KEY_QUERY, encode(query));
		}

	public byte[] getTemplate() {
		return (byte[])getResponse(REQUEST_TEMPLATE);
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
