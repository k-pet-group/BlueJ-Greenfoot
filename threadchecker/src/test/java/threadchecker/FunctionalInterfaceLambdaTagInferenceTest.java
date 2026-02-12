/*
 This file is part of the BlueJ program.
 Copyright (C) 2024,2025,2026 Michael Kolling and John Rosenberg

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
package threadchecker;

import com.sun.source.util.JavacTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the threadchecker's lambda thread-tag inference.
 *
 * <p>The threadchecker infers a lambda's thread tag from the
 * {@code @OnThread} annotation on the functional interface's abstract
 * method.  These tests verify that this inference works correctly
 * across various scenarios: simple interfaces, generics, throwing
 * interfaces, overloaded methods, nested lambdas, and chained calls.</p>
 *
 * <p>Each test compiles a Java source snippet with the threadchecker
 * plugin enabled and verifies that the expected compilation errors (or
 * absence thereof) occur.</p>
 */
class FunctionalInterfaceLambdaTagInferenceTest
{
    @TempDir
    Path tempDir;

    // ---------------------------------------------------------------
    //  Type inference from annotated functional interfaces
    // ---------------------------------------------------------------

    /**
     * A lambda passed to a method taking an annotated functional interface
     * should inherit the tag from the interface's abstract method.
     */
    @Test
    void annotatedInterface_lambdaCallsMatchingThread_shouldSucceed()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void runOnFX(FXRunnable r) {}

                @OnThread(Tag.Any)
                public void test() {
                    runOnFX(() -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda should inherit FXPlatform from annotated interface, "
            + "but got errors:\n" + result.getErrorMessages());
    }

    /**
     * A lambda inheriting FXPlatform from the interface must NOT be
     * allowed to call Worker methods.
     */
    @Test
    void annotatedInterface_lambdaCallsWrongThread_shouldFail()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.Worker)
                public static void workerMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void runOnFX(FXRunnable r) {}

                @OnThread(Tag.Any)
                public void test() {
                    runOnFX(() -> workerMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertFalse(result.success,
            "Lambda inheriting FXPlatform should not call Worker methods");
        assertTrue(result.hasErrorContaining("workerMethod"),
            "Error should mention workerMethod, actual errors:\n"
            + result.getErrorMessages());
    }

    /**
     * The interface annotation should override the surrounding method's
     * thread context.
     */
    @Test
    void annotatedInterface_overridesSurroundingMethodContext()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void runOnFX(FXRunnable r) {}

                @OnThread(Tag.Worker)
                public void test() {
                    runOnFX(() -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Interface annotation should override surrounding method's "
            + "Worker tag, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Interface annotation should work when the lambda is not the first
     * parameter.
     */
    @Test
    void annotatedInterface_atSecondParameter_shouldSucceed()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void schedule(int delayMs,
                        FXRunnable action) {}

                @OnThread(Tag.Any)
                public void test() {
                    schedule(100, () -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda at second parameter position should inherit tag "
            + "from interface, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Interface annotation should work when the lambda is passed to a
     * constructor.
     */
    @Test
    void annotatedInterface_onConstructor_shouldSucceed()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static class TaskRunner {
                    public TaskRunner(FXRunnable task) {}
                }

                @OnThread(Tag.Any)
                public void test() {
                    new TaskRunner(() -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda passed to constructor should inherit tag from "
            + "interface, but got errors:\n" + result.getErrorMessages());
    }

    /**
     * Generic annotated interface: type inference should still resolve
     * the tag when generics are involved.
     */
    @Test
    void annotatedGenericInterface_lambdaInheritsTag()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXSupplier<T> {
                    @OnThread(Tag.FXPlatform) T get();
                }

                public static <T> T compute(FXSupplier<T> supplier) {
                    return null;
                }

                @OnThread(Tag.Any)
                public void test() {
                    compute(() -> {
                        fxPlatformMethod();
                        return "result";
                    });
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda should inherit FXPlatform from generic annotated "
            + "interface, but got errors:\n" + result.getErrorMessages());
    }

    /**
     * Without any annotation on the interface or the surrounding context,
     * the lambda inherits from the enclosing method.
     */
    @Test
    void unannotatedInterface_inheritsFromEnclosingMethod()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                public static void acceptRunnable(Runnable r) {}

                @OnThread(Tag.FXPlatform)
                public void test() {
                    acceptRunnable(() -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda with unannotated interface should inherit from "
            + "enclosing method, but got errors:\n"
            + result.getErrorMessages());
    }

    // ---------------------------------------------------------------
    //  Throwing generic interface (real-world pattern)
    // ---------------------------------------------------------------

    /**
     * A generic throwing functional interface used with an expression
     * lambda.  The {@code throws Exception} clause should not prevent
     * type inference from resolving the tag.
     */
    @Test
    void throwingGenericInterface_expressionLambda_typeInference()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static String fxPlatformMethod() { return ""; }

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @OnThread(Tag.Any)
                public static <T> T runPlatformUnchecked(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(value = Tag.Any, ignoreParent = true)
                public String test() {
                    return runPlatformUnchecked(
                        () -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda should inherit FXPlatform from throwing generic "
            + "interface, but got errors:\n" + result.getErrorMessages());
    }

    /**
     * Overloaded methods where one takes a generic throwing interface
     * and the other takes a non-generic one.  The erasure fallback in
     * overload resolution must correctly keep the generic candidate.
     */
    @Test
    void throwingGenericInterface_withOverload_typeInference()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static String fxPlatformMethod() { return ""; }

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                @OnThread(Tag.Any)
                public static <T> T runPlatformUnchecked(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(Tag.Any)
                @SuppressWarnings("threadchecker")
                public static void runPlatformUnchecked(FXRunnable r) {}

                @OnThread(value = Tag.Any, ignoreParent = true)
                public String test() {
                    return runPlatformUnchecked(
                        () -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda should inherit FXPlatform from throwing generic "
            + "interface even with overloaded method, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Chained method calls inside an expression lambda, matching the
     * real-world pattern where the original failure occurred.
     */
    @Test
    void throwingGenericInterface_chainedCalls_typeInference()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public String getName() { return ""; }

                @OnThread(Tag.FXPlatform)
                public TestCase getChild(String name) { return this; }

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @OnThread(Tag.Any)
                public static <T> T runPlatformUnchecked(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(value = Tag.Any, ignoreParent = true)
                public TestCase test() {
                    TestCase self = this;
                    return runPlatformUnchecked(
                        () -> self.getChild(self.getName()));
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda with chained FXPlatform calls should inherit "
            + "FXPlatform from throwing generic interface, but got "
            + "errors:\n" + result.getErrorMessages());
    }

    // ---------------------------------------------------------------
    //  Nested lambda scenarios
    // ---------------------------------------------------------------

    /**
     * When a lambda is passed to an unannotated method inside a tagged
     * lambda, the inner lambda should inherit the outer lambda's tag via
     * the backwards walk through {@code lambdaScopeStack}.
     */
    @Test
    void nestedLambda_innerUntagged_inheritsFromOuterLambda()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void runOnFX(FXRunnable r) {}
                public static void doForEach(Runnable action) {}

                @OnThread(Tag.Any)
                public void test() {
                    runOnFX(() -> {
                        doForEach(() -> {
                            fxPlatformMethod();
                        });
                    });
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Inner untagged lambda should inherit FXPlatform from outer "
            + "lambda, but got errors:\n" + result.getErrorMessages());
    }

    /**
     * Same nesting scenario, but the inner lambda tries to call a method
     * tagged for a different thread.
     */
    @Test
    void nestedLambda_innerUntagged_cannotCallWrongThread()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.Worker)
                public static void workerMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void runOnFX(FXRunnable r) {}
                public static void doForEach(Runnable action) {}

                @OnThread(Tag.Any)
                public void test() {
                    runOnFX(() -> {
                        doForEach(() -> {
                            workerMethod();
                        });
                    });
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertFalse(result.success,
            "Inner untagged lambda inheriting FXPlatform should not "
            + "call Worker methods");
        assertTrue(result.hasErrorContaining("workerMethod"),
            "Error should mention workerMethod, actual errors:\n"
            + result.getErrorMessages());
    }

    /**
     * An inner lambda whose annotated interface carries a different tag
     * should override the outer lambda's tag.
     */
    @Test
    void nestedLambda_innerAnnotatedWorker_overridesOuterFXPlatform()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.Worker)
                public static void workerMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                @FunctionalInterface
                public interface WorkerRunnable {
                    @OnThread(Tag.Worker) void run();
                }

                public static void runOnFX(FXRunnable r) {}
                public static void runInBackground(WorkerRunnable r) {}

                @OnThread(Tag.Any)
                public void test() {
                    runOnFX(() -> {
                        runInBackground(() -> {
                            workerMethod();
                        });
                    });
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Inner lambda with Worker interface should call Worker "
            + "methods, but got errors:\n" + result.getErrorMessages());
    }

    /**
     * An inner lambda with Worker interface cannot call FXPlatform
     * methods, even when the outer lambda is FXPlatform.
     */
    @Test
    void nestedLambda_innerAnnotatedWorker_cannotCallFXPlatform()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                @FunctionalInterface
                public interface WorkerRunnable {
                    @OnThread(Tag.Worker) void run();
                }

                public static void runOnFX(FXRunnable r) {}
                public static void runInBackground(WorkerRunnable r) {}

                @OnThread(Tag.Any)
                public void test() {
                    runOnFX(() -> {
                        runInBackground(() -> {
                            fxPlatformMethod();
                        });
                    });
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertFalse(result.success,
            "Inner lambda with Worker interface should not call "
            + "FXPlatform methods");
        assertTrue(result.hasErrorContaining("fxPlatformMethod"),
            "Error should mention fxPlatformMethod, actual errors:\n"
            + result.getErrorMessages());
    }

    // ---------------------------------------------------------------
    //  Known gaps and promise verification
    // ---------------------------------------------------------------

    /**
     * Method references ({@code this::method}) are not handled by the
     * lambda type inference logic.  This test documents the gap.
     */
    @Test
    void methodReference_notCheckedByTypeInference()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.Worker)
                public void workerMethod() {}

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                public static void runOnFX(FXRunnable r) {}

                @OnThread(Tag.Any)
                public void test() {
                    // Semantically equivalent to:
                    //   runOnFX(() -> workerMethod())
                    // which WOULD be caught.  But the method reference
                    // bypasses visitLambdaExpression entirely.
                    runOnFX(this::workerMethod);
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        // Documents current behaviour: method references are NOT checked.
        assertTrue(result.success,
            "Method references are not currently checked against "
            + "interface annotations (known gap), but compilation "
            + "unexpectedly failed:\n" + result.getErrorMessages());
    }

    /**
     * When the functional interface's abstract method IS annotated,
     * calling it from the wrong thread inside the method body IS caught.
     * This provides callee-side promise verification.
     */
    @Test
    void promiseVerified_annotatedFunctionalInterfaceMethod()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @FunctionalInterface
                public interface FXTask {
                    @OnThread(Tag.FXPlatform) void execute();
                }

                @OnThread(Tag.Worker)
                public static void brokenPromise(FXTask r) {
                    r.execute();
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertFalse(result.success,
            "Calling annotated interface method from wrong thread "
            + "should be caught");
        assertTrue(result.hasErrorContaining("execute"),
            "Error should mention 'execute', actual errors:\n"
            + result.getErrorMessages());
    }

    // ---------------------------------------------------------------
    //  Overload resolution: erasure fallback for type variables
    // ---------------------------------------------------------------

    /**
     * Chained call on the generic return type after erasure-fallback
     * overload resolution: the resolved type must be correct.
     */
    @Test
    void overloadErasureFallback_chainedCall_retainsType_shouldSucceed()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                public static class Result {
                    @OnThread(Tag.Worker)
                    public void processOnWorker() {}
                }

                @OnThread(Tag.FXPlatform)
                public static Result getResult() { return null; }

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                @OnThread(Tag.Any)
                public static <T> T runPlatformUnchecked(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(Tag.Any)
                @SuppressWarnings("threadchecker")
                public static void runPlatformUnchecked(FXRunnable r) {}

                @OnThread(Tag.Worker)
                public void test() {
                    runPlatformUnchecked(() -> getResult())
                        .processOnWorker();
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Chained Worker method on generic return type should work "
            + "from Worker context, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Negative counterpart: chained method on the generic return type
     * must still be checked against the calling context's thread tag.
     */
    @Test
    void overloadErasureFallback_chainedCall_wrongThread_shouldFail()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                public static class Result {
                    @OnThread(Tag.FXPlatform)
                    public void processOnFX() {}
                }

                @OnThread(Tag.FXPlatform)
                public static Result getResult() { return null; }

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                @OnThread(Tag.Any)
                public static <T> T runPlatformUnchecked(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(Tag.Any)
                @SuppressWarnings("threadchecker")
                public static void runPlatformUnchecked(FXRunnable r) {}

                @OnThread(Tag.Worker)
                public void test() {
                    runPlatformUnchecked(() -> getResult())
                        .processOnFX();
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertFalse(result.success,
            "Chained FXPlatform method called from Worker context "
            + "should fail");
        assertTrue(result.hasErrorContaining("processOnFX"),
            "Error should mention processOnFX, actual errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Concrete generic types (no type variables) should NOT trigger
     * the erasure fallback.
     */
    @Test
    void erasureFallback_concreteGeneric_noFalseAmbiguity()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.Worker)
                public static void process(Comparable<Integer> x) {}

                @OnThread(Tag.FXPlatform)
                public static void process(java.io.Serializable x) {}

                @OnThread(Tag.FXPlatform)
                public void test() {
                    process("hello");
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Concrete generic overload should be correctly filtered "
            + "without false ambiguity, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Same-tag overloads that both survive erasure fallback should be
     * accepted without error.
     */
    @Test
    void erasureFallback_sameTag_succeeds()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void process(Comparable<Integer> x) {}

                @OnThread(Tag.FXPlatform)
                public static void process(java.io.Serializable x) {}

                @OnThread(Tag.FXPlatform)
                public void test() {
                    process("hello");
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Same-tag overloads should be accepted, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Unrelated erased types should be correctly filtered out.
     */
    @Test
    void erasureFallback_unrelatedTypes_correctlyFiltered()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @OnThread(Tag.Worker)
                public static void compute(int value) {}

                @OnThread(Tag.Any)
                public static <T> T compute(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(Tag.Any)
                public String test() {
                    return compute(() -> {
                        fxPlatformMethod();
                        return "result";
                    });
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Lambda should match FXSupplierThrowing overload, not the "
            + "int overload, but got errors:\n"
            + result.getErrorMessages());
    }

    // ---------------------------------------------------------------
    //  Erasure fallback with generic bounds
    // ---------------------------------------------------------------

    /**
     * Bounded type variable: {@code <T extends Number>}.
     */
    @Test
    void erasureFallback_boundedTypeVariable_correctlyResolved()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static Integer fxPlatformMethod() { return 0; }

                @FunctionalInterface
                public interface FXSupplierThrowing<T> {
                    @OnThread(Tag.FXPlatform) T get() throws Exception;
                }

                @FunctionalInterface
                public interface FXRunnable {
                    @OnThread(Tag.FXPlatform) void run();
                }

                @OnThread(Tag.Any)
                public static <T extends Number> T runPlatformUnchecked(
                        FXSupplierThrowing<T> task) {
                    return null;
                }

                @OnThread(Tag.Any)
                @SuppressWarnings("threadchecker")
                public static void runPlatformUnchecked(FXRunnable r) {}

                @OnThread(value = Tag.Any, ignoreParent = true)
                public Integer test() {
                    return runPlatformUnchecked(
                        () -> fxPlatformMethod());
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Bounded type variable should still trigger erasure "
            + "fallback, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Wildcard bound containing a type variable:
     * {@code List<? extends T>}.
     */
    @Test
    void erasureFallback_wildcardWithTypeVariable_correctlyResolved()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;
            import java.util.List;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @OnThread(Tag.FXPlatform)
                public static <T> void processList(
                        List<? extends T> items) {}

                @OnThread(Tag.Worker)
                public static void processList(String item) {}

                @OnThread(Tag.FXPlatform)
                public void test() {
                    List<String> names = null;
                    processList(names);
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Wildcard with type variable should trigger erasure "
            + "fallback, but got errors:\n"
            + result.getErrorMessages());
    }

    /**
     * Concrete wildcard (no type variables): {@code List<? extends Number>}.
     */
    @Test
    void erasureFallback_concreteWildcard_usesExactCheck()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;
            import java.util.List;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void processNumbers(
                        List<? extends Number> items) {}

                @OnThread(Tag.Worker)
                public static void processNumbers(String item) {}

                @OnThread(Tag.FXPlatform)
                public void test() {
                    List<Integer> ints = null;
                    processNumbers(ints);
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Concrete wildcard should work via exact check, but got "
            + "errors:\n" + result.getErrorMessages());
    }

    /**
     * Partially concrete generic: {@code Map<String, V>}.
     */
    @Test
    void erasureFallback_partiallyConcreteGeneric_correctlyResolved()
    {
        String source = """
            package testcases;
            import threadchecker.OnThread;
            import threadchecker.Tag;
            import java.util.Map;

            public class TestCase {
                @OnThread(Tag.FXPlatform)
                public static void fxPlatformMethod() {}

                @OnThread(Tag.FXPlatform)
                public static <V> void processMap(Map<String, V> map) {}

                @OnThread(Tag.Worker)
                public static void processMap(String item) {}

                @OnThread(Tag.FXPlatform)
                public void test() {
                    Map<String, Integer> data = null;
                    processMap(data);
                }
            }
            """;

        CompilationResult result = compileWithThreadChecker(source);
        assertTrue(result.success,
            "Partially concrete generic with type variable should "
            + "trigger erasure fallback, but got errors:\n"
            + result.getErrorMessages());
    }

    // ---------------------------------------------------------------
    //  Test infrastructure
    // ---------------------------------------------------------------

    /**
     * Compiles the given source code with the threadchecker plugin
     * enabled and returns the compilation result including diagnostics.
     */
    private CompilationResult compileWithThreadChecker(String sourceCode)
    {
        return compileWithThreadChecker(sourceCode, "TestCase");
    }

    private CompilationResult compileWithThreadChecker(
            String sourceCode, String className)
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler,
            "System Java compiler not available -- a full JDK is required");

        DiagnosticCollector<JavaFileObject> diagnostics =
            new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, null))
        {
            JavaFileObject source = new SimpleJavaFileObject(
                URI.create("string:///testcases/" + className + ".java"),
                JavaFileObject.Kind.SOURCE)
            {
                @Override
                public CharSequence getCharContent(
                        boolean ignoreEncodingErrors)
                {
                    return sourceCode;
                }
            };

            String classpath = System.getProperty("java.class.path");
            List<String> options = Arrays.asList(
                "-classpath", classpath,
                "-d", tempDir.toString()
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics,
                options, null, Collections.singletonList(source));

            JavacTask javacTask = (JavacTask) task;
            new TCPlugin().init(javacTask);

            boolean success = task.call();
            return new CompilationResult(success, diagnostics.getDiagnostics());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static class CompilationResult
    {
        final boolean success;
        final List<Diagnostic<? extends JavaFileObject>> diagnostics;

        CompilationResult(boolean success,
                List<Diagnostic<? extends JavaFileObject>> diagnostics)
        {
            this.success = success;
            this.diagnostics = diagnostics;
        }

        List<Diagnostic<? extends JavaFileObject>> getErrors()
        {
            return diagnostics.stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .collect(Collectors.toList());
        }

        boolean hasErrorContaining(String text)
        {
            return diagnostics.stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .anyMatch(d -> d.getMessage(null).contains(text));
        }

        String getErrorMessages()
        {
            return diagnostics.stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .map(d -> d.getMessage(null))
                .collect(Collectors.joining("\n"));
        }
    }
}
