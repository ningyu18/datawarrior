package com.actelion.research.forcefield.mmff;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.TermList;
import com.actelion.research.forcefield.interaction.ProteinLigandTerm;
import com.actelion.research.forcefield.optimizer.PreOptimizer;

public class MMFFTermList extends TermList implements Cloneable {
    
    private Tables table;


    public MMFFTermList() {
    	this(new MMFFConfig());
    }
    
    public MMFFTermList(MMFFConfig config) {
    	super(config);
    	
//    	assert false;
    }
    
    @Override
	public MMFFConfig getConfig() {
		return (MMFFConfig) super.getConfig();
	}
    
	@Override
	public void prepareMolecule(FFMolecule mol) {

		boolean changed = false;
		// Add the hydrogens (or remove), without placing them
		if(getConfig().isPrepareMolecule()) {
			if(StructureCalculator.removeLonePairs(mol)) changed = true;
			if(StructureCalculator.addHydrogens(mol, getConfig().isUseHydrogenOnProtein())) changed = true;
		}

		//Set the MMFF atom types
		MMFFParameters.setAtomTypes(mol);

		// Set the interactions atom classes
		if (mol.getNMovables()<mol.getAllAtoms() && getConfig().isUsePLInteractions()) getConfig().getClassStatistics().setClassIdsForMolecule(mol);

		// Preoptimize the H
		if(getConfig().isPrepareMolecule()) {			
			if(changed) PreOptimizer.preOptimizeHydrogens(mol, getConfig());
		}
		table = getTables(getConfig().getTableName());
		
	}
	
	

	@Override
	public void init(FFMolecule mol) {
		setMolecule(mol);
		clear();
		
		MMFFConfig conf = (MMFFConfig) config;
        
        Separation sep = new Separation(mol);

        addAll(AngleBend.findIn(table, mol));

        addAll(BondStretch.findIn(table, mol));

        if (conf.isUseElectrostatic()) {
            addAll(Electrostatic.findIn(table, mol, sep, conf.getMaxDistance(), conf.isDielectricDistance(), conf.getDielectric()));
        }

        addAll(OutOfPlane.findIn(table, mol));

        addAll(StretchBend.findIn(table, mol));

        addAll(TorsionAngle.findIn(table, mol));

        if (conf.isUseVanDerWaals()) {
            addAll(VanDerWaals.findIn(table, mol, sep, conf.getMaxDistance()));
        }

        addProteinLigandTerms(mol);
      			
	}

	@Override
	public double getBondDistance(int a1, int a2) {
		return table.bond.r0(getMolecule(), a1, a2); 
	}
	
	
    private static Map<String, Tables> tables = new HashMap<String, Tables>();

    public static Tables getTables(String tableSet) {
    	Tables t = tables.get(tableSet);
    	if(t==null) {
    		synchronized (tables) {
    			if (MMFFConfig.MMFF94.equals(tableSet)) {
    				t = Tables.newMMFF94();
    			}
    	    	if (MMFFConfig.MMFF94S.equals(tableSet)) {
    	    		t = Tables.newMMFF94s();
    	    	}
    	    	tables.put(tableSet, t);
			}
    	}
    	return t;
    	
    }

    @Override
    public String toString() {
		double sum = 0;
		double sum1 = 0, sum2 = 0, sum3 = 0, sum4 = 0, sum5 = 0, sum6 = 0, sum7 = 0, sum8 = 0;
		int n1 = 0, n2 = 0, n3 = 0, n4 = 0, n5 = 0, n6 = 0, n7 = 0, n8 = 0;
				
		double others = 0;
		int n=0;
		for(int k=0; k<size(); k++) {
			AbstractTerm term = get(k);
			double e = term.getFGValue(null);
			sum += e;
			
			if(term instanceof BondStretch) {sum1 += e; n1++;} 
			else if(term instanceof AngleBend) {sum2 += e; n2++;}
			else if(term instanceof TorsionAngle) {sum3 += e; n3++;}
			else if(term instanceof VanDerWaals) {sum4 += e; n4++;}
			else if(term instanceof Electrostatic) {sum5 += e; n5++;}
			else if(term instanceof OutOfPlane) {sum6 += e; n6++;}
			else if(term instanceof ProteinLigandTerm) {sum7 += e; n7++;}
			else if(term instanceof StretchBend) {sum8 += e; n8++;}
			else {others += e; n++;} 
			
		}				
		DecimalFormat df = new DecimalFormat("0.00");
		StringBuffer sb = new StringBuffer();
		sb.append("Total Potential Energy: " + df.format(sum) + " Kcal/mole   " +System.getProperty("line.separator"));
		if(n1>0) sb.append(" Bond Stretching:    " + df.format(sum1) + "   (" + n1 + ")" + System.getProperty("line.separator"));
		if(n2>0) sb.append(" Angle Bending:      " + df.format(sum2) + "   (" + n2 + ")" + System.getProperty("line.separator"));
		if(n8>0) sb.append(" Stretch-Bend:       " + df.format(sum8) + "   (" + n8 + ")" + System.getProperty("line.separator"));
		if(n6>0) sb.append(" Out-of-Plane Angle: " + df.format(sum6) + "   (" + n6 + ")" + System.getProperty("line.separator"));
		if(n3>0) sb.append(" Torsional Angle:    " + df.format(sum3) + "   (" + n3 + ")" + System.getProperty("line.separator"));
		if(n4>0) sb.append(" Van der Waals:      " + df.format(sum4) + "   (" + n4 + ")" + System.getProperty("line.separator"));
		if(n5>0) sb.append(" Electrostatic:      " + df.format(sum5) + "   (" + n5 + ")" + System.getProperty("line.separator"));
		if(n7>0) sb.append(" Protein-Ligand:     " + df.format(sum7) + "   (" + n7 + ")" + System.getProperty("line.separator"));
		if(n>0)  sb.append(" Others:             " + df.format(others) + "   (" + n + ")" + System.getProperty("line.separator"));
		return sb.toString();
    }


}
