/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.convert;

import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A warning which occurred during the conversion from Java to Stride
 */
public abstract class ConversionWarning
{
    private final String text;

    // labelId is looked up in the Strings, then item is appended
    private ConversionWarning(String labelId, String item)
    {
        this.text = Config.getString(labelId).trim() + " " + item;
    }

    /**
     * An unsupported modifier (e.g. synchronized, volatile) or annotation (e.g. @Test) was found
     */
    @OnThread(Tag.Any)
    public static class UnsupportedModifier extends ConversionWarning
    {
        public UnsupportedModifier(String context, String modifier)
        {
            super("stride.convert.unsupported.modifier", context + ": " + modifier);
        }
    }

    /**
     * An unsupported language feature was found (e.g. synchronized block, anonymous inner class)
     */
    @OnThread(Tag.Any)
    public static class UnsupportedFeature extends ConversionWarning
    {
        public UnsupportedFeature(String feature)
        {
            super("stride.convert.unsupported.feature", ": " + feature);
        }
    }

    /**
     * Gets the message to show to the user
     */
    public String getMessage()
    {
        return text;
    }

    // For better output in test failures:
    public String toString()
    {
        return getClass() + "[" + text + "]";
    }
}
