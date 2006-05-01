package rmiextension;

import java.util.logging.Logger;

import bluej.extensions.BObject;
import bluej.extensions.ConstructorInvoker;
import bluej.extensions.InvocationArgumentException;
import bluej.extensions.InvocationErrorException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Creates and removes objects on the object bench.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ObjectBench.java 4052 2006-05-01 11:58:26Z davmac $
 */
public class ObjectBench
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    /**
     * Creates a new object, and puts it on the objectbench
     * 
     * @param prj
     * @param className
     * @param instanceName
     */
    public static void createObject(Project prj, String className, String instanceName)
    {
        try {
            ConstructorInvoker invoker = new ConstructorInvoker(prj.getPackage(), className); 
            invoker.invokeConstructor(instanceName, new String[] {});
        }
        catch (InvocationArgumentException e) {
            e.printStackTrace();
        }
        catch (InvocationErrorException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new object, and puts it on the objectbench
     * 
     * @param prj
     * @param className
     * @param instanceName
     * @param constructorParams
     */
    public static void createObject(Project prj, String className, String instanceName, String[] constructorParams)
    {
        try {

            ConstructorInvoker launcher = new ConstructorInvoker(prj.getPackage(), className);
            logger.warning("package dir: " + prj.getPackage().getDir().toString());
            launcher.invokeConstructor(instanceName, constructorParams);

        }
        catch (InvocationArgumentException e) {
            e.printStackTrace();
        }
        catch (InvocationErrorException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Removes the object from the object bench
     * 
     * @param prj
     *            The project
     * @param string
     *            The name of the instance
     */
    public static void removeObject(Project prj, String instanceName)
    {
        try {
            BObject existing = prj.getPackage().getObject(instanceName);
            if (existing != null) {
                existing.removeFromBench();
            }
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
    }

}