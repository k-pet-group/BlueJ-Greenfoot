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
 * Records a single user method call.
 *
 * @author  Andrew Patterson
 * @version $Id: MethodCallRecord.java 1002 2001-11-01 04:08:25Z ajp $
 */
public class MethodCallRecord extends CallRecord
{
    ConstructorCallRecord parent;

    MethodCallRecord(ConstructorCallRecord parent, String methodName, CallableView theCall, String[] args)
    {
        super(methodName, theCall, args);

        this.parent = parent;
    }

	public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(parent.getName());
        sb.append(".");
        sb.append(getName());
        sb.append(");");

        Iterator it = callArgs.iterator();

        while(it.hasNext()) {
            sb.append(it.next().toString());
            if (it.hasNext())
                sb.append(", ");
        }
        sb.append(");\n");
        return sb.toString();
	}
}
