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

package com.actelion.research.chem;

import java.awt.*;
import java.util.ArrayList;

public class Depictor extends AbstractDepictor {
	private static final int MAX_TEXTSIZE = 16;

	private int			mpTextSize,mMaxTextSize;
	private float		mLineWidth;
	private ArrayList<Font>	mFonts;
    private Font currentFont;

	public Depictor(StereoMolecule mol) {
		super(mol);
		}


	public Depictor(StereoMolecule mol, int displayMode) {
		super(mol, displayMode);
		}

	
	public void setMaximumTextSize(int maxTextSize) {
		mMaxTextSize = maxTextSize;
		}


	protected void init() {
		super.init();
		mFonts = new ArrayList<Font>();
		mMaxTextSize = MAX_TEXTSIZE;
		mLineWidth = 1.0f;
		}


	protected void drawBlackLine(DepictorLine theLine) {
		((Graphics)mG).drawLine((int)Math.round(theLine.x1),(int)Math.round(theLine.y1),
								(int)Math.round(theLine.x2),(int)Math.round(theLine.y2));
		}


    protected void drawDottedLine(DepictorLine theLine) {
        Stroke stroke = ((Graphics2D)mG).getStroke();
        ((Graphics2D)mG).setStroke(new BasicStroke(mLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
		                        mLineWidth, new float[] {3.0f*mLineWidth}, 0f));
        ((Graphics)mG).drawLine((int)theLine.x1,(int)theLine.y1,
                                (int)theLine.x2,(int)theLine.y2);
        ((Graphics2D)mG).setStroke(stroke);
        }


    public void drawString(String theString,double x, double y) {
	    double strWidth = getStringWidth(theString);
		((Graphics)mG).drawString(theString,(int)Math.round(x-strWidth/2),(int)Math.round(y+1+mpTextSize/3));
		}


	protected void drawPolygon(double[] x, double[] y, int count) {
		int[] px = new int[count];
		int[] py = new int[count];
		for (int i=0; i<count; i++) {
			px[i] = (int)Math.round(x[i]);
			py[i] = (int)Math.round(y[i]);
			}
		((Graphics)mG).drawPolygon(px, py, count);
		((Graphics)mG).fillPolygon(px, py, count);
		}


	protected void fillCircle(double x, double y, double r) {
	    ((Graphics)mG).fillOval((int)Math.round(x), (int)Math.round(y), (int)Math.round(r), (int)Math.round(r));
		}


	protected double getStringWidth(String theString) {
		return ((Graphics)mG).getFontMetrics().stringWidth(theString);
		}


	public void setTextSize(int theSize)
    {
        mpTextSize = Math.min(theSize, mMaxTextSize);
        if (mG != null) {
            if (currentFont == null || currentFont.getSize() != mpTextSize) {
                for (int i = 0; i < mFonts.size(); i++) {
                    if ((mFonts.get(i)).getSize() == mpTextSize) {
                        ((Graphics) mG).setFont(mFonts.get(i));
                        return;
                    }
                }
				Font newFont = ((Graphics)mG).getFont().deriveFont(0, mpTextSize);
//                Font newFont = new Font("Helvetica", 0, mpTextSize);
                mFonts.add(newFont);
                currentFont = newFont;
                ((Graphics) mG).setFont(newFont);
            }
        }
    }

	public int getTextSize() {
	    return mpTextSize;
	    }


	protected double getLineWidth() {
		return mLineWidth;
		}


	protected void setLineWidth(double lineWidth) {
		if (lineWidth <= 1.5f)
			lineWidth = 1.0f;
		if (mLineWidth != lineWidth) {
			mLineWidth = (float)lineWidth;
			((Graphics2D)mG).setStroke(new BasicStroke((float)lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}
		}


	public void setColor(Color theColor) {
	    ((Graphics)mG).setColor(theColor);
		}
	}
