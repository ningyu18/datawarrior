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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.mm2.MM2Parameters;
import com.actelion.research.forcefield.mmff.MMFFParameters;

/**
 * This Singleton encapsulates the statistics relative to the Protein Ligand Interaction.
 * It consists of:
 * - the mapping from alphanumeric description in the pl_interation file to a numerical classId
 * - the mapping from classId to the parentClassId (ex N*Amine in Ring to N)
 * - the mapping from proteinClassId - ligandClassId to the interaction function
 * @author freyssj
 */
public class ClassInteractionStatistics {

	/**
	 * Classes that are used for compatibility reasons, so that atom types are not changed with updated statistics.
	 */

	/**
	 * Singleton instances
	 */
	private static final Map<String,ClassInteractionStatistics> instances = new HashMap<>();

	private PLConfig config;
	private Map<String, Integer> keyToClassId = new HashMap<>();
	private int classId2parentClassId[];
	private Map<Integer, PLFunction> proteinIdLigandId2function = new HashMap<>();



	public static ClassInteractionStatistics getInstance () {
		return getInstance(PLConfig.LAST_VERSION);
	}

	public static ClassInteractionStatistics getInstance (int version) {
		return getInstance(version, false);
	}

	public static ClassInteractionStatistics getInstance (boolean includeHydrogen) {
		return getInstance(PLConfig.LAST_VERSION, includeHydrogen);
	}
	public static ClassInteractionStatistics getInstance (int version, boolean includeHydrogen) {
		ClassInteractionStatistics instance = instances.get(version+"_"+includeHydrogen);
		if(instance==null) {
			synchronized (instances) {
				if(instance==null) {
					try {
						instance = new ClassInteractionStatistics(version, includeHydrogen);
						instances.put(version+"_"+includeHydrogen, instance);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return instance;
	}

	public int getNClasses() {
		return classId2parentClassId.length;
	}

	private ClassInteractionStatistics(int version, boolean includeHydrogens) {

		config = PLConfig.getInstance(version);

		//Read Interaction File
		StringBuilder sb = new StringBuilder();
		PLFunctionFile file = PLFunctionFile.read(config, includeHydrogens);
		PLFunctionSplineCalculator.calculateSplines(file);

		//0.Retrieves memorized classes
		int clas = 0;
		if(version==1) {
			for (String key: new String[]{"12*MG", "12*MG_", "13*ALTRIGPLANAR", "15*PPHOSPHATE", "15*PPHOSPHATE_", "16*SSULFONE", "16*SSULFONE_=O=OCC", "16*SSULFONE_=O=OCN", "16*SSULFONE_=O=OCO", "16*SSULFONE_=O=ONO", "16*SSULFONE_=O=OOO", "16*SSULFOXIDE", "16*STHIOCARBONYL", "16*STHIOETHER", "16*STHIOETHER_CC", "16*STHIOETHER_CCC", "16*STHIOETHER_CS", "16*STHIOL", "16*STHIOL_H1C", "16*STHIOPHENE", "16*STHIOPHENE_{NS}", "16*STHIOPHENE_{S}", "17*CL", "17*CL_", "19*K", "23*VTRIGPLANAR", "25*MN", "25*MN_", "26*FEOCTAHEDRAL", "26*FEOCTAHEDRAL_", "30*ZNTRIGPLANAR", "30*ZNTRIGPLANAR_", "35*BR", "35*BR_", "39*YTRIGPLANAR", "42*MOTETRAHEDRAL", "5*BTRIGPLANAR", "53*I", "53*I_", "6*CALKANE", "6*CALKANE_CCCC", "6*CALKANE_CCCN", "6*CALKANE_CCCO", "6*CALKANE_CCCS", "6*CALKANE_CCOO", "6*CALKANE_CFFF", "6*CALKANE_H1CCC", "6*CALKANE_H1CCF", "6*CALKANE_H1CCN", "6*CALKANE_H1CCO", "6*CALKANE_H1CCS", "6*CALKANE_H1CNN", "6*CALKANE_H1CNO", "6*CALKANE_H1COO", "6*CALKANE_H1COS", "6*CALKANE_H1NNN", "6*CALKANE_H1NNO", "6*CALKANE_H2CC", "6*CALKANE_H2CN", "6*CALKANE_H2CO", "6*CALKANE_H2CP", "6*CALKANE_H2CS", "6*CALKANE_H2NN", "6*CALKANE_H2OO", "6*CALKANE_H2PP", "6*CALKANE_H3C", "6*CALKANE_H3N", "6*CALKANE_H3O", "6*CALKANE_H3S", "6*CALKANE_H4", "6*CALKENE", "6*CALKENE_=CCC", "6*CALKENE_=CCN", "6*CALKENE_=CCO", "6*CALKENE_=CCS", "6*CALKENE_Br{}", "6*CALKENE_Cl{N}", "6*CALKENE_Cl{}", "6*CALKENE_C{NNN}", "6*CALKENE_C{NN}", "6*CALKENE_C{NO}", "6*CALKENE_C{NS}", "6*CALKENE_C{N}", "6*CALKENE_C{O}", "6*CALKENE_C{S}", "6*CALKENE_C{}", "6*CALKENE_F{}", "6*CALKENE_H1=CC", "6*CALKENE_H1=CN", "6*CALKENE_H1=CO", "6*CALKENE_H1{NNN}", "6*CALKENE_H1{NN}", "6*CALKENE_H1{NO}", "6*CALKENE_H1{NS}", "6*CALKENE_H1{N}", "6*CALKENE_H1{O}", "6*CALKENE_H1{S}", "6*CALKENE_H1{}", "6*CALKENE_H2=C", "6*CALKENE_I{}", "6*CALKENE_N{NNN}", "6*CALKENE_N{NN}", "6*CALKENE_N{NS}", "6*CALKENE_N{N}", "6*CALKENE_N{}", "6*CALKENE_O{NNN}", "6*CALKENE_O{NN}", "6*CALKENE_O{N}", "6*CALKENE_O{}", "6*CALKENE_S{NN}", "6*CALKENE_S{S}", "6*CALKENE_S{}", "6*CALKENE_{NNNNN}", "6*CALKENE_{NNNN}", "6*CALKENE_{NNN}", "6*CALKENE_{NNS}", "6*CALKENE_{NN}", "6*CALKENE_{NO}", "6*CALKENE_{NS}", "6*CALKENE_{N}", "6*CALKENE_{O}", "6*CALKENE_{S}", "6*CALKENE_{}", "6*CALKYNE", "6*CALKYNE_#CC", "6*CALKYNE_#NC", "6*CCARBONYL", "6*CCARBONYL_=NCC", "6*CCARBONYL_=NCN", "6*CCARBONYL_=NCO", "6*CCARBONYL_=NNN", "6*CCARBONYL_=OCC", "6*CCARBONYL_=OCN", "6*CCARBONYL_=OCO", "6*CCARBONYL_=OCS", "6*CCARBONYL_=ONN", "6*CCARBONYL_=ONO", "6*CCARBONYL_H1=NC", "6*CCARBONYL_H1=OC", "6*CCUMULENE", "6*CCYCLOPROPANE", "6*CCYCLOPROPANE_H1CCC", "6*CCYCLOPROPANE_H1CCN", "6*CCYCLOPROPANE_H1CCO", "6*CCYCLOPROPANE_H2CC", "6*CCYCLOPROPENE", "64*???", "7*NAMIDE", "7*NAMIDE_C(=NC)C(C)C(C)", "7*NAMIDE_C(=OC)C(C)C(C)", "7*NAMIDE_C(=OC)C(C)C(CC)", "7*NAMIDE_H1C()C(=OC)", "7*NAMIDE_H1C(=CC)C(=NC)", "7*NAMIDE_H1C(=CC)C(=NN)", "7*NAMIDE_H1C(=CC)C(=OC)", "7*NAMIDE_H1C(=CC)C(=ON)", "7*NAMIDE_H1C(=NC)C(C)", "7*NAMIDE_H1C(=OC)C(C)", "7*NAMIDE_H1C(=OC)C(CC)", "7*NAMIDE_H1C(=OC)C(CCC)", "7*NAMIDE_H1C(=OC)O", "7*NAMIDE_H1C(=ON)C(C)", "7*NAMIDE_H1C(=ON)C(CC)", "7*NAMIDE_H1C(=OO)C(CC)", "7*NAMIDE_H2C(=NC)", "7*NAMIDE_H2C(=NN)", "7*NAMIDE_H2C(=OC)", "7*NAMIDE_H2C(=ON)", "7*NAMINE", "7*NAMINE_C()C(C)C(C)", "7*NAMINE_C(C)C(C)C(C)", "7*NAMINE_C(C)C(C)C(CC)", "7*NAMINE_H1C(C)C(C)", "7*NAMINE_H1C(C)C(CC)", "7*NAMINE_H1C(CC)C(CC)", "7*NAMINE_H1C(CC)C(NO)", "7*NAMINE_H1PP", "7*NAMINE_H2C(C)", "7*NAMINE_H2C(CC)", "7*NAMINE_H2C(CCC)", "7*NAMINE_H2C(CN)", "7*NAMINE_H2C(CO)", "7*NAMINE_H2C(NN)", "7*NAMMONIUM", "7*NAMMONIUM_C()C()C()C(C)", "7*NCONNAROMATIC", "7*NCONNAROMATIC_H1C(=CC)C(=CC)", "7*NCONNAROMATIC_H1C(=CC)C(=CN)", "7*NCONNAROMATIC_H1C(=CC)C(C)", "7*NCONNAROMATIC_H2C(=CC)", "7*NCONNAROMATIC_H2C(=CN)", "7*NENAMINE", "7*NENAMINE_C(=CC)C(C)C(C)", "7*NENAMINE_H1C(=CC)C(=CC)", "7*NENAMINE_H1C(=CC)C(CC)", "7*NGUANIDINE", "7*NGUANIDINE_H1C(=NN)C(C)", "7*NGUANIDINE_H2C(=NN)", "7*NIMINE", "7*NIMINE_=C(CC)C(=CC)", "7*NIMINE_=C(CC)N", "7*NIMINE_=NC(=CC)", "7*NIMINE_H1=C(CN)", "7*NIMINE_H1=C(NN)", "7*NIMMONIUM", "7*NIMMONIUM_C{NN}", "7*NIMMONIUM_C{NS}", "7*NIMMONIUM_C{N}", "7*NNITRILE", "7*NNITRILE_#C(C)", "7*NOXAZOLE", "7*NOXAZOLE_{NNNN}", "7*NOXAZOLE_{NNN}", "7*NOXAZOLE_{NNO}", "7*NOXAZOLE_{NNS}", "7*NOXAZOLE_{NN}", "7*NOXAZOLE_{NO}", "7*NOXAZOLE_{NS}", "7*NPYRIDINE", "7*NPYRIDINE_{N}", "7*NPYRIMIDINE", "7*NPYRIMIDINE_{NNN}", "7*NPYRIMIDINE_{NN}", "7*NPYRROLE", "7*NPYRROLE_C{NN}", "7*NPYRROLE_C{N}", "7*NPYRROLE_Fe{N}", "7*NPYRROLE_H1{NN}", "7*NPYRROLE_H1{N}", "7*NPYRROLE_{NNNN}", "7*NPYRROLE_{NNN}", "7*NPYRROLE_{NN}", "7*NSULFONAMIDE", "7*NSULFONAMIDE_C(C)C(C)S", "7*NSULFONAMIDE_H1C(=CC)S", "7*NSULFONAMIDE_H1C(=OC)S", "7*NSULFONAMIDE_H1C(C)S", "7*NSULFONAMIDE_H1C(CC)S", "7*NSULFONAMIDE_H2S", "75*RETRIGBIPYR", "8*OALCOHOL", "8*OALCOHOL_H1C(=NC)", "8*OALCOHOL_H1C(=NN)", "8*OALCOHOL_H1C(=OC)", "8*OALCOHOL_H1C(C)", "8*OALCOHOL_H1C(CC)", "8*OALCOHOL_H1C(CCC)", "8*OALCOHOL_H1C(CCO)", "8*OALCOHOL_H1C(CN)", "8*OALCOHOL_H1C(CO)", "8*OALCOHOL_H1C(NN)", "8*OALCOHOL_H1N", "8*OALCOHOL_H1S", "8*OAMIDE", "8*OAMIDE_=C(CN)", "8*OAMIDE_=C(NN)", "8*OAMIDE_=C(NO)", "8*OCARBONYL", "8*OCARBONYL_=C(CO)", "8*OCARBOXYL", "8*OCARBOXYL_C()C(=CC)", "8*OCARBOXYL_C()C(=OC)", "8*OCARBOXYL_C()C(CC)", "8*OCARBOXYL_C()C(CO)", "8*OCARBOXYL_C(=CC)C(=CC)", "8*OCARBOXYL_C(=CC)C(C)", "8*OCARBOXYL_C(=CC)C(CC)", "8*OCARBOXYL_C(=CC)C(O)", "8*OCARBOXYL_C(=CC)P", "8*OCARBOXYL_C(=OC)C(C)", "8*OCARBOXYL_C(=OC)C(CC)", "8*OCARBOXYL_C(=ON)C(CC)", "8*OCARBOXYL_C(C)C(CC)", "8*OCARBOXYL_C(C)C(CO)", "8*OCARBOXYL_C(CC)C(CC)", "8*OCARBOXYL_C(CC)C(CCO)", "8*OCARBOXYL_C(CC)C(CN)", "8*OCARBOXYL_C(CC)C(CO)", "8*OCARBOXYL_C(CC)C(CS)", "8*OCARBOXYL_C(CC)P", "8*OCARBOXYL_C(CC)S", "8*OCARBOXYL_C(CO)P", "8*OENOL", "8*OENOL_H1C(=CC)", "8*OENOL_H1C(=CN)", "8*OETHER", "8*OETHER_C()C(C)", "8*OETHER_C(C)C(C)", "8*OETHER_C(C)P", "8*OETHER_C(C)S", "8*OETHER_PP", "8*OFURAN", "8*OFURAN_{NO}", "8*OFURAN_{O}", "8*OOXO", "8*OOXO_=C(C)", "8*OOXO_=C(CC)", "8*OOXO_=C(CS)", "8*OOXO_=S", "8*OPHOSPHATE", "8*OPHOSPHATE_P", "8*OWATER", "8*OWATER_H2", "81*TLTRIGPLANAR", "9*F", "9*F_", "92*???"}) {;
			sb.append((sb.length()>0?",":"")+"\""+key+"\"");
			keyToClassId.put(key, clas++);
			}
		}

		//1. Gets all the classes
		Set<String> allKeys = new TreeSet<String>();
		for(String key: file.getFunctionKeys()) {
			StringTokenizer st = new StringTokenizer(key, "-");
			if(st.countTokens()!=2) {System.err.println("Invalid key: "+key);continue;}
			String key1 = st.nextToken();
			String key2 = st.nextToken();
			allKeys.add(key1);
			allKeys.add(key2);
		}

		//2. Creates a Map of key to classId
		for(String key: allKeys) {
			if(keyToClassId.get(key)!=null) continue;
			sb.append((sb.length()>0?",":"")+"\""+key+"\"");
			keyToClassId.put(key, clas++);
		}
		int N = clas;
		classId2parentClassId = new int[N];

		//3. Creates a Map of parents
		for (String key : allKeys) {
			int claz = keyToClassId.get(key);
			String parentKey = key.indexOf('_')<0? null: key.substring(0, key.indexOf('_'));
			if(parentKey!=null) {
				classId2parentClassId[claz] = getClassId(parentKey);
			} else {
				classId2parentClassId[claz] = claz;
			}
			if(classId2parentClassId[claz]<0) {
				classId2parentClassId[claz] = claz;
			}
		}

		//4. Populates the functions array
		for(String key: file.getFunctionKeys()) {
			if(file.get(key).getSpline()==null) continue;
			StringTokenizer st = new StringTokenizer(key, "-");
			if(st.countTokens()!=2) continue;
			String key1 = st.nextToken();
			String key2 = st.nextToken();

			if(proteinIdLigandId2function.get((getClassId(key1)<<16) + getClassId(key2))!=null) {
				throw new IllegalArgumentException("The same key is used twice");
			}
			proteinIdLigandId2function.put((getClassId(key1)<<16) + getClassId(key2), file.get(key));
		}
	}

	public int getClassId(FFMolecule mol, int i) {
		int classId = getClassId(config.getSubKey(mol, i));
		if(classId<0) {
			classId = getClassId(config.getSuperKey(mol, i));
		}
		return classId;
	}

	public int getClassId(String description) {
		Integer i = keyToClassId.get(description);
		return i==null?-1: i.intValue();
	}

	public String getDescription(int claz) {
		for (String key : keyToClassId.keySet()) {
			if(keyToClassId.get(key)==claz) {
				return key;
			}
		}
		return null;
	}

	/**
	 * Get the Protein-Ligand function between a protein type and a ligand type.
	 * The type is given by the KeyAssigner.
	 *
	 * @param key1 a protein class of atom
	 * @param key2 a ligand class of atom
	 * @return
	 */
	public PLFunction getFunction(int key1, int key2) {
		if(key1<0 || key2<0) return null;
		PLFunction function = proteinIdLigandId2function.get((key1<<16)+key2);
		if(function!=null && function.getSpline()!=null) return function;

		int keyParent1 = classId2parentClassId[key1];
		int keyParent2 = classId2parentClassId[key2];

		PLFunction function1 = proteinIdLigandId2function.get((keyParent1<<16)+key2);
		PLFunction function2 = proteinIdLigandId2function.get((key1<<16)+keyParent2);

		if(function1!=null && function1.getSpline()!=null && function2!=null && function2.getSpline()!=null) {
			if(function1.getTotalOccurences()>function2.getTotalOccurences()) {
				return function1;
			} else {
				return function2;
			}
		} else if(function1!=null && function1.getSpline()!=null) {
			return function1;
		} else if(function2!=null && function2.getSpline()!=null) {
			return function2;
		} else {
			return proteinIdLigandId2function.get((keyParent1<<16)+keyParent2);
		}

	}

	/**
	 * Display the number of PL functions, found for the given molecules.
	 * If a function is not found, the program switch to VDW
	 * @param mol
	 */
	public void evaluateMolecule(FFMolecule mol) {
		MM2Parameters.setAtomTypes(mol);
		MMFFParameters.setAtomTypes(mol);

		int[] lvl = new int[5];
		for (int i = 0; i < mol.getNMovables(); i++) {
			for (int j = mol.getNMovables(); j < mol.getAllAtoms(); j++) {

				if(mol.getAtomicNo(i)<=1) continue;

				int key1 = getClassId(mol, j);
				int key2 = getClassId(mol, i);

				if(key1<0) {
					System.out.println("no class for "+config.getSuperKey(mol, j));
					lvl[0]++; continue;
				}
				if(key2<0) {
					System.out.println("no class for "+config.getSuperKey(mol, i));
					lvl[0]++; continue;
				}

				PLFunction function = proteinIdLigandId2function.get((key1<<16)+key2);
				if(function!=null && function.getSpline()!=null) {lvl[4]++; continue;}

				int keyParent1 = classId2parentClassId[key1];
				int keyParent2 = classId2parentClassId[key2];
				function = proteinIdLigandId2function.get((keyParent1<<16)+key2);
				if(function!=null && function.getSpline()!=null) {lvl[3]++; continue;}
				function = proteinIdLigandId2function.get((key1<<16)+keyParent2);
				if(function!=null && function.getSpline()!=null) {lvl[3]++; continue;}
				function = proteinIdLigandId2function.get((keyParent1<<16)+keyParent2);
				if(function!=null && function.getSpline()!=null) {lvl[2]++; continue;}


				System.out.println("no function for "+config.getSuperKey(mol, j)+"-"+config.getSuperKey(mol, i));
				lvl[1]++;

			}
		}

		System.out.println("Function evaluation: "+mol.getNMovables()+" ligand atoms and "+mol.getAtoms()+" atoms");
		System.out.println(" Lvl_0 (no key):  "+lvl[0]);
		System.out.println(" Lvl_1 (nothing): "+lvl[1]);
		System.out.println(" Lvl_2 (parent):  "+lvl[2]);
		System.out.println(" Lvl_3 (mix):     "+lvl[3]);
		System.out.println(" Lvl_4 (sub):     "+lvl[4]);
		System.out.println();

	}


	public int getParent(int claz) {
		return classId2parentClassId[claz];
	}

	/**
	 * Assigns the atomClassId for the molecule
	 * (assigns also the atom types if it has not been set up)
	 * @param mol
	 */
	public void setClassIdsForMolecule(FFMolecule mol) {

		config.prepare(mol);

		for(int i=0; i<mol.getAllAtoms(); i++) {
			mol.setAtomInteractionClass(i, getClassId(mol, i));
		}
	}


	/**
	 * Print the current order of classes (to make backward compatibilities)
	 * @param args
	 */
	public static void main(String[] args) {
		ClassInteractionStatistics s = getInstance(false);
		System.out.println("N classes="+s.getNClasses());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.getNClasses(); i++) {
			if(i>0) sb.append(", ");
			sb.append("\"" + s.getDescription(i) + "\"");
		}
		System.out.println("orderedClasses = new String[]{"+sb+"}");
	}
}
