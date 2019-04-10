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

		import java.io.*;
		import java.awt.*;
		import java.net.URL;

		import javax.swing.*;
		import javax.swing.plaf.basic.BasicHTML;
		import javax.swing.text.*;
		import javax.swing.text.html.*;

public class ScaledHTML extends BasicHTML{

	/**
	 * Create an html renderer for the given component and
	 * string of html.
	 */
	public static View createHTMLView(JComponent c, String html) {
		ScaledEditorKit kit = ScaledEditorKit.create();
		Document doc = kit.createDefaultDocument(c.getFont(),
				c.getForeground());
		Object base = c.getClientProperty(documentBaseKey);
		if (base instanceof URL) {
			((HTMLDocument)doc).setBase((URL)base);
		}
		Reader r = new StringReader(html);
		try {
			kit.read(r, doc, 0);
		} catch (Throwable e) {
		}
		ViewFactory f = kit.getViewFactory();
		View hview = f.create(doc.getDefaultRootElement());
		View v = new Renderer(c, f, hview);
		return v;
	}

	public static void updateRenderer(JComponent c, String text) {
		View value = null;
		try{
			View oldValue = (View)c.getClientProperty(propertyKey);
			if (isHTMLString(text)) {
				value = ScaledHTML.createHTMLView(c, text);
			}
			if (value != oldValue && oldValue != null) {
				for (int i = 0; i < oldValue.getViewCount(); i++) {
					oldValue.getView(i).setParent(null);
				}
			}
		}
		finally{
			c.putClientProperty(BasicHTML.propertyKey, value);
		}
	}


	/**
	 * Overrides to the default stylesheet.  Should consider
	 * just creating a completely fresh stylesheet.
	 */
	static final String styleChanges =
			"p { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0 }" +
					"body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0 }";

	/**
	 * Root text view that acts as an HTML renderer.
	 */
	static class Renderer extends View {

		Renderer(JComponent c, ViewFactory f, View v) {
			super(null);
			host = c;
			factory = f;
			view = v;
			view.setParent(this);
			// initially layout to the preferred size
			setSize(view.getPreferredSpan(X_AXIS), view.getPreferredSpan(Y_AXIS));
		}

		public AttributeSet getAttributes() {
			return null;
		}

		public float getPreferredSpan(int axis) {
			if (axis == X_AXIS) {
				// width currently laid out to
				return width;
			}
			return view.getPreferredSpan(axis);
		}

		public float getMinimumSpan(int axis) {
			return view.getMinimumSpan(axis);
		}

		public float getMaximumSpan(int axis) {
			return Integer.MAX_VALUE;
		}

		public void preferenceChanged(View child, boolean width, boolean height) {
			host.revalidate();
			host.repaint();
		}

		public float getAlignment(int axis) {
			return view.getAlignment(axis);
		}

		public void paint(Graphics g, Shape allocation) {
			Rectangle alloc = allocation.getBounds();
			view.setSize(alloc.width, alloc.height);
			view.paint(g, allocation);
		}

		public void setParent(View parent) {
			throw new Error("Can't set parent on root view");
		}

		public int getViewCount() {
			return 1;
		}
		public View getView(int n) {
			return view;
		}
		public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
			return view.modelToView(pos, a, b);
		}

		public Shape modelToView(int p0, Position.Bias b0, int p1,
								 Position.Bias b1, Shape a) throws BadLocationException {
			return view.modelToView(p0, b0, p1, b1, a);
		}

		public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
			return view.viewToModel(x, y, a, bias);
		}

		public Document getDocument() {
			return view.getDocument();
		}

		public int getStartOffset() {
			return view.getStartOffset();
		}

		public int getEndOffset() {
			return view.getEndOffset();
		}

		public Element getElement() {
			return view.getElement();
		}

		public void setSize(float width, float height) {
			this.width = (int) width;
			view.setSize(width, height);
		}

		public Container getContainer() {
			return host;
		}

		public ViewFactory getViewFactory() {
			return factory;
		}

		private int width;
		private View view;
		private ViewFactory factory;
		private JComponent host;

	}
}
