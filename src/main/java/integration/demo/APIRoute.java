package integration.demo;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.jsonpath.JsonPathExpression;
import org.apache.camel.model.rest.RestBindingMode;

public class APIRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception{

        JsonPathExpression jpl = new JsonPathExpression("$.records");
        jpl.setWriteAsString(true);
        jpl.setResultType(String.class);

        restConfiguration().bindingMode(RestBindingMode.off);

        onException(Exception.class)
        .handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .log(LoggingLevel.ERROR,"${exception.message}")
        .setBody(simple("${exception.message} "));

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
                    .setHeader("q", simple("{{sf.query.account}}='${exchangeProperty.accountId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("accountInfo", jpl)
                    .log("AccountInfo -> ${exchangeProperty.accountInfo} ")
                    //GET line Items Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.lineItems}}='${exchangeProperty.opportunityId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("lineItemsInfo").jsonpathWriteAsString("$.records")
                    //POST enriched request to Asana Adapter
                    .to("micrometer:counter:get_account_details")
                    .setBody(simple(" { \"opportunity\" : ${exchangeProperty.oppWebhook}, \"lineItems\" : ${exchangeProperty.lineItemsInfo},  \"account\" : ${exchangeProperty.accountInfo} }"))
                    .log("Sending -> ${body}")
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                    .to("{{asana.adapter.url}}/asanaAdapter")
        .endRest();

    }



}
