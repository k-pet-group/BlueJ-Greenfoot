package javablue.GUIBuilder;

import java.awt.*;

/**
 * A class encapsulating an instance of the class Color.
 * It is unlike Color not immutable.
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ColorCap
{
    private Color color = new Color(0);

    
     /**
     * Constructs a ColoCap.
     *
     * @param color	    The color to be encapsulated.
     */
    public ColorCap(Color color)
    {
        this.color = new Color(color.getRGB());
    }


     /**
     * Sets the color to be encapsulated.
     *
     * @param color	The color to be encapsulated.
     */
    public void setColor(Color color)
    {
        this.color = new Color(color.getRGB());
    }


    /**
     * Returns the encapsulated color.
     *
     * @return	The encapsulated color.
     */
    public Color getColor()
    {
        Color newColor = new Color(color.getRGB());
        return newColor;
    }
    
    
} // ColorCap
