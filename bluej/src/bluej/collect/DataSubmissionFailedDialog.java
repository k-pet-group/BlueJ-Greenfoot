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

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;

import bluej.Config;
import bluej.utility.Debug;

/**
 * A dialog to be shown in the case that you are running a trial, and
 * data collection fails for one of the participants,
 */
public class DataSubmissionFailedDialog extends JDialog
{
    private final Timer timer;
    private int countdown = 20;

    public DataSubmissionFailedDialog()
    {
        // We'd prefer system modal because then it would block
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setTitle("Data collection failure");

        JLabel message = new JLabel("<html><div style=\"width:300px;\">Connection to the data recording server has failed.  Please tell an instructor, and then restart " + (Config.isGreenfoot() ? "Greenfoot" : "BlueJ") + " to reconnect and continue working.</div></html>");
        message.setAlignmentX(Component.CENTER_ALIGNMENT);


        JButton ok = new JButton("Ok (" + countdown + ")");
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.setAction(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                DataSubmissionFailedDialog.this.setVisible(false);
            }
        });
        ok.setEnabled(false);

        timer = new Timer(1000, event -> {
            countdown -= 1;
            if (countdown > 0)
                ok.setText("Ok (" + countdown + ")");
            else
            {
                ok.setText("Ok");
                ok.setEnabled(true);
                DataSubmissionFailedDialog.this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
                getTimer().stop();
            }
        });
        timer.setCoalesce(false);
        timer.setRepeats(true);
        timer.start();

        final JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(message);
        box.add(Box.createRigidArea(new Dimension(1, 10)));
        box.add(ok);
        // To make the dialog size correctly:
        box.add(Box.createRigidArea(new Dimension(1, 15)));
        box.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(box);
        setLocationRelativeTo(null);
        pack();
    }

    public Timer getTimer()
    {
        return timer;
    }
}
