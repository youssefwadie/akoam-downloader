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


    @CommandLine.Option(names = {"-w", "--workers"}, description = "number of working processors (maximum is 3/4 of available processors)", defaultValue = "4", paramLabel = "number-of-workers")
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
                        int numberOfWorkers = parser.setNumOfWorkers(this.numberOfWorkers);
                        if (numberOfWorkers != this.numberOfWorkers) {
                            System.out.printf("%s%snumber of workers is %d%s%n", AnsiCodes.BOLD_TEXT, AnsiCodes.YELLOW_TEXT, numberOfWorkers, AnsiCodes.RESET_TEXT);
                        }
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
            String color = getColor(results.get(i).type());
            System.out.format("%s%s%-2d %s%s%s%n", AnsiCodes.BOLD_TEXT, AnsiCodes.PURPLE_TEXT, i + 1, color, results.get(i).title(), AnsiCodes.RESET_TEXT);
        }
        int chosenNumber = -1;
        while (true) {
            System.out.printf("%s%s%s%s ", AnsiCodes.BOLD_TEXT, AnsiCodes.BLUE_TEXT, AnsiCodes.RIGHT_POINTING_MARK, AnsiCodes.RESET_TEXT);
            if (StdIn.hasNextInt()) {
                chosenNumber = StdIn.nextInt();
                StdIn.nextLine();
                if (chosenNumber <= results.size() && chosenNumber > 0) {
                    break;
                } else {
                    System.out.printf("%s%sOut of range%s%n", AnsiCodes.BOLD_TEXT, AnsiCodes.YELLOW_TEXT, AnsiCodes.RESET_TEXT);
                }
            } else {
                String input = StdIn.nextLine();
                if (input.isEmpty()) {
                    System.out.printf("%s%sInvalid input%s%n", AnsiCodes.BOLD_TEXT, AnsiCodes.YELLOW_TEXT, AnsiCodes.RESET_TEXT);
                }
            }
        }
        return results.get(chosenNumber - 1);
    }

    private String getColor(SearchResult.SearchResultType type) {
        if (type == SearchResult.SearchResultType.SERIES) {
            return AnsiCodes.GREEN_TEXT;
        } else {
            return AnsiCodes.CYAN_TEXT;
        }
    }

    private void downloadPrompt(Path downloadLinksFilePath) throws IOException {
        System.out.printf("%sProceed with downloading? [y/N] %s", AnsiCodes.BOLD_TEXT, AnsiCodes.RESET_TEXT);
        String input = StdIn.nextLine().toUpperCase();
        if (input.equals("Y") || input.equals("YES")) {
            downloadService.download(downloadLinksFilePath);
        }
    }
}
