package bluej.debugger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class InspectorPanel extends JPanel
{

    protected bluej.debugger.DebuggerObject obj;

    public InspectorPanel()
    {
        super(new BorderLayout());
    }

    public abstract String[] getInspectedClassnames();

    public abstract String getInspectorTitle();

    public boolean initialize(bluej.debugger.DebuggerObject obj)
    {
        this.obj = obj;
        return true;
    }

    public abstract void refresh();

    public void addInspectorListener(InspectorListener l)
    {
        listenerList.add(InspectorListener.class, l);
    }

    public void removeInspectorListener(InspectorListener l)
    {
        listenerList.remove(InspectorListener.class, l);
    }

    // notify all listeners that have registered interest for
    // notification on this event type.
    public void fireInspectEvent(DebuggerObject obj)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == InspectorListener.class)
            {
                ((InspectorListener) listeners[i + 1]).inspectEvent(
                        new InspectorEvent(this,
                        InspectorEvent.INSPECT, obj));
            }
        }
    }

    public void fireGetEvent(DebuggerObject obj)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == InspectorListener.class)
            {
                ((InspectorListener) listeners[i + 1]).getEvent(
                        new InspectorEvent(this,
                        InspectorEvent.GET, obj));
            }
        }
    }
    
    /* Return the containing JFrame. 
       Useful for JDialog invokations */
    protected JFrame getJFrame()
    {
        java.awt.Container parent = getParent();
        while (!(parent instanceof JFrame))
        {
            parent = parent.getParent();
        }
        return (JFrame) parent;
    }

}
