/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2018,2019,2020 Michael Kölling and John Rosenberg
 
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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.editor.stride.FrameEditor;
import bluej.parser.AssistContent.Access;
import javafx.application.Platform;
import javafx.beans.binding.StringExpression;
import javafx.scene.control.TextField;
import javafx.util.Pair;

import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.ImportElement.ImportFragment;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.CompletionCalculator;
import bluej.editor.fixes.SuggestionList;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;

import bluej.editor.stride.FrameCatalogue;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A statement for import packages/classes
 * @author Amjad Altadmri
 */
public class ImportFrame extends SingleLineFrame implements CodeFrame<ImportElement>
{    
    private static final String IMPORT_STYLE_PREFIX = "import-";
    private static final List<FrameCatalogue.Hint> HINTS = Arrays.asList(
        new FrameCatalogue.Hint("greenfoot.*", "Greenfoot classes"),
        new FrameCatalogue.Hint("java.util.*", "Java utility classes")
    );
    private TextSlot<ImportFragment> importField;
    private ImportElement element;
    
    /**
     * Default constructor.
     */
    private ImportFrame(InteractionManager editor)
    {
        super(editor, "import ", IMPORT_STYLE_PREFIX);
        
        CompletionCalculator calc = new CompletionCalculator() {
            private List<Pair<SuggestionList.SuggestionShown, String>> imports;
            
            @Override
            @OnThread(Tag.FXPlatform)
            public void withCalculatedSuggestionList(PosInSourceDoc pos, CodeElement codeEl,
                                                     SuggestionListListener clickListener,
                                                     FXPlatformConsumer<SuggestionList> handler)
            {
                FrameEditor frameEditor = editor.getFrameEditor();
                Utility.runBackground(() -> {
                    imports = frameEditor.getEditorFixesManager().getImportSuggestions().entrySet().stream().flatMap(e ->
                            e.getValue().stream().filter(ac ->
                                    // Only if visible:
                                    ac.getPackage() == null || ac.getPackage().equals("") || ac.getAccessPermission() == Access.PUBLIC
                            ).flatMap(
                                    ac -> (ac.getPackage() == null || ac.getPackage().equals("")) ?
                                            Stream.of(ac.getName()) :
                                            Stream.of(ac.getPackage() + "." + (ac.getDeclaringClass() == null ? "" : ac.getDeclaringClass() + ".") + ac.getName(), ac.getPackage() + ".*")
                            ).sorted().distinct().map(v -> new Pair<SuggestionList.SuggestionShown, String>(e.getKey(), v))
                    ).collect(Collectors.toList());
                    Platform.runLater(() -> {
                        SuggestionList suggestionDisplay = new SuggestionList(editor, Utility.mapList(imports, imp -> new SuggestionList.SuggestionDetails(imp.getValue(), null, null, imp.getKey())), null, SuggestionList.SuggestionShown.COMMON, null, clickListener);
                        handler.accept(suggestionDisplay);
                    });
                });
            }
            
            @Override
            @OnThread(Tag.FXPlatform)
            public boolean execute(TextField field, int highlighted, int startOfCurWord)
            {
                if (highlighted >= 0)
                {
                    field.setText(imports.get(highlighted).getValue());
                    return true;
                }
                return false;
            }
        };
        
        importField = new TextSlot<ImportFragment>(editor, this, this, getHeaderRow(), calc, IMPORT_STYLE_PREFIX + "slot-", HINTS) {

            @Override
            protected ImportFragment createFragment(String content)
            {
                return new ImportFragment(content, this);
            }

            @Override
            public void valueChangedLostFocus(String oldValue, String newValue)
            {
                // Nothing to do
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void addError(CodeError err)
            {
                editor.ensureImportsVisible();
                super.addError(err);
            }

            @Override
            public List<? extends PossibleLink> findLinks()
            {
                if (!getText().endsWith(".*"))
                {
                    return Collections.singletonList(new PossibleTypeLink(getText(), 0, getText().length(), this));
                }
                return Collections.emptyList();
            }

            @Override
            public int getStartOfCurWord()
            {
                // Start of word is always start of slot; don't let the dots in package/class names break the word:
                return 0;
            }
        };
        importField.setPromptText("package or class");
        importField.addValueListener(new SlotTraversalChars()
        {
            @Override
            @OnThread(Tag.FXPlatform)
            public void backSpacePressedAtStart(HeaderItem slot)
            {
                backspaceAtStart(getHeaderRow(), slot);
            }
        });
        setHeaderRow(importField, previewSemi);
    }
    
    /**
     * Creates an import statement with a specific class/package.
     */
    public ImportFrame(InteractionManager editor, ImportElement element, boolean enabled)
    {
        this(editor);
        this.element = element;
        this.importField.setText(element.getImport());
        frameEnabledProperty.set(enabled);
    }
    
    // Constructor for adding new imports in response to an error-fix:
    public ImportFrame(InteractionManager editor, String src)
    {
        this(editor);
        importField.setText(src);
        this.element = new ImportElement(src, importField, frameEnabledProperty.get());
    }
    
    public static FrameFactory<ImportFrame> getFactory()
    {
        return new FrameFactory<ImportFrame>() {
            
            @Override
            public ImportFrame createBlock(InteractionManager editor)
            {
                return new ImportFrame(editor);
            }
            
            @Override 
            public Class<ImportFrame> getBlockClass()
            { 
                return ImportFrame.class;
            }
        };
    }

    public String getImport()
    {
        return importField.getText();
    }

    @Override
    public void regenerateCode()
    {
        element = new ImportElement(importField.getText(), importField, frameEnabledProperty.get());
    }

    @Override
    public ImportElement getCode()
    {
        return element;
    }

    public RecallableFocus getFocusable()
    {
        return importField;
    }
    
    public StringExpression importProperty()
    {
        return importField.textProperty();
    }
}
