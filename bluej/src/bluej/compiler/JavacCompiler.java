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
 * JavacCompiler class - an implementation for the BlueJ "Compiler"
 * class. This implementation provides an interface to Sun's javac
 * compiler running through a seperate Process.
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 * @version $Id: JavacCompiler.java 6163 2009-02-19 18:09:55Z polle $
 */
class JavacCompiler extends Compiler
{
    private String executable;   

    public JavacCompiler(String executable)
    {
        this.executable = executable;
        setDebug(true);
        setDeprecation(true);
    }   

    public boolean compile(File[] sources, CompileObserver watcher, boolean internal)
    {
        List args = new ArrayList();

        args.add(executable);       

        args.addAll(getCompileOptions());        

        for(int i = 0; i < sources.length; i++)
            args.add(sources[i].getPath());

        int length = args.size();
        String[] params = new String[length];
        args.toArray(params);

        boolean result = false;

        try {
            result = executeCompiler(params, watcher, internal);
        }
        catch (Exception ioe) {
            DialogManager.showErrorWithText(null, "cannot-run-compiler",
        				    executable);
        }

        return result;
    }

    private boolean executeCompiler(String[] params, CompileObserver watcher, boolean internal)
        throws IOException, InterruptedException
    {
        int processresult = 0;		// default to fail in case we don't even start compiler process
        boolean readerror = false;
        
        Process compiler = Runtime.getRuntime().exec(params);
        
        BufferedReader d = new BufferedReader(
                new InputStreamReader(compiler.getErrorStream()));
        JavacErrorWriter output = new JavacErrorWriter(internal);
        
        String line;
        
        while((line = d.readLine()) != null)            
            output.write(line);
        
        // Handle any error message
        if (output.hasError()) {
            watcher.errorMessage(output.getFilename(),
                        output.getLineNo(),
                        output.getMessage());
        }

        // Handle compiler warning messages        
        if (output.hasWarnings()) {
            watcher.warningMessage(output.getFilename(),
                        output.getLineNo(),
                        output.getWarning());
        }
        
        processresult = compiler.waitFor();
        
        // we consider ourselves successful if we got no error messages and the process
        // gave a 0 result
        
        return (processresult == 0 && !readerror);
    }
}

