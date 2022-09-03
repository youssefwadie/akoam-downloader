package com.github.youssefwadie.akoamdownloader.cli;

import com.github.youssefwadie.akoamdownloader.model.*;
import picocli.CommandLine;
import com.github.youssefwadie.akoamdownloader.exception.CannotBeScrapedException;
import webscraping.akoam.model.*;
import com.github.youssefwadie.akoamdownloader.parser.MovieParser;
import com.github.youssefwadie.akoamdownloader.parser.SeriesParser;
import com.github.youssefwadie.akoamdownloader.service.ParseService;
import com.github.youssefwadie.akoamdownloader.service.SearchService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

@CommandLine.Command(name = "akoam-downloader", versionProvider = VersionProvider.class)
public class AkoamDownloader implements Runnable {
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


    private final Scanner scanner;

    @CommandLine.Option(names = {"-w", "--workers"}, description = "number of working threads in parsing", defaultValue = "4", paramLabel = "number-of-workers")
    private int numberOfWorkers;

    public AkoamDownloader() {
        this.scanner = Main.scanner;
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
                    MovieParser movieParser = new MovieParser(movie, quality);
                    DownloadLinkPagePair pair = movieParser.parse();
                    ParseService.download(pair.downloadURI(), movie.getDownloadPath());
                } else if (result.type() == SearchResult.SearchResultType.SERIES) {
                    Series series = new Series(result.uri());
                    ParseService.parseDetails(series, startEpisode, endEpisode);

                    SeriesParser seriesParser = new SeriesParser(series, quality, numberOfWorkers, new ProgressBarWrapper(series.getEpisodes().size()));
                    try {
                        seriesParser.parse();
                        Path downloadLinks = seriesParser.writeToDisk(series.getEpisodes());
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
        List<SearchResult> results = SearchService.search(query);
        if (results.size() == 0) {
            throw new CannotBeScrapedException(String.format("No results for: %s was found.", query));
        }

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
            if (scanner.hasNextInt()) {
                chosenNumber = scanner.nextInt();
                scanner.nextLine();
                if (chosenNumber <= results.size() && chosenNumber > 0) {
                    break;
                } else {
                    System.out.printf("%sOut of range: %d%s%n", Colors.ANSI_RED, chosenNumber, Colors.ANSI_RESET);
                }
            } else {
                System.out.printf("%sInvalid: %s%s%n", Colors.ANSI_RED, scanner.nextLine(), Colors.ANSI_RESET);
            }
        }
        return results.get(chosenNumber - 1);
    }

    private void downloadPrompt(Path downloadLinksFilePath) throws IOException {
        System.out.print("Proceed with downloading? [y/N] ");
        String input = scanner.nextLine().toUpperCase();
        if (input.equals("Y") || input.equals("YES")) {
            ParseService.download(downloadLinksFilePath);
        }
    }
}
