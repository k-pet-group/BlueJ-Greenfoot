package bluej.pkgmgr;

import bluej.*;
import bluej.Config;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPathEntry;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DialogManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.io.File;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for choosing options when exporting
 *
 * @author  Michael Kolling
 * @version $Id: ExportDialog.java 2900 2004-08-18 11:52:28Z mik $
 */
class ExportDialog extends JDialog
{
    // Internationalisation
    private static final String dialogTitle = Config.getString("pkgmgr.export.title");
    private static final String helpLine1 = Config.getString("pkgmgr.export.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.export.helpLine2");
    private static final String classLabelText = Config.getString("pkgmgr.export.classLabel");
    private static final String libsLabel = Config.getString("pkgmgr.export.includeLibs");
    private static final String sourceLabel = Config.getString("pkgmgr.export.sourceLabel");
    private static final String noClassText = Config.getString("pkgmgr.export.noClassText");

    private String mainClassName = "";

    private JComboBox classSelect;
    private JCheckBox sourceBox;
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
        
        for(int i = 0; i < userLibs.length; i++) {
            if(userLibs[i].isSelected())
                selected.add(userLibs[i].getFile());
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
                
                JPanel userLibPanel = createUserLibPanel();
                if(userLibPanel != null) {
                    userLibPanel.setAlignmentX(LEFT_ALIGNMENT);
                    inputPanel.add(userLibPanel);
                    inputPanel.add(Box.createVerticalStrut(5));
                }
                
                sourceBox = new JCheckBox(sourceLabel, false);
                sourceBox.setAlignmentX(LEFT_ALIGNMENT);
                inputPanel.add(sourceBox);
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
     * Return a prepared panel listing the user libraries with check boxes,
     * or null if there are no user libraries.
     */
    private JPanel createUserLibPanel()
    {
        // collect info about jar files from lib/userlib and Preferences
        
        List libs = ClassMgr.getClassMgr().getUserClassPath().getPathEntries();
        if(libs.size() == 0)
            return null;
        
        userLibs = new UserLibInfo[libs.size()];
        int idx = 0;
        for(Iterator it = libs.iterator(); it.hasNext(); ) {
            ClassPathEntry cpe = (ClassPathEntry) it.next();
            userLibs[idx++] = new UserLibInfo(cpe.getFile());
        }

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
