package bluej.debugmgr.objectbench;

import java.util.*;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import bluej.Config;
import bluej.testmgr.record.InvokerRecord;

/**
 * The object responsible for the panel that displays objects
 * at the bottom of the package manager.
 * @author  Michael Cahill
 * @author  Andrew Patterson
 * @version $Id: ObjectBench.java 2753 2004-07-07 10:00:09Z mik $
 */
public class ObjectBench extends JPanel 
    implements FocusListener, KeyListener, MouseListener
{
    private static final int SCROLL_AMOUNT = (ObjectWrapper.WIDTH / 3);
    private static final Color BACKGROUND_COLOR = Config.getItemColour("colour.objectbench.background");

    private JScrollPane scroll;
    private ObjectBenchPanel obp;
    private List objectWrappers;
    private ObjectWrapper selectedObjectWrapper;
    private int currentObjectWrapperIndex = -1;
	
    // All invocations done since our last reset.
    private List invokerRecords;

   
    /**
     * Construct an object bench which is used to hold
     * a bunch of object reference Components.
     */
    public ObjectBench()
    {
        super();
        objectWrappers = new ArrayList();
        createComponent();
    }

    
    /**
     * Add an object (in the form of an ObjectWrapper) to this bench.
     */
    public void addObject(ObjectWrapper wrapper)
    {
        // check whether name is already taken

        String newname = wrapper.getName();
        int count = 1;

        while(hasObject(newname)) {
            count++;
            newname = wrapper.getName() + "_" + count;
        }
        wrapper.setName(newname);

        wrapper.addFocusListener(this);
        obp.add(wrapper);
        objectWrappers.add(wrapper);
        obp.revalidate();
        obp.repaint();
    }

    
    /**
     * Return all the wrappers stored in this object bench in an array
     */
    public List getObjects()
    {
        return Collections.unmodifiableList(objectWrappers);
    }

    
    /**
     * Get the object with name 'name', or null, if it does not
     * exist.
     *
     * @param name  The name to check for.
     * @return  The named object wrapper, or null if not found.
     */
    public ObjectWrapper getObject(String name)
    {
        for(Iterator i=objectWrappers.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = (ObjectWrapper)i.next();
            if(wrapper.getName().equals(name))
                return wrapper;
        }
        return null;
    }
    

    /**
     * Check whether the bench contains an object with name 'name'.
     *
     * @param name  The name to check for.
     */
    public boolean hasObject(String name)
    {
        return getObject(name) != null;
    }

    
    /**
     * Count of object bench copmponents that are object wrappers
     * @return number of ObjectWrappers on the bench
     */
    public int getObjectCount()
    {
        return objectWrappers.size();
    }

    
    /**
     * Remove all objects from the object bench.
     */
    public void removeAllObjects(String scopeId)
    {
        setSelectedObject (null);

        for(Iterator i = objectWrappers.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = (ObjectWrapper) i.next();
            wrapper.prepareRemove();
            wrapper.getPackage().getDebugger().removeObject(wrapper.getName());
            obp.remove(wrapper);
        }
        objectWrappers.clear();
        resetRecordingInteractions();
        obp.revalidate();
        obp.repaint();
    }

    
    /**
     * Remove an object from the object bench. When this is done, the object
     * is also removed from the scope of the package (so it is not accessible
     * as a parameter anymore) and the bench is redrawn.
     */
    public void removeObject(ObjectWrapper wrapper, String scopeId)
    {
        if(wrapper == selectedObjectWrapper)
            setSelectedObject(null);
            
        wrapper.prepareRemove();
        wrapper.getPackage().getDebugger().removeObject(wrapper.getName());
        obp.remove(wrapper);
        objectWrappers.remove(wrapper);

        obp.revalidate();
        obp.repaint();
    }

    
    /**
     * Remove the selected object from the bench. If no object is selected,
     * do nothing.
     */
    public void removeSelectedObject(String scopeId)
    {
        ObjectWrapper wrapper = getSelectedObject();
        if(wrapper != null)
            removeObject(wrapper, scopeId);
    }
    
    
    /**
     * Sets what is the currently selected ObjectWrapper, null can be given to 
     * signal that no wrapper is selected.
     */
    public void setSelectedObject(ObjectWrapper aWrapper)
    {
        if (selectedObjectWrapper != null) {
            selectedObjectWrapper.setSelected(false);
        }
        selectedObjectWrapper = aWrapper;
        
        if (selectedObjectWrapper != null) {
            selectedObjectWrapper.setSelected(true);
            currentObjectWrapperIndex = objectWrappers.indexOf(aWrapper);
            selectedObjectWrapper.requestFocusInWindow();
        }
    }

    
    /**
     * Returns the currently selected object wrapper. 
     * If no wrapper is selected null is returned.
     */
    public ObjectWrapper getSelectedObject()
    {
        return selectedObjectWrapper;
    }

    
    /**
     * Add a listener for object events to this bench.
     * @param l  The listener object.
     */
    public void addObjectBenchListener(ObjectBenchListener l)
    {
        obp.addObjectBenchListener(l);
    }
    

    /**
     * Remove a listener for object events to this bench.
     * @param l  The listener object.
     */
    public void removeObjectBenchListener(ObjectBenchListener l)
    {
        obp.removeObjectBenchListener(l);
    }
    
    
    /**
     * Fire an object event for the named object.
     */
    public void fireObjectEvent(ObjectWrapper wrapper)
    {
        obp.fireObjectEvent(wrapper);
    }

    /**
     * Show or hide the focus highlight (an emphasised border around
     * the bench).
     */
    public void showFocusHiLight(boolean hiLight)
    {
        if(hiLight)
            scroll.setBorder(Config.focusBorder);
        else
            scroll.setBorder(Config.normalBorder);
        repaint();
    }

    // --- FocusListener interface ---
    
    /**
     * Note that the object bench got keyboard focus.
     */
    public void focusGained(FocusEvent e) 
    {
        showFocusHiLight(true);
    }

    
    /**
     * Note that the object bench lost keyboard focus.
     */
    public void focusLost(FocusEvent e) 
    {
        showFocusHiLight(false);
    }

    // --- end of FocusListener interface ---

    // --- KeyListener interface ---

    /**
     * A key was pressed in the object bench.
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) 
    {
        int key = e.getKeyCode();
        switch (key){
            case KeyEvent.VK_LEFT: {
                if (currentObjectWrapperIndex > 0){
                currentObjectWrapperIndex--;
                }
                else if (currentObjectWrapperIndex < 0 ){
                    //currentObjectWrapperIndex = objectWrappers.size() - 1;
                    currentObjectWrapperIndex = 0;
                }
                break;
            }
            case KeyEvent.VK_RIGHT: {
                if (currentObjectWrapperIndex < objectWrappers.size() - 1){
                    currentObjectWrapperIndex++;
                }
                break;
            }
            case KeyEvent.VK_ENTER:{
                showPopupMenu();
                break;
            }
            case KeyEvent.VK_SPACE:{
                showPopupMenu();
                break;
            }
            case KeyEvent.VK_ESCAPE:{
                currentObjectWrapperIndex = -1;
                setSelectedObject(null);
                repaint();
                break;
            }
        }
        boolean isInRange = (0 <= currentObjectWrapperIndex && 
                             currentObjectWrapperIndex < objectWrappers.size()); 
        if (isInRange){
            ObjectWrapper currentObjectWrapper = (ObjectWrapper) objectWrappers.get(currentObjectWrapperIndex);
            setSelectedObject(currentObjectWrapper);
            repaint();
        }
    }

    
    /**
     * A key was released in the object bench.
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e) {}

    
    /**
     * A key was typed in the object bench.
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e) {}
    
    // --- end of KeyListener interface ---

    // --- MouseListener interface ---

    /**
     * The mouse was clicked in the object bench.
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {}

    
    /**
     * The mouse entered the object bench.
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {}

    
    /**
     * The mouse left the object bench.
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {}
    

    /**
     * The mouse was pressed in the object bench.
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
    }

    
    /**
     * The mouse was released in the object bench.
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {}
    

    // --- end of MouseListener interface ---


    /**
     * Post the object menu for the selected object.
     */
    private void showPopupMenu() 
    {
        if (selectedObjectWrapper != null){
            selectedObjectWrapper.showMenu();
        }
    }

    // --- methods for interaction recording ---
    
    /**
     * Reset the recording of invocations.
     */
    public void resetRecordingInteractions()
    {
        invokerRecords = new LinkedList();
    }

    
    public void addInteraction(InvokerRecord ir)
    {
        if (invokerRecords == null)
            resetRecordingInteractions();
            
        invokerRecords.add(ir);    
    }
    
    
    public String getFixtureDeclaration()
    {
        StringBuffer sb = new StringBuffer();
        Iterator it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = (InvokerRecord) it.next();
            
            if (ir.toFixtureDeclaration() != null)
            	sb.append(ir.toFixtureDeclaration());
        }                    

        return sb.toString();
    }
    
    
    public String getFixtureSetup()
    {
        StringBuffer sb = new StringBuffer();
        Iterator it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = (InvokerRecord) it.next();
            
			if (ir.toFixtureSetup() != null)
	            sb.append(ir.toFixtureSetup());
        }                    

        return sb.toString();
    }
    
    
    public String getTestMethod()
    {
        StringBuffer sb = new StringBuffer();
        Iterator it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = (InvokerRecord) it.next();
            
			if (ir.toTestMethod() != null)
	            sb.append(ir.toTestMethod());
        }                    

        return sb.toString();
    }

    
    /**
     * Create the object bench as a good looking Swing component.
     */
    private void createComponent()
    {
        setLayout(new BorderLayout());

        // a panel holding the actual object components
        obp = new ObjectBenchPanel();
        obp.setBackground(BACKGROUND_COLOR);
        
        scroll = new JScrollPane(obp);
        scroll.setBorder(Config.normalBorder);
        Dimension sz = obp.getMinimumSize();
        Insets in = scroll.getInsets();
        sz.setSize(sz.getWidth()+in.left+in.right, sz.getHeight()+in.top+in.bottom);
        scroll.setMinimumSize(sz);
        scroll.setPreferredSize(sz);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        
        add(scroll, BorderLayout.CENTER);

        // start with a clean slate recording invocations
        resetRecordingInteractions();
        //when empty, the objectbench is focusable
        setFocusable(true);        

        addFocusListener(this);
        addKeyListener(this);
        addMouseListener(this);        
    }

    
    // ------------- nested class ObjectBenchPanel --------------

    /**
     * This is an inner class so that people can't add or remove
     * components to it that are of the wrong type (ie not ObjectWrapper).
     */
    private class ObjectBenchPanel extends JPanel
    {
        public ObjectBenchPanel()
        {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setMinimumSize(new Dimension(ObjectWrapper.WIDTH, ObjectWrapper.HEIGHT));
        }

        /**
         * Add the component (like any other JPanel) and then set our preferred size
         * so that all components would be visible.
         */
        public Component add(Component comp)
        {
            super.add(comp);
            
            return comp;
        }

        /**
         * Return the preferred size of this component.
         */
        public Dimension getPreferredSize()
        {
            int objects = getComponentCount();
            int rows;
            if(objects == 0) {
                rows = 1;
            }
            else {
                int objectsPerRow = getWidth() / ObjectWrapper.WIDTH;
                rows = (objects + objectsPerRow - 1) / objectsPerRow;
            }
            return new Dimension(ObjectWrapper.WIDTH, ObjectWrapper.HEIGHT * rows);                
        }
        
        /**
         * This component will raise ObjectBenchEvents when nodes are
         * selected in the bench. The following functions manage this.
         */
        public void addObjectBenchListener(ObjectBenchListener l)
        {
            listenerList.add(ObjectBenchListener.class, l);
        }

        public void removeObjectBenchListener(ObjectBenchListener l)
        {
            listenerList.remove(ObjectBenchListener.class, l);
        }

        // notify all listeners that have registered interest for
        // notification on this event type.
        void fireObjectEvent(ObjectWrapper wrapper)
        {
            setSelectedObject(wrapper);
            // guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            // process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i] == ObjectBenchListener.class) {
                    ((ObjectBenchListener)listeners[i+1]).objectEvent(
                            new ObjectBenchEvent(this,
                                    ObjectBenchEvent.OBJECT_SELECTED, wrapper));
                }
            }
        }
    }
}
