package bluej.tester;

import java.util.List;
import java.util.*;
import java.awt.*;
import javax.swing.*;

import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.views.*;
import bluej.debugger.*;
import bluej.runtime.*;

/**
 * Records a single user object construction
 *
 * @author  Andrew Patterson
 * @version $Id: ConstructorCallRecord.java 1002 2001-11-01 04:08:25Z ajp $
 */
public class ConstructorCallRecord extends CallRecord
{
    private List methodCalls = new ArrayList();

    private ConstructorCallRecord(String name, CallableView theCall, String[] args)
    {
        super(name, theCall, args);
    }

    public void addMethodCall(MethodCallRecord methodRecord)
    {
        methodCalls.add(methodRecord);
    }

	public String toString()
    {
		StringBuffer sb = new StringBuffer();

        sb.append(classType);
        sb.append(" ");
        sb.append(name);
        sb.append(" = new ");
        sb.append(classType);
        sb.append("(");

		Iterator it = callArgs.iterator();

		while(it.hasNext()) {
			sb.append(it.next().toString());
            if (it.hasNext())
                sb.append(", ");
		}
	    sb.append(");\n");

        Iterator mit = methodCalls.iterator();

        while(mit.hasNext()) {
            sb.append(mit.next().toString());
        }


		return sb.toString();		
	}
}
