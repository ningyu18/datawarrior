package com.actelion.research.gui.viewer2d;

import javax.swing.JPanel;

public interface ActionProvider<T extends JPanel> {
	public String getActionName();
	public void performAction(T viewer);
}
