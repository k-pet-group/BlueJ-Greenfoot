package bluej.views;

import java.lang.reflect.*;
import bluej.utility.Utility;
import bluej.utility.Debug;

/**
 ** @version $Id: ConstructorView.java 202 1999-07-22 07:45:35Z mik $
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
	public String getSignature(boolean includeparamnames)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(cons.getName());
		sb.append("(");
		Class[] params = cons.getParameterTypes();
		for(int j = 0; j < params.length; j++)
		{
			sb.append(View.getTypeName(params[j]));
			if(comment != null && includeparamnames) {
			    String paramname = comment.getParamName(j);

                if(paramname != null) {
                    sb.append(" ");
                    sb.append(paramname);
                }
            }
			if (j < (params.length - 1))
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}

	public String getSignature()
	{
	    return getSignature(true);
	}
	/**
	 ** Get a short String describing this member
	 **/
	public String getShortDesc()
	{
        return getSignature(true);
	}
	
	/**
	 ** Get a longer String describing this member
	 **/
	public String getLongDesc()
	{
		return getSignature(true);
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
