/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
import bluej.parser.SourceLocation;
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
public class FindPanel extends JPanel implements ActionListener, DocumentListener, MouseListener
{
    private MoeEditor editor;

    private JComponent findBody;
    private DBox findTextBody;
    private DBox optionsBody;
    private JPanel mcBody;
    private JPanel closeBody;
    private DBox findLabelBox;
    private JLabel replaceLabel;
    private JTextField findTField;
    private JButton previousButton;
    private JButton nextButton;
    private JCheckBox matchCaseCheckBox;
    private JLabel replaceIconLabel;
    private JLabel closeIconLabel;

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

    private SourceLocation searchStart;

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel(MoeEditor ed)
    {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0));
        openIcon=Config.getImageAsIcon("image.replace.open");
        closedIcon=Config.getImageAsIcon("image.replace.close");
        findFont=PrefMgr.getStandardFont();

        editor=ed;
        initDisplay();

        setFindDisplay();
        setCaseCheckDisplay();
        setCloseDisplay();
        setPrevNextDisplay();
        setReplaceDisplay();

        addDisplayElements();  

        findTField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e)
            {
                if (searchStart == null) {
                    searchStart = editor.getCaretLocation();
                }
            }

            public void focusLost(FocusEvent e)
            {
                searchStart = null;
            }
        });

    }

    @Override
    public void setVisible(boolean aFlag)
    {
        if (aFlag && ! isVisible()) {
            // Remember the current caret location so we can revert to it if
            // the search term cannot be found.
            searchStart = editor.getSelectionBegin();
            if (searchStart == null) {
                searchStart = editor.getCaretLocation();
            }
        }
        super.setVisible(aFlag);
        if (aFlag) {
            findTField.requestFocus();
        }
    }

    /**
     * Get the maximum and preferred width of the "find:" label.
     */
    public int getLabelBoxWidth()
    {
        return findLabelBox.getPreferredSize().width;
    }

    /**
     * Initialise the structure for the display panel
     */
    private void initDisplay()
    {
        findBody = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.componentSpacingLarge, 0.5f);
        findTextBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
        //prev, next
        optionsBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
        mcBody=new DBox(DBoxLayout.X_AXIS, 0, 0, 0.5f);;

        closeBody=new JPanel(new BorderLayout());       
    }

    /**
     * Initialise find buttons and labels
     */
    private void setFindDisplay()
    {
        JLabel findLabel = new JLabel(Config.getString("editor.findpanel.findLabel"));
        findLabel.setFont(findFont);

        replaceLabel = new JLabel(Config.getString("editor.replacePanel.replaceLabel"));
        replaceLabel.setFont(findFont);

        Dimension lblSize = findLabel.getPreferredSize();
        lblSize.width = Math.max(lblSize.width, replaceLabel.getPreferredSize().width);

        findLabelBox = new DBox(DBox.X_AXIS, 0.5f);
        findLabelBox.setMaximumSize(lblSize);
        findLabelBox.setPreferredSize(lblSize);
        findLabelBox.add(Box.createHorizontalGlue());
        findLabelBox.add(findLabel);

        findTField=new JTextField(11);
        findTField.setMaximumSize(findTField.getPreferredSize());
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
        previousButton.setText(Config.getString("editor.findpanel.findPrevious")+" ");
        previousButton.setEnabled(false);
        previousButton.setFont(findFont);

        nextButton=new JButton();
        nextButton.addActionListener(this);
        nextButton.setName(NEXT__BUTTON_NAME);
        nextButton.setText(Config.getString("editor.findpanel.findNext"));
        nextButton.setEnabled(false);
        nextButton.setFont(findFont);

        if (Config.isMacOS()) {
            previousButton.putClientProperty("JButton.buttonType", "segmentedCapsule");
            previousButton.putClientProperty("JButton.segmentPosition", "first");
            nextButton.putClientProperty("JButton.buttonType", "segmentedCapsule");
            nextButton.putClientProperty("JButton.segmentPosition", "last");
        }
    }

    /**
     * Initialise the check case check box
     */
    private void setCaseCheckDisplay()
    {
        matchCaseCheckBox=new JCheckBox();
        matchCaseCheckBox.setText(Config.getString("editor.findpanel.matchCase"));
        matchCaseCheckBox.setSelected(false);
        matchCaseCheckBox.setFont(findFont);
        matchCaseCheckBox.setName(MATCHCASE_CHECKBOX);
        matchCaseCheckBox.addActionListener(this);
    }

    /**
     * Returns true if the case should be matched
     */
    public boolean getMatchCase()
    {
        return matchCaseCheckBox.isSelected();
    }

    /**
     * Initialise the close button
     */
    private void setCloseDisplay()
    {
        closeIconLabel=new JLabel();
        closeIconLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 10));
        closeIconLabel.setIcon(Config.getImageAsIcon("image.findpanel.close")); 
        closeIconLabel.addMouseListener(this);
        closeIconLabel.setName(CLOSE_BUTTON_NAME); 
    }

    /**
     * Initialise the buttons and labels for replace functionality
     */
    private void setReplaceDisplay()
    { 
        replaceIconLabel=new JLabel(Config.getString("editor.findpanel.replacePanel"));
        replaceIconLabel.setFont(findFont);
        replaceIconLabel.setIcon(closedIcon);
        replaceIconLabel.addMouseListener(this);
    }

    /**
     * Adds the different elements into the display
     */
    private void addDisplayElements()
    {
        findTextBody.add(findLabelBox);
        findTextBody.add(findTField);

        if (Config.isMacOS()) {
            DBox buttonBox = new DBox(DBoxLayout.X_AXIS, 0.5f);
            buttonBox.add(previousButton);
            buttonBox.add(nextButton);
            optionsBody.add(buttonBox);
        }
        else {
            optionsBody.add(previousButton);
            optionsBody.add(nextButton);
        }

        closeBody.add(closeIconLabel, BorderLayout.EAST);

        mcBody.add(matchCaseCheckBox);
        mcBody.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingLarge * 2));
        mcBody.add(replaceIconLabel);

        findBody.add(findTextBody);
        findBody.add(optionsBody);
        findBody.add(mcBody);

        add(findBody, BorderLayout.WEST);
        add(closeBody, BorderLayout.EAST);

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

        if (src.getName()==NEXT__BUTTON_NAME){  
            getNext();
        }
        if (src.getName()==PREVIOUS_BUTTON_NAME){
            getPrev();   
        }
        if (src.getName()==REPLACE_CLOSE_BUTTON_NAME){
            editor.toggleReplacePanelVisible();
        }
        if (src.getName()==REPLACE_OPEN_BUTTON_NAME){
            editor.toggleReplacePanelVisible();
        }
        if (src.getName()==MATCHCASE_CHECKBOX){
            if (searchStart != null) {
                editor.setCaretLocation(searchStart);
            }
            find(true);
        }
    }

    /**
     * Search forwards
     */
    public void getNext()
    {
        SourceLocation selBegin = editor.getSelectionBegin();
        if (selBegin != null) {
            // Avoid finding the same instance we found last time
            editor.moveCaretPosition(editor.getOffsetFromLineColumn(selBegin) + 1);
        }
        if (find(true)) {
            searchStart = editor.getSelectionBegin();
        }
    }

    /**
     * Search backwards
     */
    public void getPrev()
    {
        SourceLocation selEnd = editor.getSelectionEnd();
        if (selEnd != null) {
            int offset = Math.max(editor.getOffsetFromLineColumn(selEnd) - 1, 0);
            editor.setCaretLocation(editor.getLineColumnFromOffset(offset));
        }
        if (find(false)) {
            searchStart = editor.getSelectionEnd();
        }
    }

    /**
     * Finds a instance of the search string (forward, from the current selection beginning),
     * writes a message and moves the caret as required.
     */
    private void findEvent()
    {
        SourceLocation selBegin = editor.getSelectionBegin();
        if (selBegin == null) {
            selBegin = editor.getCaretLocation();
        }

        //check there has been a legitimate change in the search criteria            
        if (getSearchString()!=null){
            //previous search had a value and this search is empty
            //need to remove highlighting and have no message
            if (findTField.getText().length()==0) {
                //need to reset the search to the beginning of the last selected
                editor.removeSearchHighlights();
                setSearchString(null);
                editor.setCaretLocation(selBegin);
                writeMessage(false, 0);
                return;
            }
        }

        editor.setCaretLocation(selBegin);
        boolean found = find(true);
        if (!found && searchStart != null) {
            // If nothing found, caret should be moved back to position it was in before
            // the search started.
            editor.setCaretLocation(searchStart);
        }
        updateDisplay(found);
    }

    /**
     * Display or remove the visibility of the find panel 
     */
    public void displayFindPanel(String selection, boolean visible)
    {    
        if (selection==null)
            selection=getSearchString();       
        setSearchString(selection);
        this.setVisible(true);
        populateFindTextfield(selection);
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
    private void updateDisplay(boolean enable)
    {
        previousButton.setEnabled(enable);
        nextButton.setEnabled(enable); 
        editor.enableReplaceButtons(enable); 
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
    }

    /**
     *  HighlightAll instances of the search String with a replacement.
     * -reset number of finds to 0
     * -search forward or backward depending on choice
     * -print out number of highlights 
     */
    public boolean highlightAll(boolean ignoreCase, boolean wholeWord, boolean forwards, boolean select)
    {
        SourceLocation caretLoc = forwards ? editor.getSelectionBegin() : editor.getSelectionEnd();
        if (caretLoc == null) {
            caretLoc = editor.getCaretLocation();
        }

        editor.setCaretLocation(caretLoc);
        search(ignoreCase, wholeWord, true, select, forwards) ;
        int counter=editor.getNumHighlights();
        //if there was nothing found, need to move the caret back to its original position
        //need also disable buttons accordingly
        if (counter<1) {
            editor.setCaretLocation(caretLoc);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            editor.enableReplaceButtons(false); 
        }
        writeMessage(true, counter); 
        return counter != 0;
    }

    /**
     * Display either writes an empty message or a message reflecting the
     * number of occurrences found
     */
    private void writeMessage(boolean emptyMessage, int counter)
    {
        if (!emptyMessage){
            editor.writeMessage(" ");
            return;
        }

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
     * Search either forwards or backwards for the search string, highlighting all occurrences.
     * If no occurrences are found, the caret position is lost.
     */
    private boolean search(boolean ignoreCase, boolean wholeWord, boolean wrap, boolean select, boolean next)
    {
        String searchString = getSearchString();  
        if (searchString.length()==0)
            return true;

        boolean found =false;
        if (!next){
            found = editor.doFindBackward(searchString, ignoreCase, wholeWord, wrap);
            // position the caret so that following doFindSelect finds the correct occurrence
            editor.moveCaretPosition(editor.getCaretPosition()-searchString.length());
        }

        found=editor.doFindSelect(searchString, ignoreCase, wholeWord, wrap, select);
        return found;
    }

    /**
     * Find the current search string in either the forwards or backwards direection, 
     * highlighting all occurrences, and selecting the first found occurrence.
     */
    protected boolean find(boolean forward)
    {
        setFindValues(); 
        editor.removeSearchHighlights();
        return highlightAll(!matchCaseCheckBox.isSelected(), false, forward, true);
    }

    public String getSearchTextfield()
    {
        return findTField.getText();   
    }

    public void changedUpdate(DocumentEvent e) {  }

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
        if (findTField.getText().length()==0) {
            //need to reset the search to the beginning of the last selected
            editor.removeSearchHighlights();
            setSearchString(null);
            if (searchStart != null) {
                editor.setCaretLocation(searchStart);
            }
            writeMessage(false, 0);
            updateDisplay(false);
        }
        else {
            findEvent();
        }
    }

    public void setfindTextField(String selection)
    {
        findTField.setText(selection);
    }


    public void mouseClicked(MouseEvent e)
    {
        JComponent src = (JComponent) e.getSource();
        if(src.getName() == CLOSE_BUTTON_NAME){
            close();
            return;
        }
        editor.toggleReplacePanelVisible();
        if (replaceIconLabel.getIcon()==openIcon){
            replaceIconLabel.setIcon(closedIcon);
        }
        else if (replaceIconLabel.getIcon()==closedIcon){
            replaceIconLabel.setIcon(openIcon);
        }
    }

    public void mouseEntered(MouseEvent e) {  }

    public void mouseExited(MouseEvent e) {  }

    public void mousePressed(MouseEvent e) {  }

    public void mouseReleased(MouseEvent e) {  }

    public void setTextfieldSelected()
    {
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

    /**
     * Removes the highlights and sets the find and replace panel to invisible
     * Also resets the replace Icon to closed
     */
    public void close()
    {
        editor.removeSearchHighlights();
        this.setVisible(false);
        editor.setReplacePanelVisible(false);
        replaceIconLabel.setIcon(closedIcon);
    }

    /**
     * 
     * @return text field for search text
     */
    protected JTextField getFindTField() 
    {
        return findTField;
    } 
    
    /**
     * Puts the focus in the find field
     */
    protected void requestFindfieldFocus()
    {
        findTField.requestFocus();
    }
    
    /**
     * Populates the field and puts the focus in the text field
     */
    protected void populateFindTextfield(String selection){
        setfindTextField(selection); 
        findTField.selectAll();
        findTField.requestFocus();
    }

}