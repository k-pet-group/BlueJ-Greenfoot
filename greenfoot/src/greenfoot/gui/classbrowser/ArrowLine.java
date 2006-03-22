package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Graphics for the head of an arrow. With a connector to the right.
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowLine.java 3857 2006-03-22 00:08:17Z mik $
 */
public class ArrowLine extends ArrowElement
{
    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        g.drawLine(size.width / 2, 0, size.width / 2, size.height);
    }
}