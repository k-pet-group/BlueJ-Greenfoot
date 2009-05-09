package greenfoot.core;

import greenfoot.Actor;
import greenfoot.World;

import java.rmi.RemoteException;

import rmiextension.wrappers.RConstructor;
import rmiextension.wrappers.RField;
import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Specialisation of a GClass that represent one of the two core Greenfoot
 * classes: Actor or World. Overrides most of the public methods of GClass to do
 * nothing and return null, since they are not really valid for these two
 * classes.
 * 
 * @author Poul Henriksen
 */
public class GCoreClass extends GClass
{

	private Class<?> cls;
	private GPackage pkg;

	public GCoreClass(Class<?> cls, GProject project)
	{
		super();
		this.cls = cls;
		this.pkg = new GPackage(project);
	}

	@Override
	public void compile(boolean waitCompileEnd) throws ProjectNotOpenException,
			PackageNotFoundException, RemoteException,
			CompilationNotStartedException
	{
		return;
	}

	@Override
	public void edit() throws ProjectNotOpenException,
			PackageNotFoundException, RemoteException
	{
		return;
	}

	@Override
	public String getClassProperty(String propertyName)
	{
		return "";
	}

	@Override
	public RConstructor getConstructor(Class[] signature)
			throws ProjectNotOpenException, ClassNotFoundException,
			RemoteException
	{
		return null;
	}

	@Override
	public RConstructor[] getConstructors() throws ProjectNotOpenException,
			ClassNotFoundException, RemoteException
	{
		return null;
	}

	@Override
	public BMethod getDeclaredMethod(String methodName, Class[] params)
			throws ProjectNotOpenException, ClassNotFoundException,
			RemoteException
	{
		return null;
	}

	@Override
	public BMethod[] getDeclaredMethods() throws ProjectNotOpenException,
			ClassNotFoundException, RemoteException
	{

		return null;
	}

	@Override
	public RField getField(String fieldName) throws ProjectNotOpenException,
			ClassNotFoundException, RemoteException
	{

		return null;
	}

	@Override
	public BField[] getFields() throws ProjectNotOpenException,
			ClassNotFoundException, RemoteException
	{

		return null;
	}

	@Override
	public Class getJavaClass()
	{
		return cls;
	}

	@Override
	public String getName()
	{
		return cls.getSimpleName();
	}

	@Override
	public GPackage getPackage()
	{
		return pkg;
	}

	@Override
	public String getQualifiedName()
	{
		return cls.getName();
	}

	@Override
	public GClass getSuperclass()
	{
		return null;
	}

	@Override
	public String getSuperclassGuess()
	{
		return "";
	}

	@Override
	public String getToString()
	{
		return cls.toString();
	}

	@Override
	public boolean isActorClass()
	{
		return cls.equals(Actor.class);
	}

	@Override
	public boolean isActorSubclass()
	{
		return false;
	}

	@Override
	public boolean isCompiled()
	{
		return true;
	}

	@Override
	public boolean isSubclassOf(String className)
	{
		return false;
	}

	@Override
	public boolean isWorldClass()
	{
		return cls.equals(World.class);
	}

	@Override
	public boolean isWorldSubclass()
	{
		return false;
	}

	@Override
	public void nameChanged(String oldName)
	{
		return;
	}

	@Override
	public void reload()
	{
		return;
	}

	@Override
	public void remove() throws ProjectNotOpenException,
			PackageNotFoundException, ClassNotFoundException, RemoteException
	{
		return;
	}

	@Override
	public void setClassProperty(String propertyName, String value)
	{
		return;
	}

	@Override
	public void setSuperclassGuess(String superclassName)
	{
		return;
	}
}
