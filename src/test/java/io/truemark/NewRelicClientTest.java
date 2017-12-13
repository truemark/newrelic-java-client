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
      Map<String, Boolean> stats = client.getAlertConditionStats("Test");
      if (stats != null && !stats.isEmpty()) {
        for (String statName : stats.keySet()) {
          log.info(statName + " status is : " + stats.get(statName));
        }
      }
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
      Map<String, Boolean> stats = client.getAlertConditionStats("Test");
      if (stats != null && !stats.isEmpty()) {
        for (String statName : stats.keySet()) {
          log.info(statName + " status is : " + stats.get(statName));
        }
      }
      log.info("testDisableAlertConditions completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableAlertConditions. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreAlertConditionStates() {
    try {
      log.info("Running testRestoreAlertConditionStates for Policy : Test");
      Map<String, Boolean> states =  getConditionStates();
      for (String syntheticName : states.keySet()) {
        log.info(syntheticName + " condition status to be restored is : "
            + states.get(syntheticName));
      }
      client.restoreAlertConditionStates("Test", states);
      Map<String, Boolean> stats = client.getAlertConditionStats("Test");
      if (stats != null && !stats.isEmpty()) {
        for (String statName : stats.keySet()) {
          log.info(statName + " status is : " + stats.get(statName));
        }
      }
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
      Map<String, Boolean> states = client.getSyntheticStates();
      if (states != null && !states.isEmpty()) {
        for (String syntheticName : states.keySet()) {
          log.info(syntheticName + " status to be restored is : " + states.get(syntheticName));
        }
      }
      log.info("testGetSyntheticStates completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testGetSyntheticStates. " + e.getMessage(), e);
    }
  }

  @Test
  public void testDisableSynthetic() {
    try {
      log.info("Running testDisableSynthetic : API Test");
      client.disableSynthetic("API Test");
      Map<String, Boolean> states = client.getSyntheticStates();
      if (states != null && !states.isEmpty()) {
        for (String syntheticName : states.keySet()) {
          log.info(syntheticName + " status to be restored is : " + states.get(syntheticName));
        }
      }
      log.info("testDisableSynthetic completed successfully.");
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testDisableSynthetic. " + e.getMessage(), e);
    }
  }

  @Test
  public void testRestoreSyntheticStates() {
    log.info("Running testRestoreSyntheticStates");
    Map<String, Boolean> states = getSyntheticStates();
    for (String syntheticName : states.keySet()) {
      log.info(syntheticName + " status to be restored is : " + states.get(syntheticName));
    }
    client.restoreSyntheticStates(states);
    try {
      states = client.getSyntheticStates();
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred while testing testRestoreSyntheticStates. " + e.getMessage(), e);
    }
    if (states != null && !states.isEmpty()) {
      for (String syntheticName : states.keySet()) {
        log.info(syntheticName + " status is : " + states.get(syntheticName));
      }
    }
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
