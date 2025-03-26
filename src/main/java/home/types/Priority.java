package home.types;

public record Priority(Triplet<Integer, String, String> triplet) {
    public int id() { return triplet.first(); }
    public String descriptionEn() { return triplet.second(); }
    public String descriptionEs() { return triplet.third(); }
}