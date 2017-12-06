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
  private final String nrqlUrl;
  private final String alertExternalServicesUrl;
  private final String pluginsUrl;
  protected RestClient restClientSynthetic;
  protected RestClient restClientAlerts;
  protected RestClient restClientPolicy;
  protected RestClient restClientSyntheticCondition;
  protected RestClient restClientNrql;
  protected RestClient restClientAlertExternalServices;
  protected RestClient restClientPlugins;

  /**
   * Creates a new instance of the client using the given url.
   *
   * @param syntheticConditionUrl     the URL to use for accessing Synthetic conditions Rest Api
   * @param alertConditionsUrl        the URL to use for accessing Alert conditions Rest Api
   * @param policyUrl                 the URL to use for Policy Rest Api
   * @param syntheticUrl              the URL to use for accessing the Monitors Rest Api
   * @param nrqlUrl                   the URL to use for accessing the NRQL Rest Api
   * @param alertExternalServicesUrl  the URL to use for accessing Alerts of External Services
   *                                  Rest Api
   * @param pluginsUrl                the URL to use for accessing Plugins Rest Api
   * @param restApiKey                the rest api key for New Relic account
   * @param adminApiKey               the admin api key for New Relic account
   */
  public NewRelicClient(String syntheticConditionUrl, String alertConditionsUrl, String policyUrl,
                        String syntheticUrl, String nrqlUrl, String alertExternalServicesUrl,
                        String pluginsUrl, String restApiKey, String adminApiKey) {
    this.policyUrl = policyUrl;
    this.syntheticConditionUrl = syntheticConditionUrl;
    this.alertConditionsUrl = alertConditionsUrl;
    this.syntheticUrl = syntheticUrl;
    this.nrqlUrl = nrqlUrl;
    this.alertExternalServicesUrl = alertExternalServicesUrl;
    this.pluginsUrl = pluginsUrl;
    restClientSynthetic = new URLConnectionRestClient(syntheticUrl, adminApiKey);
    restClientAlerts = new URLConnectionRestClient(alertConditionsUrl, adminApiKey);
    restClientPolicy = new URLConnectionRestClient(policyUrl, restApiKey);
    restClientSyntheticCondition = new URLConnectionRestClient(syntheticConditionUrl, adminApiKey);
    restClientNrql = new URLConnectionRestClient(nrqlUrl, adminApiKey);
    restClientAlertExternalServices = new URLConnectionRestClient(alertExternalServicesUrl,
        adminApiKey);
    restClientPlugins = new URLConnectionRestClient(pluginsUrl, adminApiKey);
  }

  /**
   * Get the alert conditions status for a policy.
   *
   * @param policyName the policy which user is interested in
   * @return Map containing the condition's name as key and state as value
   * @throws NewRelicNotFoundException thrown in case of an error
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
        getNrqlConditions(policyName, policyId, retStats);
        getExternalServicesConditions(policyName, policyId, retStats);
        getPluginsConditions(policyName, policyId, retStats);
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
      disableAlertsConditions(policyName, policyAlertConditions);
      policyAlertConditions = getSyntheticConditions(policyName, policy);
      disableSyntheticConditions(policyName, policyAlertConditions);
      policyAlertConditions = getNrqlConditions(policyName, policy);
      disableNrqlConditions(policyName, policyAlertConditions);
      policyAlertConditions = getExternalServiceConditions(policyName, policy);
      disableExternalServiceConditions(policyName, policyAlertConditions);
      policyAlertConditions = getPluginsConditions(policyName, policy);
      disablePluginsConditions(policyName, policyAlertConditions);
    } else {
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
  }

  /**
   * This method restores the alert conditions as per the Map sent.
   *
   * @param policyName the name of policy user interested in
   * @param states map containing the restoring states for conditions
   * @throws NewRelicNotFoundException thrown in case of error
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
        restoreAlertConditions(policyName, states, policy);
        restoreSyntheticConditions(policyName, states, policy);
        restoreNrqlConditions(policyName, states, policy);
        restoreExternalServicesConditions(policyName, states, policy);
        restorePluginConditions(policyName, states, policy);
      }
    } else {
      throw new NewRelicNotFoundException("No policy by name: " + policyName + " found.");
    }
  }

  /**
   * Gets the information on the Synthetic states in New Relic account.
   *
   * @return A map containing the Synthetic state
   * @throws NewRelicNotFoundException thrown in case of error
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
              if (synthetic != null && synthetic.getName() != null && synthetic.getStatus()
                  != null) {
                retStats.put(synthetic.getName(), !synthetic.getStatus()
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
   * Disables the Synthetic.
   *
   * @param syntheticName name of the synthetic to be disabled
   * @throws NewRelicNotFoundException thrown in case of error
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
   * This method restores the synthetics as per the map sent.
   *
   * @param states information containing the state needed per synthetic
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
          if (synthetics != null && synthetics.getMonitors() != null && !synthetics.getMonitors()
              .isEmpty()) {
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
      PolicyAlertConditions policyAlertConditions = getPolicyAlertConditions(policy.getName(),
          policy);
      if (policyAlertConditions != null && policyAlertConditions.getConditions() != null
          && !policyAlertConditions.getConditions().isEmpty()) {
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

  private PolicyAlertCondition getSyntheticCondition(Policy policy, String condition) {
    PolicyAlertCondition retAlertCondition = null;
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getSyntheticConditions(policy.getName(),
          policy);
      if (policyAlertConditions != null && policyAlertConditions.getSyntheticsConditions() != null
          && !policyAlertConditions.getSyntheticsConditions().isEmpty()) {
        for (Condition alertCondition : policyAlertConditions.getSyntheticsConditions()) {
          if (alertCondition.getName().equalsIgnoreCase(condition)) {
            retAlertCondition = new PolicyAlertCondition();
            retAlertCondition.setSyntheticsCondition(alertCondition);
            break;
          }
        }
      }
    }

    return retAlertCondition;
  }

  private PolicyAlertCondition getNrqlCondition(Policy policy, String condition) {
    PolicyAlertCondition retAlertCondition = null;
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getNrqlConditions(policy.getName(), policy);
      if (policyAlertConditions != null && policyAlertConditions.getNrqlConditions() != null
          && !policyAlertConditions.getNrqlConditions().isEmpty()) {
        for (Condition alertCondition : policyAlertConditions.getNrqlConditions()) {
          if (alertCondition.getName().equalsIgnoreCase(condition)) {
            retAlertCondition = new PolicyAlertCondition();
            retAlertCondition.setNrqlCondition(alertCondition);
            break;
          }
        }
      }
    }

    return retAlertCondition;
  }

  private PolicyAlertCondition getExternalServiceCondition(Policy policy, String condition) {
    PolicyAlertCondition retAlertCondition = null;
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getExternalServiceConditions(policy.getName(),
          policy);
      if (policyAlertConditions != null && policyAlertConditions.getExternalServiceConditions()
          != null && !policyAlertConditions.getExternalServiceConditions().isEmpty()) {
        for (Condition alertCondition : policyAlertConditions.getExternalServiceConditions()) {
          if (alertCondition.getName().equalsIgnoreCase(condition)) {
            retAlertCondition = new PolicyAlertCondition();
            retAlertCondition.setExternalServiceCondition(alertCondition);
            break;
          }
        }
      }
    }

    return retAlertCondition;
  }

  private PolicyAlertCondition getPluginCondition(Policy policy, String condition) {
    PolicyAlertCondition retAlertCondition = null;
    if (policy != null) {
      PolicyAlertConditions policyAlertConditions = getPluginsConditions(policy.getName(), policy);
      if (policyAlertConditions != null && policyAlertConditions.getPluginsConditions() != null
          && !policyAlertConditions.getPluginsConditions().isEmpty()) {
        for (Condition alertCondition : policyAlertConditions.getPluginsConditions()) {
          if (alertCondition.getName().equalsIgnoreCase(condition)) {
            retAlertCondition = new PolicyAlertCondition();
            retAlertCondition.setPluginCondition(alertCondition);
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
      policyAlertConditions = restClientAlerts.get("?policy_id=" + policy.getId(),
          PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
    return policyAlertConditions;
  }

  private PolicyAlertConditions getExternalServiceConditions(String policyName, Policy policy) {
    PolicyAlertConditions policyAlertConditions = null;
    try {
      policyAlertConditions = restClientAlertExternalServices.get(alertExternalServicesUrl
          + ".json?policy_id=" + policy.getId(), PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
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

  private void getAlertConditions(String policyName, Integer policyId,
                                  Map<String, Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientAlerts.get("?policy_id=" + policyId,
          PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getConditions() != null
          && !policyAlertConditions.getConditions().isEmpty()) {
        log.debug("Found Alert conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }

  private PolicyAlertConditions getSyntheticConditions(String policyName, Policy policy) {
    PolicyAlertConditions policyAlertConditions = null;
    try {
      policyAlertConditions = restClientSyntheticCondition.get(syntheticConditionUrl
          + ".json?policy_id=" + policy.getId(), PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
    return policyAlertConditions;
  }

  private void getSyntheticConditions(String policyName, Integer policyId,
                                      Map<String, Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientSyntheticCondition.get(syntheticConditionUrl
              + ".json?policy_id=" + policyId, PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getSyntheticsConditions() != null
          && !policyAlertConditions.getSyntheticsConditions().isEmpty()) {
        log.debug("Found Synthetic conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getSyntheticsConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }

  private PolicyAlertConditions getNrqlConditions(String policyName, Policy policy) {
    PolicyAlertConditions policyAlertConditions = null;
    try {
      policyAlertConditions = restClientNrql.get(nrqlUrl + ".json?policy_id="
          + policy.getId(), PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
    return policyAlertConditions;
  }

  private void getNrqlConditions(String policyName, Integer policyId,
                                 Map<String, Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientNrql.get(nrqlUrl + ".json?policy_id=" + policyId,
          PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getNrqlConditions() != null
          && !policyAlertConditions.getNrqlConditions().isEmpty()) {
        log.debug("Found NRQL conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getNrqlConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }

  private void getExternalServicesConditions(String policyName, Integer policyId,
                                             Map<String, Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientAlertExternalServices.get(alertExternalServicesUrl
              + ".json?policy_id=" + policyId,
          PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getExternalServiceConditions()
          != null && !policyAlertConditions.getExternalServiceConditions().isEmpty()) {
        log.debug("Found External Service conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getExternalServiceConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }

  private void getPluginsConditions(String policyName, Integer policyId, Map<String,
      Boolean> retStats) {
    PolicyAlertConditions policyAlertConditions;
    // find the statistics on the policy
    try {
      policyAlertConditions = restClientPlugins.get(pluginsUrl + ".json?policy_id=" + policyId,
          PolicyAlertConditions.class);
      if (policyAlertConditions != null && policyAlertConditions.getPluginsConditions() != null
          && !policyAlertConditions.getPluginsConditions().isEmpty()) {
        log.debug("Found Plugin conditions on policy");
        for (Condition alertCondition : policyAlertConditions.getPluginsConditions()) {
          retStats.put(alertCondition.getName(), alertCondition.getEnabled());
        }
      }
    } catch (IOException e) {
      log.error("Error occurred fetching conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
  }

  private PolicyAlertConditions getPluginsConditions(String policyName, Policy policy) {
    PolicyAlertConditions policyAlertConditions = null;
    try {
      policyAlertConditions = restClientPlugins.get(pluginsUrl + ".json?policy_id="
          + policy.getId(), PolicyAlertConditions.class);
    } catch (IOException e) {
      log.error("Error occurred fetching plugin conditions on the policy  " + policyName + ". "
          + e.getMessage(), e);
    }
    return policyAlertConditions;
  }

  private void disableSyntheticConditions(String policyName,
                                          PolicyAlertConditions policyAlertConditions) {
    if (policyAlertConditions != null && policyAlertConditions.getSyntheticsConditions() != null
        && !policyAlertConditions.getSyntheticsConditions().isEmpty()) {
      for (Condition alertCondition : policyAlertConditions.getSyntheticsConditions()) {
        alertCondition.setEnabled(false);
        try {
          PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
          policyAlertCondition.setSyntheticsCondition(alertCondition);
          restClientSyntheticCondition.update(syntheticConditionUrl + "/" + alertCondition.getId()
                  + ".json", policyAlertCondition);
        } catch (IOException e) {
          log.error("Error occurred updating Synthetic Alert condition: "
              + alertCondition.getName() + " on the policy  " + policyName + ". " + e.getMessage(),
              e);
        }
      }
    }
  }

  private void disableNrqlConditions(String policyName,
                                     PolicyAlertConditions policyAlertConditions) {
    if (policyAlertConditions != null && policyAlertConditions.getNrqlConditions() != null
        && !policyAlertConditions.getNrqlConditions().isEmpty()) {
      for (Condition nrqlCondition : policyAlertConditions.getNrqlConditions()) {
        nrqlCondition.setEnabled(false);
        try {
          PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
          policyAlertCondition.setNrqlCondition(nrqlCondition);
          restClientNrql.update(nrqlUrl + "/" + nrqlCondition.getId() + ".json",
              policyAlertCondition);
        } catch (IOException e) {
          log.error("Error occurred updating NRQL Alert condition: " + nrqlCondition.getName()
              + " on the policy  " + policyName + ". " + e.getMessage(), e);
        }
      }
    }
  }

  private void disableExternalServiceConditions(String policyName, PolicyAlertConditions
      policyAlertConditions) {
    if (policyAlertConditions != null && policyAlertConditions.getExternalServiceConditions()
        != null && !policyAlertConditions.getExternalServiceConditions().isEmpty()) {
      for (Condition externalServiceCondition : policyAlertConditions
          .getExternalServiceConditions()) {
        externalServiceCondition.setEnabled(false);
        try {
          PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
          policyAlertCondition.setExternalServiceCondition(externalServiceCondition);
          restClientAlertExternalServices.update(alertExternalServicesUrl + "/"
                  + externalServiceCondition.getId() + ".json", policyAlertCondition);
        } catch (IOException e) {
          log.error("Error occurred updating External Service Alert condition: "
              + externalServiceCondition.getName() + " on the policy  " + policyName + ". "
              + e.getMessage(), e);
        }
      }
    }
  }

  private void disablePluginsConditions(String policyName, PolicyAlertConditions
      policyAlertConditions) {
    if (policyAlertConditions != null && policyAlertConditions.getPluginsConditions() != null
        && !policyAlertConditions.getExternalServiceConditions().isEmpty()) {
      for (Condition pluginCondition : policyAlertConditions.getPluginsConditions()) {
        pluginCondition.setEnabled(false);
        try {
          PolicyAlertCondition policyAlertCondition = new PolicyAlertCondition();
          policyAlertCondition.setExternalServiceCondition(pluginCondition);
          restClientAlertExternalServices.update(alertExternalServicesUrl + "/"
                  + pluginCondition.getId() + ".json", policyAlertCondition);
        } catch (IOException e) {
          log.error("Error occurred updating Plugin condition: "
              + pluginCondition.getName() + " on the policy  " + policyName + ". "
              + e.getMessage(), e);
        }
      }
    }
  }

  private void disableAlertsConditions(String policyName,
                                      PolicyAlertConditions policyAlertConditions) {
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

  private void restoreSyntheticConditions(String policyName, Map<String, Boolean> states,
                                          Policy policy) {
    for (String condition : states.keySet()) {
      Boolean state = states.get(condition);
      // update the condition with the state
      PolicyAlertCondition syntheticCondition = getSyntheticCondition(policy, condition);
      if (syntheticCondition != null && syntheticCondition.getSyntheticsCondition() != null) {
        syntheticCondition.getSyntheticsCondition().setEnabled(state);
        try {
          restClientSyntheticCondition.update(syntheticConditionUrl + "/"
              + syntheticCondition.getSyntheticsCondition().getId() + ".json", syntheticCondition);
        } catch (IOException e) {
          log.error("Error occurred restoring synthetic state on the policy  " + policyName
              + " for condition  " + condition + " ." + e.getMessage(), e);
        }
      }
    }
  }

  private void restoreNrqlConditions(String policyName, Map<String, Boolean> states,
                                     Policy policy) {
    for (String condition : states.keySet()) {
      Boolean state = states.get(condition);
      // update the condition with the state
      PolicyAlertCondition nrqlCondition = getNrqlCondition(policy, condition);
      if (nrqlCondition != null && nrqlCondition.getNrqlCondition() != null) {
        nrqlCondition.getNrqlCondition().setEnabled(state);
        try {
          restClientNrql.update(nrqlUrl + "/"
              + nrqlCondition.getNrqlCondition().getId() + ".json", nrqlCondition);
        } catch (IOException e) {
          log.error("Error occurred restoring synthetic state on the policy  " + policyName
              + " for condition  " + condition + " ." + e.getMessage(), e);
        }
      }
    }
  }

  private void restoreExternalServicesConditions(String policyName, Map<String, Boolean> states,
                                                 Policy policy) {
    for (String condition : states.keySet()) {
      Boolean state = states.get(condition);
      // update the condition with the state
      PolicyAlertCondition externalServiceCondition = getExternalServiceCondition(policy,
          condition);
      if (externalServiceCondition != null && externalServiceCondition
          .getExternalServiceCondition() != null) {
        externalServiceCondition.getExternalServiceCondition().setEnabled(state);
        try {
          restClientAlertExternalServices.update(alertExternalServicesUrl + "/"
              + externalServiceCondition.getExternalServiceCondition().getId() + ".json",
              externalServiceCondition);
        } catch (IOException e) {
          log.error("Error occurred restoring external service condition state on the policy "
              + policyName + " for condition  " + condition + " ." + e.getMessage(), e);
        }
      }
    }
  }

  private void restorePluginConditions(String policyName, Map<String, Boolean> states,
                                       Policy policy) {
    for (String condition : states.keySet()) {
      Boolean state = states.get(condition);
      // update the condition with the state
      PolicyAlertCondition pluginCondition = getPluginCondition(policy, condition);
      if (pluginCondition != null && pluginCondition.getExternalServiceCondition() != null) {
        pluginCondition.getExternalServiceCondition().setEnabled(state);
        try {
          restClientAlertExternalServices.update(alertExternalServicesUrl + "/"
              + pluginCondition.getExternalServiceCondition().getId() + ".json", pluginCondition);
        } catch (IOException e) {
          log.error("Error occurred restoring Plugin condition state on the policy  " + policyName
              + " for condition  " + condition + " ." + e.getMessage(), e);
        }
      }
    }
  }

  private void restoreAlertConditions(String policyName, Map<String, Boolean> states,
                                      Policy policy) {
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
}
