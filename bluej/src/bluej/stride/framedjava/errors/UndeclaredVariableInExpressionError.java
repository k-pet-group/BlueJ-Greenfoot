/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import bluej.Config;
import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.editor.fixes.Correction;
import bluej.editor.fixes.FixSuggestion;
import bluej.stride.framedjava.ast.StringSlotFragment;
import bluej.editor.fixes.Correction.SimpleCorrectionInfo;
import bluej.stride.framedjava.slots.ExpressionSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

public class UndeclaredVariableInExpressionError extends DirectSlotError
{
    private final String varName;
    private final int startPosInSlot;
    private final int endPosInSlot;
    private List<FixSuggestion> corrections = new ArrayList<>();

    /**
     * Creates an error about an undeclared variable being used in an expression. The quick fixes
     * will be to switch the variable name to another similarly spelt variable.
     *
     * @param slotFragment The fragment with the error.
     * @param varName The name of the variable which is used, but not declared
     * @param startPosInSlot The start position in the slot of the variable name (inclusive)
     * @param endPosInSlot The end position in the slot of the variable name (exclusive)
     * @param slot The slot with the error (which will contain slotFragment).
     * @param possibleCorrections The possible other variable names (unfiltered: all variable names which are in scope)
     */
    @OnThread(Tag.FXPlatform)
    public UndeclaredVariableInExpressionError(StringSlotFragment slotFragment, String varName, int startPosInSlot, int endPosInSlot, ExpressionSlot slot, Set<String> possibleCorrections)
    {
        super(slotFragment, DiagnosticOrigin.STRIDE_LATE);
        this.varName = varName;
        this.startPosInSlot = startPosInSlot;
        this.endPosInSlot = endPosInSlot;

        corrections.addAll(Correction.winnowAndCreateCorrections(varName, possibleCorrections.stream().map(SimpleCorrectionInfo::new), s -> slot.replace(startPosInSlot, endPosInSlot, isJavaPos(), s)));
        slot.updateError(this);
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
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return Config.getString("editor.quickfix.undeclaredVar.errorMsg") + varName;
    }

    @Override
    public List<? extends FixSuggestion> getFixSuggestions()
    {
        return corrections;
    }

    @Override
    public boolean isJavaPos()
    {
        return true;
    }
}
