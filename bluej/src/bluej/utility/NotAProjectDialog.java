/*
 This file is part of the BlueJ program.
 Copyright (C) 2016  Michael Kolling and John Rosenberg

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
package bluej.utility;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.List;

/**
 * A dialog shown when the user was asked to select a project to open, but did not
 * select a directory to open.
 */
@OnThread(Tag.FXPlatform)// package-visible
class NotAProjectDialog
{
    private static enum Choice { CANCEL, CHOOSE_AGAIN, IMPORT, SELECTED_FILE }
    private static class ChoiceAndFile
    {
        private final Choice choice;
        private final File file; // Only valid if choice == SELECTED_FILE

        public ChoiceAndFile(Choice choice, File file)
        {
            this.choice = choice;
            this.file = file;
        }
    }

    private final Dialog<ChoiceAndFile> dialog;
    private ChoiceAndFile selected;

    public NotAProjectDialog(Window parent, File original, List<File> possibilities)
    {
        this.dialog = new Dialog<>();
        dialog.initOwner(parent);
        dialog.initModality(Modality.WINDOW_MODAL);
        final String labelRoot = "notAProject." + (Config.isGreenfoot() ? "greenfoot" : "bluej");
        dialog.setTitle(Config.getString(labelRoot + ".title"));
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.OK)).setText(Config.getString(labelRoot + ".button"));
        Config.addDialogStylesheets(dialog.getDialogPane());
        VBox content = new VBox(new Label(Config.getString(labelRoot + ".message") + "\n    " + original.getAbsolutePath()));
        JavaFXUtil.addStyleClass(content, "not-a-project");
        content.setFillWidth(true);
        if (possibilities != null && !possibilities.isEmpty())
        {
            content.getChildren().add(new Label(Config.getString(labelRoot + ".subDirs")));
            GridPane subDirPanel = new GridPane();
            JavaFXUtil.addStyleClass(subDirPanel, "subDirs");
            int row = 0;
            for (File f : possibilities)
            {
                subDirPanel.add(new Label(f.getAbsolutePath()), 0, row);
                Button button = new Button(Config.getString(labelRoot + ".subDirButton"));
                button.setOnAction(e -> {
                    dialog.setResult(new ChoiceAndFile(Choice.SELECTED_FILE, f));
                    dialog.close();
                });
                subDirPanel.add(button, 1, row);
                row += 1;
            }
            subDirPanel.setAlignment(Pos.CENTER);
            content.getChildren().add(new BorderPane(subDirPanel));
        }
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK)
                return new ChoiceAndFile(Choice.CHOOSE_AGAIN, null);
            else if (button == ButtonType.NO)
                return new ChoiceAndFile(Choice.IMPORT, original);
            else if (button == ButtonType.CANCEL)
                return new ChoiceAndFile(Choice.CANCEL, null);
            else
                return selected;
        });
    }

    public void showAndWait()
    {
        this.selected = dialog.showAndWait().orElse(new ChoiceAndFile(Choice.CANCEL, null));
    }

    public boolean isCancel()
    {
        return selected.choice == Choice.CANCEL;
    }

    public boolean isChooseAgain()
    {
        return selected.choice == Choice.CHOOSE_AGAIN;
    }

    public boolean isImport()
    {
        return selected.choice == Choice.IMPORT;
    }

    public File getSelectedDir()
    {
        return selected.file;
    }
}
