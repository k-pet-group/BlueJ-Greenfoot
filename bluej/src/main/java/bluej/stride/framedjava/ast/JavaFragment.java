/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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

/**
 * A JavaFragment is a small piece of content in a Java class.  It never spans more than one line,
 * and thus never includes a newline character.
 */
public abstract class JavaFragment
{
    // Positions in .java file on disk:
    private int lineNumber;
    private int columnNumber;
    private int len;
    private String errorMessage = null;
    private int startErrorPos = -1;
    private int endErrorPos = -1;

    public static enum Destination
    {
        /** Source will be as-is, with no substitutions */
        JAVA_FILE_TO_COMPILE,
        /** Source may have substitutions to make it valid */
        SOURCE_DOC_TO_ANALYSE,
        /** Source will be as-is, and no positions recorded */
        TEMPORARY; 
        
        // Should we substitute invalid code (e.g. empty variable name in declaration) for valid code?
        public boolean substitute()
        {
            // Previously we only subsituted when doing code completion and not when compiling properly,
            // because substitution can turn invalid code into valid code.
            // Now we now allow code substitution in all cases, but we monitor code for early errors,
            // and if there's an early error we delete the class file, to prevent substitution causing 
            // successful compilation in the presence of errors: 
            return true;
        }
    };

    @OnThread(Tag.FXPlatform)
    protected abstract String getJavaCode(Destination dest, ExpressionSlot<?> completing, Parser.DummyNameGenerator dummyNameGenerator);

    @OnThread(Tag.FX)
    public abstract ErrorShower getErrorShower();

    @OnThread(Tag.FX)
    public final void recordDiskPosition(int lineNumber, int columnNumber, int len)
    {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.len = len;
    }

    /**
     * This is an enum returned from checkCompileError.  When a Java error is produced by javac, we
     * have to map this back to a location in Stride code.  The way we do this is that we go through
     * the Java fragments (produced from the Stride code) in order, and ask them "Is this error
     * position within your bounds?"  The fragment then returns one of these enum values, documented below.
     * 
     * Note that two adjacent fragments can both say that they overlap the error location, because
     * we include the left and right edges of the fragment, so an error exactly on the boundary of
     * two fragments will cause them both to say overlap.
     */
    static enum ErrorRelation
    {
        /**
         * The error location lies before this fragment.
         */
        BEFORE_FRAGMENT,
        /**
         * The error location overlaps this fragment, and we are a good place to show it (e.g. a user-entered slot).
         */
        OVERLAPS_FRAGMENT,
        /**
         * The error location does overlap this fragment, but is not the best place to show it (e.g. a piece
         * of Java syntax that the user did not enter).  Display on this only if there is no other overlapping
         * fragment that is suitable.
         */
        OVERLAPS_FRAGMENT_FALLBACK,
        /**
         * The error location lies after this fragment.
         */
        AFTER_FRAGMENT,
        /**
         * CANNOT_SHOW means that the fragment can never show errors (e.g. boilerplate)
         */
        CANNOT_SHOW;
    }
    
    public ErrorRelation checkCompileError(int startLine, int startColumn, int endLine, int endColumn)
    {
        if (startLine > lineNumber)
            return ErrorRelation.AFTER_FRAGMENT;
        else if (endLine < lineNumber)
            return ErrorRelation.BEFORE_FRAGMENT;
        
        // Assuming startLine < endLine, now we know that the startLine--endLine range includes lineNumber
        // But we could still be outside the range, if we are before the start column on the start line,
        // or after the end column on the end line, so we need to check those cases.
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

    /**
     * Shows a compile error for this Java fragment, either on the fragment
     * itself or on its redirect (see getCompileErrorRedirect()).
     *
     * @param startLine The .java file error position.  Will be mapped back to position in Java editor.
     * @param startColumn Ditto
     * @param endLine Ditto
     * @param endColumn Ditto
     * @param message The error message
     * @param identifier The error identifier (for data recording purposes).
     */
    @OnThread(Tag.FX)
    public final void showCompileError(int startLine, int startColumn, int endLine, int endColumn, String message, int identifier)
    {
        // The message may be required to evaluate actions on some errors, we save it here
        errorMessage = message;
        // This makes sure we can't end up in a loop; we only ever do one redirect:
        JavaFragment redirect = getCompileErrorRedirect();
        if (redirect != null)
        {
            // If we redirect, we span the whole fragment:
            new JavaCompileError(redirect, 0, redirect.len, message, identifier);
        }
        else
        {
            startErrorPos = getErrorStartPos(startLine, startColumn);
            endErrorPos = getErrorEndPos(endLine, endColumn);

            new JavaCompileError(this, startErrorPos, endErrorPos, message, identifier);
        }
    }

    protected String getErrorMessage()
    {
        return errorMessage;
    }

    protected int getErrorStartPos()
    {
        return startErrorPos;
    }

    protected int getErrorEndPos()
    {
        return endErrorPos;
    }

    /**
     * Gets the compile error redirect target.  Some Java fragments
     * (e.g. the class keyword of a class declaration) don't support
     * showing errors, usually because they can't get keyboard focus
     * in the Stride error (which would mean the error could only be
     * read with the mouse: bad for accessibility).  So such fragments
     * implement this method to return a nearby JavaFragment which can
     * show an error.  Returns null if there's no redirect.
     */
    @OnThread(Tag.FX)
    protected abstract JavaFragment getCompileErrorRedirect();

    public int getErrorEndPos(int endLine, int endColumn)
    {
        int endPos;
        if (endLine > lineNumber)
            endPos = len;
        else if (endLine == lineNumber)
            endPos = Math.max(0, Math.min(len, endColumn - columnNumber));
        else // If endLine is earlier, we are the nearest fragment; highlight everything:
            endPos = len;
        return endPos;
    }

    public int getErrorStartPos(int startLine, int startColumn)
    {
        int startPos;
        if (startLine < lineNumber)
            startPos = 0;
        else if (startLine == lineNumber)
            startPos = Math.min(Math.max(0, startColumn - columnNumber), len);
        else // If startLine is later, we are the nearest fragment; highlight everything:
            startPos = 0;
        return startPos;
    }

    /**
     * Finds any pre-compilation errors in this element, i.e. syntax errors.
     * 
     * If this element returns no errors, then the code can be generated into valid Java.
     * 
     * (TODO in the future, this should strengthen to: returning no errors means code won't give syntax error)
     */
    @OnThread(Tag.FXPlatform)
    public abstract Stream<SyntaxCodeError> findEarlyErrors();

    @OnThread(Tag.FXPlatform)
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
