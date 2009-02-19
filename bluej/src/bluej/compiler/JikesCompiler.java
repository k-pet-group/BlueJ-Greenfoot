/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.compiler;

import java.io.*;
import java.util.*;

import bluej.utility.DialogManager;

/**
 * JikesCompiler class - an implementation for the BlueJ "Compiler"
 * class. This implementation provides an interface to IBM's jikes
 * compiler. Verified working with Jikes 1.12.
 *
 * @author  Andrew Patterson
 * @version $Id: JikesCompiler.java 6163 2009-02-19 18:09:55Z polle $
 */
class JikesCompiler extends Compiler
{
    String executable;
   
    public JikesCompiler(String executable)
    {
        this.executable = executable;
        setDebug(true);
        setDeprecation(true);
    }  

    public boolean compile(File[] sources, CompileObserver watcher, boolean internal)
    {
        List args = new ArrayList();

        args.add(executable);

        args.add("-nowarn");	// suppress warnings
        args.add("+D");		// generate Emacs style error messages
        args.add("-Xstdout"); // errors must go to stdout

        if(getDestDir() != null) {
            args.add("-d");
            args.add(getDestDir());
        }

        // as of Jikes 0.50, jikes will not automatically find the standard
        // JDK 1.2 classes because of changes Sun has made to the classpath
        // mechanism. We will supply jikes with the sun boot classes
        if(getProjectClassLoader() != null) {
            args.add("-classpath");
            args.add(getProjectClassLoader().getClassPathAsString() + File.pathSeparator + System.getProperty("sun.boot.class.path"));
        }

        if(isDebug())
            args.add("-g");

        if(isDeprecation())
            args.add("-deprecation");

        for(int i = 0; i < sources.length; i++)
            args.add(sources[i].getPath());

        int length = args.size();
        String[] params = new String[length];
        args.toArray(params);

        boolean result = false;

        try {
            result = executeCompiler(params, watcher);
        }
        catch (Exception ioe) {
            DialogManager.showErrorWithText(null, "cannot-run-compiler",
        			    executable);
        }

        return result;
    }

    private boolean executeCompiler(String[] params, CompileObserver watcher) 
        throws IOException, InterruptedException
    {
	int processresult = 0;		// default to fail in case we don't even start compiler process
	boolean readerror = false;

	Process compiler = Runtime.getRuntime().exec(params);

	BufferedReader d = new BufferedReader(new InputStreamReader(compiler.getInputStream()));
	String line;

	while((line = d.readLine()) != null) {

	     //Debug.message("Compiler message: " + line);

	    // Jikes produces error messages in the format (subject to change)
	    // /home/ajp/sample/Tester.java:10:20:10:22:
	    //    Syntax: ; expected instead of this token

	    int first_colon = line.indexOf(':', 0);

	    if(first_colon == -1) {
				// cannot read format of error message
		DialogManager.showErrorWithText(null, "compiler-error", line);
		break;
	    }

	    String filename = line.substring(0, first_colon);

	    // Windows might have a colon after drive name. If so, ignore it
	    if(! filename.endsWith(".java")) {
		first_colon = line.indexOf(':', first_colon + 1);

		if(first_colon == -1) {
		    // cannot read format of error message
		    DialogManager.showErrorWithText(null, "compiler-error",
						    line);
		    break;
		}
		filename = line.substring(0, first_colon);
	    }

	    int second_colon = line.indexOf(':', first_colon + 1);
	    if(second_colon == -1) {
				// cannot read format of error message
		DialogManager.showErrorWithText(null, "compiler-error", line);
		break;
	    }

	    int lineNo = 0;

	    try {
		lineNo = Integer.parseInt(line.substring(first_colon + 1, second_colon));
	    } catch(NumberFormatException e) {
				// ignore it
	    }

        // the explanation is the rest of the line
	    line = line.substring(second_colon + 1);

		if(line.indexOf("arning:") == -1) {
		    //System.out.println("Indicating error " + filename + " " + lineNo);
		    readerror = true;

		    watcher.errorMessage(filename, lineNo, line);
		    break;
		}
		else {
		    //System.out.println("Ignored warning");
		}
	}

    // discard the rest of the output
	while((line = d.readLine()) != null)
	    ;

	processresult = compiler.waitFor();

	// we consider ourselves successful if we got no error messages and the process
	// gave a 0 result

	return (processresult == 0 && !readerror);
    }
}
