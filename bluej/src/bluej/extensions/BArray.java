package bluej.extensions;

import com.sun.jdi.*;

/**
 * This is the equivalent of Reflection java.lang.reflect.Array.
 * It allows you to get items of an array Object, the implementation is partial at the moment, this
 * means that there are no methods to get primitive types directly.
 * 
 * @version $Id: BArray.java 1712 2003-03-20 10:39:46Z damiano $
 */
public class BArray 
{
  /**
   * Given an array object contained inside a BObject, returns the item at the given index.
   * This returns an Object since this can return primitive types wrappers for most cases.
   * Ex: in case you have arrays of int, long, boolean and so on.
   * In case the array is composed of real OBJECTS then what will be returned is 
   * a BObject itself, this is also in the case that it is a nested array....
   * 
   * @param thisArray This MUST be an array object of which you want the given item
   * @param itemIndex The index in the array where you want to peek.
   * 
   * @return an Object that encapsulate the specific item or null if not an array.
   */
  public static Object get ( BObject thisArray, int itemIndex )
    {
    ObjectReference objRef = thisArray.getObjectReference();

    if ( ! ( objRef instanceof ArrayReference ) ) return null;

    ArrayReference array = (ArrayReference)objRef;
    ReferenceType type = objRef.referenceType();
    
    Value val = array.getValue(itemIndex);

    return BField.getVal(thisArray.getBluejPackage(), "Array", val);
    }


}