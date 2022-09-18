package com.github.youssefwadie.akoamdownloader.parser;

import com.github.youssefwadie.akoamdownloader.cli.ProgressBarWrapper;
import com.github.youssefwadie.akoamdownloader.model.Episode;
import com.github.youssefwadie.akoamdownloader.model.Series;
import com.github.youssefwadie.akoamdownloader.ui.AbstractProgressBarWrapper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SeriesWrapper {
    private final Series series;
    private final int numberOfEpisodes;
    private final AbstractProgressBarWrapper<?> progressBarWrapper;
    private int lastTakenEpisode;
    private final Lock episodesLock;
    private final Lock progressBarLock;

    public SeriesWrapper(Series series) {
        this.series = series;
        this.lastTakenEpisode = 0;
        this.numberOfEpisodes = series.getEpisodes().size();
        this.progressBarWrapper = new ProgressBarWrapper(series.getEpisodes().size());
        this.episodesLock = new ReentrantLock();
        this.progressBarLock = new ReentrantLock();

    }

    public Episode getNextEpisode() {
        if (lastTakenEpisode >= numberOfEpisodes) {
            progressBarWrapper.close();
            return null;
        }

        episodesLock.lock();
        try {
            return series.getEpisodes().get(lastTakenEpisode++);
        } finally {
            episodesLock.unlock();
        }
    }

    public void step() {
        progressBarLock.lock();
        try {
            progressBarWrapper.updateProgress();
        } finally {
            progressBarLock.unlock();
        }
    }
}
