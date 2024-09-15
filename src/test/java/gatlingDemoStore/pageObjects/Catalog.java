package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Catalog {
    private static final FeederBuilder.FileBased<String> categoryFeeder = csv("data/categoryDetails.csv").random();
    private static final FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/productDetails.json").random();

    public static class Category {
        public static ChainBuilder view() {
            return feed(categoryFeeder)
                    .exec(http("Load Category Page #{categoryName}")
                            .get("/category/#{categorySlug}")
                            .check(css("#CategoryName").isEL("#{categoryName}")));
        }
    }

    public static class Product {
        public static ChainBuilder view() {
            return feed(jsonFeeder)
                    .exec(http("Load Product Page #{name}")
                            .get("/product/#{slug}")
                            .check(css("#ProductDescription").isEL("#{description}")));
        }

        public static ChainBuilder add() {
            return exec(view())
                    .exec(http("Add Product to Cart")
                            .get("/cart/add/#{id}")
                            .check(substring("items in your cart")))
                    .exec(session -> {
                        double currentCartTotal = session.getDouble("cartTotal");
                        double itemPrice = session.getDouble("price");
                        return session.set("cartTotal", (currentCartTotal + itemPrice));
                    });
        }
    }

}
