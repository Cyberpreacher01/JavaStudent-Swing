import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CourseManager.java
 * Stores the list of Course objects and performs the operations on them:
 * add, view, search, total units, save and load.
 */
public class CourseManager {

    private ArrayList<Course> courses = new ArrayList<>();   // internal structure

    public int getCourseCount() {
        return courses.size();
    }

    // ---------- 1. Add Course ----------
    public Course addCourse(String code, String title, String units) {
        Course course = new Course(code, title, units);   // may throw IllegalArgumentException
        if (searchCourse(course.getCode()) != null) {
            throw new IllegalArgumentException("Course " + course.getCode() + " has already been added.");
        }
        courses.add(course);
        return course;
    }

    // ---------- 2. View All Courses ----------
    /** Returns a read-only view of the list, used by the window to fill its table. */
    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    // ---------- 3. Search Course by Code (RECURSIVE) ----------
    public Course searchCourse(String code) {
        String cleaned = code.trim().replace(" ", "").toUpperCase();
        return searchCourse(cleaned, 0);
    }

    /** Base cases: end of list (not found) or a match.
     *  Recursive case: check the next position. */
    private Course searchCourse(String code, int index) {
        if (index >= courses.size()) {
            return null;
        }
        if (courses.get(index).getCode().equals(code)) {
            return courses.get(index);
        }
        return searchCourse(code, index + 1);
    }

    // ---------- 4. Compute Total Units (RECURSIVE) ----------
    public int totalUnits() {
        return totalUnits(0);
    }

    /** Base case: past the last course, return 0.
     *  Recursive case: this course's units + total of the remaining courses. */
    private int totalUnits(int index) {
        if (index >= courses.size()) {
            return 0;
        }
        return courses.get(index).getUnits() + totalUnits(index + 1);
    }

    // ---------- 5. Save to File ----------
    public int saveToFile(String filename) throws IOException {
        // try-with-resources closes the file automatically, even if an error occurs
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Course course : courses) {
                writer.write(course.toFileLine());
                writer.newLine();
            }
        }
        return courses.size();
    }

    // ---------- 6. Load from File ----------
    /** Loads courses from a file. Bad lines are skipped and reported so that
     *  one damaged line does not stop the whole file from loading.
     *  Returns the list of skipped-line messages. */
    public List<String> loadFromFile(String filename) throws IOException {
        ArrayList<Course> loaded = new ArrayList<>();
        ArrayList<String> skipped = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    Course course = Course.fromFileLine(line);
                    for (Course c : loaded) {
                        if (c.getCode().equals(course.getCode())) {
                            throw new IllegalArgumentException("Duplicate code " + course.getCode());
                        }
                    }
                    loaded.add(course);
                } catch (IllegalArgumentException e) {
                    skipped.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            throw new IOException("File '" + filename + "' was not found or cannot be opened.");
        }

        courses = loaded;     // replace the current list only after a successful read
        return skipped;
    }
}
