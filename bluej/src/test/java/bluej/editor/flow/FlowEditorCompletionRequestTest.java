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

import java.util.ArrayList;
import java.util.List;

import bluej.Config;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.parser.AssistContent;
import bluej.parser.InitConfig;
import bluej.parser.SourceLocation;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the requests {@link FlowEditor} hands to completion providers: when they stop being current.
 */
public class FlowEditorCompletionRequestTest extends FXTest
{
    private FlowEditor flowEditor;
    private final List<CompletionRequest> requests = new ArrayList<>();

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        InitConfig.init();
        Config.loadFXFonts();
        // A language whose provider only records requests, so that request state can be inspected.
        FlowLanguageSupport recording = new JavaLanguageSupport()
        {
            @Override
            public CompletionProvider getCompletionProvider()
            {
                return new RecordingProvider();
            }
        };
        flowEditor = new FlowEditor(null, "", null, null, null, () -> {}, new ReadOnlyBooleanWrapper(true), true, recording);
        stage.setScene(new Scene(new BorderPane(flowEditor)));
        stage.show();
        flowEditor.insertText("class A { }", false);
    }

    @Test
    public void testRequestCurrentUntilTyping()
    {
        CompletionRequest request = request();
        assertTrue(fx(request::isCurrent));
        fx_(() -> flowEditor.insertText("x", false));
        assertFalse("Typing should make the request stale", fx(request::isCurrent));
    }

    @Test
    public void testRequestNotCurrentAfterCaretMove()
    {
        CompletionRequest request = request();
        fx_(() -> flowEditor.getSourcePane().positionCaret(0));
        assertFalse("Moving the caret should make the request stale", fx(request::isCurrent));
    }

    @Test
    public void testNewRequestSupersedesOlder()
    {
        CompletionRequest first = request();
        CompletionRequest second = request();
        assertFalse("A newer request should supersede the older one", fx(first::isCurrent));
        assertTrue(fx(second::isCurrent));
        assertEquals("Each request should report the caret position at the time", fx(first::getCaretPosition),
            fx(second::getCaretPosition));
    }

    @Test
    public void testStaleRequestIgnoresMessages()
    {
        CompletionRequest request = request();
        fx_(() -> flowEditor.insertText("x", false));
        // Must not throw or show anything for a stale request:
        fx_(() -> request.showMessage("ignored"));
        fx_(() -> request.showResult(new CompletionResult(0, List.of())));
    }

    @Test
    public void testShortcutThatTypesACharacterDoesNotEditTheDocument()
    {
        // On macOS Option+Space types a non-breaking space, and Shift+Space types a space anywhere. When
        // such a key is bound to code completion, the typed character must not reach the document:
        // it would make a background (Kotlin) completion request stale before its result arrives.
        KeyCodeCombination altSpace = new KeyCodeCombination(KeyCode.SPACE, KeyCombination.ALT_DOWN);
        KeyCodeCombination shiftSpace = new KeyCodeCombination(KeyCode.SPACE, KeyCombination.SHIFT_DOWN);
        fx_(() -> {
            FlowActions.addKeyCombinationForActionToAllEditors(altSpace, "code-completion");
            FlowActions.addKeyCombinationForActionToAllEditors(shiftSpace, "code-completion");
        });
        try {
            assertShortcutOnlyRequestsCompletion("\u00A0", false, true);
            assertShortcutOnlyRequestsCompletion(" ", true, false);
        }
        finally {
            fx_(() -> {
                FlowActions.removeKeyCombinationForActionToAllEditors(altSpace, "code-completion");
                FlowActions.removeKeyCombinationForActionToAllEditors(shiftSpace, "code-completion");
            });
        }
    }

    @Test
    public void testDefaultShortcutUnchangedAndNextKeyStillTyped()
    {
        String before = text();
        // Ctrl+Space types nothing (the typed event, if any, is filtered), so nothing is swallowed afterwards:
        fx_(() -> pressAndType(KeyCode.SPACE, null, false, true, false));
        assertEquals("One request per Ctrl+Space", 1, requests.size());
        fx_(() -> pressAndType(KeyCode.A, "a", false, false, false));
        assertEquals("The next key typed after a shortcut is inserted", before + "a", text());
    }

    private void assertShortcutOnlyRequestsCompletion(String typed, boolean shift, boolean alt)
    {
        String before = text();
        int requestCount = requests.size();
        fx_(() -> pressAndType(KeyCode.SPACE, typed, shift, false, alt));
        assertEquals("The shortcut should request completion", requestCount + 1, requests.size());
        assertEquals("The shortcut's typed character must not be inserted", before, text());
        assertTrue("The request must still be current", fx(requests.get(requestCount)::isCurrent));
    }

    // Deliver a key press and the character it types (if any), as the keyboard would.
    private void pressAndType(KeyCode code, String typed, boolean shift, boolean control, boolean alt)
    {
        FlowEditorPane pane = flowEditor.getSourcePane();
        pane.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, control, alt, false));
        if (typed != null) {
            pane.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, typed, "", KeyCode.UNDEFINED, shift, control, alt, false));
        }
        pane.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, shift, control, alt, false));
    }

    private String text()
    {
        return fx(() -> flowEditor.getText(new SourceLocation(1, 1),
            flowEditor.getLineColumnFromOffset(flowEditor.getTextLength())));
    }

    private CompletionRequest request()
    {
        int before = requests.size();
        fx_(() -> flowEditor.createContentAssist());
        assertEquals("One request per Ctrl+Space", before + 1, requests.size());
        return requests.get(before);
    }

    // Records requests without ever answering them.
    private class RecordingProvider implements CompletionProvider
    {
        @Override
        public void requestCompletions(FlowEditor editor, CompletionRequest request)
        {
            requests.add(request);
        }

        @Override
        public SuggestionDetails getSuggestionDetails(AssistContent candidate)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionInsertion getInsertion(AssistContent candidate)
        {
            throw new UnsupportedOperationException();
        }
    }
}
