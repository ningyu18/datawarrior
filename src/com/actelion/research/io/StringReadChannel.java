package com.actelion.research.io;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import com.actelion.research.util.ConstantsDWAR;
import com.actelion.research.util.Pipeline;

/**
 * 
 * StringReadChannel
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2007 MvK: Start implementation
 * 25.06.2009 MvK: implementation changed
 * 12.02.2014 MvK: added charset encoding to handle Umlaute.
 * 24.04.2014 MvK: Pipeline replaced simple LinkedList because of needed concurrent access.
 * 29.01.2015 MvK: Increased capacity CAPACITY_LINE_BUFFER to 10,000,000 because of overflow when reading PubMed records.
 * 03.06.2015 MvK: Increased capacity CAPACITY_LINE_BUFFER to 50,000,000 because of overflow when reading g2dDiseasePublicationSlope.dwar
 */
public class StringReadChannel {

	private static final int CAPACITY_LINE_BUFFER = 50000000;
	
	private static final int CAPACITY_READ_BUFFER = 100000;
	
	private static final int CAPACITY_PIPE = 1000;
	
	
	
	private ReadableByteChannel byteChannel;
		
	private ByteBuffer buffer;
	
	private ByteBuffer byteBufferLine;
	
	private Pipeline<String> pipeline;
		
	public StringReadChannel(ReadableByteChannel ch)throws IOException{
		init(ch);
	}
	
	private void init(ReadableByteChannel bc) throws IOException{
		
		byteChannel = bc;
		

		// We will fill up this ByteBuffer instance later.
		byteBufferLine = ByteBuffer.allocate(CAPACITY_LINE_BUFFER);
		
		buffer = ByteBuffer.allocate(CAPACITY_READ_BUFFER);
		
		pipeline = new Pipeline<String>();
		
		readLine2List();
	}
	
	public boolean hasMoreLines() throws IOException {
		
		return !pipeline.wereAllDataFetched();
		
	}
	
	/**
	 * 
	 * @return null if EOF reached.
	 * @throws IOException
	 */
	public String readLine() throws IOException {

		int maxCycles = 100;
		
		String str = null;
		
		
		if(!pipeline.isEmpty()){
			
			str = pipeline.pollData();
			
		} else if(!pipeline.isAllDataIn()){
			
			int ccCycle=0;
			
			while(pipeline.isEmpty()){
			
				try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			
				ccCycle++;
				
				if(ccCycle>maxCycles){
					
					RuntimeException ex = new RuntimeException("Arbitrary break. Max number of cycles (" + maxCycles + ") exceeded.");
					
					// ex.printStackTrace();
					
					byteChannel.close();
					
					throw ex;
				}
				
				str = pipeline.pollData();
			}
		}
		
		
		if(!pipeline.isAllDataIn()){
			if(pipeline.sizePipe() < CAPACITY_PIPE){
				readLine2List();	
			}
			
			
		}
		
		
		return str;
	}
	
	public void finalize()throws IOException{
		close();
	}
	
	private int readLine2List() throws IOException {
				
		boolean lineFinished=false;
		
		int sizeLine = 0;
		
		while(!lineFinished) {
			
			((Buffer)buffer).clear();
			
			int size = byteChannel.read(buffer);
					
			if(size==-1){ // end of stream
				
				if(byteBufferLine.position() > 0){
				
					writeBuffer2Pipe();
					lineFinished = true;
					
				}
				
				pipeline.setAllDataIn(true);
							
				return -1;
			}
			
			//
			// Copying from one buffer to the other
			//
			for (int i = 0; i < size; i++) {
				
				byte c = buffer.get(i);
							
				if(c=='\n') {
										
					writeBuffer2Pipe();
						
					lineFinished = true;
					
				} else {
					if(c!='\r') {
					
						byteBufferLine.put(c);
						
						sizeLine++;
						
					}
				}
			}
			
		}
		
		return sizeLine;
	}
	
	private void writeBuffer2Pipe() throws UnsupportedEncodingException{
		
		byte [] contentsOnly = Arrays.copyOf(byteBufferLine.array(), byteBufferLine.position());
		
		String str = new String(contentsOnly, ConstantsDWAR.CHARSET_ENCODING);
		
		StringBuilder sb = new StringBuilder(str);
		
		pipeline.addData(sb.toString());
		
		((Buffer)byteBufferLine).clear();
	}
	
	
	
//	private int readLine2List() throws IOException {
//		
//		ByteBuffer buffer = ByteBuffer.allocate(CAPACITY);
//		
//		int size = byteChannel.read(buffer);
//				
//		if(VERBOSE){
//			System.out.println("StringReadChannel readLine2List() size " + size);
//		}
//		
//		
//		if(size==-1){
//			
//			if(byteBufferLine.position() > 0){
//			
//				byte [] contentsOnly = Arrays.copyOf(byteBufferLine.array(), byteBufferLine.position());
//							
//				String str = new String(contentsOnly, ConstantsDWAR.CHARSET_ENCODING);
//				
//				StringBuilder sb = new StringBuilder(str);
//				
//				liLine.add(sb);
//			
//			}
//			
//			bEOF = true;
//			return -1;
//		}
//		
//		for (int i = 0; i < size; i++) {
//			byte c = buffer.get(i);
//			
//			if(c==-1) {
//				bEOF=true;
//			} else {
//				if(c=='\n') {
//										
//					byte [] contentsOnly = Arrays.copyOf(byteBufferLine.array(), byteBufferLine.position());
//					
//					byteBufferLine.clear();
//					
//					String str = new String(contentsOnly, ConstantsDWAR.CHARSET_ENCODING);
//					
//					StringBuilder sb = new StringBuilder(str);
//					
//					liLine.add(sb);
//										
//				} else {
//					if(c!='\r') {
//					
//						byteBufferLine.put(c);
//											
//					}
//				}
//			}
//		}
//		
//		return size;
//	}
	
    public static void skipUntilLineMatchesRegEx(StringReadChannel src, String regex) throws NoSuchFieldException, IOException {
    	int limit = 10000;
    	
    	skipUntilLineMatchesRegEx(src, regex, limit);
    }
    
    public static String skipUntilLineMatchesRegEx(StringReadChannel src, String regex, int limit) throws NoSuchFieldException, IOException {
    	    	    		
    	String line = src.readLine();
    	
    	boolean match = false;
    	
    	if(line.matches(regex)){
    		match = true;
    	}
    	
    	int cc=0;
    	
		while(!match){
    		
			if(!src.hasMoreLines()){
				break;
			}
			
			line = src.readLine();
			
	    	if(line.matches(regex)){
	    		match = true;
	    	} 
			
	    	cc++;
	    	
	    	if(cc > limit){
	    		break;
	    	}
	    	
    		
    	}	
		
		String lineMatch = null;
		
		if(match){
			
			lineMatch = line;
			
		} else {
			throw new NoSuchFieldException("Regex " + regex + " was not found.");
		}
    	
		return lineMatch;
		
    	
    }

    public void close() throws IOException {
    	byteChannel.close();
    }

}
