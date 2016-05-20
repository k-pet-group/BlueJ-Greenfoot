package threadchecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class LocatedTag
{
    final Tag tag;
    private final boolean ignoreParent;
    private final boolean requireSynchronized;
    private final String info;
    private final boolean applyToAllSubclassMethods;
    
    private final static HashMap<String, LocatedTag> foundTags = new HashMap<>();

    public LocatedTag(Tag tag, boolean ignoreParent, boolean requireSynchronized, boolean applyToAllSubclassMethods, Supplier<String> info)
    {
        this(tag, ignoreParent, requireSynchronized, applyToAllSubclassMethods, info.get());
    }

    public LocatedTag(Tag tag, boolean ignoreParent, boolean applyToAllSubclassMethods, String info)
    {
        this(tag, ignoreParent, false, applyToAllSubclassMethods, info);
    }
    
    public LocatedTag(Tag tag, boolean ignoreParent, boolean requireSynchronized, boolean applyToAllSubclassMethods, String info)
    {
        if (tag == null)
            throw new NullPointerException();
        this.tag = tag;
        this.ignoreParent = ignoreParent;
        this.requireSynchronized = requireSynchronized;
        this.applyToAllSubclassMethods = applyToAllSubclassMethods;
        this.info = info;
        
        if (!this.info.startsWith("<"))
            foundTags.put(this.info, this);
    }        
    
    @Override
    public int hashCode()
    {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof LocatedTag))
            return false;
        LocatedTag other = (LocatedTag) obj;
        if (tag != other.tag)
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "" + tag + " (" + info + "; iP:" + ignoreParent + "; aTS:" + applyToAllSubclassMethods + ")";
    }

    public boolean ignoreParent()
    {
        return ignoreParent;
    }
    
    public boolean requireSynchronized() { return requireSynchronized; }

    public boolean applyToAllSubclassMethods()
    {
        return applyToAllSubclassMethods;
    }

    public static void dumpTagList(File loc) throws IOException
    {
        HashMap<String, String> m = new HashMap<>();
        if (loc.exists())
        {
            Files.readAllLines(loc.toPath()).forEach(l -> {
                String[] split = l.split("\t");
                if (split.length == 2)
                    m.put(split[0], split[1]); 
            });
        }
        foundTags.values().forEach(lt -> {
            m.put(padTo(lt.info, 80), "" + lt.tag);
        });
        Files.write(loc.toPath(), m.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue()).sorted().collect(Collectors.toList()));
    }
    
    private static String padTo(String s, int len)
    {
        if (s.length() >= len) return s;
        char[] spcs = new char[len - s.length()];
        Arrays.fill(spcs, ' ');
        return s + new String(spcs);
    }

}