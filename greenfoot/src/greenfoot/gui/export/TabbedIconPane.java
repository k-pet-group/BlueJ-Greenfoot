/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.export;

import bluej.Config;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Class TabbedIconPane is a component that holds a few mutually exclusive
 * selectable icons - a bit like a JTabbedPane.
 *
 * Currently hardcoded for one set of icons. Could be generalised if needed.
 *
 * @author Michael Kolling
 */
public class TabbedIconPane extends JPanel implements ActionListener
{
    private static final Color backgroundColor = new Color(250, 250, 250);
    private static final Color selectedColor = new Color(220, 220, 220);
    private static final Color lineColor = new Color(180, 180, 180);
    private static final Border emptyBorder = new EmptyBorder(4, 10, 4, 10);
    private static final Border selectedBorder = new CompoundBorder(new LeftRightBorder(lineColor), 
                                                    new EmptyBorder(3, 9, 3, 9));
                
    private JRadioButton selected;
    private TabbedIconPaneListener listener;
    private Map<String, JRadioButton> buttons = new HashMap<String, JRadioButton>();
    
    /**
     * Creates a new instance of TabbedIconPane.
     */
    public TabbedIconPane(String initialSelect) 
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(backgroundColor);
        add(makeButtonRow(initialSelect));
        //add(new JSeparator());
    }
    
    /**
     * Attach a (single) listener to this pane.
     */
    public void setListener(TabbedIconPaneListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Make the row of toggle/radio buttons along the top of the dialogue.
     */
    private JPanel makeButtonRow(String initialSelect)
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0,0));
        
        panel.setBorder(null);
        panel.setBackground(backgroundColor);

        makeButton(Config.getString("export.icontab.publish"), "export-publish", ExportPublishPane.FUNCTION, initialSelect, panel);
        makeButton(Config.getString("export.icontab.webpage"), "export-webpage", ExportWebPagePane.FUNCTION, initialSelect, panel);
        makeButton(Config.getString("export.icontab.application"), "export-app", ExportAppPane.FUNCTION, initialSelect, panel);
        makeButton(Config.getString("export.icontab.project"), "export-project", ExportProjectPane.FUNCTION, initialSelect, panel);

        return panel;
    }
    
    /**
     * Make one of the buttons to go into this component.
     */
    private JRadioButton makeButton(String text, String iconName, String command, 
                                    String selectCommand, JPanel parent)
    {
        URL iconFile = this.getClass().getClassLoader().getResource(iconName + ".png");
        ImageIcon icon = null;
        if(iconFile != null) {
            icon = new ImageIcon(iconFile);
        }
        JRadioButton toggle = new JRadioButton(text, icon);
        toggle.setHorizontalTextPosition(SwingConstants.CENTER);
        toggle.setVerticalTextPosition(SwingConstants.BOTTOM);
        toggle.setActionCommand(command);
        toggle.addActionListener(this);
        toggle.setOpaque(false);
        buttons.put(command, toggle);

        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        panel.setBorder(emptyBorder);
        panel.add(toggle);
        parent.add(panel);
        
        if(command.equals(selectCommand)) {
            toggle.setSelected(true);
            select(toggle);
        }
        return toggle;
    }
    
    /**
     * Directly selects the given button without informing the listener
     */
    public void select(String function)
    {
        JRadioButton button = buttons.get(function);
        if (button != null) {
            deselect(selected);
            select(button);
        }
    }
    
    /**
     * A tab in this tabbed pane has been selected.
     */
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        deselect(selected);
        JRadioButton button = (JRadioButton)e.getSource();
        select(button);
        listener.tabSelected(e.getActionCommand());
    }

    /**
     * Decorate the given button so that it appears selected.
     */
    private void select(JRadioButton button)
    {
        JPanel parent = (JPanel)button.getParent();
        parent.setBackground(selectedColor);
        parent.setBorder(selectedBorder);
        selected = button;
    }

    /**
     * Decorate the given button so that it appears deselected.
     */
    private static void deselect(JRadioButton button)
    {
        JPanel parent = (JPanel)button.getParent();
        parent.setBackground(backgroundColor);
        parent.setBorder(emptyBorder);
    }
}
