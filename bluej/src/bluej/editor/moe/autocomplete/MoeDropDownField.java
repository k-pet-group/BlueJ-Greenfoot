package bluej.editor.moe.autocomplete;

import bluej.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.swing.ImageIcon;

/**
 * This class is used for the data model of the MoeDropDownList.
 * It can be constructed using a java.lang.reflect.Field object.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeDropDownField extends MoeDropDownItem{

    /** The name of the field as a String */
    private String fieldName;

    /** The type of the field as a String */
    private String fieldType;

    /**
     * The icon to be displayed when this
     * item isn't selected in the MoeDropDownList
     */
    private static final ImageIcon icon =
        Config.getImageAsIcon("image.autocomplete.field");

    /**
     * The icon to be displayed when this
     * item is selected in the MoeDropDownList
     */
    private static final ImageIcon iconSel =
        Config.getImageAsIcon("image.autocomplete.field.sel");

    /**
     * Stores whether this field is static
     */
    private boolean staticField = false;


    /**
     *  Constructs a MoeDropDownField using a java.lang.reflect.Field object.
     *
     *  @param f the Field object that this object will represent.
     */
    public MoeDropDownField(Field f){
        fieldName = f.getName();
        Class c = f.getType();
        fieldType = formatParameter(c);
        int mods = f.getModifiers();
        staticField = Modifier.isStatic(mods);
    }


    /**
     * Constructs a MoeDropDownField
     *
     * @param fieldName the name of the field as a String.
     * @param fieldType the type of the field as a String.
     * @param staticField whether the field is a static field
     */
    public MoeDropDownField(String fieldName,
                            String fieldType,
                            boolean staticField){
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.staticField = staticField;
    }


    /**
     * This method returns whether this field is static.
     *
     * @return true if the field is static.
     */
    public boolean isStatic(){
        return staticField;
    }



    /**
     *  This method defines what code gets inserted into the source
     *  code during an auto-complete operation.  The method simply
     *  returns the field name.
     *
     *  @return the code to be inserted into the source code.
     */
    public String getInsertableCode(){
        return fieldName;
    }


    /**
     *  This method defines what text is displayed in the MoeDropDownList
     *  The returned text states the field name followed by the type
     *  of the field.
     *
     *  @return the text to be shown for this field in the MoeDropDownList
     */
    public String getDisplayString(){
        return fieldName + " " + fieldType;
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
