package com.github.youssefwadie.akoamdownloader.service;

import com.github.youssefwadie.akoamdownloader.injector.annotations.DependsOn;
import com.github.youssefwadie.akoamdownloader.model.SearchResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SearchService {
    private final HttpClient httpClient;
    private final URI baseURI;

    public SearchService(@DependsOn("httpClient") HttpClient httpClient) throws URISyntaxException {
        this.httpClient = httpClient;
        this.baseURI = new URI("https://akwam.to");
    }

    public List<SearchResult> search(String searchTerm) throws URISyntaxException, IOException {

        URI queryURI = new URI("/search?q=" + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8));
        URI absoluteURI = baseURI.resolve(queryURI);

        List<SearchResult> searchResults = new ArrayList<>();

        Document searchPage = Jsoup.parse(httpClient.get(absoluteURI));

        for (Element entry : searchPage.select("h3.entry-title a")) {
            String title = entry.text();
            String href = entry.attr("href");

            if (href.contains("series")) {
                searchResults.add(new SearchResult(title, new URI(href), SearchResult.SearchResultType.SERIES));
            } else if (href.contains("movie")) {
                searchResults.add(new SearchResult(title, new URI(href), SearchResult.SearchResultType.MOVIE));
            }
        }

        return searchResults;
    }
}
