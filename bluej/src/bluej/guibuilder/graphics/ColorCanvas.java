package javablue.GUIGraphics;

import java.awt.Graphics;
import java.awt.SystemColor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Canvas;
import java.awt.Point;


/**
 * A custom component showing the 13 standard colors in java.
 *
 * Created: Oct 2, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ColorCanvas extends Canvas
{
    Color[] color = new Color[13];


    /**
     * Constructs a new ColorCanvas.
     **/
    public ColorCanvas()
    {
	color[0] = new Color((Color.white).getRGB());
	color[1] = new Color((Color.lightGray).getRGB());
	color[2] = new Color((Color.gray).getRGB());
	color[3] = new Color((Color.darkGray).getRGB());
	color[4] = new Color((Color.black).getRGB());
	color[5] = new Color((Color.cyan).getRGB());
	color[6] = new Color((Color.blue).getRGB());
	color[7] = new Color((Color.green).getRGB());
	color[8] = new Color((Color.magenta).getRGB());
	color[9] = new Color((Color.orange).getRGB());
	color[10] = new Color((Color.pink).getRGB());
	color[11] = new Color((Color.red).getRGB());
	color[12] = new Color((Color.yellow).getRGB());
    }


    /**
     * This method is called to repaint this color canvas.
     *
     * @param g	    The graphics context.
     **/
    public void paint (Graphics g)
    {
	Dimension dim = getSize();
       
        int thirdwidth = dim.width/3;
        int height = dim.height/5;
        int height2 = dim.height/4;
        
        g.setColor (color[0]);
        g.fillRect (0, 0,thirdwidth,height);
        g.setColor (color[4]);
        g.fillRect (0, height*4,thirdwidth,height);
        g.setColor (color[1]);
        g.fillRect (0, height,thirdwidth, height);
        g.setColor (color[2]);
        g.fillRect (0, height*2,thirdwidth, height);
        g.setColor (color[3]);
        g.fillRect (0, height*3,thirdwidth, height);
        g.setColor (color[5]);
        g.fillRect (thirdwidth, 0,thirdwidth, height2);
        g.setColor (color[6]);
        g.fillRect (thirdwidth, height2,thirdwidth, height2);
        g.setColor (color[7]);
        g.fillRect (thirdwidth, height2*2,thirdwidth, height2);
        g.setColor (color[8]);
        g.fillRect (thirdwidth, height2*3,thirdwidth, dim.height-height2*3);
        g.setColor (color[9]);
        g.fillRect (thirdwidth*2, 0,thirdwidth, height2);
        g.setColor (color[10]);
        g.fillRect (thirdwidth*2, height2,thirdwidth, height2);
        g.setColor (color[11]);
        g.fillRect (thirdwidth*2, height2*2,thirdwidth, height2);
        g.setColor (color[12]);
        g.fillRect (thirdwidth*2, height2*3,thirdwidth, dim.height-height2*3);
    }


    /**
     * Gets the mininimum size of this component. 
     *
     * @return	A dimension object indicating this seperator's minimum size.
     **/
    public Dimension getMinimumSize ()
    {
	return (new Dimension(72, 60));
    }


    /**
     * Gets the preferred size of this component.
     *
     * @return	A dimension object indicating this seperator's preferred size.
     **/
    public Dimension getPreferredSize ()
    {
	return (new Dimension(108,90));
    }


    /**
     * Gets the color set at the specified point. It is the true java standard
     * color, not the color shown at the screen, since that may be changed due
     * to hardware limitations.
     *
     * @param point The location to check the color.
     *
     * @return	The color at that point.
     **/
    public Color getColorAtPoint(Point point)
    {
	Dimension dim = getSize();

	int thirdwidth = dim.width/3;
	int height = dim.height/5;
	int height2 = dim.height/4;

	if (point.x > thirdwidth)
	{
	    if(point.y > height2*3)
		return color[4+(point.x/thirdwidth)*4];
	    else
		return color[1+(point.x/thirdwidth)*4+(point.y/height2)];
	}
	else
	    return color[(point.y/height)];
    }
}
