import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * UnitsBar.java
 * A small drawing component that shows the semester's credit units as a bar:
 * one block per course, sized by its number of units.
 */
public class UnitsBar extends JPanel {

    private static final Color[] TONES = {
        new Color(0x4A5B94), new Color(0x36467A), new Color(0x5D6FAB), new Color(0x2C3A68)
    };
    private static final Color GOLD = new Color(0xB8862B);
    private static final Color INK = new Color(0x1C2541);
    private static final Color EMPTY_LINE = new Color(0x6D7DB0);
    private static final Color EMPTY_TEXT = new Color(0xA9B6DC);
    private static final int GAP = 3;

    private List<Course> courses = new ArrayList<>();
    private String highlightCode = null;
    private final Font codeFont;
    private final Font unitsFont;

    public UnitsBar(Font codeFont, Font unitsFont) {
        this.codeFont = codeFont;
        this.unitsFont = unitsFont;
        setOpaque(false);
        setPreferredSize(new Dimension(400, 64));
    }

    /** Called by the window whenever the course list changes. */
    public void setCourses(List<Course> courses, String highlightCode) {
        this.courses = new ArrayList<>(courses);
        this.highlightCode = highlightCode;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        if (courses.isEmpty()) {
            g2.setColor(EMPTY_LINE);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
            g2.setFont(unitsFont);
            g2.setColor(EMPTY_TEXT);
            String text = "Courses you add will appear here.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (width - fm.stringWidth(text)) / 2, (height + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
            return;
        }

        int total = 0;
        for (Course course : courses) {
            total += course.getUnits();
        }

        // Each block gets a share of the width that matches its share of the units
        int usable = width - GAP * (courses.size() - 1);
        int x = 0;
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            int blockWidth = (i == courses.size() - 1)
                    ? width - x
                    : Math.round((float) usable * course.getUnits() / total);

            boolean highlighted = course.getCode().equals(highlightCode);
            g2.setColor(highlighted ? GOLD : TONES[i % TONES.length]);
            g2.fillRoundRect(x, 0, blockWidth, height, 8, 8);

            // Write the code and units inside the block if there is room
            g2.setColor(highlighted ? INK : Color.WHITE);
            g2.setFont(codeFont);
            FontMetrics codeMetrics = g2.getFontMetrics();
            String units = course.getUnits() + (course.getUnits() == 1 ? " unit" : " units");
            if (codeMetrics.stringWidth(course.getCode()) + 16 <= blockWidth) {
                g2.drawString(course.getCode(), x + 10, height - 26);
                g2.setFont(unitsFont);
                g2.drawString(units, x + 10, height - 10);
            }
            x += blockWidth + GAP;
        }
        g2.dispose();
    }
}
