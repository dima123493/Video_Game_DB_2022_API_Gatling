package videogamedb;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers;
import static io.gatling.javaapi.http.HttpDsl.http;

public class VideoGameDbSimulation extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));

    private static FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").random();

    @Override
    public void before(){
        System.out.printf("Running tests with %d users%n",USER_COUNT);
        System.out.printf("Ramping users over %d seconds%n",RAMP_DURATION);
    }

    private static ChainBuilder getAllGames = exec(http("Get all games")
            .get("/videogame"));

    private static ChainBuilder authenticate = exec(http("Authenticate")
            .post("/authenticate")
            .body(StringBody("""
                    {
                      "password": "admin",
                      "username": "admin"
                    }
                    """))
            .check(jmesPath("token").saveAs("jwtTocken")));

    private static ChainBuilder createNewGame = feed(jsonFeeder)
            .exec(http("Create a new game - #{name}")
            .post("/videogame")
            .header("Authorization", "Bearer #{jwtTocken}")
            .body(ElFileBody("bodies/newGameTemplate.json")).asJson());

    private static ChainBuilder getLastPostedGame = exec(http("Get last posted game - #{name}")
            .get("/videogame/#{id}")
            .check(jmesPath("name").isEL("#{name}")));

    private static ChainBuilder deleteLastPostedGame = exec(http("Delete last posted game - #{name}")
            .delete("/videogame/#{id}")
            .header("Authorization", "Bearer #{jwtTocken}")
            .check(bodyString().is("Video game deleted")));

    private ScenarioBuilder scenarioBuilder = scenario("Video Game DB Stress Test")
            .exec(getAllGames)
            .pace(2)
            .exec(authenticate)
            .pace(2)
            .exec(createNewGame)
            .pace(2)
            .exec(getLastPostedGame)
            .pace(2)
            .exec(deleteLastPostedGame);

    private ScenarioBuilder scenarioGetAllGames = scenario("Video Game DB Stress Test")
            .exec(http("Get all games")
                    .get("/videogame"));

    {
        //setUp(scenarioGetAllGames.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
        //setUp(scenarioBuilder.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
        setUp(scenarioBuilder.injectOpen(nothingFor(5),rampUsers(USER_COUNT).during(RAMP_DURATION))).protocols(httpProtocol);
    }

}
