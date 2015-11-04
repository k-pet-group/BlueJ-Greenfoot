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
package bluej.stride.framedjava.ast;

import java.util.stream.Stream;

import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.errors.JavaCompileError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

public abstract class JavaFragment
{
    // Positions in .java file on disk:
    private int lineNumber;
    private int columnNumber;
    private int len;

    public static enum Destination
    {
        /** Source will be as-is, with no substitutions */
        JAVA_FILE_TO_COMPILE,
        /** Source may have substitutions to make it valid */
        SOURCE_DOC_TO_ANALYSE,
        /** Source will be as-is, and no positions recorded */
        TEMPORARY; 
        
        public boolean substitute()
        {
            return this == SOURCE_DOC_TO_ANALYSE;
        }
    };
    
    protected abstract String getJavaCode(Destination dest, ExpressionSlot<?> completing);

    @OnThread(Tag.FX)
    public abstract ErrorShower getErrorShower();

    @OnThread(Tag.FX)
    public final void recordDiskPosition(int lineNumber, int columnNumber, int len)
    {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.len = len;
    }
    
    public static enum ErrorRelation
    {
        // CANNOT_SHOW means that the Fragment can never show errors (e.g. boilerplate)
        BEFORE_FRAGMENT, OVERLAPS_FRAGMENT, AFTER_FRAGMENT, CANNOT_SHOW;
    }
    
    public ErrorRelation checkCompileError(int startLine, int startColumn, int endLine, int endColumn)
    {
        if (startLine > lineNumber)
            return ErrorRelation.AFTER_FRAGMENT;
        else if (endLine < lineNumber)
            return ErrorRelation.BEFORE_FRAGMENT;
        
        // Assuming startLine < endLine, now we know that the startLine--endLine range includes lineNumber
        
        if (startLine == lineNumber)
        {
            if (startColumn > columnNumber + len)
                return ErrorRelation.AFTER_FRAGMENT;
            
            // We are after the start; but are we before the end?
            
            if (endLine > lineNumber)
                return ErrorRelation.OVERLAPS_FRAGMENT;
            // Now we know endLine == lineNumber:
            else if (endColumn < columnNumber)
                return ErrorRelation.BEFORE_FRAGMENT;
            
            // Otherwise we must be before the end
        }
        // Now we know that startLine < lineNumber
        else if (endLine == lineNumber)
        {
            if (endColumn < columnNumber + len)
                return ErrorRelation.BEFORE_FRAGMENT;
        }
        // Otherwise, startLine < lineNumber and endLine > lineNumber
                    
        return ErrorRelation.OVERLAPS_FRAGMENT;
    }

    @OnThread(Tag.FX)
    public final void showCompileError(int startLine, int startColumn, int endLine, int endColumn, String message)
    {
        // This makes sure we can't end up in a loop; we only ever do one redirect:
        JavaFragment redirect = getCompileErrorRedirect();
        if (redirect != null)
        {
            // If we redirect, we span the whole fragment:
            new JavaCompileError(redirect, 0, redirect.len, message);
        }
        else
            this.showCompileErrorDirect(startLine, startColumn, endLine, endColumn, message);
    }

    @OnThread(Tag.FX)
    protected abstract JavaFragment getCompileErrorRedirect();

    private void showCompileErrorDirect(int startLine, int startColumn, int endLine, int endColumn, String message)
    {
        int startPos;
        int endPos;

        if (startLine < lineNumber)
            startPos = 0;
        else if (startLine == lineNumber)
            startPos = Math.min(Math.max(0, startColumn - columnNumber), len);
        else // If startLine is later, we are the nearest fragment; highlight everything:
            startPos = 0;
        
        if (endLine > lineNumber)
            endPos = len;
        else if (endLine == lineNumber)
            endPos = Math.max(0, Math.min(len, endColumn - columnNumber));
        else // If endLine is earlier, we are the nearest fragment; highlight everything:
            endPos = len;

        new JavaCompileError(this, startPos, endPos, message);
    }
    
    /**
     * Finds any pre-compilation errors in this element, i.e. syntax errors.
     * 
     * If this element returns no errors, then the code can be generated into valid Java.
     * 
     * (TODO in the future, this should strengthen to: returning no errors means code won't give syntax error)
     */
    public abstract Stream<SyntaxCodeError> findEarlyErrors();

    @OnThread(Tag.FX)
    public abstract void addError(CodeError codeError);

    public class PosInSourceDoc
    {
        public final int offset;

        public PosInSourceDoc(int offset)
        {
            this.offset = offset;
        }
        /*public PosInSourceDoc()
        {
            this.offset = 0;
        }*/
        public JavaFragment getFragment() { return JavaFragment.this; }
    }


    public PosInSourceDoc getPosInSourceDoc(int offset)
    {
        return new PosInSourceDoc(offset);
    }
    public PosInSourceDoc getPosInSourceDoc()
    {
        return getPosInSourceDoc(0);
    }
}
