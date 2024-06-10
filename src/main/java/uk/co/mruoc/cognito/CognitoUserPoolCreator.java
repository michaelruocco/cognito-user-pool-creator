package uk.co.mruoc.cognito;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TimeUnitsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;
import software.amazon.awssdk.services.cognitoidentityprovider.paginators.ListGroupsIterable;
import software.amazon.awssdk.services.cognitoidentityprovider.paginators.ListUsersIterable;
import uk.co.mruoc.cognito.CognitoUserPoolConfig.UserParams;

@Builder
@Slf4j
public class CognitoUserPoolCreator {

    private final CognitoIdentityProviderClient client;
    private final CognitoUserPoolConfig config;

    public CognitoUserPoolAndClientId create() {
        String poolId = findOrCreateUserPool();
        String clientId = findOrCreateUserPoolClient(poolId);
        findOrCreateGroups(poolId, config.getAllGroups());
        findOrCreateUsers(poolId, clientId);
        return CognitoUserPoolAndClientId.builder()
                .poolId(poolId)
                .clientId(clientId)
                .build();
    }

    private String findOrCreateUserPool() {
        String poolName = config.getUserPoolName();
        return findUserPoolId(poolName).orElseGet(() -> createUserPool(poolName));
    }

    private Optional<String> findUserPoolId(String name) {
        ListUserPoolsRequest request = ListUserPoolsRequest.builder().build();
        ListUserPoolsResponse response = client.listUserPools(request);
        return response.userPools().stream()
                .filter(pool -> pool.name().equals(name))
                .map(UserPoolDescriptionType::id)
                .findFirst();
    }

    private String createUserPool(String poolName) {
        CreateUserPoolRequest request =
                CreateUserPoolRequest.builder().poolName(poolName).build();
        CreateUserPoolResponse response = client.createUserPool(request);
        String poolId = response.userPool().id();
        log.info("created user pool with id {} and name {}", poolId, poolName);
        return poolId;
    }

    private String findOrCreateUserPoolClient(String poolId) {
        String clientName = config.getUserPoolClientName();
        return findUserPoolClientId(poolId, clientName).orElseGet(() -> createUserPoolClient(poolId, clientName));
    }

    private Optional<String> findUserPoolClientId(String poolId, String clientName) {
        ListUserPoolClientsRequest request =
                ListUserPoolClientsRequest.builder().userPoolId(poolId).build();
        ListUserPoolClientsResponse response = client.listUserPoolClients(request);
        return response.userPoolClients().stream()
                .filter(poolClient -> poolClient.clientName().equals(clientName))
                .map(UserPoolClientDescription::clientId)
                .findFirst();
    }

    private String createUserPoolClient(String poolId, String clientName) {
        CreateUserPoolClientRequest request = CreateUserPoolClientRequest.builder()
                .clientName(clientName)
                .userPoolId(poolId)
                .tokenValidityUnits(u -> u.accessToken(TimeUnitsType.HOURS))
                .accessTokenValidity(1)
                .build();
        CreateUserPoolClientResponse response = client.createUserPoolClient(request);
        String clientId = response.userPoolClient().clientId();
        log.info("created user pool client with id {} with name {} in pool with id {}", clientId, clientName, poolId);
        return clientId;
    }

    private void findOrCreateGroups(String poolId, Collection<String> groups) {
        groups.forEach(group -> findOrCreateGroup(poolId, group));
    }

    private void findOrCreateGroup(String poolId, String groupName) {
        Optional<GroupType> group = findGroup(poolId, groupName);
        if (group.isEmpty()) {
            createGroup(poolId, groupName);
        }
    }

    private Optional<GroupType> findGroup(String poolId, String name) {
        ListGroupsRequest request =
                ListGroupsRequest.builder().userPoolId(poolId).build();
        ListGroupsIterable responses = client.listGroupsPaginator(request);
        return responses.stream()
                .map(ListGroupsResponse::groups)
                .flatMap(Collection::stream)
                .filter(group -> group.groupName().equals(name))
                .findFirst();
    }

    private void createGroup(String poolId, String groupName) {
        CreateGroupRequest request = CreateGroupRequest.builder()
                .userPoolId(poolId)
                .groupName(groupName)
                .build();
        CreateGroupResponse response = client.createGroup(request);
        log.info(
                "created group in pool {} with name {}",
                poolId,
                response.group().groupName());
    }

    private void findOrCreateUsers(String poolId, String clientId) {
        config.getUserParams().forEach(params -> findOrCreateUser(poolId, clientId, params));
    }

    private void findOrCreateUser(String poolId, String clientId, UserParams params) {
        if (userExists(poolId, params)) {
            return;
        }
        createUser(poolId, clientId, params);
    }

    private boolean userExists(String poolId, UserParams params) {
        ListUsersRequest request = ListUsersRequest.builder().userPoolId(poolId).build();
        ListUsersIterable responses = client.listUsersPaginator(request);
        return responses.stream()
                .map(ListUsersResponse::users)
                .flatMap(Collection::stream)
                .anyMatch(user -> user.username().equals(params.getUsername()));
    }

    private void createUser(String poolId, String clientId, UserParams params) {
        signUp(clientId, params);
        if (params.isConfirmed()) {
            confirmSignUp(clientId, params.getUsername());
        }
        addUserToGroups(poolId, params);
    }

    private void signUp(String clientId, UserParams params) {
        String username = params.getUsername();
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .username(username)
                .password(params.getPassword())
                .userAttributes(toAttributes(params))
                .build();
        SignUpResponse response = client.signUp(signUpRequest);
        log.info("user {} has been signed up with subject {}", username, response.userSub());
    }

    private void confirmSignUp(String clientId, String username) {
        ConfirmSignUpRequest signUpRequest = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(username)
                .confirmationCode(config.getConfirmationCode())
                .build();
        ConfirmSignUpResponse response = client.confirmSignUp(signUpRequest);
        log.debug("user {} confirmed {}", username, response.sdkHttpResponse().isSuccessful());
    }

    private void addUserToGroups(String poolId, UserParams params) {
        params.getGroups().forEach(group -> addUserToGroup(poolId, params.getUsername(), group));
    }

    private void addUserToGroup(String poolId, String username, String group) {
        AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder()
                .userPoolId(poolId)
                .username(username)
                .groupName(group)
                .build();
        client.adminAddUserToGroup(request);
        log.info("added user {} to group {}", username, group);
    }

    private static List<AttributeType> toAttributes(UserParams params) {
        return List.of(
                subjectAttribute(params.getSubject()),
                givenNameAttribute(params.getGivenName()),
                familyNameAttribute(params.getFamilyName()),
                emailAttribute(params.getEmail()),
                emailVerifiedAttribute(params.isEmailVerified()));
    }

    private static AttributeType subjectAttribute(String givenName) {
        return AttributeType.builder().name("sub").value(givenName).build();
    }

    private static AttributeType givenNameAttribute(String givenName) {
        return AttributeType.builder().name("given_name").value(givenName).build();
    }

    private static AttributeType familyNameAttribute(String familyName) {
        return AttributeType.builder().name("family_name").value(familyName).build();
    }

    private static AttributeType emailAttribute(String email) {
        return AttributeType.builder().name("email").value(email).build();
    }

    private static AttributeType emailVerifiedAttribute(boolean verified) {
        return AttributeType.builder()
                .name("email_verified")
                .value(Boolean.toString(verified))
                .build();
    }
}
