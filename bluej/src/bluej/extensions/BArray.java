package bluej.extensions;

import bluej.debugger.ObjectWrapper;
import bluej.debugger.jdi.JdiObject;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.*;

import java.lang.reflect.Modifier;
import bluej.pkgmgr.Package;
import bluej.views.*;
import bluej.debugger.*;
import bluej.utility.Debug;



/**
 * This is the equivalent of Reflection java.lang.reflect.Array.
 * It allows you to get items of an array Object.
 * The implementation may be partial at the moment.
 * As from the Reflection API all of he ones here are static Methods.
 * We could include them into the BField Class but we are tryng to be similar to Reflection API<p>
 * Damiano
 */
public class BArray 
{
  /**
   * <pre>I am returning an object since this can return primitive types for most cases
   * This is in case you have arrays of int, long, boolean and so on.
   * In case the array is composed of real OBJECTS then what will be returned is 
   * a BObject itself, this is also in the case that it is a nested array....
   * </pre>
   * 
   * @param thisObj This MUST be an array object of which you want the given item
   * @param itemIndex The index in the array where you want to peek.
   * 
   * @return an Object that encapsulate the specific item.
   */
  public static Object get ( BObject thisArray, int itemIndex )
    {
    ObjectReference objRef = thisArray.getObjectReference();

    if ( ! ( objRef instanceof ArrayReference ) ) return null;

    ArrayReference array = (ArrayReference)objRef;
    ReferenceType type = objRef.referenceType();
    
    Value val = array.getValue(itemIndex);
    if ( val == null ) return null;
        
    if (val instanceof StringReference) return ((StringReference) val).value();
    if (val instanceof BooleanValue) return new Boolean (((BooleanValue) val).value());
    if (val instanceof ByteValue)    return new Byte (((ByteValue) val).value());
    if (val instanceof CharValue)    return new Character (((CharValue) val).value());
    if (val instanceof DoubleValue)  return new Double (((DoubleValue) val).value());
    if (val instanceof FloatValue)   return new Float (((FloatValue) val).value());
    if (val instanceof IntegerValue) return new Integer (((IntegerValue) val).value());
    if (val instanceof LongValue)    return new Long (((LongValue) val).value());
    if (val instanceof ShortValue)   return new Short (((ShortValue) val).value());

    if (val instanceof ObjectReference)
      {
      PkgMgrFrame pmf = PkgMgrFrame.findFrame (thisArray.getBluejPackage());
      return new BObject ( new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val), thisArray.getInstanceName()));
      }

    return val.toString();
    }


}