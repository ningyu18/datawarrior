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

package com.actelion.research.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.ImageObserver;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import com.actelion.research.util.CursorHelper;

public class JImagePanel extends JPanel implements ActionListener,ImageObserver,KeyListener,MouseListener,MouseMotionListener,MouseWheelListener {
	private static final long serialVersionUID = 0x20120502;

	public static final String cLargeImageExtension = ".jpg";
	public static final String cSmallImageExtension = "_s.jpg";		// used in case of thumbnails

	private static final int IMAGE_ERROR = -1;
	private static final int IMAGE_NO_IMAGE = 0;
	private static final int IMAGE_PENDING = 1;
	private static final int IMAGE_LOADING = 2;
	private static final int IMAGE_AVAILABLE = 3;

	private static final int MIN_SELECTION_PIXELS = 8;
	private static final float MAX_ZOOM_FACTOR = 8f;

	private String				mImagePath,mFileName;
	private Image				mImage,mOffImage,mLowResImage;
	private byte[]				mImageData;
	private Graphics			mOffG;
	private boolean				mOffImageValid,mImageIsThumbNail,mUseThumbNail,mAltIsDown,mMouseIsDown,mMouseIsInside;
	private int					mImageStatus,mImageCenterOffsetX,mImageCenterOffsetY,mCurrentCursor,mImageUpdateCount;
	private float				mZoomFactor;
	private Point				mMouseLocation;
	private Rectangle			mSelectionRect,mImageRect;
	private ImageDataSource		mImageDataSource;
	private PopupItemProvider	mPopupItemProvider;

	public JImagePanel() {
		this("", false);
		}

	public JImagePanel(String imagePath) {
		this(imagePath, false);
		}

	public JImagePanel(String imagePath, boolean useThumbNail) {
		setFocusable(true);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		mImagePath = (imagePath == null) ? "" : imagePath;
		mUseThumbNail = useThumbNail;
		mImageRect = new Rectangle();
		resetZoomState();
		mCurrentCursor = CursorHelper.cPointerCursor;
		}

	/**
	 * If this panel uses thumbnails and if this panel is not based on path names,
	 * then use this method to define the source for high resolution image data.
	 * @param source
	 */
	public void setHighResolutionImageSource(ImageDataSource source) {
		mImageDataSource = source;
		}

	/**
	 * Use this, if you want the popup menu to contain more than just "Copy Full Image"
	 * and "Copy Visible".
	 * @param pip
	 */
	public void setPopupItemProvider(PopupItemProvider pip) {
		mPopupItemProvider = pip;
		}

	public void paintComponent(Graphics g) {
		Dimension theSize = getSize();
		if (theSize.width == 0 || theSize.height == 0)
			return;

		if (mOffImage == null
		 || mOffImage.getWidth(null) != theSize.width
		 || mOffImage.getHeight(null) != theSize.height) {
			mOffImage = createImage(theSize.width, theSize.height);
			mOffG = mOffImage.getGraphics();
			mOffImageValid = false;
			}

		if (!mOffImageValid) {
			mOffG.setColor(getBackground());
			mOffG.fillRect(0, 0, mOffImage.getWidth(null), mOffImage.getHeight(null));

			if (mImageStatus == IMAGE_PENDING) {
				if (mImageData != null || mFileName != null) {
					createAndPrepareImage();	// changes status to IMAGE_LOADING or IMAGE_AVAILABLE
					}
				else {
					mImage = null;
					mImageStatus = IMAGE_NO_IMAGE;
					}
				}

			if (mImage != null) {
				if (mImageStatus == IMAGE_LOADING) {
					if (mLowResImage != null)	// if we have a thumbnail
						drawImage(mLowResImage);

					String message = (mUseThumbNail && !mImageIsThumbNail) ?
							"higher resolution loading..." : "image loading...";
					mOffG.setColor(Color.blue);
					mOffG.drawString(message, 4, theSize.height - 4);
					}
				else if (mImageStatus == IMAGE_ERROR) {
					mOffG.setColor(Color.red);
					mOffG.drawString("image loading error.", 4, theSize.height - 4);
					}
				else if (mImageStatus == IMAGE_AVAILABLE) {
					drawImage(mImage);
					}
				}
			mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, null);
		if (mSelectionRect != null) {
			g.setColor(mSelectionRect.width >= MIN_SELECTION_PIXELS
					&& mSelectionRect.height >= MIN_SELECTION_PIXELS ? Color.GREEN : Color.RED);
			g.drawRect(mSelectionRect.x-1, mSelectionRect.y-1, mSelectionRect.width+2, mSelectionRect.height+2);
			g.drawRect(mSelectionRect.x-2, mSelectionRect.y-2, mSelectionRect.width+4, mSelectionRect.height+4);
			}
		}

	private void drawImage(Image image) {
		if (image.getWidth(this) > 0 && image.getHeight(this) > 0) {
			if (mZoomFactor == 1f) {
				mImageRect.x = (int)mImageCenterOffsetX + (mOffImage.getWidth(null) - image.getWidth(this)) / 2;
				mImageRect.y = (int)mImageCenterOffsetY + (mOffImage.getHeight(null) - image.getHeight(this)) / 2;
				mImageRect.width = image.getWidth(this);
				mImageRect.height = image.getHeight(this);
				mOffG.drawImage(image, mImageRect.x, mImageRect.y, this);
				}
			else {
				mImageRect.width = (int)(image.getWidth(this)*mZoomFactor);
				mImageRect.height = (int)(image.getHeight(this)*mZoomFactor);
				mImageRect.x = (int)mImageCenterOffsetX + (mOffImage.getWidth(null) - mImageRect.width) / 2;
				mImageRect.y = (int)mImageCenterOffsetY + (mOffImage.getHeight(null) - mImageRect.height) / 2;
				mOffG.drawImage(image, mImageRect.x, mImageRect.y, mImageRect.width, mImageRect.height, this);
				}
			}
		}

	public void update(Graphics g) {
		paint(g);
		}

	public boolean imageUpdate(Image img, final int flags, int x, int y, int width, int height) {
		if (((ImageObserver.ALLBITS
			| ImageObserver.ERROR
			| ImageObserver.ABORT) & flags) != 0) {

			final int request = ++mImageUpdateCount;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (mImage != null && request == mImageUpdateCount) {	// check whether this is the latest request
						mImageStatus = ((ImageObserver.ALLBITS & flags) == 0) ? IMAGE_ERROR : IMAGE_AVAILABLE;
						if (mImageStatus == IMAGE_AVAILABLE)
							setInitialFullImageZoomState();
						mOffImageValid = false;
						repaint();
						}
					}
				} );
			return false;
			}

		return true;
		}

	/**
	 * If this uses thumbnails and if you use this method to provide the thumbnail,
	 * then you must also define a source for high resolution images with
	 * setHighResolutionImageSource().
	 * @param imageData
	 */
	public void setImageData(byte[] imageData) {
		mFileName = null;	// other potential source for image information

		if (imageData == mImageData)
			return;		// no change

		mImageData = imageData;

		mImageStatus = IMAGE_PENDING;
		mImageIsThumbNail = mUseThumbNail;
		mSelectionRect = null;
		mLowResImage = null;
		resetZoomState();

		mOffImageValid = false;
		repaint();
		}

	public void setFileName(String fileName) {
		mImageData = null;	// other potential source for image information

		if (fileName != null && fileName.length() == 0)
			fileName = null;
		if (mFileName == null && fileName == null)
			return;
		if (mFileName != null && fileName != null && mFileName.equals(fileName))
			return;

		mFileName = fileName;

		mImageStatus = IMAGE_PENDING;
		mImageIsThumbNail = mUseThumbNail;
		mSelectionRect = null;
		mLowResImage = null;
		resetZoomState();

		mOffImageValid = false;
		repaint();
		}

	private void createAndPrepareImage() {
		if (mImageData != null) {
			mImage = Toolkit.getDefaultToolkit().createImage(mImageData);
			}
		else if (mFileName != null) {
			String filePath = (!mUseThumbNail) ? mImagePath + mFileName
											   : mImagePath + mFileName
						+ (mImageIsThumbNail ? cSmallImageExtension : cLargeImageExtension);

			mImage = Toolkit.getDefaultToolkit().createImage(filePath);
			}

		if (Toolkit.getDefaultToolkit().prepareImage(mImage, -1, -1, this))
			mImageStatus = IMAGE_AVAILABLE;
		else
			mImageStatus = IMAGE_LOADING;

		if (mImageStatus == IMAGE_AVAILABLE)
			setInitialFullImageZoomState();
		}

	private void loadFullImage() {
		mImageIsThumbNail = false;

		mLowResImage = mImage;
		mImageStatus = IMAGE_LOADING;
		repaint();

		if (mImageData != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					mImageData = mImageDataSource.getImageData();
					createAndPrepareImage();
					}
				} );
			}

/*		mImageIsThumbNail = false;

		if (mImageData != null)
			mImageData = mImageDataSource.getImageData();

		mLowResImage = mImage;
		createAndPrepareImage();	*/
		}

	public void setImagePath(String imagePath) {
		mImagePath = (imagePath == null) ? "" : imagePath;
		mFileName = null;
		mImageData = null;
		mImageStatus = IMAGE_NO_IMAGE;

		mOffImageValid = false;
		repaint();
		}

	public String getImagePath() {
		return mImagePath;
		}

	public void setUseThumbNail(boolean value) {
		mUseThumbNail = value;
		}

	public boolean usesThumbNail() {
		return mUseThumbNail;
		}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			mAltIsDown = true;
			updateCursor();
			}
		}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			mAltIsDown = false;
			updateCursor();
			}
		if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
			if (e.getKeyCode() == KeyEvent.VK_C)
				copyVisible();
			}
		}

	public void keyTyped(KeyEvent e) {}

	public void mouseClicked(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (handlePopupTrigger(e))
			return;

		mMouseIsDown = true;

		mSelectionRect = null;
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0 && mImageStatus == IMAGE_AVAILABLE) {
			mMouseLocation = e.getPoint();
			if (mAltIsDown)	// start a rectangular selection
				mSelectionRect = new Rectangle(e.getX(), e.getY(), 0, 0);
			}

		updateCursor();
		repaint();
		}

	public void mouseReleased(MouseEvent e) {
		if (handlePopupTrigger(e))
			return;

		mMouseIsDown = false;
		updateCursor();

		if (mSelectionRect != null) {
			Rectangle sharedRect = mSelectionRect.intersection(mImageRect);
			if (sharedRect.width >= MIN_SELECTION_PIXELS
			 && sharedRect.height >= MIN_SELECTION_PIXELS) {
				if (mImageIsThumbNail)
					loadFullImage();
				float factor = Math.min(MAX_ZOOM_FACTOR / mZoomFactor,
										Math.min((float)getWidth() / (float)sharedRect.width,
												 (float)getHeight() / (float)sharedRect.height));
		   		if (factor > 1.000001) {
					mImageCenterOffsetX = (int)(factor * (mImageRect.x-sharedRect.x+(mImageRect.width-sharedRect.width)/2));
					mImageCenterOffsetY = (int)(factor * (mImageRect.y-sharedRect.y+(mImageRect.height-sharedRect.height)/2));
					mZoomFactor *= factor;
					mOffImageValid = false;
		   			}
				}

			mSelectionRect = null;
			repaint();
			}
		}

	public void mouseEntered(MouseEvent e) {
		requestFocusInWindow();
		mMouseIsInside = true;
		updateCursor();
		}

	public void mouseExited(MouseEvent e) {
		mMouseIsInside = false;
		}

	public synchronized void mouseMoved(MouseEvent e) {}

	public synchronized void mouseDragged(MouseEvent e) {
		if (mSelectionRect != null) {
			mSelectionRect.x = Math.min(e.getX(), mMouseLocation.x);
			mSelectionRect.y = Math.min(e.getY(), mMouseLocation.y);
			mSelectionRect.width = Math.abs(mMouseLocation.x - e.getX());
			mSelectionRect.height = Math.abs(mMouseLocation.y - e.getY());
			repaint();
			}
		else if (mImageStatus == IMAGE_AVAILABLE) {
			int dx = e.getX() - mMouseLocation.x;
			int dy = e.getY() - mMouseLocation.y;
			mImageCenterOffsetX += dx;
			mImageCenterOffsetY += dy;
			mMouseLocation.x += dx;
			mMouseLocation.y += dy;
	   		validateOffsets();
			mOffImageValid = false;
			repaint();
			}
		}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (mImageStatus == IMAGE_AVAILABLE) {
			zoom(e.getX(), e.getY(), e.getWheelRotation());
			if (mImageIsThumbNail)
				loadFullImage();
			}
		}

	private void zoom(int sx, int sy, int steps) {
   		float f = (float)Math.exp(-steps / 20.0);
   		float fullScaleFactor = Math.min((float)getWidth() / (float)mImage.getWidth(this), (float)getHeight() / (float)mImage.getHeight(this));
   		float minZoomFactor = Math.min(fullScaleFactor, Math.min(mZoomFactor, 1f));
   		float newZoomFactor = Math.max(minZoomFactor, Math.min(MAX_ZOOM_FACTOR, f*mZoomFactor));
   		if (mZoomFactor != newZoomFactor) {
   	   		int oldZoomedImageWidth = (int)(mImage.getWidth(this)*mZoomFactor);
   	   		int oldZoomedImageHeight = (int)(mImage.getHeight(this)*mZoomFactor);
   	   		float relX = Math.max(0f, Math.min(1f, (sx-mImageCenterOffsetX+(oldZoomedImageWidth-getWidth())/2f)/oldZoomedImageWidth));
   	   		float relY = Math.max(0f, Math.min(1f, (sy-mImageCenterOffsetY+(oldZoomedImageHeight-getHeight())/2f)/oldZoomedImageHeight));
	   		mZoomFactor = newZoomFactor;
	   		mImageCenterOffsetX += (0.5f-relX)*((int)(mImage.getWidth(this)*mZoomFactor)-oldZoomedImageWidth);
	   		mImageCenterOffsetY += (0.5f-relY)*((int)(mImage.getHeight(this)*mZoomFactor)-oldZoomedImageHeight);
	   		validateOffsets();
			mOffImageValid = false;
			repaint();
   			}
		}

	private void validateOffsets() {
   		int zoomedImageWidth = (int)(mImage.getWidth(this)*mZoomFactor);
   		int zoomedImageHeight = (int)(mImage.getHeight(this)*mZoomFactor);
   		int horizontalOversize = (zoomedImageWidth-getWidth())/2;
   		int verticalOversize = (zoomedImageHeight-getHeight())/2;

   		int leftWhiteSpace = mImageCenterOffsetX - horizontalOversize;
   		int rightWhiteSpace = -mImageCenterOffsetX - horizontalOversize;
   		int topWhiteSpace = mImageCenterOffsetY - verticalOversize;
   		int bottomWhiteSpace = -mImageCenterOffsetY - verticalOversize;

   		if (horizontalOversize < 0) {
	   		if (leftWhiteSpace < 0)
	   			mImageCenterOffsetX -= leftWhiteSpace;
	   		else if (rightWhiteSpace < 0)
	   			mImageCenterOffsetX += rightWhiteSpace;
   			}
   		else {
	   		if (leftWhiteSpace > 0)
	   			mImageCenterOffsetX -= leftWhiteSpace;
	   		else if (rightWhiteSpace > 0)
	   			mImageCenterOffsetX += rightWhiteSpace;
   			}

   		if (verticalOversize < 0) {
	   		if (topWhiteSpace < 0)
	   			mImageCenterOffsetY -= topWhiteSpace;
	   		else if (bottomWhiteSpace < 0)
	   			mImageCenterOffsetY += bottomWhiteSpace;
   			}
   		else {
	   		if (topWhiteSpace > 0)
	   			mImageCenterOffsetY -= topWhiteSpace;
	   		else if (bottomWhiteSpace > 0)
		   		mImageCenterOffsetY += bottomWhiteSpace;
   			}
		}

	private void resetZoomState() {
		mZoomFactor = 1f;
		mImageCenterOffsetX = 0;
		mImageCenterOffsetY = 0;
		}

	private void setInitialFullImageZoomState() {
		if (mLowResImage != null) {	// translate zoom from thumbnail to high-res image
			mZoomFactor *= (float)mLowResImage.getWidth(null) / (float)mImage.getWidth(null);
			mLowResImage = null;	// use high-res image from now on
			}
		else {
			Dimension theSize = getSize();
			if (theSize.width != 0 && theSize.height != 0)
				mZoomFactor = Math.min(1f, Math.min((float)theSize.width/(float)mImage.getWidth(null),
													(float)theSize.height/(float)mImage.getHeight(null)));
			mImageCenterOffsetX = 0;
			mImageCenterOffsetY = 0;
			}
		}

	private boolean handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (mImageStatus == IMAGE_AVAILABLE) {
				JPopupMenu popup = new JPopupMenu();
				JMenuItem item1 = new JMenuItem("Copy Full Image");
				item1.addActionListener(this);
				popup.add(item1);
				JMenuItem item2 = new JMenuItem("Copy Visible");
				item2.addActionListener(this);
				popup.add(item2);
				if (mPopupItemProvider != null) {
					JMenuItem[] extendedItemList = mPopupItemProvider.getPopupItems();
					if (extendedItemList != null) {
						popup.addSeparator();
						for (JMenuItem item:extendedItemList)
							popup.add(item);
						}
					}
				popup.show(this, e.getX(), e.getY());
				}
			return true;
			}
		return false;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Copy Full Image")) {
			copyImage();
			return;
			}
		if (e.getActionCommand().equals("Copy Visible")) {
			copyVisible();
			return;
			}
		}

	private void copyImage() {
		if (mImageStatus == IMAGE_AVAILABLE)
			ImageClipboardHandler.copyImage(mImage);
		}

	private void copyVisible() {
		if (mImageStatus == IMAGE_AVAILABLE) {
			Rectangle visRect = mImageRect.intersection(new Rectangle(0, 0, getWidth(), getHeight()));
			if (!visRect.isEmpty()) {
				int visWidth = (int)((float)visRect.width / mZoomFactor);
				int visHeight = (int)((float)visRect.height / mZoomFactor);
				Image image = createImage(visWidth, visHeight);
				int x = (int)((float)(mImageRect.x-visRect.x)/mZoomFactor);
				int y = (int)((float)(mImageRect.y-visRect.y)/mZoomFactor);
				image.getGraphics().drawImage(mImage, x, y, this);
				ImageClipboardHandler.copyImage(image);
				}
			}
		}

	private void updateCursor() {
		if (mMouseIsInside) {
			int cursor = CursorHelper.cPointerCursor;

			if (mImageStatus == IMAGE_AVAILABLE) {
				if (mAltIsDown)
					cursor = CursorHelper.cSelectRectCursor;
				else
					cursor = mMouseIsDown ? CursorHelper.cFistCursor : CursorHelper.cHandCursor;
				}

			if (mCurrentCursor != cursor) {
				mCurrentCursor = cursor;
				setCursor(CursorHelper.getCursor(cursor));
				}
			}
		}
	}
