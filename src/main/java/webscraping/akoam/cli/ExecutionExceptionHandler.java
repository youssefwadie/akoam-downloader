package webscraping.akoam.cli;

import picocli.CommandLine;

import java.io.PrintWriter;

public class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, CommandLine.ParseResult parseResult) {
        PrintWriter err = cmd.getErr();
        if ("DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"))) {
            err.println(cmd.getColorScheme().stackTraceText(ex));
        }
        err.println(cmd.getColorScheme().errorText(ex.getMessage())); // bold red
        return 1;
    }
}
