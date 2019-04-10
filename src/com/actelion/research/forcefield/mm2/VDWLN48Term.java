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

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.TermList;
import com.actelion.research.forcefield.mm2.MM2Parameters.SingleVDWParameters;
import com.actelion.research.forcefield.mm2.MM2Parameters.VDWParameters;

/**
 * VDW interactions implementation using the Lennard Jones formula.
 * The advantage of LN over Buck is that 
 * 1. the derivate is continuous and
 * 2. the function is faster to calculate
 *  
 * energy = epsilon * (p12 - 2 * p6) where p = radmin / r
 * 
 * @author freyssj
 */
public final class VDWLN48Term extends AbstractTerm {
	private final static MM2Parameters parameters = MM2Parameters.getInstance();

	private final static double CUTOFF = 8.0;
	private final double VCUTOFF;
	
	//Parameters used 
	private final double epsilon, radmin;
	private final double r1, r2;
	private final int iv, kv;
	private double energy;	

	
	private VDWLN48Term(FFMolecule mol, int[] atoms, double epsilon, double radmin, double r1, double r2, int iv, int kv) {
		super(mol, atoms);
		this.epsilon = epsilon / 1.169;//* 1.169; //-290000 * exp(-12.5) - 2.25
		this.radmin = radmin * Math.pow(2, 1/6.0);
		this.r1 = r1;
		this.r2 = r2;
		this.iv = iv;
		this.kv = kv;
		
		final double p2 = radmin * radmin / (CUTOFF * CUTOFF);
		final double p4 = p2 * p2;
		final double p8 = p4 * p4;
		VCUTOFF = epsilon * (p8 - 2 * p4);
	}
	
	public static VDWLN48Term create(TermList tl, int a1, int a2) {
		FFMolecule mol = tl.getMolecule();
		int n1 = mol.getMM2AtomType(a1);
		int n2 = mol.getMM2AtomType(a2);
		SingleVDWParameters param1 = parameters.getSingleVDWParameters(n1);
		SingleVDWParameters param2 = parameters.getSingleVDWParameters(n2);
		if(param1==null||param2==null) return null;

		double r1 = param1.reduct;
		double r2 = param2.reduct;
				
		VDWParameters paramPair = parameters.getVDWParameters(n1, n2);
		double epsilon, radmin;
		if(paramPair!=null) {
			radmin = paramPair.radius;
			epsilon = paramPair.esp;
		}  else {					
			radmin = (param1.radius + param2.radius);			
			epsilon = Math.sqrt(param1.epsilon * param2.epsilon);				
		}
		
		int[] atoms;
		int iv = -1, kv = -1;
		if(mol.getAtomicNo(a1)==1 && mol.getAtomicNo(a2)==1) {
			iv = mol.getConnAtom(a1, 0);
			kv = mol.getConnAtom(a2, 0);
			atoms = new int[]{a1, a2, iv, kv};			
		} else if(mol.getAtomicNo(a1)==1) {
			iv = mol.getConnAtom(a1, 0);
			atoms = new int[]{a1, a2, iv};			
		} else if(mol.getAtomicNo(a2)==1) {
			kv = mol.getConnAtom(a2, 0);
			atoms = new int[]{a1, a2, kv};			
		} else {
			atoms = new int[]{a1, a2};
		}
		
		return new VDWLN48Term(mol, atoms, epsilon, radmin, r1, r2, -1, -1);
	}
	
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		Coordinates ci = getMolecule().getCoordinates(atoms[0]);
		Coordinates ck = getMolecule().getCoordinates(atoms[1]);
		Coordinates cci = null, cck = null;
		if(iv>=0) {
			cci = getMolecule().getCoordinates(iv);
			ci = new Coordinates(
				(ci.x-cci.x)*r1+cci.x,
				(ci.y-cci.y)*r1+cci.y,
				(ci.z-cci.z)*r1+cci.z);			
		}
		if(kv>=0) {
			cck = getMolecule().getCoordinates(kv);
			ck = new Coordinates(
				(ck.x-cck.x)*r2+cck.x,
				(ck.y-cck.y)*r2+cck.y,
				(ck.z-cck.z)*r2+cck.z);
		}
		
		double rik2 = ci.distSquareTo(ck);
		if(rik2<.1) rik2 = .1;
				
		if(rik2>CUTOFF*CUTOFF) {
			energy = 0;
		} else {
			final Coordinates cr = ci.subC(ck);
			final double rik = Math.sqrt(rik2);
			final double p2 = radmin * radmin / rik2;
			final double p4 = p2 * p2;
			final double p8 = p4 * p4;
		
			energy = epsilon * (p8 - 2 * p4) - VCUTOFF;

			/*
			double taper, dtaper;
			if(rik2>TAPER_CUTOFF * TAPER_CUTOFF) { 
				//close to the cutoff distance, apply smoothing effect
				taper = MathUtils.evaluateTaper(TAPER_COEFFS, rik);
				dtaper = MathUtils.evaluateDTaper(TAPER_COEFFS, rik);				
			} else {
				taper = 1;
				dtaper = 0; 
			}
			*/
			if(gradient!=null) {
				double de = epsilon * (p8-p4) * (-8/rik);
				
				//if(taper!=1.0) de = energy * dtaper + de * taper;
		  
				double deddt = de/rik;
				Coordinates g0, g1;

				if(iv>=0 && kv>=0) {
					g0 = cr.scaleC(deddt*r1); 
					g1 = cr.scaleC(-deddt*r2);
					if(atoms[2]<gradient.length) gradient[atoms[2]].add(cr.scaleC(deddt*(1-r1)));
					if(atoms[3]<gradient.length) gradient[atoms[3]].add(cr.scaleC(-deddt*(1-r2)));
				} else if(iv>=0) {
					g0 = cr.scaleC(deddt*r1);
					g1 = cr.scaleC(-deddt);
					if(atoms[2]<gradient.length) gradient[atoms[2]].add(cr.scaleC(deddt*(1-r1)));
				} else if(kv>=0) {
					g0 = cr.scaleC(deddt);
					g1 = cr.scaleC(-deddt*r2);
					if(atoms[2]<gradient.length) gradient[atoms[2]].add(cr.scaleC(-deddt*(1-r2)));
				} else {
					g0 = cr.scaleC(deddt);
					g1 = cr.scaleC(-deddt);
				}						
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(g0);
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(g1);
			}
			
			//energy *= taper;
			
		}
		return energy;
			
	}
	
	@Override
	public final String toString() {
		return "VDW-LN      " +new DecimalFormat("00").format(atoms[0])+" - "+new DecimalFormat("00").format(atoms[1])+"    " + new DecimalFormat("0.0000").format(radmin) + " "+new DecimalFormat("0.0000").format(getMolecule().getCoordinates(atoms[0]).subC(getMolecule().getCoordinates(atoms[1])).dist())+ " -> "+new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return epsilon>0;
	}


}
