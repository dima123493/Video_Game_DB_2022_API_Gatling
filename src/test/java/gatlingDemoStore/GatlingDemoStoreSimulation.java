package gatlingDemoStore;

import gatlingDemoStore.pageObjects.Catalog;
import gatlingDemoStore.pageObjects.Checkout;
import gatlingDemoStore.pageObjects.CmsPages;
import gatlingDemoStore.pageObjects.Customer;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

@Slf4j
public class GatlingDemoStoreSimulation extends Simulation {
    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    private static final Duration RAMP_DURATION = Duration.ofSeconds(Integer.parseInt(System.getProperty("RAMP_DURATION", "10")));
    private static final String DOMAIN = "demostore.gatling.io";
    private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("https://" + DOMAIN);

    private static ChainBuilder initSession() {
        return exec(flushCookieJar())
                .exec(session -> session.set("randomNumber", ThreadLocalRandom.current().nextInt()))
                .exec(session -> session.set("customerLoggedIn", false))
                .exec(session -> session.set("cartTotal", 0.00))
                .exec(addCookie(Cookie("sessionId", SessionIdGenerator.random())
                        .withDomain(DOMAIN)));
    }

    private static ChainBuilder goToAllProducts() {
        return exec(http("Visit All Products page")
                .get("/category/all"));
    }

    private static ChainBuilder selectProduct() {
        return exec(http("Select a Product")
                .get("/product/black-and-red-glasses"));
    }

    private static ChainBuilder addProductToCart() {
        return exec(http("Add Product to Cart")
                .get("/cart/add/19"));
    }

    private static ChainBuilder viewCart() {
        return exec(http("View a Cart")
                .get("/cart/view"));
    }

    private static ChainBuilder checkout() {
        return exec(http("Checkout")
                .get("/cart/checkout"));
    }

    private static final ScenarioBuilder scenarioBuilder = scenario("Video Game DB Stress Test")
            .exec(initSession())
            .pace(2)
            .exec(CmsPages.aboutUsPage())
            .pace(2)
            .exec(Catalog.Category.view())
            .pace(2)
            .exec(Catalog.Product.add())
            .pace(2)
            .exec(Checkout.viewCart())
            .pace(2)
            .exec(Checkout.completeCheckout());

    private static class UserJourneys {
        private static final Duration MIN_PAUSE = Duration.ofMillis(100);
        private static final Duration MAX_PAUSE = Duration.ofMillis(500);

        private static ChainBuilder browsStore() {
            return exec(initSession())
                    .exec(Customer.login())
                    .pause(MAX_PAUSE)
                    .exec(CmsPages.aboutUsPage())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .repeat(5)
                    .on(exec(Catalog.Category.view())
                            .pause(MIN_PAUSE, MAX_PAUSE)
                            .exec(Catalog.Product.view())
                    );
        }

        private static ChainBuilder abandonCart() {
            return exec(initSession())
                    .exec(Customer.login())
                    .pause(MAX_PAUSE)
                    .exec(Catalog.Category.view())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.view())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.add());
        }

        private static ChainBuilder completePurchase() {
            return exec(initSession())
                    .exec(Customer.login())
                    .pause(MAX_PAUSE)
                    .exec(Catalog.Category.view())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.view())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Catalog.Product.add())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Checkout.viewCart())
                    .pause(MIN_PAUSE, MAX_PAUSE)
                    .exec(Checkout.completeCheckout());
        }
    }

    private static class Scenarios {
        public static final ScenarioBuilder defaultPurchase = scenario("Default Load Test")
                .during(RAMP_DURATION)
                .on(randomSwitch()
                        .on(Choice.withWeight(75.0, exec(UserJourneys.browsStore())),
                                Choice.withWeight(15.0, exec(UserJourneys.abandonCart())),
                                Choice.withWeight(10.0, exec(UserJourneys.completePurchase()))));
        public static final ScenarioBuilder highPurchase = scenario("High Purchase Load Test")
                .during(RAMP_DURATION)
                .on(randomSwitch()
                        .on(Choice.withWeight(25.0, exec(UserJourneys.browsStore())),
                                Choice.withWeight(25.0, exec(UserJourneys.abandonCart())),
                                Choice.withWeight(50.0, exec(UserJourneys.completePurchase()))));
    }

    {
        //open systems
        /*setUp(scenarioBuilder.injectOpen(
                atOnceUsers(3),
                nothingFor(Duration.ofSeconds(5)),
                rampUsers(10).during(Duration.ofSeconds(20)),
                nothingFor(Duration.ofSeconds(10)),
                constantUsersPerSec(1).during(Duration.ofSeconds(20))

        )).protocols(HTTP_PROTOCOL);*/

        //throttling
        /*setUp(scenarioBuilder.injectOpen(
                constantUsersPerSec(1).during(Duration.ofMinutes(3))
        )).protocols(HTTP_PROTOCOL)
                .throttle(
                        reachRps(10).during(Duration.ofSeconds(30)),
                        holdFor(Duration.ofSeconds(60)),
                        jumpToRps(20),
                        holdFor(Duration.ofSeconds(60))
                )
                .maxDuration(Duration.ofMinutes(3));*/

        //scenario tests
        /*setUp(Scenarios.defaultPurchase.injectOpen(
                rampUsers(USER_COUNT).during(RAMP_DURATION)
        )).protocols(HTTP_PROTOCOL);*/

        //sequential scenario run
        setUp(Scenarios.defaultPurchase
                .injectOpen(rampUsers(USER_COUNT).during(RAMP_DURATION)).protocols(HTTP_PROTOCOL)
                .andThen(Scenarios.highPurchase
                        .injectOpen(rampUsers(USER_COUNT).during(RAMP_DURATION)).protocols(HTTP_PROTOCOL)));
    }
}
