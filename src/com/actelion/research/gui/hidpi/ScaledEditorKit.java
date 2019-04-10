package com.actelion.research.gui.hidpi;

/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2012 Freeplane team and others
 *
 *  this file is created by Dimitry Polivaev in 2012.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


		import java.awt.Color;
		import java.awt.Font;
		import java.io.StringReader;

		import javax.swing.text.Document;
		import javax.swing.text.Element;
		import javax.swing.text.View;
		import javax.swing.text.ViewFactory;
		import javax.swing.text.html.HTMLDocument;
		import javax.swing.text.html.HTMLEditorKit;
		import javax.swing.text.html.ImageView;
		import javax.swing.text.html.StyleSheet;

@SuppressWarnings("serial")
public class ScaledEditorKit extends HTMLEditorKit {
	public static float getFontScaleFactor() {
		return HiDPIHelper.getUIScaleFactor();
	}

	/** Shared base style for all documents created by us use. */
	private static StyleSheet defaultStyles;

	public ScaledEditorKit() {
	};

	/**
	 * Overriden to return our own slimmed down style sheet.
	 */
	public StyleSheet getStyleSheet() {
		if (defaultStyles == null) {
			defaultStyles = new StyleSheet();
			StringReader r = new StringReader(ScaledHTML.styleChanges);
			try {
				defaultStyles.loadRules(r, null);
			}
			catch (Throwable e) {
				// don't want to die in static initialization...
				// just display things wrong.
			}
			r.close();
			defaultStyles.addStyleSheet(super.getStyleSheet());
		}
		return defaultStyles;
	}

	@Override
	public Document createDefaultDocument() {
		StyleSheet styles = getStyleSheet();
		StyleSheet ss = new ScaledStyleSheet();
		ss.addStyleSheet(styles);
		HTMLDocument doc = new HTMLDocument(ss);
		doc.setParser(getParser());
		doc.setAsynchronousLoadPriority(4);
		doc.setTokenThreshold(100);
		return doc;
	}

	/**
	 * Sets the async policy to flush everything in one chunk, and
	 * to not display unknown tags.
	 */
	Document createDefaultDocument(Font defaultFont, Color foreground) {
		StyleSheet styles = getStyleSheet();
		StyleSheet ss = new ScaledStyleSheet();
		ss.addStyleSheet(styles);
		HTMLDocument doc = new HTMLDocument(ss);
		doc.setPreservesUnknownTags(false);
		doc.getStyleSheet().addRule(displayPropertiesToCSS(defaultFont, foreground));
		doc.setParser(getParser());
		doc.setAsynchronousLoadPriority(Integer.MAX_VALUE);
		doc.setPreservesUnknownTags(false);
		return doc;
	}

	private String displayPropertiesToCSS(Font font, Color fg) {
		StringBuffer rule = new StringBuffer("body {");
		if (font != null) {
			rule.append(" font-family: ");
			rule.append(font.getFamily());
			rule.append(" ; ");
			rule.append(" font-size: ");
			final int fontSize = Math.round(font.getSize() / getFontScaleFactor());
			rule.append(fontSize);
			rule.append("pt ;");
			if (font.isBold()) {
				rule.append(" font-weight: bold ; ");
			}
			if (font.isItalic()) {
				rule.append(" font-style: italic ; ");
			}
		}
		if (fg != null) {
			rule.append(" color: ").append(ColorUtils.colorToString(fg)).append(" ; ");
		}
		rule.append(" }");
		return rule.toString();
	}

	/**
	 * Returns the ViewFactory that is used to make sure the Views don't
	 * load in the background.
	 */
	public ViewFactory getViewFactory() {
		if (basicHTMLViewFactory == null) {
			basicHTMLViewFactory = new BasicHTMLViewFactory();
		}
		return basicHTMLViewFactory;
	}

	/**
	 * BasicHTMLViewFactory extends HTMLFactory to force images to be loaded
	 * synchronously.
	 */
	static class BasicHTMLViewFactory extends HTMLEditorKit.HTMLFactory {
		public View create(Element elem) {
			View view = super.create(elem);

			if (view instanceof ImageView) {
				((ImageView)view).setLoadsSynchronously(true);
			}
			return view;
		}
	}


	static public ScaledEditorKit create() {
		if (kit == null) {
			kit = new ScaledEditorKit();
		}
		return kit;
	}

	/**
	 * The source of the html renderers
	 */
	private static ScaledEditorKit kit;
	/**
	 * Creates the Views that visually represent the model.
	 */
	static ViewFactory basicHTMLViewFactory;
}
