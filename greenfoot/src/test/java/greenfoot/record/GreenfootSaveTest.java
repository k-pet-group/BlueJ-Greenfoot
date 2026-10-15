/*
 This file is part of the Greenfoot program.
 Copyright (C) 2026  Poul Henriksen and Michael Kolling

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
package greenfoot.record;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import bluej.debugger.DebuggerObject;
import bluej.editor.Editor.InsertionResult;
import bluej.editor.TextEditor;
import bluej.editor.flow.HoleDocument;
import bluej.extensions2.SourceType;
import bluej.parser.SourceLocation;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.utility.javafx.FXPlatformConsumer;
import greenfoot.World;
import greenfoot.record.GreenfootRecorder.SaveResult;
import greenfoot.record.GreenfootRecorder.WorldSource;

import org.junit.Test;

import static org.junit.Assert.*;

/** Source-save and acknowledgement boundaries, without a live debugger or application window. */
public class GreenfootSaveTest
{
    private final DebuggerObject world = new GreenfootRecorderTest.StubObject(World.class)
    {
        /**
         * Identifies the World source fixture instead of the underlying engine class.
         *
         * @return the replay World class name
         */
        @Override
        public String getClassName()
        {
            return "ReplayWorld";
        }
    };

    /**
     * Repeated saves use the current buffer, reuse prepare/init, and consume only inserted actions.
     *
     * @throws Exception if the recorded World method cannot be found
     */
    @Test
    public void testKotlinSaveAndRepeat() throws Exception
    {
        GreenfootRecorder recorder = recorder();
        SourceEditor editor = new SourceEditor("class ReplayWorld {\n    init { prepare() }\n    private fun prepare() { /* keep */ }\n}\n");
        record(recorder, "first");
        List<SaveResult> results = new ArrayList<>();
        recorder.writeCode(name -> new WorldSource(editor.editor, SourceType.Kotlin), results::add);
        assertEquals(List.of(SaveResult.SAVED), results);
        assertTrue(editor.text().contains("/* keep */"));
        assertTrue(editor.text().contains("setBackground(\"first\")"));
        assertEquals(1, editor.replacements);
        assertTrue(recorder.getCode().isEmpty());

        record(recorder, "second");
        recorder.writeCode(name -> new WorldSource(editor.editor, SourceType.Kotlin), results::add);
        assertEquals(List.of(SaveResult.SAVED, SaveResult.SAVED), results);
        assertEquals(2, editor.replacements);
        assertEquals(1, editor.text().split("private fun prepare", -1).length - 1);
        assertEquals(1, editor.text().split("init \\{", -1).length - 1);
        assertTrue(editor.text().indexOf("\"first\"") < editor.text().indexOf("\"second\""));
        assertTrue(recorder.getCode().isEmpty());
    }

    /**
     * Read-only and structurally incompatible source leave both source and recording intact.
     *
     * @throws Exception if the recorded World method cannot be found
     */
    @Test
    public void testUneditableSourceRetainsRecording() throws Exception
    {
        for (String source : List.of("class ReplayWorld {}", "class ReplayWorld { fun prepare() = 1 }")) {
            GreenfootRecorder recorder = recorder();
            record(recorder, "retained");
            SourceEditor editor = new SourceEditor(source);
            editor.readOnly = source.endsWith("{}");
            List<SaveResult> results = new ArrayList<>();
            recorder.writeCode(name -> new WorldSource(editor.editor, SourceType.Kotlin), results::add);
            assertEquals(source, List.of(SaveResult.EDIT_FAILED), results);
            assertEquals(source, editor.text());
            assertEquals(0, editor.replacements);
            assertEquals(1, recorder.getCode().size());
        }
    }

    /**
     * A changed current buffer must invalidate the planned offsets instead of overwriting user edits.
     *
     * @throws Exception if the recorded World method cannot be found
     */
    @Test
    public void testChangedSnapshotIsNotApplied() throws Exception
    {
        GreenfootRecorder recorder = recorder();
        record(recorder, "retained");
        SourceEditor editor = new SourceEditor("class ReplayWorld {}");
        String changed = "// newer unsaved text\nclass ReplayWorld {}";
        int[] resolutions = {0};
        List<SaveResult> results = new ArrayList<>();
        recorder.writeCode(name -> {
            if (++resolutions[0] == 2) {
                editor.document.replaceText(0, editor.document.getLength(), changed);
            }
            return new WorldSource(editor.editor, SourceType.Kotlin);
        }, results::add);
        assertEquals(List.of(SaveResult.EDIT_FAILED), results);
        assertEquals(changed, editor.text());
        assertEquals(0, editor.replacements);
        assertEquals(1, recorder.getCode().size());
    }

    /**
     * Deferred legacy insertions acknowledge the captured prefix, not later actions or overlapping saves.
     *
     * @throws Exception if the recorded World method cannot be found
     */
    @Test
    public void testDeferredSaveAcknowledgesOnlyItsSnapshot() throws Exception
    {
        for (SourceType type : List.of(SourceType.Java, SourceType.Stride)) {
            GreenfootRecorder recorder = recorder();
            record(recorder, "first");
            SourceEditor editor = new SourceEditor("");
            List<SaveResult> results = new ArrayList<>();
            recorder.writeCode(name -> new WorldSource(editor.editor, type), results::add);
            assertTrue(results.isEmpty());
            assertEquals(1, recorder.getCode().size());
            record(recorder, "later");
            List<SaveResult> overlapping = new ArrayList<>();
            recorder.writeCode(name -> new WorldSource(editor.editor, type), overlapping::add);
            assertEquals(List.of(SaveResult.CANCELLED), overlapping);
            editor.constructorDone.accept(InsertionResult.EXISTING);
            String inserted = editor.method.toJavaSource().toTemporaryJavaCodeString();
            assertTrue(inserted.contains("\"first\""));
            assertFalse(inserted.contains("\"later\""));
            assertTrue(results.isEmpty());
            editor.methodDone.accept(InsertionResult.EXISTING);
            assertEquals(List.of(SaveResult.SAVED), results);
            assertEquals(1, recorder.getCode().size());
            assertTrue(recorder.getCode().get(0).toJavaSource().toTemporaryJavaCodeString().contains("\"later\""));
        }
    }

    /**
     * A reset's identical-looking recording must survive completion of a previous save.
     *
     * @throws Exception if the recorded World method cannot be found
     */
    @Test
    public void testResetRecordingSurvivesPendingSave() throws Exception
    {
        GreenfootRecorder recorder = recorder();
        record(recorder, "same");
        SourceEditor editor = new SourceEditor("");
        List<SaveResult> results = new ArrayList<>();
        recorder.writeCode(name -> new WorldSource(editor.editor, SourceType.Stride), results::add);
        recorder.clearCode(true);
        record(recorder, "same");
        editor.constructorDone.accept(InsertionResult.EXISTING);
        editor.methodDone.accept(InsertionResult.EXISTING);
        assertEquals(List.of(SaveResult.CANCELLED), results);
        assertEquals(1, recorder.getCode().size());
    }

    /**
     * A failed second legacy insertion retains the snapshot and permits retry.
     *
     * @throws Exception if the recorded World method cannot be found
     */
    @Test
    public void testFailedLegacyInsertionCanRetry() throws Exception
    {
        GreenfootRecorder recorder = recorder();
        record(recorder, "retry");
        SourceEditor editor = new SourceEditor("");
        List<SaveResult> results = new ArrayList<>();
        recorder.writeCode(name -> new WorldSource(editor.editor, SourceType.Stride), results::add);
        editor.constructorDone.accept(InsertionResult.EXISTING);
        editor.methodDone.accept(InsertionResult.FAILED);
        assertEquals(List.of(SaveResult.EDIT_FAILED), results);
        assertEquals(1, recorder.getCode().size());
        recorder.writeCode(name -> new WorldSource(editor.editor, SourceType.Stride), results::add);
        editor.constructorDone.accept(InsertionResult.EXISTING);
        editor.methodDone.accept(InsertionResult.EXISTING);
        assertEquals(List.of(SaveResult.EDIT_FAILED, SaveResult.SAVED), results);
        assertTrue(recorder.getCode().isEmpty());
    }

    private GreenfootRecorder recorder()
    {
        GreenfootRecorder recorder = new GreenfootRecorder();
        recorder.setWorld(world);
        recorder.recordingValid();
        return recorder;
    }

    private void record(GreenfootRecorder recorder, String value) throws NoSuchMethodException
    {
        recorder.callActorOrWorldMethod(world, World.class.getMethod("setBackground", String.class),
            new String[] {"\"" + value + "\""}, null);
    }

    // Only the public editor boundary is substituted; document replacement uses the real buffer.
    private static class SourceEditor
    {
        final HoleDocument document = new HoleDocument();
        final TextEditor editor;
        int replacements;
        boolean readOnly;
        FXPlatformConsumer<InsertionResult> constructorDone;
        FXPlatformConsumer<InsertionResult> methodDone;
        NormalMethodElement method;

        @SuppressWarnings("unchecked")
        SourceEditor(String source)
        {
            document.replaceText(0, 0, source);
            editor = (TextEditor) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {TextEditor.class},
                (proxy, called, args) -> {
                    switch (called.getName()) {
                        case "assumeText": return proxy;
                        case "isReadOnly": return readOnly;
                        case "getTextLength": return document.getLength();
                        case "getLineColumnFromOffset": return document.makeSourceLocation((Integer) args[0]);
                        case "getText": return document.getContent(document.getPosition((SourceLocation) args[0]),
                            document.getPosition((SourceLocation) args[1])).toString();
                        case "setText":
                            document.replaceText(document.getPosition((SourceLocation) args[0]),
                                document.getPosition((SourceLocation) args[1]), (String) args[2]);
                            replacements++;
                            return null;
                        case "setEditorVisible": return null;
                        case "insertMethodCallInConstructor":
                            constructorDone = (FXPlatformConsumer<InsertionResult>) args[2];
                            return null;
                        case "insertAppendMethod":
                            method = (NormalMethodElement) args[0];
                            methodDone = (FXPlatformConsumer<InsertionResult>) args[1];
                            return null;
                        default: throw new AssertionError("Unexpected editor operation: " + called.getName());
                    }
                });
        }

        String text()
        {
            return document.getFullContent();
        }
    }
}
