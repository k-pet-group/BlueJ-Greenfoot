package bluej.tester;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import java.util.Enumeration;
import java.util.Vector;

import bluej.utility.Debug;
import bluej.debugger.*;

/**
 * The panel that displays unit test objects
 *
 * @author  Michael Cahill (ObjectBench.java)
 * @author  Andrew Patterson
 * @version $Id: UnitTestBench.java 1012 2001-11-30 01:26:31Z ajp $
 */
public class UnitTestBench extends JPanel
{
    static final int WIDTH = 3 * (FixtureWrapper.WIDTH + 10);
    static final int HEIGHT = FixtureWrapper.HEIGHT;

    public UnitTestBench()
    {
        setLayout(new FlowLayout(FlowLayout.LEFT));
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

    /**
     * This component will raise ObjectBenchEvents when nodes are
     * selected in the bench. The following functions manage this.
     */

    public void addObjectBenchListener(ObjectBenchListener l) {
        listenerList.add(ObjectBenchListener.class, l);
    }

    public void removeObjectBenchListener(ObjectBenchListener l) {
        listenerList.remove(ObjectBenchListener.class, l);
    }

    // notify all listeners that have registered interest for
    // notification on this event type.
    void fireObjectEvent(ObjectWrapper wrapper)
    {
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

    public void add(FixtureWrapper fw)
    {
        super.add(fw);
        revalidate();
        repaint();
    }

    /**
     * Helper function to return all the wrappers stored in this object
     * bench in an array
     */
    private ObjectWrapper[] getWrappers()
    {
        Component[] components = getComponents();
        ObjectWrapper[] wrappers = new ObjectWrapper[components.length];
        System.arraycopy(components, 0, wrappers, 0, components.length);
        return wrappers;
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
     * Remove all objects from the object bench.
     */
    public void removeAll(String scopeId)
    {
        ObjectWrapper[] wrappers = getWrappers();

        for(int i=0; i<wrappers.length; i++) {
            super.remove(wrappers[i]);
            Debugger.debugger.removeObjectFromScope(
                                scopeId, wrappers[i].getName());
        }

    	revalidate();
        repaint();
    }
}
