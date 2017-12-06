package io.truemark;

import io.truemark.exception.NewRelicNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Test class for the New Relic client.
 *
 * @author Abhijeet Kale
 */
@Slf4j
public class NewRelicClientTest {

  private static NewRelicClient client;
  private static Properties config;

  @BeforeClass
  public static void init() throws IOException {
    client = IntegrationTestHelper.getNewRelicClient();
    config = IntegrationTestHelper.getConfig();
  }

  @Test
  public void testGetAlertConditionsStats() {
    try {
      log.info("Running testGetAlertConditionsStats");
      client.getAlertConditionStats("Test");
      log.info("testGetAlertConditionsStats completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing getAlertConditionStats. " + e.getMessage(), e);
    }
  }

  @Test
  public void testDisableAlertConditions() {
    try {
      log.info("Running testDisableAlertConditions");
      client.disableAlertConditions("Test");
      log.info("testDisableAlertConditions completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableAlertConditions. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreAlertConditionStates() {
    try {
      log.info("Running testRestoreAlertConditionStates");
      client.restoreAlertConditionStates("Test", getConditionStates());
      log.info("testRestoreAlertConditionStates completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testRestoreAlertConditionStates. " + e.getMessage(),
          e);
    }
  }

  @Test
  public void testGetSyntheticStates() {
    try {
      log.info("Running testGetSyntheticStates");
      client.getSyntheticStates();
      log.info("testGetSyntheticStates completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testGetSyntheticStates. " + e.getMessage(), e);
    }
  }

  @Test
  public void testDisableSynthetic() {
    try {
      log.info("Running testDisableSynthetic");
      client.disableSynthetic("API Test");
      log.info("testDisableSynthetic completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableSynthetic. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreSyntheticStates() {
    log.info("Running testRestoreSyntheticStates");
    client.restoreSyntheticStates(getSyntheticStates());
    log.info("testRestoreSyntheticStates completed successfully.");
  }

  private Map<String,Boolean> getSyntheticStates() {
    Map<String,Boolean> syntheticStates = new HashMap<>();
    syntheticStates.put("Scripted Browser Test", true);
    syntheticStates.put("Test", true);
    syntheticStates.put("API Test", true);

    return syntheticStates;
  }

  private Map<String,Boolean> getConditionStates() {
    Map<String,Boolean> conditions = new HashMap<>();
    conditions.put("Apdex (Low)", true);
    conditions.put("Check failure", true);
    conditions.put("NRQL Condition1", true);
    conditions.put("External Condition 1", true);
    conditions.put("External Condition 2", true);
    return conditions;
  }

}
