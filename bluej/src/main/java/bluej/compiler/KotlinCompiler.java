/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import bluej.compiler.Diagnostic.DiagnosticOrigin;

import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

/**
 * A compiler implementation wrapping kotlin-compiler-embeddable for
 * Kotlin compilation, analogous to CompilerAPICompiler for Java.
 *
 * @author BlueJ Team
 */
public class KotlinCompiler extends Compiler
{
    public KotlinCompiler()
    {
        setDebug(true);
        setDeprecation(true);
    }

    /**
     * Compile Kotlin source files using the K2 compiler (kotlin-compiler-embeddable).
     *
     * @param sources      The .kt files to compile
     * @param observer     The compilation observer for diagnostic callbacks
     * @param internal     True if compiling BlueJ-generated code; false for user code
     * @param userOptions  Additional compiler options
     * @param fileCharset  The character set of source files
     * @param type         The compilation type (determines whether class files are kept)
     * @return true if compilation succeeded with no errors
     */
    @Override
    public boolean compile(File[] sources, CompileObserver observer,
            boolean internal, List<String> userOptions, Charset fileCharset, CompileType type)
    {
        // Stage 1: Determine output directory
        File outputDir;
        File tempDir = null;
        if (type.keepClasses())
        {
            outputDir = getDestDir();
        }
        else
        {
            // For ERROR_CHECK_ONLY, use a temp directory (class files are discarded)
            try
            {
                tempDir = Files.createTempDirectory("bluej-kotlin").toFile();
                outputDir = tempDir;
            }
            catch (IOException e)
            {
                observer.compilerMessage(new Diagnostic(Diagnostic.ERROR,
                        DiagnosticMessage.fromEnglish("Failed to create temporary directory for Kotlin compilation: " + e.getMessage())),
                        type);
                return false;
            }
        }

        // Stage 2: Collect diagnostics via MessageCollector
        List<Diagnostic> collectedDiagnostics = new ArrayList<>();
        boolean[] hasErrors = {false};

        MessageCollector messageCollector = new MessageCollector()
        {
            @Override
            public void report(CompilerMessageSeverity severity, String message, CompilerMessageSourceLocation location)
            {
                // Filter out non-diagnostic messages (logging, output, etc.)
                if (severity == CompilerMessageSeverity.LOGGING ||
                    severity == CompilerMessageSeverity.OUTPUT)
                {
                    return;
                }

                if (severity.isError())
                {
                    hasErrors[0] = true;
                }

                int diagType;
                if (severity == CompilerMessageSeverity.ERROR || severity == CompilerMessageSeverity.EXCEPTION)
                {
                    diagType = Diagnostic.ERROR;
                }
                else if (severity == CompilerMessageSeverity.WARNING || severity == CompilerMessageSeverity.STRONG_WARNING)
                {
                    diagType = Diagnostic.WARNING;
                }
                else
                {
                    diagType = Diagnostic.NOTE;
                }

                DiagnosticMessage diagMessage = DiagnosticMessage.fromEnglish(message);

                Diagnostic diagnostic;
                if (location != null)
                {
                    String fileName = location.getPath();
                    // Extract just the filename from the path
                    if (fileName != null)
                    {
                        int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
                        if (lastSep >= 0)
                        {
                            fileName = fileName.substring(lastSep + 1);
                        }
                    }

                    long startLine = location.getLine();
                    long startColumn = location.getColumn();
                    // Kotlin's MessageCollector may provide lineEnd/columnEnd via
                    // CompilerMessageSourceLocation, but the base interface only
                    // guarantees line and column. Use them if > 0, else default.
                    long endLine = location.getLineEnd() > 0 ? location.getLineEnd() : startLine;
                    long endColumn = location.getColumnEnd() > 0 ? location.getColumnEnd() : startColumn;

                    diagnostic = new Diagnostic(diagType, diagMessage, fileName,
                            startLine, startColumn, endLine, endColumn,
                            DiagnosticOrigin.KOTLIN, getNewErrorIdentifier());
                }
                else
                {
                    diagnostic = new Diagnostic(diagType, diagMessage);
                }

                collectedDiagnostics.add(diagnostic);
            }

            @Override
            public boolean hasErrors()
            {
                return hasErrors[0];
            }

            @Override
            public void clear()
            {
                hasErrors[0] = false;
                collectedDiagnostics.clear();
            }
        };

        // Stage 3: Build K2 compiler arguments and invoke
        ExitCode exitCode;
        try
        {
            K2JVMCompiler compiler = new K2JVMCompiler();
            K2JVMCompilerArguments args = new K2JVMCompilerArguments();

            // Output directory
            args.setDestination(outputDir.getAbsolutePath());

            // Classpath
            List<File> classPath = getClassPath();
            if (classPath != null && !classPath.isEmpty())
            {
                StringBuilder cp = new StringBuilder();
                for (int i = 0; i < classPath.size(); i++)
                {
                    if (i > 0)
                    {
                        cp.append(File.pathSeparator);
                    }
                    cp.append(classPath.get(i).getAbsolutePath());
                }
                args.setClasspath(cp.toString());
            }

            // JVM target
            args.setJvmTarget("21");

            // No reflection dependency
            args.setNoReflect(true);

            // Source files
            String[] sourcePaths = new String[sources.length];
            for (int i = 0; i < sources.length; i++)
            {
                sourcePaths[i] = sources[i].getAbsolutePath();
            }
            args.setFreeArgs(List.of(sourcePaths));

            // Invoke K2 compiler
            exitCode = compiler.exec(messageCollector, Services.EMPTY, args);
        }
        catch (Exception e)
        {
            observer.compilerMessage(new Diagnostic(Diagnostic.ERROR,
                    DiagnosticMessage.fromEnglish("Kotlin compiler internal error: " + e.getMessage())),
                    type);
            cleanupTempDir(tempDir);
            return false;
        }

        // Stage 4: Dispatch collected diagnostics to observer
        for (Diagnostic diagnostic : collectedDiagnostics)
        {
            observer.compilerMessage(diagnostic, type);
        }

        // Stage 5: Cleanup and return
        cleanupTempDir(tempDir);
        return exitCode == ExitCode.OK;
    }

    /**
     * Recursively deletes a temporary directory if non-null.
     */
    private void cleanupTempDir(File tempDir)
    {
        if (tempDir != null && tempDir.exists())
        {
            File[] files = tempDir.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isDirectory())
                    {
                        cleanupTempDir(file);
                    }
                    else
                    {
                        file.delete();
                    }
                }
            }
            tempDir.delete();
        }
    }
}
