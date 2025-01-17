package com.spring2025.vietchefs.utils;

import java.util.Random;

public class CodeGenerator {
    public static String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(9000) + 1000;
        return String.valueOf(code);
    }
}
