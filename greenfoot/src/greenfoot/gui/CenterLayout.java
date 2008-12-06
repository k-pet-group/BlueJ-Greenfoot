package greenfoot.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/*
 * This is a layout class for a container that has exactly one child component.
 * The layout places that child in the centre of the container.
 *
 * @author mik
 * @version 0.1
 */

public class CenterLayout extends FlowLayout {
    
    /**
     * Creates a new instance of CenterLayout.
     */
    public CenterLayout() {
        super(FlowLayout.LEFT, 0, 0);
        
    }
    
    
    /**
     * Layout the target container.
     */
    public void layoutContainer(Container target)
    {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right + getHgap() * 2);
            int maxheight = target.getHeight() - (insets.top + insets.bottom + getVgap() * 2);

            Component m = target.getComponent(0);
            if (m.isVisible()) {
                Dimension d = m.getPreferredSize();
                m.setSize(d.width, d.height);

                int hspace = maxwidth - d.width;
                int xpos = insets.left + getHgap() + (hspace / 2);

                int vspace = maxheight - d.height;
                int ypos = insets.top + getVgap() + (vspace / 2);

                m.setLocation(xpos, ypos);

            }
        }
    }
}
