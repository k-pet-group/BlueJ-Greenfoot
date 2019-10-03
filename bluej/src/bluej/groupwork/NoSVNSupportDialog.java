/*
 This file is part of the BlueJ program.
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.groupwork;

import bluej.Config;
import bluej.groupwork.git.GitProvider;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 *  A dialog which indicates the user the steps to take when opening a shared SVN project
 *  since BlueJ 5 onwwards does not support SVN anymore.
 *
 * @author Pierre Weill-Tessier
 */
@OnThread(Tag.FXPlatform)
public class NoSVNSupportDialog extends FXCustomizedDialog
{
    public NoSVNSupportDialog(Window owner)
    {
        super(owner, "team.load.SVNnotSupported.title", "team-load-SVNnotsupported");
        buildUI();
    }

    private void buildUI()
    {
        VBox contentPane = new VBox();
        contentPane.setMinHeight(120.0);
        JavaFXUtil.addStyleClass(contentPane, "pane");
        getDialogPane().setContent(contentPane);
        ButtonType removeSVNInfoButton = new ButtonType(Config.getString("team.load.SVNnotSupported.details.RemoveSVNOption"));
        ButtonType keepSVNButton = new ButtonType(Config.getString("team.load.SVNnotSupported.details.KeepSVNOption"));
        getDialogPane().getButtonTypes().add(0,removeSVNInfoButton);
        getDialogPane().getButtonTypes().add(1,keepSVNButton);

        Text message = new Text(Config.getString("team.load.SVNnotSupported.details"));
        contentPane.getChildren().add(new TextFlow(message));

        contentPane.setFillWidth(true);
    }
}
