package bluej.extensions;

import com.sun.jdi.*;

/**
 * This is an equivalent to java.lang.reflect.Array for Arrays objects in BlueJ.
 * It allows you to get items of an array Object.
 * 
 * @version $Id: BArray.java 1815 2003-04-10 11:13:50Z damiano $
 */

/*
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class BArray 
{
  /**
   * Given an array object contained inside a BObject, returns the item at the given index.
   * This returns an Object since this can return primitive types wrappers for most cases.
   * Ex: in case you have arrays of int, long, boolean and so on.
   * In case the array is composed of real objects then what will be returned is 
   * a BObject itself, this is also in the case that it is a nested array.
   * 
   * @param thisArray This must be an array object of which you want the given item.
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