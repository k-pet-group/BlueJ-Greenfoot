/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.parser.SourceLocation;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ReparseableDocument;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.nio.charset.Charset;

@OnThread(Tag.FXPlatform)
public interface TextEditor extends Editor
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
    boolean showFile(String filename, Charset charset, boolean compiled, String docFilename);
    
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
     * Set the selection of the editor to go from first position to second position
     * 
     * @param start The start of the selection (where the anchor will be positioned)
     * @param end The end of the selection (where the caret will be positioned)
     */
    void setSelection(SourceLocation start, SourceLocation end);
    
    /**
     * Get the source document that this editor is currently editing. Certain
     * operations (such as reload) might change the document; that is, the
     * returned document may become invalid at some later point in time.
     * 
     * @return  the document being edited.
     */
    ReparseableDocument getSourceDocument();

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
    public void setText(SourceLocation begin, SourceLocation end, String newText);
        
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
