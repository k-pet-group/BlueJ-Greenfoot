/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import bluej.classmgr.BPClassLoader;
import bluej.Config;

/**
 * Compiler class - an abstract interface to a source -> bytecode compiler. This
 * can be implemented by different compiler implementations.
 * 
 * Currently known implementations: JavacCompiler, JikesCompiler.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Poul Henriksen
 * @version $Id: Compiler.java 6215 2009-03-30 13:28:25Z polle $
 */
abstract class Compiler
{
    public static final String COMPILER_OPTIONS = "bluej.compiler.options";
    public static final String JAVAME_COMPILER_OPTIONS = "bluej.javame.compiler.options";
    
    private File destDir;
    private BPClassLoader bpClassLoader;
    private boolean debug;
    private boolean deprecation;

    public void setDestDir(File destDir)
    {
        this.destDir = destDir;
    }

    public void setProjectClassLoader(BPClassLoader bpClassLoader)
    {
        this.bpClassLoader = bpClassLoader;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setDeprecation(boolean deprecation)
    {
        this.deprecation = deprecation;
    }

    public BPClassLoader getProjectClassLoader()
    {
        return bpClassLoader;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public boolean isDeprecation()
    {
        return deprecation;
    }

    public File getDestDir()
    {
        return destDir;
    }

    /**
     * Creates a list of the options that should be used for the compilation.
     * 
     * @return A list of compile options.
     */
    protected List getCompileOptions()
    {
        List args = new ArrayList();

        if (getDestDir() != null) {
            args.add("-d");
            args.add(getDestDir().getPath());
        }
               
        boolean isJavaMEproject = false;
        if (getProjectClassLoader() != null) {
            args.add("-classpath");
            args.add(getProjectClassLoader().getClassPathAsString());
            
            if ( getProjectClassLoader( ).loadsForJavaMEproject( ) )
                isJavaMEproject = true;
        }       
        
       if ( isJavaMEproject ) {
            addUserSpecifiedOptions(args, JAVAME_COMPILER_OPTIONS);
            args.add( "-bootclasspath" );
            args.add ( getProjectClassLoader( ).getJavaMElibsAsPath( ) ); 
        }
        else {
            String majorVersion = System.getProperty("java.specification.version"); 
            args.add("-source");
            args.add(majorVersion);
        }
        
        if (isDebug())
            args.add("-g");

        if (isDeprecation())
            args.add("-deprecation");      

        addUserSpecifiedOptions(args, COMPILER_OPTIONS);

        return args;
    }

    private void addUserSpecifiedOptions(List args, String options)
    {
        String compilerOptions = Config.getPropString(options, null);
        if (compilerOptions != null) {
            StringTokenizer st = new StringTokenizer(compilerOptions);
            while (st.hasMoreTokens()) {
                args.add(st.nextToken());
            }
        }
    }


    /**
     * Compile some source files.
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code (shell files) False if
     *            compiling user code
     * @return
     */
    public abstract boolean compile(File[] sources, CompileObserver observer, boolean internal);

}
