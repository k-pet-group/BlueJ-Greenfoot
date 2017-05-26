/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.stride.generic.InteractionManager;

/**
 * This is a class to make sure a TextArea always resizes to fit its content.
 *
 * In JavaFX, a TextArea always has a ScrollPane surrounding its content; this class
 * tries to make it as if the ScrollPane doesn't exist, and the TextArea is always
 * just the right size to fit its content.
 */
public class ScrollFreeTextArea
{
    // We can share this because we always operate on it in a single
    // uninterrupted procedure on the FX thread:
    private static final Scene scene = new Scene(new VBox(), 4000, 4000);
    private static Scene calculationAidScene = null;
    private static TextArea calculationAid = null;
    private static String calculationAidFontCSS = null;
    private static List<String> calculationAidStyleClass = null;
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1.0);
    // We encapsulate TextArea to make sure no-one external messes with our sizes:
    private final TextArea textArea;
    private boolean initialised = false;
    private double blankHeight;
    private double suggestedOneLineHeight;
    public ScrollFreeTextArea(InteractionManager editor)
    {
        this.textArea = new TextArea();
        // We hold a reference to an off-screen TextArea with identical content and width, which we
        // use to determine the ideal height of our TextArea.  We bind to offScreen's height
        // via the contentHeight property, below
        TextArea offScreen = new TextArea();
        SimpleDoubleProperty contentHeight = new SimpleDoubleProperty();

        // We can't snapshot textArea until it is in a Scene, so we wait until its Scene is set:
        //JavaFXUtil.addSelfRemovingListener(textArea.sceneProperty(), s -> { JavaFXUtil.addSelfRemovingListener(textArea.skinProperty(), sk -> {
        ChangeListener<Object> listener = new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
            {
                if (textArea.getScene() != null && textArea.getSkin() != null && !initialised)
                {
                    initialised = true;
                    textArea.sceneProperty().removeListener(this);
                    textArea.skinProperty().removeListener(this);

                    ScrollFreeTextArea.this.recalculateOneTwoLineHeights(editor.getFontCSS().get());
                    textArea.getStyleClass().addListener((ListChangeListener<String>) c -> ScrollFreeTextArea.this.recalculateOneTwoLineHeights(editor.getFontCSS().get()));


                    // Snapshot to make sure textArea internals are all created:
                    textArea.snapshot(null, null);


                    // Make sure the on-screen textArea never shows a scroll bar, and never lets
                    // its content be less than the available space:
                    ScrollPane p = ((ScrollPane) textArea.lookup(".scroll-pane"));
                    p.setFitToHeight(true);
                    p.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

                    // Bind our contentHeight property to be the height calculated via the offScreen
                    // TextArea:
                    contentHeight.bind(new DoubleBinding()
                    {
                        private boolean accessedInternals = false;
                        private Region content;

                        @Override
                        protected double computeValue()
                        {
                            // We have to add offScreen to a Scene, both to set the stylesheets correctly, and because
                            // snapshot doesn't work correctly unless an object is in a Scene
                            scene.setRoot(new Pane(offScreen));

                            // Instead of snapshotting, I think CSS and layout is enough:
                            //offScreen.snapshot(null, null);
                            offScreen.applyCss();
                            scene.getRoot().layout();

                            // Only need to do this part once:
                            if (!accessedInternals)
                            {
                                ((ScrollPane) offScreen.lookup(".scroll-pane")).setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                                ((ScrollPane) offScreen.lookup(".scroll-pane")).setFitToWidth(true);
                                content = (Region) offScreen.lookup(".content");
                                accessedInternals = true;
                            }
                            // We need to invalidate any caches of the previous preferred height,
                            // by calculating it for a different width:
                            double prev = content.getPrefHeight();
                            content.setPrefHeight(Region.USE_COMPUTED_SIZE);
                            content.prefHeight(content.getWidth() + 1.0);
                            // Then we can do the real calculation:
                            double r = content.prefHeight(content.getWidth());
                            content.setPrefHeight(r);
                            // Remove us from the Scene:
                            scene.setRoot(new VBox());
                            if (r <= blankHeight)
                                return scale.get() * (suggestedOneLineHeight + textArea.getPadding().getTop() + textArea.getPadding().getBottom());
                            else
                                return scale.get() * (r + textArea.getPadding().getTop() + textArea.getPadding().getBottom());
                        }

                        {
                            bind(scale);
                            bind(textArea.widthProperty());
                            bind(textProperty());
                            bind(offScreen.styleProperty());
                            bind(textArea.paddingProperty());
                        }
                    });
                }
            }
        };
        textArea.sceneProperty().addListener(listener);
        textArea.skinProperty().addListener(listener);

        textArea.setWrapText(true);
        offScreen.setWrapText(true);
        textArea.setPrefRowCount(0);
        textArea.setMinHeight(0);
        textArea.prefHeightProperty().bind(contentHeight);
        JavaFXUtil.bindList(offScreen.getStyleClass(), textArea.getStyleClass());
        offScreen.textProperty().bind(textArea.textProperty());
        offScreen.minWidthProperty().bind(offScreen.prefWidthProperty());
        offScreen.prefWidthProperty().bind(textArea.widthProperty());
        offScreen.maxWidthProperty().bind(offScreen.prefWidthProperty());

        offScreen.setPrefRowCount(0);
        offScreen.setMinHeight(0);
        offScreen.styleProperty().bind(editor.getFontCSS());
        JavaFXUtil.addChangeListener(editor.getFontCSS(), this::recalculateOneTwoLineHeights);
        
        // Given the way we are currently doing the sizes, that we bind the preferred height,
        // we run into a problem that the width may change during a layout pass, but the
        // preferred height binding is not recalculated.  To do this we must request another
        // layout.  However, requestLayout during a layout pass is a no-op, so we must actually
        // request it later.  This may result in a visible bounce on load or big change.  We could
        // maybe fix this in future by directly overriding the prefHeight(double) method of
        // TextArea, but for now, this seems to work:
        JavaFXUtil.addChangeListener(textArea.widthProperty(), w -> {
            Platform.runLater(textArea::requestLayout);
        });
    }

    private void recalculateOneTwoLineHeights(String fontSize)
    {
        // TODO should cache this
        blankHeight = calculateHeight(fontSize, "X");
        double twoLine = calculateHeight(fontSize, "X\nX");
        double threeLine = calculateHeight(fontSize, "X\nX\nX");
        double extraLine = threeLine - twoLine;
        suggestedOneLineHeight = twoLine - extraLine + 2 /* fudge factor */;
    }
    
    private double calculateHeight(String fontCSS, String text)
    {
        if (calculationAid == null || calculationAidScene == null)
        {
            calculationAid = new TextArea();
            calculationAid.setWrapText(true);
            calculationAid.setPrefRowCount(0);
            calculationAid.setMinHeight(0);
            calculationAidScene = new Scene(new Pane(calculationAid), 4000, 4000);
            Config.addEditorStylesheets(calculationAidScene);
        }
        if (!fontCSS.equals(calculationAidFontCSS))
        {
            calculationAid.setStyle(fontCSS);
            calculationAidFontCSS = fontCSS;
            calculationAid.applyCss();
        }
        if (!calculationAid.getStyleClass().equals(calculationAidStyleClass))
        {
            calculationAid.getStyleClass().setAll(textArea.getStyleClass());
            // Must take our own copy to avoid keeping reference to old node:
            calculationAidStyleClass = new ArrayList<>(textArea.getStyleClass());
            calculationAid.applyCss();
        }
        calculationAid.setText(text);
        calculationAid.layout();
        ((ScrollPane) calculationAid.lookup(".scroll-pane")).setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ((ScrollPane) calculationAid.lookup(".scroll-pane")).setFitToWidth(true);
        Region content = (Region) calculationAid.lookup(".content");
        double r = content.prefHeight(1000);
        return r;
    }

    public void setPromptText(String s)
    {
        textArea.setPromptText(s);
    }

    public String getText()
    {
        return textArea.getText();
    }

    public void setText(String value)
    {
        textArea.setText(value);
    }

    public StringProperty textProperty()
    {
        return textArea.textProperty();
    }

    public StringProperty promptTextProperty()
    {
        return textArea.promptTextProperty();
    }

    public ReadOnlyIntegerProperty caretPositionProperty()
    {
        return textArea.caretPositionProperty();
    }

    public void positionCaret(int pos)
    {
        textArea.positionCaret(pos);
    }

    public int getLength()
    {
        return textArea.getLength();
    }

    public int getCaretPosition()
    {
        return textArea.getCaretPosition();
    }

    public void selectAll()
    {
        textArea.selectAll();
    }

    public boolean isDisable()
    {
        return textArea.isDisable();
    }

    public void setDisable(boolean value)
    {
        textArea.setDisable(value);
    }

    public boolean isFocused()
    {
        return textArea.isFocused();
    }

    public ObservableBooleanValue focusedProperty()
    {
        return textArea.focusedProperty();
    }

    protected void setFocusTraversable(boolean on)
    {
        textArea.setFocusTraversable(on);
    }

    protected void addTextStyleClasses(String... styleClasses)
    {
        JavaFXUtil.addStyleClass(textArea, styleClasses);
    }

    public Node getNode()
    {
        return textArea;
    }

    public ReadOnlyDoubleProperty heightProperty()
    {
        return textArea.heightProperty();
    }

    public void setPseudoclass(String name, boolean on)
    {
        JavaFXUtil.setPseudoclass(name, on, textArea);
    }

    public <T extends Event> void addEventFilter(EventType<T> eventType, EventHandler<? super T> eventFilter)
    {
        textArea.addEventFilter(eventType, eventFilter);
    }

    public void requestFocus()
    {
        textArea.requestFocus();
    }

    public void bindPrefMaxWidth(DoubleBinding amount)
    {
        textArea.maxWidthProperty().bind(amount);
        textArea.prefWidthProperty().bind(textArea.maxWidthProperty());
    }

    public void insertAtCaret(String s)
    {
        textArea.insertText(textArea.getCaretPosition(), s);
    }

    public void shrinkToNothingUsing(SharedTransition animate)
    {
        scale.unbind();
        scale.bind(animate.getOppositeProgress());
        animate.addOnStopped(scale::unbind);
        animate.addOnStopped(() -> textArea.setVisible(false));
    }

    public void growFromNothingUsing(SharedTransition animate)
    {
        textArea.setVisible(true);
        scale.unbind();
        scale.bind(animate.getProgress());
        animate.addOnStopped(scale::unbind);
    }
    
    public Bounds getSceneBounds()
    {
        return textArea.localToScene(textArea.getBoundsInLocal());
    }

    static {
        Config.addEditorStylesheets(scene);
    }
}
