package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * Graphics for the head of an arrow. With a connector to the right.
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowLine.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ArrowLine extends JComponent
{
    Dimension minimumSize;

    /**
     * @param i
     */
    public ArrowLine(int width)
    {
        minimumSize = new Dimension(width + 2, 0);
    }

    /**
     *  
     */
    public ArrowLine()
    {
        minimumSize = new Dimension();
    }

    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        g.drawLine(size.width / 2, 0, size.width / 2, size.height);
    }

    public Dimension getMinimumSize()
    {
        return minimumSize;
    }

    public Dimension getPreferredSize()
    {
        return minimumSize;
    }

}