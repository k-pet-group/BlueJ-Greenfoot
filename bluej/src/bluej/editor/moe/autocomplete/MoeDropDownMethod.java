package bluej.editor.moe.autocomplete;

import bluej.Config;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.swing.ImageIcon;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * This class is used for the data model of the MoeDropDownList.
 * It can be constructed using java.lang.reflect.Method objects.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeDropDownMethod extends MoeDropDownItem{

    /** The name of the method as a String */
    private String methodName;

    /** The arguments that this method requires */
    //private Class[] arguments;

    /**
     * Stores a String representation of every argument
     * that this method requires
     */
    private String[] argumentDescriptions;


    private String allArguments;


    /** The return type of the method as a String */
    private String returnType;

    /**
     * The description of this method that
     * is displayed in the MoeDropDownList
     */
    private String displayString = null;

    /**
     * The icon to be displayed when this
     * item isn't selected in the MoeDropDownList
     */
    private static final ImageIcon icon =
        Config.getImageAsIcon("image.autocomplete.method");

    /**
     * The icon to be displayed when this
     * item is selected in the MoeDropDownList
     */
    private static final ImageIcon iconSel =
        Config.getImageAsIcon("image.autocomplete.method.sel");


    /**
     * Stores whether this method is static
     */
    private boolean staticMethod = false;


    /**
     *  Constructs a MoeDropDownMethod using a java.lang.reflect.Method
     *
     *  @param m the Method object that this object will represent.
     */
    public MoeDropDownMethod(Method m){

        //Set the method name
        setMethodName(m.getName());

        //Set the arguments
        setArguments(m.getParameterTypes());

        //Set the return type
        setReturnType(formatParameter(m.getReturnType()));

        //Set whether this method is static
        setStatic(Modifier.isStatic(m.getModifiers()));
    }


    /**
     * Constructs a MoeDropDownMethod
     *
     * @param methodName the name of the method as a String.
     * @param methodReturnType the return type of the method as a String.
     * @param methodParams all the parameters separated by commas.
     * @param staticMethod whether the method is static.
     */
    public MoeDropDownMethod(String methodName,
                             String methodReturnType,
                             String methodParams,
                             boolean staticMethod){

        //Set the method name
        setMethodName(methodName);

        //Set the arguments
        StringBuffer allArgs = new StringBuffer();
        for(int i=0; i<methodParams.length(); i++){
            char c = methodParams.charAt(i);
            if(!(c=='(' || c==')')){
                allArgs.append(c);
            }
        }

        ArrayList params = new ArrayList();
        StringTokenizer st = new StringTokenizer(allArgs.toString(), ",");
        while (st.hasMoreTokens()) {
            String param = st.nextToken();
            if(param!=null){
                param = param.trim();
                params.add(param);
            }
        }

        String[] temp = new String[params.size()];
        for(int i=0; i<params.size(); i++){
            temp[i] = (String) params.get(i);
        }

        setArguments(temp);


        //Set the return type
        setReturnType(methodReturnType);

        //Set whether this method is static
        setStatic(staticMethod);

    }



    private void setMethodName(String methodName){
        this.methodName = methodName;
    }

    private void setArguments(Class[] arguments){
        String[] temp = new String[arguments.length];
        for(int i=0; i<arguments.length; i++){
            temp[i] = formatParameter(arguments[i]);
        }
        setArguments(temp);

    }

    private void setArguments(String[] argDesc){
        argumentDescriptions = argDesc;
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<argumentDescriptions.length; i++){
            if(i>0) sb.append(", ");
            sb.append(argumentDescriptions[i]);
        }
        allArguments = sb.toString();
    }

    private void setReturnType(String returnType){
        this.returnType = returnType;
    }

    private void setStatic(boolean staticMethod){
        this.staticMethod = staticMethod;
    }


    /**
     *  This accessor method returns how many arguments the method requires.
     *
     *  @return the number of arguments the method requires.
     */
    public int getNoOfArguments(){
        return argumentDescriptions.length;
    }



    /**
     * This method returns an array of Strings that describe
     * each argument that this method should take.
     *
     * @return a String array describing each argument.
     */
    public String[] getArgumentDescriptions(){
        return argumentDescriptions;
    }


    /**
     * This method returns the return type of this wrapped method
     * as a String
     *
     * @return the return type of the wrapped method as a String.
     */
    public String getReturnType(){
        return returnType;
    }


    /**
     * This method returns whether the wrapped method is static.
     *
     * @return true if the method is static.
     */
    public boolean isStatic(){
        return staticMethod;
    }


    /**
     *  This method defines what code gets inserted into the source
     *  code during an auto-complete operation. The method simply
     *  returns the method name without any brackets.
     *
     *  @return the code to be inserted into the source code.
     */
    public String getInsertableCode(){
        return methodName;
    }


    /**
     *  This method returns the text that is displayed in the MoeDropDownList
     *  The returned text states the method name, the parameters
     *  expected and also the return type.
     *
     *  @return the text to be shown for this method in the MoeDropDownList
     */
    public String getDisplayString(){
        if(displayString==null){
            StringBuffer displayStringBuffer = new StringBuffer();
            displayStringBuffer.append(methodName);
            displayStringBuffer.append("(");
            displayStringBuffer.append(allArguments);
            displayStringBuffer.append(") ");
            displayStringBuffer.append(returnType);
            displayString = displayStringBuffer.toString();
        }
        return displayString;
    }


    /**
     * This method returns the ImageIcon that should be displayed
     * in the MoeDropDownList when the item is not selected.
     *
     * @return the non selected icon to be displayed in the MoeDropDownList.
     */
    public ImageIcon getIcon(){
        return icon;
    }


    /**
     * This method returns the ImageIcon that should be displayed
     * in the MoeDropDownList when the item is selected.
     *
     * @return the selected icon to be displayed in the MoeDropDownList.
     */
    public ImageIcon getSelectedIcon(){
        return iconSel;
    }

}
