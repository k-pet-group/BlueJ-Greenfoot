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
package bluej.editor.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import bluej.Config;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.Access;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.InitConfig;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate;
import bluej.parser.kotlin.analysis.KotlinCompletionResult;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for how {@link KotlinCompletionProvider} shows the outcome of a completion computation.
 */
public class KotlinCompletionProviderTest extends FXTest
{
    private static final long WAIT_MILLIS = 10_000;

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        InitConfig.init();
    }

    @Test
    public void testResultShownWithGreenfootShortcutsFirstThenByName() throws Exception
    {
        RecordingRequest request = deliver(new KotlinCompletionProvider(), CompletableFuture.completedFuture(
            new KotlinCompletionResult(3, List.of(field("", "zeta"), field("Greenfoot.", "delay"), field("", "alpha")))));
        waitFor(() -> request.result != null);
        assertEquals(List.of("Greenfoot.delay", "alpha", "zeta"),
            request.result.candidates().stream().map(AssistContent::getName).toList());
        assertEquals(3, request.result.replaceStart());
        assertTrue("No message alongside a result: " + request.messages, request.messages.isEmpty());
    }

    @Test
    public void testNothingToCompleteShowsMessage() throws Exception
    {
        RecordingRequest request = deliver(new KotlinCompletionProvider(), CompletableFuture.completedFuture(null));
        waitFor(() -> !request.messages.isEmpty());
        assertEquals(List.of(Config.getString("editor.info.noCompletions")), request.messages);
    }

    @Test
    public void testFailureShowsMessage() throws Exception
    {
        RecordingRequest request = deliver(new KotlinCompletionProvider(),
            CompletableFuture.failedFuture(new IllegalStateException("expected test failure")));
        waitFor(() -> !request.messages.isEmpty());
        assertEquals(List.of(Config.getString("editor.info.completionFailed")), request.messages);
    }

    @Test
    public void testSupersededRequestShowsNothing() throws Exception
    {
        CompletableFuture<KotlinCompletionResult> future = new CompletableFuture<>();
        RecordingRequest request = deliver(new KotlinCompletionProvider(), future);
        future.cancel(false);
        Thread.sleep(500);
        assertTrue(fx(() -> request.result == null && request.messages.isEmpty()));
    }

    @Test
    public void testSlowResultShowsLoadingMessageFirst() throws Exception
    {
        CompletableFuture<KotlinCompletionResult> future = new CompletableFuture<>();
        RecordingRequest request = deliver(new KotlinCompletionProvider(Duration.millis(50), Duration.seconds(30)), future);
        waitFor(() -> !request.messages.isEmpty());
        assertEquals(List.of(Config.getString("editor.info.completionLoading")), fx(() -> List.copyOf(request.messages)));
        future.complete(new KotlinCompletionResult(0, List.of(field("", "speed"))));
        waitFor(() -> request.result != null);
    }

    @Test
    public void testTimeoutShowsMessageAndDropsLateResult() throws Exception
    {
        CompletableFuture<KotlinCompletionResult> future = new CompletableFuture<>();
        RecordingRequest request = deliver(new KotlinCompletionProvider(Duration.seconds(30), Duration.millis(50)), future);
        waitFor(() -> !request.messages.isEmpty());
        assertEquals(List.of(Config.getString("editor.info.completionTimeout")), fx(() -> List.copyOf(request.messages)));
        future.complete(new KotlinCompletionResult(0, List.of(field("", "speed"))));
        Thread.sleep(500);
        assertTrue("A result after the timeout is discarded", fx(() -> request.result == null));
    }

    private RecordingRequest deliver(KotlinCompletionProvider provider, CompletableFuture<KotlinCompletionResult> future)
    {
        RecordingRequest request = new RecordingRequest();
        // These candidates are not Java members, so no Javadoc resolver or class loading is needed.
        fx_(() -> provider.deliver(future, request, candidates -> KotlinAssistContent.create(candidates, null, name -> null)));
        return request;
    }

    private void waitFor(BooleanSupplier condition) throws InterruptedException
    {
        long end = System.currentTimeMillis() + WAIT_MILLIS;
        while (!fx(condition::getAsBoolean)) {
            if (System.currentTimeMillis() > end) {
                throw new AssertionError("Timed out waiting for the completion outcome");
            }
            Thread.sleep(20);
        }
    }

    private static KotlinCompletionCandidate field(String prefix, String name)
    {
        return new KotlinCompletionCandidate(prefix, name, CompletionKind.FIELD, "Int", null, null, Access.PUBLIC,
            null, null);
    }

    // Records what the provider shows; always current.
    private static class RecordingRequest implements CompletionRequest
    {
        private CompletionResult result;
        private final List<String> messages = new ArrayList<>();

        @Override
        public int getCaretPosition()
        {
            return 0;
        }

        @Override
        public boolean isCurrent()
        {
            return true;
        }

        @Override
        public void showResult(CompletionResult result)
        {
            this.result = result;
        }

        @Override
        public void showMessage(String message)
        {
            messages.add(message);
        }
    }
}
