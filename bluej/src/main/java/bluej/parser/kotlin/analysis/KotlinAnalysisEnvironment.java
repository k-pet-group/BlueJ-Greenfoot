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
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CommonConfigurationKeysKt;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtFile;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Kotlin compiler environment configured for analysing one project's sources with the K1
 * (BindingContext) frontend. Kotlin sources are supplied as text on each analysis; Java sources
 * and the classpath are fixed when the environment is created, as described by its {@link Key}.
 */
@OnThread(Tag.Worker)
class KotlinAnalysisEnvironment
{
    /**
     * Everything an environment fixes at creation. An environment whose key differs from the
     * current one is stale and must be replaced.
     *
     * @param projectDir       the project directory, used as the Java source root
     * @param classPath        the compilation classpath
     * @param javaSourceStamps see {@link ProjectSources#javaSourceStamps()}
     */
    @OnThread(Tag.Any)
    record Key(File projectDir, List<File> classPath, List<String> javaSourceStamps)
    {
    }

    /**
     * Source text of one Kotlin file.
     *
     * @param fileName the file's simple name (affects only generated facade names and diagnostics)
     * @param text     the source text
     */
    @OnThread(Tag.Any)
    record SourceText(String fileName, String text)
    {
    }

    private final Key key;
    private final Disposable disposable;
    private final KotlinCoreEnvironment environment;

    /**
     * Create an environment (around 0.3s for the first one in the session, much less afterwards).
     *
     * @param key the classpath and Java sources to analyse against
     */
    KotlinAnalysisEnvironment(Key key)
    {
        this.key = key;
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "bluej-analysis");
        configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.Companion.getNONE());
        // The JDK BlueJ runs on is also the one both compilers use.
        configuration.put(JVMConfigurationKeys.JDK_HOME, new File(System.getProperty("java.home")));
        JvmContentRootsKt.addJvmClasspathRoots(configuration, key.classPath());
        // Reading Java sources lets uncompiled Java classes resolve. The classpath also contains the
        // project directory (and so its class files), but sources take precedence over stale bytecode.
        JvmContentRootsKt.addJavaSourceRoot(configuration, key.projectDir());

        disposable = Disposer.newDisposable("BlueJ-KotlinAnalysis");
        try {
            environment = KotlinCoreEnvironment.createForProduction(disposable, configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES);
        }
        catch (RuntimeException | Error e) {
            Disposer.dispose(disposable);
            throw e;
        }
    }

    /**
     * Get the classpath and Java sources this environment was created for.
     *
     * @return the key
     */
    @OnThread(Tag.Any)
    Key getKey()
    {
        return key;
    }

    /**
     * Analyse a set of Kotlin sources together.
     *
     * @param target the file the analysis is for
     * @param others the project's other Kotlin files
     * @return the analysis, valid until this environment is disposed
     */
    KotlinAnalysis analyze(SourceText target, List<SourceText> others)
    {
        Project project = environment.getProject();
        KtFile targetFile = createFile(project, target);
        List<KtFile> files = new ArrayList<>();
        files.add(targetFile);
        for (SourceText other : others) {
            files.add(createFile(project, other));
        }

        // CliBindingTrace, unlike NoScopeRecordCliBindingTrace, keeps the lexical scopes completion needs.
        AnalysisResult result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, files,
            new CliBindingTrace(project), environment.getConfiguration(), environment::createPackagePartProvider);
        return new KotlinAnalysis(result.getBindingContext(), targetFile,
            CommonConfigurationKeysKt.getLanguageVersionSettings(environment.getConfiguration()));
    }

    /**
     * Release the environment. It must not be used afterwards.
     */
    void dispose()
    {
        Disposer.dispose(disposable);
    }

    private static KtFile createFile(Project project, SourceText source)
    {
        return (KtFile) PsiFileFactory.getInstance(project).createFileFromText(source.fileName(),
            KotlinLanguage.INSTANCE, source.text(), true, false);
    }
}
