/**
 * Title:        OsirisConnectionHelper.java
 * Description:  Helper class to provide an interactive Connection object to the OSIRIS database
 * Copyright:    Copyright (c) 2001
 * Company:      Actelion Ltd.
 * @author       Thomas Sander
 * @version 1.0
 */

package com.actelion.research.database;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.actelion.research.gui.JLoginDialog;

public class OsirisConnectionHelper implements ActionListener {
	private static String		sConnectString = "jdbc:oracle:thin:@hypnos.idorsia.com:1521:act0";
	private static String       sProgramName = "UnnamedActelionApplication";
	private static boolean		sDriverRegistered;
	private static Frame		sParentFrame;
	private static JLoginDialog	sLoginDialog;
	private static volatile Connection	sConnection;
	private static String		sUserID;
	private static ArrayList<OsirisConnectionListener>	sConnectionListener;

	public static boolean registerDriver(Frame owner) {
		if (!sDriverRegistered) {
			try {
				Class.forName("oracle.jdbc.driver.OracleDriver"); // instead of  DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
				sDriverRegistered = true;
			}
			catch (Exception ex) {
				JOptionPane.showMessageDialog(owner, ex);
				return false;
			}
		}
		return sDriverRegistered;
	}

	/**
	 * Returns the default OSIRIS connection instance, if there is one.
	 * If not, shows a connection dialog and tries to establish a new connection.
	 * This can be called from any thread.
	 * @param owner for connection dialog
	 * @return connection or null (if user canceled or other error occurred)
	 */
	public static Connection getConnection(final Frame owner) {
		if (SwingUtilities.isEventDispatchThread()) {
			connect(owner);
		}
		else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						connect(owner);
					}
				} );
			}
			catch (Exception e) {}
		}
		return sConnection;
	}

	private static boolean connect(Frame owner) {
		// needs to be called from the event dispatcher thread!!!

		if (!registerDriver(owner))
			return false;

		// don't allow nested calls...
		if (sLoginDialog != null)
			return false;

		if (sConnection == null) {
			sParentFrame = owner;
			sLoginDialog = new JLoginDialog(owner, new OsirisConnectionHelper());
			sLoginDialog.setVisible(true);
		}

		return true;	// false if user cancels login dialog
	}

	public static void requestConnection(OsirisConnectionListener l, Frame owner) {
		// needs to be called from the event dispatcher thread!!!
		if (sConnection != null) {
			l.connectionEstablished(true);
			return;
		}

		boolean found = false;
		if (sConnectionListener == null) {
			sConnectionListener = new ArrayList<OsirisConnectionListener>();
		}
		else {
			for (OsirisConnectionListener ocl:sConnectionListener) {
				if (l == ocl) {
					found = true;
					break;
				}
			}
		}
		if (!found)
			sConnectionListener.add(l);

		getConnection(owner);
	}

	public static void setConnectString(String cs) {
		sConnectString = cs;
	}

	public static void setProgramName(String pn) {
		sProgramName = pn;
	}

	public static void closeConnection() {
		try {
			if (sConnection == null) {
				sConnection.close();
				sConnection = null;
			}
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(sParentFrame, ex);
		}
	}

	/**
	 * Returns the Connection object - null if getConnection(Frame) has not been
	 * called yet. This function is useful when an application behaves differently
	 * with or without a DB Connection
	 * @return a Connection
	 */
	public static Connection getConnection() {
		return sConnection;
	}

	public static String getUserID() {
		return sUserID;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JLoginDialog.cLoginOK)) {
			try {
				if (sLoginDialog.getUserID().length() == 0
						|| sLoginDialog.getPassword().length() == 0)
					return;

				java.util.Properties props = new java.util.Properties();
				props.put("v$session.program", sProgramName);
				props.put("user", sLoginDialog.getUserID());
				props.put("password", sLoginDialog.getPassword());
				sConnection = DriverManager.getConnection(sConnectString, props);

				sConnection.setAutoCommit(false);

				sUserID = sLoginDialog.getUserID().toUpperCase();
			}
			catch (Exception ex) {
				JOptionPane.showMessageDialog(sParentFrame, ex);
				sUserID = null;
				return;
			}
		}

		sLoginDialog.setVisible(false);
		sLoginDialog.dispose();
		sLoginDialog = null;

		boolean succeeded = e.getActionCommand().equals(JLoginDialog.cLoginOK);
		if (sConnectionListener != null)
			for (int i=0; i<sConnectionListener.size(); i++)
				sConnectionListener.get(i).connectionEstablished(succeeded);
		sConnectionListener = null;
	}

	/**
	 * Creates and returns an ArrayList of results from a SQL query with one
	 * parameter. In case of an empty result an empty list is returned.
	 * @param sql any valid SQL with one column in column list
	 * @return result list or null if there is no connection.
	 */
	public static ArrayList<String> getResultList(String sql) {
		if (sConnection == null)
			return null;

		ArrayList<String> resultList = new ArrayList<String>();
		Statement stmt = null;
		ResultSet rset = null;
		try {
			stmt = sConnection.createStatement();
			rset = stmt.executeQuery(sql);
			while (rset.next())
				resultList.add(rset.getString(1));
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(sParentFrame, e);
			resultList.clear();
		}

		try {
			if (rset != null)
				rset.close();
			if (stmt != null)
				stmt.close();
		}
		catch (SQLException e) {}

		return resultList;
	}

	/**
	 * Creates and returns an ArrayList of results from a SQL query with multiple
	 * parameters. In case of an empty result an empty list is returned.
	 * Can be called from any thread.
	 * @param sql any valid SQL with one column in column list
	 * @param columnCount
	 * @return result list or null if there is no connection.
	 */
	public static ArrayList<String[]> getResultList(String sql, int columnCount) {
		if (sConnection == null)
			return null;

		ArrayList<String[]> resultList = new ArrayList<String[]>();
		Statement stmt = null;
		ResultSet rset = null;
		try {
			stmt = sConnection.createStatement();
			rset = stmt.executeQuery(sql);
			while (rset.next()) {
				String[] result = new String[columnCount];
				for (int i=0; i<columnCount; i++)
					result[i] = rset.getString(i+1);
				resultList.add(result);
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(sParentFrame, e);
			resultList.clear();
		}

		try {
			if (rset != null)
				rset.close();
			if (stmt != null)
				stmt.close();
		}
		catch (SQLException e) {}

		return resultList;
	}

	/**
	 * Return list of projects where the user has at least the privilege <privilege>
	 * and the project has at least one defined test that contains at least one experiment!
	 * @param privilege
	 * @return result list or null if there is no connection.
	 */
	public static ArrayList<String> getProjectList(int privilege) {
		return getProjectList(privilege, false);
	}

	/**
	 * Return list of projects where the user has at least the privilege <privilege>
	 * and (if !includeEmptyProjects) the project has at least one defined test that
	 * contains at least one experiment!
	 * @param privilege
	 * @param includeEmptyProjects
	 * @return result list or null if there is no connection.
	 */
	public static ArrayList<String> getProjectList(int privilege, boolean includeEmptyProjects) {
		return includeEmptyProjects ?
				getResultList("(SELECT project_name FROM osiris.project_admin"
						+" WHERE (project_member=USER OR project_member='EVERYONE')"
						+" AND NOT project_name='EVERYPROJECT'"
						+" AND privilege>="+privilege
						+" UNION "
						+"SELECT project_name FROM osiris.project"
						+" WHERE EXISTS (SELECT * FROM osiris.project_admin"
						+" WHERE project_name='EVERYPROJECT' AND project_member=USER AND privilege>="+privilege+"))"
						+" ORDER BY project_name")
				: getResultList("SELECT DISTINCT project_name from osiris.assay a"
						+" WHERE EXISTS (SELECT * FROM osiris.assay_result r WHERE r.assay_id=a.assay_id) AND project_name IN("
						+"SELECT project_name FROM osiris.project_admin"
						+" WHERE (project_member=USER OR project_member='EVERYONE')"
						+" AND NOT project_name='EVERYPROJECT'"
						+" AND privilege>="+privilege
						+" UNION "
						+"SELECT project_name FROM osiris.project"
						+" WHERE EXISTS (SELECT * FROM osiris.project_admin"
						+" WHERE project_name='EVERYPROJECT' AND project_member=USER AND privilege>="+privilege+"))"
						+" ORDER BY project_name");
	}

	/**
	 * Return list of projects including those with not even a test defined.
	 * @return result list or null if there is no connection.
	 */
	public static ArrayList<String> getProjectList() {
		return getResultList("SELECT project_name FROM osiris.project ORDER BY project_name");
	}

	public static byte[] getBLOB(String sql, String key) {
		if (sConnection == null)
			return null;

		byte[] bytes = null;
		try {
			PreparedStatement pstmt = sConnection.prepareStatement(sql);
			pstmt.setString(1, key);
			ResultSet rset = pstmt.executeQuery();

			if (rset.next ()) {
				Blob blob = (Blob)rset.getObject(1);
				int blobSize = (int)blob.length();
				bytes = blob.getBytes(1, blobSize);
			}

			rset.close();
			pstmt.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return bytes;
	}
}