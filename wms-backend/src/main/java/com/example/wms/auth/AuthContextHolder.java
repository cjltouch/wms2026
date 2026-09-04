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

    public static String getNickName() {
        LoginUser loginUser = CONTEXT.get();
        if (loginUser == null) {
            return "系统";
        }
        if (loginUser.getRealName() != null && !loginUser.getRealName().isEmpty()) {
            return loginUser.getRealName();
        }
        if (loginUser.getNickname() != null && !loginUser.getNickname().isEmpty()) {
            return loginUser.getNickname();
        }
        if (loginUser.getUsername() != null && !loginUser.getUsername().isEmpty()) {
            return loginUser.getUsername();
        }
        return "系统";
    }

    public static String getUserName() {
        LoginUser loginUser = CONTEXT.get();
        if (loginUser == null) {
            return "系统";
        }
        if (loginUser.getUsername() != null && !loginUser.getUsername().isEmpty()) {
            return loginUser.getUsername();
        }
        if (loginUser.getNickname() != null && !loginUser.getNickname().isEmpty()) {
            return loginUser.getNickname();
        }
        return "系统";
    }

}
