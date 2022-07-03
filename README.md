# akoam-downloader
A cli to browse and download movies and tv shows from [akoam](https://akwam.to)

#### To run the application with the [uber jar](/releases), you should have JRE 17+ installed
`$ java -jar <path-to-akoam-downloader-jar>`
#### There's a statically linked build under the [releases](/releases) with GCC 12.1.0 for Linux AMD64 

### Usage
```
    akoam-downloader [operation]
    akoam-downlaoder [options] <query>
    Options:
          -w, --workers=<number-of-workers> number of working threads in parsing
          -q, --quality=<quality>           the quality of video to download(FHD/1080p, HD/720p, SD480/480p, SD360/360p, SD240/240p), falls back to the best quality found.
          -s, --start=<start-episode>       start episode to download (works only for tv shows only, ignored otherwise)
          -e, --end=<end-episode>           last episode to download  (works only for tv shows only, ignored otherwise)
    Operations:
           -v, --version                    print the version information and exit
           -h, --help                       print this help and exit
```