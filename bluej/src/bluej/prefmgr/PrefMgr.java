/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013, 2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.*;
import java.util.List;
import java.util.*;

import bluej.Config;
import bluej.editor.EditorManager;
import bluej.editor.moe.BlueJSyntaxView;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;

/**
 * A class to manage the user editable preferences
 * settings.
 *
 * <p>Note that this is a singleton class. There can be only one
 * instance of PrefMgr at any time.
 *
 * @author  Andrew Patterson
 */
public class PrefMgr
{
    // publicly accessible names for flags
    public static final String HILIGHTING = "bluej.editor.syntaxHilighting";
    public static final String AUTO_INDENT = "bluej.editor.autoIndent";
    public static final String LINENUMBERS = "bluej.editor.displayLineNumbers";
    public static final String MAKE_BACKUP = "bluej.editor.makeBackup";
    public static final String MATCH_BRACKETS = "bluej.editor.matchBrackets";
    public static final String LINK_LIB = "doctool.linkToStandardLib";
    public static final String SHOW_TEST_TOOLS = "bluej.testing.showtools";
    public static final String SHOW_TEAM_TOOLS = "bluej.teamwork.showtools";
    public static final String SHOW_JAVAME_TOOLS = "bluej.javame.showtools";   
    public static final String SHOW_TEXT_EVAL = "bluej.startWithTextEval";
    public static final String SHOW_UNCHECKED = "bluej.compiler.showunchecked";
    public static final String SCOPE_HIGHLIGHTING_STRENGTH = "bluej.editor.scopeHilightingStrength";
    public static final String NAVIVIEW_EXPANDED="bluej.naviviewExpanded.default";
    public static final String ACCESSIBILITY_SUPPORT = "bluej.accessibility.support";
    
    public static final String USE_THEMES = "bluej.useTheme";
    // font property names
    private static final String editorFontPropertyName = "bluej.editor.font";
    private static final String editorMacFontPropertyName = "bluej.editor.MacOS.font";
    private static final String editorFontSizePropertyName = "bluej.editor.fontsize";

    // other constants
    private static final int NUM_RECENT_PROJECTS = Config.getPropInteger("bluej.numberOfRecentProjects", 12);
    
    // preference variables: FONTS
    private static int fontSize;
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
    private static Font editorStandardFont;

    // preference variables: (other than fonts)
    
    /** transparency of the scope highlighting */
    private static int highlightStrength; 
    
    // last value of naviviewExpanded
    private static boolean isNaviviewExpanded=true;
    
    // the current project directory
    private static String projectDirectory;

    // list of recently used projects
    private static List<String> recentProjects;
    
    // flags are all boolean preferences
    private static HashMap<String,String> flags = new HashMap<String,String>();

    /**
     * Initialise the preference manager. Font information is loaded from bluej.defs,
     * defaults for other prefs are loaded from bluej.defs.
     */
    static {
        //set up fonts
        initEditorFontSize(Config.getPropInteger(editorFontSizePropertyName, 12));

        //bluej menu font
        menuFontSize = Config.getPropInteger("bluej.menu.fontsize", 12);
        menuFont = Config.getFont("bluej.menu.font", "SansSerif", menuFontSize);
        
        // popup menus are not permitted to be bold (MIK style guide) at present
        // make popup menus same font as drop down menus
        italicMenuFont = menuFont.deriveFont(Font.ITALIC);
        popupMenuFont = menuFont.deriveFont(Font.PLAIN);

        //standard font for UI components
        fontSize = Config.getPropInteger("bluej.fontsize", 12);
        normalFont = Config.getFont("bluej.font", "SansSerif", fontSize);

        targetFontSize = Config.getPropInteger("bluej.target.fontsize", 12);
        targetFont = Config.getFont("bluej.target.font", "SansSerif-bold", targetFontSize);
        
        // preferences other than fonts:
        highlightStrength = initializeHighlighStrengh();
        isNaviviewExpanded=initializeisNavivewExpanded();
        
        projectDirectory = Config.getPropString("bluej.projectPath");
        recentProjects = readRecentProjects();
        
        flags.put(HILIGHTING, Config.getPropString(HILIGHTING, "true"));
        flags.put(AUTO_INDENT, Config.getPropString(AUTO_INDENT, "false"));
        flags.put(LINENUMBERS, Config.getPropString(LINENUMBERS, "false"));
        flags.put(MAKE_BACKUP, Config.getPropString(MAKE_BACKUP, "false"));
        flags.put(MATCH_BRACKETS, Config.getPropString(MATCH_BRACKETS, "true"));
        flags.put(LINK_LIB, Config.getPropString(LINK_LIB, "true"));
        flags.put(USE_THEMES, Config.getPropString(USE_THEMES, "false"));
        flags.put(SHOW_TEST_TOOLS, Config.getPropString(SHOW_TEST_TOOLS, "false"));
        flags.put(SHOW_TEAM_TOOLS, Config.getPropString(SHOW_TEAM_TOOLS, "false"));
        flags.put(SHOW_JAVAME_TOOLS, Config.getPropString(SHOW_JAVAME_TOOLS, "false"));        
        flags.put(SHOW_TEXT_EVAL, Config.getPropString(SHOW_TEXT_EVAL, "false"));
        flags.put(SHOW_UNCHECKED, Config.getPropString(SHOW_UNCHECKED, "true"));
        flags.put(ACCESSIBILITY_SUPPORT, Config.getPropString(ACCESSIBILITY_SUPPORT, "false"));
    }

    /**
     * Private constructor to prevent instantiation
     */
    private PrefMgr()
    {
        
    }
    
    /**
     * Check if BlueJ is runnung on a ARM processor (Raspberry Pi). If so, sets highlighStrengh to 0.
     * @return 0 if ARM processor. 20 otherwise
     */
    public static int initializeHighlighStrengh()
    {
     if (Config.isRaspberryPi()) {
         return Config.getPropInteger(SCOPE_HIGHLIGHTING_STRENGTH, 0);
     }else{
         return Config.getPropInteger(SCOPE_HIGHLIGHTING_STRENGTH, 20);
     }
     
    }
    
    /**
     * Check if BlueJ is runnung on a ARM processor (Raspberry Pi). If so, sets hides the code preview.
     * @return false if ARM processor. true otherwise.
     */
    public static boolean initializeisNavivewExpanded()
    {
     if (Config.isRaspberryPi()) {
         return Boolean.parseBoolean(Config.getPropString(NAVIVIEW_EXPANDED, "false"));
     }else{
         return Boolean.parseBoolean(Config.getPropString(NAVIVIEW_EXPANDED, "true"));
     }
     
    }
    
    // ----- system interface to read or set prefences: -----

    public static String getProjectDirectory()
    {
        return projectDirectory;
    }

    public static void setProjectDirectory(String newDir)
    {
        projectDirectory = newDir;
        Config.putPropString("bluej.projectPath", newDir);
    }

    public static List<String> getRecentProjects()
    {
        return recentProjects;
    }
    
    public static void addRecentProject(String projectName)
    {
        if(projectName == null)
            return;
            
        recentProjects.remove(projectName);
        
        if(recentProjects.size() == NUM_RECENT_PROJECTS)
            recentProjects.remove(NUM_RECENT_PROJECTS-1);
        
        recentProjects.add(0, projectName);
        
        for(int i = 0; i < recentProjects.size(); i++) {
            Config.putPropString("bluej.recentProject" + i, (String)recentProjects.get(i));
        }
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
        if(value == null){
            return false;
        }
        else
            return value.equals("true");
    }

    /**
     * Set a users preference flag (a boolean preference).
     *
     * @param flag    The name of the flag to set
     * @param enabled The new value of the flag
     */
    public static void setFlag(String flag, boolean enabled)
    {
        String value = String.valueOf(enabled);
        String systemDefault = Config.getDefaultPropString(flag, "");

        if ((systemDefault.length() > 0) &&
                (Boolean.valueOf(systemDefault).booleanValue() == enabled))
            Config.removeProperty(flag);  // remove from user defaults
        else
            Config.putPropString(flag, value);

        flags.put(flag, value);
    }

    // ----- end of system preference interface -----
    
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
     * The following methods are protected and should only be accessed by the
     * code which implements the various preferences dialog panels
     */

    /**
     * Set the editor font size preference to a particular point size
     *
     * @param size  the size of the font
     */
    public static void setEditorFontSize(int size)
    {
        if (size > 0) {
            initEditorFontSize(size);
            EditorManager.getEditorManager().refreshAll();
            Collection<Project> projects = Project.getProjects();
            Iterator<Project> i = projects.iterator();
            while (i.hasNext()) {
                Project project = i.next();
                if (project.hasTerminal()) {
                    project.getTerminal().resetFont();
                }
            }
            PkgMgrFrame [] frames = PkgMgrFrame.getAllFrames();
            for (int j = 0; j < frames.length; j++) {
                if(frames[j].getCodePad() != null) {
                    frames[j].getCodePad().resetFontSize();
                }
            }
        }
    }
    
    /**
     * Set up the editor font size, without informing various dependent components
     * of a size change.
     */
    private static void initEditorFontSize(int size)
    {
        if (size > 0 && size != editorFontSize) {
            editorFontSize = size;

            Config.putPropInteger(editorFontSizePropertyName, size);

            Font font;
            if(Config.isMacOS()) {
                font = Config.getFont(editorMacFontPropertyName, "Monaco", size);
            }
            else {
                font = Config.getFont(editorFontPropertyName, "Monospaced", size);
            }
            editorStandardFont = font;
        }
    }
    
    /**
     * Return the editor font size as an integer size
     * (use getStandardEditorFont() if access to the actual font is required)
     */
    public static int getEditorFontSize()
    {
        return editorFontSize;
    }
    
    public static int getScopeHighlightStrength()
    {
        return highlightStrength;
    }

    /**
     * Sets the highlight strength in the configs
     * @param strength representing light<->dark
     */
    public static void setScopeHighlightStrength(int strength)
    {
        highlightStrength = strength;
        BlueJSyntaxView.setHighlightStrength(strength);
        Config.putPropInteger(SCOPE_HIGHLIGHTING_STRENGTH, strength);
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
    
    /**
     * Returns the value of whether the naviview is expanded/collapsed
     * @return true if expanded; false if not
     */
    public static boolean getNaviviewExpanded()
    {   
        return isNaviviewExpanded;            
    }
    
}
