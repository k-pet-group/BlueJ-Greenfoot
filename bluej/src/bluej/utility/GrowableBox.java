package bluej.utility;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;

import bluej.Config;

/**
 * A box that can be used to dynamically (from the UI) add and remove
 * components in either a horizonatal or vertical direction.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GrowableBox.java 2653 2004-06-23 09:34:29Z polle $
 */
public class GrowableBox extends Box
{
    private ComponentFactory componentFactory;
    private JComponent emptyGrowable;
    private Border emptyBorder;
    private static Insets buttonInsets = new Insets(0, 1, 0, 1);	
    private static Font buttonFont = new Font("Monospaced", Font.BOLD, 12);	
    private static String addText = "+";
    private static ImageIcon addIcon;
    private static String removeText = "-";
    private static ImageIcon removeIcon;    
  
    /**
     * Creates a growable panel along the specifed axis. 
     * By default it includes two buttons to add new components. These are removed when new components are added.
     * 
     * @see ComponentFactory
     * @see javax.swing.BoxLayout#X_AXIS
     * @see javax.swing.BoxLayout#Y_AXIS
     * @see javax.swing.Box#Box(int)
     * @param axis The X_AXIS or Y_AXIS
     * @param componentFactory The factory to create new components
     */
    public GrowableBox(ComponentFactory componentFactory, int axis, int gap) {
        super(axis);        
        this.componentFactory = componentFactory;
        emptyBorder = BorderFactory.createEmptyBorder(0,0,gap,0);
        emptyGrowable = new JPanel();
        emptyGrowable.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        JButton addButton = new JButton();
        JButton removeButton = new JButton();        
        initButtons(addButton, removeButton);
        removeButton.setEnabled(false);
        emptyGrowable.add(addButton);
        emptyGrowable.add(removeButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createNewComponent(0);
            }
        });
        clear();
    }

    /**
     * Clears the GrowableBox to its initial state.
     */
    public void clear() {     
        removeAll();
        createNewComponent(0);
        validate();
    }

    /**
     * Inserts a new component at the given position.
     *
     */
    public void createNewComponent(int index) {
        JButton addButton = new JButton();
        JButton removeButton = new JButton();
        initButtons(addButton, removeButton);
        final JComponent component = componentFactory.createComponent(addButton, removeButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = getIndex(component);
                createNewComponent(index + 1);
            }
        });
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeGrowableComponent(component);
            }
        });
        addGrowableComponent(index, component);
    }
    
    /**
     * Returns the number of components - not counting the emptyComponent which is dislayed if all other components are removed.
     * @return
     */
    public int getComponentCountWithoutEmpty() {
        int count = getComponentCount();
        if (count == 1 && getComponent(0) == emptyGrowable) {
            return 0;
        } else {
            return count;
        }
    }

    /**
     * Intialises the buttons to get the correct look.
     * 
     * @param addButton
     * @param removeButton
     */
    private void initButtons(JButton addButton, JButton removeButton) {
    	addButton.setFont(buttonFont);
    	removeButton.setFont(buttonFont);
        addButton.setText(addText);
        addButton.setIcon(addIcon);
        removeButton.setText(removeText);
        removeButton.setIcon(removeIcon);
        addButton.setMargin(buttonInsets);
        removeButton.setMargin(buttonInsets);
    }

    private void addGrowableComponent(int index, JComponent growableComponent) {        
        if (getComponentCount() > 0 && getComponent(0) == emptyGrowable) {
            removeAll();
        }
        add(growableComponent, index);     
        
        if(index != getComponentCount()-1) {
            //this is not the last component
            growableComponent.setBorder(emptyBorder);
        } else {
            //This is last component so no border is needed.
            //But we need to set border on the nextlast
            int nextLastIndex = index - 1;
            if(nextLastIndex >= 0) {
                JComponent nextLast = (JComponent) getComponent(nextLastIndex);
                nextLast.setBorder(emptyBorder);
            }
        }            
        
        validate();
        repaint();
        fireResizedEvent();
    }

    private void removeGrowableComponent(JComponent growableComponent) {
        int index = getIndex(growableComponent);
        if(index == getComponentCount()-1) {
            //About to remove last component.
            //So we remove border from the soon to be last component.
            int nextLastIndex = index - 1;
            if(nextLastIndex >= 0) {
                JComponent nextLast = (JComponent) getComponent(nextLastIndex);
                nextLast.setBorder(null);
            }
        }
        
        remove(growableComponent);
        if (getComponentCount() == 0) {
            addGrowableComponent(0,emptyGrowable);
        }
        validate();
        repaint();
        fireResizedEvent();
    }

    /**
     * To make sure that containers can listen for resize events.
     *
     */
    private void fireResizedEvent() {
        ComponentEvent e = new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
    }

    private int getIndex(Component c) {
        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component == c) {
                return i;
            }
        }
        return -1;
    }    
}