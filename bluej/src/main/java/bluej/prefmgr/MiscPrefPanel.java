/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2016,2018,2022,2023  Michael Kolling and John Rosenberg

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
package bluej.prefmgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import bluej.pkgmgr.Project;
import bluej.debugger.RunOnThread;
import com.google.common.collect.ImmutableList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import bluej.Boot;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * various miscellaneous settings
 *
 * @author  Andrew Patterson
 */
@OnThread(Tag.FXPlatform)
public class MiscPrefPanel extends VBox 
                           implements PrefPanelListener
{
    private final int normalChildren;
    private CheckBox showUncheckedBox; // show "unchecked" compiler warning
    private TextField playerNameField;
    private TextField participantIdentifierField;
    private TextField experimentIdentifierField;
    private Label statusLabel;
    private ComboBox<RunOnThread> runOnThread;
    private Node threadRunSetting;
    private List<MiscPrefPanelItem> extraItems;

    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    public MiscPrefPanel()
    {
        JavaFXUtil.addStyleClass(this, "prefmgr-pref-panel");

        if (Config.isGreenfoot()) {
            getChildren().add(makePlayerNamePanel());
            if (Boot.isTrialRecording())
            {
                getChildren().add(makeDataCollectionPanel());
            }
        }
        else {
            getChildren().add(makeVMPanel());
            getChildren().add(makeDataCollectionPanel());
        }
        normalChildren = getChildren().size();
    }

    private Node makeDataCollectionPanel()
    {
        List<Node> dataCollectionPanel = new ArrayList<>();
        {
            statusLabel = new Label(DataCollector.getOptInOutStatus());
            statusLabel.setMinWidth(100.0);
            Button optButton = new Button(Config.getString("prefmgr.collection.change"));
            optButton.setOnAction(e ->{
                DataCollector.changeOptInOut(false);
                statusLabel.setText(DataCollector.getOptInOutStatus());
            });
            optButton.setMinWidth(Control.USE_PREF_SIZE);
            dataCollectionPanel.add(PrefMgrDialog.labelledItem(statusLabel, optButton));
        }


        {
            Label identifierLabel = new Label(Config.getString("prefmgr.collection.identifier.explanation") + ":");
            dataCollectionPanel.add(identifierLabel);

            GridPane experimentParticipantPanel = new GridPane();
            JavaFXUtil.addStyleClass(experimentParticipantPanel, "prefmgr-experiment-participant");

            Label experimentLabel = new Label(Config.getString("prefmgr.collection.identifier.experiment"));
            experimentParticipantPanel.add(experimentLabel, 0, 0);
            experimentIdentifierField = new TextField();
            experimentParticipantPanel.add(experimentIdentifierField, 1, 0);

            Label participantLabel = new Label(Config.getString("prefmgr.collection.identifier.participant"));
            experimentParticipantPanel.add(participantLabel, 0, 1);
            participantIdentifierField = new TextField();
            experimentParticipantPanel.add(participantIdentifierField, 1, 1);

            dataCollectionPanel.add(experimentParticipantPanel);
        }
        return PrefMgrDialog.headedVBox("prefmgr.collection.title", dataCollectionPanel);
    }

    // Not called in Greenfoot
    private Node makeVMPanel()
    {
        showUncheckedBox = new CheckBox(Config.getString("prefmgr.misc.showUnchecked"));
        ObservableList<RunOnThread> runOnThreadPoss = FXCollections.observableArrayList(RunOnThread.DEFAULT, RunOnThread.FX, RunOnThread.SWING);
        runOnThread = new ComboBox<>(runOnThreadPoss);
        threadRunSetting = PrefMgrDialog.labelledItem("prefmgr.misc.runOnThread", runOnThread);
        return PrefMgrDialog.headedVBox("prefmgr.misc.vm.title", Arrays.asList(showUncheckedBox, threadRunSetting));
    }

    private Node makePlayerNamePanel()
    {
        List<Node> contents = new ArrayList<>();

        // get Accelerator text
        KeyCodeCombination accelerator = Config.GREENFOOT_SET_PLAYER_NAME_SHORTCUT;
        String shortcutText = " " + accelerator.getDisplayText();

        playerNameField = new TextField(PrefMgr.getPlayerName().get());
        playerNameField.setPrefColumnCount(20);
        contents.add(PrefMgrDialog.labelledItem("playername.dialog.help", playerNameField));

        contents.add(PrefMgrDialog.wrappedLabel(Config.getString("prefmgr.misc.playerNameNote") + shortcutText));

        return PrefMgrDialog.headedVBox("prefmgr.misc.playername.title", contents);
    }

    public void beginEditing(Project project)
    {
        if(!Config.isGreenfoot()) {
            showUncheckedBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_UNCHECKED));
            if (project == null)
            {
                threadRunSetting.setVisible(false);
                threadRunSetting.setManaged(false);
            }
            else
            {
                runOnThread.getSelectionModel().select(project.getRunOnThread());
                threadRunSetting.setVisible(true);
                threadRunSetting.setManaged(true);
            }
            statusLabel.setText(DataCollector.getOptInOutStatus());
            experimentIdentifierField.setText(DataCollector.getExperimentIdentifier());
            participantIdentifierField.setText(DataCollector.getParticipantIdentifier());
        }
        else
        {
            playerNameField.setText(PrefMgr.getPlayerName().get());
        }
        extraItems.forEach(p -> p.beginEditing(project));
    }

    public void revertEditing(Project project)
    {
        extraItems.forEach(p -> p.revertEditing(project));
    }

    public void commitEditing(Project project)
    {
        if(!Config.isGreenfoot()) {
            PrefMgr.setFlag(PrefMgr.SHOW_UNCHECKED, showUncheckedBox.isSelected());
            if (project != null)
            {
                // Important to use .name() because we overrode toString() for localized display:
                project.setRunOnThread(runOnThread.getSelectionModel().getSelectedItem());
            }

            String expId = experimentIdentifierField.getText();
            String partId = participantIdentifierField.getText();
            DataCollector.setExperimentIdentifier(expId);
            DataCollector.setParticipantIdentifier(partId);
        }

        if (Config.isGreenfoot())
        {
            PrefMgr.getPlayerName().set(playerNameField.getText());
        }
        extraItems.forEach(p -> p.commitEditing(project));
    }

    /**
     * Sets the extra items at the bottom of the panel (in Greenfoot, this is the sound devices).
     * Replaces the previous extra items
     * 
     * @param miscPrefPanelItems The extra items to set.  All existing items will be removed.  Pass the empty list to make it blank.
     */
    public void setExtraItems(List<MiscPrefPanelItem> miscPrefPanelItems)
    {
        // Get rid of any old extra items:
        getChildren().remove(normalChildren, getChildren().size());
        // Add the new ones:
        for (MiscPrefPanelItem miscPrefPanelItem : miscPrefPanelItems)
        {
            getChildren().add(PrefMgrDialog.headedVBoxTranslated(miscPrefPanelItem.getMiscPanelTitle(), miscPrefPanelItem.getMiscPanelContents()));
        }
        this.extraItems = ImmutableList.copyOf(miscPrefPanelItems);
    }
}
