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
 ** @author Michael Kolling
 ** @author Bruce Quig
 **
 **/

public class Finder extends JDialog
    implements ActionListener
{
    static final String title = Config.getString("editor.find.title");
    static final String findLabel = Config.getString("editor.find.find.label");
    static final String replaceLabel = Config.getString("editor.find.replace.label");

    // -------- CONSTANTS --------

    // search direction for the finder
    static final int FORWARD = 0; 
    static final int BACKWARD = 1;
  
    // -------- INSTANCE VARIABLES --------

    protected String searchString;	// the last search string used
    protected boolean searchFound;	// true if last find was successfull
    protected int searchDirection;	// direction of search
    protected boolean cancelled;	// last dialog cancelled

    JButton findButton;
    JButton replaceButton;
    JButton replaceAllButton;
    JButton cancelButton;
    JTextField textField;
    JTextField replaceField;
    
    // ------------- METHODS --------------

    public Finder()
    {
        super((Frame)null, title, true);

        searchString = null;
        searchFound = true;
        searchDirection = FORWARD;

        makeDialog();
    }

    /**
     * set the search string
     */
    public void setSearchString(String s)
    {
        searchString = s;
    }

     
    /**
     * Ask the user for input of search details via a dialogue.
     *  Returns null if operation was cancelled.
     *
     * @param
     * @param direction  either FORWARD or BACKWARD
     */
    public String getNewSearchString(JFrame parent, int direction)
    {
        if(direction == FORWARD)
            getRootPane().setDefaultButton(findButton);
        //        if(direction == BACKWARD)
        //    getRootPane().setDefaultButton(backwardButton);

        textField.selectAll();
        textField.requestFocus();
        setVisible(true);

        // the dialog is modal, so when we get here it was closed.
        if(cancelled)
            return null;
        else
            return textField.getText();
    }

    /**
     * return the last search string
     */
    public String getLastSearchString()
    {
        return searchString;
    }

    /**
     * return the direction chosen in the last dialog
     */
    public int getDirection()
    {
        return searchDirection;
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
    public boolean lastSearchFound()
    {
        return searchFound;
    }

    /**
     * A button was pressed. Find out which one and do the appropriate
     * thing.
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if(src == findButton) {
            searchDirection = FORWARD;
            cancelled = false;
        }
//          else if(src == replaButton) {
//              searchDirection = BACKWARD;
//              cancelled = false;
//          }
        else if(src == cancelButton)
            cancelled = true;

        setVisible(false);
    }

    protected  void makeDialog()
    {
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent E) {
                    cancelled = true;
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
            textField = new JTextField(16);
            findPanel.add(textField, BorderLayout.CENTER);
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
                    JCheckBox ignoreCase = new JCheckBox("Ignore case");
                    optionBox.add(ignoreCase);
                    optionBox.add(Box.createVerticalStrut(6));
                    JCheckBox wholeWord = new JCheckBox("Whole word");
                    optionBox.add(wholeWord);
                }
                togglesBox.add(optionBox);

                Box directionBox = new Box(BoxLayout.Y_AXIS);
                {
                    JToggleButton dirUp = new JRadioButton("Search up");
                    directionBox.add(dirUp);
                    directionBox.add(Box.createVerticalStrut(6));
                    JToggleButton dirDown = new JRadioButton("Search down");
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
        buttonPanel.add(findButton);
        findButton.addActionListener(this);
   
        replaceButton = new JButton(Config.getString("editor.find.replace"));
        buttonPanel.add(replaceButton);
        replaceButton.addActionListener(this);
   
        replaceAllButton = new JButton(Config.getString("editor.find.replaceAll"));
        buttonPanel.add(replaceAllButton);
        replaceAllButton.addActionListener(this);
   
        cancelButton = new JButton(Config.getString("close"));
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(this);
        getContentPane().add("East", buttonPanel);

        pack();
    }

}  // end class Finder
