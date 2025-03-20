package space.aleph.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class User {
    protected StringProperty name;
    protected IntegerProperty age;
    protected String preference1;
    protected String preference2;

    public User(String name, int age, String preference1, String preference2) {
        this.name = new SimpleStringProperty(name);
        this.age = new SimpleIntegerProperty(age);
        this.preference1 = preference1;
        this.preference2 = preference2;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public IntegerProperty ageProperty() {
        return age;
    }

    public String getPreference1() {
        return preference1;
    }

    public String getPreference2() {
        return preference2;
    }
}