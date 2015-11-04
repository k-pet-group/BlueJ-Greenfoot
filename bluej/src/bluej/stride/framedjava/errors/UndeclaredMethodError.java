/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.errors;

import java.util.List;

import bluej.stride.framedjava.ast.StringSlotFragment;
import bluej.stride.framedjava.errors.Correction.SimpleCorrectionInfo;
import bluej.stride.framedjava.slots.ExpressionSlot;

public class UndeclaredMethodError extends DirectSlotError
{

    private final String methodName;
    private final int startPosInSlot;
    private final int endPosInSlot;
    private List<? extends FixSuggestion> corrections;

    public UndeclaredMethodError(StringSlotFragment slotFragment, String methodName, int startPosInSlot, int endPosInSlot, ExpressionSlot slot, List<String> possibleCorrections)
    {
        super(slotFragment);
        this.methodName = methodName;
        this.startPosInSlot = startPosInSlot;
        this.endPosInSlot = endPosInSlot;
        
        corrections = Correction.winnowAndCreateCorrections(methodName, possibleCorrections.stream().map(SimpleCorrectionInfo::new), s -> slot.replace(startPosInSlot, endPosInSlot, isJavaPos(), s));
    }
    
    @Override
    public int getStartPosition()
    {
        return startPosInSlot;
    }

    @Override
    public int getEndPosition()
    {
        return endPosInSlot;
    }    

    @Override
    public String getMessage()
    {
        return "Undeclared method: " + methodName;
    }

    @Override
    public List<? extends FixSuggestion> getFixSuggestions()
    {
        return corrections;
    }

    @Override
    public boolean isJavaPos()
    {
        return false;
    }
}
