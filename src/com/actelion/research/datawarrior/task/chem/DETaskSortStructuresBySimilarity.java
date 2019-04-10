package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.*;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_Flexophore;
import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_LIST;

/**
 * Created by thomas on 9/22/16.
 */
public class DETaskSortStructuresBySimilarity extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Sort Structures By Similarity";

	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_IDCODE = "idcode";
	private static final String PROPERTY_IDCOORDINATES = "idcoords";
	private static final String PROPERTY_DESCRIPTOR_TYPE = "descriptorType";
	private static final String PROPERTY_DESCRIPTOR = "descriptor";

	private CompoundTableModel mTableModel;
	private CompoundRecord mRecord;
	private int mDescriptorColumn;
	private JComboBox mComboBoxStructureColumn,mComboBoxDescriptorType;
	private JEditableStructureView mStructureView;

	public DETaskSortStructuresBySimilarity(DEFrame parent, int descriptorColumn, CompoundRecord record) {
		super(parent, record == null);
		mTableModel = parent.getTableModel();
		mDescriptorColumn = descriptorColumn;
		mRecord = record;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mRecord != null && mDescriptorColumn != -1) {
			Properties configuration = new Properties();
			int structureColumn = mTableModel.getParentColumn(mDescriptorColumn);
			int coordinateColumn = mTableModel.getChildColumn(structureColumn, CompoundTableConstants.cColumnType2DCoordinates);
			configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, mTableModel.getColumnTitleNoAlias(structureColumn));
			configuration.setProperty(PROPERTY_IDCODE, mTableModel.getValue(mRecord, structureColumn));
			if (coordinateColumn != -1)
				configuration.setProperty(PROPERTY_IDCOORDINATES, mTableModel.getValue(mRecord, coordinateColumn));
			configuration.setProperty(PROPERTY_DESCRIPTOR_TYPE, mTableModel.getDescriptorHandler(mDescriptorColumn).getInfo().shortName);

			Object descriptor = mRecord.getData(mDescriptorColumn);
			if (descriptor != null)
				configuration.setProperty(PROPERTY_DESCRIPTOR, mTableModel.getDescriptorHandler(mDescriptorColumn).encode(descriptor));
			return configuration;
			}

		return null;
		}

	@Override
	public JComponent createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
				{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED,
						16, TableLayout.PREFERRED, 8, HiDPIHelper.scale(320), 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		int[] structureColumn = getStructureColumnList();

		mComboBoxStructureColumn = new JComboBox();
		if (structureColumn != null)
			for (int i=0; i<structureColumn.length; i++)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(structureColumn[i]));
		content.add(new JLabel("Structure column:"), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");
		mComboBoxStructureColumn.setEditable(!isInteractive());
		if (isInteractive())
			mComboBoxStructureColumn.addActionListener(this);

		mComboBoxDescriptorType = new JComboBox();
		populateComboBoxDescriptor(structureColumn == null? -1 : structureColumn[0]);
		content.add(new JLabel("Descriptor:"), "1,3");
		content.add(mComboBoxDescriptorType, "3,3");

		content.add(new JLabel("Reference structure for similarity:"), "1,5,3,5");
		mStructureView = new JEditableStructureView();
		content.add(mStructureView, "1,7,3,7");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String structureColumn = (String)mComboBoxStructureColumn.getSelectedItem();
		if (structureColumn != null)
			configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, structureColumn);

		String descriptorType = (String)mComboBoxDescriptorType.getSelectedItem();
		if (descriptorType != null)
			configuration.setProperty(PROPERTY_DESCRIPTOR_TYPE, descriptorType);

		StereoMolecule mol = mStructureView.getMolecule();
		if (mol != null && mol.getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mol);
			configuration.setProperty(PROPERTY_IDCODE, canonizer.getIDCode());
			configuration.setProperty(PROPERTY_IDCOORDINATES, canonizer.getEncodedCoordinates());
			if (descriptorType != null) {
				DescriptorHandler dh = DescriptorHandlerStandardFactory.getFactory().create(descriptorType);
				Object descriptor = dh.createDescriptor(mol);
				configuration.setProperty(PROPERTY_DESCRIPTOR, dh.encode(descriptor));
				}
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String structureColumnName = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_TYPE, "");
		if (structureColumnName.length() != 0) {
			int column = mTableModel.findColumn(structureColumnName);
			if (column != -1) {
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));

				if (descriptorName.length() != 0) {
					int descriptorColumn = mTableModel.getChildColumn(column, descriptorName);
					if (descriptorColumn != -1 || !isInteractive())
						mComboBoxDescriptorType.setSelectedItem(descriptorName);
					}
				}
			else if (!isInteractive()) {
				mComboBoxStructureColumn.setSelectedItem(structureColumnName);
				if (descriptorName != null)
					mComboBoxDescriptorType.setSelectedItem(descriptorName);
				}
			}
		else if (!isInteractive()) {
			mComboBoxStructureColumn.setSelectedItem("Structure");
			mComboBoxDescriptorType.setSelectedItem(descriptorName);
			}
		else if (mComboBoxStructureColumn.getItemCount() != 0) {
			mComboBoxStructureColumn.setSelectedIndex(0);
			}

		String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
		if (idcode != null) {
			new IDCodeParser().parse(mStructureView.getMolecule(), idcode, configuration.getProperty(PROPERTY_IDCOORDINATES));
			mStructureView.structureChanged();
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxStructureColumn.setSelectedItem("Structure");
		if (mComboBoxDescriptorType.getItemCount() != 0)
			mComboBoxDescriptorType.setSelectedIndex(0);

		mStructureView.getMolecule().deleteMolecule();
		mStructureView.structureChanged();
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxStructureColumn)
			populateComboBoxDescriptor(mTableModel.findColumn((String)mComboBoxStructureColumn.getSelectedItem()));
		}

	private void populateComboBoxDescriptor(int structureColumn) {
		mComboBoxDescriptorType.removeAllItems();
		if (!isInteractive()) {
			for (DescriptorInfo di:DESCRIPTOR_LIST)
				mComboBoxDescriptorType.addItem(di.shortName);
			}
		else if (structureColumn != -1) {
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getParentColumn(column) == structureColumn && mTableModel.isDescriptorColumn(column))
					mComboBoxDescriptorType.addItem(mTableModel.getDescriptorHandler(column).getInfo().shortName);
			}
		}

	private int[] getStructureColumnList() {
		int[] structureColumn = null;

		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumn != null) {
			int count = 0;
			for (int column:idcodeColumn)
				if (mTableModel.hasDescriptorColumn(column))
					count++;

			if (count != 0) {
				structureColumn = new int[count];
				count = 0;
				for (int column:idcodeColumn)
					if (mTableModel.hasDescriptorColumn(column))
						structureColumn[count++] = column;
				}
			}

		return structureColumn;
		}


	@Override
	public boolean isConfigurable() {
		int[] columnList = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		if (columnList != null)
			for (int column:columnList)
				if (mTableModel.hasDescriptorColumn(column))
					return true;

		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String structureColumnName = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		if (structureColumnName.length() == 0) {
			showErrorMessage("Structure column not defined.");
			return false;
			}
		String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
		if (idcode.length() == 0) {
			showErrorMessage("Reference structure not defined.");
			return false;
			}
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_TYPE, "");
		if (descriptorName.length() == 0) {
			showErrorMessage("Descriptor type not defined.");
			return false;
			}
		if (configuration.getProperty(PROPERTY_DESCRIPTOR, "").length() == 0) {
			showErrorMessage("Missing descriptor.");
			return false;
			}

		if (isLive) {
			int structureColumn = mTableModel.findColumn(structureColumnName);
			if (structureColumn == -1) {
				showErrorMessage("Structure column '"+structureColumnName+"' not found.");
				return false;
				}
			int descriptorColumn = mTableModel.getChildColumn(structureColumn, descriptorName);
			if (descriptorColumn == -1) {
				showErrorMessage("Column '"+structureColumnName+"' has no '"+descriptorName+"' descriptor.");
				return false;
				}
			if (!mTableModel.isDescriptorAvailable(descriptorColumn)) {
				showErrorMessage("The calculation of the '"+descriptorName+"' descriptor of column '"+structureColumnName+"' hasn't finished yet.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String idcode = configuration.getProperty(PROPERTY_IDCODE);
		String idcoords = configuration.getProperty(PROPERTY_IDCOORDINATES);
		int structureColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN));
		int descriptorColumn = mTableModel.getChildColumn(structureColumn, configuration.getProperty(PROPERTY_DESCRIPTOR_TYPE));
		Object descriptor = mTableModel.getDescriptorHandler(descriptorColumn).decode(configuration.getProperty(PROPERTY_DESCRIPTOR));
		mTableModel.sortBySimilarity(createSimilarityList(idcode, descriptor, descriptorColumn), descriptorColumn);
		}

	private float[] createSimilarityList(String idcode, Object descriptor, int descriptorColumn) {
		return (DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn))
				|| mTableModel.getTotalRowCount() > 400000) ?

				// if we have the slow 3DPPMM2 then use a progress dialog
				createSimilarityListSMP(idcode, descriptor, descriptorColumn)

				// else calculate similarity list in current thread
				: mTableModel.createSimilarityList(null, descriptor, descriptorColumn);
	}

	private float[] createSimilarityListSMP(String idcode, Object descriptor, int descriptorColumn) {
		float[] similarity = mTableModel.getSimilarityListFromCache(idcode, descriptorColumn);
		if (similarity != null)
			return similarity;

		ProgressController pc = !isInteractive() ? getProgressController() : new JProgressDialog(getParentFrame()) {
			private static final long serialVersionUID = 0x20160922;

			public void stopProgress() {
				super.stopProgress();
				close();
				}
			};

		mTableModel.createSimilarityListSMP(null, descriptor, idcode, descriptorColumn, pc, !isInteractive());

		if (isInteractive())
			((JProgressDialog)pc).setVisible(true);

		similarity = mTableModel.getSimilarityListSMP();

		return similarity;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
