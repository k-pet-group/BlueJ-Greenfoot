/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016  Michael Kolling and John Rosenberg
 
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
package bluej.extmgr;

import javax.swing.*;

import bluej.*;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.net.URL;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * The Extensions Manager help panel allows the user to view current extensions.
 *
 * @author Clive Millaer, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
@OnThread(Tag.FXPlatform)
public class ExtensionsDialog
{
    private final String systemString = Config.getString("extmgr.systemExtensionShort");
    private final String projectString = Config.getString("extmgr.projectExtensionShort");
    private final String systemLongString = Config.getString("extmgr.systemExtensionLong");
    private final String projectLongString = Config.getString("extmgr.projectExtensionLong");
    private final String locationTag = Config.getString("extmgr.details.location");
    private final String versionTag = Config.getString("extmgr.details.version");
    private Dialog<Void> mainFrame;
    private VBox extensionsVBox;

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     * This new version is guarantee to have a valid extension manager.
     */
    @OnThread(Tag.Swing)
    ExtensionsDialog(List<ExtensionWrapper> extensionsList, FXPlatformSupplier<Window> parent)
    {
        Platform.runLater(() -> {
            mainFrame = new Dialog();
            mainFrame.initOwner(parent.get());
            mainFrame.setTitle(Config.getString("extmgr.title"));
            mainFrame.initModality(Modality.WINDOW_MODAL);
            mainFrame.setResizable(true);

            extensionsVBox = new VBox();
            extensionsVBox.setFillWidth(true);
            extensionsVBox.setMinWidth(200.0);
            JavaFXUtil.addStyleClass(extensionsVBox, "extension-list");
            ScrollPane extensionsPane = new ScrollPane(extensionsVBox);
            extensionsPane.setPrefHeight(300.0);
            extensionsPane.setPrefWidth(500.0);
            extensionsPane.setFitToWidth(true);

            Config.addDialogStylesheets(mainFrame.getDialogPane());
            mainFrame.getDialogPane().setContent(extensionsPane);
            mainFrame.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        });
        
        extensionsList.forEach(wrapper -> {
            // We must get details on the Swing thread...
            String extensionName = wrapper.safeGetExtensionName();
            String extensionStatus = wrapper.getExtensionStatus();
            String extensionVersion = wrapper.safeGetExtensionVersion();
            String extensionDescription = wrapper.safeGetExtensionDescription();
            boolean isProject = wrapper.getProject() != null;
            String extensionFileName = wrapper.getExtensionFileName();
            URL url = wrapper.safeGetURL();
            // But create the TitledPane on the FX thread:
            Platform.runLater(() -> 
                extensionsVBox.getChildren().add(makeDisplay(extensionName, extensionStatus, extensionVersion, extensionDescription, isProject, extensionFileName, url))
            );
        });
    }
    
    public void showAndWait()
    {
        mainFrame.showAndWait();
    }

    @OnThread(Tag.FXPlatform)
    private TitledPane makeDisplay(String extensionName, String extensionStatus, String extensionVersion, String extensionDescription, boolean isProject, String extensionFileName, URL url)
    {
        String typeShort = isProject ? projectString : systemString;
        String typeLong = isProject ? projectLongString : systemLongString;
        String title = extensionName + " (" + typeShort + ", " + extensionStatus + ")";

        VBox mainPanel = new VBox();
        JavaFXUtil.addStyleClass(mainPanel, "extension-info");

        mainPanel.getChildren().add(new Label(extensionName + " " + versionTag + " "
            + extensionVersion));
        mainPanel.getChildren().add(new Label(typeLong));

        mainPanel.getChildren().add(new Label(locationTag + " " + extensionFileName +
            " (" + extensionStatus +')'));

        Text description = new Text(extensionDescription);
        mainPanel.getChildren().add(new TextFlow(description));
        if (url != null)
        {
            Hyperlink link = new Hyperlink(url.toExternalForm());
            link.setOnAction(e -> SwingUtilities.invokeLater(() -> Utility.openWebBrowser(url.toExternalForm())));
            mainPanel.getChildren().add(link);
        }

        TitledPane titledPane = new TitledPane(title, mainPanel);
        titledPane.setExpanded(false);
        return titledPane;
    }
}
