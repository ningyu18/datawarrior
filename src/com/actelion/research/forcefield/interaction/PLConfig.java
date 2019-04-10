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
 * @author Joel Freyss
 */
package com.actelion.research.forcefield.interaction;

import java.util.HashMap;
import java.util.Map;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;

/**
 * Strategy Pattern used to defined how keys are assigned to a specific atom
 */
public abstract class PLConfig {
	
	public static int LAST_VERSION = 2;
	private static Map<Integer, PLConfig> instances = new HashMap<>();

	private int version;
	
	protected PLConfig(int version) {
		this.version = version;
	}
	
	public static PLConfig getInstance(int version) {
		PLConfig config = instances.get(version);
		if(config==null) {
			switch (version) {
			case 1:
				config = new PLConfigV1();
				break;
			case 2:
				config = new PLConfigV2();
				break;
			default:
				throw new IllegalArgumentException("Invalid version: "+version);
			}
			System.out.println("PLConfig initialized: " + config);
			instances.put(version, config);
		}
		return config;
	}
	
	public int getVersion() {
		return version;
	}
	
	
	public abstract void prepare(FFMolecule mol);
	
	public abstract String getParameterFile();
	
	public abstract String getSuperKey(FFMolecule mol, int a);
	public abstract String getSubKey(FFMolecule mol, int a);

	
	protected String getElement(FFMolecule mol, int a) {
		if(mol.getAtomicNo(a)<=1) return null;
		String s = Molecule.cAtomLabel[mol.getAtomicNo(a)];
		if(mol.isAromaticAtom(a)) s = s.toLowerCase();
		else s = s.toUpperCase();
		return s;
	}
	
	public abstract boolean isHydrogen(String key);
	
	public abstract boolean isSymetric();
	
	@Override
	public String toString() {
		return "[PLConfig: version=" + version + ", symetric=" + isSymetric() + "]";
	}
	
}
