package bluej.views;

import java.lang.reflect.*;
import bluej.utility.Utility;

/**
 ** @version $Id: FieldView.java 244 1999-08-20 06:42:33Z mik $
 ** @author Michael Cahill
 **
 ** A representation of a Java field in BlueJ
 **/
public final class FieldView extends MemberView
{
    protected Field field;
    protected View type;
	
    /**
     ** Constructor.
     **/
    public FieldView(View view, Field field)
    {
	super(view);
		
	this.field = field;
    }

    /**
     ** Returns the name of this method as a String
     **/
    public String getName()
    {
	return field.getName();
    }
	
    /**
     ** Returns a Class object that represents the type of the field represented
     **  by this object.
     **/
    public View getType()
    {
	if(type == null)
	    type = View.getView(field.getType());
		
	return type;
    }

    /**
     ** Returns a string describing this Method.
     **/
    public String toString()
    {
	return field.toString();
    }
	
    public int getModifiers()
    {
	return field.getModifiers();
    }
	
    public String getShortDesc()
    {
	StringBuffer sb = new StringBuffer();
	sb.append(View.getTypeName(field.getType()));
	sb.append(" ");
	sb.append(field.getName());
	return sb.toString();
    }

    public String getLongDesc()
    {
        return getShortDesc();        
    }
    
    /**
     ** Returns a string describing this Field in a human-readable format
     **/
    public String getSignature()
    {
	StringBuffer sb = new StringBuffer();
	//		sb.append(View.getTypeName(field.getType()));
	//		sb.append(" ");
	sb.append(field.getName());
	return sb.toString();
    }
}
