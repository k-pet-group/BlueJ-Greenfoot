/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.*;

/**
 * Dialog for exporting the project to a jar file. Here, the jar
 * creation options can be specified.
 *
 * @author  Michael Kolling
 * @version $Id: ExportDialog.java 8121 2010-08-20 04:20:13Z davmac $
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
    private UserLibInfo[] userLibs;
    
    private boolean ok;		// result: which button?
	private JPanel userLibPanel;

    public ExportDialog(PkgMgrFrame parent)
    {
        super(parent, dialogTitle, true);
        makeDialog(parent.getProject());
    }
    
    public void updateDialog(PkgMgrFrame parent)
    {
    	Project project = parent.getProject();
		fillClassPopup(project);
    	fillUserLibPanel(project, getSelectedLibs());
    	String sel = mainClassName;
    	if (sel.equals(""))
    		sel = noClassText;
    	classSelect.setSelectedItem(sel);
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
    public List<File> getSelectedLibs()
    {
        List<File> selected = new ArrayList<File>();

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
                    
                    createClassPopup();
                    fillClassPopup(project);
                    mainClassPanel.add(classSelect);
                    
                }
                mainClassPanel.setAlignmentX(LEFT_ALIGNMENT);
                inputPanel.add(mainClassPanel);
                inputPanel.add(Box.createVerticalStrut(5));
                
                {
                	createUserLibPanel();
                	fillUserLibPanel(project, null);
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

                DialogManager.addOKCancelButtons(buttonPanel, continueButton, cancelButton);

                getRootPane().setDefaultButton(continueButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

	private void createClassPopup()
	{
		classSelect = new JComboBox();
		classSelect.setFont(PrefMgr.getPopupMenuFont());
	}

    /**
     * Fill the class name popup selector with all classes of the project
     */
    private void fillClassPopup(Project project)
    {
    	classSelect.removeAllItems();
    	classSelect.addItem(noClassText);
    	
        List<String> packageNames = project.getPackageNames();
        Collections.sort(packageNames);

        for (Iterator<String> packages = packageNames.iterator(); packages.hasNext(); ) {
            String pkgName = (String)packages.next();
            // SHould be a getPackage, Damiano
            List<String> classNames = project.getPackage(pkgName).getAllClassnames();
            Collections.sort(classNames);
            if(pkgName.length() > 0) 
                for (Iterator<String> classes = classNames.iterator(); classes.hasNext();)
                	classSelect.addItem(pkgName + "." + classes.next());
            else
                for (Iterator<String> classes = classNames.iterator(); classes.hasNext();)
                	classSelect.addItem(classes.next());
        }
    }
    
    /**
     * Return a prepared panel listing the user libraries with check boxes.
     * @param project the project the libraries belong to.
     */
    private void fillUserLibPanel(Project project, List<File> startChecked)
    {
    	userLibPanel.removeAll();
    	
        // collect info about jar files from the project classloader.
        ArrayList<UserLibInfo> userlibList = new ArrayList<UserLibInfo>();
        
        // get user specified libs
        ArrayList<URL> libList = PrefMgrDialog.getInstance().getUserConfigLibPanel().getUserConfigContent(); 
        
        // also get any libs in userlib directory
        libList.addAll(Project.getUserlibContent());
        
        for (Iterator<URL> it = libList.iterator(); it.hasNext(); ) {
            URL url = (URL)it.next();
            try {
                File file = new File(new URI(url.toString()));
                
                if ( file == null || file.isDirectory() ) continue;
                
                boolean shouldBeChecked = startChecked != null && startChecked.contains(file);
                
                userlibList.add (new UserLibInfo(file, shouldBeChecked));
            } catch (URISyntaxException use) {
                // Should never happen. If there is a problem with the conversion we want to know about it.
                Debug.reportError("ExportDialog.createUserLibPanel(Project) invalid url=" + url.getPath());
            }
            // Skip directories.
            
        }
        
        if ( userlibList.size() < 1 ) { 
        	userLibPanel.setVisible(false);
        }
        else {
        	userLibPanel.setVisible(true);
            userLibs = (UserLibInfo[])userlibList.toArray(new UserLibInfo[userlibList.size()]);

            for(int i = 0; i < userLibs.length; i++) {
            	userLibPanel.add(userLibs[i].getCheckBox());
            }
        }
    }

	private void createUserLibPanel()
	{
		userLibPanel = new JPanel(new GridLayout(0,2));

		userLibPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(libsLabel),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	}
    
    class UserLibInfo {
        private File sourceFile;
        private JCheckBox checkBox;
        
        public UserLibInfo(File source, boolean selected)
        {
            sourceFile = source;
            this.checkBox = new JCheckBox(sourceFile.getName(), selected);
        }
        
        /**
         * Return a checkBox with this lib's name as a label.
         * @param shouldBeChecked 
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
