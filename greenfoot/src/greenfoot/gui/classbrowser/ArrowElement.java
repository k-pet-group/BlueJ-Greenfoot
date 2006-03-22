package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;

import javax.swing.JComponent;

/**
 * Superclass for all elements of an arrow. Takes care of preferred sizes.
 *  ^ | |__ | |
 * 
 * @author mik
 * @version $Id: ArrowElement.java 3859 2006-03-22 17:54:17Z mik $
 */
public abstract class ArrowElement extends JComponent
{
    protected static final int ARROW_WIDTH = 10;
    protected static final int ARROW_HEIGHT = 7;
    
    public Dimension minimumSize;

    public ArrowElement()
    {
        minimumSize = new Dimension(ARROW_WIDTH + 4, ARROW_HEIGHT);
    }

    public abstract void paintComponent(Graphics g);

    public Dimension getMinimumSize()
    {
        return minimumSize;
    }

    public Dimension getPreferredSize()
    {
        return minimumSize;
    }

//    public Dimension getMaximumSize()
//    {
//        return minimumSize;
//    }
}