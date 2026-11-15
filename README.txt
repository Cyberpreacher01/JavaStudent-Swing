STUDENT COURSE MANAGEMENT SYSTEM - COS201 Computer Programming I
Java Swing (graphical interface), 100% Java

HOW TO RUN
1. Open a terminal in this project folder.
2. Compile:   javac -d out src/*.java
3. Start:     java -cp out Main
   A window opens with the menu on the left and the course list on the right.

MENU (click a button, or press keys 1 to 7)
1. Add Course   2. View All Courses   3. Search Course by Code
4. Compute Total Units   5. Save to File   6. Load from File   7. Exit Program

Courses are saved as plain text (one course per line, e.g. COS201,Computer Programming I,3),
by default to courses.txt in the project folder.

FILES
src/Course.java          One course + validation (trimming, casing, format checks)
src/CourseManager.java   Course list, recursive search and total, save/load
src/Main.java            The window: menu, course table, dialogs, error messages
src/UnitsBar.java        Draws the bar showing each course's share of the total units
