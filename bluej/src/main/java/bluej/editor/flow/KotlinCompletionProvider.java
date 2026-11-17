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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import bluej.Config;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.parser.AssistContent;
import bluej.parser.kotlin.analysis.KotlinAnalysisRequest;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate;
import bluej.parser.kotlin.analysis.KotlinCompletionEngine;
import bluej.parser.kotlin.analysis.KotlinCompletionResult;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;

import javafx.application.Platform;
import javafx.util.Duration;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Kotlin code completion: analyses the project in the background and shows the result when ready,
 * with a message if that takes a while, fails, times out or finds nothing to complete.
 */
@OnThread(Tag.FXPlatform)
public class KotlinCompletionProvider implements CompletionProvider
{
    // The session's first request starts the Kotlin compiler (around a second); say so if it takes a while.
    private static final Duration LOADING_MESSAGE_DELAY = Duration.millis(300);
    // Stop waiting after this; the analysis finishes in the background and its result is discarded.
    private static final Duration TIMEOUT = Duration.seconds(5);

    private final Duration loadingMessageDelay;
    private final Duration timeout;

    /**
     * Create a provider with the standard loading-message delay and timeout.
     */
    public KotlinCompletionProvider()
    {
        this(LOADING_MESSAGE_DELAY, TIMEOUT);
    }

    // For tests: shorter delays.
    KotlinCompletionProvider(Duration loadingMessageDelay, Duration timeout)
    {
        this.loadingMessageDelay = loadingMessageDelay;
        this.timeout = timeout;
    }

    @Override
    public void requestCompletions(FlowEditor editor, CompletionRequest request)
    {
        Project project = editor.getProject();
        File file = editor.getSourceFile();
        if (project == null || file == null) {
            return;
        }
        KotlinAnalysisRequest analysisRequest = KotlinAnalysisRequest.forProject(project, file, editor);
        CompletableFuture<KotlinCompletionResult> future = KotlinCompletionEngine.complete(
            project.getKotlinAnalysisService(), analysisRequest, request.getCaretPosition());
        deliver(future, request, candidates -> KotlinAssistContent.create(candidates, project.getJavadocResolver(),
            project::loadClass));
    }

    /**
     * Show the outcome of a completion computation through the request: the result, or a message if
     * it takes a while, fails, times out or finds nothing to complete. Exactly one outcome is shown.
     *
     * @param future the computation
     * @param request where to show the outcome
     * @param items  turns candidates into editor items (performing any Javadoc lookups)
     */
    void deliver(CompletableFuture<KotlinCompletionResult> future, CompletionRequest request,
                 Function<List<KotlinCompletionCandidate>, List<KotlinAssistContent>> items)
    {
        Outcome outcome = new Outcome();
        FXPlatformRunnable cancelLoadingMessage = JavaFXUtil.runAfter(loadingMessageDelay, () -> {
            if (!outcome.isSettled()) {
                // Stays up until the result, the timeout or a failure replaces it.
                request.showProgress(Config.getString("editor.info.completionLoading"));
            }
        });
        FXPlatformRunnable cancelTimeout = JavaFXUtil.runAfter(timeout, () -> {
            if (outcome.settle()) {
                request.showMessage(Config.getString("editor.info.completionTimeout"));
            }
        });
        future.whenComplete(new BiConsumer<>()
        {
            @Override
            @OnThread(Tag.Worker)
            public void accept(KotlinCompletionResult result, Throwable error)
            {
                Platform.runLater(() -> {
                    cancelLoadingMessage.run();
                    cancelTimeout.run();
                    if (outcome.settle()) {
                        show(result, error, request, items);
                    }
                });
            }
        });
    }

    private static void show(KotlinCompletionResult result, Throwable error, CompletionRequest request,
                             Function<List<KotlinCompletionCandidate>, List<KotlinAssistContent>> items)
    {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof CancellationException) {
            // Superseded by a newer request, which shows its own outcome.
            return;
        }
        if (cause != null) {
            Debug.reportError("Kotlin code completion failed", cause);
            request.showMessage(Config.getString("editor.info.completionFailed"));
        }
        else if (result == null) {
            request.showMessage(Config.getString("editor.info.noCompletions"));
        }
        else {
            request.showResult(new CompletionResult(result.replaceStart(), sorted(items.apply(result.candidates()))));
        }
    }

    // As in Java completion: the Greenfoot shortcuts first, then everything else by name.
    private static List<AssistContent> sorted(List<KotlinAssistContent> items)
    {
        Comparator<AssistContent> greenfootFirst = Comparator.comparing(
            item -> !item.getName().startsWith(KotlinCompletionEngine.GREENFOOT_PREFIX));
        List<AssistContent> sorted = new ArrayList<>(items);
        sorted.sort(greenfootFirst.thenComparing(AssistContent.getComparator()));
        return sorted;
    }

    @Override
    public SuggestionDetails getSuggestionDetails(AssistContent candidate)
    {
        return ((KotlinAssistContent) candidate).getSuggestionDetails();
    }

    @Override
    public CompletionInsertion getInsertion(AssistContent candidate)
    {
        return ((KotlinAssistContent) candidate).getInsertion();
    }

    // Ensures exactly one outcome (result, failure, timeout) is shown for a request.
    private static final class Outcome
    {
        private boolean settled;

        boolean isSettled()
        {
            return settled;
        }

        // True the first time only.
        boolean settle()
        {
            boolean first = !settled;
            settled = true;
            return first;
        }
    }
}
