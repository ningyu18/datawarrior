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

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.FastSpline;
import com.actelion.research.util.Formatter;
import com.actelion.research.util.MathUtils;
import com.actelion.research.util.SmoothingSplineInterpolator;


/**
 * ClassStatistics uses the StatisticsResults (occurences of each pair of atoms) to create
 * potential functions.
 * 	1. The data is normalized according to the distance
 *  2. The data is compared to the reference potential
 *  3. Corrective terms are then applied to add a vdw effect at shortest distance
 *
 * Some of the tuning parameters used here are very hard to set up and should be changed
 * with extreme caution only
 *
 * There are 2 versions available of the stats: one with the Hydrogens and one without.
 * Both are comparable, but the one without has lowered minimas for the non-H.
 *
 * @author freyssj
 *
 */
public final class PLFunctionSplineCalculator {

	public static final double DELTA_RADIUS = .25;
	public static final double CUTOFF_STATS = 7;

	/** MIN_DATA should be >200 to avoid artifacts and false positives */
	//	public static int MIN_DATA = 100;
	public static int MIN_DATA = 300;

	private static String DEBUG_KEY = null;


	public static void calculateSplines(PLFunctionFile file) {

		if(DEBUG_KEY!=null) {
			System.out.println(DEBUG_KEY);
			System.out.print("r");
			for (int i = 0; i < CUTOFF_STATS/DELTA_RADIUS; i++) System.out.print("\t"+Formatter.format2(i*DELTA_RADIUS));
			System.out.println();
		}

		//PLFunction init
		double[] coeffs = MathUtils.getTaperCoeffs(CUTOFF_STATS, CUTOFF_STATS-1);

		if(DEBUG_KEY!=null) {
			for(String key: file.getFunctionKeys()) {
				if(key.equals(DEBUG_KEY)) {
					System.out.print("N(r)");
					for (int i = 0; i < CUTOFF_STATS/DELTA_RADIUS; i++) System.out.print("\t"+file.getOccurences(key, i));
					System.out.println();
				}
			}
		}

		/**/
		//
		// Reference function
		//
		double[] referenceSum = new double[(int)(CUTOFF_STATS/DELTA_RADIUS)];
		for(int index=0; index<referenceSum.length; index++) {
			int sum = 0;
			for(String key: file.getFunctionKeys()) {
				String sp[] = key.split("-");
				if(sp[0].indexOf('_')>=0 || sp[1].indexOf('_')>=0) continue; //Select only main types
				if(file.getOccurences(key, index)>0) {
					sum += file.getOccurences(key, index);
				}
			}
			referenceSum[index] = sum;
		}


		//
		// Normalization
		//
		Map<String, DiscreteFunction> functions = new TreeMap<String, DiscreteFunction>();
		Map<String, Integer> occurences = new TreeMap<String, Integer>();
		DiscreteFunction ref = new DiscreteFunction(referenceSum);


		//		double tot =0;
		//		int n =0;
		for(String key1: file.getAllKeys()) {
			for(String key2: file.getAllKeys()) {
				String key = key1 + "-" + key2;

				PLFunction f = file.getFunction(key);
				int[] occurencesArray;
				int nOcc;
				occurencesArray = f.getOccurencesArray();;
				nOcc = f.getTotalOccurences();
				//				} else {
				//					occurencesArray = new int[f.getOccurencesArray().length];
				//					String reverseKey = key2 + "-" + key1;
				//					PLFunction f2 = file.getFunction(reverseKey);
				//					for (int i = 0; i < occurencesArray.length; i++) {
				//						occurencesArray[i] = f.getOccurencesArray()[i] + f2.getOccurencesArray()[i];
				//					}
				//					nOcc = f.getTotalOccurences() + f2.getTotalOccurences();
				//
				//Compare
				//					if(f.getTotalOccurences()>50000 && f2.getTotalOccurences()>50000) {
				//						double diff = 0;
				//						int n1 = 0;
				//						int n2 = 0;
				//						for (int i = 0; i < occurencesArray.length/2; i++) {
				//							n1+= f.getOccurencesArray()[i];
				//							n2+= f2.getOccurencesArray()[i];
				//						}
				//						for (int i = 0; i < occurencesArray.length/2; i++) {
				//							diff += Math.abs((double)f.getOccurencesArray()[i]/n1 - (double)f2.getOccurencesArray()[i]/n2) / (occurencesArray.length/2);
				//						}
				//						tot+=diff;
				//						n++;
				//						System.out.println(key+" > "+diff+" "+f.getTotalOccurences()+" / " +f2.getTotalOccurences());
				//					}
				//				}
				if(nOcc<MIN_DATA ) continue;

				DiscreteFunction function = new DiscreteFunction(occurencesArray);

				functions.put(key, function);
				occurences.put(key, nOcc);

				if(DEBUG_KEY!=null && key.equals(DEBUG_KEY)) {
					System.out.print("g(r)");
					for (int i = 0; i < CUTOFF_STATS/DELTA_RADIUS; i++) System.out.print("\t"+Formatter.format2(function.value(i*DELTA_RADIUS)));
					System.out.println();
				}
			}

		}
		//		System.out.println("diff="+(tot/n)+" n="+n);
		//
		// Log function
		//
		for(String key :functions.keySet()) {
			DiscreteFunction f = functions.get(key);
			for(int index=0; index<CUTOFF_STATS/DELTA_RADIUS; index++) {
				f.setValue(index, -Math.log(
						( f.value(index)   + 1e-4) /
						( ref.value(index) + 1e-4*100)));	//the factor used for repulsion at close distance (when f.value()<<1)
			}
		}

		//
		// Create the smooth log function
		//
		for(String key: functions.keySet()) {
			DiscreteFunction f = functions.get(key);

			double X[] = f.getX();
			double Y[] = f.getY();
			X = (double[]) ArrayUtils.resize(X, X.length+1);
			Y = (double[]) ArrayUtils.resize(Y, Y.length+1);
			X[X.length-1] = CUTOFF_STATS;


			//Cut the front part where data is insufficient
			int index = 2;
			while(Y[index]>10.0 && X[index]<3.0) index++;
			for(int i=index; X[i]<3.0; i++) if(Y[i]>Y[index]) index = i;
			X = ArrayUtils.cut(X, 1, index-1);
			Y = ArrayUtils.cut(Y, 1, index-1);


			Y[0] = 100;
			//Y[1] = 2;

			for(int i=1; i<X.length; i++) if(X[i]>CUTOFF_STATS-1) Y[i] *= MathUtils.evaluateTaper(coeffs, X[i]);

			double[] sigma = new double[X.length];

			Arrays.fill(sigma, 1);
			sigma[0] = 100;

			//
			//  Smoothing Spline
			//
			SmoothingSplineInterpolator interpolator = new SmoothingSplineInterpolator();
			interpolator.setLambda(0.005);
			interpolator.setSigma(sigma);
			FastSpline ff = interpolator.interpolate(X, Y);

			file.get(key).setSplineFunction(ff);
			//			mapFunctions.put(key, new PLFunction(key, ff, occurences.get(key)));

			if(DEBUG_KEY!=null && key.equals(DEBUG_KEY)) {
				System.out.print("W(r)\tInf");
				for (int i = 1; i < CUTOFF_STATS/DELTA_RADIUS; i++) System.out.print("\t"+Formatter.format2(file.get(key).getFGValue(i*DELTA_RADIUS)[0]));
				System.out.println();
			}


		}

	}
	//	public static final String getSuperKey(FFMolecule mol, int a) {
	//		return KeyAssigner.keyAssigner.getSuperKey(mol, a);
	//	}
	//
	//	public static final String getSubKey(FFMolecule mol, int a) {
	//		return KeyAssigner.keyAssigner.getSubKey(mol, a);
	//	}
	//
	//	public static int getAtomicNo(String key, int n) {
	//		String k = key.split("-")[n];
	//		int index = Math.min((k+' ').indexOf(' '), (k+'V').indexOf('V'));
	//		int an = Integer.parseInt(k.substring(1, index));
	//		return an;
	//	}
	//
	//	public static int getValence(String key, int n) {
	//		String k = key.split("-")[n];
	//		int index = k.indexOf('V');
	//		if(index<0) return 0;
	//		int index2 = (k+' ').indexOf(' ');
	//		int v = Integer.parseInt(k.substring(index+1, index2));
	//		return v;
	//	}
	//
	//	public static boolean isDonor(String key, int n) {
	//		int an = getAtomicNo(key, n);
	//		int v = getValence(key, n);
	//		return (an==8 && v<2) || (an==7 && v<3) || (an==16 && v<2);
	//	}
	//
	//	public static boolean isAcceptor(String key, int n) {
	//		int an = getAtomicNo(key, n);
	//		return an==8 || an==7 || an==15 || an==16;
	//	}
}
