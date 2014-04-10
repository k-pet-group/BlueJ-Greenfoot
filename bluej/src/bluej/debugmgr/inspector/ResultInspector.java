/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExpressionInformation;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.JavaUtils;
import bluej.utility.MultiLineLabel;
import bluej.views.Comment;
import bluej.views.LabelPrintWriter;
import bluej.views.MethodView;

/**
 * A window that displays a method return value.
 * 
 * @author Poul Henriksen
 */
public class ResultInspector extends Inspector
{

    // === static variables ===

    protected final static String resultTitle = Config.getString("debugger.inspector.result.title");
    protected final static String returnedString = Config.getString("debugger.inspector.result.returned");

    // === instance variables ===

    protected DebuggerObject obj;
    protected String objName; // name on the object bench

    private ExpressionInformation expressionInformation;
    private JavaType resultType; // static result type

    
    /**
     * Note: 'pkg' may be null if 'ir' is null.
     * 
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we created this result/object
     *            if null, the "get" button is permanently disabled
     * @param info
     *            The expression used to create the object (ie. the method call
     *            information)
     * @param parent
     *            The parent frame of this frame
     */
    public ResultInspector(DebuggerObject obj, InspectorManager inspectorManager, String name,
            Package pkg, InvokerRecord ir, ExpressionInformation info)
    {
        super(inspectorManager, pkg, ir, new Color(226, 224, 220));

        expressionInformation = info;
        this.obj = obj;
        this.objName = name;

        calcResultType();

        makeFrame();
        update();
        updateLayout();
    }

    /**
     * Determine the expected static type of the result.
     */
    private void calcResultType()
    {
        GenTypeClass instanceType = expressionInformation.getInstanceType();
        // We know it's a MethodView, as we don't inspect the result of a
        // constructor!
        MethodView methodView = (MethodView) expressionInformation.getMethodView();
        Method m = methodView.getMethod();

        // Find the expected return type
        JavaType methodReturnType = methodView.getGenericReturnType();

        // TODO: infer type of generic parameters based on the actual
        // arguments passed to the method.
        // For now, use the base type of the any generic type parameters
        if (methodReturnType instanceof GenTypeParameter) {
            
            // The return type may contain type parameters. First, get the
            // type parameters of the object:
            Map<String,GenTypeParameter> tparmap;
            if (instanceType != null)
                tparmap = instanceType.mapToSuper(m.getDeclaringClass().getName()).getMap();
            else
                tparmap = new HashMap<String,GenTypeParameter>();
            
            // It's possible the mapping result is a raw type.
            if (tparmap == null) {
                resultType = JavaUtils.getJavaUtils().getRawReturnType(m);
                return;
            }
            
            // Then put in the type parameters from the method itself,
            // if there are any (ie. if the method is a generic method).
            // Tpars from the method override those from the instance.
            List<GenTypeDeclTpar> tpars = JavaUtils.getJavaUtils().getTypeParams(m);
            if (tparmap != null) {
                tparmap.putAll(JavaUtils.TParamsToMap(tpars));
            }
            
            methodReturnType = methodReturnType.mapTparsToTypes(tparmap).getUpperBound();
        }

        resultType = methodReturnType;
    }

    /**
     * Returns a single string representing the return value.
     */
    @Override
    protected List<FieldInfo> getListData()
    {
        String fieldString;
        DebuggerField resultField = obj.getField(0);
        if (!resultType.isPrimitive()) {
            DebuggerObject resultObject = resultField.getValueObject(resultType);
            if (!resultObject.isNullObject()) {
                fieldString = resultObject.getGenType().toString(true);
            }
            else {
                fieldString = resultType.toString(true);
            }
        }
        else {
            fieldString = resultField.getType().toString(true);
        }
        
        List<FieldInfo> rlist = new ArrayList<FieldInfo>(1);
        rlist.add(new FieldInfo(fieldString, resultField.getValueString()));
        return rlist;
    }

    /**
     * Build the GUI
     * 
     * @param showAssert
     *            Indicates if assertions should be shown.
     */
    protected void makeFrame()
    {
        setTitle(resultTitle);

        // Create the header

        JComponent header = new JPanel();
        if (!Config.isRaspberryPi()) header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        Comment comment = expressionInformation.getComment();
        LabelPrintWriter commentLabelPrintWriter = new LabelPrintWriter();
        comment.print(commentLabelPrintWriter);
        MultiLineLabel commentLabel = commentLabelPrintWriter.getLabel();
        if (!Config.isRaspberryPi()) commentLabel.setOpaque(false);
        header.add(commentLabel);
        JLabel sig = new JLabel(expressionInformation.getSignature());
        sig.setForeground(Color.BLACK);

        header.add(sig);
        header.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(191,190,187));
        if (!Config.isRaspberryPi()) sep.setBackground(new Color(0,0,0,0));
        header.add(sep);

        // Create the main part that shows the expression and the result

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        if (!Config.isRaspberryPi()) mainPanel.setOpaque(false);

        Box result = Box.createVerticalBox();
        if (!Config.isRaspberryPi()) result.setOpaque(false);

        final JLabel expression = new JLabel(expressionInformation.getExpression(), JLabel.LEFT);
        expression.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        JPopupMenu copyPopup = new JPopupMenu();
        copyPopup.add(new AbstractAction(Config.getString("editor.copyLabel")) {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try {
                    StringSelection ss = new StringSelection(expression.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
                }
                catch (IllegalStateException ise) {
                    Debug.log("Copy: clipboard unavailable.");
                }
            }
        });
        expression.setComponentPopupMenu(copyPopup);

        result.add(expression);
        result.add(Box.createVerticalStrut(5));

        JLabel returnedLabel = new JLabel("  " + returnedString, JLabel.LEADING);
        returnedLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        result.add(returnedLabel);
        result.add(Box.createVerticalStrut(5));

        JScrollPane scrollPane = createFieldListScrollPane();
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        result.add(scrollPane);
        result.add(Box.createVerticalStrut(5));

        Box resultPanel = new Box(BoxLayout.Y_AXIS) {
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                
                Graphics2D g2d = (Graphics2D)g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(236,235,234);
                Color color2 = new Color(220,218,214);
                if (!Config.isRaspberryPi()){
                    g2d.setPaint(new GradientPaint(width/4, 0, color1,
                                                   width*3/4, height, color2));
                }else{
                    g2d.setPaint(new Color(228, 227, 224));
                }
                g2d.fillRect(0, 0, width, height);
            }
        };
        
        result.setAlignmentX(CENTER_ALIGNMENT);
        result.setAlignmentY(TOP_ALIGNMENT);
        resultPanel.add(result);
        
        Border lineBorder = BorderFactory.createLineBorder(new Color(101, 101, 101), 1);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border resultPanelBorder = BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
        
        resultPanel.setBorder(resultPanelBorder);
        mainPanel.add(resultPanel, BorderLayout.CENTER);

        JPanel inspectAndGetButtons = createInspectAndGetButtons();
        mainPanel.add(inspectAndGetButtons, BorderLayout.EAST);

        Insets insets = BlueJTheme.generalBorderWithStatusBar.getBorderInsets(mainPanel);
        mainPanel.setBorder(new EmptyBorder(insets));

        // create bottom button pane with "Close" button

        JPanel bottomPanel = new JPanel();
        if (!Config.isRaspberryPi()) bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        if (inspectorManager != null && inspectorManager.inTestMode()) {
            assertPanel = new AssertPanel();
            {
                assertPanel.setAlignmentX(LEFT_ALIGNMENT);
                assertPanel.setResultType(resultType);
                bottomPanel.add(assertPanel);
            }
        }
        
        JPanel buttonPanel;
        buttonPanel = new JPanel(new BorderLayout());
        if (!Config.isRaspberryPi()) buttonPanel.setOpaque(false);
        JButton button = createCloseButton();
        buttonPanel.add(button, BorderLayout.EAST);

        bottomPanel.add(buttonPanel);
        
        // add the components
        JPanel contentPane = new JPanel() {
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                
                Graphics2D g2d = (Graphics2D)g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(230,229,228);
                Color color2 = new Color(191,186,178);
                if (!Config.isRaspberryPi()){
                    g2d.setPaint(new GradientPaint(width/4, 0, color1,
                                                   width*3/4, height, color2));
                }else{
                    g2d.setPaint(new Color(214, 217, 223));
                }
                g2d.fillRect(0, 0, width, height);
            }
        };
        setContentPane(contentPane);
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new BorderLayout());
        contentPane.add(header, BorderLayout.NORTH);
        contentPane.add(mainPanel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(button);
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        DebuggerField field = obj.getInstanceField(0);
        if (field.isReferenceType() && ! field.isNull()) {
            // Don't use the name, since it is meaningless anyway (it is always "result")
            setCurrentObj(field.getValueObject(resultType), null, resultType.toString(false));
            setButtonsEnabled(true, true);
        }
        else {
            setCurrentObj(null, null, null);
            setButtonsEnabled(false, false);
        }
    }

    @Override
    protected void doInspect()
    {
        if (selectedField != null) {
            boolean isPublic = getButton.isEnabled();
            inspectorManager.getInspectorInstance(selectedField, selectedFieldName, pkg, isPublic ? ir : null, this);
        }
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        if(inspectorManager != null) {
            inspectorManager.removeInspector(obj);
        }
    }

    /**
     * return a String with the result.
     * 
     * @return The Result value
     */
    public String getResult()
    {
        DebuggerField resultField = obj.getField(0);
        
        String result = resultField.getType() + " " + resultField.getName() + " = " + resultField.getValueString();
        return result;
    }

    protected int getPreferredRows()
    {
        return 2;
    }
    
    protected void doGet()
    {
        if (selectedField != null) {
            pkg.getEditor().raisePutOnBenchEvent(this, selectedField, resultType.asClass(), ir);
        }
    }
}
