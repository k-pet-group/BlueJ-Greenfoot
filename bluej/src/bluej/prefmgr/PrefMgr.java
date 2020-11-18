/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.prefmgr;

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableIntegerValue;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;

/**
 * A class to manage the user editable preferences
 * settings.
 *
 * <p>Note that this is a singleton class. There can be only one
 * instance of PrefMgr at any time.
 *
 * @author  Andrew Patterson
 */
@OnThread(Tag.FXPlatform)
public class PrefMgr
{
    // publicly accessible names for flags
    public static final String HIGHLIGHTING = "bluej.editor.syntaxHilighting";
    public static final String AUTO_INDENT = "bluej.editor.autoIndent";
    public static final String LINENUMBERS = "bluej.editor.displayLineNumbers";
    public static final String MATCH_BRACKETS = "bluej.editor.matchBrackets";
    public static final String CHECK_DISKFILECHANGES = "bluej.editor.checkDiskFileChanges";
    public static final String LINK_LIB = "doctool.linkToStandardLib";
    public static final String SHOW_TEST_TOOLS = "bluej.testing.showtools";
    public static final String SHOW_TEAM_TOOLS = "bluej.teamwork.showtools";
    public static final String SHOW_TEXT_EVAL = "bluej.startWithTextEval";
    public static final String SHOW_UNCHECKED = "bluej.compiler.showunchecked";
    public static final String SCOPE_HIGHLIGHTING_STRENGTH = "bluej.editor.scopeHilightingStrength";
    public static final String NAVIVIEW_EXPANDED="bluej.naviviewExpanded.default";
    public static final String ACCESSIBILITY_SUPPORT = "bluej.accessibility.support";
    public static final String NEWS_TESTING = "bluej.news.testing";
    public static final String START_WITH_SUDO = "bluej.startWithSudo";
    public static final String STRIDE_SIDEBAR_SHOWING = "bluej.editor.stride.sidebarShowing";
    public static final String PACKAGE_PRINT_SOURCE = "bluej.packagePrint.source";
    public static final String PACKAGE_PRINT_DIAGRAM = "bluej.packagePrint.diagram";
    public static final String PACKAGE_PRINT_README = "bluej.packagePrint.readme";
    public static final String PRINT_LINE_NUMBERS = "bluej.print.lineNumbers";
    public static final String PRINT_SCOPE_HIGHLIGHTING = "bluej.print.scopeHighlighting";
    // This is stored as 4,5,6 for small, standard, large in case we later want to add a tiny size.
    // (if we called it 0,1,2, tiny would be -1 which seems like a bad idea)
    public static final String PRINT_FONT_SIZE = "bluej.print.fontSize";
    
    public static final int MIN_EDITOR_FONT_SIZE = 6;
    public static final int MAX_EDITOR_FONT_SIZE = 160;
    public static final int DEFAULT_STRIDE_FONT_SIZE = 11;
    public static final int DEFAULT_JAVA_FONT_SIZE = 10;
    // font property names
    private static final String editorFontPropertyName = "bluej.editor.font";
    private static final String editorMacFontPropertyName = "bluej.editor.MacOS.font";
    private static final String editorFontSizePropertyName = "bluej.editor.fontsize";
    // other constants
    private static final int NUM_RECENT_PROJECTS = Config.getPropInteger("bluej.numberOfRecentProjects", 12);
    // initialised by a call to setMenuFontSize()
    @OnThread(Tag.Swing)
    private static Font popupMenuFont;
    @OnThread(Tag.Swing)
    private static Font italicMenuFont;
    // initialised by a call to setEditorFontSize()
    @OnThread(Tag.FX)
    private static final IntegerProperty editorFontSize = new SimpleIntegerProperty(DEFAULT_JAVA_FONT_SIZE);
    @OnThread(Tag.FX)
    private static final StringProperty editorStandardFont = new SimpleStringProperty("Roboto Mono");
    private static final StringProperty editorFallbackFont = new SimpleStringProperty("monospace");
    @OnThread(Tag.FX)
    private static IntegerProperty strideFontSize = null; // Setup in call to strideFontSizeProperty

    private static PrintSize printFontSize = PrintSize.STANDARD;

    // preference variables: (other than fonts)
    
    /** transparency of the scope highlighting */
    @OnThread(Tag.FXPlatform)
    private static final IntegerProperty highlightStrength = new SimpleIntegerProperty(0);
    
    // last value of naviviewExpanded
    private static boolean isNaviviewExpanded=true;
    
    // the current project directory
    @OnThread(Tag.Any)
    private static String projectDirectory;

    // list of recently used projects
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static List<String> recentProjects;
    
    // flags are all boolean preferences
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static HashMap<String,String> flags = new HashMap<String,String>();
    // Flags have 0 or 1 properties.  Once requested for a flag, it
    // is shared between all uses of that property flag.
    @OnThread(Tag.FXPlatform)
    private static HashMap<String, BooleanProperty> flagProperties = new HashMap<>();
    // The CSS style needed to apply the editor font styling
    @OnThread(Tag.FX)
    private static StringExpression editorFontCSS;
    // The CSS style needed to apply the editor font size styling, but
    // slightly shrunken and doesn't set family
    @OnThread(Tag.FX)
    private static StringExpression editorFontSizeOnlyCSS;

    // A property to hold the Greenfoot player's name
    private static StringProperty playerName;

    /**
     * Private constructor to prevent instantiation
     */
    private PrefMgr()
    {
        
    }

    @OnThread(Tag.Any)
    public static File getProjectDirectory()
    {
        File dir = new File(projectDirectory);
        if (dir.exists() && dir.isDirectory())
            return dir;
        else
            return new File(System.getProperty("user.home"));
    }
    
    // ----- system interface to read or set prefences: -----

    @OnThread(Tag.Any)
    public static void setProjectDirectory(String newDir)
    {
        projectDirectory = newDir;
        Config.putPropString("bluej.projectPath", newDir);
    }

    @OnThread(Tag.Any)
    public static synchronized List<String> getRecentProjects()
    {
        // Take copy to avoid race hazards:
        return new ArrayList<>(recentProjects);
    }

    public static synchronized void addRecentProject(File projectDir)
    {
        if (Config.isGreenfoot() && Config.isGreenfootStartupProject(projectDir))
            return; // Don't add startup project to recent projects

        String projectName = projectDir.getAbsolutePath();

        recentProjects.remove(projectName);
        
        if(recentProjects.size() == NUM_RECENT_PROJECTS)
            recentProjects.remove(NUM_RECENT_PROJECTS-1);
        
        recentProjects.add(0, projectName);
        
        for(int i = 0; i < recentProjects.size(); i++) {
            Config.putPropString("bluej.recentProject" + i, recentProjects.get(i));
        }
    }

    @OnThread(Tag.Swing)
    public static Font getStandoutMenuFont()
    {
        return italicMenuFont;
    }

    @OnThread(Tag.Swing)
    public static Font getPopupMenuFont()
    {
        return popupMenuFont;   
    }

    /**
     * Get the value for a flag. Flags are boolean preferences.
     * 'flag' must be one of the flag names defined as public
     * constants in this class.
     */
    @OnThread(Tag.Any)
    public static synchronized boolean getFlag(String flag)
    {
        String value = flags.get(flag);
        if(value == null){
            return false;
        }
        return value.equals("true");
    }

    /**
     * Provides a read-only observable view of a flag's value.
     */
    @OnThread(Tag.FXPlatform)
    public static BooleanExpression flagProperty(String flagName)
    {
        return flagProperties.computeIfAbsent(flagName, f -> new SimpleBooleanProperty(getFlag(f)));
    }

    /**
     * Set a users preference flag (a boolean preference).
     *
     * @param flag    The name of the flag to set
     * @param enabled The new value of the flag
     */
    @OnThread(Tag.Any)
    public static synchronized void setFlag(String flag, boolean enabled)
    {
        String value = String.valueOf(enabled);
        String systemDefault = Config.getDefaultPropString(flag, "");

        if ((systemDefault.length() > 0) &&
                (Boolean.valueOf(systemDefault).booleanValue() == enabled))
            Config.removeProperty(flag);  // remove from user defaults
        else
            Config.putPropString(flag, value);

        flags.put(flag, value);
        JavaFXUtil.runNowOrLater(() -> {
            BooleanProperty prop = flagProperties.get(flag);
            if (prop != null)
                prop.set(enabled);
        });
    }

    private static List<String> readRecentProjects()
    {
        List<String> projects = new ArrayList<String>(NUM_RECENT_PROJECTS);
        
        for(int i = 0; i < NUM_RECENT_PROJECTS; i++) {
            String projectName = Config.getPropString("bluej.recentProject" + i, "");
            if(projectName.length() > 0)
                projects.add(projectName);
        }
        return projects;
    }

    /**
     * Set the editor font size preference to a particular point size
     *
     * @param size  the size of the font
     */
    public static void setEditorFontSize(int size)
    {
        if (size > 0) {
            initEditorFontSize(size);
        }
    }
    
    /**
     * Set up the editor font size, without informing various dependent components
     * of a size change.
     */
    private static void initEditorFontSize(int size)
    {
        if (size > 0 && size != editorFontSize.get()) {
            editorFontSize.set(size);

            Config.putPropInteger(editorFontSizePropertyName, size);

            String font;
            if(Config.isMacOS()) {
                font = Config.getPropString(editorMacFontPropertyName, "Roboto Mono");
            }
            else {
                font = Config.getPropString(editorFontPropertyName, "Roboto Mono");
            }
            editorStandardFont.set(font);
        }
    }
    
    /**
     * Return the editor font size as an integer size
     * (use getStandardEditorFont() if access to the actual font is required)
     */
    @OnThread(Tag.FXPlatform)
    public static IntegerProperty getEditorFontSize()
    {
        return editorFontSize;
    }

    @OnThread(Tag.FX)
    public static StringExpression getEditorFontCSS(boolean includeFamily)
    {
        if (editorFontCSS == null)
        {
            editorFontSizeOnlyCSS = Bindings.concat(
                    "-fx-font-size: ", editorFontSize, "pt;");
            editorFontCSS = Bindings.concat(
                    "-fx-font-size: ", editorFontSize, "pt;",
                    "-fx-font-family: \"", editorStandardFont, "\";"
            );
        }
        return includeFamily ? editorFontCSS : editorFontSizeOnlyCSS;
    }

    /**
     * Get the CSS for setting the Java editor font family like {{{-fx-font-family: "Arial";}}}
     * Note this can change later if the user changes style, so only
     * use this for an instant query (e.g. for printing)
     */
    public static String getEditorFontFamilyCSS()
    {
        return "-fx-font-family: \"" + editorStandardFont.get() + "\", " + editorFallbackFont.get() + ";";
    }

    public static String getEditorFontFamily()
    {
        return editorStandardFont.get();
    }

    @OnThread(Tag.FXPlatform)
    public static ObservableIntegerValue getScopeHighlightStrength()
    {
        return highlightStrength;
    }
    
    /**
     * Sets the highlight strength in the configs
     * @param strength representing light<->dark
     */
    public static void setScopeHighlightStrength(int strength)
    {
        highlightStrength.set(strength);
        Config.putPropInteger(SCOPE_HIGHLIGHTING_STRENGTH, strength);
    }

    /**
     * Returns the value of whether the naviview is expanded/collapsed
     * @return true if expanded; false if not
     */
    public static boolean getNaviviewExpanded()
    {   
        return isNaviviewExpanded;            
    }
    
    /**
     * Sets the value of the naviview to expanded/collapsed 
     * to the local variable and to the configs
     * @param expanded true if expanded; false if not
     */
    public static void setNaviviewExpanded(boolean expanded)
    {
        isNaviviewExpanded=expanded;
        Config.putPropString(NAVIVIEW_EXPANDED, String.valueOf(expanded));
    }
    
    @OnThread(Tag.FX)
    public static IntegerProperty strideFontSizeProperty()
    {
        if (strideFontSize == null)
        {
            String fontSizePropName = "bluej.stride.editor.fontSize";
            int sizeFromConfig = Config.getPropInteger(fontSizePropName,DEFAULT_STRIDE_FONT_SIZE);
            int clampedSize = Math.max(MIN_EDITOR_FONT_SIZE, Math.min(MAX_EDITOR_FONT_SIZE, sizeFromConfig));
            strideFontSize = new SimpleIntegerProperty(clampedSize);
            
            strideFontSize.addListener((a, b, newVal) -> {
                Config.putPropInteger(fontSizePropName, newVal.intValue());
            });
        }
        
        return strideFontSize;
    }
    
    /**
     * Get the property holding the player name, used for Greenfoot (set to determine what UserInfo
     * "current user" name will return).
     */
    public static StringProperty getPlayerName()
    {
        return playerName;
    }

    /**
     * Initialise the preference manager. Font information is loaded from bluej.defs,
     * defaults for other prefs are loaded from bluej.defs.
     */
    static {
        //set up fonts
        initEditorFontSize(Config.getPropInteger(editorFontSizePropertyName, 12));
        JavaFXUtil.addChangeListener(editorFontSize, size -> Config.putPropInteger(editorFontSizePropertyName, size.intValue()));

        //bluej menu font
        int menuFontSize = Config.getPropInteger("bluej.menu.fontsize", 12);
        Font menuFont = Config.getFont("bluej.menu.font", "SansSerif", menuFontSize);
        
        // popup menus are not permitted to be bold (MIK style guide) at present
        // make popup menus same font as drop down menus
        italicMenuFont = menuFont.deriveFont(Font.ITALIC);
        popupMenuFont = menuFont.deriveFont(Font.PLAIN);

        // preferences other than fonts:
        highlightStrength.set(Config.getPropInteger(SCOPE_HIGHLIGHTING_STRENGTH, 20));
        
        projectDirectory = Config.getPropString("bluej.projectPath", System.getProperty("user.home"));
        recentProjects = readRecentProjects();
        
        flags.put(HIGHLIGHTING, Config.getPropString(HIGHLIGHTING, "true"));
        flags.put(AUTO_INDENT, Config.getPropString(AUTO_INDENT, "false"));
        flags.put(LINENUMBERS, Config.getPropString(LINENUMBERS, "false"));
        flags.put(MATCH_BRACKETS, Config.getPropString(MATCH_BRACKETS, "true"));
        flags.put(CHECK_DISKFILECHANGES, Config.getPropString(CHECK_DISKFILECHANGES, "true"));
        flags.put(LINK_LIB, Config.getPropString(LINK_LIB, "true"));
        flags.put(SHOW_TEST_TOOLS, Config.getPropString(SHOW_TEST_TOOLS, "false"));
        flags.put(SHOW_TEAM_TOOLS, Config.getPropString(SHOW_TEAM_TOOLS, "false"));
        flags.put(SHOW_TEXT_EVAL, Config.getPropString(SHOW_TEXT_EVAL, "false"));
        flags.put(SHOW_UNCHECKED, Config.getPropString(SHOW_UNCHECKED, "true"));
        flags.put(ACCESSIBILITY_SUPPORT, Config.getPropString(ACCESSIBILITY_SUPPORT, "false"));
        flags.put(START_WITH_SUDO, Config.getPropString(START_WITH_SUDO, "true"));
        flags.put(STRIDE_SIDEBAR_SHOWING, Config.getPropString(STRIDE_SIDEBAR_SHOWING, "true"));
        flags.put(NEWS_TESTING, Config.getPropString(NEWS_TESTING, "false"));

        flags.put(PRINT_LINE_NUMBERS, Config.getPropString(PRINT_LINE_NUMBERS, "false"));
        flags.put(PRINT_SCOPE_HIGHLIGHTING, Config.getPropString(PRINT_SCOPE_HIGHLIGHTING, "true"));
        flags.put(PACKAGE_PRINT_DIAGRAM, Config.getPropString(PACKAGE_PRINT_DIAGRAM, "true"));
        flags.put(PACKAGE_PRINT_README, Config.getPropString(PACKAGE_PRINT_README, "true"));
        flags.put(PACKAGE_PRINT_SOURCE, Config.getPropString(PACKAGE_PRINT_SOURCE, "true"));

        // See comments on PRINT_FONT_SIZE:
        switch (Config.getPropInteger(PRINT_FONT_SIZE, 4))
        {
            case 3:
                printFontSize = PrintSize.SMALL;
                break;
            case 5:
                printFontSize = PrintSize.LARGE;
                break;
            case 4:
            default:
                printFontSize = PrintSize.STANDARD;
                break;
        }

        if (Config.isGreenfoot())
        {
            playerName = new SimpleStringProperty(Config.getPropString("greenfoot.player.name", "Player1"));
            JavaFXUtil.addChangeListener(playerName,
                    name -> Config.putPropString("greenfoot.player.name", name));
        }
    }

    /**
     * Gets the saved preference for print font size.
     */
    public static PrintSize getPrintFontSize()
    {
        return printFontSize;
    }

    /**
     * Saves a new value for the preferred print font size
     */
    public static void setPrintFontSize(PrintSize size)
    {
        printFontSize = size;
        // See comments on PRINT_FONT_SIZE:
        switch (size)
        {
            case SMALL:
                Config.putPropInteger(PRINT_FONT_SIZE, 3);
                break;
            case STANDARD:
                Config.putPropInteger(PRINT_FONT_SIZE, 4);
                break;
            case LARGE:
                Config.putPropInteger(PRINT_FONT_SIZE, 5);
                break;
        }
    }

    public static enum PrintSize
    {
        SMALL, STANDARD, LARGE;


        // The label to use when showing in the interface:
        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public String toString()
        {
            return Config.getString("editor.printDialog.fontSize." + this.name().toLowerCase());
        }
    }
}
