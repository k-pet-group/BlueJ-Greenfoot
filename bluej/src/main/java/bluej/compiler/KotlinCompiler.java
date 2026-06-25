/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
 */
public class KotlinCompiler extends Compiler
{
    // Java sources K2 loads for Kotlin→Java symbol resolution on the next compile() call;
    // consumed and cleared inside compile(). Safe — CompilerThread is single-threaded.
    private List<CompileInputFile> javaSymbolSources;

    public KotlinCompiler()
    {
        setDebug(true);
        setDeprecation(true);
    }

    /**
     * Provide Java sources for K2 to load as symbol sources during the next
     * compile() call. Used in mixed-language packages so Kotlin code can resolve
     * Java references whose bytecode does not yet exist.
     */
    public void setJavaSymbolSources(List<CompileInputFile> javaSymbolSources)
    {
        this.javaSymbolSources = javaSymbolSources;
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
        // Snapshot and clear so state never leaks to the next compile() even if K2 throws.
        List<CompileInputFile> auxJavaSources = this.javaSymbolSources;
        this.javaSymbolSources = null;

        if (!KotlinFileFormValidator.validate(sources, observer, type)) {
            return false;
        }

        File outputDir = resolveOutputDir(type, observer);
        if (outputDir == null) {
            return false;
        }

        List<Diagnostic> diagnostics = new ArrayList<>();
        MessageCollector collector = createMessageCollector(diagnostics);

        ExitCode exitCode;
        try {
            exitCode = invokeCompiler(sources, auxJavaSources, collector, outputDir);
        }
        catch (Exception e) {
            observer.compilerMessage(new Diagnostic(Diagnostic.ERROR,
                    DiagnosticMessage.fromEnglish("Kotlin compiler internal error: " + e.getMessage())),
                    type);
            if (!type.keepClasses()) {
                cleanupTempDir(outputDir);
            }
            return false;
        }

        for (Diagnostic diagnostic : diagnostics) {
            observer.compilerMessage(diagnostic, type);
        }

        if (!type.keepClasses()) {
            cleanupTempDir(outputDir);
        }
        return exitCode == ExitCode.OK;
    }

    /**
     * Resolve the output directory for compiled class files.
     *
     * @return the output directory, or null if creation failed
     */
    private File resolveOutputDir(CompileType type, CompileObserver observer)
    {
        if (type.keepClasses()) {
            return getDestDir();
        }
        try {
            return Files.createTempDirectory("bluej-kotlin").toFile();
        }
        catch (IOException e) {
            observer.compilerMessage(new Diagnostic(Diagnostic.ERROR,
                    DiagnosticMessage.fromEnglish("Failed to create temporary directory for Kotlin compilation: " + e.getMessage())),
                    type);
            return null;
        }
    }

    /**
     * Create a MessageCollector that converts Kotlin compiler messages into
     * BlueJ Diagnostic objects. Diagnostics K2 emits against .java files (loaded
     * as symbol sources) are dropped — javac is the authoritative source for them.
     */
    private MessageCollector createMessageCollector(List<Diagnostic> diagnostics)
    {
        return new MessageCollector()
        {
            private boolean hasErrors = false;

            @Override
            public void report(CompilerMessageSeverity severity, String message,
                    CompilerMessageSourceLocation location)
            {
                if (severity == CompilerMessageSeverity.LOGGING ||
                    severity == CompilerMessageSeverity.OUTPUT) {
                    return;
                }

                if (location != null && location.getPath().toLowerCase().endsWith(".java")) {
                    return;
                }

                if (severity.isError()) {
                    hasErrors = true;
                }

                int diagType = mapSeverity(severity);
                DiagnosticMessage diagMessage = DiagnosticMessage.fromEnglish(message);

                if (location != null) {
                    diagnostics.add(createLocatedDiagnostic(diagType, diagMessage, location));
                } else {
                    diagnostics.add(new Diagnostic(diagType, diagMessage));
                }
            }

            @Override
            public boolean hasErrors()
            {
                return hasErrors;
            }

            @Override
            public void clear()
            {
                hasErrors = false;
                diagnostics.clear();
            }
        };
    }

    /**
     * Map a Kotlin compiler severity to a BlueJ diagnostic type constant.
     */
    private static int mapSeverity(CompilerMessageSeverity severity)
    {
        if (severity == CompilerMessageSeverity.ERROR || severity == CompilerMessageSeverity.EXCEPTION) {
            return Diagnostic.ERROR;
        } else if (severity == CompilerMessageSeverity.WARNING || severity == CompilerMessageSeverity.STRONG_WARNING) {
            return Diagnostic.WARNING;
        }
        return Diagnostic.NOTE;
    }

    /**
     * Create a Diagnostic with source location from a Kotlin compiler message.
     */
    private Diagnostic createLocatedDiagnostic(int diagType, DiagnosticMessage message,
            CompilerMessageSourceLocation location)
    {
        String fileName = location.getPath();
        if (fileName != null) {
            int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
            if (lastSep >= 0) {
                fileName = fileName.substring(lastSep + 1);
            }
        }

        long startLine = location.getLine();
        long startColumn = location.getColumn();
        // Use lineEnd/columnEnd if provided, else default to start position
        long endLine = location.getLineEnd() > 0 ? location.getLineEnd() : startLine;
        long endColumn = location.getColumnEnd() > 0 ? location.getColumnEnd() : startColumn;

        return new Diagnostic(diagType, message, fileName,
                startLine, startColumn, endLine, endColumn,
                DiagnosticOrigin.KOTLIN, getNewErrorIdentifier());
    }

    /**
     * Build K2 compiler arguments and invoke the compiler. Kotlin sources compile
     * to .class; auxiliary Java sources (when provided) are loaded for symbol
     * resolution only — no bytecode is emitted for them.
     */
    private ExitCode invokeCompiler(File[] sources, List<CompileInputFile> auxJavaSources,
            MessageCollector collector, File outputDir) throws Exception
    {
        K2JVMCompiler compiler = new K2JVMCompiler();
        K2JVMCompilerArguments args = new K2JVMCompilerArguments();

        args.setDestination(outputDir.getAbsolutePath());

        List<File> classPath = getClassPath();
        if (classPath != null && !classPath.isEmpty()) {
            StringBuilder cp = new StringBuilder();
            for (int i = 0; i < classPath.size(); i++) {
                if (i > 0) {
                    cp.append(File.pathSeparator);
                }
                cp.append(classPath.get(i).getAbsolutePath());
            }
            args.setClasspath(cp.toString());
        }

        args.setJvmTarget("21");
        args.setNoReflect(true);

        Stream<File> auxStream = auxJavaSources == null
                ? Stream.empty()
                : auxJavaSources.stream().map(CompileInputFile::getJavaCompileInputFile);
        args.setFreeArgs(Stream.concat(Arrays.stream(sources), auxStream)
                .map(File::getAbsolutePath)
                .toList());

        return compiler.exec(collector, Services.EMPTY, args);
    }

    /**
     * Recursively deletes a temporary directory if non-null.
     */
    private void cleanupTempDir(File tempDir)
    {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        cleanupTempDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            tempDir.delete();
        }
    }
}
