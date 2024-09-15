package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Checkout {
    public static ChainBuilder viewCart() {
        return doIf(session -> !session.getBoolean("customerLoggedIn"))
                .then(exec(Customer.loadLogin())
                        .exec(Customer.login()))
                .exec(http("Load Cart Page")
                        .get("/cart/view")
                        .check(css("#grandTotal").isEL("$#{cartTotal}")));
    }

    public static ChainBuilder completeCheckout() {
        return exec(http("Checkout")
                .get("/cart/checkout")
                .check(substring("Thanks for your order! See you soon!")));
    }

}