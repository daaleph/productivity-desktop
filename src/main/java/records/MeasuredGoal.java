package records;

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
    public void logEntity() {
         String measuredGoalStructure = String.format(
            """
            MeasuredGoal {
                order: %d,
                item: "%s",
                weight: %.2f,
                real: %s,
                discrete: %s,
                finished: %b,
                failures: %s
            }""",
            order, item, weight, real, discrete, finished, failures
         );
         System.out.println(measuredGoalStructure);
    }
}