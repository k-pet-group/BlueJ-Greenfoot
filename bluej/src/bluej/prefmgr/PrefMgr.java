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
 * @version $Id: PrefMgr.java 888 2001-05-10 02:16:47Z bquig $
 */
public class PrefMgr
{
    private static final String hilightingPropertyName = "bluej.syntaxHilighting";
    private static final String linkingPropertyName = "doctool.linkToStandardLib";
    private static final String editorFontPropertyName = "bluej.editor.font";
    private static final String editorFontSizePropertyName = "bluej.editor.fontsize";
    private static final String notationStyle = "bluej.notation.style";

    private static int fontSize;
    private static int editFontsize;
    private static int printFontsize;
    private static int printTitleFontsize;
    private static int printInfoFontsize;
    private static int targetFontSize;

    private static Font normalFont;
    private static Font targetFont;

    // initialised by a call to setMenuFontSize()
    private static int menuFontSize;
    private static Font menuFont;
    private static Font italicMenuFont;

    // initialised by a call to setEditorFontSize()
    private static int editorFontSize;
    private static Font editorStandardFont, editorStandoutFont;

    // syntax hilighting
    private static boolean isSyntaxHilighting;
    private static boolean isLinkDocumentation;
    private static boolean isUML;

    private static PrefMgr prefmgr = new PrefMgr();

	/**
	 */
    private PrefMgr()
    {
        //set up fonts
        setEditorFontSize(Config.getPropInteger(editorFontSizePropertyName, 12));

        //bluej menu font
        String menuFontName = Config.getPropString("bluej.menu.font", "SansSerif");
        menuFontSize = Config.getPropInteger("bluej.menu.fontsize", 12);
        menuFont = deriveFont(menuFontName, menuFontSize);
        italicMenuFont = new Font(menuFontName, Font.ITALIC, menuFontSize);

        //standard font for UI components
        String normalFontName = Config.getPropString("bluej.font", "SansSerif");
        fontSize = Config.getPropInteger("bluej.fontsize", 12);
        normalFont = deriveFont(normalFontName, fontSize);

        String targetFontName = Config.getPropString("bluej.target.font", "SansSerif-bold");
        targetFontSize = Config.getPropInteger("bluej.target.fontsize", 12);
        targetFont = deriveFont(targetFontName, targetFontSize);        
        
        isSyntaxHilighting = Boolean.valueOf(
            Config.getPropString(hilightingPropertyName, "true")).booleanValue();

        isLinkDocumentation = Boolean.valueOf(
            Config.getPropString(linkingPropertyName, "true")).booleanValue();

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

    public static Font getTargetFont()
    {
        return targetFont;        
    }
    
    public static Font getStandardEditorFont()
    {
        return editorStandardFont;
    }

    public static boolean useSyntaxHilighting()
    {
        return isSyntaxHilighting;
    }

    public static boolean linkDocToLibrary()
    {
        return isLinkDocumentation;
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
         
            editorStandardFont = deriveFont(fontName, size);
            editorStandoutFont = new Font(fontName, Font.BOLD, size);
        }
    }
    
    /**
    * Create font from name, style and size info.  Styles allowed are PLAIN
    * and BOLD.  Bold is determined by -bold suffix on font name.
    */
    private static Font deriveFont(String fontName, int size)
    {
        int style;
            if(fontName.endsWith("-bold")) {
                style = Font.BOLD;
                fontName = fontName.substring(0, fontName.length()-5);
            }
            else
                style = Font.PLAIN;
        
        return new Font(fontName, style, size);
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
     * Set users preference of whether to link documentation or not
     *
     * @param enabled   true if syntax documentation should be linked
     */
    protected static void setDocumentationLinking(boolean enabled)
    {
        String hs = Config.getDefaultPropString(linkingPropertyName,
                                                "true");

        if (Boolean.valueOf(hs).booleanValue() == enabled)
            Config.removeProperty(linkingPropertyName);
        else
            Config.putPropString(linkingPropertyName,
                                    new Boolean(enabled).toString());

        isLinkDocumentation = enabled;
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
