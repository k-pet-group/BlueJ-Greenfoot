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

import java.io.File;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.dialog.DialogPaneAnimateError;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A dialog shown when the user was asked to select a destination to save/create a project, but did not
 * select an empty directory.
 *
 * If the user asks to select again, the File parameter passed to the constructor will be returned,
 * otherwise a new File will be returned which is either non-existent, or an existing empty directory.
 */
@OnThread(Tag.FXPlatform)
// package-visible
class NonEmptyDirectoryDialog
{
    private final Dialog<File> dialog;
    private final Label errorLabel;
    private final TextField subDirName;
    private final DialogPaneAnimateError dialogPane;

    /**
     *
     * @param parent The parent window for showing the dialog
     * @param originalChoice The original problematic selection of the user.
     */
    public NonEmptyDirectoryDialog(Window parent, File originalChoice)
    {
        this.dialog = new Dialog<>();
        dialog.initOwner(parent);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(Config.getString("nonEmptyDirectory.title"));
        this.errorLabel = new Label("");
        JavaFXUtil.addStyleClass(errorLabel, "dialog-error-label");
        ToggleGroup group = new ToggleGroup();
        RadioButton makeSubDir = new RadioButton(Config.getString("nonEmptyDirectory.makeSubDir"));
        dialogPane = new DialogPaneAnimateError(errorLabel, () -> updateErrorState(originalChoice, group.getSelectedToggle() == makeSubDir));
        dialog.setDialogPane(dialogPane);

        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        Config.addDialogStylesheets(dialog.getDialogPane());
        VBox content = new VBox(new Label(Config.getString("nonEmptyDirectory.message")));
        JavaFXUtil.addStyleClass(content, "non-empty-directory");
        content.setFillWidth(true);
        RadioButton chooseAgain = new RadioButton(Config.getString("nonEmptyDirectory.chooseAgain"));
        chooseAgain.setToggleGroup(group);
        makeSubDir.setToggleGroup(group);
        this.subDirName = new TextField();
        subDirName.disableProperty().bind(makeSubDir.selectedProperty().not());
        makeSubDir.setSelected(true);
        // Disable to begin with because there's no content, but don't show an error yet:
        dialogPane.getOKButton().setDisable(true);
        Label parentPath = new Label(originalChoice.getAbsolutePath() + File.separator);
        JavaFXUtil.addStyleClass(parentPath, "parent-path");

        HBox hBox = new HBox(parentPath, subDirName);
        JavaFXUtil.addStyleClass(hBox, "subDirRow");
        content.getChildren().addAll(makeSubDir, hBox, chooseAgain, errorLabel);
        dialog.getDialogPane().setContent(content);
        JavaFXUtil.addChangeListener(subDirName.textProperty(), t -> updateErrorState(originalChoice, group.getSelectedToggle() == makeSubDir));
        JavaFXUtil.addChangeListener(group.selectedToggleProperty(), t -> updateErrorState(originalChoice, group.getSelectedToggle() == makeSubDir));

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK)
            {
                if (group.getSelectedToggle() == chooseAgain)
                    return originalChoice;
                else
                    return new File(originalChoice, subDirName.getText().trim());

            }
            else
                return null;
        });
    }

    private void updateErrorState(File parent, boolean makingSubDir)
    {
        Button okButton = dialogPane.getOKButton();
        // If the selected option is to choose again, blank error and enable OK:
        if (!makingSubDir)
        {
            okButton.setDisable(false);
            errorLabel.setText("");
        }
        else
        {
            // If they want to make subdirectory, needs to be non-blank and either non-existent, or existent empty dir
            // (We don't have to allow existent empty dir, but seems punitive to complain about it given it would work...)
            String subDir = subDirName.getText().trim();
            if (subDir.isEmpty())
            {
                okButton.setDisable(true);
                errorLabel.setText(Config.getString("nonEmptyDirectory.blank"));
            }
            else
            {
                File proposedDir = new File(parent, subDir);
                if (proposedDir.exists())
                {
                    if (proposedDir.isDirectory())
                    {
                        if (proposedDir.list().length == 0)
                        {
                            // Exists but is empty, allow it:
                            okButton.setDisable(false);
                            errorLabel.setText("");
                        }
                        else
                        {
                            // Exists and non-empty
                            okButton.setDisable(true);
                            errorLabel.setText(Config.getString("nonEmptyDirectory.nonEmptyDirExists"));
                        }
                    }
                    else
                    {
                        okButton.setDisable(true);
                        errorLabel.setText(Config.getString("nonEmptyDirectory.fileExists"));
                    }
                }
                else
                {
                    // Doesn't exist, so fine:
                    okButton.setDisable(false);
                    errorLabel.setText("");
                }
            }

        }
    }

    public File showAndWait()
    {
        return dialog.showAndWait().orElse(null);
    }

}
