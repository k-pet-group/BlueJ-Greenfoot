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
 * @version $Id: ConstructorCallRecord.java 1003 2001-11-01 06:42:01Z ajp $
 */
public class ConstructorCallRecord extends CallRecord
{
    private List methodCalls = new ArrayList();

    protected ConstructorCallRecord(String name, CallableView theCall, String[] args)
    {
        super(name, theCall, args);
    }

    public void addMethodCall(MethodCallRecord methodRecord)
    {
        methodCalls.add(methodRecord);
    }

    private void addTabs(StringBuffer sb, int tabs)
    {
        for(int i=0; i<tabs; i++)
            sb.append("\t");
    }

	public String dump(int tablevel, String newname, boolean includeDeclaration)
    {
        List argNames = new ArrayList();
        int constructArgCount = 0;
		StringBuffer sb = new StringBuffer();

        
        if(includeDeclaration) {
            addTabs(sb, tablevel);
            sb.append(classType);
            sb.append(" ");
            sb.append(newname);
            sb.append(";\n");
        }

        addTabs(sb, tablevel);
        sb.append("{\n");

		Iterator it = callArgs.iterator();

		while(it.hasNext()) {
			CallArg ca = (CallArg) it.next();

            if (ca.preConstruct()) {
                sb.append(ca.dump(tablevel+1, "var" + constructArgCount, true));
                constructArgCount++;
            }
		}

        addTabs(sb, tablevel+1);
        sb.append(newname);
        sb.append(" = new ");
        sb.append(classType);
        sb.append("(");

        it = callArgs.iterator();

        constructArgCount = 0;

		while(it.hasNext()) {
			CallArg ca = (CallArg) it.next();

            if (ca.preConstruct()) {
                sb.append("var" + constructArgCount++);
            } else
                sb.append(ca.toString());

            if (it.hasNext())
                sb.append(", ");
		}
	    sb.append(");\n");

        addTabs(sb, tablevel);
        sb.append("}\n");

/*        Iterator mit = methodCalls.iterator();

        while(mit.hasNext()) {
            sb.append(mit.next().toString());
        } */

		return sb.toString();		
	}
}
