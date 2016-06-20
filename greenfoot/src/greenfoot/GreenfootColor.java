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

import java.awt.Color;

/**
 * A representation of a Color. The Color can be used to draw or fill shapes on
 * the screen.
 *
 * @author Fabio Heday
 */
public class GreenfootColor
{

    /**
     * The color white. In the default sRGB space.
     */
    public final static Color white = new Color(255, 255, 255);

    /**
     * The color white. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color WHITE = white;

    /**
     * The color light gray. In the default sRGB space.
     */
    public final static Color lightGray = new Color(192, 192, 192);

    /**
     * The color light gray. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color LIGHT_GRAY = lightGray;

    /**
     * The color gray. In the default sRGB space.
     */
    public final static Color gray = new Color(128, 128, 128);

    /**
     * The color gray. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color GRAY = gray;

    /**
     * The color dark gray. In the default sRGB space.
     */
    public final static Color darkGray = new Color(64, 64, 64);

    /**
     * The color dark gray. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color DARK_GRAY = darkGray;

    /**
     * The color black. In the default sRGB space.
     */
    public final static Color black = new Color(0, 0, 0);

    /**
     * The color black. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color BLACK = black;

    /**
     * The color red. In the default sRGB space.
     */
    public final static Color red = new Color(255, 0, 0);

    /**
     * The color red. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color RED = red;

    /**
     * The color pink. In the default sRGB space.
     */
    public final static Color pink = new Color(255, 175, 175);

    /**
     * The color pink. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color PINK = pink;

    /**
     * The color orange. In the default sRGB space.
     */
    public final static Color orange = new Color(255, 200, 0);

    /**
     * The color orange. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color ORANGE = orange;

    /**
     * The color yellow. In the default sRGB space.
     */
    public final static Color yellow = new Color(255, 255, 0);

    /**
     * The color yellow. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color YELLOW = yellow;

    /**
     * The color green. In the default sRGB space.
     */
    public final static Color green = new Color(0, 255, 0);

    /**
     * The color green. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color GREEN = green;

    /**
     * The color magenta. In the default sRGB space.
     */
    public final static Color magenta = new Color(255, 0, 255);

    /**
     * The color magenta. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color MAGENTA = magenta;

    /**
     * The color cyan. In the default sRGB space.
     */
    public final static Color cyan = new Color(0, 255, 255);

    /**
     * The color cyan. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color CYAN = cyan;

    /**
     * The color blue. In the default sRGB space.
     */
    public final static Color blue = new Color(0, 0, 255);

    /**
     * The color blue. In the default sRGB space.
     *
     * @since 1.4
     */
    public final static Color BLUE = blue;

    private final Color color;

    /**
     * Creates an opaque RGB color with the specified red, green and blue values
     * in the range (0.0 - 1.0)
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     */
    public GreenfootColor(float r, float g, float b)
    {
        this.color = new Color(r, g, b);
    }

    /**
     * Creates a GreenfootColor based on a java.awt.Color
     *
     * @param c the java.awt.Color
     */
    public GreenfootColor(Color c)
    {
        this.color = c;
    }

    /**
     * Creates a RGB color with the specified red, green, blue, and alpha values
     * in the range (0.0 - 1.0).
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the aplha component
     */
    public GreenfootColor(float r, float g, float b, float a)
    {
        this.color = new Color(r, g, b, a);
    }

    /**
     * Creates an opaque RGB color with the specified combined RGB value
     * consisting of the red component in bits 16-23, the green component in
     * bits 8-15, and the blue component in bits 0-7.
     *
     * @param rgb the combined RGB component
     */
    public GreenfootColor(int rgb)
    {
        this.color = new Color(rgb);
    }

    /**
     * Creates a RGB color with the specified combined RGBA value consisting of
     * the alpha component in bits 24-31, the red component in bits 16-23, the
     * green component in bits 8-15, and the blue component in bits 0-7. If the
     * hasalpha argument is false, alpha is defaulted to 255.
     *
     * @param rgba the combined RGBA component
     * @param hasalpha true if alpha bits are valid; false otherwise
     */
    public GreenfootColor(int rgba, boolean hasalpha)
    {
        this.color = new Color(rgba, hasalpha);
    }

    /**
     * Creates a RGB color with the specified red, green, blue values in the
     * range (0 - 255).
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     */
    public GreenfootColor(int r, int g, int b)
    {
        this.color = new Color(r, g, b);
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
    public GreenfootColor(int r, int g, int b, int a)
    {
        this.color = new Color(r, g, b, a);
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
    public GreenfootColor brighter()
    {
        return new GreenfootColor(this.color.brighter());
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
    public GreenfootColor darker()
    {
        return new GreenfootColor(this.color.darker());
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
    public boolean equals(Object obj)
    {
        return obj instanceof GreenfootColor && ((GreenfootColor) obj).getRGB() == this.getRGB();
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
     * Returns the RGB value representing the color in the default RGB.
     * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are
     * blue).
     * @return the RGB value of the color.
     */
    public int getRGB() {
        return this.color.getRGB();
    }

    /**
     * return the internal Color object representing the GreenfootColor
     *
     * @return the Color object.
     */
    protected Color getColorObject()
    {
        return this.color;
    }

}
