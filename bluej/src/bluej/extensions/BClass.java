package bluej.extensions;

import bluej.compiler.JobQueue;
import bluej.extensions.editor.Editor;
import bluej.extensions.editor.EditorBridge;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.views.ConstructorView;
import bluej.views.FieldView;
import bluej.views.MethodView;
import bluej.views.View;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper for a BlueJ class.
 * From this you can create BlueJ objects and call their methods.
 * Behaviour is similar to the Java reflection API.
 *
 * @version    $Id: BClass.java 3012 2004-09-22 11:05:04Z iau $
 */

/*
 * @author Damiano Bolla, University of Kent at Canterbury, 2002,2003,2004
 */
public class BClass
{
    private Identifier classId;


    /**
     * Constructor for the BClass.
     * It is duty of the caller to guarantee that it is a reasonable classId
     *
     * @param  thisClassId  Description of the Parameter
     */
    BClass(Identifier thisClassId)
    {
        classId = thisClassId;
    }


    /**
     * Removes this class from BlueJ, including the underlying files.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @throws  ClassNotFoundException    if the class has been deleted by the user.
     */
    public void remove()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        Package bluejPkg = classId.getBluejPackage();
        ClassTarget bluejClass = classId.getClassTarget();

        bluejPkg.removeClass(bluejClass);
    }


    /**
     * Returns the Java class being wrapped by this BClass.
     * Use this method when you need more information about the class than
     * is provided by the BClass interface. E.g.:
     * What is the real class being hidden?
     * Is it an array?
     * What is the type of the array element?
     *
     * Note that this is for information only. If you want to interact with BlueJ you must
     * use the methods provided in BClass.
     *
     * @return                           The javaClass value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Class getJavaClass()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return classId.getJavaClass();
    }


    /**
     * Returns the package this class belongs to.
     * Similar to reflection API.
     *
     * @return                            The package value
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public BPackage getPackage() throws ProjectNotOpenException, PackageNotFoundException
    {
        Project bluejProject = classId.getBluejProject();
        Package bluejPkg = classId.getBluejPackage();

        return new BPackage(new Identifier(bluejProject, bluejPkg));
    }


    /**
     * Returns a proxy object that provide an interface to the editor for this BClass.
     * If an editor already exists, a proxy for it is returned. Otherwise, an editor is created but not made visible.
     *
     * @return                            The proxy editor object or null if it cannot be created
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public Editor getEditor() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();

        return EditorBridge.newEditor(aTarget);
    }


    /**
     * Checks to see if this class has been compiled.
     *
     * @return                            true if it is compiled false othervise.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public boolean isCompiled()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();

        return aTarget.isCompiled();
    }


    /**
     * Compile this class, and any dependents.
     * After the compilation has finished the method isCompiled() can be used to determined the class status.
     * A single CompileEvent with all dependent files listed will be generated.
     *
     * @param  waitCompileEnd                   <code>true</code> waits for the compilation to be finished.
     * @throws  ProjectNotOpenException         if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException        if the package to which this class belongs has been deleted by the user.
     * @throws  CompilationNotStartedException  if BlueJ is currently executing Java code.
     */
    public void compile(boolean waitCompileEnd)
             throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        Package bluejPkg = classId.getBluejPackage();
        ClassTarget aTarget = classId.getClassTarget();

        if (!bluejPkg.isDebuggerIdle()) {
            throw new CompilationNotStartedException("BlueJ is currently executing Java code");
        }

        // Ask for compilation of this target
        bluejPkg.compile(aTarget);

        // if requested wait for the compilation to finish.
        if (waitCompileEnd) {
            JobQueue.getJobQueue().waitForEmptyQueue();
        }

        // We do not return aTarget.isCompiled() since it is meaningless when we do not wait
        // for the compilation to be finished.
    }


    /**
     * Utility. Finds the package name given a fully qualified name
     * If no package exist then an empty string is retrned.
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
     * Returns the superclass of this class.
     * Similar to reflection API.
     * If this class represents either the Object class, an interface,
     * a primitive type, or void, then null is returned.
     * If the superclass is not part of a package in the current BlueJ project then
     * null is returned.
     *
     * @return                            The superclass value
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @throws  ClassNotFoundException    if the class has been deleted by the user.
     */
    public BClass getSuperclass()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        // Tested 22 may 2003, Damiano

        Project bluejPrj = classId.getBluejProject();

        View bluejView = classId.getBluejView();
        View superView = bluejView.getSuper();

        // If this <code>Class</code> represents either the Object class, an interface,
        // a primitive type, or void, then null is returned
        if (superView == null) {
            return null;
        }

        // The class exists, is it part of this project ?
        Class aTest = bluejPrj.loadClass(superView.getQualifiedName());
        // Really strange, a superclass  that is not part of this project classloader...
        if (aTest == null) {
            return null;
        }

        String classPkgName = findPkgName(superView.getQualifiedName());
//      System.out.println ("Parent="+classPkgName);

        // Now I need to find out to what package it belongs to...
        boolean foundPackageMatch = false;
        List pkgList = bluejPrj.getPackageNames();
        for (Iterator iter = pkgList.iterator(); iter.hasNext(); ) {
            if (!classPkgName.equals(iter.next())) {
                continue;
            }
            // Fount it, remembar that we found it and get out.
            foundPackageMatch = true;
            break;
        }

        // There is no point to return a BClass whose package does not match..
        // Things would just fall here and there...
        if (!foundPackageMatch) {
            return null;
        }

        // Let me get the package I want now...
        Package bluejPkg = bluejPrj.getCachedPackage(classPkgName);

        return new BClass(new Identifier(bluejPrj, bluejPkg, superView.getQualifiedName()));
    }


    /**
     * Returns all the constructors of this class.
     * Similar to reflection API.
     *
     * @return                           The constructors value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
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
     * @return                           the requested constructor of this class, or null if
     * the class has not been compiled or the constructor cannot be found.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public BConstructor getConstructor(Class[] signature)
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
     * @return                           The declaredMethods value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
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
     * @param  methodName                Description of the Parameter
     * @param  params                    Description of the Parameter
     * @return                           The declaredMethod value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        View bluejView = classId.getBluejView();

        MethodView[] methodView = bluejView.getDeclaredMethods();
        BMethod[] methods = new BMethod[methodView.length];

        for (int index = 0; index < methods.length; index++) {
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
     * @return                           The fields value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
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
     * @param  fieldName                 Description of the Parameter
     * @return                           The field value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
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
            if (result.matches(fieldName)) {
                return result;
            }
        }

        return null;
    }


    /**
     * Returns this class's .class file.
     *
     * @return                            the class .class file.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public File getClassFile()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();

        return aTarget.getClassFile();
    }


    /**
     * Returns this class's .java file.
     * If the file is currently being edited, calling this method will cause it to be saved.
     *
     * @return                            the class .java file.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public File getJavaFile()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        bluej.editor.Editor anEditor = aTarget.getEditor();
        if (anEditor != null) {
            anEditor.save();
        }

        return aTarget.getSourceFile();
    }


    /**
     * Signal to BlueJ that an extension is about to begin changing the source file of this class.
     * The file containing the source for this class can be found using getJavaFile();
     * If the file is currently being edited it will be saved and the editor will be set read-only.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @deprecated As of BlueJ 2.0, replaced by {@link #getEditor()}
     */
    public void beginChangeSource()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        bluej.editor.Editor anEditor = aTarget.getEditor();
        if (anEditor == null) {
            return;
        }

        anEditor.save();
        anEditor.setReadOnly(true);
    }


    /**
     * Signal to BlueJ that an extension has finished changing the source file of this class.
     * If the file is currently being edited, this will cause it to be re-loaded and the editor to be set read/write.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @deprecated As of BlueJ 2.0, replaced by {@link #getEditor()}
     */
    public void endChangeSource()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget aTarget = classId.getClassTarget();
        bluej.editor.Editor anEditor = aTarget.getEditor();
        if (anEditor == null) {
            return;
        }

        anEditor.reloadFile();
        anEditor.setReadOnly(false);
    }



    /**
     * Returns a string representation of the Object
     *
     * @return    Description of the Return Value
     */
    public String toString()
    {
        try {
            Class javaClass = classId.getJavaClass();
            return "BClass: " + javaClass.getName();
        }
        catch (ExtensionException exc) {
            return "BClass: INVALID";
        }
    }
}
