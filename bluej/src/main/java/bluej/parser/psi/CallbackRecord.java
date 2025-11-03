package bluej.parser.psi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CallbackRecord {
    private final String callbackName;
    private final Map<String, Object> parameters;
    private final long timestamp;

    /**
     * Creates a new callback record.
     *
     * @param callbackName The name of the callback that was invoked
     * @param parameters   Map of parameter names to values
     */
    public CallbackRecord(String callbackName, Map<String, Object> parameters) {
        this.callbackName = callbackName;
        this.parameters = new HashMap<>(parameters); // Defensive copy
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the callback name.
     *
     * @return The callback name (e.g., "beginClass", "gotMethodDeclaration")
     */
    public String getCallbackName() {
        return callbackName;
    }

    /**
     * Returns all parameters passed to the callback.
     *
     * <p>The returned map is unmodifiable to preserve record immutability.
     *
     * @return Unmodifiable map of parameter names to values
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Returns a specific parameter value.
     *
     * <p>Returns null if the parameter doesn't exist. Tests should use
     * appropriate casting based on the callback's expected parameter types.
     *
     * @param name The parameter name
     * @return The parameter value, or null if not present
     */
    public Object getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Returns the timestamp when this callback was invoked.
     *
     * @return Milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return callbackName + parameters.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallbackRecord that = (CallbackRecord) o;
        return timestamp == that.timestamp &&
                Objects.equals(callbackName, that.callbackName) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callbackName, parameters, timestamp);
    }
}
