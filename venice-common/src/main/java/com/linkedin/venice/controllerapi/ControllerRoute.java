package com.linkedin.venice.controllerapi;

import com.linkedin.venice.HttpMethod;

import java.util.List;
import java.util.Arrays;
import org.apache.commons.collections.ListUtils;

import static com.linkedin.venice.controllerapi.ControllerApiConstants.*;

public enum ControllerRoute {
  REQUEST_TOPIC("/request_topic", HttpMethod.POST, Arrays.asList(NAME, STORE_SIZE, PUSH_TYPE, PUSH_JOB_ID)), // topic that writer should produce to
  EMPTY_PUSH("/empty_push", HttpMethod.POST, Arrays.asList(NAME, STORE_SIZE, PUSH_JOB_ID)), // do an empty push into a new version for this store
  END_OF_PUSH("/end_of_push", HttpMethod.POST, Arrays.asList(NAME, VERSION)), // write an END OF PUSH message into the topic
  STORE("/store", HttpMethod.GET, Arrays.asList(NAME)), // get all information about that store
  NEW_STORE("/new_store", HttpMethod.POST, Arrays.asList(NAME, KEY_SCHEMA, VALUE_SCHEMA), OWNER),
  MIGRATE_STORE("/migrate_store", HttpMethod.POST, Arrays.asList(NAME, CLUSTER, CLUSTER_SRC)),
  ABORT_MIGRATION("/abort_migration", HttpMethod.POST, Arrays.asList(NAME, CLUSTER, CLUSTER_DEST)),
  DELETE_STORE("/delete_store", HttpMethod.POST, Arrays.asList(NAME)),
  // Beside store name, others are all optional parameters for flexibility and compatibility.
  UPDATE_STORE("/update_store", HttpMethod.POST, Arrays.asList(NAME),OWNER, VERSION, LARGEST_USED_VERSION_NUMBER, PARTITION_COUNT,
      ENABLE_READS, ENABLE_WRITES, STORAGE_QUOTA_IN_BYTE, READ_QUOTA_IN_CU, REWIND_TIME_IN_SECONDS,
      OFFSET_LAG_TO_GO_ONLINE, ACCESS_CONTROLLED, COMPRESSION_STRATEGY, CHUNKING_ENABLED, SINGLE_GET_ROUTER_CACHE_ENABLED,
      BATCH_GET_ROUTER_CACHE_ENABLED, BATCH_GET_LIMIT, NUM_VERSIONS_TO_PRESERVE, WRITE_COMPUTATION_ENABLED, READ_COMPUTATION_ENABLED,
      LEADER_FOLLOWER_MODEL_ENABLED, BACKUP_STRATEGY),
  SET_VERSION("/set_version", HttpMethod.POST, Arrays.asList(NAME, VERSION)),
  ENABLE_STORE("/enable_store", HttpMethod.POST, Arrays.asList(NAME, OPERATION, STATUS)), // status "true" or "false", operation "read" or "write" or "readwrite".
  DELETE_ALL_VERSIONS("/delete_all_versions", HttpMethod.POST, Arrays.asList(NAME)),
  DELETE_OLD_VERSION("/delete_old_version", HttpMethod.POST, Arrays.asList(NAME, VERSION)),

  JOB("/job", HttpMethod.GET, Arrays.asList(NAME, VERSION)),
  KILL_OFFLINE_PUSH_JOB("/kill_offline_push_job", HttpMethod.POST, Arrays.asList(TOPIC)),
  LIST_STORES("/list_stores", HttpMethod.GET, Arrays.asList(), INCLUDE_SYSTEM_STORES),
  LIST_CHILD_CLUSTERS("/list_child_clusters", HttpMethod.GET, Arrays.asList()),
  LIST_NODES("/list_instances", HttpMethod.GET, Arrays.asList()),
  CLUSTER_HEALTH_STORES("/cluster_health_stores", HttpMethod.GET, Arrays.asList()),
  ClUSTER_HEALTH_INSTANCES("/cluster_health_instances", HttpMethod.GET, Arrays.asList()),
  LIST_REPLICAS("/list_replicas", HttpMethod.GET, Arrays.asList(NAME, VERSION)),
  NODE_REPLICAS("/storage_node_replicas", HttpMethod.GET, Arrays.asList(STORAGE_NODE_ID)),
  NODE_REMOVABLE("/node_removable", HttpMethod.GET, Arrays.asList(STORAGE_NODE_ID), INSTANCE_VIEW),
  WHITE_LIST_ADD_NODE("/white_list_add_node", HttpMethod.POST, Arrays.asList(STORAGE_NODE_ID)),
  WHITE_LIST_REMOVE_NODE("/white_list_remove_node", HttpMethod.POST, Arrays.asList(STORAGE_NODE_ID)),
  REMOVE_NODE("/remove_node", HttpMethod.POST, Arrays.asList(STORAGE_NODE_ID)),
  SKIP_ADMIN("/skip_admin_message", HttpMethod.POST, Arrays.asList(OFFSET)),

  GET_KEY_SCHEMA("/get_key_schema", HttpMethod.GET, Arrays.asList(NAME)),
  ADD_VALUE_SCHEMA("/add_value_schema", HttpMethod.POST,  Arrays.asList(NAME, VALUE_SCHEMA)),
  SET_OWNER("/set_owner", HttpMethod.POST, Arrays.asList(NAME, OWNER)),
  SET_PARTITION_COUNT("/set_partition_count", HttpMethod.POST, Arrays.asList(NAME, PARTITION_COUNT)),
  GET_ALL_VALUE_SCHEMA("/get_all_value_schema", HttpMethod.GET, Arrays.asList(NAME)),
  GET_VALUE_SCHEMA("/get_value_schema", HttpMethod.GET, Arrays.asList(NAME, SCHEMA_ID)),
  GET_VALUE_SCHEMA_ID("/get_value_schema_id", HttpMethod.POST, Arrays.asList(NAME, VALUE_SCHEMA)),
  MASTER_CONTROLLER("/master_controller", HttpMethod.GET, Arrays.asList()),

  EXECUTION("/execution", HttpMethod.GET, Arrays.asList(EXECUTION_ID)),
  LAST_SUCCEED_EXECUTION_ID("/last_succeed_execution_id", HttpMethod.GET, Arrays.asList()),

  STORAGE_ENGINE_OVERHEAD_RATIO("/storage_engine_overhead_ratio", HttpMethod.GET, Arrays.asList(NAME)),

  ENABLE_THROTTLING("/enable_throttling", HttpMethod.POST, Arrays.asList(STATUS)),
  ENABLE_MAX_CAPACITY_PROTECTION("/enable_max_capacity_protection", HttpMethod.POST, Arrays.asList(STATUS)),
  ENABLE_QUOTA_REBALANCED("/enable_quota_rebalanced", HttpMethod.POST, Arrays.asList(STATUS, EXPECTED_ROUTER_COUNT)),
  GET_ROUTERS_CLUSTER_CONFIG("/get_routers_cluster_config", HttpMethod.GET, Arrays.asList()),

  // TODO: those operations don't require param: cluster.
  // This could be resolved in multi-cluster support project.
  GET_ALL_MIGRATION_PUSH_STRATEGIES("/get_all_push_strategies", HttpMethod.GET, Arrays.asList()),
  SET_MIGRATION_PUSH_STRATEGY("/set_push_strategy", HttpMethod.GET, Arrays.asList(VOLDEMORT_STORE_NAME, PUSH_STRATEGY)),

  CLUSTER_DISCOVERY("/discover_cluster", HttpMethod.GET, Arrays.asList(NAME)),
  LIST_BOOTSTRAPPING_VERSIONS("/list_bootstrapping_versions", HttpMethod.GET, Arrays.asList()),

  OFFLINE_PUSH_INFO("/offline_push_info", HttpMethod.POST, Arrays.asList(NAME, VERSION)),

  UPLOAD_PUSH_JOB_STATUS("/upload_push_job_status", HttpMethod.POST, Arrays.asList(CLUSTER, NAME, VERSION, PUSH_JOB_STATUS,
      PUSH_JOB_DURATION, PUSH_JOB_ID)),

  ADD_VERSION("/add_version", HttpMethod.POST, Arrays.asList(NAME, PUSH_JOB_ID, VERSION, PARTITION_COUNT));

  private final String path;
  private final HttpMethod httpMethod;
  private final List<String> params;
  private final List<String> optionalParams;

  ControllerRoute(String path, HttpMethod httpMethod, List<String> params, String... optionalParams) {
    this.path = path;
    this.httpMethod = httpMethod;
    this.params = ListUtils.union(params, getCommonParams());
    this.optionalParams = Arrays.asList(optionalParams);
  }

  private static List<String> getCommonParams() {
    // This will work together with AdminSparkServer#validateParams
    return Arrays.asList(HOSTNAME);
  }

  public String getPath(){
    return path;
  }

  public HttpMethod getHttpMethod() {
    return this.httpMethod;
  }

  public List<String> getParams(){
    return params;
  }

  public List<String> getOptionalParams() {
    return optionalParams;
  }
}