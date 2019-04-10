package com.actelion.research.forcefield.mmff;

import java.util.HashMap;
import java.util.Map;

import com.actelion.research.forcefield.FFConfig;

public class MMFFConfig extends FFConfig {

	public static final String MMFF94 = "MMFF94";
	public static final String MMFF94S = "MMFF94s";

	private String tableName = MMFF94;
	private Map<String, Object> options = new HashMap<String, Object>();
	
	
	private boolean useVanDerWaals = true;
	private boolean useElectrostatic = true;
	private boolean dielectricDistance = false;
	
	
	
	public MMFFConfig() {
		this(Mode.OPTIMIZATION);
	}
	
	public MMFFConfig(Mode mode) {
		super("MMFF94");
    	setProteinLigandFactor(1);		
		setMode(mode);
	}

	
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public Map<String, Object> getOptions() {
		return options;
	}
	
	public boolean isUseVanDerWaals() {
		return useVanDerWaals;
	}


	public void setUseVanDerWaals(boolean useVanDerWaals) {
		this.useVanDerWaals = useVanDerWaals;
	}


	public boolean isUseElectrostatic() {
		return useElectrostatic;
	}


	public void setUseElectrostatic(boolean useElectrostatic) {
		this.useElectrostatic = useElectrostatic;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	public boolean isDielectricDistance() {
		return dielectricDistance;
	}


	public void setDielectricDistance(boolean dielectricDistance) {
		this.dielectricDistance = dielectricDistance;
	}
	
	@Override
	public void setMode(Mode mode) {
		super.setMode(mode);
	} 


}
