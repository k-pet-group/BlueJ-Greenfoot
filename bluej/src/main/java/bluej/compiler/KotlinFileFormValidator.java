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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.parser.kotlin.KotlinEnvironmentManager;

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.psi.*;

/**
 * Validates Kotlin source file structure against BlueJ's one-concept-per-file
 * educational model. Reports violations as hard errors via CompileObserver.
 */
public final class KotlinFileFormValidator
{
    private KotlinFileFormValidator()
    {
    }

    /**
     * Validate a set of Kotlin source files for file form violations.
     * All files are checked before returning (not fail-fast).
     *
     * @param sources  the .kt files to validate
     * @param observer the compilation observer for diagnostic callbacks
     * @param type     the compilation type (passed through to observer)
     * @return true if all files pass validation (no violations found)
     */
    public static boolean validate(File[] sources, CompileObserver observer,
            CompileType type)
    {
        boolean allValid = true;

        for (File file : sources) {
            String source;
            try {
                source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            catch (IOException e) {
                // If we can't read the file, let K2 handle it
                continue;
            }

            List<Diagnostic> violations = validateFile(file, source);
            if (!violations.isEmpty()) {
                allValid = false;
                for (Diagnostic diag : violations) {
                    observer.compilerMessage(diag, type);
                }
            }
        }

        return allValid;
    }

    /**
     * Checks a Kotlin file for the following violations:
     * <ul>
     *   <li>More than one class/object declaration in the file.</li>
     *   <li>A single class/object whose name does not match the file stem.</li>
     *   <li>A mix of a class/object declaration with top-level functions or properties.</li>
     * </ul>
     *
     * @param file   the source file
     * @param source the full text of the file
     * @return one {@link Diagnostic} per violation site
     */
    private static List<Diagnostic> validateFile(File file, String source)
    {
        String fileName = file.getName();
        if (!fileName.toLowerCase().endsWith(".kt"))
            return List.of();
        String fileStem = fileName.substring(0, fileName.length() - 3);

        KtFile ktFile = KotlinEnvironmentManager.getPsiFactory()
                .createFile(fileName, source);

        List<KtClassOrObject> classDecls = new ArrayList<>();
        boolean hasFunction = false;
        boolean hasProperty = false;
        KtDeclaration firstNonClass = null;

        for (KtDeclaration decl : ktFile.getDeclarations()) {
            if (decl instanceof KtClassOrObject co) {
                classDecls.add(co);
            } else if (decl instanceof KtNamedFunction) {
                hasFunction = true;
                if (firstNonClass == null) {
                    firstNonClass = decl;
                }
            } else if (decl instanceof KtProperty) {
                hasProperty = true;
                if (firstNonClass == null) {
                    firstNonClass = decl;
                }
            }
        }

        List<Diagnostic> violations = new ArrayList<>();

        if (classDecls.size() > 1) {
            String names = classDecls.stream()
                    .map(c -> c.getName() != null ? c.getName() : "<anonymous>")
                    .collect(Collectors.joining(", "));
            String message = "Only one class or object declaration is allowed per file. Found: " + names;
            for (int i = 1; i < classDecls.size(); i++) {
                violations.add(createDiagnostic(message, fileName, source,
                        getKeywordRange(classDecls.get(i))));
            }
        }

        if (classDecls.size() == 1) {
            KtClassOrObject classOrObject = classDecls.get(0);
            String className = classOrObject.getName();
            if (className != null && !className.equals(fileStem)) {
                String message = "Class '" + className + "' should be declared in a file named '" + className + ".kt'";
                TextRange range = classOrObject.getNameIdentifier() != null
                        ? classOrObject.getNameIdentifier().getTextRange()
                        : getKeywordRange(classOrObject);
                violations.add(createDiagnostic(message, fileName, source, range));
            }
        }

        if (!classDecls.isEmpty() && firstNonClass != null) {
            String what = hasFunction && hasProperty
                    ? "top-level functions and properties"
                    : hasFunction ? "top-level functions" : "top-level properties";
            String message = "A file cannot contain both a class declaration and " + what + ". "
                    + "Move one to a separate file.";
            violations.add(createDiagnostic(message, fileName, source,
                    getKeywordRange(classDecls.get(0))));

            TextRange range;
            if (firstNonClass instanceof KtNamedFunction fun && fun.getFunKeyword() != null) {
                range = fun.getFunKeyword().getTextRange();
            } else if (firstNonClass instanceof KtProperty prop && prop.getValOrVarKeyword() != null) {
                range = prop.getValOrVarKeyword().getTextRange();
            } else {
                range = firstNonClass.getTextRange();
            }
            violations.add(createDiagnostic(message, fileName, source, range));
        }

        return violations;
    }

    // Returns the keyword range (class/interface/object), skipping doc comments
    private static TextRange getKeywordRange(KtClassOrObject classOrObject)
    {
        if (classOrObject.getDeclarationKeyword() != null) {
            return classOrObject.getDeclarationKeyword().getTextRange();
        }
        if (classOrObject.getNameIdentifier() != null) {
            return classOrObject.getNameIdentifier().getTextRange();
        }
        return classOrObject.getTextRange();
    }

    private static Diagnostic createDiagnostic(String message, String fileName,
            String source, TextRange range)
    {
        int[] startPos = offsetToLineColumn(source, range.getStartOffset());
        int[] endPos = offsetToLineColumn(source, range.getEndOffset());

        return new Diagnostic(Diagnostic.ERROR,
                DiagnosticMessage.fromEnglish(message),
                fileName, startPos[0], startPos[1], endPos[0], endPos[1],
                DiagnosticOrigin.KOTLIN, Compiler.getNewErrorIdentifier());
    }

    // Converts a 0-based character offset to a 1-based [line, column] pair
    private static int[] offsetToLineColumn(String source, int offset)
    {
        int line = 1;
        int column = 1;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }
}
