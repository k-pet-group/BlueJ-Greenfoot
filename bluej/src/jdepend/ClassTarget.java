package jdepend;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 ** ClassTarget: maintain information about a class file
 ** $Id: ClassTarget.java 36 1999-04-27 04:04:54Z mik $
 **/
 
public class ClassTarget
{
	String name;
	String sourceFileName;
	static String classdir;
	Hashtable depNames;
	Vector dependencies;
	Job job;

	// Fields used in Tarjan's algorithm:
	int id, min_id;
	
	public ClassTarget(String name, String sourceFileName)
	{
		this.name = name;
		this.sourceFileName = sourceFileName;
		
		depNames = new Hashtable();
		dependencies = new Vector();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getBaseName()
	{
		int lastDot = name.lastIndexOf('.');
		if(lastDot > 0)
			return name.substring(lastDot + 1);
		else
			return name;
	}

	public String getSourceFileName()
	{
		return sourceFileName.replace(File.separatorChar, '/');
		// return sourceFileName;
	}

	public String getRealSourceFileName()
	{
		return sourceFileName;
	}

	public String getSourceDir()
	{
		String dir = new File(sourceFileName).getParent();
		
		return (dir != null) ? dir : ".";
	}

	public String getClassFileName()
	{
		String filename;
		
		if(classdir == null)
			filename = (new File(getSourceDir(), getBaseName() + ".class")).getPath();
		else
			filename = classdir + File.separator + name.replace('.', File.separatorChar) + ".class";
		
		return filename.replace(File.separatorChar, '/');
		// return filename;
	}
	
	public void addDependency(String depName)
	{
		depNames.put(depName, depName);
	}
	
	public void resolveDependencies(Hashtable classes)
	{
		for(Enumeration e = depNames.elements(); e.hasMoreElements(); )
		{
			String depFileName = (String)e.nextElement();
			ClassTarget dep = (ClassTarget)classes.get(depFileName);
			
			if(dep != null)
				dependencies.addElement(dep);
			// else
				// JDepend.printWarning("Couldn't find class " + depName + " that " + name + " depends on");
		}
	}
	
	public Enumeration getDependencies()
	{
		return dependencies.elements();
	}
	
	public void setJob(Job job)
	{
		this.job = job;
	}
	
	public Job getJob()
	{
		return job;
	}
}
