/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016,2018  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr;

import java.util.Arrays;
import java.util.stream.Collectors;

import bluej.views.ViewFilter.StaticOrInstance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;

import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.pkgmgr.Package;
import bluej.utility.javafx.JavaFXUtil;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This dialog allows selection of classes and their static methods from
 * available libraries. When a constructor or static method is selected
 * it can be invoked.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class LibraryCallDialog extends Dialog<CallableView>
{
    private static final String[] clickHere = {
        Config.getString("callLibraryDialog.clickHere1"),
        Config.getString("callLibraryDialog.clickHere2"),
    };

    private static final String[] classNotFound = {
        Config.getString("callLibraryDialog.classNotFound1"),
        Config.getString("callLibraryDialog.classNotFound2"),
    };

    private ComboBox<String> classField;
    private ListView<CallableView> methodList;
    private Label textOverlay;
    private Button docButton;

    private final ClassHistory history;
    private final Package pkg;
    private final ObservableList<CallableView> currentViews;      // views currently displayed in list
    private final BPClassLoader classLoader;

    public LibraryCallDialog(Window parent, Package pkg, BPClassLoader classLoader)
    {
        setTitle(Config.getString("callLibraryDialog.title"));
        initOwner(parent);
        initModality(Modality.WINDOW_MODAL);
        Config.addDialogStylesheets(getDialogPane());
        setResultConverter(this::calculateResult);
        
        this.pkg = pkg;
        this.classLoader = classLoader;
        currentViews = FXCollections.observableArrayList();
        history = ClassHistory.getClassHistory(10);
        makeDialog();
        classSelected();
    }

    /**
     * set the focus on the class filed .
     */
    public  void requestfocus(){
        this.classField.requestFocus();
    }
    /**
     * Show the javadoc documentation for the selected class in a browser.
     */
    private void showDocumentation()
    {
        String className = classField.getEditor().getText();
        
        // Assume unqualified classes are in java.lang
        if(className.indexOf('.') == -1)
            className = "java.lang." + className;
        
        pkg.getProject().getDefaultFXTabbedEditor().openJavaCoreDocTab(className, "#constructor_summary");
    }

    private CallableView calculateResult(ButtonType buttonType)
    {
        if (buttonType == ButtonType.OK)
        {
            CallableView viewToCall = methodList.getSelectionModel().getSelectedItem();
            if (viewToCall != null)
                history.add(classField.getEditor().getText());
            return viewToCall;
        }
        else
            return null;
    }

    /**
     * A class was selected in the class selection box. Try to load that
     * class. If successful, display its constructors and methods. Otherwise
     * clear the method list and return.
     */
    private void classSelected()
    {
        Class<?> cl = null;
        currentViews.clear();

        String className = classField.getEditor().getText();

        if(className.length() == 0) {
            displayTextInClassList(clickHere);
            return;
        }

        boolean loaded;
        try {
            ClassLoader loader = classLoader;
            cl = loader.loadClass(className);
            loaded = true;
        }
        catch(Exception exc) {
            loaded = false;
        }
        if (!loaded) {   // try for unqualified names in java.lang
            try {
                ClassLoader loader = classLoader;
                cl = loader.loadClass("java.lang."+className);
            }
            catch(Exception exc) {
                displayTextInClassList(classNotFound);
                return;
            }
        }
        displayMethodsForClass(cl);
    }

    /**
     * Given a class, display its constructors and methods in the method list.
     */
    private void displayMethodsForClass(Class<?> cl)
    {
        View classView = View.getView(cl);
        ViewFilter filter;

        ConstructorView[] constructors = classView.getConstructors();

        filter = new ViewFilter(StaticOrInstance.INSTANCE, pkg.getQualifiedName());
        Arrays.stream(constructors).filter(filter).forEach(currentViews::add);

        MethodView[] methods = classView.getAllMethods();
        filter = new ViewFilter(StaticOrInstance.STATIC, pkg.getQualifiedName());
        Arrays.stream(methods).filter(filter).forEach(currentViews::add);

        textOverlay.setVisible(false);
        methodList.getSelectionModel().clearSelection();
        methodList.setDisable(false);
        docButton.setDisable(false);
    }

    /**
     * Display a message that the current class was not found.
     */
    private void displayTextInClassList(String[] text)
    {
        textOverlay.setVisible(true);
        textOverlay.setText(Arrays.stream(text).collect(Collectors.joining("\n")));
        methodList.getItems().clear();
        methodList.setDisable(true);
        docButton.setDisable(true);
    }

    /**
     * Build the Swing dialog.
     */
    private void makeDialog()
    {
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Pane classPanel = new HBox();
        JavaFXUtil.addStyleClass(classPanel, "library-call-class");
        {
            Label classLabel = new Label(
                Config.getString("callLibraryDialog.classLabel"));

            classField = new ComboBox<>(FXCollections.observableArrayList(history.getHistory()));
            classField.setEditable(true);
            classField.setVisibleRowCount(10);
            TextField textField = classField.getEditor();
            textField.setPrefColumnCount(16);
            JavaFXUtil.addChangeListener(classField.getSelectionModel().selectedItemProperty(), x -> JavaFXUtil.runNowOrLater(this::classSelected));

            docButton = new Button(Config.getString("callLibraryDialog.docButton"));
            docButton.setOnAction(e -> showDocumentation());
            docButton.setDisable(true);
            classPanel.getChildren().setAll(classLabel, classField, docButton);
        }

    
        methodList = new ListView<>();
        JavaFXUtil.addStyleClass(methodList, "library-call-methods");
        methodList.setEditable(false);
        methodList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        methodList.setItems(currentViews);
        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(methodList.getSelectionModel().selectedItemProperty().isNull());
        
        textOverlay = new Label();
        methodList.setCellFactory(v -> {
            ListCell<CallableView> cell = new TextFieldListCell<>(new StringConverter<CallableView>()
            {
                @Override
                public String toString(CallableView object)
                {
                    return object.getShortDesc();
                }

                @Override
                public CallableView fromString(String string)
                {
                    throw new UnsupportedOperationException();
                }
            });
            cell.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY)
                {
                    methodList.getSelectionModel().select(cell.getItem());
                    ((Button)getDialogPane().lookupButton(ButtonType.OK)).fire();
                }
            });
            return cell;
        });

        VBox contentPane = new VBox();
        JavaFXUtil.addStyleClass(contentPane, "library-call-dialog-content");
        contentPane.getChildren().add(classPanel);
        contentPane.getChildren().add(JavaFXUtil.withStyleClass(new Label(Config.getString("callLibraryDialog.listHeading")), "library-call-heading"));
        contentPane.getChildren().add(new StackPane(methodList, textOverlay));

        getDialogPane().setContent(contentPane);
    }
}
