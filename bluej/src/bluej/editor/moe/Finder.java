/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import bluej.Config;
import bluej.utility.EscapeDialog;

/**
 * The Finder class implements the find and replace functionality of the Moe editor.
 * It provides both the user interface dialogue and the high level implementation
 * of the find and replace functionality.
 *
 * @author  Michael Kolling
 * @author  Bruce Quig
 * @version $Id: Finder.java 6163 2009-02-19 18:09:55Z polle $
 */

public class Finder extends EscapeDialog
    implements ActionListener, DocumentListener
{
    static final String title = Config.getString("editor.find.title");
    static final String findLabel = Config.getString("editor.find.find.label");
    static final String replaceLabel = Config.getString("editor.find.replace.label");

    // -------- CONSTANTS --------

    // search direction for the finder
    static final String UP = "up"; 
    static final String DOWN = "down";
  
    // -------- INSTANCE VARIABLES --------

    private boolean searchFound;	// true if last find was successfull
    private boolean replacing;

    private JButton findButton;
    private JButton replaceButton;
    private JButton replaceAllButton;
    private JButton cancelButton;
    private JTextField searchField;
    private JTextField replaceField;
    private JCheckBox wholeWord;
    private JCheckBox ignoreCase;
    private ButtonGroup directionButtons;

    private MoeEditor editor;

    // ------------- METHODS --------------

    public Finder()
    {
        super((Frame)null, title, true);
        searchFound = true;
        makeDialog();
    }

    /**
     * Ask the user for input of search details via a dialogue.
     *
     */
    public void show(MoeEditor currentEditor, String selection, boolean replace)
    {
        editor = currentEditor;
        replacing = replace;
        getRootPane().setDefaultButton(findButton);

        if(selection != null && selection.length() > 0) {
            setSearchString(selection);
            replaceButton.setEnabled(true);
        }
        else
            replaceButton.setEnabled(false);

        if(!replacing)
            replaceField.setText("");

        searchField.selectAll();
        searchField.requestFocus();

        setVisible(true);
    }

    /**
     * search for the next instance of a text string
     */
    private void find()
    {
        searchFound = editor.findString(getSearchString(), getSearchBack(), 
                                        getIgnoreCase(), getWholeWord(), !searchFound);
        replaceButton.setEnabled(searchFound);
        if(searchFound && replacing)
            getRootPane().setDefaultButton(replaceButton);
    }

    /**
     * replaces selected text with the contents of the replaceField and return
     * next instance of the searchString.
     */
    private void replace()
    {
        String replaceText = smartFormat(editor.getSelectedText(), replaceField.getText());
        editor.insertText(replaceText, getSearchBack());
        find();
    }

    /**
     * Replace all instances of the search String with a replacement.
     * -check for valid search criteria
     * - TODO: get initial cursor pos
     * -start at beginning
     * -do initial find
     * -replace until not found, no wrapping!
     * -print out number of replacements (?)
     * -TODO: return cursor/caret to original place
     */
    private void replaceAll()
    {
        String searchString = getSearchString();
        String replaceString = replaceField.getText();

        int count = 0;
        if(getSearchBack()) {
            while(editor.doFindBackward(searchString, getIgnoreCase(), getWholeWord(), false)) {
                editor.insertText(smartFormat(editor.getSelectedText(), replaceString), true);
                count++;
            }
        }
        else {
            while(editor.doFind(searchString, getIgnoreCase(), getWholeWord(), false)) {
                editor.insertText(smartFormat(editor.getSelectedText(), replaceString), false);
                count++;
            }
        }
        if(count > 0)
            //editor.writeMessage("Replaced " + count + " instances of " + searchString);
        	editor.writeMessage(Config.getString("editor.replaceAll.replaced") +
        		 count + Config.getString("editor.replaceAll.intancesOf") + 
				 searchString);
        else
            //editor.writeMessage("String " + searchString + " not found. Nothing replaced.");
        	editor.writeMessage(Config.getString("editor.replaceAll.string") + 
        			searchString + Config.getString("editor.replaceAll.notFoundNothingReplaced"));
    }

    /**
     * Replace the text currently selected in the editor with
     */
    private String smartFormat(String original, String replacement)
    {
        if(original == null || replacement == null)
            return replacement;

        // only do smart stuff if search and replace strings were entered in lowercase.
        // check here. if not lowercase, just return.

        String search = getSearchString();
        if( !isLowerCase(replacement) || !isLowerCase(search))
            return replacement;

        if(isUpperCase(original))
            return replacement.toUpperCase();
        if(isTitleCase(original))
            return Character.toTitleCase(replacement.charAt(0)) + 
                replacement.substring(1);
        else
            return replacement;
    }
       
    /**
     * True if the string is in lower case.
     */
    public boolean isLowerCase(String s)
    {
        for(int i=0; i<s.length(); i++) {
            if(! Character.isLowerCase(s.charAt(i)))
                return false;
        }
        return true;
    }

    /**
     * True if the string is in Upper case.
     */
    public boolean isUpperCase(String s)
    {
        for(int i=0; i<s.length(); i++) {
            if(! Character.isUpperCase(s.charAt(i)))
                return false;
        }
        return true;
    }

    /**
     * True if the string is in title case.
     */
    public boolean isTitleCase(String s)
    {
        if(s.length() < 2)
            return false;
        return Character.isUpperCase(s.charAt(0)) &&
            Character.isLowerCase(s.charAt(1));
    }

    /**
     * set the search string
     */
    public void setSearchString(String s)
    {
        searchField.setText(s);
    }
     
    /**
     * return the last search string
     */
    public String getSearchString()
    {
        return searchField.getText();
    }

    /**
     * return true if the current search direction is backward
     */
    private boolean getSearchBack()
    {
        return directionButtons.getSelection().getActionCommand() == UP;
    }

    /**
     * return true if "Ignore case" search is selected
     */
    public boolean getIgnoreCase()
    {
        return ignoreCase.isSelected();
    }

    /**
     * return true if "whole word" search is selected
     */
    public boolean getWholeWord()
    {
        return wholeWord.isSelected();
    }

    /**
     * set last search found
     */
    public void setSearchFound(boolean found)
    {
        searchFound = found;
    }

    /**
     * return info whether the last search was successful
     */
    public boolean getSearchFound()
    {
        return searchFound;
    }

    // === Actionlistener interface ===
    /**
     * A button was pressed. Find out which one and do the appropriate
     * thing.
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if(src == findButton)
            find();
        else if(src == replaceButton)
            replace();
        else if(src == replaceAllButton)
            replaceAll();
        else if(src == cancelButton)
            setVisible(false);
    }

    // === Documentlistener interface ===
    /**
     * The search text was changed.
     */
    public void changedUpdate(DocumentEvent evt) { }

    public void insertUpdate(DocumentEvent evt) 
    {
        findButton.setEnabled(true);
        replaceAllButton.setEnabled(true);
    }

    public void removeUpdate(DocumentEvent evt) 
    {
        if(getSearchString().length() == 0) {
            findButton.setEnabled(false);
            replaceAllButton.setEnabled(false);
        }
    }

    private  void makeDialog()
    {
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent E) {
                    setVisible(false);
                }
            });
        
        // add search and replace text fields with labels

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(10,20,20,20));
        {
            JPanel findPanel = new JPanel(new BorderLayout());
            findPanel.add(new JLabel(findLabel), BorderLayout.WEST);
            searchField = new JTextField(16);
            searchField.getDocument().addDocumentListener(this);
            findPanel.add(searchField, BorderLayout.CENTER);
            textPanel.add(findPanel);

            textPanel.add(Box.createVerticalStrut(6));

            JPanel replacePanel = new JPanel(new BorderLayout());
            replacePanel.add(new JLabel(replaceLabel), BorderLayout.WEST);
            replaceField = new JTextField(16);
            replacePanel.add(replaceField, BorderLayout.CENTER);
            textPanel.add(replacePanel);

            textPanel.add(Box.createVerticalStrut(6));

            Box togglesBox = new Box(BoxLayout.X_AXIS);
            {
                Box optionBox = new Box(BoxLayout.Y_AXIS);
                {
                    ignoreCase = new JCheckBox(Config.getString("editor.find.ignoreCase"), true);
                    optionBox.add(ignoreCase);
                    optionBox.add(Box.createVerticalStrut(6));
                    wholeWord = new JCheckBox(Config.getString("editor.find.wholeWord"));
                    optionBox.add(wholeWord);
                }
                togglesBox.add(optionBox);

                Box directionBox = new Box(BoxLayout.Y_AXIS);
                {
                    directionButtons = new ButtonGroup();
                    JToggleButton dirUp = new JRadioButton(Config.getString("editor.find.up"));
                    dirUp.setActionCommand(UP);
                    directionButtons.add(dirUp);
                    directionBox.add(dirUp);
                    directionBox.add(Box.createVerticalStrut(6));
                    JToggleButton dirDown = new JRadioButton(Config.getString("editor.find.down"), true);
                    dirDown.setActionCommand(DOWN);
                    directionButtons.add(dirDown);
                    directionBox.add(dirDown);
                }
                togglesBox.add(directionBox);
            }
            textPanel.add(togglesBox);
        }
        getContentPane().add(textPanel, BorderLayout.CENTER);

        // add buttons

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        findButton = new JButton(Config.getString("editor.find.findNext"));
        findButton.setEnabled(false);
        buttonPanel.add(findButton);
        findButton.addActionListener(this);
   
        replaceButton = new JButton(Config.getString("editor.find.replace"));
        buttonPanel.add(replaceButton);
        replaceButton.setEnabled(false);
        replaceButton.addActionListener(this);
   
        replaceAllButton = new JButton(Config.getString("editor.find.replaceAll"));
        replaceAllButton.setEnabled(false);
        buttonPanel.add(replaceAllButton);
        replaceAllButton.addActionListener(this);
   
        cancelButton = new JButton(Config.getString("close"));
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(this);
        getContentPane().add("East", buttonPanel);

        pack();
    }

}
