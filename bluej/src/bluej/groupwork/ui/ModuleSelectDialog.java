/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2014,2016,2017  Michael Kolling and John Rosenberg

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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
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
public class ModuleSelectDialog extends FXCustomizedDialog<Void>
{
    private Repository repository;

    private ActivityIndicatorFX progressBar;
    private TextField moduleField;
    private ListView moduleList;
    private ModuleListerThread worker;

    private boolean wasOk;

    public ModuleSelectDialog(FXPlatformSupplier<Window> owner, Repository repository)
    {
        super(owner.get(), "team.moduleselect.title", "team-module-select");
        this.repository = repository;
        setModal(true);
        buildUI();
    }

    private void buildUI()
    {
        // Content pane
        VBox contentPane = new VBox();
        setContentPane(contentPane);

        moduleField = new TextField();
        moduleField.setPrefColumnCount(20);

        // Module text field
        HBox moduleBox = new HBox();
        moduleBox.getChildren().addAll(new Label(Config.getString("team.moduleselect.label")),
                new Separator(Orientation.HORIZONTAL), moduleField);

        contentPane.getChildren().addAll(moduleBox,
                new Separator(Orientation.VERTICAL),
                new Separator(),
                new Separator(Orientation.VERTICAL),
                new Label(Config.getString("team.moduleselect.available")));

        // Modules list
        HBox moduleListBox = new HBox();
        moduleList = new ListView();

        /*moduleList.setSelectionMode(ObservableList.SINGLE_SELECTION);
        moduleList.getSelectionModel().addListSelectionListener(
                // ---- ListSelectionListener interface ----

        public void valueChanged(ListSelectionEvent e)
        {
            if (! e.getValueIsAdjusting()) {
                int selected = moduleList.getSelectedIndex();
                if (selected != -1) {
                    String module = moduleList.getModel().getElementAt(selected).toString();
                    moduleField.setText(module);
                }
            }
        }
        );
        */

        moduleList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
//                int index = moduleList.getlocationToIndex(event.getSource());
//                int index = moduleList.getEditingIndex();??
                if (moduleList.getItems().contains(event.getSource())) {
                    wasOk = true;
                    hide();
                }
            }
        });

        ScrollPane moduleListSP = new ScrollPane(moduleList);
        final Button listButton = new Button(Config.getString("team.moduleselect.show"));
        listButton.setOnAction(event -> {
            listButton.setDisable(true);
            startProgressBar();
            worker = new ModuleListerThread();
            worker.start();
        });
        moduleListBox.getChildren().addAll(moduleListSP,
                new Separator(Orientation.HORIZONTAL),
                listButton);

        contentPane.getChildren().addAll(moduleListBox, new Separator(Orientation.VERTICAL));

        // Button box
        HBox buttonBox = new HBox();
        progressBar = new ActivityIndicatorFX();
        buttonBox.getChildren().add(progressBar);
//        buttonBox.getChildren().add(Box.createHorizontalGlue());//
        buttonBox.getChildren().add(new Separator(Orientation.HORIZONTAL));

        contentPane.getChildren().addAll(buttonBox);

        // Ok button
        buttonBox.getChildren().add(new Separator(Orientation.HORIZONTAL));

        Button okButton = new Button();
        okButton.requestFocus();
        okButton.disableProperty().bind(moduleField.textProperty().isEmpty());
        okButton.setOnAction(event -> {
            wasOk = true;
            hide();
        });
        okButton.setDisable(true);
        buttonBox.getChildren().add(okButton);

        // Cancel button
        buttonBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        Button cancelButton = new Button(); //BlueJTheme.getCancelButton();
        cancelButton.setOnAction(event -> {
            if (worker != null) {
                worker.cancel();
            }
            hide();
        });
        buttonBox.getChildren().add(cancelButton);
    }

    /**
     * Get the selected module name, or null if no module was selected
     * (dialog was cancelled).
     */
    public String getModuleName()
    {
        if (wasOk) {
            return moduleField.getText();
        }
        else {
            return null;
        }
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
    private class ModuleListerThread extends FXWorker
    {
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private ObservableList<String> modules;

        public ModuleListerThread()
        {
            modules = FXCollections.observableArrayList();
            command = repository.getModules(modules);
        }

        @OnThread(Tag.Unique)
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
                    Platform.runLater(() -> TeamUtils.handleServerResponseFX(result, ModuleSelectDialog.this.asWindow()));
                }
            }
        }

        public void cancel()
        {
            if (command != null) {
                command.cancel();
                command = null;
            }
        }
    }
}
