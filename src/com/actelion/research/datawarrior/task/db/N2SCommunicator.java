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

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.calc.ProgressController;
import org.openmolecules.comm.ClientCommunicator;
import org.openmolecules.n2s.N2SServerConstants;

public class N2SCommunicator extends ClientCommunicator implements N2SServerConstants {
	private static String sURL = "n2s.openmolecules.org";

	private ProgressController mProgressController;
	private boolean mConnectionProblem;
	private int mErrorCount;

	public static void setServerURL(String url) {
		sURL = url;
		}

	public N2SCommunicator(ProgressController task) {
		super(false);
		mProgressController = task;
		mErrorCount = 0;
		}

	public String getIDCode(String name) {
		return (String)getResponse("idcode", KEY_STRUCTURE_NAME, name);
		}

	public String getMolfile(String name) {
		return (String)getResponse("molfile", KEY_STRUCTURE_NAME, name);
		}

	public boolean hasConnectionProblem() {
		return mConnectionProblem;
	}

	public int getErrorCount() {
		return mErrorCount;
	}

	@Override
	public String getServerURL() {
		return sURL;
	}

	@Override
	public void showBusyMessage(String message) {
	}

	@Override
	public void showErrorMessage(String message) {
		if (message.startsWith("java.net.")) {
			mConnectionProblem = true;
			if (mProgressController != null)
				mProgressController.showErrorMessage(message);
			}
		else {
			mErrorCount++;
			}
		}
	}
