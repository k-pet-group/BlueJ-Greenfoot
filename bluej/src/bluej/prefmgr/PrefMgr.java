package bluej.prefmgr;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;

import bluej.Config;
import bluej.utility.Debug;

/**
 * A class to manage the user editable preferences
 * settings.
 *
 * Note that this is a singleton class. There can be only one
 * instance of PrefMgr at any time.
 *
 * @author  Andrew Patterson
 * @version $Id: PrefMgr.java 345 2000-01-12 03:52:49Z ajp $
 */
public class PrefMgr
{
    private static int fontsize;
    private static int editFontsize;
    private static int printFontsize;
    private static int printTitleFontsize;
    private static int printInfoFontsize;

    private static Font normalFont = new Font("SansSerif", Font.BOLD, 12);

    // initialised by a call to setMenuFontSize()
    private static int menuFontSize;
    static final Font menuFont = new Font("SansSerif", Font.PLAIN, 12);
    static final Font italicMenuFont = new Font("SansSerif", Font.ITALIC, 12);

    // initialised by a call to setEditorFontSize()
    private static int editorFontSize;
    private static Font editorStandardFont, editorStandoutFont;

    private static PrefMgr prefmgr = new PrefMgr();

	/**
	 */
    private PrefMgr()
    {
        setEditorFontSize(Config.getPropInteger("bluej.fontsize.editor", 12));

/*        fontsize = Integer.parseInt(Config.getProperty("bluej.fontsize","12"));

        printFontsize = Integer.parseInt(Config.getProperty("bluej.fontsize.printText","10"));
        printTitleFontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize.printTitle","14"));
        printInfoFontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize.printInfo","10")); */

    }

    public static void initialise()
    {

    }

    public static Font getStandardFont()
    {
        return normalFont;
    }

    public static Font getStandoutFont()
    {
        return normalFont;
    }

    public static Font getStandardMenuFont()
    {
        return menuFont;
    }

    public static Font getStandoutMenuFont()
    {
        return italicMenuFont;

    }

    public static Font getStandardEditorFont()
    {
        return editorStandardFont;
    }

    protected static void setEditorFontSize(int size)
    {
        if (size > 0 && size != editorFontSize) {
            editorFontSize = size;

            // check if the font size we are setting it to is already the default
            // bluej value in which case we do not need to have it saved in the
            // users settings (thereby hopefully keeping the user preferences
            // file to a minimum size)

            if (Config.getDefaultPropInteger("bluej.fontsize.editor", -1) == size)
                Config.removeProperty("bluej.fontsize.editor");
            else
                Config.putPropInteger("bluej.fontsize.editor", size);

            editorStandardFont = new Font("Monospaced", Font.PLAIN, size);
            editorStandoutFont = new Font("Monospaced", Font.BOLD, size);
        }
    }

    protected static int getEditorFontSize()
    {
        return editorFontSize;
    }

}
