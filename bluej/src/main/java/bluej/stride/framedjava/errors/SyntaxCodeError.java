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

import java.util.Collections;
import java.util.List;

import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.editor.fixes.FixSuggestion;
import bluej.stride.framedjava.ast.SlotFragment;
import threadchecker.OnThread;
import threadchecker.Tag;

public class SyntaxCodeError extends DirectSlotError
{
    private final String message;

    @OnThread(Tag.Any)
    public SyntaxCodeError(SlotFragment slot)
    {
        this(slot, "Syntax error");
    }
    
    @OnThread(Tag.Any)
    public SyntaxCodeError(SlotFragment slot, String msg)
    {
        super(slot, DiagnosticOrigin.STRIDE_EARLY);
        this.message = msg;
    }
    
    @Override
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return message;
    }

    @Override
    public List<FixSuggestion> getFixSuggestions()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isJavaPos()
    {
        return false;
    }

}
