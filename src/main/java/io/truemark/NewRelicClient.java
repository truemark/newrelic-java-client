package io.truemark;

import io.truemark.data.Condition;
import io.truemark.data.Policies;
import io.truemark.data.Policy;
import io.truemark.data.PolicyAlertCondition;
import io.truemark.data.PolicyAlertConditions;
import io.truemark.data.Synthetic;
import io.truemark.data.Synthetics;
import io.truemark.exception.NewRelicNotFoundException;
import io.truemark.http.RestClient;
import io.truemark.http.URLConnectionRestClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client to handle New Relic requests.
 *
 * @author Abhijeet Kale
 */
@Slf4j
public class NewRelicClient implements Serializable {

  private static final int OFFSET = 0;
  private static final int LIMIT = 20;
  public static final String DISABLED = "DISABLED";
  public static final String ENABLED = "ENABLED";
  private final String policyUrl;
  private final String syntheticConditionUrl;
  private final String alertConditionsUrl;
  private final String syntheticUrl;

  protected RestClient restClientSynthetic;
  protected RestClient restClientAlerts;
  protected RestClient restClientPolicy;
  protected RestClient restClientSyntheticCondition;

  /**
   * Creates a new instance of the client using the given url.
   *
   * @param syntheticConditionUrl the URL to use for accessing synthetic conditions
   * @param alertConditionsUrl    the URL to use for accessing alert conditions
   * @param syntheticUrl          the URL to use for accessing the monitors api
   * @param restApiKey            the rest api key for New Relic account
   * @param adminApiKey           the admin api key for New Relic account
   */
  public NewRelicClient(String syntheticConditionUrl, String alertConditionsUrl, String policyUrl,
                        String syntheticUrl, String restApiKey, String adminApiKey) {
    this.policyUrl = policyUrl;
    this.syntheticConditionUrl = syntheticConditionUrl;
    this.alertConditionsUrl = alertConditionsUrl;
    this.syntheticUrl = syntheticUrl;
    restClientSynthetic = new URLConnectionRestClient(syntheticUrl, adminApiKey);
    restClientAlerts = new URLConnectionRestClient(alertConditionsUrl, restApiKey);
    restClientPolicy = new URLConnectionRestClient(policyUrl, restApiKey);
    restClientSyntheticCondition = new URLConnectionRestClient(syntheticConditionUrl, adminApiKey);
  }

  /**
   * @param policyName
   * @return
   * @throws NewRelicNotFoundException
   */
  public Map<String, Boolean> getAlertConditionStats(String policyName) throws
      NewRelicNotFoundException {
    // first get all the policies
    Policies policies = getPolicies();
    if (policies == null) {
      throw new NewRelicNotFoundException("No policies found.");
    }
    Integer policyId = null;
    Map<String, Boolean> retStats;
    // if there are policy list found, iterate to find out the policy in which user is interested
    if (policies != null && policies.getPolicies() != null && policies.getPolicies().length > 0) {
      for (Policy policy : policies.getPolicies()) {
        if (policy.getName().equals(policyName)) {
          log.debug("Found the policy with Name : " + policyName);
          policyId = policy.getId();
        }
      }
      if (policyId == null) {
        throw new NewRelicNotFoundException(policyName + " policy not found.");
      } else {
        retStats = new HashMap<>();
        getAlertConditions(policyName, policyId, retStats);
        getSyntheticConditions(policyName, policyId, retStats);
      }
    } else {
      throw new NewRelicNotFoundException("No policies found.");
    }
    return retStats;
  }

  /**
   * Disables the alert conditions under a policy.
   *
   * @param policyName accepts the policy name
   * @throws NewRelicNotFoundException thrown in case of an error.
   */
  public void disableAlertConditions(String policyName) throws NewRelicNotFoundException {
    Policy policy = getPolicyByName(policyName);
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getPolicyAlertConditions(policyName, policy);
      disableAlertConditions(policyName, policyAlertConditions);
      policyAlertConditions = getSyntheticConditions(policyName, policy);
      disableSyntheticConditions(policyName, policyAlertConditions);
    } else {
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
  }

  private void disableSyntheticConditions(String policyName, PolicyAlertConditions policyAlertConditions) {
    if (policyAlertConditions != null && policyAlertConditions.getSyntheticsConditions() != null
        && !policyAlertConditions.getSyntheticsConditions().isEmpty()) {
      for (Condition alertCondition : policyAlertConditions.getSyntheticsConditions()) {
        alertCondition.setEnabled(false);
        try {
          PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
          policyAlertCondition.setSyntheticsCondition(alertCondition);
          restClientSyntheticCondition.update(syntheticConditionUrl + "/" + alertCondition.getId() + ".json",
              policyAlertCondition);
        } catch (IOException e) {
          log.error("Error occurred updating alert synthetic condition: " + alertCondition.getName()
              + " on the policy  " + policyName + ". " + e.getMessage(), e);
        }
      }
    }
  }

  private void disableAlertConditions(String policyName, PolicyAlertConditions policyAlertConditions) {
    if (policyAlertConditions != null && policyAlertConditions.getConditions() != null
        && !policyAlertConditions.getConditions().isEmpty()) {
      for (Condition alertCondition : policyAlertConditions.getConditions()) {
        alertCondition.setEnabled(false);
        try {
          PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
          policyAlertCondition.setCondition(alertCondition);
          restClientAlerts.update(alertCondition.getId() + ".json", policyAlertCondition);
        } catch (IOException e) {
          log.error("Error occurred updating alert condition: " + alertCondition.getName()
              + " on the policy  " + policyName + ". " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * @param policyName
   * @param states
   * @throws NewRelicNotFoundException
   */
  public void restoreAlertConditionStates(String policyName, Map<String, Boolean> states) throws
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
              restClientAlerts.update(alertConditionsUrl + alertCondition.getCondition().getId()
                  + ".json", alertCondition);
            } catch (IOException e) {
              log.error("Error occurred restoring state on the policy  " + policyName
                  + " for condition  " + condition + " ." + e.getMessage(), e);
            }
          }
        }
      }
    } else {
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
  }

  /**
   * @return
   * @throws NewRelicNotFoundException
   */
  public Map<String, Boolean> getSyntheticStates() throws NewRelicNotFoundException {
    // get the count of monitors
    // figure out how many calls to make to collect the synthetic data
    Integer syntheticCount = getSyntheticCount();
    Map<String, Boolean> retStats;
    if (syntheticCount == null || syntheticCount == 0) {
      throw new NewRelicNotFoundException("No Synthetics found .");
    } else {
      retStats = new HashMap<>();
      int offset = OFFSET;
      int limit = LIMIT;
      // first get all Synthetics/Monitors
      Synthetics synthetics;
      for (int i = offset; i < syntheticCount; i += LIMIT) {
        try {
          synthetics = restClientSynthetic.get(syntheticUrl + "?offset=" + i + "&limit="
              + limit, Synthetics.class);
          if (synthetics != null && synthetics.getMonitors() != null && !synthetics.getMonitors()
              .isEmpty()) {
            List<Synthetic> monitors = synthetics.getMonitors();
            for (Synthetic synthetic : monitors) {
              if (synthetic != null && synthetic.getName() != null && synthetic.getStatus() != null) {
                retStats.put(synthetic.getName(), synthetic.getStatus()
                    .equalsIgnoreCase("DISABLED"));
              }
            }
          }
        } catch (IOException e) {
          log.error("Error occurred fetching synthetics from New Relic. " + e.getMessage(), e);
        }
      }
      return retStats;
    }
  }

  /**
   * @param syntheticName
   * @throws NewRelicNotFoundException
   */
  public void disableSynthetic(String syntheticName) throws NewRelicNotFoundException {
    Synthetic synthetic = getSyntheticByName(syntheticName);
    if (synthetic != null) {
      updateSynthetic(synthetic, DISABLED);
    } else {
      throw new NewRelicNotFoundException("No Synthetic by name : " + syntheticName + " found.");
    }
  }

  /**
   * @param states
   */
  public void restoreSyntheticStates(Map<String, Boolean> states) {
    if (states != null && !states.isEmpty()) {
      for (String syntheticName : states.keySet()) {
        Boolean state = states.get(syntheticName);
        Synthetic synthetic = getSyntheticByName(syntheticName);
        if (synthetic != null) {
          String status = state == true ? ENABLED : DISABLED;
          updateSynthetic(synthetic, status);
        } else {
          log.error("Unable to find synthetic by name " + syntheticName);
        }
      }
    }
  }

  private void updateSynthetic(Synthetic synthetic, String status) {
    // update the synthetic status
    synthetic.setStatus(status);
    try {
      restClientSynthetic.update(syntheticUrl + "/" + synthetic.getId(), synthetic);
    } catch (IOException e) {
      log.error("Error occurred fetching synthetics from New Relic. " + e.getMessage(), e);
    }
  }

  private Synthetic getSyntheticByName(String syntheticName) {
    Integer syntheticCount = getSyntheticCount();
    Synthetic retSynthetic = null;
    if (syntheticCount != null && syntheticCount > 0) {
      int offset = OFFSET;
      int limit = LIMIT;
      // first get all Synthetics/Monitors
      Synthetics synthetics;
      for (int i = offset; i < syntheticCount; i += LIMIT) {
        try {
          synthetics = restClientSynthetic.get(syntheticUrl + "?offset=" + i + "&limit="
              + limit, Synthetics.class);
          if (synthetics != null && synthetics.getMonitors() != null && !synthetics.getMonitors().isEmpty()) {
            List<Synthetic> monitors = synthetics.getMonitors();
            for (Synthetic synthetic : monitors) {
              if (synthetic != null && synthetic.getName() != null && synthetic.getName()
                  .equalsIgnoreCase(syntheticName)) {
                retSynthetic = synthetic;
                break;
              }
            }
          }
        } catch (IOException e) {
          log.error("Error occurred fetching synthetics from New Relic. " + e.getMessage(), e);
        }
      }
    }

    return retSynthetic;
  }

  private Integer getSyntheticCount() {
    Synthetics synthetics = null;
    try {
      synthetics = restClientSynthetic.get("", Synthetics.class);
    } catch (IOException e) {
      log.error("Error occurred fetching synthetics from New Relic. " + e.getMessage(), e);
    }

    return synthetics != null ? synthetics.getCount() : 0;
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

  private Policies getPolicies() {
    Policies policies = null;
    try {
      policies = restClientPolicy.get("", Policies.class);
    } catch (IOException e) {
      log.error("Error occurred fetching policies from New Relic. " + e.getMessage(), e);
    }
    return policies;
  }

  private PolicyAlertConditions getPolicyAlertConditions(String policyName, Policy policy) {
    PolicyAlertConditions policyAlertConditions = null;
    try {
      policyAlertConditions = restClientAlerts.get("?policy_id=" + policy.getId(), PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". " + e.getMessage(), e);
    }
    return policyAlertConditions;
  }

  private PolicyAlertConditions getSyntheticConditions(String policyName, Policy policy) {
    PolicyAlertConditions policyAlertConditions = null;
    try {
      policyAlertConditions = restClientSyntheticCondition.get(syntheticConditionUrl + ".json?policy_id="
              + policy.getId(), PolicyAlertConditions.class);
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

  private void getAlertConditions(String policyName, Integer policyId, Map<String, Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientAlerts.get("?policy_id=" + policyId,
          PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getConditions() != null
          && !policyAlertConditions.getConditions().isEmpty()) {
        log.debug("Found alert conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }

  private void getSyntheticConditions(String policyName, Integer policyId, Map<String, Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientSyntheticCondition.get(syntheticConditionUrl + ".json?policy_id=" + policyId,
          PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getSyntheticsConditions() != null
          && !policyAlertConditions.getSyntheticsConditions().isEmpty()) {
        log.debug("Found alert conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getSyntheticsConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }
}
