package bluej.guibuilder.graphics;

import java.awt.*;
import java.awt.event.*;


/**
 * A custom component representing an edit field that accepts only integers.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class IntegerField extends TextField
{
    private long value;
    private String text = new String();

    
    /**
     * Constructs a new IntegerField with "0" set as the initial value.
     **/
    public IntegerField()
    {
	super("0");
	this.text = "0";
	enableEvents(AWTEvent.KEY_EVENT_MASK);
    }


    /**
     * Constructs a new IntegerField with the initial value set as specified.
     *
     * @param text  The initial value. This must be a string that represents a valid integer.
     **/
    public IntegerField(String text)
    {
	super(text);
	this.text = text;
	enableEvents(AWTEvent.KEY_EVENT_MASK);
    }


    /**
     * Constructs a new IntegerField room for specified number of digits.
     *
     * @param cols  The width of the field.
     **/
    public IntegerField(int cols)
    {
	super(cols);
	enableEvents(AWTEvent.KEY_EVENT_MASK);
    }


    /**
     * Constructs a new IntegerField with the initial value set as specified
     * and room for specified number of digits.
     *
     * @param text  The initial value. This must be a string that represents a valid integer.
     * @param cols  The width of the field.
     **/
    public IntegerField(String text,int cols)
    {
	super(text,cols);
	this.text = text;
	enableEvents(AWTEvent.KEY_EVENT_MASK);
    }


    /**
     * This method is overwritten to filter all non-integers keystrokes away.
     *
     * @param e	    The key event.
     **/
    protected void processKeyEvent(KeyEvent e)
    {
	super.processKeyEvent(e);
	if(!getText().equals(""))
	{
	    try
	    {
        	Integer.parseInt(getText());
		text = getText();
	    }
            catch(NumberFormatException n)
            {
		setText(text);
            }
	}
	else
            text = "0";
    }


    /**
     * Sets the value of the IntegerField.
     *
     * @param text  The value. This must be a string that represents a valid integer.
     **/
    public void setText(String text)
    {
	this.text = text;
	super.setText(text);
    }


    /**
     * Sets the value of the IntegerField.
     *
     * @param value	The value.
     **/
    public void setValue(int value)
    {
	text = ""+value;
	setText(text);
    }


    /**
     * Returns the value of the field.
     *
     * @return The value.
     **/
    public int getValue()
    {
	int tmp = 0;
            
	try
	{
	    tmp  = Integer.parseInt("0"+getText());
	}
	catch(NumberFormatException e)
	{
	    System.out.println("Exception caught"+getText());
	}
	return tmp;
    }
}
