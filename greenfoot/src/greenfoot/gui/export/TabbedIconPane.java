/*
 * Class TabbedIconPane is a component that holds a few mutually exclusive
 * selectable icons - a bit like a JTabbedPane.
 *
 * Currently hardcoded for one set of icons. Could be generalised if needed.
 *
 * @author Michael Kolling
 * @version 1.0
 */

package greenfoot.gui.export;

import bluej.Config;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class TabbedIconPane extends JPanel
        implements ActionListener
{
    private static final Color backgroundColor = new Color(250, 250, 250);
    private static final Color selectedColor = new Color(220, 220, 220);
    private static final Color lineColor = new Color(180, 180, 180);
    private static final Border emptyBorder = new EmptyBorder(4, 10, 4, 10);
    private static final Border selectedBorder = new CompoundBorder(new LeftRightBorder(lineColor), 
                                                    new EmptyBorder(3, 9, 3, 9));
                
    private JRadioButton selected;
    
    /**
     * Creates a new instance of TabbedIconPane.
     */
    public TabbedIconPane() 
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(backgroundColor);
        add(makeButttonRow());
        add(new JSeparator());
    }
    
    /**
     * Make the row of toggle/radio buttons along the top of the dialogue.
     */
    private JPanel makeButttonRow()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0,0));
        
        panel.setBorder(null);
        panel.setBackground(backgroundColor);

        ButtonGroup group = new ButtonGroup();

        JRadioButton toggle1 = makeButton("Publish", "export-publish", group, panel);
        JRadioButton toggle2 = makeButton("Webpage", "export-webpage", group, panel);
        JRadioButton toggle3 = makeButton("Application", "export-app", group, panel);

        toggle1.setSelected(true);
        select(toggle1);
        
        return panel;
    }
    
    /**
     * Make one of the buttons to go into this component.
     */
    private JRadioButton makeButton(String text, String iconName, ButtonGroup group, JPanel parent)
    {
        JRadioButton toggle = new JRadioButton(text, Config.getHardImageAsIcon(iconName + ".png"));
        toggle.setHorizontalTextPosition(SwingConstants.CENTER);
        toggle.setVerticalTextPosition(SwingConstants.BOTTOM);
        toggle.addActionListener(this);
        toggle.setOpaque(false);

        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        panel.setBorder(emptyBorder);
        panel.add(toggle);
        parent.add(panel);
        
        return toggle;
    }
    
    public void actionPerformed(ActionEvent e) {
        deselect(selected);
        JRadioButton button = (JRadioButton)e.getSource();
        select(button);
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
    private void deselect(JRadioButton button)
    {
        JPanel parent = (JPanel)button.getParent();
        parent.setBackground(backgroundColor);
        parent.setBorder(emptyBorder);
    }
    
    
    
    /**
     * Custom Border class to draw just the left and right sides of a lin border.
     */
    static class LeftRightBorder extends LineBorder
    {
        public LeftRightBorder(Color col)
        {
            super(col);
        }
        
        /**
         * Paints the border only on the left and right sides.
         */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color oldColor = g.getColor();

            g.setColor(lineColor);
            g.drawLine(x, y, x, height-1);
            g.drawLine(width-1, y, width-1, height-1);
            g.setColor(oldColor);
        }
    }
}
