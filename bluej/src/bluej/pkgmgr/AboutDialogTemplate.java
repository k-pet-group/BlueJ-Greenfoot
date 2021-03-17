/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Pair;

import javax.swing.SwingUtilities;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A BlueJ and Greenfoot Shared About-Dialog.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class AboutDialogTemplate extends Dialog<Void>
{

    /**
     * Construct an About Dialog for BlueJ or Greenfoot.
     *
     * @param parent       The parent window.
     * @param version      The application (bluej/greenfoot) version
     * @param websiteURL   A url for the application website.
     * @param image        The splash screen image for the application.
     * @param translators  An array containing the translators names.
     * @param previousTeamMembers An array containing previous team members names.
     */
    public AboutDialogTemplate(Window parent, String version, String websiteURL, Image image,
                               String[] translators, String[] previousTeamMembers)
    {
        initOwner(parent);
        initModality(Modality.WINDOW_MODAL);
        setTitle(Config.getString("menu.help.about"));
        setDialogPane(new DialogPane() {
            @Override
            @OnThread(Tag.FX)
            protected Node createButtonBar()
            {
                // Center-align the close button:
                ButtonBar buttonBar = (ButtonBar)super.createButtonBar();
                buttonBar.setButtonOrder("_C_");
                return buttonBar;
            }
        });
        Config.addDialogStylesheets(getDialogPane());
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TabPane tabs = JavaFXUtil.withStyleClass(new TabPane(
                createMainTab(version, websiteURL, image), createContributorsTab(translators, previousTeamMembers)),
                "about-tabs");

        getDialogPane().setContent(tabs);
        setResizable(false);
    }

    /**
     * Construct the tab which contains the main information of the about dialog.
     *
     * @param version     The application (bluej/greenfoot) version
     * @param websiteURL  A url for the application website.
     * @param image       The splash screen image for the application.
     */
    private Tab createMainTab(String version, String websiteURL, Image image)
    {
        // Create About box text
        BorderPane aboutPanel = new BorderPane();
        JavaFXUtil.addStyleClass(aboutPanel, "about-dialog-content");

        // insert logo
        aboutPanel.setCenter(JavaFXUtil.withStyleClass(new ImageView(image), "about-dialog-image"));

        // All text inserted as bottom
        VBox bottom = JavaFXUtil.withStyleClass(new VBox(), "about-more-info");
        aboutPanel.setBottom(bottom);

        // Create team names list
        String teamText = String.join(", ", "Neil Brown", "Michael Kölling", "Charalampos Kyfonidis", "Pierre Weill-Tessier" + ".");

        bottom.getChildren().add(JavaFXUtil.withStyleClass(
                new Label(Config.getString("about.theTeam") + " " + teamText),
                "about-team"));

        bottom.getChildren().add(new Label(""));
        bottom.getChildren().add(JavaFXUtil.withStyleClass(
                new Label(Config.getString("about.version") + " " + version + "  (" +
                        Config.getString("about.java.version") + " " +
                        System.getProperty("java.version") + ")"), "about-version"));
        bottom.getChildren().add(new Label(Config.getString("about.vm") + " " +
                System.getProperty("java.vm.name") + " " +
                System.getProperty("java.vm.version") +
                " (" + System.getProperty("java.vm.vendor") + ")"));
        bottom.getChildren().add(new Label(Config.getString("about.runningOn") + " " +
                System.getProperty("os.name") + " " + System.getProperty("os.version") +
                " (" + System.getProperty("os.arch") + ")"));

        Button debugLogShow = new Button(Config.getString("about.openFolder"));
        debugLogShow.setOnAction(e -> SwingUtilities.invokeLater(() -> {
            try
            {
                // Linux has a bug in Desktop class, see bug BLUEJ-1039
                if (!Config.isLinux() && Desktop.isDesktopSupported())
                {
                    Desktop.getDesktop().open(Config.getUserConfigDir());
                }
                else if (Config.isLinux())
                {
                    Runtime.getRuntime().exec(
                            new String[] {"xdg-open", Config.getUserConfigDir().getAbsolutePath()});
                }
            }
            catch (IOException ex)
            {
                Debug.reportError(ex);
            }
        }));

        HBox debugLog = new HBox(new Label(Config.getString("about.logfile") + " " +
                Config.getUserConfigFile(Config.debugLogName)), debugLogShow);
        JavaFXUtil.addStyleClass(debugLog, "about-debuglog");
        debugLog.setAlignment(Pos.BASELINE_LEFT);
        bottom.getChildren().add(debugLog);

        try
        {
            final URL softwareURL = new URL(websiteURL);
            Hyperlink link = new Hyperlink(softwareURL.toString());
            link.setOnMouseClicked(e -> SwingUtilities.invokeLater(() ->
                    Utility.openWebBrowser(softwareURL.toExternalForm())));
            
            HBox hbox = new HBox(new Label(Config.getString("about.moreInformation")), link);
            hbox.setAlignment(Pos.CENTER);
            JavaFXUtil.addStyleClass(hbox, "about-info-link");
            bottom.getChildren().add(hbox);
        }
        catch (MalformedURLException exc)
        {
            // should not happen - URL is constant
        }

        Tab tab = new Tab(Config.getString("about.general.title"), aboutPanel);
        tab.setClosable(false);
        return tab;
    }

    /**
     * Construct the tab which contains the Translators information.
     *
     * @param translators  An array containing the translators names. Could be null.
     * @param previousTeamMembers An array containing previous team members names.
     */
    private Tab createContributorsTab(String[] translators, String[] previousTeamMembers)
    {
        Tab tab = new Tab(Config.getString("about.contributors.title"));
        tab.setClosable(false);

        VBox vbox = new VBox();
        if (previousTeamMembers != null)
        {
            Label teamTitle = new Label("\n" + Config.getString("about.previousTeamMembers.title") + "\n");
            teamTitle.getStyleClass().add("about-contributors-title");
            String names = String.join(", ", previousTeamMembers);
            Label teamLabel = new Label(names + ".");
            teamLabel.setPrefWidth(500);
            teamLabel.setWrapText(true);
            vbox.getChildren().addAll(teamTitle, teamLabel);
        }
        if (translators != null)
        {
            Label translatorTitle = new Label("\n" + Config.getString("about.translators.title") + "\n");
            translatorTitle.getStyleClass().add("about-contributors-title");
            vbox.getChildren().add(translatorTitle);
            ObservableList<Pair<String, String>> pairs = FXCollections.observableArrayList();
            for (int i = 0; i <translators.length - 1; i += 2)
            {
                pairs.add(new Pair<>(translators[i], translators[i + 1]));
            }
            TableView<Pair<String, String>> tableView = new TableView<>(pairs);
            tableView.getStyleClass().add("about-translators-table");
            tableView.setEditable(false);

            // The width properties add up to 97% and not 100% to forbid the scroll from appearing,
            // as there is no API, currently, to achieve this
            TableColumn<Pair<String, String>, String> languageColumn = new TableColumn<>();
            languageColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.17));
            languageColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getKey()));

            TableColumn<Pair<String, String>, String> nameColumn = new TableColumn<>();
            nameColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.80));
            nameColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

            tableView.getColumns().setAll(languageColumn, nameColumn);
            vbox.getChildren().add(tableView);
        }
        tab.setContent(vbox);
        return tab;
    }
}

