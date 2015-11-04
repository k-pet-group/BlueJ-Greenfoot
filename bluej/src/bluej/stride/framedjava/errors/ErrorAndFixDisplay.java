/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.errors;

import java.util.ArrayList;
import java.util.List;

import bluej.editor.stride.CodeOverlayPane.WidthLimit;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import bluej.editor.stride.CodeOverlayPane;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

public class ErrorAndFixDisplay
{
    private static final Duration SHOW_DELAY = Duration.millis(400);
    /** Spacing between display and node it refers to, in pixels */
    private static final double SPACING = 5.0;
    private final InteractionManager editor;
    private final CodeError error;
    private final ErrorFixListener slot;
    private final VBox vbox = new VBox();
    private final List<FixDisplay> fixes = new ArrayList<>();
    private int highlighted = -1; // Offset into error.getFixSuggestions()
    private FXRunnable cancelShow;
    
    public static interface ErrorFixListener
    {
        public void fixedError(CodeError err);
    }
    
    public ErrorAndFixDisplay(InteractionManager editor, CodeError err, ErrorFixListener slot)
    {
        this(editor, "", err, slot);
    }
    
    public ErrorAndFixDisplay(InteractionManager editor, String prefix, CodeError err, ErrorFixListener slot)
    {
        this.editor = editor;
        this.error = err;
        this.slot = slot;
        
        // We must consume the mouse pressed event to stop the focus from moving away
        // from the text slot when the mouse is clicked on us:
        vbox.setOnMousePressed(MouseEvent::consume);
        
        Label errorLabel = new Label(prefix + err.getMessage());
        JavaFXUtil.addStyleClass(errorLabel, "error-label");
        vbox.getChildren().add(errorLabel);
        
        for (FixSuggestion fix : err.getFixSuggestions())
        {
            FixDisplay l = new FixDisplay("  Fix: " + fix.getDescription());
            l.onMouseClickedProperty().set(e ->
                {
                    fix.execute();
                    ErrorAndFixDisplay.this.hide();
                    slot.fixedError(error);
                    e.consume();
                });
            l.onMouseEnteredProperty().set(e -> setHighlighted(fixes.indexOf(l)));
            vbox.getChildren().add(l);
            fixes.add(l);
        }
        
        JavaFXUtil.addStyleClass(vbox, "error-fix-display");
        vbox.setMinWidth(250.0);
        
        CodeOverlayPane.setDropShadow(vbox);
    }
    
    public CodeError getError()
    {
        return error;
    }
    
    public void showAbove(final Region n, Duration delay)
    {
        if (cancelShow != null)
        {
            cancelShow.run();
            cancelShow = null;
        }
        cancelShow = JavaFXUtil.runAfter(delay, () -> {
            editor.getCodeOverlayPane().addOverlay(vbox, n, null, vbox.heightProperty().negate().subtract(SPACING), WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT);
            // Make errors appear underneath everything else in the overlay pane, to avoid
            // them getting in the way of code completion:
            vbox.toBack();
            error.focusedProperty().set(true);
        });
    }
    
    public void showAbove(final Region n)
    {
        showAbove(n, SHOW_DELAY);
    }
    
    public void showBelow(final Region n)
    {
        showBelow(n, SHOW_DELAY);
    }
    
    public void showBelow(final Region n, Duration delay)
    {
        if (cancelShow != null)
        {
            cancelShow.run();
            cancelShow = null;
        }
        cancelShow = JavaFXUtil.runAfter(delay, () -> {
            editor.getCodeOverlayPane().addOverlay(vbox, n, null, n.heightProperty().add(SPACING), WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT);
            // Make errors appear underneath everything else in the overlay pane, to avoid
            // them getting in the way of code completion:
            vbox.toBack();
            error.focusedProperty().set(true);
        });
    }
    
    public void hide()
    {
        if (cancelShow != null)
        {
            cancelShow.run();
            cancelShow = null;
        }
        editor.getCodeOverlayPane().removeOverlay(vbox);
        error.focusedProperty().set(false);
    }
    
    public boolean hasFixes()
    {
        return !fixes.isEmpty();
    }
    
    /**
     * Called when down is pressed on the slot we are joined to.
     * Only call this if hasFixes() returns true, otherwise handle keypress differently.
     */
    public void down()
    {
        setHighlighted(Math.min(fixes.size() - 1, highlighted + 1));
    }
    
    /**
     * Called when up is pressed on the slot we are joined to.
     * Only call this if hasFixes() returns true, otherwise handle keypress differently.
     */
    public void up()
    {
        setHighlighted(Math.max(0, highlighted - 1));
    }
    
    private void setHighlighted(int newHighlight)
    {
        if (highlighted != newHighlight)
        {
            // Remove highlight from old item:
            if (highlighted != -1)
                fixes.get(highlighted).setHighlight(false);
            highlighted = newHighlight;
            // Highlight new item:
            if (highlighted != -1)
                fixes.get(highlighted).setHighlight(true);
        }
    }

    
    
    private static class FixDisplay extends HBox
    {
        private final Label enterHint = new Label("\u21B5");
        
        public FixDisplay(String display)
        {
            enterHint.setVisible(false);
            Label l = new Label(display);
            getChildren().addAll(l, enterHint);
            HBox.setHgrow(l, Priority.ALWAYS);
            l.setMaxWidth(9999);
        }
        
        private void setHighlight(boolean highlight)
        {
            if (highlight)
                JavaFXUtil.addStyleClass(this, "fix-highlight");
            else
                JavaFXUtil.removeStyleClass(this, "fix-highlight");
            
            enterHint.setVisible(highlight);
        }
    }

    public void executeSelected()
    {
        if (highlighted != -1)
        {
            error.getFixSuggestions().get(highlighted).execute();
            ErrorAndFixDisplay.this.hide();
            slot.fixedError(error);
        }
    }
}

