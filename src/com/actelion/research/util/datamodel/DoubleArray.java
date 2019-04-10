package com.actelion.research.util.datamodel;

import com.actelion.research.calc.INumericalDataColumn;

/**
 * 
 * DoubleArray
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 26 Jun 2010 MvK: Start implementation
 */
public class DoubleArray implements INumericalDataColumn {
	
	private static final int START_CAPACITY = 32;
	
	private static final int MAX_DELTA_CAPACITY = (int)Math.pow(2, 20);
	
	private double [] data;
	
	private int size;
	
	private int delta_capacity;
	
	public DoubleArray() {
		init(START_CAPACITY);
	}
	
	public DoubleArray(int capacity) {
		init(capacity);
	}
	
	private void init(int capacity){
		data = new double[capacity];
		delta_capacity = capacity/2;
		size = 0;
	}
	
	public double get(int i){
		return data[i];
	}
	
	public double [] get(){
		resize(size);
		return data;
	}
	
	public int add(double v){
		data[size]=v;
		
		int index = size;
		
		size++;
		
		if(size==data.length){
			resize(data.length + delta_capacity);
			if(delta_capacity<MAX_DELTA_CAPACITY){
				delta_capacity *= 2;
			}
		}
		
		return index;
	}
	
	public double avr(){
		
		double avr = 0;
		
		for (int i = 0; i < size; i++) {
			avr += data[i];
		}
		
		return avr/size;
	}
	
	public double max(){
		
		double max = Double.MAX_VALUE * -1;
		
		for (int i = 0; i < size; i++) {
			if(data[i]>max)
				max = data[i];
		}
		
		return max;
	}
	
	public double min(){
		
		double min = Double.MAX_VALUE;
		
		for (int i = 0; i < size; i++) {
			if(data[i] < min)
				min = data[i];
		}
		
		return min;
	}
	
	private void resize(int newlen){
		double [] arr = new double [newlen];
		
		System.arraycopy(data, 0, arr, 0, Math.min(data.length, newlen));
		
		data = arr;
		
	}
	
	public int size(){
		return size;
	}

	@Override
	public int getValueCount() {
		return size;
	}

	@Override
	public double getValueAt(int i) {
		return data[i];
	}
}
