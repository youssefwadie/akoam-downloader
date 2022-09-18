package com.github.youssefwadie.akoamdownloader.parser;

import com.github.youssefwadie.akoamdownloader.exception.CannotBeScrapedException;
import com.github.youssefwadie.akoamdownloader.injector.annotations.DependsOn;
import com.github.youssefwadie.akoamdownloader.model.DownloadLinkPagePair;
import com.github.youssefwadie.akoamdownloader.model.Quality;
import com.github.youssefwadie.akoamdownloader.service.HttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

class LinksParser {

    private final HttpClient httpClient;

    LinksParser(@DependsOn("httpClient") HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<Quality> getQualities(URI uri) throws IOException {

        String response = httpClient.get(uri);

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

    public DownloadLinkPagePair getDownloadLinksPair(URI shortenURI) throws IOException {
        URI downloadPage = getDownloadPage(shortenURI);
        URI downloadLink = getDownloadLink(downloadPage);
        return new DownloadLinkPagePair(downloadPage, downloadLink);
    }
    private URI getDownloadPage(URI shortenURI) throws IOException {
        if (shortenURI.toString().contains("download")) {
            return shortenURI;
        }

        String respond = httpClient.get(shortenURI);
        try {
            Document page = Jsoup.parse(respond);
            Element divContent = page.selectFirst("div.content");
            Element downloadLink = divContent.selectFirst("a.download-link");
            return new URI(downloadLink.attr("href"));
        } catch (NullPointerException | URISyntaxException e) {
            throw new CannotBeScrapedException("Failed to get the download page for: " + shortenURI);
        }
    }

    private URI getDownloadLink(URI downloadPage) throws IOException {
        String respond = httpClient.get(downloadPage);
        try {
            Document page = Jsoup.parse(respond);
            Element downloadButton = page.selectFirst("div.btn-loader");
            Element downloadLink = downloadButton.selectFirst("a.link");
            return new URI(downloadLink.attr("href"));

        } catch (NullPointerException | URISyntaxException e) {
            throw new CannotBeScrapedException("Failed to get the download link for: " + downloadPage);
        }
    }
}
