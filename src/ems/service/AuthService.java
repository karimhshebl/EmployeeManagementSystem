package ems.service;

import java.util.Arrays;

/** Fixed demonstration login (username: admin, password: admin123). */
public class AuthService {
    private static final String USERNAME = "admin";
    private static final char[] PASSWORD = "admin123".toCharArray();

    public boolean authenticate(String username, char[] password) {
        if (username == null || password == null) {
            return false;
        }
        return USERNAME.equals(username.trim()) && Arrays.equals(PASSWORD, password);
    }
}
