package bluej.extensions;

import bluej.pkgmgr.*;
import com.sun.jdi.*;

/**
 * A wrapper for an array object in BlueJ.
 * Behaviour is similar to the Java reflection API.
 * 
 * @version $Id: BArray.java 2314 2003-11-10 14:49:48Z damiano $
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
   * @throws ProjectNotOpenException if the project to which this array belongs has been closed by the user.
   * @throws PackageNotFoundException if the package to which this array belongs has been deleted by the user.
   */
  public static Object getValue ( BObject thisArray, int itemIndex )
    throws ProjectNotOpenException, PackageNotFoundException
    {
    ObjectReference objRef = thisArray.getObjectReference();

    if ( ! ( objRef instanceof ArrayReference ) ) return null;

    ArrayReference array = (ArrayReference)objRef;
    ReferenceType type = objRef.referenceType();
    
    Value val = array.getValue(itemIndex);

    PkgMgrFrame aFrame = thisArray.getPackageFrame();
    return BField.doGetVal(aFrame, "Array", val);
    }


}
