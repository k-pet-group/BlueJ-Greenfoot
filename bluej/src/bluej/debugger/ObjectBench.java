package bluej.debugger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import java.util.Enumeration;
import java.util.Vector;

import bluej.utility.Debug;

/**
 * The panel that displays objects at the bottom of the package manager
 *
 * @author  Michael Cahill
 * @version $Id: ObjectBench.java 291 1999-11-30 06:24:36Z ajp $
 */
public class ObjectBench extends JPanel
{
    static final int WIDTH = 3 * (ObjectWrapper.WIDTH + 10);
    static final int HEIGHT = ObjectWrapper.HEIGHT + 10;
	
    Vector watchers = new Vector();
	
    public ObjectBench()
    {
	setLayout(new FlowLayout(FlowLayout.LEFT));
	setSize(WIDTH, HEIGHT);
    }
	
    public Dimension getMinimumSize()
    {
	Dimension minSize = super.getMinimumSize();
	minSize.width = Math.max(minSize.width, WIDTH);
	minSize.height = Math.max(minSize.height, HEIGHT);
	return minSize;
    }
	
    public Dimension getPreferredSize()
    {
	Dimension prefSize = super.getPreferredSize();
	prefSize.width = Math.max(prefSize.width, WIDTH);
	prefSize.height = Math.max(prefSize.height, HEIGHT);
	return prefSize;
    }
	
    public void addWatcher(ObjectBenchWatcher watcher)
    {
	watchers.addElement(watcher);
    }
	
    public void removeWatcher(ObjectBenchWatcher watcher)
    {
	watchers.removeElement(watcher);
    }
	
    public ObjectWrapper[] getWrappers()
    {
	Component[] components = getComponents();
	ObjectWrapper[] wrappers = new ObjectWrapper[components.length];
	System.arraycopy(components, 0, wrappers, 0, components.length);
	return wrappers;
    }
	
    void objectSelected(ObjectWrapper wrapper)
    {
	for(Enumeration e = watchers.elements(); e.hasMoreElements(); ) {
	    ObjectBenchWatcher watcher = (ObjectBenchWatcher)e.nextElement();
	    watcher.objectSelected(wrapper);
	}
    }
	
    public void add(ObjectWrapper wrapper)
    {
	// check whether name is already taken

	String newname = wrapper.getName();
	int count = 1;

	while(hasObject(newname)) {
	    count++;
	    newname = wrapper.getName() + "_" + count;
	}
	wrapper.setName(newname);

	// add to bench

	super.add(wrapper);
	validate();
	repaint();
    }

    /**
     * Check whether the bench contains an object with name 'name'.
     * 
     * @param name  The name to check for.
     */
    public boolean hasObject(String name)
    {
	ObjectWrapper[] wrappers = getWrappers();

	for(int i=0; i<wrappers.length; i++)
	    if(wrappers[i].getName().equals(name))
		return true;
	return false;
    }

    /**
     * Remove (as inherited from JPanel) should never be called. Call remove
     * with the scopeId instead (see below).
     */
    public void remove(ObjectWrapper wrapper)
    {
	Debug.reportError("attempt to incorrectly remove object from bench");
    }

    /**
     * Remove an object from the object bench. When this is done, the object
     * is also removed from the scope of the package (so it is not accessible
     * as a parameter anymore) and the bench is redrawn.
     */
    public void remove(ObjectWrapper wrapper, String scopeId)
    {
	super.remove(wrapper);
	Debugger.debugger.removeObjectFromScope(scopeId, wrapper.getName());

	doLayout();
	invalidate();
	getParent().invalidate();
	repaint();
    }
}
