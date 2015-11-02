package org.openmolecules.render;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.chem.StereoMolecule;

public class MoleculeRaytraceDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 20150604L;

	private static final String[] LIGHT_OPTIONS = {
			"Bright sun",
			"Black background" };
	private static final String[] SIZE_OPTIONS = {
			"640 x 480", 
			"1024 x 768",
			"1600 x 1200",
			"1920 x 1080",
			"2560 x 1600",
			"4000 x 3000" };

	private JComboBox		mComboboxSize,mComboboxLight;
	private JCheckBox		mCheckboxFillImage;
	private StereoMolecule	mMol;
	private float			mCameraDistance,mCameraX,mCameraZ,mFieldOfView;

	/**
	 * Creates a raytrace dialog that will render an image with the default parameters
	 * for camera distance (12.5) and field of view (40f). 
	 * @param parent
	 * @param mol
	 */
	public MoleculeRaytraceDialog(Frame parent, StereoMolecule mol) {
		this(parent, mol, MoleculeRenderer.DEFAULT_CAMERA_DISTANCE,
						  MoleculeRenderer.DEFAULT_CAMERA_X,
						  MoleculeRenderer.DEFAULT_CAMERA_Z,
						  MoleculeRenderer.DEFAULT_FIELD_OF_VIEW);
		}

	/**
	 * 
	 * @param parent
	 * @param mol
	 * @param cameraDistance
	 * @param fieldOfView
	 */
	public MoleculeRaytraceDialog(Frame parent, StereoMolecule mol, float cameraDistance, float cameraX, float cameraZ, float fieldOfView) {
		super(parent, "Create Photo-Realistic Image", true);

		mMol = mol;
		mCameraDistance = cameraDistance;
		mCameraX = cameraX;
		mCameraZ = cameraZ;
		mFieldOfView = fieldOfView;

		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 24, TableLayout.PREFERRED, 8} };

		getContentPane().setLayout(new TableLayout(size));

		getContentPane().add(new JLabel("Image size:"), "1,1");
		mComboboxSize = new JComboBox(SIZE_OPTIONS);
		mComboboxSize.setSelectedIndex(0);
		getContentPane().add(mComboboxSize, "3,1");

		getContentPane().add(new JLabel("Environment:"), "1,3");
		mComboboxLight = new JComboBox(LIGHT_OPTIONS);
		mComboboxLight.setSelectedIndex(0);
		getContentPane().add(mComboboxLight, "3,3");

		mCheckboxFillImage = new JCheckBox("Move and zoom to fill image", true);
		getContentPane().add(mCheckboxFillImage, "1,5,3,5");

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2, 8, 8));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		buttonPanel.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		buttonPanel.add(bok);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(buttonPanel, BorderLayout.EAST);
		getContentPane().add(bottomPanel, "1,7,3,7");

		getRootPane().setDefaultButton(bok);

		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			renderMolecule((String)mComboboxSize.getSelectedItem(), (String)mComboboxLight.getSelectedItem(), mCheckboxFillImage.isSelected());
			}

		setVisible(false);
		dispose();
		}

	private void renderMolecule(final String sizeOption, final String lightOption, final boolean fillImage) {
		final boolean moveToCenter = fillImage;
		final boolean zoomToOptimum = fillImage;
		new Thread(new Runnable() {
			@Override
			public void run() {
				int i = sizeOption.indexOf(" x ");
				int width = Integer.parseInt(sizeOption.substring(0, i));
				int height = Integer.parseInt(sizeOption.substring(i+3));
				boolean onBlack = lightOption.startsWith("Black");

				StereoMolecule mol = new StereoMolecule(mMol);

				if (moveToCenter) {
					float[] cog = new float[3];	// center of gravity
					for (int atom=0; atom<mol.getAllAtoms(); atom++) {
						cog[0] += mol.getAtomX(atom);
						cog[1] += mol.getAtomY(atom);
						cog[2] += mol.getAtomZ(atom);
						}
					for (int j=0; j<3; j++)
						cog[j] /= mol.getAllAtoms();
	
					for (int atom=0; atom<mol.getAllAtoms(); atom++) {
						mol.setAtomX(atom, mol.getAtomX(atom) - cog[0]);
						mol.setAtomY(atom, mol.getAtomY(atom) - cog[1]);
						mol.setAtomZ(atom, mol.getAtomZ(atom) - cog[2]);
						}
					}

				MoleculeRenderer mr = fillImage ? new MoleculeRenderer() : new MoleculeRenderer(mCameraDistance, mCameraX, mCameraZ, mFieldOfView);
				mr.drawMolecule(mol, width, height, false, zoomToOptimum, onBlack);
//				mr.render("/home/thomas/sunflowTest.png");
				mr.render();
				}
			} ).start();
		}
	}
