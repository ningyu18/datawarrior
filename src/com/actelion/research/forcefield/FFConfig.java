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
package com.actelion.research.forcefield;

import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;

/**
 * Generic config class to be parameterize the forcefield 
 *
 */
public class FFConfig implements Cloneable {
	
	public static enum Mode {
		PREOPTIMIZATION,
		OPTIMIZATION,
		DOCKING
	}
	
	private String name;
	
	/**Ligand preparation?*/
	private boolean prepareMolecule = true;
	
	/**Non Bonded treshold*/
	private double maxDistance = 10;

	// ////////// Protein Ligand interactions ///////////////
	/** The maximum distance considered to add a intermolecular term */
	private double maxPLDistance = 50;

	/** E = SE (structure energy) + proteinLigandFactor * IE (interaction energy) */
	private double proteinLigandFactor;
	
	/** Do we use the ProteinLigand stats */
	private boolean usePLInteractions = true;	
	private boolean useHydrogenOnProtein = false;
	
	private double dielectric = 1.0; //1 in vaccum, 78 in water.

	

	public FFConfig(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public final boolean isPrepareMolecule() {
		return prepareMolecule;
	}

	/**
	 * Should we prepare the ligand (add hydrogens, lone pairs?). Should always be true, except in Preoptimize mode
	 * @param b
	 */
	public final void setPrepareMolecule(boolean b) {
		prepareMolecule = b;
	}
	
	public final double getMaxPLDistance() {
		return maxPLDistance;
	}

	public final void setMaxPLDistance(double d) {
		maxPLDistance = d;
	}

	public boolean isUsePLInteractions() {
		return usePLInteractions;
	}

	public void setUsePLInteractions(boolean b) {
		usePLInteractions = b;
	}

	public boolean isUseHydrogenOnProtein() {
		return useHydrogenOnProtein;
	}
	
	public void setUseHydrogenOnProtein(boolean useHydrogenOnProtein) {
		this.useHydrogenOnProtein = useHydrogenOnProtein;
	}
	
	public ClassInteractionStatistics getClassStatistics() {
		return ClassInteractionStatistics.getInstance(useHydrogenOnProtein);
	}

	public double getProteinLigandFactor() {
		return proteinLigandFactor;
	}
	
	public void setProteinLigandFactor(double proteinLigandFactor) {
		this.proteinLigandFactor = proteinLigandFactor;
	}
	
	public double getDielectric() {
		return dielectric;
	}

	public void setDielectric(double dielectric) {
		this.dielectric = dielectric;
	}


	public void setMode(Mode mode) {
		if(mode==Mode.PREOPTIMIZATION) {
			setPrepareMolecule(false);
			setMaxPLDistance(0);
			setMaxDistance(5);
			setUsePLInteractions(false);
			setUseHydrogenOnProtein(false);
		} else if(mode==Mode.OPTIMIZATION) {
			setMaxDistance(20);
		} else if(mode==Mode.DOCKING) {
			setMaxDistance(100);
			setUseHydrogenOnProtein(true);
			setDielectric(1.5);
		}
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public FFConfig clone() {
		try {
			return (FFConfig) super.clone();
		} catch(CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public final double getMaxDistance() {
		return maxDistance;
	}

	public final void setMaxDistance(double d) {
		maxDistance = d;
	}


	
}
