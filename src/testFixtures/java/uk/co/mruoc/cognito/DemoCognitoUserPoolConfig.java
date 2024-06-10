package uk.co.mruoc.cognito;

import java.util.Collection;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DemoCognitoUserPoolConfig implements CognitoUserPoolConfig {

    private final Collection<UserParams> userParams;

    public DemoCognitoUserPoolConfig() {
        this(buildUserParams());
    }

    @Override
    public String getUserPoolName() {
        return "demo-user-pool";
    }

    @Override
    public String getUserPoolClientName() {
        return "demo-user-pool-client";
    }

    @Override
    public String getConfirmationCode() {
        return "9999";
    }

    @Override
    public Collection<UserParams> getUserParams() {
        return userParams;
    }

    @Override
    public Collection<String> getGroups() {
        return List.of("empty-group");
    }

    private static Collection<UserParams> buildUserParams() {
        return List.of(user1(), user2());
    }

    private static UserParams user1() {
        return UserParams.builder()
                .subject("707d9fa6-13dd-4985-93aa-a28f01e89a6b")
                .username("user-1")
                .password("pwd1")
                .givenName("User-1")
                .familyName("One")
                .email("user-1@email.com")
                .emailVerified(true)
                .groups(List.of("group-1"))
                .confirmed(true)
                .build();
    }

    private static UserParams user2() {
        return UserParams.builder()
                .subject("dadfde25-9924-4982-802d-dfd0bce2218d")
                .username("user-2")
                .password("pwd2")
                .givenName("User-2")
                .familyName("Two")
                .email("user-2@email.com")
                .emailVerified(false)
                .groups(List.of("group-1", "group-2"))
                .confirmed(false)
                .build();
    }
}
