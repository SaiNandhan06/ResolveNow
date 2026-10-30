package resolvenow.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();
    private final Set<String> suppressedHeaders = new HashSet<>();

    public HeaderMapRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    public void suppressHeader(String name) {
        suppressedHeaders.add(name.toLowerCase());
        customHeaders.remove(name);
    }

    @Override
    public String getHeader(String name) {
        String value = customHeaders.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (value != null) {
            return value;
        }
        return suppressedHeaders.contains(name.toLowerCase()) ? null : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(customHeaders.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (suppressedHeaders.contains(name.toLowerCase())) {
            String value = customHeaders.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            return value == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(Collections.singletonList(value));
        }
        String value = customHeaders.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (value != null) {
            return Collections.enumeration(Collections.singletonList(value));
        }
        String originalValue = super.getHeader(name);
        return originalValue == null
                ? Collections.emptyEnumeration()
                : Collections.enumeration(Collections.singletonList(originalValue));
    }
}