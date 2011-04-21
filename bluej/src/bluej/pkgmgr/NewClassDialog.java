/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011  Michael Kolling and John Rosenberg 
 
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
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;

import bluej.*;
import bluej.utility.*;

/**
 * Dialog for creating a new class
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 */
class NewClassDialog extends EscapeDialog
{
    private JTextField textFld;
    ButtonGroup templateButtons;

    private String newClassName = "";
    private boolean ok;   // result: which button?
    private static List<String> windowsRestrictedWords;  //stores restricted windows class filenames

    /**
     * Construct a NewClassDialog
     */
    public NewClassDialog(JFrame parent, boolean isJavaMEpackage)
    {
        super(parent, Config.getString("pkgmgr.newClass.title"), true);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                ok = false;
                setVisible(false);
            }
        });

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel newclassTag = new JLabel(Config.getString("pkgmgr.newClass.label"));
            {
                newclassTag.setAlignmentX(LEFT_ALIGNMENT);
            }

            textFld = new JTextField(24);
            {
                textFld.setAlignmentX(LEFT_ALIGNMENT);
            }

            mainPanel.add(newclassTag);
            mainPanel.add(textFld);
            mainPanel.add(Box.createVerticalStrut(5));

            JPanel choicePanel = new JPanel();
            {
                choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
                choicePanel.setAlignmentX(LEFT_ALIGNMENT);

                //create compound border empty border outside of a titled border
                Border b = BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(Config.getString("pkgmgr.newClass.classType")),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10));

                choicePanel.setBorder(b);

                addClassTypeButtons(choicePanel, isJavaMEpackage);
            }

            choicePanel.setMaximumSize(new Dimension(textFld.getMaximumSize().width,
                                                     choicePanel.getMaximumSize().height));
            choicePanel.setPreferredSize(new Dimension(textFld.getPreferredSize().width,
                                                       choicePanel.getPreferredSize().height));

            mainPanel.add(choicePanel);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton okButton = BlueJTheme.getOkButton();
                {
                    okButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent evt)
                        {
                            doOK();
                        }
                    });
                }

                JButton cancelButton = BlueJTheme.getCancelButton();
                {
                    cancelButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent evt)
                        {
                            doCancel();
                        }
                    });
                }

                DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);
                
                getRootPane().setDefaultButton(okButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    /**
     * Add the class type buttons (defining the class template to be used
     * to the panel. The templates are defined in the "defs" file.
     */
    private void addClassTypeButtons(JPanel panel, boolean isJavaMEpackage)
    {
        String templateSuffix = ".tmpl";
        int suffixLength = templateSuffix.length();

        // first, get templates out of defined templates from bluej.defs
        // (we do this rather than usign the directory only to be able to
        // force an order on the templates.)

        String templateString = Config.getPropString("bluej.classTemplates");

        StringTokenizer t = new StringTokenizer(templateString);
        List<String> templates = new ArrayList<String>();

        while (t.hasMoreTokens()) {
            templates.add(t.nextToken());
        }

        // next, get templates from files in template directory and
        // merge them in

        File templateDir = Config.getClassTemplateDir();
        if(!templateDir.exists()) {
            DialogManager.showError(this, "error-no-templates");
        }
        else {
            String[] files = templateDir.list();
            
            for(int i=0; i < files.length; i++) {
                if(files[i].endsWith(templateSuffix)) {
                    String template = files[i].substring(0, files[i].length() - suffixLength);
                    if(!templates.contains(template))
                        templates.add(template);
                }
            }
        }        
       
        // In Java ME packages disallow the creation of enum, unittest, and applet
        // classes. In SE packages disallow the creation of midlets.
        if ( isJavaMEpackage ) {
            templates.remove( "enum"     );
            templates.remove( "unittest" );  
            templates.remove( "appletj"  );            
         }
         else {            
            templates.remove( "midlet" );             
         }
       
        // Create a radio button for each template found
        JRadioButton button;
        JRadioButton previousButton = null;
        templateButtons = new ButtonGroup();

        for(Iterator<String> i=templates.iterator(); i.hasNext(); ) {
            String template = i.next();
            String label = Config.getString("pkgmgr.newClass." + template, template);
            button = new JRadioButton(label, (previousButton==null));  // enable first
            button.setActionCommand(template);
            templateButtons.add(button);
            panel.add(button);
            previousButton = button;
        }
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        textFld.requestFocus();
        setVisible(true);  // modal - we sit here until closed
        return ok;
    }

    public String getClassName()
    {
        return newClassName;
    }

    public String getTemplateName()
    {
        return templateButtons.getSelection().getActionCommand();
    }

    /**
     * Close action when OK is pressed.
     */
    public void doOK()
    {
        newClassName = textFld.getText().trim();
        initialiseRestrictedWordList();
        if (JavaNames.isIdentifier(newClassName) && 
                !(isWindowsRestrictedWord(newClassName))) {
            ok = true;
            setVisible(false);
        }
        else 
        {
            if (isWindowsRestrictedWord(newClassName)) {
                DialogManager.showError((JFrame)this.getParent(), "windows-reserved-class-name");
            }
            else {
                DialogManager.showError((JFrame)this.getParent(), "invalid-class-name");            
            }
            textFld.selectAll();
            textFld.requestFocus();
        }

    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
        ok = false;
        setVisible(false);
    }
    
    /**
     * Tests for restricted class names (case insensitive)
     * @param fileName potential class name
     * @return true if restricted word
     */
    private boolean isWindowsRestrictedWord(String fileName)
    {
        if (windowsRestrictedWords.contains(fileName.toUpperCase())){
            return true;
        }
        return false;
    }
    
    /**
     * Initialises the list of restricted words
     */
    private void initialiseRestrictedWordList()
    {
        if (windowsRestrictedWords==null){
            windowsRestrictedWords=new ArrayList<String>();
            windowsRestrictedWords.add("CON");
            windowsRestrictedWords.add("PRN");
            windowsRestrictedWords.add("AUX");
            windowsRestrictedWords.add("NUL");
            windowsRestrictedWords.add("COM1");
            windowsRestrictedWords.add("COM2");
            windowsRestrictedWords.add("COM3");
            windowsRestrictedWords.add("COM4");
            windowsRestrictedWords.add("COM5");
            windowsRestrictedWords.add("COM6");
            windowsRestrictedWords.add("COM7");
            windowsRestrictedWords.add("COM8");
            windowsRestrictedWords.add("COM9");
            windowsRestrictedWords.add("LPT1");
            windowsRestrictedWords.add("LPT2");
            windowsRestrictedWords.add("LPT3");
            windowsRestrictedWords.add("LPT4");
            windowsRestrictedWords.add("LPT5");
            windowsRestrictedWords.add("LPT6");
            windowsRestrictedWords.add("LPT7");
            windowsRestrictedWords.add("LPT8");
            windowsRestrictedWords.add("LPT9");
        }
    }
}
