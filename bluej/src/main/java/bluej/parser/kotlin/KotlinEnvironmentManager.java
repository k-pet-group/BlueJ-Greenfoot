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
package bluej.parser.kotlin;

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Manages a shared {@link KotlinCoreEnvironment} for editor PSI parsing
 * and Kotlin compilation. The environment is created lazily on first use
 * and cached for the lifetime of the BlueJ process.
 *
 * @author BlueJ Team
 */
@OnThread(Tag.Any)
public final class KotlinEnvironmentManager
{
    private KotlinEnvironmentManager()
    {
    }

    // Singleton state (guarded by class lock)

    private static volatile KotlinCoreEnvironment environment;
    private static volatile KtPsiFactory psiFactory;
    private static Disposable parentDisposable;

    /**
     * Get the shared {@link KotlinCoreEnvironment}. Creates it on first call
     * (~1-2 seconds). Subsequent calls return the cached instance.
     *
     * @return the shared environment, never null
     */
    public static KotlinCoreEnvironment getEnvironment()
    {
        KotlinCoreEnvironment env = environment;
        if (env == null) {
            synchronized (KotlinEnvironmentManager.class) {
                env = environment;
                if (env == null) {
                    env = createEnvironment();
                    environment = env;
                }
            }
        }
        return env;
    }

    /**
     * Get the {@link Project} instance for PSI parsing.
     *
     * @return the IntelliJ Project instance
     */
    public static Project getProject()
    {
        return getEnvironment().getProject();
    }

    /**
     * Get a {@link KtPsiFactory} for creating PSI elements from text.
     *
     * @return a reusable KtPsiFactory instance
     */
    public static KtPsiFactory getPsiFactory()
    {
        KtPsiFactory factory = psiFactory;
        if (factory == null) {
            synchronized (KotlinEnvironmentManager.class) {
                factory = psiFactory;
                if (factory == null) {
                    factory = new KtPsiFactory(getProject());
                    psiFactory = factory;
                }
            }
        }
        return factory;
    }

    /**
     * Dispose the environment (called on BlueJ shutdown).
     * After disposal, {@link #getEnvironment()} will create a fresh instance
     * if called again.
     */
    public static synchronized void dispose()
    {
        if (parentDisposable != null) {
            Disposer.dispose(parentDisposable);
            psiFactory = null;
            environment = null;
            parentDisposable = null;
        }
    }

    /**
     * Check whether the environment has been initialized.
     *
     * @return true if the environment has been created
     */
    public static boolean isInitialized()
    {
        return environment != null;
    }

    /**
     * Create the KotlinCoreEnvironment. Called exactly once under the
     * class lock.
     */
    private static KotlinCoreEnvironment createEnvironment()
    {
        parentDisposable = Disposer.newDisposable("BlueJ-KotlinEnvironment");

        CompilerConfiguration configuration = new CompilerConfiguration();
        // No special settings needed for PSI-only parsing.
        // Language version defaults to the bundled compiler's version.

        return KotlinCoreEnvironment.createForProduction(
            parentDisposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }
}
