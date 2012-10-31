/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.classmgr.BPClassLoader;

/**
 * A compiler "job". A list of filenames to compile + parameters.
 * Jobs are held in a queue by the CompilerThread, which compiles them
 * by running the job's "compile" method.
 *
 * @author  Michael Cahill
 */
class Job
{
    Compiler compiler;  // The compiler for this job
    CompileObserver observer;
    File destDir;
    BPClassLoader bpClassLoader;
    File sources[];
    boolean internal; // true for compiling shell files, 
                      // or user files if we want to suppress 
                      // "unchecked" warnings, false otherwise
    private List<String> userCompileOptions;
    private Charset fileCharset;
    
    /**
     * Create a job with a set of sources.
     */
    public Job(File[] sourceFiles, Compiler compiler, CompileObserver observer,
                        BPClassLoader bpClassLoader, File destDir, boolean internal,
                        List<String> userCompileOptions, Charset fileCharset)
    {
        this.sources = sourceFiles;
        this.compiler = compiler;
        this.observer = observer;
        this.bpClassLoader = bpClassLoader;
        this.destDir = destDir;
        this.internal = internal;
        this.userCompileOptions = userCompileOptions;
        this.fileCharset = fileCharset;
    }
    
    /**
     * Compile this job
     */
    public void compile()
    {
        try {
            if(observer != null) {
                observer.startCompile(sources);
            }

            if(destDir != null) {
                compiler.setDestDir(destDir);
            }

            compiler.setClasspath(bpClassLoader.getClassPathAsFiles());
            if (bpClassLoader.loadsForJavaMEproject()) {
                compiler.setBootClassPath(bpClassLoader.getJavaMElibsAsFiles());
            }
            else {
                compiler.setBootClassPath(null);
                String majorVersion = System.getProperty("java.specification.version"); 
                userCompileOptions.add(0, "-source");
                userCompileOptions.add(1, majorVersion);
            }

            boolean successful = compiler.compile(sources, observer, internal, userCompileOptions, fileCharset);

            if(observer != null) {
                observer.endCompile(sources, successful);
            }
        } catch(Exception e) {
            System.err.println(Config.getString("compileException") + ": " + e);
            e.printStackTrace();
            if (observer != null) {
                observer.endCompile(sources, false);
            }
        }
    }
}
