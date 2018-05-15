/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A BlueJ and Greenfoot Shared About-Dialog.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class AboutDialogTemplate extends Dialog<Void>
{

    public AboutDialogTemplate(Window parent, String version, String softwareTitle,
                               String websiteURL, Image image, TitledPane titledPane)
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

        // Create About box text
        BorderPane aboutPanel = new BorderPane();
        ScrollPane scroll = new ScrollPane();
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setContent(aboutPanel);
        getDialogPane().setContent(scroll);
        JavaFXUtil.addStyleClass(aboutPanel, "about-dialog-content");

        // insert logo
        aboutPanel.setCenter(JavaFXUtil.withStyleClass(new ImageView(image), "about-dialog-image"));

        // All text inserted as bottom
        VBox bottom = JavaFXUtil.withStyleClass(new VBox(), "about-more-info");

        // Create team names list
        String teamText = String.join(", ", "Amjad Altadmri", "Neil Brown", "Hamza Hamza",
                "Michael K\u00F6lling", "Davin McCall", "Ian Utting");

        bottom.getChildren().add(JavaFXUtil.withStyleClass(
                new Label(Config.getString("about.theTeam") + " " + teamText),
                "about-team"));

        if (titledPane != null)
        {
            bottom.getChildren().add(titledPane);
        }

        bottom.getChildren().add(new Label(""));
        bottom.getChildren().add(JavaFXUtil.withStyleClass(
                new Label(Config.getString("about.version") + " " + version + "  (" +
                        Config.getString("about.java.version") + " " + System.getProperty("java.version") +
                        ")"), "about-version"));
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

        aboutPanel.setBottom(bottom);
        setResizable(false);
    }

}

