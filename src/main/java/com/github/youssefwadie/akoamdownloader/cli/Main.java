package com.github.youssefwadie.akoamdownloader.cli;

import com.github.youssefwadie.akoamdownloader.injector.Container;
import com.github.youssefwadie.akoamdownloader.service.StdIn;
import picocli.CommandLine;


public class Main {

    public static void main(String[] args) {
        try {
            try (Container container = new Container()) {
                AkoamDownloaderCLI akoamDownloaderCLI = container.getBean(AkoamDownloaderCLI.class);
                CommandLine cmd = new CommandLine(akoamDownloaderCLI);
                cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
                cmd.setParameterExceptionHandler(new ParameterMessageHandler());
                int exitCode = cmd.execute(args);
                if (exitCode != 0) {
                    System.exit(1);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            StdIn.close();
        }
    }
}
