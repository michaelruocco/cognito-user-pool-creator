package uk.co.mruoc.cognito;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CognitoUserPoolAndClientId {
    private final String poolId;
    private final String clientId;
}
