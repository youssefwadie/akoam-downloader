package webscraping.akoam.model;

import lombok.EqualsAndHashCode;
import webscraping.akoam.exception.UnknownQualityException;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;


@EqualsAndHashCode
public class Quality {

    public enum VideoQuality {
        FHD,
        HD,
        SD480,
        SD360,
        SD240;

        @Override
        public String toString() {
            return switch (this) {
                case FHD -> "1080p";
                case HD -> "720p";
                case SD480 -> "480p";
                case SD360 -> "360p";
                case SD240 -> "240p";
            };
        }
    }

    private static final Map<VideoQuality, String> AVAILABLE_QUALITIES = new EnumMap<>(VideoQuality.class) {{
        put(VideoQuality.FHD, "tab-5");
        put(VideoQuality.HD, "tab-4");
        put(VideoQuality.SD480, "tab-3");
        put(VideoQuality.SD360, "tab-2");
        put(VideoQuality.SD240, "tab-1");
    }};


    private VideoQuality videoQuality;
    private URI uri;

    public Quality(String tabNumber, URI uri) {
        this.videoQuality = getQualityForTab(tabNumber);
        this.uri = uri;
    }

    private VideoQuality getQualityForTab(String tabNumber) {
        return AVAILABLE_QUALITIES
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(tabNumber))
                .findFirst()
                .orElseThrow(() -> new UnknownQualityException("Unknown quality: " + tabNumber))
                .getKey();
    }

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public URI getUri() {
        return uri;
    }

}
