package javablue.GUIBuilder;

import java.util.*;
import java.io.Serializable;

/**
 * A class used to store all component listeners.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ListenerContainer implements Serializable
{
    private Hashtable table = new Hashtable();
    private static int counters[] = {0,0,0,0,0,0,0,0,0,0,0,0,0};


    /**
     * Constructs a new ListenerContainer.
     */
    public ListenerContainer ()
    {
	// List of all components that can have a listener:
        String[] componentName = {"Button","Canvas","Checkbox","Choice",
	    "Label","List","Scrollbar","TextArea","TextField","Panel","Dialog","Frame","MenuItem"};

	// List of listeners possible for each component:
	String[][] compListeners = {
	    {"Component","Focus","Key","Mouse","MouseMotion","Action"},
	    {"Component","Focus","Key","Mouse","MouseMotion"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Item"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Item"},
	    {"Component","Focus","Key","Mouse","MouseMotion"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Action","Item"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Adjustment"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Text"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Text"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Container"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Window","Container"},
	    {"Component","Focus","Key","Mouse","MouseMotion","Window","Container"},
	    {"Action"}
	    };

	// Initialize container:
	ComponentListeners tmpCL;
	for (int i=0; i<componentName.length; i++)
	{
	    tmpCL = new ComponentListeners();
	    for (int t=0; t<compListeners[i].length; t++)
		tmpCL.addListenerType(compListeners[i][t]+"Listener");
	    table.put(componentName[i], tmpCL);
	}
    }


    /**
     * Gets the possible listener types for the specified component.
     *
     * @param component The type of component, ie. "Button" or "MenuItem".
     * @return An enumeration of strings representing the possible types.
     */
    public Enumeration getListenerTypes (String component)
    {
	ComponentListeners tmpCL = (ComponentListeners)table.get(component);
	if (tmpCL!=null)
	    return tmpCL.getTypes();
	else
	    return (new Hashtable()).elements();
    }


    /**
     * Gets the listeners for the specified component and specified listener
     * type.
     *
     * @param component	The type of component, ie. "Button" or "MenuItem".
     * @param type	The type of listener, ie. "ActionListener".
     * @return An enumeration of strings representing the listeners.
     */
    public Enumeration getListeners (String component, String type)
    {
	ComponentListeners tmpCL = (ComponentListeners)table.get(component);
	if (tmpCL!=null)
	    return tmpCL.getListeners (type);
	else
	    return (new Hashtable()).elements();
    }


    /**
     * Adds a new listener to the container.
     *
     * @param component	The type of component, ie. "Button" or "MenuItem".
     * @param pair	The listener pair to add.
     */
    public void addListener (String component, ListenerPair pair)
    {
	ComponentListeners tmpCL = (ComponentListeners)table.get(component);
	if (tmpCL!=null)
	    tmpCL.addListener (pair);
    }


    /**
     * Deletes a listener from the container.
     *
     * @param component	The type of component, ie. "Button" or "MenuItem".
     * @param pair	The listener pair to delete.
     * @return	The succes of the operation. true means success, false means failure.
     */
    public boolean deleteListener (String component, ListenerPair pair)
    {
	ComponentListeners tmpCL = (ComponentListeners)table.get(component);
	if (tmpCL!=null)
	    return tmpCL.deleteListener (pair);
        return true;
    }


    /**
     * Increments the reference count of the specified listener by one. This
     * must be done every time a listener is added to a component.
     *
     * @param component	The type of component, ie. "Button" or "MenuItem".
     * @param pair	The listener pair to increment.
     */
    public void incReference (String component, ListenerPair pair)
    {
	ComponentListeners tmpCL = (ComponentListeners)table.get(component);
	if (tmpCL!=null)
	    tmpCL.incReference (pair);
    }


    /**
     * Decrements the reference count of the specified listener by one. This
     * must be done every time a listener is removed from a component.
     *
     * @param component	The type of component, ie. "Button" or "MenuItem".
     * @param pair	The listener pair to decrement.
     */
    public void decReference (String component, ListenerPair pair)
    {
	ComponentListeners tmpCL = (ComponentListeners)table.get(component);
	if (tmpCL!=null)
	    tmpCL.decReference (pair);
    }


    /**
     * Generates the Java code used to make all the listeners with a
     * referencecount above zero.
     *
     * @return The generated code.
     */
    public ComponentCode generateCode ()
    {
	// The comment to insert in the methods:
	// name, eventtype, [method, description]...
	String[][] methods = {
	    {"ActionListener", "Action",
		"actionPerformed", "Invoked when an action occurs."},
	    {"AdjustmentListener", "Adjustment",
		"adjustmentValueChanged", "Invoked when the value of the adjustable has changed."},
	    {"ComponentListener", "Component",
		"componentHidden", "Invoked when component has been hidden.",
		"componentMoved", "Invoked when component has been moved.",
		"componentResized", "Invoked when component has been resized.",
		"componentShown", "Invoked when component has been shown."},
	    {"ContainerListener", "Container",
		"componentAdded", "Invoked when a component has been added to the container.",
		"componentRemoved", "Invoked when a component has been removed from the container."},
	    {"FocusListener", "Focus",
		"focusGained", "Invoked when a component gains the keyboard focus.",
		"focusLost", "Invoked when a component loses the keyboard focus."},
	    {"ItemListener", "Item",
		"itemStateChanged", "Invoked when an item's state has been changed."},
	    {"KeyListener", "Key",
		"keyPressed", "Invoked when a key has been pressed.",
		"keyReleased", "Invoked when a key has been released.",
		"keyTyped", "Invoked when a key has been typed."},
	    {"MouseListener", "Mouse",
		"mouseClicked", "Invoked when the mouse has been clicked on a component.",
		"mouseEntered", "Invoked when the mouse enters a component.",
		"mouseExited", "Invoked when the mouse exits a component.",
		"mousePressed", "Invoked when a mouse button has been pressed on a component.",
		"mouseReleased", "Invoked when a mouse button has been released on a component."},
	    {"MouseMotionListener", "Mouse",
		"mouseDragged", "Invoked when a mouse button is pressed on a component and then dragged.",
		"mouseMoved", "Invoked when the mouse button has been moved on a component (with no buttons no down)."},
	    {"TextListener", "Text",
		"textValueChanged", "Invoked when the value of the text has changed."},
	    {"WindowListener", "Window",
		"windowActivated", "Invoked when a window is activated.",
		"windowClosed", "Invoked when a window has been closed.",
		"windowClosing", "Invoked when a window is in the process of being closed.",
		"windowDeactivated", "Invoked when a window is de-activated.",
		"windowDeiconified", "Invoked when a window is de-iconified.",
		"windowIconified", "Invoked when a window is iconified.",
		"windowOpened", "Invoked when a window has been opened."}
	    };

	ComponentCode code = new ComponentCode();

	ComponentListeners cl;
	Enumeration enumType, enumListener;
	Enumeration enumComponent = table.keys();
	boolean anyListeners = false;

	while (enumComponent.hasMoreElements())
	{
	    String component = (String)enumComponent.nextElement();
	    cl = (ComponentListeners)table.get (component);
	    enumType = cl.getTypes();
	    while (enumType.hasMoreElements())
	    {
		ListenerPair pair = new ListenerPair();
		pair.type = (String)enumType.nextElement();
		enumListener = cl.getListeners(pair.type);
		while (enumListener.hasMoreElements())
		{
		    pair.name = (String)enumListener.nextElement();
		    if (cl.getReferenceCount(pair)>0)
		    {
			for (int i=0; i<methods.length; i++)
			{
			    if (methods[i][0].equals(pair.type))
			    {
				anyListeners = true;
				code.addCreation("private class "+pair.type+counters[i]+" implements "+pair.type+"\n{\n");
				for (int t=2; t<methods[i].length; t+=2)
				{
				    code.addCreation("public void "+methods[i][t]+" ("+methods[i][1]+"Event e)\n{\n");
				    code.addCreation("// "+methods[i][t+1]+"\n}\n\n");
				}
				code.addCreation("}\n"+pair.type+counters[i]+" "+pair.name+" = new "+pair.type+(counters[i]++)+"();"+"\n\n");
			    }
			}
		    }
		}
	    }
	}
	if (anyListeners)
	    code.addGlobal("import java.awt.event.*;\n");
	return code;
    }



    /**
     * A inner class used to store all listeners for a certain component.
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    class ComponentListeners implements Serializable
    {
	private Hashtable table = new Hashtable();


	/**
	 * Adds a new listener type to the component.
	 *
	 * @param type	The name of the new listener type.
	*/
	public void addListenerType (String type)
	{
	    Hashtable tmpTable = new Hashtable();
	    tmpTable.put("default"+type, new Integer(0));

	    table.put(type, tmpTable);
	}


	/**
	 * Gets the listener types for the component.
	 *
	 * @return An enumeration of strings representing the listener types.
	 */
	public Enumeration getTypes ()
	{
	    return table.keys();
	}


	/**
	 * Gets the specified listeners of the specified type for the component.
	 *
	 * @param type	The type of listener.
	 * @return An enumeration of strings representing the names of the listeners.
	 */
	public Enumeration getListeners (String type)
	{
            Vector listeners = new Vector();
	    Hashtable tmpTable = (Hashtable)table.get(type);
	    if (tmpTable!=null)
		return tmpTable.keys();
	    else
		return (new Hashtable()).elements();
	}


	/**
	 * Adds a new listener pair.
	 *
	 * @param pair The pair to add.
	 */
	public void addListener (ListenerPair pair)
	{
	    Hashtable tmpTable = (Hashtable)table.get(pair.type);
	    if (tmpTable!=null)
		tmpTable.put (pair.name, new Integer(0));
	}


	/**
	 * Deletes a listener pair.
	 *
	 * @param pair The pair to delete.
	 * @return The success of the operation. true means success.
	 */
	public boolean deleteListener (ListenerPair pair)
	{
            if(getReferenceCount(pair)!=0)
        	return false;
            Hashtable tmpTable = (Hashtable)table.get(pair.type);
	    if (tmpTable!=null)
		tmpTable.remove (pair.name);
            return true;
	}


	/**
	 * Increments the reference count of the specified listener pair by one.
	 *
	 * @param pair	The listener pair to increment.
	 */
	public void incReference (ListenerPair pair)
	{
	    Hashtable tmpTable = (Hashtable)table.get(pair.type);
	    if (tmpTable!=null)
	    {
		Integer count = (Integer)tmpTable.get(pair.name);
		if (count!=null)
		{
		    tmpTable.remove (pair.name);
		    tmpTable.put (pair.name, new Integer((count.intValue())+1));
		}
	    }
	}


	/**
	 * Decrements the reference count of the specified listener pair by one.
	 *
	 * @param pair	The listener pair to increment.
	 */
	public void decReference (ListenerPair pair)
	{
	    Hashtable tmpTable = (Hashtable)table.get(pair.type);
	    if (tmpTable!=null)
	    {
		Integer count = (Integer)tmpTable.get(pair.name);
		if (count!=null)
		{
		    tmpTable.remove (pair.name);
		    tmpTable.put (pair.name, new Integer((count.intValue())-1));
		}
	    }
	}


	/**
	 * Gets the reference count of the specified listener pair.
	 *
	 * @param pair	The listener pair to check.
	 * @return The number of references to this listener.
	 */
	public int getReferenceCount (ListenerPair pair)
	{
	    Hashtable tmpTable = (Hashtable)table.get(pair.type);
	    if (tmpTable!=null)
	    {
		Integer count = (Integer)tmpTable.get(pair.name);
		if (count!=null)
		    return count.intValue();
	    }
	    return 0;
	}
    }
}
