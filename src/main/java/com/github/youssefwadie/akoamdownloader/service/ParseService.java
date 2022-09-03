package com.github.youssefwadie.akoamdownloader.service;

import com.github.youssefwadie.akoamdownloader.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.github.youssefwadie.akoamdownloader.exception.CannotBeScrapedException;
import webscraping.akoam.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ParseService {

    public static void parseDetails(Movie movie) throws IOException {
        // parsed before
        if (movie.getName() != null) {
            return;
        }

        String response = RequestsManager.sendGET(movie.getUri());
        Document mainPage = Jsoup.parse(response, movie.getUri().toString());
        movie.setName(mainPage.selectFirst("h1.entry-title").text());
    }

    public static void parseDetails(Series series, int startEpisode, int endEpisode) throws IOException {
        // parsed before
        if (series.getName() != null) {
            return;
        }

        String response = RequestsManager.sendGET(series.getUri());
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


    public static List<Quality> getQualities(URI uri) throws IOException {

        String response = RequestsManager.sendGET(uri);

        Document mainPage = Jsoup.parse(response, uri.toString());

        List<Quality> qualities = new ArrayList<>();

        for (Element container : mainPage.select("div.widget")) {
            if (container.getElementById("downloads") == null) {
                continue;
            }
            Element widgetBody = container.selectFirst("div.widget-body");
            try {
                for (Element qualityTab : widgetBody.select("div.tab-content.quality")) {
                    String qualityTabId = qualityTab.id();
                    URI shortedURI = new URI(qualityTab.selectFirst("a.link-btn.link-download").attr("href"));
                    qualities.add(new Quality(qualityTabId, shortedURI));
                }
            } catch (URISyntaxException e) {
                throw new CannotBeScrapedException("Failed to scrape: " + uri);
            }
        }
        return qualities;
    }

    public static DownloadLinkPagePair getDownloadLinksPair(URI shortenURI) throws IOException {
        URI downloadPage = ParseService.getDownloadPage(shortenURI);
        URI downloadLink = ParseService.getDownloadLink(downloadPage);
        return new DownloadLinkPagePair(downloadPage, downloadLink);
    }


    private static URI getDownloadPage(URI shortenURI) throws IOException {
        if (shortenURI.toString().contains("download")) {
            return shortenURI;
        }

        String respond = RequestsManager.sendGET(shortenURI);
        try {
            Document page = Jsoup.parse(respond);
            Element divContent = page.selectFirst("div.content");
            Element downloadLink = divContent.selectFirst("a.download-link");
            return new URI(downloadLink.attr("href"));
        } catch (NullPointerException | URISyntaxException e) {
            throw new CannotBeScrapedException("Failed to get the download page for: " + shortenURI);
        }
    }

    private static URI getDownloadLink(URI downloadPage) throws IOException {
        String respond = RequestsManager.sendGET(downloadPage);
        try {
            Document page = Jsoup.parse(respond);
            Element downloadButton = page.selectFirst("div.btn-loader");
            Element downloadLink = downloadButton.selectFirst("a.link");
            return new URI(downloadLink.attr("href"));

        } catch (NullPointerException | URISyntaxException e) {
            throw new CannotBeScrapedException("Failed to get the download link for: " + downloadPage);
        }
    }


    public static void download(URI downloadLink, Path downloadPath) throws IOException {
        File file = downloadPath.toFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Unable to create dirs: " + file.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder("aria2c", "-c", "--check-certificate=false",
                "--auto-file-renaming=false", "--summary-interval=0",
                "--dir", downloadPath.toAbsolutePath().toString(),
                downloadLink.toString());

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void download(Path downloadLinksFilePath) throws IOException {
        File file = downloadLinksFilePath.toFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Unable to create dirs: " + file.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder("aria2c", "-c", "--check-certificate=false",
                "--auto-file-renaming=false", "--summary-interval=0", "-j1",
                "--dir", downloadLinksFilePath.getParent().toAbsolutePath().toString(),
                "-i", downloadLinksFilePath.toAbsolutePath().toString());

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}
