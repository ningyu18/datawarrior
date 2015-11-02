/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
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
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;
import org.openmolecules.chem.conf.so.SelfOrganizedConformer;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.optimizer.AlgoLBFGS;
import com.actelion.research.forcefield.optimizer.EvaluableConformation;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.gui.viewer2d.MoleculeViewer;
import com.actelion.research.util.DoubleFormat;

public class DEConformerDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 0x20130605;

	private static final String DATAWARRIOR_DEBUG_FILE = "../../../data/ccdc/ConformationGeneratorConformersDebug.dwar";
	private static final int CONFORMATION_COUNT = 9;
	private static final String[] OPTION_TEXT = { "Adaptive collision avoidance, low energy bias; MMFF94",
												  "Adaptive collision avoidance, low energy bias",
												  "Random, low energy bias",
												  "Pure random",
												  "Systematic, low energy bias",
												  "Diverse self-organized; MMFF94",
												  "Diverse self-organized" };
	private static final String OPTION_TEXT_ACTELION3D = "Actelion3D and Actelion forcefield minimized";

	private static final int OPTION_ADAPTIVE_RANDOM_MMFF94 = 0;
	private static final int OPTION_ADAPTIVE_RANDOM = 1;
	private static final int OPTION_LIKELY_RANDOM = 2;
	private static final int OPTION_PURE_RANDOM = 3;
	private static final int OPTION_LIKELY_SYSTEMATIC = 4;
	private static final int OPTION_SELF_ORGANIZED_MMFF94 = 5;
	private static final int OPTION_SELF_ORGANIZED = 6;
	private static final int OPTION_ACTELION3D = 7;

	private static final int DEFAULT_OPTION = OPTION_ADAPTIVE_RANDOM_MMFF94;

	private StereoMolecule		mMolecule;
	private JComboBox			mComboBoxOption;
	private ConformationPanel[]	mConformationPanel;

	public DEConformerDialog(Frame parent, StereoMolecule mol) {
		super(parent, "Conformer Explorer", false);

		mMolecule = mol;

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(3,3));

		mConformationPanel = new ConformationPanel[CONFORMATION_COUNT];
		for (int i=0; i<CONFORMATION_COUNT; i++) {
			mConformationPanel[i] = new ConformationPanel(parent, i);
			mainPanel.add(mConformationPanel[i]);
			}

		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.FILL, 8, TableLayout.PREFERRED, 8, TableLayout.FILL, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new TableLayout(size));

		JButton button = new JButton("Generate Conformers");
		button.addActionListener(this);
		buttonPanel.add(button, "1,1");

		mComboBoxOption = new JComboBox(OPTION_TEXT);
		if (System.getProperty("development") != null)
			mComboBoxOption.addItem(OPTION_TEXT_ACTELION3D);
		mComboBoxOption.setSelectedIndex(DEFAULT_OPTION);
		buttonPanel.add(mComboBoxOption, "3,1");

        if (System.getProperty("development") != null) {
			JButton dwButton = new JButton("Write DW Files");
			dwButton.addActionListener(this);
			buttonPanel.add(dwButton, "7,1");
			}

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton, "11,1");

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		setSize(1024, 768);
		setLocationRelativeTo(parent);
		setVisible(true);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Close")) {
			setVisible(false);
			dispose();
			return;
			}

		if (e.getActionCommand().startsWith("Generate")) {
			int option = mComboBoxOption.getSelectedIndex();
			generateConformers(option);
			return;
			}

		if (e.getActionCommand().startsWith("Write DW Files")) {
			writeDataWarriorDebugFile();
			return;
			}
		}

	public void generateConformers() {
		generateConformers(DEFAULT_OPTION);
		}

	public void generateConformers(int option) {
		HashMap<String, Object> mmffOptions = null;
		if (option == OPTION_ADAPTIVE_RANDOM_MMFF94
		 || option == OPTION_SELF_ORGANIZED_MMFF94) {
			mmff.ForceField.initialize(mmff.ForceField.MMFF94);
			mmffOptions = new HashMap<String, Object>();
			}

		for (int i=0; i<CONFORMATION_COUNT; i++) {
			mConformationPanel[i].setMolecule(null);
			mConformationPanel[i].setText("");
			}

		StereoMolecule mol = new StereoMolecule(mMolecule);
		mol.stripSmallFragments();
		ConformerGenerator.addHydrogenAtoms(mol);

		int[] rotatableBond = null;
		ArrayList<TorsionDescriptor> torsionDescriptorList = null;
		if (option == OPTION_ADAPTIVE_RANDOM_MMFF94
		 || option == OPTION_SELF_ORGANIZED_MMFF94) {
			rotatableBond = TorsionDescriptor.getRotatableBonds(mol);
			torsionDescriptorList = new ArrayList<TorsionDescriptor>();
			}

		if (option == OPTION_LIKELY_SYSTEMATIC) {
			ConformerGenerator cg = new ConformerGenerator();
//				cg.setRandomSeed(sSeedStart);
			if (cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC)) {
				for (int i=0; i<CONFORMATION_COUNT; i++) {
					StereoMolecule conformer = cg.getNextConformer(null);
					if (conformer == null) {
						mConformationPanel[i].setText("no more conformers");
						}
					else {
						mConformationPanel[i].setText("Contribution:"+DoubleFormat.toString(cg.getPreviousConformerContribution()));
						mConformationPanel[i].setMolecule(conformer);
						}
				  	}
				}
			else {
				for (int i=0; i<CONFORMATION_COUNT; i++)
					mConformationPanel[i].setText("structure validation problem");
				}
			return;
			}

		if (option == OPTION_PURE_RANDOM) {
			ConformerGenerator cg = new ConformerGenerator();
//				cg.setRandomSeed(sSeedStart);
			if (cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM)) {
				for (int i=0; i<CONFORMATION_COUNT; i++) {
					StereoMolecule conformer = cg.getNextConformer(null);
					if (conformer == null) {
						mConformationPanel[i].setText("max tries exceeded");
						}
					else {
						mConformationPanel[i].setText("Contribution:"+DoubleFormat.toString(cg.getPreviousConformerContribution()));
						mConformationPanel[i].setMolecule(conformer);
						}
					}
			  	}
			else {
				for (int i=0; i<CONFORMATION_COUNT; i++)
					mConformationPanel[i].setText("structure validation problem");
				}
			return;
			}

		if (option == OPTION_LIKELY_RANDOM) {
			ConformerGenerator cg = new ConformerGenerator();
//				cg.setRandomSeed(sSeedStart);
			if (cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM)) {
				for (int i=0; i<CONFORMATION_COUNT; i++) {
					StereoMolecule conformer = cg.getNextConformer(null);
					if (conformer == null) {
						mConformationPanel[i].setText("max tries exceeded");
						}
					else {
						mConformationPanel[i].setMolecule(conformer);
						mConformationPanel[i].setText("contribution:"+DoubleFormat.toString(cg.getPreviousConformerContribution()));
						}
					}
			  	}
			else {
				for (int i=0; i<CONFORMATION_COUNT; i++)
					mConformationPanel[i].setText("structure validation problem");
				}
			return;
			}

		if (option == OPTION_ADAPTIVE_RANDOM || option == OPTION_ADAPTIVE_RANDOM_MMFF94) {
			ConformerGenerator cg = new ConformerGenerator();
//				cg.setRandomSeed(sSeedStart);
			if (cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM)) {
				for (int i=0; i<CONFORMATION_COUNT; i++) {
					StereoMolecule conformer = cg.getNextConformer(null);
					if (conformer == null) {
						mConformationPanel[i].setText("max tries exceeded");
						}
					else {
						String message = "";
						if (option == OPTION_ADAPTIVE_RANDOM_MMFF94) {
							try {
								mmff.ForceField ff = new mmff.ForceField(conformer, mmff.ForceField.MMFF94, mmffOptions);
								int error = ff.minimise(10000, 0.0001, 1.0e-6);
								if (error != 0) {
									message = "MMFF94 error code: "+error;
									}
								else {
									// if we minimize, we need to check for redundancy again
									if (isRedundantConformer(conformer, rotatableBond, torsionDescriptorList)) {
										i--;
										continue;
										}
									
									message = "MMFF94 energy: "+DoubleFormat.toString(ff.getTotalEnergy())+" kcal/mol";
									}
								}
							catch (Exception e) {
								message = "MMFF94:"+e.toString();
								}
							}
						else {
							message = "contribution:"+DoubleFormat.toString(cg.getPreviousConformerContribution());
							}

						mConformationPanel[i].setText(message);
						mConformationPanel[i].setMolecule(conformer);
						}
				  	}
				}
			else {
				for (int i=0; i<CONFORMATION_COUNT; i++)
					mConformationPanel[i].setText("structure validation problem");
				}
			return;
			}

		if (option == OPTION_SELF_ORGANIZED || option == OPTION_SELF_ORGANIZED_MMFF94) {
			ConformationSelfOrganizer sampler = new ConformationSelfOrganizer(mol, true);
			sampler.initializeConformers(0, -1);
			for (int i=0; i<CONFORMATION_COUNT; i++) {
				SelfOrganizedConformer conformer = sampler.getNextConformer();
				if (conformer == null) {
					mConformationPanel[i].setText("no more conformers");
					}
				else {
					String message = "";
					StereoMolecule aConformer = conformer.toMolecule();
					if (option == OPTION_SELF_ORGANIZED_MMFF94) {
						try {
							mmff.ForceField ff = new mmff.ForceField(aConformer, mmff.ForceField.MMFF94, mmffOptions);
							int error = ff.minimise(10000, 0.0001, 1.0e-6);
							if (error != 0) {
								message = "MMFF94 error code: "+error;
								}
							else {
								// if we minimize, we need to check for redundancy again
								if (isRedundantConformer(aConformer, rotatableBond, torsionDescriptorList)) {
									i--;
									continue;
									}

								message = "MMFF94 energy: "+DoubleFormat.toString(ff.getTotalEnergy())+" kcal/mol";
								}
							}
						catch (Exception e) {
							message = "MMFF94:"+e.toString();
							}
						}
					else {
						message = "raw conformer; strain: "+DoubleFormat.toString(conformer.getTotalStrain());
						}
					
					mConformationPanel[i].setText(message);
					mConformationPanel[i].setMolecule(aConformer);
					}
				}
			return;
			}

		if (option == OPTION_ACTELION3D) {
			List<FFMolecule> isomerList = TorsionCalculator.createAllConformations(new FFMolecule(mol));
			for (int i=0; i<CONFORMATION_COUNT; i++) {
				if (i >= isomerList.size()) {
					mConformationPanel[i].setMolecule(null);
					mConformationPanel[i].setText("no more conformers");
					}
				else {
					FFMolecule ffmol = isomerList.get(i);

					try {
						ForceField f = new ForceField(ffmol);
						new AlgoLBFGS().optimize(new EvaluableConformation(f));	//optimize torsions -> 6+nRot degrees of freedom, no change of angles and bond distances
						new AlgoLBFGS().optimize(new EvaluableForceField(f));	//optimize cartesians -> 3n degrees of freedem
						mConformationPanel[i].setText("minimized conformer");
						}
					catch (Exception e) {
						mConformationPanel[i].setText("raw conformer; minimization failed");
						}

					mConformationPanel[i].setMolecule(ffmol.toStereoMolecule());
					}
				}
			}
		}

	private boolean isRedundantConformer(StereoMolecule mol, int[] rotatableBond, ArrayList<TorsionDescriptor> torsionDescriptorList) {
		TorsionDescriptor ntd = new TorsionDescriptor(mol, rotatableBond);
		for (TorsionDescriptor td:torsionDescriptorList)
			if (td.equals(ntd))
				return true;

		torsionDescriptorList.add(ntd);
		return false;
		}

	private void writeDataWarriorDebugFile() {
		ConformerGenerator cg = new ConformerGenerator(12345L);
		cg.setWriteFragmentFile();
		cg.initializeConformers(mMolecule.getCompactCopy(), ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC);
		StereoMolecule conformer = cg.getNextConformer(null);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(DATAWARRIOR_DEBUG_FILE));
	        bw.write("<column properties>");
	        bw.newLine();
	        bw.write("<columnName=\"Structure\">");
	        bw.newLine();
	        bw.write("<columnProperty=\"specialType\tidcode\">");
	        bw.newLine();
	        bw.write("<columnName=\"coords\">");
	        bw.newLine();
	        bw.write("<columnProperty=\"specialType\tidcoordinates3D\">");
	        bw.newLine();
	        bw.write("<columnProperty=\"parent\tStructure\">");
	        bw.newLine();
	        bw.write("</column properties>");
	        bw.newLine();
	        bw.write("Structure\tcoords\ttorsionIndexes\tcollision");
	        bw.newLine();
			while (conformer != null) {
				if (cg.mDiagnosticCollisionAtoms != null) {
					conformer.setAtomicNo(cg.mDiagnosticCollisionAtoms[0], 5);
					conformer.setAtomicNo(cg.mDiagnosticCollisionAtoms[1], 5);
					}
				Canonizer canonizer = new Canonizer(conformer);
				String idcode = canonizer.getIDCode();
				String coords = canonizer.getEncodedCoordinates();
				bw.write(idcode+"\t"+coords+"\t"+cg.mDiagnosticTorsionString+"\t"+cg.mDiagnosticCollisionString);
				bw.newLine();
				conformer = cg.getNextConformer(null);
				}
			bw.close();
			}
		catch (IOException ioe) {
			ioe.printStackTrace();
			}
		}

	class ConformationPanel extends JPanel {
		private static final long serialVersionUID = 0x20080217;

		private Frame				mParentFrame;
		private JLabel				mLabel;
		private MoleculeViewer		mViewer;
		private StereoMolecule		mMolecule;		
	
		public ConformationPanel(Frame parent, int no) {
			mParentFrame = parent;
			mViewer = new MoleculeViewer();
			mViewer.addActionProvider(new RayTraceActionProvider(parent));
			mLabel = new JLabel("Conformation "+(no+1), SwingConstants.CENTER);
			mLabel.setFont(new Font("Helvetica",0,14));
			mLabel.setForeground(Color.cyan);
			mLabel.setBackground(new Color(99,99,99));
			mLabel.setOpaque(true);
	
			setLayout(new BorderLayout());
			add(mViewer, BorderLayout.CENTER);
			add(mLabel, BorderLayout.SOUTH);
			setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
			}
	
		public void setMolecule(StereoMolecule mol) {
			if (mol == null) {
				mViewer.setMolecule((Molecule)null);
				mViewer.repaint();
				return;
				}
	
			mMolecule = mol;
			mMolecule.ensureHelperArrays(Molecule.cHelperCIP);
			mViewer.setMolecule(mMolecule);
			mViewer.resetView();
			mViewer.repaint();
			}
	
		public void setText(String text) {
			mLabel.setText(text);
			}
		}
	}