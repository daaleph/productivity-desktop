package records;

import java.util.Map;
import java.util.Objects;

public record MeasuredSet<T>(Map<String, T> quantities, Class<T> type) {
    public MeasuredSet {
        quantities = Map.copyOf(Objects.requireNonNull(quantities));
        Objects.requireNonNull(type);
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
    public String toString() {
        return "MeasuredSet[" + type.getSimpleName() + "] " + quantities;
    }
}