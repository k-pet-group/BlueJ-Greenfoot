/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.generic;

import java.util.IdentityHashMap;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.editor.stride.CodeOverlayPane;
import bluej.stride.generic.InteractionManager.ShortcutKey;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

public class SuggestedFollowUpDisplay
{
    private final InteractionManager editor;
    private final FXRunnable action;
    private final VBox content = new VBox();
    // It makes sense to only allow one of these displays per editor.  So we keep track,
    // and if we try to show a second display in the same editor, we hide the existing one:
    private static final IdentityHashMap<InteractionManager, SuggestedFollowUpDisplay> displays = new IdentityHashMap<>();
    
    public SuggestedFollowUpDisplay(InteractionManager editor, String text, FXRunnable action)
    {
        this.editor = editor;
        this.action = action;
        JavaFXUtil.addStyleClass(content, "suggested-followup-pane");

        Button yes = new Button("Yes (" + Config.getKeyCodeForYesNo(ShortcutKey.YES_ANYWHERE) + ")");
        yes.setOnAction(e -> { action.run(); hide(); });

        Button no = new Button("No (" + Config.getKeyCodeForYesNo(ShortcutKey.NO_ANYWHERE) + ")");
        no.setOnAction(e -> hide());
        
        HBox hbox = new HBox(yes, no);
        JavaFXUtil.addStyleClass(hbox, "suggested-followup-hbox");
        
        content.getChildren().addAll(new Label(text), hbox);
        
        CodeOverlayPane.setDropShadow(content);
    }

    @OnThread(Tag.FXPlatform)
    public void showBefore(final Node n)
    {
        // Remove any previous display for this editor:
        if (displays.get(editor) != null)
        {
            displays.get(editor).hide(); // Will also remove it from the map
        }
        
        editor.getCodeOverlayPane().addOverlay(content, n, null, content.heightProperty().add(5.0).negate());
        // Make suggestions appear underneath everything else in the overlay pane, to avoid
        // them getting in the way of code completion:
        content.toBack();
        displays.put(editor, this);
    }

    public void hide()
    {
        JavaFXUtil.runNowOrLater(() -> editor.getCodeOverlayPane().removeOverlay(content));
        displays.remove(editor);
    }

    @OnThread(Tag.FXPlatform)
    public static void shortcutTyped(InteractionManager editor, ShortcutKey key)
    {
        SuggestedFollowUpDisplay display = displays.get(editor);
        if (display != null)
        {
            if (key == ShortcutKey.YES_ANYWHERE)
                display.action.run();
            display.hide();
        }
    }

    public static void modificationIn(InteractionManager editor)
    {
        SuggestedFollowUpDisplay display = displays.get(editor);
        if (display != null)
        {
            display.hide();
        }
    }
}
