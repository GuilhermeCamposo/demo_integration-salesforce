package integration.demo;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.jsonpath.JsonPathExpression;
import org.apache.camel.model.rest.RestBindingMode;
import java.util.HashMap;
import java.util.Map;

public class APIRoute extends RouteBuilder {

    private static final String TRIGGER_ID = "trigger-request-0e931a24-3a52-42f8-9f5d-3bb6a4cf4add";
    private static final String ACCOUNT_ID = "account-9494ce35-b5c9-4215-b2db-e97c98e2f2cf";
    private static final String LINE_ITEMS_ID = "line-items-65e7bec8-01e3-49e6-be8e-cfbf991a9191";

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
                    .convertBodyTo(String.class)
                    .setProperty("trigger", simple("${body}"))
                    .log("Trigger  -> ${exchangeProperty.trigger}")
                    .setProperty("accountId", jsonpath("$.new[0].AccountId"))
                    .setProperty("opportunityId", jsonpath("$.new[0].Id"))
                    //GET Account Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.account}}='${exchangeProperty.accountId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("account", jpl)
                    .log("Account  -> ${exchangeProperty.account}")
                    //GET line Items Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.lineItems}}='${exchangeProperty.opportunityId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("line-items", jpl)
                    .log("Line Items  -> ${exchangeProperty.account}")
                    //POST enriched request to Asana Adapter
                    .to("micrometer:counter:get_account_details")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            Map<String, Object> docs = new HashMap<String, Object> ();
                            docs.put(TRIGGER_ID, exchange.getProperty("trigger"));
                            docs.put(ACCOUNT_ID, exchange.getProperty("account"));
                            docs.put(LINE_ITEMS_ID, exchange.getProperty("line-items"));

                            exchange.getIn().setBody(docs);
                        }
                    })
                    .to("atlasmap:atlasmap-mapping.adm")
                    .log("Sending -> ${body}")
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                    .to("{{asana.adapter.url}}/asanaAdapter")
        .endRest();

    }



}
