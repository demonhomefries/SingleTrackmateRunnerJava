public class Pair {
    private String first;
    private String second;

    // Constructor
    public Pair(String first, String second) {
        this.first = first;
        this.second = second;
    }

    // Getters
    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    // Setters (optional)
    public void setFirst(String first) {
        this.first = first;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}