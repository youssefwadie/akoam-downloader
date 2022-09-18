package com.github.youssefwadie.akoamdownloader.cli;

import com.github.youssefwadie.akoamdownloader.ui.AbstractProgressBarWrapper;
import me.tongfei.progressbar.ProgressBar;

public class ProgressBarWrapper extends AbstractProgressBarWrapper<ProgressBar> {
    public ProgressBarWrapper(int elements) {
        super.progressBar = new ProgressBar("Parsed", elements);
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
