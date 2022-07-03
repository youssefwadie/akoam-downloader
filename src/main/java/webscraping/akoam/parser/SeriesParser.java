package webscraping.akoam.parser;

import lombok.RequiredArgsConstructor;
import webscraping.akoam.exception.CannotBeScrapedException;
import webscraping.akoam.model.DownloadLinkPagePair;
import webscraping.akoam.model.Episode;
import webscraping.akoam.model.Quality;
import webscraping.akoam.model.Series;
import webscraping.akoam.ui.AbstractProgressBarWrapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
public class SeriesParser {

    private final Series series;
    private final Quality.VideoQuality quality;
    private final int numOfWorkers;

    private int lastTakenEpisode = 0;

    private final AbstractProgressBarWrapper<?> progressBarWrapper;

    public synchronized Episode getNextEpisode() {
        if (lastTakenEpisode >= series.getEpisodes().size()) {
            return null;
        }
        Episode nextEpisode = series.getEpisodes().get(lastTakenEpisode++);
        progressBarWrapper.updateProgress();
        return nextEpisode;
    }

    public void parse() throws ExecutionException, InterruptedException {
        final List<CompletableFuture<List<DownloadLinkPagePair>>> workers = new ArrayList<>(numOfWorkers);
        CompletableFuture<?>[] arrayOfWorkers = new CompletableFuture<?>[numOfWorkers];

        for (int i = 0; i < numOfWorkers; ++i) {
            workers.add(CompletableFuture.supplyAsync(new SeriesWorker(this, quality)));
        }

        workers.toArray(arrayOfWorkers);
        CompletableFuture<Void> allWorkers = CompletableFuture.allOf(arrayOfWorkers);
        allWorkers.get();

        final List<DownloadLinkPagePair> pairs = new ArrayList<>();

        for (CompletableFuture<List<DownloadLinkPagePair>> worker : workers) {
            pairs.addAll(worker.get());
        }
        progressBarWrapper.close();
    }

    public Path writeToDisk(List<Episode> episodes) throws IOException {
        Path downloadPath = series.getDownloadPath();
        if (!Files.exists(downloadPath)) {
            Files.createDirectories(downloadPath);
        }
        Path downloadPageLinksPath = downloadPath.resolve("./pages.txt");
        Path downloadLinksPath = downloadPath.resolve("./links.txt");

        try (BufferedWriter downloadLinksWriter = Files.newBufferedWriter(downloadLinksPath, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             BufferedWriter downloadPagesWriter = Files.newBufferedWriter(downloadPageLinksPath, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            episodes.sort(Comparator.comparingInt(Episode::number));

            for (Episode episode : episodes) {
                DownloadLinkPagePair pair = episode.getDownloadLinkPagePair();
                downloadPagesWriter.append(String.format("%02d - %s", episode.number(), episode.name())).append('\n');
                downloadPagesWriter.append(pair.downloadPageURI().toString()).append('\n');
                downloadPagesWriter.append("========================================================================\n");

                downloadLinksWriter.append(pair.downloadURI().toString()).append('\n');
            }
        }
        if (episodes.size() == 0) {
            throw new CannotBeScrapedException("NO DOWNLOAD LINKS WAS FOUND!");
        }

        return downloadLinksPath;
    }
}
