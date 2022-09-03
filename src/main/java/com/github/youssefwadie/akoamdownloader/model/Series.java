package com.github.youssefwadie.akoamdownloader.model;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Series {
    private String name;
    private final URI uri;
    private List<Episode> episodes;

    public Series(URI uri) {
        this.episodes = new ArrayList<>();
        this.name = null;
        this.uri = uri;
    }

    public void addEpisode(Episode episode) {
        episodes.add(episode);
    }

    public Path getDownloadPath() {
        return Path.of(System.getProperty("user.home"), "Downloads", "akoam", "tv-shows", name);
    }

}
