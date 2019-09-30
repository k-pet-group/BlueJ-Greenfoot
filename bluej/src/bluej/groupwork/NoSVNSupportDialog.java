package bluej.groupwork;

import bluej.Config;
import bluej.groupwork.git.GitProvider;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

public class NoSVNSupportDialog extends FXCustomizedDialog {
    public NoSVNSupportDialog(Window owner)
    {
        super(owner, "team.load.SVNnotSupported.title", "team-load-SVNnotsupported");
        buildUI();
    }

    private void buildUI()
    {
        VBox contentPane = new VBox();
        contentPane.setMinHeight(120.0);
        //JavaFXUtil.addStyleClass(contentPane, "pane");
        getDialogPane().setContent(contentPane);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK);

        Text message = new Text(Config.getString("team.load.SVNnotSupported.details"));
        contentPane.getChildren().add(new TextFlow(message));

        contentPane.setFillWidth(true);
    }
}
