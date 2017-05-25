/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.inspector;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.gentype.JavaType;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Optional;
import java.util.function.Function;

/**
 * A panel that can record assertion statements.
 * 
 * @author  Andrew Patterson  
 */
@OnThread(Tag.FXPlatform)
public class AssertPanel extends VBox
{
    private static final String equalToLabel =
        Config.getString("debugger.assert.equalTo");
    private static final String sameAsLabel =
        Config.getString("debugger.assert.sameAs");
    private static final String notSameAsLabel =
        Config.getString("debugger.assert.notSameAs");
    private static final String notNullLabel =
        Config.getString("debugger.assert.notNull");
    private static final String assertNullLabel =
        Config.getString("debugger.assert.null");
    private static final String equalToFloatingPointLabel =
        Config.getString("debugger.assert.equalToFloatingPoint");

    /**
     * The panels and UI elements of this panel.
     */
    private Pane standardPanel;
    private Label assertLabel;
    private Label deltaLabel;
    private TextField assertData;
    // used for delta in float and double comparison
    private TextField deltaData; 
    private ComboBox<AssertInfo> assertCombo;
    protected CheckBox assertCheckbox;
    
    private static class AssertInfo
    {
        public final String label;
        public final String assertMethodName;
        public final int fieldsNeeded;
        public final boolean supportsFloatingPoint;
        public final boolean supportsObject;
        public final boolean supportsPrimitive;

        public AssertInfo(String label, String assertMethodName, int fieldsNeeded, boolean supportsFloatingPoint, boolean supportsObject, boolean supportsPrimitive)
        {
            this.label = label;
            this.assertMethodName = assertMethodName;
            this.fieldsNeeded = fieldsNeeded;
            this.supportsFloatingPoint = supportsFloatingPoint;
            this.supportsObject = supportsObject;
            this.supportsPrimitive = supportsPrimitive;
        }
        
        public boolean needsFirstField()
        {
            return fieldsNeeded >= 1;
        }
        
        public boolean needsSecondField()
        {
            return fieldsNeeded >= 2;
        }

        // For display in the combo box:
        @Override
        public String toString()
        {
            return label;
        }
    }
    
    private final ObservableList<AssertInfo> asserts = FXCollections.observableArrayList(
        new AssertInfo(equalToLabel, "assertEquals", 1, false, true, true),
        new AssertInfo(sameAsLabel, "assertSame", 1, false, true, false),
        new AssertInfo(notSameAsLabel, "assertNotSame", 1, false, true, false),
        new AssertInfo(notNullLabel, "assertNotNull", 0, false, true, false),
        new AssertInfo(assertNullLabel, "assertNull", 0, false, true, false),
        new AssertInfo(equalToFloatingPointLabel, "assertEquals", 2, true, false, false)
    );
    
    /**
     * A panel which presents an interface for making a single
     * assertion about a result. 
     */
    public AssertPanel(JavaType type)
    {
        JavaFXUtil.addStyleClass(this, "assert-panel");
        
        boolean isFloat = type.typeIs(JavaType.JT_FLOAT) || type.typeIs(JavaType.JT_DOUBLE);
        
        // a checkbox which enables/disables all the assertion UI

        assertCheckbox = new CheckBox(Config.getString("debugger.assert.assertThat"));

        standardPanel = new HBox();
        {
            JavaFXUtil.addStyleClass(standardPanel, "assert-row");
            assertLabel = new Label(Config.getString("debugger.assert.resultIs"));
            standardPanel.getChildren().add(assertLabel);

            assertCombo = new ComboBox<>();                 
            
            assertData = new TextField();
            JavaFXUtil.addStyleClass(assertData, "assert-field-data");

            deltaData = new TextField("0.1");
            JavaFXUtil.addStyleClass(deltaData, "assert-field-delta");

            standardPanel.getChildren().add(assertCombo);

            deltaLabel = new Label(Config.getString("debugger.assert.delta"));
            standardPanel.getChildren().add(assertData);
            standardPanel.getChildren().add(deltaLabel);
            standardPanel.getChildren().add(deltaData);

            deltaData.setVisible(false);
            deltaLabel.setVisible(false);
        }

        getChildren().add(assertCheckbox);
        getChildren().add(standardPanel);

        assertCombo.disableProperty().bind(assertCheckbox.selectedProperty().not());
        assertData.disableProperty().bind(
            // We calculate enabled.  This is if assertCheckBox is selected
            assertCheckbox.selectedProperty()
                // And the given assert selection needs a first field
                .and(ofB(assertCombo.getSelectionModel().selectedItemProperty(),
                    ai -> ai == null ? false : ai.needsFirstField()))
                // Now flip the enable calculation to disable:
                .not());
        BooleanBinding secondFieldExp = ofB(assertCombo.getSelectionModel().selectedItemProperty(),
            ai -> ai == null ? false : ai.needsSecondField());
        deltaData.disableProperty().bind(
            // We calculate enabled.  This is if assertCheckBox is selected
            assertCheckbox.selectedProperty()
                // And the given assert selection needs a first field
                .and(secondFieldExp)
                // Now flip the enable calculation to disable:
                .not());
        // if the second field is needed, we _always_ make it visible
        // (but perhaps not enabled)
        deltaData.visibleProperty().set(isFloat);
        deltaData.managedProperty().bind(deltaData.visibleProperty());
        deltaLabel.visibleProperty().bind(deltaData.visibleProperty());
        deltaLabel.managedProperty().bind(deltaData.visibleProperty());
        assertCheckbox.setSelected(true);
        
        assertCombo.setItems(asserts.filtered(a -> {
            if (isFloat)
                return a.supportsFloatingPoint;
            else if (type.isPrimitive())
                return a.supportsPrimitive;
            else
                return a.supportsObject;
        }));
        assertCombo.getSelectionModel().select(0);
    }
    
    private static <T> BooleanBinding ofB(ObservableValue<T> t, Function<T, Boolean> accessor)
    {
        return Bindings.createBooleanBinding(() -> accessor.apply(t.getValue()), t);
    }

    /**
     * Check whether the user has asked for an assertion to be recorded.
     */
    public boolean isAssertEnabled()
    {
        return assertCheckbox != null ? assertCheckbox.isSelected() : false;
    }
    
    /**
     * Check whether the necessary fields have been filled in to make a compilable
     * assert statement.
     */
    public boolean isAssertComplete()
    {
        AssertInfo info = assertCombo.getSelectionModel().getSelectedItem();
        
        if (info.needsSecondField()) {
            if (deltaData.getText().trim().length() == 0) {
                return false;
            }
        }
        
        if (info.needsFirstField()) {
            if (assertData.getText().trim().length() == 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Return an assertion statement out of the data in the UI.
     * 
     * @return a String representing the assertion specified in this
     *         assertion panel.
     */
    public String getAssertStatement()
    {
        // which type of assertion is selected
        AssertInfo info = assertCombo.getSelectionModel().getSelectedItem();
        
        // for double/float assertEquals() assertions, we need a delta value
        if (info.needsSecondField()) {
            return InvokerRecord.makeAssertionStatement(info.assertMethodName,
                                                        assertData.getText(),
                                                        deltaData.getText());
        }
        else if (info.needsFirstField()) {
            return InvokerRecord.makeAssertionStatement(info.assertMethodName,
                                                        assertData.getText());
        }
        else {
            return InvokerRecord.makeAssertionStatement(info.assertMethodName);
        }
    }
    
    public void recordAssertion(Package pkg, FXPlatformSupplier<Optional<Integer>> testIdentifier, int invocationIdentifier)
    {
        AssertInfo info = assertCombo.getSelectionModel().getSelectedItem();

        String param1 = info.needsFirstField() ? assertData.getText() : null;
        String param2 = info.needsSecondField() ? deltaData.getText() : null;
        Optional<Integer> optTestId = testIdentifier.get();
        optTestId.ifPresent(testId ->
            DataCollector.assertTestMethod(pkg,
                testId,
                invocationIdentifier,
                info.assertMethodName,
                param1,
                param2)
        );
    }
    
}
