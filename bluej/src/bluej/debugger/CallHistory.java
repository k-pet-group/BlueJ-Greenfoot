package bluej.debugger;


import java.util.Vector;
import java.util.Hashtable;
import bluej.utility.Utility;
import bluej.utility.Debug;
import java.util.Enumeration;

/** 
 ** @author Bruce Quig
 **
 ** Manages an invocation history of arguments used in a package when objects 
 ** created on the ObjectBench
 **/
public class CallHistory
{
    private Hashtable objectTypes = null;
    private Vector objectClasses = null;
    private Vector objectParams = null;

    private int historyLength;

    static final int DEFAULT_LENGTH = 6;
    static final int INSERTION_POINT = 0;  // front of history list

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
	objectTypes = new Hashtable(8);
	objectTypes.put(INT_NAME, new Vector(length));
	objectTypes.put(LONG_NAME, new Vector(length));
	objectTypes.put(BOOLEAN_NAME, new Vector(length)); 
	objectTypes.put(FLOAT_NAME, new Vector(length));
	objectTypes.put(DOUBLE_NAME, new Vector(length));
	objectTypes.put(SHORT_NAME, new Vector(length));
	objectTypes.put(STRING_NAME, new Vector(length));
	objectClasses = new Vector();
	objectParams = new Vector();

    }


    /**
     ** Gets the appropriate history for the specified data type
     ** 
     ** @param objectType  the name of the object's class
     ** @return the Vector containing the appropriate history of invocations
     **/	
    public Vector getHistory(Class objectClass)
    {
	Vector history = null;
	// if listed in hashtable ie primitive or String
	if( objectTypes.containsKey(objectClass.getName())) {
	    history = (Vector)objectTypes.get(objectClass.getName());
	}
	// otherwise get general object history
	else {
	    history = new Vector();
	    for(int i = 0; i < objectClasses.size(); i++) {
	      // if object parameter can be assigned from element in Class vector add to history
	      if (objectClass.isAssignableFrom((Class)objectClasses.elementAt(i)))
		history.addElement(objectParams.elementAt(i));
	    }
	}
	return history;
    }


    /**
     ** Adds a call to the history of a particular datatype
     ** 
     ** @param objectType  the object's class
     ** @param argument  the parameter 
     ** @return the Vector containing the appropriate history of invocations
     **/
    public void addCall(Class objectType, String argument)
    {
	  if(argument != null) {
	      // if a primitive or String
	      if(objectTypes.containsKey(objectType.getName())) {

		  Vector history = getHistory(objectType);
		  int index = history.indexOf(argument);
	
		  // if first no change
		  if( index != 0) {
		      // if already there remove
		      if(index > 0)
			  history.removeElementAt(index);
		      history.insertElementAt(argument, INSERTION_POINT);
		  }
		  // trim to size if necessary
		  if(history.size() > historyLength) {
		      history.setSize(historyLength);
		  }
	      }
	      //else add to other object's class and param vectors
	      else {
		  int index = objectParams.indexOf(argument);
		
		  // if first no change
		  if( index != 0) {
		      // if already there remove
		      if(index > 0) {
			  objectParams.removeElementAt(index);
			  objectClasses.removeElementAt(index);
		      }
		      objectClasses.insertElementAt(objectType, INSERTION_POINT);
		      objectParams.insertElementAt(argument, INSERTION_POINT);
		  }
	      }
	  }
    }
}
