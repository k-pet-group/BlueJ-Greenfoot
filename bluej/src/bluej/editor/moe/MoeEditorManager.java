// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor.moe;

import bluej.Config;
import bluej.utility.Debug;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;

import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;

import java.awt.*;		// Font
import java.io.*;		// Object input, ouput streams

/**
** @author Michael Kolling
**
**/

public final class MoeEditorManager
    extends bluej.editor.EditorManager
{
    // public static variables

    protected static MoeEditorManager editorManager;   // the manager object itself

    // private variables

    private Properties resources;
    private Vector editors;			// open editors
    private Finder finder;			// the finder object
    private Replacer replacer;		// the replacer object
    // user preferences

    private boolean showLineNum;
    private boolean showToolBar;

    // =========================== PUBLIC METHODS ===========================

    public MoeEditorManager()
    {
        editors = new Vector(4,4);
        finder = new Finder();
        replacer = new Replacer();
        
        showToolBar = true;
        showLineNum = false;

        resources = Config.moe_props;

        editorManager = this;	// make this object publicly available
    }


    // ------------------------------------------------------------------------
    /**
    ** Open an editor to display a class. The filename may be "null"
    ** to open an empty editor (e.g. for displaying a view). The editor
    ** is initially hidden. A call to "Editor::show" is needed to make
    ** is visible after opening it.
    **
    ** @param filename	name of the source file to open (may be null)
    ** @param windowTitle	title of window (usually class name)
    ** @param watcher	an object interested in editing events
    ** @param compiled	true, if the class has been compiled
    ** @param breakpoints	vector of Integers: line numbers where bpts are
    ** @returns		the new editor, or null if there was a problem
    **/

    public Editor openClass(String filename, String windowTitle,
                            EditorWatcher watcher, boolean compiled,
                            Vector breakpoints)	// inherited from EditorManager
    {
        return openEditor (filename, true, windowTitle, watcher, compiled,
                           breakpoints);
    }

    // ------------------------------------------------------------------------
    /**
    ** Open an editor to display a text document. The difference to
    ** "openClass" is that code specific functions (such as compile,
    ** debug, view) are disabled in the editor. The filename may be
    ** "null" to open an empty editor. The editor is initially hidden.
    ** A call to "Editor::show" is needed to make is visible after
    ** opening it.
    **
    ** @param filename	name of the source file to open (may be null)
    ** @param windowTitle	title of window (usually class name)
    ** @param watcher	an object interested in editing events
    ** @returns		the new editor, or null if there was a problem
    **/

    public Editor openText(String filename, String windowTitle,
                           EditorWatcher watcher)	// inherited from EditorManager
    {
        return openEditor (filename, false, windowTitle, watcher, false, null);
    }

    public void refreshAll()
    {
        Iterator e = editors.iterator();

        while(e.hasNext())
            {
                Editor ed = (Editor)e.next();

                if(ed.isShowing())
                    ed.refresh();
            }
    }

    // ------------------------------------------------------------------------
    /**
    ** The 'open' item from the menu was chosen.  Show a file selection dialog
    ** to let the user choose a file to open. If a file name was given try to
    ** open that file. The file is open if it exists else an exception is
    ** thrown in a dialog.
    **
    ** Only used in stand-alone version.
    **/

    //   public void openRequest(RedEditor editor)
    //   {
    //     // Create a file dialog to query the user for a filename
    //     FileDialog fd = new FileDialog(editor.getFrame(), "Load File",
    // 				   FileDialog.LOAD);
    //
    //     fd.show();                      	// Display the dialog and block
    //     String filename = fd.getFile();  	// Get users response
    //
    //     if(filename != null)          	// if user didn't click "Cancel"
    //     {
    // 	// See if the requested file is readable
    // 	File file = new File(filename);
    // 	if(file.canRead())
    // 	    openText(filename, null, null);
    // 	else
    // 	{
    // 	    // Print out error
    // 	    String comment = new String("ERROR: File Not Found.");
    // 	    new QuestionDialog(editor.getFrame(), "Error", comment, "OK");
    // 	}
    //     }
    //   }

    // ------------------------------------------------------------------------
    /**
    ** An editor has issued a "save_as" (or a save with no
    ** filename specified).  It now requests of Red to initiate a "save-as"
    ** dialog.  The file selection dialog is shown. If the
    ** OK button was clicked save the buffer under that new filename.
    **
    ** Only used in stand-alone version.
    **/

    //   public void saveAsRequest(RedEditor editor)
    //   {
    //     // Create a file dialog to query the user for a filename
    //     FileDialog fd = new FileDialog(editor.getFrame(), "Save File As",
    // 				   FileDialog.SAVE);
    //     fd.show();                      	// Display the dialog and block
    //     String filename = fd.getFile();     	// Get users response
    //     if(filename != null)		// if user didn't click "Cancel"
    // 	editor.do_save_as(filename);
    //   }

    // ------------------------------------------------------------------------
    /**
    ** Show preference dialog so user can change preferences
    **/
    //   public void show_pref_dialog(RedEditor editor)
    //   {
    // 	   pref_dialog = new PrefDialog(editor);
    //   }

    // ------------------------------------------------------------------------
    /**
    ** Sound a beep if the "beep with warning" option is true
    **/
    public void beep()
    {
        if(true) // if beepWarning option is on...
            Toolkit.getDefaultToolkit().beep();
    }

    public void discardEditor(Editor ed)
    {
        ed.close();
        editors.removeElement(ed);
    }

    // ========================== PACKAGE METHODS ===========================

    // ------------------------------------------------------------------------
    /**
    ** Return the shared finder object
    **/

    Finder getFinder()
    {
        return finder;
    }

       // ------------------------------------------------------------------------
    /**
    ** Return the shared replacer object
    **/

    Replacer getReplacer()
    {
        return replacer;
    }
    
    // ========================== PRIVATE METHODS ===========================

    // ------------------------------------------------------------------------
    /**
    ** Open an editor to display a class. The filename may be "null"
    ** to open an empty editor (e.g. for displaying a view). The editor
    ** is initially hidden. A call to "Editor::show" is needed to make
    ** is visible after opening it.
    **
    ** @param filename	name of the source file to open (may be null)
    ** @param windowTitle	title of window (usually class name)
    ** @param watcher	an object interested in editing events
    ** @param compiled	true, if the class has been compiled
    ** @param breakpoints	vector of Integers: line numbers where bpts are
    ** @returns		the new editor, or null if there was a problem
    **/

    private Editor openEditor(String filename, boolean isCode,
                              String windowTitle, EditorWatcher watcher,
                              boolean compiled, Vector breakpoints)
    {
        MoeEditor editor;

        editor = new MoeEditor(windowTitle, isCode, watcher, showToolBar,
                               showLineNum, resources);
        editors.addElement(editor);
        if (watcher!=null && filename==null)	// editor for class interface
            return editor;
        if (editor.showFile(filename, compiled, null))
            return editor;
        else {
            editor.doClose();			// editor will remove itself
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    ** Read the preferences file.
    **/
    public void read_prefs ()
    {
        //     String filename = red_prefs_file;
        //     String version = RedVersion.versionString();
        //     boolean done = false;
        //
        //     try {
        // 	FileInputStream fis = new FileInputStream(filename);
        // 	ObjectInputStream file = new ObjectInputStream(fis);
        //
        // 	version = (String)file.readObject();
        // 	if(!version.equals(RedVersion.versionString()))
        // 	{
        // 	    //// messages.show_error (getFrame(), Messages.ErrReadPrefs);
        // 	    System.err.println("ERROR: Could not read preferences file");
        // 	    done = false;
        // 	    file.close ();
        // 	}
        //
        // 	show_toolbar = file.readBoolean();
        // 	show_line_num = file.readBoolean();
        // 	beep_warning = file.readBoolean();
        // 	make_backup = file.readBoolean();
        // 	append_newline = file.readBoolean();
        // 	convert_dos = file.readBoolean();
        // 	quote_string = (String)file.readObject();
        // 	comment_start_string = (String)file.readObject();
        // 	comment_end_string = (String)file.readObject();
        //
        // 	file.close ();
        // 	done = true;
        //     } catch(Exception e) {
        // 	done = false; 			// pref file does not exist
        //     }
        //
        //     if (!done)		//  -> use defaults
        //     {
        // 	show_toolbar = true;
        // 	show_line_num = false;
        // 	beep_warning = true;
        // 	make_backup = true;
        // 	append_newline = true;
        // 	convert_dos = true;
        // 	quote_string = ">";
        // 	comment_start_string = "//";
        // 	comment_end_string = "";
        //     }
        //
        //     if (!version.equals(RedVersion.versionString())) // pref version is old...
        // 	write_prefs ();
    }

    // ------------------------------------------------------------------------
    /**
    ** Write the preferences file.
    **/

    public void write_prefs ()
    {
        //     String filename = red_prefs_file;
        //
        //     try {
        // 	FileOutputStream fos = new FileOutputStream(filename);
        // 	ObjectOutputStream file = new ObjectOutputStream(fos);
        // 	file.writeObject(RedVersion.versionString());
        // 	file.writeBoolean(show_toolbar);
        // 	file.writeBoolean(show_line_num);
        // 	file.writeBoolean(beep_warning);
        // 	file.writeBoolean(make_backup);
        // 	file.writeBoolean(append_newline);
        // 	file.writeBoolean(convert_dos);
        // 	file.writeObject(quote_string);
        // 	file.writeObject(comment_start_string);
        // 	file.writeObject(comment_end_string);
        // 	file.close ();
        //     } catch(Exception e) {
        // 	String msg = "ERROR: Could not write preferences file";
        // 	// new QuestionDialog(getFrame(), "Error", msg, "OK");
        // 	System.err.println("ERROR: Could not write preferences file");
        //     }
    }


} // end class MoeEditorManager
