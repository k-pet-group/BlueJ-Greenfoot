/*
 This file is part of the BlueJ program. 
 Copyright (C) 2023  Michael Kolling and John Rosenberg

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
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

import bluej.compiler.Diagnostic.DiagnosticOrigin;

/**
 * A compiler implementation for Kotlin source files.
 */
public class KotlinCompiler extends Compiler
{
    private static final AtomicInteger nextDiagnosticIdentifier = new AtomicInteger(1);

    public KotlinCompiler()
    {
        setDebug(true);
        setDeprecation(true);
    }

    /**
     * Compile some Kotlin source files using the Kotlin compiler.
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code; false if
     *            compiling user code
     * 
     * @return  true if successful
     */
    @Override
    public boolean compile(final List<File> sources, final CompileObserver observer,
            final boolean internal, List<String> userOptions, Charset fileCharset, CompileType type, File outputDir)
    {
        K2JVMCompiler compiler = new K2JVMCompiler();
        K2JVMCompilerArguments arguments = new K2JVMCompilerArguments();

        // Set up the compiler arguments
        arguments.setJvmTarget("1.8");
        arguments.setNoStdlib(false);
        arguments.setNoReflect(true);
        arguments.setDestination(outputDir.getAbsolutePath());

        // Set the classpath
        List<File> classPath = getClassPath();
        String classPathString = String.join(File.pathSeparator,
                classPath.stream().map(File::getAbsolutePath).toArray(String[]::new));
        arguments.setClasspath(classPathString);

        // Set the source files

        List<String> freeArgs = sources.stream().map(File::getAbsolutePath).toList();
        arguments.setFreeArgs(freeArgs);
        arguments.setAllowNoSourceFiles(true);

        // Create a message collector to handle compiler messages
        MessageCollector messageCollector = new MessageCollector() {
            @Override
            public void report(CompilerMessageSeverity severity, String message, CompilerMessageSourceLocation location) {
                if (severity.isError()) {
                    String src = location != null ? location.getPath() : null;
                    long line = location != null ? location.getLine() : -1;
                    long column = location != null ? location.getColumn() : -1;

                    Diagnostic diagnostic = new Diagnostic(Diagnostic.ERROR, 
                        new DiagnosticMessage(message, message), src, line, column, line, column, DiagnosticOrigin.KOTLIN, getNewErrorIdentifier());

                    observer.compilerMessage(diagnostic, type);
                } else if (severity.isWarning()) {
                    String src = location != null ? location.getPath() : null;
                    long line = location != null ? location.getLine() : -1;
                    long column = location != null ? location.getColumn() : -1;

                    Diagnostic diagnostic = new Diagnostic(Diagnostic.WARNING,
                        new DiagnosticMessage(message, message), src, line, column, line, column, DiagnosticOrigin.KOTLIN, getNewErrorIdentifier());

                    observer.compilerMessage(diagnostic, type);
                }
            }

            @Override
            public void clear() {
                // Nothing to do
            }

            @Override
            public boolean hasErrors() {
                return false;
            }
        };

        // Compile the source files
        ExitCode exitCode = compiler.exec(messageCollector, Services.EMPTY, arguments);

        return exitCode == ExitCode.OK;
    }

    private int getNewErrorIdentifier()
    {
        return nextDiagnosticIdentifier.getAndIncrement();
    }
}
