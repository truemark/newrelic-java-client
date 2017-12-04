package io.truemark;

import com.netradius.commons.lang.ValidationHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Helper class for integration test.
 *
 * @author Abhijeet Kale
 */
@Slf4j
public class IntegrationTestHelper {

  private static Properties config;
  private static NewRelicClient newRelicClient;

  private static Properties read(final File file) {
    log.info("Loading integration test settings from " + file.getAbsolutePath());
    try (FileInputStream in = new FileInputStream(file)) {
      Properties properties = new Properties();
      properties.load(in);
      return properties;
    } catch (IOException x) {
      log.error("Error reading " + file.getAbsolutePath() + ": " + x.getMessage(), x);
    }
    return null;
  }

  private static boolean loadConfig() {
    // Try and read from default location first
    File file = new File("integration.properties");
    if (file.exists()) {
      config = read(file);
    }

    // Try alternate location
    if (config == null) {

      // Check for system property
      String loc = System.getProperty("config");
      if (loc == null) {
        loc = System.getProperty("CONFIG");
      }

      // Check environment variable
      if (loc == null) {
        loc = System.getenv("CONFIG");
      }

      if (loc == null) {
        loc = System.getenv("config");
      }

      if (loc != null) {
        file = new File(loc);
        if (file.exists()) {
          config = read(file);
        }
      }
    }

    if (config == null) {
      String loc =  System.getProperty("user.home") + "/newrelic-client-integration.properties";
      if (loc != null) {
        file = new File(loc);
        if (file.exists()) {
          config = read(file);
        }
      }
    }

    if (config == null) {
      String loc =  System.getProperty("user.dir") + "/integration.properties";
      if (loc != null) {
        file = new File(loc);
        if (file.exists()) {
          config = read(file);
        }
      }
    }

    return config != null;
  }

  private static void init() throws IOException {
    if (config == null) {

      log.info("Initializing account for integration tests");

      if (!loadConfig()) {
        throw new IllegalStateException("Unable to load integration test settings");
      }

      String syntheticConditionsUrl = config.getProperty("synthetic.conditions.rest.base.url");
      ValidationHelper.checkForEmpty(syntheticConditionsUrl);
      String alertConditionUrl = config.getProperty("alert.conditions.rest.base.url");
      ValidationHelper.checkForEmpty(alertConditionUrl);
      String policyUrl = config.getProperty("policy.rest.base.url");
      ValidationHelper.checkForEmpty(policyUrl);
      String syntheticUrl = config.getProperty("synthetic.rest.base.url");
      ValidationHelper.checkForEmpty(syntheticConditionsUrl);
      String apiKey = config.getProperty("api.key");
      ValidationHelper.checkForEmpty(apiKey);

      newRelicClient = new NewRelicClient(syntheticConditionsUrl, alertConditionUrl, policyUrl,
          syntheticUrl,apiKey);
    }
  }

  public static Properties getConfig() throws IOException {
    init();
    return config;
  }

  public static NewRelicClient getNewRelicClient() throws IOException {
    init();
    return newRelicClient;
  }

}
