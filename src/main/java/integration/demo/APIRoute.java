package integration.demo;

import org.apache.camel.builder.RouteBuilder;

public class APIRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

      rest("/v1/ingestor")
      .post()
      .to("direct:postRoute");


      from("direct:postRoute")
      .log("received body: ${body} ");

    }

  

}
