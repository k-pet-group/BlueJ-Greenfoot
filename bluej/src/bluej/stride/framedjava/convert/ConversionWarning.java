package bluej.stride.framedjava.convert;

import bluej.Config;

/**
 * Created by neil on 03/06/16.
 */
public abstract class ConversionWarning
{
    private final String text;

    private ConversionWarning(String labelId, String item)
    {
        this.text = Config.getString(labelId) + item;
    }

    public static class UnsupportedModifier extends ConversionWarning
    {
        public UnsupportedModifier(String context, String modifier)
        {
            super("stride.convert.unsupported.modifier", context + ": " + modifier);
        }
    }

    public static class UnsupportedFeature extends ConversionWarning
    {
        public UnsupportedFeature(String feature)
        {
            super("stride.convert.unsupported.feature", ": " + feature);
        }
    }

    // For better output in test failures:
    public String toString()
    {
        return getClass() + "[" + text + "]";
    }
}
