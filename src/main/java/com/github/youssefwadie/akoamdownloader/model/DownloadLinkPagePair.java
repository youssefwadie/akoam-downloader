package com.github.youssefwadie.akoamdownloader.model;

import java.net.URI;

public class DownloadLinkPagePair {
    private final URI downloadPageURI;
    private final URI downloadURI;

    public DownloadLinkPagePair(URI downloadPageURI, URI downloadURI) {
        this.downloadPageURI = downloadPageURI;
        this.downloadURI = downloadURI;
    }

    public URI downloadPageURI() {
        return downloadPageURI;
    }

    public URI downloadURI() {
        return downloadURI;
    }
}
