package io.truemark;

import io.truemark.data.*;
import io.truemark.exception.NewRelicNotFoundException;
import io.truemark.http.RestClient;
import io.truemark.http.URLConnectionRestClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Client to handle New Relic requests.
 *
 * @author Abhijeet Kale
 */
@Slf4j
public class NewRelicClient implements Serializable {

  public static final int DEFAULT_TIMEOUT = 3000; // 30 seconds
//  private final String policyUrl;
//  private final String syntheticConditionUrl;
  private final String alertConditionsUrl;
//  private final String syntheticUrl;

  protected RestClient restClientSynthetic;
  protected RestClient restClientAlerts;
  protected RestClient restClientPolicy;
  protected RestClient restClientSyntheticCondition;

  /**
   * Creates a new instance of the client using the given url.
   *
   * @param syntheticConditionUrl the URL to use for accessing synthetic condition
   * @param alertConditionsUrl the URL to use for accessing alert condition
   * @param policyUrl the URL to use for accessing synthetic condition
   * @param syntheticUrl the URL to use for accessing synthetic condition
   */
  public NewRelicClient(String syntheticConditionUrl, String alertConditionsUrl, String policyUrl,
                        String syntheticUrl, String apiKey) {
//    this.policyUrl = policyUrl;
//    this.syntheticConditionUrl = syntheticConditionUrl;
    this.alertConditionsUrl = alertConditionsUrl;
//    this.syntheticUrl = syntheticUrl;
    restClientSynthetic = new URLConnectionRestClient(syntheticUrl, apiKey);
    restClientAlerts = new URLConnectionRestClient(alertConditionsUrl, apiKey);
    restClientPolicy = new URLConnectionRestClient(policyUrl, apiKey);
    restClientSyntheticCondition = new URLConnectionRestClient(syntheticConditionUrl, apiKey);
  }

  public Map<String, Boolean> getAlertConditionStats(String policyName) throws NewRelicNotFoundException {
    // first get all the policies
    Policies policies = getPolicies();
    if (policies == null) {
      throw new NewRelicNotFoundException("No policies found.");
    }
    Integer policyId = null;
    Map<String, Boolean> retStats;
    // if there are policy list found, iterate to find out the policy in which user is interested
    if (policies != null && policies.getPolicies() != null && policies.getPolicies().length > 0) {
      for(Policy policy : policies.getPolicies()) {
        if (policy.getName().equals(policyName)) {
          log.debug("Found the policy with Name : " + policyName);
          policyId = policy.getId();
        }
      }
      if (policyId == null) {
        throw new NewRelicNotFoundException(policyName + " policy not found.");
      } else {
        retStats = new HashMap<>();
        PolicyAlertConditions  policyAlertConditions;
        // find the statistics on the policy
        try {
          policyAlertConditions = restClientAlerts.get("?policy_id=" + policyId, PolicyAlertConditions.class);
          if (policyAlertConditions != null && policyAlertConditions.getConditions() != null && !policyAlertConditions
              .getConditions().isEmpty()) {
            log.debug("Found alert conditions on policy");
            for (Condition alertCondition: policyAlertConditions.getConditions()) {
              retStats.put(alertCondition.getName().concat(":").concat(String.valueOf(alertCondition.getId())),
                  alertCondition.getEnabled());
            }
          }
        } catch (IOException e) {
          log.error("Error occurred fetching conditions on the policy  "+ policyName + ". " + e.getMessage(), e);
          return retStats;
        }
      }
    } else {
      throw new NewRelicNotFoundException("No policies found.");
    }
    return retStats;
  }

  private Policies getPolicies() {
    Policies policies = null;
    try {
      policies = restClientPolicy.get("", Policies.class);
    } catch (IOException e) {
      log.error("Error occurred fetching policies from New Relic. " + e.getMessage(), e);
    }
    return policies;
  }

  public void disableAlertConditions(String policyName) throws NewRelicNotFoundException {
    Policy policy = getPolicyByName(policyName);
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getPolicyAlertConditions(policyName, policy);
      if (policyAlertConditions != null && policyAlertConditions.getConditions() != null && !policyAlertConditions
          .getConditions().isEmpty()) {
        for (Condition alertCondition : policyAlertConditions.getConditions()) {
          alertCondition.setEnabled(false);
          try {
            PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
            policyAlertCondition.setCondition(alertCondition);
            restClientAlerts.update( alertCondition.getId()  + ".json",
                policyAlertCondition);
          } catch (IOException e) {
            log.error("Error occurred updating alert condition: " + alertCondition.getName() + " on the policy  " +
                    policyName + ". " + e.getMessage(), e);
          }
        }
      }
    } else {
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
  }

  private PolicyAlertConditions getPolicyAlertConditions(String policyName, Policy policy) {
    PolicyAlertConditions  policyAlertConditions = null;
    try {
      policyAlertConditions = restClientAlerts.get("?policy_id=" + policy.getId(), PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". " + e.getMessage(), e);
    }
    return policyAlertConditions;
  }

  private Policy getPolicyByName(String policyName) throws NewRelicNotFoundException {
    // first get all the policies
    Policies policies = getPolicies();
    if (policies == null) {
      throw new NewRelicNotFoundException("No policies found.");
    }
    Policy retPolicy = null;
    if (policies != null && policies.getPolicies() != null && policies.getPolicies().length > 0) {
      for (Policy policy : policies.getPolicies()) {
        if (policy.getName().equals(policyName)) {
          log.debug("Found the policy with Name : " + policyName);
          retPolicy = policy;
          break;
        }
      }
    }
    return retPolicy;
  }

  public void restoreAlertConditionStates(String policyName, Map <String, Boolean> states) throws
      NewRelicNotFoundException {
    Policy policy;
    try {
      policy = getPolicyByName(policyName);
    } catch (NewRelicNotFoundException e) {
      log.error("Error occurred fetching policy by name: " + policyName + ". ", e);
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
    if (policy != null) {
      if (states != null && !states.isEmpty()) {
        for (String condition : states.keySet()) {
          Boolean state = states.get(condition);
          // update the condition with the state
          PolicyAlertCondition alertCondition = getAlertCondition(policy, condition);
          if (alertCondition != null && alertCondition.getCondition() != null) {
            alertCondition.getCondition().setEnabled(state);
            try {
              restClientAlerts.update(alertConditionsUrl + alertCondition.getCondition().getId()  + ".json",
                  alertCondition);
            } catch (IOException e) {
              log.error("Error occurred restoring state on the policy  " + policyName + " for condition  " + condition
                  + " ." + e.getMessage(), e);
            }
          }
        }
      }
    } else {
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
  }

  private PolicyAlertCondition getAlertCondition(Policy policy, String condition) {
    PolicyAlertCondition retAlertCondition = null;
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getPolicyAlertConditions(policy.getName(), policy);
      if (policyAlertConditions != null && policyAlertConditions.getConditions() != null && !policyAlertConditions
          .getConditions().isEmpty()) {
        for (Condition alertCondition : policyAlertConditions.getConditions()) {
          if (alertCondition.getName().equalsIgnoreCase(condition)) {
            retAlertCondition = new PolicyAlertCondition();
            retAlertCondition.setCondition(alertCondition);
            break;
          }
        }
      }
    }

    return retAlertCondition;
  }

  public Map <String, Boolean> getSyntheticStates() {

    return null;
  }

  public void disableSynthetic() {

  }

  public void restoreSyntheticStates(Map <String, Boolean> states) {

  }
}
