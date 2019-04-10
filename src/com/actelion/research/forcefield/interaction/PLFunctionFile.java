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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import com.actelion.research.util.ArrayUtils;



/**
 * PLFunctionPool contains the number of occurences of all pair of atomTypes
 *
 */
public class PLFunctionFile {

	private Set<String> allKeys = new TreeSet<String>();
	private Map<String, PLFunction> proteinLigandfunction = new TreeMap<String, PLFunction>();

	public PLFunctionFile() {

	}


	public Set<String> getAllKeys() {
		return allKeys;
	}

	public PLFunction get(String key1Key2) {
		return proteinLigandfunction.get(key1Key2);
	}

	public void remove(String key1Key2) {
		proteinLigandfunction.remove(key1Key2);
	}
	public PLFunction getFunction(String key1Key2) {
		PLFunction f = proteinLigandfunction.get(key1Key2);
		if(f==null) {
			String sp[] = key1Key2.split("-");
			if(sp.length!=2) {
				System.err.println("Invalid key: "+key1Key2);
			}
			allKeys.add(sp[0]);
			allKeys.add(sp[1]);

			f = new PLFunction(key1Key2);
			proteinLigandfunction.put(key1Key2, f);
		}
		return f;
	}


	/**
	 * Add an Interaction for the given atom pair (key) and at the given distance
	 * @param key
	 * @param dist
	 */
	public void addInteraction(String key1key2, double dist) {
		if(dist>=PLFunctionSplineCalculator.CUTOFF_STATS) return;
		PLFunction f = getFunction(key1key2);

		int index = DiscreteFunction.distanceToIndex(dist);
		int[] occurences = f.getOccurencesArray();
		if(index<occurences.length) occurences[index]++;
	}


	public int getTotalOccurences(String key1key2) {
		PLFunction occ = get(key1key2);
		return occ==null? 0: occ.getTotalOccurences();
	}

	public int getOccurences(String key1key2, int index) {
		PLFunction occ = get(key1key2);
		return occ==null? 0: occ.getOccurencesArray()[index];
	}

	public Set<String> getFunctionKeys() {
		return proteinLigandfunction.keySet();
	}

	//////////////////// IO FUNCTIONS ////////////////////////////////////
	public synchronized void write(String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		//Write the distance pair interactions
		for(String key1key2: getFunctionKeys()) {
			PLFunction occ = proteinLigandfunction.get(key1key2);
			writer.write(key1key2.replace(' ', '_'));
			for(int index=0; index<occ.getOccurencesArray().length; index++) {
				writer.write(" "+occ.getOccurencesArray()[index]);
			}
			writer.write(System.getProperty("line.separator"));
		}

		writer.write(System.getProperty("line.separator"));
		writer.close();

	}


	public static PLFunctionFile read(PLConfig config, boolean includeHydrogens) {
		try {
			String file = "/resources/forcefield/" + config.getParameterFile();
			URL url =  PLFunctionFile.class.getResource(file);
			if(url==null) {
				throw new RuntimeException("Could not find the interactions parameter file in the classpath: " + file);
			}
			return read(url.openStream(), config, config.isSymetric(), includeHydrogens);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static PLFunctionFile read(InputStream is, PLConfig config, boolean symmetric, boolean includeHydrogens) throws IOException {
		PLFunctionFile file = new PLFunctionFile();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;

		while((line = reader.readLine())!=null) {
			if(line.length()==0) break;
			StringTokenizer st = new StringTokenizer(line);
			String key = st.nextToken();

			//Skip hydrogens?
			if(!includeHydrogens) {
				String s[] = key.split("-");
				if(config.isHydrogen(s[0]) || config.isHydrogen(s[1])) continue;
			}

			int[] occurencesArray = new int[PLFunction.ARRAY_SIZE];
			for(int index=0; st.hasMoreTokens() && index<occurencesArray.length; index++) {
				int occ = Integer.parseInt(st.nextToken());
				occurencesArray[index] = occ;
			}

			if(symmetric) {
				String s[] = key.split("-");
				PLFunction f1 = file.getFunction(key);

				if(s[0].equals(s[1])) {
					f1.setOccurencesArray(occurencesArray);
				} else {
					PLFunction f2 = file.getFunction(s[1]+"-"+s[0]);
					for(int index=0; index<occurencesArray.length; index++) {
						occurencesArray[index] = f1.getOccurencesArray()[index] + f2.getOccurencesArray()[index] + occurencesArray[index];
					}
					f1.setOccurencesArray(occurencesArray);
					f2.setOccurencesArray((int[])ArrayUtils.copy(occurencesArray));
				}
				//
				//				if(key.startsWith("7*NAMINE-8*OCARBOXYL") || key.startsWith("8*OCARBOXYL-7*NAMINE")) {
				//					System.out.println("PLFunctionFile.read() "+key+" > "+f1.getTotalOccurences()+" - "+Arrays.toString(occurencesArray));
				//				}

			} else {
				PLFunction f = file.getFunction(key);
				f.setOccurencesArray(occurencesArray);
			}
		}

		is.close();
		return file;
	}

}