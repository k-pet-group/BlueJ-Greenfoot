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
 * @version $Id: CallRecord.java 1003 2001-11-01 06:42:01Z ajp $
 */
public abstract class CallRecord
{
    // ======= static (factory) section =======

    private static Map callRecords = new HashMap();

    public static CallRecord getCallRecord(String name)
    {
        CallRecord existingRecord = (CallRecord) callRecords.get(name);

        return existingRecord;
    }

    public static CallRecord getCallRecord(String instanceName, CallableView theCall,
                                            String[] args)
    {
        ConstructorCallRecord newRecord = new ConstructorCallRecord(instanceName, theCall, args);

        callRecords.put(instanceName, newRecord);

        return newRecord;
    }

    public static CallRecord addMethodCallRecord(String instanceName, String methodName,
                                                    CallableView theCall, String[] args)
    {
        ConstructorCallRecord ccr = (ConstructorCallRecord) getCallRecord(instanceName);

        if (ccr == null)
            throw new IllegalArgumentException();

        MethodCallRecord mcr = new MethodCallRecord(ccr, methodName, theCall, args);

        ccr.addMethodCall(mcr);

        return mcr;
    }

    // ======= instance section =======

    protected String name;
    protected String classType;
    protected List callArgs = new ArrayList();  // of type CallArg

    protected CallRecord(String name, CallableView theCall, String[] args)
    {
        this.name = name;
        classType = theCall.getClassName();

        if (args != null) {
            for(int a = 0; a < args.length; a++) {
                CallRecord cr;

                if((cr = CallRecord.getCallRecord(args[a])) != null) {
                    this.callArgs.add(new ReferenceCallArg(cr));
                } else {
                    this.callArgs.add(new LiteralCallArg(args[a]));
                }
            }
        }
    }

    String getName()
    {
        return name;
    }

    public abstract String dump(int tablevel, String name, boolean declare);

    protected abstract class CallArg 
    {
        public abstract boolean preConstruct();    

        public abstract String dump(int tablevel, String name, boolean dec);
    }

    protected class LiteralCallArg extends CallArg
    {

        private String literal;

        LiteralCallArg(String lit) {
            literal = lit;
        }

        public boolean preConstruct() {
            return false;
        }

        public String getLiteral() {
            return literal;
        }
        
        public String dump(int tablevel, String name, boolean dec)
        {
            return "XXX";
        }

        public String toString() {
            return getLiteral();
        }
    }

    protected class ReferenceCallArg extends CallArg
    {
        private CallRecord reference;

        ReferenceCallArg(CallRecord ref) {
            reference = ref;
        }

        public boolean preConstruct() {
            return true;
        }

        public CallRecord getReference() {
            return reference;
        }

        public String dump(int tablevel, String name, boolean dec)
        {
            return getReference().dump(tablevel, name, dec);
        }

        public String toString() {
            return "CR";
        }

    }

}
