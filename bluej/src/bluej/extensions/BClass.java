package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.ClassTarget;
import bluej.utility.Debug;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Modifier;
import java.awt.Point;

/**
 * The BlueJ proxy Class object. This represents a Class within a package, and functions relating
 * to that class. It can be got from {@link bluej.extensions.BPackage#getClass(java.lang.String) getClass}.
 * It can also represent System classes.
 * <p>
 * A 'virtual' class is one that is not represented by an actual class in the class browser. This
 * could be a result of creating a system class or getting the superclass of an actual one.
 *
 * @author Clive Miller
 * @version $Id: BClass.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class BClass
{
    private static Map primitiveArrayNameMap;
    static {
        primitiveArrayNameMap = new HashMap();
        primitiveArrayNameMap.put ("boolean", "Z");
        primitiveArrayNameMap.put ("byte", "B");
        primitiveArrayNameMap.put ("short", "S");
        primitiveArrayNameMap.put ("char", "C");
        primitiveArrayNameMap.put ("int", "I");
        primitiveArrayNameMap.put ("long", "J");
        primitiveArrayNameMap.put ("float", "F");
        primitiveArrayNameMap.put ("double", "D");
    }

    private static String transJavaToClass (final String javaStyle)
    {
        String className = javaStyle;
        int array = 0;
        while (className.endsWith ("[]")) {
            array++;
            className = className.substring (0, className.length()-2);
        }
        if (array > 0) {
            String replace = (String)BClass.primitiveArrayNameMap.get(className);
            if (replace != null) {
                className = replace;
            } else {
                className = "L"+className+";";
            }
            while (array-- > 0) className = "["+className;
        }
        return className;
    }
    
    private BPackage pkg;
    private final ClassTarget classTarget;
    private final Class loadedClass;
    private final View view;

    BClass (BPackage pkg, ClassTarget classTarget)
    {
        this.pkg = pkg;
        this.classTarget = classTarget;
        this.loadedClass = pkg.getRealPackage().loadClass (classTarget.getQualifiedName());
        this.view = View.getView (loadedClass);
    }
    
    BClass (BPackage pkg, Class systemClass)
    {
        this.pkg = pkg;
        this.classTarget = null;
        this.loadedClass = systemClass;
        this.view = View.getView (loadedClass);
    }

    BClass (BPackage pkg, View view)
    {
        this.pkg = pkg;
        this.classTarget = null;
        this.loadedClass = pkg.getRealPackage().loadClass (view.getQualifiedName());
        this.view = view;
    }

    /**
     * @param className the Java style className, eg int[][]
     */    
    BClass (BPackage pkg, String className)
    {
        this.pkg = pkg;
        this.classTarget = null;
        this.loadedClass = pkg.getRealPackage().loadClass (transJavaToClass (className));
        this.view = View.getView (loadedClass);
    }
    
    /**
     * Gets the owning Package of this class
     * @return the originator
     */
    public BPackage getPackage()
    {
        return pkg;
    }
    
    /**
     * Loads a class of the given name.
     * @param name the simple name of the required class. For example, <CODE>Person</CODE>.
     * @return Class object of the requested Class, or <CODE>null</CODE> if no such class exists.
     */
    public Class loadClass()
    {
        return loadedClass;
    }
    
    /**
     * Gets the name of this class
     * @return the simple name of the class, ie no package name
     */
    public String getName()
    {
        String name = getFullName();
        int i=name.lastIndexOf ('.');
        return  name.substring (i+1);
    }
    
    /**
     * Gets the fully-qualified name of this class
     * @return the full name of the class, ie with package name
     */
    public String getFullName()
    {
        Class cl = loadedClass;
        String postfix = "";
        while (cl.isArray()) {
            postfix += "[]";
            cl = cl.getComponentType();
        }
        return cl.getName()+postfix;
    }
    
    /**
     * Checks to see if a class has been compiled. Ignored and returns <code>true</code> if it
     * is a virtual class.
     * @return <code>true</code> if the class has been compiled and has not been modified since.
     */
    public boolean isCompiled()
    {
        if (classTarget == null) return true;
        return classTarget.isCompiled();
    }
    
    /**
     * Compile this class, and any dependants. Performed synchronously.  Ignored and returns 
     * <code>true</code> if it is a virtual class.
     * @return <code>true</code> if the compilation was successful. 
     */
    public boolean compile()
    {
        if (classTarget == null) return true;
        if (isCompiled()) return true;
        classTarget.compile (null);
        JobQueue.getJobQueue().waitForEmptyQueue();
        return isCompiled();
    }

    /**
     * Gets the superclass to this one
     * @return the immediate superclass to this one, or <code>null</code> if it has none
     */
    public BClass getSuper()
    {
        View sup = view.getSuper();
        return sup == null ? null
                           : new BClass (pkg, sup);
    }
    
    /** Gets the array type of this array
     * @return the type of each element of this array
     * @throws IllegalArgumentException if this is not an array
     */
    public BClass getArrayType()
    {
        if (!loadedClass.isArray()) throw new IllegalArgumentException ("Not an array");
        return new BClass (pkg, loadedClass.getComponentType());
    }
    
    /**
     * Gets the constructors available for this class
     * @return the constructors of this class, or an empty array if none exist, or <code>null</code> if
     * the class has not been compiled.
     */
    public BMethod[] getConstructors()
    {
        if (!isCompiled()) return null;
        ConstructorView[] constructorViews = view.getConstructors();
        BMethod[] methods = new BMethod [constructorViews.length];
        for (int i=0; i<constructorViews.length; i++) {
            methods[i] = new BMethod (pkg, constructorViews[i], null);
        }
        return methods;
    }
    
    /**
     * Gets a constructor for this class complying with the given criteria
     * @param signature the signature of the required constructor
     * @return the requested constructor of this class, or <code>null</code> if
     * the class has not been compiled or the constructor cannot be found.
     */
    public BMethod getConstructor (Class[] signature)
    {
        if (!isCompiled()) return null;
        ConstructorView[] constructorViews = view.getConstructors();
        for (int i=0; i<constructorViews.length; i++) {
            BMethod method = new BMethod (pkg, constructorViews[i], null);
            if (BMethod.matches (method, null, signature)) return method;
        }
        return null;
    }
    
    /**
     * Gets the static methods available for this class
     * @return the methods of this class, or an empty array if none exist, or <code>null</code> if
     * the class has not been compiled.
     */
    public BMethod[] getStaticMethods()
    {
        if (!isCompiled()) return null;
        MethodView[] methodViews = view.getDeclaredMethods();
        BMethod[] methods = new BMethod [methodViews.length];
        for (int i=0; i<methods.length; i++) {
            methods[i] = new BMethod (pkg, methodViews[i], null);
        }
        return methods;
    }

    /**
     * Gets a static method that matches the given criteria
     * @param name the name of the static method
     * @param signature the signature of the method wanted
     * @return the methods of this class, or an empty array if none exist, or <code>null</code> if
     * the class has not been compiled.
     */
    public BMethod getStaticMethod (String name, Class[] signature)
    {
        if (!isCompiled()) return null;
        MethodView[] methodViews = view.getAllMethods();
        for (int i=0; i<methodViews.length; i++) {
            BMethod method = new BMethod (pkg, methodViews[i], null);
            if (BMethod.matches (method, name, signature)) return method;
        }
        return null;
    }

    /**
     * Gets the static fields of this class.
     * <p>This function is not yet implemented.
     * @return the static fields belonging to this class, or an empty array if none exist, or <code>null</code> if
     * the class has not been compiled.
     * <p>Always returns <code>null</code>
     */
    public BField[] getStaticFields()
    {
        if (!isCompiled()) return null;
        return null;
/*        
        
        ((JdiClassLoader)pkg.getRemoteClassLoader()).getLoader()
        .getId()

        ReferenceType classMirror = findClassByName(getVM(), className, null);

        //Debug.message("[getStaticValue] " + className + ", " + fieldName);

        if(classMirror == null) {
            Debug.reportError("Cannot find class for result value");
            object = null;
        }
        else {
            Field resultField = classMirror.fieldByName(fieldName);
            ObjectReference obj = (ObjectReference)classMirror.getValue(resultField);
            object = JdiObject.getDebuggerObject(obj);
        }

        return object;
        
        
        
        Class cl = loadClass();
        View view = View.getView(cl);
        FieldView[] fieldViews = view.getAllFields();

        DebuggerObject obj = wrapper.getObject();
        ObjectReference ref = obj.getObjectReference();
        ReferenceType type = ref.referenceType();
        List fields = type.fields();
        BField[] bFields = new BField [fields.size());
        for (ListIterator li=fields.listIterator(); li.hasNext();) {
            int i=li.nextIndex();
            Field field = (Field)li.next();
            bFields[i] = new BField (pkg, field);
        }
        return bFields;
*/
    }

    /**
     * Gets a static field in this class of the given name.
     * <p>This function is not yet implemented.
     * @param name the name of the field to get
     * @return the static field belonging to this class of the given name, or <code>null</code> if
     * the class has not been compiled or the field does not exist.
     * <p>Always returns <code>null</code>
     */
    public BField getStaticField (String name)
    {
        if (!isCompiled()) return null;
        return null;
    }

    /**
     * Returns an array containing <code>Method</code> objects reflecting all
     * the public <em>member</em> methods of the class or interface represented
     * by this <code>Class</code> object, including those declared by the class
     * or interface and and those inherited from superclasses and
     * superinterfaces.
     */
    public int getModifiers()
    {
        return loadedClass.getModifiers();
    }

    /**
     * Determines the position of this class if it is real!
     * @return the location on the screen of the centre of this class,
     * or <code>null</code> if it's a virtual object
    public Point getLocationOnScreen()
    {
        if (classTarget == null) return null;
        Rectangle rec = classTarget.getRectangle;
        int y = classTarget.y + classTarget.height/2;
        return new Point (x,y);
    }
     */
    
    /**
     * Gets a description of this object
     * @return the classname of this class
     */
    public String toString()
    {
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getFullName();
    }
}   