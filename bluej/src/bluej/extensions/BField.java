package bluej.extensions;

import bluej.debugger.*;
import bluej.debugger.jdi.JdiObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;
import bluej.views.*;

import com.sun.jdi.*;

/**
 * A wrapper for a field of a BlueJ class.
 * Behaviour is similar to the Reflection API.
 * 
 * @version $Id: BField.java 2032 2003-06-12 05:04:28Z ajp $
 */

/*
 * The same reasoning of BConstructor apply here.
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 * Previous attempt by Clive Miller, University of Kent at Canterbury, 2002
 */
 
public class BField
{
    private FieldView bluej_view;
    private Identifier parentId;
    
    BField ( Identifier aParentId, FieldView i_bluej_view )
    {
        parentId = aParentId;
        bluej_view = i_bluej_view;
    }        

    /**
     * Check to see if the field name matches the given one.
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
     * Return the name of the field.
     * Similar to reflection API.
     */
    public String getName()
        {
        // Tested ok, 070303 Damiano
        return bluej_view.getName();
        }

    /**
     * Return the type of the field.
     * Similar to Reflection API.
     */
    public Class getType()
        {
        // Tested ok, 070303 Damiano
        return bluej_view.getType().getViewClass();
        }

    /**
     * Returns the modifiers of this field.
     * The <code>java.lang.reflect.Modifier</code> class can be used to decode the modifiers.
     * Similar to reflection API
     */
    public int getModifiers ()
        {
        return bluej_view.getModifiers();
        }
        

    /**
     * When you are inspecting a static field use this one.
     * @throws ProjectNotOpenException if the project to which this field belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this field belongs has been deleted by the user.
     */
    private Object getStaticField () throws ProjectNotOpenException, PackageNotFoundException
      {
      Package bluejPkg = parentId.getBluejPackage();
      PkgMgrFrame aFrame = parentId.getPackageFrame();
      String wantFieldName = getName();

      // I need to get the view of the parent of this Field
      // That must be the Class that I want to look after....
      View parentView = bluej_view.getDeclaringView();
      String className = parentView.getQualifiedName();

	  DebuggerClass debuggerClass;
	  try {
		debuggerClass = bluejPkg.getDebugger().getClass(className);
      }
	  catch (java.lang.ClassNotFoundException cnfe) {
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

      return doGetVal(aFrame, wantFieldName, objRef);
      }


    /**
     * Return the value of this field of the given object.
     * This is similar to Reflection API.
     *
     * In the case that the field is of primitive type (<code>int</code> etc.), 
     * the return value is of the appropriate Java wrapper type (<code>Integer</code> etc.).
     * In the case that the field contains an object then 
     * an appropriate BObject will be returned. 
     *
     * The main reason that this method is on a field (derived from a class), 
     * rather than directly on an object, is to allow for the retrieval of 
     * static field values without having to create an object of the appropriate type.
     *
     * As in the Relection API, in order to get the value of a static field pass 
     * null as the parameter to this method.
     * @throws ProjectNotOpenException if the project to which the field belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which the field belongs has been deleted by the user.
     */
    public Object getValue ( BObject onThis ) 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        // If someone gives me a null it means that he wants a static field
        if ( onThis == null ) return getStaticField();
        
        ObjectReference objRef = onThis.getObjectReference();

        ReferenceType type = objRef.referenceType();

        Field thisField = type.fieldByName (bluej_view.getName());
        if ( thisField == null ) return null;
       
        PkgMgrFrame aFrame = onThis.getPackageFrame();
        return doGetVal(aFrame, bluej_view.getName(), objRef.getValue(thisField));
        }


    /**
     * Given a Value that comes from the remote debugger machine, converts it into somethig
     * that is usable. The real important thing here is to return a BObject for objects 
     * that can be put into the bench.
     */
    static Object doGetVal ( PkgMgrFrame packageFrame, String instanceName, Value val )
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
          ObjectWrapper objWrap = new ObjectWrapper (packageFrame, packageFrame.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val),instanceName);
          return new BObject ( objWrap );
          }

        return val.toString();
        }
    }
