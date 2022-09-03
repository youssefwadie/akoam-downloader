package com.github.youssefwadie.akoamdownloader.ui;

public abstract class AbstractProgressBarWrapper<T> implements AutoCloseable {
    protected T progressBar;

    public abstract void updateProgress();

    @Override
    public abstract void close();
}
