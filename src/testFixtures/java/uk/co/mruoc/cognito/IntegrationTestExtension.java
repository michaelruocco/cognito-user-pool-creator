package uk.co.mruoc.cognito;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Slf4j
@RequiredArgsConstructor
public class IntegrationTestExtension implements BeforeAllCallback, AfterAllCallback {

    private boolean started = false;

    private static final TestCognitoContainer COGNITO = new TestCognitoContainer(new DemoCognitoUserPoolConfig());

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        startup();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        shutdown();
    }

    public void createUserPool() {
        COGNITO.createUserPool();
    }

    public void forceCreateUserPool() {
        COGNITO.forceCreateUserPool();
    }

    public String getUserPoolId() {
        return COGNITO.getUserPoolId();
    }

    public String getUserPoolClientId() {
        return COGNITO.getUserPoolClientId();
    }

    public CognitoIdentityProviderClient getIdentityProviderClient() {
        return COGNITO.buildIdentityProviderClient();
    }

    public String getIssuerUrl() {
        return COGNITO.getIssuerUrl();
    }

    private void startup() {
        if (started) {
            log.info("extension already started, no need to start");
            return;
        }
        log.info("starting extension");
        COGNITO.start();
        log.info("extension startup complete");
        started = true;
    }

    private void shutdown() {
        if (!started) {
            log.info("extension not started, no need to shutdown");
            return;
        }
        log.info("shutting down extension");
        COGNITO.stop();
        log.info("extension shutdown complete");
        started = false;
    }
}
