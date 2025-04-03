package home.records;

import java.util.List;

public record MeasuredGoal(
        int order,
        String item,
        double weight,
        MeasuredSet<Double> real,
        MeasuredSet<Integer> discrete,
        boolean finished,
        List<Failure> failures
) {
}