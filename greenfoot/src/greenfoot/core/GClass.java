package greenfoot.core;

import greenfoot.event.CompileListener;

import java.io.IOException;
import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RConstructor;
import rmiextension.wrappers.RField;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.extensions.*;
import bluej.extensions.ClassNotFoundException;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;


/**
 * Represents a class in Greenfoot. This class wraps the RMI class and contains
 * some extra functionality. The main reason for createing this class is to have
 * a place to store information about inheritance relations between classes that
 * have not been compiled.
 * 
 * @author Poul Henriksen
 * 
 */
public class GClass implements CompileListener
{
    private RClass rmiClass;
    private GPackage pkg;
    private String superclassGuess;

    public GClass(RClass cls, GPackage pkg)
    {
        this.rmiClass = cls;
        this.pkg = pkg;
        Greenfoot.getInstance().addCompileListener(this);
        guessSuperclass();
    }

    /**
     * Get the value of a persistent property for this class
     * 
     * @param propertyName   The property name
     * @return   The property value (a String)
     */
    public String getClassProperty(String propertyName)
    {
        try {
            return pkg.getProperty("class." + getName() + "." + propertyName);
        }
        catch (RemoteException re) {
            // TODO handle/report error
            return null;
        }
        catch (PackageNotFoundException pnfe) {
            // TODO handle/report error
            return null;
        }
    }
    
    /**
     * Set the value of a persistent property for this class
     * 
     * @param propertyName  The property name to set
     * @param value         The value to set the property to
     */
    public void setClassProperty(String propertyName, String value)
    {
        try {
            pkg.setProperty("class." + getName() + "." + propertyName, value);
        }
        catch (RemoteException re) {
            // TODO handle/report error
        }
        catch (PackageNotFoundException pnfe) {
            // TODO handle/report error
        }
        catch (IOException ioe) {
            // TODO handle/report error
        }
    }
    
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException
    {
        rmiClass.compile(waitCompileEnd);
    }

    public void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        rmiClass.edit();
    }


    public void remove() throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        rmiClass.remove();
    }
    
    public RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getConstructor(signature);
    }

    public RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getConstructors();
    }

    public BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getDeclaredMethod(methodName, params);
    }

    public BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getDeclaredMethods();
    }

    public RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getField(fieldName);
    }

    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getFields();
    }

    public Class getJavaClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getJavaClass();
    }

    public GPackage getPackage()
    {
        return pkg;
    }

    /**
     * Gets the qulified name of this class.
     * @return
     */
    public String getQualifiedName()
    {
        try {
            return rmiClass.getQualifiedName();
        }
        catch (RemoteException e) {
            // TODO error reporting
        }
        catch (ProjectNotOpenException e) {}
        catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Gets the name of this class. NOT the qualified name.
     * @return
     */
    public String getName() {
        String name = getQualifiedName();
        int index = name.lastIndexOf('.');
        if (index >= 0) {
            name = name.substring(index + 1);
        }
        return name;
    }
    /**
     * Returns the superclass or null if no superclass can be found.
     * 
     * @return superclass, or null if the superclass is not part of this
     *         project.
     */
    public GClass getSuperclass()
    {
        try {
            GProject proj = pkg.getProject();
            String superclassName = getSuperclassGuess();
            if (superclassName == null) {
                return null;
            }
            
            // The superclass could belong to a different package...
            String superclassPkg;
            int lastDot = superclassName.lastIndexOf('.');
            if (lastDot == -1) {
                superclassPkg = "";
            }
            else {
                superclassPkg = superclassName.substring(0, lastDot);
                superclassName = superclassName.substring(lastDot + 1);
            }
            
            // Get the package, return the class
            GPackage thePkg = proj.getPackage(superclassPkg);
            if (thePkg == null) {
                return null;
            }
            return thePkg.getClass(superclassName);
        }
        catch (RemoteException re) {
            re.printStackTrace();
            return null;
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
            return null;
        }
    }
    
    /**
     * This method tries to guess which class is the superclass. This can be used for non compilable and non parseable classes.
     * <p>
     * If the class is compiled, it will return the real superclass.
     * <br>
     * If the class is parseable this information will be used to extract the superclass.
     * <br>
     * If the is not parseable it will use the last superclass that was known.
     * <br>
     * In general, we will try to remember the last known superclass, and report that back.
     * 
     * @return Best guess of the fully qualified name of the superclass.
     */
    public String getSuperclassGuess() {
        return superclassGuess;
    }
    
    /**
     * Sets the superclass guess that will be returned if it is not possible to
     * find it in another way.
     * 
     * This name will be stripped of any qualifications
     * 
     * @param superclass
     */
    public void setSuperclassGuess(String superclassName)
    {
        superclassGuess = superclassName;
    }
    
    /**
     * This method tries to guess which class is the superclass. This can be used for non compilable and non parseable classes.
     * <p>
     * If the class is compiled, it will return the real superclass.
     * <br>
     * If the class is parseable this information will be used to extract the superclass.
     * <br>
     * If the is not parseable it will use the last superclass that was known.
     * <br>
     * In general, we will try to remember the last known superclass, and report that back.
     * <p>
     * 
     * OBS: This method can be very slow and shouldn't be called unless needed. Especially if the class isn't compiled it can be very slow.
     * 
     * @return Best guess of the name of the superclass (NOT the qualified name).
     */
    private void guessSuperclass()
    {
        // This should be called each time the source file is saved. However,
        // this is not possible at the moment, so we just do it when it is
        // compiled.
        
        String name = this.getName();
        if(name.equals("World") || name.equals("Actor")) {
            //We do not want to waste time on guessing the name of the superclass for these to classes.
            return;
        }
        
        //First, try to get the real super class.
        String realSuperclass = null;
        try {
            if(isCompiled()) {                
                realSuperclass = rmiClass.getSuperclass().getQualifiedName();
            }
        }
        catch (RemoteException e) {
        }
        catch (ProjectNotOpenException e) {
        }
        catch (PackageNotFoundException e) {
        }
        catch (ClassNotFoundException e) {
        }
        catch (NullPointerException e) {
        }
        if(realSuperclass != null) {
            superclassGuess = realSuperclass;
            return;
        }
        
        
        //Second, try to parse the file
        String parsedSuperclass = null;
        try {
            //GClass[] gClasses = pkg.getClasses();

           /* Vector classes = new Vector();
            for (int i = 0; i < gClasses.length; i++) {
                GClass cls = gClasses[i];
                classes.add(cls.getQualifiedName());
            }*/
            ClassInfo info = ClassParser.parse(rmiClass.getJavaFile());//, classes);
            parsedSuperclass = info.getSuperclass();
            // TODO hack! If the superclass is Actor or World,
            // put it in the right package... parsing does not resolve references...
            if (parsedSuperclass.equals("Actor")) {
                parsedSuperclass = "greenfoot.Actor";
            }
            if (parsedSuperclass.equals("World")) {
                parsedSuperclass = "greenfoot.World";
            }
        }
        catch (ProjectNotOpenException e) {}
        catch (PackageNotFoundException e) {}
        catch (RemoteException e) {}
        catch (Exception e) {}
        
        if(parsedSuperclass != null) {
            superclassGuess = parsedSuperclass;
            return;
        }
        
        //Ok, nothing more to do. We just let the superclassGuess be whatever it is.   
        
    }
    
    /**
     * Strips the name of a class for its qualified part.
     */
    private String removeQualification(String classname)
    {
        int lastDotIndex = classname.lastIndexOf(".");
        if(lastDotIndex != -1) {
            return classname.substring(lastDotIndex+1);
        } else {
            return classname;
        }
    }

    public String getToString()
    {
        try {
            return rmiClass.getToString();
        }
        catch (RemoteException e) {
            // TODO error reporting
        }
        catch (ProjectNotOpenException e) {}
        catch (ClassNotFoundException e) {}
        return "Error getting real toString. super: " + super.toString();
    }

    public boolean isCompiled()
    {
        try {
            return rmiClass.isCompiled();
        }
        catch (ProjectNotOpenException e) {
            // TODO error reporting
        }
        catch (PackageNotFoundException e) {}
        catch (RemoteException e) {}
        return false;
    }

    /**
     * Returns true if this class is a subclass of the given class.
     * 
     * A class is not considered a subclass of itself. So, if the two classes
     * are same it returns false.
     * 
     * It only looks at the name of class and not the fully qualified name.
     * 
     * @param className
     * @return
     */
    public boolean isSubclassOf(String className)
    {        
        className = removeQualification(className);
       // guessSuperclass();
        GClass superclass = this;
        if(this.getName().equals(className)) {
            return false;
        }
        //Recurse through superclasses
        while (superclass != null) {
            String superclassName = superclass.getSuperclassGuess();
            //TODO Fix this hack. Should be done when non-greenfoot classes gets support.
            //HACK to ensure that a class with no superclass has "" as superclass. This is becuase of the ClassForest building which then allows the clas to show up even though it doesn't have any superclass.
            if(superclassName == null) {
                superclassName = "";
            }
            if (superclassName != null && (className.equals(removeQualification(superclassName)))) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }
        return false;
    }   

    public void compileError(RCompileEvent event)
    {
        guessSuperclass();
    }

    public void compileWarning(RCompileEvent event)
    {
        guessSuperclass();
    }

    public void compileSucceeded(RCompileEvent event)
    {
        guessSuperclass();
    }

    public void compileFailed(RCompileEvent event)
    {
        guessSuperclass();
    }

    public void compileStarted(RCompileEvent event)
    {   
    }

}
