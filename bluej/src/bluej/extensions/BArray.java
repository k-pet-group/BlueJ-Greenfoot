package bluej.extensions;

import com.sun.jdi.*;

/**
 * A wrapper for an array object in BlueJ.
 * Behaviour is similar to the Java reflection API.
 * 
 * @version $Id: BArray.java 1965 2003-05-20 17:30:25Z damiano $
 */

/*
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class BArray 
{
  /**
   * Given a BlueJ array object, returns the item at the given index.
   * In the case that the array contains elements of primitive type (<code>int</code> etc.), 
   * the return value is of the appropriate Java wrapper type (<code>Integer</code> etc.).
   * In the case that the array is composed of BlueJ objects (including nested arrays) then 
   * an appropriate BObject will be returned. 
   * 
   * @param thisArray This must be an array object of which you want the given item.
   * @param itemIndex The index in the array where you want to peek.
   * 
   * @return an Object that encapsulate the specific item or null if not an array.
   */
  public static Object getValue ( BObject thisArray, int itemIndex )
    {
    ObjectReference objRef = thisArray.getObjectReference();

    if ( ! ( objRef instanceof ArrayReference ) ) return null;

    ArrayReference array = (ArrayReference)objRef;
    ReferenceType type = objRef.referenceType();
    
    Value val = array.getValue(itemIndex);

    return BField.doGetVal(thisArray.getBluejPackage(), "Array", val);
    }


}
