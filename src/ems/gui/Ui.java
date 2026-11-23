package ems.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Locale;

/** Shared colours, fonts and small helpers so every screen looks the same. */
final class Ui {
    static final Color PRIMARY = new Color(0x1F3A5F);
    static final Color ACCENT = new Color(0x2E86DE);
    static final Color SUCCESS = new Color(0x27AE60);
    static final Color DANGER = new Color(0xE74C3C);
    static final Color WARNING = new Color(0xF39C12);
    static final Color NEUTRAL = new Color(0x7F8C8D);
    static final Color BACKGROUND = new Color(0xF4F6F8);
    static final Color TEXT = new Color(0x2C3E50);

    private Ui() {
    }

    static Font font(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    // ------------------------------------------------------------------ messages

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    // ------------------------------------------------------------------ building blocks

    /** A coloured title bar shown at the top of every panel. */
    static JPanel header(String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PRIMARY);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel label = new JLabel(title);
        label.setFont(font(Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        bar.add(label, BorderLayout.WEST);
        return bar;
    }

    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 12));
        label.setForeground(TEXT);
        return label;
    }

    static GridBagConstraints cell(int x, int y, int width, double weightX) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = x;
        gc.gridy = y;
        gc.gridwidth = width;
        gc.weightx = weightX;
        gc.insets = new Insets(5, 8, 5, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = weightX > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        return gc;
    }

    static ActionButton button(String text, Color color) {
        return new ActionButton(text, color);
    }

    /** A table with single selection, sortable columns and money formatting for Double columns. */
    static JTable createTable(TableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(font(Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.setRowSorter(new TableRowSorter<>(model));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTableHeader header = table.getTableHeader();
        header.setFont(font(Font.BOLD, 13));
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number ? money(((Number) value).doubleValue()) : "");
            }
        };
        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.setDefaultRenderer(Double.class, moneyRenderer);
        return table;
    }

    /** Sensible column widths for the 8-column employee table (the columns still scale with the window). */
    static void sizeEmployeeColumns(JTable table) {
        int[] widths = {40, 140, 40, 130, 130, 75, 255, 170};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** A flat coloured button. It paints itself, so the colour looks the same on every look and feel. */
    static final class ActionButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color base;

        ActionButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(Math.max(110, fm.stringWidth(getText()) + 36), 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color fill;
            if (!isEnabled()) {
                fill = new Color(0xBDC3C7);
            } else if (getModel().isPressed()) {
                fill = base.darker().darker();
            } else if (getModel().isRollover()) {
                fill = base.darker();
            } else {
                fill = base;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (isFocusOwner()) {
                g2.setColor(new Color(255, 255, 255, 170));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
            }
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}
