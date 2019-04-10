package com.actelion.research.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * 
 * Pipeline
 * Enables concurrent access to a queue.
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Mar 27, 2012 MvK: Start implementation
 * Oct 9 2012 MvK: bug fix, added reset()
 */
public class Pipeline<T> implements IPipeline<T> {

	private AtomicBoolean allDataIn;
	
	private ConcurrentLinkedQueue<T> queue;
	
	private AtomicLong added;
	
	private AtomicLong polled;
	
	public Pipeline() {
		
		allDataIn = new AtomicBoolean(false);
		
		queue = new ConcurrentLinkedQueue<T>();
		
		added = new AtomicLong();
		
		polled = new AtomicLong();
		
	}
	
	/**
	 * The 'all data in' flag is set true.
	 * @param li
	 */
	public Pipeline(List<T> li) {
		this();
		queue.addAll(li);
		setAllDataIn(true);
	}

	/**
	 * Sets all to 0 and allDataIn to false..
	 */
	public void reset(){
		allDataIn.set(false);
		added.set(0);
		polled.set(0);
		queue.clear();
	}
	
	public boolean isAllDataIn() {
		return allDataIn.get();
	}

	/**
	 * has to be set true or <code>wereAllDataFetched()</code> will never become true. 
	 */
	public void setAllDataIn(boolean allDataIn) {
		this.allDataIn.set(allDataIn);
	}

	public void setAllDataIn() {
		this.allDataIn.set(true);
	}

	public void addData(T t) {
		queue.add(t);
		added.incrementAndGet();
	}
	
	public void addData(List<T> li) {
		queue.addAll(li);
		added.addAndGet(li.size());
	}

	/**
	 * 
	 * @return null if nothing is in the queue.
	 */
	public T pollData() {
		
		T t = queue.poll();
		
		if(t!=null)
			polled.incrementAndGet();
		
		return t;
	}
	
	public int sizePipe(){
		return queue.size();
	}

	public boolean isEmpty(){
		return queue.isEmpty();
	}
	
	public long getAdded() {
		return added.get();
	}

	public long getPolled() {
		return polled.get();
	}

	/**
	 * all data in flag has to be set.
	 * @return all data
	 */
	public List<T> pollAll(){
		
		if(!isAllDataIn()){
			throw new RuntimeException("all_data_in flag not set.");
		}
		
		List<T> li = new ArrayList<T>();
		
		while(!isEmpty()){
			li.add(pollData());
		}
		
		return li;
	}
	
	/**
	 * Returns true if all data in was set and the queue is empty.
	 */
	public boolean wereAllDataFetched() {
		
		if(!isAllDataIn()){
			return false;
		}
		
		return queue.isEmpty();
	}

	public void clear(){
		queue.clear();
	}
}
