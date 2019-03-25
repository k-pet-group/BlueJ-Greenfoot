package bluej.editor.flow;

import bluej.Config;
import bluej.editor.flow.gen.GenRandom;
import bluej.editor.flow.gen.GenString;
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.parser.InitConfig;
import bluej.prefmgr.PrefMgr;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class TestBasicEditorInteraction extends FXTest
{
    private Stage stage;
    private FlowEditorPane flowEditorPane;

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);

        InitConfig.init();
        Config.loadFXFonts();
        PrefMgr.setScopeHighlightStrength(100);
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, true);

        this.stage = stage;
        FlowEditor flowEditor = new FlowEditor(null, null);
        flowEditorPane = flowEditor.getSourcePane();
        flowEditorPane.setPrefWidth(800.0);
        flowEditorPane.setPrefHeight(600.0);
        stage.setScene(new Scene(flowEditor));
        stage.show();
    }
    
    interface KeyboardMover
    {
        // Moves to new pos and returns expected position.  For convenience, current position and length are given
        int move(int curPos, int curLen);
    }
    
    class NamedKeyboardMover
    {
        private final String name;
        private final KeyboardMover mover;

        public NamedKeyboardMover(String name, KeyboardMover mover)
        {
            this.name = name;
            this.mover = mover;
        }
    }
    
    @Property(trials = 5)
    public void testKeyboardMovement(@From(GenString.class) String rawContent, @From(GenRandom.class) Random r)
    {
        String content = removeInvalid(rawContent);
        setText(content);
        clickOn(flowEditorPane);

        List<NamedKeyboardMover> movers = getMovers();

        for (int i = 0; i < 12; i++)
        {
            int curPos = r.nextInt(content.length() + 1);
            int curAnchor = r.nextInt(content.length() + 1);
            fx_(() -> {
                flowEditorPane.positionCaret(curPos);
                flowEditorPane.positionAnchor(curAnchor);
            });
            boolean shiftDown = r.nextBoolean();
            NamedKeyboardMover mover = movers.get(r.nextInt(movers.size()));
            if (shiftDown)
            {
                press(KeyCode.SHIFT);
            }
            int newPos = mover.mover.move(curPos, content.length());
            if (shiftDown)
            {
                release(KeyCode.SHIFT);
            }
            assertEquals("Pressing " + mover.name + (shiftDown ? " holding shift" : ""), newPos, fx(() -> flowEditorPane.getCaretPosition()).intValue());
            assertEquals("Pressing " + mover.name + (shiftDown ? " holding shift" : ""), shiftDown ? curAnchor : newPos, fx(() -> flowEditorPane.getAnchorPosition()).intValue());
        }
    }

    @Property(trials = 5)
    public void testKeyboardDelete(@When(seed=1L) @From(GenString.class) String rawContent, @When(seed=1L) @From(GenRandom.class) Random r)
    {
        String content = removeInvalid(rawContent);
        setText(content);
        clickOn(flowEditorPane);

        List<NamedKeyboardMover> movers = getMovers();

        for (int i = 0; i < 12; i++)
        {
            int curPos = r.nextInt(content.length() + 1);
            int curAnchor = r.nextInt(10) == 1 ? curPos : r.nextInt(content.length() + 1);
            int initialPos = curPos;
            int initialAnchor = curAnchor;
            fx_(() -> {
                flowEditorPane.positionCaret(initialPos);
                flowEditorPane.positionAnchor(initialAnchor);
            });
            if (r.nextBoolean())
            {
                // Move selection with keyboard
                NamedKeyboardMover mover = movers.get(r.nextInt(movers.size()));
                press(KeyCode.SHIFT);
                curPos = mover.mover.move(curPos, content.length());
                release(KeyCode.SHIFT);
            }            
            boolean deleteForward = r.nextBoolean();
            if (curPos == curAnchor)
            {
                // Will do it without selection; this is equivalent to a one char selection in that direction:
                if (deleteForward && curAnchor < content.length())
                    curAnchor += 1;
                else if (!deleteForward && curAnchor > 0)
                    curAnchor -= 1;
            }
            push(deleteForward ? KeyCode.DELETE : KeyCode.BACK_SPACE);

            int begin = Math.min(curAnchor, curPos);
            int end = Math.max(curAnchor, curPos);
            String newContent = content.substring(0, begin) + content.substring(end);
            assertEquals(newContent, fx(() -> flowEditorPane.getDocument().getFullContent()));
            // Caret should be at beginning of old selection afterwards:
            assertEquals(begin, fx(() -> flowEditorPane.getCaretPosition()).intValue());
            assertEquals(begin, fx(() -> flowEditorPane.getAnchorPosition()).intValue());
            content = newContent;
        }
    }

    private List<NamedKeyboardMover> getMovers()
    {
        List<NamedKeyboardMover> movers = new ArrayList<>();

        movers.add(new NamedKeyboardMover("Ctrl-Home", (pos, len) -> {
            push(KeyCode.SHORTCUT, KeyCode.HOME);
            return 0;
        }));
        movers.add(new NamedKeyboardMover("Ctrl-End", (pos, len) -> {
            push(KeyCode.SHORTCUT, KeyCode.END);
            return len;
        }));
        movers.add(new NamedKeyboardMover("Home", (pos, len) -> {
            push(KeyCode.HOME);
            String curContent = fx(() -> flowEditorPane.getDocument().getFullContent());
            int prevNewLine = curContent.lastIndexOf('\n', pos - 1);
            if (prevNewLine == -1)
                return 0;
            else
                return prevNewLine + 1;
        }));
        movers.add(new NamedKeyboardMover("End", (pos, len) -> {
            push(KeyCode.END);
            String curContent = fx(() -> flowEditorPane.getDocument().getFullContent());
            int nextNewLine = curContent.indexOf('\n', pos);
            if (nextNewLine == -1)
                return len;
            else
                return nextNewLine;
        }));
        movers.add(new NamedKeyboardMover("Left", (pos, len) -> {
            push(KeyCode.LEFT);
            return Math.max(0, pos - 1);
        }));
        movers.add(new NamedKeyboardMover("Right", (pos, len) -> {
            push(KeyCode.RIGHT);
            return Math.min(len, pos + 1);
        }));
        return movers;
    }

    private void setText(String content)
    {
        fx_(() -> flowEditorPane.getDocument().replaceText(0, flowEditorPane.getDocument().getLength(), content));
        sleep(1000);
    }
}
