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

package com.actelion.research.datawarrior;

import com.actelion.research.gui.LookAndFeelHelper;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.util.ArrayList;

public class DataWarriorLinux extends DataWarrior {
	private static final String DEFAULT_LAF = "org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel";

	private static final String[] LOOK_AND_FEEL_ITEM_NAME = {
			"Classic",
			"Graphite",
			"Gray",
			"Nebula",
	};
	private static final String[] LOOK_AND_FEEL_CLASS_NAME = {
			"org.jvnet.substance.SubstanceLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel",
	};

	protected static DataWarriorLinux sDataExplorer;
	protected static ArrayList<String> sPendingDocumentList;

	private static class OldSubstanceFontSet implements org.jvnet.substance.fonts.FontSet {
		private float factor;
		private org.jvnet.substance.fonts.FontSet delegate;

		/**
		 * @param delegate The base Substance font set.
		 * @param factor Extra size in pixels. Can be positive or negative.
		 */
		public OldSubstanceFontSet(org.jvnet.substance.fonts.FontSet delegate, float factor) {
			super();
			this.delegate = delegate;
			this.factor = factor;
			}

		/**
		 * @param systemFont Original font.
		 * @return Wrapped font.
		 */
		private FontUIResource getWrappedFont(FontUIResource systemFont) {
			return new FontUIResource(systemFont.getFontName(), systemFont.getStyle(),
									  Math.round(this.factor * systemFont.getSize()));
			}

		public FontUIResource getControlFont() {
			return this.getWrappedFont(this.delegate.getControlFont());
			}

		public FontUIResource getMenuFont() {
			return this.getWrappedFont(this.delegate.getMenuFont());
			}

		public FontUIResource getMessageFont() {
			return this.getWrappedFont(this.delegate.getMessageFont());
			}

		public FontUIResource getSmallFont() {
			return this.getWrappedFont(this.delegate.getSmallFont());
			}

		public FontUIResource getTitleFont() {
			return this.getWrappedFont(this.delegate.getTitleFont());
			}

		public FontUIResource getWindowTitleFont() {
			return this.getWrappedFont(this.delegate.getWindowTitleFont());
			}
		}

	private static class NewSubstanceFontSet implements org.pushingpixels.substance.api.fonts.FontSet {
		private float factor;
		private org.pushingpixels.substance.api.fonts.FontSet delegate;

		/**
		 * @param delegate The base Substance font set.
		 * @param factor Extra size in pixels. Can be positive or negative.
		 */
		public NewSubstanceFontSet(org.pushingpixels.substance.api.fonts.FontSet delegate, float factor) {
			super();
			this.delegate = delegate;
			this.factor = factor;
		}

		/**
		 * @param systemFont Original font.
		 * @return Wrapped font.
		 */
		private FontUIResource getWrappedFont(FontUIResource systemFont) {
			return new FontUIResource(systemFont.getFontName(), systemFont.getStyle(),
									  Math.round(this.factor * systemFont.getSize()));
		}

		public FontUIResource getControlFont() {
			return this.getWrappedFont(this.delegate.getControlFont());
		}

		public FontUIResource getMenuFont() {
			return this.getWrappedFont(this.delegate.getMenuFont());
		}

		public FontUIResource getMessageFont() {
			return this.getWrappedFont(this.delegate.getMessageFont());
		}

		public FontUIResource getSmallFont() {
			return this.getWrappedFont(this.delegate.getSmallFont());
		}

		public FontUIResource getTitleFont() {
			return this.getWrappedFont(this.delegate.getTitleFont());
		}

		public FontUIResource getWindowTitleFont() {
			return this.getWrappedFont(this.delegate.getWindowTitleFont());
		}
	}

	/**
	 *  This is called from the Windows bootstrap process instead of main(), when
	 *  the user tries to open a new DataWarrior instance while one is already running.
	 * @param args
	 */
	public static void initSingleApplication(String[] args) {
		if (args != null && args.length != 0) {
			String[] filename = sDataExplorer.deduceFileNamesFromArgs(args);
			if (sDataExplorer == null) {
				if (sPendingDocumentList == null)
					sPendingDocumentList = new ArrayList<String>();

				for (String f:filename)
					sPendingDocumentList.add(f);
				}
			else {
				for (final String f:filename) {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								sDataExplorer.readFile(f);
								}
							});
						}
					catch(Exception e) {}
					}
				}
			}
		}

	public boolean isMacintosh() {
		return false;
		}

	@Override
	public String[] getAvailableLAFNames() {
		return LOOK_AND_FEEL_ITEM_NAME;
		};

	@Override
	public String[] getAvailableLAFClassNames() {
		return LOOK_AND_FEEL_CLASS_NAME;
		}

	@Override
	public String getDefaultLaFName() {
		return DEFAULT_LAF;
		}

	private void setFontSetOldSubstance(final float factor) {
		// reset the base font policy to null - this
		// restores the original font policy (default size).
		org.jvnet.substance.SubstanceLookAndFeel.setFontPolicy(null);

		// reduce the default font size a little
		final org.jvnet.substance.fonts.FontSet substanceCoreFontSet = org.jvnet.substance.SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null);
		org.jvnet.substance.fonts.FontPolicy newFontPolicy = new org.jvnet.substance.fonts.FontPolicy() {
			public org.jvnet.substance.fonts.FontSet getFontSet(String lafName, UIDefaults table) {
				return new OldSubstanceFontSet(substanceCoreFontSet, factor);
				}
			};
		org.jvnet.substance.SubstanceLookAndFeel.setFontPolicy(newFontPolicy);
		}

	private void setFontSetNewSubstance(final float factor) {
		// reset the base font policy to null - this
		// restores the original font policy (default size).
		org.pushingpixels.substance.api.SubstanceLookAndFeel.setFontPolicy(null);

		// reduce the default font size a little
		final org.pushingpixels.substance.api.fonts.FontSet substanceCoreFontSet = org.pushingpixels.substance.api.SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null);
		org.pushingpixels.substance.api.fonts.FontPolicy newFontPolicy = new org.pushingpixels.substance.api.fonts.FontPolicy() {
			public org.pushingpixels.substance.api.fonts.FontSet getFontSet(String lafName, UIDefaults table) {
				return new NewSubstanceFontSet(substanceCoreFontSet, factor);
				}
			};
		org.pushingpixels.substance.api.SubstanceLookAndFeel.setFontPolicy(newFontPolicy);
		}

	@Override
	public boolean setLookAndFeel(String lafName) {
		float fontFactor = 1f;
		String dpiFactor = System.getProperty("dpifactor");
		if (dpiFactor != null)
			try { fontFactor = Float.parseFloat(dpiFactor); } catch (NumberFormatException nfe) {}

		if (fontFactor != 1f) {
			if (lafName.contains("jvnet")) {
				// for OLD substance we have to set the alternative font set before setting the LaF
				setFontSetOldSubstance(fontFactor);
				}
			}

		// if we don't remove the old substance font set, setting to NEW substance LaF crashes
		if (LookAndFeelHelper.isOldSubstance())
			org.jvnet.substance.SubstanceLookAndFeel.setFontPolicy(null);

		if (super.setLookAndFeel(lafName)) {
			if (fontFactor != 1f) {
				if (LookAndFeelHelper.isNewSubstance()) {
					setFontSetNewSubstance(fontFactor);
					}
				}
			if (LookAndFeelHelper.isOldSubstance()) {
				if (System.getProperty("development") != null) {
					// nice yellow-brown based mixed look and feel
					org.jvnet.substance.theme.SubstanceTheme t2 = new org.jvnet.substance.theme.SubstanceSunGlareTheme();
					org.jvnet.substance.theme.SubstanceTheme t3 = new org.jvnet.substance.theme.SubstanceBrownTheme();
					org.jvnet.substance.SubstanceLookAndFeel.setCurrentTheme(new org.jvnet.substance.theme.SubstanceMixTheme(t3, t2));
					}
				else {
					org.jvnet.substance.theme.SubstanceTheme t1 = new org.jvnet.substance.theme.SubstanceLightAquaTheme().hueShift(0.04);
					org.jvnet.substance.SubstanceLookAndFeel.setCurrentTheme(t1);
					}
				UIManager.put(org.jvnet.substance.SubstanceLookAndFeel.TABBED_PANE_CONTENT_BORDER_KIND, org.jvnet.substance.utils.SubstanceConstants.TabContentPaneBorderKind.SINGLE_PLACEMENT);
				}
			return true;
			}
		return false;
		}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					sDataExplorer = new DataWarriorLinux();

					Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
						@Override
						public void uncaughtException(final Thread t, final Throwable e) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									e.printStackTrace();
									JOptionPane.showMessageDialog(sDataExplorer.getActiveFrame(), "Uncaught Exception:"+e.getMessage());
									}
								});
							}
						});

					if (args != null && args.length != 0) {
						String[] filename = sDataExplorer.deduceFileNamesFromArgs(args);
						for (String f:filename)
							sDataExplorer.readFile(f);
						}
					if (sPendingDocumentList != null)
						for (String doc:sPendingDocumentList)
							sDataExplorer.readFile(doc);
					}
				catch(Exception e) {
					e.printStackTrace();
					}
				}
			} );
		}
	}