/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2018  Michael Kolling and John Rosenberg

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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import bluej.Config;
import bluej.compiler.Diagnostic.DiagnosticOrigin;

/**
 * A compiler implementation using the Compiler API introduced in Java 6.
 * 
 * @author Marion Zalk
 */
public class CompilerAPICompiler extends Compiler
{
    private static final AtomicInteger nextDiagnosticIdentifier = new AtomicInteger(1);

    public CompilerAPICompiler()
    {
        setDebug(true);
        setDeprecation(true);
    }
    
    /**
     * Compile some source files by using the JavaCompiler API. Allows for the addition of user
     * options
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code (shell files); false if
     *            compiling user code
     * 
     * @return  true if successful
     */
    @Override
    public boolean compile(final File[] sources, final CompileObserver observer,
            final boolean internal, List<String> userOptions, Charset fileCharset, CompileType type)
    {
        boolean result = true;
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        List<String> optionsList = new ArrayList<String>();
        
        if (jc == null) {
            // We'd expect that this should never happen, but it's been reported once.
            observer.compilerMessage(new bluej.compiler.Diagnostic(bluej.compiler.Diagnostic.ERROR,
                    "The compiler does not appear to be available."), type);
            return false;
        }
        
        DiagnosticListener<JavaFileObject> diagListener = new DiagnosticListener<JavaFileObject>() {
            @Override
            public void report(Diagnostic<? extends JavaFileObject> diag)
            {
                String src = null;
                if (diag.getSource() != null)
                {
                    // With JDK 6, diag.getSource().getName()  apparently just returns the base
                    // name without a path. To get the path we need to ask for the URI.
                    //     However:
                    // With JDK 7, the diag.getSource().toURI() returns an unusable URI if the
                    // path is a UNC path (\\server\sharename\projdir\somefile.java).
                    
                    if (Config.isJava17()) {
                        src = diag.getSource().getName();
                    }
                    else {
                        // See bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6419926
                        // JDK6 returns URIs without a scheme in some cases, so always resolve against a
                        // known "file:/" URI:
                        URI srcUri = sources[0].toURI().resolve(diag.getSource().toUri());
                        src = new File(srcUri).getPath();
                    }
                }
                
                int diagType;
                bluej.compiler.Diagnostic bjDiagnostic;
                String message = diag.getMessage(null);
                
                if (diag.getKind() == Diagnostic.Kind.ERROR) {
                    diagType = bluej.compiler.Diagnostic.ERROR;
                    message = processMessage(src, (int) diag.getLineNumber(), message);
                    long beginCol = diag.getColumnNumber();
                    long endCol = diag.getEndPosition() - diag.getPosition() + beginCol;
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7158654
                    // getEndPosition() shouldn't return NOPOS (-1) if getStartPosition()
                    //    doesn't - but sometimes it does.
                    if (diag.getEndPosition() == Diagnostic.NOPOS) {
                        endCol = beginCol;
                    }
                    // It turns out that sometimes (not sure if bug or feature) java can
                    // give back a diagnostic with a line number of -1.  In the case I've seen
                    // (see GREENFOOT-596), it givens an error with -1 "'void' type not allowed here"
                    // then gives a second error with actual line number and more helpful message
                    // "bad operand types for binary operator '+'".  So I think we can just ignore
                    // the -1 error instead of trying to display it:
                    
                    if (diag.getLineNumber() == -1)
                        bjDiagnostic = null;
                    else
                        bjDiagnostic = new bluej.compiler.Diagnostic(diagType,
                            message, src, diag.getLineNumber(), beginCol,
                            diag.getLineNumber(), endCol, DiagnosticOrigin.JAVAC, getNewErrorIdentifer());
                }
                else if (diag.getKind() == Diagnostic.Kind.WARNING) {
                    if (message.startsWith("bootstrap class path not set in conjunction with -source ")) {
                        // Java 7 produces this warning if "-source 1.6" is specified
                        return;
                    }
                    if (message.startsWith("未与 -source") && message.endsWith("一起设置引导类路径")) {
                        // Chinese version of above
                        return;
                    }
                    
                    if (message.startsWith("ブートストラップ・クラスパスが-source") && message.endsWith("一緒に設定されていません")){
                        // Japanese version of above
                        return;
                    }
                    diagType = bluej.compiler.Diagnostic.WARNING;
                    long beginCol = diag.getColumnNumber();
                    long endCol = diag.getEndPosition() - diag.getPosition() + beginCol;
                    bjDiagnostic = new bluej.compiler.Diagnostic(diagType,
                            message, src, diag.getLineNumber(), beginCol,
                            diag.getLineNumber(), endCol, DiagnosticOrigin.JAVAC, getNewErrorIdentifer());
                }
                else {
                    diagType = bluej.compiler.Diagnostic.NOTE;
                    bjDiagnostic = new bluej.compiler.Diagnostic(diagType, message);
                    // Two variants of the warning message:
                    // - for a single file, "xyz.java uses unchecked or unsafe operations"
                    // - for multiple, "Some input files use unchecked or unsafe operations"
                    if (internal &&
                            (message.endsWith(" uses unchecked or unsafe operations.") ||
                            message.endsWith("Some input files use unchecked or unsafe operations.") ||
                            message.endsWith("Recompile with -Xlint:unchecked for details."))) {
                        return;
                    }
                }
                
                if (bjDiagnostic != null)
                    observer.compilerMessage(bjDiagnostic, type);
            }
        };
        
        try
        {  
            //setup the filemanager
            StandardJavaFileManager sjfm = jc.getStandardFileManager(diagListener, null, fileCharset);
            List<File> pathList = new ArrayList<File>();
            List<File> outputList = new ArrayList<File>();
            outputList.add(getDestDir());
            pathList.addAll(getClassPath());
            
            // In BlueJ, the destination directory and the source path are
            // always the same
            sjfm.setLocation(StandardLocation.SOURCE_PATH, outputList);
            sjfm.setLocation(StandardLocation.CLASS_PATH, pathList);
            File tempDir = null;
            if (type.keepClasses())
            {
                sjfm.setLocation(StandardLocation.CLASS_OUTPUT, outputList);
            }
            else
            {
                // We could make a new file manager that memory-mapped the output files
                // and discarded them... but creating a temporary dir is much more
                // straightforward:
                tempDir = Files.createTempDirectory("bluej").toFile();
                sjfm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tempDir));
            }
            
            //get the source files for compilation  
            Iterable<? extends JavaFileObject> compilationUnits1 =
                sjfm.getJavaFileObjectsFromFiles(Arrays.asList(sources));
            //add any options
            if(isDebug()) {
                optionsList.add("-g");
            }
            if(isDeprecation()) {
                optionsList.add("-deprecation");
            }
            
            File[] bootClassPath = getBootClassPath();
            if (bootClassPath != null && bootClassPath.length != 0) {
                sjfm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, Arrays.asList(bootClassPath));
            }
            
            optionsList.addAll(userOptions);
            
            //compile
            result = jc.getTask(null, sjfm, diagListener, optionsList, null, compilationUnits1).call();
            sjfm.close();
            if (tempDir != null)
                tempDir.delete();
        }
        catch(IOException e)
        {
            e.printStackTrace(System.out);
            return false;
        }

        return result;
    }

    /**
     * Processes messages returned from the compiler. This just slightly adjusts the format of some
     * messages.
     * 
     * @param src  The source file path
     * @param pos  The line number at which the error occurrs
     * @param message  The error message
     */
    protected String processMessage(String src, int pos, String message)
    {
        // For JDK 6, the message is in this format: 
        //   path and filename:line number:message
        // i.e includes the path and line number; so we need to strip that off.
        String expected = src + ":" + pos + ": ";
        if (message.startsWith(expected)) 
        {
            message = message.substring(expected.length());
        }
        
        if (message.contains("cannot resolve symbol")
                || message.contains("cannot find symbol")
                || message.contains("incompatible types")) 
        {
            // divide the message into lines so we can retrieve necessary values
            int index1, index2;
            String line2, line3;
            index1 = message.indexOf('\n');
            if (index1 == -1) 
            {
                // We don't know how to handle this.
                return message;
            }
            index2 = message.indexOf('\n',index1+1);
            //i.e there are only 2 lines not 3
            if (index2 < index1) 
            {
                line2 = message.substring(index1).trim();
                line3 = "";
            }
            else {
                line2 = message.substring(index1, index2).trim();
                line3 = message.substring(index2).trim();
            }
            message = message.substring(0, index1);

            //e.g incompatible types
            //found   : int
            //required: java.lang.String
            if (line2.startsWith("found") && line2.indexOf(':') != -1) 
            {
                message = message +" - found " + line2.substring(line2.indexOf(':') + 2, line2.length());
            }
            if (line3.startsWith("required") && line3.indexOf(':') != -1) {
                message = message +" but expected " + line3.substring(line3.indexOf(':') + 2, line3.length());
            }
            //e.g cannot find symbol
            //symbol: class Persons
            if (line2.startsWith("symbol") && line2.indexOf(':') != -1) 
            {
                message = message + " - " + line2.substring(line2.indexOf(':') + 2, line2.length());
            }
        }
        return message;
    }

    public static int getNewErrorIdentifer()
    {
        return nextDiagnosticIdentifier.getAndIncrement();
    }
}