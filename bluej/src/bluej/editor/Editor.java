/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2015  Michael Kolling and John Rosenberg 
 
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
import java.util.function.Consumer;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.compiler.Diagnostic;
import bluej.editor.stride.FrameEditor;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.NormalMethodElement;


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
     * 
     * @param message    the message to be displayed
     * @param lineNumber    the line to move the cursor to (the line is also
     *        highlighted)
     * @param column        the column to move the cursor to
     * @param beep        if true, do a system beep
     * @param setStepMark    if true, set step mark (for single stepping)
     * @param help        name of help group (may be null); this should be the compiler
     *                    name such as "javac".
     */
    void displayMessage(String message, int lineNumber, int column, 
                        boolean beep, boolean setStepMark, String help);

    /**
     * Display a diagnostic message from the compiler.
     * 
     * @param diagnostic  The diagnostic to be displayed.
     * @param errorIndex The index of the error (first is 0, second is 1, etc)
     * @return Whether the error was shown to the user (true) or not (false).  Some editors
     *          only show the first error, for example, or the first N.
     */
    boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex);
    
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
     */
    void compileFinished(boolean successful);

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
     *  Tell whether the editor is currently displaying the interface or the
     *  source of the class.
     *  @return  True, if interface is currently shown, false otherwise.
     */
    boolean isShowingInterface();
 
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

    /**
     * Shows the next error in the editor, if there are any.  This will differ slightly by editor,
     * but broadly: if the cursor is outside an error, it should be taken to the next error (and details displayed)
     * or if it is already in an error, it should be taken to the next error, cycling if necessary. 
     */
    void showNextError();

    void cancelFreshState();

    void focusMethod(String methodName);
}
