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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import bluej.Config;
import bluej.editor.TextEditor;
import bluej.parser.InitConfig;
import bluej.parser.SourceLocation;
import bluej.parser.kotlin.KotlinEnvironmentManager;
import bluej.parser.kotlin.KotlinSourceEditor;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Exercises Kotlin source insertion through the real editor document and undo stack. */
public class KotlinPreparationEditTest extends FXTest
{
    /** Holds the source file loaded by the editor. */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();
    private FlowEditor editor;

    /**
     * Opens a Kotlin editor with its configuration and fonts on the test stage.
     *
     * @param stage stage supplied by the JavaFX test harness
     * @throws Exception if test application or editor setup fails
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        InitConfig.init();
        Config.loadFXFonts();
        editor = new FlowEditor(null, "Scene", null, null, null, () -> {},
            new ReadOnlyBooleanWrapper(true), true, new KotlinLanguageSupport());
        stage.setScene(new Scene(editor, 800, 600));
        stage.show();
    }

    /**
     * Checks native method/init insertion, dirty state, and one-step undo/redo.
     * @throws IOException if the temporary source cannot be written
     */
    @Test
    public void testPreparationIsOneUndoableEdit() throws IOException
    {
        String initial = "class Scene {\n    var count = 0 // Keep this 🌍 comment.\n}\n";
        Path file = temporary.newFile("Scene.kt").toPath();
        Files.writeString(file, initial, StandardCharsets.UTF_8);
        assertTrue(fx(() -> editor.showFile(file.toString(), StandardCharsets.UTF_8, false, null)));
        String before = source();
        assertEquals(initial, before);

        KotlinSourceEditor.Edit edit = KotlinSourceEditor.appendPreparation(
            before, "Scene", "prepare", List.of("count = 1"), "");
        assertNotNull(edit);
        assertTrue(edit.newMethod());
        assertEquals(before, source());
        assertFalse(fx(() -> editor.isModified()));
        fx_(() -> {
            TextEditor textEditor = editor;
            SourceLocation begin = textEditor.getLineColumnFromOffset(edit.start());
            SourceLocation end = textEditor.getLineColumnFromOffset(edit.end());
            textEditor.setText(begin, end, edit.replacement());
        });
        String after = source();
        assertEquals("""
            class Scene {
                var count = 0 // Keep this 🌍 comment.

                private fun prepare() {
                    count = 1
                }

                init {
                    prepare()
                }
            }
            """, after);
        assertTrue(fx(() -> editor.isModified()));

        fx_(() -> editor.getActions().getActionByName("undo").actionPerformed(false));
        assertEquals(before, source());
        fx_(() -> editor.getActions().getActionByName("redo").actionPerformed(false));
        assertEquals(after, source());
    }

    /**
     * The existing Insert Method action must generate Kotlin and remain one undoable edit.
     * @throws IOException if the temporary source cannot be written
     */
    @Test
    public void testInsertMethodUsesKotlinTemplate() throws IOException
    {
        String initial = "class Scene {\n}\n";
        Path file = temporary.newFile("Scene.kt").toPath();
        Files.writeString(file, initial, StandardCharsets.UTF_8);
        assertTrue(fx(() -> editor.showFile(file.toString(), StandardCharsets.UTF_8, false, null)));
        assertFalse(fx(() -> editor.isModified()));
        fx_(() -> {
            editor.enableParser(true);
            editor.getSourceDocument().flushReparseQueue();
            editor.getSourcePane().positionCaret(initial.indexOf('}'));
            editor.getActions().getActionByName("insert-method").actionPerformed(false);
        });
        String after = source();
        assertTrue(after, after.contains("    fun sampleMethod(y: Int): Int"));
        assertTrue(after, after.contains("        return y\n"));
        assertFalse(after, after.contains("public int"));
        assertFalse(PsiTreeUtil.hasErrorElements(KotlinEnvironmentManager.getPsiFactory().createFile(after)));
        assertTrue(fx(() -> editor.isModified()));
        fx_(() -> editor.getActions().getActionByName("undo").actionPerformed(false));
        assertEquals(initial, source());
        fx_(() -> editor.getActions().getActionByName("redo").actionPerformed(false));
        assertEquals(after, source());
    }

    private String source()
    {
        return fx(() -> editor.getSourcePane().getDocument().getFullContent());
    }
}
