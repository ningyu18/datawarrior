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
package com.actelion.research.forcefield.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.chem.calculator.IntQueue;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFConfig;
import com.actelion.research.forcefield.FFConfig.Mode;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.TermList;
import com.actelion.research.forcefield.mm2.AngleTerm;
import com.actelion.research.forcefield.mm2.MM2Config;
import com.actelion.research.forcefield.mm2.MM2Parameters;
import com.actelion.research.forcefield.mm2.OutOfPlaneAngleTerm;
import com.actelion.research.forcefield.mm2.StretchBendTerm;
import com.actelion.research.forcefield.mm2.TorsionTerm;
import com.actelion.research.forcefield.mmff.AngleBend;
import com.actelion.research.forcefield.mmff.MMFFConfig;
import com.actelion.research.forcefield.mmff.OutOfPlane;
import com.actelion.research.forcefield.mmff.StretchBend;
import com.actelion.research.forcefield.mmff.TorsionAngle;
import com.actelion.research.util.ArrayUtils;

/**
 * Preoptimize a molecule to give reasonable starting coordinates
 */
public class PreOptimizer {
	
	
	private final static Random random = new Random();	
	private final static double PRECISION = Math.PI / 64;
	
	/** The molecule to be optimized */
	private final FFMolecule mol;
		
	/** The forcefield used to optimize the data */
	private final ForceField forceField;
	
	/** The coordinate of the first atom */
	private Coordinates firstCoordinate = null;
		
	/** This array describes which atoms have already been visited */
	private boolean[] seen = null;
	
	
	public PreOptimizer(FFMolecule mol, FFConfig config) {		
		forceField = new ForceField(mol, config);
		this.mol = mol;
	}		
	
	
	/**
	 * Function used to create a good starting position for the energy minimization.
	 * @param mol
	 */
	public static void preOptimize(FFMolecule mol) {
		PreOptimizer optimizer = new PreOptimizer(mol, new MMFFConfig());
		optimizer.setFirstCoordinate(new Coordinates());		
		optimizer.doit();
	}

	public static void preOptimizeHydrogens(FFMolecule mol) {
		preOptimizeHydrogens(mol, new MM2Config());
	}
	public static void preOptimizeHydrogens(FFMolecule mol, FFConfig config) {		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)<=1  && mol.getConnAtoms(i)>0) {
				mol.setAtomFlag(i, FFMolecule.PREOPTIMIZED, false);
				mol.setAtomFlag(i, FFMolecule.RIGID, false);
			} else {
				mol.setAtomFlag(i, FFMolecule.PREOPTIMIZED, true);
			}
		}
		mol.reorderAtoms();
		
		
		//Optimize hydrogens (marked as preoptimized=false) 
		PreOptimizer optimizer = new PreOptimizer(mol, config instanceof MMFFConfig?  new MMFFConfig(Mode.PREOPTIMIZATION): new MM2Config(Mode.PREOPTIMIZATION));		
		optimizer.doit();
		
		//Reset
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)<=1 && mol.getConnAtoms(i)>0) {
				mol.setAtomFlags(i, mol.getAtomFlags(mol.getConnAtom(i, 0)));				
			}
		}
		mol.reorderAtoms();
	}
	
	/**
	 * Does The Preoptimization
	 */
	private void doit() {
		final TermList terms = forceField.getTerms();		
		final MM2Parameters parameters = MM2Parameters.getInstance();
		
		if(seen==null) seen = new boolean[mol.getAllAtoms()];
		else if(seen.length<mol.getAllAtoms()) seen = (boolean[]) ArrayUtils.resize(seen, mol.getAllAtoms());
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.isAtomFlag(i, FFMolecule.PREOPTIMIZED)) seen[i] = true;
		}
		
		
		//
		// Step 1: Find the smallest covering set of rings
		//		   For each ring, we determine, its radius and the height
		//
		//                 X (center) 
		//                
		//
		//         ----      ----
		//             \    /    \h
		//              ----      ----
		//
		//Find a covering set of rings
		//final List ringSizes = new ArrayList();
		final List<int[]> allRings = new ArrayList<int[]>();
		final List<Integer>[] atomToRing = StructureCalculator.getRings(mol, allRings);

		//Approximate the size of each ring
		double ringRadius[] = new double[allRings.size()];
		final Coordinates[] ringCenters = new Coordinates[allRings.size()];
		double h[] = new double[allRings.size()];
		for(int i=0; i<allRings.size(); i++) {
			int a1 = allRings.get(i)[0];
			int a2 = allRings.get(i)[1];
			int a3 = allRings.get(i)[2];
			if(seen[a1] && seen[a2]) continue;
			int size = allRings.get(i).length;
			double eq = terms.getBondDistance(a1, a2);
			assert mol.getMM2AtomType(a1)>0;
			double angle = parameters.getAngleParameters(mol.getMM2AtomType(a1), mol.getMM2AtomType(a2), mol.getMM2AtomType(a3), 0, 0).eq/180*Math.PI;
			double adjustedAngle = Math.max(0f, -(angle - Math.PI + 2*Math.PI/size));			
			double adjustedEq = Math.sqrt(eq*eq/2*(2-Math.sin(adjustedAngle)));			
			ringRadius[i] = adjustedEq / Math.sqrt(2*(1-Math.cos(2*Math.PI/size)));
			h[i] = Math.min(eq/2, adjustedEq * Math.sin(adjustedAngle));
		}
		//
		// Step 2: Have a queue with all unseen atoms.
		// 		   A. Each atom is placed empirically so that its energy is minimized
		//		   B. If the atom is part of a ring, we determine the ring's center and
		//            place each atom of the ring.
		//
				
		//Place all the unseen atoms
		IntQueue s = new IntQueue();			
		for(int atom=0; atom<mol.getAllAtoms(); atom++) {
			if(seen[atom]) continue;
			s.push(atom);
			while(!s.isEmpty()) {

				int a2 = s.pop(); //atom to be treated
				if(seen[a2]) continue;
				
				Coordinates best = null;
				double bestEnergy = Float.MAX_VALUE;
				seen[a2]=true;
				mol.setAtomFlag(a2, FFMolecule.PREOPTIMIZED, true);
				//
				// STEP 2 A. Place the atom a2 as to minimize its energy
				//          1. if a2 is the first atom, place it at the first coordinate
				//          2. if a2 is not connected to anything, place it randomly 
				//				(after trying a few positions)
				//          3. if a2 is connected to a1 and a0, place it using the appropriate 
				//				angle and bond distance (optimization around a circle)
				//          4. if a2 is connected to a1, place it using the appropriate bond distance
				//				(optimization around a sphere) 
				//
				//   a0 --   angle
				//         -- a1 ---- a2
				//
				
				//Find an atom a1 connected to a2 (a1==-1 if none)
				int a1 = -1; //atom connected to a2
				for(int i=0; i<mol.getAllConnAtoms(a2); i++) {
					int a = mol.getConnAtom(a2, i);
					if(seen[a]) {a1 = a; break;} 
				}
				
				if(a2==0 && firstCoordinate!=null && a1<0) {
					best = firstCoordinate;
					
				} else if(a1<0) { //a2 is not connected, place it randomly in the boundaries of the molecule
					Coordinates bounds[] = GeometryCalculator.getBounds(mol);
					for(int i=0; i<10; i++) {
						double x, y, z;
						x = random.nextFloat() * (bounds[1].x - bounds[0].x + 1) + bounds[0].x - 0.5f;
						y = random.nextFloat() * (bounds[1].y - bounds[0].y + 1) + bounds[0].y - 0.5f;
						z = random.nextFloat() * (bounds[1].z - bounds[0].z + 1) + bounds[0].z - 0.5f;
						Coordinates c = new Coordinates(x, y, z);				
						mol.setCoordinates(a2, c);
						double energy = terms.getEnergy(seen);								
						if(best == null || energy<bestEnergy) {
							//if(energy<bestEnergy && protein!=null && !protein.isInCavity(c)) energy+=20; 
							if(best == null || energy<bestEnergy) {best = c; bestEnergy = energy;}
						}			
					}
					
				} else { //a2 is connected to a1, place it so that its energy is minimized
					TermList selectedTerms = extractTerms(terms, a2, seen);
					double dist = terms.getBondDistance(a1, a2);
					double angle = 0;
					int a0 = -1;
					for(int k=0; k<selectedTerms.size(); k++) {						
						AbstractTerm t = selectedTerms.get(k);
						if(t instanceof AngleTerm && ((AngleTerm)t).getAtoms()[1]==a1) {
							angle = ((AngleTerm)t).getPreferredAngle()/180*Math.PI;
							int[] atoms = t.getAtoms();
							if(atoms[0]==a2 && seen[atoms[2]]) {a0 = atoms[2]; break;}
							if(atoms[2]==a2 && seen[atoms[0]]) {a0 = atoms[0]; break;}
						} else if(t instanceof AngleBend && ((AngleBend)t).getAtoms()[1]==a1) {
							angle = ((AngleBend)t).getPreferredAngle()/180*Math.PI;
							int[] atoms = t.getAtoms();
							if(atoms[0]==a2 && seen[atoms[2]]) {a0 = atoms[2]; break;}
							if(atoms[2]==a2 && seen[atoms[0]]) {a0 = atoms[0]; break;}
						}
					}
					if( a0<0 /*|| mol.getCoordinates(a0).distSquareTo(mol.getCoordinates(a1))<.5 */) { // place a2 at distance dist of a1 
						for(double theta = 0; theta<2*Math.PI; theta += PRECISION) {
							for(double phi = 0; phi<Math.PI; phi += PRECISION) {
								Coordinates c = new Coordinates(dist*Math.cos(theta),dist*Math.sin(theta)*Math.cos(phi),dist*Math.sin(theta)*Math.sin(phi));
								c.add(mol.getCoordinates(a1));
								mol.setCoordinates(a2, c);
								double energy = selectedTerms.getFGValue(null);		
								if(best == null || energy<bestEnergy) {best = c; bestEnergy = energy;}
							}
						}
					} else {  // place a2 at distance dist of a1, while maintaining angle a0-a1-a2				
						Coordinates ca0 = mol.getCoordinates(a0);
						Coordinates ca1 = mol.getCoordinates(a1);
						Coordinates u = ca1.subC(ca0);
						Coordinates n = new Coordinates(-u.y,u.x,0);
						if(u.cross(n).distSq()<0.1) n = new Coordinates(0,-u.z,u.y);
						n = u.cross(n).unitC().scaleC(dist*Math.sin(angle)); // perpendicular to u 
						Coordinates proj = ca1.addC(ca1.subC(ca0).unitC().scaleC(-dist*Math.cos(angle)));
						for(double theta = 0; theta<2*Math.PI; theta += PRECISION) {
							Coordinates c = n.rotate(ca0.subC(ca1).unitC(), theta).addC(proj);
							mol.setCoordinates(a2, c);							
							double energy = selectedTerms.getFGValue(null);							
							if(best == null || energy<bestEnergy) {best = c; bestEnergy = energy;}
						}
					}					
				}				
				//Now that we have empirically found a position for a2, update it 
				mol.setCoordinates(a2, best);

				//We need to perform the next iterations on the a2 neighbours
				for(int i=0; i<mol.getAllConnAtoms(a2); i++) {					
					int a = mol.getConnAtom(a2, i);
					s.push(a);
				}

				//
				// STEP 2 B: insert the rings connected to a2 if applicable
				//			1. Find the unknown rings that have not been visited
				//				and for each each one, find how many atoms are knowns 
				//			2. Sort the unknowns rings according to the number of seen atoms
				//			3. Find the center and a normal of those rings
				//		    4. Insert the atoms belonging to the ring
				//

				if(mol.getAtomicNo(a2)==1 || atomToRing[a2].size()==0) {
					//Case 1: a2 is not in a ring
				} else {
					//Case 2: a2 is in a ring
					final List<Integer> ringNos = atomToRing[a2]; //The List of ring No connected to the current atom a2
					final List<Integer> unknowns = new ArrayList<Integer>(); 	   //List of ring No with an unknown center that we need to add 
					final HashMap<Integer,List<Integer>> ringNoToSeen = new HashMap<Integer,List<Integer>>();   //The atom No that have already been setup for each unknown ring No 
					
					Iterator<Integer> iter = ringNos.iterator();
					while(iter.hasNext()) {
						int ringNo = iter.next();
						Coordinates center = ringCenters[ringNo];
						if(center==null) {
							unknowns.add(ringNo);
							int[] atoms = allRings.get(ringNo);
							for(int i=0; i<atoms.length; i++) {
								if(seen[atoms[i]]) {
									List<Integer> l = ringNoToSeen.get(ringNo);
									if(l==null) {
										l = new ArrayList<Integer>();
										ringNoToSeen.put(ringNo, l);
									}
									l.add(atoms[i]);
								}
							}
						}
					}
					
					//2 B 2 Sort them in order to create first the rings having more seen atoms  
					Collections.sort(unknowns, new Comparator<Integer>() {
						@Override
						public int compare(Integer o1, Integer o2) {
							int i1 = ringNoToSeen.get(o1).size();
							int i2 = ringNoToSeen.get(o2).size();
							if(i1!=i2) return  i1-i2;
							return allRings.get(o1).length - allRings.get(o2).length;
						}
					});
					
					while(!unknowns.isEmpty()) {
						
						//2 B 3 Insert the rings' centers
						int ringNo = unknowns.remove(0);
						Coordinates center = ringCenters[ringNo];
						if(center!=null) continue;						
						
						List<Integer> alreadySeen = new ArrayList<Integer>();
						int[] atoms = allRings.get(ringNo);
						for(int i=0; i<atoms.length; i++) {
							if(seen[atoms[i]]) {
								alreadySeen.add(atoms[i]);
							}
						}										
						
						int refAtom = alreadySeen.get(0);


						double radius = ringRadius[ringNo];
						bestEnergy = Double.MAX_VALUE;
						best = null;
						int refAtom2 = -1;
						if(alreadySeen.size()<2) {
							for(double theta = -ringNo*0.01; theta<2*Math.PI; theta += PRECISION) {
								for(double phi = -a2*0.01; phi<Math.PI; phi += PRECISION) {
									Coordinates c = new Coordinates(radius*Math.cos(theta),radius*(Math.sin(theta)*Math.cos(phi)),radius*(Math.sin(theta)*Math.sin(phi))).addC(mol.getCoordinates(refAtom));
									double energy = evaluateRingCenterPosition(c, alreadySeen, radius, seen, ringCenters);
									if(energy<bestEnergy) {best = c;bestEnergy = energy;}
								}		
							}														
						} else {
							int aX=-1, aY=-1;
							double biggestDistance = -1;
							for(int i=0; i<alreadySeen.size(); i++) {
								for(int j=i+1; j<alreadySeen.size(); j++) {
									int ind1 = alreadySeen.get(i);
									int ind2 = alreadySeen.get(j);
									Coordinates ca1 = mol.getCoordinates(ind1);
									Coordinates ca2 = mol.getCoordinates(ind2);
									double dist = ca1.distSquareTo(ca2);
									if(dist>biggestDistance) {
										biggestDistance = dist;
										aX = ind1;
										aY = ind2; 
									}
								}								
							}
							
							refAtom2 = aY;
							Coordinates cX = mol.getCoordinates(aX);
							Coordinates cY = mol.getCoordinates(aY);
							int indX = ArrayUtils.indexOf(atoms, aX);
							int indY = ArrayUtils.indexOf(atoms, aY);
							int diff = (indY - indX + atoms.length) % atoms.length;
							if(diff>atoms.length/2) diff = atoms.length-diff;
							double hh = radius * Math.cos(Math.PI * diff / atoms.length);
							Coordinates u = new Coordinates(mol.getCoordinates(aY).y-mol.getCoordinates(aX).y, mol.getCoordinates(aX).x-mol.getCoordinates(aY).x, 0 );
							Coordinates cMed = cX.addC(cY).scaleC(0.5); 
							Coordinates cZ = u.unitC().scaleC(hh);
							Coordinates normal = cY.subC(cX).unitC();
							for(double theta = 0; theta<2*Math.PI; theta += PRECISION) {
								Coordinates c = cZ.rotate(normal, theta).addC(cMed);
								double energy = evaluateRingCenterPosition(c, alreadySeen, radius, seen, ringCenters);
								if(energy<bestEnergy) {best = c; bestEnergy = energy;}
							}
						}
						
						center = ringCenters[ringNo] = best;		
												
						//Where do we start building the ring
						int index = ArrayUtils.indexOf(atoms, refAtom);
			
						//2B3' get a normal vector
						Coordinates ref = mol.getCoordinates(refAtom);
						Coordinates u = ref.subC(center);
						Coordinates normal = null;
						if(alreadySeen.size()>=2) {
							Coordinates t = mol.getCoordinates(refAtom2).subC(center);
							//Define the direction of the normal depending of the angle between the 2 center and the 2 connected atoms
							int diff = (index - ArrayUtils.indexOf(atoms, refAtom2) + atoms.length) % atoms.length;
							if(diff<atoms.length/2.0)  normal = u.cross(t);
							else normal = t.cross(u);
						}
						if(normal==null) {
							if(a1>=0) normal = u.cross(ref.subC(mol.getCoordinates(a1)));
							else normal = new Coordinates(0,0,1);
						}
						if(normal.distSq()<1E-10) normal = u.cross(new Coordinates(1, 1, 1));
						if(normal.distSq()<1E-10) normal = u.cross(new Coordinates(1, -1, -1));
						normal = normal.unitC();														


						//2 B 4 Insert the atoms
						//Add the complete ring starting at refAtom
						for(int i=1; i<atoms.length; i++) {
							int a = atoms[(i+index) % atoms.length];
							if(seen[a]) continue;
							seen[a] = true;
				
							Coordinates c2 = u.rotate(normal, (Math.PI * 2.0 / atoms.length * ((i+index) % atoms.length))).addC(center);
							if(atoms.length>6) {
								//Start with an alternated conformation
								if((i+index+1)%4>=2) c2 = c2.addC(normal.scaleC(2*h[ringNo]));
							} else if(atoms.length<=6) {
								//Start with the chair conformation
								if((i+index+1)%6==0) c2 = c2.subC(normal.scaleC(2*h[ringNo]));
								if((i+index+1)%6==3) c2 = c2.addC(normal.scaleC(2*h[ringNo]));
							}
							mol.setCoordinates(a, c2);
							for(int j=0; j<mol.getAllConnAtoms(a); j++) {					
								int a3 = mol.getConnAtom(a, j);
								s.push(a3);
							}
																					
							//Add the rings connected to this ring
							List<Integer> rings = atomToRing[a];
							for (int no: rings) {
								if(ringCenters[no]==null) unknowns.add(no);
							}							
						}						
					}
				}
			}				
		}
	}
	
	private double evaluateRingCenterPosition(Coordinates c, List<Integer> neigbours, double radius, boolean seen[], Coordinates[] centers) {
		double sum = 0f;
		int n = 0;
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(!seen[i]) continue;
			Coordinates c2 = mol.getCoordinates(i);
			sum += Math.exp(-c2.subC(c).distSq());
			n++;			
		}
		if(n>0) sum /= n;
		
		
		
		double sum2 = 0;
		Iterator<Integer> iter = neigbours.iterator();
		while(iter.hasNext()) {
			int a = iter.next().intValue();
			double dt = mol.getCoordinates(a).subC(c).dist() - radius; 
			sum2 += Math.exp(-dt*dt);
		}
		if(neigbours.size()>0) sum2 /= neigbours.size();
		
		double sum3 = 0f;
		n=0;
		for(int i=0; i<centers.length; i++) {
			if(centers[i]!=null) {
				sum3 += Math.exp(-centers[i].subC(c).distSq());
				n++;
			}
		}
		if(n>0) sum3 /= n;

		return sum*3 - sum2*2 + sum3;
	}
	
	
	private static TermList extractTerms(TermList terms, int a, boolean [] seen) {
		TermList l = TermList.create(terms.getConfig(), terms.getMolecule());
		loop: for(int k=0; k<terms.size(); k++) {
			AbstractTerm t = terms.get(k);
			int[] atoms = t.getAtoms();
			boolean ok = false;
			for(int i=0; i<atoms.length; i++) {
				if(atoms[i]==a) ok = true;
				else if(!seen[atoms[i]]) continue loop;
			}						
			if(ok) {
				l.add(t.clone());					
			}
		}
		return l;
		
	}
	
	public void setFirstCoordinate(Coordinates coordinates) {
		firstCoordinate = coordinates;
	}


}
