# Library Template

[![Build](https://github.com/michaelruocco/cognito-user-pool-creator/workflows/pipeline/badge.svg)](https://github.com/michaelruocco/cognito-user-pool-creator/actions)
[![codecov](https://codecov.io/gh/michaelruocco/cognito-user-pool-creator/branch/master/graph/badge.svg?token=FWDNP534O7)](https://codecov.io/gh/michaelruocco/cognito-user-pool-creator)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/272889cf707b4dcb90bf451392530794)](https://www.codacy.com/gh/michaelruocco/cognito-user-pool-creator/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=michaelruocco/cognito-user-pool-creator&amp;utm_campaign=Badge_Grade)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=michaelruocco_cognito-user-pool-creator&metric=alert_status)](https://sonarcloud.io/dashboard?id=michaelruocco_cognito-user-pool-creator)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=michaelruocco_cognito-user-pool-creator&metric=sqale_index)](https://sonarcloud.io/dashboard?id=michaelruocco_cognito-user-pool-creator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=michaelruocco_cognito-user-pool-creator&metric=coverage)](https://sonarcloud.io/dashboard?id=michaelruocco_cognito-user-pool-creator)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=michaelruocco_cognito-user-pool-creator&metric=ncloc)](https://sonarcloud.io/dashboard?id=michaelruocco_cognito-user-pool-creator)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.michaelruocco/cognito-user-pool-creator.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.michaelruocco%22%20AND%20a:%22cognito-user-pool-creator%22)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

This library contains code to enable Java applications to be able to create cognito user
pools when using this [cognito-local](https://github.com/jagregory/cognito-local) for local
testing. It would be preferable to use [local-stack](https://www.localstack.cloud/) but
unfortunately the AWS Cognito support is not free when using local-stack, so the approach
of using this library with cognito-local offers an alterantive for Java applications that use
Cognito.

## Implementing config

The `CognitoUserPoolConfig` interface is available for you to implement the configuration
you require for your user pool. Only a handful of options within Cognito are configurable
at present, these include:

* User pool name
* User pool client name
* Groups
* Users

## Creating a user pool

Once you have implemented your config class you can create your used pool by creating
and instance of `CognitoUserPoolCreater` passing in your config class and an instance of
`CognitoIdentityProviderClient` that is able to connect to your AWS account (or cognito-local
docker image.) Then you can call the `create()` method to create the user pool based on
the configuration class you have provided. The create method returns an instance of
`CognitoUserPoolAndClientId` which will contain the ids of your newly created cognito
user pool and client, which you can use in your application configuration to connect to
the user pool.

```java
    CognitoUserPoolCreator creator = CognitoUserPoolCreator.builder()
            .config(userPoolConfig)
            .client(buildIdentityProviderClient())
            .build();
    CognitoUserPoolAndClientId poolAndClientIds = creator.create();
    System.out.println(poolAndClientIds.getPoolId());
    System.out.println(poolAndClientIds.getClientId());
```

The `CognitoUserPoolIdPopulator` is also available help you populate the user
pool id into your application config if required. For example, if your application
requires the cognito user pool for access token authentication, then the application
will require an issuer url to be configured, one option is to configure the url with
a placeholder for the pool id e.g. `http://localhost:9229/%POOL_ID%` then you can
populate the pool id returned from the user pool creator in place of the placeholder
e.g.

```java
    CognitoUserPoolCreator creator = CognitoUserPoolCreator.builder()
            .config(userPoolConfig)
            .client(buildIdentityProviderClient())
            .build();
    CognitoUserPoolAndClientId poolAndClientIds = creator.create();
    CognitoUserPoolIdPopulator populator = new CognitoUserPoolIdPopulator(poolAndClientIds.getPoolId());
    String placeholder = "http://localhost:9229/%POOL_ID%";
    System.out.println(populator.replacePoolIdIfRequired(placeholder));
```

In the example code above, if the created pool id was `local_2E5pXlz0` then
the output of `replacePoolIdIfRequired` in the code above would be:
`http://localhost:9229/local_2E5pXlz0`

## Useful Commands

```gradle
// cleans build directories
// checks dependency versions
// checks for gradle issues
// formats code
// builds code
// runs tests
// runs integration tests
./gradlew clean dependencyUpdates criticalLintGradle spotlessApply build integrationTest
```