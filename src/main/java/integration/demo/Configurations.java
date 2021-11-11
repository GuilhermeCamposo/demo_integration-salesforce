package integration.demo;

import org.apache.camel.component.salesforce.SalesforceComponent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
public class Configurations {

    @ConfigProperty(name = "sf.client.id")
    String clientId;

    @ConfigProperty(name = "sf.client.secret")
    String clientSecret;

    @ConfigProperty(name = "sf.username")
    String username;

    @ConfigProperty(name = "sf.password")
    String password;

    @ConfigProperty(name = "sf.instance")
    String instance;

    @ConfigProperty(name = "sf.login.url")
    String loginURL;

    @Named
    SalesforceComponent salesforce(){

        SalesforceComponent component = new SalesforceComponent();
        component.setClientId(clientId);
        component.setClientSecret(clientSecret);
        component.setUserName(username);
        component.setPassword(password);
        component.setInstanceUrl(instance);
        component.setLoginUrl(loginURL);

        return component;
    }

}
