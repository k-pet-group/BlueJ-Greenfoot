package bluej.parser.entity;

import bluej.debugger.gentype.JavaType;

/**
 * Represents a value entity with a known (constant) integer value.
 * 
 * @author Davin McCall
 */
public class ConstantIntValue extends ValueEntity
{
    private long value;
    
    /**
     * Construct a constant integer value entity.
     * @param name   The entity name (may be null)
     * @param type   The entity type (should be a primitive integer type)
     * @param value  The value represented
     */
    public ConstantIntValue(String name, JavaType type, long value)
    {
        super(name, type);
        this.value = value;
    }
    
    @Override
    public boolean hasConstantIntValue()
    {
        return true;
    }
    
    @Override
    public long getConstantIntValue()
    {
        return value;
    }
}
