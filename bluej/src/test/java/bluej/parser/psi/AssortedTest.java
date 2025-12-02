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
package bluej.parser.psi;

import bluej.extensions2.SourceType;
import bluej.parser.InitConfig;
import bluej.parser.TestEntityResolver;
import bluej.parser.TestableDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ContainerNode;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static bluej.parser.KotlinEditorParserTest.printLinesWithPositions;
import static bluej.utility.ResourceFileReader.getResourceFile;
import static org.junit.Assert.*;

/**
 * Tests for Phase 3 Milestone 3.1 Task 1: Core visitClass() callback sequence.
 *
 * <p>Validates that {@link bluej.parser.psi.visitor.FileVisitor#visitClass(org.jetbrains.kotlin.psi.KtClass)}
 * invokes the complete callback sequence for simple class declarations:</p>
 * <ol>
 *   <li>{@code gotDeclBegin(token)} - Begin declaration</li>
 *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
 *   <li>{@code gotTypeDef(token, tdType)} - Type definition</li>
 *   <li>{@code gotTypeDefName(nameToken)} - Type name</li>
 *   <li>{@code beginTypeBody(token)} - Begin body</li>
 *   <li>{@code endTypeBody(token, true)} - End body</li>
 *   <li>{@code gotTypeDefEnd(token, true)} - End declaration</li>
 * </ol>
 *
 * <p><b>Scope:</b> Tests simple class declarations without modifiers, inheritance, or members.
 * Complex scenarios (modifiers, supertypes, nested classes) are deferred to later tasks.</p>
 *
 * @see bluej.parser.psi.visitor.FileVisitor
 * @see CallbackRecorder
 */
public class AssortedTest extends BasePsiTest {
    /**
     * Setup test environment before each test.
     */
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment must be initialized", env.isInitialized());
    }

    // ==================== CORE CALLBACK SEQUENCE ====================

    /**
     * Test 1: Simple empty class invokes correct callback sequence.
     *
     * <p>Validates that a minimal class declaration {@code class SimpleClass { }}
     * produces the expected 7-callback sequence without errors.</p>
     *
     * <p><b>Success Criteria:</b></p>
     * <ul>
     *   <li>All 7 callbacks invoked in correct order</li>
     *   <li>No extra or missing callbacks</li>
     *   <li>Callback pairing is balanced (validated by {@link PairingValidator})</li>
     * </ul>
     */
    @Test
    public void testSimpleEmptyClass_invokesCorrectSequence() throws PsiParseException {
        String kotlinCode = "class SimpleClass { }";

        CallbackRecorder recorder = parseAndVisit(kotlinCode);

        // Extract just the callback names for sequence validation
        List<String> sequence = recorder.getRecords().stream()
                .map(CallbackRecord::getCallbackName)
                .collect(Collectors.toList());

        // Validate exact sequence (7 callbacks)
        assertEquals("Should have exactly 7 callbacks", 7, sequence.size());
        assertEquals("Callback 1: gotDeclBegin", "gotDeclBegin", sequence.get(0));
        assertEquals("Callback 2: modifiersConsumed", "modifiersConsumed", sequence.get(1));
        assertEquals("Callback 3: gotTypeDef", "gotTypeDef", sequence.get(2));
        assertEquals("Callback 4: gotTypeDefName", "gotTypeDefName", sequence.get(3));
        assertEquals("Callback 5: beginTypeBody", "beginTypeBody", sequence.get(4));
        assertEquals("Callback 6: endTypeBody", "endTypeBody", sequence.get(5));
        assertEquals("Callback 7: gotTypeDefEnd", "gotTypeDefEnd", sequence.get(6));
    }

    @Test
    public void testDog() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
                .filter(f -> f.contains("ClassWithPropertyAndMethod"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Class with property and method file not found in test corpus"));

        String source = TestCorpus.loadTestFile(filePath);
        CallbackRecorder recorder = parseAndVisit(source);
    }

    @Test
    public void testBasicKtFile() throws IOException, PsiParseException {
        SourceInput input = getResourceFile(getClass(), "/bluej/parser/kotlin/kotlin_basic.kt");

        CallbackRecorder recorder = parseAndVisit(input);
    }

    @Test
    public void testBasicJavaFile() throws IOException, PsiParseException {
        InitConfig.init();

        SourceInput input = getResourceFile(getClass(), "/bluej/parser/BasicsDemo.java");

        EntityResolver resolver = new PackageResolver( new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader())), "");
        TestableDocument document = new TestableDocument(resolver);

        document.enableParser(true);
        document.insertString(0, input.content());

        ParsedCUNode node = document.getParser();
    }

    @Test
    public void testSampleProjectFile() throws IOException, PsiParseException {
        String filePath = TestCorpus.getModerateTests().stream()
                .filter(f -> f.contains("ProjectExample"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Project example file not found in test corpus"));

        String source = TestCorpus.loadTestFile(filePath);
        CallbackRecorder recorder = parseAndVisit(source);
    }

    @Test
    public void testKotlinNestedLoops() throws PsiParseException {
        String source = """
            class Dog {
                fun bark() {
                    while(true) {
                        break
                        while(true) {
                            println("")
                            break
                        }
                    }
                }
            }
            """;

        CallbackRecorder recorder = parseAndVisit(source);
    }

    @Test
    public void testJavaNestedLoops() throws IOException, PsiParseException {
        InitConfig.init();
        String source = """
            class Dog {
                void bark() {
                    while(true) {
                        break;
                        while(true) {
                            System.out.println("");
                            break;
                        }
                    }
                }
            }
            """;

        EntityResolver resolver = new PackageResolver( new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader())), "");
        TestableDocument document = new TestableDocument(resolver);

        document.enableParser(true);
        document.insertString(0, source);

        ParsedCUNode parsedNode = document.getParser();

        NodeTree.NodeAndPosition<ParsedNode> nap = parsedNode.findNodeAt(111, 0); // class
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // class body
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // method
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // method body (it's internal span)
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // outer while loop node
        assertTrue("Outer while node must be ContainerNode", nap.getNode() instanceof ContainerNode);
        // End brace - start brace + 1
        var expectedOuterLoopSize = 182 - 38 + 1;
        assertEquals("Outer while size is incorrect",expectedOuterLoopSize, nap.getSize());

        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // outer while loop body node (it's internal span)
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // inner while loop node
        assertTrue("Inner while node must be ContainerNode", nap.getNode() instanceof ContainerNode);
        // End brace - start brace + 1
        var innerLoopSize = 172 - 83 + 1;
        assertEquals("Inner while size is incorrect", innerLoopSize, nap.getSize());
    }

    @Test
    public void testJavaPartialParse() throws IOException, PsiParseException {
        InitConfig.init();
        String source = """
            // BasicsDemo.java
            // A simple Java 21 example covering fundamental features
            
            import java.util.*;
            import java.util.stream.*;
            
            public class BasicsDemo {
            
                public static void main(String[] args) {
                    new BasicsDemo().runDemo();
                }
            
                public void runDemo() {
                    System.out.println("=== Java Basics Demo ===\\n");
            
                    // 1. Variables and Constants
                    var name = "Alice";       // local variable type inference (Java 10+)
                    final int age = 25;       // final == val in Kotlin (immutable)
            
                    System.out.println(String.format("Hello, %s! You are %d years old.", name, age));
            
                    // 2. Conditionals
                    String category = (age < 18) ? "Minor"
                                     : (age <= 65) ? "Adult"
                                     : "Senior";
                    System.out.println("Category: " + category);
            
                    // 3. Methods and Defaults
                    System.out.println(greet("Java Learner"));
                    System.out.println(greet("Java Enthusiast", true));
            
                    // 4. Collections and Functional Transformations
                    List<Integer> numbers = List.of(1, 2, 3, 4, 5);
            
                    List<Integer> squares = numbers.stream()
                            .map(n -> n * n)
                            .toList();
            
                    List<Integer> evenSquares = squares.stream()
                            .filter(n -> n % 2 == 0)
                            .toList();
            
                    System.out.println("\\nNumbers: " + numbers);
                    System.out.println("Squares: " + squares);
                    System.out.println("Even Squares: " + evenSquares);
            
                    // 5. Loops
                    System.out.println("\\nCounting with a loop:");
                    for (int n : numbers) {
                        System.out.print(n + " ");
                    }
                    System.out.println("\\nDone!\\n");
            
                    // 6. Classes and Records
                    Person person = new Person("Bob", 32);
                    System.out.println("Created a person: " + person);
            
                    person.haveBirthday();
                    System.out.println("After birthday: " + person);
            
                    // 7. Null Safety (using Optional)
                    Optional<String> maybeName = (person.age() > 30)
                            ? Optional.empty()
                            : Optional.of(person.name());
                    System.out.println("\\nMaybe name length: " +
                            maybeName.map(String::length).orElse(-1));
            
                    // 8. Using a higher-order-like function (with a lambda)
                    int total = applyToList(numbers, list -> list.stream().mapToInt(Integer::intValue).sum());
                    System.out.println("\\nSum of numbers (using lambda): " + total);
            
                    // 9. Pattern Matching (Java 21)
                    Object someendForInitthing = person;
                    String description = switch (something) {
                        case Person(var n, var a) -> String.format("It's a person named %s, age %d", n, a);
                        case String s -> "It's a string: " + s;
                        case null -> "It's null!";
                        default -> "Something else: " + something.getClass().getSimpleName();
                    };
                    System.out.println("\\nPattern matching result: " + description);
            
                    System.out.println("\\n=== End of Demo ===");
                
            
                // Method with optional parameter (simulated with overloads)
                public String greet(String person) {
                    return greet(person, false);
                }
            
                public String greet(String person, boolean excited) {
                    var message = "Hello, " + person;
                    return excited ? message + "!!!" : message + ".";
                }
            
                // Higher-order-like method that takes a function
                public <T, R> R applyToList(List<T> list, java.util.function.Function<List<T>, R> operation) {
                    return operation.apply(list);
                }
            
                // Record — immutable data carrier (like Kotlin data class)
                record Person(String name, int age) {
                    void haveBirthday() {
                        System.out.println("🎉 Happy birthday, " + name + "!");
                        // Records are immutable, but you can return a new instance
                        // If you want to mutate, use a normal class instead
                    }
                }
            }
            """;


        EntityResolver resolver = new PackageResolver( new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader())), "");
        TestableDocument document = new TestableDocument(resolver);

        document.enableParser(true);
        document.insertString(0, source);

        ParsedCUNode parsedNode = document.getParser();
    }
}
