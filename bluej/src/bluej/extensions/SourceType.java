package bluej.extensions;

// Note: this is not part of the extensions API as such, but because they need to be able
// to see it, it lives in the extensions package.

/**
 * The type of source that is available.
 */
public enum SourceType
{
    NONE, Java, Stride;

    public static SourceType getEnum(String s)
    {
        if (s == null || s.equals("null")) {
            return NONE;
        }
        String lowerCase = s.toLowerCase();
        if(lowerCase.equals("stride")){
            return Stride;
        }
        if(lowerCase.equals("java")){
            return Java;
        }
        throw new IllegalArgumentException("No Enum specified for this string");
    }
}