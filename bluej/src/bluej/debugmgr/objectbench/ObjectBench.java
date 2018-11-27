/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2016,2018  Michael Kolling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.TilePane;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaNames;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The class responsible for the panel that displays objects
 * at the bottom of the package manager.
 * 
 * @author  Michael Cahill
 * @author  Andrew Patterson
 */
@OnThread(Tag.FXPlatform)
public class ObjectBench extends javafx.scene.control.ScrollPane implements ValueCollection,
    ObjectBenchInterface, PkgMgrFrame.PkgMgrPane
{
    @OnThread(Tag.Any)
    private final List<ObjectBenchListener> listenerList = new ArrayList<>();
    private ObjectBenchPanel obp;
    /**
     * This list maintains the correct internal state of the object bench.  Whenever you
     * modify this list, you must manually mirror the change into obp.getChildren()
     * using correct threading rules.
     */
    @OnThread(Tag.Any)
    private final List<ObjectWrapper> objects = new ArrayList<>();
    /**
     * This can be read from any thread, but should only be altered on FXPlatform thread.
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private ObjectWrapper selectedObject;
    private final PkgMgrFrame pkgMgrFrame;
    
    // All invocations done since our last reset.
    @OnThread(Tag.FXPlatform)
    private List<InvokerRecord> invokerRecords;
   
    /**
     * Construct an object bench which is used to hold
     * a bunch of object reference Components.
     */
    @OnThread(Tag.FXPlatform)
    public ObjectBench(PkgMgrFrame pkgMgrFrame)
    {
        super();
        createComponent();
        this.pkgMgrFrame = pkgMgrFrame;
        updateAccessibleName();
    }
    
    /**
     * Updates the accessible name for screen readers, based on the number
     * of objects currently on the bench
     */
    @OnThread(Tag.FXPlatform)
    private void updateAccessibleName()
    {
        String name = Config.getString("pkgmgr.objBench.title");
        final int n = getObjectCount();
        name += ": " + n + " " + Config.getString(n == 1 ? "pkgmgr.objBench.suffix.singular" : "pkgmgr.objBench.suffix.plural");
        //getAccessibleContext().setAccessibleName(name);
    }

    /**
     * Add an object (in the form of an ObjectWrapper) to this bench.
     */
    @OnThread(Tag.Any)
    public void addObject(ObjectWrapper wrapper)
    {
        addObject(wrapper, Optional.empty());
    }

    @OnThread(Tag.Any)
    public void addObject(ObjectWrapper wrapper, Optional<Point2D> animateFromScene)
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

        synchronized (ObjectBench.this)
        {
            objects.add(wrapper);
        }

        // wrapper.addFocusListener(this); -- not needed
        JavaFXUtil.runNowOrLater(() -> {
            // Keep it invisible until positioned:
            wrapper.setVisible(false);
            // Allows us to know when it has been positioned:
            wrapper.setLayoutY(-1);
            obp.getChildren().add(wrapper);
            wrapper.animateIn(animateFromScene);
            updateAccessibleName();
        });
        
    }

    @Override
    public String addObject(DebuggerObject object, GenTypeClass type, String name)
    {
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                pkgMgrFrame, this,
                object,
                type,
                name);       
        addObject(wrapper);
        return wrapper.getName();
    }
    
    /**
     * Return all the wrappers stored in this object bench in an array
     */
    @OnThread(Tag.Any)
    public synchronized List<ObjectWrapper> getObjects()
    {
        // We take a copy because we want to avoid unsafe thread accesses
        // on the returned list:
        return Collections.unmodifiableList(new ArrayList<>(objects));
    }

    /**
     * Return an iterator through all the objects on the bench (to meet
     * the ValueCollection interface)
     */
    @OnThread(Tag.Any)
    public Iterator<ObjectWrapper> getValueIterator()
    {
        // Iterates on the copy that getObjects provides, so thread-safe:
        return getObjects().iterator();
    }
    
    /**
     * Get the object with name 'name', or null, if it does not
     * exist.
     *
     * @param name  The name to check for.
     * @return  The named object wrapper, or null if not found.
     */
    @OnThread(Tag.Any)
    public synchronized ObjectWrapper getObject(String name)
    {
        for(Iterator<ObjectWrapper> i = objects.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = i.next();
            if(wrapper.getName().equals(name))
                return wrapper;
        }
        return null;
    }
    
    @OnThread(Tag.Any)
    public NamedValue getNamedValue(String name)
    {
        return getObject(name);
    }

    /**
     * Check whether the bench contains an object with name 'name'.
     *
     * @param name  The name to check for.
     */
    @OnThread(Tag.Any)
    public boolean hasObject(String name)
    {
        return getObject(name) != null;
    }

    
    /**
     * Count of object bench copmponents that are object wrappers
     * @return number of ObjectWrappers on the bench
     */
    @OnThread(Tag.Any)
    public synchronized int getObjectCount()
    {
        return objects.size();
    }

    
    /**
     * Remove all objects from the object bench.
     */
    public synchronized void removeAllObjects(String scopeId)
    {
        setSelectedObject (null);

        for(Iterator<ObjectWrapper> i = objects.iterator(); i.hasNext(); ) {
            ObjectWrapper wrapper = i.next();
            wrapper.prepareRemove();
            wrapper.getPackage().getDebugger().removeObject(scopeId, wrapper.getName());
        }
        objects.clear();
        JavaFXUtil.runNowOrLater(() -> obp.getChildren().clear());
        resetRecordingInteractions();
        updateAccessibleName();
    }

    
    /**
     * Remove an object from the object bench. When this is done, the object
     * is also removed from the scope of the package (so it is not accessible
     * as a parameter anymore) and the bench is redrawn.
     */
    public synchronized void removeObject(ObjectWrapper wrapper, String scopeId)
    {
        if(wrapper == selectedObject) {
            setSelectedObject(null);
        }
     
        DataCollector.removeObject(wrapper.getPackage(), wrapper.getName());
        
        wrapper.prepareRemove();
        wrapper.getPackage().getDebugger().removeObject(scopeId, wrapper.getName());
        objects.remove(wrapper);
        wrapper.animateOut(() -> obp.getChildren().remove(wrapper));

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
    @OnThread(Tag.FXPlatform)
    public synchronized void setSelectedObject(ObjectWrapper aWrapper)
    {
        if (selectedObject != null) {
            selectedObject.setSelected(false);
        }
        selectedObject = aWrapper;
        
        if (selectedObject != null && !selectedObject.isFocused()) {
            selectedObject.requestFocus();
        }
    }
    
    /**
     * Notify that an object has gained focus. The object becomes the selected object.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void objectGotFocus(ObjectWrapper aWrapper)
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
    @OnThread(Tag.Any)
    public synchronized ObjectWrapper getSelectedObject()
    {
        return selectedObject;
    }

    
    /**
     * Add a listener for object events to this bench.
     * @param l  The listener object.
     */
    @OnThread(Tag.Any)
    public void addObjectBenchListener(ObjectBenchListener l)
    {
        synchronized (listenerList)
        {
            listenerList.add(l);
        }
    }
    

    /**
     * Remove a listener for object events to this bench.
     * @param l  The listener object.
     */
    @OnThread(Tag.Any)
    public void removeObjectBenchListener(ObjectBenchListener l)
    {
        synchronized (listenerList)
        {
            listenerList.remove(l);
        }
    }
    
    
    /**
     * Fire an object event for the named object. This will
     * notify all listeners that have registered interest for
     * notification on this event type.
     */
    @OnThread(Tag.FXPlatform)
    public void fireObjectSelectedEvent(ObjectWrapper wrapper)
    {
        synchronized (listenerList)
        {
            // process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listenerList.size() - 1; i >= 0; i--)
            {
                listenerList.get(i).objectEvent(
                    new ObjectBenchEvent(this,
                        ObjectBenchEvent.OBJECT_SELECTED, wrapper));
            }
        }
    }
    
    // --- KeyListener interface ---

    /**
     * A key was pressed in the object bench.
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public synchronized void keyPressed(KeyEvent e) 
    {
        int selectedObjectIndex;
        if(selectedObject == null) {
            selectedObjectIndex = -1;
        }
        else {
            selectedObjectIndex = objects.indexOf(selectedObject);
        }
        switch (e.getCode()){
            case LEFT:
                if (selectedObjectIndex > 0) {
                    selectedObjectIndex--;
                }
                else {
                    selectedObjectIndex = 0;
                }
                setSelectedObjectByIndex(selectedObjectIndex);
                break;

            case RIGHT:
                if (selectedObjectIndex < objects.size() - 1) {
                    setSelectedObjectByIndex(selectedObjectIndex + 1);
                }
                break;

            case UP:
                selectedObjectIndex = selectedObjectIndex - obp.getNumberOfColumns();
                if (selectedObjectIndex >= 0) {
                    setSelectedObjectByIndex(selectedObjectIndex);
                }
                break;

            case DOWN:
                selectedObjectIndex = selectedObjectIndex + obp.getNumberOfColumns();
                if (selectedObjectIndex < objects.size()) {
                    setSelectedObjectByIndex(selectedObjectIndex);
                }
                break;

            case ENTER:
            case SPACE:
            case CONTEXT_MENU:
                showPopupMenu();
                break;
        }
    }

    /**
     * Sets the selected object from an index in the objects list.
     * The index MUST be valid.
     */
    private synchronized void setSelectedObjectByIndex(int i)
    {
        ((ObjectWrapper) objects.get(i)).requestFocus();
    }


    /**
     * Post the object menu for the selected object.
     */
    private synchronized void showPopupMenu() 
    {
        if(selectedObject != null) {
            selectedObject.showMenu();
        }
    }

    // --- methods for interaction recording ---
    
    /**
     * Reset the recording of invocations.
     */
    @OnThread(Tag.FXPlatform)
    public void resetRecordingInteractions()
    {
        invokerRecords = new LinkedList<InvokerRecord>();
    }

    @OnThread(Tag.FXPlatform)
    public void addInteraction(InvokerRecord ir)
    {
        if (invokerRecords == null)
            resetRecordingInteractions();
            
        invokerRecords.add(ir);    
    }
    
    /**
     * Get the recorded interaction fixture declarations as Java code.
     */
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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

    @OnThread(Tag.FXPlatform)
    private synchronized void createComponent()
    {
        // a panel holding the actual object components
        obp = new ObjectBenchPanel();
        JavaFXUtil.addStyleClass(this, "object-bench");

        setContent(obp);
        setFitToWidth(true);
        setMinViewportWidth(ObjectWrapper.WIDTH);
        setMinViewportHeight(ObjectWrapper.HEIGHT);
        this.setMinWidth(ObjectWrapper.WIDTH + 35 /* space for scroll bar, foldout control */);
        this.setMinHeight(ObjectWrapper.HEIGHT);
        // start with a clean slate recording invocations
        resetRecordingInteractions();

        setOnKeyPressed(this::keyPressed);
    }

    // ------------- nested class ObjectBenchPanel --------------

    /**
     * This is the panel that lives inside the object bench's scrollpane
     * and actually holds the object wrapper components.
     */
    @OnThread(Tag.FXPlatform)
    private final class ObjectBenchPanel extends TilePane
    {
        public ObjectBenchPanel()
        {
            //this.setPrefWidth(ObjectWrapper.WIDTH);
            //this.prefHeightProperty().bind()  TODO is this necessary?  HEIGHT * numrows
            JavaFXUtil.addStyleClass(this, "object-bench-panel");
        }

        /**
         * Return the current number of rows or objects on this bench.
         */
        public int getNumberOfRows()
        {
            int objects = getChildren().size();
            if(objects == 0) {
                return 1;
            }
            else {
                int objectsPerRow = (int)getWidth() / ObjectWrapper.WIDTH;
                return (objects + objectsPerRow - 1) / objectsPerRow;
            }
        }


        /**
         * Return the current number of rows or objects on this bench.
         */
        public int getNumberOfColumns()
        {
            return (int)getWidth() / ObjectWrapper.WIDTH;
        }

    }

    @Override
    @OnThread(Tag.FX)
    public void requestFocus()
    {
        // Override default behaviour (in which clicking on scroll pane
        // gives it focus).
        // Don't let the pane request focus.
    }

    /**
     * Does one of the objects contained within have focus?
     */
    @OnThread(Tag.FXPlatform)
    public synchronized boolean objectHasFocus()
    {
        return objects.stream().anyMatch(w -> w.isFocused());
    }

    /**
     * Highlights the given object, and clears highlights on all
     * other objects.
     *
     * @param currentObject The object to highlight (may be null,
     *                      to just clear all existing highlights)
     */
    public void highlightObject(DebuggerObject currentObject)
    {
        // Clear highlights on other objects:
        for (ObjectWrapper wrapper : objects)
        {
            wrapper.setHighlight(currentObject != null && Objects.equals(wrapper.obj, currentObject));
        }
    }
}
