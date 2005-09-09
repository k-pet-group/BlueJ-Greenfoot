package greenfoot.core;

import greenfoot.event.CompileListener;

import java.rmi.RemoteException;
import java.util.Vector;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RConstructor;
import rmiextension.wrappers.RField;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;


/**
 * Represents a class in Greenfoot. This class wraps the RMI class and contains
 * some extra functionality. The main reason for createing this class is to have
 * a place to store information about inheritance realtions between classes that
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
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return pkg;
    }

    public String getQualifiedName()
        throws RemoteException
    {
        return rmiClass.getQualifiedName();
    }

    /**
     * Returns the superclass or null if no superclass can be found.
     * @return superclass, or null if the superclass is not part of this project.
     */
    public GClass getSuperclass()
    {
        return pkg.getClass(getSuperclassGuess());
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
     * Sets the superclass guess that will be returned if it is not possible to find it in another way.
     * @param superclass
     */
    public void setSuperclassGuess(String superclassName) {
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
     * 
     * @return Best guess of the fully qualified name of the superclass.
     */
    private void guessSuperclass()
    {
        // This should be called each time the source file is saved. However,
        // this is not possible at the moment, so we just do it when it is
        // compiled.
        
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
        }
        catch (ProjectNotOpenException e) {}
        catch (PackageNotFoundException e) {}
        catch (RemoteException e) {}
        catch (Exception e) {}
        
        if(parsedSuperclass != null) {
            superclassGuess = parsedSuperclass;
            try {
                System.out.println("Found parsed superclass. "  + getQualifiedName() +" extends " + superclassGuess);
            }
            catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return;
        }
        
        //Ok, nothing more to do. We just let the superclassGuess be whatever it is.   
        if(superclassGuess != null) {
            try {
                System.out.println("Keeping superclass "  + getQualifiedName() +" extends " + superclassGuess);
            }
            catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public String getToString()
        throws RemoteException
    {
        return rmiClass.getToString();
    }

    public boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return rmiClass.isCompiled();
    }

    public boolean isSubclassOf(String className)
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        guessSuperclass();
        GClass superclass = this;

        //Recurse through superclasses
        while (superclass != null) {
            String superclassName = superclass.getSuperclassGuess();
            //TODO Fix this hack. Should be done when non-greenfoot classes gets support.
            //HACK to ensure that a class with no superclass has "" as superclass. This is becuase of the ClassForest building which then allows the clas to show up even though it doesn't have any superclass.
            if(superclassName == null) {
                superclassName = "";
            }
            //TODO bug: also matches partly. ex Beeper and SubBeeper
            if (superclassName != null && className.endsWith(superclassName)) {
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
