/*
 * Created on Sep 27, 2005
 *
 */
package org.openmolecules.pdb;

import javax.swing.filechooser.FileFilter;

/**
 * 
 * @author freyssj
 */
public abstract class ParserFileFilter extends FileFilter {

	public abstract String getExtension();
}
