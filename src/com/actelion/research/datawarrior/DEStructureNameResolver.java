package com.actelion.research.datawarrior;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.name.INameResolver;
import com.actelion.research.datawarrior.task.db.N2SCommunicator;

import java.util.TreeMap;

/**
 * Created by thomas on 7/13/17.
 */
public class DEStructureNameResolver implements INameResolver {
	private N2SCommunicator sCommunicator;
	private TreeMap<String,String> sNameMap;

	@Override
	public StereoMolecule resolveName(String name) {
		if (name == null)
			return null;

		if (sCommunicator == null)
			sCommunicator = new N2SCommunicator(null);
		name = name.trim();
		String idcode = null;
		if (name != null && name.length() != 0) {
			if (sNameMap != null)
				idcode = sNameMap.get(name);

			if (idcode == null) {
				idcode = sCommunicator.getIDCode(name);

				if (sCommunicator.hasConnectionProblem())
					return null;

				if (idcode != null) {
					if (sNameMap == null)
						sNameMap = new TreeMap<String,String>();
					sNameMap.put(name, idcode);
					}
				}

			if (idcode != null) {
				StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode);
				if (mol != null && mol.getAllAtoms() != 0)
					return mol;
				}
			}

		return null;
		}
	}
