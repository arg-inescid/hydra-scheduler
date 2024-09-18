package org.fakeworker.client;

import org.fakeworker.utils.NetworkUtils;

import java.util.Scanner;

public class Client {

    private static final String ADDRESS = "localhost:5454";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input;
        while (true) {
            input = scanner.nextLine();
            if ("close".equals(input)) {
                break;
            } else if (!input.isEmpty()) {
                NetworkUtils.send(ADDRESS, input, true, System.out::println);
            }
            NetworkUtils.readAvailable(ADDRESS);
        }
        NetworkUtils.closeConnection(ADDRESS);
        scanner.close();
    }
}
