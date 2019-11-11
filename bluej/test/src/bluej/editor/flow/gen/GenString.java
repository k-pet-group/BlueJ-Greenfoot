/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.editor.flow.gen;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class GenString extends Generator<String>
{
    public GenString()
    {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus)
    {
        // Make the empty string slightly more likely:
        if (sourceOfRandomness.nextInt(50) == 1)
            return "";
        int[] codepoints = new int[sourceOfRandomness.nextInt(4000)];
        for (int i = 0; i < codepoints.length; i++)
        {
            // Make ASCII more likely:
            int limit = sourceOfRandomness.nextBoolean() ? 128 : 0x10ffff;
            // Good amount of newlines:
            int n = sourceOfRandomness.nextInt(30) == 1 ? '\n' : sourceOfRandomness.nextInt(limit);
            // Avoid undefined and invalid characters:
            if (Character.isValidCodePoint(n) && Character.isDefined(n) && n != '\r')
            {
                codepoints[i] = n;
            }
        }
        return new String(codepoints, 0, codepoints.length);
    }
}
