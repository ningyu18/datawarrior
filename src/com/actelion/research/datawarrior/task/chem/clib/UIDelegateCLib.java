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

package com.actelion.research.datawarrior.task.chem.clib;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.RXNFileParser;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class UIDelegateCLib implements ActionListener,ChangeListener,TaskConstantsCLib,TaskUIDelegate {
	private static final int EDITOR_HEIGHT = 360;

	private Component mParent;
	private JDrawPanel	mDrawPanel;
	private JPanel		mReactantPanel;
	private JComboBox mComboBoxMode;
	private ArrayList<CompoundCollectionPane<String[]>> mReactantPaneList;

	public UIDelegateCLib(Component parent) {
		mParent = parent;
		mReactantPaneList = new ArrayList<>();
		}

	@Override
	public JComponent createDialogContent() {
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(this);

		double[][] size1 = { {8, TableLayout.FILL, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, TableLayout.FILL, 8},
							 {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8} };
		JPanel editorPanel = new JPanel();
		editorPanel.setLayout(new TableLayout(size1));

		StereoMolecule mol = new StereoMolecule();
		mol.setFragment(true);
		mDrawPanel = new JDrawPanel(mol, true);
		mDrawPanel.getDrawArea().setClipboardHandler(new ClipboardHandler());
		mDrawPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(600), HiDPIHelper.scale(EDITOR_HEIGHT)));
		editorPanel.add(mDrawPanel, "1,1,5,1");

		JButton bopen = new JButton("Open Reaction...");
		bopen.addActionListener(this);
		editorPanel.add(bopen, "2,3");
		JButton bsave = new JButton("Save Reaction...");
		bsave.addActionListener(this);
		editorPanel.add(bsave, "4,3");

		double[][] size2 = { {8, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, 8},
							 {8, TableLayout.PREFERRED, 8, TableLayout.FILL, 8} };
		mReactantPanel = new JPanel();
		mReactantPanel.setLayout(new TableLayout(size2));

		mComboBoxMode = new JComboBox(MODE_TEXT);
		JPanel cbp = new JPanel();
		cbp.add(new JLabel("Generate"));
		cbp.add(mComboBoxMode);
		cbp.add(new JLabel("multiple possible products"));
		mReactantPanel.add(cbp, "2,3");

		tabbedPane.add("Generic Reaction", editorPanel);
		tabbedPane.add("Reactants", mReactantPanel);

		return tabbedPane;
		}

	private void updateReactantPanel() {
		Reaction reaction = mDrawPanel.getDrawArea().getReaction();
		int reactantCount = (reaction == null) ? 0 : reaction.getReactants();

		if (mReactantPaneList.size() == reactantCount)
			return;

		if (mReactantPanel.getComponentCount() > 1)
			mReactantPanel.remove(1);

		if (mReactantPaneList.size() > reactantCount)
			for (int i=mReactantPaneList.size()-1; i>=reactantCount; i--)
				mReactantPaneList.remove(i);

		JPanel reactantPanel = new JPanel();
		double[] sizeH = {8, TableLayout.PREFERRED, 8, TableLayout.FILL};
		double[] sizeV = new double[6*reactantCount-1];
		for (int i=0; i<reactantCount; i++) {
			sizeV[6*i] = 4;
			sizeV[6*i+1] = HiDPIHelper.scale(74);
			sizeV[6*i+2] = 4;
			sizeV[6*i+3] = TableLayout.PREFERRED;
			sizeV[6*i+4] = 4;
			if (i != reactantCount-1)
				sizeV[6*i+5] = 8;
			}
		double[][] size = {sizeH, sizeV};
		reactantPanel.setLayout(new TableLayout(size));

		for (int i=0; i<reactantCount; i++) {
			JStructureView sview = new JStructureView(reaction.getReactant(i),
					DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
			sview.setClipboardHandler(new ClipboardHandler());
			reactantPanel.add(sview, "1,"+(6*i+1));
			JButton bload = new JButton("Open File...");
			bload.setActionCommand("open"+i);
			bload.addActionListener(this);
			reactantPanel.add(bload, "1,"+(6*i+3));

			mReactantPaneList.add(new CompoundCollectionPane<String[]>(new DefaultCompoundCollectionModel.IDCodeWithName(), false));
			mReactantPaneList.get(i).setFileSupport(CompoundCollectionPane.FILE_SUPPORT_NONE);
			mReactantPaneList.get(i).setEditable(true);
			reactantPanel.add(mReactantPaneList.get(i), "3,"+(6*i)+",2,"+(6*i+4));
			}

		if (reactantCount <= 3)
			mReactantPanel.add(reactantPanel, "1,1,3,1");
		else {
			JScrollPane scrollPane = new JScrollPane(reactantPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
																	JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, HiDPIHelper.scale(EDITOR_HEIGHT)));
			mReactantPanel.add(scrollPane, "1,1,3,1");
			}
		mReactantPanel.validate();
		}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (((JTabbedPane)e.getSource()).getSelectedIndex() == 1) {
			updateReactantPanel();
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("Open Reaction...")) {
			File rxnFile = FileHelper.getFile(mParent, "Please select a reaction file", FileHelper.cFileTypeRXN);
			if (rxnFile == null)
				return;

			try {
				Reaction reaction = new RXNFileParser().getReaction(rxnFile);

				// allow for query features
				for (int i=0; i<reaction.getMolecules(); i++)
					reaction.getMolecule(i).setFragment(true);

				mDrawPanel.getDrawArea().setReaction(reaction);
			}
			catch (Exception ex) {}
			return;
			}
		if (cmd.equals("Save Reaction...")) {
			Reaction rxn = mDrawPanel.getDrawArea().getReaction();
			if (isReactionValid(rxn))
				new FileHelper(mParent).saveRXNFile(rxn);
			return;
			}
		if (cmd.startsWith("open")) {
			int reactant = cmd.charAt(4) - '0';

			ArrayList<String[]> idcodeWithNameList = new FileHelper(mParent).readIDCodesWithNamesFromFile(null, true);

			if (idcodeWithNameList != null) {
				Reaction rxn = mDrawPanel.getDrawArea().getReaction();
				SSSearcher searcher = new SSSearcher();
				searcher.setFragment(rxn.getReactant(reactant));
				int matchErrors = 0;
				for (int i=idcodeWithNameList.size()-1; i>=0; i--) {
					searcher.setMolecule(new IDCodeParser().getCompactMolecule(idcodeWithNameList.get(i)[0]));
					if (!searcher.isFragmentInMolecule()) {
						idcodeWithNameList.remove(i);
						matchErrors++;
						}
					}

				if (matchErrors != 0) {
					String message = (idcodeWithNameList.size() == 0) ?
							"None of your file's compounds have generic reactant "+(char)('A'+reactant)+" as substructure.\n"
									+ "Therefore no compound could be added to the reactant list."
							: ""+matchErrors+" of your file's compounds don't contain generic reactant "+(char)('A'+reactant)+" as substructure.\n"
							+ "Therefore these compounds were not added to the reactant list.";
					JOptionPane.showMessageDialog(mParent, message);
					}

				if (idcodeWithNameList.size() != 0) {
					if (mReactantPaneList.get(reactant).getModel().getSize() != 0 && 0 == JOptionPane.showOptionDialog(mParent,
							"Do you want to add these compounds or to replace the current list?",
							"Add Or Replace Compounds", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, new String[] {"Add", "Replace"}, "Replace" ))
						mReactantPaneList.get(reactant).getModel().addCompoundList(idcodeWithNameList);
					else
						mReactantPaneList.get(reactant).getModel().setCompoundList(idcodeWithNameList);
					}
				}
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String reaction = ReactionEncoder.encode(mDrawPanel.getDrawArea().getReaction(), true,
												 ReactionEncoder.RETAIN_REACTANT_AND_PRODUCT_ORDER
											   | ReactionEncoder.INCLUDE_COORDS
											   | ReactionEncoder.INCLUDE_MAPPING
											 /*  | ReactionEncoder.INCLUDE_DRAWING_OBJECTS*/);
		if (reaction != null)
			configuration.setProperty(PROPERTY_REACTION, reaction);

		int index = 0;
		for (CompoundCollectionPane<String[]> ccp:mReactantPaneList) {
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			CompoundCollectionModel<String[]> model = ccp.getModel();
			for (int i = 0; i < model.getSize(); i++) {
				if (i != 0) {
					sb1.append('\t');
					sb2.append('\t');
					}
				sb1.append(model.getCompound(i)[0]);
				if (model.getCompound(i)[1] != null)
					sb2.append(model.getCompound(i)[1]);
			}
			configuration.setProperty(PROPERTY_REACTANT+(index), sb1.toString());
			configuration.setProperty(PROPERTY_REACTANT_NAME+(index), sb2.toString());
			index++;
		}

		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		Reaction reaction = ReactionEncoder.decode(configuration.getProperty(PROPERTY_REACTION), false);
		if (reaction != null)
			mDrawPanel.getDrawArea().setReaction(reaction);

		updateReactantPanel();
		int reactantCount = (reaction == null) ? 0 : reaction.getReactants();

		for (int i=0; i<reactantCount; i++) {
			CompoundCollectionPane<String[]> ccp = mReactantPaneList.get(i);
			String[] idcode = configuration.getProperty(PROPERTY_REACTANT+i, "").split("\\t");
			String[] name = configuration.getProperty(PROPERTY_REACTANT_NAME+i, "").split("\\t");
			for (int j=0; j<idcode.length; j++) {
				String[] idcodeWithName = new String[2];
				idcodeWithName[0] = idcode[j];
				idcodeWithName[1] = name[j];
				ccp.getModel().addCompound(idcodeWithName);
				}
			}

		mComboBoxMode.setSelectedIndex(AbstractTask.findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mDrawPanel.getDrawArea().clearAll();
		updateReactantPanel();
		mComboBoxMode.setSelectedIndex(0);
		}

	private boolean isReactionValid(Reaction rxn) {
		try {
			if (rxn.getReactants() < 1)
				throw new Exception("For combinatorial enumeration you need at least one reactant.");
			if (rxn.getReactants() > 4)
				throw new Exception("Combinatorial enumeration is limited to a maximum of 4 reactants.");
			if (rxn.getProducts() == 0)
				throw new Exception("No product defined.");
			rxn.validateMapping();
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParent, e);
			return false;
			}
		return true;
		}
	}
