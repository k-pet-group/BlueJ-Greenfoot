package bluej.views;

import java.lang.reflect.*;
import bluej.utility.Utility;

/**
 ** @version $Id: ConstructorView.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** A representation of a Java constructor in BlueJ
 **/
public final class ConstructorView extends MemberView
{
	protected Constructor cons;
	
	/**
	 ** Constructor.
	 **/
	public ConstructorView(View view, Constructor cons)
	{
		super(view);
		
		this.cons = cons;
	}
	
	/**
	 ** Returns a string describing this Constructor.
	 **/
	public String toString()
	{
		return cons.toString();
	}
	
	public int getModifiers()
	{
		return cons.getModifiers();
	}
	
	/**
	 ** @returns a boolean indicating whether this method has parameters
	 **/
	public boolean hasParameters()
	{
		return (cons.getParameterTypes().length > 0);
	}
	
	/**
	 ** Returns a string describing this Method in a different format
	 **/
	public String getSignature()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("new ");
		sb.append(cons.getName());
		sb.append("(");
		Class[] params = cons.getParameterTypes();
		for(int j = 0; j < params.length; j++)
		{
			sb.append(View.getTypeName(params[j]));
			if (j < (params.length - 1))
				sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 ** Count of constructor's parameters
	 ** @returns the number of parameters
	 **/
	public int getParameterCount()
	{
		return (cons.getParameterTypes().length);
	}


	/**
	 ** Get an array of Class objects representing constructor's parameters
	 ** @returns array of Class objects
	 **/
	public Class[] getParameters()
	{
		return cons.getParameterTypes();
	}


}
