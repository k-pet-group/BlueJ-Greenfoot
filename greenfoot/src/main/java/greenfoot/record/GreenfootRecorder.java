/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010,2011,2014,2015,2016,2018,2026 Poul Henriksen and Michael Kolling
 
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.editor.Editor;
import bluej.editor.TextEditor;
import bluej.extensions2.SourceType;
import bluej.parser.SourceLocation;
import bluej.parser.kotlin.KotlinSourceEditor;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformFunction;
import greenfoot.record.RecordedAction.Arguments;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Builder for code sequences representing a recording of what the user has
 * done interactively to the world.
 */
public class GreenfootRecorder
{
    /** A map of known objects to their name as it appears in the code */
    private final HashMap<DebuggerObject, String> objectNames;
    private final List<RecordedAction> actions;
    private DebuggerObject world;
    
    public static final String METHOD_ACCESS = "private";
    public static final String METHOD_RETURN = "void";
    public static final String METHOD_NAME = "prepare";
    private String lastWorldClass;
    
    // Only valid if no run has occurred in the mean time. 
    private boolean validToSave = false;
    private long recordingGeneration;
    private Recording saving;

    /** The current source destination, resolved by the UI rather than inferred from recorded expressions.
     * @param editor the editor owning the current source buffer
     * @param sourceType the World target's source language
     */
    public record WorldSource(Editor editor, SourceType sourceType)
    {
    }

    /** Completion of source insertion, not of persistence or compilation. */
    public enum SaveResult
    {
        /** The captured actions were inserted and consumed. */
        SAVED,
        /** Act/Run has made the recording invalid. */
        INVALID_RECORDING,
        /** Editing failed; the recording is retained and partial source edits may remain. */
        EDIT_FAILED,
        /** This request was superseded or another save is still pending. */
        CANCELLED
    }

    private record Recording(long generation, String className, List<RecordedAction> actions)
    {
    }

    /**
     * Construct a new GreenfootRecorder instance.
     */
    public GreenfootRecorder()
    {
        objectNames = new HashMap<>();
        actions = new ArrayList<>();
    }

    /**
     * Record the interactive construction of an actor object.
     * @param actor   The newly constructed actor
     * @param args     The arguments supplied to the actor's constructor, as Java expressions
     * @param paramTypes  The parameter types of the called constructor
     */
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    public synchronized void createActor(DebuggerObject actor, String[] args, JavaType[] paramTypes)
    {
        String className = actor.getGenType().toString(true);
        String name = nameActor(actor);
        if (name != null) {
            // Constructor capture does not supply a varargs flag; retain the existing non-varargs handling.
            actions.add(new RecordedAction.Create(className, name, Arguments.capture(args, paramTypes, false)));
        }
    }
    
    /**
     * Called when the prepare method is replayed to indicate that the actor's name should be recorded.
     * Returns the name assigned to the actor (or null on failure).
     * 
     * This is called from the debugger thread.
     */
    private synchronized String nameActor(DebuggerObject actor)
    {
        if (objectNames.containsKey(actor))
        {
            return objectNames.get(actor);
        }

        String root = actor.getClassName().replace("." , "").replace("$", "");
        root = root.substring(0, 1).toLowerCase() + root.substring(1);
        String name = root;
        // The first item has no number (e.g. crab), the next one is crab2, then crab3, etc.  There is no crab1.
        // General note: it's important that this naming mechanism remains consistent across Greenfoot versions,
        // because we rely on the names being the same sequence to do save-the-world updates, i.e. if someone
        // has got crab, crab2, crab3, and we save the world again, it's only by naming the objects using
        // the same algorithm that we know the next one should be crab4 (the alternative of introspecting the
        // prepare method to see the existing names would be too intricate -- this way is simple best-effort).
        for (int i = 2; objectNames.values().contains(name); i++)
        {
            name = root + i;
        }
        objectNames.put(actor, name);
        return name;
    }

    /**
     * Names all the actors in the list, in order (first to last)
     */
    public synchronized void nameActors(List<DebuggerObject> actors)
    {
        for (DebuggerObject actor : actors)
        {
            nameActor(actor);
        }
    }
    
    /**
     * An actor was interactively added to the world: record the interaction
     * @param actor   The added actor
     * @param x       The actor's x position
     * @param y       The actor's y position
     */
    @OnThread(Tag.Any)
    public synchronized void addActorToWorld(DebuggerObject actor, int x, int y)
    {
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            //An actor that we don't know about is being added to the world: ignore
            return;
        }
        actions.add(new RecordedAction.Add(actorObjectName, x, y));
    }

    /**
     * Record an interactive method call on object (actor or world). Called after the method
     * successfully returns.
     * 
     * @param obj        The object on which the method was invoked
     * @param method     The method being called
     * @param args       The arguments to the method, as Java expressions
     * @param paramTypes  The parameter types of the method
     */
    public synchronized void callActorOrWorldMethod(DebuggerObject obj, Method method,
                                                    String[] args, JavaType[] paramTypes)
    {
        if (obj != null && !objectNames.containsKey(obj) && !obj.equals(world)) {
            //Method is being called on an actor we don't know about: ignore
            return;
        }
        String receiver;
        if (world != null && world.equals(obj)) {
            // Called on the world, so don't use the world's object name before the call:
            receiver = null;
        }
        else {
            receiver = String.valueOf(objectNames.get(obj));
        }
        actions.add(new RecordedAction.MethodCall(receiver, false, method,
                Arguments.capture(args, paramTypes, method.isVarArgs())));
    }

    /**
     * Record an interactive static method call. Called after the method
     * successfully returns.
     * 
     * @param className  The name of the class to which the called method belongs
     * @param method     The method
     * @param args       The arguments to the method, as Java expressions
     * @param argTypes   The parameter types of the method
     */
    public synchronized void callStaticMethod(String className, Method method, String[] args, JavaType[] argTypes)
    {
        actions.add(new RecordedAction.MethodCall(className, true, method,
                Arguments.capture(args, argTypes, method.isVarArgs())));
    }
    
    /**
     * Notify the recorder that it should clear its recording.
     * 
     * @param clearObjectNames Whether to clear the object names
     */
    public synchronized void clearCode(boolean clearObjectNames)
    {
        actions.clear();
        recordingGeneration++;
        if (clearObjectNames)
        {
            objectNames.clear();
        }
    }

    /**
     * Notify the recorder that a new world has become the current world.
     */
    public synchronized void setWorld(DebuggerObject newWorld)
    {
        if (!Objects.equals(world, newWorld)) {
            recordingGeneration++;
        }
        world = newWorld;
        lastWorldClass = newWorld.getClassName();
    }

    /**
     * Record a dragged actor interaction. This is currently called from the simulation
     * thread (i.e. with the world locked).
     * @param actor  The actor being moved
     * @param xCell  The actor's new x position
     * @param yCell  The actor's new y position
     */
    public synchronized void moveActor(DebuggerObject actor, int xCell, int yCell)
    {
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user drags them around afterwards.
            // We'll just have to ignore it
            return;
        }
        actions.add(new RecordedAction.Move(actorObjectName, xCell, yCell));
    }

    /**
     * Record a remove actor interaction.
     * @param obj  The actor being removed
     */
    public synchronized void removeActor(DebuggerObject obj)
    {
        String actorObjectName = objectNames.get(obj);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user tries to remove them afterwards.
            // We'll just have to ignore it
            return;
        }
        actions.add(new RecordedAction.Remove(actorObjectName));
        objectNames.remove(obj);
    }

    /**
     * Retrieve the code elements representing the interactions recorded up to this point.
     * @return a fresh, mutable snapshot rendered as Java/Stride code elements
     */
    public List<CodeElement> getCode()
    {
        return javaCode(snapshotActions());
    }

    private synchronized List<RecordedAction> snapshotActions()
    {
        return List.copyOf(actions);
    }

    private static List<CodeElement> javaCode(List<RecordedAction> recorded)
    {
        List<CodeElement> code = new LinkedList<>();
        for (RecordedAction action : recorded) {
            code.add(action.toJava());
        }
        return code;
    }

    /**
     * Retrieve native Kotlin statements representing the interactions recorded up to this point.
     * @return a fresh, mutable snapshot of statements without indentation or a method wrapper
     */
    @OnThread(Tag.FXPlatform)
    public List<String> getKotlinCode()
    {
        return kotlinCode(snapshotActions());
    }

    @OnThread(Tag.FXPlatform)
    private static List<String> kotlinCode(List<RecordedAction> recorded)
    {
        List<String> code = new ArrayList<>(recorded.size());
        for (RecordedAction action : recorded) {
            code.add(action.toKotlin());
        }
        return code;
    }

    /**
     * Build the prepare method from a snapshot of the recorded interactions.
     * @return the Java/Stride prepare method
     */
    public NormalMethodElement getPrepareMethod()
    {
        return prepareMethod(snapshotActions());
    }

    private static NormalMethodElement prepareMethod(List<RecordedAction> recorded)
    {
        return new NormalMethodElement(METHOD_ACCESS, METHOD_RETURN, METHOD_NAME,
            null, javaCode(recorded), preparationDocumentation());
    }

    private static String preparationDocumentation()
    {
        return Config.getString("record.method.comment1") + "\n" + Config.getString("record.method.comment2");
    }

    public CallElement getPrepareMethodCall()
    {
        return callElement(METHOD_NAME + "()");
    }
    
    private CallElement callElement(String content)
    {
        return new CallElement(content, content);
    }

    /**
     * Insert a snapshot of the recorded setup into the World's source.
     * @param getWorldSource resolves the current editor and language for the recorded World class
     * @param after completion on the FX thread, possibly deferred while a frame editor loads;
     *              only SAVED permits the caller to request compilation
     */
    @OnThread(Tag.FXPlatform)
    public void writeCode(FXPlatformFunction<String, WorldSource> getWorldSource, FXPlatformConsumer<SaveResult> after)
    {
        Recording recording = null;
        SaveResult result;
        synchronized (this) {
            result = !validToSave ? SaveResult.INVALID_RECORDING : saving != null ? SaveResult.CANCELLED : null;
            if (result == null) {
                recording = new Recording(recordingGeneration, lastWorldClass, List.copyOf(actions));
                saving = recording;
            }
        }
        if (result != null) {
            after.accept(result);
            return;
        }
        Recording request = recording;
        try {
            WorldSource source = request.className() == null ? null : getWorldSource.apply(request.className());
            if (source == null || source.editor() == null || source.editor().isReadOnly()) {
                result = SaveResult.EDIT_FAILED;
            }
            else if (!isCurrent(request)) {
                result = SaveResult.CANCELLED;
            }
            else if (source.sourceType() == SourceType.Kotlin) {
                result = writeKotlin(request, source, getWorldSource);
            }
            else if (source.sourceType() == SourceType.Java || source.sourceType() == SourceType.Stride) {
                NormalMethodElement method = prepareMethod(request.actions());
                source.editor().insertMethodCallInConstructor(request.className(), getPrepareMethodCall(), constructor -> {
                    if (!inserted(constructor)) {
                        finish(request, SaveResult.EDIT_FAILED, after);
                        return;
                    }
                    try {
                        source.editor().insertAppendMethod(method, appended -> {
                            if (appended == Editor.InsertionResult.INSERTED) {
                                showPreparedSource(source.editor());
                            }
                            finish(request, inserted(appended) ? SaveResult.SAVED : SaveResult.EDIT_FAILED, after);
                        });
                    }
                    catch (RuntimeException e) {
                        insertionFailed(request, e, after);
                    }
                });
                return;
            }
            else {
                result = SaveResult.EDIT_FAILED;
            }
        }
        catch (RuntimeException e) {
            insertionFailed(request, e, after);
            return;
        }
        finish(request, result, after);
    }

    @OnThread(Tag.FXPlatform)
    private SaveResult writeKotlin(Recording request, WorldSource destination,
                                   FXPlatformFunction<String, WorldSource> getWorldSource)
    {
        TextEditor editor = destination.editor().assumeText();
        String source = editorText(editor);
        KotlinSourceEditor.Edit edit = KotlinSourceEditor.appendPreparation(source,
            JavaNames.getBase(request.className()), METHOD_NAME, kotlinCode(request.actions()), preparationDocumentation());
        if (edit == null) {
            return SaveResult.EDIT_FAILED;
        }
        WorldSource current = getWorldSource.apply(request.className());
        if (!isCurrent(request) || current == null || current.editor() != destination.editor()
            || current.sourceType() != destination.sourceType()) {
            return SaveResult.CANCELLED;
        }
        if (editor.isReadOnly() || !source.equals(editorText(editor))) {
            return SaveResult.EDIT_FAILED;
        }
        if (edit.start() != edit.end() || !edit.replacement().isEmpty()) {
            // One enclosing replacement keeps init and method changes in the same undo operation.
            editor.setText(editor.getLineColumnFromOffset(edit.start()), editor.getLineColumnFromOffset(edit.end()),
                edit.replacement());
        }
        if (edit.newMethod()) {
            showPreparedSource(editor);
        }
        return SaveResult.SAVED;
    }

    @OnThread(Tag.FXPlatform)
    private static String editorText(TextEditor editor)
    {
        return editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(editor.getTextLength()));
    }

    private static boolean inserted(Editor.InsertionResult result)
    {
        return result == Editor.InsertionResult.INSERTED || result == Editor.InsertionResult.EXISTING;
    }

    private synchronized boolean isCurrent(Recording recording)
    {
        return saving == recording && recording.generation() == recordingGeneration;
    }

    @OnThread(Tag.FXPlatform)
    private void insertionFailed(Recording request, RuntimeException failure, FXPlatformConsumer<SaveResult> after)
    {
        synchronized (this) {
            // A synchronous completion callback may throw; that is not an insertion failure.
            if (saving != request) {
                throw failure;
            }
        }
        Debug.reportError("Could not insert recorded World setup", failure);
        finish(request, SaveResult.EDIT_FAILED, after);
    }

    @OnThread(Tag.FXPlatform)
    private void finish(Recording request, SaveResult result, FXPlatformConsumer<SaveResult> after)
    {
        synchronized (this) {
            if (saving != request) {
                return;
            }
            if (request.generation() != recordingGeneration) {
                result = SaveResult.CANCELLED;
            }
            else if (result == SaveResult.SAVED) {
                // Consume only the acknowledged prefix, retaining later captures and replay names.
                // As before, manually deleting saved setup can leave subsequent actions referring
                // to actors whose declarations no longer exist; source reconstruction is not inferred.
                actions.subList(0, request.actions().size()).clear();
            }
            saving = null;
        }
        after.accept(result);
    }

    @OnThread(Tag.FXPlatform)
    private static void showPreparedSource(Editor editor)
    {
        try {
            editor.setEditorVisible(true, false);
        }
        catch (RuntimeException e) {
            // Failure to open a window must not turn an already completed source insertion into a retry.
            Debug.reportError("Could not show recorded World setup", e);
        }
    }

    /**
     * Mark the recording as not valid (because Act/Run has been used)
     */
    public synchronized void invalidateRecording()
    {
        validToSave = false;
    }

    /**
     * Mark the recording as valid, because the user has manually created a World, 
     * or triggered a reset (including by recompiling or focusing main window).
     */
    public synchronized void recordingValid()
    {
        validToSave = true;
    }
}
