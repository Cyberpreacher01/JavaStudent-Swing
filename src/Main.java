import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Main.java
 * Student Course Management System - COS201 Mid-Semester Lab Project.
 * A simple graphical interface (Java Swing) with a numbered menu of the
 * seven required operations.
 *
 * Compile:  javac -d out src/*.java
 * Run:      java -cp out Main
 */
public class Main extends JFrame {

    // ---------- Colours ----------
    private static final Color INK = new Color(0x1C2541);
    private static final Color INK_HOVER = new Color(0x2A3966);
    private static final Color INK_SOFT = new Color(0xA9B6DC);
    private static final Color PAPER = new Color(0xF5F7FB);
    private static final Color RULE = new Color(0xDDE2EC);
    private static final Color MUTED = new Color(0x5D6680);
    private static final Color GOLD = new Color(0xB8862B);
    private static final Color GOLD_SOFT = new Color(0xF6ECD6);
    private static final Color OK = new Color(0x2F6F4F);
    private static final Color ERROR = new Color(0xA8322D);
    private static final Color HOVER = new Color(0xEEF1F7);

    // ---------- Fonts (first one installed on the computer is used) ----------
    private static final String SANS = pickFont("Segoe UI", "Inter", "Helvetica Neue", "Arial", "SansSerif");
    private static final String SERIF = pickFont("Georgia", "Lora", "Cambria", "Serif");

    private static final String DEFAULT_FILE = "courses.txt";
    private static final int MAX_ATTEMPTS = 3;

    private final CourseManager manager = new CourseManager();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final UnitsBar unitsBar;
    private final JLabel totalNumber = new JLabel("0");
    private final JLabel totalNote = new JLabel();
    private final JLabel tableFooter = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private String highlightCode = null;    // course to highlight after Add or Search

    public static void main(String[] args) {
        setUpLookAndFeel();
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    public Main() {
        super("Student Course Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitProgram();          // closing the window works like 7. Exit Program
            }
        });

        unitsBar = new UnitsBar(new Font(SANS, Font.BOLD, 13), new Font(SANS, Font.PLAIN, 12));

        tableModel = new DefaultTableModel(new Object[] {"S/N", "Code", "Title", "Units"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;           // the table is for display only
            }
        };
        table = new JTable(tableModel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAPER);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);

        addKeyboardShortcuts();
        refresh();
        setStatus("Welcome. Choose an option from the menu, or press keys 1 to 7.", true);

        setMinimumSize(new Dimension(820, 580));
        setSize(1100, 680);
        setLocationRelativeTo(null);
    }

    // =====================================================================
    //  MENU SELECTION
    // =====================================================================

    /** Decides which operation to run for the menu option chosen (1 to 7). */
    private void handleMenuChoice(String choice) {
        try {
            switch (choice) {
                case "1": addCourse();      break;
                case "2": viewAllCourses(); break;
                case "3": searchCourse();   break;
                case "4": computeTotal();   break;
                case "5": saveCourses();    break;
                case "6": loadCourses();    break;
                case "7": exitProgram();    break;
                default:
                    showError("'" + choice + "' is not a valid option. Please choose 1 to 7.");
            }
        } catch (Exception e) {
            // Safety net: an unexpected problem is reported instead of crashing the program
            showError("Unexpected error: " + e.getMessage());
        }
    }

    // =====================================================================
    //  THE SEVEN OPERATIONS
    // =====================================================================

    // ---------- 1. Add Course ----------
    private void addCourse() {
        Course course = askForCourse("", "", "", MAX_ATTEMPTS);
        if (course == null) {
            return;     // cancelled or too many invalid attempts
        }
        highlightCode = course.getCode();
        refresh();
        setStatus("Course " + course.getCode() + " - " + course.getTitle()
                + " (" + course.getUnits() + " units) added successfully.", true);
    }

    /** RECURSIVE input dialog: shows the form again, with what was typed,
     *  until the course is valid.
     *  Base cases: the course is added (return it), the user cancels (return null),
     *  or no attempts are left (return null). */
    private Course askForCourse(String code, String title, String units, int attemptsLeft) {
        if (attemptsLeft <= 0) {
            return null;
        }
        JTextField codeField = new JTextField(code, 18);
        JTextField titleField = new JTextField(title, 18);
        JTextField unitsField = new JTextField(units, 18);
        JPanel form = formPanel(
                new String[] {"Course code (e.g. COS201)", "Course title (e.g. Computer Programming I)", "Credit units (1-6)"},
                new JTextField[] {codeField, titleField, unitsField});
        focusWhenShown(codeField);

        int result = JOptionPane.showConfirmDialog(this, form, "1. Add Course",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            setStatus("Add Course cancelled.", true);
            return null;
        }

        try {
            return manager.addCourse(codeField.getText(), titleField.getText(), unitsField.getText());
        } catch (IllegalArgumentException e) {
            int remaining = attemptsLeft - 1;
            if (remaining == 0) {
                showError(e.getMessage() + "\n\nToo many invalid attempts. Returning to the menu.");
                setStatus("Course not added: too many invalid attempts.", false);
                return null;
            }
            showError(e.getMessage() + "\n\nPlease correct it and try again (" + remaining + " attempt(s) left).");
            return askForCourse(codeField.getText(), titleField.getText(), unitsField.getText(), remaining);
        }
    }

    // ---------- 2. View All Courses ----------
    private void viewAllCourses() {
        highlightCode = null;
        refresh();
        if (manager.getCourseCount() == 0) {
            setStatus("No courses recorded yet. Choose 1. Add Course to add one.", false);
        } else {
            setStatus("Showing all " + manager.getCourseCount() + " course(s).", true);
        }
    }

    // ---------- 3. Search Course by Code ----------
    private void searchCourse() {
        String input = JOptionPane.showInputDialog(this, "Enter the course code to search for:",
                "3. Search Course by Code", JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;     // cancelled
        }
        String code = input.trim();
        if (code.isEmpty()) {
            showError("You did not enter a course code.");
            return;
        }

        Course course = manager.searchCourse(code);      // recursive search
        if (course == null) {
            highlightCode = null;
            refresh();
            setStatus("No course with code '" + code.toUpperCase() + "' was found.", false);
            JOptionPane.showMessageDialog(this, "No course with code '" + code.toUpperCase() + "' was found.",
                    "3. Search Course by Code", JOptionPane.WARNING_MESSAGE);
            return;
        }

        highlightCode = course.getCode();
        refresh();
        String found = "Found: " + course.getCode() + " - " + course.getTitle() + " (" + course.getUnits() + " units)";
        setStatus(found, true);
        JOptionPane.showMessageDialog(this, found, "3. Search Course by Code", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- 4. Compute Total Units ----------
    private void computeTotal() {
        int total = manager.totalUnits();                 // recursive total
        int count = manager.getCourseCount();
        refresh();
        String message = "Total credit units: " + total + "\nNumber of courses: " + count;
        setStatus("Total credit units: " + total + " across " + count + " course(s).", true);
        JOptionPane.showMessageDialog(this, message, "4. Compute Total Units", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- 5. Save to File ----------
    private void saveCourses() {
        if (manager.getCourseCount() == 0) {
            showError("Nothing to save - add some courses first.");
            return;
        }
        JFileChooser chooser = fileChooser("5. Save to File");
        chooser.setSelectedFile(new File(DEFAULT_FILE));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            setStatus("Save cancelled.", true);
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(this, "'" + file.getName() + "' already exists. Replace it?",
                    "5. Save to File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                setStatus("Save cancelled.", true);
                return;
            }
        }

        try {
            int count = manager.saveToFile(file.getPath());
            setStatus(count + " course(s) saved to '" + file.getName() + "'.", true);
        } catch (IOException e) {
            showError("Could not save the file:\n" + e.getMessage());
            setStatus("Save failed.", false);
        }
    }

    // ---------- 6. Load from File ----------
    private void loadCourses() {
        if (manager.getCourseCount() > 0) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Loading will replace the courses currently in the list. Continue?",
                    "6. Load from File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                setStatus("Load cancelled.", true);
                return;
            }
        }
        JFileChooser chooser = fileChooser("6. Load from File");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            setStatus("Load cancelled.", true);
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            List<String> skipped = manager.loadFromFile(file.getPath());
            highlightCode = null;
            refresh();
            String message = manager.getCourseCount() + " course(s) loaded from '" + file.getName() + "'.";
            if (skipped.isEmpty()) {
                setStatus(message, true);
            } else {
                setStatus(message + " " + skipped.size() + " bad line(s) skipped.", false);
                JOptionPane.showMessageDialog(this,
                        message + "\n\nThese lines were skipped:\n" + String.join("\n", skipped),
                        "6. Load from File", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            showError(e.getMessage());
            setStatus("Load failed.", false);
        }
    }

    // ---------- 7. Exit Program ----------
    private void exitProgram() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Exit the program? Courses that have not been saved will be lost.",
                "7. Exit Program", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    // =====================================================================
    //  DISPLAY
    // =====================================================================

    /** Redraws the table, the units bar and the totals from the course list. */
    private void refresh() {
        tableModel.setRowCount(0);
        List<Course> courses = manager.getCourses();
        int highlightRow = -1;
        for (int i = 0; i < courses.size(); i++) {                      // iteration
            Course course = courses.get(i);
            tableModel.addRow(new Object[] {i + 1, course.getCode(), course.getTitle(), course.getUnits()});
            if (course.getCode().equals(highlightCode)) {
                highlightRow = i;
            }
        }

        int total = manager.totalUnits();
        int count = manager.getCourseCount();
        totalNumber.setText(String.valueOf(total));
        totalNote.setText((total == 1 ? "credit unit" : "credit units") + " across " + count
                + (count == 1 ? " course" : " courses"));
        tableFooter.setText(count == 0
                ? "No courses yet."
                : "Total: " + total + (total == 1 ? " unit, " : " units, ") + count + (count == 1 ? " course" : " courses"));
        unitsBar.setCourses(courses, highlightCode);

        if (highlightRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(highlightRow, 0, true));
        }
        table.clearSelection();
        table.repaint();
    }

    private void setStatus(String message, boolean ok) {
        statusLabel.setText(message);
        statusLabel.setForeground(ok ? OK : ERROR);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid input", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  BUILDING THE WINDOW
    // =====================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(28, 0));
        header.setBackground(INK);
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 22, 28));

        JLabel title = new JLabel("Student Course Management System");
        title.setFont(new Font(SERIF, Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("COS201 Computer Programming I, mid-semester lab project");
        subtitle.setFont(new Font(SANS, Font.PLAIN, 13));
        subtitle.setForeground(INK_SOFT);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(3));
        titles.add(subtitle);

        totalNumber.setFont(new Font(SERIF, Font.BOLD, 46));
        totalNumber.setForeground(Color.WHITE);
        totalNote.setFont(new Font(SANS, Font.PLAIN, 13));
        totalNote.setForeground(INK_SOFT);
        JPanel totals = new JPanel();
        totals.setOpaque(false);
        totals.setLayout(new BoxLayout(totals, BoxLayout.Y_AXIS));
        totals.add(totalNumber);
        totals.add(totalNote);
        totals.setPreferredSize(new Dimension(230, 84));

        JPanel loadRow = new JPanel(new BorderLayout(24, 0));
        loadRow.setOpaque(false);
        loadRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        loadRow.add(totals, BorderLayout.WEST);
        loadRow.add(unitsBar, BorderLayout.CENTER);

        header.add(titles, BorderLayout.NORTH);
        header.add(loadRow, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        body.add(buildMenu(), BorderLayout.WEST);
        body.add(buildRegister(), BorderLayout.CENTER);
        return body;
    }

    /** The numbered menu: one button per required operation. */
    private JPanel buildMenu() {
        JPanel menu = new JPanel(new BorderLayout());
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)));
        menu.setPreferredSize(new Dimension(242, 10));

        JLabel heading = new JLabel("Menu");
        heading.setFont(new Font(SERIF, Font.BOLD, 18));
        heading.setForeground(INK);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 0));

        String[] options = {"Add Course", "View All Courses", "Search Course by Code",
                            "Compute Total Units", "Save to File", "Load from File", "Exit Program"};
        JPanel buttons = new JPanel(new GridLayout(options.length, 1, 0, 6));
        buttons.setOpaque(false);
        for (int i = 0; i < options.length; i++) {                      // iteration
            buttons.add(menuButton(String.valueOf(i + 1), options[i]));
        }

        JLabel hint = new JLabel("Tip: press keys 1 to 7");
        hint.setFont(new Font(SANS, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(12, 4, 0, 0));

        menu.add(heading, BorderLayout.NORTH);
        menu.add(buttons, BorderLayout.CENTER);
        menu.add(hint, BorderLayout.SOUTH);
        return menu;
    }

    private JButton menuButton(String number, String label) {
        boolean isExit = number.equals("7");
        Color textColour = isExit ? ERROR : INK;
        JLabel numberLabel = new JLabel(number + ".");
        numberLabel.setFont(new Font(SANS, Font.BOLD, 14));
        numberLabel.setForeground(textColour);
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font(SANS, Font.PLAIN, 14));
        textLabel.setForeground(textColour);

        // The button holds two labels: the bold number and the option name
        JButton button = new JButton();
        button.setLayout(new BorderLayout(8, 0));
        button.add(numberLabel, BorderLayout.WEST);
        button.add(textLabel, BorderLayout.CENTER);
        button.getAccessibleContext().setAccessibleName(number + ". " + label);
        button.setActionCommand(number);
        button.addActionListener(e -> handleMenuChoice(e.getActionCommand()));
        button.setBackground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Border normal = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE), BorderFactory.createEmptyBorder(8, 10, 8, 10));
        Border focused = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2), BorderFactory.createEmptyBorder(7, 9, 7, 9));
        button.setBorder(normal);

        Color hoverBackground = isExit ? new Color(0xFBECEB) : HOVER;
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBackground);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                button.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                button.setBorder(normal);
            }
        });
        return button;
    }

    private JPanel buildRegister() {
        JPanel register = new JPanel(new BorderLayout(0, 8));
        register.setBackground(Color.WHITE);
        register.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RULE),
                BorderFactory.createEmptyBorder(16, 18, 12, 18)));

        JLabel heading = new JLabel("Course Register");
        heading.setFont(new Font(SERIF, Font.BOLD, 18));
        heading.setForeground(INK);

        // Table appearance
        table.setFont(new Font(SANS, Font.PLAIN, 14));
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(RULE);
        table.setFillsViewportHeight(true);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new CourseCellRenderer());
        // S/N, Code and Units keep enough room for their contents; Title gets the rest
        setColumnWidth(0, 46, 46);
        setColumnWidth(1, 84, 84);
        setColumnWidth(3, 60, 60);
        table.getColumnModel().getColumn(2).setPreferredWidth(360);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setDefaultRenderer(new HeaderRenderer());
        tableHeader.setPreferredSize(new Dimension(10, 32));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        tableFooter.setFont(new Font(SERIF, Font.BOLD, 14));
        tableFooter.setForeground(INK);
        tableFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, INK),
                BorderFactory.createEmptyBorder(8, 8, 2, 8)));

        register.add(heading, BorderLayout.NORTH);
        register.add(scroll, BorderLayout.CENTER);
        register.add(tableFooter, BorderLayout.SOUTH);
        return register;
    }

    private void setColumnWidth(int column, int min, int max) {
        table.getColumnModel().getColumn(column).setMinWidth(min);
        table.getColumnModel().getColumn(column).setPreferredWidth(min);
        table.getColumnModel().getColumn(column).setMaxWidth(max);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RULE),
                BorderFactory.createEmptyBorder(9, 28, 9, 28)));
        statusLabel.setFont(new Font(SANS, Font.PLAIN, 13));
        bar.add(statusLabel, BorderLayout.CENTER);
        return bar;
    }

    /** Pressing 1 to 7 on the keyboard chooses the same menu options. */
    private void addKeyboardShortcuts() {
        JComponent root = getRootPane();
        for (int i = 1; i <= 7; i++) {
            String choice = String.valueOf(i);
            AbstractAction action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleMenuChoice(choice);
                }
            };
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0), "menu" + i);
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + i, 0), "menu" + i);
            root.getActionMap().put("menu" + i, action);
        }
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    private JPanel formPanel(String[] labels, JTextField[] fields) {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setForeground(MUTED);
            c.gridy = i * 2;
            c.insets = new Insets(i == 0 ? 0 : 10, 0, 4, 0);
            form.add(label, c);
            c.gridy = i * 2 + 1;
            c.insets = new Insets(0, 0, 0, 0);
            fields[i].setFont(new Font(SANS, Font.PLAIN, 14));
            fields[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(RULE), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            form.add(fields[i], c);
        }
        return form;
    }

    /** Puts the typing cursor in the given field when its dialog opens. */
    private void focusWhenShown(JTextField field) {
        field.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                SwingUtilities.invokeLater(field::requestFocusInWindow);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) { }

            @Override
            public void ancestorMoved(AncestorEvent event) { }
        });
    }

    private JFileChooser fileChooser(String title) {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        return chooser;
    }

    private static String pickFont(String... names) {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : names) {
            if (installed.contains(name)) {
                return name;
            }
        }
        return names[names.length - 1];
    }

    /** Uses Java's cross-platform look so the window looks the same on every computer,
     *  and sets one clear font for dialogs and buttons. */
    private static void setUpLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // If this fails, Java's default look is used instead - the program still works
        }
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        FontUIResource font = new FontUIResource(SANS, Font.PLAIN, 13);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("Button.background", Color.WHITE);
    }

    // =====================================================================
    //  TABLE DRAWING
    // =====================================================================

    /** Draws each table cell: padding, bold codes, right-aligned units,
     *  and a gold background on the highlighted course. */
    private class CourseCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            Object code = table.getValueAt(row, 1);
            boolean highlighted = code != null && code.equals(highlightCode);
            setBackground(highlighted ? GOLD_SOFT : Color.WHITE);
            setForeground(column == 0 ? MUTED : INK);
            setFont(new Font(SANS, column == 1 ? Font.BOLD : Font.PLAIN, 14));
            setHorizontalAlignment(column == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            Border padding = BorderFactory.createEmptyBorder(0, 8, 0, 8);
            setBorder(highlighted && column == 0
                    ? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, GOLD), padding)
                    : padding);
            return this;
        }
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(new Font(SANS, Font.BOLD, 13));
            setForeground(MUTED);
            setBackground(Color.WHITE);
            setHorizontalAlignment(column == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, RULE), BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            return this;
        }
    }
}
