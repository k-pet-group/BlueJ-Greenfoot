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
import bluej.graph.Graph;

/**
 * A class to manage the user editable preferences
 * settings.
 *
 * Note that this is a singleton class. There can be only one
 * instance of PrefMgr at any time.
 *
 * @author  Andrew Patterson
 * @version $Id: PrefMgr.java 555 2000-06-19 00:35:11Z mik $
 */
public class PrefMgr
{
    private static final String hilightingPropertyName = "bluej.syntaxHilighting";
    private static final String editorFontPropertyName = "bluej.editor.font";
    private static final String editorFontSizePropertyName = "bluej.editor.fontsize";
    private static final String notationStyle = "bluej.notation.style";

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

    // syntax hilighting
    private static boolean isSyntaxHilighting;
    private static boolean isUML;

    private static PrefMgr prefmgr = new PrefMgr();

	/**
	 */
    private PrefMgr()
    {
        setEditorFontSize(Config.getPropInteger(editorFontSizePropertyName, 12));

        isSyntaxHilighting = Boolean.valueOf(
            Config.getPropString(hilightingPropertyName, "true")).booleanValue();

        isUML = (Config.getDefaultPropString(notationStyle, Graph.UML).equals(Graph.UML));

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

    public static boolean useSyntaxHilighting()
    {
        return isSyntaxHilighting;
    }

    /**
     * The following methods are protected and should only be accessed by the
     * code which implements the various preferneces dialog panels
     */

    /**
     * Set the editor font size preference to a particular point size
     *
     * @param size  the size of the font
     */
    protected static void setEditorFontSize(int size)
    {
        if (size > 0 && size != editorFontSize) {
            editorFontSize = size;

            // check if the font size we are setting it to is already the default
            // bluej value in which case we do not need to have it saved in the
            // users settings (thereby hopefully keeping the user preferences
            // file to a minimum size)

            if (Config.getDefaultPropInteger(editorFontSizePropertyName, -1) == size)
                Config.removeProperty(editorFontSizePropertyName);
            else
                Config.putPropInteger(editorFontSizePropertyName, size);

            String fontName = Config.getPropString(editorFontPropertyName, 
                                                   "Monospaced");
            int style;
            if(fontName.endsWith("-bold")) {
                style = Font.BOLD;
                fontName = fontName.substring(0, fontName.length()-5);
            }
            else
                style = Font.PLAIN;
            editorStandardFont = new Font(fontName, style, size);
            editorStandoutFont = new Font(fontName, Font.BOLD, size);
        }
    }

    /**
     * Return the editor font size as an integer size
     * (use getStandardEditorFont() if access to the actual font is required)
     */
    protected static int getEditorFontSize()
    {
        return editorFontSize;
    }

    /**
     * Set users preference of whether to use syntax hilighting or not
     *
     * @param enabled   true if syntax hilighting should be used
     */
    protected static void setSyntaxHilighting(boolean enabled)
    {
        String hs = Config.getDefaultPropString(hilightingPropertyName,
                                                "true");

        if (Boolean.valueOf(hs).booleanValue() == enabled)
            Config.removeProperty(hilightingPropertyName);
        else
            Config.putPropString(hilightingPropertyName,
                                    new Boolean(enabled).toString());

        isSyntaxHilighting = enabled;
    }

    /**
     * Return the notation style for bluej
     * Two styles available: uml (default) or blue 
     */
    public static boolean isUML()
    {
        return isUML;
    }

   /**
     * Set users preference of whether to use syntax hilighting or not
     *
     * @param enabled   true if syntax hilighting should be used
     */
    protected static void setNotationStyle(String style)
    {
        // assumes UML is default, ie. if not blue style then is UML
        isUML = (!Graph.BLUE.equals(style));
        Config.putPropString(notationStyle, style);
        
    }
}
