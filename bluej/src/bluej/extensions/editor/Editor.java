/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions.editor;

import java.io.IOException;

import javax.swing.text.BadLocationException;

import bluej.parser.SourceLocation;

/**
 * Proxy object that allows interaction with the BlueJ Editor for a
 * particular class.
 * Except as marked, methods of this class must be called from a swing compatible thread.
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 * @version    $Id: Editor.java 7337 2010-04-14 14:52:24Z nccb $
 */

public class Editor
{
    private bluej.editor.Editor bjEditor;


    /**
     * Constructor must not be public.
     * You get an Editor object by calling BClass.getEditor(), which
     * will create a (non-visible) editor if one does not already exist.
     *
     * @param  bjEditor  Description of the Parameter
     */
    Editor(bluej.editor.Editor bjEditor)
    {
        this.bjEditor = bjEditor;
    }
    
    bluej.editor.Editor getEditor()
    {
        return bjEditor;
    }


    /**
     * Request the editor to save the file currently opened.
     */
    public void saveFile()
    {
        try {
            bjEditor.save();
        }
        catch (IOException ioe) {}
    }

    /**
     * Request the editor to load the file currently opened.
     */
    public void loadFile()
    {
        bjEditor.reloadFile();
    }


    /**
     * Show or hide this Editor.
     *
     * @param  visible  If true, make this editor visible
     */
    public void setVisible(boolean visible)
    {
        bjEditor.setVisible(visible);
    }


    /**
     * Is this Editor currently visible?
     *
     * @return    true if the Editor is visible, false otherwise.
     */
    public boolean isVisible()
    {
        return bjEditor.isShowing();
    }


    /**
     * Returns the current caret location (the position of the user's cursor) within the edited text.
     *
     * @return    the textLocation.
     */
    public TextLocation getCaretLocation()
    {
        return convertLocation(bjEditor.getCaretLocation());
    }


    /**
     * Sets the current caret location within the edited text.
     *
     * @param  location                   The location in the text to set the Caret to.
     * @throws  IllegalArgumentException  if the specified TextLocation represents a position which does not exist in the text.
     */
    public void setCaretLocation(TextLocation location)
    {
        bjEditor.setCaretLocation(convertLocation(location));
    }


    /**
     * Request the editor to display the given message in the editor message area.
     * The message will be cleared using BlueJ's usual rules.
     *
     * @param  message  The message to display.
     */
    public void showMessage(String message)
    {
        bjEditor.writeMessage(message);
    }


    /**
     * Returns the location at which current selection begins.
     *
     * @return    the current beginning of the selection or null if no text is selected.
     */
    public TextLocation getSelectionBegin()
    {
        return convertLocation(bjEditor.getSelectionBegin());
    }


    /**
     * Returns the location at which the current selection ends.
     *
     * @return    the current end of the selection or null if no text is selected
     */
    public TextLocation getSelectionEnd()
    {
        return convertLocation(bjEditor.getSelectionEnd());
    }


    /**
     * Returns the text which lies between the two TextLocations.
     *
     * @param  begin                      The beginning of the text to get
     * @param  end                        The end of the text to get
     * @return                            The text value
     * @throws  IllegalArgumentException  if either of the specified TextLocations represent a position which does not exist in the text.
     */
    public String getText(TextLocation begin, TextLocation end)
    {
        return bjEditor.getText(convertLocation(begin), convertLocation(end));
    }


    /**
     * Request the editor to replace the text between beginning and end with the given newText
     * If begin and end refer to the same location, the text is inserted.
     *
     * @param  begin                      where to start to replace
     * @param  end                        where to end to replace
     * @param  newText                    The new text value
     * @throws  IllegalArgumentException  if either of the specified TextLocations
     * represent a position which does not exist in the text.
     */
    public void setText(TextLocation begin, TextLocation end, String newText)
    {
        try {
            bjEditor.setText(convertLocation(begin), convertLocation(end), newText);
        }
        catch (BadLocationException exc) {
            throw new IllegalArgumentException(exc.getMessage());
        }
    }


    /**
     * Request the editor to mark the text between begin and end as selected.
     *
     * @param  begin                      where to start the selection
     * @param  end                        where to end the selection
     * @throws  IllegalArgumentException  if either of the specified TextLocations
     * represent a position which does not exist in the text.
     */
    public void setSelection(TextLocation begin, TextLocation end)
    {
        bjEditor.setSelection(convertLocation(begin), convertLocation(end));
    }


    /**
     * Request the editor to permit or deny editor content modification (via the editor GUI).
     * Extensions should set readOnly to true before changing the editor content programmatically.
     *
     * @param  readOnly  If true user cannot change the editor content using the GUI, false allows user interaction.
     */
    public void setReadOnly(boolean readOnly)
    {
        bjEditor.setReadOnly(readOnly);
    }


    /**
     * Is the editor currently set to readOnly?.
     *
     * @return    true if the user cannot change the text using the GUI, false othervise
     */
    public boolean isReadOnly()
    {
        return bjEditor.isReadOnly();
    }


    /**
     * Returns a property of the current editor.
     * This allows custom versions of the editor to communicate with extensions.
     *
     * @param  propertyKey  The propertyKey of the property to retrieve.
     * @return              the property value or null if it is not found
     */
    public Object getProperty(String propertyKey)
    {
        return bjEditor.getProperty(propertyKey);
    }


    /**
     * Set a property for the current editor. Any existing property with
     * this key will be overwritten.
     *
     * @param  propertyKey  The property key of the new property
     * @param  value        The new property value
     */
    public void setProperty(String propertyKey, Object value)
    {
        bjEditor.setProperty(propertyKey,value);
    }


    /**
     * Translates a text location into an offset into the text held by the editor.
     *
     * @param  location                   position to be translated
     * @return                            the offset into the text of this location
     * @throws  IllegalArgumentException  if the specified TextLocation
     * represent a position which does not exist in the text.
     */
    public int getOffsetFromTextLocation(TextLocation location)
    {
        return bjEditor.getOffsetFromLineColumn(convertLocation(location));
    }


    /**
     * Translate an offset in the text held by the editor into a TextLocation.
     *
     * @param  offset  location to be translated
     * @return         the TextLocation in the text of this offset or null if the offset is invalid
     */
    public TextLocation getTextLocationFromOffset(int offset)
    {
        return convertLocation(bjEditor.getLineColumnFromOffset(offset));
    }


    /**
     * Returns the length of the line indicated in the edited text.
     *
     * @param  line  the line in the text for which the length should be calculated, starting from zero.
     * @return       the length of the line, -1 if line is invalid
     */
    public int getLineLength(int line)
    {
        return bjEditor.getLineLength(line);
    }


    /**
     * Returns the total number of lines in the currently edited text.
     *
     * @return    The number of lines in the text >= 0
     */
    public int getLineCount()
    {
        return bjEditor.numberOfLines();
    }


    /**
     * Returns the length of the currently edited text.  This is the number of
     * characters of content that represents the user's data.
     *
     * The line number and column of the last character of text can be obtained by using
     * the getLineColumnFromOffset(getTextLength()) method.
     *
     * @return the length >= 0
     */
    public int getTextLength ()
    {
        return bjEditor.getTextLength();
    }

    /**
     * Utility to convert a TextLocation into a LineColumn.
     * If null is given as parameter then null is returned.
     *
     * @param  location  The point in the editor to convert to a LineColumn.
     * @return           The LineColumn object describing a point in the editor.
     */
    private SourceLocation convertLocation(TextLocation location)
    {
        if (location == null) {
            return null;
        }

        return new SourceLocation(location.getLine() + 1, location.getColumn() + 1);
    }


    /**
     * Utility to convert a LineColumn into a TextLocation.
     * If null is given as parameter then null is returned.
     * 
     * @param  location  The point in the editor to convert to a TextLocation.
     * @return           The TextLocation object describing a point in the editor.
     */
    private TextLocation convertLocation(SourceLocation location)
    {
        if (location == null) {
            return null;
        }

        return new TextLocation(location.getLine() - 1, location.getColumn() - 1);
    }

}

