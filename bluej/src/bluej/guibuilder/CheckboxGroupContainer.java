package javablue.GUIBuilder;

import java.util.Vector;
import java.util.Enumeration;
import java.awt.CheckboxGroup;
import java.io.Serializable;


/**
 * A class used to store all GUICheckbox groups.
 *
 * Created: Oct 2, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class CheckboxGroupContainer implements Serializable
{
    private Vector nameVector = new Vector();
    private Vector countVector = new Vector();
    private Vector groupVector = new Vector();


    /**
     * Constructs a new CheckboxGroupContainer.
     */
    public CheckboxGroupContainer()
    {
    }


    /**
     * Returns the CheckboxGroup associated with the specified key.
     *
     * @param name  The name of the checkbox group.
     * @return	    The associated CheckboxGroup.
     */
    public CheckboxGroup getGUICheckboxGroup (String name)
    {
	int index = nameVector.indexOf(name);
	if (index>=0)
	    return (CheckboxGroup)(groupVector.elementAt(index));
	else
	    return null;
    }


    /**
     * Returns a vector of strings with all the checkbox groups in this
     * container.
     *
     * @return	    A string vector of checkbox group names.
     */
    public Enumeration getGroups ()
    {
	return nameVector.elements();
    }


    /**
     * Adds a new checkbox group to this container.
     *
     * @param name The name of the new group.
     */
    public void addGroup (String name)
    {
	nameVector.addElement(name);
	countVector.addElement(new Integer(0));
	groupVector.addElement(new CheckboxGroup());
    }


    /**
     * Delete a group from this container. A group is only deleted if it's
     * reference count is zero.
     *
     * @param name The name of the group to be deleted.
     * @return true if the group where deleted, false otherwise.
     */
    public boolean deleteGroup (String name)
    {
	int index = nameVector.indexOf(name);
	if (index>=0)
	{
	    int count = ((Integer)countVector.elementAt(index)).intValue();
	    if (count==0)
	    {
		nameVector.removeElementAt(index);
		countVector.removeElementAt(index);
		groupVector.removeElementAt(index);
		return true;
	    }
	}
	return false;
    }


    /**
     * Increments the reference count of the specified group by one. This must
     * be done every time a GUICheckbox is added to a checkbox group.
     *
     * @param name The name of the group.
     */
    public void incReference (String name)
    {
	adjustValue(name, 1);
    }


    /**
     * Decrements the reference count of the specified group by one. This must
     * be done every time a GUICheckbox is removed from a checkbox group.
     *
     * @param name The name of the group.
     */
    public void decReference (String name)
    {
	adjustValue(name, -1);
    }


    /**
     * Generates the Java code used to make all the checkbox groups with
     * referencecounts above zero.
     *
     * @return The generated code.
     */
    public ComponentCode generateCode ()
    {
	ComponentCode code = new ComponentCode();

	for (int index=0; index<nameVector.size(); index++)
	{
	    if (((Integer)countVector.elementAt(index)).intValue() > 0)
	    {
		code.addGlobal ("private CheckboxGroup "+nameVector.elementAt(index));
		code.addGlobal (" = new CheckboxGroup();\n");
	    }
	}
	return code;
    }


    /**
     * Adjust the reference count of the specified group by the specified
     * value.
     *
     * @param name  The name of the group.
     * @param delta The value added to the reference count. (this can be negative)
     */
    private void adjustValue (String name, int delta)
    {
	int index = nameVector.indexOf(name);
	if (index>=0)
	{
	    int count = ((Integer)countVector.elementAt(index)).intValue();
	    countVector.setElementAt(new Integer(count+delta), index);
	}
    }
}
