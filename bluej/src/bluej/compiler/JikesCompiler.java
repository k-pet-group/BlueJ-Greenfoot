package bluej.compiler;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.lang.Runtime;

import bluej.utility.Debug;
import bluej.utility.Utility;

/**
 ** @version $Id: JikesCompiler.java 85 1999-05-17 07:16:31Z ajp $
 ** @author Andrew Patterson
 **
 ** JikesCompiler class - an implementation for the BlueJ "Compiler"
 ** class. This implementation provides an interface to IBM's jikes
 ** compiler.
 **/

public class JikesCompiler extends Compiler
{
    PrintStream output;
    String destdir;
    String classpath;
    boolean debug;
    boolean deprecation;

    public JikesCompiler(PrintStream output)
    {
	this.output = output;
	setDebug(true);
    }
	
    public void setDestDir(String destdir)
    {
	this.destdir = destdir;
    }
	
    public void setClassPath(String classpath)
    {
	this.classpath = classpath;
    }

    public void setDebug(boolean debug)
    {
	this.debug = debug;
    }

    public void setDeprecation(boolean deprecation)
    {
	this.deprecation = deprecation;
    }

    public boolean compile(String[] sources, CompileObserver watcher)
    {
	Vector args = new Vector();

	args.addElement("jikes");

	if(destdir != null) {
	    args.addElement("-d");
	    args.addElement(destdir);
	}

	if(classpath != null) {
	    args.addElement("-classpath");
	    args.addElement(classpath + ":/usr/local/jdk1.2/jre/lib/rt.jar:/usr/local/jdk1.2/jre/lib/i18n.jar");
	}
		
	if(debug)
	    args.addElement("-g");

//	currently Jikes does not have a deprecation mode		
//	if(deprecation)
//	    args.addElement("-deprecation");

	args.addElement("-nowarn");	// suppress warnings
	args.addElement("+D");		// generate Emacs style error messages
		
	for(int i = 0; i < sources.length; i++)
	    args.addElement(sources[i]);
			
	int length = args.size();
	String[] params = new String[length];
	args.copyInto(params);
		
//	System.out.println(args.toString());
		
	boolean result = false;
		
	try {
		Process compiler = Runtime.getRuntime().exec(params);

		int processresult = compiler.waitFor();

		if (processresult == 0) {	// process returns 0 - no need to read msg's
			result = true;
		}
		else {
			BufferedReader d = new BufferedReader(new InputStreamReader(compiler.getInputStream()));
			String line;

			while((line = d.readLine()) != null) {

				// Jikes produces error messages in the format (subject to change)
				// /home/ajp/sample/Tester.java:10:20:10:22:
				//    Syntax: ; expected instead of this token

				int first_colon = line.indexOf(':', 0);

				if(first_colon == -1) {
					// cannot read format of error message
					Utility.showError(null, "Compiler error:\n" + line);
					break;
				}

				String filename = line.substring(0, first_colon);

				int second_colon = line.indexOf(':', first_colon + 1);
				if(second_colon == -1) {
					// cannot read format of error message
					Utility.showError(null, "Compiler error:\n" + line);
					break;
				}

				int lineNo = 0;

				try {
        				lineNo = Integer.parseInt(line.substring(first_colon + 1, second_colon));
				} catch(NumberFormatException e) {
					// ignore it
				}

				if((line = d.readLine()) != null) {
					watcher.errorMessage(filename, lineNo, line, true);
				}
				else {
					// missing explanation part of error message
					Utility.showError(null, "Compiler error\n");
				}
 			}
		}
	}
	catch (Exception ioe) {
		Utility.showError(null, "Compiler error invoking Jikes (is jikes in your path)\n");
	}
		
	return result;
    }
}
