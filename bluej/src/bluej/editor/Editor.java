/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2015,2016,2017,2018,2019,2020,2021  Michael Kolling and John Rosenberg
 
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

import java.io.IOException;
import java.util.List;

import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.editor.fixes.EditorFixesManager;
import bluej.prefmgr.PrefMgr.PrintSize;
import bluej.editor.stride.FrameEditor;
import bluej.parser.symtab.ClassInfo;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXRunnable;
import javafx.print.PrinterJob;
import javafx.scene.image.Image;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Interface between an editor and the rest of BlueJ
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public interface Editor
{
    /**
     * Reload and display the same file that was displayed before.
     * This should generated a modificationEvent followed by a saveEvent.
     */
    void reloadFile();

    /**
     * Show the editor window. This includes whatever is necessary of the
     * following: make visible, de-iconify, bring to front of window stack.
     * 
     * @param vis  true to make the editor visible, or false to hide it.
     * @param openInNewWindow if this is true, the editor opens in a new window
     */
    void setEditorVisible(boolean vis, boolean openInNewWindow);

    /**
     * True if the editor is open in the tabbed window.
     * 
     * @return true if editor is an open tab
     */
    boolean isOpen();

    /**
     * Save the buffer to disk under the current file name. This is an error if
     * the editor has not been given a file name (ie. if readFile was not
     * executed).
     * 
     * If save() is called on an unmodified file, it returns immediately without
     * re-writing the file to disk.
     */
    void save() throws IOException;

    default void saveJavaWithoutWarning() throws IOException
    {
        save();
    }

    /**
     * Close the editor window.
     */
    void close();

    /**
     * Refresh the editor display (needed if font size has changed)
     */
    void refresh();

    /**
     * Display a message (used for compile/runtime errors). An editor must
     * support at least two lines of message text, so the message can contain
     * a newline character.
     *  @param message    the message to be displayed
     * @param lineNumber    the line to move the cursor to (the line is also
     *        highlighted)
     * @param column        the column to move the cursor to
     */
    void displayMessage(String message, int lineNumber, int column);

    /**
     * Display a diagnostic message from the compiler.
     * 
     * @param diagnostic  The diagnostic to be displayed.
     * @param errorIndex The index of the error (first is 0, second is 1, etc)
     * @param compileType The type of the compilation which caused the error to show
     * @return Whether the error was shown to the user (true) or not (false).  Some editors
     *          only show the first error, for example, or the first N.
     */
    boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType);
    
    /**
     * Set a step mark due to execution hitting a break point / completing a step, or selection
     * of a stack frame in the debugger.
     * 
     * @param lineNumber  The line number of the step/selection
     * @param message     Message to be displayed (may be null)
     * @param isBreak     Thread execution was suspended at the given line (i.e. breakpoint/step/halt).
     * @param thread      The thread that was suspended/selected
     *                    
     * @return true if the debugger display is already taken care of, or
     *         false if you still want to show the ExecControls window afterwards.
     */
    boolean setStepMark(int lineNumber, String message, boolean isBreak, DebuggerThread thread);
    
    /**
     *  Display a message into the info area.
     *  The message will be cleared when the caret is moved.
     *  
     *  @param msg the message to display
     */
    public void writeMessage(String msg);

    /**
     * Remove the step mark (the mark that shows the current line when
     * single-stepping through code). If it is not currently displayed, do
     * nothing.
     */
    void removeStepMark();

    /**
     * Change class name.
     * 
     * @param title        new window title
     * @param filename     new file name
     * @param javaFilename If Java source, same as filename.  If Stride source, target Java file.
     * @param docFileName  new documentation file name
     */
    void changeName(String title, String filename, String javaFilename, String docFileName);

    /**
     * Set the "compiled" status, as shown in the class diagram.
     * 
     * @param compiled    true if the class has been compiled
     */
    void setCompiled(boolean compiled);
    
    /**
     * Tells the editor that a compilation has begun
     *
     * @param compilationSequence A sequence identifier for this compilation, for data recording purposes.
     *                            This is to be passed to the DataCollector.compiled method for recording.
     *                            It may be -1 if it is non-applicable or unknown.
     * @return True if there is a known error
     */
    boolean compileStarted(int compilationSequence);
    
    /**
     * Informs the editor that a compilation requested via the EditorWatcher interface has finished.
     *
     * @param successful   whether the compilation was successful
     * @param classesKept
     */
    void compileFinished(boolean successful, boolean classesKept);

    /**
     * All breakpoints have been cleared for this class, update the
     * editor display to reflect this.
     */
    void removeBreakpoints();
    
    /**
     * Breakpoints have been reset due to compilation or
     * similar. Re-initialize the breakpoints by re-setting them via the
     * EditorWatcher interface.
     */
    void reInitBreakpoints();

    /**
     * Determine whether this editor has been modified from the version on disk
     * 
     * @return a boolean indicating whether the file is modified
     */
    boolean isModified();

    void removeErrorHighlights();

    /**
     * A callback to update the latest progress when printing a file.  Also allows cancellation
     * by returning false.  Callers must check and obey the return!
     */
    public static interface PrintProgressUpdate
    {
        /**
         * 
         * @param curProgress Number of lines printed
         * @param totalProgress Total number of lines to print
         * @return true to continue printing, false to cancel
         */
        @OnThread(Tag.Any)
        public boolean printProgress(int curProgress, int totalProgress);
    }
    
    /**
     * Returns an action which will print the contents of the editor
     */
    FXRunnable printTo(PrinterJob printerJob, PrintSize printSize, boolean printLineNumbers, boolean printScopeBackgrounds, PrintProgressUpdate progressUpdateCallback);

    /**
     * Set the 'read-only' property of this editor.
     * @param readOnly  If true, editor is non-editable.
     */
    void setReadOnly(boolean readOnly);

    /**
     * Test if this editor is 'read-only'.
     * @return the readOnlyStatus. If true, editor is non-editable.
     */
    boolean isReadOnly();
    
    /**
     * Set the view of this editor to display either the source or the interface
     * of the class.
     * @param interfaceStatus If true, display class interface, otherwise source.
     */
    void showInterface(boolean interfaceStatus);

    /**
     * Returns a property of the current editor.
     *
     * @param  propertyKey  The propertyKey of the property to retrieve.
     * @return              the property value or null if it is not found
     */
    Object getProperty(String propertyKey);

    /**
     * Set a property for the current editor. Any existing property with
     * this key will be overwritten.
     *
     * @param  propertyKey  The property key of the new property
     * @param  value        The new property value
     */
    void setProperty(String propertyKey, Object value);

    /**
     * Obtain the TextEditor implementation of this editor, if it has one. May return null if no
     * TextEditor implementation is available.
     */
    TextEditor assumeText();
    
    /**
     * Obtain the FrameEditor implementation of this editor, if it has one. May return null if no
     * FrameEditor implementation is available.
     */
    FrameEditor assumeFrame();
    
    /**
     * Create a new method, or appending the contents if the method already exists
     *
     * @param method element
     * @param after will be passed true if the method did not exist already and was inserted, false otherwise (will always be run)
     */
    void insertAppendMethod(NormalMethodElement method, FXPlatformConsumer<Boolean> after);

    /**
     * Insert a method call in constructor, if it does not already exists
     * 
     * @param methodCall element 
     * @param after will be passed true if the call did not exist already and was inserted
     */
    void insertMethodCallInConstructor(String className, CallElement methodCall, FXPlatformConsumer<Boolean> after);

    /**
     * Focuses the method of the given name in the editor.  If the paramTypes are non-null
     * then it uses them to distinguish between overloaded methods.  If paramTypes is null,
     * it focuses an arbitrary choice of any overloaded methods with that name.
     *
     * @param methodName The name of the method to focus in the editor
     * @param paramTypes The types of the parameters, to narrow down overloads, or null
     *                   if you don't know them (in which case if the method is overloaded,
     *                   it will show an arbitrary pick for the method.)
     */
    void focusMethod(String methodName, List<String> paramTypes);

    /**
     * For a class, set the extends section to extend the given className
     */
    void setExtendsClass(String className, ClassInfo classInfo);

    /**
     * For a class, remove the extends section
     */
    void removeExtendsClass(ClassInfo classInfo);

    /**
     * For a class, add the given (interface) type name to the implements clause
     */
    void addImplements(String interfaceName, ClassInfo classInfo);

    /**
     * For an interface, add to the extends section to extend the given interfaceName
     */
    void addExtendsInterface(String interfaceName, ClassInfo classInfo);

    /**
     * For a class or interface, remove the given interface from the implements/extends section
     */
    void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo);

    /**
     * Removes any imports that exactly match the given imports (e.g. java,awt,Color, java.util.*)
     * 
     * Other imports which may overlap (e.g. java.awt.*, java.util.List) will not be altered.
     */
    void removeImports(List<String> importTargets);

    /**
     * Adds the specified import into the editor;
     */
    void addImportFromQuickFix(String importName);


    /**
     * Set the header image (in the tab header) for this editor
     * @param image The image to use (any size).
     */
    void setHeaderImage(Image image);

    /**
     * Sets the editor's last modified time for the file.
     * @param millisSinceEpoch
     */
    void setLastModified(long millisSinceEpoch);

    /**
     *  Gets the associated Quick Fixes error manager of an Editor
     *  Each implementation of an editor is responsible for holding an instance
     *  of EditorFixesManager.
     */
    EditorFixesManager getEditorFixesManager();
}
