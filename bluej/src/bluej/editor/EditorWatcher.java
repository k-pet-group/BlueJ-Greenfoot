/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2014,2016  Michael Kolling and John Rosenberg
 
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
package bluej.editor;

import java.util.List;

import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.extensions.SourceType;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Interface between the editor and the rest of BlueJ
 * The editor uses this class
 * @author Michael Kolling
 */
public interface EditorWatcher
{
    //key for storing the value of the expand/collapse of the naviview
    public final static String NAVIVIEW_EXPANDED_PROPERTY="naviviewExpandedProperty";
    /**
     * Called by Editor when a file is changed
     */
    void modificationEvent(Editor editor);

    /**
     * Called by Editor when a file is saved
     */
    void saveEvent(Editor editor);

    /**
     * Called by Editor when it is closed
     */
    void closeEvent(Editor editor);

    /**
     * Called by Editor to set/clear a breakpoint
     * @param lineNo the line number of the breakpoint
     * @param set    whether the breakpoint is set (true) or cleared
     * @return             An error message or null if okay.
     */
    String breakpointToggleEvent(int lineNo, boolean set);

    /**
     * Called by Editor when a file is to be compiled
     */
    void compile(Editor editor, CompileReason reason, CompileType type);
    
    /**
     * Called by Editor when documentation is to be compiled
     */
    void generateDoc();  
    
    /**
     * Sets a property
     */
    void setProperty(String key, String value);
    
    /**
     * Gets a property
     */
    String getProperty(String key);

    /**
     * Schedule compilation due to reload or modification
     * @param immediate  True if compilation should be performed immediately; false if compilation should be
     *                   postponed until the user VM is idle
     * @param reason    Reason for compilation
     */
    @OnThread(Tag.Any)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type);
    
    default void recordEdit(SourceType sourceType, String curSource, boolean includeOneLineEdits)
    {
        recordEdit(sourceType, curSource, includeOneLineEdits, null);
    }

    void recordEdit(SourceType sourceType, String curSource, boolean includeOneLineEdits, StrideEditReason reason);

    void clearAllBreakpoints();

    void recordOpen();

    void recordSelected();

    void recordClose();

    void recordShowErrorIndicator(int identifier);

    void recordShowErrorMessage(int identifier, List<String> quickFixes);

    void recordEarlyErrors(List<DiagnosticWithShown> diagnostics);

    void recordLateErrors(List<DiagnosticWithShown> diagnostics);

    void recordFix(int errorIdentifier, int fixIndex);

    // Either lineNumber and columnNumber are non-null and xpath and elementOffset are null,
    // or vice versa
    void recordCodeCompletionStarted(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem);

    // If replacement is null, it was cancelled
    void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement);

    void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key);

    /**
     * Notifies watcher whether we are showing the interface (docs) or not
     */
    void showingInterface(boolean showingInterface);
}
