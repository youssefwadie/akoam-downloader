package com.github.youssefwadie.akoamdownloader.cli;

import me.tongfei.progressbar.ProgressBar;
import com.github.youssefwadie.akoamdownloader.ui.AbstractProgressBarWrapper;

public class ProgressBarWrapper extends AbstractProgressBarWrapper<ProgressBar> {
    public ProgressBarWrapper(int elements) {
        super.progressBar = new ProgressBar("Working on", elements);
    }

    @Override
    public void updateProgress() {
        super.progressBar.step();
    }

    @Override
    public void close() {
        progressBar.close();
    }
}
