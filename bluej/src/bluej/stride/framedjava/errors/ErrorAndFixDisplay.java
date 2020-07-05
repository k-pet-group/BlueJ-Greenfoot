/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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

import bluej.editor.fixes.FixDisplayManager;
import bluej.editor.fixes.FixSuggestion;
import bluej.editor.stride.CodeOverlayPane;
import bluej.editor.stride.CodeOverlayPane.WidthLimit;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

public class ErrorAndFixDisplay extends FixDisplayManager
{
    private static final Duration SHOW_DELAY = Duration.millis(400);
    /**
     * Spacing between display and node it refers to, in pixels
     */
    private static final double SPACING = 5.0;
    private final InteractionManager editor;
    private final CodeError error;
    private final ErrorFixListener slot;
    private final VBox vbox = new VBox();
    private FXPlatformRunnable cancelShow;
    private boolean showing = false;

    public boolean isShowing()
    {
        return showing;
    }

    public static interface ErrorFixListener
    {
        @OnThread(Tag.FXPlatform)
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

        TextFlow errorLabel = null;
        if (!(err instanceof DirectSlotError) || (err instanceof DirectSlotError && (((DirectSlotError) err).getItalicMessageStartIndex() == -1 || ((DirectSlotError) err).getItalicMessageEndIndex() == -1)))
        {
            errorLabel = new TextFlow(new Label(prefix + err.getMessage()));
        } else
        {
            DirectSlotError dsError = (DirectSlotError) err;
            Label beforeItalicText = (dsError.getItalicMessageStartIndex() > 0) ? new Label(dsError.getMessage().substring(0, dsError.getItalicMessageStartIndex())) : new Label("");
            Label italicText = new Label(dsError.getMessage().substring(dsError.getItalicMessageStartIndex(), dsError.getItalicMessageEndIndex()));
            JavaFXUtil.withStyleClass(italicText, "error-fix-display-italic");
            Label afterItalicText = (dsError.getItalicMessageEndIndex() < dsError.getMessage().length() - 1) ? new Label(dsError.getMessage().substring(dsError.getItalicMessageEndIndex())) : new Label("");
            errorLabel = new TextFlow(beforeItalicText, italicText, afterItalicText);
        }
        JavaFXUtil.addStyleClass(errorLabel, "error-label");
        vbox.getChildren().add(errorLabel);
        prepareFixDisplay(vbox, (List<FixSuggestion>) err.getFixSuggestions(), () -> editor.getFrameEditor().getWatcher(), err.getIdentifier());
        CodeOverlayPane.setDropShadow(vbox);
    }
    
    public CodeError getError()
    {
        return error;
    }

    @OnThread(Tag.FXPlatform)
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
            showing = true;
            recordShow(() -> editor.getFrameEditor().getWatcher());
        });
    }

    @OnThread(Tag.FXPlatform)
    public void showAbove(final Region n)
    {
        showAbove(n, SHOW_DELAY);
    }

    @OnThread(Tag.FXPlatform)
    public void showBelow(final Region n)
    {
        showBelow(n, SHOW_DELAY);
    }

    @OnThread(Tag.FXPlatform)
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
            showing = true;
            recordShow(() -> editor.getFrameEditor().getWatcher());
        });
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void hide()
    {
        if (cancelShow != null)
        {
            cancelShow.run();
            cancelShow = null;
        }
        editor.getCodeOverlayPane().removeOverlay(vbox);
        error.focusedProperty().set(false);
        showing = false;
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void postFixError()
    {
        slot.fixedError(error);
    }

    @OnThread(Tag.FXPlatform)
    public void executeSelected()
    {
        if (highlighted != -1)
        {
            recordExecute(() -> editor.getFrameEditor().getWatcher(), highlighted);
            error.getFixSuggestions().get(highlighted).execute();
            ErrorAndFixDisplay.this.hide();
            slot.fixedError(error);
        }
    }
}

