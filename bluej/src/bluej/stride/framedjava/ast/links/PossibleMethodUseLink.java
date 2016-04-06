/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.slots.UnderlineContainer;

import java.util.function.Supplier;

/**
 * Created by neil on 30/06/2015.
 */
public class PossibleMethodUseLink extends PossibleLink
{
    private final Supplier<PosInSourceDoc> position;
    private final String methodName;
    private final int numParams;

    public PossibleMethodUseLink(String methodName, int numParams, Supplier<PosInSourceDoc> position, int startPosition, int endPosition, UnderlineContainer slot)
    {
        super(startPosition, endPosition, slot);
        this.methodName = methodName;
        this.position = position;
        this.numParams = numParams;
    }

    public Supplier<PosInSourceDoc> getSourcePositionSupplier()
    {
        return position;
    }

    public String getMethodName()
    {
        return methodName;
    }

    /**
     * Used to guess which method is required from an overloaded list
     */
    public int getNumParams()
    {
        return numParams;
    }
}
