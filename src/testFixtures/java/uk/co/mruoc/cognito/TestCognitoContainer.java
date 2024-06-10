package uk.co.mruoc.cognito;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Slf4j
public class TestCognitoContainer extends FixedHostPortGenericContainer<TestCognitoContainer> {

    private static final String ACCESS_KEY_ID = "abc";
    private static final String SECRET_ACCESS_KEY = "123";

    private final CognitoUserPoolConfig userPoolConfig;
    private final int port;

    private CognitoUserPoolAndClientId poolAndClientIds;

    public TestCognitoContainer(CognitoUserPoolConfig userPoolConfig) {
        this(userPoolConfig, AvailablePortFinder.findAvailableTcpPort());
    }

    public TestCognitoContainer(CognitoUserPoolConfig userPoolConfig, int port) {
        super("michaelruocco/cognito-local:latest");
        this.userPoolConfig = userPoolConfig;
        withEnv("AWS_ACCESS_KEY_ID", ACCESS_KEY_ID);
        withEnv("AWS_SECRET_ACCESS_KEY", SECRET_ACCESS_KEY);
        withEnv("PORT", Integer.toString(port));
        withEnv("CODE", userPoolConfig.getConfirmationCode());
        withFixedExposedPort(port, port);
        this.port = port;
    }

    public void createUserPool() {
        if (Objects.nonNull(poolAndClientIds)) {
            return;
        }
        forceCreateUserPool();
    }

    public void forceCreateUserPool() {
        CognitoUserPoolCreator creator = CognitoUserPoolCreator.builder()
                .config(userPoolConfig)
                .client(buildIdentityProviderClient())
                .build();
        poolAndClientIds = creator.create();
    }

    public CognitoIdentityProviderClient buildIdentityProviderClient() {
        return CognitoIdentityProviderClient.builder()
                .credentialsProvider(credentialsProvider())
                .endpointOverride(URI.create(getBaseUri()))
                .region(Region.EU_CENTRAL_1)
                .build();
    }

    private static AwsCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.builder()
                .accessKeyId(ACCESS_KEY_ID)
                .secretAccessKey(SECRET_ACCESS_KEY)
                .build();
        return StaticCredentialsProvider.create(credentials);
    }

    public String getIssuerUrl() {
        return String.format("%s/%s", getBaseUri(), getUserPoolId());
    }

    public String getBaseUri() {
        return String.format("http://localhost:%d", getMappedPort(port));
    }

    public String getUserPoolId() {
        return Optional.ofNullable(poolAndClientIds)
                .map(CognitoUserPoolAndClientId::getPoolId)
                .orElseThrow();
    }

    public String getUserPoolClientId() {
        return Optional.ofNullable(poolAndClientIds)
                .map(CognitoUserPoolAndClientId::getClientId)
                .orElseThrow();
    }
}
