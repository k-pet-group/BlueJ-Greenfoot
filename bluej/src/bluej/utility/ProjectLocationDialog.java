/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017,2018,2019  Michael Kolling and John Rosenberg 
 
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
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.dialog.DialogPaneAnimateError;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * A dialog shown when choosing a location to save a project to,
 * e.g. for new projects, save as or checkout.
 */
@OnThread(Tag.FXPlatform)
class ProjectLocationDialog
{
    private final Dialog<File> dialog;
    private final TextField nameField;
    private final TextField parentField;
    private final Label compoundPath;
    private final Label errorLabel = new Label();
    private final DialogPaneAnimateError dialogPane;
    private boolean dialogHasBeenEdited = false;

    public ProjectLocationDialog(Project project, Window owner, String title)
    {
        dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);
        dialogPane = new DialogPaneAnimateError(errorLabel, () -> updateOKButton(true));
        dialog.setDialogPane(dialogPane);
        Config.addDialogStylesheets(dialog.getDialogPane());
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        JavaFXUtil.addStyleClass(errorLabel, "dialog-error-label");
        GridPane gridPane = new GridPane();
        JavaFXUtil.addStyleClass(gridPane, "proj-grid");
        gridPane.add(makeLabel(Config.getString("newProject.name")), 0, 0);
        gridPane.add(makeLabel(Config.getString("newProject.parent")), 0, 1);
        gridPane.add(makeLabel(Config.getString("newProject.path")), 0, 2);
        nameField = new TextField(project != null ? project.getProjectName() + "-copy" : "");
        nameField.setPromptText(Config.getString("newProject.prompt"));
        gridPane.add(nameField, 1, 0);
        JavaFXUtil.addChangeListenerPlatform(nameField.textProperty(), s -> {dialogHasBeenEdited = true;});
        parentField = new TextField(project != null ? project.getProjectDir().getParent() : 
                PrefMgr.getProjectDirectory().getAbsolutePath());
        JavaFXUtil.addChangeListenerPlatform(parentField.textProperty(), s -> {dialogHasBeenEdited = true;});
        gridPane.add(parentField, 1, 1);
        Button chooseParent = new Button(Config.getString("newProject.parent.choose"));
        chooseParent.setOnAction(e -> {
            DirectoryChooser newChooser = new DirectoryChooser();
            newChooser.setTitle(title);
            newChooser.setInitialDirectory(new File(parentField.getText()));
            File chosen = newChooser.showDialog(dialogPane.getScene().getWindow());
            if (chosen != null)
                parentField.setText(chosen.getAbsolutePath());
        });
        chooseParent.setMinWidth(Region.USE_PREF_SIZE);
        gridPane.add(chooseParent, 2, 1);
        compoundPath = new Label();
        compoundPath.setMinWidth(250.0);
        compoundPath.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        JavaFXUtil.addStyleClass(compoundPath, "compound-path");
        compoundPath.textProperty().bind(new StringBinding()
        {
            {
                super.bind(nameField.textProperty());
                super.bind(parentField.textProperty());
            }
            @Override
            protected String computeValue()
            {
                return new File(parentField.getText(), nameField.getText()).getAbsolutePath();
            }
        });
        JavaFXUtil.addChangeListenerPlatform(compoundPath.textProperty(), x -> updateOKButton(false));
        gridPane.add(compoundPath, 1, 2);

        VBox content = new VBox(gridPane, errorLabel);
        JavaFXUtil.addStyleClass(content, "new-project-dialog");
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(new ColumnConstraints(), column2, new ColumnConstraints(), new ColumnConstraints() );
        dialogPane.setContent(content);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK)
            {
                return new File(compoundPath.getText());
            }
            else
                return null;
        });
        dialog.setResizable(true);

        content.setMinWidth(450);
        content.setMinHeight(120);

        updateOKButton(false);
    }

    private Label makeLabel(String string)
    {
        Label label = new Label(string);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    public File showAndWait()
    {
        return dialog.showAndWait().orElse(null);
    }

    /**
     * Enable/disable the OK button, and set the error label
     *
     * @param force True if we want to display a message for the blank class name,
     *              even if it has been blank since the dialog was shown (we do
     *              this when the user mouses over OK).
     */
    private void updateOKButton(boolean force)
    {
        boolean enable = false;
        // First check that the parent doesn't exist:
        try
        {
            if (!Paths.get(parentField.getText()).toFile().exists())
            {
                showError(Config.getString("newProject.error.parentNotExist"), false);
            }
            else if (dialogHasBeenEdited || force)
            {
                if (!nameField.getText().isEmpty())
                {
                    // Check if the compound path is valid:
                    File compound = Paths.get(compoundPath.getText()).toFile();

                    // Check if it exists:
                    if (compound.exists())
                    {
                        showError(Config.getString("newProject.error.compoundExist"), false);
                    }
                    else
                    {
                        hideError();
                        enable = true;
                    }
                }
            }
            else
            {
                hideError();
                enable = !nameField.getText().isEmpty();
            }
        }
        catch (InvalidPathException e)
        {
            showError(Config.getString("newProject.error.pathInvalid"), true);
        }

        setOKEnabled(enable);
        dialog.setOnShown(e -> Platform.runLater(nameField::requestFocus));
    }

    private void hideError()
    {
        errorLabel.setText("");
        JavaFXUtil.setPseudoclass("bj-dialog-error", false, nameField);
        JavaFXUtil.setPseudoclass("bj-dialog-error", false, parentField);
    }

    private void showError(String error, boolean problemIsName)
    {
        // show error, highlight field red if problem is name:
        errorLabel.setText(error);
        JavaFXUtil.setPseudoclass("bj-dialog-error", problemIsName, nameField);
        JavaFXUtil.setPseudoclass("bj-dialog-error", !problemIsName, parentField);
    }

    /**
     * Sets the OK button of the dialog to be enabled (pass true) or not (pass false)
     */
    private void setOKEnabled(boolean okEnabled)
    {
        dialogPane.getOKButton().setDisable(!okEnabled);
    }

}
