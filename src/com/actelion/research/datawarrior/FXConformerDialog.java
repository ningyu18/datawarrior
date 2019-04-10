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
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior;

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.SuperposeCalculator;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.optimizer.EvaluableConformation;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.forcefield.optimizer.OptimizerLBFGS;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JDrawDialog;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.viewer2d.MoleculeViewer;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import javafx.scene.paint.Color;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;
import org.openmolecules.chem.conf.so.SelfOrganizedConformer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FXConformerDialog extends JDialog implements ActionListener,ChangeListener {
	private static final long serialVersionUID = 0x20130605;

	private static final String DATAWARRIOR_DEBUG_FILE = "/home/thomas/data/debug/conformationGeneratorConformers.dwar";
	private static final String[] OPTION_COUNT = { "one conformer","2 conformers","4 conformers","8 conformers","16 conformers","32 conformers","64 conformers","128 conformers" };

	private static final String[] MINIMIZE_TEXT = { "MMFF94s+", "MMFF94s", "Idorsia-FF", "Don't minimize" };
	private static final int MINIMIZE_MMFFSPLUS = 0;
	private static final int MINIMIZE_MMFFS = 1;
	private static final int MINIMIZE_IDFF = 2;
	private static final int MINIMIZE_NONE = 3;
	private static final int DEFAULT_MINIMIZATION = MINIMIZE_MMFFSPLUS;

	private static final String[] SURFACE_TEXT = { "None", "Wires", "Filled" };
	private static final int SURFACE_NONE = 0;
	private static final int SURFACE_WIRES = 1;
	private static final int SURFACE_FILLED = 2;
	private static final int DEFAULT_SURFACE = SURFACE_NONE;

	private static final int LOW_ENERGY_RANDOM = 0;
	private static final int PURE_RANDOM = 1;
	private static final int ADAPTIVE_RANDOM = 2;
	private static final int SYSTEMATIC = 3;
	private static final int SELF_ORGANIZED = 4;
	private static final int ACTELION3D = 5;
	private static final int DEFAULT_ALGO = LOW_ENERGY_RANDOM;

	private static final String NAME_ENERGY_SEPARATOR = ", ";

	private static final String[] ALGO_TEXT = { "Random, low energy bias", "Pure random", "Adaptive collision avoidance", "Systematic, low energy bias", "Self-organized" };
	private static final String ALGO_TEXT_ACTELION3D = "Actelion3D";

	private static final int DEFAULT_COUNT = 16;

	private StereoMolecule		mMol;
	private JComboBox			mComboBoxCount, mComboBoxAlgo, mComboBoxMinimization,mComboBoxSurface;
	private FXConformerPanel	mConformationPanel;
	private JSlider             mSliderSplitting;
	private int					mPreviousAlgo,mPreviousMinimization;
	private int[]               mSuperposeAtoms;

	public FXConformerDialog(Frame parent, StereoMolecule mol) {
		super(parent, "Conformer Explorer", false);

		mMol = new StereoMolecule(mol);
		mMol.stripSmallFragments(true);

		mConformationPanel = new FXConformerPanel();

		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED,
				8, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, 8,
				TableLayout.PREFERRED, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, TableLayout.FILL,
				TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new TableLayout(size));

		mComboBoxCount = new JComboBox(OPTION_COUNT);
		mComboBoxCount.setSelectedItem(Integer.toString(DEFAULT_COUNT).concat(" conformers"));
		buttonPanel.add(mComboBoxCount, "1,1");

		mComboBoxAlgo = new JComboBox(ALGO_TEXT);
		if (System.getProperty("development") != null)
			mComboBoxAlgo.addItem(ALGO_TEXT_ACTELION3D);
		mComboBoxAlgo.setSelectedIndex(DEFAULT_ALGO);
		buttonPanel.add(mComboBoxAlgo, "3,1");

		mComboBoxMinimization = new JComboBox(MINIMIZE_TEXT);
		mComboBoxMinimization.setSelectedIndex(DEFAULT_MINIMIZATION);
		buttonPanel.add(mComboBoxMinimization, "5,1");

		JButton button = new JButton("Generate");
		button.addActionListener(this);
		buttonPanel.add(button, "7,1");

		mSliderSplitting = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		mSliderSplitting.addChangeListener(this);
		mSliderSplitting.setPreferredSize(new Dimension(HiDPIHelper.scale(120), mSliderSplitting.getPreferredSize().height));
		buttonPanel.add(new JLabel("Separate:"), "9,1");
		buttonPanel.add(mSliderSplitting, "10,1");

		mComboBoxSurface = new JComboBox(SURFACE_TEXT);
		mComboBoxSurface.setSelectedIndex(DEFAULT_SURFACE);
		mComboBoxSurface.addActionListener(this);
		buttonPanel.add(new JLabel("Surface:"), "12,1");
		buttonPanel.add(mComboBoxSurface, "13,1");

		JButton superposeButton = new JButton("Superpose...");
		superposeButton.addActionListener(this);
		buttonPanel.add(superposeButton, "15,1");

		JMenu saveMenu = new JMenu("Save");
		JMenuItem saveNativeItem = new JMenuItem("As DataWarrior File...");
		saveNativeItem.addActionListener(this);
		saveMenu.add(saveNativeItem);

		JMenuItem saveSDF2Item = new JMenuItem("As SD-File (V2)...");
		saveSDF2Item.addActionListener(this);
		saveMenu.add(saveSDF2Item);

		JMenuItem saveSDF3Item = new JMenuItem("As SD-File (V3)...");
		saveSDF3Item.addActionListener(this);
		saveMenu.add(saveSDF3Item);

		if (System.getProperty("development") != null) {
			JMenuItem debugItem = new JMenuItem("Write DW Files");
			debugItem.addActionListener(this);
			saveMenu.add(debugItem);
			}

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(saveMenu);
		buttonPanel.add(menuBar, "17,1");

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mConformationPanel, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		setSize(HiDPIHelper.scale(1024), HiDPIHelper.scale(768));
		setLocationRelativeTo(parent);
		setVisible(true);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxSurface) {
			mConformationPanel.setConollySurfaceMode(mComboBoxSurface.getSelectedIndex());
			return;
			}

		if (e.getActionCommand().equals("Close")) {
			setVisible(false);
			dispose();
			return;
			}

		if (e.getActionCommand().equals("Superpose...")) {
			String idcode1 = new Canonizer(mMol).getIDCode();
			JDrawDialog dd = new JDrawDialog((Frame)getOwner(), new StereoMolecule(mMol), "Select Atoms to be Superposed...");
			dd.setVisible(true);

			mMol = dd.getStructure();
			String idcode2 = new Canonizer(mMol).getIDCode();
			boolean isPreviousStructure = idcode1.equals(idcode2);

			// if we have selected atoms, we use them as core atoms
			int count = 0;
			for (int atom = 0; atom< mMol.getAllAtoms(); atom++)
				if (mMol.isSelectedAtom(atom))
					count++;
			if (count != 0) {
				mSuperposeAtoms = new int[count];
				int index = 0;
				for (int atom = 0; atom< mMol.getAllAtoms(); atom++)
					if (mMol.isSelectedAtom(atom))
						mSuperposeAtoms[index++] = atom;
				}
			else if (!isPreviousStructure) {
				mSuperposeAtoms = null; // for a new structure we need also new superpose atoms
				}

			if (isPreviousStructure) {  // remove, superpose and add existing conformers
				ArrayList<Conformer> conformerList = mConformationPanel.getConformers();
				mConformationPanel.clear();
				mSliderSplitting.setValue(0);
				mComboBoxSurface.setSelectedIndex(0);
				addConformersToPanel(conformerList);
				}
			else {
				int algo = mComboBoxAlgo.getSelectedIndex();
				int minimization = mComboBoxMinimization.getSelectedIndex();
				count = (1 << mComboBoxCount.getSelectedIndex());
				generateConformers(algo, minimization, count);
				}

			return;
			}

		if (e.getActionCommand().equals("Generate")) {
			int algo = mComboBoxAlgo.getSelectedIndex();
			int minimization = mComboBoxMinimization.getSelectedIndex();
			int count = (1 << mComboBoxCount.getSelectedIndex());
			generateConformers(algo, minimization, count);
			return;
			}

		if (e.getActionCommand().equals("As DataWarrior File...")) {
			saveConformers(FileHelper.cFileTypeDataWarrior);
			return;
			}

		if (e.getActionCommand().equals("As SD-File (V2)...")) {
			saveConformers(FileHelper.cFileTypeSDV2);
			return;
			}

		if (e.getActionCommand().equals("As SD-File (V3)...")) {
			saveConformers(FileHelper.cFileTypeSDV3);
			return;
			}

		if (e.getActionCommand().equals("Write DW Files")) {
			writeDataWarriorDebugFile();
			return;
			}
		}

	@Override
	public void stateChanged(ChangeEvent e) {
		double value = (double)((JSlider)e.getSource()).getValue()/100;
		mConformationPanel.setConformerSplitting(value*value);
		}

	public void generateConformers() {
		generateConformers(DEFAULT_ALGO, MINIMIZE_MMFFSPLUS, DEFAULT_COUNT);
		}

	public void generateConformers(int algo, int minimization, int maxConformers) {
		mConformationPanel.clear();

		mPreviousAlgo = algo;
		mPreviousMinimization = minimization;

		mSliderSplitting.setValue(0);
		mComboBoxSurface.setSelectedIndex(0);

		ConformerGenerator cg = null;
		ConformationSelfOrganizer cs = null;
		List<FFMolecule> isomerList = null;
		int[] rotatableBond = null;
		ArrayList<TorsionDescriptor> torsionDescriptorList = null;

		ArrayList<Conformer> conformerList = new ArrayList<Conformer>();

		int maxTorsionSets = (int) Math.max(2 * maxConformers, (1000 * Math.sqrt(maxConformers)));

		if (minimization == MINIMIZE_MMFFSPLUS)
			mmff.ForceField.initialize(mmff.ForceField.MMFF94SPLUS);
		if (minimization == MINIMIZE_MMFFS)
			mmff.ForceField.initialize(mmff.ForceField.MMFF94S);

		StereoMolecule mol = new StereoMolecule(mMol);

		for (int i = 0; i < maxConformers; i++) {
			Conformer conformer = null;
			FFMolecule ffmol = null;
			String message = null;

			switch (algo) {
				case ADAPTIVE_RANDOM:
					if (cg == null) {
						cg = new ConformerGenerator();
						cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, maxTorsionSets, false);
					}
					conformer = cg.getNextConformer();
					break;
				case SYSTEMATIC:
					if (cg == null) {
						cg = new ConformerGenerator();
						cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC, maxTorsionSets, false);
					}
					conformer = cg.getNextConformer();
					break;
				case LOW_ENERGY_RANDOM:
					if (cg == null) {
						cg = new ConformerGenerator();
						cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, maxTorsionSets, false);
					}
					conformer = cg.getNextConformer();
					break;
				case PURE_RANDOM:
					if (cg == null) {
						cg = new ConformerGenerator();
						cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM, maxTorsionSets, false);
					}
					conformer = cg.getNextConformer();
					break;
				case SELF_ORGANIZED:
					if (cs == null) {
						ConformerGenerator.addHydrogenAtoms(mol);
						cs = new ConformationSelfOrganizer(mol, true);
						cs.initializeConformers(0, maxConformers);
					}
					conformer = cs.getNextConformer();
					break;
				case ACTELION3D:
					try {
						if (isomerList == null) {
							ConformerGenerator.addHydrogenAtoms(mol);
							isomerList = TorsionCalculator.createAllConformations(new FFMolecule(mol));
						}
						if (isomerList.size() > i)
							ffmol = isomerList.get(i);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
			}

			if (conformer == null && ffmol == null)
				break;

			// if we minimize, we check again, whether the minimized conformer is a very similar sibling in the list
			boolean isRedundantConformer = false;
			MinimizationResult result = null;

			if (minimization == MINIMIZE_NONE) {
				if (ffmol != null) {
					ffmol.copyCoordinatesTo(mol);
					conformer = new Conformer(mol);
					}
				if (cg != null) {
					message = DoubleFormat.toString(cg.getPreviousConformerContribution() * 100, 3) + "%";
					conformer.setEnergy(cg.getPreviousConformerContribution());
				} else if (cs != null) {
					message = " strain:" + DoubleFormat.toString(((SelfOrganizedConformer) conformer).getTotalStrain(), 3);
					conformer.setEnergy(((SelfOrganizedConformer) conformer).getTotalStrain());
				}
			} else {
				result = new MinimizationResult();

				if (minimization == MINIMIZE_MMFFSPLUS) {
					if (ffmol != null)
						ffmol.copyCoordinatesTo(mol);
					else
						conformer.copyTo(mol);

					minimizeMMFF(mol, result, mmff.ForceField.MMFF94SPLUS);
				}
				else if (minimization == MINIMIZE_MMFFS) {
					if (ffmol != null)
						ffmol.copyCoordinatesTo(mol);
					else
						conformer.copyTo(mol);

					minimizeMMFF(mol, result, mmff.ForceField.MMFF94S);
				} else {  // Idorsia FF
					if (ffmol == null) {
						conformer.copyTo(mol);
						ffmol = new FFMolecule(mol);
					}

					minimizeIDFF(ffmol, result);
					ffmol.copyCoordinatesTo(mol);
				}

				if (conformer == null)
					conformer = new Conformer(mol);
				else
					conformer.copyFrom(mol);

				conformer.setEnergy(result.energy);
				message = (result.errorMessage != null) ? result.errorMessage : "E:" + DoubleFormat.toString(result.energy, 3) + " kcal/mol";

				if (rotatableBond == null)
					rotatableBond = TorsionDescriptor.getRotatableBonds(mol);
				if (torsionDescriptorList == null)
					torsionDescriptorList = new ArrayList<TorsionDescriptor>();

				TorsionDescriptor ntd = new TorsionDescriptor(mol, rotatableBond);
				for (TorsionDescriptor td : torsionDescriptorList) {
					if (td.equals(ntd)) {
						isRedundantConformer = true;
						break;
					}
				}
				if (!isRedundantConformer)
					torsionDescriptorList.add(ntd);
			}

			if (!isRedundantConformer) {
				conformer.setName("Conf" + (i + 1) + (message == null ? "" : NAME_ENERGY_SEPARATOR + message));
				conformerList.add(conformer);
			}
		}

		conformerList.sort((c1, c2) -> {
			if (Double.isNaN(c1.getEnergy()) || Double.isNaN(c2.getEnergy()))
				return !Double.isNaN(c1.getEnergy()) ? -1 : !Double.isNaN(c2.getEnergy()) ? 1 : 0;
			int comparison = c1.getEnergy() < c2.getEnergy() ? -1 : c1.getEnergy() == c2.getEnergy() ? 0 : 1;
			return c1.getName().endsWith("%") ? -comparison : comparison;
		});

		addConformersToPanel(conformerList);
	}

	/**
	 * Superposes all conformers of the list using either the the previously defined list
	 * mSuperposeAtoms or, if this is null, all atoms of the most centric rigid fragment.
	 * Then all conformers are added to the panel.
	 * @param conformerList
	 */
	private void addConformersToPanel(ArrayList<Conformer> conformerList) {
		Color[] color = new Color[conformerList.size()];
		for (int i=0; i<conformerList.size(); i++)
			color[i] = Color.hsb(360f*i/conformerList.size(), 0.75, 0.6);

		Matrix4d matrix = null;
		Coordinates[] refCoords = null;
		Coordinates[] coords = null;

		int conformerIndex = 0;
		for (Conformer conformer:conformerList) {
			if (conformerIndex == 0) {
				centerMolecule(conformer);
				if (conformerList.size() > 1) {
					matrix = new Matrix4d();
					if (mSuperposeAtoms == null)
						mSuperposeAtoms = findSuperposeAtoms(conformer.getMolecule());
					coords = new Coordinates[mSuperposeAtoms.length];
					refCoords = new Coordinates[mSuperposeAtoms.length];
					for (int i=0; i<mSuperposeAtoms.length; i++)
						refCoords[i] = conformer.getCoordinates(mSuperposeAtoms[i]);
					}
				}
			else {
				for (int i=0; i<mSuperposeAtoms.length; i++)
					coords[i] = conformer.getCoordinates(mSuperposeAtoms[i]);
				SuperposeCalculator.superpose(refCoords, coords, matrix);
				int index = 0;
				for (int atom=0; atom<conformer.getSize(); atom++) {
					if (index < mSuperposeAtoms.length && atom == mSuperposeAtoms[index]) {
						index++;
						continue;
					}
					Coordinates c = conformer.getCoordinates(atom);
					Point3d p = new Point3d(c.x, c.y, c.z);
					matrix.transform(p);
					c.x = p.x;
					c.y = p.y;
					c.z = p.z;
					}
				}

			mConformationPanel.addMolecule(conformer, color[conformerIndex++]);
			}
		}

	/**
	 * @param mol receives minimized coodinates; taken as start conformer if ffmol == null
	 * @param result receives energy and possibly error message
	 */
	private void minimizeMMFF(StereoMolecule mol, MinimizationResult result, String tableSet) {
		try {
			int[] fragmentNo = new int[mol.getAllAtoms()];
			int fragmentCount = mol.getFragmentNumbers(fragmentNo, false, true);
			if (fragmentCount == 1) {
				mmff.ForceField ff = new mmff.ForceField(mol, tableSet, new HashMap<String, Object>());
				int error = ff.minimise(10000, 0.0001, 1.0e-6);
				if (error != 0)
					throw new Exception("MMFF94 error code "+error);
				result.energy = (float)ff.getTotalEnergy();
			}
			else {
				int maxAtoms = 0;

				StereoMolecule[] fragment = mol.getFragments(fragmentNo, fragmentCount);
				for (StereoMolecule f:fragment) {
					if (f.getAllAtoms() > 2) {
						mmff.ForceField ff = new mmff.ForceField(f, tableSet, new HashMap<String, Object>());
						int error = ff.minimise(10000, 0.0001, 1.0e-6);
						if (error != 0)
							throw new Exception("MMFF94 error code "+error);

						if (maxAtoms < f.getAllAtoms()) {	// we take the energy value from the largest fragment
							maxAtoms = f.getAllAtoms();
							result.energy = (float)ff.getTotalEnergy();
						}
					}
				}
				int[] atom = new int[fragmentCount];
				for (int i=0; i<fragmentNo.length; i++) {
					int f = fragmentNo[i];
					mol.setAtomX(i, fragment[f].getAtomX(atom[f]));
					mol.setAtomY(i, fragment[f].getAtomY(atom[f]));
					mol.setAtomZ(i, fragment[f].getAtomZ(atom[f]));
					atom[f]++;
					}
				}
			}
		catch (Exception e) {
			e.printStackTrace();
			result.energy = Double.NaN;
			result.errorMessage = "MMFF-err:"+e.toString();
			}
		}

	/**
	 * @param ffmol if not null this is taken as start conformer
	 * @param result receives energy and possibly error message
	 */
	private void minimizeIDFF(FFMolecule ffmol, MinimizationResult result) {
		try {
			ForceField f = new ForceField(ffmol);
			new OptimizerLBFGS().optimize(new EvaluableConformation(f));	//optimize torsions -> 6+nRot degrees of freedom, no change of angles and bond distances
			result.energy = (float)new OptimizerLBFGS().optimize(new EvaluableForceField(f));	//optimize cartesians -> 3n degrees of freedem

			// EvaluableForcefield -> optimize everything in a cartesian referential
			// EvaluableConformation -> optimize the torsions in the torsion referential
			// EvaluableDockFlex -> optimize the torsions + translation/rotation in the torsion referential
			// EvaluableDockRigid -> optimize the translation/rotation in the cartesian referential

			// AlgoLBFGS -> faster algo
			// AlgoConjugateGradient -> very slow, not used anymore
			// AlgoMD -> test of molecular dynamic, not a optimization
			}
		catch (Exception e) {
			result.energy = Double.NaN;
			result.errorMessage = "IDFF-err:"+e.toString();
			}
		}

	private void copyFFMolCoordsToMol(StereoMolecule mol, FFMolecule ffmol) {
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, ffmol.getAtomX(atom));
			mol.setAtomY(atom, ffmol.getAtomY(atom));
			mol.setAtomZ(atom, ffmol.getAtomZ(atom));
			}
		}

	private void centerMolecule(Conformer conformer) {
		Coordinates cog = new Coordinates();
		double atomicNoSum = 0.0;
		StereoMolecule mol = conformer.getMolecule();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			Coordinates c = conformer.getCoordinates(atom);
			cog.add(c.x * mol.getAtomicNo(atom),
					c.y * mol.getAtomicNo(atom),
					c.z * mol.getAtomicNo(atom));
			atomicNoSum += mol.getAtomicNo(atom);
			}
		cog.scale(1.0 / atomicNoSum);

		// Here we move the conformer's internal coordinates to center it around its COG!!!!!!!
		for (int atom=0; atom<mol.getAllAtoms(); atom++)
			conformer.getCoordinates(atom).sub(cog);
		}

	private int[] findSuperposeAtoms(StereoMolecule mol) {
		boolean[] isRotatableBond = new boolean[mol.getAllBonds()];
		int count = TorsionDB.findRotatableBonds(mol, true, isRotatableBond);
		if (count == 0) {
			int[] coreAtom = new int[mol.getAllAtoms()];
			for (int atom=0; atom<mol.getAllAtoms(); atom++)
				coreAtom[atom] = atom;
			return coreAtom;
			}

		int[] fragmentNo = new int[mol.getAllAtoms()];
		int fragmentCount = mol.getFragmentNumbers(fragmentNo, isRotatableBond, true);
		int[] fragmentSize = new int[fragmentCount];
		float[] atad = mol.getAverageTopologicalAtomDistance();
		float[] fragmentATAD = new float[fragmentCount];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			fragmentATAD[fragmentNo[atom]] += atad[atom];
			fragmentSize[fragmentNo[atom]]++;
			}
		int bestFragment = -1;
		float bestATAD = Float.MAX_VALUE;
		for (int i=0; i<fragmentCount; i++) {
			fragmentATAD[i] /= fragmentSize[i];
			if (bestATAD > fragmentATAD[i]) {
				bestATAD = fragmentATAD[i];
				bestFragment = i;
				}
			}
		int fragmentSizeWithNeighbours = fragmentSize[bestFragment];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (fragmentNo[atom] == bestFragment) {
				for (int i = 0; i < mol.getConnAtoms(atom); i++) {
					int connAtom = mol.getConnAtom(atom, i);
					if (fragmentNo[connAtom] != bestFragment
					 && fragmentNo[connAtom] != fragmentCount) {
						fragmentNo[connAtom] = fragmentCount;
						fragmentSizeWithNeighbours++;
						}
					}
				}
			}
		int[] coreAtom = new int[fragmentSizeWithNeighbours];
		int index = 0;
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (fragmentNo[atom] == bestFragment
			 || fragmentNo[atom] == fragmentCount)
				coreAtom[index++] = atom;
		return coreAtom;
		}

	private boolean isRedundantConformer(StereoMolecule mol, int[] rotatableBond, ArrayList<TorsionDescriptor> torsionDescriptorList) {
		TorsionDescriptor ntd = new TorsionDescriptor(mol, rotatableBond);
		for (TorsionDescriptor td:torsionDescriptorList)
			if (td.equals(ntd))
				return true;

		torsionDescriptorList.add(ntd);
		return false;
		}

	private void saveConformers(int fileType) {
		String filename = new FileHelper(this).selectFileToSave("Save Conformers", fileType, "Conformers");
		if (filename == null)
			return;

		File file = new File(filename);
		if (file.exists() && !file.canWrite())
			return;

		String energyTitle = (mPreviousMinimization != MINIMIZE_NONE) ? "Energy in kcal/mol"
				: (mPreviousAlgo == SELF_ORGANIZED) ? "Strain"
				: (mPreviousAlgo == ACTELION3D) ? "" : "Percent Contribution";

		ArrayList<Conformer> conformerList = mConformationPanel.getConformers();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			if (fileType == FileHelper.cFileTypeDataWarrior) {
				bw.write("<column properties>");
				bw.newLine();
				bw.write("<columnName=\"Conformer\">");
				bw.newLine();
				bw.write("<columnProperty=\"specialType\tidcode\">");
				bw.newLine();
				bw.write("<columnName=\"coords\">");
				bw.newLine();
				bw.write("<columnProperty=\"specialType\tidcoordinates3D\">");
				bw.newLine();
				bw.write("<columnProperty=\"parent\tConformer\">");
				bw.newLine();
				bw.write("</column properties>");
				bw.newLine();
				bw.write("Conformer\tcoords\tName");
				if (energyTitle.length() != 0)
					bw.write("\t"+energyTitle);
				bw.newLine();
				for (Conformer conformer:conformerList) {
					conformer.copyTo(conformer.getMolecule());
					Canonizer canonizer = new Canonizer(conformer.getMolecule());
					bw.write(canonizer.getIDCode());
					bw.write('\t');
					bw.write(canonizer.getEncodedCoordinates());
					bw.write('\t');
					bw.write(conformer.getName());
					bw.write('\t');
					if (energyTitle.length() != 0) {
						bw.write(DoubleFormat.toString(conformer.getEnergy()));
						bw.newLine();
						}
					}
				}
			else {
				for (Conformer conformer:conformerList) {
					conformer.copyTo(conformer.getMolecule());

					if (fileType == FileHelper.cFileTypeSDV2)
						new MolfileCreator(conformer.getMolecule()).writeMolfile(bw);
					else
						new MolfileV3Creator(conformer.getMolecule()).writeMolfile(bw);

					bw.write(">  <Conformer Name>");
					bw.newLine();
					bw.write(conformer.getName());
					bw.newLine();
					bw.newLine();

					if (energyTitle.length() != 0) {
						bw.write(">  <"+energyTitle+">");
						bw.newLine();
						bw.write(DoubleFormat.toString(conformer.getEnergy()));
						bw.newLine();
						bw.newLine();
						}

					bw.write("$$$$");
					bw.newLine();
					}
				}

			bw.close();
			}
		catch (IOException ioe) {
			ioe.printStackTrace();
			}
		}

	private void writeDataWarriorDebugFile() {
		ConformerGenerator.WRITE_DW_FRAGMENT_FILE = true;
		ConformerGenerator cg = new ConformerGenerator(12345L);
		cg.initializeConformers(mMol.getCompactCopy(), ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC, 10000, false);
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
		ConformerGenerator.WRITE_DW_FRAGMENT_FILE = false;
		}

	class ConformationPanel extends JPanel {
		private static final long serialVersionUID = 0x20080217;

		private JLabel				mLabel;
		private MoleculeViewer		mViewer;
		private StereoMolecule		mMolecule;		
	
		public ConformationPanel(Frame parent, int no) {
			mViewer = new MoleculeViewer();
			mViewer.addActionProvider(new MolViewerActionCopy(parent));
			mViewer.addActionProvider(new MolViewerActionRaytrace(parent));
			mLabel = new JLabel("Conformation "+(no+1), SwingConstants.CENTER);
			mLabel.setFont(mLabel.getFont().deriveFont(0,14));
			mLabel.setForeground(java.awt.Color.cyan);
			mLabel.setBackground(new java.awt.Color(99,99,99));
			mLabel.setOpaque(true);
	
			setLayout(new BorderLayout());
			add(mViewer, BorderLayout.CENTER);
			add(mLabel, BorderLayout.SOUTH);
			setBorder(BorderFactory.createLineBorder(java.awt.Color.LIGHT_GRAY, 2));
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

	private class MinimizationResult {
		double energy;
		String errorMessage;

		public MinimizationResult() {
			energy = Double.NaN;
			errorMessage = null;
			}
		}
	}
