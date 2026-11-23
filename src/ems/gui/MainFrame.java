package ems.gui;

import ems.service.EmployeeManager;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

/** The main window shown after login: a side menu and a panel area. */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Refreshable> panels = new LinkedHashMap<>();
    private final Runnable onLogout;

    public MainFrame(EmployeeManager manager, String username, Runnable onLogout) {
        super("Employee Management System");
        this.onLogout = onLogout;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setPreferredSize(new Dimension(1150, 760));
        setLayout(new BorderLayout());

        register("dashboard", new DashboardPanel(manager, username, this::showPanel));
        register("add", new AddEmployeePanel(manager));
        register("view", new ViewEmployeesPanel(manager));
        register("search", new SearchUpdatePanel(manager));
        register("departments", new DepartmentPanel(manager));

        add(buildSideMenu(username), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        showPanel("dashboard");
    }

    private void register(String name, Refreshable panel) {
        panels.put(name, panel);
        content.add((JPanel) panel, name);
    }

    private JPanel buildSideMenu(String username) {
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBackground(new Color(0x16294A));
        menu.setPreferredSize(new Dimension(210, 0));
        menu.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));

        JLabel title = new JLabel("<html>Employee<br>Management</html>");
        title.setFont(Ui.font(Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        JLabel user = new JLabel("Logged in as: " + username);
        user.setFont(Ui.font(Font.PLAIN, 12));
        user.setForeground(new Color(0xBDC3C7));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.gridy = 0;
        menu.add(title, gc);
        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 20, 0);
        menu.add(user, gc);
        gc.insets = new Insets(5, 0, 5, 0);

        int row = 2;
        row = addNav(menu, gc, row, "Dashboard", "dashboard", Ui.ACCENT);
        row = addNav(menu, gc, row, "Add Employee", "add", Ui.ACCENT);
        row = addNav(menu, gc, row, "View Employees", "view", Ui.ACCENT);
        row = addNav(menu, gc, row, "Search & Update", "search", Ui.ACCENT);
        row = addNav(menu, gc, row, "Departments", "departments", Ui.ACCENT);

        gc.gridy = row;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        menu.add(new JPanel() {
            private static final long serialVersionUID = 1L;

            {
                setOpaque(false);
            }
        }, gc);

        Ui.ActionButton logout = Ui.button("Log Out", Ui.DANGER);
        logout.setName("logoutButton");
        logout.addActionListener(e -> logout());
        gc.gridy = row + 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        menu.add(logout, gc);
        return menu;
    }

    private int addNav(JPanel menu, GridBagConstraints gc, int row, String text, String target, Color color) {
        Ui.ActionButton button = Ui.button(text, color);
        button.setName("nav-" + target);
        button.addActionListener(e -> showPanel(target));
        gc.gridy = row;
        menu.add(button, gc);
        return row + 1;
    }

    /** Reloads the panel's data, then shows it. */
    void showPanel(String name) {
        Refreshable panel = panels.get(name);
        if (panel == null) {
            return;
        }
        panel.refresh();
        cards.show(content, name);
    }

    private void logout() {
        if (Ui.confirm(this, "Are you sure you want to log out?", "Log Out")) {
            dispose();
            onLogout.run();
        }
    }
}
