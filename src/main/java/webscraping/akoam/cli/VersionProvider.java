package webscraping.akoam.cli;

import lombok.NoArgsConstructor;
import picocli.CommandLine;

@NoArgsConstructor
public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        return new String[]{"${COMMAND-FULL-NAME} v" + getClass().getPackage().getImplementationVersion()};
    }
}
