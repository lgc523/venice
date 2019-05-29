package com.linkedin.venice.controllerapi;

public class ControllerApiConstants {

  public static final String HOSTNAME = "hostname";
  public static final String CLUSTER = "cluster_name";
  public static final String CLUSTER_SRC = "cluster_name_src";
  public static final String CLUSTER_DEST = "cluster_name_dest";

  public static final String NAME = "store_name";
  public static final String OWNER = "owner";
  public static final String PARTITION_COUNT = "partition_count";
  public static final String STORE_SIZE = "store_size";
  public static final String VERSION = "version";
  public static final String INCREMENTAL_PUSH_VERSION = "incremental_push_version";
  public static final String STATUS = "status";
  public static final String FROZEN = "frozen";
  public static final String ERROR = "error";
  public static final String STORAGE_NODE_ID = "storage_node_id"; /* host_port */
  public static final String INSTANCE_VIEW = "instance_view";
  public static final String KEY_SCHEMA = "key_schema";
  public static final String VALUE_SCHEMA = "value_schema";
  public static final String SCHEMA_ID = "schema_id";
  public static final String TOPIC = "topic"; // TODO remove this, everyone should use store and version instead
  public static final String OFFSET = "offset";
  public static final String OPERATION = "operation";
  public static final String READ_OPERATION = "read";
  public static final String WRITE_OPERATION = "write";
  public static final String READ_WRITE_OPERATION = READ_OPERATION + WRITE_OPERATION;
  public static final String EXECUTION_ID = "execution_id";
  public static final String ENABLE_READS = "enable_reads";
  public static final String ENABLE_WRITES = "enable_writes";
  public static final String STORAGE_QUOTA_IN_BYTE = "storage_quota_in_byte";
  public static final String READ_QUOTA_IN_CU = "read_quota_in_cu";
  public static final String REWIND_TIME_IN_SECONDS = "rewind_time_seconds";
  public static final String OFFSET_LAG_TO_GO_ONLINE = "offset_lag_to_go_online";
  public static final String COMPRESSION_STRATEGY = "compression_strategy";
  public static final String CHUNKING_ENABLED = "chunking_enabled";
  public static final String INCREMENTAL_PUSH_ENABLED = "incremental_push_enabled";
  public static final String SINGLE_GET_ROUTER_CACHE_ENABLED = "single_get_router_cache_enabled";
  public static final String BATCH_GET_ROUTER_CACHE_ENABLED = "batch_get_router_cache_enabled";
  public static final String BATCH_GET_LIMIT = "batch_get_limit";
  public static final String LARGEST_USED_VERSION_NUMBER = "largest_used_version_number";
  public static final String NUM_VERSIONS_TO_PRESERVE = "num_versions_to_preserve";
  public static final String MESSAGE = "message";

  public static final String PUSH_TYPE = "push_type";
  public static final String PUSH_JOB_ID = "push_job_id";
  public static final String SEND_START_OF_PUSH = "start_of_push";

  public static final String EXPECTED_ROUTER_COUNT = "expected_router_count";

  public static final String VOLDEMORT_STORE_NAME = "voldemort_store_name";
  public static final String PUSH_STRATEGY = "push_strategy";

  public static final String ACCESS_CONTROLLED = "access_controlled";
  public static final String STORE_MIGRATION = "store_migration";

  public static final String PUSH_JOB_STATUS = "push_job_status";
  public static final String PUSH_JOB_DURATION = "push_job_duration";

  public static final String WRITE_COMPUTATION_ENABLED = "write_computation_enabled";
  public static final String READ_COMPUTATION_ENABLED = "read_computation_enabled";
  public static final String BOOTSTRAP_TO_ONLINE_TIMEOUT_IN_HOURS = "bootstrap_to_online_timeout_in_hours";

  public static final String LEADER_FOLLOWER_MODEL_ENABLED = "leader_follower_model_enabled";
  public static final String INCLUDE_SYSTEM_STORES = "include_system_stores";

  public static final String BACKUP_STRATEGY = "backup_strategy";


  public static final String SKIP_DIV = "skip_div";

  private ControllerApiConstants(){}

  /**
   * Producer type for pushing data
   */
  public enum PushType {
    BATCH, //This is a batch push that will create a new version
    INCREMENTAL, //This is a batch push that will re-use current version's topic
    STREAM //This is a stream job that writes into a buffer topic (or possibly the current version topic depending on store-level configs)
  }

  /**
   * Possible push job status at the end of its lifecycle.

  public enum PushJobStatus {
    SUCCESS, // The job succeeded
    ERROR, // Some error has occurred during the job
    KILLED // The job was terminated by user
  }*/
}