package com.github.youssefwadie.akoamdownloader.service;

import java.util.Scanner;

public final class StdIn {
    private final static Scanner SCANNER = new Scanner(System.in);

    private StdIn() {
    }

    public static boolean hasNextInt() {
        return SCANNER.hasNextInt();
    }

    public static String nextLine() {
        return SCANNER.nextLine();
    }
    public static int nextInt() {
        return SCANNER.nextInt();
    }

    public static void close() {
        SCANNER.close();
    }
}
