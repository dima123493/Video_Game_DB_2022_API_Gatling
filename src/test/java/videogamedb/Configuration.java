package videogamedb;

import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.jsonFile;

public class Configuration {
    public static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    public static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));
    public static final FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").random();
}
