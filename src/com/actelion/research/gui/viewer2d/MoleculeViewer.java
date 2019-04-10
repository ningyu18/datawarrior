package com.actelion.research.gui.viewer2d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.chem.calculator.StructureCalculator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * 
 * @author freyssj
 */
@SuppressWarnings("rawtypes")
public class MoleculeViewer extends MoleculeCanvas implements MouseListener {

	public static final int MODE_NONE = 0;
	public static final int MODE_ATOMS = 32;
	public static final int MODE_CLASSES = 64;
	public static final int MODE_INTERACTIONS = 128;
	public static final int MODE_FORCES = 256;
	public static final int MODE_FLAGS = 512;
	public static final int MODE_COORDINATES = 2048;
	public static final int MODE_DESCRIPTIONS = 4096;

	private static final long serialVersionUID = 5614581272287603559L;
	
	/** defaultStyles if <> null, will enable the default feature */ 
	private int[] defaultStyles = null;
	private List<FFMolecule> undo = new ArrayList<FFMolecule>();
	private ArrayList<ActionProvider> mActionProviderList;

	/**
	 * Creates a new Viewer3DPanel with a default configuration 
	 */
	public MoleculeViewer() {
		createPopupMenu();
		
		addMouseListener(this);
		setMolecule(mol);
		setPreferredSize(new Dimension(300,200));
		/*
		//Set up D&D
		DragSource ds = DragSource.getDefaultDragSource();
		DragGestureListener dgl = new DragGestureListener(){
			public void dragGestureRecognized(DragGestureEvent dge) {
				
				if(!dge.getTriggerEvent().isControlDown()) return;
				FFMolecule m;
				
				if(getMolecule() instanceof FFMolecule) {
					m = StructureCalculator.extractLigand(getMolecule());
				} else {
					m = new FFMolecule(getMolecule());
				}
				
				for (int i = m.getAllAtoms()-1; i >=0; i--) {
					if(m.getAtomicNo(i)<=0) m.deleteAtom(i);
				}
				Transferable transfer = new MoleculeTransferable(m.toStereoMolecule());
				System.out.println("Start drag "+m.getAllAtoms()+" "+getMolecule());
				dge.startDrag(DragSource.DefaultCopyDrop, transfer);
			}};
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
				*/
	}

	/**
	 * Creates a new Viewer3D
	 */
	public MoleculeViewer(FFMolecule mol) {		
		this();
		setMolecule(mol);
		
	}

	public void addActionProvider(ActionProvider ap) {
		if (mActionProviderList == null)
			mActionProviderList = new ArrayList<ActionProvider>();
		mActionProviderList.add(ap);
	}

	public void removeActionProvider(ActionProvider ap) {
		if (mActionProviderList != null)
			mActionProviderList.remove(ap);
	}

	/**
	 * Saves the Image to the specified filename 
	 */
	public void saveImage(String name) throws IOException {
		BufferedImage bufferedImage = this.getBufferedImage();
		File file = new File(name);

		ImageIO.write(bufferedImage, "jpeg", file);

		/* JPEGImageEncoder is ancient and not supported by Java 6 anymore

		FileOutputStream fileOutputStream = new FileOutputStream(file);
		JPEGImageEncoder jpegImageEncoder =
			JPEGCodec.createJPEGEncoder(fileOutputStream);
		JPEGEncodeParam jpegEncodeParam =
			jpegImageEncoder.getDefaultJPEGEncodeParam(bufferedImage);
		jpegEncodeParam.setQuality(1.0f, false);
		jpegImageEncoder.setJPEGEncodeParam(jpegEncodeParam);
		jpegImageEncoder.encode(bufferedImage);
		fileOutputStream.close();*/
	}

	public void saveImage() {
		try {
			String name = new File(System.getProperty("user.home"), "molecule.jpg").getPath();
			saveImage(name);
			JOptionPane.showMessageDialog(
			this,
				"<html><font color=#000000>The image has been saved to<br> <i>"
					+ name
					+ "</i></font></html>",
				"Image saved",
				JOptionPane.PLAIN_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
				this,
				"The image could not be saved\n" + e,
				"Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void processSubPopupMenu(JPopupMenu menu) {}

	protected JPopupMenu createPopupMenu() {
		JMenuItem item;
		JPopupMenu menu = new JPopupMenu();

		JMenu menuRender = new JMenu("Render"); 
		menuRender.setMnemonic('R');
		menuRender.add(createMenuItem(new ActionRender(AtomShape.WIREFRAME, "WireFrame"), 'w', false));
		menuRender.add(createMenuItem(new ActionRender(AtomShape.BALLSTICKS, "Balls & Sticks"), 's', false));
		menuRender.add(createMenuItem(new ActionRender(AtomShape.STICKS, "Sticks"), 't', false));
		menuRender.add(createMenuItem(new ActionRender(AtomShape.BALLS, "Balls"), 'b', false));
		menuRender.add(new JSeparator());		
		
		item = createMenuItem(new ActionShowBackbone(), 'k', isMode(MoleculeCanvas.SHOW_BACKBONE));
		if(getMolecule()!=null && getMolecule().getAllAtoms()>50) menuRender.add(item);
		menuRender.add(createMenuItem(new ActionShowHideHydrogen(), 'h', false));
		menuRender.add(new JSeparator());
		menuRender.add(createMenuItem(new ActionStereo("Stereo Mode"), '-', false));
		menu.add(menuRender);

		JMenu menuColors = new JMenu("Colors"); 
		menuColors.setMnemonic('C');
		menuColors.add(createMenuItem(new ActionColors(0, "Atomic No"), 'a', false));
		menuColors.add(createMenuItem(new ActionColors(1, "Groups"), 'g', false));
		menuColors.add(createMenuItem(new ActionColors(2, "Aminos"), 'm', false));
		menu.add(menuColors);
				
		JMenu menuMeasurements = new JMenu("Measurements");
		menuMeasurements.setMnemonic('M');
		menuMeasurements.add(createMenuItem(new ActionPickDistance(), 'D', getTool() instanceof ToolMeasureDistance));
		menuMeasurements.add(createMenuItem(new ActionPickAngle(), 'A', getTool() instanceof ToolMeasureAngle));
		menuMeasurements.add(createMenuItem(new ActionPickDihedral(), 'T', getTool() instanceof ToolMeasureDihedral));
		menu.add(menuMeasurements);

		JMenu info = new JMenu("Informations");
		info.add(createMenuItem(new ActionInfoAxes(), (char)0, isMode(MODE_COORDINATES)));
		info.add(createMenuItem(new ActionInfoDescriptions(), (char)0, isMode(MODE_COORDINATES)));
		info.add(createMenuItem(new ActionInfoCoordinates(), (char)0, isMode(MODE_COORDINATES)));
		info.add(createMenuItem(new ActionInfoAtomLabel(), (char)0, isMode(MODE_COORDINATES)));
		info.add(new JSeparator());
		info.add(createMenuItem(new ActionInfoInteractions(), (char)0, isMode(MODE_INTERACTIONS)));	
		info.add(new JSeparator());
		menu.add(info);

		processSubPopupMenu(menu);
				
		menu.add(createMenuItem(new ActionClearDecorations(), 'C', false));
		
		menu.add(new JSeparator());
		menu.add(createMenuItem(new ActionSlab(), 'S', false));
		menu.add(createMenuItem(new ActionResetView(), 'c', false));

		menu.add(new JSeparator());
		//menu.add(createMenuItem(new ActionCopy(), 'c', false, KeyEvent.CTRL_DOWN_MASK ));
		//menu.add(createMenuItem(new ActionPaste(), 'p', false, KeyEvent.CTRL_DOWN_MASK ));
		menu.add(createMenuItem(new ActionSaveImage(), 'i', false));

		if (mActionProviderList != null) {
			menu.add(new JSeparator());
			for (ActionProvider ap:mActionProviderList)
				menu.add(createMenuItem(new ActionExternal(ap), (char)0, false));
			}

		return menu;
	}

	protected JMenuItem createMenuItem(final AbstractAction action, final char accelerator, boolean high) {
		return createMenuItem(action, accelerator, high, 0);
	}
	private final Set<Character> usedAccelerators = new HashSet<Character>(); 
	protected JMenuItem createMenuItem(final AbstractAction action, final char accelerator, boolean high, int modif) {
		JMenuItem menu = new JMenuItem(action);
		if(accelerator>0) {
			menu.setAccelerator(KeyStroke.getKeyStroke(new Character(accelerator), modif));
			if(!usedAccelerators.contains(accelerator)) {
				addKeyListener(new KeyAdapter(){
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyChar()==accelerator) action.actionPerformed(null);
					}	
				});
				usedAccelerators.add(accelerator);
			}
		}
		return menu;
	}
	
	////////////////// KEY LISTENER /////////////////////////
	public void keyPressed(KeyEvent event) {}
	public void keyReleased(KeyEvent event) {}	

	////////////////// MOUSE LISTENER /////////////////////////
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getModifiers() == MouseEvent.BUTTON3_MASK) {
			JPopupMenu menu = createPopupMenu();
			menu.show(this, e.getX(), e.getY());
		}
	}
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	@Override
	public void mouseReleased(MouseEvent e) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
		requestFocus();
	}

	/////////////////// ACTIONS ///////////////////////////////////
	public class ActionInfoAxes extends AbstractAction {
		public ActionInfoAxes() {super("Show Axes");}
		@Override
		public void actionPerformed(ActionEvent e) {

			Coordinates g2 = StructureCalculator.getLigandCenter((FFMolecule) mol);
			if(g2==null) g2 = GeometryCalculator.getCenterGravity(mol);
			final Coordinates g = g2;
			
			addShape(new Text(g.addC(new Coordinates(0,0,0)), g.toString(), 10, Color.green));
			addShape(new Line(g, g.addC(new Coordinates(10,0,0)), Color.green, Line.DASHED_STROKE));
			addShape(new Text(g.addC(new Coordinates(10,0,0)), "X", 14, Color.green));
			addShape(new Line(g, g.addC(new Coordinates(0,10,0)), Color.green, Line.DASHED_STROKE));
			addShape(new Text(g.addC(new Coordinates(0,10,0)), "Y", 14, Color.green));
			addShape(new Line(g, g.addC(new Coordinates(0,0,10)), Color.green, Line.DASHED_STROKE));
			addShape(new Text(g.addC(new Coordinates(0,0,10)), "Z", 14, Color.green));
			repaint();
		}		
	}

	public class ActionInfoAtomLabel extends AbstractAction {
		public ActionInfoAtomLabel() {super("Show Atom Label");}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(mol==null) return;

			for(int i = 0; i<mol.getAllAtoms(); i++) {
				if(isMode(HIDE_HYDROGENS) && mol.getAtomicNo(i)<=1) continue;
				Shape s = new Text(mol.getCoordinates(i) , " "+i+" "+ Molecule.cAtomLabel[mol.getAtomicNo(i)], 9, Color.CYAN);
				addShape(s);
			}
			repaint();
		}
	}

	public class ActionInfoCoordinates extends AbstractAction {
		public ActionInfoCoordinates() {super("Show Coordinates");}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(mol==null) return;
		
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				if(isMode(HIDE_HYDROGENS) && mol.getAtomicNo(i)<=1) continue;
				Coordinates c = new Coordinates(mol.getAtomX(i), mol.getAtomY(i), mol.getAtomZ(i));
				Shape s = new Text(c , i+" "+c, 9, Color.orange);
				addShape(s);
			}					
			repaint();	
		}		
	}
	public class ActionInfoDescriptions extends AbstractAction {
		public ActionInfoDescriptions() {super("Show Descriptions");}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(mol==null || !(mol instanceof FFMolecule)) return;
			FFMolecule mol = (FFMolecule)getMolecule();
		
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				if(isMode(HIDE_HYDROGENS) && mol.getAtomicNo(i)<=1) continue;
				Shape s = new Text(mol.getCoordinates(i), " "+(mol.getAtomDescription(i)!=null?mol.getAtomDescription(i):""), 9, Color.white);
				addShape(s);
			}					
			repaint();	
		}		
	}
	
	
	public class ActionInfoInteractions extends AbstractAction {
		public ActionInfoInteractions() {super("Show H-Bonds");}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(mol==null || !(mol instanceof FFMolecule)) return;
			FFMolecule mol = (FFMolecule) getMolecule();
			List interactions = StructureCalculator.getHBonds(mol);
			Iterator iter = interactions.iterator();
			while(iter.hasNext()) {
				int[] couple = (int[]) iter.next();
				//if(!mol.isAtomFlag(couple[0], FFMolecule.LIGAND) && !mol.isAtomFlag(couple[1], FFMolecule.LIGAND)) continue;
				Shape line = new Line(mol.getCoordinates(couple[0]), mol.getCoordinates(couple[1]), Color.green, Line.DOTTED_STROKE);
				addShape(line);
			}

			repaint();	
		}
	}

	
	private class ActionSlab extends AbstractAction {
		public ActionSlab() {
			super(getSlab()==0?"Set Slab to 6 A":"Remove Slab");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setSlab(getSlab()==0?6:0);
			init(false, false);
			repaint();
		}
	}
	
	private class ActionRender extends AbstractAction {
		private final int style;
		public ActionRender(int style, String text) {
			super(text);
			this.style = style;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(style==0 && defaultStyles!=null) setStyles(defaultStyles);
			else setStyle(style);
			

			init(false, false);
			repaint();
		}
	}
	
	private class ActionStereo extends AbstractAction {
		public ActionStereo(String text) {
			super(text);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setStereo(!isStereo());
			repaint();
		}
	}

	private class ActionColors extends AbstractAction {
		private final int style;
		public ActionColors(int style, String text) {
			super(text);
			this.style = style;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(style==0) {setMode(SHOW_GROUPS, false); setMode(SHOW_AMINO, false);} 
			else if(style==1) {setMode(SHOW_GROUPS, true); setMode(SHOW_AMINO, false);}
			else {setMode(SHOW_GROUPS, false) ; setMode(SHOW_AMINO, true);}
			repaint();
		}
	}

	private class ActionResetView extends AbstractAction {
		public ActionResetView() {super("Recenter View");}
		@Override
		public void actionPerformed(ActionEvent e) {
			resetView();
			repaint();
		}
	}
	
	private class ActionSaveImage extends AbstractAction {
		public ActionSaveImage() {super("Save Image");}
		@Override
		public void actionPerformed(ActionEvent e) {
			saveImage();
		}
	}
	private class ActionShowBackbone extends AbstractAction {
		public ActionShowBackbone() {super((!isMode(MoleculeCanvas.SHOW_BACKBONE)?"Show":"Hide") + " Backbone");}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(getMolecule()!=null && getMolecule().getAllAtoms()>50) setMode(MoleculeCanvas.SHOW_BACKBONE, !isMode(MoleculeCanvas.SHOW_BACKBONE)); repaint();
		}
	}	
	
	private class ActionPickDistance extends AbstractAction {
		public ActionPickDistance() {super("Measure Distance");}
		@Override
		public void actionPerformed(ActionEvent e) {
			setTool(new ToolMeasureDistance());repaint();
		}
	}
	public class ActionPickAngle extends AbstractAction {
		public ActionPickAngle() {super("Measure Angle");}
		@Override
		public void actionPerformed(ActionEvent e) {
			setTool(new ToolMeasureAngle());repaint();
		}
	}
	private class ActionPickDihedral extends AbstractAction {
		public ActionPickDihedral() {super("Measure Torsion");}
		@Override
		public void actionPerformed(ActionEvent e) {
			setTool(new ToolMeasureDihedral());repaint();
		}
	}	
	public class ActionClearDecorations extends AbstractAction {
		public ActionClearDecorations() {super("Clear Decorations");}
		@Override
		public void actionPerformed(ActionEvent e) {
			paintProcessors.clear();
			init();
			repaint();
		}
	}
	private class ActionShowHideHydrogen extends AbstractAction {
		public ActionShowHideHydrogen() {super((isMode(MoleculeCanvas.HIDE_HYDROGENS)?"Show":"Hide") + " Hydrogens");}
		@Override
		public void actionPerformed(ActionEvent e) {
			setMode(MoleculeCanvas.HIDE_HYDROGENS, !isMode(MoleculeCanvas.HIDE_HYDROGENS));repaint();
		}
	}
	private class ActionExternal extends AbstractAction {
		public ActionExternal(ActionProvider ap) {super(ap.getActionName());}
		@Override
		public void actionPerformed(ActionEvent e) {
			for (ActionProvider ap:mActionProviderList)
				if (ap.getActionName().equals(e.getActionCommand()))
					ap.performAction(MoleculeViewer.this);
		}
	}

	public static MoleculeViewer viewMolecule(FFMolecule mol) {
		JPanel panel = new JPanel(new GridLayout(1,1));
		final MoleculeViewer viewer = new MoleculeViewer();
		viewer.setMolecule(mol);
		viewer.repaint();
		panel.add(viewer);
	
		final JFrame frame = new JFrame();
		frame.setTitle("Test 3D molecule representation");
		frame.setSize(1000, 600);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(BorderLayout.CENTER, panel);
		frame.setVisible(true);
	
		
		frame.addComponentListener(new ComponentListener(){
			@Override
			public void componentHidden(ComponentEvent e) {}
			@Override
			public void componentResized(ComponentEvent e) {}
			@Override
			public void componentShown(ComponentEvent e) {};
			@Override
			public void componentMoved(ComponentEvent e) {
				Point p = frame.getLocationOnScreen();
				if(p.x%2==0) frame.setLocation(p.x+1, p.y);
			}
		});
		return viewer;	
	}
	

	/**
	 * @return
	 */
	public int[] getDefaultStyles() {
		return defaultStyles;
	}

	/**
	 * @param is
	 */
	public void setDefaultStyles(int[] is) {
		defaultStyles = is;
	}
	
	public void saveUndoStep() {
		FFMolecule copy = new FFMolecule(getMolecule());
		copy.compact();
		undo.add(copy);
		if(undo.size()>10) undo.remove(0);
	}

	public void undo() {
		if(undo.size()>0) updateMolecule(undo.remove(undo.size()-1));
	}

}
