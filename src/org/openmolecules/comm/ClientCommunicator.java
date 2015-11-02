/*
 * @(#)ClientCommunicator.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of openmolecules.org.  The intellectual and technical concepts contained
 * herein are proprietary to openmolecules.org.
 * Actelion Pharmaceuticals Ltd. is granted a non-exclusive, non-transferable
 * and timely unlimited usage license.
 *
 * @author Thomas Sander
 */

package org.openmolecules.comm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public abstract class ClientCommunicator extends CommunicationHelper {
    private boolean	mWithSessions;
	private long	mMostRecentBusyTime;
	private String	mSessionID;

	public abstract String getServerURL();
	public abstract void showBusyMessage(String message);
	public abstract void showErrorMessage(String message);

	public ClientCommunicator(boolean withSessions) {
		mWithSessions = withSessions;
		}

	private URLConnection getConnection() throws MalformedURLException, IOException {
        URL urlServlet = new URL(getServerURL());
        URLConnection con = urlServlet.openConnection();
    
        // konfigurieren
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");

        if (mWithSessions && mSessionID != null)
            con.addRequestProperty(KEY_SESSION_ID, mSessionID);

        return con;
        }

    private String getResponse(URLConnection con) {
        final int BUFFER_SIZE = 1024;
        StringBuilder sb = new StringBuilder();
        try {
            BufferedInputStream is = new BufferedInputStream(con.getInputStream());
            byte[] buf = new byte[BUFFER_SIZE];
            while (true) {
                int size = is.read(buf, 0, BUFFER_SIZE);
                if (size == -1)
                    break;

                sb.append(new String(buf, 0, size));
                }
            if (sb.length() != 0)
                return sb.toString();
            }
        catch (IOException ioe) {
        	showErrorMessage(ioe.toString());
            }
        catch (Throwable t) {
        	showErrorMessage(t.toString());
            }

        return null;
        }

    public void closeConnection() {
		if (mSessionID != null) {
			showBusyMessage("Closing Communication Channel ...");

	        try {
	            URLConnection con = getConnection();
                con.addRequestProperty(KEY_REQUEST, REQUEST_END_SESSION);

                getResponse(con);
	            }
	        catch (Exception ex) {
				showErrorMessage(ex.toString());
	            }

			mSessionID = null;
			System.runFinalization();
					// supposed to call the unreferenced() method on the server object

			showBusyMessage("");
			}
		}

	private void getNewSession() {
		String msg = null;
		if (mSessionID == null) {
			showBusyMessage("Opening session...");

			if (System.currentTimeMillis() - mMostRecentBusyTime < 10000)
				msg = "Please wait at least 10 seconds before retrying when the server is busy.";
			else {
				try {
	                URLConnection con = getConnection();
	                con.addRequestProperty(KEY_REQUEST, REQUEST_NEW_SESSION);

	                String response = getResponse(con);
	                if (response == null)
	                    msg = "No response from server";
                    else if (response.startsWith(BODY_ERROR))
                        msg = response;
	                else if (response.startsWith(BODY_MESSAGE))
	                    mSessionID = response.substring(BODY_MESSAGE.length()+1).trim();
	                else
	                    msg = "Unexpected response:"+response;
					}
				catch (IOException e) {
					showErrorMessage(e.toString());
					}

				if (msg != null)
				    mMostRecentBusyTime = System.currentTimeMillis();
			    }

			showBusyMessage("");
			}

		if (msg != null)
			showErrorMessage(msg);
		}

	public Object getResponse(String request, String... keyValuePair) {
		if (mWithSessions && mSessionID == null) {
			getNewSession();
			if (mSessionID == null)
				return null;
			}

		showBusyMessage("Requesting data ...");
		String msg = null;
		Object result = null;
        try {
            URLConnection con = getConnection();
        	con.addRequestProperty(KEY_REQUEST, request);
            for (int i=0; i<keyValuePair.length; i+=2)
            	con.addRequestProperty(keyValuePair[i], keyValuePair[i+1]);

            String response = getResponse(con);
            if (response == null)
                msg = "No response from server";
            else if (response.startsWith(BODY_ERROR))
                msg = response;
            else if (response.startsWith(BODY_MESSAGE))
                result = response.substring(BODY_MESSAGE.length()+1).trim();
            else if (response.startsWith(BODY_OBJECT))
                result = decode(response.substring(BODY_OBJECT.length()+1));
            else
                msg = "Unexpected response:"+response;
            }
        catch (IOException e) {
			showErrorMessage(e.toString());
            }

		showBusyMessage("");

        if (msg != null) {
            if (msg.equals(BODY_ERROR_INVALID_SESSION))
                mSessionID = null;

			showErrorMessage(msg);
            return null;
            }

		return result;
		}
	}

