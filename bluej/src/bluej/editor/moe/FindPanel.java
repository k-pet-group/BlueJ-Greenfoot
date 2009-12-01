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
package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;


public class FindPanel extends JPanel implements ActionListener, KeyListener {

    private MoeEditor editor;

    private JPanel body;
    private DBox findBody;
    private JPanel closeBody;
    private JLabel findLabel;
    private JButton closeButton;
    private JTextField findTField;
    private JButton previousButton;
    private JButton nextButton;
    private JButton replaceWithButton;
    private JCheckBox matchCaseCheckBox;
    //private JCheckBox highlightAllBox;
    //private BasicArrowButton prevArrowButton;
    //private BasicArrowButton nextArrowButton;

    private final static String CLOSE_BUTTON_NAME ="closeBtn";
    private final static String INPUT_QUERY_NAME ="queryText";
    private final static String PREVIOUS_BUTTON_NAME ="prevBtn";
    private final static String NEXT__BUTTON_NAME ="nextBtn";
    private final static String REPLACE_WITH_BUTTON_NAME ="replaceWithBtn";
    private final static String MATCHCASE_CHECKBOX="matchCaseCheckBox";

    private String searchString=""; 
    private static Font findFont;

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel() {
        super();
        findFont=new Font(PrefMgr.getStandardFont().getFontName(), PrefMgr.getStandardFont().getSize(), PrefMgr.getStandardFont().getSize());
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));

        initDisplay();

        setFindDisplay();
        setCaseCheckDisplay();
        setCloseDisplay();
        setPrevNextDisplay();
        setReplaceDisplay();

        addDisplayElements();     
    }

    /**
     * Initialise the structure for the display panel
     */
    private void initDisplay()
    {
        body = new JPanel(new BorderLayout()); // one row, many columns
        body.setBackground(MoeEditor.infoColor);
        //body.setBorder(new EmptyBorder(3,6,3,4));
        body.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
        body.setName("FindPanelBody");

        //findOptions=new JPanel(new GridLayout(1, 7));
        findBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        findBody.setName("FindPanelFindBody");

        closeBody=new JPanel(new GridLayout(1,5));
        closeBody.setBackground(MoeEditor.infoColor);
        closeBody.setName("FindPanelFindBody");
    }

    /**
     * Initialise find buttons and labels
     */
    private void setFindDisplay()
    {
        findLabel = new JLabel();
        findLabel.setText("Find: ");
        findLabel.setFont(findFont);

        findTField=new JTextField(10);
        findTField.setFont(findFont);
        setSearchString("");
        findTField.addKeyListener(this);
        findTField.setName(INPUT_QUERY_NAME);

    }

    /**
     * Initialise the previous and next buttons
     */
    private void setPrevNextDisplay()
    {
        previousButton=new JButton();
        previousButton.addActionListener(this);
        previousButton.setName(PREVIOUS_BUTTON_NAME);
        previousButton.setText("Prev");
        previousButton.setEnabled(false);
        previousButton.setFont(findFont);

        //        prevArrowButton=new BasicArrowButton(BasicArrowButton.WEST);
        //        prevArrowButton.addActionListener(this);
        //        prevArrowButton.setName(PREVIOUS_BUTTON_NAME);
        //        prevArrowButton.setText("Prev");
        //        prevArrowButton.setEnabled(false);

        nextButton=new JButton();
        nextButton.addActionListener(this);
        nextButton.setName(NEXT__BUTTON_NAME);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
        nextButton.setFont(findFont);

        //        nextArrowButton=new BasicArrowButton(BasicArrowButton.EAST);
        //        nextArrowButton.addActionListener(this);
        //        nextArrowButton.setName(NEXT__BUTTON_NAME);
        //        nextArrowButton.setText("Next");
        //        nextArrowButton.setEnabled(false);

    }

    /**
     * Initialise the check case check box
     */
    private void setCaseCheckDisplay()
    {
        matchCaseCheckBox=new JCheckBox();
        matchCaseCheckBox.setText("Match Case");
        matchCaseCheckBox.setSelected(false);
        matchCaseCheckBox.setFont(findFont);
        matchCaseCheckBox.setName(MATCHCASE_CHECKBOX);
        matchCaseCheckBox.addActionListener(this);

    }

    /**
     * Returns true if the case should be matched
     */
    public boolean getMatchCase(){
        return matchCaseCheckBox.isSelected();
    }
    /**
     * Initialise the close button
     */
    private void setCloseDisplay()
    {
        closeButton=new JButton();
        closeButton.setText("Done");
        closeButton.addActionListener(this);
        closeButton.setName(CLOSE_BUTTON_NAME); 
        closeButton.setFont(findFont);

    }

    /**
     * Initialise the buttons and labels for replace functionality
     */
    private void setReplaceDisplay()
    {       
        replaceWithButton=new JButton();
        replaceWithButton.addActionListener(this);
        replaceWithButton.setName(REPLACE_WITH_BUTTON_NAME);
        replaceWithButton.setText("Replace");
        replaceWithButton.setEnabled(true);
        replaceWithButton.setFont(findFont);
    }

    /**
     * Adds the different elements into the display
     */
    private void addDisplayElements()
    {
        findBody.add(findLabel);
        findBody.add(findTField);

        findBody.add(previousButton);
        findBody.add(nextButton);
        findBody.add(replaceWithButton);
        findBody.add(matchCaseCheckBox);
        //findBody.addSpacer(500);
        //findBody.add(closeButton);

        closeBody.add(closeButton);

        body.add(findBody, BorderLayout.WEST);
        body.add(closeBody, BorderLayout.EAST);

        add(body);
    }

    /**
     * Performs the required action dependent on source of action event 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) 
    {
        JComponent src = (JComponent) e.getSource();
        if(src.getName() == CLOSE_BUTTON_NAME){
            editor.removeHighlighting();
            editor.setSelText(null);
            editor.moveCaretPosition(editor.getSelectionBegin().getColumn());
            this.setVisible(false);
            return;
        }
        if (src.getName()==NEXT__BUTTON_NAME){  
            getNext();
        }
        if (src.getName()==PREVIOUS_BUTTON_NAME){
            getPrev();   
        }
        if (src.getName()==REPLACE_WITH_BUTTON_NAME){
            replace();
        }
        if (src.getName()==INPUT_QUERY_NAME){
            find(true);
        }
        if (src.getName()==MATCHCASE_CHECKBOX){
            //editor.setCaretPositionForward(-getSearchString().length());
            find(true);
        }
    }

    public void getNext()
    {
        //move the caret forward ONLY if the search string is the same
        if (getSearchString()!=null && getSearchString().equals(findTField.getText())){
            editor.moveCaretPosition(editor.getCaretPosition()+getSearchString().length());
        }
        find(true);  
    }

    public void getPrev(){
        //editor.removeHighlighting();
        //editor.moveCaretPosition(editor.getCaretPosition()-getSearchString().length());
        find(false);
    }

    /** 
     * Handle the key-pressed event from the text field. 
     */
    public void keyPressed(KeyEvent e) {

    }

    /**
     * Handle the key-released event from the text field. 
     */
    public void keyReleased(KeyEvent e) 
    {
        boolean doFind=false;
        JComponent src = (JComponent) e.getSource();
        if (src.getName()== INPUT_QUERY_NAME){
            JTextField findT=(JTextField)src;
            //check there has been a legitimate change in the search criteria            
            if (getSearchString()!=null){
                //previous search had a value and this search is empty
                //need to remove highlighting and have no message
                if (findT.getText().length()==0){
                    editor.removeHighlighting();
                    writeMessage(false);
                }
                else if (!getSearchString().equals(findT.getText()))
                    doFind=true;
            }
            //if it is the first letter of the search
            if (((JTextField)src).getText().length()>0)
                doFind=true;
            if (doFind){
                find(true);
            }
        }
    }

    /**
     * Display or remove the visibility of the find panel 
     */
    public void displayFindPanel(String selection, boolean visible)
    { 
        if (!visible){
            this.setVisible(false);
            return;
        }
        setSearchString(selection);
        updateDisplay();
        this.setVisible(true);
        findTField.requestFocus();

    }

    /**
     * Returns whether the panel is visible 
     */
    public boolean isVisibleFindPanel()
    { 
        return this.isVisible();
    }

    public void setEditor(MoeEditor editor) 
    {
        this.editor = editor;
    }

    public String getSearchString() 
    {
        return searchString;
    }

    /**
     * Sets the text in the textfield and resets the searchString to the new text
     * @param searchString
     */
    public void setSearchString(String searchString) 
    {
        findTField.setText(searchString);
        this.searchString = searchString;
    }

    /**
     * Enable (previous and next) buttons if there is a valid search string.
     * If not these buttons should be disabled
     */
    private void updateDisplay()
    {
        boolean validSearch=false;
        if(getSearchString() != null && getSearchString().length() > 0) 
            validSearch=true;

        if (validSearch){
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);
            //prevArrowButton.setEnabled(true);
            //nextArrowButton.setEnabled(true);
        }
        else{
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            //prevArrowButton.setEnabled(false);
            //nextArrowButton.setEnabled(false);
        }
        findTField.requestFocus();
    }

    /**
     * When the editor finds a value it needs to reset the caret to before the search string
     * This ensures that the find will continue to find including what has already 
     * just been found. This is required as partial finds are done.
     * @param src JTextField is the source and focus is reset there and searchQuery is reset
     */
    private void setFindValues()
    {
        //now get and reset fields
        setSearchString(findTField.getText());
        findTField.requestFocus(); 
        //if the search is empty should it reset the caret position to the start
        //of the doc? so that it can start searching from there
        if (getSearchString().length()==0){

        }
    }

    /**
     * Replaces selected text with the contents of the replaceField and return
     * next instance of the searchString.
     */
    private void replace()
    {
        editor.replace();
    }

    /**
     *  HighlightAll instances of the search String with a replacement.
     * -reset number of finds to 0
     * -search forward or backward depending on choice
     * -print out number of highlights 
     */
    public void highlightAll(boolean ignoreCase, boolean wholeWord, boolean wrap, boolean next)
    {
        searchForward(ignoreCase, wholeWord, wrap, next);
        writeMessage(true); 
    }

    /**
     * writeMessage either writes an empty message or a message reflecting the number of founds
     * @param emptyMessage
     */
    private void writeMessage(boolean emptyMessage){
        if (!emptyMessage){
            editor.writeMessage(" ");
            return;
        }
        int counter=editor.getNumHighlights();
        if(counter > 0){
            if (editor.getSelectedText()!=null){
                //move the caret to the beginning of the selected item
                //editor.moveCaretPosition(editor.getCaretPosition()-getSearchString().length());
            }
            editor.writeMessage(Config.getString("editor.highlight.found") +
                    counter + Config.getString("editor.replaceAll.intancesOf") + 
                    getSearchString());
        }
        else{
            //only write msg if there was a search string
            if (counter<1 && getSearchString().length()>0) {               
                editor.writeMessage(Config.getString("editor.replaceAll.string") + 
                        getSearchString() + Config.getString("editor.highlight.notFound"));

            }
        }
    }

    /**
     * search initiates a backward search if required and a forward 
     * search if it has not been found, does one last effort 
     * by resetting the  caret to 0 and searching from there
     * @param ignoreCase
     * @param wholeWord
     * @param wrap
     * @param select
     * @param next
     * @return
     */
    private boolean search (boolean ignoreCase, boolean wholeWord, boolean wrap, boolean select, boolean next)
    {
        String searchString = getSearchString();  
        if (searchString.length()==0)
            return true;

        boolean found =false;
        if (!next){
            editor.doFindBackward(searchString, ignoreCase, wholeWord, wrap);
            //editor.doFindBackward(searchString, ignoreCase, wholeWord, wrap);
            editor.moveCaretPosition(editor.getCaretPosition()-searchString.length());
        }

        found=editor.doFindSelect(searchString, ignoreCase, wholeWord, wrap, select);
        if (!found){
            editor.moveCaretPosition(0);
            found=editor.doFindSelect(searchString, ignoreCase, wholeWord, wrap, select);
        }
        return found;
    }

    private void searchForward(boolean ignoreCase, boolean wholeWord, boolean wrap, boolean next)
    {
        boolean select=true;
        search(ignoreCase, wholeWord, wrap, select, next) ;
    }

    /**
     * Find requires the display to be reset (i.e button enabled/disabled) and calling the editor to find
     */
    protected void find(boolean next)
    {
        if (getSearchString()!=null)
            editor.moveCaretPosition(editor.getCaretPosition()-getSearchString().length());
        setFindValues(); 
        updateDisplay();
        editor.removeHighlighting();
        highlightAll(!matchCaseCheckBox.isSelected(), false, true, next);
    }

    public void keyTyped(KeyEvent e) {

    }
}
