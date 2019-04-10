package com.actelion.research.chem.name;

import com.actelion.research.chem.StereoMolecule;

/**
 * Created by thomas on 7/13/17.
 */
public interface INameResolver {
	public StereoMolecule resolveName(String name);
}
