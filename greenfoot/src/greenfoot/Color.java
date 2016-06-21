/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2016  Poul Henriksen and Michael Kolling
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;


/**
 * A representation of a Color. The Color can be used to draw or fill shapes on
 * the screen.
 *
 * @author Fabio Heday
 */
public class Color
{

    /**
     * The color white. In the default RGB space.
     */
    public final static Color white = new Color(255, 255, 255);

    /**
     * The color white. In the default RGB space.
     */
    public final static Color WHITE = white;

    /**
     * The color light gray. In the default RGB space.
     */
    public final static Color lightGray = new Color(192, 192, 192);

    /**
     * The color light gray. In the default RGB space.
     */
    public final static Color LIGHT_GRAY = lightGray;

    /**
     * The color gray. In the default RGB space.
     */
    public final static Color gray = new Color(128, 128, 128);

    /**
     * The color gray. In the default RGB space.
     */
    public final static Color GRAY = gray;

    /**
     * The color dark gray. In the default RGB space.
     */
    public final static Color darkGray = new Color(64, 64, 64);

    /**
     * The color dark gray. In the default RGB space.
     */
    public final static Color DARK_GRAY = darkGray;

    /**
     * The color black. In the default RGB space.
     */
    public final static Color black = new Color(0, 0, 0);

    /**
     * The color black. In the default RGB space.
     */
    public final static Color BLACK = black;

    /**
     * The color red. In the default RGB space.
     */
    public final static Color red = new Color(255, 0, 0);

    /**
     * The color red. In the default RGB space.
     */
    public final static Color RED = red;

    /**
     * The color pink. In the default RGB space.
     */
    public final static Color pink = new Color(255, 175, 175);

    /**
     * The color pink. In the default RGB space.
     */
    public final static Color PINK = pink;

    /**
     * The color orange. In the default RGB space.
     */
    public final static Color orange = new Color(255, 200, 0);

    /**
     * The color orange. In the default RGB space.
     */
    public final static Color ORANGE = orange;

    /**
     * The color yellow. In the default RGB space.
     */
    public final static Color yellow = new Color(255, 255, 0);

    /**
     * The color yellow. In the default RGB space.
     */
    public final static Color YELLOW = yellow;

    /**
     * The color green. In the default RGB space.
     */
    public final static Color green = new Color(0, 255, 0);

    /**
     * The color green. In the default RGB space.
     */
    public final static Color GREEN = green;

    /**
     * The color magenta. In the default RGB space.
     */
    public final static Color magenta = new Color(255, 0, 255);

    /**
     * The color magenta. In the default RGB space.
     */
    public final static Color MAGENTA = magenta;

    /**
     * The color cyan. In the default RGB space.
     */
    public final static Color cyan = new Color(0, 255, 255);

    /**
     * The color cyan. In the default RGB space.
     */
    public final static Color CYAN = cyan;

    /**
     * The color blue. In the default RGB space.
     */
    public final static Color blue = new Color(0, 0, 255);

    /**
     * The color blue. In the default RGB space.
     */
    public final static Color BLUE = blue;

    private final java.awt.Color color;

    /**
     * Creates a GreenfootColor based on a java.awt.Color
     *
     * @param c the java.awt.Color
     */
    protected Color(java.awt.Color c)
    {
        this.color = c;
    }
    
    /**
     * Creates a RGB color with the specified red, green, blue values in the
     * range (0 - 255).
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     */
    public Color(int r, int g, int b)
    {
        this.color = new java.awt.Color(r, g, b);
    }

    /**
     * Creates a RGB color with the specified red, green, blue, and alpha values
     * in the range (0 - 255).
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     */
    public Color(int r, int g, int b, int a)
    {
        this.color = new java.awt.Color(r, g, b, a);
    }

    /**
     * Creates a new GreenfootColor that is a brighter version of this
     * GreenfootColor.
     * <p>
     * This method creates a brighter version of this Color. The alpha value is
     * preserved. Although <code>brighter</code> and <code>darker</code> are
     * inverse operations, the results of a series of invocations of these two
     * methods might be inconsistent because of rounding errors.
     *
     * @return a new GreenfootColor object that is a brighter version of this
     * GreenfootColor with the same alpha value.
     */
    public Color brighter()
    {
        return new Color(this.color.brighter());
    }

    /**
     * Creates a new GreenfootColor that is a darker version of this
     * GreenfootColor.
     * <p>
     * This method creates a darker version of this GreenfootColor. The alpha
     * value is preserved. Although <code>brighter</code> and
     * <code>darker</code> are inverse operations, the results of a series of
     * invocations of these two methods might be inconsistent because of
     * rounding errors.
     *
     * @return a new GreenfootColor object that is a darker version of this
     * GreenfootColor with the same alpha value.
     */
    public Color darker()
    {
        return new Color(this.color.darker());
    }

    /**
     * Determines whether another object is equal to this GreenfootColor.
     * <p>
     * The result is true if and only if the argument is not null and is a
     * Greenfoot object that has the same red, green, blue, and alpha values as
     * this object.
     *
     * @param obj the object to test for equality with this GreenfootColor
     * @return true if the objects are the same; false otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Color && ((Color) obj).getColorObject().getRGB() == this.color.getRGB();
    }

    /**
     * Returns the red component in the range 0-255 in the default RGB space.
     *
     * @return the red component.
     */
    public int getRed()
    {
        return this.color.getRed();
    }

    /**
     * Returns the green component in the range 0-255 in the default RGB space.
     *
     * @return the green component.
     */
    public int getGreen()
    {
        return this.color.getGreen();
    }

    /**
     * Returns the alpha component in the range 0-255.
     *
     * @return the alpha component.
     */
    public int getAlpha()
    {
        return this.color.getAlpha();
    }

    /**
     * Returns the blue component in the range 0-255 in the default RGB space.
     *
     * @return the blue component.
     */
    public int getBlue()
    {
        return this.color.getBlue();
    }
    
    
    /**
     * return the internal Color object representing the GreenfootColor
     *
     * @return the Color object.
     */
    protected java.awt.Color getColorObject()
    {
        return this.color;
    }

}
