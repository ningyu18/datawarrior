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
package com.actelion.research.chem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.util.ArrayUtils;

/**
 * FFMolecule extends Molecule3D and contains some more information relative to
 * the Forcefield:
 *  - atomClasses: the MM2 atom types
 *  - classIds:  the interaction atom types (derived from the MM2 atomTypes)
 * 
 * The atomClasses can be set using MM2Parameters.getInstance().setAtomClasses(mol)
 * The classIds can be set ClassStatistics.getInstance().setClassIds(mol)
 * 
 * 
 * The differences with the ExtendedMolecules are:
 * - the y,z coordinates are not inverted when reading a file
 * - the x,y,z coordinates are double instead of floats, and can be used directly in the optimization 
 * - the FFMolecule is optimized for adding/removing atoms and 3d-file reading, not for memory usage
 * 
 * @author freyssj
 * 
 */
public class FFMolecule implements java.io.Serializable, Comparable<FFMolecule>  {

	public static final int RIGID = 1<<0;
	public static final int LIGAND = 1<<1;
	public static final int BACKBONE = 1<<2;
	public static final int FLAG1 = 1<<3; //Flag used for different purpose
	public static final int IMPORTANT = 1<<4;
	public static final int PREOPTIMIZED = 1<<6;		
	
	
	public static final int INFO_DESCRIPTION = 0;
	public static final int INFO_ATOMSEQUENCE = 1;
	public static final int INFO_MM2ATOMDESCRIPTION = 2;
	public static final int INFO_ATOMNAME = 3;
	public static final int INFO_AMINO = 4;
	public static final int INFO_PPP = 5;
	public static final int INFO_CHAINID = 6;
	
	private static final int MAX_INFOS = 7;
	private static final int MAX_NEIGHBOURS = 16;
	private static final int NEIGHBOURS_BITS = 4;

	//Molecule information
	private String name;
	private int nAtoms;
	private int nMovables = -1;
	private final Map<String, Object> auxiliaryInfos = new HashMap<String, Object>();
	
	//Atom information
	private Coordinates[] coords;
	private int[] atomicNos; 
	private int[] atomCharges; 
	private int[] atomFlags;
	private Object[][] infos;
	private double[] partialCharges;

	//Bond information (always kept up to date)
	private int nBonds;
	private int[][] bonds;   // [bondNo][0->1st atom, 1->2nd atom, 2->order]	
	private int[] nConn; 	 // <MAX_NEIGHBOURS
	private int[] connAtoms; // [atom<<NEIGHBOURS_BITS+connNo]  -> atmNo
	private int[] connBonds; // [atom<<NEIGHBOURS_BITS+connNo]  -> bondNo
	

	//Molecule properties (calculated during the first call at getRings)
	private List<Integer>[] atomToRings = null; 
	private List<int[]> allRings = null;
	
	//Molecule properties (calculated during the first call at isAromatic)
	private boolean aromaticComputed;	
	private boolean[] aromaticAtoms;
	private boolean[] aromaticRing;
	
	//Set by the force field in TermList.prepareMolecule
	private int[] classIds;
	private int[] mm2AtomTypes;
	private int[] mmffAtomTypes;
    private Boolean[] mmffRingAtoms;


	
	public FFMolecule(Molecule mol) {
		this(mol.getAllAtoms(), mol.getAllBonds());
		setName(mol.getName());
		
		this.atomicNos = new int[mol.getAllAtoms()];
		this.atomCharges = new int[mol.getAllAtoms()];
		this.coords = new Coordinates[mol.getAllAtoms()];
		this.partialCharges = new double[mol.getAllAtoms()];
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			atomicNos[nAtoms] = mol.getAtomicNo(i);
			atomCharges[nAtoms] = mol.getAtomCharge(i);
			partialCharges[nAtoms] = 0;
			coords[nAtoms] = new Coordinates(mol.getAtomX(i), -mol.getAtomY(i), -mol.getAtomZ(i));
			nAtoms++;
		}
		
		for(int i=0; i<mol.getAllBonds(); i++) {
			addBond(mol.getBondAtom(0, i), mol.getBondAtom(1, i), mol.getBondOrder(i));
		}
		setAllAtomFlag(FFMolecule.LIGAND, true);
		
	}
	
	/**
	 * Converts an ExtendedMolecule to a FFMolecule, while copying the original rings and aromaticity
	 * @param mol
	 */
	public FFMolecule(ExtendedMolecule mol) {	
		this((Molecule) mol);

		//Copy rings
		allRings = new ArrayList<int[]>();
		atomToRings = new ArrayList[mol.getAllAtoms()];
			
		for (int i = 0; i < atomToRings.length; i++) {
			atomToRings[i] = new ArrayList<Integer>();
		}
		RingCollection ringSet = mol.getRingSet();
		for(int r=0; r<ringSet.getSize(); r++) {
			allRings.add(ringSet.getRingAtoms(r));
			for (int a : ringSet.getRingAtoms(r)) {
				atomToRings[a].add(r);
			}
		}			
		
		//Copy aromaticity
		aromaticComputed = true;
		aromaticRing = new boolean[allRings.size()];
		for(int i=0; i<mol.getAllAtoms(); i++) {
			aromaticAtoms[i] = mol.isAromaticAtom(i);
		}
		for(int r=0; r<ringSet.getSize(); r++) {
			aromaticRing[r] = ringSet.isAromatic(r);
		}
		
	}

	public FFMolecule(String name) {
		this();
		setName(name);
	}

	public FFMolecule(FFMolecule mol) {
		this(mol.getAllAtoms(), mol.getAllBonds());
		this.name = mol.name;
		this.auxiliaryInfos.putAll(mol.auxiliaryInfos);
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			atomicNos[nAtoms] = mol.getAtomicNo(i);
			atomCharges[nAtoms] = mol.getAtomCharge(i);
			partialCharges[nAtoms] = mol.getPartialCharge(i);
			infos[nAtoms] = mol.infos[i];
			coords[nAtoms] = new Coordinates(mol.getAtomX(i), mol.getAtomY(i), mol.getAtomZ(i));
			nAtoms++;
		}
		for(int i=0; i<mol.getAllBonds(); i++) {
			addBond(mol.getBondAtom(0, i), mol.getBondAtom(1, i), mol.getBondOrder(i));
		}
		System.arraycopy(mol.atomFlags, 0, atomFlags, 0, mol.getAllAtoms());		
		System.arraycopy(mol.infos, 0, infos, 0, mol.getAllAtoms());		
		System.arraycopy(mol.mm2AtomTypes, 0, mm2AtomTypes, 0, mol.getAllAtoms());
		System.arraycopy(mol.mmffAtomTypes, 0, mmffAtomTypes, 0, mol.getAllAtoms());
		System.arraycopy(mol.classIds, 0, classIds, 0, mol.getAllAtoms());
		System.arraycopy(mol.partialCharges, 0, partialCharges, 0, mol.getAllAtoms());
	}	

	public FFMolecule() {
		this(5, 5);
	}

	public FFMolecule(int a, int b) {
		if(a<5) a = 5;
		if(b<5) b = 5;

		name = "Molecule";
		nAtoms = 0;				
		coords = new Coordinates[a];
		atomicNos = new int[a];
		atomCharges = new int[a];
		atomFlags   = new int[a];
		partialCharges = new double[a];
		//atomDescriptions = new String[a];
		infos = new Object[a][MAX_INFOS];

		nConn = new int[a];
		connAtoms = new int[a<<NEIGHBOURS_BITS];
		connBonds = new int[a<<NEIGHBOURS_BITS];

		nBonds = 0;				
		bonds = new int[b][3];		
		
		mm2AtomTypes = new int[a];
		mmffAtomTypes = new int[a];
		classIds = new int[a];
		aromaticAtoms = new boolean[a];
	}	

	@Override
	public String toString() {
 		return (name!=null? name : "");
	}

	public String getDataAsString() {
		StringBuilder sb = new StringBuilder();
		DecimalFormat df2 = new DecimalFormat("0.00");
		for (String key: getAuxiliaryInfos().keySet()) {
			if(key.equals("idcode")) continue;
			if(key.equals("idcoordinates")) continue;
			if(key.startsWith("group3")) continue;
			Object value = getAuxiliaryInfo(key);
			if(value==null) continue;
			String s = value instanceof Double? df2.format(value): ""+value;
			sb.append(key+"="+s+" ");
		}
 		return sb.toString();
	}

	public void clear() {
		nBonds = 0;
		nAtoms = 0;
	}

	public final void setAtomFlag(int atm, int flag, boolean value) {
		nMovables = -1;
		if(value) atomFlags[atm] |= flag;
		else atomFlags[atm] &= ~flag;
	}
	
	public final boolean isAtomFlag(int atm, int flag) {
		return (atomFlags[atm]&flag)>0;		
	}
	public final int getAtomFlags(int atm) {
		return atomFlags[atm];		
	}
	public final void setAtomFlags(int atm, int flags) {
		nMovables = -1;
		atomFlags[atm] = flags;		
	}
	public final void setAllAtomFlag(int flag, boolean value) {
		nMovables = -1;
		for (int i = 0; i < getAllAtoms(); i++) setAtomFlag(i, flag, value);	
	}
	
	public final Coordinates getCoordinates(int atm) {
		return coords[atm];
	}
	public final Coordinates[] getCoordinates() {
		return coords;
	}
	
	public final void setCoordinates(int atm, Coordinates c) {
		coords[atm] = c;
	}
	public final void setCoordinates(Coordinates[] c) {
		if(c.length!=coords.length) throw new IllegalArgumentException("Length "+c.length+" <> "+coords.length);
		coords = c;
	}

	public final double getAtomX(int atm) {
		return getCoordinates(atm).x;
	}

	public final double getAtomY(int atm) {
		return getCoordinates(atm).y;
	}

	public final double getAtomZ(int atm) {
		return getCoordinates(atm).z;
	}

	public final void setAtomX(int atm, double x) {
		getCoordinates(atm).x = x;
	}

	public final void setAtomY(int atm, double y) {
		getCoordinates(atm).y = y;
	}

	public final void setAtomZ(int atm, double z) {
		getCoordinates(atm).z = z;
	}

	/**
	 * This method will append a Molecule3D to the end.  
	 * @param m2
	 * @return the index dividing the 2 molecules
	 */
	public int fusion(FFMolecule m2) {
		if(m2==this) throw new IllegalArgumentException("Cannot fusion a molecule with itself");
		int index = getAllAtoms();
		int[] oldToNew = new int[m2.getAllAtoms()];
		for(int i=0; i<m2.getAllAtoms(); i++) {			
			oldToNew[i] = addAtom(m2, i); 
		}
		for(int i=0; i<m2.getAllBonds(); i++) {
			addBond(oldToNew[m2.getBondAtom(0, i)], oldToNew[m2.getBondAtom(1, i)], m2.getBondOrder(i));
		}
		return index;
	}
	public int fusionKeepCoordinatesObjects(FFMolecule m2) {
		int i = getAllAtoms();
		int res = fusion(m2);
		System.arraycopy(m2.getCoordinates(), 0, getCoordinates(), i, m2.getAllAtoms());
		return res;
		
	}
	
	
	public final int getAllAtoms() {
		return nAtoms;
	}

	public int getAtoms() {
		int res = 0;
		for (int i = 0; i < getAllAtoms(); i++) {
			if(getAtomicNo(i)>1) res++;
		}
		return res;
	}


	public final void setAtomicNo(int atm, int atomicNo) {
		atomicNos[atm] = atomicNo;		
	}

	public final int getAtomicNo(int atm) {
		return atomicNos[atm];
	}

	public final int getAllConnAtoms(int atm) {
		return nConn[atm];
	}

	public final int getConnAtoms(int atm) {
		int count = 0;
		for (int i = 0; i < getAllConnAtoms(atm); i++) {
			if(getAtomicNo(getConnAtom(atm,i))>1) count++;
		}
		return count;
	}
	public final int getValence(int atm) {
		int count = 0;
		for (int i = 0; i < getAllConnAtoms(atm); i++) {
			if(getAtomicNo(getConnAtom(atm,i))>1) count += getConnBondOrder(atm, i);
		}
		return count;
	}

	public final int getConnAtom(int atm, int i) {
		return connAtoms[(atm<<NEIGHBOURS_BITS)+i];
	}

	public int getConnBond(int atm, int i) {
		return connBonds[(atm<<NEIGHBOURS_BITS)+i];
	}

	public final int getConnBondOrder(int atm, int i) {
		return bonds[connBonds[(atm<<NEIGHBOURS_BITS)+i]][2];
	}

	public final int getAllBonds() {
		return nBonds;
	}

	public final int getBondAtom(int i, int bond) {
		return bonds[bond][i];
	}

	public final void setAtomCharge(int atm, int charge) {
		atomCharges[atm] = charge;		
	}

	public final int getAtomCharge(int atm) {
		return atomCharges[atm];
	}
	
	public final void setAtomDescription(int atm, String s) {
		infos[atm][INFO_DESCRIPTION] = s;
	}

	public final String getAtomDescription(int atm) {
		return (String) infos[atm][INFO_DESCRIPTION];
	}
	
	public final void setPPP(int atm, int[] a) {
		infos[atm][INFO_PPP] = a;
	}

	public final int[] getPPP(int atm) {
		return (int[]) infos[atm][INFO_PPP];
	}
	public final void setAtomSequence(int atm, int a) {
		infos[atm][INFO_ATOMSEQUENCE] = a;
	}

	public final int getAtomSequence(int atm) {
		return infos[atm][INFO_ATOMSEQUENCE]==null?-1 : (Integer) infos[atm][INFO_ATOMSEQUENCE];
	}
	
	public final void setAtomChainId(int atm, String a) {
		infos[atm][INFO_CHAINID] = a;
	}

	public final String getAtomChainId(int atm) {
		return (String) infos[atm][INFO_CHAINID];
	}
	
	public final void setAtomName(int atm, String a) {
		infos[atm][INFO_ATOMNAME] = a;
	}

	public final String getAtomName(int atm) {
		return (String) infos[atm][INFO_ATOMNAME];
	}
	
	public final void setAtomAmino(int atm, String a) {
		infos[atm][INFO_AMINO] = a;
	}

	public final String getAtomAmino(int atm) {
		return (String) infos[atm][INFO_AMINO];
	}
	
	public final int getBond(int a1, int a2) {
		for(int connBond=0; connBond<getAllConnAtoms(a1); connBond++) {
			if(getConnAtom(a1, connBond)==a2) return getConnBond(a1, connBond);
		}
		return -1;
				
	}
	


	

	public final boolean setBondOrder(int bond, int order) {
		bonds[bond][2] = order;
		return true;	
	}

	public final int getBondOrder(int bond) {
		return bonds[bond][2];
	}

	public final void setBondAtom(int i, int bond, int atm) {
		bonds[bond][i] = atm;
	}


	
	
	

	
	
	////////////////////////////// UTILITIES ////////////////////////////////////////
	
	public final void deleteAtoms(List<Integer> atomsToBeDeleted) {
		Collections.sort(atomsToBeDeleted);
		for (int i = atomsToBeDeleted.size()-1; i>=0; i--) {
			deleteAtom(atomsToBeDeleted.get(i));
		}		
	}

	public String getName() {
		return name==null?"":name;
	}

	public String getShortName() {
		String name = getName();
		if(name.indexOf(' ')>0) name = name.substring(0, name.indexOf(' '));
		if(name.length()>12) name = name.substring(0, 12);
		return name;
	}

	private static transient int n = 0;
	public void setName(String name) {
		this.name = name;
		if(name==null || name.length()==0) {
			name = "Mol-" + (++n);
		}
	}

	public Map<String, Object> getAuxiliaryInfos() {
		return auxiliaryInfos;
	}	
	public Object getAuxiliaryInfo(String name) {
		return auxiliaryInfos.get(name);
	}	
	public void setAuxiliaryInfo(String name, Object value) {
		if(value==null) {
			System.err.println("Attempt to set "+name+" to null");
			auxiliaryInfos.remove(name);
		} else {
			auxiliaryInfos.put(name, value);
		}
	}	

	@Override
	public boolean equals(Object obj) {
		return obj==this;
	}
	


	public final void setMM2AtomDescription(int atm, String desc) {
		infos[atm][INFO_MM2ATOMDESCRIPTION] = desc;
	}

	/**
	 * Returns the Interaction Atom Type
	 * @param atm
	 * @param id
	 */
	public final void setAtomInteractionClass(int atm, int id) {
		classIds[atm] = id;
	}

	/**
	 * Set the MMFF atom type
	 * @param atm
	 * @param atomType
	 */
	public final void setMMFFAtomType(int atm, int atomType) {
		mmffAtomTypes[atm] = atomType;		
	}
	
	/**
	 * Returns the MMFF Atom Type
	 * @param atm
	 * @return
	 */
	public final int getMMFFAtomType(int atm) {
		return mmffAtomTypes[atm];
	}
	
	/**
	 * Returns the MM2 Atom Type
	 * @param atm
	 * @return
	 */
	public final int getMM2AtomType(int atm) {
		return mm2AtomTypes[atm];
	}
	
	/**
	 * Set the MM2 atom type
	 * @param atm
	 * @param atomType
	 */
	public final void setMM2AtomType(int atm, int atomType) {
		mm2AtomTypes[atm] = atomType;		
	}

	
	public final String getMM2AtomDescription(int atm) {
		return (String) infos[atm][INFO_MM2ATOMDESCRIPTION];
	}
	
	public final int getAtomInteractionClass(int atm) {
		return classIds[atm];
	}
	
	/**
	 * Adds a bond between 2 atoms
	 */
	public final int addBond(int atm1, int atm2, int order) {
		atomToRings = null;
		aromaticComputed = false;

		//check for existing connections
		for(int i=0; i<getAllConnAtoms(atm1); i++)  {
			if(getConnAtom(atm1, i)==atm2) {
				int bnd = getConnBond(atm1, i);
				setBondOrder(bnd, order);
				return bnd;
			}
		}
		
		//Add the bond
		if(nConn[atm1]>=MAX_NEIGHBOURS || nConn[atm2]>=MAX_NEIGHBOURS)
			throw new IllegalArgumentException("Maximum "+MAX_NEIGHBOURS+" neighbours");
		if(atm2<atm1) {int tmp = atm2; atm2 = atm1; atm1 = tmp;}
		connAtoms[(atm1<<NEIGHBOURS_BITS)+nConn[atm1]] = atm2;
		connBonds[(atm1<<NEIGHBOURS_BITS)+nConn[atm1]++] = nBonds;
		connAtoms[(atm2<<NEIGHBOURS_BITS)+nConn[atm2]] = atm1;
		connBonds[(atm2<<NEIGHBOURS_BITS)+nConn[atm2]++] = nBonds;
		
		if(bonds.length<=nBonds) {
			bonds = (int[][]) ArrayUtils.resize(bonds, 5+bonds.length*2);
		} 
		int n = nBonds;
		bonds[nBonds++] = new int[]{atm1, atm2, order};
		return n;
	}
	

	/**
	 * Delete an atom
	 */
	public void deleteAtom(int atm) {
		atomToRings = null;
		nMovables = -1;
		aromaticComputed = false;
		
		//Delete bonds going to atm
		for(int i=nConn[atm]-1; i>=0; i--) {
			deleteBond(connBonds[(atm<<NEIGHBOURS_BITS)+i]);
		}		
		//if(getAllConnAtoms(atm)!=0 )throw new IllegalArgumentException();

		//Delete the atom
		--nAtoms;
		if(atm==nAtoms) return;	
		copyAtom(atm, nAtoms);		
	}
	
	/**
	 * Deletes a bond
	 */
	public final void deleteBond(int bond) {
		atomToRings = null;
		aromaticComputed = false;

		//Delete the connected atoms
		int i;
		int a1 = bonds[bond][0];
		int a2 = bonds[bond][1];
		for(i=0; connBonds[(a1<<NEIGHBOURS_BITS)+i]!=bond; i++) {}
		--nConn[a1];
		connBonds[(a1<<NEIGHBOURS_BITS)+i] = connBonds[(a1<<NEIGHBOURS_BITS)+ nConn[a1]];
		connAtoms[(a1<<NEIGHBOURS_BITS)+i] = connAtoms[(a1<<NEIGHBOURS_BITS)+ nConn[a1]];

		for(i=0; connBonds[(a2<<NEIGHBOURS_BITS)+i]!=bond; i++) {}
		--nConn[a2];
		connBonds[(a2<<NEIGHBOURS_BITS)+i] = connBonds[(a2<<NEIGHBOURS_BITS)+ nConn[a2]];
		connAtoms[(a2<<NEIGHBOURS_BITS)+i] = connAtoms[(a2<<NEIGHBOURS_BITS)+ nConn[a2]];
		
		//Move the last bond to the new position
		--nBonds;
		if(nBonds<=0 || bond==nBonds) return;		
		bonds[bond] = bonds[nBonds];
		
		//update references to this bond
		a1 = bonds[nBonds][0];
		a2 = bonds[nBonds][1];
		for(i=0; connBonds[(a1<<NEIGHBOURS_BITS)+i]!=nBonds; i++) {}
		connBonds[(a1<<NEIGHBOURS_BITS)+i] = bond;
		for(i=0; connBonds[(a2<<NEIGHBOURS_BITS)+i]!=nBonds; i++) {}
		connBonds[(a2<<NEIGHBOURS_BITS)+i] = bond;
	}

	public List<Integer>[] getAtomToRings() {
		if(atomToRings==null) {
			allRings = new ArrayList<int[]>();
			atomToRings = StructureCalculator.getRingsFast(this, allRings);
		}
		return atomToRings;
	}

	public List<int[]> getAllRings() {
		if(atomToRings==null) getAtomToRings();
		return allRings;
	}

	public int getAtomRingSize(int atom) {
		if(getAtomToRings()[atom].size()==0) return -1;
		int min = -1;
		for(int r: atomToRings[atom]) {
			int size = allRings.get(r).length;
			if(min<0 || size<min) min = size;			
		}
		return min;
	}
	
	/**
	 * Get the size of the ring that is shared by the given atoms 
	 * @param atoms
	 * @return
	 */
	public final int getRingSize(int[] atoms) {
		Set<Integer> set = new TreeSet<Integer>();
		set.addAll(getAtomToRings()[atoms[0]]);
		for(int i=1; i<atoms.length; i++) {
			set.retainAll(getAtomToRings()[atoms[i]]);			
		}
		if(set.size()==0) return 0;
		int ringNo = set.iterator().next();
		int ringSize = getAllRings().get(ringNo).length;
		if(ringSize>=3 && ringSize<=6) return ringSize;
		return 0;
	}	
	
	public void compact() {
		extendAtomsSize(getAllAtoms());
		bonds = (int[][]) ArrayUtils.resize(bonds, getAllBonds());
	}
	
	protected void extendAtomsSize(int newSize) {
		coords = (Coordinates[]) ArrayUtils.resize(coords, newSize);
		atomFlags  = (int[]) ArrayUtils.resize(atomFlags, newSize);
		atomicNos  = (int[]) ArrayUtils.resize(atomicNos, newSize);
		atomCharges  = (int[]) ArrayUtils.resize(atomCharges, newSize);
		//atomDescriptions = (String[]) ArrayUtils.resize(atomDescriptions, newSize);
		infos = (Object[][]) ArrayUtils.resize(infos, newSize);
		partialCharges = (double[]) ArrayUtils.resize(partialCharges, newSize);
		
		connAtoms  = (int[]) ArrayUtils.resize(connAtoms, newSize<<NEIGHBOURS_BITS);
		connBonds  = (int[]) ArrayUtils.resize(connBonds, newSize<<NEIGHBOURS_BITS);
		nConn  = (int[]) ArrayUtils.resize(nConn, newSize);
		mm2AtomTypes  = (int[]) ArrayUtils.resize(mm2AtomTypes, newSize);
		mmffAtomTypes  = (int[]) ArrayUtils.resize(mmffAtomTypes, newSize);
		classIds = (int[]) ArrayUtils.resize(classIds, newSize);
		aromaticAtoms = (boolean[]) ArrayUtils.resize(aromaticAtoms, newSize);
	}
	

	/**
	 * Copy an atom from src to dest. 
	 * src can be freely deleted after that.
	 * This has to be overriden by subclasses
	 * @param dest
	 * @param src
	 */
	protected void copyAtom(int dest, int src) {
		if(src>=0) {
			atomFlags[dest] = atomFlags[src];
			atomicNos[dest] = atomicNos[src];
			atomCharges[dest] = atomCharges[src];
			
			partialCharges[dest] = partialCharges[src];
			infos[dest] = infos[src].clone();			
			coords[dest] = coords[src];
			nConn[dest] = nConn[src];
			//nConn[src] = 0;
			for(int i=0; i<nConn[dest]; i++) {				
				connAtoms[(dest<<NEIGHBOURS_BITS)+i] = connAtoms[(src<<NEIGHBOURS_BITS)+i];
				connBonds[(dest<<NEIGHBOURS_BITS)+i] = connBonds[(src<<NEIGHBOURS_BITS)+i];
			}	
			
			//Update the references to atm (replace src by dest everywhere)
			for(int i=0; i<connAtoms.length; i++) if(connAtoms[i]==src) connAtoms[i]=dest;
			
			for(int i=0; i<nBonds; i++) {
				if(bonds[i][0]==src) bonds[i][0]=dest;
				else if(bonds[i][1]==src) bonds[i][1]=dest;
			} 

			mm2AtomTypes[dest] = mm2AtomTypes[src];
			mmffAtomTypes[dest] = mmffAtomTypes[src];
			classIds[dest] = classIds[src];
		} else {
			atomFlags[dest] = 0;
			atomicNos[dest] = 0;
			atomCharges[dest] = 0;

			partialCharges[dest] = 0;
			infos[dest] = new Object[MAX_INFOS];
			coords[dest] = new Coordinates();
			nConn[dest] = 0;		
			mm2AtomTypes[dest] = -1;
			mmffAtomTypes[dest] = -1;
			classIds[dest] = -1;
		}
	}
	
	/**
	 * Add an atom with the given atomicNo
	 */
	public int addAtom(int atomicNo) {
		if(atomicNos.length<=nAtoms) {
			extendAtomsSize(10+atomicNos.length*2);
		}
		int n = nAtoms;
		copyAtom(n, -1);
		atomicNos[n] = atomicNo;
		nAtoms++;
		nMovables = -1;	
		return n;
	}		
	
	/**
	 * Add an atom by copying its properties from the given Molecule3D
	 * This has to be overriden by subclasses
	 * @param m
	 * @param i
	 * @return
	 */
	public int addAtom(FFMolecule m, int i) {		
		int a = addAtom(m.getAtomicNo(i));			
		atomFlags[a] = m.getAtomFlags(i);
		infos[a] = m.infos[i].clone();
		coords[a] = new Coordinates(m.getCoordinates(i));
		atomCharges[a] = m.getAtomCharge(i);
		mm2AtomTypes[a] = m.getMM2AtomType(i);
		mmffAtomTypes[a] = m.getMMFFAtomType(i);
		classIds[a] = m.getAtomInteractionClass(i);
		partialCharges[a] = m.getPartialCharge(i);
		nMovables = -1;
		return a;
	}
	
	///////////////////// UTILITY FUNCTIONS ////////////////////////////////////
	/**
	 * Reorganizes atom indexes, so that moveable atoms are first
	 * @return the number of moveable atoms
	 */
	public boolean reorderAtoms() {
		boolean changed = false;
		int N = getAllAtoms();
		int i = 0; 	 //index of the first moveable atom
		int j = N-1; //index of the last non-moveable atom
		
		if(N==0) {
			nMovables = 0;
			return false;
		}

		//Increase the array size if needed (to have up to N+1 atoms)
		if(mm2AtomTypes.length==N) extendAtomsSize(N*2+10);
		
		while(i<j) {
			//Move i to the first non-moveable atom
			while(i<j && !isAtomFlag(i, RIGID)) i++;

			//Move j to the last moveable atom
			while(i<j && isAtomFlag(j, RIGID)) j--;
			if(isAtomFlag(i, RIGID) && !isAtomFlag(j, RIGID)) {
				//Switch the 2 atoms
				copyAtom(N, i);
				copyAtom(i, j);
				copyAtom(j, N);
				changed = true;
			}
		}
		nMovables = isAtomFlag(i, RIGID)? i: i+1;		

		if(changed) {
			atomToRings = null;
			aromaticComputed = false;
		}
		return changed;
	}
	
	/**
	 * Reorganizes atom indexes, so that heavy atoms are first 
	 */
	public void reorderHydrogens() {
		int N = getAllAtoms();
		int i = 0; 	 //index of the first hydrogen
		int j = N-1; //index of the last hydrogen
		
		//Increase the array size if needed (to have up to N+1 atoms)
		if(mm2AtomTypes.length==N) extendAtomsSize(N*2+10);
		
		while(i<j) {
			//Move i to the first non-moveable atom
			while(i<j && getAtomicNo(i)<=1) i++;

			//Move j to the last moveable atom
			while(i<j && !(getAtomicNo(j)<=1)) j--;
			if(i<j && !(getAtomicNo(i)<=1) && (getAtomicNo(j)<=1)) {
				//Switch the 2 atoms
				copyAtom(N, i);
				copyAtom(i, j);
				copyAtom(j, N);
				atomToRings = null;
				aromaticComputed = false;
				
			}
		}
	}		

	/**
	 * @return the number of movable atoms (after reorderatoms has been called)
	 */
	public int getNMovables() {
		if(nMovables<0 || (getAllAtoms()>0 && !isAtomFlag(0, LIGAND))) reorderAtoms();
		return nMovables;
	}
	public boolean isMoleculeInOrder() {
		return reorderAtoms()==false;
	}
	
	/**
	 * Huckel's rule for aromaticity prediction
	 * 
	 */
	public boolean isAromaticAtom(int a) {
		if(!aromaticComputed) computeAromaticity();
		return aromaticAtoms[a];
	}
	public boolean isAromaticRing(int r) {
		if(!aromaticComputed) computeAromaticity();
		return aromaticRing[r];
	}

	private void computeAromaticity() {
		determineAromaticity();
		aromaticComputed = true;		
	}
	
	

	private void determineAromaticity() {
		

		List<int[]> allRings = getAllRings();
		aromaticRing = new boolean[allRings.size()];

		FFMolecule mMol = this;
		
		//mRingAtomSet = ring to list of atome, ie allRings
		List<int[]> mRingAtomSet = getAllRings();
		boolean[] mAromaticityHandled = new boolean[mRingAtomSet.size()];
		int[] mHeteroPosition = new int[mRingAtomSet.size()];
		boolean[] mIsAromatic = aromaticRing;
		boolean[] mIsDelocalized = new boolean[mRingAtomSet.size()];

		List<int[]> mRingBondSet = new ArrayList<int[]>();
		for (int i = 0; i < mRingAtomSet.size(); i++) {
			int[] bonds = new int[mRingAtomSet.get(i).length];
			for (int j = 0; j < mRingAtomSet.get(i).length; j++) {
				bonds[j] = getBond(mRingAtomSet.get(i)[j], mRingAtomSet.get(i)[(j+1)%mRingAtomSet.get(i).length]);
				assert bonds[j]>=0;
			}
			mRingBondSet.add(bonds);
					
		}
		
		
		
		int[][] annelatedRing = new int[mRingAtomSet.size()][];
		for (int i = 0; i < mRingAtomSet.size(); i++) {
			annelatedRing[i] = new int[mRingAtomSet.get(i).length];
			for (int j = 0; j < mRingAtomSet.get(i).length; j++)
				annelatedRing[i][j] = -1;
		}

		int[] ringMembership = new int[mMol.getAllBonds()];
		for (int ring = 0; ring < mRingBondSet.size(); ring++) {
			int[] ringBond = mRingBondSet.get(ring);
			if (ringBond.length >= 5 && ringBond.length <= 7) {
				for (int i = 0; i < ringBond.length; i++) {
					int bond = ringBond[i];
					if (mMol.getConnAtoms(mMol.getBondAtom(0, bond)) == 3 && mMol.getConnAtoms(mMol.getBondAtom(1, bond)) == 3) {
						if (ringMembership[bond] > 0) {
							annelatedRing[ringMembership[bond] >>> 16][ringMembership[bond] & 0x7FFF] = ring;
							annelatedRing[ring][i] = (ringMembership[bond] >>> 16);
						} else {
							ringMembership[bond] = (ring << 16) + 0x8000 + i;
						}
					}
				}
			}
		}

		int ringsHandled = 0;
		int lastRingsHandled = -1;
		while (ringsHandled > lastRingsHandled) {
			lastRingsHandled = ringsHandled;
			for (int ring = 0; ring < mRingAtomSet.size(); ring++) {
				if (!mAromaticityHandled[ring]) {
					if (determineAromaticity(this, mRingBondSet, mAromaticityHandled, mIsAromatic, mHeteroPosition, mIsDelocalized, ring, annelatedRing)) {
						mAromaticityHandled[ring] = true;
						ringsHandled++;
					}
				}
			}
		}
		
		
		//Set the aromaticAtoms array
		Arrays.fill(aromaticAtoms, false);		
		for (int r = 0; r < allRings.size(); r++) {
			if(!aromaticRing[r]) continue;
			for (int a : allRings.get(r)) {
				if(a<aromaticAtoms.length) aromaticAtoms[a] = true;
			}
		}
	}

	private static boolean determineAromaticity(FFMolecule mMol, List<int[]> mRingBondSet, boolean[] mAromaticityHandled, boolean[] mIsAromatic, int[] mHeteroPosition, boolean[] mIsDelocalized, int ringNo, int[][] annelatedRing) {
		List<int[]> mRingAtomSet = mMol.getAllRings();
		
		// returns true if it can successfully determine and set the ring's
		// aromaticity
		int ringAtom[] = mRingAtomSet.get(ringNo);
		int ringBond[] = mRingBondSet.get(ringNo);
		int ringBonds = ringBond.length;
		int bondSequence = 0;
		int aromaticButNotDelocalizedSequence = 0;
		boolean unhandledAnnelatedRingFound = false;
		for (int i = 0; i < ringBonds; i++) {
			bondSequence <<= 1;
			aromaticButNotDelocalizedSequence <<= 1;
			if (mMol.getBondOrder(ringBond[i]) > 1 /*|| mMol.getBondType(ringBond[i]) == Molecule.cBondTypeDelocalized*/)
				bondSequence |= 1;
			else {
				int annelated = annelatedRing[ringNo][i];
				if (annelated != -1) {
					if (mAromaticityHandled[annelated]) {
						if (mIsAromatic[annelated]) {
							bondSequence |= 1;
							if (!mIsDelocalized[annelated])
								aromaticButNotDelocalizedSequence |= 1;
						}
					} else {
						unhandledAnnelatedRingFound = true;
					}
				}
			}
		}

		boolean hasDelocalizationLeak = false;
		switch (ringBonds) {
		case 5:
			final int[] cSequence5Ring = { 10, // 01010
					5, // 00101
					18, // 10010
					9, // 01001
					20 }; // 01010
			hasDelocalizationLeak = true;
			for (int heteroPosition = 0; heteroPosition < 5; heteroPosition++) {
				if ((bondSequence & cSequence5Ring[heteroPosition]) == cSequence5Ring[heteroPosition]) {
					switch (mMol.getAtomicNo(ringAtom[heteroPosition])) {
					case 6:
						if (mMol.getAtomCharge(ringAtom[heteroPosition]) == -1) {
							mIsAromatic[ringNo] = true;
							mHeteroPosition[ringNo] = heteroPosition;
							if ((aromaticButNotDelocalizedSequence & cSequence5Ring[heteroPosition]) == 0)
								hasDelocalizationLeak = false;
						}
						break;
					case 7:
						if (mMol.getAtomCharge(ringAtom[heteroPosition]) <= 0) {
							mIsAromatic[ringNo] = true;
							mHeteroPosition[ringNo] = heteroPosition;
						}
						break;
					case 8:
						mIsAromatic[ringNo] = true;
						mHeteroPosition[ringNo] = heteroPosition;
						break;
					case 16:
						if (mMol.getConnAtoms(ringAtom[heteroPosition]) == 2) {
							mIsAromatic[ringNo] = true;
							mHeteroPosition[ringNo] = heteroPosition;
						}
						break;
					}
				}
			}
			break;
		case 6:
			hasDelocalizationLeak = true;
			if ((bondSequence & 21) == 21) { // 010101
				mIsAromatic[ringNo] = true;
				if ((aromaticButNotDelocalizedSequence & 21) == 0)
					hasDelocalizationLeak = false;
			}
			if ((bondSequence & 42) == 42) { // 101010
				mIsAromatic[ringNo] = true;
				if ((aromaticButNotDelocalizedSequence & 42) == 0)
					hasDelocalizationLeak = false;
			}
			break;
		case 7:
			final int[] cSequence7Ring = { 42, // 0101010
					21, // 0010101
					74, // 1001010
					37, // 0100101
					82, // 1010010
					41, // 0101001
					84 };// 1010100
			hasDelocalizationLeak = true;
			for (int carbeniumPosition = 0; carbeniumPosition < 7; carbeniumPosition++) {
				if ((bondSequence & cSequence7Ring[carbeniumPosition]) == cSequence7Ring[carbeniumPosition]) {
					if ((mMol.getAtomicNo(ringAtom[carbeniumPosition]) == 6 && mMol.getAtomCharge(ringAtom[carbeniumPosition]) == 1) || (mMol.getAtomicNo(ringAtom[carbeniumPosition]) == 5 && mMol.getAtomCharge(ringAtom[carbeniumPosition]) == 0)) {
						mIsAromatic[ringNo] = true;
						mHeteroPosition[ringNo] = carbeniumPosition;
						if ((aromaticButNotDelocalizedSequence & cSequence7Ring[carbeniumPosition]) == 0)
							hasDelocalizationLeak = false;
					}
				}
			}
			break;
		}

		if (mIsAromatic[ringNo] && !hasDelocalizationLeak)
			mIsDelocalized[ringNo] = true;

		if (mIsAromatic[ringNo])
			return true;

		return !unhandledAnnelatedRingFound;
	}

	public double getPartialCharge(int a) {
		return partialCharges[a];
	}
	public void setPartialCharge(int a, double v) {
		partialCharges[a] = v;
	}
	
	/**
	 * Factory Constructor to a StereoMolecule 
	 */
	public StereoMolecule toStereoMolecule() {
		StereoMolecule m = new StereoMolecule(getAllAtoms(), getAllBonds());
		populate(m);
		return m;
	}
	public ExtendedMolecule toExtendedMolecule() {
		ExtendedMolecule m = new ExtendedMolecule(getAllAtoms(), getAllBonds());
		populate(m);
		return m;
	}

	public void copyCoordinatesTo(ExtendedMolecule em) {
		for (int i = 0; i < em.getAllAtoms(); i++) {
			assert em.getAtomicNo(i) == getAtomicNo(i);
			em.setAtomX(i, getAtomX(i));
			em.setAtomY(i, -getAtomY(i));
			em.setAtomZ(i, -getAtomZ(i));
		}
	}
	
	private void populate(ExtendedMolecule m) {
		m.setName(getName());
		int[] ff2em = new int[getAllAtoms()];
		for(int i=0; i<getAllAtoms(); i++) {
			int at = getAtomicNo(i);
			//if(at==0) continue;
			int a = m.addAtom(at);
			ff2em[i] = a+1;
			m.setAtomX(a, getAtomX(i));
			m.setAtomY(a, -getAtomY(i));
			m.setAtomZ(a, -getAtomZ(i));
			m.setAtomCharge(a, getAtomCharge(i));
		}
		for(int i=0; i<getAllBonds(); i++) {
			int order = getBondOrder(i);
			int a1 = ff2em[getBondAtom(0, i)]-1;
			int a2 = ff2em[getBondAtom(1, i)]-1;
			if(a1>=0 && a2>=0 && order==1) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeSingle);
			} else if(a1>=0 && a2>=0 && order==2) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeDouble);
			} else if(a1>=0 && a2>=0 && order==3) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeTriple);
			} else if(a1>=0 && a2>=0 && order==0) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeMetalLigand);
			} else {
				throw new IllegalArgumentException("Invalid bond : "+a1+"-"+a2+"=" +order+ "  was "+getBondAtom(0, i)+"-"+getBondAtom(1, i)+" atoms="+getAllAtoms());
			}
		}		
	}

	@Override
	public int compareTo(FFMolecule o) {
		return o==null? 1: getName().compareTo(o.getName());
	}

	@Override
	public int hashCode() {
		return name==null? 0 : name.hashCode();
	}
	
	/**
	 * Gets the sum of the connected bond orders (including H but ignoring Lp) 
	 * @param atom
	 * @return
	 */
	public int getOccupiedValence(int atom) {
		int valence = 0;
		for (int i=0; i<getAllConnAtoms(atom); i++) {
			if(getAtomicNo(getConnAtom(atom, i))>=1) {
				valence += getConnBondOrder(atom, i);
			}
		}
		return valence;
	}	

    public void setMmffRingAtoms(Boolean[] ringAtoms) {
    	this.mmffRingAtoms = ringAtoms;
	}


	/**
     * Determine if a ring is aromatic according to MMFF criteria. Only
     * designed to work with rings of size 5 and 6. Returns the cached value.
     *  @param r The ring index in the molecule.
     *  @return True if the ring is aromatic, false otherwise.
     */
    public boolean ringIsMMFFAromatic(int r) {
        return mmffRingAtoms[r] == Boolean.TRUE;
    }
    
    /**
     * Returns true if the given ring has had its MMFF aromaticity flag set.
     *  @param r The ring index in the molecule.
     *  @return True if the ring has had its flag set, false otherwise.
     */
    public boolean isSetRingMMFFAromaticity(int r) {
        return mmffRingAtoms[r] != null;
    }

    
    public boolean isAromaticBond(int b) {
    	int a = getBondAtom(0, b);
    	int a2 = getBondAtom(1, b);
    	for(int r: getAtomToRings()[a]) {
    		if(!isAromaticRing(r)) continue;
    		int[] ring = getAllRings().get(r);
    		for (int i = 0; i < ring.length; i++) {
				if(ring[i]!=a) continue;
				if(ring[(i-1+ring.length)%ring.length]==a2 || ring[(i+1)%ring.length]==a2) {
					return true;
				}
			}
    	}
    	return false;
    	
//    	return isAromaticAtom(getBondAtom(0, b)) && isAromaticAtom(getBondAtom(1, b)); 
    }  

    public boolean isAtomMember(int ringNo, int atom) {
		int[] ringAtom = getAllRings().get(ringNo);
		for (int i=0; i<ringAtom.length; i++)
			if (atom == ringAtom[i])
				return true;

		return false;
	}
	public int getInRings(int atom) {
		return getAtomToRings()[atom].size();
	}

    public int getImplicitHydrogens(int a) { 
    	return StructureCalculator.getImplicitHydrogens(this, a); 
    }

    public boolean isRingAtom(int a) { 
    	return getAtomRingSize(a)>0;
    }

}
