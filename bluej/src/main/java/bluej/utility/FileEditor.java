/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.io.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;

import bluej.parser.symtab.Selection;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An object which allows (semi) direct editing of files on
 * disk.
 *
 * @author  Andrew Patterson
 * @version $Id: FileEditor.java 6215 2009-03-30 13:28:25Z polle $
 */
@OnThread(Tag.FXPlatform) // We should just remove this whole class
public class FileEditor extends PlainDocument
{
    private File fileToEdit;

	/**
	 * Construct a FileEditor object which allows "editor" style
	 * replacements to be made to the file, and then allows the changes
	 * to be committed back to disk.
	 *
	 * @param fileToEdit    the file to edit
	 */
    public FileEditor(File fileToEdit) throws IOException
    {
        this.fileToEdit = fileToEdit;

        Reader in = null;
        Writer out = null;

        in = new BufferedReader(new FileReader(fileToEdit));
        out = new StringWriter();

        for(int c; (c = in.read()) != -1; )
            out.write(c);

        try {
            insertString(0, out.toString(), null);
        }
        catch(BadLocationException ble)
        {
            ble.printStackTrace();
        }
        finally {
            if(in != null)
                in.close();
        }
    }

	/**
	 * Replace the specified selection region with
	 * new text.
	 *
	 * @param s     the Selection to replace
	 * @param text  the text to insert
	 */
    public void replaceSelection(Selection s, String text)
    {
        try {
            int lineNo = s.getLine() - 1;
            int endLineNo = s.getEndLine() - 1;

            Element root = getDefaultRootElement();

            if (endLineNo < root.getElementCount()) {
                Element line = root.getElement(lineNo);
                Element endLine = root.getElement(endLineNo);

                int pos = line.getStartOffset() + s.getColumn() - 1;
                int len = endLine.getStartOffset() + s.getEndColumn() - pos - 1;
                
                remove(pos, len);

                insertString(line.getStartOffset() + s.getColumn() - 1,
                                text, null);
             }
        }
        catch(BadLocationException ble)
        {
            ble.printStackTrace();
        }
    }

    /**
     * Save the changes made to this file back to disk.
     */
    public void save() throws IOException
    {
        try {
            Writer out = new BufferedWriter(new FileWriter(fileToEdit));

            for(int c=0; c<getLength(); c++)
            {
                out.write(getText(c,1));
            }

            out.close();
        }
        catch(BadLocationException ble)
        {
            ble.printStackTrace();
        }
    }
}
