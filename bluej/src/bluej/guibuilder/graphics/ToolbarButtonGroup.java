package javablue.GUIGraphics;

import java.awt.*;
import java.util.Vector;
import javablue.GUIGraphics.*;

/**
 * This class is used to group a set of ToolbarButtons together on one group.
 *
 * Exactly one toolbar button in a ToolbarButtonGroup can be in the "on" state
 * at any given time. Pushing any button sets its state to "SELECTED" or
 * "PERMANENT" and forces any other button that is in the "on" state into the
 * "UNSELECTED" state. 
 *
 * Created: Oct 2, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 * @see GUIGraphics.ToolbarButton
 */
public class ToolbarButtonGroup
{
    private Vector vButton = new Vector(10,5);// initial capacity 10 increment 5
    private int current = -1;


    /**
     * Constructs a new ToolbarButtonGroup.
     */
    public ToolbarButtonGroup()
    {
    }


    /**
     * Adds a button to this group.
     *
     * @param button The Button to be added.
     *
     * @see GUIGraphics.ToolbarButtonGroup#removeButton
     */
    public void addButton(ToolbarButton button)
    {
	vButton.addElement(button);
	button.setGroup(this);
    }


    /**
     * Removes a button from this group.
     *
     * @return The success of the operation. true means successful and false indicates a failure.
     *
     * @see GUIGraphics.ToolbarButtonGroup#addButton
     */
    public boolean removeButton(ToolbarButton b)
    {
	return(vButton.removeElement(b));
    }


    /**
     * Returns the selected button in this group. In case no button is selected
     * a null value is returned.
     *
     * @return The selected button or null.
     */
    public ToolbarButton getSelectedButton()
    {
	if(current == -1)
	    return null;
	else
	    return (ToolbarButton)vButton.elementAt(current);
    }


    /**
     * Unselects all buttons in this group.
     */
    public void unPopAll()
    {
	if(current!=-1)
	    ((ToolbarButton)vButton.elementAt(current)).setState(ToolbarButton.UNSELECTED);
    }


    /**
     * Returns the number of buttons belonging to this group
     *
     * @return The number of buttons.
     */
    public int getNumberOfButtons()
    {
	return vButton.size();
    }


    /**
     * Changes the state of the specified button to the specified state.
     *
     * @param button	The button to change the state of.
     * @param state	The new state of the specified button.
     *
     * @see GUIGraphics.ToolbarButton#UNSELECTED
     * @see GUIGraphics.ToolbarButton#SELECTED
     * @see GUIGraphics.ToolbarButton#PERMANENT
     */
    public void changeState(ToolbarButton button,int state)
    {
	int index = vButton.indexOf(button);
	if(index==-1)
	    return;
    
	if(index!=current)
	{
	    if(state==ToolbarButton.SELECTED || state==ToolbarButton.PERMANENT)
	    {
		((ToolbarButton)(vButton.elementAt(index))).setStateFromGroup(state);
		if(current!=-1) // Unselect the current selected button.
		    ((ToolbarButton)(vButton.elementAt(current))).setStateFromGroup(ToolbarButton.UNSELECTED);
		current = index;
	    }
	}
	else
	{
	    if(state==ToolbarButton.UNSELECTED)
	    {
		((ToolbarButton)(vButton.elementAt(current))).setStateFromGroup(ToolbarButton.UNSELECTED);
		current = -1;
	    }
	    else if(state==ToolbarButton.PERMANENT)
		((ToolbarButton)(vButton.elementAt(current))).setStateFromGroup(ToolbarButton.PERMANENT);
	}
    }
}
