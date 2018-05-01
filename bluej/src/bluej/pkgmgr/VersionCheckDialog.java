/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016  Michael Kolling and John Rosenberg
 
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Boot;
import bluej.Config;
import bluej.utility.Debug;

/**
 * Dialog implementing version check functionality.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
final public class VersionCheckDialog extends Dialog<Void>
{
    // Internationalisation
    private static final String dialogTitle = Config.getString("pkgmgr.versionDlg.title");
    private static final String helpLine1 = Config.getString("pkgmgr.versionDlg.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.versionDlg.helpLine2");

    private static final String versionURL = Config.getPropString("bluej.url.versionCheck");

    private TextArea textArea;

    private String newVersion = null;
    private Thread versionThread = null;
    private boolean isClosed = false;

    /**
     * Create a new version check dialogue and make it visible.
     */
    public VersionCheckDialog(Window parent)
    {
        setTitle(dialogTitle);
        initOwner(parent);
        initModality(Modality.WINDOW_MODAL);
        setDialogPane(new DialogPane() {
            @Override
            protected @OnThread(Tag.FX) Node createButtonBar()
            {
                // Center-align the close button:
                ButtonBar buttonBar = (ButtonBar)super.createButtonBar();
                buttonBar.setButtonOrder("_C_");
                return buttonBar;
            }
        });
        Config.addDialogStylesheets(getDialogPane());
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        makeDialog();
    }
    
    /**
     * Perform a version check. 
     */
    private void doVersionCheck()
    {
        textArea.setText(Config.getString("pkgmgr.checkingVersion"));
        versionThread = new VersionChecker();
        //versionThread.setPriority(Thread.MIN_PRIORITY);
        versionThread.start();
    }
    
    /**
     * Create the dialog interface.
     */
    private void makeDialog()
    {
        VBox mainPanel = new VBox();
        {
            JavaFXUtil.addStyleClass(mainPanel, "version-check-dialog-content");
            Label helpLabel = new Label(helpLine1 + " " + helpLine2);
            helpLabel.setWrapText(true);
            helpLabel.setMaxWidth(400.0);
            mainPanel.getChildren().add(helpLabel);
            
            textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(400.0);

            mainPanel.getChildren().add(textArea);
        }

        getDialogPane().setContent(mainPanel);

        doVersionCheck();
    }

    @OnThread(Tag.Any)
    private void setTextLater(String txt)
    {
        Platform.runLater(() -> textArea.setText(txt));
    }

    /**
     * Private class to run the actual version checking in separate thread.
     */
    private class VersionChecker extends Thread
    {
        @OnThread(Tag.Any)
        public VersionChecker()
        {
        }
        
        /**
         * Do a version check. That is: open a URL connection to the remote 
         * version file and read it. Display version info as appropriate.
         */
        @OnThread(value = Tag.Worker, ignoreParent = true)
        public void run()
        {
            try {
                InputStream is = new URL(versionURL).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                
                if(isOutOfDate(reader)) {
                    if(!isClosed)
                        displayNewVersionInfo(reader);
                }
                else {
                    if(!isClosed)
                        setTextLater(Config.getString("pkgmgr.versionDlg.upToDate"));
                }
            }
            catch(IOException exc) {
                if(!isClosed)
                    setTextLater("Error: could not access remote version information");
                Debug.reportError("IO error when trying to access URL\n" + exc);
            }
        }

        /**
         * Given a reader for the (remote) version file, check whether this
         * version is out of date. We know that the first line of the version
         * file contains the up-to-date version number.
         */
        @OnThread(Tag.Worker)
        private boolean isOutOfDate(BufferedReader versionReader)
        {
            try {
                newVersion = versionReader.readLine();
                if(newVersion != null)
                    newVersion = newVersion.trim();
            }
            catch(IOException exc) {
                setTextLater("Error: could not read remote version information");
                Debug.reportError("IO error when reading remote version info\n" + exc);
            }
            return ! Boot.BLUEJ_VERSION.equals(newVersion);
        }

        /**
         * Given a reader for the (remote) version file, read the version
         * info text out of it and display it in the text area.
         */
        @OnThread(Tag.Worker)
        private void displayNewVersionInfo(BufferedReader versionReader)
        {
            if(newVersion == null)
                setTextLater("Error: could not read remote version info");
            else {
                StringBuffer text = new StringBuffer(Config.getString("pkgmgr.versionDlg.currentVersion"));
                text.append(" ");
                text.append(Boot.BLUEJ_VERSION);
                text.append("\n");
                text.append(Config.getString("pkgmgr.versionDlg.newVersion"));
                text.append(" ");
                text.append(newVersion);
                text.append("\n");

                try {
                    String line = versionReader.readLine();
                    while(line != null) {
                        text.append(line);
                        text.append("\n");
                        line = versionReader.readLine();
                    }
                }
                catch(IOException exc) {
                    Debug.reportError("IO error when reading from version file");
                }
                Platform.runLater(() -> {
                    textArea.setText(text.toString());
                    textArea.positionCaret(0);
                });
            }
        }

    } // end class VersionChecker

}
