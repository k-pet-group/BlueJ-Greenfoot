package bluej.utility;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A box that can be used to dynamically (from the UI) add and remove
 * components in either a horizonatal or vertical direction.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GrowableBox.java 2585 2004-06-10 13:27:46Z polle $
 */
public class GrowableBox extends Box
{
    private ComponentFactory componentFactory;
    private Container emptyGrowable;
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
    public GrowableBox(ComponentFactory componentFactory, int axis) {
        super(axis);
        this.componentFactory = componentFactory;
        emptyGrowable = new JPanel();
        emptyGrowable.setLayout(new FlowLayout());
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
               
        /*addButton = new JButton();
        removeButton = new JButton();
        initButtons(addButton, removeButton);
        defaultGrowable = componentFactory.createComponent(addButton, removeButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = getIndex(defaultGrowable);
                createNewComponent(index + 1);
            }
        });
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeGrowableComponent(defaultGrowable);
            }
        });*/
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
        final Component component = componentFactory.createComponent(addButton, removeButton);
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
        addButton.setText(addText);
        addButton.setIcon(addIcon);
        removeButton.setText(removeText);
        removeButton.setIcon(removeIcon);
        addButton.setMargin(new Insets(0, 0, 0, 0));
        removeButton.setMargin(new Insets(0, 0, 0, 0));
    }

    private void addGrowableComponent(int index, Component growableComponent) {        
        if (getComponentCount() > 0 && getComponent(0) == emptyGrowable) {
            remove(0);
        }
        add(growableComponent, index);
        
        validate();
        repaint();
        fireResizedEvent();
    }

    private void removeGrowableComponent(Component growableComponent) {
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