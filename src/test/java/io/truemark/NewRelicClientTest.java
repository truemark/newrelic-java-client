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
      client.getAlertConditionStats("Michael Dollar's policy");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing getAlertConditionStats. " + e.getMessage(), e);
    }
  }

  @Test
  public void testDisableAlertConditions() {
    try {
      client.disableAlertConditions("Michael Dollar's policy");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableAlertConditions. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreAlertConditionStates() {
    try {
      client.restoreAlertConditionStates("Michael Dollar's policy", getConditionStates());
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableAlertConditions. " + e.getMessage(), e);
    }
  }

  @Test
  public void testGetSyntheticStates() {
    try {
      client.getSyntheticStates();
    } catch (NewRelicNotFoundException e) {

    }
  }

  private Map<String,Boolean> getConditionStates() {
    Map<String,Boolean> conditions = new HashMap<>();
    conditions.put("Apdex (Low) 1", true);
    conditions.put("Apdex (Low) 2", true);
    conditions.put("Deadlocked threads (Low)", true);
    conditions.put("Check failure", true);

    return conditions;
  }

}
