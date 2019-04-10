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

package com.actelion.research.gui;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class JDrawToolbar extends JComponent
        implements MouseListener,MouseMotionListener {
    static final long serialVersionUID = 0x20090402;

    protected static final int cButtonsPerColumn = 17;

	protected static final int cButtonClear = 0;
	protected static final int cButtonCleanStructure = 1;
	public static final int cToolLassoPointer = 2;
	protected static final int cToolUnknownParity = 3;
	protected static final int cToolDelete = 4;
	protected static final int cToolStdBond = 5;
	protected static final int cToolUpBond = 6;
	protected static final int cTool3Ring = 7;
	protected static final int cTool5Ring = 8;
	protected static final int cTool7Ring = 9;
	protected static final int cToolPosCharge = 10;
	protected static final int cToolAtomC = 11;
	protected static final int cToolAtomN = 12;
	protected static final int cToolAtomO = 13;
	protected static final int cToolAtomF = 14;
	protected static final int cToolAtomBr = 15;
	protected static final int cToolAtomH = 16;
	protected static final int cButtonUndo = 17;
	protected static final int cToolZoom = 18;
	protected static final int cToolMapper = 19;
    protected static final int cToolESR = 20;
	protected static final int cToolText = 21;
	protected static final int cToolChain = 22;
	protected static final int cToolDownBond = 23;
	protected static final int cTool4Ring = 24;
	protected static final int cTool6Ring = 25;
	protected static final int cToolAromRing = 26;
	protected static final int cToolNegCharge = 27;
	protected static final int cToolAtomSi = 28;
	protected static final int cToolAtomP = 29;
	protected static final int cToolAtomS = 30;
	protected static final int cToolAtomCl = 31;
	protected static final int cToolAtomI = 32;
	protected static final int cToolAtomOther = 33;

    private static final int cESRMenuBorder = 4;
    private static final int cESRMenuX = HiDPIHelper.scale(10);
    private static final int cESRMenuY = HiDPIHelper.scale(54);
	private static final float cButtonBorder = HiDPIHelper.getUIScaleFactor()*3f;
	private static final float cButtonSize = HiDPIHelper.getUIScaleFactor()*21f;
    protected static final int cToolESRAbs = 101;
    protected static final int cToolESROr = 102;
    protected static final int cToolESRAnd  = 103;

	private JDrawArea	mArea;
	private Image		mImageUp,mImageDown,mESRImageUp,mESRImageDown;
	private int			mCurrentTool,mPressedButton,mMode,mESRSelected,mESRHilited;
    private boolean     mESRMenuVisible;

	public JDrawToolbar(JDrawArea theArea) {
		mArea = theArea;
		init();
		}


	public JDrawToolbar(JDrawArea theArea, int mode) {
		mArea = theArea;
		mMode = mode;
		if ((mMode & JDrawArea.MODE_REACTION) != 0)
			mMode |= JDrawArea.MODE_MULTIPLE_FRAGMENTS;
		init();
		}

    // CXR added this 09/05/2011
    public void setReactionMode(boolean rxn) {
        if (rxn)  {
            mMode = JDrawArea.MODE_MULTIPLE_FRAGMENTS | JDrawArea.MODE_REACTION;
        } else
            mMode &= ~JDrawArea.MODE_REACTION;
        }


	private void init() {
		MediaTracker t = new MediaTracker(this);

		mImageDown = createImage("drawButtonsDown.gif");
		mImageUp = createImage("drawButtonsUp.gif");
        mESRImageDown = createImage("ESRButtonsDown.gif");
        mESRImageUp = createImage("ESRButtonsUp.gif");

		scaleImages();

		int w = mImageUp.getWidth(this);
		int h = mImageUp.getHeight(this);
		setMinimumSize(new Dimension(w,h));
		setPreferredSize(new Dimension(w,h));
		setSize(w,h);

		mCurrentTool = cToolStdBond;
		mPressedButton = -1;

		addMouseListener(this);
        addMouseMotionListener(this);
		}

	private void scaleImages() {
		if (cButtonSize == 21)	// no scaling
			return;

		mImageDown = scaleImage(mImageDown);
		mImageUp = scaleImage(mImageUp);
		mESRImageDown = scaleImage(mESRImageDown);
		mESRImageUp = scaleImage(mESRImageUp);
		}

	public static BufferedImage createImage(String fileName) {
		// Once double resolution is available use HiDPIHelper.createImage() !!!

		URL url = JDrawToolbar.class.getResource("/images/" + fileName);
		if (url == null)
			throw new RuntimeException("Could not find: " + fileName);

		try {
			BufferedImage image = ImageIO.read(url);
			return image;
			}
		catch (IOException ioe) { return null; }
		}

	private Image scaleImage(Image image) {
		return image.getScaledInstance(HiDPIHelper.scale(image.getWidth(this)),
									   HiDPIHelper.scale(image.getHeight(this)),
									   Image.SCALE_SMOOTH);
		}

	public void setCurrentTool(int tool) {
		if (mCurrentTool != tool) {
			mCurrentTool = tool;
			mArea.toolChanged(tool);
			repaint();
			}
		}

	public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(mImageUp,0,0,this);
        drawPressedButton(g, mCurrentTool);
        if (mPressedButton != -1)
            drawPressedButton(g, mPressedButton);

        if (mESRSelected != 0) {
            setButtonClip(g, cToolESR);
            Point l = getButtonLocation(cToolESR);
            Image esrButtons = (mCurrentTool == cToolESR) ? mESRImageDown : mESRImageUp;
            g.drawImage(esrButtons, l.x-cESRMenuBorder, Math.round(l.y-cESRMenuBorder-cButtonSize*mESRSelected), this);
            g.setClip(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            }

        if (mESRMenuVisible) {
            g.drawImage(mESRImageUp, cESRMenuX-cESRMenuBorder, cESRMenuY-cESRMenuBorder, this);
            if (mESRHilited != -1) {
                g.setClip(cESRMenuX, Math.round(cESRMenuY+cButtonSize*mESRHilited), Math.round(cButtonSize-1), Math.round(cButtonSize-1));
                g.drawImage(mESRImageDown, cESRMenuX-cESRMenuBorder, cESRMenuY-cESRMenuBorder, this);
                g.setClip(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
               }
            }
        }

	public void update(Graphics g) {
		paint(g);
		}

	public void mousePressed(MouseEvent e) {
		int b = getButtonNo(e);
		if (b == -1) return;

        if (b == cToolESR) {
            mESRMenuVisible = true;
            validateESRHiliting(e);
            repaint();
            }

        mPressedButton = b;

        if (b != mCurrentTool)
            repaint();
		}

	public void mouseReleased(MouseEvent e) {
        int releasedButton = -1;
        if (mESRMenuVisible) {
            if (mESRHilited != -1) {
                mESRSelected = mESRHilited;
                releasedButton = cToolESR;
                }
            mESRMenuVisible = false;
            }

        if (mPressedButton == -1)
            return;

            // if user didn't change esr menu than require that the button beneath
            // mouse pointer was the same when pressing and releasing the mouse
        if (releasedButton == -1)
            releasedButton = getButtonNo(e);

        if (releasedButton != mPressedButton
		 || (mPressedButton == cToolMapper && (mMode & JDrawArea.MODE_REACTION) == 0)
		 || (mPressedButton == cToolText && (mMode & JDrawArea.MODE_DRAWING_OBJECTS) == 0)) {
			mPressedButton = -1;
            repaint();
			return;
			}

        mPressedButton = -1;
		if (releasedButton == cButtonClear
		 || releasedButton == cButtonCleanStructure
		 || releasedButton == cButtonUndo) {
//		 || (releasedButton == cButtonChiral && (mMode & JDrawArea.MODE_MULTIPLE_FRAGMENTS) == 0)) {
            repaint();
			mArea.buttonPressed(releasedButton);
			return;
			}
		mCurrentTool = releasedButton;
        repaint();

        if (mCurrentTool == cToolESR)
            mArea.toolChanged((mESRSelected == 0) ? cToolESRAbs
                            : (mESRSelected == 1) ? cToolESROr : cToolESRAnd);
        else
            mArea.toolChanged(releasedButton);
		}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}

    public void mouseMoved(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {
        int oldESRHilited = mESRHilited;
        validateESRHiliting(e);
        if (oldESRHilited != mESRHilited)
            repaint();
        }

 	private int getButtonNo(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
		if (x<0 || x>=2*cButtonSize+cButtonBorder || y<0 || y>cButtonsPerColumn*cButtonSize) return -1;
		x -= cButtonBorder;
		y -= cButtonBorder;
		if ((x % cButtonSize) > cButtonSize-cButtonBorder) return -1;
		if ((y % cButtonSize) > cButtonSize-cButtonBorder) return -1;
		return cButtonsPerColumn * (int)(x/cButtonSize) + (int)(y/cButtonSize);
		}

    private void validateESRHiliting(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (x>cESRMenuX && x<cESRMenuX+cButtonSize) {
            mESRHilited = Math.round((y-cESRMenuY) / cButtonSize);
            if (mESRHilited < 0 || mESRHilited > 2)
                mESRHilited = -1;
            }
        }

    private void setButtonClip(Graphics g, int button) {
        Point l = getButtonLocation(button);
        g.setClip(l.x, l.y,Math.round(cButtonSize-1),Math.round(cButtonSize-1));
        }

	private void drawPressedButton(Graphics g, int button) {
        setButtonClip(g, button);
		g.drawImage(mImageDown,0,0,this);
        g.setClip(0,0,Integer.MAX_VALUE,Integer.MAX_VALUE);
		}

    private Point getButtonLocation(int button) {
        return new Point(Math.round(cButtonSize * (button / cButtonsPerColumn) + cButtonBorder-2),
						 Math.round(cButtonSize * (button % cButtonsPerColumn) + cButtonBorder-2));
        }
	}
