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

package com.actelion.research.util;

import java.awt.Color;

public class ColorHelper {
	private static final float[] PERCEIVED_BRIGHTNESS = { 0.299f, 0.587f, 0.114f };
	// the desired perceived brightness difference between foreground color and background
	private static final float MIN_CONTRAST_TO_BACKGROUND = 0.30f;

	/**
	 * Creates an intermediate color between color c1 and color c2
	 * in the RGB color space.
	 * @param c1
	 * @param c2
	 * @param ratio 0.0 -> returns c1; 1.0 -> returns c2
	 * @return color in between c1 and c2
	 */
	public static Color intermediateColor(Color c1, Color c2, float ratio) {
		return new Color((int)(c1.getRed()+ratio*(c2.getRed()-c1.getRed())),
						 (int)(c1.getGreen()+ratio*(c2.getGreen()-c1.getGreen())),
						 (int)(c1.getBlue()+ratio*(c2.getBlue()-c1.getBlue())));
		}

	/**
	 * Creates a new <code>Color</code> that is a brighter version of this
	 * <code>Color</code>. This is a copy of Color.brighter(), but lets you choose
	 * the factor.
	 * @param c the color to be brightened
	 * @param factor value < 1.0; Color.brighter() uses 0.7
	 * @return     a new <code>Color</code> object that is
	 *                 a brighter version of this <code>Color</code>
	 *                 with the same {@code alpha} value.
	 */
	public static Color brighter(Color c, float factor) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		int alpha = c.getAlpha();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
		int i = (int)(1.0/(1.0-factor));
		if ( r == 0 && g == 0 && b == 0) {
			return new Color(i, i, i, alpha);
		}
		if ( r > 0 && r < i ) r = i;
		if ( g > 0 && g < i ) g = i;
		if ( b > 0 && b < i ) b = i;

		return new Color(Math.min((int)(r/factor), 255),
				Math.min((int)(g/factor), 255),
				Math.min((int)(b/factor), 255),
				alpha);
		}

	/**
	 * Creates a new <code>Color</code> that is a darker version of this
	 * <code>Color</code>. This is a copy of Color.darker(), but lets you choose
	 * the factor.
	 * @param c the color to be darkened
	 * @param factor value < 1.0; Color.darker() uses 0.7
	 * @return  a new <code>Color</code> object that is
	 *                    a darker version of this <code>Color</code>
	 *                    with the same {@code alpha} value.
	 */
	public static Color darker(Color c, float factor) {
		return new Color(Math.max((int)(c.getRed()  *factor), 0),
						 Math.max((int)(c.getGreen()*factor), 0),
						 Math.max((int)(c.getBlue() *factor), 0),
						 c.getAlpha());
		}

	/**
	 * This is a color's perceived brightness by the human eye
	 * @param c
	 * @return brightness from 0.0 to 1.0
	 */
	public static float perceivedBrightness(Color c) {
		return (c == null) ? 1.0f : (PERCEIVED_BRIGHTNESS[0]*c.getRed()
									+PERCEIVED_BRIGHTNESS[1]*c.getGreen()
									+PERCEIVED_BRIGHTNESS[2]*c.getBlue()) / 255f;
		}

	/**
	 * Creates a new color with the hue taken from color <code>c</code>, but adjusted
	 * in brightness to match the desired perceived brightness.
	 * @param c
	 * @param perceivedBrightness
	 * @return
	 */
	public static Color createColor(Color c, float perceivedBrightness) {
		float[] cc = c.getRGBComponents(null);

		float pb = perceivedBrightness(c);
		if (pb == 0f)
			return new Color(pb, pb, pb, cc[3]);

		float f = perceivedBrightness / perceivedBrightness(c);
		float surplusBrightness = 0f;
		float sum = 0f;
		for (int i=0; i<3; i++) {
			cc[i] *= f;
			if (cc[i] < 1f) {
				sum += PERCEIVED_BRIGHTNESS[i];
				}
			else {
				surplusBrightness += (cc[i] - 1f) * PERCEIVED_BRIGHTNESS[i];
				cc[i] = 1f;
				}
			}
		if (surplusBrightness != 0) {
			float remainingBrightness = 0f;
			for (int i=0; i<3; i++) {
				if (cc[i] < 1f) {
					cc[i] += surplusBrightness / sum;
					if (cc[i] > 1f) {
						remainingBrightness += (cc[i] - 1f) * PERCEIVED_BRIGHTNESS[i];
						cc[i] = 1f;
						}
					}
				}
			if (remainingBrightness != 0f) {
				for (int i=0; i<3; i++) {
					if (cc[i] < 1f) {
						cc[i] += remainingBrightness / PERCEIVED_BRIGHTNESS[i];
						if (cc[i] > 1f) {
							cc[i] = 1f;
						}
					}
				}
				}
			}

		return new Color(cc[0], cc[1], cc[2], cc[3]);
		}

	/**
	 * Based on the differences of hue and perceived brightness of foreground color
	 * <code>fg</code> and background color <code>bg</code>, this method checks and
	 * possibly adjusts the given foreground color <code>fg</code> such that its hue
	 * stays unchanged, but its brightness is adapted to make it better perceivable
	 * on the background.
	 * @param fg foreground color
	 * @param bg background color
//	 * @param minContrast the minimum desired contrast (0 to 0.5)
	 * @return unchanged or adjusted fg
	 */
	public static Color getContrastColor(Color fg, Color bg) {
		float bgb = ColorHelper.perceivedBrightness(bg);
		float fgb = ColorHelper.perceivedBrightness(fg);

		float contrast = Math.abs(bgb - fgb);
		if (contrast > MIN_CONTRAST_TO_BACKGROUND)
			return fg;

		float[] hsbBG = new float[3];
		Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), hsbBG);
		float[] hsbFG = new float[3];
		Color.RGBtoHSB(fg.getRed(), fg.getGreen(), fg.getBlue(), hsbFG);

		double hueDif = Math.abs(hsbFG[0] - hsbBG[0]);
		if (hueDif > 0.5)
			hueDif = 1.0 - hueDif;

		float saturationFactor = 1-Math.max(hsbFG[1], hsbBG[1]);
		float brightnessFactor = Math.abs(fgb + bgb - 1);
		float hueDifferenceFactor = (float)Math.cos(Math.PI*hueDif*3);

		float neededContrast = MIN_CONTRAST_TO_BACKGROUND * Math.max(saturationFactor, Math.max(brightnessFactor, hueDifferenceFactor));

		if (contrast > neededContrast)
			return fg;

		boolean darken = (fgb > bgb) ? (fgb + neededContrast > 1f) : (fgb - neededContrast > 0f);

		return createColor(fg, darken ? bgb - neededContrast : bgb + neededContrast);
		}
	}
