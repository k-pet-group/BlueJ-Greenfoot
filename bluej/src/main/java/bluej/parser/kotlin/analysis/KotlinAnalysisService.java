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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import bluej.parser.kotlin.analysis.KotlinAnalysisEnvironment.Key;
import bluej.parser.kotlin.analysis.KotlinAnalysisEnvironment.SourceText;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Runs Kotlin analyses for one project, one at a time on the shared background pool, reusing a
 * {@link KotlinAnalysisEnvironment} until the classpath or the Java sources on disk change.
 *
 * <p>Only the newest waiting request is kept: submitting a request cancels any request still
 * waiting to start. A request that has started always runs to completion.</p>
 */
@OnThread(Tag.Any)
public class KotlinAnalysisService
{
    // A submitted job: its request, the work to do on the analysis, and where its result goes.
    private record Job<T>(KotlinAnalysisRequest request, KotlinAnalysisTask<T> task, CompletableFuture<T> future)
    {
    }

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Job<?> pending;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean running;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean disposed;
    // Only the running job (jobs never overlap) or the final disposal touches the environment.
    @OnThread(Tag.Worker)
    private KotlinAnalysisEnvironment environment;
    @OnThread(Tag.Worker)
    private int environmentsCreated;

    /**
     * Queue an analysis of the request's sources, followed by the given task.
     *
     * @param request the sources to analyse
     * @param task    the work to perform on the analysis, on a background thread
     * @param <T>     the task's result type
     * @return a future completed on a background thread with the task's result. It completes
     *         exceptionally if analysis or the task fails, or if the service has been disposed, and
     *         is cancelled if a newer request replaces it before it starts.
     */
    public <T> CompletableFuture<T> submit(KotlinAnalysisRequest request, KotlinAnalysisTask<T> task)
    {
        Job<T> job = new Job<>(request, task, new CompletableFuture<>());
        synchronized (this) {
            if (disposed) {
                job.future().completeExceptionally(new IllegalStateException("Kotlin analysis service has been disposed"));
                return job.future();
            }
            if (pending != null) {
                pending.future().cancel(false);
            }
            pending = job;
            if (!running) {
                startNext();
            }
        }
        return job.future();
    }

    /**
     * Stop accepting requests and release the analysis environment once any running analysis has
     * finished. A waiting request is cancelled. Called when the owning project closes.
     */
    public void dispose()
    {
        synchronized (this) {
            if (disposed) {
                return;
            }
            disposed = true;
            if (pending != null) {
                pending.future().cancel(false);
                pending = null;
            }
            if (running) {
                // The running job releases the environment when it finishes.
                return;
            }
        }
        Utility.runBackground(this::disposeEnvironment);
    }

    // Start the pending job if there is one; otherwise go idle, releasing the environment if disposed.
    private synchronized void startNext()
    {
        Job<?> next = pending;
        pending = null;
        if (next == null) {
            running = false;
            if (disposed) {
                Utility.runBackground(this::disposeEnvironment);
            }
            return;
        }
        running = true;
        Utility.runBackground(() -> runJob(next));
    }

    @OnThread(Tag.Worker)
    private <T> void runJob(Job<T> job)
    {
        try {
            // The requester may have cancelled the future while the job was waiting.
            if (!job.future().isDone()) {
                job.future().complete(analyze(job));
            }
        }
        catch (Throwable t) {
            // The Kotlin frontend can fail in many ways (including assertion errors and stack
            // overflows on unusual code); report it to the requester and keep the queue going.
            job.future().completeExceptionally(t);
        }
        finally {
            startNext();
        }
    }

    @OnThread(Tag.Worker)
    private <T> T analyze(Job<T> job) throws IOException
    {
        KotlinAnalysisRequest request = job.request();
        ProjectSources sources = ProjectSources.scan(request.projectDir());
        Key key = new Key(request.projectDir(), request.classPath(), sources.javaSourceStamps());
        if (environment == null || !environment.getKey().equals(key)) {
            // Create the replacement before releasing the old environment, so that the Kotlin
            // application environment they share is kept alive rather than rebuilt.
            KotlinAnalysisEnvironment replacement = new KotlinAnalysisEnvironment(key);
            environmentsCreated++;
            if (environment != null) {
                environment.dispose();
            }
            environment = replacement;
        }

        List<SourceText> others = new ArrayList<>();
        for (File file : sources.kotlinFiles()) {
            if (file.equals(request.targetFile())) {
                continue;
            }
            String text = request.openEditorTexts().get(file);
            if (text == null) {
                try {
                    text = Files.readString(file.toPath(), request.charset());
                }
                catch (IOException e) {
                    // An unreadable file is simply not part of the analysis.
                    continue;
                }
            }
            others.add(new SourceText(file.getName(), text));
        }
        SourceText target = new SourceText(request.targetFile().getName(), request.targetText());
        return job.task().run(environment.analyze(target, others));
    }

    @OnThread(Tag.Worker)
    private void disposeEnvironment()
    {
        if (environment != null) {
            environment.dispose();
            environment = null;
        }
    }

    // For tests: how many environments this service has created, to check reuse and rebuilding.
    @OnThread(Tag.Worker)
    int getEnvironmentsCreated()
    {
        return environmentsCreated;
    }
}
