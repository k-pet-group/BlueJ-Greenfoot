/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011,2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx.export;

import bluej.Config;
import bluej.utility.javafx.FXCustomizedDialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Display a "proxy authentication required" dialog, prompting for username and password.
 * 
 * @author Davin McCall
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ProxyAuthDialog extends FXCustomizedDialog<ProxyAuthDialog.ProxyAuthInfo>
{
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    
    /**
     * Construct a new proxy authentication dialog.
     *
     * @param parent The owner {@link Window} for this dialog.
     *               Should not be null, as this is not a top Application window.
     */
    public ProxyAuthDialog(Window parent)
    {
        super(parent, Config.getString("export.publish.proxyAuth"), null);
        setModal(true);
        buildUI();

        // Ok and cancel buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        getDialogPane().lookupButton(ButtonType.OK).disableProperty()
                .bind(usernameField.textProperty().isEmpty().or(passwordField.textProperty().isEmpty()));
        setResultConverter(bt -> bt == ButtonType.OK
                ? new ProxyAuthInfo(usernameField.getText(), passwordField.getText())
                : null);
    }
    
    /**
     * Build the user interface
     */
    private void buildUI()
    {
        GridPane authPanel = new GridPane();
        usernameField.setPrefColumnCount(20);
        passwordField.setPrefColumnCount(20);

        authPanel.addRow(0, new Label(Config.getString("export.publish.username")), usernameField);
        authPanel.addRow(1, new Label(Config.getString("export.publish.password")), passwordField);

        setContentPane(new VBox(new Label(Config.getString("export.publish.needProxyAuth")), authPanel));
    }

    /**
     * Proxy authentication credentials.
     */
    public class ProxyAuthInfo
    {
        private String username;
        private String password;

        private ProxyAuthInfo(String username, String password)
        {
            this.username = username;
            this.password = password;
        }

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }
    }
}
