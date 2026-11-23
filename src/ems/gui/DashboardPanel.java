package ems.gui;

import ems.service.EmployeeManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

/** Home screen: welcome message, quick statistics and navigation shortcuts. */
class DashboardPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final EmployeeManager manager;
    private final JLabel employeesValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel departmentsValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel payrollValue = new JLabel("0.00", SwingConstants.CENTER);

    DashboardPanel(EmployeeManager manager, String username, Consumer<String> navigator) {
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Ui.BACKGROUND);
        add(Ui.header("Dashboard"), BorderLayout.NORTH);

        JLabel welcome = new JLabel("Welcome, " + username + "!");
        welcome.setFont(Ui.font(Font.BOLD, 28));
        welcome.setForeground(Ui.PRIMARY);
        JLabel subtitle = new JLabel("Employee Management System - choose what you want to do.");
        subtitle.setFont(Ui.font(Font.PLAIN, 14));
        subtitle.setForeground(Ui.TEXT);
        JPanel greeting = new JPanel(new GridLayout(2, 1, 0, 4));
        greeting.setOpaque(false);
        greeting.add(welcome);
        greeting.add(subtitle);

        JPanel stats = new JPanel(new GridLayout(1, 3, 20, 0));
        stats.setOpaque(false);
        stats.add(statCard("Employees", employeesValue, Ui.ACCENT));
        stats.add(statCard("Departments", departmentsValue, Ui.SUCCESS));
        stats.add(statCard("Monthly Payroll (EGP)", payrollValue, Ui.WARNING));

        JPanel shortcuts = new JPanel(new GridLayout(2, 2, 15, 15));
        shortcuts.setOpaque(false);
        shortcuts.add(shortcut("Add Employee", Ui.SUCCESS, "add", navigator));
        shortcuts.add(shortcut("View Employees", Ui.ACCENT, "view", navigator));
        shortcuts.add(shortcut("Search & Update", Ui.WARNING, "search", navigator));
        shortcuts.add(shortcut("Departments", Ui.PRIMARY, "departments", navigator));

        JPanel body = new JPanel(new BorderLayout(0, 25));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        body.add(greeting, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(0, 25));
        middle.setOpaque(false);
        middle.add(stats, BorderLayout.NORTH);
        middle.add(shortcuts, BorderLayout.CENTER);
        body.add(middle, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private static JPanel statCard(String title, JLabel value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(5, 0, 0, 0, color),
                BorderFactory.createEmptyBorder(15, 10, 15, 10)));
        value.setFont(Ui.font(Font.BOLD, 30));
        value.setForeground(color);
        JLabel caption = new JLabel(title, SwingConstants.CENTER);
        caption.setFont(Ui.font(Font.PLAIN, 13));
        caption.setForeground(Ui.TEXT);
        card.add(value, BorderLayout.CENTER);
        card.add(caption, BorderLayout.SOUTH);
        return card;
    }

    private static Ui.ActionButton shortcut(String text, Color color, String target, Consumer<String> navigator) {
        Ui.ActionButton button = Ui.button(text, color);
        button.setFont(Ui.font(Font.BOLD, 18));
        button.setName("shortcut-" + target);
        button.addActionListener(e -> navigator.accept(target));
        return button;
    }

    @Override
    public void refresh() {
        employeesValue.setText(String.valueOf(manager.count()));
        departmentsValue.setText(String.valueOf(manager.getDepartmentManager().getAll().size()));
        payrollValue.setText(Ui.money(manager.getTotalPayroll()));
    }
}
