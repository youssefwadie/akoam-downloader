package webscraping.akoam.parser;

import lombok.NonNull;
import webscraping.akoam.model.DownloadLinkPagePair;
import webscraping.akoam.model.Movie;
import webscraping.akoam.model.Quality;
import webscraping.akoam.service.ParseService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MovieParser {

    private final Movie movie;

    private final Quality.VideoQuality quality;

    private boolean parsed;

    public MovieParser(@NonNull Movie movie, Quality.VideoQuality quality) {
        this.movie = movie;
        this.quality = quality;
        this.parsed = false;

    }

    public DownloadLinkPagePair parse() throws IOException {
        if (parsed) {
            throw new IllegalStateException("The movie is already parsed");
        }

        ParseService.parseDetails(movie);

        List<Quality> qualityList = ParseService.getQualities(movie.getUri());

        Optional<Quality> first = qualityList
                .stream()
                .filter(q -> q.getVideoQuality() == quality)
                .findFirst();

        Quality fallbackQuality = qualityList.get(0);
        if (first.isEmpty()) {
            System.err.println("Unable to find " + quality + " quality, falling back to: " +
                    fallbackQuality.getVideoQuality().toString());
        }

        DownloadLinkPagePair pair = ParseService.getDownloadLinksPair(first.orElse(fallbackQuality).getUri());

        parsed = true;
        return pair;
    }
}
