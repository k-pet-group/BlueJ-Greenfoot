package bluej.debugger;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import bluej.utility.Utility;
import bluej.utility.Debug;
import java.util.Enumeration;

/** 
 ** Manages an invocation history of arguments used in a package when objects 
 ** created on the ObjectBench
 **
 ** @author Bruce Quig
 **
 **/
public class CallHistory
{
    private Map objectTypes = null;
    private List objectClasses = null;
    private List objectParams = null;

    private int historyLength;

    static final int DEFAULT_LENGTH = 6;

    static final String INT_NAME = "int";
    static final String BOOLEAN_NAME = "boolean";
    static final String LONG_NAME = "long";
    static final String FLOAT_NAME = "float";
    static final String DOUBLE_NAME = "double";
    static final String SHORT_NAME = "short";
    static final String STRING_NAME = "java.lang.String"; 


    public CallHistory()
    {
	this(DEFAULT_LENGTH);
    }


    public CallHistory(int length)
    {
	historyLength = length;
	objectTypes = new HashMap(8);
	objectTypes.put(INT_NAME, new ArrayList(length));
	objectTypes.put(LONG_NAME, new ArrayList(length));
	objectTypes.put(BOOLEAN_NAME, new ArrayList(length)); 
	objectTypes.put(FLOAT_NAME, new ArrayList(length));
	objectTypes.put(DOUBLE_NAME, new ArrayList(length));
	objectTypes.put(SHORT_NAME, new ArrayList(length));
	objectTypes.put(STRING_NAME, new ArrayList(length));
	objectClasses = new ArrayList();
	objectParams = new ArrayList();

    }


    /**
     ** Gets the appropriate history for the specified data type
     ** 
     ** @param objectType  the name of the object's class
     ** @return the List containing the appropriate history of invocations
     **/	
    public List getHistory(Class objectClass)
    {
	List history = null;
	// if listed in hashtable ie primitive or String
	if( objectTypes.containsKey(objectClass.getName())) {
	    history = (List)objectTypes.get(objectClass.getName());
	}
	// otherwise get general object history
	else {
	    history = new ArrayList();
	    for(int i = 0; i < objectClasses.size(); i++) {
		// if object parameter can be assigned from element in Class 
		// vector add to history
		if (objectClass.isAssignableFrom((Class)objectClasses.get(i)))
		    history.add(objectParams.get(i));
	    }
	}
	return history;
    }


    /**
     ** Adds a call to the history of a particular datatype
     ** 
     ** @param objectType  the object's class
     ** @param argument  the parameter 
     ** @return the List containing the appropriate history of invocations
     **/
    public void addCall(Class objectType, String argument)
    {
	if(argument != null) {
	    // if a primitive or String
	    if(objectTypes.containsKey(objectType.getName())) {

		List history = getHistory(objectType);
		int index = history.indexOf(argument);
	
		// if first no change
		if( index != 0) {
		    // if already there remove
		    if(index > 0)
			history.remove(index);
		    history.add(0, argument);
		}
		// trim to size if necessary
		if(history.size() > historyLength)
		    history.remove(historyLength);
	    }
	    //else add to other object's class and param vectors
	    else {
		int index = objectParams.indexOf(argument);
		
		// if first no change
		if( index != 0) {
		    // if already there remove
		    if(index > 0) {
			objectParams.remove(index);
			objectClasses.remove(index);
		    }
		    objectClasses.add(0, objectType);
		    objectParams.add(0, argument);
		}
	    }
	}
    }
}
