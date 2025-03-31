package home.records;

import home.models.MeasuredSet;

import java.util.List;

public record MeasuredGoal(
        int order,
        String item,
        double weight,
        MeasuredSet<Double> real,
        MeasuredSet<Integer> discrete,
//        int realGoal,         within real
//        int discreteGoal,     within discrete
//        int realAdvance,      within real
//        int discreteAdvance,  within discrete
        boolean finished,
        List<Failure> failures
) {
}