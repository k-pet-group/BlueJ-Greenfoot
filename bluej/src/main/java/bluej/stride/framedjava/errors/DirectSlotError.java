/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2020 Michael Kölling and John Rosenberg
 
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

import java.io.File;

import bluej.collect.DiagnosticWithShown;
import bluej.compiler.CompilerAPICompiler;
import bluej.compiler.Diagnostic;
import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.StringSlotFragment;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.application.Platform;

/**
 * A class for errors which are directly targeted at a given slot,
 * like our own unknown-variable errors or extra semi-colon error
 * (as distinct from JavaCompileError, which is targeted at Java code, which maps to a slot)
 */
public abstract class DirectSlotError extends CodeError
{
    @OnThread(Tag.Any)
    private final DiagnosticOrigin origin;

    /**
     * Creates a new DirectSlotError
     * @param code The SlotFragment which this error refers to
     * @param origin The origin of the error (typically, STRIDE_EARLY or STRIDE_LATE)
     */
    @OnThread(Tag.FXPlatform)
    public DirectSlotError(SlotFragment code, DiagnosticOrigin origin)
    {
        super(code, CompilerAPICompiler.getNewErrorIdentifer());
        this.origin = origin;
    }

    @OnThread(Tag.FXPlatform)
    public synchronized DiagnosticWithShown toDiagnostic(String javaFileName, File strideFileName)
    {
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.ERROR, getMessage(), javaFileName, -1, -1, -1, -1, origin, getIdentifier());
        diagnostic.setXPath(path, -1, -1);
        return new DiagnosticWithShown(diagnostic, false, strideFileName);
    }

    @OnThread(Tag.Any)
    public int getItalicMessageStartIndex(){
        return -1;
    }

    @OnThread(Tag.Any)
    public int getItalicMessageEndIndex(){
        return -1;
    }
}
