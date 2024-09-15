package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Customer {
    private static final FeederBuilder.FileBased<String> loginFeeder = csv("data/loginDetails.csv").circular();

    public static ChainBuilder loadLogin() {
        return feed(loginFeeder)
                .exec(http("Login Page Load")
                        .get("/login")
                        .check(substring("Username:")));
    }

    public static ChainBuilder login() {

        return exec(http("Login")
                .post("/login")
                .formParam("_csrf", "#{csrfValue}")
                .formParam("username", "#{username}")
                .formParam("password", "#{password}"))

                .exec(session -> session.set("customerLoggedIn", true));
                    /*.exec(session -> {
                        log.info("Customer is LoggedIn " + session.get("customerLoggedIn"));
                        return session;
                    });*/
    }

}
