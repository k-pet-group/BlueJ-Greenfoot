/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2016  Michael Kolling and John Rosenberg 
 
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
import bluej.extensions.SourceType;
import bluej.utility.*;

/**
 * Dialog for creating a new class
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 */
class NewClassDialog extends EscapeDialog
{
    private final JTextField textFld;
    private final JComboBox<SourceType> languageSelectionBox;
    private final ButtonGroup templateButtons;
    private final List<TemplateInfo> templates = new ArrayList<>();

    private String newClassName = "";
    private boolean ok;   // result: which button?
    private static List<String> windowsRestrictedWords;  //stores restricted windows class filenames
    private final JButton okButton;

    /**
     * Construct a NewClassDialog
     */
    public NewClassDialog(Frame parent, Package pkg, boolean isJavaMEpackage)
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

            textFld = new JTextField(12);
            
            Box nameBox = new Box(BoxLayout.X_AXIS);
            nameBox.add(new JLabel(Config.getString("pkgmgr.newClass.label")));
            nameBox.add(textFld);
            nameBox.setAlignmentX(RIGHT_ALIGNMENT);

            mainPanel.add(nameBox);
            mainPanel.add(Box.createVerticalStrut(5));

            SourceType[] items = { SourceType.Stride, SourceType.Java };
            languageSelectionBox = new JComboBox<>(items);
            languageSelectionBox.setSelectedItem(pkg.getDefaultSourceType());
            languageSelectionBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    updateButtons((SourceType)e.getItem());
                }
            });
            languageSelectionBox.setMaximumSize(languageSelectionBox.getPreferredSize());
            
            Box langBox = new Box(BoxLayout.X_AXIS);
            langBox.add(new JLabel(Config.getString("pkgmgr.newClass.lang")));
            langBox.add(languageSelectionBox);
            langBox.setAlignmentX(RIGHT_ALIGNMENT);
            mainPanel.add(langBox);
            mainPanel.add(Box.createVerticalStrut(5));

            JPanel choicePanel = new JPanel();
            {
                choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
                choicePanel.setAlignmentX(RIGHT_ALIGNMENT);

                //create compound border empty border outside of a titled border
                Border b = BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(Config.getString("pkgmgr.newClass.classType")),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10));

                choicePanel.setBorder(b);

                templateButtons = new ButtonGroup();
                addClassTypeButtons(choicePanel, isJavaMEpackage);
            }
            choicePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            mainPanel.add(choicePanel);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(RIGHT_ALIGNMENT);

                okButton = BlueJTheme.getOkButton();
                okButton.addActionListener(evt -> doOK());

                JButton cancelButton = BlueJTheme.getCancelButton();
                cancelButton.addActionListener(evt -> doCancel());

                DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);
                getRootPane().setDefaultButton(okButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
        updateButtons((SourceType)languageSelectionBox.getSelectedItem());
    }

    public SourceType getSourceType()
    {
        return (SourceType)languageSelectionBox.getSelectedItem();
    }

    private static class TemplateInfo
    {
        private final String name;
        private final Set<SourceType> sourceTypes = new HashSet<>();
        private JRadioButton button;

        public TemplateInfo(String name)
        {
            this.name = name;
        }
        public TemplateInfo(String name, SourceType sourceType)
        {
            this.name = name;
            sourceTypes.add(sourceType);
        }

        public void setButton(JRadioButton button)
        {
            this.button = button;
        }
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

        StringTokenizer tokenizer = new StringTokenizer(templateString);

        while (tokenizer.hasMoreTokens()) {
            templates.add(new TemplateInfo(tokenizer.nextToken()));
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
                    String templateName = files[i].substring(0, files[i].length() - suffixLength);
                    SourceType sourceType = SourceType.Java;
                    if (templateName.endsWith("Stride"))
                    {
                        templateName = templateName.substring(0, templateName.length() - "Stride".length());
                        sourceType = SourceType.Stride;
                    }
                    String finalTemplateName = templateName;
                    TemplateInfo template = templates.stream().filter(t -> t.name.equals(finalTemplateName)).findFirst().orElse(null);
                    if (template != null)
                    {
                        template.sourceTypes.add(sourceType);
                    }
                    else
                    {
                        templates.add(new TemplateInfo(templateName, sourceType));
                    }
                }
            }
        }        
       
        // In Java ME packages disallow the creation of enum, unittest, and applet
        // classes. In SE packages disallow the creation of midlets.
        if ( isJavaMEpackage ) {
            templates.removeIf(t -> t.name.equals("enum"));
            templates.removeIf(t -> t.name.equals("unittest"));
            templates.removeIf(t -> t.name.equals("appletj"));
         }
         else {
            templates.removeIf(t -> t.name.equals("midlet"));
         }
       
        // Create a radio button for each template found
        JRadioButton button;
        boolean first = true;

        for (TemplateInfo template : templates)
        {
            String label = Config.getString("pkgmgr.newClass." + template.name, template.name);
            button = new JRadioButton(label, first);  // select first
            button.setActionCommand(template.name);
            template.setButton(button);
            templateButtons.add(button);
            panel.add(button);
            first = false;
            button.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    updateOKButton();
            });
        }
    }
    
    private void updateButtons(SourceType sourceType)
    {
        for (TemplateInfo template : templates)
        {
            template.button.setEnabled(template.sourceTypes.contains(sourceType));
        }
        updateOKButton();
    }
    
    private void updateOKButton()
    {
        boolean canOK = false;
        for (JRadioButton button : Utility.iterableStream(templates.stream().map(t -> t.button)))
        {
            if (button.isSelected() && button.isEnabled())
                canOK = true;
        }
        okButton.setEnabled(canOK);
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
        return templateButtons.getSelection().getActionCommand() + (languageSelectionBox.getSelectedItem() == SourceType.Stride ? "Stride" : "");
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
                DialogManager.showError(this.getParent(), "windows-reserved-class-name");
            }
            else {
                DialogManager.showError(this.getParent(), "invalid-class-name");
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
