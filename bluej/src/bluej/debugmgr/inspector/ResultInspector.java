package bluej.debugmgr.inspector;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenType;
import bluej.debugger.gentype.GenTypeParameterizable;
import bluej.debugmgr.ExpressionInformation;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.*;
import bluej.views.Comment;
import bluej.views.LabelPrintWriter;
import bluej.views.MethodView;

/**
 * A window that displays a method return value.
 * 
 * @author Poul Henriksen
 * @version $Id: ResultInspector.java 2762 2004-07-08 11:13:23Z mik $
 */
public class ResultInspector extends Inspector implements InspectorListener {

	// === static variables ===

	protected final static String resultTitle = Config
			.getString("debugger.inspector.result.title");
	protected final static String returnedString = Config
			.getString("debugger.inspector.result.returned");

	
	// === instance variables ===

	protected DebuggerObject obj;
	protected String objName; 	// name on the object bench

	private ExpressionInformation expressionInformation;
    private GenType resultType; // static result type

	/**
	 * Return an ObjectInspector for an object. The inspector is visible. This
	 * is the only way to get access to viewers - they cannot be directly
	 * created.
	 * 
	 * @param obj
	 *            The object displayed by this viewer
	 * @param name
	 *            The name of this object or "null" if the name is unobtainable
	 * @param pkg
	 *            The package all this belongs to
	 * @param ir
	 *            the InvokerRecord explaining how we got this result/object if
	 *            null, the "get" button is permanently disabled
	 * @param info
	 *            The information about the the expression that gave this result
	 * @param parent
	 *            The parent frame of this frame
	 * @return The Viewer value
	 */
	public static ResultInspector getInstance(DebuggerObject obj, String name,
			Package pkg, InvokerRecord ir, ExpressionInformation info,
			JFrame parent) {
		ResultInspector inspector = (ResultInspector) inspectors.get(obj);

		if (inspector == null) {
			inspector = new ResultInspector(obj, name, pkg, ir, info, parent);
			inspectors.put(obj, inspector);
		}
		inspector.update();

		inspector.setVisible(true);
		inspector.bringToFront();

		return inspector;
	}


	/**
	 * Constructor Note: private -- Objectviewers can only be created with the
	 * static "getViewer" method. 'pkg' may be null if 'ir' is null.
	 * 
	 * @param isResult
	 *            false is this is an inspection, true for result displays
	 * @param obj
	 *            The object displayed by this viewer
	 * @param name
	 *            The name of this object or "null" if the name is unobtainable
	 * @param pkg
	 *            The package all this belongs to
	 * @param ir
	 *            the InvokerRecord explaining how we created this result/object
	 *            if null, the "get" button is permanently disabled
	 * @param parent
	 *            The parent frame of this frame
	 */
	private ResultInspector(DebuggerObject obj, String name, Package pkg,
			InvokerRecord ir, ExpressionInformation info, JFrame parent) {
		super(pkg, ir);
		String className = JavaNames.stripPrefix(obj.getClassName());

		expressionInformation = info;

		this.obj = obj;
		this.objName = name;
        
        calcResultType();

		makeFrame();
		DialogManager.centreWindow(this, parent);
	}
    
    /**
     * Determine the expected static type of the result.
     */
    private void calcResultType()
    {
        GenType instanceType = expressionInformation.getInstanceType();
        // We know it's a MethodView, as we don't inspect the result of a
        // constructor!
        MethodView methodView = (MethodView)expressionInformation.getMethodView();
        
        // Find the expected return type
        Method m = methodView.getMethod();
        GenType methodReturnType = JavaUtils.getJavaUtils().getReturnType(m);
        
        // TODO: infer type of generic parameters based on the actual
        // arguments passed to the method.
        // For now, use the base type of the any generic type parameters
        if( methodReturnType instanceof GenTypeParameterizable) {
            List tpars = JavaUtils.getJavaUtils().getTypeParams(m);
            Map tparmap = JavaUtils.TParamsToMap(tpars);
            methodReturnType = ((GenTypeParameterizable)methodReturnType).mapTparsToTypes(tparmap);
        
            // Pull in parameters from declaring type
            tparmap = obj.getGenType().mapToSuper(m.getDeclaringClass().getName());
            methodReturnType = ((GenTypeParameterizable)methodReturnType).mapTparsToTypes(tparmap);
        }

        resultType = methodReturnType;
    }
    
    /**
	 * Returns a single string representing the return value.
	 */
	protected Object[] getListData()
    {		
        String fieldString;
        if( ! resultType.isPrimitive() ) {
            DebuggerObject resultObject = obj.getFieldObject(0, resultType);
            if( ! resultObject.isNullObject() )
                fieldString = resultObject.getGenType().toString(true);
            else
                fieldString = resultType.toString(true);
        }
        else
            fieldString =  JavaNames.stripPrefix(obj.getFieldValueTypeString(0));        
        fieldString += " = " + obj.getFieldValueString(0);        

		return new Object[]{fieldString};
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
        setBorder(BlueJTheme.dialogBorder);

		// Create the header
        
		JComponent header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		
		Comment comment = expressionInformation.getComment();
		LabelPrintWriter commentLabelPrintWriter = new LabelPrintWriter();
		comment.print(commentLabelPrintWriter);
		MultiLineLabel commentLabel = commentLabelPrintWriter.getLabel(); 
		commentLabel.setForeground(Color.GRAY);
		header.add(commentLabel);
		JLabel sig = new JLabel(expressionInformation.getSignature());
		sig.setForeground(Color.GRAY);

        header.add(sig);
        header.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        header.add(new JSeparator());
        
		//Create the main part that shows the expression and the result
		
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);

        Box result = Box.createVerticalBox();	

		JLabel expression = new JLabel(expressionInformation.getExpression(), JLabel.LEFT);
		expression.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		
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
		
		mainPanel.add(result, BorderLayout.CENTER);

		JPanel inspectAndGetButtons = createInspectAndGetButtons();
		mainPanel.add(inspectAndGetButtons, BorderLayout.EAST);
		
        Insets insets = BlueJTheme.generalBorderWithStatusBar.getBorderInsets(mainPanel);
        mainPanel.setBorder(new EmptyBorder(insets));           


		// create bottom button pane with "Close" button
		
        JPanel bottomPanel = new JPanel();
		bottomPanel.setOpaque(false);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

		JPanel buttonPanel;
		buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.setOpaque(false);
		JButton button = createCloseButton();
		buttonPanel.add(button, BorderLayout.EAST);

		bottomPanel.add(buttonPanel);

		if (pkg.getProject().inTestMode()) {
			assertPanel = new AssertPanel();
			{
				assertPanel.setAlignmentX(LEFT_ALIGNMENT);
				bottomPanel.add(assertPanel);
			}
		}

        // add the components
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());        
        contentPane.add(header, BorderLayout.NORTH);        
        contentPane.add(mainPanel, BorderLayout.CENTER);
		contentPane.add(bottomPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(button);
		pack();
	}

	/**
	 * An element in the field list was selected.
	 */
	protected void listElementSelected(int slot) {

		if (obj.instanceFieldIsObject(slot)) {
			String newInspectedName;

			if (objName != null) {
				newInspectedName = objName + "."
						+ obj.getInstanceFieldName(slot);
			} else {
				newInspectedName = obj.getInstanceFieldName(slot);
			}

			setCurrentObj(obj.getInstanceFieldObject(slot, resultType), newInspectedName);

			if (obj.instanceFieldIsPublic(slot)) {
				setButtonsEnabled(true, true);
			} else {
				setButtonsEnabled(true, false);
			}
		} else {
			setCurrentObj(null, null);
			setButtonsEnabled(false, false);
		}
	}

	/**
	 * Show the inspector for the class of an object.
	 */
	protected void showClass() {
		ClassInspector insp = ClassInspector.getInstance(obj.getClassRef(),
				pkg, this);
	}

	/**
	 * We are about to inspect an object - prepare.
	 */
	protected void prepareInspection() {
	}

	/**
	 * Remove this inspector.
	 */
	protected void remove() {
		inspectors.remove(obj);
	}

	/**
	 * return a String with the result.
	 * 
	 * @return The Result value
	 */
	public String getResult() {
		return (String) obj.getInstanceFields(false).get(0);
	}

	public void inspectEvent(InspectorEvent e) {
		getInstance(e.getDebuggerObject(), null, pkg, null, null, this);
	}

	protected int getPreferredRows() {
		return 2;
	}

	protected void initInspectors(JTabbedPane inspTabs) {
	}
}