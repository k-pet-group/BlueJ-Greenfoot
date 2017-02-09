/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2016,2017  Poul Henriksen and Michael Kolling
 
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
    public final static Color WHITE = new Color(255, 255, 255);

    /**
     * The color light gray. In the default RGB space.
     */
    public final static Color LIGHT_GRAY = new Color(192, 192, 192);

    /**
     * The color gray. In the default RGB space.
     */
    public final static Color GRAY = new Color(128, 128, 128);

    /**
     * The color dark gray. In the default RGB space.
     */
    public final static Color DARK_GRAY = new Color(64, 64, 64);

    /**
     * The color black. In the default RGB space.
     */
    public final static Color BLACK = new Color(0, 0, 0);

    /**
     * The color red. In the default RGB space.
     */
    public final static Color RED = new Color(255, 0, 0);

    /**
     * The color pink. In the default RGB space.
     */
    public final static Color PINK = new Color(255, 175, 175);

    /**
     * The color orange. In the default RGB space.
     */
    public final static Color ORANGE = new Color(255, 200, 0);

    /**
     * The color yellow. In the default RGB space.
     */
    public final static Color YELLOW = new Color(255, 255, 0);

    /**
     * The color green. In the default RGB space.
     */
    public final static Color GREEN = new Color(0, 255, 0);

    /**
     * The color magenta. In the default RGB space.
     */
    public final static Color MAGENTA = new Color(255, 0, 255);

    /**
     * The color cyan. In the default RGB space.
     */
    public final static Color CYAN = new Color(0, 255, 255);

    /**
     * The color blue. In the default RGB space.
     */
    public final static Color BLUE = new Color(0, 0, 255);

    private final java.awt.Color color;

    /**
     * Creates a color based on a java.awt.Color
     *
     * @param c the java.awt.Color
     */
    Color(java.awt.Color c)
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
     * Creates a new color that is a brighter version of this GreenfootColor.
     * <p>
     * This method creates a brighter version of this color. The alpha value is
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
     * This method creates a darker version of this color. The alpha value is
     * preserved. Although <code>brighter</code> and <code>darker</code> are
     * inverse operations, the results of a series of invocations of these two
     * methods might be inconsistent because of rounding errors.
     *
     * @return a new GreenfootColor object that is a darker version of this
     * GreenfootColor with the same alpha value.
     */
    public Color darker()
    {
        return new Color(this.color.darker());
    }

    /**
     * Determines whether another object is equal to this color.
     * <p>
     * The result is true if and only if the argument is not null and is a
     * greenfoot.Color object that has the same red, green, blue, and alpha
     * values as this object.
     *
     * @param obj the object to test for equality with this color
     * @return true if the colors are the same; false otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Color && ((Color) obj).getColorObject().equals(this.color);
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
     * Computes the hash code for this <code>Color</code>.
     *
     * @return a hash code value for this object.
     *
     */
    public int hashCode()
    {
        return this.color.hashCode();
    }

    /**
     * Return a text representation of the color.
     */
    @Override
    public String toString()
    {
        return "Color{" + "color=" + color + '}';
    }

    /**
     * Return the internal color object representing the color
     *
     * @return the Color object.
     */
    java.awt.Color getColorObject()
    {
        return this.color;
    }

}
