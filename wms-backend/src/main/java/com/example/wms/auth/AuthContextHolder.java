package com.example.wms.auth;

public class AuthContextHolder {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    public static LoginUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getUserId() {
        LoginUser loginUser = CONTEXT.get();
        if (loginUser == null || loginUser.getUserId() == null) {
            return 1L;
        }
        return loginUser.getUserId();
    }
}
