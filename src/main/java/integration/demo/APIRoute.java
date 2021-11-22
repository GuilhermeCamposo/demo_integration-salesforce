package integration.demo;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
public class APIRoute extends RouteBuilder {



    
    MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    @Override
       public void configure() throws Exception{

        restConfiguration().bindingMode(RestBindingMode.off);

        rest("/opportunities")
              .post()
                .consumes("application/json")
                .route()
                .to("micrometer:timer:opportunity_update_timer?action=start")
                .to("micrometer:counter:opportunity_update")
                .routeId("opportunityPost")
                    .setProperty("oppWebhook", simple("${body}"))
                    .setProperty("accountId", jsonpath("$.new[0].AccountId"))
                    .setProperty("opportunityId", jsonpath("$.new[0].Id"))
                    //GET Account Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.account}}='${exchangeProperty.accountId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .to("micrometer:counter:get_account_details")
                    .setProperty("accountInfo").jsonpathWriteAsString("$.records")
                    //TODO remove this workaround
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            exchange.setProperty("accountInfo", exchange.getProperty("accountInfo", String.class).replace("[", "").replace("]", "") );
                        }
                    })
                    //GET line Items Info
                    .removeHeaders("*")
                    .setBody(constant(""))
                    .setHeader("q", simple("{{sf.query.lineItems}}='${exchangeProperty.opportunityId}'"))
                    .to("salesforce:raw?format=JSON&rawMethod=GET&rawQueryParameters=q&rawPath={{sf.url.path}}")
                    .setProperty("lineItemsInfo").jsonpathWriteAsString("$.records")
                    .setBody(simple(" { \"opportunity\" : ${exchangeProperty.oppWebhook}, \"lineItems\" : ${exchangeProperty.lineItemsInfo},  \"account\" : ${exchangeProperty.accountInfo} }"))
                    .log("Sending -> ${body}")
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                    .to("vertx-http:{{asana.adapter.url}}/asanaAdapter")
        .endRest();

    }

   



}
