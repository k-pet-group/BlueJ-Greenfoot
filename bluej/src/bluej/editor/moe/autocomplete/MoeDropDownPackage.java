package bluej.editor.moe.autocomplete;

import bluej.Config;
import java.lang.Package;
import javax.swing.ImageIcon;



/**
 * This class is used for the data model of the MoeDropDownList.
 * It is constructed using a java.lang.Package object.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeDropDownPackage extends MoeDropDownItem{

    /** The Package object wrapped by this class */
    private Package pkg;

    /**
     * The full name of this package as a String. (ie. java.lang.zip)
     */
    private String pkgName;

    /** The root of this package as a String. (ie. java) */
    private String rootPkgName;

    /** The rest of the package name after the package root. (ie. lang.zip) */
    private String subPackages;

    /**
     * The icon to be displayed when this
     * item isn't selected in the MoeDropDownList
     */
    private static final ImageIcon icon = Config.getImageAsIcon("image.autocomplete.package");

    /**
     * The icon to be displayed when this
     * item is selected in the MoeDropDownList
     */
    private static final ImageIcon iconSel = Config.getImageAsIcon("image.autocomplete.package.sel");



    /**
     *  Constructor for objects of class MoeDropDownPackage
     *
     *  @param p the Package object that this object will represent.
     */
    public MoeDropDownPackage(Package p) throws IllegalArgumentException{
        pkg = p;
        pkgName = p.getName();
        int dotPos = pkgName.indexOf(".");
        if(dotPos>0){
            rootPkgName = pkgName.substring(0, dotPos);
            subPackages = pkgName.substring(dotPos + 1);
        }
        else{
            //Throw an invalid package exception
            throw new IllegalArgumentException("Package doesn't contain sub packages");
        }
    }


    /**
     *  This accessor method returns the 'wrapped' Package object
     *
     *  @return the 'wrapped' Package object
     */
    public Package getPackage(){
        return pkg;
    }


    /**
     *  This method defines what code gets inserted into the source
     *  code during an auto-complete operation.  The method simply
     *  returns the sub packages of this package.
     *
     *  @return the code to be inserted into the source code.
     */
    public String getInsertableCode(){
        return subPackages;
    }


    /**
     *  This method defines what text is displayed in the MoeDropDownList
     *  The returned text states the full package name of the package.
     *
     *  @return the text to be shown for this field in the MoeDropDownList
     */
    public String getDisplayString(){
        return pkgName;
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
