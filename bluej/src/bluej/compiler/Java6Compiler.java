/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Compiler class implemented using the JavaCompiler
 * 
 * @author Marion Zalk
 *
 */
public class Java6Compiler extends Compiler {

    /**
     * Compile some source files by using the JavaCompiler API. Allows for the addition of user
     * options
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code (shell files) False if
     *            compiling user code
     * @return    success
     */
    public boolean compile(File[] sources, CompileObserver observer,
            boolean internal) {
        boolean result = true;
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        JavacErrorWriter output = new JavacErrorWriter(internal);
        String[] options = new String[]{}; 
        try
        {            
            StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null);
            List<File> pathList = new ArrayList<File>();
            List<File> outputList= new ArrayList<File>();
            outputList.add(getDestDir());
            pathList.addAll(Arrays.asList(getProjectClassLoader().getClassPathAsFiles()));
            sjfm.setLocation(StandardLocation.SOURCE_PATH, pathList);
            sjfm.setLocation(StandardLocation.CLASS_PATH, pathList);
            sjfm.setLocation(StandardLocation.CLASS_OUTPUT, outputList);

            Iterable<? extends JavaFileObject> compilationUnits1 =
                sjfm.getJavaFileObjectsFromFiles(Arrays.asList(sources));

            if(isDebug())
                options[0]="-g";

            if(isDeprecation())
                options[1]="-deprecation"; 
            List<String> optionsList=new ArrayList<String>();
            optionsList.addAll(Arrays.asList(options));
            addUserSpecifiedOptions(optionsList, COMPILER_OPTIONS);

            jc.getTask(output, sjfm, null, optionsList, null, compilationUnits1).call();
            sjfm.close();
        }
        catch(IOException e)
        {
            e.printStackTrace(System.out);
            return false;
        }

        if (output.hasError()) 
        {
            result=false;
            observer.errorMessage(output.getFilename(),
                    output.getLineNo(),
                    output.getMessage());
        }

        // Handle compiler warning messages        
        if (output.hasWarnings()) 
        {
            observer.warningMessage(output.getFilename(),
                    output.getLineNo(),
                    output.getWarning());
        }
        return result;
    }

}
