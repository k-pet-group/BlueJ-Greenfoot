package bluej.extensions;

import bluej.extensions.event.ExtEventListener;
import bluej.extmgr.ExtensionWrapper;
import bluej.extmgr.PrefManager;
import bluej.extmgr.MenuManager;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Provides services to BlueJ extensions. 
 * This is the top-level object of the proxy hierarchy, bear in mind that
 * there is much similarity between the Reflection API and this API.<p>
 * Every effort has been made to retain the logic of Reflection and to provide
 * methods that behave in a very similar way.
 * 
 * <PRE>
 * BlueJ
 *   |
 *   +---- BProject
 *             |
 *             +---- BPackage
 *                      |
 *                      +--------- BClass
 *                      |            |
 *                      +- BObject   + BConstuctor
 *                                   |      |
 *                                   |      +- BObject
 *                                   |
 *                                   +---- BMethod
 *                                   |      |
 *                                   |      +- BObject
 *                                   |
 *                                   +---- BField
 *    
 * </PRE>
 * @version $Id: BlueJ.java 1672 2003-03-10 08:58:56Z damiano $
 */

public class BlueJ
{
    private final ExtensionWrapper myWrapper;
    private final PrefManager      prefManager;

    private PrefGen    currentPrefGen=null;
    private MenuGen    currentMenuGen=null;
    private Properties localLabels;

    /**
     * Extensions should not call this constructor!
     * When this constructor is called you can safely make use of the object given.
     */
    public BlueJ (ExtensionWrapper myWrapper, PrefManager prefManager)
    {
        this.myWrapper   = myWrapper;
        this.prefManager = prefManager;

        /**
         * I do NOT want lazy initialization othervise I may try to load it
         * may times just because I cannof find anything.
         * Or having state variables to know I I did load it but had nothing found
         */
        localLabels = myWrapper.getLabelProperties();
    }
    
    

    /**
     * Opens a project
     * 
     * @return the BProject that describes the newly opened or null if it cannot be opened
     */
    public BProject openProject (File directory)
    {
        // Yes somebody may just call it with null, for fun... TODO: Needs error reporting
        if ( directory == null ) return null;
        
        PkgMgrFrame currentFrame = PkgMgrFrame.getMostRecent();
        if (currentFrame == null) return null;
        
        Project openProj = Project.openProject (directory.getAbsolutePath());
        if(openProj == null) return null;
        
        Package pkg = openProj.getPackage(openProj.getInitialPackageName());
        if ( pkg == null ) return null;

        PkgMgrFrame pmf = currentFrame.findFrame(pkg);
        
        if (pmf == null) 
            {
            if (currentFrame.isEmptyFrame()) 
                {
                pmf = currentFrame;
                currentFrame.openPackage(pkg);
                }
            else 
                {
                pmf = currentFrame.createFrame(pkg);
                DialogManager.tileWindow(pmf, currentFrame);
                }
            }

        pmf.show();
        return new BProject (openProj);
    }
        
    /**
     * Create new project.
     * 
     * @return the newly created BProject if successful. null otherwise.
     */
    public BProject newProject (File directory)
    {
        String pathString = directory.getAbsolutePath();
        if (!pathString.endsWith (File.separator)) pathString += File.separator;
        if  ( ! Project.createNewProject(pathString) ) return null;
        return openProject ( directory );
    }



    /**
     * Gets all the currently open projects.
     * 
     * @return an array of the currently open project objects. It can be an empty array
     */
    public BProject[] getOpenProjects()
        {
        // If this extension is not valid return an empty array.
        if (!myWrapper.isValid()) return new BProject[0];

        Set projSet = Project.getProjectKeySet();
        BProject [] result = new BProject[projSet.size()];
        Iterator iter = projSet.iterator();
        int insIndex = 0;
        
        while ( iter.hasNext() )
            {
            Object projKey = iter.next();
            result[insIndex++] = new BProject ((File)projKey);
            }

        return result;
        }


    /**
     * Gets the current package. That is, the most recently accessed package.
     * It can return null if this information is not available.
     * This is here and NOT into a BProject since it depends on user interface.
     * Depending on what is the currently selected Frame you may get packages that
     * belongs to different projects.
     *
     * @return the current package
     */
    public BPackage getCurrentPackage()
    {
        PkgMgrFrame pmf = PkgMgrFrame.getMostRecent();
        // If there is nothing at all open there is no Frame open...
        if (pmf == null) return null;

        Package pkg = pmf.getPackage();
        // The frame may be there BUT have no package. 
        // I do NOT want to create what is NOT there
        if ( pkg == null ) return null;

        return new BPackage (pkg);
    }



    /**
     * Install a new menu generator for this extension
     * If you want no menues then just set it to null
     */
    public void setMenuGen ( MenuGen menuGen )
    {
        currentMenuGen = menuGen;

        MenuManager menuManager=myWrapper.getMenuManager();
        if ( menuManager == null ) return;
        menuManager.menuExtensionRevalidateReq();
    }

    /**
     * @return What you have set with the setMenuGen
     */
    public MenuGen getMenuGen ()
    {
        return currentMenuGen;
    }

    /**
     * Install a new preference panel for this extension
     * If you want to delete it just set prefGen to null
     */
    public void setPrefGen(PrefGen prefGen)
    {
        currentPrefGen = prefGen;
        prefManager.panelRevalidate();
    }
    
    /**
     * @return what you have set with setBPrefPanel
     */
    public PrefGen getPrefGen()
    {
        return currentPrefGen;
    }


    /**
     * Returns the arguments with which BlueJ was started.
     * 
     * @return args
     */
    public List getArgs()
    {
        return myWrapper.getArgs();
    }
    
    /**
     * Returns the path to the BlueJ system library directory 
     * <CODE>&lt;bluej&gt;/lib/</CODE>
     * @return the lib directory
     */
    public File getSystemLib()
    {
        return myWrapper.getBlueJLib();
    }

    /**
     * Returns the path to a file contained in the
     * user's bluej settings <CODE>&lt;user&gt;/bluej/<I>file</I></CODE>
     * @param subpath the name of a file or subpath and file
     * @return the path to the user file, which may not exist.
     */
    public File getUserFile (String subpath)
    {
        return Config.getUserConfigFile (subpath);
    }
    
    /**
     * Registers a package listener with the BlueJ object.
     * Opening and Closing events will be passed to the listener.
     * @param pl the listener
     */
    public void addExtEventListener (ExtEventListener el)
    {
        myWrapper.addExtEventListener (el);
    }

    /**
     * Shows a message box, with this text and an OK button
     * @param message the text to be displayed in the box.
     */
    public void showMessage (String message)
    {
        DialogManager.showText (PkgMgrFrame.getMostRecent(), message);
    }
   
    /**
     * Gets the bluej default dialog border
     * @return a blank border of 5 pixels
     */
    public javax.swing.border.Border getDialogBorder()
    {
        return Config.dialogBorder;
    }
     
    /**
     * Centres a frame onto the package frame
     * @param frame the frame to be centred
     */
    public void centreWindow (java.awt.Window child)
    {
        centreWindow (child, PkgMgrFrame.getMostRecent());
    }
    
    /**
     * centreWindow - tries to center a window within a parent window
     */
    public static void centreWindow(Window child, Window parent)
    {
        child.pack();

        Point p_topleft = parent.getLocationOnScreen();
        Dimension p_size = parent.getSize();
        Dimension d_size = child.getSize();

        Dimension screen = parent.getToolkit().getScreenSize(); // Avoid window going off the screen
        int x = p_topleft.x + (p_size.width - d_size.width) / 2;
        int y = p_topleft.y + (p_size.height - d_size.height) / 2;
        if (x + d_size.width > screen.width) x = screen.width - d_size.width;
        if (y + d_size.height > screen.height) y = screen.height - d_size.height;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        child.setLocation(x,y);
    }

    /**
     * As for showLabelDialog, but you can specify the modal parent frame
     * @param title the title of the dialog box
     * @param body the contents of the dialog
     * @param parent the Frame that is to be the modal parent
     * @return the dialog
     */
    public JDialog showGeneralDialog (String title, Component body, Frame parent)
    {
        final JDialog dialog = new JDialog (parent, title, true);
        addDialogBody (dialog, body);
        return dialog;
    }
    
    /**
     * As for showLabelDialog, but you can specify the modal parent dialog
     * @param title the title of the dialog box
     * @param body the contents of the dialog
     * @param parent the Dialog that is to be the modal parent
     * @return the dialog
     */
    public JDialog showGeneralDialog (String title, Component body, Dialog parent)
    {
        final JDialog dialog = new JDialog (parent, title, true);
        addDialogBody (dialog, body);
        return dialog;
    }
    
    /**
     * Creates a skeleton dialog box plus a close button in the local language.
     * @param title the title of the dialog box
     * @param body the contents of the dialog box
     * @return the dialog so you can dispose of it if you need to.
     */
    public JDialog showGeneralDialog (String title, Component body)
    {
        return showGeneralDialog (title, body, PkgMgrFrame.getMostRecent());
    }

    private void addDialogBody (final JDialog dialog, Component body)
    {
        JPanel panel = new JPanel();
        panel.setLayout (new BoxLayout (panel, BoxLayout.Y_AXIS));
        panel.add (body);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                dialog.dispose();
            }
        });

        panel.add (Box.createVerticalStrut (5));
        
        JButton close = new JButton (Config.getString("close"));
        close.setAlignmentX (0.5f);
        close.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                dialog.dispose();
            }
        });
        panel.add (close);
        panel.setBorder(getDialogBorder());
        dialog.setContentPane(panel);

        dialog.pack();
        centreWindow (dialog);
        dialog.setVisible(true);
    }

     /**
      * Gets a property from BlueJ properties, but include a default value.
      * @param property The name of the required global property
      * @param def The default value to use if the property
      * cannot be found.
      * @return the value of that property.
      */
    public String getBJPropString (String property, String def)
    {
        return Config.getPropString ( property, def);
    }

     /**
      * Gets a property from BlueJ properties file, but include a default value.
      * You MUST use the setExtPropString to write the propertie that you want stored.
      * You can then come back and retrieve it using this function.
      * @param property The name of the required global property
      * @param def The default value to use if the property cannot be found
      * @return the value of that property
      */
    public String getExtPropString (String property, String def)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        return Config.getPropString (thisKey, def);
    }
     
     /**
      * Sets a property into BlueJ properties file.
      * The property name does not NEED to be fully qulified since a prefix will be prepended to it.
      * @param property The name of the required global property
      * @param value the required value of that property.
      */
    public void setExtPropString (String property, String value)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        Config.putPropString (thisKey, value);
    }
    
    /**
     * Gets a language-independent label. First the BlueJ library is
     * searched, <CODE>&lt;bluej&gt;/lib/&lt;language&gt;/labels</CODE>,
     * then the local, extension's library (if it has one) is searched:
     * <CODE>lib/&lt;language&gt;/labels</CODE>, for example,
     * <CODE>lib/english/labels</CODE> for the English language settings.
     * If no labels are found for the current language, the default language (english) will be tried.
     * @param id the id of the label to be searched
     * @return the label appropriate to the current language, or,
     * if that fails, the name of the label will be returned.
     */
    public String getLabel (String wantKey)
    {
        // First try from the standard BlueJ properties
        String label = Config.getString (wantKey, null);
        if ( label != null ) return label;

        if ( localLabels == null ) return wantKey;

        return localLabels.getProperty (wantKey, wantKey);
    }
    
    /**
     * Gets a language-independent label, and replaces the first occurrance
     * of a <code>$</code> symbol with the given replacement string.
     * If there is no occurrance of <code>$</code> then it will be added
     * after a space, to the end of the resulting string
     * @param id the id of the label to be searched in the dictionaries
     * @param replacement the string to replace the <code>$</code>.
     * @return the label, suitably modified.
     */
    public String getLabelInsert (String id, String replacement)
    {
        String label = getLabel (id);
        int p = label.indexOf ('$');
        if (p == -1) {
            label += " $";
            p = label.indexOf ('$');
        }
        label = label.substring (0, p) + replacement + label.substring (p+1);
        return label;
    }

    
    
    /**
     * Close BlueJ
     */
    public void closeBlueJ()
    {
        PkgMgrFrame.getMostRecent().wantToQuit();
    }
}