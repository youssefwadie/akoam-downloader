package com.github.youssefwadie.akoamdownloader.cli;

import com.github.youssefwadie.akoamdownloader.exception.CannotBeScrapedException;
import com.github.youssefwadie.akoamdownloader.injector.annotations.DependsOn;
import com.github.youssefwadie.akoamdownloader.model.*;
import com.github.youssefwadie.akoamdownloader.parser.Parser;
import com.github.youssefwadie.akoamdownloader.service.DownloadService;
import com.github.youssefwadie.akoamdownloader.service.SearchService;
import com.github.youssefwadie.akoamdownloader.service.StdIn;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@CommandLine.Command(name = "akoam-downloader", versionProvider = VersionProvider.class)
public class AkoamDownloaderCLI implements Runnable {
    @CommandLine.Parameters(arity = "1..", description = "search for a movie or series", paramLabel = "query")
    private final List<String> searchTerm = new ArrayList<>();

    @CommandLine.Option(names = {"-s", "--start"}, description = "start episode to download (tv shows only)", defaultValue = "1", paramLabel = "start-episode")
    private int startEpisode;

    @CommandLine.Option(names = {"-e", "--end"}, description = "last episode to download (tv shows only)", defaultValue = "-1", paramLabel = "end-episode")
    private int endEpisode;

    @CommandLine.Option(names = {"-q", "--quality"}, description = "the quality of video to download", defaultValue = "720p")
    private Quality.VideoQuality quality;

    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "print the version information and exit")
    private boolean versionInfoRequested;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "print this help and exit")
    private boolean usageHelpRequested;


    @CommandLine.Option(names = {"-w", "--workers"}, description = "number of working threads in parsing", defaultValue = "4", paramLabel = "number-of-workers")
    private int numberOfWorkers;

    private final Parser parser;
    private final DownloadService downloadService;
    private final SearchService searchService;

    public AkoamDownloaderCLI(@DependsOn("parser") Parser parser,
                              @DependsOn("downloadService") DownloadService downloadService,
                              @DependsOn("searchService") SearchService searchService) {
        this.downloadService = downloadService;
        this.searchService = searchService;
        this.parser = parser;
    }

    @Override
    public void run() {
        if (usageHelpRequested) {
            CommandLine.usage(this, System.out);
        } else if (versionInfoRequested) {
            System.out.println(Main.class.getPackage().getImplementationTitle()
                    + " v" + Main.class.getPackage().getImplementationVersion());
        } else {
            try {
                SearchResult result = getChoice();
                if (result.type() == SearchResult.SearchResultType.MOVIE) {
                    Movie movie = new Movie(result.uri());
                    DownloadLinkPagePair pair = parser.parserMovie(movie, quality);
                    downloadService.download(pair.downloadURI(), movie.getDownloadPath());
                } else if (result.type() == SearchResult.SearchResultType.SERIES) {
                    try {
                        Series series = new Series(result.uri());
                        parser.setNumOfWorkers(numberOfWorkers);
                        Path downloadLinks = parser.parserSeries(series, quality, startEpisode, endEpisode);
                        downloadPrompt(downloadLinks);
                    } catch (ExecutionException | InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                }

            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SearchResult getChoice() throws URISyntaxException, IOException {
        String query = String.join(" ", searchTerm);
        List<SearchResult> results = searchService.search(query);
        if (results.size() == 0) {
            throw new CannotBeScrapedException(String.format("No results for: %s was found.", query));
        }

        if (results.size() == 1) return results.get(0);


        for (int i = 0; i < results.size(); ++i) {
            String color = switch (results.get(i).type()) {
                case MOVIE -> Colors.ANSI_CYAN;
                case SERIES -> Colors.ANSI_PURPLE;
            };
            System.out.format("%-2d - %s%s%s%n", i + 1, color, results.get(i).title(), Colors.ANSI_RESET);
        }
        int chosenNumber = -1;
        while (true) {
            System.out.printf("%s[+] 1 - %d: %s", Colors.ANSI_PURPLE, results.size(), Colors.ANSI_RESET);
            if (StdIn.hasNextInt()) {
                chosenNumber = StdIn.nextInt();
                StdIn.nextLine();
                if (chosenNumber <= results.size() && chosenNumber > 0) {
                    break;
                } else {
                    System.out.printf("%sOut of range: %d%s%n", Colors.ANSI_RED, chosenNumber, Colors.ANSI_RESET);
                }
            } else {
                System.out.printf("%sInvalid: %s%s%n", Colors.ANSI_RED, StdIn.nextLine(), Colors.ANSI_RESET);
            }
        }
        return results.get(chosenNumber - 1);
    }

    private void downloadPrompt(Path downloadLinksFilePath) throws IOException {
        System.out.print("Proceed with downloading? [y/N] ");
        String input = StdIn.nextLine().toUpperCase();
        if (input.equals("Y") || input.equals("YES")) {
            downloadService.download(downloadLinksFilePath);
        }
    }
}
