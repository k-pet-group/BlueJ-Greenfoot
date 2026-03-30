/*
 This file is part of the BlueJ program.
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin;

import bluej.parser.symtab.ClassInfo;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Tests for KotlinInfoParser — ClassInfo extraction from Kotlin source.
 */
public class KotlinInfoParserTest
{
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ClassInfo parse(String source)
    {
        return KotlinInfoParser.parse(new StringReader(source), null);
    }

    private ClassInfo parseWithPkg(String source, String targetPkg)
    {
        return KotlinInfoParser.parse(new StringReader(source), targetPkg);
    }

    // -----------------------------------------------------------------------
    // Simple class declarations
    // -----------------------------------------------------------------------

    @Test
    public void testSimpleClass()
    {
        ClassInfo info = parse("class Dog");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertFalse(info.isInterface());
        assertFalse(info.isAbstract());
        assertFalse(info.isEnum());
    }

    @Test
    public void testOpenClass()
    {
        ClassInfo info = parse("open class Animal");
        assertNotNull(info);
        assertEquals("Animal", info.getName());
        assertFalse(info.isAbstract());
    }

    @Test
    public void testAbstractClass()
    {
        ClassInfo info = parse("abstract class Shape");
        assertNotNull(info);
        assertEquals("Shape", info.getName());
        assertTrue(info.isAbstract());
    }

    @Test
    public void testDataClass()
    {
        ClassInfo info = parse("data class Point(val x: Int, val y: Int)");
        assertNotNull(info);
        assertEquals("Point", info.getName());
        assertFalse(info.isAbstract());
    }

    @Test
    public void testSealedClass()
    {
        ClassInfo info = parse("sealed class Result");
        assertNotNull(info);
        assertEquals("Result", info.getName());
        assertTrue("Sealed classes should be abstract", info.isAbstract());
    }

    @Test
    public void testEnumClass()
    {
        ClassInfo info = parse("enum class Color { RED, GREEN, BLUE }");
        assertNotNull(info);
        assertEquals("Color", info.getName());
        assertTrue(info.isEnum());
    }

    @Test
    public void testInterface()
    {
        ClassInfo info = parse("interface Drawable");
        assertNotNull(info);
        assertEquals("Drawable", info.getName());
        assertTrue(info.isInterface());
        assertTrue("Interfaces should be abstract", info.isAbstract());
    }

    @Test
    public void testObjectDeclaration()
    {
        ClassInfo info = parse("object Singleton");
        assertNotNull(info);
        assertEquals("Singleton", info.getName());
        assertFalse(info.isAbstract());
    }

    // -----------------------------------------------------------------------
    // Superclass and interfaces
    // -----------------------------------------------------------------------

    @Test
    public void testWithSuperclass()
    {
        ClassInfo info = parse("class Dog : Animal()");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertEquals("Animal", info.getSuperclass());
    }

    @Test
    public void testWithInterface()
    {
        ClassInfo info = parse("class Dog : Runnable");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertNull("No superclass (Runnable has no parens)", info.getSuperclass());
        assertTrue(info.getImplements().contains("Runnable"));
    }

    @Test
    public void testWithSuperclassAndInterfaces()
    {
        ClassInfo info = parse("class Dog : Animal(), Runnable, Serializable");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertEquals("Animal", info.getSuperclass());
        assertTrue(info.getImplements().contains("Runnable"));
        assertTrue(info.getImplements().contains("Serializable"));
    }

    @Test
    public void testWithPrimaryConstructorAndSuperclass()
    {
        ClassInfo info = parse("class Dog(val name: String) : Animal()");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertEquals("Animal", info.getSuperclass());
    }

    // -----------------------------------------------------------------------
    // Type parameters
    // -----------------------------------------------------------------------

    @Test
    public void testTypeParameter()
    {
        ClassInfo info = parse("class Box<T>(val item: T)");
        assertNotNull(info);
        assertEquals("Box", info.getName());
        assertTrue(info.getTypeParameterTexts().contains("T"));
    }

    @Test
    public void testMultipleTypeParameters()
    {
        ClassInfo info = parse("class Pair<A, B>");
        assertNotNull(info);
        assertEquals("Pair", info.getName());
        assertEquals(2, info.getTypeParameterTexts().size());
    }

    // -----------------------------------------------------------------------
    // Package and imports (basic — class found correctly)
    // -----------------------------------------------------------------------

    @Test
    public void testWithPackageDeclaration()
    {
        ClassInfo info = parse("package com.example\n\nclass Dog");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
    }

    @Test
    public void testWithImports()
    {
        ClassInfo info = parse(
            "package com.example\n\n"
            + "import kotlin.collections.List\n"
            + "import kotlin.math.PI\n\n"
            + "class Calculator"
        );
        assertNotNull(info);
        assertEquals("Calculator", info.getName());
    }

    // -----------------------------------------------------------------------
    // Modifiers and annotations
    // -----------------------------------------------------------------------

    @Test
    public void testPrivateClass()
    {
        ClassInfo info = parse("private class Helper");
        assertNotNull(info);
        assertEquals("Helper", info.getName());
        // ClassInfo.foundPublicClass() should be false for private classes
        assertFalse(info.foundPublicClass());
    }

    @Test
    public void testPublicClass()
    {
        ClassInfo info = parse("public class Service");
        assertNotNull(info);
        assertEquals("Service", info.getName());
        assertTrue(info.foundPublicClass());
    }

    @Test
    public void testClassWithAnnotation()
    {
        ClassInfo info = parse("@Deprecated\nclass OldClass");
        assertNotNull(info);
        assertEquals("OldClass", info.getName());
    }

    @Test
    public void testClassWithAnnotationAndArgs()
    {
        ClassInfo info = parse("@Suppress(\"unused\")\nclass Helper");
        assertNotNull(info);
        assertEquals("Helper", info.getName());
    }

    // -----------------------------------------------------------------------
    // Complex realistic examples
    // -----------------------------------------------------------------------

    @Test
    public void testFullClassWithBody()
    {
        String source =
            "package animals\n\n"
            + "/** A dog that can run. */\n"
            + "open class Dog(val name: String, val age: Int) : Animal(), Runnable {\n"
            + "    override fun run() {\n"
            + "        println(\"$name is running!\")\n"
            + "    }\n"
            + "    fun bark(): String = \"Woof!\"\n"
            + "}";

        ClassInfo info = parse(source);
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertEquals("Animal", info.getSuperclass());
        assertTrue(info.getImplements().contains("Runnable"));
        assertFalse(info.isAbstract());
    }

    @Test
    public void testAbstractClassWithBody()
    {
        String source =
            "abstract class Shape {\n"
            + "    abstract fun area(): Double\n"
            + "    fun describe(): String = \"I am a shape\"\n"
            + "}";

        ClassInfo info = parse(source);
        assertNotNull(info);
        assertEquals("Shape", info.getName());
        assertTrue(info.isAbstract());
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyFile()
    {
        ClassInfo info = parse("");
        assertNull("Empty file should return null", info);
    }

    @Test
    public void testOnlyComments()
    {
        ClassInfo info = parse("// just a comment\n/* block */");
        assertNull("File with only comments should return null", info);
    }

    @Test
    public void testOnlyPackageAndImports()
    {
        ClassInfo info = parse("package foo\nimport bar.Baz");
        assertNull("File with only package/imports should return null", info);
    }

    // =======================================================================
    // NEW TESTS — Package extraction (fixes gap 1)
    // =======================================================================

    @Test
    public void testPackageNameExtracted()
    {
        ClassInfo info = parse("package animals\nclass Dog");
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertEquals("animals", info.getPackage());
        assertTrue(info.hasPackageStatement());
    }

    @Test
    public void testSubPackageNameExtracted()
    {
        ClassInfo info = parse("package com.example.animals\nclass Dog");
        assertNotNull(info);
        assertEquals("com.example.animals", info.getPackage());
        assertTrue(info.hasPackageStatement());
    }

    @Test
    public void testDefaultPackage()
    {
        ClassInfo info = parse("class Dog");
        assertNotNull(info);
        assertEquals("", info.getPackage());
        assertFalse(info.hasPackageStatement());
    }

    // =======================================================================
    // NEW TESTS — Import tracking (fixes gap 2)
    // =======================================================================

    @Test
    public void testImportsAddedToUsed()
    {
        ClassInfo info = parse(
            "import animals.Animal\n"
            + "import animals.Habitat\n"
            + "class Dog : Animal()"
        );
        assertNotNull(info);
        // Animal is the superclass (goes to superclass, removed from used)
        // Habitat should be in the used list from imports
        assertTrue("Imported non-primitive type should be in used list",
            info.getUsed().contains("Habitat"));
    }

    @Test
    public void testPrimitiveImportsExcluded()
    {
        ClassInfo info = parse(
            "import kotlin.Int\n"
            + "import kotlin.String\n"
            + "class Foo"
        );
        assertNotNull(info);
        assertFalse("Primitive type Int should not be in used list",
            info.getUsed().contains("Int"));
        assertFalse("Primitive type String should not be in used list",
            info.getUsed().contains("String"));
    }

    // =======================================================================
    // NEW TESTS — Method/property type extraction (fixes gap 3)
    // =======================================================================

    @Test
    public void testMethodReturnTypeInUsed()
    {
        ClassInfo info = parse(
            "class Foo {\n"
            + "    fun getItems(): ArrayList<String> = TODO()\n"
            + "}"
        );
        assertNotNull(info);
        assertTrue("Method return type should be in used list",
            info.getUsed().contains("ArrayList"));
    }

    @Test
    public void testMethodParamTypeInUsed()
    {
        ClassInfo info = parse(
            "class Foo {\n"
            + "    fun process(handler: Handler): Unit = TODO()\n"
            + "}"
        );
        assertNotNull(info);
        assertTrue("Method parameter type should be in used list",
            info.getUsed().contains("Handler"));
    }

    @Test
    public void testPropertyTypeInUsed()
    {
        ClassInfo info = parse(
            "class Foo {\n"
            + "    val items: MutableList<String> = mutableListOf()\n"
            + "}"
        );
        assertNotNull(info);
        assertTrue("Property type should be in used list",
            info.getUsed().contains("MutableList"));
    }

    // =======================================================================
    // NEW TESTS — KDoc extraction
    // =======================================================================

    @Test
    public void testKDocCommentExtracted()
    {
        ClassInfo info = parse("/** A dog. */\nclass Dog");
        assertNotNull(info);
        assertFalse("Class KDoc should be extracted",
            info.getCommentsAsList().isEmpty());
        assertEquals("Dog", info.getCommentsAsList().get(0).target);
    }

    @Test
    public void testMethodKDocExtracted()
    {
        ClassInfo info = parse(
            "class Foo {\n"
            + "    /** Runs the task. */\n"
            + "    fun run() {}\n"
            + "}"
        );
        assertNotNull(info);
        // Should have at least one comment for the method
        boolean hasMethodComment = info.getCommentsAsList().stream()
            .anyMatch(c -> c.target.contains("run"));
        assertTrue("Method KDoc should be extracted", hasMethodComment);
    }

    // =======================================================================
    // NEW TESTS — targetPkg validation (fixes gap 4)
    // =======================================================================

    @Test
    public void testTargetPkgMismatchSetsError()
    {
        ClassInfo info = parseWithPkg("package foo\nclass X", "bar");
        assertNotNull(info);
        assertTrue("Package mismatch should set parse error",
            info.hadParseError());
    }

    @Test
    public void testTargetPkgMatchNoError()
    {
        ClassInfo info = parseWithPkg("package foo\nclass X", "foo");
        assertNotNull(info);
        assertFalse("Package match should not set parse error",
            info.hadParseError());
    }

    // =======================================================================
    // NEW TESTS — Constructor parameter types
    // =======================================================================

    @Test
    public void testConstructorParamTypesInUsed()
    {
        ClassInfo info = parse("class Dog(val name: String, val owner: Person)");
        assertNotNull(info);
        assertTrue("Non-primitive constructor param type should be in used",
            info.getUsed().contains("Person"));
        assertFalse("Primitive constructor param type should not be in used",
            info.getUsed().contains("String"));
    }

    // =======================================================================
    // NEW TESTS — Qualified and generic supertypes
    // =======================================================================

    @Test
    public void testQualifiedSupertype()
    {
        ClassInfo info = parse("class Dog : com.example.Animal()");
        assertNotNull(info);
        assertEquals("Simple name should be extracted from qualified supertype",
            "Animal", info.getSuperclass());
    }

    @Test
    public void testGenericSupertype()
    {
        ClassInfo info = parse("class StringList : ArrayList<String>()");
        assertNotNull(info);
        assertEquals("Generic supertype name should be extracted without type args",
            "ArrayList", info.getSuperclass());
    }

    // =======================================================================
    // NEW TESTS — Object with supertype
    // =======================================================================

    @Test
    public void testObjectWithSupertype()
    {
        ClassInfo info = parse("object Singleton : Base()");
        assertNotNull(info);
        assertEquals("Singleton", info.getName());
        assertEquals("Base", info.getSuperclass());
    }

    // =======================================================================
    // NEW TESTS — Top-level functions (function-only files)
    // =======================================================================

    /**
     * Parse with an explicit file name, needed for function-only files
     * where the ClassInfo name is derived from the file stem.
     */
    private ClassInfo parseWithFileName(String source, String fileName)
    {
        return KotlinInfoParser.parse(new StringReader(source), null, fileName);
    }

    @Test
    public void testFunctionOnlyFile()
    {
        ClassInfo info = parseWithFileName(
            "fun greet(name: String): String {\n"
            + "    return \"Hello, $name!\"\n"
            + "}",
            "Utils.kt"
        );
        assertNotNull("Function-only file should return non-null ClassInfo", info);
        assertEquals("Utils", info.getName());
        assertTrue("Should be marked as top-level functions only",
            info.isTopLevelFunctionsOnly());
        assertTrue(info.foundPublicClass());
    }

    @Test
    public void testFunctionOnlyFileModifiers()
    {
        ClassInfo info = parseWithFileName(
            "fun doSomething() {}",
            "Helpers.kt"
        );
        assertNotNull(info);
        assertFalse("Function file should not be interface", info.isInterface());
        assertFalse("Function file should not be abstract", info.isAbstract());
        assertFalse("Function file should not be enum", info.isEnum());
        assertTrue(info.isTopLevelFunctionsOnly());
    }

    @Test
    public void testFunctionOnlyFileWithPackage()
    {
        ClassInfo info = parseWithFileName(
            "package com.example\n\n"
            + "fun compute(): Int = 42",
            "MathUtils.kt"
        );
        assertNotNull(info);
        assertEquals("MathUtils", info.getName());
        assertEquals("com.example", info.getPackage());
        assertTrue(info.hasPackageStatement());
        assertTrue(info.isTopLevelFunctionsOnly());
    }

    @Test
    public void testFunctionOnlyFileWithImports()
    {
        ClassInfo info = parseWithFileName(
            "import animals.Dog\n"
            + "import animals.Cat\n\n"
            + "fun createDog(): Dog = Dog()",
            "Factory.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        assertTrue("Imported type Dog should be in used list",
            info.getUsed().contains("Dog"));
        assertTrue("Imported type Cat should be in used list",
            info.getUsed().contains("Cat"));
    }

    @Test
    public void testFunctionOnlyFileDependencies()
    {
        ClassInfo info = parseWithFileName(
            "fun process(data: DataSet): Result {\n"
            + "    return Result()\n"
            + "}",
            "Processor.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        assertTrue("Parameter type should be in used list",
            info.getUsed().contains("DataSet"));
        assertTrue("Return type should be in used list",
            info.getUsed().contains("Result"));
    }

    @Test
    public void testFunctionOnlyFilePrimitiveTypesExcluded()
    {
        ClassInfo info = parseWithFileName(
            "fun add(a: Int, b: Int): Int = a + b\n"
            + "fun greet(name: String): String = \"Hi\"",
            "Primitives.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        assertFalse("Int should not be in used list",
            info.getUsed().contains("Int"));
        assertFalse("String should not be in used list",
            info.getUsed().contains("String"));
    }

    @Test
    public void testFunctionOnlyFileMultipleFunctions()
    {
        ClassInfo info = parseWithFileName(
            "fun first(): Handler = TODO()\n"
            + "fun second(): Processor = TODO()\n"
            + "fun third(input: DataSource): Output = TODO()",
            "Operations.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        assertTrue(info.getUsed().contains("Handler"));
        assertTrue(info.getUsed().contains("Processor"));
        assertTrue(info.getUsed().contains("DataSource"));
        assertTrue(info.getUsed().contains("Output"));
    }

    @Test
    public void testFunctionOnlyFileKDocExtracted()
    {
        ClassInfo info = parseWithFileName(
            "/** Greets the user. */\n"
            + "fun greet(name: String): String = \"Hello\"",
            "Greetings.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        boolean hasGreetComment = info.getCommentsAsList().stream()
            .anyMatch(c -> c.target.contains("greet"));
        assertTrue("KDoc on top-level function should be extracted", hasGreetComment);
    }

    @Test
    public void testFunctionOnlyFileNameStemExtraction()
    {
        // File name without .kt extension should still work
        ClassInfo info = parseWithFileName(
            "fun example() {}",
            "MyHelpers.kt"
        );
        assertNotNull(info);
        assertEquals("Name should be file stem without .kt",
            "MyHelpers", info.getName());
    }

    @Test
    public void testMixedFileReturnsClass()
    {
        // File has both a class and top-level functions.
        // The class should be returned (existing behavior).
        ClassInfo info = parseWithFileName(
            "class Dog(val name: String)\n\n"
            + "fun createDog(): Dog = Dog(\"Rex\")",
            "Dog.kt"
        );
        assertNotNull(info);
        assertEquals("Dog", info.getName());
        assertFalse("Mixed file should return class, not function-only",
            info.isTopLevelFunctionsOnly());
    }

    @Test
    public void testEmptyFileStillNull()
    {
        // Regression: empty file should still return null
        ClassInfo info = parseWithFileName("", "Empty.kt");
        assertNull("Empty file should return null", info);
    }

    @Test
    public void testOnlyCommentsStillNull()
    {
        // Regression: file with only comments should return null
        ClassInfo info = parseWithFileName(
            "// just a comment\n/* block comment */",
            "Comments.kt"
        );
        assertNull("File with only comments should return null", info);
    }

    @Test
    public void testOnlyPackageAndImportsStillNull()
    {
        // Regression: file with only package/imports but no class or functions
        ClassInfo info = parseWithFileName(
            "package foo\nimport bar.Baz",
            "Empty.kt"
        );
        assertNull("File with only package/imports should return null", info);
    }

    @Test
    public void testFunctionOnlyFileTargetPkgMismatch()
    {
        ClassInfo info = KotlinInfoParser.parse(
            new StringReader("package foo\nfun test() {}"),
            "bar",
            "Test.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        assertTrue("Package mismatch should set parse error",
            info.hadParseError());
    }

    @Test
    public void testFunctionOnlyFileTargetPkgMatch()
    {
        ClassInfo info = KotlinInfoParser.parse(
            new StringReader("package foo\nfun test() {}"),
            "foo",
            "Test.kt"
        );
        assertNotNull(info);
        assertTrue(info.isTopLevelFunctionsOnly());
        assertFalse("Package match should not set parse error",
            info.hadParseError());
    }

    @Test
    public void testClassFileNotMarkedAsTopLevelFunctions()
    {
        // Regression: regular class files should NOT have the flag set
        ClassInfo info = parse("class Dog");
        assertNotNull(info);
        assertFalse("Regular class should not be top-level functions only",
            info.isTopLevelFunctionsOnly());
    }
}
