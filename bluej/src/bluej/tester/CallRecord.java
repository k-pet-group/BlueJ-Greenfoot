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
 * Records a single user interaction with the object construction/
 * method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: CallRecord.java 1001 2001-10-29 05:35:57Z ajp $
 */
public class CallRecord
{
    // ======= static (factory) section =======

    private static Map callRecords = new HashMap();

    public static CallRecord getCallRecord(String name)
    {
        CallRecord existingRecord = (CallRecord) callRecords.get(name);

        return existingRecord;
    }

    public static CallRecord getCallRecord(String instanceName, CallableView theCall,
                                            String[] args, Component[] objectBench)
    {
        CallRecord newRecord = new CallRecord(instanceName, theCall, args, objectBench);

        callRecords.put(instanceName, newRecord);

        return newRecord;
    }


    // ======= instance section =======

    private CallableView theCall;
    private List constructorArgs = new ArrayList();

    private CallRecord(String instanceName, CallableView theCall,
                         String[] args, Component[] objectBench)
    {
	if (args != null) {
	for(int a = 0; a < args.length; a++) {
		CallRecord cr;

		if((cr = CallRecord.getCallRecord(args[a])) != null) {
			this.args.add(cr);
		} else {
			this.args.add(args[a]);
		}
	}
	}
    }
	
	public String toString() {
		String res = "CR ";

		Iterator it = args.iterator();

		while(it.hasNext()) {
			res += it.next().toString();
		}
		
		return res;		
	}

}
