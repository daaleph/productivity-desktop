package home.models;

import java.util.Map;
import java.util.Objects;

public final class MeasuredSet<T> {
    private final Map<String, T> quantities;
    private final Class<T> type;

    private MeasuredSet(Map<String, T> quantities, Class<T> type) {
        this.quantities = Map.copyOf(quantities);
        this.type = type;
    }

    public static <T> MeasuredSet<T> create(Class<T> type, Map<String, T> quantities) {
        return new MeasuredSet<>(quantities, type);
    }

    public T getQuantity(String name) {
        return quantities.get(name);
    }

    public Map<String, T> getAllQuantities() {
        return quantities;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeasuredSet<?> metricSet = (MeasuredSet<?>) o;
        return quantities.equals(metricSet.quantities) && type.equals(metricSet.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantities, type);
    }

    @Override
    public String toString() {
        return "MeasuredSet[" + type.getSimpleName() + "] " + quantities;
    }
}