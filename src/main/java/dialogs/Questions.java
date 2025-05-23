package dialogs;

public enum Questions {
    PROJECT_NAME("What's the name of this?"),
    COMPLETING_DAYS("Mow many days?"),
    COMPLETING_WEEKS("Mow many weeks?"),
    COMPLETING_MONTHS("Mow many months?"),
    COMPLETING_YEARS("How many years?"),
    PROJECT_DESCRIPTION("Which are the details of this?"),
    REAL_GOAL("How much?"),
    REAL_ADVANCE("How much done?"),
    DISCRETE_GOAL("To what extent?"),
    DISCRETE_ADVANCE("What's the advance?");

    private final String question;

    Questions(String question) {
        this.question = question;
    }

    public String get() {
        return question;
    }
}