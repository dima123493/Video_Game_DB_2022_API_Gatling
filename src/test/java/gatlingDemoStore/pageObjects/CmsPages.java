package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class CmsPages {
    public static ChainBuilder homePage() {
        return exec(http("Get csrf value")
                .get("/")
                .check(regex("<title>Gatling Demo-Store</title>").exists())
                .check(css("#_csrf", "content").saveAs("csrfValue")));
                /*.exec(session -> {
                    String csrfValue = session.getString("csrfValue");
                    System.out.println("Extracted CSRF Value: " + csrfValue);
                    return session;
                });*/
    }

    public static ChainBuilder aboutUsPage() {
        return exec(http("Visit About Us page")
                .get("/about-us"));
    }

}
