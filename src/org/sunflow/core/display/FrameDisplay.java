package org.sunflow.core.display;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.image.Color;
import org.sunflow.system.ImagePanel;

import com.actelion.research.gui.FileHelper;

public class FrameDisplay implements Display {
    private String filename;
    private RenderFrame frame;

    public FrameDisplay() {
        this(null);
    }

    public FrameDisplay(String filename) {
        this.filename = filename;
        frame = null;
    }

    public void imageBegin(int w, int h, int bucketSize) {
        if (frame == null) {
            frame = new RenderFrame();
            frame.imagePanel.imageBegin(w, h, bucketSize);
            Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
            boolean needFit = false;
            if (w >= (screenRes.getWidth() - 200) || h >= (screenRes.getHeight() - 200)) {
                frame.imagePanel.setPreferredSize(new Dimension((int) screenRes.getWidth() - 200, (int) screenRes.getHeight() - 200));
                needFit = true;
            } else
                frame.imagePanel.setPreferredSize(new Dimension(w, h));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            if (needFit)
                frame.imagePanel.fit();
        } else
            frame.imagePanel.imageBegin(w, h, bucketSize);
    }

    public void imagePrepare(int x, int y, int w, int h, int id) {
        frame.imagePanel.imagePrepare(x, y, w, h, id);
    }

    public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
        frame.imagePanel.imageUpdate(x, y, w, h, data, alpha);
    }

    public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
        frame.imagePanel.imageFill(x, y, w, h, c, alpha);
    }

    public void imageEnd() {
        frame.imagePanel.imageEnd();
        if (filename != null)
            frame.imagePanel.save(filename);
    }

    @SuppressWarnings("serial")
    private static class RenderFrame extends JFrame implements ActionListener,ClipboardOwner {
        ImagePanel imagePanel;

        RenderFrame() {
            super("Molecule Rendered By Sunflow v" + SunflowAPI.VERSION);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            imagePanel = new ImagePanel();
            imagePanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                	handlePopupTrigger(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                	handlePopupTrigger(e);
                }
            });
            setContentPane(imagePanel);
            pack();
        }

		private boolean handlePopupTrigger(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JPopupMenu popup = new JPopupMenu();
				JMenuItem item1 = new JMenuItem("Copy Image");
				item1.addActionListener(this);
				popup.add(item1);
				JMenuItem item2 = new JMenuItem("Save Image...");
				item2.addActionListener(this);
				popup.add(item2);
				popup.show(this, e.getX(), e.getY());
				return true;
		    	}
			return false;
			}
	
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Copy Image")) {
				BufferedImage bi = imagePanel.getImage();
				if (bi != null) {
		            TransferableImage trans = new TransferableImage( bi );
		            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		            c.setContents( trans, this );
		            }
	    		return;
				}
			if (e.getActionCommand().equals("Save Image...")) {
				BufferedImage bi = imagePanel.getImage();
				if (bi != null) {
					String filename = new FileHelper(this).selectFileToSave("Save Image As", FileHelper.cFileTypeJPG, "Untitled");
					if (filename != null)
						try { ImageIO.write(bi, "JPEG", new File(filename)); } catch (IOException ioe) {};
		            }
	    		return;
				}
			}

		public void lostOwnership(Clipboard clip, Transferable trans) {  
//	        System.out.println( "Lost Clipboard Ownership" );  
			}

		private class TransferableImage implements Transferable {
		    Image mImage;

		    public TransferableImage(Image image) {
		        this.mImage = image;
		    	}

		    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		        if ( flavor.equals(DataFlavor.imageFlavor ) && mImage != null) {
		            return mImage;
		        	}
		        else {
		            throw new UnsupportedFlavorException(flavor);
			        }  
			    }  
		      
		    public DataFlavor[] getTransferDataFlavors() {
		        DataFlavor[] flavors = new DataFlavor[1];
		        flavors[0] = DataFlavor.imageFlavor;
		        return flavors;
		    	}

		    public boolean isDataFlavorSupported( DataFlavor flavor) {
		        DataFlavor[] flavors = getTransferDataFlavors();
		        for (int i=0; i<flavors.length; i++) {
		            if (flavor.equals(flavors[i])) {
		                return true;
		            	}
		        	}

		        return false;
			    }
			}
		}
	}