package bluej.editor.flow;

import bluej.Config;
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Properties;

public class TempFexMain extends Application
{
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void start(Stage stage) throws Exception
    {
        Properties tempCommandLineProps = new Properties();
        tempCommandLineProps.put("bluej.debug", "true");
        Config.initialise(new File("/Users/neil/intellij/bjgf/bluej/lib"), tempCommandLineProps, false);
        FlowEditorPane editorPane = new FlowEditorPane("public class Foo\n{\n    int x = 8;\n    public static void main(String[] args) {\n" +
                "        int local = 12;\n    }\n}\n");
        stage.setScene(new Scene(editorPane));
        JavaFXUtil.runAfter(Duration.seconds(1), () -> {
            ScopeColorsBorderPane scopeColors = new ScopeColorsBorderPane();
            scopeColors.scopeClassColorProperty().set(Color.LIGHTGREEN);
            scopeColors.scopeMethodColorProperty().set(Color.GOLDENROD);
            JavaSyntaxView javaSyntaxView = new JavaSyntaxView(editorPane.getDocument(), editorPane, scopeColors);
            javaSyntaxView.flushReparseQueue();
            javaSyntaxView.recalculateAllScopes();
            javaSyntaxView.flushReparseQueue();
        });
        stage.show();
    }
}
