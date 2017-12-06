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
      client.getAlertConditionStats("Test");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing getAlertConditionStats. " + e.getMessage(), e);
    }
  }

  @Test
  public void testDisableAlertConditions() {
    try {
      client.disableAlertConditions("Test");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableAlertConditions. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreAlertConditionStates() {
    try {
      client.restoreAlertConditionStates("Test", getConditionStates());
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testRestoreAlertConditionStates. " + e.getMessage(), e);
    }
  }

  @Test
  public void testGetSyntheticStates() {
    try {
      client.getSyntheticStates();
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testGetSyntheticStates. " + e.getMessage(), e);
    }
  }

  @Test
  public void testDisableSynthetic() {
    try {
      client.disableSynthetic("API Test");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableSynthetic. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreSyntheticStates() {
      client.restoreSyntheticStates(getSyntheticStates());
  }

  private Map<String,Boolean> getSyntheticStates() {
    Map<String,Boolean> syntheticStates = new HashMap<>();
    syntheticStates.put("", true);
    syntheticStates.put("Scripted Browser Test", true);
    syntheticStates.put("Test", true);
    syntheticStates.put("API Test", false);

    return syntheticStates;
  }

  private Map<String,Boolean> getConditionStates() {
    Map<String,Boolean> conditions = new HashMap<>();
    conditions.put("Apdex (Low)", null);
    conditions.put("Apdex (Low) 2", null);
    conditions.put("Deadlocked threads (Low)", false);
    conditions.put("Check failure", false);
    conditions.put("NRQL Condition1", false);
    conditions.put("External Condition 1", false);
    conditions.put("External Condition 2", true);
    return conditions;
  }

}
