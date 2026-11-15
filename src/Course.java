/**
 * Course.java
 * Represents one registered course with a code, title and credit units.
 */
public class Course {

    public static final int MIN_UNITS = 1;
    public static final int MAX_UNITS = 6;

    private String code;
    private String title;
    private int units;

    public Course(String code, String title, String units) {
        // Every value is cleaned and validated before it is stored
        this.code = cleanCode(code);
        this.title = cleanTitle(title);
        this.units = cleanUnits(units);
    }

    // ---------- Getters ----------
    public String getCode()  { return code; }
    public String getTitle() { return title; }
    public int getUnits()    { return units; }

    // ---------- Validation / string processing ----------

    /** Trims, removes inner spaces, converts to upper case and checks the
     *  format: 3 letters followed by 3 digits, e.g. COS201. */
    public static String cleanCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Course code cannot be empty.");
        }
        code = code.trim().replace(" ", "").toUpperCase();
        if (!code.matches("[A-Z]{3}[0-9]{3}")) {
            throw new IllegalArgumentException(
                "Course code must be 3 letters followed by 3 digits (e.g. COS201).");
        }
        return code;
    }

    /** Trims, collapses repeated spaces and applies title casing. */
    public static String cleanTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be empty.");
        }
        if (title.contains(",")) {
            throw new IllegalArgumentException("Course title cannot contain commas.");
        }
        String[] words = title.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    /** Converts the text to a whole number and checks that it is in range. */
    public static int cleanUnits(String units) {
        int value;
        try {
            value = Integer.parseInt(units.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Units must be a whole number.");
        }
        if (value < MIN_UNITS || value > MAX_UNITS) {
            throw new IllegalArgumentException(
                "Units must be between " + MIN_UNITS + " and " + MAX_UNITS + ".");
        }
        return value;
    }

    // ---------- File conversion ----------

    /** Converts the course to one line of text for saving, e.g. COS201,Programming I,3 */
    public String toFileLine() {
        return code + "," + title + "," + units;
    }

    /** Creates a Course object from one line read from a file. */
    public static Course fromFileLine(String line) {
        String[] parts = line.trim().split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Badly formatted line: '" + line.trim() + "'");
        }
        return new Course(parts[0], parts[1], parts[2]);
    }

    @Override
    public String toString() {
        return String.format("%-8s %-40s %5d", code, title, units);
    }
}
