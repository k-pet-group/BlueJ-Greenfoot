package rmiextension.wrappers;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import rmiextension.ProjectManager;
import bluej.extensions.BField;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RFieldImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public class RFieldImpl extends java.rmi.server.UnicastRemoteObject
    implements RField
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private BField bField;

    /**
     * @param class1
     */
    public RFieldImpl(BField bField)
        throws RemoteException
    {
        this.bField = bField;
        if (bField == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    /**
     * @throws RemoteException
     */
    protected RFieldImpl()
        throws RemoteException
    {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @return
     */
    public int getModifiers()
    {
        return bField.getModifiers();
    }

    /**
     * @return
     */
    public String getName()
    {
        return bField.getName();
    }

    /**
     * @return
     */
    public Class getType()
    {
        return bField.getType();
    }

    /**
     * @param onThis
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RObject getValue(RObject onThis)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        //We need to get the BObject that is represented by RObject onThis.
        //This is a bit tricky...
        String instanceName = onThis.getInstanceName();
        try {
            RPackage rPkg = onThis.getPackage();
            String pkgName = rPkg.getName();
            String prjName = rPkg.getProject().getName();

            BPackage bPkg = ProjectManager.instance().getPackage(prjName, pkgName);

            BObject bObj = bPkg.getObject(instanceName);
            Object fieldValue = bField.getValue(bObj);

            if (fieldValue instanceof BObject) {

                BObject bFieldValue = (BObject) fieldValue;

                String newInstanceName = "noName";
                try {
                    newInstanceName = bFieldValue.getBClass().getQualifiedName();
                    newInstanceName = newInstanceName.substring(0, 1).toLowerCase() + newInstanceName.substring(1);
                }
                catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                //must add to object bench in order to get the menu later
                bFieldValue.addToBench(newInstanceName);

                RObject wrapper = WrapperPool.instance().getWrapper(bFieldValue);
                return wrapper;
            }
            else {
                logger.info("It is something else: " + fieldValue);
                logger.info(" We can't use that for anything... return null");
                return null;

            }
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Object obj =bField.getValue(onThis.getBObject());
        // onThis.getField(RField)
        return null;
    }

    /**
     * @param fieldName
     * @return
     */
    public boolean matches(String fieldName)
    {
        return bField.matches(fieldName);
    }
}