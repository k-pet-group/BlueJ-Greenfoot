package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * Graphics for the line part of an arrow. With a connector to the right.
 *  | |__ | |
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowConnect.java 3857 2006-03-22 00:08:17Z mik $
 */
public class ArrowConnect extends ArrowElement
{
    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        g.drawLine(size.width / 2, 0, size.width / 2, size.height);
        g.drawLine(size.width / 2, size.height / 2, size.width, size.height / 2);
    }

}