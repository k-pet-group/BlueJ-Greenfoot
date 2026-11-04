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
package bluej.parser.kotlin.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.compiler.KotlinTestClasspath;

import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link KotlinAnalysisService}: what is analysed, and request queueing.
 */
public class KotlinAnalysisServiceTest
{
    private static final int TIMEOUT_SECONDS = 60;

    private Path projectDir;
    private KotlinAnalysisService service;

    @Before
    public void setUp() throws IOException
    {
        projectDir = Files.createTempDirectory("kotlin-analysis-test");
        service = new KotlinAnalysisService();
    }

    @After
    public void tearDown() throws IOException
    {
        service.dispose();
        try (Stream<Path> paths = Files.walk(projectDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void testAnalysesProjectSourcesPreferringEditorText() throws Exception
    {
        File rock = write("Rock.kt", "class Rock { fun rollOld() = 1 }");
        write("JavaHelper.java", "public class JavaHelper { public int helper() { return 1; } }");
        Set<String> resolved = resolve("fun main() { Rock().rollOld(); JavaHelper().helper(); listOf(1).sum() }", Map.of());
        assertTrue("Other project Kotlin file should resolve: " + resolved, resolved.contains("rollOld"));
        assertTrue("Uncompiled Java source should resolve: " + resolved, resolved.contains("helper"));
        assertTrue("Kotlin stdlib on the classpath should resolve: " + resolved, resolved.contains("sum"));

        // An open editor's text is analysed instead of the saved file:
        resolved = resolve("fun main() { Rock().rollNew(); Rock().rollOld() }",
            Map.of(rock, "class Rock { fun rollNew() = 1 }"));
        assertTrue("Unsaved editor text should be analysed: " + resolved, resolved.contains("rollNew"));
        assertFalse("Saved text of an open editor should be ignored: " + resolved, resolved.contains("rollOld"));

        // The target itself is analysed once, from its editor text, never also from its saved copy
        // (otherwise the two declarations of Dup would clash):
        write("Main.kt", "class Dup\nfun saved() {}");
        assertTrue("Target declaration should resolve unambiguously",
            resolve("class Dup\nfun main() { Dup() }", Map.of()).contains("Dup"));
    }

    @Test
    public void testQueueKeepsNewestWaitingRequestSurvivesFailuresAndStopsOnDispose() throws Exception
    {
        // Only the newest waiting request is kept; a started request always runs to completion:
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> running = service.submit(request("fun main() {}", Map.of()), analysis -> {
            started.countDown();
            await(release);
            return "running";
        });
        assertTrue("First request should start", started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        CompletableFuture<String> superseded = service.submit(request("fun main() {}", Map.of()), analysis -> "superseded");
        CompletableFuture<String> newest = service.submit(request("fun main() {}", Map.of()), analysis -> "newest");
        assertTrue("A waiting request should be cancelled by a newer one", superseded.isCancelled());
        release.countDown();
        assertEquals("A started request runs to completion", "running", get(running));
        assertEquals("newest", get(newest));

        // A failing task is reported to its requester and the queue carries on:
        CompletableFuture<Object> failed = service.submit(request("fun main() {}", Map.of()), analysis -> {
            throw new IllegalStateException("task failure");
        });
        try {
            get(failed);
            fail("Task failure should be reported");
        }
        catch (ExecutionException e) {
            assertEquals("task failure", e.getCause().getMessage());
        }
        assertEquals("ok", get(service.submit(request("fun main() {}", Map.of()), analysis -> "ok")));

        // Disposal cancels the waiting request, lets the running one finish and rejects new ones:
        CountDownLatch started2 = new CountDownLatch(1);
        CountDownLatch release2 = new CountDownLatch(1);
        CompletableFuture<String> running2 = service.submit(request("fun main() {}", Map.of()), analysis -> {
            started2.countDown();
            await(release2);
            return "running";
        });
        assertTrue("Request should start", started2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        CompletableFuture<String> waiting = service.submit(request("fun main() {}", Map.of()), analysis -> "waiting");
        service.dispose();
        assertTrue("Disposal should cancel the waiting request", waiting.isCancelled());
        release2.countDown();
        assertEquals("Disposal should let the running request finish", "running", get(running2));
        CompletableFuture<String> rejected = service.submit(request("fun main() {}", Map.of()), analysis -> "late");
        try {
            get(rejected);
            fail("Requests after disposal should be rejected");
        }
        catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    // Analyse Main.kt with the given text and return the names in it that resolved to a declaration.
    private Set<String> resolve(String targetText, Map<File, String> openEditorTexts) throws Exception
    {
        return get(service.submit(request(targetText, openEditorTexts), analysis -> {
            BindingContext context = analysis.bindingContext();
            return PsiTreeUtil.collectElementsOfType(analysis.targetFile(), KtSimpleNameExpression.class).stream()
                .filter(name -> context.get(BindingContext.REFERENCE_TARGET, name) != null)
                .map(KtSimpleNameExpression::getReferencedName)
                .collect(Collectors.toSet());
        }));
    }

    private KotlinAnalysisRequest request(String targetText, Map<File, String> openEditorTexts)
    {
        return new KotlinAnalysisRequest(projectDir.toFile(), classPath(), StandardCharsets.UTF_8,
            projectDir.resolve("Main.kt").toFile(), targetText, openEditorTexts);
    }

    // Like BlueJ's project class loader: the student libraries plus the project directory itself.
    private List<File> classPath()
    {
        List<File> classPath = new ArrayList<>(KotlinTestClasspath.getKotlinStdlib());
        classPath.add(projectDir.toFile());
        return classPath;
    }

    private File write(String name, String text) throws IOException
    {
        Path file = projectDir.resolve(name);
        Files.writeString(file, text);
        return file.toFile();
    }

    private static <T> T get(CompletableFuture<T> future) throws Exception
    {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch)
    {
        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the test to release the task");
            }
        }
        catch (InterruptedException e) {
            throw new CancellationException("Interrupted");
        }
    }
}
