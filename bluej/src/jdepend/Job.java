package jdepend;

import java.io.File;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 ** @version $Id: Job.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** ClassTarget: maintain information about a class file
 **/
 
public class Job
{
	Vector classes = new Vector();			// of ClassTargets
	Hashtable dependencies = new Hashtable();	// of Jobs
	
	public void addClass(ClassTarget ct)
	{
		classes.addElement(ct);
		ct.setJob(this);
	}
	
	private void resolveDependencies()
	{
		for(Enumeration c = classes.elements(); c.hasMoreElements(); )
		{
			ClassTarget ct = (ClassTarget)c.nextElement();
			
			for(Enumeration d = ct.getDependencies(); d.hasMoreElements(); )
			{
				ClassTarget dep = (ClassTarget)d.nextElement();
				Job depjob = dep.getJob();
				if(depjob != this)
					dependencies.put(depjob, depjob);
			}
		}
	}
	
	public String getTarget()
	{
		return ((ClassTarget)classes.elementAt(0)).getClassFileName();
	}
	
	public void writeRule(PrintStream out)
	{
		if(classes.size() < 1)
			return;		// nothing to do!
			
		resolveDependencies();
			
		out.print(getTarget());
		out.print(" : ");

		// Write the source files
		for(Enumeration j = classes.elements(); j.hasMoreElements(); )
		{
			ClassTarget c = (ClassTarget)j.nextElement();

			out.print(c.getSourceFileName() + " ");
		}

		// Link to the other jobs
		for(Enumeration d = dependencies.elements(); d.hasMoreElements(); )
		{
			Job dep = (Job)d.nextElement();
			out.print(dep.getTarget() + " ");
		}

		out.println();

		if(ClassTarget.classdir != null)
			out.print("\t$(JAVAC) $(JAVAC_OPTS) -d " + quoteSloshes(ClassTarget.classdir, File.separatorChar == '\\'));
		else
			out.print("\t$(JAVAC) $(JAVAC_OPTS)");

		// Write the source files
		for(Enumeration j = classes.elements(); j.hasMoreElements(); )
		{
			ClassTarget c = (ClassTarget)j.nextElement();

			out.print(" " + quoteSloshes(c.getRealSourceFileName(), File.separatorChar == '\\'));
		}

		out.println();
		out.println();
	}
	
	private String quoteSloshes(String src, boolean quoteSep)
	{
		StringBuffer buf = new StringBuffer();
		
		for(int i = 0; i < src.length(); i++)
		{
			if((src.charAt(i) == File.separatorChar) && quoteSep)
				buf.append("\\\\");
			else
			{
				if(src.charAt(i) == '\\')
					buf.append('\\');
				buf.append(src.charAt(i));
			}
		}
		
		return buf.toString();
	}
}
