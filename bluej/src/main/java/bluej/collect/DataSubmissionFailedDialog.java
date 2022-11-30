/*
 This file is part of the BlueJ program.
 Copyright (C) 2016  Michael Kolling and John Rosenberg

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
package bluej.collect;


import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Modality;

import bluej.Config;
import bluej.utility.Debug;
import threadchecker.OnThread;

/**
 * A dialog to be shown in the case that you are running a trial, and
 * data collection fails for one of the participants,
 */
public class DataSubmissionFailedDialog extends Dialog<Void>
{
    private final AnimationTimer timer;
    private final long countdown = 20;

    public DataSubmissionFailedDialog()
    {
        // We'd prefer system modal because then it would block
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Data collection failure");

        Label message = new Label("Connection to the data recording server has failed.  Please tell an instructor, and then restart " + (Config.isGreenfoot() ? "Greenfoot" : "BlueJ") + " to reconnect and continue working.");
        message.setWrapText(true);
        message.setPrefWidth(300.0);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK);
        Button ok = (Button)getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Ok (" + countdown + ")");
        ok.setOnAction(e -> close());
        ok.setDisable(true);

        timer = new AnimationTimer()
        {
            long time = -1;
            long prevRemaining = countdown;
            @Override
            public void handle(long now)
            {
                if (time == -1)
                    time = now;
                else
                {
                    long remaining = countdown - ((now - time) / 1000000000);
                    if (prevRemaining == remaining)
                        return;
                    prevRemaining = remaining;
                    if (remaining > 0)
                        ok.setText("Ok (" + remaining + ")");
                    else
                    {
                        ok.setText("Ok");
                        ok.setDisable(false);
                        stop();
                    }
                }
            }
        };
        // This should work if we set the close request on the dialog
        // but it seems there is a bug, so we set it on the stage instead:
        setOnShown(ev -> {
            timer.start();
            ok.getScene().getWindow().setOnCloseRequest(e -> {
                if (ok.isDisable())
                    e.consume();
            });
        });

        getDialogPane().setContent(message);
    }
}
