package javablue.gui;

import java.awt.*;
import java.awt.event.*;

public class Bubble extends Window {
	private BubblePanel panel;

	public Bubble(Component comp, String text) {
		super(Util.getFrame(comp)); 
		add(new BubblePanel(text), "Center");
	}
	public void setVisible(boolean b) {
		pack();
		super.setVisible(b);
	}
}
class BubblePanel extends Panel {
	String text;

	public BubblePanel(String text) {
		this.text = text;
		setForeground(Color.black);
	}
	public void paint(Graphics g) {
		Dimension   size = getSize();
		FontMetrics fm   = g.getFontMetrics();
		g.drawRect(0,0,size.width-1,size.height-1);
		g.drawString(text,2,fm.getAscent()+2);
	}
	/**
	 * @deprecated for JDK1.1
	 */
	public Dimension preferredSize() {
		Graphics    g  = getGraphics();
		FontMetrics fm = g.getFontMetrics();
		return new Dimension(fm.stringWidth(text)+4, 
		                     fm.getHeight()+4);
	}
	public Dimension getPreferredSize() {
		return preferredSize();
	}
}
