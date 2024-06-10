package uk.co.mruoc.cognito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.cognitoidentityprovider.paginators.ListGroupsIterable;
import software.amazon.awssdk.services.cognitoidentityprovider.paginators.ListUsersInGroupIterable;
import software.amazon.awssdk.services.cognitoidentityprovider.paginators.ListUsersIterable;

public class CognitoUserPoolCreatorIntegrationTest {

    @RegisterExtension
    public static final IntegrationTestExtension EXTENSION = new IntegrationTestExtension();

    @Test
    void shouldCreateUserPool() {
        EXTENSION.createUserPool();

        String poolId = EXTENSION.getUserPoolId();

        assertThat(poolId).isNotEmpty();
    }

    @Test
    void shouldCreateUserPoolClient() {
        EXTENSION.createUserPool();

        String clientId = EXTENSION.getUserPoolClientId();

        assertThat(clientId).isNotEmpty();
    }

    @Test
    void shouldCreateGroups() {
        EXTENSION.createUserPool();

        Collection<String> groups = getGroups();

        assertThat(groups).containsExactly("empty-group", "group-1", "group-2");
    }

    @Test
    void shouldCreateUsers() {
        EXTENSION.createUserPool();

        Collection<UserType> users = getUsers();

        assertThat(users).map(UserType::username).containsExactly("user-1", "user-2");
    }

    @Test
    void shouldAddUsersToGroup1() {
        EXTENSION.createUserPool();

        Collection<String> usernames = getUsernamesInGroup("group-1");

        assertThat(usernames).containsExactly("user-1", "user-2");
    }

    @Test
    void shouldAddUsersToGroup2() {
        EXTENSION.createUserPool();

        Collection<String> usernames = getUsernamesInGroup("group-2");

        assertThat(usernames).containsExactly("user-2");
    }

    @Test
    void shouldNotAddUsersToEmptyGroup() {
        EXTENSION.createUserPool();

        Collection<String> usernames = getUsernamesInGroup("empty-group");

        assertThat(usernames).isEmpty();
    }

    @Test
    void shouldCreateUser1Correctly() {
        EXTENSION.createUserPool();

        List<UserType> users = getUsers();

        UserType user = users.get(0);
        assertThat(user.username()).isEqualTo("user-1");
        assertThat(user.enabled()).isTrue();
        assertThat(user.userStatusAsString()).isEqualTo("CONFIRMED");
        List<AttributeType> attributes = user.attributes();
        assertThat(attributes).hasSize(5);
        assertThat(extractSub(attributes)).isEqualTo("707d9fa6-13dd-4985-93aa-a28f01e89a6b");
        assertThat(extractGivenName(attributes)).isEqualTo("User-1");
        assertThat(extractFamilyName(attributes)).isEqualTo("One");
        assertThat(extractEmail(attributes)).isEqualTo("user-1@email.com");
        assertThat(extractEmailVerified(attributes)).isTrue();
    }

    @Test
    void shouldCreateUser2Correctly() {
        EXTENSION.createUserPool();

        List<UserType> users = getUsers();

        UserType user = users.get(1);
        assertThat(user.username()).isEqualTo("user-2");
        assertThat(user.enabled()).isTrue();
        assertThat(user.userStatusAsString()).isEqualTo("UNCONFIRMED");
        List<AttributeType> attributes = user.attributes();
        assertThat(attributes).hasSize(5);
        assertThat(extractSub(attributes)).isEqualTo("dadfde25-9924-4982-802d-dfd0bce2218d");
        assertThat(extractGivenName(attributes)).isEqualTo("User-2");
        assertThat(extractFamilyName(attributes)).isEqualTo("Two");
        assertThat(extractEmail(attributes)).isEqualTo("user-2@email.com");
        assertThat(extractEmailVerified(attributes)).isFalse();
    }

    @Test
    void shouldGenerateTokenForConfirmedUser1() {
        EXTENSION.createUserPool();

        String accessToken = generateAccessToken("user-1", "pwd1");

        assertThat(accessToken).isNotEmpty();
    }

    @Test
    void shouldNotGenerateTokenIfPasswordIsIncorrect() {
        EXTENSION.createUserPool();

        Throwable error = catchThrowable(() -> generateAccessToken("user-1", "incorrect-pwd"));

        assertThat(error).isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void shouldNotGenerateTokenForUnconfirmedUser2() {
        EXTENSION.createUserPool();

        Throwable error = catchThrowable(() -> generateAccessToken("user-2", "pwd2"));

        assertThat(error).isInstanceOf(UserNotConfirmedException.class);
    }

    @Test
    void shouldBeIdempotent() {
        EXTENSION.createUserPool();
        String originalPoolId = EXTENSION.getUserPoolId();
        String originalClientId = EXTENSION.getUserPoolClientId();
        Collection<String> originalGroups = getGroups();
        Collection<UserType> originalUsers = getUsers();

        EXTENSION.forceCreateUserPool();

        assertThat(EXTENSION.getUserPoolId()).isEqualTo(originalPoolId);
        assertThat(EXTENSION.getUserPoolClientId()).isEqualTo(originalClientId);
        assertThat(getGroups()).containsExactlyElementsOf(originalGroups);
        assertThat(getUsers()).containsExactlyElementsOf(originalUsers);
    }

    @Test
    void shouldReturnIssuerUrl() {
        EXTENSION.createUserPool();

        String issuerUrl = EXTENSION.getIssuerUrl();

        String poolId = EXTENSION.getUserPoolId();
        assertThat(issuerUrl).startsWith("http://localhost:").endsWith(String.format("/%s", poolId));
    }

    private static Collection<String> getGroups() {
        ListGroupsRequest request = ListGroupsRequest.builder()
                .userPoolId(EXTENSION.getUserPoolId())
                .build();
        CognitoIdentityProviderClient client = EXTENSION.getIdentityProviderClient();
        ListGroupsIterable responses = client.listGroupsPaginator(request);
        return responses.stream()
                .map(ListGroupsResponse::groups)
                .flatMap(Collection::stream)
                .map(GroupType::groupName)
                .toList();
    }

    private static List<UserType> getUsers() {
        ListUsersRequest request =
                ListUsersRequest.builder().userPoolId(EXTENSION.getUserPoolId()).build();
        CognitoIdentityProviderClient client = EXTENSION.getIdentityProviderClient();
        ListUsersIterable responses = client.listUsersPaginator(request);
        return responses.stream()
                .map(ListUsersResponse::users)
                .flatMap(Collection::stream)
                .toList();
    }

    private static Collection<String> getUsernamesInGroup(String group) {
        ListUsersInGroupRequest request = ListUsersInGroupRequest.builder()
                .userPoolId(EXTENSION.getUserPoolId())
                .groupName(group)
                .build();
        CognitoIdentityProviderClient client = EXTENSION.getIdentityProviderClient();
        ListUsersInGroupIterable responses = client.listUsersInGroupPaginator(request);
        return responses.stream()
                .map(ListUsersInGroupResponse::users)
                .flatMap(Collection::stream)
                .map(UserType::username)
                .toList();
    }

    private static String extractSub(List<AttributeType> attributes) {
        return extractValue("sub", attributes);
    }

    private static String extractGivenName(List<AttributeType> attributes) {
        return extractValue("given_name", attributes);
    }

    private static String extractFamilyName(List<AttributeType> attributes) {
        return extractValue("family_name", attributes);
    }

    private static String extractEmail(List<AttributeType> attributes) {
        return extractValue("email", attributes);
    }

    private static boolean extractEmailVerified(List<AttributeType> attributes) {
        return Boolean.parseBoolean(extractValue("email_verified", attributes));
    }

    private static String extractValue(String name, List<AttributeType> attributes) {
        return attributes.stream()
                .filter(a -> a.name().equals(name))
                .map(AttributeType::value)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format("attribute with name %s not found", name)));
    }

    public static String generateAccessToken(String username, String password) {
        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(EXTENSION.getUserPoolId())
                .clientId(EXTENSION.getUserPoolClientId())
                .authParameters(Map.of("USERNAME", username, "PASSWORD", password))
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .build();
        CognitoIdentityProviderClient client = EXTENSION.getIdentityProviderClient();
        AdminInitiateAuthResponse response = client.adminInitiateAuth(authRequest);
        AuthenticationResultType type = response.authenticationResult();
        return type.accessToken();
    }
}
