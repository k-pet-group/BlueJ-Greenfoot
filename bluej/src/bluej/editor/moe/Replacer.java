// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor.moe;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;		// all the GUI components

import bluej.Config;
import bluej.utility.Debug;

/**
 * Provides Search and Replace functionality for the Moe Text Editor
 * 
 * @author Bruce Quig
 */

public class Replacer extends JDialog

    implements ActionListener
{
    static final String title = Config.getString("editor.find.title");
    static final String textfieldLabel = Config.getString("editor.find.textfield.label");
    static final String cancel = Config.getString("cancel");
    static final String forwardText = Config.getString("editor.find.forward");
    static final String backwardText = Config.getString("editor.find.backward");
    static final String replaceFieldLabel = Config.getString("editor.find.replacefield.label");
    static final String replaceText = Config.getString("editor.find.replace");
    static final String replaceAllText = Config.getString("editor.find.replaceall");
    
    // -------- CONSTANTS --------

    // search direction for the finder
    static final int FORWARD = 0; 
    static final int BACKWARD = 1;
    static final String SEARCH = "SEARCH";
    static final String REPLACE = "REPLACE";
 

    // -------- INSTANCE VARIABLES --------

    protected String searchString;	// the last search string used
    protected String replaceString;
    protected boolean searchFound;	// true if last find was successfull
    protected int searchDirection;	// direction of search
    //protected boolean cancelled;	// last dialog cancelled

    JButton forwardButton;
    JButton backwardButton;
    JButton cancelButton;
    JButton replaceButton;
    JButton replaceAllButton;
    JTextField searchField;
    JTextField replaceField;

    MoeEditor editor;
    
    
    // ------------- METHODS --------------

    public Replacer()
    {
        super((Frame)null, title, true);
        searchString = null;
        searchFound = true;
        searchDirection = FORWARD;

        makeDialog();
    }
    
    /**
     * The method called from the editor to open the dialog
     * @param currentEditor the current text editor
     */
    public void doReplace(MoeEditor currentEditor)
    {
        editor = currentEditor;   
        
        // if(direction == FORWARD)
        getRootPane().setDefaultButton(forwardButton);
        
        searchField.selectAll();
        searchField.requestFocus();
    
        setVisible(true);
    }  

    /**
     * search for the next instance of a text string
     * 
     * @return true if an instance is found
     */
    private boolean find()
    {
        if(searchDirection == FORWARD)
            return findForward();
        else
            return findBackward();
    
    }

    /**
     * search forward for the next instance of a text string
     * 
     * @return returns true if an instance is found
     */
    private boolean findForward()
    {
        searchString = searchField.getText();
        return editor.doFind(searchString, true);
    }

    /**
     * search forward for the next instance of a text string
     * 
     * @return returns true if an instance is found
     */
    private boolean findBackward()
    {
        searchString = searchField.getText();
        return editor.doFindBackward(searchString, true);
    }
    
    /**
     * replaces selected text with the contents of the replaceField and return 
     * next instance of the searchString.
     *  
     * @return boolean for whether another instance was found
     */
    private boolean replace()
    {
        replaceString = replaceField.getText();
        editor.insertText(replaceString, false, false);
        return find();
               
    }

    /**
     * Check that a search String is neither en empty String
     * or a null String
     *
     * @param s the String to be checked
     * @return true if considered valid (length greater than 0 and not null)
     */
    private boolean verifySearchString(String s)
    {
        return (s != null && s.length() > 0);
    }
    
    /**
     * Replace all instances of the search String with a replacement.
     * -check for valid search criteria
     * -get initial cursor pos (TODO)
     * -start at beginning
     * -do initial find
     * -replace until not found, no wrapping!
     * -print out number of replacements (?)
     * -return cursor/caret to original place (TODO)
     */
    private void replaceAll()
    {
        // start at beginning of file
        editor.setSelection(1, 1, 0);
        searchString = searchField.getText();
        replaceString = replaceField.getText();
        
        int count = 0;
        while(editor.doFind(searchString, false)) {
            editor.insertText(replaceString, false, false); 
            count++;
        }
        if(count > 0)
            editor.writeMessage("Replaced " + count + " instances of " + searchString);
                
        
    }
    
    /**
     * A button was pressed. Find out which one and do the appropriate
     * thing.
     * @param evt The ActionEvent that occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if(src == forwardButton) {
            searchDirection = FORWARD;
            findForward();
        }
        else if(src == backwardButton) {
            searchDirection = BACKWARD;
            findBackward();
        }
        else if(src == replaceButton)
            replace();
        else if(src == replaceAllButton)
            replaceAll();
        else if(src == cancelButton) {
            setVisible(false);
        }
    }

    /** 
     * Set up the Dialog UI
     */
    protected  void makeDialog()
    {

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E) {
                setVisible(false);
            }
        });

        // add buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 0, 0, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        forwardButton = new JButton(forwardText);
        buttonPanel.add(forwardButton);
        forwardButton.addActionListener(this);
   
        backwardButton = new JButton(backwardText);
        buttonPanel.add(backwardButton);
        backwardButton.addActionListener(this);
   
        replaceButton = new JButton(replaceText);
        buttonPanel.add(replaceButton);
        replaceButton.addActionListener(this);
        
        replaceAllButton = new JButton(replaceAllText);
        buttonPanel.add(replaceAllButton);
        replaceAllButton.addActionListener(this);
     
        cancelButton = new JButton(cancel);
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(this);

        JPanel replaceMainPanel = new JPanel();
        replaceMainPanel.setLayout(new BorderLayout()); 
        replaceMainPanel.add(buttonPanel, BorderLayout.EAST);
        
        JPanel replacePanel = new JPanel();
        replacePanel.setBorder(BorderFactory.createEmptyBorder(10,20,20,20));
        replacePanel.setLayout(new GridLayout(0, 1));

        // add search text field
        replacePanel.add(new JLabel(textfieldLabel));
        searchField = new JTextField(16);
        replacePanel.add(searchField);
        replacePanel.add(new JPanel());
        replacePanel.add(new JLabel(replaceFieldLabel));
        replaceField = new JTextField(16);
        replacePanel.add(replaceField);
        replacePanel.add(new JPanel());
        replaceMainPanel.add(replacePanel, BorderLayout.CENTER);

        pack();
        getContentPane().add(replaceMainPanel);
 
    }
}  // end class Finder
