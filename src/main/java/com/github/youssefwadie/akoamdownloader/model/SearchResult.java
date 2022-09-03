package com.github.youssefwadie.akoamdownloader.model;

import java.net.URI;

public record SearchResult(String title, URI uri, SearchResultType type) {

    public enum SearchResultType {
        MOVIE,
        SERIES
    }
}