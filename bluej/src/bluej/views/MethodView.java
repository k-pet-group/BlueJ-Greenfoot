package bluej.views;

import java.lang.reflect.*;
import bluej.utility.Utility;
import bluej.utility.Debug;

/**
 ** @version $Id: MethodView.java 202 1999-07-22 07:45:35Z mik $
 ** @author Michael Cahill
 **
 ** A representation of a Java method in BlueJ
 **/
public class MethodView extends MemberView
{
    protected Method method;
	
    protected View returnType;
	
    /**
     ** Constructor.
     **/
    public MethodView(View view, Method method)
    {
	super(view);
		
	this.method = method;
    }

    /**
     ** Returns the name of this method as a String
     **/
    public String getName()
    {
	return method.getName();
    }
	
    /**
     ** Returns a Class object that represents the formal return type
     ** of the method represented by this Method object.
     **/
    public View getReturnType()
    {
	if(returnType == null)
	    returnType = View.getView(method.getReturnType());
		
	return returnType;
    }

    /**
     ** Returns a string describing this Method.
     **/
    public String toString()
    {
	return method.toString();
    }
	
    public int getModifiers()
    {
	return method.getModifiers();
    }
	
    /**
     ** @returns a boolean indicating whether this method has no return value
     **/
    public boolean isVoid()
    {
	String resultName = getReturnType().getName();
	return "void".equals(resultName);
    }
	
    /**
     ** @returns a boolean indicating whether this method has parameters
     **/
    public boolean hasParameters()
    {
	return (method.getParameterTypes().length > 0);
    }

    /**
     ** Returns a string describing this Method in a human-readable format
     **/
    public String getSignature(boolean includeparamnames)
    {
	StringBuffer sb = new StringBuffer();
	Comment comment = getComment();

	sb.append(View.getTypeName(method.getReturnType()));
	sb.append(" ");
	sb.append(method.getName());
	sb.append("(");
	Class[] params = method.getParameterTypes();
	for(int j = 0; j < params.length; j++) {
	    sb.append(View.getTypeName(params[j]));
			
	    if(comment != null && includeparamnames) {
		String paramname = comment.getParamName(j);
		
		if(paramname != null) {
		    sb.append(" ");
		    sb.append(paramname);
		}
	    }
	    if (j < (params.length - 1))
		sb.append(", ");
	}
	sb.append(")");
	return sb.toString();
    }

    public String getSignature()
    {
        return getSignature(false);        
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
     ** @returns the number of parameters
     **/
    public int getParameterCount()
    {
	return (method.getParameterTypes().length);
    }


    /**
     ** Get an array of Class objects representing method's parameters
     ** @returns array of Class objects
     **/
    public Class[] getParameters()
    {
	return method.getParameterTypes();
    }

}
