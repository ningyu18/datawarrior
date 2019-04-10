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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.mm2.MM2Parameters;

/**
 * Strategy Pattern used to defined how keys are assigned to a specific atom
 */
public class PLConfigV2 extends PLConfig {
	
	
	public PLConfigV2() {
		super(2);
	}
	
	public void prepare(FFMolecule mol) {
		MM2Parameters.setAtomTypes(mol);
	}
	
	public String getParameterFile() {
		return "pl_interactions_v2.txt";			
	}
	
	public String getSuperKey(FFMolecule mol, int a) {		
		int an = mol.getAtomicNo(a);
		return an + "*" + (mol.getMM2AtomDescription(a)==null?"": mol.getMM2AtomDescription(a).replace(" ", ""));
	}
		
	/**
	 * Gets a subkey = superkey + " " + more info
	 * the character '-' is not allowed  
	 * @param mol
	 * @param a
	 * @return
	 */
	public String getSubKey(FFMolecule mol, int a)  {			
		String k1 = getSuperKey(mol, a);
		String info = getNeighborInfos(mol, a);
		return k1 + (info.length()>0? "_" + info:"");
	}
	
	
	/**
	 * Creates a code for each atom based on the neighboring atoms
	 * 
	 * NON AROMATIC ATOM
	 * The code equals neighbor1(neighborsOfNeighbor1)neighbor2(neighborsOfneighbor2)neighbor3(neighborsOfNeighbor3)HnumberOfH
	 * Ex:
	 *         C
	 *       /
	 * O = C
	 *       \
	 *         OH
	 * The code of O is "=C(CO)"
	 * The code of central C is "=OCO(H)"
	 * The code of OH is "C(=OC)H"
	 *        
	 * 
	 * 
	 * 
	 * AROMATIC ATOM
	 * The code is "{non C atoms in the ring if <=6}neighbors"
	 * Ex:
	 * 
	 * 	code of H - C - C5N is H1{N}
	 * 
	 * 
	 * @param mol
	 * @param a
	 * @return
	 */
	private String getNeighborInfos(FFMolecule mol, int a) {
		//Compute neighboring atoms
		int an = mol.getAtomicNo(a);
		if(an!=6 && an!=7 && an!=8 && an!=16) return "";
		
		if(mol.isAromaticAtom(a)) {			

			int size = 0;
			List<String> set = new ArrayList<String>();
			for(int ringNo: mol.getAtomToRings()[a]) {
				if(!mol.isAromaticRing(ringNo)) continue;
				for(int a2: mol.getAllRings().get(ringNo)) {
					size++;
					if(mol.getAtomicNo(a2)!=6 && mol.getAtomicNo(a2)!=7 && mol.getAtomicNo(a2)!=8) continue;
					set.add(getElement(mol, a2));
				}
			}
//			StringBuilder atomsInAromaticRing = new StringBuilder();
//			if(set.size()<=6) {
//				Collections.sort(set);
//				for (String s : set) {
//					atomsInAromaticRing.append(s);
//				}
//			}

			set.clear();			
			for (int i = 0; i < mol.getConnAtoms(a); i++) {
				int a2 = mol.getConnAtom(a, i);
//				if(mol.getAtomicNo(a2)<=1 || a==a2 || mol.isAromaticAtom(a2)) continue;
				if(mol.getAtomicNo(a2)<=1 || a==a2) continue;
				if(mol.getAtomicNo(a2)==8 || mol.getAtomicNo(a2)==7) {
					set.add(getElement(mol, a2));
				}
			}
			Collections.sort(set);

			int nH = StructureCalculator.getImplicitHydrogens(mol, a) + StructureCalculator.getExplicitHydrogens(mol, a);
			StringBuilder sb2 = new StringBuilder();
			for (String s : set) sb2.append(s);
			return (nH>0? "H" + nH: "") + sb2 + "{" + (size>6?"": "" + size) + "}";

		} else {
			List<String> set1 = new ArrayList<>();
			for (int i = 0; i < mol.getConnAtoms(a); i++) {
				int a2 = mol.getConnAtom(a, i);
				if(mol.getAtomicNo(a2)<=1) continue;
//				if((mol.getAtomicNo(a)==8 || mol.getAtomicNo(a)==7) && mol.getAtomicNo(a2)==6) { //Polar - C - ... -> display C+neighbors
//					Set<String> set2 = new TreeSet<>();
//					for (int j = 0; j < mol.getConnAtoms(a2); j++) {
//						int a3 = mol.getConnAtom(a2, j);
//						if(a3==a || (mol.getAtomicNo(a3)!=7 && mol.getAtomicNo(a3)!=8)) continue;
//						set2.add((mol.getConnBondOrder(a2, j)==1?"": "=")+getElement(mol, a3));
//					}
////					Collections.sort(set2);
//					StringBuilder sb2 = new StringBuilder();
//					for (String s : set2) sb2.append(s);
//					set1.add((mol.getConnBondOrder(a, i)==1?"": "=") + getElement(mol, a2) + (sb2.length()>0? "(" + sb2 + ")":""));
//				} else 
				if(mol.getAtomicNo(a2)==8 || mol.getAtomicNo(a2)==7 || mol.getAtomicNo(a2)==6) { //display neighbors only
					set1.add((mol.getConnBondOrder(a, i)==1?"": "=") + getElement(mol, a2));
				}
			}
			StringBuilder sb1 = new StringBuilder();
			int nH = StructureCalculator.getImplicitHydrogens(mol, a) + StructureCalculator.getExplicitHydrogens(mol, a);
			sb1.append(nH>0? "H" + nH: "");
			Collections.sort(set1);
			for (String s : set1) sb1.append(s);
			
			return sb1.toString();
		}
	}

	public boolean isHydrogen(String key) {
		return key.startsWith("1*"); 
	}

	@Override
	public boolean isSymetric() {
		return true; //normal version is symetric
	}
	
}
