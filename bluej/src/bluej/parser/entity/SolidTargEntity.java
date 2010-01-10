package bluej.parser.entity;

import bluej.debugger.gentype.GenTypeParameter;

/**
 * A TypeArgumentEntity representing a solid (i.e. non-wildcard) type argument.
 * 
 * @author Davin McCall
 */
public class SolidTargEntity extends TypeArgumentEntity
{
    private JavaEntity solid;
    
    public SolidTargEntity(JavaEntity solid)
    {
        this.solid = solid;
    }
    
    @Override
    public GenTypeParameter getType()
    {
        TypeEntity ce = solid.resolveAsType();
        if (ce != null) {
            return ce.getType();
        }
        return null;
    }
}
