package com.github.youssefwadie.akoamdownloader.model;

import java.net.URI;

public class SearchResult {
    private final String title;
    private final URI uri;
    private final SearchResultType type;

    public SearchResult(String title, URI uri, SearchResultType type) {
        this.title = title;
        this.uri = uri;
        this.type = type;
    }

    public String title() {
        return title;
    }

    public URI uri() {
        return uri;
    }

    public SearchResultType type() {
        return type;
    }

    public enum SearchResultType {
        MOVIE,
        SERIES
    }
}