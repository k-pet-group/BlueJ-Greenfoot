package bluej.utility;

import bluej.parser.symtab.Selection;

import java.util.*;
import java.io.*;
import javax.swing.text.*;

/**
 * An object which allows (semi) direct editing of files on
 * disk.
 *
 * @author  Andrew Patterson
 * @version $Id: FileEditor.java 417 2000-04-04 02:57:53Z bquig $
 */
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
            getContent().insertString(0, out.toString());
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
            Element line = getParagraphElement(s.getLine() - 1);

            remove(line.getStartOffset() + s.getColumn() - 1,
                    s.getLength());

            insertString(line.getStartOffset() + s.getColumn() - 1,
                            text, new SimpleAttributeSet());
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
