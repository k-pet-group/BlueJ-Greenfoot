package bluej.debugmgr.objectbench;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


import javax.swing.JPanel;

class ContainerPanel extends JPanel implements FocusListener, KeyListener
{
	private ObjectBench ob;

	public ContainerPanel(ObjectBench objectBench){
		super();
		this.ob = objectBench;
		addFocusListener(this);
		addKeyListener(this);
	}
	
	public void paint(Graphics g){
		super.paint(g);
		if (ob.hasFocus){
			g.setColor(Color.BLUE);
			g.drawRect(0, 0, getWidth() - 2, getHeight() - 3);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	public void focusGained(FocusEvent e) {
		ob.hasFocus = true;
		repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	public void focusLost(FocusEvent e) {
		ob.hasFocus = false;
		repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		ob.handleKeyPressed(key);
	}

	

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
	}
}