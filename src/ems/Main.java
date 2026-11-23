package ems;

import ems.gui.LoginFrame;
import ems.gui.MainFrame;
import ems.io.FileHandler;
import ems.service.AuthService;
import ems.service.DepartmentManager;
import ems.service.EmployeeManager;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Entry point: loads the saved data, then shows the login screen. */
public class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            FileHandler fileHandler = new FileHandler(Paths.get("data"));
            DepartmentManager departmentManager = new DepartmentManager(fileHandler);
            EmployeeManager employeeManager = new EmployeeManager(fileHandler, departmentManager);

            List<String> warnings = new ArrayList<>(departmentManager.getLoadWarnings());
            warnings.addAll(employeeManager.getLoadWarnings());
            if (!warnings.isEmpty()) {
                StringBuilder sb = new StringBuilder("Some saved data could not be loaded:\n");
                for (String w : warnings) {
                    sb.append("\n- ").append(w);
                }
                JOptionPane.showMessageDialog(null, sb.toString(), "Data Warning", JOptionPane.WARNING_MESSAGE);
            }
            showLogin(employeeManager, new AuthService());
        });
    }

    /** Nimbus looks modern on every operating system; if it is missing the default look is used. */
    private static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {
            // keep the default look and feel
        }
    }

    /** Shows the login screen; after a successful login opens the main window. Logout comes back here. */
    private static void showLogin(EmployeeManager employeeManager, AuthService authService) {
        LoginFrame[] holder = new LoginFrame[1];
        holder[0] = new LoginFrame(authService, () -> {
            MainFrame main = new MainFrame(employeeManager, holder[0].getLoggedInUser(),
                    () -> showLogin(employeeManager, authService));
            main.setVisible(true);
        });
        holder[0].setVisible(true);
    }
}
