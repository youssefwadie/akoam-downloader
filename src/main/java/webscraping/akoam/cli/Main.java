package webscraping.akoam.cli;

import picocli.CommandLine;

import java.util.Scanner;

public class Main {
    public static final Scanner scanner = new Scanner(System.in);


    public static void main(String[] args) {
        try {
            CommandLine cmd = new CommandLine(new AkoamDownloader());
            cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
            cmd.setParameterExceptionHandler(new ParameterMessageHandler());
            int exitCode = cmd.execute(args);
            if (exitCode != 0) {
                System.exit(1);
            }
        } finally {
            scanner.close();
        }
    }
}
