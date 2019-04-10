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

package com.actelion.research.gui.hidpi;

import com.actelion.research.gui.LookAndFeelHelper;

import javax.swing.*;
import java.awt.*;

/**
 * Created by sandert on 04/12/15.
 */
public class HiDPIIconButton extends JButton {
	private String mImageName,mStyle;
	private int mRotation;

	/**
	 * Creates a button that, if image2 is given, toggles between two states indicated
	 * by two different button images. The button optimizes its size to for QuaQua
	 * and Substance look&feels and uses adequate higher resolution images on HiDPI monitors.
	 * For Retina displays (Mac) it expects double resulution images named 'originalName@2x.png'.
	 *
	 * @param imageName initial appearance
	 * @param tooltip may be null
	 * @param command action command to be used for action listeners (may be null)
	 */
	public HiDPIIconButton(String imageName, String tooltip, String command) {
		this(imageName, tooltip, command, 0, "bevel");
		}

		/**
		 * Creates a button that, if image2 is given, toggles between two states indicated
		 * by two different button images. The button optimizes its size to for QuaQua
		 * and Substance look&feels and uses adequate higher resolution images on HiDPI monitors.
		 * For Retina displays (Mac) it expects double resulution images named 'originalName@2x.png'.
		 *
		 * @param imageName initial appearance
		 * @param tooltip may be null
		 * @param command action command to be used for action listeners (may be null)
		 * @param rotation 0, 90, 180, or 270 degrees in clockwise direction
		 * @param style one of "bevel","square",null (used for Quaqua LaF only)
		 */
	public HiDPIIconButton(String imageName, String tooltip, String command, int rotation, String style) {
		super();

		mImageName = imageName;
		mRotation = rotation;
		mStyle = style;
		updateIconSet();

		if (command != null)
			setActionCommand(command);

		setFocusable(false);

		if (tooltip != null)
			setToolTipText(tooltip);
		}

	private void updateIconSet() {
		if (mImageName != null) {
			setIcon(HiDPIHelper.createIcon(mImageName, mRotation));
			setDisabledIcon(HiDPIHelper.createDisabledIcon(mImageName, mRotation));

			Icon icon = getIcon();
			int w = icon.getIconWidth() / (int)HiDPIHelper.getRetinaScaleFactor() + 2;
			int h = icon.getIconHeight() / (int)HiDPIHelper.getRetinaScaleFactor() + 2;
			if (LookAndFeelHelper.isQuaQua()) {
				w += 2;
				h += 2;
				putClientProperty("Quaqua.Component.visualMargin", new Insets(1, 1, 1, 1));
				if (mStyle != null)
					putClientProperty("Quaqua.Button.style", mStyle);
				}
			setPreferredSize(new Dimension(w, h));
			}
		}

	@Override
	public void updateUI() {
		updateIconSet();
		super.updateUI();
		}
	}
