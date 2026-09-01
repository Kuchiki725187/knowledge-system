package com.knowledge.common.context;

public class BaseContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private BaseContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void remove() {
        USER_ID.remove();
    }
}
