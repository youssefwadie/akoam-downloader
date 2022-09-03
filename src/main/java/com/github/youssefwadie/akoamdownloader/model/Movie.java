package com.github.youssefwadie.akoamdownloader.model;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.nio.file.Path;

@Getter
@Setter
public class Movie {
    private String name;
    private final URI uri;

    public Movie(URI uri) {
        this.uri = uri;
        this.name = null;
    }

    public Path getDownloadPath() {
        return Path.of(System.getProperty("user.home"), "Downloads", "akoam", "movies");
    }

}
