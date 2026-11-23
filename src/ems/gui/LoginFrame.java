package ems.gui;

import ems.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;

/** The first screen of the application. */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AuthService authService;
    private final Runnable onSuccess;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private String loggedInUser;

    public LoginFrame(AuthService authService, Runnable onSuccess) {
        super("Employee Management System - Login");
        this.authService = authService;
        this.onSuccess = onSuccess;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        usernameField.setName("usernameField");
        passwordField.setName("passwordField");

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));
        JLabel title = new JLabel("Employee Management System", SwingConstants.CENTER);
        title.setFont(Ui.font(Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Please log in to continue", SwingConstants.CENTER);
        subtitle.setFont(Ui.font(Font.PLAIN, 13));
        subtitle.setForeground(new Color(0xD0D8E4));
        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.BACKGROUND);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        form.add(Ui.label("Username"), Ui.cell(0, 0, 1, 0));
        form.add(usernameField, Ui.cell(1, 0, 1, 1));
        form.add(Ui.label("Password"), Ui.cell(0, 1, 1, 0));
        form.add(passwordField, Ui.cell(1, 1, 1, 1));
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        buttons.setBackground(Ui.BACKGROUND);
        Ui.ActionButton loginButton = Ui.button("Login", Ui.SUCCESS);
        loginButton.setName("loginButton");
        loginButton.addActionListener(e -> login());
        Ui.ActionButton exitButton = Ui.button("Exit", Ui.DANGER);
        exitButton.setName("exitButton");
        exitButton.addActionListener(e -> System.exit(0));
        buttons.add(loginButton);
        buttons.add(exitButton);
        add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton); // Enter key logs in
        pack();
        setSize(Math.max(getWidth(), 460), getHeight());
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /** The username that was accepted (valid after onSuccess has been called). */
    public String getLoggedInUser() {
        return loggedInUser;
    }

    private void login() {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();
        boolean ok = authService.authenticate(username, password);
        Arrays.fill(password, '\0');
        if (ok) {
            loggedInUser = username.trim();
            dispose();
            onSuccess.run();
        } else {
            Ui.error(this, "Incorrect username or password. Please try again.");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}
