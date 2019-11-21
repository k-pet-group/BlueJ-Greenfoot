/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2013,2014,2015,2016,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extensions2;

import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.JobQueue;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.editor.EditorBridge;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.JavaNames;
import bluej.views.ConstructorView;
import bluej.views.FieldView;
import bluej.views.MethodView;
import bluej.views.View;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A wrapper for a class. This is used to represent both classes which have a representation
 * within the BlueJ project, and those that don't.
 * 
 * <p>From an instance of this class, an extension can create BlueJ objects and call their methods.
 * Behaviour is similar to the Java reflection API.
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2002,2003,2004
 */
public class BClass
{
    private static Map<Identifier, BClass> externalClasses = new WeakHashMap<Identifier, BClass>();

    private Identifier classId;

    /**
     * Constructor for the BClass.
     * It is the duty of the caller to guarantee that it is a reasonable classId
     *
     * @param thisClassId the identifier of the underlying class.
     */
    BClass(Identifier thisClassId)
    {
        classId = thisClassId;
    }
    
    /**
     * Gets a BClass for some class identifier. To be used for classes which don't have a
     * representation (ClassTarget) in BlueJ.
     *
     * @param classId the identifier of the underlying class.
     * @return A BClass object with the <code>classId</code> identifier.
     */
    synchronized static BClass getBClass(Identifier classId)
    {
        BClass r = externalClasses.get(classId);
        if (r == null) {
            r = new BClass(classId);
            externalClasses.put(classId, r);
        }
        return r;
    }
    

    /**
     * Notification that the name of the class has changed.
     * @param newName the new class name, fully qualified.
     */
    void nameChanged(String newName)
    {
        try {
            Project proj = classId.getBluejProject();
            Package pkg = classId.getBluejPackage();
            classId = new Identifier(proj, pkg, newName);
        }
        catch (ProjectNotOpenException pnoe) { }
        catch (PackageNotFoundException pnfe) { }
    }

    /**
     * Returns the name of this BClass.
     * 
     * @return The fully qualified name of the wrapped BlueJ class.
     */
    public final String getName()
    {
        return classId.getClassName();
    }

    /**
     * Removes this class from BlueJ, including the underlying files.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @throws ClassNotFoundException    if the class has been deleted by the user, or if the class does
     *                                    not otherwise have a representation within the project.
     */
    public void remove()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        ClassTarget bluejClass = classId.getClassTarget();
        if (bluejClass == null) {
            throw new ClassNotFoundException("Can't find class: " + classId.getClassName());
        }
        bluejClass.remove();
    }

    /**
     * Converts a Stride class to Java, by removing the Stride file and retaining the generated Java.
     *
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public void convertStrideToJava()
            throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        ClassTarget bluejClass = classId.getClassTarget();
        if (bluejClass == null) {
            throw new ClassNotFoundException("Can't find class: " + classId.getClassName());
        }
        bluejClass.convertStrideToJava();
    }

    /**
     * Converts a Java class to Stride.
     *
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public void convertJavaToStride()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        ClassTarget bluejClass = classId.getClassTarget();
        if (bluejClass == null) {
            throw new ClassNotFoundException("Can't find class: " + classId.getClassName());
        }

        bluejClass.promptAndConvertJavaToStride(null);
    }


    /**
     * Returns the Java Class object being wrapped by this BClass.
     * An extension uses this method when more information about the class than
     * is provided by the BClass interface is needed. E.g.:
     * 
     * <ul>
     * <li>What is the real class being hidden?
     * <li>Is it an array?
     * <li>What is the type of the array element?
     * </ul>
     *
     * <p>Note that this is for information only. To interact with BlueJ, an extension must
     * use the methods provided in BClass.
     *
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public Class<?> getJavaClass()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return classId.getJavaClass();
    }


    /**
     * Returns the BPackage this class belongs to.
     * Similar to reflection API.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public BPackage getPackage() throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = classId.getBluejPackage();
        return bluejPkg.getBPackage();
    }


    /**
     * Returns a proxy object that provide an interface to the <b>Java</b> editor for this BClass.
     * If an editor already exists, a proxy for it is returned. Otherwise, an editor is created but not made visible.
     * If the class is not a Java class (Stride) the method returns <code>null</code>.
     *
     * @return                            A proxy {@link JavaEditor} object or null if it cannot be created or if the class is not of type Java
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public JavaEditor getJavaEditor() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        if (aTarget == null || aTarget.getSourceType() != SourceType.Java)
        {
            return null;
        }
        
        return EditorBridge.newJavaEditor(aTarget);
    }
    
    /**
     * Finds out whether this class has source code available, and whether it's Java or Stride
     *
     * @return Either {@link SourceType#Java} or {@link SourceType#Stride} if the type can be found out, otherwise <code>null</code>.
     */
    public SourceType getSourceType() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        if (aTarget != null)
            return aTarget.getSourceType();
        else
            return null;
    }


    /**
     * Checks to see if this class has been compiled.
     *
     * @return                            True if it is compiled, false otherwise.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public boolean isCompiled()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        return aTarget == null || aTarget.isCompiled();
    }


    /**
     * Compiles this class, and any dependents.
     * 
     * <p>After the compilation has finished the method isCompiled() can be used to determined the class
     * status.
     * 
     * <p>A single CompileEvent will be generated with all dependent files listed.
     * 
     * <p>A call to this method is equivalent to: <code>compile(waitCompileEnd, false)</code>.
     *
     * @param  waitCompileEnd                   <code>true</code> waits for the compilation to be finished.
     * @throws  ProjectNotOpenException         if the project to which this class belongs has been closed.
     * @throws  PackageNotFoundException        if the package to which this class belongs has been deleted.
     * @throws  CompilationNotStartedException  if BlueJ is currently executing Java code.
     */
    public void compile(boolean waitCompileEnd)
             throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        compile(waitCompileEnd, false);
    }

    /**
     * Compiles this class, and any dependents, optionally without showing compilation errors to the user.
     * 
     * <p>After the compilation has finished the method isCompiled() can be used to determined the class
     * status.
     * 
     * <p>A single CompileEvent with all dependent files listed will be generated.
     *
     * @param  waitCompileEnd                   <code>true</code> waits for the compilation to be finished.
     * @param  forceQuiet                       if true, compilation errors will not be shown/highlighted to the user.
     * @throws  ProjectNotOpenException         if the project to which this class belongs has been closed.
     * @throws  PackageNotFoundException        if the package to which this class belongs has been deleted.
     * @throws  CompilationNotStartedException  if BlueJ is currently executing Java code.
     */
    public void compile(boolean waitCompileEnd, boolean forceQuiet)
             throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        Package bluejPkg = classId.getBluejPackage();
        ClassTarget aTarget = classId.getClassTarget();
        if (aTarget == null) {
            throw new CompilationNotStartedException("Class target does not (any longer) exist");
        }

        if (!bluejPkg.isDebuggerIdle()) {
            throw new CompilationNotStartedException("BlueJ is currently executing Java code");
        }

        // Ask for compilation of this target
        bluejPkg.compile(aTarget, forceQuiet, null, CompileReason.EXTENSION, CompileType.EXTENSION);

        // if requested wait for the compilation to finish.
        if (waitCompileEnd) {
            JobQueue.getJobQueue().waitForEmptyQueue();
        }

        // We do not return aTarget.isCompiled() since it is meaningless when we do not wait
        // for the compilation to be finished.
    }


    /**
     * Utility. Finds the package name given a fully qualified name
     * If no package exist then an empty string is returned.
     *
     * @param  fullyQualifiedName  Description of the Parameter
     * @return                     Description of the Return Value
     */
    private String findPkgName(String fullyQualifiedName)
    {
        if (fullyQualifiedName == null) {
            return "";
        }

        int dotIndex = fullyQualifiedName.lastIndexOf(".");
        // If there is no package name to be found return an empty one.
        if (dotIndex < 0) {
            return "";
        }

        return fullyQualifiedName.substring(0, dotIndex);
    }


    /**
     * Returns the superclass of this class. Similar to reflection API.
     *
     * @return The superclass wrapped in a BClass object. If this class represents either the Object class or an interface then <code>null</code> is returned.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @throws ClassNotFoundException    if the class has been deleted by the user.
     */
    public BClass getSuperclass()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        Project bluejPrj = classId.getBluejProject();
        
        ClassTarget ct = classId.getClassTarget();
        if (ct != null && ! ct.isCompiled()) {
            // Class is not compiled: we can still know the superclass!
            ClassInfo info = ct.getSourceInfo().getInfo(getJavaFile(), ct.getPackage());
            if (info != null) {
                String superClass = info.getSuperclass();
                superClass = (superClass == null) ? "" : superClass;
                String pkgString = JavaNames.getPrefix(superClass);
                Package bjPkg = bluejPrj.getPackage(pkgString);
                if (bjPkg != null) {
                    Target sct = bjPkg.getTarget(JavaNames.getBase(superClass));
                    if (sct instanceof ClassTarget) {
                        return ((ClassTarget) sct).getBClass();
                    }
                }
                
                // Superclass isn't in the project?
                Identifier sid = new Identifier(bluejPrj, null, superClass);
                return BClass.getBClass(sid);
            }
            
            return null; 
        }

        View bluejView = classId.getBluejView();
        View superView = bluejView.getSuper();

        // If this <code>Class</code> represents either the Object class, an interface,
        // a primitive type, or void, then null is returned
        if (superView == null) {
            return null;
        }

        // The class exists, is it part of this project ?
        Class<?> aTest = bluejPrj.loadClass(superView.getQualifiedName());
        // Really strange, a superclass  that is not part of this project classloader...
        if (aTest == null) {
            return null;
        }

        String classPkgName = findPkgName(superView.getQualifiedName());

        // Now I need to find out to what package it belongs to...
        Package bluejPkg = bluejPrj.getPackage(classPkgName);
        if (bluejPkg != null) {
            // I need the Target for the class I want.
            Target aTarget = bluejPkg.getTarget (superView.getBaseName());

            if (aTarget instanceof ClassTarget) {
                ClassTarget classTarget = (ClassTarget) aTarget;
                return classTarget.getBClass();
            }
        }

        Identifier id = new Identifier(bluejPrj, null, superView.getQualifiedName());
        return BClass.getBClass(id);
    }


    /**
     * Returns all the constructors of this class.
     * Similar to reflection API.
     *
     * @return                           An array of {@link BConstructor} objects wrapping the constructors of this class.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BConstructor[] getConstructors()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();

        ConstructorView[] constructorViews = bluejView.getConstructors();
        BConstructor[] result = new BConstructor[constructorViews.length];
        for (int index = 0; index < constructorViews.length; index++) {
            result[index] = new BConstructor(classId, constructorViews[index]);
        }

        return result;
    }


    /**
     * Returns the constructor for this class which has the given signature.
     * Similar to reflection API.
     *
     * @param  signature                 the signature of the required constructor.
     * @return                           The {@link BConstructor} object wrapping the matching constructor of this class, or null if
     * the class has not been compiled or the constructor cannot be found.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BConstructor getConstructor(Class<?>[] signature)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();

        ConstructorView[] constructorViews = bluejView.getConstructors();
        for (int index = 0; index < constructorViews.length; index++) {
            BConstructor aConstr = new BConstructor(classId, constructorViews[index]);
            if (aConstr.matches(signature)) {
                return aConstr;
            }
        }
        return null;
    }


    /**
     * Returns the declared methods of this class.
     * Similar to reflection API.
     *
     * @return                           An array of {@link BMethod} objects wrapping the methods of this class.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BMethod[] getDeclaredMethods()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();

        MethodView[] methodView = bluejView.getDeclaredMethods();
        BMethod[] methods = new BMethod[methodView.length];

        for (int index = 0; index < methods.length; index++) {
            methods[index] = new BMethod(classId, methodView[index]);
        }

        return methods;
    }


    /**
     * Returns the declared method of this class which has the given signature.
     * Similar to reflection API.
     *
     * @param  methodName                The name of the method.
     * @param  params                    The parameters of the method. Pass a zero length array if the method takes no arguments. 
     * @return                           The {@link BMethod} object wrapping the matching method or <code>null</code> if the method is not found.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BMethod getDeclaredMethod(String methodName, Class<?>[] params)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();
        MethodView[] methodView = bluejView.getDeclaredMethods();

        for (int index = 0; index < methodView.length; index++) {
            BMethod aResul = new BMethod(classId, methodView[index]);
            if (aResul.matches(methodName, params)) {
                return aResul;
            }
        }

        return null;
    }

    
    /**
     * Returns all methods of this class, those declared and those inherited from all ancestors. 
     * Similar to reflection API, except that all methods, declared and inherited, are returned, and not only the public ones.
     * That is, it returns all public, private, protected, and package-access methods, inherited or declared.
     * The elements in the array returned are not sorted and are not in any particular order.
     *
     * @return                           An array of {@link BMethod} objects wrapping the methods of this class.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BMethod[] getMethods()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();

        MethodView[] methodView = bluejView.getAllMethods();
        BMethod[] methods = new BMethod[methodView.length];

        for (int index = 0; index < methods.length; index++) {
            methods[index] = new BMethod(classId, methodView[index]);
        }

        return methods;
    }

    
    /**
     * Returns the method of this class with the given signature.
     * Similar to reflection API, except that all methods, declared and inherited, are searched, and not only the public ones.
     * That is, it searches all public, private, protected, and package-access methods, declared or inherited from all ancestors.
     * If the searched method has been redefined, the returned method is chosen arbitrarily from the list of inherited and declared methods.
     *
     * @param  methodName                The name of the method
     * @param  params                    The parameters of the method. Pass a zero length array if the method takes no arguments. 
     * @return                           The {@link BMethod} object wrapping the matching method or <code>null</code> if the method is not found.
     * @throws ProjectNotOpenException  if the project to which this class belongs has been closed by the user
     * @throws ClassNotFoundException   if the class has been deleted by the user
     */
    public BMethod getMethod(String methodName, Class<?>[] params)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();
        MethodView[] methodView = bluejView.getAllMethods();
 
        for (int index = 0; index < methodView.length; index++) {
            BMethod aResul = new BMethod(classId, methodView[index]);
            if (aResul.matches(methodName, params)) {
                return aResul;
            }
        }

        return null;
    }


    /**
     * Returns all the fields of this class.
     * Similar to reflection API.
     *
     * @return                           An array of {@link BField} objects wrapping this class's fields.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BField[] getFields()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();

        FieldView[] fieldView = bluejView.getAllFields();
        BField[] bFields = new BField[fieldView.length];
        for (int index = 0; index < fieldView.length; index++) {
            bFields[index] = new BField(classId, fieldView[index]);
        }

        return bFields;
    }


    /**
     * Returns the field of this class which has the given name.
     * Similar to Reflection API.
     *
     * @param  fieldName                 name of the field
     * @return                           The {@link BField} object wrapping the matching field, or <code>null</code> if no field is found.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws ClassNotFoundException   if the class has been deleted by the user.
     */
    public BField getField(String fieldName)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        if (fieldName == null) {
            return null;
        }

        View bluejView = classId.getBluejView();

        FieldView[] fieldView = bluejView.getAllFields();
        for (int index = 0; index < fieldView.length; index++) {
            BField result = new BField(classId, fieldView[index]);
            if (result.getName().equals(fieldName)) {
                return result;
            }
        }

        return null;
    }

    /**
     * Returns this class's .class file.
     *
     * @return                            A {@link java.io.File} object for the class's .class file, <code>null</code> if the class no longer exists in the project.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public File getClassFile()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        if (aTarget == null) {
            return null;
        }

        return aTarget.getClassFile();
    }


    /**
     * Returns this class's .java file.
     * If the file is currently being edited, calling this method will cause it to be saved.
     *
     * @return                            A {@link java.io.File} object for the class's .java file.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public File getJavaFile()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        if (aTarget == null) {
            return null;
        }
        if(aTarget.editorOpen()) {
            bluej.editor.Editor anEditor = aTarget.getEditor();
            if (anEditor != null) {
                try {
                    anEditor.save();
                }
                catch (IOException ioe) {}
            }
        }

        return aTarget.getJavaSourceFile();
    }

    /**
     * Returns a string representation of the Object
     */
    public String toString()
    {
        try {
            Class<?> javaClass = classId.getJavaClass();
            return "BClass: " + javaClass.getName();
        }
        catch (ExtensionException exc) {
            return "BClass: INVALID";
        }
    }
}
