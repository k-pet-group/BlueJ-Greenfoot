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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

/**
 * The FindPanel class implements the find functionality of the MoeEditor.
 * It provides both the user interface panel and the high level implementation
 * of the find functionality.It also is a link to the replace panel.
 *
 * @author  Marion Zalk
 */

public class FindPanel extends JPanel implements ActionListener, DocumentListener, MouseListener {

    private MoeEditor editor;

    private JPanel body;
    private JPanel findBody;
    private DBox findTextBody;
    private DBox optionsBody;
    private JPanel otherBody;
    private JPanel mcBody;
    private JPanel closeBody;
    private JLabel findLabel;
    private JButton closeButton;
    private JTextField findTField;
    private JButton previousButton;
    private JButton nextButton;
    private JCheckBox matchCaseCheckBox;
    private JLabel replaceLabel;
    private JLabel replaceIconLabel;

    private final static String CLOSE_BUTTON_NAME ="closeBtn";
    private final static String INPUT_QUERY_NAME ="queryText";
    private final static String PREVIOUS_BUTTON_NAME ="prevBtn";
    private final static String NEXT__BUTTON_NAME ="nextBtn";
    private final static String MATCHCASE_CHECKBOX="matchCaseCheckBox";   
    private final static String REPLACE_OPEN_BUTTON_NAME ="replaceOpen";
    private final static String REPLACE_CLOSE_BUTTON_NAME ="replaceClose";

    private String searchString=""; 
    private static Font findFont;
    private ImageIcon openIcon;
    private ImageIcon closedIcon;

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel(MoeEditor ed) {
        super();
        openIcon=Config.getImageAsIcon("image.replace.open");
        closedIcon=Config.getImageAsIcon("image.replace.close");
        findFont=new Font(PrefMgr.getStandardFont().getFontName(), PrefMgr.getStandardFont().getSize(), PrefMgr.getStandardFont().getSize());
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));

        editor=ed;
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
        //body = new JPanel(new BorderLayout()); // one row, many columns
        body = new JPanel(new GridLayout(1, 2));
        body.setBackground(MoeEditor.infoColor);
        //body.setBorder(new EmptyBorder(3,6,3,4));
        body.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
        body.setName("FindPanelBody");

        //findOptions=new JPanel(new GridLayout(1, 7));
        //findBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        findBody=new JPanel(new GridLayout(1, 2));
        findTextBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        optionsBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);

        otherBody=new JPanel (new GridLayout(1, 2));
        mcBody=new JPanel(new GridLayout(1, 1));
        closeBody=new JPanel(new GridLayout(1,2));
        closeBody.setBackground(MoeEditor.infoColor);    
    }



    /**
     * Initialise find buttons and labels
     */
    private void setFindDisplay()
    {
        findLabel = new JLabel();
        findLabel.setText("Find:       ");
        findLabel.setFont(findFont);

        findTField=new JTextField(10);
        findTField.setFont(findFont);
        setSearchString("");
        setfindTextField("");
        findTField.setName(INPUT_QUERY_NAME);
        findTField.getDocument().addDocumentListener(this);
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

        nextButton=new JButton();
        nextButton.addActionListener(this);
        nextButton.setName(NEXT__BUTTON_NAME);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
        nextButton.setFont(findFont);
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
        replaceLabel=new JLabel(" Replace");
        replaceLabel.setFont(findFont);

        replaceIconLabel=new JLabel("  Replace  ");
        replaceIconLabel.setFont(findFont);
        replaceIconLabel.setIcon(closedIcon);
        replaceIconLabel.addMouseListener(this);
    }

    /**
     * Adds the different elements into the display
     */
    private void addDisplayElements()
    {
        findTextBody.add(findLabel);
        findTextBody.add(findTField);
        optionsBody.add(previousButton);
        optionsBody.add(nextButton);

        findBody.add(findTextBody, BorderLayout.WEST);
        findBody.add(optionsBody, BorderLayout.EAST);
        closeBody.add(replaceIconLabel);
        closeBody.add(closeButton);

        mcBody.add(matchCaseCheckBox);
        otherBody.add(mcBody);
        otherBody.add(closeBody);

        body.add(findBody, BorderLayout.WEST);
        body.add(otherBody, BorderLayout.EAST);    
        this.add(body);

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke ,"escapeAction");
        this.getActionMap().put("escapeAction", new AbstractAction(){ //$NON-NLS-1$
            public void actionPerformed(ActionEvent e)
            {
                close();
            }
        });

    }

    /**
     * Performs the required action dependent on source of action event 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) 
    {
        JComponent src = (JComponent) e.getSource();
        if(src.getName() == CLOSE_BUTTON_NAME){
            close();
            return;
        }
        if (src.getName()==NEXT__BUTTON_NAME){  
            getNext();
        }
        if (src.getName()==PREVIOUS_BUTTON_NAME){
            getPrev();   
        }
        if (src.getName()==REPLACE_CLOSE_BUTTON_NAME){
            enableReplace();
        }
        if (src.getName()==REPLACE_OPEN_BUTTON_NAME){
            enableReplace();
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
        if (getSearchString()!=null){
            editor.moveCaretPosition(editor.getCaretPosition()+getSearchString().length());
        }
        find(false);
    }

    private void findEvent()
    {
        boolean doFind=false;
        int caretPos=editor.getCaretPosition();
        //check there has been a legitimate change in the search criteria            
        if (getSearchString()!=null){
            //previous search had a value and this search is empty
            //need to remove highlighting and have no message
            if (findTField.getText().length()==0){
                caretPos=caretPos-getSearchString().length();
                //need to reset the search to the beginning of the last selected
                editor.removeSelectionHighlights();
                setSearchString(null);
                editor.moveCaretPosition(caretPos);
                writeMessage(false);
            }
            else if (!getSearchString().equals(findTField.getText()))
                doFind=true;
        }
        //if it is the first letter of the search
        if (findTField.getText().length()>0)
            doFind=true;
        if (doFind){
            editor.moveCaretPosition(caretPos);
            find(true);
        }
    }

    /**
     * Display or remove the visibility of the find panel 
     */
    public void displayFindPanel(String selection, boolean visible)
    { 
        boolean selectFindText=false;
        //if it is already visible and the request is to become 
        //visible just set the focus in the text field
        if (this.isVisible() && visible){
            findTField.requestFocus();
            return;
        }
        if (selection==null)
            selection=getSearchString();
        else   {      //need to highlight the text in the find field as it is from the editor
             selectFindText=true;
        }       
        setSearchString(selection);
        updateDisplay();
        this.setVisible(true);
        setfindTextField(selection); //this triggers a find
        if (selectFindText){
            //findTField.selectAll();
            editor.setSelectionVisible();
        }
        findTField.requestFocus();

    }

    /**
     * Returns whether the panel is visible 
     */
    public boolean isVisibleFindPanel()
    { 
        return this.isVisible();
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
            //update display in the replace panel if necessary
            if (editor.isReplacePanelVisible())
                // if (editor.isReplacePopulated())
                editor.enableReplaceButtons(true); 
        }
        else{
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            editor.enableReplaceButtons(false);
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
    private void enableReplace()
    {
        editor.toggleReplacePanelVisible();
    }

    /**
     *  HighlightAll instances of the search String with a replacement.
     * -reset number of finds to 0
     * -search forward or backward depending on choice
     * -print out number of highlights 
     */
    public void highlightAll(boolean ignoreCase, boolean wholeWord, boolean wrap, boolean next, boolean select)
    {
        searchForward(ignoreCase, wholeWord, wrap, next, select);
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

    private void searchForward(boolean ignoreCase, boolean wholeWord, boolean wrap, boolean next, boolean select)
    {
        search(ignoreCase, wholeWord, wrap, select, next) ;
    }

    /**
     * Assumes that the select is true, calls find(boolean next, boolean select)
     * @param next forward/backward
     */
    protected void find(boolean next){
        find(next, true);
    }

    /**
     * Find requires the display to be reset (i.e button enabled/disabled) and calling the editor to find
     */
    protected void find(boolean next, boolean select)
    {
        if (getSearchString()!=null){
            editor.moveCaretPosition(editor.getCaretPosition()-getSearchString().length());
        }
        setFindValues(); 
        updateDisplay();
        editor.removeSelectionHighlights();
        highlightAll(!matchCaseCheckBox.isSelected(), false, true, next, select);
    }

    public String getSearchTextfield()
    {
        return findTField.getText();   
    }

    public void changedUpdate(DocumentEvent e) 
    {

    }

    /**
     * Initiates a find
     */
    public void insertUpdate(DocumentEvent e) 
    {
            findEvent();
    }

    /**
     * Initiates a find
     */
    public void removeUpdate(DocumentEvent e) 
    {
        findEvent();
        updateDisplay();
    }

    public void setfindTextField(String selection)
    {
        findTField.setText(selection);
    }


    public void mouseClicked(MouseEvent e) {
        enableReplace();
        if (replaceIconLabel.getIcon()==openIcon){
            replaceIconLabel.setIcon(closedIcon);
        }
        else if (replaceIconLabel.getIcon()==closedIcon){
            replaceIconLabel.setIcon(openIcon);
        }
    }


    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    public void setTextfieldSelected(){
        findTField.selectAll();
    }

    /**
     * setFindReplaceIcon can set the icon for the replace as being open/closed
     * @param open icon is open/closed
     */
    protected void setFindReplaceIcon(boolean open)
    {
        if (open)
            replaceIconLabel.setIcon(openIcon);
        else
            replaceIconLabel.setIcon(closedIcon);
    }


    public void close()
    {
        int caretPos=editor.getCaretPosition();
        editor.removeSelectionHighlights();
        this.setVisible(false);
        editor.toggleReplacePanelVisible();
        editor.moveCaretPosition(caretPos);
        replaceIconLabel.setIcon(closedIcon);
    }

}
