/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2014,2016,2017,2018  Michael Kolling and John Rosenberg

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
package bluej.groupwork.ui;

import bluej.utility.javafx.JavaFXUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.FXPlatformSupplier;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A dialog for selecting a module to checkout.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ModuleSelectDialog extends FXCustomizedDialog<String>
{
    private Repository repository;
    private ModuleListerWorker worker;

    private final ActivityIndicator progressBar = new ActivityIndicator();
    // Module text field
    private final TextField moduleField = new TextField();
    // Modules list
    private final ListView<String> moduleList = new ListView<>();

    public ModuleSelectDialog(FXPlatformSupplier<Window> owner, Repository repository)
    {
        super(owner.get(), "team.moduleselect.title", "team-module-select");
        this.repository = repository;
        getDialogPane().setContent(makeMainPane());
        prepareButtonPane();
        DialogManager.centreDialog(this);
    }

    private Pane makeMainPane()
    {
        moduleField.setPrefColumnCount(20);
        HBox moduleBox = new HBox();
        moduleBox.getChildren().addAll(new Label(Config.getString("team.moduleselect.label")), moduleField);

        JavaFXUtil.addChangeListenerPlatform(moduleList.getSelectionModel().selectedItemProperty(), sel -> {
            moduleField.setText(sel);
        });
        
        moduleList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
            {
                String selectedItem = moduleList.getSelectionModel().getSelectedItem();
                if (selectedItem != null)
                {
                    setResult(selectedItem);
                    close();
                }
            }
        });

        ScrollPane moduleListSP = new ScrollPane(moduleList);
        moduleListSP.setFitToWidth(true);
        moduleListSP.setFitToHeight(true);
        final Button listButton = new Button(Config.getString("team.moduleselect.show"));
        listButton.setOnAction(event -> {
            listButton.setDisable(true);
            startProgressBar();
            worker = new ModuleListerWorker();
            worker.start();
        });
        HBox moduleListBox = new HBox();
        moduleListBox.getChildren().addAll(moduleListSP, listButton);

        // Main pane
        VBox mainPane = new VBox();
        mainPane.getChildren().addAll(moduleBox, new Label(Config.getString("team.moduleselect.available")),
                                      moduleListBox, progressBar);
        return mainPane;
    }

    /**
     * Set up the buttons panel to contain ok and cancel buttons, and associate their actions.
     */
    private void prepareButtonPane()
    {
        // Set the button types.
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        // Ok button
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(moduleField.textProperty().isEmpty());
        okButton.setOnAction(event -> {
            setResult(moduleField.getText());
            close();
        });

        this.setOnCloseRequest(event -> {
            if (worker != null) {
                worker.abort();
            }
        });
    }

    /**
     * Start the progress bar. Safe to call from any thread.
     */
    private void startProgressBar()
    {
        progressBar.setRunning(true);
    }

    /**
     * Stop the progress bar. Safe to call from any thread.
     */
    private void stopProgressBar()
    {
        progressBar.setRunning(false);
    }

    private void setModuleList(ObservableList<String> modules)
    {
        moduleList.setItems(modules);
    }

    /**
     * A thread to find the available modules in the background.
     *
     * @author Davin McCall
     */
    private class ModuleListerWorker extends FXWorker
    {
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private ObservableList<String> modules;

        public ModuleListerWorker()
        {
            modules = FXCollections.observableArrayList();
            command = repository.getModules(modules);
        }

        @OnThread(Tag.Worker)
        public Object construct()
        {
            result = command.getResult();
            return result;
        }

        public void finished()
        {
            stopProgressBar();
            if (command != null) {
                if (result != null && ! result.isError()) {
                    setModuleList(modules);
                }
                else {
                    TeamUtils.handleServerResponseFX(result, ModuleSelectDialog.this.asWindow());
                }
            }
        }

        public void abort()
        {
            if (command != null) {
                command.cancel();
                command = null;
            }
        }
    }
}
