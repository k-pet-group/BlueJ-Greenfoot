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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.editor.moe.MoeEditor.FindNavigator;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import static javafx.scene.input.KeyCombination.SHIFT_DOWN;

/**
 * The FindPanel class implements the find functionality of the MoeEditor.
 * It provides both the user interface panel and the high level implementation
 * of the find functionality.It also is a link to the replace panel.
 *
 * @author  Marion Zalk
 * @author  Michael Kölling
 */
public class FindPanel extends BorderPane
{
    private final TextField replaceField;
    private final MoeEditor editor;
    private final CheckBox matchCaseCheckBox;
    private final Button previousButton;
    private final Button nextButton;
    private final TextField findField;
    private FindNavigator currentNavigator;
    private final BooleanProperty findResultsFound = new SimpleBooleanProperty(false);

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel(MoeEditor ed)
    {
        //MOEFX: replace these with our drawing
        ImageView openIcon = new ImageView(Config.getFixedImageAsFXImage("bluej_arrow_open.gif"));
        ImageView closedIcon = new ImageView(Config.getFixedImageAsFXImage("bluej_arrow_close.gif"));

        editor = ed;

        // Various MoeEditor calls make us invisible; take us out of the layout when that happens
        managedProperty().bind(visibleProperty());

        GridPane findBody = new GridPane();
        //prev, next
        HBox optionsBody = new HBox();
        HBox mcBody = new HBox();

        BorderPane closeBody = new BorderPane();

        Label findLabel = new Label(Config.getString("editor.findpanel.findLabel"));

        Label replaceFoldOutLabel = new Label(Config.getString("editor.replacePanel.replaceLabel"));

        findField = new TextField();
        JavaFXUtil.addStyleClass(findField, "moe-find-field");
        JavaFXUtil.addChangeListenerPlatform(findField.textProperty(), search -> {
            findEvent();
        });
        matchCaseCheckBox = new CheckBox();
        matchCaseCheckBox.setText(Config.getString("editor.findpanel.matchCase"));
        matchCaseCheckBox.setSelected(false);
        JavaFXUtil.addChangeListenerPlatform(matchCaseCheckBox.selectedProperty(), cs -> {
            findEvent();
        });
        Label closeIconLabel = new Label();
        closeIconLabel.setGraphic(makeCloseIcon());
        closeIconLabel.setOnMouseClicked(e -> cancelFind());

        previousButton = new Button();
        previousButton.setOnAction(e -> {
            if (currentNavigator != null && currentNavigator.validProperty().get())
            {
                currentNavigator.highlightPrevAsSpecial();
            }
        });
        Label prevShortcut = new Label("\u21e7\u23ce");
        previousButton.setText(Config.getString("editor.findpanel.findPrevious"));
        previousButton.setGraphic(prevShortcut);
        previousButton.setDisable(true);

        nextButton = new Button();
        nextButton.setOnAction(e -> {
            if (currentNavigator != null && currentNavigator.validProperty().get())
            {
                currentNavigator.highlightNextAsSpecial();
            }
        });
        nextButton.setText(Config.getString("editor.findpanel.findNext"));
        Label nextShortcut = new Label("\u23ce");
        nextButton.setGraphic(nextShortcut);
        nextButton.setDisable(true);

        nextShortcut.visibleProperty().bind(findField.focusedProperty());
        prevShortcut.visibleProperty().bind(findField.focusedProperty());

        Nodes.addInputMap(findField, InputMap.sequence(
            InputMap.consume(EventPattern.keyPressed(KeyCode.ESCAPE), e -> cancelFind()),
            InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> nextButton.fire()),
            InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, SHIFT_DOWN), e -> previousButton.fire())
        ));

        Label replaceIconLabel = new Label(Config.getString("editor.findpanel.replacePanel"));
        replaceIconLabel.setGraphic(closedIcon);
        //MOEFX
        //replaceIconLabel.addMouseListener(this);

        optionsBody.getChildren().add(previousButton);
        optionsBody.getChildren().add(nextButton);

        closeBody.setRight(closeIconLabel);

        mcBody.getChildren().add(matchCaseCheckBox);
        mcBody.getChildren().add(replaceIconLabel);
        mcBody.getChildren().add(replaceFoldOutLabel);

        Label replaceLabel = new Label(Config.getString("editor.replacePanel.replaceLabel"));
        replaceField = new TextField();

        Button replaceOne = new Button(Config.getString("editor.replacePanel.replaceOnce"));
        Button replaceAll = new Button(Config.getString("editor.replacePanel.replaceAll"));

        replaceOne.setOnAction(e -> {
            if (currentNavigator.validProperty().get())
            {
                setCurrentNavigator(currentNavigator.replaceCurrent(replaceField.getText()));
            }
        });

        replaceOne.disableProperty().bind(findField.textProperty().isEmpty().or(replaceField.textProperty().isEmpty()).or(findResultsFound.not()));
        replaceAll.disableProperty().bind(replaceOne.disableProperty());

        HBox replaceButtons = new HBox(replaceOne, replaceAll);

        findBody.add(findLabel, 0, 0);
        findBody.add(findField, 1, 0);
        findBody.add(optionsBody, 2, 0);
        findBody.add(mcBody, 3, 0);

        findBody.add(replaceLabel, 0, 1);
        findBody.add(replaceField, 1, 1);
        findBody.add(replaceButtons, 2, 1);

        setLeft(findBody);
        setRight(closeBody);

        //MOEFX
        /*
        findField.addFocusListener(new FocusListener()
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
        */
    }

    private static Node makeCloseIcon()
    {
        // Some of this could be moved to CSS, but the size of the circle and lines must be specified in code,
        // and various display aspects like shadow size and stroke width are dependent on size, so it makes
        // sense to have it all together in one place:
        Circle circle = new Circle(10);
        circle.setEffect(new InnerShadow(BlurType.GAUSSIAN, Color.rgb(128, 128, 128, 0.4), 2, 0.5, 1, 1));
        circle.setFill(Color.rgb(190, 190, 190, 1.0));
        Line lineA = new Line(0, 0, 7, 7);
        lineA.setStrokeWidth(3);
        lineA.setStroke(Color.rgb(240, 240, 240));
        Line lineB = new Line(0, 7, 7, 0);
        lineB.setStrokeWidth(3);
        lineB.setStroke(Color.rgb(240, 240, 240));
        StackPane stackPane = new StackPane(circle, lineA, lineB);
        // Make it a bit easier to click on by using square bounds, not the inner circle:
        stackPane.setPickOnBounds(true);
        return stackPane;
    }

    private void cancelFind()
    {
        findField.clear();
        replaceField.clear();
        setVisible(false);
    }

    /*MOEFX
    @Override
    public void setVisible(boolean aFlag)
    {
        if (aFlag && !isVisible()) {
            // Remember the current caret location so we can revert to it if
            // the search term cannot be found.
            //MOEFX
            //searchStart = editor.getCurrentTextPane().getSelectionStart();
        }
        super.setVisible(aFlag);
        if (aFlag) {
            findTField.requestFocus();
        }
    }
    */

    /**
     * Get the maximum and preferred width of the "find:" label.
     */
    public int getLabelBoxWidth()
    {
        return 0; //MOEFX
    }

    /**
     * Returns true if the case should be matched
     */
    public boolean getMatchCase()
    {
        return matchCaseCheckBox.isSelected();
    }

    /**
     * Search forwards
     */
    public void getNext()
    {
        //MOEFX
        //editor.getCurrentTextPane().setCaretPosition((editor.getCurrentTextPane().getSelectionStart() + 1));
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
        setCurrentNavigator(editor.doFind(getSearchString(), !matchCaseCheckBox.isSelected()));


        /*MOEFX
        int selBegin = editor.getCurrentTextPane().getCaretPosition();
        int searchStart = selBegin;

        //check there has been a legitimate change in the search criteria            
        if (getSearchString() != null) {
            //previous search had a value and this search is empty
            //need to remove highlighting and have no message
            if (findField.getText().length() == 0) {
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
        updateDisplay(found);*/
    }

    private void setCurrentNavigator(FindNavigator navigator)
    {
        currentNavigator = navigator;
        previousButton.disableProperty().unbind();
        nextButton.disableProperty().unbind();
        if (currentNavigator == null)
        {
            // Don't turn us red if the search string is empty:
            JavaFXUtil.setPseudoclass("bj-no-find-result", !getSearchString().isEmpty(), findField);
            previousButton.setDisable(true);
            nextButton.setDisable(true);
            findResultsFound.set(false);
        }
        else
        {

            JavaFXUtil.setPseudoclass("bj-no-find-result", false, findField);
            currentNavigator.highlightAll();
            previousButton.disableProperty().bind(currentNavigator.validProperty().not());
            nextButton.disableProperty().bind(currentNavigator.validProperty().not());
            findResultsFound.set(true);
        }
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
        return findField.getText();
    }

    /**
     * Sets the text in the textfield and resets the searchString to the new text
     * @param searchString
     */
    public void setSearchString(String searchString)
    {
        this.findField.setText(searchString);
    }

    /**
     * When the editor finds a value it needs to reset the caret to before the search string
     * This ensures that the find will continue to find including what has already 
     * just been found. This is required as partial finds are done.
     * @param src JTextField is the source and focus is reset there and searchQuery is reset
     */
    private void setFindValues()
    {
        /*MOEFX
        setSearchString(findTField.getText());
        findTField.requestFocus();
        */
    }

    /**
     *  HighlightAll instances of the search String with a replacement.
     * -reset number of finds to 0
     * -search forward or backward depending on choice
     * -print out number of highlights 
     */
    public boolean highlightAll(boolean ignoreCase, boolean forwards)
    {
        //MOEFX
        /*
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
        */
        return false;
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
            //if (editor.getCurrentTextPane().getSelectedText() != null) {
                //move the caret to the beginning of the selected item
                //editor.moveCaretPosition(editor.getCaretPosition()-getSearchString().length());
            //}
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
    //MOEFX
    /*
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
    */

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
        /*MOEFX
        if (findTField.getText().length() == 0) {
            //need to reset the search to the beginning of the last selected
            editor.removeSearchHighlights();
            setSearchString(null);
            if (searchStart != -1) {
                //MOEFX
                //editor.getCurrentTextPane().setCaretPosition(searchStart);
            }
            writeMessage(false, 0);
            updateDisplay(false);
        } else {
            findEvent();
        }
        */
    }

    /*MOEFX
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
    */

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

    /*MOEFX is this called?
    public void setTextfieldSelected()

    {
        findTField.selectAll();
    }
    */

    /**
     * setFindReplaceIcon can set the icon for the replace as being open/closed
     * @param open icon is open/closed
     */
    protected void setFindReplaceIcon(boolean open)
    {
        /*MOEFX
        if (open) {
            replaceIconLabel.setIcon(openIcon);
        } else {
            replaceIconLabel.setIcon(closedIcon);
        }
        */
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
        editor.getCurrentTextPane().requestFocus();
        //MOEFX
        //replaceIconLabel.setIcon(closedIcon);
    }

    /**
     * 
     * @return text field for search text
     */
    /*MOEFX is this called?
    protected JTextField getFindTField()
    {
        return findTField;
    }
    */

    /**
     * Puts the focus in the find field
     */
    protected void requestFindfieldFocus()
    {
        findField.requestFocus();
    }

    /**
     * Populates the field and puts the focus in the text field
     */
    protected void populateFindTextfield(String selection)
    {
        findField.setText(selection);
        findField.selectAll();
        findField.requestFocus();
    }

    /**
     * Allows the replace button to be en/disabled
     * @param isEnable true for enable; false if not
     */
    protected void setReplaceEnabled(boolean isEnabled)
    {
        /*MOEFX
        replaceIconLabel.setEnabled(isEnabled);
        //if it is in documentation view (i.e disabled) the icon should be closed (even though it is disabled)
        if (!isEnabled) {
            setFindReplaceIcon(false);
        }
        */
    }
}
