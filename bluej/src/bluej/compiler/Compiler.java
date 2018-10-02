/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2016,2018  Michael Kolling and John Rosenberg
 
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
import java.nio.charset.Charset;
import java.util.List;

/**
 * Compiler class - an abstract interface to a source-to-bytecode compiler. This
 * can be implemented by different compiler implementations.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Poul Henriksen
 */
abstract class Compiler
{
    public static final String COMPILER_OPTIONS = "bluej.compiler.options";
    
    private File destDir;
    private List<File> classPath;
    /** "boot" class path - may be null if not specified */
    private File[] bootClassPath;
    private boolean debug;
    private boolean deprecation;
    
    /**
     * Set the destination directory - the base directory for where the compiled class files
     * are output to. (The final folder for a given class depends on the class' package).
     * This currently also specifies the source path.
     * 
     * @param destDir  The destination directory
     */
    public void setDestDir(File destDir)
    {
        this.destDir = destDir;
    }

    public void setClasspath(List<File> classPath)
    {
        this.classPath = classPath;
    }
    
    /**
     * Specify the "boot classpath".
     * 
     * @param bootClassPath  The boot classpath, or null to use the default.
     */
    public void setBootClassPath(File[] bootClassPath)
    {
        this.bootClassPath = bootClassPath;
    }
    
    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setDeprecation(boolean deprecation)
    {
        this.deprecation = deprecation;
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
    
    public List<File> getClassPath()
    {
        return classPath;
    }
    
    public File[] getBootClassPath()
    {
        return bootClassPath;
    }

    /**
     * Compile some source files.
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code (shell files); false if
     *            compiling user code
     * @param options
     *            Option strings to pass to the compiler
     * @param fileCharset
     *            The character set in which source files are encoded 
     * 
     * @return  true if the compilation was successful
     */
    public abstract boolean compile(File[] sources, CompileObserver observer,
            boolean internal, List<String> options, Charset fileCharset, CompileType type);

}
