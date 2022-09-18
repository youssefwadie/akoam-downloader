package com.github.youssefwadie.akoamdownloader.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class DownloadService {


    public void download(URI downloadLink, Path downloadPath) throws IOException {
        File file = downloadPath.toFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Unable to create dirs: " + file.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder("aria2c", "-c", "--check-certificate=false",
                "--auto-file-renaming=false", "--summary-interval=0",
                "--dir", downloadPath.toAbsolutePath().toString(),
                downloadLink.toString());

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    public void download(Path downloadLinksFilePath) throws IOException {
        File file = downloadLinksFilePath.toFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Unable to create dirs: " + file.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder("aria2c", "-c", "--check-certificate=false",
                "--auto-file-renaming=false", "--summary-interval=0", "-j1",
                "--dir", downloadLinksFilePath.getParent().toAbsolutePath().toString(),
                "-i", downloadLinksFilePath.toAbsolutePath().toString());

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}
