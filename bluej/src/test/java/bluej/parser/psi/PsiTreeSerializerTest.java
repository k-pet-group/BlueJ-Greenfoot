package bluej.parser.psi;

import org.jetbrains.kotlin.psi.KtFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for PsiTreeSerializer.
 * 
 * <p>Tests cover all major Kotlin constructs and edge cases:</p>
 * <ul>
 *   <li>Simple functions</li>
 *   <li>Data classes with properties</li>
 *   <li>Class inheritance and supertypes</li>
 *   <li>Companion objects</li>
 *   <li>Location information</li>
 *   <li>File writing</li>
 *   <li>Empty files</li>
 *   <li>Complex multi-element files</li>
 *   <li>Objects (singleton)</li>
 *   <li>Properties with accessors</li>
 * </ul>
 * 
 * @see PsiTreeSerializer
 * @see PsiEnvironment
 */
public class PsiTreeSerializerTest {
    
    private PsiEnvironment env;
    
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }
    
    @After
    public void tearDown() {
        // No cleanup needed - singleton persists
    }
    
    // ==================== BASIC TESTS ====================
    
    @Test
    public void testSerializeSimpleFunction() {
        String source = "fun hello() = 42";
        KtFile file = env.parseFile("Test.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertNotNull("Output should not be null", output);
        assertTrue("Should contain PSI Tree header", output.contains("PSI Tree: Test.kt"));
        assertTrue("Should contain function", output.contains("Function: hello"));
        assertTrue("Should contain return type", output.contains("Return Type:"));
        assertTrue("Should contain parameters", output.contains("Parameters: []"));
        assertTrue("Should contain body presence", output.contains("Body: present"));
    }
    
    @Test
    public void testSerializeDataClass() {
        String source = "data class User(val name: String, var age: Int)";
        KtFile file = env.parseFile("User.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should be data class", output.contains("Data Class: User"));
        assertTrue("Should have name property", output.contains("Property: name"));
        assertTrue("Should have age property", output.contains("Property: age"));
        assertTrue("Should have data modifier", output.contains("data"));
        assertTrue("Should have val modifier", output.contains("val"));
        assertTrue("Should have var modifier", output.contains("var"));
    }
    
    @Test
    public void testSerializeClassWithInheritance() {
        String source = "open class Base\nclass Derived : Base()";
        KtFile file = env.parseFile("Inheritance.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have Base class", output.contains("Class: Base"));
        assertTrue("Should have Derived class", output.contains("Class: Derived"));
        assertTrue("Should have supertypes section", output.contains("Supertypes:"));
        assertTrue("Should reference Base", output.contains("Base"));
        assertTrue("Should have open modifier", output.contains("open"));
    }
    
    @Test
    public void testSerializeCompanionObject() {
        String source = 
            "class MyClass {\n" +
            "    companion object {\n" +
            "        fun create() = MyClass()\n" +
            "    }\n" +
            "}";
        
        KtFile file = env.parseFile("Companion.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have companion object", output.contains("Companion Object"));
        assertTrue("Should have create function", output.contains("Function: create"));
    }
    
    // ==================== LOCATION TESTS ====================
    
    @Test
    public void testSerializeWithLocation() {
        String source = "fun test() = 1";
        KtFile file = env.parseFile("Location.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have location info", output.contains("line"));
        assertTrue("Should have column info", output.contains("col"));
        // Function should be at line 1
        assertTrue("Should have line 1", output.contains("line 1"));
    }
    
    // ==================== FILE OPERATIONS ====================
    
    @Test
    public void testWriteToFile() throws IOException {
        KtFile file = env.parseFile("Write.kt", "fun test() = 1");
        assertNotNull("File should be parsed", file);
        
        String content = PsiTreeSerializer.serialize(file);
        
        Path tempFile = Files.createTempFile("psi-test", ".psi");
        try {
            PsiTreeSerializer.writeToFile(content, tempFile);
            
            assertTrue("File should exist", Files.exists(tempFile));
            String read = Files.readString(tempFile);
            assertEquals("Content should match", content, read);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    public void testWriteToFileCreatesDirectories() throws IOException {
        KtFile file = env.parseFile("Test.kt", "fun test() = 1");
        String content = PsiTreeSerializer.serialize(file);
        
        Path tempDir = Files.createTempDirectory("psi-test-dir");
        Path nestedPath = tempDir.resolve("nested/deep/file.psi");
        
        try {
            PsiTreeSerializer.writeToFile(content, nestedPath);
            
            assertTrue("Nested file should exist", Files.exists(nestedPath));
            String read = Files.readString(nestedPath);
            assertEquals("Content should match", content, read);
        } finally {
            Files.deleteIfExists(nestedPath);
            Files.deleteIfExists(nestedPath.getParent());
            Files.deleteIfExists(nestedPath.getParent().getParent());
            Files.deleteIfExists(tempDir);
        }
    }
    
    // ==================== EDGE CASES ====================
    
    @Test
    public void testSerializeEmptyFile() {
        // Note: PsiEnvironment.parseFile() returns null for empty source by design
        // So we test with a minimal valid Kotlin file (just whitespace/comments)
        KtFile file = env.parseFile("Empty.kt", "// Empty file\n");
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertNotNull("Should handle empty file", output);
        assertTrue("Should have header", output.contains("PSI Tree:"));
        assertTrue("Should have KtFile", output.contains("KtFile:"));
    }
    
    @Test
    public void testSerializeComplexFile() {
        String source = 
            "package com.example\n" +
            "\n" +
            "import kotlin.collections.List\n" +
            "\n" +
            "data class Person(val name: String, var age: Int)\n" +
            "\n" +
            "object Logger {\n" +
            "    fun log(message: String) = println(message)\n" +
            "}\n" +
            "\n" +
            "fun main(args: Array<String>) {\n" +
            "    val person = Person(\"Alice\", 30)\n" +
            "    Logger.log(person.name)\n" +
            "}";
        
        KtFile file = env.parseFile("Complex.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        // Check package
        assertTrue("Should have package", output.contains("Package: com.example"));
        
        // Check imports
        assertTrue("Should have imports", output.contains("Imports:"));
        assertTrue("Should have List import", output.contains("kotlin.collections.List"));
        
        // Check data class
        assertTrue("Should have Person class", output.contains("Data Class: Person"));
        assertTrue("Should have name property", output.contains("Property: name"));
        assertTrue("Should have age property", output.contains("Property: age"));
        
        // Check object
        assertTrue("Should have Logger object", output.contains("Object: Logger"));
        assertTrue("Should have log function", output.contains("Function: log"));
        
        // Check main function
        assertTrue("Should have main function", output.contains("Function: main"));
        assertTrue("Should have args parameter", output.contains("args"));
        assertTrue("Should have Array type", output.contains("Array"));
    }
    
    // ==================== MODIFIER TESTS ====================
    
    @Test
    public void testSerializeFunctionModifiers() {
        String source = "suspend inline fun process() = Unit";
        KtFile file = env.parseFile("Modifiers.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have suspend modifier", output.contains("suspend"));
        assertTrue("Should have inline modifier", output.contains("inline"));
    }
    
    @Test
    public void testSerializePropertyModifiers() {
        String source = 
            "class Example {\n" +
            "    private lateinit var data: String\n" +
            "    const val MAX = 100\n" +
            "}";
        
        KtFile file = env.parseFile("Props.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have private modifier", output.contains("private"));
        assertTrue("Should have lateinit modifier", output.contains("lateinit"));
        assertTrue("Should have const modifier", output.contains("const"));
    }
    
    // ==================== OBJECT TESTS ====================
    
    @Test
    public void testSerializeSingletonObject() {
        String source = 
            "object Database {\n" +
            "    private val connection = \"jdbc:...\"\n" +
            "    fun connect() = connection\n" +
            "}";
        
        KtFile file = env.parseFile("Singleton.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have object", output.contains("Object: Database"));
        assertTrue("Should have property", output.contains("Property: connection"));
        assertTrue("Should have function", output.contains("Function: connect"));
    }
    
    // ==================== PROPERTY TESTS ====================
    
    @Test
    public void testSerializePropertyWithAccessors() {
        String source = 
            "class Example {\n" +
            "    var name: String = \"\"\n" +
            "        get() = field.uppercase()\n" +
            "        set(value) { field = value }\n" +
            "}";
        
        KtFile file = env.parseFile("Accessors.kt", source);
        assertNotNull("File should be parsed", file);
        
        String output = PsiTreeSerializer.serialize(file);
        
        assertTrue("Should have property", output.contains("Property: name"));
        assertTrue("Should have getter", output.contains("Getter: present"));
        assertTrue("Should have setter", output.contains("Setter: present"));
        assertTrue("Should have initializer", output.contains("Initializer: present"));
    }
    
    // ==================== ERROR HANDLING ====================
    
    @Test(expected = NullPointerException.class)
    public void testSerializeNullFile() {
        PsiTreeSerializer.serialize(null);
    }
    
    @Test(expected = NullPointerException.class)
    public void testWriteToFileNullContent() throws IOException {
        Path tempFile = Files.createTempFile("test", ".psi");
        try {
            PsiTreeSerializer.writeToFile(null, tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void testWriteToFileNullPath() throws IOException {
        PsiTreeSerializer.writeToFile("content", null);
    }
}