/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.importer.scratch;

import java.math.BigDecimal;

/**
 * Represents a Rectangle in Scratch.
 * 
 * We can't use java.awt.Rectangle (for example) because Scratch rectangles can
 * have floating point or integer limits, so hence we must store all measurements
 * using the a class that can represent all of those.  Number would do it, but we also
 * need to perform calculations so BigDecimal seems like a good alternative (plus, Scratch
 * does support arbitrary precision integers).
 * 
 * @author neil
 *
 */
public class ScratchRectangle extends ScratchObject
{
    // x and y are the top-left of the rectangle, x2 and y2 are the bottom right
    public BigDecimal x, y, x2, y2;

    public ScratchRectangle(BigDecimal x, BigDecimal y, BigDecimal x2, BigDecimal y2)
    {
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
    }    

}
