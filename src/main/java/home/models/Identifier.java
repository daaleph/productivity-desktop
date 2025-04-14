package home.models;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Identifier<T> {
    private final List<T> ids;

    // Private constructor ensures controlled creation
    private Identifier(List<T> ids) {
        this.ids = Collections.unmodifiableList(ids);
    }

    // Factory method for single ID
    public static <T> Identifier<T> of(T singleId) {
        Objects.requireNonNull(singleId, "ID cannot be null");
        return new Identifier<>(List.of(singleId));
    }

    // Factory method for multiple IDs
    public static <T> Identifier<T> ofMultiple(List<T> ids) {
        Objects.requireNonNull(ids, "IDs list cannot be null");
        return new Identifier<>(List.copyOf(ids));
    }

    public List<T> getIds() {
        return ids;
    }

    public boolean isSingleId() {
        return ids.size() == 1;
    }
}