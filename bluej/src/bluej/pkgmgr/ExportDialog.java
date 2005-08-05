package bluej.pkgmgr;

import bluej.*;
import bluej.Config;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPath;
import bluej.classmgr.ClassPathEntry;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.io.File;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for exporting the project to a jar file. Here, the jar
 * creation options can be specified.
 *
 * @author  Michael Kolling
 * @version $Id: ExportDialog.java 3505 2005-08-05 15:43:20Z damiano $
 */
class ExportDialog extends EscapeDialog
{
    // Internationalisation
    private static final String dialogTitle = Config.getString("pkgmgr.export.title");
    private static final String helpLine1 = Config.getString("pkgmgr.export.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.export.helpLine2");
    private static final String classLabelText = Config.getString("pkgmgr.export.classLabel");
    private static final String libsLabel = Config.getString("pkgmgr.export.includeLibs");
    private static final String sourceLabel = Config.getString("pkgmgr.export.sourceLabel");
    private static final String pkgFilesLabel = Config.getString("pkgmgr.export.pkgFilesLabel");
    private static final String noClassText = Config.getString("pkgmgr.export.noClassText");

    private String mainClassName = "";

    private JComboBox classSelect;
    private JCheckBox sourceBox;
    private JCheckBox pkgFilesBox;
    private JCheckBox otherFilesBox;
    private UserLibInfo[] userLibs;
    
    private boolean ok;		// result: which button?

    public ExportDialog(PkgMgrFrame parent)
    {
        super(parent, dialogTitle, true);
        makeDialog(parent.getProject());
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        setVisible(true);  // returns after OK or Cancel, which set 'ok'
        return ok;
    }

    /**
     * Return the name of the main class in the project.
     */
    public String getMainClass()
    {
        return mainClassName;
    }

    /**
     * Return userlibs selected in the dialogue.
     * @return  A list of File objects.
     */
    public List getSelectedLibs()
    {
        List selected = new ArrayList();

        if(userLibs != null) {
            for(int i = 0; i < userLibs.length; i++) {
                if(userLibs[i].isSelected())
                    selected.add(userLibs[i].getFile());
            }
        }
        
        return selected;
    }

    /**
     * Return true if user wants to include the source.
     */
    public boolean includeSource()
    {
        return sourceBox.isSelected();
    }
    
    /**
     * Return true if the user wants to include the BlueJ project info files
     * (.pkg files)
     */
    public boolean includePkgFiles()
    {
        return pkgFilesBox.isSelected();
    }
    
    /**
     * Close action when OK is pressed.
     */
    private void doOK()
    {
        mainClassName = (String)classSelect.getSelectedItem();
        if(mainClassName.equals(noClassText))
            mainClassName = "";
        ok = true;
        setVisible(false);
    }

    /**
     * Close action when Cancel is pressed.
     */
    private void doCancel()
    {
        ok = false;
        setVisible(false);
    }

    /**
     * Create the dialog interface.
     */
    private void makeDialog(Project project)
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            mainPanel.add(helpText1);

            JLabel helpText2 = new JLabel(helpLine2);
            mainPanel.add(helpText2);

            Font smallFont = helpText1.getFont().deriveFont(Font.ITALIC, 11.0f);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            mainPanel.add(new JSeparator());
            mainPanel.add(Box.createVerticalStrut(5));

            JPanel inputPanel = new JPanel();
            {
                inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
                inputPanel.setAlignmentX(LEFT_ALIGNMENT);

                JPanel mainClassPanel = new JPanel();
                {
                    JLabel classLabel = new JLabel(classLabelText);
                    mainClassPanel.add(classLabel);

                    classSelect = makeClassPopup(project);
                    mainClassPanel.add(classSelect);
                    
                }
                mainClassPanel.setAlignmentX(LEFT_ALIGNMENT);
                inputPanel.add(mainClassPanel);
                inputPanel.add(Box.createVerticalStrut(5));
                
                JPanel userLibPanel = createUserLibPanel(project);
                if(userLibPanel != null) {
                    userLibPanel.setAlignmentX(LEFT_ALIGNMENT);
                    inputPanel.add(userLibPanel);
                    inputPanel.add(Box.createVerticalStrut(5));
                }
                
                sourceBox = new JCheckBox(sourceLabel, false);
                sourceBox.setAlignmentX(LEFT_ALIGNMENT);
                inputPanel.add(sourceBox);
                inputPanel.add(Box.createVerticalStrut(5));
                pkgFilesBox = new JCheckBox(pkgFilesLabel);
                inputPanel.add(pkgFilesBox);
            }

            mainPanel.add(inputPanel);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton continueButton = BlueJTheme.getContinueButton();
				continueButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) { doOK(); }        		
				});

                JButton cancelButton = BlueJTheme.getCancelButton();
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) { doCancel(); }        		
				});

                buttonPanel.add(continueButton);
                buttonPanel.add(cancelButton);

                getRootPane().setDefaultButton(continueButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    /**
     * Fill the class name popup selector with all classes of the project
     */
    private JComboBox makeClassPopup(Project project)
    {
        JComboBox popup = new JComboBox();

        popup.setFont(PrefMgr.getPopupMenuFont());
        popup.addItem(noClassText);

        List packageNames = project.getPackageNames();
        Collections.sort(packageNames);

        for (Iterator packages = packageNames.iterator(); packages.hasNext(); ) {
            String pkgName = (String)packages.next();
            // SHould be a getPackage, Damiano
            List classNames = project.getPackage(pkgName).getAllClassnames();
            Collections.sort(classNames);
            if(pkgName.length() > 0) 
                for (Iterator classes = classNames.iterator(); classes.hasNext();)
                    popup.addItem(pkgName + "." + classes.next());
            else
                for (Iterator classes = classNames.iterator(); classes.hasNext();)
                    popup.addItem(classes.next());
        }
        
        return popup;
    }
    
    /**
     * Return a prepared panel listing the user libraries with check boxes.
     * @param project the project the libraries belong to.
     */
    private JPanel createUserLibPanel(Project project)
    {
        // collect info about jar files from the project classloader.
        ArrayList userlibList = new ArrayList();
        File [] fileClasspath = project.getClassLoader().getClassPathAsFiles();

        for (int index=0; index<fileClasspath.length;index++) {
            File file = fileClasspath[index];
            
            // Skip directories.
            if ( file == null || file.isDirectory() ) continue;
            
            userlibList.add (new UserLibInfo(file));
        }
        
        if ( userlibList.size() < 1 ) return null;
        
        userLibs = (UserLibInfo[])userlibList.toArray(new UserLibInfo[userlibList.size()]);

        // Create the panel with the user libs listed
        
        JPanel panel = new JPanel(new GridLayout(0,2));

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(libsLabel),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        for(int i = 0; i < userLibs.length; i++) {
            panel.add(userLibs[i].getCheckBox());
        }
        return panel;
    }
    
    class UserLibInfo {
        private File sourceFile;
        private JCheckBox checkBox;
        
        public UserLibInfo(File source)
        {
            sourceFile = source;
            this.checkBox = new JCheckBox(sourceFile.getName(), false);
        }
        
        /**
         * Return a checkBox with this lib's name as a label.
         */
        public JCheckBox getCheckBox()
        {
            return checkBox;
        }
        
        /**
         * Return the file of this lib.
         */
        public File getFile()
        {
            return sourceFile;
        }
        
        /**
         * Tell whether this lib has been selected.
         */
        public boolean isSelected()
        {
            return checkBox.isSelected();
        }
    }
}
