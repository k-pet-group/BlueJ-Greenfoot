/*
This file is part of the BlueJ program. 
Copyright (C) 1999-2010,2011,2014,2019  Michael Kolling and John Rosenberg 

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
package bluej.editor.flow;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import static javafx.scene.input.KeyCombination.SHIFT_DOWN;

/**
 * The FindPanel class implements the find functionality of the MoeEditor.
 * It provides both the user interface panel and the high level implementation
 * of the find functionality.It also is a link to the replace panel.
 *
 * @author  Marion Zalk
 * @author  Michael Kölling
 */
@OnThread(Tag.FXPlatform)
public class FindPanel extends GridPane
{
    private final TextField replaceField;
    private final FlowEditor editor;
    private final CheckBox matchCaseCheckBox;
    private final Button previousButton;
    private final Button nextButton;
    private final TextField findField;
    private FindNavigator currentNavigator;
    private final BooleanProperty findResultsFound = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showingReplace;

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel(FlowEditor ed)
    {
        this.editor = ed;
        JavaFXUtil.addStyleClass(this, "moe-find-panel");

        // Various MoeEditor calls make us invisible; take us out of the layout when that happens
        managedProperty().bind(visibleProperty());

        JavaFXUtil.addStyleClass(this, "moe-find-grid");
        //prev, next

        HBox mcBody = new HBox();

        BorderPane closeBody = new BorderPane();
        JavaFXUtil.addStyleClass(closeBody, "moe-find-close-wrapper");

        Label findLabel = new Label(Config.getString("editor.findpanel.findLabel"));
        JavaFXUtil.addStyleClass(findLabel, "moe-find-label");
        // We don't want the Find label to move when the replace pane is toggled,
        // so we want the find label to be the same size as the replace label
        // although due to translations we don't know which one is bigger.  Easiest
        // way I can think of is to have invisible copy of replace label underneath
        // find label in a stack pane, thus forcing the larger of the two sizes:
        Label replaceDummyLabel = new Label(Config.getString("editor.replacePanel.replaceLabel"));
        JavaFXUtil.addStyleClass(replaceDummyLabel, "moe-find-label");
        replaceDummyLabel.setVisible(false);
        StackPane findLabelPane = new StackPane(replaceDummyLabel, findLabel);

        Label replaceFoldOutLabel = new Label(Config.getString("editor.findpanel.replacePanel"));

        StackPane.setAlignment(findLabel, Pos.CENTER_RIGHT);

        findField = new TextField();
        JavaFXUtil.addStyleClass(findField, "moe-find-field");
        JavaFXUtil.addChangeListenerPlatform(findField.textProperty(), search -> {
            updateFindResult();
        });
        matchCaseCheckBox = new CheckBox();
        matchCaseCheckBox.setText(Config.getString("editor.findpanel.matchCase"));
        matchCaseCheckBox.setSelected(false);
        JavaFXUtil.addChangeListenerPlatform(matchCaseCheckBox.selectedProperty(), cs -> {
            updateFindResult();
        });
        Label closeIconLabel = new Label();
        closeIconLabel.setGraphic(makeCloseIcon());
        closeIconLabel.setOnMouseClicked(e -> cancelFind());

        previousButton = new Button();
        previousButton.setOnAction(e -> {
            updateFindResult();
            if (currentNavigator != null && currentNavigator.validProperty().get())
            {
                currentNavigator.selectPrev();
            }
        });
        Label prevShortcut = new Label("\u21e7\u23ce");
        previousButton.setText(Config.getString("editor.findpanel.findPrevious"));
        previousButton.setGraphic(prevShortcut);
        previousButton.setDisable(true);

        nextButton = new Button();
        nextButton.setOnAction(e -> {
            updateFindResult();
            if (currentNavigator != null && currentNavigator.validProperty().get())
            {
                currentNavigator.selectNext(false);
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

        showingReplace = new SimpleBooleanProperty(false);
        Polygon triangle = new Polygon(0, 0, 8, 5, 0, 10);
        triangle.rotateProperty().bind(Bindings.when(showingReplace).then(90).otherwise(0));
        replaceFoldOutLabel.setGraphic(triangle);
        // We must use an event filter on pressed, rather than a handler
        // on clicked, so that we intercept the click and stop it hitting the
        // background, which moves focus to the editor.
        replaceFoldOutLabel.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            showingReplace.set(!showingReplace.get());
            e.consume();
        });

        GridPane.setHalignment(closeBody, HPos.RIGHT);
        closeBody.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(closeBody, Priority.ALWAYS);
        closeBody.setCenter(closeIconLabel);
        BorderPane.setAlignment(closeIconLabel, Pos.CENTER_RIGHT);

        JavaFXUtil.addStyleClass(mcBody, "moe-find-options");
        mcBody.setAlignment(Pos.CENTER);
        matchCaseCheckBox.setAlignment(Pos.CENTER);
        replaceFoldOutLabel.setAlignment(Pos.CENTER);
        mcBody.getChildren().add(matchCaseCheckBox);
        mcBody.getChildren().add(replaceFoldOutLabel);

        Label replaceLabel = new Label(Config.getString("editor.replacePanel.replaceLabel"));
        JavaFXUtil.addStyleClass(replaceLabel, "moe-find-label");
        replaceField = new TextField();

        Nodes.addInputMap(replaceField, InputMap.sequence(
                InputMap.consume(EventPattern.keyPressed(KeyCode.ESCAPE), e -> cancelFind())
        ));

        Button replaceOne = new Button(Config.getString("editor.replacePanel.replaceOnce"));
        Button replaceAll = new Button(Config.getString("editor.replacePanel.replaceAll"));

        replaceOne.setOnAction(e -> {
            updateFindResult();
            if (currentNavigator.validProperty().get())
            {
                setCurrentNavigator(currentNavigator.replaceCurrent(replaceField.getText()));
            }
        });
        replaceAll.setOnAction(e -> {
            updateFindResult();
            if (currentNavigator.validProperty().get())
            {
                currentNavigator.replaceAll(replaceField.getText());
            }
        });

        // The find field needs to be filled to do a replace,
        // but the replace field can be empty (to remove the find string)
        replaceOne.disableProperty().bind(findField.textProperty().isEmpty().or(findResultsFound.not()));
        replaceAll.disableProperty().bind(replaceOne.disableProperty());

        add(findLabelPane, 0, 0);
        add(findField, 1, 0);
        add(replaceField, 1, 1);
        add(previousButton, 2, 0);
        add(nextButton, 3, 0);
        add(mcBody, 4, 0);
        add(closeBody, 5, 0);

        add(replaceLabel, 0, 1);
        add(replaceOne, 2, 1);
        add(replaceAll, 3, 1);

        for (Node n : new Node[] {replaceLabel, replaceField, replaceOne, replaceAll})
        {
            n.visibleProperty().bind(showingReplace);
            n.managedProperty().bind(n.visibleProperty());
        }

        mcBody.setFillHeight(true);
        for (Button b : new Button[]{previousButton, nextButton, replaceOne, replaceAll})
        {
            b.setMaxHeight(Double.MAX_VALUE);
            b.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /**
     * Makes a cross inside a circle for the close icon
     */
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

    /**
     * Hides the panel and clears the text fields.
     */
    private void cancelFind()
    {
        findField.clear();
        replaceField.clear();
        setReplaceEnabled(false);
        setVisible(false);
    }

    /**
     * Finds a instance of the search string (forward, from the current selection beginning,
     * including the possibility that the search result begins at current selection beginning).
     */
    private void updateFindResult()
    {
        setCurrentNavigator(editor.doFind(getSearchString(), !matchCaseCheckBox.isSelected()));
    }

    /**
     * Updates the search state based on the given FindNavigator object.
     *
     * This occurs either when the search term changes, or a replacement has occurred
     * (which basically triggers a new find).
     *
     * All results will be highlighted, and if there are any results, the first will be selected.
     */
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
            currentNavigator.selectNext(true);
            previousButton.disableProperty().bind(findResultsFound.not());
            nextButton.disableProperty().bind(findResultsFound.not());
            findResultsFound.set(true);
        }
    }

    /**
     * Display the find panel and initiate a search. If the selection is null the search
     * the previous search String is used (if there is a previous search) 
     */
    public void displayFindPanel(String selection)
    {
        if (selection == null)
        {
            selection = getSearchString();
        }
        this.setVisible(true);
        populateFindTextfield(selection);
    }

    public String getSearchString()
    {
        return findField.getText();
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
     * Removes the highlights and sets the find and replace panel to invisible
     * Also resets the replace icon to closed
     */
    public void close()
    {
        editor.removeSearchHighlights();
        this.setVisible(false);
        editor.getSourcePane().requestFocus();
    }

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
        showingReplace.set(isEnabled);
    }
}
