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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.MultipleIntMap;

/**
 * 
 */
public class MM2Parameters  {

	private static volatile MM2Parameters instance = null;
	private static boolean DEBUG = false;
	

	
	/**
	 * Description of an atom's class
	 * @author freyssj
	 */
	public static class AtomClass {
		public AtomClass(int number, int atomicNo, String description, double charge, int doubleBonds, int tripleBonds, int[] replacement) {
			this.atomClass = number;
			this.atomicNo = atomicNo;
			this.description = description;
			this.charge = charge;
			this.replacement = replacement;
		}
		
		public final int atomClass;
		public final int atomicNo;
		public final String description;
		public final double charge;
		/** Replacement is an array of atomClass, cost used to define
		 * which atom can be used to replace this one in case of missing parameters
		 */
		public final int[] replacement;
		
		@Override
		public String toString() {
			return description;			
		}
	}
	
	public static class BondParameters {
		public BondParameters(double fc, double eq) {
			this.fc = fc;
			this.eq = eq;
		}
		@Override
		public String toString() { return "Bond: " + fc + " " + eq;}
		public double fc;
		public double eq;
	}
	
	public static class AngleParameters {
		public AngleParameters(double fc, double eq) {
			this.fc = fc;
			this.eq = eq;
		}
		@Override
		public String toString() { return "Angle: " + fc + " " + eq;}
		public double fc;
		public double eq;
	}

	public static class TorsionParameters {
		public TorsionParameters(double v1, double v2, double v3) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
		}
		@Override
		public String toString() { return "Torsion: " + v1 + " " + v2 + " " + v3;}
		public double v1, v2, v3;
	}
	
	public static class SingleVDWParameters {
		public SingleVDWParameters(double radius, double epsilon, double reduct) {
			this.radius = radius;
			this.epsilon = epsilon;
			this.reduct = reduct;
		}
		
		public double radius;	
		public double epsilon;	
		public double reduct;	
	}
	
	public static class OutOfPlaneBendParameters {
		public OutOfPlaneBendParameters(double fopb) {
			this.fopb = fopb;
		}
		
		public double fopb;	
	}
	
	public static class VDWParameters {
		public VDWParameters(double radius, double esp) {
			this.radius = radius;
			this.esp = esp;
		}
		@Override
		public String toString() { return "VDW: " + radius + " " + esp;}
		public double radius, esp;
	}
	
	

	protected final Map<String, AtomClass> descriptionToAtom = new ConcurrentHashMap<String, AtomClass>();	 
	protected final Map<Integer, AtomClass> classNoToAtomClass = new ConcurrentHashMap<Integer, AtomClass>();	 
	protected final MultipleIntMap<BondParameters> bondParameters = new MultipleIntMap<BondParameters>(2);
	protected final MultipleIntMap<Double> dipoleParameters = new MultipleIntMap<Double>(2);
	protected final MultipleIntMap<TorsionParameters> torsionParameters = new MultipleIntMap<TorsionParameters>(5);
	protected final MultipleIntMap<AngleParameters> angleParameters = new MultipleIntMap<AngleParameters>(5);
	protected final Map<Integer, SingleVDWParameters> singleVDWParameters = new ConcurrentHashMap<Integer, SingleVDWParameters>();
	protected final MultipleIntMap<VDWParameters> vdwParameters = new MultipleIntMap<VDWParameters>(2);
	protected final MultipleIntMap<OutOfPlaneBendParameters> outOfPlaneBendParameters = new MultipleIntMap<OutOfPlaneBendParameters>(2);	
	protected final Map<Integer, double[]> strBendParameters = new ConcurrentHashMap<Integer, double[]>();	
	protected final Map<String, double[]> piAtoms = new ConcurrentHashMap<String, double[]>();	
	protected final Map<String, double[]> piBonds = new ConcurrentHashMap<String, double[]>();	
	protected final MultipleIntMap<Double> electronegativity = new MultipleIntMap<Double>(3);	
	
	public Collection<AtomClass> getAtomClasses() {
		return descriptionToAtom.values();	
	}
	public AtomClass getAtomClass(int classNo) {
		return classNoToAtomClass.get(classNo);	 
	}
	
	public String getDescription(int classNo) {
		AtomClass a = classNoToAtomClass.get(classNo);
		if(a==null) return null;
		return a.description;
	}
	
	/**
	 * Get the atomType matching the description
	 * @param desc
	 * @return
	 */
	public AtomClass getAtomType(String desc) {		
		AtomClass res = descriptionToAtom.get(desc);	 
		if(res!=null) return res;
		
		for (String s : descriptionToAtom.keySet()) {
			if(s.equalsIgnoreCase(desc) || s.replace(" ", "").equalsIgnoreCase(desc)) return descriptionToAtom.get(s);
		}
		System.err.println("Invalid atomType: "+desc);
		return null;
	}
	
	public BondParameters getBondParameters(int n1, int n2) {
		int[] key = n1<n2? new int[]{n1, n2}:  new int[]{n2, n1};		
		 
		BondParameters p = (BondParameters) bondParameters.get(key);
		if(p==null) {
			int bestCost = 1000;
			BondParameters bestP = null;
			for(int[] k: bondParameters.keys()) {
				int cost = Math.min(cost(n1, k[0]) + cost(n2, k[1]),  cost(n1, k[1]) + cost(n2, k[0]));
				
				if(cost<bestCost) {
					bestCost = cost;
					bestP = (BondParameters) bondParameters.get(k);
				}
			}
			if(bestP!=null) {
				bondParameters.put(key, bestP);				
				p = bestP;
			}
			if(p==null) {
				if(DEBUG) System.err.println("no Bond parameters between "+n1 + " and "+n2);
				p = new BondParameters(0.5, 1.5);
				if(n1>=0 && n2>=0) bondParameters.put(key, p);				
			} 
		}
		return p;
	}
	
	public OutOfPlaneBendParameters getOutOfPlaneBendParameters(int n1, int n2) {
		int[] key = n1<n2? new int[]{n1, n2}: new int[]{n2, n1};
		OutOfPlaneBendParameters p = null;
		p = (OutOfPlaneBendParameters) outOfPlaneBendParameters.get(key);
		return p;
	}
	
	public double getDipoleParameters(int n1, int n2) {
		if(n1<n2) {
			Double res = (Double) dipoleParameters.get(new int[]{n1,n2});
			return res==null? 0: res.doubleValue();
		} 
		Double res = (Double) dipoleParameters.get(new int[]{n2,n1});
		return res==null? 0:  -res.doubleValue();
	}
	
	public AngleParameters getAngleParameters(int n1, int n2, int n3, int nHydrogen, int ringSize) {
		if(ringSize>4) ringSize = 0;
		int[] key;
		if(n1<n3) key = new int[]{n1, n2, n3, nHydrogen, ringSize};
		else key = new int[]{n3, n2, n1, nHydrogen, ringSize};
		
		AngleParameters p = null;
		p = (AngleParameters) angleParameters.get(key);

		if(p==null) {
			int bestCost = 1000;
			AngleParameters bestP = null;
//			int[] bestKey = null;
			synchronized (angleParameters) {
				
				for(int[] k: angleParameters.keys()) {
					int cost = Math.min(cost(n1, k[0]) + cost(n2, k[1])*10 + cost(n3, k[2]),
						cost(n3, k[0]) + cost(n2, k[1])*10 + cost(n1, k[2])) +
						Math.abs(k[3]-nHydrogen)*2 + Math.abs(ringSize-k[4])*5;
					if(cost<bestCost) {
						bestCost = cost;
						bestP = (AngleParameters) angleParameters.get(k);
//						bestKey = k;
					}
				}
			}
			if(bestP!=null) {
//				logger.fine("approximate Angle for " + ArrayUtils.toString(key) +" with "+ArrayUtils.toString(bestKey)+" (cost:"+bestCost+")");
				angleParameters.put(key, bestP);
				p = bestP;				
			}
		}

		if(p==null) {
			if(DEBUG) System.err.println("Guessed Angle for " + ArrayUtils.toString(key));
			p = new AngleParameters(0.5, 120);
			angleParameters.put(key, p);
		} 
		return p;
	}
	
	public double getElectronegativity(int n1, int n2, int n3) {
		Double res = (Double) electronegativity.get(new int[]{n1, n2, n3});
		return res!=null? res.doubleValue(): 0;
	}

	public TorsionParameters getTorsionParameters(int n1, int n2, int n3, int n4, int ringSize) {
		int[] key;
		if(ringSize!=4) ringSize = 0;
		if(n1<n4 || (n1==n4 && n2<=n3)) key = new int[]{n1, n2, n3, n4, ringSize};		
		else key = new int[]{n4, n3, n2, n1, ringSize};	

		TorsionParameters p = (TorsionParameters) torsionParameters.get(key);
		
		if(p==null) {
			int bestCost = 1000;
			TorsionParameters bestP = null;
			for(int[] k: torsionParameters.keys()) {
				int cost = Math.min(cost(n1, k[0])*10 + cost(n2, k[1]) + cost(n3, k[2]) + cost(n4, k[3])*10,
						cost(n1, k[3])*10 + cost(n2, k[2]) + cost(n3, k[1]) + cost(n4, k[0])*10);
				if(cost<bestCost) {
					bestCost = cost;
					bestP = (TorsionParameters) torsionParameters.get(k);
				}
			}
			if(bestP!=null) {
				torsionParameters.put(key, bestP);			
				p = bestP;	
			}
		}
		
//		if(p==null) {
//				
//			if(DEBUG) System.err.println("no Torsion parameters for " + ArrayUtils.toString(key));
//			p = new TorsionParameters(0, 0, 0);
//			torsionParameters.put(key, p);			
//		} 
		return p;
	}
	
	/**
	 * Returns the costs of replacing class n1 by class n2
	 * @return the cost or 1000 if none is found
	 */
	private int cost(int n1, int n2) {
		if(n1==n2) return 0;
		AtomClass claz = classNoToAtomClass.get(n1);
		if(claz==null) {System.err.println("invalid class "+n1); return 1000;}  
		if(claz.replacement==null) return 1000;
		for(int i=0; i<claz.replacement.length; i+=2) {
			if(claz.replacement[i]==n2) return claz.replacement[i+1]*3+(i/2);
		}
		return 1000;
	}
	
	public SingleVDWParameters getSingleVDWParameters(int n1) {
		SingleVDWParameters p = singleVDWParameters.get(n1);
		if(p==null && n1>=0) {
			if(DEBUG) System.err.println("no Single VDW parameters for " + n1);
		}
		return p;
	}


	public VDWParameters getVDWParameters(int n1, int n2) {
		int[] key = n1<n2? new int[]{n1,n2}: new int[]{n2, n1};		
		VDWParameters p = (VDWParameters) vdwParameters.get(key);
		return p;
	}
	
	public double[] getPiAtom(int n1) {		
		return piAtoms.get(""+n1);
	}
	
	public double[] getPiBond(int n1, int n2) {
		String key = n1<n2? n1+"-"+n2: n2+"-"+n1;		
		return piBonds.get(key);
	}
	
	public double getStretchBendParameter(int n1, int nHydro) {
		double[] res = strBendParameters.get(n1);
		if(nHydro==2) nHydro = 1;
		return res==null? 0: res[nHydro];
	}
	

	
	public static MM2Parameters getInstance() {
		if(instance==null) {
			synchronized(MM2Parameters.class) {
				if(instance==null) {
					try {
						instance = new MM2Parameters();
					} catch(Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
		return instance;
	}
	
	private MM2Parameters() throws Exception {
		//System.out.println("Load MM2 Parameters");
		URL url = getClass().getResource("/resources/forcefield/mm2/MM2.parameters");
		if(url==null) throw new Exception("Could not find MM2.parameters in the classpath: /resources/forcefield/mm2/MM2.parameters");
		try {
			InputStream is = url.openStream();
			load(is);
			is.close();
		} catch(IOException e) {
			throw new Exception(e);
		}
	}
		
	
	/**
	 * Parses the given parameter file
	 * @param fileName
	 * @throws ActelionException
	 */	
	private void load(InputStream is) throws Exception {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			int state = 0;
			while((line = reader.readLine())!=null) {
				String token[] = line.split("\t");
				if(token.length==0 || token[0].length()==0) continue;
				else if(token.length==1) {
					if(line.equals("--- ATOM ---")) state = 1;
					else if(line.equals("--- BOND ---")) state = 2;
					else if(line.equals("--- ANGLE ---")) state = 3;
					else if(line.equals("--- 3-ANGLE ---")) state = 8;
					else if(line.equals("--- 4-ANGLE ---")) state = 9;
					else if(line.equals("--- TORSION ---")) state = 4;
					else if(line.equals("--- 4-TORSION ---")) state = 10;
					else if(line.equals("--- OPBEND ---")) state = 5;
					else if(line.equals("--- VDW ---")) state = 6;
					else if(line.equals("--- VDW PAIRS ---")) state = 7;
					else if(line.equals("--- STRETCH-BEND ---")) state = 11;
					else if(line.equals("--- PI-BONDS ---")) state = 12;
					else if(line.equals("--- PI-ATOMS ---")) state = 13;
					else if(line.equals("--- ELECTRONEGATIVITY ---")) state = 14;
					continue;
				}
				
				try {
					switch(state) {
						case 1: {// ---- ATOM -----
							String description = token[0].toUpperCase().intern();
							String label = token[1];
							int atomicNo = Molecule.getAtomicNoFromLabel(label);
							//double VDW = Double.parseDouble(token[2]);
							int classNo = Integer.parseInt("0"+token[3]);
							//int hybridization = 3;//Integer.parseInt(line.substring(64,65).trim());
							int doubleBonds = token.length>8?Integer.parseInt("0"+token[8]):0;
							int tripleBonds = token.length>9?Integer.parseInt("0"+token[9]):0;		
							double charge = token[4].length()>0? Double.parseDouble(token[4]): 0;
							int[] replacement = null;
							if(token.length>13) {								
								String[] s = token[13].trim().split(",");
								if(s.length>0) {
									if(s.length%2==1) throw new Exception("The replacement string is invalid on "+line);
									replacement = new int[s.length];
									for(int i=0; i<s.length; i++) replacement[i] = Integer.parseInt(s[i]);
								}
							}
							AtomClass atom = new AtomClass(classNo, atomicNo, description, charge, doubleBonds, tripleBonds, replacement);
							descriptionToAtom.put(description.toUpperCase(), atom);
							if(classNoToAtomClass.get(classNo)==null) {
								classNoToAtomClass.put(classNo, atom);
							}
							break;
							
						} case 2: {// ---- BOND -----
							int[] atoms = toIntArray(token[0]); 
							double fc = Double.parseDouble(token[1]);
							double eq = Double.parseDouble(token[2]);
							double dip = Double.parseDouble(token[3]);
							bondParameters.put(getOrdered(atoms), new BondParameters(fc, eq));
							dipoleParameters.put(atoms, dip);
							break;
							
						} case 3:
						  case 8:
						  case 9: {// ---- ANGLE -----
							if(token[2].length()==0) continue;
							int ring = state==3? 0: state==8? 3: state==9? 4: -1;
							
							int[] atoms = getOrdered(toIntArray(token[0]));
							double fc = Double.parseDouble(token[1]);
							for(int nHydrogens = 0; nHydrogens<=2 && token.length>2+nHydrogens && token[2+nHydrogens].length()>0; nHydrogens++) {
								double eq = Double.parseDouble(token[2+nHydrogens]);
								angleParameters.put(new int[]{atoms[0], atoms[1], atoms[2], nHydrogens, ring}, new AngleParameters(fc, eq));
							}
							break;
							
						} case 4:
						  case 10: {// ---- TORSION -----
						  	int ring = state==4? 0: 4;
							int[] atoms = getOrdered(toIntArray(token[0]));
							double v1 = Double.parseDouble(token[1]);
							double v2 = Double.parseDouble(token[2]);
							double v3 = Double.parseDouble(token[3]);
							TorsionParameters p = new TorsionParameters(v1, v2, v3);
							torsionParameters.put(new int[]{atoms[0], atoms[1], atoms[2], atoms[3], ring}, p);
							
							break;
							
						} case 5: {// ---- OPBEND -----
							int[] atoms = getOrdered(toIntArray(token[0]));
							double opb = Double.parseDouble(token[1]);
							OutOfPlaneBendParameters p = new OutOfPlaneBendParameters(opb);
							outOfPlaneBendParameters.put(atoms, p);
							break;
							
						} case 6: {// ---- VDW -----
							int atoms = Integer.parseInt(token[0]);
							double rad = Double.parseDouble(token[1]);
							double eps = Double.parseDouble(token[2]);
							double red = Double.parseDouble(token[3]);
							SingleVDWParameters p = new SingleVDWParameters(rad, eps, red);
							singleVDWParameters.put(atoms, p);
							break;
							
						  } case 7: {// ---- VDW PAIRS-----
							  int[] atoms = getOrdered(toIntArray(token[0]));
							  double rad = Double.parseDouble(token[1]);
							  double eps = Double.parseDouble(token[2]);
							  VDWParameters p = new VDWParameters(rad, eps);
							  vdwParameters.put(atoms, p);
							  break;
							
						  } case 11: {// --- STRETCH-BEND ---
 							int atoms = Integer.parseInt(token[0]);
							  double h0 = Double.parseDouble(token[1]);
							  double h1 = Double.parseDouble(token[2]);
							  strBendParameters.put(atoms, new double[]{h0, h1});
							  break;
							  
						  } case 12: {// --- PI-BONDS ---
								String atoms = token[0];
								double t0 = Double.parseDouble(token[1]);
								double t1 = Double.parseDouble(token[2]);
								piBonds.put(atoms, new double[]{t0, t1});
								break;
						  } case 13: {// --- PI-ATOMS ---
								String atoms = token[0];
								double t0 = Double.parseDouble(token[1]);
								double t1 = Double.parseDouble(token[2]);
								double t2 = Double.parseDouble(token[3]);
								piAtoms.put(atoms, new double[]{t0, t1, t2});
						  		break;
						  } case 14: {// --- ELECTRONEGATIVITY ---
								int[] atoms = toIntArray(token[0]);
								double t0 = Double.parseDouble(token[1]);
								electronegativity.put(atoms, new Double(t0));
						  		break;
						  } default:
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					System.err.println("Error on "+line);
					throw e;
				}
							
			}
			reader.close();
		} catch(IOException e) {
			throw new Exception("Could not read the MM2 parameter file", e);
		}		
	}
	
	private static final int[] toIntArray(String s) {
		String[] split = s.split("-");
		int n = split.length;
		
		int[] res = new int[n];
		for(int i=0; i<n; i++) res[i] = Integer.parseInt(split[i]);
		return res;		
	}
	
	private static final int[] getOrdered(int[] v) {
		
		if(v[0]<v[v.length-1]) return v;
		if(v.length==4 && v[0]==v[3] && v[1]<v[2]) return v;
		
		//Inverse order
		int[] res = new int[v.length];
		for(int i=0; i<v.length; i++) {
			res[i] = v[v.length-1-i];
		}
		return res;
	}

	public static void setAtomTypes(FFMolecule mol) {
		MM2Parameters params = getInstance();

		for(int i=0; i<mol.getAllAtoms(); i++) {
			mol.setMM2AtomDescription(i, "");
		}
	
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)>1) {				
				String description = getAtomDescription(mol, i);				
				mol.setMM2AtomDescription(i, description==null?"???": description);
			} else if(mol.getAtomicNo(i)==0)  mol.setMM2AtomDescription(i, "LP LONE PAIR".intern());
		}
		
		//HYDROGENS
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)!=1) continue;
			String connDesc = mol.getMM2AtomDescription(mol.getConnAtom(i,0)).intern();
			String description;
			
			//String comparison using the intern() representation
			if(connDesc=="O ENOL") 		description = "H ENOL";
			else if(connDesc=="O CARBONYL") 	description = "H CARBOXYL";
			else if(connDesc=="O CARBOXYL") 	description = "H CARBOXYL";
			else if(connDesc=="O PHOSPHATE") 	description = "H PHOSPHATE";
			else if(connDesc=="O WATER")		description = "H WATER";
			else if(connDesc=="O ENOL")			description = "H ENOL";
			else if(connDesc.startsWith("O "))	description = "H ALCOHOL";
			
			else if(connDesc=="N AMMONIUM") 	description = "H AMMONIUM";
			else if(connDesc=="N IMMONIUM") 	description = "H AMMONIUM";
			else if(connDesc=="N PYRIDINIUM")	description = "H AMMONIUM";
			else if(connDesc=="N GUANIDINE") 	description = "H GUANIDINE";
			else if(connDesc=="N THIOAMIDE")	description = "H AMIDE";
			else if(connDesc=="N SULFONAMIDE")	description = "H AMIDE";
			else if(connDesc=="N AMIDE") 		description = "H AMIDE";
			else if(connDesc=="N PYRROLE") 		description = "H AMIDE";
			else if(connDesc=="N CONNAROMATIC") description = "H AMIDE";
			else if(connDesc=="N ENAMINE")		description = "H AMIDE";
			else if(connDesc.startsWith("N ")) 	description = "H AMINE";
			
			else if(connDesc.startsWith("P ")) 	description = "H PHOSPHATE";

			else if(connDesc=="S THIOL") 		description = "H THIOL";

			else if(connDesc=="S THIOL") 		description = "H THIOL";
			else if(connDesc=="S THIOETHER") 	description = "H THIOL";
			else description = "H";
			mol.setMM2AtomDescription(i, description.intern());
		}
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			AtomClass a = params.getAtomClassFromDescription(mol.getMM2AtomDescription(i));
			if(a==null) {
				System.err.println("MM2Parameters: Invalid description for "+i+": "+mol.getMM2AtomDescription(i)+" > "+mol.getAtomicNo(i));
				mol.setMM2AtomType(i, 1);
			} else {
				mol.setMM2AtomType(i, a.atomClass);				
			}
		}
//		logger.finest("MM2 parameters computed in "+(System.currentTimeMillis()-s)+"ms");
	}
	
	public static boolean addLonePairs(FFMolecule mol) {
		boolean modified = false;
		//Add Lone Pairs
		for(int i=0; i<mol.getAllAtoms(); i++) {
			
			if(mol.isAtomFlag(i, FFMolecule.RIGID)) continue;
			
			int nLonePairs = getNLonePairs(mol, i);
			for(int j=0; j<mol.getAllConnAtoms(i); j++) {
				if(mol.getAtomicNo(mol.getConnAtom(i, j))==0) {
					if(nLonePairs>0) nLonePairs--;
					else {
						mol.deleteAtom(mol.getConnAtom(i, j));
						j--;
						modified = true;
					}					
				} 
			}
			for(int j=0; j<nLonePairs; j++) {
				int lp = mol.addAtom(0);
				mol.setMM2AtomDescription(lp, "LP LONE PAIR".intern());
				mol.setMM2AtomType(lp, 20);
				mol.setAtomFlags(lp, mol.getAtomFlags(i) & ~FFMolecule.PREOPTIMIZED & ~FFMolecule.RIGID);
				mol.addBond(i, lp, 1);
				mol.setCoordinates(lp, mol.getCoordinates(i));
				modified = true;
			}
		}
		return modified;			
	}
	public static int getNLonePairs(FFMolecule mol, int i) {
		int nLonePairs = 0;
		if(mol.getMM2AtomType(i)==6 && mol.getAllConnAtoms(i)>1) nLonePairs = 2;
		else if(mol.getMM2AtomType(i)==37) nLonePairs = 1;
		else if(mol.getMM2AtomType(i)==8) nLonePairs = 1;
		else if(mol.getMM2AtomType(i)==41) nLonePairs = 1;
		else if(mol.getMM2AtomType(i)==49) nLonePairs = 2;
		else if(mol.getMM2AtomType(i)==82) nLonePairs = 1;
		else if(mol.getMM2AtomType(i)==83) nLonePairs = 1;

		return nLonePairs;
	}
	
	private static String getAtomDescription(FFMolecule mol, int a) {
		String description = null;
		
		int atomicNo = mol.getAtomicNo(a);
		int connected = 0;
		int nHeavy = 0;
		int valence = 0;
		
		int ringSize = mol.getAtomRingSize(a);
		int doubleBonds = 0;
		int tripleBonds = 0;		
//		int nonH = 0;
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int order = mol.getConnBondOrder(a, i);
			if(order==2) doubleBonds++;
			else if(order==3) tripleBonds++;
			
			if(mol.getAtomicNo(mol.getConnAtom(a, i))!=0) {
				connected++;
				valence+=order;
			}
			if(mol.getAtomicNo(mol.getConnAtom(a, i))>1) {nHeavy++;}
		}
		int nH = Math.max(0, StructureCalculator.getImplicitHydrogens(mol, a));
		connected += nH;
		valence += nH;
		nH += StructureCalculator.getExplicitHydrogens(mol, a);
//		boolean aromatic = mol.isAromatic(a);

		sw: switch(atomicNo) {
			case 0: { // Lp
				description = "LP LONE PAIR";
				break;				
				
			} case 5: {
				if(mol.getAtomCharge(a)<0) description = "B TETRAHEDRAL";
				else description = "B TRIG PLANAR";
				break;
			} case 6: {
				if(ringSize==3) {
					if(doubleBonds==1) description = "C CYCLOPROPENE";
					else description = "C CYCLOPROPANE";
				} else if(doubleBonds==1 && !mol.isAromaticAtom(a)) {
					if(connected(mol, a, 7, 2)>=0)  description = "C CARBONYL";			
					else if(connected(mol, a, 8, 2)>=0)  description = "C CARBONYL";			
					else if(connected(mol, a, 15, 2)>=0)  description = "C CARBONYL";			
					else if(connected(mol, a, 16, 2)>=0) description = "C CARBONYL";
					else description = "C ALKENE";
				} else if(doubleBonds==1) {
					description = "C ALKENE";
				} else if(doubleBonds == 2)  {
					description = "C CUMULENE";
				} else if(tripleBonds == 1)  {
					if(StructureCalculator.connected(mol, a, 7, 3)>=0 && connected==1) description = "C ISONITRILE";
					else description = "C ALKYNE";
				} else {
					description = "C ALKANE"; 
				}
				// --> C METAL CO, C CYCLOPENTADIENYL (-0.2e)
				// --> C EPOXY , C CARBOCATION
				break;		
								
			} case 7: { //N
								
				if(connected==3 && mol.getAtomCharge(a)>0 && nConnected(mol, a, 8, -1)>=2) { description = "N NITRO"; break sw;}

				if(valence>3) {
					if(tripleBonds>0) description = "N ISONITRILE";
					else if(doubleBonds>0) description = "N IMMONIUM";
					else description = "N AMMONIUM"; 
					break;
				}
				
				//N in aromatic ring
				for (Integer ringNo : mol.getAtomToRings()[a]) {
					if(!mol.isAromaticRing(ringNo)) continue;
					int[] ringAtoms = mol.getAllRings().get(ringNo);
					int nN = 0;
					int nSO = 0;
					for (int i = 0; i < ringAtoms.length; i++) {
						if(mol.getAtomicNo(ringAtoms[i])==7) {
							nN++;
						} else if(mol.getAtomicNo(ringAtoms[i])==8 || mol.getAtomicNo(ringAtoms[i])==16 ) {
							nSO++;
						}
					}
					
					if(ringAtoms.length==6) {						
//						if(nN>=2) {description = "N PYRIMIDINE"; break sw;}	
//						else 
						{description = "N PYRIDINE"; break sw;} 						
					} else if(ringAtoms.length==5) {
						if(doubleBonds==0 && connected==3) {
							//(C1=CNC1)
							description = "N PYRROLE";  break sw; 
						} else {
							//(N1=COC1)
							if(nSO>0) {description = "N OXAZOLE"; break sw;}
							//(N1=CNC1)
//							else if(nN>1) {description = "N OXAZOLE"; break sw;} //greater than 1 to exclude itself
							//??
//							else { description = "N IMINE";  break sw;}
							else { description = "N IMIDAZOLE";  break sw;}  //IMIDAZOLE
						}
					}
				}
				
				
				if(tripleBonds>0) {description = "N NITRILE"; break sw;}

				/*
				 *     N
				 * N - C(sp2) - N -
				 */
				if(doubleBonds==0) {
					guanidine:for(int i=0; i<mol.getAllConnAtoms(a); i++) {
						int a2 = mol.getConnAtom(a, i);
						if(mol.getAtomicNo(a2)==6 && mol.getConnAtoms(a2)==3 && mol.getAtomRingSize(a2)<0 && connected(mol, a2, -1, 2)>=0) {
							for(int j=0; j<mol.getAllConnAtoms(a2); j++) {
								if(mol.getAtomicNo(mol.getConnAtom(a2, j))!=7) continue guanidine;
							}
							description = "N GUANIDINE"; break sw;
						}
					}
					if(connected(mol, a, 16, 1, 7, -1)>=0 || connected(mol, a, 16, 1, 8, -1)>=0) {
						description = "N SULFONAMIDE"; break sw;
					}
					if((connected(mol, a, -1, 1, 7, 2)>=0 || connected(mol, a, -1, 1, 16, 2)>=0 || connected(mol, a, -1, 1, 8, 2)>=0) && connected(mol, a, 7, 1, 8, 2)<0) {
						description = "N AMIDE"; break sw;
					}
				}
				
				
				
//				if(mol.getAtomToRings()[a].size()<=0 && doubleBonds==0) {
//					for(int i=0; i<mol.getAllConnAtoms(a); i++) {
//						int a2 = mol.getConnAtom(a, i);
//						if(mol.isAromaticAtom(a2)) {
//							description = "N CONNAROMATIC"; break sw; 
//						}  
//					}
//				}
				
				if(doubleBonds==0 && !mol.isAromaticAtom(a) && (connected(mol, a, 6, 1, -1, 2)>=0 || connected(mol, a, 6, 1, -1, 3)>=0)) { description = "N ENAMINE"; break sw;}
				else if(doubleBonds>0) {description = "N IMINE"; break sw;}				
				else {description = "N AMINE"; break;}
				
			} case 8: { // O
				
				if(nHeavy==0) {
					description = "O WATER"; break sw;
				} else if(nHeavy==1) {
					if(nH>0) {						
						if(nH>0 && connected(mol, a, 6, 1, 6, 2)>=0) {description = "O ENOL"; break sw;} //O-C=C						
						else if(connected(mol, a, 6, 1, 8, 2)>=0) {description = "O CARBOXYL"; break sw;} //O-C=O
						else {description = "O ALCOHOL"; break sw;}
					} else {
						if(connected(mol, a, 7, -1, 8, -1)>=0 && mol.getAtomCharge(connected(mol, a, 7, -1))>0) { description = "O NITRO"; break sw;}
						else if(connected(mol, a, 15, -1)>=0) {description = "O PHOSPHATE"; break sw;}
						else if(mol.getAtomCharge(a)<0) {description = "O ALKOXIDE"; break sw;}
						else if(connected(mol, a, 6, 2, 7, 1)>=0) {description = "O AMIDE"; break sw;} //O=CN
						else if(connected(mol, a, 6, 2, 8, 1)>=0) {description = "O CARBONYL"; break sw;} //O=CO
						else {description = "O OXO"; break sw;} //O=C
					}

					
				} else {//2 heavy atoms
					if(mol.getAtomCharge(a)>0) {description = "O OXONIUM"; break sw;}
					if(mol.isAromaticAtom(a)) { description = "O FURAN"; break sw;}
					

					if(connected(mol, a, 15, -1)>=0) {
						description = "O PHOSPHATE"; break sw;
					} else if(connected(mol, a, 6, 1, -1, 2)>=0) {
						//ROC(=R)R
						description = "O CARBOXYL"; break sw;
					} else {
						//COC
						description = "O ETHER"; break sw;
					}
				}
				
			} case 9: { // F
				description = "F";
				break;
				
			} case 11: { // Na
				description = "NA";
				break;
				
			} case 12: { // Mg
				description = "MG";
				break;
				
			} case 13: { // Al
				description = "AL TRIG PLANAR";
				break;
				
			} case 15: { // P
				description = "P PHOSPHATE";
				break;
				
			} case 16: { // S
				if(ringSize==5 && mol.isAromaticAtom(a)) description = "S THIOPHENE";
				else if(doubleBonds==0 && nH>0) description = "S THIOL";
				else if(valence>=4 && nConnected(mol, a, 8, -1) + nConnected(mol, a, 7, -1)>=2) description = "S SULFONE";
				else if(doubleBonds==0) description = "S THIOETHER";
//				else if(mol.getAtomCharge(a)>0) description = "S SULFONIUM";
				else if(connected>=3 && connected(mol, a, 8, -1)>=0) description = "S SULFOXIDE";
				else if(connected(mol, a, 6, 2)>=0) description = "S THIOCARBONYL"; 
				else if(connected(mol, a, 1, 1)>=0) description = "S THIOL"; 
				//else if(doubleBonds==1 ) description = "S THIO";
				else description = "S SULFONE";
				break;
				
			} case 17: { // Cl
				description = "CL";
				break;
				
			} case 19: { // K
				description = "K";
				break;
				
			} case 20: { // Ca
				description = "CA";
				break;
				
			} case 25: { // Mn
				description = "MN";
				break;
				
			} case 26: { // Fe
				description = "FE OCTAHEDRAL";
				break;
				
			} case 30: { // Zn
				description = "ZN TRIG PLANAR";
				break;
				
			} case 35: { // Br
				description = "BR";
				break;
				
			} case 45: { // Rh
				description = "RH TETRAHEDRAL";
				break;
								
			} case 53: { // I
				description = "I";
				break;
				
			} case 78: { // Pt
				description = "PT SQUARE PLANAR";
				break;
				
			} case 14: { // SI
				description = "SI SILANE";
				break;
				
			} case 3: { // Li
				description = "LI";
				break;
				
			} case 29: { // Cu
				description = "CU TRIG PLANAR";
				break;
				
			} default: {
				MM2Parameters params = getInstance();
				//Find a class with the same atomicNo
				for(AtomClass ac : params.getAtomClasses()) {
					if(ac.atomicNo==atomicNo /*&& atom.charge==mol.getAtomCharge(a)*/) {
						if(DEBUG) System.err.println("Warning: used "+ac.description+" for atomicNo "+atomicNo);
						description = ac.description;
						break;
					}
				}
			}
		}
		
		if(description!=null) { 
			return description;
		} else {
			System.err.println("Could not find atom class for " +atomicNo + " " +description + " Val=" + connected + " dbl=" + doubleBonds + " tri="+ tripleBonds + " rsize="+ringSize);
			return null;			
		}
	}
	
	public AtomClass getAtomClassFromDescription(String desc) {
		return descriptionToAtom.get(desc);
	}
	
	public static int connected(FFMolecule mol, int a, int atomicNo, int bondOrder) {
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);
			if(atomicNo>0 && mol.getAtomicNo(atm)!=atomicNo) continue;
			if(bondOrder>0 && mol.getConnBondOrder(a, i)!=bondOrder) continue;
			return atm;
		}
		return -1;
	}

	public static int nConnected(FFMolecule mol, int a, int atomicNo, int bondOrder) {
		assert atomicNo!=1;
		
		int n = 0;
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);
			if(atomicNo>0 && mol.getAtomicNo(atm)!=atomicNo) continue;
			if(bondOrder>0 && mol.getConnBondOrder(a, i)!=bondOrder) continue;
			n++;
		}
		return n;
	}

	public static int connected(FFMolecule mol, int a, int atomicNo, int bondOrder, int atomicNo2, int bondOrder2) {
		assert atomicNo!=1;
		assert atomicNo2!=1;
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);
			if(atomicNo>0 && mol.getAtomicNo(atm)!=atomicNo) continue;
			if(bondOrder>0 && mol.getConnBondOrder(a, i)!=bondOrder) continue;
			
			for(int j=0; j<mol.getAllConnAtoms(atm); j++) {
				int atm2 = mol.getConnAtom(atm, j);
				if(a==atm2) continue;
				if(atomicNo2>0 && mol.getAtomicNo(atm2)!=atomicNo2) continue;
				if(bondOrder2>0 && mol.getConnBondOrder(atm, j)!=bondOrder2) continue;
				return atm2;
				
			}
			
		}
		return -1;
	}



}
