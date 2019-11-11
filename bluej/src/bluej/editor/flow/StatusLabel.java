/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2019  Michael Kolling and John Rosenberg 
 
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
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Status label for the Moe editor.
 * 
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class StatusLabel extends VBox
{
    public static enum Status
    {
        READONLY("editor.state.readOnly"),
        SAVED("editor.state.saved"),
        CHANGED("editor.state.changed");

        private final String displayText;

        private Status(String displayKey)
        {
            this.displayText = Config.getString(displayKey);
        }

        public String getDisplayText()
        {
            return displayText;
        }
    }

    private final Label statusLabel;
    private final Label errorLabel;
    private Status state;
    private int errorCount = 0;

    public StatusLabel(Status initialState, FlowEditor editor, FlowErrorManager errorManager)
    {
        JavaFXUtil.addStyleClass(this, "moe-status-label-wrapper");
        styleProperty().bind(PrefMgr.getEditorFontCSS(false));
        state = initialState;
        statusLabel = new Label();
        errorLabel = new Label();
        JavaFXUtil.addStyleClass(errorLabel, "error-count-label");
        getChildren().setAll(statusLabel, errorLabel);
        updateLabel();
        errorManager.listenForErrorChange(errs -> {
            errorCount = errs.size();
            updateLabel();
        });
        // Click on either of the labels, or background will work:
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
            {
                // We don't mean for this to function as a compile button when there are no errors:
                if (errorCount > 0)
                {
                    editor.compileOrShowNextError();
                }
                e.consume();
            }
        });
    }

    // ------------- PUBLIC METHODS ---------------

    public boolean isSaved() 
    {
        return (state != Status.CHANGED);
    }

    public boolean isChanged() 
    {
        return (state == Status.CHANGED);
    }

    public boolean isReadOnly() 
    {
        return (state == Status.READONLY);
    }

    public void setState(Status newState)
    {
        state = newState;
        updateLabel();
    }

    private void updateLabel()
    {
        statusLabel.setText(state.getDisplayText().replace("\n", ""));
        if (errorCount > 0)
        {
            errorLabel.setText("Errors: " + errorCount);
        }
        else
        {
            errorLabel.setText("");
        }
        JavaFXUtil.setPseudoclass("bj-status-error", errorCount > 0, this);
    }
}
