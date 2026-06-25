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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for KotlinFileFormValidator — verifies that the validator correctly
 * enforces BlueJ's one-concept-per-file model for Kotlin source files.
 */
public class KotlinFileFormValidatorTest
{
    private Path sourceDir;
    private TestObserver observer;

    @Before
    public void setUp() throws IOException
    {
        sourceDir = Files.createTempDirectory("kotlin-validator-test");
        observer = new TestObserver();
    }

    @After
    public void tearDown()
    {
        deleteRecursive(sourceDir.toFile());
    }

    // --- Valid files (no violations expected) ---

    @Test
    public void testSingleClassMatchingName() throws IOException
    {
        File file = writeSource("Animal.kt", "class Animal { }");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Single class matching filename should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testSingleObjectMatchingName() throws IOException
    {
        File file = writeSource("Utils.kt", "object Utils { fun help() {} }");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Single object matching filename should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testFunctionsOnlyFile() throws IOException
    {
        File file = writeSource("Helpers.kt",
                "fun greet(): String = \"Hello\"\nfun farewell(): String = \"Bye\"");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Functions-only file should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testFunctionsAndPropertiesFile() throws IOException
    {
        File file = writeSource("Config.kt",
                "val x = 1\nfun f(): Int = x");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Functions + properties file should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testNestedClassAllowed() throws IOException
    {
        File file = writeSource("Outer.kt",
                "class Outer {\n    class Inner\n}");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Nested classes inside a class should be allowed", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testCompanionObjectAllowed() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo {\n    companion object {\n        fun create(): Foo = Foo()\n    }\n}");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Companion object should be allowed", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testEmptyFile() throws IOException
    {
        File file = writeSource("Empty.kt", "   \n  \n");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Empty file should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testPackageOnlyFile() throws IOException
    {
        File file = writeSource("Pkg.kt", "package animals\n");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Package-only file should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testEnumClassMatchingName() throws IOException
    {
        File file = writeSource("Color.kt",
                "enum class Color { RED, GREEN, BLUE }");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Enum class matching filename should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testDataClassMatchingName() throws IOException
    {
        File file = writeSource("Point.kt",
                "data class Point(val x: Int, val y: Int)");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Data class matching filename should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testSealedClassMatchingName() throws IOException
    {
        File file = writeSource("Shape.kt",
                "sealed class Shape");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Sealed class matching filename should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testInterfaceMatchingName() throws IOException
    {
        File file = writeSource("Drawable.kt",
                "interface Drawable { fun draw() }");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Interface matching filename should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testTypeAliasAlongsideClass() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\ntypealias FooList = List<Foo>");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Type alias alongside class should be allowed", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    // --- V1: Multiple classes ---

    @Test
    public void testV1TwoClasses() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nclass Bar");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Two classes should fail validation", result);
        assertEquals("One error expected (on second class)", 1, observer.errors.size());
        assertTrue("Error should mention both class names",
                observer.errors.get(0).getMessage().englishMessage().contains("Foo")
                && observer.errors.get(0).getMessage().englishMessage().contains("Bar"));
    }

    @Test
    public void testV1ErrorPointsAtKeywordNotDocComment() throws IOException
    {
        // Doc comment on lines 1-3, "class Bar" on line 4
        File file = writeSource("Foo.kt",
                "class Foo\n/**\n * Some doc\n */\nclass Bar");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        Diagnostic error = observer.errors.get(0);
        assertEquals("Error should point at 'class' keyword, not doc comment",
                5, error.getStartLine());
    }

    @Test
    public void testV1ClassAndObject() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nobject Bar");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class + object should fail validation", result);
        assertEquals("One error expected (on object)", 1, observer.errors.size());
        assertTrue("Error message should mention multiple declarations",
                observer.errors.get(0).getMessage().englishMessage().contains("Only one class or object"));
    }

    @Test
    public void testV1ThreeClasses() throws IOException
    {
        File file = writeSource("A.kt",
                "class A\nclass B\nclass C");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Three classes should fail validation", result);
        assertEquals("Two errors expected (on second and third class)", 2, observer.errors.size());
    }

    // --- V2: Name mismatch ---

    @Test
    public void testV2NameMismatch() throws IOException
    {
        File file = writeSource("Animal.kt",
                "class Dog");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class name mismatch should fail validation", result);
        assertEquals("One error expected", 1, observer.errors.size());

        String errorText = observer.errors.get(0).getMessage().englishMessage();
        assertTrue("Error should mention the class name",
                errorText.contains("Dog"));
        assertTrue("Error should suggest the correct filename",
                errorText.contains("Dog.kt"));
    }

    // --- V3: Mixed class + functions ---

    @Test
    public void testV3ClassAndFunction() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nfun helper(): Int = 42");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class + top-level function should fail validation", result);
        assertEquals("Two errors expected (one on class, one on function)",
                2, observer.errors.size());
    }

    @Test
    public void testV3ClassAndMultipleFunctions() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nfun a() {}\nfun b() {}");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class + multiple functions should fail", result);
        // One error on class + one error on first function only
        assertEquals("Two errors: first class + first function", 2, observer.errors.size());
    }

    @Test
    public void testV3ErrorsPointAtBothSides() throws IOException
    {
        // "class Foo" on line 1, "fun helper" on line 2
        File file = writeSource("Foo.kt",
                "class Foo\nfun helper(): Int = 42");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertEquals("First error on line 1 (class)", 1, observer.errors.get(0).getStartLine());
        assertEquals("Second error on line 2 (function)", 2, observer.errors.get(1).getStartLine());
    }

    // --- V4: Mixed class + properties ---

    @Test
    public void testV4ClassAndProperty() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nval x = 1");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class + top-level property should fail validation", result);
        assertEquals("Two errors (one on class, one on property)", 2, observer.errors.size());

        String errorText = observer.errors.get(0).getMessage().englishMessage();
        assertTrue("Error should mention top-level properties",
                errorText.contains("top-level properties"));
    }

    @Test
    public void testV4ClassAndVarProperty() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nvar y = \"hello\"");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class + var property should fail", result);
        assertEquals("Two errors (one on class, one on property)", 2, observer.errors.size());
    }

    @Test
    public void testClassWithFunctionsAndProperties() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nfun f() {}\nval x = 1");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Class + functions + properties should fail", result);
        assertEquals("Two errors only (no duplicates)", 2, observer.errors.size());
        assertTrue("Message should mention both functions and properties",
                observer.errors.get(0).getMessage().englishMessage().contains("functions and properties"));
    }

    // --- Diagnostic properties ---

    @Test
    public void testDiagnosticHasKotlinOrigin() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nclass Bar");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertEquals("Should have KOTLIN origin",
                "kotlin", observer.errors.get(0).getOrigin());
    }

    @Test
    public void testDiagnosticHasLineNumber() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nclass Bar");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        Diagnostic error = observer.errors.get(0);
        assertTrue("Error should have a valid start line (> 0)",
                error.getStartLine() > 0);
        assertTrue("Error should have a valid start column (> 0)",
                error.getStartColumn() > 0);
    }

    @Test
    public void testDiagnosticHasCorrectFileName() throws IOException
    {
        File file = writeSource("Animal.kt", "class Dog");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertEquals("Diagnostic should reference the source file",
                "Animal.kt", observer.errors.get(0).getFileName());
    }

    @Test
    public void testDiagnosticHasPositiveIdentifier() throws IOException
    {
        File file = writeSource("Foo.kt",
                "class Foo\nclass Bar");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Diagnostic should have a non-negative identifier",
                observer.errors.get(0).getIdentifier() >= 0);
    }

    // --- Multi-file behavior ---

    @Test
    public void testMultipleFilesOneViolation() throws IOException
    {
        File good = writeSource("Good.kt", "class Good");
        File bad = writeSource("Bad.kt", "class Bad\nclass Extra");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{good, bad}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Validation should fail when any file has violations", result);
        assertEquals("One error from the bad file", 1, observer.errors.size());
        assertEquals("Error should reference Bad.kt",
                "Bad.kt", observer.errors.get(0).getFileName());
    }

    @Test
    public void testMultipleFilesBothValid() throws IOException
    {
        File file1 = writeSource("Cat.kt", "class Cat");
        File file2 = writeSource("Dog.kt", "class Dog");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file1, file2}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Both valid files should pass", result);
        assertEquals("No errors expected", 0, observer.errors.size());
    }

    @Test
    public void testMultipleFilesBothInvalid() throws IOException
    {
        File file1 = writeSource("A.kt", "class A\nclass B");
        File file2 = writeSource("C.kt", "class C\nclass D");

        boolean result = KotlinFileFormValidator.validate(
                new File[]{file1, file2}, observer, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Both invalid files should fail", result);
        assertEquals("One error per file", 2, observer.errors.size());
    }

    // --- V2 line/column positioning ---

    @Test
    public void testV2ErrorPointsToClassName() throws IOException
    {
        // "class Dog" — "Dog" starts at column 7 (1-based)
        File file = writeSource("Animal.kt", "class Dog");

        KotlinFileFormValidator.validate(
                new File[]{file}, observer, CompileType.EXPLICIT_USER_COMPILE);

        Diagnostic error = observer.errors.get(0);
        assertEquals("Error should be on line 1", 1, error.getStartLine());
        // "Dog" starts at offset 6 (0-based) → column 7 (1-based)
        assertEquals("Error should point to the class name identifier", 7, error.getStartColumn());
    }

    // ---- Test infrastructure ----

    private File writeSource(String fileName, String content) throws IOException
    {
        Path file = sourceDir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }

    private static class TestObserver implements CompileObserver
    {
        final List<Diagnostic> errors = new ArrayList<>();
        final List<Diagnostic> warnings = new ArrayList<>();
        final List<Diagnostic> notes = new ArrayList<>();

        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason,
                CompileType type, int compilationSequence)
        {
        }

        @Override
        public void compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            if (diagnostic.getType() == Diagnostic.ERROR) {
                errors.add(diagnostic);
            } else if (diagnostic.getType() == Diagnostic.WARNING) {
                warnings.add(diagnostic);
            } else {
                notes.add(diagnostic);
            }
        }

        @Override
        public void endCompile(CompileInputFile[] sources, boolean succesful,
                CompileType type, int compilationSequence)
        {
        }
    }

    private static void deleteRecursive(File file)
    {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
