package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;


/**
 * A class for generating code for GridBagConstraints.
 * It generates code on an incremental basis.
 * It only generates code for the fields for a given GridBagConstraints object
 * that differs from another GridBagConstraints object.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GridBagConstraintsCodeGenerator
{
    private static int counter = 0;  
    private Insets defInsets = new Insets(0,0,0,0);
    private String name = new String("constraints");


    /**
     * Constructs a GridBagConstraintsCodeGenerator object.
     * It generates a unique name for the GridBagConstraints to be used in
     * the generated code.
     */
    public GridBagConstraintsCodeGenerator()
    {
	super();
	name = new String("gridbagconstraints"+counter);
	counter++;
    }


    /**
     * This is the main method of this class.
     * It generates code for the fields in gbc that differ from gbcOld
     * If gbcOld is null a whole new GridBagConstraints object is generated.
     *
     * @param  The GridBagConstraints object to generate code for.
     * @param  The previous GridBagConstraints object in the code. null, if gbc is the first GridBagConstraints object.
     * @return the code necessary for either specifying the differences between gbc and gbcOld, or if gbcOld is null a whole new GridBagConstraints object.
     */
    public String getCode(GridBagConstraints gbc, GridBagConstraints gbcOld)
    {
        StringBuffer code = new StringBuffer();
        if(gbcOld == null)
        {
            code.append("GridBagConstraints "+name+" = new GridBagConstraints();\n");
            if(gbc.gridx != GridBagConstraints.RELATIVE)
                code.append(name+".gridx = "+gbc.gridx+";\n");
            if(gbc.gridy != GridBagConstraints.RELATIVE)
                code.append(name+".gridy = "+gbc.gridy+";\n");
            if(gbc.gridwidth != 1)
                code.append(name+".gridwidth = "+gbc.gridwidth+";\n");
            if(gbc.gridheight != 1)
                code.append(name+".gridheight = "+gbc.gridheight+";\n");
            if(gbc.weightx != 0)
                code.append(name+".weightx = "+gbc.weightx+";\n");
            if(gbc.weighty != 0)
                code.append(name+".weighty = "+gbc.weighty+";\n");
            if(gbc.anchor != GridBagConstraints.CENTER)
                code.append(name+".anchor = "+getAnchor(gbc)+";\n");
            if(gbc.fill != GridBagConstraints.NONE)
                code.append(name+".fill = "+getFill(gbc)+";\n");
            if(!defInsets.equals(gbc.insets))
            {
                code.append(name+".insets.bottom = "+gbc.insets.bottom+";\n");
                code.append(name+".insets.left = "+gbc.insets.left+";\n");
                code.append(name+".insets.right = "+gbc.insets.right+";\n");
                code.append(name+".insets.top = "+gbc.insets.top+";\n");
            }
            if(gbc.ipadx != 0)
                code.append(name+".ipadx = "+gbc.ipadx+";\n");
            if(gbc.ipady != 0)
                code.append(name+".ipady = "+gbc.ipady+";\n");
        }
        else
        {
            code.append(getDifferences(gbc,gbcOld));
        }
        
        return code.toString();
    }


    /**
     * Sets the name of the generated GridBagConstraints object. Should not be called after the first
     * GridBagConstraints object is generated as the following changes
     * then will refer to the wrong variable name.
     *
     * @return name The name of the generated GridBagConstraints object.
     */
    public void setName(String name)
    {
        this.name = name;
    }


    /**
     * Returns the name of the generated GridBagConstraints object.
     *
     * @return The name of the generated GridBagConstraints object.
     */
    public String getName()
    {
        return name;
    }

    
    private String getDifferences(GridBagConstraints gbc,GridBagConstraints gbcOld)
    {
        StringBuffer code = new StringBuffer();
        if(gbc.gridx != gbcOld.gridx)
            code.append(name+".gridx = "+gbc.gridx+";\n");
        if(gbc.gridy != gbcOld.gridy)
            code.append(name+".gridy = "+gbc.gridy+";\n");
        if(gbc.gridwidth != gbcOld.gridwidth)
            code.append(name+".gridwidth = "+gbc.gridwidth+";\n");
        if(gbc.gridheight != gbcOld.gridheight)
            code.append(name+".gridheight = "+gbc.gridheight+";\n");
        if(gbc.weightx != gbcOld.weightx)
            code.append(name+".weightx = "+gbc.weightx+";\n");
        if(gbc.weighty != gbcOld.weighty)
            code.append(name+".weighty = "+gbc.weighty+";\n");
        if(gbc.anchor != gbcOld.anchor)
            code.append(name+".anchor = "+getAnchor(gbc)+";\n");
        if(gbc.fill != gbcOld.fill)
            code.append(name+".fill = "+getFill(gbc)+";\n");
        if(!(gbc.insets).equals(gbcOld.insets))
        {
            code.append(name+".insets.bottom = "+gbc.insets.bottom+";\n");
            code.append(name+".insets.left = "+gbc.insets.left+";\n");
            code.append(name+".insets.right = "+gbc.insets.right+";\n");
            code.append(name+".insets.top = "+gbc.insets.top+";\n");
        }
        
        if(gbc.ipadx != gbcOld.ipadx)
            code.append(name+".ipadx = "+gbc.ipadx+";\n");
        if(gbc.ipady != gbcOld.ipady)
            code.append(name+".ipady = "+gbc.ipady+";\n");
        return code.toString();
    }

    
    private String getAnchor(GridBagConstraints constraints)
    {
        if(constraints.anchor==GridBagConstraints.CENTER)
            return "GridBagConstraints.CENTER";
        if(constraints.anchor==GridBagConstraints.NORTH)
            return "GridBagConstraints.NORTH";
        if(constraints.anchor==GridBagConstraints.NORTHEAST)
            return "GridBagConstraints.NORTHEAST";
        if(constraints.anchor==GridBagConstraints.EAST)
            return "GridBagConstraints.EAST";
        if(constraints.anchor==GridBagConstraints.SOUTHEAST)
            return "GridBagConstraints.SOUTHEAST";
        if(constraints.anchor==GridBagConstraints.SOUTH)
            return "GridBagConstraints.SOUTH";
        if(constraints.anchor==GridBagConstraints.SOUTHWEST)
            return "GridBagConstraints.SOUTHWEST";
        if(constraints.anchor==GridBagConstraints.WEST)
            return "GridBagConstraints.WEST";
        return "GridBagConstraints.NORTHWEST";
    }

    
    private String getFill(GridBagConstraints constraints)
    {
        if(constraints.fill==GridBagConstraints.NONE)
            return "GridBagConstraints.NONE";
        if(constraints.fill==GridBagConstraints.HORIZONTAL)
            return "GridBagConstraints.HORIZONTAL";
        if(constraints.fill==GridBagConstraints.VERTICAL)
            return "GridBagConstraints.VERTICAL";
        
        return "GridBagConstraints.BOTH";
    }    
}
