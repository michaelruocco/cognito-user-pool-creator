package uk.co.mruoc.cognito;

import java.util.Collection;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;

public interface CognitoUserPoolConfig {

    default Collection<String> getAllGroups() {
        return Stream.concat(getGroups().stream(), getUserGroups().stream())
                .distinct()
                .toList();
    }

    default Collection<String> getUserGroups() {
        return getUserParams().stream()
                .map(UserParams::getGroups)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    String getUserPoolName();

    String getUserPoolClientName();

    Collection<String> getGroups();

    Collection<UserParams> getUserParams();

    String getConfirmationCode();

    @Builder
    @Data
    class UserParams {
        private final String username;
        private final String password;
        private final String subject;
        private final String givenName;
        private final String familyName;
        private final String email;
        private final boolean emailVerified;
        private final Collection<String> groups;
        private final boolean confirmed;
    }
}
