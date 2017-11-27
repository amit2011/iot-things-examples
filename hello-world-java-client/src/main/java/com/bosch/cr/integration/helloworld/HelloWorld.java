/*
 * Bosch SI Example Code License Version 1.0, January 2016
 *
 * Copyright 2017 Bosch Software Innovations GmbH ("Bosch SI"). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * BOSCH SI PROVIDES THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE
 * QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
 * NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH BOSCH
 * SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT LIMITATION FOR
 * INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF THE GERMAN
 * PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN UNAFFECTED
 * BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF LIABILITY
 * ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S EMPLOYEES,
 * REPRESENTATIVES AND ORGANS.
 */
package com.bosch.cr.integration.helloworld;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.auth.AuthorizationSubject;
import org.eclipse.ditto.model.things.AclEntry;
import org.eclipse.ditto.model.things.Feature;
import org.eclipse.ditto.model.things.Permission;
import org.eclipse.ditto.model.things.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.iot.things.client.ThingsClientFactory;
import com.bosch.iot.things.client.configuration.CommonConfiguration;
import com.bosch.iot.things.client.configuration.CredentialsAuthenticationConfiguration;
import com.bosch.iot.things.client.configuration.PublicKeyAuthenticationConfiguration;
import com.bosch.iot.things.client.messaging.MessagingProviders;
import com.bosch.iot.things.client.messaging.ThingsWsMessagingProviderConfiguration;
import com.bosch.iot.things.clientapi.ThingsClient;
import com.bosch.iot.things.clientapi.things.FeatureHandle;
import com.bosch.iot.things.clientapi.twin.Twin;

/**
 * This example shows how to create and use the Java Integration Client for managing your first Hello World Thing.
 */
public class HelloWorld {

    // Insert your Solution ID here
    private static final String SOLUTION_ID = "<your-solution-id>";
    private static final String CLIENT_ID = SOLUTION_ID + ":connector";
    private static final String SOLUTION_API_TOKEN = "<your-solution-api-token-registered-in-things-service>";
    private static final String SOLUTION_DEFAULT_NAMESPACE = "com.your.namespace";
    private static final String USER_ID = "<UUID-of-your-user>";

    // Authentication credentials for Thing Websocket channel
    private static final String USER_NAME = "<your-user-name";
    private static final String PASSWORD = "<your-password>";

    // Insert your keystore passwords here
    private static final URL KEYSTORE_LOCATION = HelloWorld.class.getResource("/CRClient.jks");
    private static final String KEYSTORE_PASSWORD = "<your-keystore-password>";
    private static final String ALIAS = "CR";
    private static final String ALIAS_PASSWORD = "<your-alias-password>";

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class);

    private static final int TIMEOUT = 5;
    private static final String NAMESPACE = "com.bosch.example:";
    private static final String THING_ID = NAMESPACE + UUID.randomUUID().toString();
    private static final String COUNTER = "counter";
    private static final String COUNTER_VALUE = "value";

    // optionally configure a proxy server
    // public static final String PROXY_HOST = "proxy.server.com";
    // public static final int PROXY_PORT = 8080;

    final ThingsClient integrationClient;
    private final Twin twin;

    /**
     * Client instantiation
     */
    public HelloWorld() {
        // Build a credential authentication configuration if you want to directly connect to the IoT Things service
        // via its websocket channel
        final CredentialsAuthenticationConfiguration credentialsAuthenticationConfiguration =
                CredentialsAuthenticationConfiguration
                        .newBuilder()
                        .username(USER_NAME)
                        .password(PASSWORD)
                        .build();

        // or alternatively, build a key-based authentication configuration for communicating with IoT Things service
        final PublicKeyAuthenticationConfiguration publicKeyAuthenticationConfiguration =
                PublicKeyAuthenticationConfiguration
                        .newBuilder()
                        .clientId(CLIENT_ID)
                        .keyStoreLocation(KEYSTORE_LOCATION)
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .alias(ALIAS)
                        .aliasPassword(ALIAS_PASSWORD)
                        .build();

        final ThingsWsMessagingProviderConfiguration thingsWsMessagingProviderConfiguration = MessagingProviders
                .thingsWebsocketProviderBuilder()
                .authenticationConfiguration(credentialsAuthenticationConfiguration /* or publicKeyAuthenticationConfiguration */)
                .build();

        // Optionally configure a proxy server
        /*final ProxyConfiguration proxyConfiguration = ProxyConfiguration.newBuilder()
                .proxyHost(PROXY_HOST)
                .proxyPort(PROXY_PORT)
                .build();*/

        final CommonConfiguration twinConfiguration = ThingsClientFactory.configurationBuilder()
                .apiToken(SOLUTION_API_TOKEN)
                .defaultNamespace(SOLUTION_DEFAULT_NAMESPACE)
                .providerConfiguration(thingsWsMessagingProviderConfiguration)
                //.proxyConfiguration(proxyConfiguration)
                .build();

        LOGGER.info("Creating integration client ...");

        // Create a new integration client object to start interacting with IoT Things service
        integrationClient = ThingsClientFactory.newInstance(twinConfiguration);

        // Create a new twin client for managing things
        twin = integrationClient.twin();
    }

    public static void main(final String... args) {
        new HelloWorld().execute();
    }

    /**
     * Create and update a thing with the java client.
     */
    private void execute() {
        try {
            // Create a Thing with a counter Feature and get the FeatureHandle
            final FeatureHandle counter = createThingWithCounter();

            // Update the ACL with your User ID to see your thing in the Demo Web UI
            updateACL();

            // Log full Thing info (as JSON)
            LOGGER.info("Thing looks like this: {}", getThingById(THING_ID).toJson());

            // Loop to update the attributes of the Thing
            for (int i = 0; i <= 100; i++) {
                updateCounter(counter, i);
                Thread.sleep(2000);
            }

            // This step must always be concluded to terminate the Java client.
            terminate();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Create a {@code Thing} with the counter {@code Feature}. Blocks until Thing has been created.
     *
     * @return a handle for the counter.
     */
    public FeatureHandle createThingWithCounter() {
        final Thing thing = Thing.newBuilder() //
                .setId(THING_ID) //
                .setFeature(Feature.newBuilder() //
                        .properties(JsonObject.newBuilder() //
                                .set(COUNTER_VALUE, 0) //
                                .build()) //
                        .withId(COUNTER) //
                        .build()) //
                .build();

        FeatureHandle featureHandle = null;

        try {
            featureHandle = twin.create(thing) //
                    .thenApply(created -> twin.forFeature(THING_ID, COUNTER)) //
                    .get(TIMEOUT, TimeUnit.SECONDS);

            LOGGER.info("Thing with ID '{}' created.", THING_ID);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error(e.getMessage());
        }

        return featureHandle;
    }

    /**
     * Find a Thing with given ThingId. Blocks until the Thing has been retrieved.
     */
    public Thing getThingById(final String thingId) throws InterruptedException, ExecutionException, TimeoutException {
        return twin.forId(thingId).retrieve().get(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Delete a Thing.
     */
    public void deleteThing(final String thingId) throws InterruptedException, ExecutionException, TimeoutException {
        twin.delete(thingId) //
                .whenComplete((aVoid, throwable) -> {
                    if (null == throwable) {
                        LOGGER.info("Thing with ID deleted: {}", thingId);
                    } else {
                        LOGGER.error(throwable.getMessage());
                    }
                }) //
                .get(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Update the ACL of a specified Thing. Blocks until ACL has been updated.
     */
    public void updateACL() throws InterruptedException, ExecutionException, TimeoutException {
        twin.forId(THING_ID) //
                .retrieve() //
                .thenCompose(thing -> {
                    final AclEntry aclEntry = AclEntry.newInstance(AuthorizationSubject.newInstance(USER_ID), //
                            Permission.READ, //
                            Permission.WRITE, //
                            Permission.ADMINISTRATE);

                    final Thing updated = thing.setAclEntry(aclEntry);
                    return twin.update(updated);
                }) //
                .whenComplete((aVoid, throwable) -> {
                    if (null == throwable) {
                        LOGGER.info("Thing with ID '{}' updated ACL entry!", THING_ID);
                    } else {
                        LOGGER.error(throwable.getMessage());
                    }
                }).get(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Update {@code counter} with {@code value}. Method does not block but returns as soon as the update has been
     * triggered.
     */
    public void updateCounter(final FeatureHandle counter, final int value) {
        counter.putProperty(COUNTER_VALUE, value) //
                .whenComplete((aVoid, throwable) -> {
                    if (null == throwable) {
                        LOGGER.info("Thing with ID '{}' updated with Counter={}!", counter.getThingId(), value);
                    } else {
                        LOGGER.error(throwable.getMessage());
                    }
                });
    }

    /**
     * Destroys the client and waits for its graceful shutdown.
     */
    public void terminate() {
        // Gracefully shutdown the integrationClient
        integrationClient.destroy();
    }

}
