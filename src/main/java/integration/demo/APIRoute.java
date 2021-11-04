package integration.demo;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class APIRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        rest("/opportunities")
                .post()
                .consumes("application/json")
                .route()
                .routeId("opportunityPost")
                .setProperty("oppWebhook", simple("${body}"))
                .setProperty("accountId", jsonpath("$.new[0].AccountId"))
                .setProperty("opportunityId", jsonpath("$.new[0].Id"))
                //GET Account Info
                .removeHeaders("*")
                .setBody(constant(""))
                .setHeader("Authorization", simple("{{sf.accessToken}}"))
                .setHeader(Exchange.HTTP_QUERY, simple("q={{sf.account.query}}='${exchangeProperty.accountId}'"))
                .to("vertx-http:{{sf.host}}?httpMethod=GET")
                .setProperty("accountInfo", simple("${body}"))
                //GET line Items Info
                .removeHeaders("*")
                .setBody(constant(""))
                .setHeader("Authorization", simple("{{sf.accessToken}}"))
                .setHeader(Exchange.HTTP_QUERY, simple("q={{sf.lineItems.query}}='${exchangeProperty.opportunityId}'"))
                .to("vertx-http:{{sf.host}}?httpMethod=GET")
                .setProperty("lineItemsInfo", simple("${body}"))
                //TODO Aggregate
                .setBody(simple("webhook :: ${exchangeProperty.oppWebhook} , accountInfo :: ${exchangeProperty.accountInfo}, ItemLinesInfo :: ${exchangeProperty.lineItemsInfo}"))
                .endRest();

    }

  

}
