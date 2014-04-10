/*
This file is part of the BlueJ program. 
Copyright (C) 1999-2010,2011,2014  Michael Kolling and John Rosenberg 

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
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

/**
 * The FindPanel class implements the find functionality of the MoeEditor.
 * It provides both the user interface panel and the high level implementation
 * of the find functionality.It also is a link to the replace panel.
 *
 * @author  Marion Zalk
 * @author  Michael Kölling
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
    private String searchString = "";
    private static Font findFont;
    private ImageIcon openIcon;
    private ImageIcon closedIcon;
    private int searchStart = -1;

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel(MoeEditor ed)
    {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0));
        openIcon = Config.getFixedImageAsIcon("bluej_arrow_open.gif");
        closedIcon = Config.getFixedImageAsIcon("bluej_arrow_close.gif");
        findFont = PrefMgr.getStandardFont();

        editor = ed;
        initDisplay();

        setFindDisplay();
        setCaseCheckDisplay();
        setCloseDisplay();
        setPrevNextDisplay();
        setReplaceDisplay();

        addDisplayElements();

        findTField.addFocusListener(new FocusListener()
        {

            public void focusGained(FocusEvent e)
            {
                if (searchStart == -1) {
                    searchStart = editor.getCurrentTextPane().getCaretPosition();
                }
            }

            public void focusLost(FocusEvent e)
            {
                searchStart = -1;
            }
        });

    }

    @Override
    public void setVisible(boolean aFlag)
    {
        if (aFlag && !isVisible()) {
            // Remember the current caret location so we can revert to it if
            // the search term cannot be found.
            searchStart = editor.getCurrentTextPane().getSelectionStart();
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
        findTextBody = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
        //prev, next
        optionsBody = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
        mcBody = new DBox(DBoxLayout.X_AXIS, 0, 0, 0.5f);

        if (!Config.isRaspberryPi()) findBody.setOpaque(false);
        if (!Config.isRaspberryPi()) findTextBody.setOpaque(false);
        if (!Config.isRaspberryPi()) optionsBody.setOpaque(false);
        if (!Config.isRaspberryPi()) mcBody.setOpaque(false);

        closeBody = new JPanel(new BorderLayout());
        if (!Config.isRaspberryPi()) closeBody.setOpaque(false);
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
        findLabelBox.setOpaque(false);

        findTField = new JTextField(11);
        findTField.setMaximumSize(findTField.getPreferredSize());
        findTField.setFont(findFont);
        setSearchString("");
        setfindTextField("");
        findTField.getDocument().addDocumentListener(this);
        findTField.addActionListener(this);
    }

    /**
     * Initialise the previous and next buttons
     */
    private void setPrevNextDisplay()
    {
        previousButton = new JButton();
        previousButton.addActionListener(this);
        previousButton.setText(Config.getString("editor.findpanel.findPrevious") + " ");
        previousButton.setEnabled(false);
        previousButton.setFont(findFont);

        nextButton = new JButton();
        nextButton.addActionListener(this);
        nextButton.setText(Config.getString("editor.findpanel.findNext"));
        nextButton.setEnabled(false);
        nextButton.setFont(findFont);

        if (Config.isMacOS()) {
            previousButton.putClientProperty("JButton.buttonType", "segmentedTextured");
            previousButton.putClientProperty("JButton.segmentPosition", "first");
            nextButton.putClientProperty("JButton.buttonType", "segmentedTextured");
            nextButton.putClientProperty("JButton.segmentPosition", "last");
        }
    }

    /**
     * Initialise the check case check box
     */
    private void setCaseCheckDisplay()
    {
        matchCaseCheckBox = new JCheckBox();
        matchCaseCheckBox.setText(Config.getString("editor.findpanel.matchCase"));
        matchCaseCheckBox.setSelected(false);
        matchCaseCheckBox.setFont(findFont);
        matchCaseCheckBox.addActionListener(this);
        if (!Config.isRaspberryPi()) matchCaseCheckBox.setOpaque(false);
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
        closeIconLabel = new JLabel();
        closeIconLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 10));
        closeIconLabel.setIcon(Config.getFixedImageAsIcon("cross.png"));
        closeIconLabel.addMouseListener(this);
    }

    /**
     * Initialise the buttons and labels for replace functionality
     */
    private void setReplaceDisplay()
    {
        replaceIconLabel = new JLabel(Config.getString("editor.findpanel.replacePanel"));
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
            if (!Config.isRaspberryPi()) buttonBox.setOpaque(false);
            buttonBox.add(previousButton);
            buttonBox.add(nextButton);
            optionsBody.add(buttonBox);
        } else {
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

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "escapeAction");
        this.getActionMap().put("escapeAction", new AbstractAction()
        { //$NON-NLS-1$

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

        if (src == nextButton || src == findTField) {
            getNext();
        } else if (src == previousButton) {
            getPrev();
        } else if (src == matchCaseCheckBox) {
            editor.getCurrentTextPane().setCaretPosition(editor.getCurrentTextPane().getSelectionStart());
            find(true);
        }
    }

    /**
     * Search forwards
     */
    public void getNext()
    {
        editor.getCurrentTextPane().setCaretPosition((editor.getCurrentTextPane().getSelectionStart() + 1));
        find(true);
        editor.enableReplaceButtons();
    }

    /**
     * Search backwards
     */
    public void getPrev()
    {
        find(false);
        editor.enableReplaceButtons();
    }

    /**
     * Finds a instance of the search string (forward, from the current selection beginning),
     * writes a message and moves the caret as required.
     */
    private void findEvent()
    {
        int selBegin = editor.getCurrentTextPane().getSelectionStart();

        //check there has been a legitimate change in the search criteria            
        if (getSearchString() != null) {
            //previous search had a value and this search is empty
            //need to remove highlighting and have no message
            if (findTField.getText().length() == 0) {
                //need to reset the search to the beginning of the last selected
                editor.removeSearchHighlights();
                setSearchString(null);
                editor.getCurrentTextPane().setCaretPosition(selBegin);
                writeMessage(false, 0);
                return;
            }
        }

        editor.getCurrentTextPane().setCaretPosition(selBegin);
        boolean found = find(true);
        if (!found && searchStart != -1) {
            // If nothing found, caret should be moved back to position it was in before
            // the search started.
            editor.getCurrentTextPane().setCaretPosition(searchStart);
        }
        updateDisplay(found);
    }

    /**
     * Display the find panel and initiate a search. If the selection is null the search
     * the previous search String is used (if there is a previous search) 
     */
    public void displayFindPanel(String selection)
    {
        if (selection == null) {
            selection = getSearchString();
        }
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
    public boolean highlightAll(boolean ignoreCase, boolean forwards)
    {
        int counter = search(ignoreCase, true, forwards);
        //if there was nothing found, need to move the caret back to its original position
        //also need to disable buttons accordingly
        if (counter < 1) {
            if (searchStart != -1) {
                editor.getCurrentTextPane().setCaretPosition(searchStart);
            }
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
        if (!emptyMessage) {
            editor.writeMessage(" ");
            return;
        }

        if (counter > 0) {
            if (editor.getCurrentTextPane().getSelectedText() != null) {
                //move the caret to the beginning of the selected item
                //editor.moveCaretPosition(editor.getCaretPosition()-getSearchString().length());
            }
            editor.writeMessage(Config.getString("editor.highlight.found").trim() + " "
                    + counter + " " + Config.getString("editor.replaceAll.intancesOf").trim() + " "
                    + getSearchString());
        } else {
            //only write msg if there was a search string
            if (counter < 1 && getSearchString().length() > 0) {
                editor.writeMessage(Config.getString("editor.replaceAll.string").trim() + " "
                        + getSearchString() + " " + Config.getString("editor.highlight.notFound").trim());

            }
        }
    }

    /**
     * Search either forwards or backwards for the search string, highlighting all occurrences.
     * If no occurrences are found, the caret position is lost.
     */
    private int search(boolean ignoreCase, boolean wrap, boolean next)
    {
        String searchStr = getSearchString();
        if (searchStr.length() == 0) {
            return 0;
        }

        int found;
        if (!next) {
            editor.doFindBackward(searchStr, ignoreCase, wrap);
        } else {
            editor.doFind(searchStr, ignoreCase, wrap);
        }

        // position the caret so that following doFindSelect finds the correct occurrence
        int caretPos = editor.getCurrentTextPane().getCaretPosition();
        if (caretPos > getSearchString().length()) {
            caretPos = editor.getCurrentTextPane().getCaretPosition() - searchStr.length();
        }
        editor.getCurrentTextPane().setCaretPosition(caretPos);
        found = editor.doFindSelect(searchStr, ignoreCase, wrap);
        return found;
    }

    /**
     * Find the current search string in either the forwards or backwards direction, 
     * highlighting all occurrences, and selecting the first found occurrence.
     */
    protected boolean find(boolean forward)
    {
        setFindValues();
        editor.removeSearchHighlights();
        return highlightAll(!matchCaseCheckBox.isSelected(), forward);
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
        if (findTField.getText().length() == 0) {
            //need to reset the search to the beginning of the last selected
            editor.removeSearchHighlights();
            editor.removeSelections();
            setSearchString(null);
            if (searchStart != -1) {
                editor.getCurrentTextPane().setCaretPosition(searchStart);
            }
            writeMessage(false, 0);
            updateDisplay(false);
        } else {
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
        if (src == closeIconLabel) {
            close();
            return;
        }
        if (src == replaceIconLabel) {
            if (editor.isShowingInterface()) {
                return;
            }
            editor.toggleReplacePanelVisible();
            if (replaceIconLabel.getIcon() == openIcon) {
                replaceIconLabel.setIcon(closedIcon);
            } else if (replaceIconLabel.getIcon() == closedIcon) {
                replaceIconLabel.setIcon(openIcon);
            }
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

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
        if (open) {
            replaceIconLabel.setIcon(openIcon);
        } else {
            replaceIconLabel.setIcon(closedIcon);
        }
    }

    /**
     * Removes the highlights and sets the find and replace panel to invisible
     * Also resets the replace icon to closed
     */
    public void close()
    {
        editor.removeSearchHighlights();
        this.setVisible(false);
        editor.setReplacePanelVisible(false);
        editor.getCurrentTextPane().requestFocusInWindow();
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
    protected void populateFindTextfield(String selection)
    {
        setfindTextField(selection);
        findTField.selectAll();
        findTField.requestFocus();
    }

    /**
     * Allows the replace button to be en/disabled
     * @param isEnable true for enable; false if not
     */
    protected void setReplaceEnabled(boolean isEnabled)
    {
        replaceIconLabel.setEnabled(isEnabled);
        //if it is in documentation view (i.e disabled) the icon should be closed (even though it is disabled)
        if (!isEnabled) {
            setFindReplaceIcon(false);
        }
    }

    /**
     * Returns current search location
     * @return current search location
     */
    protected int getSearchStart()
    {
        return searchStart;
    }

    /**
     * Sets the search location to the specified value
     * @param searchStart SourceLocation specifying the beginning of the search
     */
    protected void setSearchStart(int searchStart)
    {
        this.searchStart = searchStart;
    }
}
