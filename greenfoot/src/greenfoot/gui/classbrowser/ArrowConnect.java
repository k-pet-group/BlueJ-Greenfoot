package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * Graphics for the line part of an arrow. With a connector to the right.
 *  | |__ | |
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowConnect.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ArrowConnect extends JComponent
{
    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        g.drawLine(size.width / 2, 0, size.width / 2, size.height);
        g.drawLine(size.width / 2, size.height / 2, size.width, size.height / 2);
    }

}