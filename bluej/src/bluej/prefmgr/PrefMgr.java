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
 * @version $Id: PrefMgr.java 1149 2002-03-08 11:14:09Z mik $
 */
public class PrefMgr
{
    // publicly accessible names for flags
    public static final String HILIGHTING = "bluej.editor.syntaxHilighting";
    public static final String AUTO_INDENT = "bluej.editor.autoIndent";
    public static final String LINENUMBERS = "bluej.editor.displayLineNumbers";
    public static final String MAKE_BACKUP = "bluej.editor.makeBackup";
    public static final String LINK_LIB = "doctool.linkToStandardLib";

    public static final String USE_UML = "bluej.notation.style";
    public static final String USE_THEMES = "bluej.useTheme";

    private static final String editorFontPropertyName = "bluej.editor.font";
    private static final String editorFontSizePropertyName = "bluej.editor.fontsize";
    private static final String terminalFontPropertyName = "bluej.terminal.font";
    private static final String terminalFontSizePropertyName = "bluej.terminal.fontsize";

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
    private static Font popupMenuFont;
    private static Font italicMenuFont;

    // initialised by a call to setEditorFontSize()
    private static int editorFontSize;
    private static Font editorStandardFont, editorStandoutFont;

    // flags are all boolean preferences
    private static HashMap flags = new HashMap();

    // syntax hilighting
    /*
    private static boolean isSyntaxHilighting;
    private static boolean isAutoIndent;
    private static boolean isDisplayLineNumbers;
    private static boolean isMakeBackup;
    private static boolean isLinkDocumentation;
    private static boolean isUML;
    private static boolean hasTheme;
    */

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
        
        // popup menus are not permitted to be bold (MIK style guide) at present
        // make popup menus same font as drop down menus
        if(menuFontName.endsWith("-bold")) {
            menuFontName = menuFontName.substring(0, menuFontName.length()-5);
        }
        italicMenuFont = new Font(menuFontName, Font.ITALIC, menuFontSize);
        popupMenuFont = new Font(menuFontName, Font.PLAIN, menuFontSize);

        //standard font for UI components
        String normalFontName = Config.getPropString("bluej.font", "SansSerif");
        fontSize = Config.getPropInteger("bluej.fontsize", 12);
        normalFont = deriveFont(normalFontName, fontSize);

        String targetFontName = Config.getPropString("bluej.target.font", "SansSerif-bold");
        targetFontSize = Config.getPropInteger("bluej.target.fontsize", 12);
        targetFont = deriveFont(targetFontName, targetFontSize);        
        
        flags.put(HILIGHTING, Config.getPropString(HILIGHTING, "true"));
        flags.put(AUTO_INDENT, Config.getPropString(AUTO_INDENT, "false"));
        flags.put(LINENUMBERS, Config.getPropString(LINENUMBERS, "false"));
        flags.put(MAKE_BACKUP, Config.getPropString(MAKE_BACKUP, "false"));
        flags.put(LINK_LIB, Config.getPropString(LINK_LIB, "true"));
        flags.put(USE_THEMES, Config.getPropString(USE_THEMES, "false"));
        flags.put(USE_UML, 
                  String.valueOf(Config.getDefaultPropString(USE_UML, Graph.UML).equals(Graph.UML)));
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
    
    public static Font getPopupMenuFont()
    {
        return popupMenuFont;   
    }

    public static Font getTargetFont()
    {
        return targetFont;        
    }
    
    public static Font getStandardEditorFont()
    {
        return editorStandardFont;
    }

    /**
     * Get the value for a flag. Flags are boolean preferences.
     * 'flag' must be one of the flag names defined as public
     * constants in this class.
     */
    public static boolean getFlag(String flag)
    {
        String value = (String)flags.get(flag);
        if(value == null)
            return false;
        else
            return value.equals("true");
    }

    /**
     * Set a users preference flag (a boolean preference).
     *
     * @param flag    The name of the flag to set
     * @param enabled The new value of the flag
     */
    protected static void setFlag(String flag, boolean enabled)
    {
        String value = String.valueOf(enabled);
        String hs = Config.getDefaultPropString(flag, "true");

        if (Boolean.valueOf(hs).booleanValue() == enabled)
            Config.removeProperty(flag);
        else
            Config.putPropString(flag, value);

        flags.put(flag, value);
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
     * Set the editor font size preference to a particular point size
     *
     * @param size  the size of the font
     */
    public static Font getTerminalFont()
    {
        int size = Config.getPropInteger(terminalFontSizePropertyName, 12);
        String fontName = Config.getPropString(terminalFontPropertyName, "Monospaced");
         
        return deriveFont(fontName, size);
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
}
