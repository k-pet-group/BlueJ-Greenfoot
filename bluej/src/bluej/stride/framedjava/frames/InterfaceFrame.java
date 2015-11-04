/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import bluej.utility.javafx.SharedTransition;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import bluej.editor.stride.BirdseyeManager;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.InterfaceElement;
import bluej.stride.generic.DocumentedSingleCanvasFrame;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.InteractionManager.Kind;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.ClassNameDefTextSlot;
import bluej.stride.slots.ExtendsList;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.stride.slots.TypeTextSlot;

public class InterfaceFrame extends DocumentedSingleCanvasFrame
  implements TopLevelFrame<InterfaceElement>
{
    private TextSlot<NameDefSlotFragment> paramInterfaceName;
    private final InteractionManager editor;
    
    private final ExtendsList extendsList;
    
    private InterfaceElement element;
    private final EntityResolver projectResolver;
    
    public InterfaceFrame(InteractionManager editor, NameDefSlotFragment interfaceName, List<TypeSlotFragment> extendsTypes,
            EntityResolver projectResolver, JavadocUnit documentation, boolean enabled)
    {
        super(editor, "interface", "interface-");
        this.editor = editor;
        this.projectResolver = projectResolver;
        
        //Parameters
        paramInterfaceName = new ClassNameDefTextSlot(editor, this, getHeaderRow(), "interface-name-");
        paramInterfaceName.addValueListener(SlotTraversalChars.IDENTIFIER);
        paramInterfaceName.setPromptText("interface name");
        paramInterfaceName.setText(interfaceName);
        
        extendsList = new ExtendsList(this, () -> {
            TypeTextSlot s = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor, Kind.INTERFACE), "interface-extends-");
            s.setPromptText("interface type");
            return s;
        }, () -> getCanvas().getFirstCursor().requestFocus());
        extendsTypes.forEach(t -> this.extendsList.addTypeSlotAtEnd(t.getContent()));

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(paramInterfaceName),
                extendsList.getHeaderItems()
        ));
        
        setDocumentationPromptText("Write a description of your " + interfaceName.getContent() + " interface here...");
        setDocumentation(documentation.toString());
        
        frameEnabledProperty.set(enabled);

//  this.canvas = new FrameCanvas(editor, new CanvasParent() {
//          
//          @Override
//          public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor,
//                  FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
//          {
//              return InterfaceFrame.this.findCursor(sceneX, sceneY, prevCursor, nextCursor, exclude, isDrag, canDescend);
//          }
//          
//          @Override
//          public List<ExtensionDescription> getAvailableInnerExtensions()
//          {
//              ExtensionDescription extendsExtension;
//              if (!showingExtends.get()) {
//                  extendsExtension = new ExtensionDescription(GreenfootFrameDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration", () -> {
//                      addExtends();
//                      extendsSlot.requestFocus();
//                      return true;
//                  });
//              }
//              else {
//                  extendsExtension = new ExtensionDescription('\b', "Delete extends declaration", () -> {
//                      removeExtends();
//                      extendsSlot.setText("");
//                      return true;
//                  });
//              }
    //
//              return Arrays.asList(extendsExtension);
//          }
//          
//          @Override
//          public FrameCursor getCursorBefore()
//          {
//              // TODO do not return null if an inherited frame is visible
//              return null;
//          }
//          
//          @Override
//          public FrameCursor getCursorAfter()
//          {
//              // Never a cursor after main canvas:
//              return null;
//          }
//          
//          @Override
//          public boolean acceptsType(FrameCanvas canvasBase, Class<? extends Frame> frameClass)
//          {
//              return getEditor().getDictionary().isValidInterfaceMember(frameClass);
//          }
//          
//          @Override
//          public Frame getFrame()
//          {
//              return InterfaceFrame.this;
//          }
//          
    //  }, "interface-");
      
    }

    @Override
    public synchronized void regenerateCode()
    {
        List<CodeElement> members = new ArrayList<CodeElement>();
        getCanvas().getBlocksSubtype(CodeFrame.class).forEach(c -> {
            c.regenerateCode();
            members.add(c.getCode());
        });
        element = new InterfaceElement(this, projectResolver, paramInterfaceName.getSlotElement(), 
                extendsList.getTypes(), members,
                new JavadocUnit(getDocumentation()), FXCollections.observableArrayList() /* TODO */, frameEnabledProperty.get());
        
    }

    @Override
    @OnThread(value = Tag.Any, ignoreParent = true)
    public synchronized InterfaceElement getCode()
    {
        return element;
    }

    @Override
    public BirdseyeManager prepareBirdsEyeView(SharedTransition animate)
    {
        return null;
    }

    @Override
    public void focusOnBody(BodyFocus on)
    {
        FrameCursor c = on != BodyFocus.BOTTOM ? canvas.getFirstCursor() : canvas.getLastCursor();
        c.requestFocus();
        editor.scrollTo(c.getNode(), -100);
    }

    @Override
    public void saved()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean canDoBirdseye()
    {
        // No point, since we only have prototypes in
        return false;
    }

    @Override
    public void bindMinHeight(DoubleBinding prop)
    {
        getRegion().minHeightProperty().bind(prop);        
    }

    @Override
    public List<NormalMethodFrame> getMethods()
    {
        return Collections.emptyList();
    }

    @Override
    public List<ConstructorFrame> getConstructors()
    {
        return Collections.emptyList();
    }

    @Override
    public void insertAtEnd(Frame frame)
    {
        canvas.getLastCursor().insertBlockAfter(frame); 
    }

    @Override
    public boolean acceptsType(FrameCanvas canvas, Class<? extends Frame> blockClass)
    {
        return getEditor().getDictionary().isValidInterfaceMethod(blockClass);
    }

//  @Override
//  public List<ExtensionDescription> getAvailableInnerExtensions()
//  {
//      ExtensionDescription extendsExtension;
//      if (!showingExtends.get()) {
//          extendsExtension = new ExtensionDescription(GreenfootFrameDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration", () -> {
//              addExtends();
//              extendsSlot.requestFocus();
//              return true;
//          });
//      }
//      else {
//          extendsExtension = new ExtensionDescription('\b', "Delete extends declaration", () -> {
//              removeExtends();
//              extendsSlot.setText("");
//              return true;
//          });
//      }
//
//      return Arrays.asList(extendsExtension);
//  }
    
    @Override
    public ObservableStringValue nameProperty()
    {
        return paramInterfaceName.textProperty();
    }
    
    @Override
    public Stream<RecallableFocus> getFocusables()
    {
        // All slots, and all cursors:
        return getFocusablesInclContained(this);
    }

    @Override
    protected List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
        return null;
    }

    @Override
    public List<ExtensionDescription> getAvailableInnerExtensions(FrameCanvas canvas, FrameCursor cursor)
    {
        ExtensionDescription extendsExtension = new ExtensionDescription(GreenfootFrameDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration", () -> {
            extendsList.addTypeSlotAtEnd("");
        });
        
        return Arrays.asList(extendsExtension);
    }

    @Override
    public ObservableList<String> getImports()
    {
        return FXCollections.observableArrayList();
    }

    @Override
    public void addImport(String importSrc)
    {
        // TODO        
    }

    public void addDefaultConstructor()
    {
        throw new IllegalAccessError();
    }

    @Override
    public FrameCanvas getImportCanvas() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void ensureImportCanvasShowing() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void restore(InterfaceElement target)
    {
        // TODO
    }
}
