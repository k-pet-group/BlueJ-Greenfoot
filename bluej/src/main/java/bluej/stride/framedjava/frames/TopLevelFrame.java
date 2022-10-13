/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.frames;

import java.util.List;
import java.util.stream.Stream;

import bluej.utility.javafx.SharedTransition;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import bluej.editor.stride.BirdseyeManager;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.generic.CursorFinder;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.HeaderItem;

public interface TopLevelFrame<T extends CodeElement & TopLevelCodeElement> extends CodeFrame<T>, CursorFinder
{
    public BirdseyeManager prepareBirdsEyeView(SharedTransition animate);

    void addExtendsClassOrInterface(String className);
    void removeExtendsClass();
    void addImplements(String className);
    void removeExtendsOrImplementsInterface(String interfaceName);

    public static enum BodyFocus { TOP, BOTTOM, BEST_PICK };
    public void focusOnBody(BodyFocus on);
    @OnThread(Tag.FXPlatform)
    public void saved();
    public boolean canDoBirdseye();
    public void bindMinHeight(DoubleBinding prop);
    // Returned list should be treated as read-only; use addImport to add an import
    public ObservableList<String> getImports();
    public void addImport(String importSrc);
    public void addDefaultConstructor();
    public List<? extends Frame> getMethods();
    public List<ConstructorFrame> getConstructors();
    public void insertAtEnd(Frame frame);
    public ObservableStringValue nameProperty();
    public FrameCanvas getImportCanvas();
    public void ensureImportCanvasShowing();
    
    @OnThread(value = Tag.Any, ignoreParent = true)
    @Override
    public T getCode();
    
    // Imports that mirror Frame methods, to get around the fact that we are an interface:    
    public Node getNode();
    @OnThread(Tag.FXPlatform)
    public void flagErrorsAsOld();
    @OnThread(Tag.FXPlatform)
    public void removeOldErrors();
    public Stream<HeaderItem> getHeaderItems();
    default public Stream<EditableSlot> getEditableSlots()
    {
        return getHeaderItems().map(HeaderItem::asEditable).filter(x -> x != null);
    }
    public Stream<RecallableFocus> getFocusables();
    public Stream<Frame> getAllFrames();
    
    public void restore(T target);
    default public void restoreCast(TopLevelCodeElement target)
    {
        restore((T)target);
    }
}
