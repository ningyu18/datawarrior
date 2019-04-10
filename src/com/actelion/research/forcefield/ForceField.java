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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.FFConfig.Mode;
import com.actelion.research.forcefield.mm2.MM2Config;
import com.actelion.research.forcefield.mm2.MM2TermList;
import com.actelion.research.forcefield.mmff.MMFFConfig;
import com.actelion.research.forcefield.mmff.MMFFTermList;

/**
 * Generic Forcefield implementation, using MM2 by default, with its standard parameters 
 * 
 */
public class ForceField implements Cloneable {
	

	private static boolean defaultMMFF = true; 
	private FFMolecule mol;	
	private TermList terms = null;

	public ForceField(FFMolecule mol) {		
		this(mol, Mode.OPTIMIZATION);
	}
	
	public ForceField(FFMolecule mol, Mode mode) {
		this(mol, defaultMMFF? new MMFFConfig(mode):  new MM2Config(mode));
	}
	
	public ForceField(FFMolecule mol, FFConfig config) {
		this(mol, (config instanceof MMFFConfig)? new MMFFTermList((MMFFConfig)config): new MM2TermList((MM2Config)config));
	}
	
	public ForceField(FFMolecule mol, TermList terms) {
		if(mol.getAllAtoms()>28000) throw new IllegalArgumentException("The molecule is too big: "+mol.getAllAtoms()+" atoms");
		this.mol = mol;
		this.terms = terms;
		terms.prepareMolecule(mol); 
	}
	
	@Override
	public String toString() {
		initTerms();
		return terms.toString();
	}
	
	public String getEnergyBreakdown() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<getTerms().size(); i++) {
			sb.append(getTerms().get(i)+System.getProperty("line.separator"));
		}
		sb.append(System.getProperty("line.separator"));
		sb.append(toString());
		return sb.toString();
	}
	
	public String getEnergy(boolean mainTermOnly) {
		
		StringBuffer sb = new StringBuffer();
		//PriorityQueue all = new PriorityQueue();
		List<AbstractTerm> all = new ArrayList<AbstractTerm>(); 
		for(int i=0; i<getTerms().size(); i++) {
			AbstractTerm t = getTerms().get(i);
			double v = t.getFGValue(null);
			if(Math.abs(v)>1 || !mainTermOnly) all.add(t/*, -Math.abs(v)*/);
		}

		for(int i=0; i<all.size(); i++) {
			AbstractTerm t = (AbstractTerm) all.get(i);
			sb.append(t+System.getProperty("line.separator"));
		}
		sb.append(System.getProperty("line.separator"));
		//sb.append(toString());
		return sb.toString();
	}
	
	public String getEnergyInterMolecularMain() {
		
		StringBuffer sb = new StringBuffer();
		
		final Map<AbstractTerm, Double> all = new HashMap<AbstractTerm, Double>();
		for(int i=0; i<getTerms().size(); i++) {			
			AbstractTerm t = getTerms().get(i);
			if(!t.isExtraMolecular()) continue;
			double v = t.getFGValue(null);
			if(Math.abs(v)>1) all.put(t, -Math.abs(v));
		}
		List<AbstractTerm> terms = new ArrayList<>();
		Collections.sort(terms, new Comparator<AbstractTerm>() {
			@Override
			public int compare(AbstractTerm o1, AbstractTerm o2) {
				return all.get(o1).compareTo(all.get(o2));
			}
		});
		
		for(AbstractTerm t: terms) {
			sb.append(t+System.getProperty("line.separator"));
		}
		sb.append(System.getProperty("line.separator"));
		sb.append(toString());
		return sb.toString();
	}	
	
	public void initTerms() {
		terms.init(mol);
	}
	
	
	public FFMolecule getMolecule() {
		return mol;
	}

	/**
	 * @return
	 */
	public TermList getTerms() {
		if(terms.size()==0) {
			initTerms();
		} 
		return terms;
	}

	/**
	 * @return
	 */
	public FFConfig getConfig() {
		return terms.getConfig();
	}

//
//	/**
//	 * Recreates and preoptimize the hydrogens. This function will
//	 * remove all extra hydrogens and add all missing hydrogens
//	 * Warning: this function changes the atom's order
//	 * @param forcefield
//	 * @param atms
//	 */
//	public void recreateHydrogens(int[] atms) {
//		FFMolecule mol = getMolecule();
//		
//		if(atms!=null) {
//			//Delete the current hydrogens (and lone pairs) 
//			List<Integer> atomsToBeDeleted = new ArrayList<Integer>();
//			for(int k=0; k<atms.length; k++) {
//				int atm = atms[k];
//				if(atm<0 || mol.isAtomFlag(atm, FFMolecule.RIGID)) continue;
//				
//				int nH = StructureCalculator.getImplicitHydrogens(mol, atm);
//				
//				for (int i = 0; nH<0 && i<mol.getAllConnAtoms(atm) ; i++) {
//					if(mol.getAtomicNo(mol.getConnAtom(atm, i))<=1) {
//						atomsToBeDeleted.add(new Integer(mol.getConnAtom(atm, i)));
//						nH++;
//					} 
//				}
//				
//				//Add and preoptimize the new hydrogens
//				for(int i=0; i<nH; i++) {
//					int a = mol.addAtom(1);
//					mol.setAtomFlags(a, mol.getAtomFlags(atm));
//					mol.setAtomFlag(a, FFMolecule.PREOPTIMIZED, false);
//					mol.addBond(atm, a, 1);
//				}
//			}
//			Collections.sort(atomsToBeDeleted);			
//			for(int i=atomsToBeDeleted.size()-1; i>=0; i--) {
//				mol.deleteAtom(atomsToBeDeleted.get(i));
//			}
//		} else {
//			int N = Math.min(mol.getNMovables(), mol.getAllAtoms());
//			for(int i=N-1; i>=0; i--) {
//				if(mol.getAtomicNo(i)<=1) mol.deleteAtom(i);
//			}
//			StructureCalculator.addHydrogens(mol);
//		}
//		init();
//		PreOptimizer.preOptimize(this);
//	}
	
	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ForceField clone()  {
		
		try {
			FFMolecule m = new FFMolecule(mol);
			ForceField copy = new ForceField(m, terms);
			if(terms!=null) {
				copy.terms = terms.clone();
				copy.terms.setMolecule(m);
			}
			
			return copy;
		} catch (Exception e) {
			e.printStackTrace();
			return this;
		}
		/*
		try {
			return (ForceField) super.clone();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}*/
	}

	public static boolean isDefaultMMFF() {
		return defaultMMFF;
	}
	public static void setDefaultMMFF(boolean defaultMMFF) {
		ForceField.defaultMMFF = defaultMMFF;
	}

}
