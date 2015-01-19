/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015  Michael Kolling and John Rosenberg 

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

import bluej.BlueJTheme;
import bluej.Config;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * "Raspberry Pi" preferences panel. Shows Raspberry Pi specific options.
 *
 * @author Fabio Hedayioglu
 */
public class RaspberryPiPanel extends JPanel implements PrefPanelListener {

    private JCheckBox useSudoBox;

    public RaspberryPiPanel() {

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        add(box);

        setBorder(BlueJTheme.generalBorder);

        box.add(Box.createVerticalGlue());
        box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        JPanel jvmPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        {
            jvmPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            Config.getString("extmgr.raspberryPi.title")),
                    BlueJTheme.generalBorder));
            jvmPanel.setAlignmentX(LEFT_ALIGNMENT);

            useSudoBox = new JCheckBox(Config.getString("extmgr.raspberryPi.superUser"));
            jvmPanel.add(useSudoBox);
            
            JLabel sudoNoteLine1 = new JLabel(
                              Config.getString("prefmgr.raspberryPi.NoteLine1"));
            Font smallFont = sudoNoteLine1.getFont().deriveFont(10);
            sudoNoteLine1.setFont(smallFont);
            sudoNoteLine1.setAlignmentX(LEFT_ALIGNMENT);
            jvmPanel.add(sudoNoteLine1);
            
            JLabel sudoNoteLine2 = new JLabel(
                              Config.getString("prefmgr.raspberryPi.NoteLine2"));
            sudoNoteLine2.setFont(smallFont);
            sudoNoteLine2.setAlignmentX(LEFT_ALIGNMENT);
            jvmPanel.add(sudoNoteLine2);
            
            JLabel sudoNoteLine3 = new JLabel(
                              Config.getString("prefmgr.raspberryPi.NoteLine3"));
            sudoNoteLine3.setFont(smallFont);
            sudoNoteLine3.setAlignmentX(LEFT_ALIGNMENT);
            jvmPanel.add(sudoNoteLine3);
        }
        box.add(jvmPanel);

        box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

    }

    @Override
    public void beginEditing() {
        useSudoBox.setSelected(PrefMgr.getFlag(PrefMgr.START_WITH_SUDO));
    }

    @Override
    public void revertEditing() {
    }

    @Override
    public void commitEditing() {
        PrefMgr.setFlag(PrefMgr.START_WITH_SUDO, useSudoBox.isSelected());
    }

}
