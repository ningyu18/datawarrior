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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.chem.calculator.MoleculeGrid;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.mm2.MM2Config;

/**
 * 
 */
public class FFUtils {

					
		
	/**
	 * Returns the DockScore
	 * Precondition: call computeProperties or this may return a nullpointerexception
	 * @param mol
	 * @return
	 */
	public static double getDockScore(FFMolecule mol) {
		int constraintsScore = mol.getAuxiliaryInfo("constraints")!=null? ((Integer)mol.getAuxiliaryInfo("constraints")) * 50: 0;
		if(mol.getAuxiliaryInfo("IE")==null || mol.getAuxiliaryInfo("SE")==null) {System.err.println("IE is null"); return 0;}
		try {
			return (Double) mol.getAuxiliaryInfo("IE") + (Double) mol.getAuxiliaryInfo("SE") + constraintsScore * 50;
		} catch (Exception e) {
			System.err.println("DockScore cannot be cast to a double: SE="+mol.getAuxiliaryInfo("SE")+": "+mol.getAuxiliaryInfo("SE").getClass()+ " IE="+mol.getAuxiliaryInfo("IE")+": "+mol.getAuxiliaryInfo("IE").getClass());
			return 0;
		}
	}
	
	public static double getSE(FFMolecule mol) {
		if(mol.getAuxiliaryInfo("SE")==null) {System.err.println("SE is null"); return 0;}
		try {
			return (Double) mol.getAuxiliaryInfo("SE");
		} catch (Exception e) {
			System.err.println("SE cannot be cast to a double: "+mol.getAuxiliaryInfo("SE")+": "+mol.getAuxiliaryInfo("SE").getClass());
			return 0;
		}
	}
	
	/**
	 * Precondition: computeProperties was called or this may return a nullexception
	 * @param mol
	 * @return
	 */
	public static double getLigandEfficiency(FFMolecule mol) {		
		return getActivityScore(mol) / Math.sqrt((Integer) mol.getAuxiliaryInfo("atoms"));
	}
		
	public static double getActivityScore(FFMolecule mol) {		
		return getDockScore(mol);
//		return (Double) mol.getAuxiliaryInfo("IE") + (mol.getAuxiliaryInfo("constraints")!=null? ((Integer)mol.getAuxiliaryInfo("constraints")) * 50: 0);
//		return (Double) mol.getAuxiliaryInfo("IE") + (mol.getAuxiliaryInfo("constraints")!=null? ((Integer)mol.getAuxiliaryInfo("constraints")) * 50: 0);
	}
		
		
	public static void computeProperties(FFMolecule mol) {
		computeProperties(mol, new MM2Config());
	}
	
	public static void computeProperties(FFMolecule mol, FFConfig config) {
		FFMolecule l = StructureCalculator.extractLigand(mol);
		if(l==null) return;
		ForceField f = new ForceField(mol, config);
		
		List<int[]> hbonds = new ArrayList<int[]>();

		//double surface = SurfaceCalculator.getLigandSurface(mol);
		//double SAS = SurfaceCalculator.calculateSAS(mol, null);
		mol.setAuxiliaryInfo("atoms", l.getAtoms());
		mol.setAuxiliaryInfo("IE", f.getTerms().getInteractionEnergy());
		mol.setAuxiliaryInfo("SE", f.getTerms().getStructureEnergy());
		mol.setAuxiliaryInfo("HB", getHBonds(mol, hbonds));
		//mol.setAuxiliaryInfo("Hbonds", hbonds);
		
		
		//mol.setAuxiliaryInfo("SEnoH", f.getTerms().getStructureEnergyNoH());
		//mol.setData("relaxSE", getRelaxedSE(mol));
		//mol.setAuxiliaryInfo("surface", surface);
		//mol.setData("volume", SurfaceCalculator.getVolume(l));
		//mol.setAuxiliaryInfo("SAS", SAS);
		//mol.setAuxiliaryInfo("buried", surface-SAS>0?(surface-SAS)/surface:0);
		//mol.setAuxiliaryInfo("rot", StructureCalculator.getNRotatableBonds(mol));
		//mol.setData("close", evalCloseInteractions(mol));
		//mol.setData("close2", evalClose2Interactions(mol));
		//mol.setAuxiliaryInfo("repu", evalRepulsiveInteractions(mol));
		//mol.setData("SEsmooth", f.getTerms().getStructureEnergySmooth());
		//mol.setData("SEvdw", f.getTerms().getVDW());
		//mol.setData("flex", StructureCalculator.evaluateFlexibility(l));
		//mol.setData("hydrophobic", SurfaceCalculator.calculateHydrophobic(mol, null));
		//mol.setAuxiliaryInfo("compact", getCompactness(mol, 5));
		//mol.setData("IE5", f.getTerms().getInteractionEnergy(5));
		//mol.setData("IE55", f.getTerms().getInteractionEnergy(5.5));
		
		StructureCalculator.deleteHydrogens(l);
		mol.setAuxiliaryInfo("weight", l.toExtendedMolecule().getMolweight());
		
	}
	
	private static double getEnergy(int donorAtomicNo, int acceptorAtomicNo) {
		if(donorAtomicNo==7 && acceptorAtomicNo==7) return -3.1; 		//NH -- N
		else if(donorAtomicNo==7 && acceptorAtomicNo==8) return -1.9; //NH -- O
		else if(donorAtomicNo==8 && acceptorAtomicNo==7) return -6.9; //OH -- N
		else if(donorAtomicNo==8 && acceptorAtomicNo==8) return -5.0; //OH -- O
		return 0;
	}
	
	public static double getHBonds(FFMolecule mol, List<int[]> hbonds) {
		MoleculeGrid grid = new MoleculeGrid(mol);

		double res = 0;
		for (int a=0; a<mol.getNMovables(); a++) {			
			if(!mol.isAtomFlag(a, FFMolecule.LIGAND)) continue;
			int atomicNo = mol.getAtomicNo(a);
			if(atomicNo!=7 && atomicNo!=8) continue;
				
			Set<Integer> set = grid.getNeighbours(mol.getCoordinates(a), 3.8);
			
			//Find ligand donor to protein acceptors
			double v1 = 0;
			int[] b1 = null;
			for (int i=0; i<mol.getAllConnAtoms(a); i++) {
				int a2 = mol.getConnAtom(a, i);
				if(mol.getAtomicNo(a2)==1) {
					for (int a3 : set) {
						int atomicNo2 = mol.getAtomicNo(a3);
						if(mol.isAtomFlag(a3, FFMolecule.LIGAND)) continue;
						if(atomicNo2!=7 && atomicNo2!=8) continue; 
						double dist =  mol.getCoordinates(a2).distance(mol.getCoordinates(a3));
						double angle = GeometryCalculator.getAngle(mol, a, a2, a3);
						if(Math.abs(angle-2*Math.PI/3)<Math.PI/12 && Math.abs(dist-1.97)<.5) {
							v1 = Math.min(v1, getEnergy(atomicNo, atomicNo2));
							b1 = new int[] {a, a2};
							
							res+=v1;
							hbonds.add(b1);
						}
					}
					
				}
			}					
				
			//Find ligand acceptor to protein donor
			double v2 = 0;
			int[] b2 = null;
			for (int a2 : set) {
				int atomicNo2 = mol.getAtomicNo(a2);
				if(mol.isAtomFlag(a2, FFMolecule.LIGAND)) continue;
				if(atomicNo2!=7 && atomicNo2!=8) continue; 
				if(StructureCalculator.getMaxFreeValence(mol, a2)==0) continue; 
				double dist =  mol.getCoordinates(a).distance(mol.getCoordinates(a2));
				if(Math.abs(dist-2.6)<.4) { //2.2<..<2.8
					v2 = Math.min(v2, getEnergy(atomicNo2, atomicNo));
					b2 = new int[] {a2, a};

					res+=v2;
					hbonds.add(b2);
				}
			}
		}
		return res;
	}
	
	public static Comparator<FFMolecule> ACTIVITYSCORE_COMPARATOR = new Comparator<FFMolecule>() {
		@Override
		public int compare(FFMolecule m1, FFMolecule m2) {			
			return getActivityScore(m1)>getActivityScore(m2)?1:-1;
		}		
	};
	public static Comparator<FFMolecule> LIGAND_EFF_COMPARATOR = new Comparator<FFMolecule>() {
		@Override
		public int compare(FFMolecule m1, FFMolecule m2) {			
			return getLigandEfficiency(m1)>getLigandEfficiency(m2)?1:-1;
		}		
	};
	public static Comparator<FFMolecule> DOCKSCORE_COMPARATOR = new Comparator<FFMolecule>() {
		@Override
		public int compare(FFMolecule m1, FFMolecule m2) {			
			return getDockScore(m1)>getDockScore(m2)?1:-1;
		}		
	};	
	public static Comparator<FFMolecule> SE_COMPARATOR = new Comparator<FFMolecule>() {
		@Override
		public int compare(FFMolecule m1, FFMolecule m2) {			
			return getSE(m1)>getSE(m2)?1:-1;
		}		
	};	
}
