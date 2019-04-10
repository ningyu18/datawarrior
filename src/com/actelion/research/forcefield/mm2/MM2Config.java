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
package com.actelion.research.forcefield.mm2;

import com.actelion.research.forcefield.FFConfig;

/**
 * MM2Config is used to specify which terms of the forcefield have to be used
 * 
 * <pre>
 * The MM2 Terms used for the conformation of a structure are
 *  - bonds
 *  - angles
 *  - out of plane angles
 *  - torsion
 *  - vdw                 --
 *  - dipoles              | _ if the atoms are separated by less than maxDistance
 *  - charge               |     
 *  - charge - dipole     --
 * 
 * 
 * The intermolecular interactions are set using:
 *  - usePLStats  (statistics)
 *  - usePLDipole (include vdw, dipoles from MM2, much slower)
 * Those terms are added only if the distance is less than maxPLDistance
 * </pre>
 * 
 * 
 */
public class MM2Config extends FFConfig {

	// ////////////MM2 Terms ////////////////////////////////
	private boolean useOutOfPlaneAngle = true;
	private boolean useStretchBend = true;
	private boolean useTorsion = true;
	private int vdwType = 1; // 0 -> normal, 1 ->4-8 with H, 2->4-8 potential without H
	private boolean useDipole = true;
	private boolean useCharge = true;
	private boolean useChargeDipole = true;
	private boolean useOrbitals = true;

	public MM2Config() {
		this(Mode.OPTIMIZATION);
	}
	
	public MM2Config(Mode mode) {
		super("MM2");
		setProteinLigandFactor(.9);
		setMode(mode);
	}
	
	public final boolean isUseDipole() {
		return useDipole;
	}

	public final boolean isUseOutOfPlaneAngle() {
		return useOutOfPlaneAngle;
	}

	public final void setUseDipole(boolean b) {
		useDipole = b;
	}

	public final void setUseOutOfPlaneAngle(boolean b) {
		useOutOfPlaneAngle = b;
	}

	public final boolean isUseStretchBend() {
		return useStretchBend;
	}

	public final void setUseStretchBend(boolean b) {
		useStretchBend = b;
	}

	public final boolean isUseCharge() {
		return useCharge;
	}

	public final boolean isUseChargeDipole() {
		return useChargeDipole;
	}

	public final void setUseCharge(boolean b) {
		useCharge = b;
	}

	public final void setUseChargeDipole(boolean b) {
		useChargeDipole = b;
	}
	
	public boolean isUseOrbitals() {
		return useOrbitals;
	}

	public void setUseOrbitals(boolean useOrbitals) {
		this.useOrbitals = useOrbitals;
	}

	public int getVdwType() {
		return vdwType;
	}

	public void setVdwType(int vdwType) {
		this.vdwType = vdwType;
	}

	public void setUseTorsion(boolean useTorsion) {
		this.useTorsion = useTorsion;
	}

	public boolean isUseTorsion() {
		return useTorsion;
	}
	
	@Override
	public void setMode(Mode mode) {
		super.setMode(mode);
		if(mode==Mode.PREOPTIMIZATION) {
			setUseDipole(false);
			setUsePLInteractions(false);
			setUseChargeDipole(false);
			setUseCharge(false);
//			setUseOutOfPlaneAngle(false);
			setUseOrbitals(false);
		} else if(mode==Mode.OPTIMIZATION) {			
		} else if(mode==Mode.DOCKING) {			
		}
		//DockFInal
	} 

}
