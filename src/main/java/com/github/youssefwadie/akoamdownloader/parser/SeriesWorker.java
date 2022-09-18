package com.github.youssefwadie.akoamdownloader.parser;

import com.github.youssefwadie.akoamdownloader.model.DownloadLinkPagePair;
import com.github.youssefwadie.akoamdownloader.model.Episode;
import com.github.youssefwadie.akoamdownloader.model.Quality;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class SeriesWorker implements Supplier<List<DownloadLinkPagePair>> {
    private final Quality.VideoQuality quality;
    private final SeriesWrapper wrapper;
    private final LinksParser linksParser;

    public SeriesWorker(SeriesWrapper wrapper, Quality.VideoQuality quality, LinksParser linksParser) {
        this.quality = quality;
        this.wrapper = wrapper;
        this.linksParser = linksParser;
    }

    @Override
    public List<DownloadLinkPagePair> get() {
        final List<DownloadLinkPagePair> pairs = new ArrayList<>();
        Episode currentEpisode = wrapper.getNextEpisode();
        while (currentEpisode != null) {
            List<Quality> episodeQualities;
            try {
                episodeQualities = linksParser.getQualities(currentEpisode.uri());
                Quality episodeQuality = episodeQualities
                        .stream()
                        .filter(q -> q.getVideoQuality().equals(quality))
                        .findFirst()
                        .orElse(episodeQualities.get(0));

                DownloadLinkPagePair pair = linksParser.getDownloadLinksPair(episodeQuality.getUri());
                pairs.add(pair);
                wrapper.step();
                currentEpisode = wrapper.getNextEpisode();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return pairs;
    }
}
