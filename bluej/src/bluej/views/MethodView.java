package bluej.views;

import java.lang.reflect.*;
import bluej.utility.Utility;

/**
 ** @version $Id: MethodView.java 156 1999-07-06 14:37:16Z ajp $
 ** @author Michael Cahill
 **
 ** A representation of a Java method in BlueJ
 **/
public final class MethodView extends MemberView
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
	public String getSignature()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(View.getTypeName(method.getReturnType()));
		sb.append(" ");
		sb.append(method.getName());
		sb.append("(");
		Class[] params = method.getParameterTypes();
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
