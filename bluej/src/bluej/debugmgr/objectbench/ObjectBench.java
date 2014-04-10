/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.objectbench;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.accessibility.Accessible;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaNames;

/**
 * The class responsible for the panel that displays objects
 * at the bottom of the package manager.
 * 
 * @author  Michael Cahill
 * @author  Andrew Patterson
 */
public class ObjectBench extends JPanel implements Accessible, ValueCollection,
    FocusListener, KeyListener, MouseListener, ObjectBenchInterface
{
    private static final Color TRANSPARENT = new Color(0f, 0f, 0f, 0.0f);

    private JScrollPane scroll;
    private ObjectBenchPanel obp;
    private List<ObjectWrapper> objects;
    private ObjectWrapper selectedObject;
    private PkgMgrFrame pkgMgrFrame;
    
    // All invocations done since our last reset.
    private List<InvokerRecord> invokerRecords;
   
    /**
     * Construct an object bench which is used to hold
     * a bunch of object reference Components.
     */
    public ObjectBench(PkgMgrFrame pkgMgrFrame)
    {
        super();
        objects = new ArrayList<ObjectWrapper>();
        createComponent();
        this.pkgMgrFrame = pkgMgrFrame;
        updateAccessibleName();
    }
    
    /**
     * Updates the accessible name for screen readers, based on the number
     * of objects currently on the bench
     */
    private void updateAccessibleName()
    {
        String name = Config.getString("pkgmgr.objBench.title");
        final int n = getObjectCount();
        name += ": " + n + " " + Config.getString(n == 1 ? "pkgmgr.objBench.suffix.singular" : "pkgmgr.objBench.suffix.plural");
        getAccessibleContext().setAccessibleName(name);
    }

    /**
     * Add an object (in the form of an ObjectWrapper) to this bench.
     */
    public void addObject(ObjectWrapper wrapper)
    {
        // check whether name is already taken

        String newname = wrapper.getName();
        int count = 1;
        
        if (JavaNames.isJavaKeyword(newname)) {
            newname = "x" + newname;
        }

        while(hasObject(newname)) {
            count++;
            newname = wrapper.getName() + count;
        }
        wrapper.setName(newname);

        // wrapper.addFocusListener(this); -- not needed
        obp.add(wrapper);
        objects.add(wrapper);
        obp.revalidate();
        obp.repaint();
        updateAccessibleName();
    }

    
    /**
     * Return all the wrappers stored in this object bench in an array
     */
    public List<ObjectWrapper> getObjects()
    {
        return Collections.unmodifiableList(objects);
    }

    /**
     * Return an iterator through all the objects on the bench (to meet
     * the ValueCollection interface)
     */
    public Iterator<ObjectWrapper> getValueIterator()
    {
        return getObjects().iterator();
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
        for(Iterator<ObjectWrapper> i = objects.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = i.next();
            if(wrapper.getName().equals(name))
                return wrapper;
        }
        return null;
    }
    
    public NamedValue getNamedValue(String name)
    {
        return getObject(name);
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
        return objects.size();
    }

    
    /**
     * Remove all objects from the object bench.
     */
    public void removeAllObjects(String scopeId)
    {
        setSelectedObject (null);

        for(Iterator<ObjectWrapper> i = objects.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = i.next();
            wrapper.prepareRemove();
            wrapper.getPackage().getDebugger().removeObject(scopeId, wrapper.getName());
            obp.remove(wrapper);
        }
        objects.clear();
        resetRecordingInteractions();
        obp.revalidate();
        obp.repaint();
        updateAccessibleName();
    }

    
    /**
     * Remove an object from the object bench. When this is done, the object
     * is also removed from the scope of the package (so it is not accessible
     * as a parameter anymore) and the bench is redrawn.
     */
    public void removeObject(ObjectWrapper wrapper, String scopeId)
    {
        if(wrapper == selectedObject) {
            setSelectedObject(null);
        }
     
        DataCollector.removeObject(wrapper.getPackage(), wrapper.getName());
        
        wrapper.prepareRemove();
        wrapper.getPackage().getDebugger().removeObject(scopeId, wrapper.getName());
        obp.remove(wrapper);
        objects.remove(wrapper);

        obp.revalidate();
        obp.repaint();
        updateAccessibleName();
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
        if (selectedObject != null) {
            selectedObject.setSelected(false);
        }
        selectedObject = aWrapper;
        
        if (selectedObject != null) {
            selectedObject.requestFocusInWindow();
        }
    }
    
    /**
     * Notify that an object has gained focus. The object becomes the selected object.
     */
    public void objectGotFocus(ObjectWrapper aWrapper)
    {
        if (selectedObject == aWrapper) {
            return;
        }
        
        if (selectedObject != null) {
            selectedObject.setSelected(false);
        }
        
        selectedObject = aWrapper;
        selectedObject.setSelected(true);
    }
    
    /**
     * Returns the currently selected object wrapper. 
     * If no wrapper is selected null is returned.
     */
    public ObjectWrapper getSelectedObject()
    {
        return selectedObject;
    }

    
    /**
     * Add a listener for object events to this bench.
     * @param l  The listener object.
     */
    public void addObjectBenchListener(ObjectBenchListener l)
    {
        listenerList.add(ObjectBenchListener.class, l);
    }
    

    /**
     * Remove a listener for object events to this bench.
     * @param l  The listener object.
     */
    public void removeObjectBenchListener(ObjectBenchListener l)
    {
        listenerList.remove(ObjectBenchListener.class, l);
    }
    
    
    /**
     * Fire an object event for the named object. This will
     * notify all listeners that have registered interest for
     * notification on this event type.
     */
    public void fireObjectEvent(ObjectWrapper wrapper)
    {
        // guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            // Loop steps by 2 because the list is a list of pairs: [listener class, listener]
            if (listeners[i] == ObjectBenchListener.class) {
                ((ObjectBenchListener)listeners[i+1]).objectEvent(
                        new ObjectBenchEvent(this,
                                ObjectBenchEvent.OBJECT_SELECTED, wrapper));
            }
        }
    }
    
    /**
     * Do the usual enabling/disabling, while also removing the focus
     * border when disabling.
     */
    public void setEnabled(boolean enable)
    {
        super.setEnabled(enable);
        if(!enable) {
            showFocusHiLight(false);
        }
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
        if (isEnabled()) {
            showFocusHiLight(true);
        }
    }

    
    /**
     * Note that the object bench lost keyboard focus.
     */
    public void focusLost(FocusEvent e) 
    {
        if (!e.isTemporary()) {
            showFocusHiLight(false);
        }
    }

    // --- end of FocusListener interface ---

    // --- KeyListener interface ---

    /**
     * A key was pressed in the object bench.
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) 
    {
        int selectedObjectIndex;
        if(selectedObject == null) {
            selectedObjectIndex = -1;
        }
        else {
            selectedObjectIndex = objects.indexOf(selectedObject);
        }
        int key = e.getKeyCode();
        
        switch (key){
            case KeyEvent.VK_LEFT:
                if (selectedObjectIndex > 0) {
                    selectedObjectIndex--;
                }
                else {
                    selectedObjectIndex = 0;
                }
                setSelectedObjectByIndex(selectedObjectIndex);
                break;

            case KeyEvent.VK_RIGHT:
                if (selectedObjectIndex < objects.size() - 1) {
                    setSelectedObjectByIndex(selectedObjectIndex + 1);
                }
                break;

            case KeyEvent.VK_UP:
                selectedObjectIndex = selectedObjectIndex - obp.getNumberOfColumns();
                if (selectedObjectIndex >= 0) {
                    setSelectedObjectByIndex(selectedObjectIndex);
                }
                break;

            case KeyEvent.VK_DOWN:
                selectedObjectIndex = selectedObjectIndex + obp.getNumberOfColumns();
                if (selectedObjectIndex < objects.size()) {
                    setSelectedObjectByIndex(selectedObjectIndex);
                }
                break;

            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_CONTEXT_MENU:
                showPopupMenu();
                break;
        }
    }

    /**
     * Sets the selected object from an index in the objects list.
     * The index MUST be valid.
     */
    private void setSelectedObjectByIndex(int i)
    {
        ((ObjectWrapper) objects.get(i)).requestFocusInWindow();
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
    public void mouseClicked(MouseEvent e)
    {
        setSelectedObject(null);
    }
    
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
        requestFocus();
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
        if(selectedObject != null) {
            selectedObject.showMenu();
        }
    }

    // --- methods for interaction recording ---
    
    /**
     * Reset the recording of invocations.
     */
    public void resetRecordingInteractions()
    {
        invokerRecords = new LinkedList<InvokerRecord>();
    }

    public void addInteraction(InvokerRecord ir)
    {
        if (invokerRecords == null)
            resetRecordingInteractions();
            
        invokerRecords.add(ir);    
    }
    
    /**
     * Get the recorded interaction fixture declarations as Java code.
     */
    public String getFixtureDeclaration(String firstIndent)
    {
        StringBuffer sb = new StringBuffer();
        Iterator<InvokerRecord> it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = it.next();
            
            if (ir.toFixtureDeclaration(firstIndent) != null) {
                sb.append(ir.toFixtureDeclaration(firstIndent));
            }
        }                    

        return sb.toString();
    }
    
    /**
     * Get the recorded interaction fixture setup as Java code.
     */
    public String getFixtureSetup(String secondIndent)
    {
        StringBuffer sb = new StringBuffer();
        Iterator<InvokerRecord> it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = it.next();
            
            if (ir.toFixtureSetup(secondIndent) != null) {
                sb.append(ir.toFixtureSetup(secondIndent));
            }
        }                    

        return sb.toString();
    }
    
    /**
     * Get the recorded interactions as Java code.
     */
    public String getTestMethod(String secondIndent)
    {
        StringBuffer sb = new StringBuffer();
        Iterator<InvokerRecord> it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = it.next();

            String testMethod = ir.toTestMethod(pkgMgrFrame, secondIndent);
            if (testMethod != null) {
                sb.append(testMethod);
            }
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
        if (!Config.isRaspberryPi()) obp.setBackground(TRANSPARENT);
        if (!Config.isRaspberryPi()) obp.setOpaque(true);
        if (!Config.isRaspberryPi()) setOpaque(false);
        
        scroll = new JScrollPane(obp);
        scroll.setBorder(Config.normalBorder);
        if (!Config.isRaspberryPi()) scroll.setOpaque(false);
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
        obp.addMouseListener(this);
    }

    
    // ------------- nested class ObjectBenchPanel --------------

    /**
     * This is the panel that lives inside the object bench's scrollpane
     * and actually holds the object wrapper components.
     */
    private final class ObjectBenchPanel extends JPanel
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
            int rows = getNumberOfRows();
            return new Dimension(ObjectWrapper.WIDTH, ObjectWrapper.HEIGHT * rows);                
        }
        
        /**
         * Return the current number of rows or objects on this bench.
         */
        public int getNumberOfRows()
        {
            int objects = getComponentCount();
            if(objects == 0) {
                return 1;
            }
            else {
                int objectsPerRow = getWidth() / ObjectWrapper.WIDTH;
                return (objects + objectsPerRow - 1) / objectsPerRow;
            }            
        }
        
        /**
         * Return the current number of rows or objects on this bench.
         */
        public int getNumberOfColumns()
        {
            return getWidth() / ObjectWrapper.WIDTH;
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            
            if (g instanceof Graphics2D && false == pkgMgrFrame.isEmptyFrame()) {
                Graphics2D g2d = (Graphics2D)g;
                
                int w = getWidth();
                int h = getHeight();
                
                boolean codePadVisible = pkgMgrFrame.isTextEvalVisible();
                 
                // Paint a gradient from top to bottom:
                if (!Config.isRaspberryPi()){
                    GradientPaint gp;
                    if (codePadVisible) {
                        gp = new GradientPaint(
                                w/4, 0, new Color(209, 203, 179),
                                w*3/4, h, new Color(235, 230, 200));
                    } else {
                        gp = new GradientPaint(
                            w/4, 0, new Color(235, 230, 200),
                            w*3/4, h, new Color(209, 203, 179));
                    }
       
                    g2d.setPaint(gp);
                }else{
                    g2d.setPaint(new Color(222,217, 190));
                }
                g2d.fillRect(0, 0, w+1, h+1);

            }
        }
    }
}
