package bluej.parser.symtab;

import bluej.extensions2.SourceType;
import bluej.parser.psi.SourceInput;
import bluej.utility.StreamUtils;

import java.util.*;
import java.util.regex.Pattern;

public class SourceInfo {
    private static final Pattern FIRST_LETTER_REGEX = Pattern.compile("(?<first>^.)(?<rest>.+)\\.kt$");

    private SourceInput inputFile;
    private Map<String, ClassInfo> classes = new HashMap<>();

    public SourceInfo(SourceInput inputFile) {
        this.inputFile = inputFile;
    }

    public void addClassInfo(ClassInfo classInfo) {
        classes.put(classInfo.getName(), classInfo);
    }

    public Optional<ClassInfo> getClassInfo(String name) {
        return Optional.ofNullable(classes.get(name));
    }

    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }

    public List<ClassInfo> getAllClassInfos() {
        return List.copyOf(classes.values());
    }

    public Map<String, ClassInfo> getAllClassInfosByName() {
        return Map.copyOf(classes);
    }

    public Set<String> getAllClassNames() {
        return new HashSet<>(classes.keySet());
    }

    public boolean hasTopLevelFunctions() {
        assert inputFile.sourceType() == SourceType.Kotlin;
        return getClassInfo(getTopLevelFacadeClassName())
            .map(ClassInfo::hasMethods)
            .orElse(false);
    }

    public String getTopLevelFacadeClassName() {
        assert inputFile.sourceType() == SourceType.Kotlin;

        return FIRST_LETTER_REGEX.matcher(inputFile.filename())
            .replaceFirst(result ->
                result.group("first").toUpperCase() + result.group("rest") + "Kt");
    }

    public Optional<ClassInfo> getTopLevelFacadeClassInfo() {
        assert inputFile.sourceType() == SourceType.Kotlin;

        return getClassInfo(getTopLevelFacadeClassName());
    }

    public Optional<ClassInfo> getSinglePublicClassInfo() {
        assert inputFile.sourceType() != SourceType.Kotlin;

        return getAllClassInfos().stream().filter(ClassInfo::isPublic).findFirst();
    }
}
