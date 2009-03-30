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

import bluej.classmgr.BPClassLoader;
import java.io.File;

import bluej.Config;
import bluej.utility.Debug;

/**
 * Reasonably generic interface between the BlueJ IDE and the Java compiler.
 * 
 * @author Michael Cahill
 * @version $Id: JobQueue.java 6215 2009-03-30 13:28:25Z polle $
 */
public class JobQueue
{
    private static JobQueue queue = null;

    public static synchronized JobQueue getJobQueue()
    {
        if (queue == null)
            queue = new JobQueue();
        return queue;
    }

    // ---- instance ----

    private CompilerThread thread = null;
    private Compiler compiler = null;

    /**
     *  
     */
    private JobQueue()
    {
        // determine which compiler we should be using

        String compilertype = Config.getPropString("bluej.compiler.type");

        if (compilertype.equals("internal")) {

            compiler = new JavacCompilerInternal();

        }
        else if (compilertype.equals("javac")) {

            compiler = new JavacCompiler(Config.getJDKExecutablePath("bluej.compiler.executable", "javac"));

        }
        else if (compilertype.equals("jikes")) {

            compiler = new JikesCompiler(Config.getPropString("bluej.compiler.executable", "jikes"));

        }
        else {
            Debug.message(Config.getString("compiler.invalidcompiler"));
        }

        thread = new CompilerThread();

        // Lower priority to improve GUI response time during compilation
        int priority = Thread.currentThread().getPriority() - 1;
        if (priority < Thread.MIN_PRIORITY)
            priority = Thread.MIN_PRIORITY;
        thread.setPriority(Thread.currentThread().getPriority() - 1);

        thread.start();
    }

    /**
     * Adds a job to the compile queue.
     * 
     * @param sources   The files to compile
     * @param observer  Observer to be notified when compilation begins,
     *                  errors/warnings, completes
     * @param classPath The classpath to use to locate objects/source code
     * @param destDir   Destination for class files?
     * @param suppressUnchecked    Suppress "unchecked" warning in java 1.5
     */
    public void addJob(File[] sources, CompileObserver observer, BPClassLoader bpClassLoader, File destDir, boolean suppressUnchecked)
    {
        thread.addJob(new Job(sources, compiler, observer, bpClassLoader, destDir, suppressUnchecked));
    }

    /**
     * Wait until the compiler job queue is empty, then return.
     */
    public void waitForEmptyQueue()
    {
        synchronized (thread) {
            while (thread.isBusy()) {
                try {
                    thread.wait();
                }
                catch (InterruptedException ex) {}
            }
        }
    }
}
