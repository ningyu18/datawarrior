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
 * @author Modest von Korff
 */

package com.actelion.research.chem;

import com.actelion.research.util.Formatter;

import java.util.Comparator;

public class Element {

	private int orderNumber;
	
	private String name;
	
	private String symbol;
	
	private double weight;
	
	private double covalentRadius;
	
	private double vdwRadius;

	private double electronegativity;

	public Element() {

	}

	public Element(int orderNumber, String name, String symbol,
			double weight, double covalentRadius, double vdwRadius, double electronegativity) {

		this.orderNumber = orderNumber;
		
		this.name = name;
		
		this.symbol = symbol;
		
		this.weight = weight;
		
		this.covalentRadius = covalentRadius;

		this.vdwRadius = vdwRadius;

		this.electronegativity = electronegativity;
	}

	public int getOrderNumber() {
		return orderNumber;
	}

	public String getName() {
		return name;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getWeight() {
		return weight;
	}

	public double getCovalentRadius() {
		return covalentRadius;
	}

	public double getVDWRadius() {
		return vdwRadius;
	}

	public double getElectronegativity() {
		return electronegativity;
	}

	public void setElectronegativity(double electronegativity) {
		this.electronegativity = electronegativity;
	}

	public String toString() {
		
		return symbol;
	}


	public String toStringForValueTable(){

		StringBuilder sb = new StringBuilder();

		sb.append("new Element(");
		sb.append(orderNumber);
		sb.append(",");
		sb.append("\"" + name + "\"");
		sb.append(",");
		sb.append("\"" + symbol + "\"");
		sb.append(",");
		sb.append(Formatter.format3(weight));
		sb.append(",");
		sb.append(Formatter.format3(covalentRadius));
		sb.append(",");
		sb.append(Formatter.format3(vdwRadius));
		sb.append(",");
		sb.append(Formatter.format3(electronegativity));
		sb.append("),");

		return sb.toString();

	}


	public static Comparator<Element> getComparatorOrderNumber(){

		return new Comparator<Element>() {
			@Override
			public int compare(Element o1, Element o2) {

				int cmp = 0;

				if(o1.getOrderNumber() > o2.getOrderNumber()){
					cmp=1;
				}else if(o1.getOrderNumber() < o2.getOrderNumber()){
					cmp=-1;
				}

				return cmp;
			}
		};
	}



}