/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013  Michael Kolling and John Rosenberg 
 
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

import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import bluej.compiler.Diagnostic;
import bluej.parser.SourceLocation;
import bluej.parser.nodes.ParsedCUNode;


/**
 * Interface between an editor and the rest of BlueJ
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public interface Editor
{
    /**
     * Read a file into the editor buffer and show the editor. If the editor
     * already contains text, it is cleared first. If the file cannot be read,
     * the editor should not be displayed.
     * 
     * @param filename    the file to be read
     * @param compiled    true if this is a compiled class
     * 
     * @return false is there was a problem, true otherwise
     */
    boolean showFile(String filename, Charset charset, boolean compiled, String docFilename, Rectangle bounds);

    /**
     * Reload and display the same file that was displayed before.
     * This should generated a modificationEvent followed by a saveEvent.
     */
    void reloadFile();

    /**
     * Clear the current buffer. The editor is not redisplayed after a call to
     * this function. It is typically used in a sequence "clear; [insertText];
     * show".
     */
    void clear();

    /**
     * Insert a string into the buffer. The editor is not immediately
     * redisplayed. This function is typically used in a sequence "clear;
     * [insertText]; show".
     * 
     * @param text        the text to be inserted
     * @param caretBack    move the caret to the beginning of the inserted text
     */
    void insertText(String text, boolean caretBack);

    /**
     * Set the selection of the editor to be a len characters on the line
     * lineNumber, starting with column columnNumber
     * 
     * @param lineNumber the line to select characters on
     * @param column the column to start selection at (1st column is 1 - not 0)
     * @param len the number of characters to select
     */
    void setSelection(int lineNumber, int column, int len);

    /**
     * Request to the editor to mark the text between begin and end as selected.
     *
     * @param  begin                      where to start the selection
     * @param  end                        where to end the selection
     * @throws  IllegalArgumentException  if either of the specified TextLocations
     * represent a position which does not exist in the text.
     */
    public void setSelection(SourceLocation begin, SourceLocation end);
    
    /**
     * Set the selection of the editor to be a len characters on the line
     * lineNumber, starting with column columnNumber
     * 
     * @param lineNumber the line to select characters on
     * @param column the column to start selection at (1st column is 1 - not 0)
     * @param len the number of characters to select
     */
    void setSelection(int firstlineNumber, int firstColumn,
                      int secondLineNumber, int SecondColumn);

    /**
     * Show the editor window. This includes whatever is necessary of the
     * following: make visible, de-iconify, bring to front of window stack.
     * 
     * @param vis  true to make the editor visible, or false to hide it.
     */
    void setVisible(boolean vis);

    /**
     * True is the editor is on screen.
     * 
     * @return true if editor is on screen
     */
    boolean isShowing();

    /**
     * Save the buffer to disk under the current file name. This is an error if
     * the editor has not been given a file name (ie. if readFile was not
     * executed).
     * 
     * If save() is called on an unmodified file, it returns immediately without
     * re-writing the file to disk.
     */
    void save() throws IOException;

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
     */
    void displayDiagnostic(Diagnostic diagnostic);
    
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
     * @param docFileName  new documentation file name
     */
    void changeName(String title, String filename, String docFileName);

    /**
     * Set the "compiled" status
     * 
     * @param compiled    true if the class has been compiled
     */
    void setCompiled(boolean compiled);
    
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
    void print(PrinterJob printerJob);

    /**
     * Set the 'read-only' property of this editor.
     * @param readOnlyStatus  If true, editor is non-editable.
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
     * Gets the bounds for this editor window.
     * This method is used to store the bounds between sessions.
     * 
     * @return The bounds
     */
    Rectangle getBounds();
    
    /**
     * Get the source document that this editor is currently editing. Certain
     * operations (such as reload) might change the document; that is, the
     * returned document may become invalid at some later point in time.
     * 
     * @return  the document being edited.
     */
    Document getSourceDocument(); 

    /**
     * Returns the current caret location within the edited text.
     *
     * @return    the LineColumn object.
     */
    public SourceLocation getCaretLocation();
    
    /**
     * Sets the current Caret location within the edited text.
     *
     * @param  location                   The location in the text to set the Caret to.
     * @throws  IllegalArgumentException  if the specified TextLocation represents a position which does not exist in the text.
     */
    public void setCaretLocation(SourceLocation location);


    /**
     * Returns the location at which current selection begins.
     *
     * @return    the current beginning of the selection or null if no text is selected.
     */
    public SourceLocation getSelectionBegin();
    
    /**
     * Returns the location where the current selection ends.
     *
     * @return    the current end of the selection or null if no text is selected.
     */
    public SourceLocation getSelectionEnd();

    /**
     * Returns the text which lies between the two LineColumn.
     *
     * @param  begin                      The beginning of the text to get
     * @param  end                        The end of the text to get
     * @return                            The text value
     * @throws  IllegalArgumentException  if either of the specified SourceLocations represent a position which does not exist in the text.
     */
    public String getText( SourceLocation begin, SourceLocation end );    

    /**
     * Request to the editor to replace the text between beginning and end with the given newText
     * If begin and end points to the same location, the text is inserted.
     *
     * @param  begin                      where to start to replace
     * @param  end                        where to end to replace
     * @param  newText                    The new text value
     * @throws  IllegalArgumentException  if either of the specified LineColumn
     * represent a position which does not exist in the text.
     * @throws  BadLocationException  if internally the text points outside a location in the text.
     */
    public void setText(SourceLocation begin, SourceLocation end, String newText)
        throws BadLocationException;
        
    /**
     * Returns the LineColumn object from the given offset in the text.
     *
     * @return    the LineColumn object or null if the offset points outside the text.
     */
    public SourceLocation getLineColumnFromOffset(int offset);
    
    /**
     * Translates a LineColumn into an offset into the text held by the editor.
     *
     * @param  location  position to be translated
     * @return           the offset into the content of this editor
     * @throws  IllegalArgumentException  if the specified LineColumn
     * represent a position which does not exist in the text.
     */
    public int getOffsetFromLineColumn(SourceLocation location);
    
    /**
     * Returns a property of the current editor.
     *
     * @param  propertyKey  The propertyKey of the property to retrieve.
     * @return              the property value or null if it is not found
     */
    public Object getProperty(String propertyKey);

    /**
     * Set a property for the current editor. Any existing property with
     * this key will be overwritten.
     *
     * @param  propertyKey  The property key of the new property
     * @param  value        The new property value
     */
    public void setProperty(String propertyKey, Object value);
    
    /**
     * Returns the length of the line indicated in the edited text.
     *
     * @param  line  the line in the text for which the length should be calculated, starting from 0
     * @return       the length of the line, -1 if line is invalid
     */
    public int getLineLength(int line);
    
    /**
     * Return the number of lines in the document.
     */
    public int numberOfLines();
    
    /**
     * Returns the length of the data.  This is the number of
     * characters of content that represents the users data.
     *
     * It is possible to obtain the line and column of the last character of text by using
     * the getLineColumnFromOffset() method.
     *
     * @return the length >= 0
     */
    public int getTextLength ();    
    
    /**
     * Get a node representing the the parsed structure of the source
     * document as a tree.
     * 
     * @return A ParsedNode instance, or null if not supported.
     */
    public ParsedCUNode getParsedNode();
    
}
