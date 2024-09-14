package videogamedb;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import lombok.extern.slf4j.Slf4j;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.internal.HttpCheckBuilders.status;

@Slf4j
public class VideoGameDbSimulation extends Simulation {
    @Override
    public void before() {
        log.info("Running tests with {} users", Configuration.USER_COUNT);
        log.info("Ramping users over {} seconds", Configuration.RAMP_DURATION);
    }

    private HttpProtocolBuilder httpProtocol() {
        return http.baseUrl("https://videogamedb.uk/api")
                .acceptHeader("application/json")
                .contentTypeHeader("application/json");
    }

    private ChainBuilder getAllGames() {
        return exec(http("Get all games")
                .get("/videogame"));
    }

    private ChainBuilder authenticate() {
        return exec(http("Authenticate")
                .post("/authenticate")
                .body(StringBody("""
                        {
                          "password": "admin",
                          "username": "admin"
                        }
                        """))
                .check(status().is(200))
                .check(jmesPath("token").saveAs("jwtToken")));
    }

    private ChainBuilder createNewGame() {
        return feed(Configuration.jsonFeeder)
                .exec(http("Create a new game - #{name}")
                        .post("/videogame")
                        .header("Authorization", "Bearer #{jwtToken}")
                        .body(ElFileBody("bodies/newGameTemplate.json")).asJson());
    }

    private ChainBuilder getLastPostedGame(){
       return exec(http("Get last posted game - #{name}")
                .get("/videogame/#{id}")
                .check(jmesPath("name").isEL("#{name}")));
    }

    private ChainBuilder deleteLastPostedGame(){
       return exec(http("Delete last posted game - #{name}")
                .delete("/videogame/#{id}")
                .header("Authorization", "Bearer #{jwtToken}")
                .check(bodyString().is("Video game deleted")));
    }

    private final ScenarioBuilder scenarioBuilder = scenario("Video Game DB Stress Test")
            .exec(getAllGames())
            .pace(2)
            .exec(authenticate())
            .pace(2)
            .exec(createNewGame())
            .pace(2)
            .exec(getLastPostedGame())
            .pace(2)
            .exec(deleteLastPostedGame());

    private ScenarioBuilder scenarioGetAllGames(){
       return scenario("Video Game DB Get All Games Test")
                .exec(getAllGames());
    }

    {
        setUp(scenarioGetAllGames().injectOpen(atOnceUsers(1)),
                scenarioBuilder.injectOpen(nothingFor(5), rampUsers(Configuration.USER_COUNT).during(Configuration.RAMP_DURATION)))
                .protocols(httpProtocol());
    }

}