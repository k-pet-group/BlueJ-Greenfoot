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

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;

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
    private static final ButtonType OPEN_BUTTON = ButtonType.NEXT;
    private final ListView<File> subDirList;

    private static enum Choice { CANCEL, CHOOSE_AGAIN, SELECTED_FILE }
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
        Config.addDialogStylesheets(dialog.getDialogPane());
        VBox content = new VBox(new Label(Config.getString(labelRoot + ".message") + "\n    " + original.getAbsolutePath()));
        JavaFXUtil.addStyleClass(content, "not-a-project");
        content.setFillWidth(true);
        if (possibilities != null && !possibilities.isEmpty())
        {
            dialog.getDialogPane().getButtonTypes().add(OPEN_BUTTON);
            Button openButton = (Button) dialog.getDialogPane().lookupButton(OPEN_BUTTON);
            openButton.setText(Config.getString(labelRoot + ".subDirButton"));
            content.getChildren().add(new Label(Config.getString(labelRoot + ".subDirs")));
            subDirList = new ListView<>(FXCollections.observableArrayList(possibilities));
            subDirList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            subDirList.setEditable(false);
            subDirList.setCellFactory(v -> {
                ListCell<File> cell = new TextFieldListCell<>(new StringConverter<File>()
                {
                    @Override
                    public String toString(File object)
                    {
                        return object.getAbsolutePath();
                    }

                    @Override
                    public File fromString(String string)
                    {
                        throw new UnsupportedOperationException();
                    }
                });
                cell.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY)
                    {
                        subDirList.getSelectionModel().select(cell.getItem());
                        openButton.fire();
                    }
                });
                return cell;
            });
            // From http://stackoverflow.com/questions/17429508/how-do-you-get-javafx-listview-to-be-the-height-of-its-items
            subDirList.prefHeightProperty().set(possibilities.size() * 26 + 20);

            subDirList.setMaxHeight(300.0);
            openButton.disableProperty().bind(subDirList.getSelectionModel().selectedItemProperty().isNull());
            JavaFXUtil.addStyleClass(subDirList, "subDirs");
            content.getChildren().add(subDirList);
        }
        else
            subDirList = null;
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.OK)).setText(Config.getString(labelRoot + ".button"));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK)
                return new ChoiceAndFile(Choice.CHOOSE_AGAIN, null);
            //else if (button == ButtonType.NO)
                //return new ChoiceAndFile(Choice.IMPORT, original);
            else if (button == OPEN_BUTTON)
                return new ChoiceAndFile(Choice.SELECTED_FILE, subDirList.getSelectionModel().getSelectedItem());
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

    public File getSelectedDir()
    {
        return selected.file;
    }
}
