/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.schemarepo.server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.schemarepo.config.Config;
import org.schemarepo.config.ConfigBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

/**
 * A {@link RepositoryServer} is a stand-alone server for running a
 * {@link RESTRepository}. {@link #main(String...)} takes a single argument
 * containing a property file for configuration. <br/>
 * <br/>
 *
 */
public class RepositoryServer {
  private final HttpServer server;

  /**
   * Constructs an instance of this class, overlaying the default properties
   * with any identically-named properties in the supplied {@link Properties}
   * instance.
   *
   * @param props
   *          Property values for overriding the defaults.
   *          <p>
   *          <b><i>Any overriding properties must be supplied as type </i>
   *          <code>String</code><i> or they will not work and the default
   *          values will be used.</i></b>
   *
   */
  public RepositoryServer(Properties props) throws IOException {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final String julToSlf4jDep = "jul-to-slf4j dependency";
    final String julPropName = Config.LOGGING_ROUTE_JUL_TO_SLF4J;
    if (Boolean.parseBoolean(props.getProperty(julPropName, Config.getDefault(julPropName)))) {
      final String slf4jBridgeHandlerName = "org.slf4j.bridge.SLF4JBridgeHandler";
      try {
        final Class<?> slf4jBridgeHandler = Class.forName(slf4jBridgeHandlerName, true,
            Thread.currentThread().getContextClassLoader());
        slf4jBridgeHandler.getMethod("removeHandlersForRootLogger").invoke(null);
        slf4jBridgeHandler.getMethod("install").invoke(null);
        logger.info("Routing java.util.logging traffic through SLF4J");
      } catch (Exception e) {
        logger.error(
            "Failed to install {}, java.util.logging is unaffected. Perhaps you need to add {}",
            slf4jBridgeHandlerName, julToSlf4jDep, e);
      }
    } else {
      logger.info(
          "java.util.logging is NOT routed through SLF4J. Set {} property to true and add {} if you want otherwise",
          julPropName, julToSlf4jDep);
    }

    String name = "schema-repo-" + UUID.randomUUID().toString();

    ServiceLocator locator = ServiceLocatorUtilities.bind(name, new ConfigBinder(props), new ServerBinder());

    URI baseUri = UriBuilder.fromUri("http://localhost/").port(8888).build();
    ResourceConfig config = new ServerResourceConfig();
    this.server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config, locator);
  }

  public static void main(String... args) throws Exception {
    Properties props = new Properties();
    RepositoryServer server = new RepositoryServer(props);
    server.start();
    Thread.currentThread().join();
  }

  public void start() throws Exception {
    server.start();
  }

  public void stop() throws Exception {
    server.stop();
  }

  static class ServerBinder extends AbstractBinder {
    @Override
    protected void configure() {
      bind(MachineOrientedRESTRepository.class);
      bind(HumanOrientedRESTRepository.class);
      bind(AuxiliaryRESTRepository.class);
    }
  }

  static class ServerResourceConfig extends ResourceConfig {
    public ServerResourceConfig() {
      packages("org.schemarepo.server");
    }
  }
}

