package org.example.formulaone.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class F1ProviderFactory {
    private final Map<String, F1Provider> providersMap = new HashMap<>();
    private final F1Provider defaultProvider;

    public F1ProviderFactory(List<F1Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No F1Provider beans available");
        }

        providers.forEach(p -> {
            providersMap.put(p.getName().toLowerCase(Locale.ROOT), p);
        });
        this.defaultProvider = providers.get(0);
    }

    public F1Provider getProvider(String name) {
        if (name == null || name.isBlank()) return defaultProvider;
        F1Provider p = providersMap.get(name.toLowerCase(Locale.ROOT));
        if (p == null)
            throw new IllegalArgumentException("No provider found: " + name);
        return p;
    }
}
