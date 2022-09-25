package integration.demo;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class APIRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception{

        restConfiguration().bindingMode(RestBindingMode.auto);

        onException(Exception.class)
        .handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .log(LoggingLevel.ERROR,"${exception.message}")
        .setBody(simple("${exception.message} "));

        rest("/")
            .get("/tracing")
                .route()
                    .routeId("tracing")
                    .to("{{asana.adapter.url}}?bridgeEndpoint=true")
                    .log("trace sent to asana-adapter")
                .endRest()
            .post("/opportunities")
                .consumes("application/json")
                .route()
                    .routeId("postOpportunity")
                    .setProperty("trigger", jsonpath("$"))
                    .log("Trigger  -> ${exchangeProperty.trigger}")
                    .setProperty("accountId", jsonpath("$.new[0].AccountId"))
                    .setProperty("opportunityId", jsonpath("$.new[0].Id"))
                    //GET Account Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.account}}='${exchangeProperty.accountId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("account", jsonpath("$.records[0]"))
                    .log("Account  -> ${exchangeProperty.account}")
                    //GET line Items Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.lineItems}}='${exchangeProperty.opportunityId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("line-items", jsonpath("$.records"))
                    .log("Line Items  -> ${exchangeProperty.line-items}")
                    //POST enriched request to Asana Adapter
                    .to("micrometer:counter:get_account_details")
                    .to("jslt:spec.json?allowContextMapAll=true")
                    .log("Sending -> ${body}")
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                    .to("{{asana.adapter.url}}/asanaAdapter");
    }

}
