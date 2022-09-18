package com.github.youssefwadie.akoamdownloader.parser;

import com.github.youssefwadie.akoamdownloader.cli.Colors;
import com.github.youssefwadie.akoamdownloader.exception.CannotBeScrapedException;
import com.github.youssefwadie.akoamdownloader.injector.annotations.DependsOn;
import com.github.youssefwadie.akoamdownloader.model.*;
import com.github.youssefwadie.akoamdownloader.service.HttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Parser {
    private int numOfWorkers;
    private final LinksParser linksParser;
    private final HttpClient httpClient;

    public Parser(@DependsOn("linksParser") LinksParser linksParser,
                  @DependsOn("httpClient") HttpClient httpClient) {

        this.linksParser = linksParser;
        this.httpClient = httpClient;
        this.numOfWorkers = 4;
    }

    public int getNumOfWorkers() {
        return numOfWorkers;
    }

    public void setNumOfWorkers(int numOfWorkers) {
        this.numOfWorkers = numOfWorkers;
    }

    public DownloadLinkPagePair parserMovie(Movie movie, Quality.VideoQuality quality) throws IOException {
        parseDetails(movie);
        List<Quality> qualityList = linksParser.getQualities(movie.getUri());

        Optional<Quality> first = qualityList
                .stream()
                .filter(q -> q.getVideoQuality() == quality)
                .findFirst();

        Quality fallbackQuality = qualityList.get(0);
        if (first.isEmpty()) {
            System.err.println("Unable to find " + quality + " quality, falling back to: " +
                    fallbackQuality.getVideoQuality().toString());
        }
        return linksParser.getDownloadLinksPair(first.orElse(fallbackQuality).getUri());
    }

    private void parseDetails(Movie movie) throws IOException {
        // parsed before
        if (movie.getName() != null) {
            return;
        }

        String response = httpClient.get(movie.getUri());
        Document mainPage = Jsoup.parse(response, movie.getUri().toString());
        movie.setName(mainPage.selectFirst("h1.entry-title").text());
    }


    public Path parserSeries(Series series, Quality.VideoQuality quality, int startEpisode, int endEpisode) throws IOException,
            ExecutionException,
            InterruptedException {
        parseDetails(series, startEpisode, endEpisode);
        List<DownloadLinkPagePair> pairs = parse(series, quality);
        return writeLinksToDisk(series.getDownloadPath(), series.getEpisodes(), pairs);
    }

    public void parseDetails(Series series, int startEpisode, int endEpisode) throws IOException {
        // parsed before
        if (series.getName() != null) {
            return;
        }

        String response = httpClient.get(series.getUri());
        Document mainPage = Jsoup.parse(response, series.getUri().toString());
        series.setName(mainPage.selectFirst("h1.entry-title").text());
        for (Element episodeEntry : mainPage.select("div.widget-body div.row > div")) {
            try {
                if (episodeEntry.selectFirst("p.entry-date") != null) {
                    int episodeNumber = Integer.parseInt(
                            episodeEntry.selectFirst("img.img-fluid").attr("alt")
                                    .split(":")[0].strip());

                    Element entryDetails = episodeEntry.selectFirst("a");
                    URI episodeURI = new URI(entryDetails.attr("href"));
                    String episodeName = entryDetails.text().split(":")[1].strip();

                    series.addEpisode(new Episode(episodeNumber, episodeName, episodeURI));
                }
            } catch (URISyntaxException e) {
                throw new CannotBeScrapedException("Failed to scrape: " + series.getName());
            }
        }
        List<Episode> episodes = series.getEpisodes();
        if (episodes.size() < startEpisode || startEpisode < 0) {
            throw new IllegalArgumentException("the first episode should be 1 - " + episodes.size());
        }
        if (endEpisode < 0 && endEpisode != -1 || (episodes.size() < endEpisode)) {
            throw new IllegalArgumentException("the end episode is and the end either -1 or positive value smaller than or equal to " + episodes.size());
        }

        if (endEpisode == -1) {
            endEpisode = episodes.size();
        }
        series.setEpisodes(episodes.subList(startEpisode - 1, endEpisode));
    }

    public List<DownloadLinkPagePair> parse(Series series, Quality.VideoQuality quality) throws ExecutionException, InterruptedException {
        final List<CompletableFuture<List<DownloadLinkPagePair>>> workers = new ArrayList<>(numOfWorkers);
        CompletableFuture<?>[] arrayOfWorkers = new CompletableFuture<?>[numOfWorkers];
        SeriesWrapper wrapper = new SeriesWrapper(series);
        for (int i = 0; i < numOfWorkers; ++i) {
            workers.add(CompletableFuture.supplyAsync(new SeriesWorker(wrapper, quality, linksParser)));
        }

        workers.toArray(arrayOfWorkers);
        CompletableFuture<Void> allWorkers = CompletableFuture.allOf(arrayOfWorkers);
        allWorkers.get();

        final List<DownloadLinkPagePair> pairs = new ArrayList<>();

        for (CompletableFuture<List<DownloadLinkPagePair>> worker : workers) {
            pairs.addAll(worker.get());
        }
        return pairs;
    }

    public Path writeLinksToDisk(Path basePath, List<Episode> episodes, List<DownloadLinkPagePair> pairs) throws IOException {
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
        }
        Path downloadPageLinksPath = basePath.resolve("./pages.txt");
        Path downloadLinksPath = basePath.resolve("./links.txt");

        try (BufferedWriter downloadLinksWriter = Files.newBufferedWriter(downloadLinksPath, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             BufferedWriter downloadPagesWriter = Files.newBufferedWriter(downloadPageLinksPath, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            Episode firstEpisode = episodes.get(0);
            episodes.sort(Comparator.comparingInt(Episode::number));
            if (firstEpisode != episodes.get(0)) {
                Collections.reverse(pairs);
            }

            if (episodes.size() != pairs.size()) {
                System.err.printf("%sSome episodes were not parsed.%s%n", Colors.ANSI_RED, Colors.ANSI_RESET);
            }
            for (int i = 0, size = Integer.min(pairs.size(), episodes.size()); i < size; i++) {
                Episode episode = episodes.get(i);
                downloadPagesWriter.append(String.format("%02d - %s", episode.number(), episode.name())).append('\n');
                downloadPagesWriter.append(pairs.get(i).downloadPageURI().toString()).append('\n');
                downloadPagesWriter.append("========================================================================\n");
            }
            for (var pair : pairs) {
                downloadLinksWriter.append(pair.downloadURI().toString()).append('\n');
            }
        }

        return downloadLinksPath;
    }

}
