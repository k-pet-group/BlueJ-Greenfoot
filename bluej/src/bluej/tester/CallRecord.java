package bluej.tester;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import bluej.utility.Utility;
import bluej.utility.Debug;
import java.util.Enumeration;

/**
 * Records a single user interaction with the object construction/
 * method call mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: CallRecord.java 1000 2001-10-29 03:06:09Z ajp $
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
        CallRecord newRecord = new CallRecord(theCall, args);

        callRecords.put(instanceName, newRecord);

        return newRecord;
    }


    // ======= instance section =======

    private CallableView theCall;
    private List args;

    private CallRecord(String instanceName, CallableView theCall,
                         String[] args, Component[] objectBench)
    {



    }
}
