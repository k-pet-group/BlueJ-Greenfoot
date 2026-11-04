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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bluej.editor.Editor;
import bluej.editor.TextEditor;
import bluej.extensions2.SourceType;
import bluej.parser.SourceLocation;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Immutable snapshot of what a Kotlin analysis reads: the project's sources and classpath, plus the
 * current text of the file being analysed and of any other open Kotlin editors.
 *
 * @param projectDir      the project directory (Kotlin and Java sources are found beneath it)
 * @param classPath       the compilation classpath, as passed to the compilers
 * @param charset         the project's source character set, for reading files from disk
 * @param targetFile      the Kotlin file being analysed
 * @param targetText      the current (possibly unsaved) text of {@code targetFile}
 * @param openEditorTexts current text of open Kotlin editors, by source file ({@code targetText}
 *                        takes precedence for the target itself)
 */
@OnThread(Tag.Any)
public record KotlinAnalysisRequest(File projectDir, List<File> classPath, Charset charset, File targetFile,
                                    String targetText, Map<File, String> openEditorTexts)
{
    /**
     * Create a request; the classpath and editor texts are copied and file paths normalised.
     */
    public KotlinAnalysisRequest
    {
        classPath = List.copyOf(classPath);
        // Paths are compared with those found by scanning the project directory, so normalise them:
        targetFile = normalize(targetFile);
        Map<File, String> normalized = new HashMap<>();
        openEditorTexts.forEach((file, text) -> normalized.put(normalize(file), text));
        openEditorTexts = Map.copyOf(normalized);
    }

    /**
     * Snapshot a project for analysing one of its Kotlin files.
     *
     * @param project      the project containing the file
     * @param targetFile   the Kotlin file being analysed
     * @param targetEditor the editor of that file, whose current text is analysed
     * @return the request
     */
    @OnThread(Tag.FXPlatform)
    public static KotlinAnalysisRequest forProject(Project project, File targetFile, TextEditor targetEditor)
    {
        // Only loaded packages can have open editors; unopened files are read from disk later.
        Map<File, String> openEditorTexts = new HashMap<>();
        for (Package pkg : project.getProjectPackages()) {
            for (ClassTarget target : pkg.getClassTargets()) {
                Editor editor = target.getEditorIfOpen();
                if (editor != null && target.getSourceType() == SourceType.Kotlin) {
                    openEditorTexts.put(target.getSourceFile(), getFullText(editor.assumeText()));
                }
            }
        }
        return new KotlinAnalysisRequest(project.getProjectDir(), project.getClassLoader().getClassPathAsFiles(),
            project.getProjectCharset(), targetFile, getFullText(targetEditor), openEditorTexts);
    }

    /**
     * Get a copy of this request with different text for the target file.
     *
     * @param text the replacement target text
     * @return the new request
     */
    public KotlinAnalysisRequest withTargetText(String text)
    {
        return new KotlinAnalysisRequest(projectDir, classPath, charset, targetFile, text, openEditorTexts);
    }

    static File normalize(File file)
    {
        return file.toPath().toAbsolutePath().normalize().toFile();
    }

    @OnThread(Tag.FXPlatform)
    private static String getFullText(TextEditor editor)
    {
        return editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(editor.getTextLength()));
    }
}
