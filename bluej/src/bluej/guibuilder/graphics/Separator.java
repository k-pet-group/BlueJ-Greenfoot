package javablue.GUIGraphics;

import java.awt.Graphics;
import java.awt.SystemColor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Canvas;


/**
 * A custom component representing a separator.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class Separator extends Canvas
{
    private Color light = SystemColor.controlLtHighlight;
    private Color shadow = SystemColor.controlShadow;


    /**
     * Constructs a new Separator.
     **/
    public Separator()
    {
    }


    /**
     * This method is called to repaint this separator.
     *
     * @param g	    The graphics context.
     **/
    public void paint (Graphics g)
    {
	Dimension dim = getSize();
        g.setColor (shadow);
	g.drawLine (0, dim.height/2-1, dim.width, dim.height/2-1);
	g.setColor (light);
	g.drawLine (0, dim.height/2, dim.width, dim.height/2);
    }


    /**
     * Gets the mininimum size of this component. 
     *
     * @return	A dimension object indicating this seperator's minimum size.
     **/
    public Dimension getMinimumSize ()
    {
	return (new Dimension(1, 3));
    }


    /**
     * Gets the preferred size of this component.
     *
     * @return	A dimension object indicating this seperator's preferred size.
     **/
    public Dimension getPreferredSize ()
    {
	return (new Dimension(1, 6));
    }
}
