package bluej.extensions;

import bluej.debugger.ObjectWrapper;
import bluej.debugger.jdi.JdiObject;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.*;

import bluej.pkgmgr.Package;
import bluej.views.*;
import bluej.debugger.*;
import bluej.utility.Debug;

/**
 * A wrapper for a field of a BlueJ class.
 * Similar to Reflection API.
 * The main reason to have a field coming from a Class and not from an Object is that
 * logically we should be able to get static Field without having objects.
 * Reflection states that to get a static field we can use a Field and pass null as the object to work on.
 * The get method returns an Object, in most cases it is a String, Integer, Long and so on but
 * when a real Object is actually returned it is encapsulated into a BObject. 
 * You must look for this.
 * 
 * @version $Id: BField.java 1817 2003-04-10 11:28:14Z damiano $
 */

/*
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 * Previous attempt by Clive Miller, University of Kent at Canterbury, 2002
 */
 
public class BField
{
    private FieldView bluej_view;
    private Package   bluej_package;
    
    BField (Package i_bluej_package, FieldView i_bluej_view )
    {
        bluej_package = i_bluej_package;
        bluej_view = i_bluej_view;
    }        

    /**
     * Used to see if this field matches with the given criteria.
     * 
     * @return true if it does, false othervide
     */
    public boolean matches ( String fieldName )
        {
        // Who is so crazy to give me a null name ?
        if ( fieldName == null ) return false;

        return fieldName.equals(getName());
        }


    /**
     * The name of the Field
     * Similar to reflection API.
     */
    public String getName()
        {
        // Tested ok, 070303 Damiano
        return bluej_view.getName();
        }

    /**
     * The type of the field.
     * Similar to Reflection API.
     */
    public Class getType()
        {
        // Tested ok, 070303 Damiano
        return bluej_view.getType().getViewClass();
        }


    /**
     * When you are inspecting a static Field use this one.
     */
    private Object getStaticField ()
      {
      String wantFieldName = getName();

      // I need to get the view of the parent of this Field
      // That must be the Class that I want to look after....
      View parentView = bluej_view.getDeclaringView();
      String className = parentView.getQualifiedName();

      // UFF, there seems to be no way to get the package from the view...
      // Maybe should ask Michael, when he has time...
      DebuggerClassLoader loader = bluej_package.getRemoteClassLoader();
      if ( loader == null ) 
        {
        // This is really an error
        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" ERROR: cannod get DebuggerClassLoader");
        return null;
        }
      
      DebuggerClass debuggerClass = Debugger.debugger.getClass(className, loader);
      if ( debuggerClass == null ) 
        {
        // This may not be an error, the class name may be wrong...
        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" WARNING: cannod get debuggerClass");
        return null;
        }

      // Now I want the Debugger object of that field.
      // I do it this way since there is no way to get it by name...
      int staticCount=debuggerClass.getStaticFieldCount();
      DebuggerObject debugObj=null;
      for ( int index=0; index<staticCount; index++ )
        {
        if ( wantFieldName.equals(debuggerClass.getStaticFieldName(index)) )
          {
          debugObj = debuggerClass.getStaticFieldObject(index);
          break;
          }
        }

      if ( debugObj == null ) 
        {
        // No need to complain about it it may not be a static field...
//        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" DEBUG: fieldObject==null");
        return null;
        }

      ObjectReference objRef = debugObj.getObjectReference();
      if ( objRef == null )
        {
        // WARNING At the moment it is not possible to have reflection behaviour
        // You NEED to have an instance of the class to get static fields...
        // Therefore.... this is normal behaviour and not an ERROR...
//        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" ERROR: ObjectReference==null");
        return null;
        }

      return getVal(bluej_package, wantFieldName, objRef);
      }


    /**
     * Gets this filed value on the given object.
     * This is similar to Reflection API.
     */
    public Object get ( BObject onThis )
        {
        // If someone gives me a null it means that he wants a static field
        if ( onThis == null ) return getStaticField();
        
        ObjectReference objRef = onThis.getObjectReference();

        ReferenceType type = objRef.referenceType();

        Field thisField = type.fieldByName (bluej_view.getName());
        if ( thisField == null ) return null;
       
        Package bluej_pkg = onThis.getBluejPackage();
        return getVal(bluej_pkg, bluej_view.getName(), objRef.getValue(thisField));
        }


    /**
     * WARNING: This is COPIED into the extension/event.
     * if you change something you MUST keep it in sync.
     * The reason of the copy is simply because javadoc does not (yet) have a way to hide public methods.
     * Utility to avoid duplicated code. To be used from within the bluej.extensions package
     * Given a Value that comes from th remote debugger machine, converts it into somethig
     * that is usable. The real important thing here is to return a BObject for objects 
     * that can be put into the bench.
     */
    static Object getVal ( Package bluej_pkg, String instanceName, Value val )
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
          ObjectWrapper objWrap = new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val),instanceName);
          return new BObject ( objWrap );
          }

        return val.toString();
        }
    }