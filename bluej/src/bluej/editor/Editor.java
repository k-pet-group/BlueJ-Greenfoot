/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg 
 
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

import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.editor.stride.FrameEditor;
import bluej.parser.symtab.ClassInfo;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
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
     */
    void setVisible(boolean vis);

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
     */
    void setStepMark(int lineNumber, String message, boolean isBreak, DebuggerThread thread);
    
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
     * Set the "compiled" status
     * 
     * @param compiled    true if the class has been compiled
     */
    void setCompiled(boolean compiled);
    
    /**
     * Tells the editor that a compilation has begun
     * 
     * @return True if there is a known error
     */
    boolean compileStarted();
    
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

    /**
     * Prints the contents of the editor
     */
    @OnThread(Tag.Any)
    void printTo(PrinterJob printerJob, boolean printLineNumbers, boolean printBackground);

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
    @OnThread(Tag.Swing)
    TextEditor assumeText();
    
    /**
     * Obtain the FrameEditor implementation of this editor, if it has one. May return null if no
     * FrameEditor implementation is available.
     */
    @OnThread(Tag.FX)
    FrameEditor assumeFrame();
    
    /**
     * Create a new method, or appending the contents if the method already exists
     *   
     * @param e extensions editor
     * @param method element
     * @param after will be passed true if the method existed already, false otherwise (will always be run)
     */
    void insertAppendMethod(bluej.extensions.editor.Editor e, NormalMethodElement method, Consumer<Boolean> after);

    /**
     * Insert a method call in constructor, if it does not already exists
     *   
     * @param e extensions editor
     * @param className string
     * @param methodCall element 
     * @param after will be passed true if the call existed already
     */
    void insertMethodCallInConstructor(bluej.extensions.editor.Editor e, String className, CallElement methodCall, Consumer<Boolean> after);

    void cancelFreshState();
    
    void focusMethod(String methodName);

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
}
