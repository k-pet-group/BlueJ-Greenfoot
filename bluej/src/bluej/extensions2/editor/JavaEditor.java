/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2015,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.editor;

import bluej.parser.SourceLocation;

import java.io.IOException;

/**
 * Proxy object that allows interaction with the BlueJ Editor for a
 * particular <b>Java</b> class.
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 */

public class JavaEditor
{
    private bluej.editor.TextEditor bjEditor;


    /**
     * Constructor must not be public.
     * An extension can get an Editor object by calling {@link bluej.extensions2.BClass#getJavaEditor()}, which
     * will create a (non-visible) editor if one does not already exist.
     *
     * @param  bjEditor  a {@link bluej.editor.TextEditor} object referencing a BlueJ editor for this JavaEditor.
     */
    JavaEditor(bluej.editor.TextEditor bjEditor)
    {
        this.bjEditor = bjEditor;
    }

    /**
     * @return The {@link bluej.editor.TextEditor} object referencing a BlueJ editor for this JavaEditor.
     */
    bluej.editor.Editor getEditor()
    {
        return bjEditor;
    }


    /**
     * Requests the editor to save the file currently opened.
     */
    public void saveFile()
    {
        try {
            bjEditor.save();
        }
        catch (IOException ioe) {}
    }

    /**
     * Requests the editor to load the file currently opened.
     */
    public void loadFile()
    {
        bjEditor.reloadFile();
    }

    /**
     * Shows or hides this editor.
     *
     * @param  visible  a boolean value indicating whether to show (<code>true</code>) the editor or hide it (<code>false</code>).
     */
    public void setVisible(boolean visible)
    {
        bjEditor.setEditorVisible(visible, false);
    }


    /**
     * Returns the visibility status of this editor.
     *
     * @return  A boolean value indicating whether this editor is visible (<code>true</code>) or not (<code>false</code>).
     */
    public boolean isVisible()
    {
        return bjEditor.isOpen();
    }


    /**
     * Returns the current caret location (the position of the user's cursor) within the edited text.
     *
     * @return   A {@link TextLocation} object representing the current caret location of this editor.
     */
    public TextLocation getCaretLocation()
    {
        return convertLocation(bjEditor.getCaretLocation());
    }


    /**
     * Sets the current caret location within the edited text.
     *
     * @param  location                   a {@link TextLocation} object representing the location in the text to set the caret to.
     * @throws  IllegalArgumentException  if the specified TextLocation represents a position which does not exist in the text.
     */
    public void setCaretLocation(TextLocation location)
    {
        bjEditor.setCaretLocation(convertLocation(location));
    }

    /**
     * Requests the editor to display the given message in the editor message area.
     * The message will be cleared using BlueJ's usual rules.
     *
     * @param  message  the message to display.
     */
    public void showMessage(String message)
    {
        bjEditor.writeMessage(message);
    }

    /**
     * Returns the location at which current selection begins.
     *
     * @return   A {@link TextLocation} object representing the current beginning location of the selection in this editor, <code>null</code> if no text is selected.
     */
    public TextLocation getSelectionBegin()
    {
        return convertLocation(bjEditor.getSelectionBegin());
    }


    /**
     * Returns the location at which the current selection ends.
     *
     * @return   A {@link TextLocation} object representing the current end location of the selection in this editor, <code>null</code> if no text is selected.
     */
    public TextLocation getSelectionEnd()
    {
        return convertLocation(bjEditor.getSelectionEnd());
    }


    /**
     * Returns the text which lies between the two TextLocations.
     *
     * @param  begin                      a {@link TextLocation} object representing the beginning location of the text to get.
     * @param  end                        a {@link TextLocation} object representing the end location of the text to get.
     * @return                            The text value
     * @throws  IllegalArgumentException  if either of the specified TextLocations represent a position which does not exist in the text.
     */
    public String getText(TextLocation begin, TextLocation end)
    {
        return bjEditor.getText(convertLocation(begin), convertLocation(end));
    }


    /**
     * Requests the editor to replace the text between beginning and end with the given newText
     * If begin and end refer to the same location, the text is inserted.
     *
     * @param  begin                      a {@link TextLocation} object representing the beginning location of the text to replace.
     * @param  end                        a {@link TextLocation} object representing the end location of the text to replace.
     * @param  newText                    a {@link String} object containing the new text.
     * @throws  IllegalArgumentException  if either of the specified TextLocations
     * represent a position which does not exist in the text.
     */
    public void setText(TextLocation begin, TextLocation end, String newText)
    {
        bjEditor.setText(convertLocation(begin), convertLocation(end), newText);
    }


    /**
     * Requests the editor to mark the text between begin and end as selected.
     *
     * @param  begin                      a {@link TextLocation} object representing the beginning location of the selection to make.
     * @param  end                        a {@link TextLocation} object representing the end location of the selection to make.
     * @throws  IllegalArgumentException  if either of the specified TextLocations
     * represent a position which does not exist in the text.
     */
    public void setSelection(TextLocation begin, TextLocation end)
    {
        bjEditor.setSelection(convertLocation(begin), convertLocation(end));
    }


    /**
     * Requests the editor to permit or deny editor content modification (via the editor GUI).
     * Extensions should set it to <code>true</code>true before changing the editor content programmatically.
     *
     * @param  readOnly  a boolean indicating whether the user cannot change the editor content using the GUI (<code>true</code>), (<code>false</code>) otherwise.
     */
    public void setReadOnly(boolean readOnly)
    {
        bjEditor.setReadOnly(readOnly);
    }


    /**
     * Returns the readonly status of this.
     *
     * @return    A boolean indicating whether the user cannot change the editor content using the GUI (<code>true</code>), (<code>false</code>) otherwise.
     */
    public boolean isReadOnly()
    {
        return bjEditor.isReadOnly();
    }


    /**
     * Returns a property of the current editor.
     * This allows custom versions of the editor to communicate with extensions.
     *
     * @param  propertyKey  the property key of the property to retrieve.
     * @return              An {@link Object} object representing the property value or <code>null</code> if it is not found.
     */
    public Object getProperty(String propertyKey)
    {
        return bjEditor.getProperty(propertyKey);
    }


    /**
     * Sets a property for the current editor. Any existing property with
     * this key will be overwritten.
     *
     * @param  propertyKey  the property key of the property to set.
     * @param  value       An {@link Object} object to assign to the new property value.
     */
    public void setProperty(String propertyKey, Object value)
    {
        bjEditor.setProperty(propertyKey,value);
    }


    /**
     * Translates a text location into an offset into the text held by this editor.
     *
     * @param  location                   a {@link TextLocation} object representing the location where to apply the translation on.
     * @return                            an integer representing the offset into the text of this location
     * @throws  IllegalArgumentException  if the specified TextLocation
     * represent a position which does not exist in the text.
     */
    public int getOffsetFromTextLocation(TextLocation location)
    {
        return bjEditor.getOffsetFromLineColumn(convertLocation(location));
    }


    /**
     * Translates an offset in the text held by this editor into a TextLocation.
     *
     * @param  offset  an integer representing the offset to translate.
     * @return         A {@link TextLocation} object representing the text of this offset, <code>null</code> if the offset is invalid
     */
    public TextLocation getTextLocationFromOffset(int offset)
    {
        return convertLocation(bjEditor.getLineColumnFromOffset(offset));
    }


    /**
     * Returns the length of the line indicated in the edited text.
     *
     * @param  line  an integer representing the line number in the text for which the length should be calculated, starting from zero.
     * @return       An integer representing the length of the line, <code>-1</code> if line is invalid.
     */
    public int getLineLength(int line)
    {
        return bjEditor.getLineLength(line);
    }


    /**
     * Returns the total number of lines in the currently edited text.
     *
     * @return    An integer representing the number of lines in the text.
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
     * the {@link bluej.editor.TextEditor#getLineColumnFromOffset(int)} method with {@link #getTextLength()} as argument.
     *
     * @return  An integer representing the length of the text.
     */
    public int getTextLength ()
    {
        return bjEditor.getTextLength();
    }

    /**
     * Utility to convert a {@link TextLocation} into a {@link SourceLocation}.
     * If <code>null</code> is given as parameter then <code>null</code> is returned.
     *
     * @param  location  a {@link TextLocation} object representing the location in this editor to convert to a {@link SourceLocation}.
     * @return           A {@link SourceLocation}  object describing a point in this editor.
     */
    private SourceLocation convertLocation(TextLocation location)
    {
        if (location == null) {
            return null;
        }

        return new SourceLocation(location.getLine() + 1, location.getColumn() + 1);
    }


    /**
     * Utility to convert a {@link SourceLocation} into a {@link TextLocation}.
     * If <code>null</code> is given as parameter then <code>null</code> is returned.
     *
     * @param  location  a {@link SourceLocation} object representing the location in this editor to convert to a {@link TextLocation}.
     * @return           A {@link TextLocation}  object describing a point in this editor.
     */
    private TextLocation convertLocation(SourceLocation location)
    {
        if (location == null) {
            return null;
        }

        return new TextLocation(location.getLine() - 1, location.getColumn() - 1);
    }
}
