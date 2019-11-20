/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.print;

import bluej.Config;
import bluej.editor.Editor.PrintProgressUpdate;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A dialog which shows the printing progress.  It can be in one of two modes:
 *  - Standard: one progress bar is shown, with the progress printing a single file
 *  - Multi-file: an additional progress bar is shown above, with the progress printing multiple files.
 * The parameter passed to the constructor determines if it is multi-file mode.
 * 
 * The cancel button in the dialog will cancel the print job after the next page (i.e. after the next
 * call to setMultiFileProgress or updateFileProgress
 */
@OnThread(Tag.FXPlatform)
public class PrintProgressDialog extends Dialog<Boolean> 
{
    private final ProgressBar fileProgress = new ProgressBar(0.0);
    private final VBox content;
    
    @OnThread(Tag.Any)
    private AtomicBoolean cancelPending = new AtomicBoolean(false);
    private final Label fileHeader;
    // Null if not in multi-file mode:
    private final Label multiFileHeader;
    // Null if not in multi-file mode:
    private final ProgressBar multiFileProgress;

    public PrintProgressDialog(Window owner, boolean multiFile)
    {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle(Config.getString("printing.progress.title"));
        fileProgress.setPrefWidth(300);
        fileHeader = new Label(makeLabel("printing.progress.file", 0, 1));
        fileHeader.setTextAlignment(TextAlignment.CENTER);
        VBox.setMargin(fileHeader, new Insets(0, 0, 15, 0));
        content = new VBox(fileHeader, fileProgress);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        if (multiFile)
        {
            this.multiFileHeader = new Label(makeLabel("printing.progress.project", 0, 1));
            multiFileHeader.setTextAlignment(TextAlignment.CENTER);
            this.multiFileProgress = new ProgressBar();
            multiFileProgress.setPrefWidth(300);
            VBox.setMargin(multiFileProgress, new Insets(15, 0, 30, 0));
            content.getChildren().addAll(0, List.of(multiFileHeader, multiFileProgress));
        }
        else
        {
            this.multiFileProgress = null;
            this.multiFileHeader = null;
        }
        
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.addEventFilter(ActionEvent.ACTION, e -> {
            cancelPending.set(true);
            cancelButton.setText(Config.getString("printing.progress.canceling"));
            cancelButton.setDisable(true);
            e.consume();
        });
    }

    /**
     * Sets the latest multi-file progress.
     * @param filesDone How many files are already complete
     * @param totalFiles Total number of files to print
     * @return true to continue printing, false if printing should stop.
     */
    @OnThread(Tag.Any)
    public boolean setMultiFileProgress(int filesDone, int totalFiles)
    {
        Platform.runLater(() -> {
            multiFileProgress.setProgress((double)filesDone / (double)totalFiles);
            multiFileHeader.setText(makeLabel("printing.progress.project", filesDone, totalFiles));
        });
        if (cancelPending.get())
        {
            finished();
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Gets an update for updating the current file's progress.  @see {@link PrintProgressUpdate}
     */
    @OnThread(Tag.Any)
    public PrintProgressUpdate getWithinFileUpdater()
    {
        return this::updateFileProgress;
    }

    @OnThread(Tag.Any)
    private boolean updateFileProgress(int cur, int total)
    {
        Platform.runLater(() -> {
            fileHeader.setText(makeLabel("printing.progress.file", cur, total));
            fileProgress.setProgress((double)cur / (double)total);
        });
        if (cancelPending.get())
        {
            finished();
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Reads a label from the labels file and substitutes cur for ${cur} and total for ${total}
     */
    private String makeLabel(String labelKey, int cur, int total)
    {
        Properties p = new Properties();
        p.put("cur", "" + cur);
        p.put("total", "" + total);
        return Config.getString(labelKey, null, p);
    }

    /**
     * Called when the job is finished; will hide the dialog.
     */
    @OnThread(Tag.Any)
    public void finished()
    {
        Platform.runLater(() -> close());
    }
}
