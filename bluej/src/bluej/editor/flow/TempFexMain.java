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
package bluej.editor.flow;

import bluej.Config;
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import com.google.common.io.Files;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;

public class TempFexMain extends Application
{

    private JavaSyntaxView javaSyntaxView;

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void start(Stage stage) throws Exception
    {
        Properties tempCommandLineProps = new Properties();
        tempCommandLineProps.put("bluej.debug", "true");
        Config.initialise(new File("/Users/neil/intellij/bjgf/bluej/lib"), tempCommandLineProps, false);
        FlowEditor flowEditor = new FlowEditor(null);
        flowEditor.showFile("/Users/neil/intellij/bjgf/bluej/src/bluej/pkgmgr/PkgMgrFrame.java", StandardCharsets.UTF_8, true, "");
        flowEditor.setPrefWidth(800);
        flowEditor.setPrefHeight(600);
        PrefMgr.setScopeHighlightStrength(100);
        stage.setScene(new Scene(flowEditor));
        Config.addEditorStylesheets(stage.getScene());
        //JavaFXUtil.runAfter(Duration.seconds(1), () -> {
            ScopeColorsBorderPane scopeColors = new ScopeColorsBorderPane();
            scopeColors.scopeBackgroundColorProperty().set(Color.WHITE);
            scopeColors.scopeClassColorProperty().set(Color.LIGHTGREEN);
            scopeColors.scopeMethodColorProperty().set(Color.GOLDENROD);
            scopeColors.scopeClassOuterColorProperty().set(Color.GRAY);
            scopeColors.scopeMethodOuterColorProperty().set(Color.GRAY);
            scopeColors.scopeClassInnerColorProperty().set(Color.GRAY);
            javaSyntaxView = new JavaSyntaxView(editorPane, scopeColors);
            //javaSyntaxView.flushReparseQueue();
            //javaSyntaxView.recalculateAllScopes();
            //javaSyntaxView.flushReparseQueue();
        //});
        stage.show();
    }
}
