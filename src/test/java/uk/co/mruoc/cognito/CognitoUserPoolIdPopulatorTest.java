package uk.co.mruoc.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CognitoUserPoolIdPopulatorTest {

    private final CognitoUserPoolIdPopulator populator = new CognitoUserPoolIdPopulator("test-pool-id");

    @Test
    void shouldReplacePoolIdInIssuerUrl() {
        String template = "http://localhost:9229/%POOL_ID%";

        String url = populator.replacePoolIdIfRequired(template);

        assertThat(url).isEqualTo("http://localhost:9229/test-pool-id");
    }

    @Test
    void shouldDoNothingIfTemplateStringDoesNotContainPlaceholder() {
        String template = "http://localhost:9229/some-value";

        String url = populator.replacePoolIdIfRequired(template);

        assertThat(url).isEqualTo(template);
    }
}
