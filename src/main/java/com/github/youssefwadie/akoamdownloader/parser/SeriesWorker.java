package com.github.youssefwadie.akoamdownloader.parser;

import com.github.youssefwadie.akoamdownloader.model.DownloadLinkPagePair;
import com.github.youssefwadie.akoamdownloader.model.Episode;
import com.github.youssefwadie.akoamdownloader.model.Quality;
import com.github.youssefwadie.akoamdownloader.service.ParseService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class SeriesWorker implements Supplier<List<DownloadLinkPagePair>> {
    private final Quality.VideoQuality quality;

    private final SeriesParser parser;

    public SeriesWorker(SeriesParser parser, Quality.VideoQuality quality) {
        this.quality = quality;
        this.parser = parser;
    }

    @Override
    public List<DownloadLinkPagePair> get() {
        final List<DownloadLinkPagePair> pairs = new ArrayList<>();
        Episode currentEpisode = parser.getNextEpisode();
        while (currentEpisode != null) {
            List<Quality> episodeQualities;
            try {
                episodeQualities = ParseService.getQualities(currentEpisode.uri());
                Quality episodeQuality = episodeQualities
                        .stream()
                        .filter(q -> q.getVideoQuality().equals(quality))
                        .findFirst()
                        .orElse(episodeQualities.get(0));
                DownloadLinkPagePair pair = ParseService.getDownloadLinksPair(episodeQuality.getUri());
                pairs.add(pair);
                currentEpisode.setDownloadLinkPagePair(pair);
                currentEpisode = parser.getNextEpisode();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return pairs;
    }
}
