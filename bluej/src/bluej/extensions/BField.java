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
 * This is similar to the Reflection Field<P>
 * The main reason to have a field coming from a Class and not from an Object is that
 * logically we should be able to get static methods without having objects around.
 * Reflection states that to get a static field we can use a Field and pass null as the object to work on.<P>
 * NOTE: the get method returns an Object, in most cases it is a String, Integer, Long and so on BUT
 * when a real Object is actually returned it is encapsulated into a BObject. You MUST look for this.
 * Damiano
 */
public class BField
{
    private FieldView bluej_view;

    BField (FieldView i_bluej_view )
    {
        bluej_view = i_bluej_view;
    }        

    /**
     * Used to see if this field matches with the given criteria
     */
    public boolean matches ( String fieldName )
        {
        // Who is so crazy to give me a null name ?
        if ( fieldName == null ) return false;

        return fieldName.equals(getName());
        }


    /**
     * The name of the Field, as from reflection.
     * Tested ok, 070303 Damiano
     */
    public String getName()
        {
        return bluej_view.getName();
        }

    /**
     * The type of the field, as from reflection
     * Tested ok, 070303 Damiano
     */
    public Class getType()
        {
        return bluej_view.getType().getViewClass();
        }


    /**
     * When you are inspecting a static Field use this one to get hold
     * of a reference to the debgged Class
     */
    private Object getStaticField (BObject onThis)
      {
      Package bluej_pkg = onThis.getBluejPackage();
      DebuggerClassLoader loader = bluej_pkg.getRemoteClassLoader();

      View parentView = bluej_view.getDeclaringView();
      String className = parentView.getQualifiedName();

      System.out.println ("Parent Class name="+className);
      
      DebuggerClass debuggerClass = Debugger.debugger.getClass(className, loader);
      if ( debuggerClass == null ) 
        {
        Debug.message("BField.getStatucField: Class="+className+" Field="+getName()+" ERROR: cannod get debuggerClass");
        return null;
        }

      int staticCount=debuggerClass.getStaticFieldCount();
      String wantField = getName();
      DebuggerObject debugObj=null;
      for ( int index=0; index<staticCount; index++ )
        {
        if ( wantField.equals(debuggerClass.getStaticFieldName(index)) )
          {
          debugObj = debuggerClass.getStaticFieldObject(index);
          break;
          }
        }

      // No need to compalin about it it may not be a static field...
      if ( debugObj == null ) 
        {
        Debug.message("BField.getStatucField: Class="+className+" Field="+getName()+" DEBUG: fieldObject==null");
        return null;
        }

      ObjectReference objRef = debugObj.getObjectReference();

// I need a way to get hold of the value in a non coded form.....

      
      Debug.message("Got objRef="+objRef);
      ReferenceType type = objRef.referenceType();
      Debug.message("Got type="+type);
      Field thisField = type.fieldByName (getName());
      if ( thisField == null ) return null;
      Debug.message("Got thisField");
       
      return getVal(bluej_pkg, objRef.getValue(thisField));
      }


    /**
     * Gets this Filed Value on the given BObject
     */
    public Object get ( BObject onThis )
        {
        // If someone gives me a null it means that he wants a static field
        if ( onThis == null ) return getStaticField(onThis);
        
        ObjectReference objRef = onThis.getObjectReference();

        ReferenceType type = objRef.referenceType();

        Field thisField = type.fieldByName (bluej_view.getName());
        if ( thisField == null ) return null;
       
        Package bluej_pkg = onThis.getBluejPackage();
        return getVal(bluej_pkg, objRef.getValue(thisField));
        }


    /**
     * Utility to avoid duplicated code
     */
    private Object getVal ( Package bluej_pkg, Value val )
        {
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
          PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
          return new BObject ( new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val), getName()));
          }

        return val.toString();
        }
    }