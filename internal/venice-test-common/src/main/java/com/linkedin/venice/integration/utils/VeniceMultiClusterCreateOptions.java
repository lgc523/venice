package com.linkedin.venice.integration.utils;

import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_DELAYED_TO_REBALANCE_MS;
import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_NUMBER_OF_CONTROLLERS;
import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_NUMBER_OF_ROUTERS;
import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_NUMBER_OF_SERVERS;
import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_PARTITION_SIZE_BYTES;
import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_REPLICATION_FACTOR;
import static com.linkedin.venice.integration.utils.VeniceClusterWrapperConstants.DEFAULT_SSL_TO_STORAGE_NODES;

import com.linkedin.venice.utils.VeniceProperties;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;


public class VeniceMultiClusterCreateOptions {
  private final String coloName;
  private final int numberOfClusters;
  private final int numberOfControllers;
  private final int numberOfServers;
  private final int numberOfRouters;
  private final int replicationFactor;
  private final int partitionSize;
  private final int minActiveReplica;
  private final long rebalanceDelayMs;
  private final boolean enableAllowlist;
  private final boolean enableAutoJoinAllowlist;
  private final boolean sslToStorageNodes;
  private final boolean randomizeClusterName;
  private final boolean multiColoSetup;
  private final boolean multiD2;
  private final boolean forkServer;
  private final Map<String, Map<String, String>> kafkaClusterMap;
  private final ZkServerWrapper zkServerWrapper;
  private final KafkaBrokerWrapper kafkaBrokerWrapper;
  private final Properties childControllerProperties;
  private final VeniceProperties veniceProperties;

  public String getColoName() {
    return coloName;
  }

  public int getNumberOfClusters() {
    return numberOfClusters;
  }

  public int getNumberOfControllers() {
    return numberOfControllers;
  }

  public int getNumberOfServers() {
    return numberOfServers;
  }

  public int getNumberOfRouters() {
    return numberOfRouters;
  }

  public int getReplicationFactor() {
    return replicationFactor;
  }

  public int getPartitionSize() {
    return partitionSize;
  }

  public int getMinActiveReplica() {
    return minActiveReplica;
  }

  public long getRebalanceDelayMs() {
    return rebalanceDelayMs;
  }

  public boolean isEnableAllowlist() {
    return enableAllowlist;
  }

  public boolean isEnableAutoJoinAllowlist() {
    return enableAutoJoinAllowlist;
  }

  public boolean isSslToStorageNodes() {
    return sslToStorageNodes;
  }

  public boolean isRandomizeClusterName() {
    return randomizeClusterName;
  }

  public boolean isMultiColoSetup() {
    return multiColoSetup;
  }

  public boolean isMultiD2() {
    return multiD2;
  }

  public boolean isForkServer() {
    return forkServer;
  }

  public Map<String, Map<String, String>> getKafkaClusterMap() {
    return kafkaClusterMap;
  }

  public ZkServerWrapper getZkServerWrapper() {
    return zkServerWrapper;
  }

  public KafkaBrokerWrapper getKafkaBrokerWrapper() {
    return kafkaBrokerWrapper;
  }

  public Properties getChildControllerProperties() {
    return childControllerProperties;
  }

  public VeniceProperties getVeniceProperties() {
    return veniceProperties;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("VeniceMultiClusterCreateOptions - ")
        .append("coloName:")
        .append(coloName)
        .append(", ")
        .append("clusters:")
        .append(numberOfClusters)
        .append(", ")
        .append("controllers:")
        .append(numberOfControllers)
        .append(", ")
        .append("servers:")
        .append(numberOfServers)
        .append(", ")
        .append("routers:")
        .append(numberOfRouters)
        .append(", ")
        .append("replicationFactor:")
        .append(replicationFactor)
        .append(", ")
        .append("rebalanceDelayMs:")
        .append(rebalanceDelayMs)
        .append(", ")
        .append("partitionSize:")
        .append(partitionSize)
        .append(", ")
        .append("minActiveReplica:")
        .append(minActiveReplica)
        .append(", ")
        .append("enableAllowlist:")
        .append(enableAllowlist)
        .append(", ")
        .append("enableAutoJoinAllowlist:")
        .append(enableAutoJoinAllowlist)
        .append(", ")
        .append("sslToStorageNodes:")
        .append(sslToStorageNodes)
        .append(", ")
        .append("forkServer:")
        .append(forkServer)
        .append(", ")
        .append("multiColoSetup:")
        .append(multiColoSetup)
        .append(", ")
        .append("randomizeClusterName:")
        .append(randomizeClusterName)
        .append(", ")
        .append("childControllerProperties:")
        .append(childControllerProperties)
        .append(", ")
        .append("veniceProperties:")
        .append(veniceProperties)
        .append(", ")
        .append("multiD2:")
        .append(multiD2)
        .append(", ")
        .append("zk:")
        .append(zkServerWrapper == null ? "null" : zkServerWrapper.getAddress())
        .append(", ")
        .append("kafka:")
        .append(kafkaBrokerWrapper == null ? "null" : kafkaBrokerWrapper.getAddress())
        .append(", ")
        .append("kafkaClusterMap:")
        .append(kafkaClusterMap)
        .toString();
  }

  public VeniceMultiClusterCreateOptions(Builder builder) {
    coloName = builder.coloName;
    numberOfClusters = builder.numberOfClusters;
    numberOfControllers = builder.numberOfControllers;
    numberOfServers = builder.numberOfServers;
    numberOfRouters = builder.numberOfRouters;
    replicationFactor = builder.replicationFactor;
    partitionSize = builder.partitionSize;
    enableAllowlist = builder.enableAllowlist;
    enableAutoJoinAllowlist = builder.enableAutoJoinAllowlist;
    rebalanceDelayMs = builder.rebalanceDelayMs;
    minActiveReplica = builder.minActiveReplica;
    sslToStorageNodes = builder.sslToStorageNodes;
    randomizeClusterName = builder.randomizeClusterName;
    multiColoSetup = builder.multiColoSetup;
    zkServerWrapper = builder.zkServerWrapper;
    kafkaBrokerWrapper = builder.kafkaBrokerWrapper;
    childControllerProperties = builder.childControllerProperties;
    veniceProperties = builder.veniceProperties;
    multiD2 = builder.multiD2;
    forkServer = builder.forkServer;
    kafkaClusterMap = builder.kafkaClusterMap;
  }

  public static class Builder {
    private String coloName = "";
    private final int numberOfClusters;
    private int numberOfControllers = DEFAULT_NUMBER_OF_CONTROLLERS;
    private int numberOfServers = DEFAULT_NUMBER_OF_SERVERS;
    private int numberOfRouters = DEFAULT_NUMBER_OF_ROUTERS;
    private int replicationFactor = DEFAULT_REPLICATION_FACTOR;
    private int partitionSize = DEFAULT_PARTITION_SIZE_BYTES;
    private int minActiveReplica;
    private long rebalanceDelayMs = DEFAULT_DELAYED_TO_REBALANCE_MS;
    private boolean enableAllowlist = false;
    private boolean enableAutoJoinAllowlist = false;
    private boolean sslToStorageNodes = DEFAULT_SSL_TO_STORAGE_NODES;
    private boolean randomizeClusterName = true;
    private boolean multiColoSetup = false;
    private boolean multiD2 = false;
    private boolean forkServer = false;
    private boolean isMinActiveReplicaSet = false;
    private Map<String, Map<String, String>> kafkaClusterMap;
    private ZkServerWrapper zkServerWrapper;
    private KafkaBrokerWrapper kafkaBrokerWrapper;
    private Properties childControllerProperties;
    private VeniceProperties veniceProperties;

    public Builder(int numberOfClusters) {
      this.numberOfClusters = numberOfClusters;
    }

    public Builder coloName(String coloName) {
      this.coloName = coloName;
      return this;
    }

    public Builder numberOfControllers(int numberOfControllers) {
      this.numberOfControllers = numberOfControllers;
      return this;
    }

    public Builder numberOfServers(int numberOfServers) {
      this.numberOfServers = numberOfServers;
      return this;
    }

    public Builder numberOfRouters(int numberOfRouters) {
      this.numberOfRouters = numberOfRouters;
      return this;
    }

    public Builder replicationFactor(int replicationFactor) {
      this.replicationFactor = replicationFactor;
      return this;
    }

    public Builder partitionSize(int partitionSize) {
      this.partitionSize = partitionSize;
      return this;
    }

    public Builder minActiveReplica(int minActiveReplica) {
      this.minActiveReplica = minActiveReplica;
      this.isMinActiveReplicaSet = true;
      return this;
    }

    public Builder rebalanceDelayMs(long rebalanceDelayMs) {
      this.rebalanceDelayMs = rebalanceDelayMs;
      return this;
    }

    public Builder enableAllowlist(boolean enableAllowlist) {
      this.enableAllowlist = enableAllowlist;
      return this;
    }

    public Builder enableAutoJoinAllowlist(boolean enableAutoJoinAllowlist) {
      this.enableAutoJoinAllowlist = enableAutoJoinAllowlist;
      return this;
    }

    public Builder sslToStorageNodes(boolean sslToStorageNodes) {
      this.sslToStorageNodes = sslToStorageNodes;
      return this;
    }

    public Builder randomizeClusterName(boolean randomizeClusterName) {
      this.randomizeClusterName = randomizeClusterName;
      return this;
    }

    public Builder multiColoSetup(boolean multiColoSetup) {
      this.multiColoSetup = multiColoSetup;
      return this;
    }

    public Builder multiD2(boolean multiD2) {
      this.multiD2 = multiD2;
      return this;
    }

    public Builder forkServer(boolean forkServer) {
      this.forkServer = forkServer;
      return this;
    }

    public Builder kafkaClusterMap(Map<String, Map<String, String>> kafkaClusterMap) {
      this.kafkaClusterMap = kafkaClusterMap;
      return this;
    }

    public Builder zkServerWrapper(ZkServerWrapper zkServerWrapper) {
      this.zkServerWrapper = zkServerWrapper;
      return this;
    }

    public Builder kafkaBrokerWrapper(KafkaBrokerWrapper kafkaBrokerWrapper) {
      this.kafkaBrokerWrapper = kafkaBrokerWrapper;
      return this;
    }

    public Builder childControllerProperties(Properties childControllerProperties) {
      this.childControllerProperties = childControllerProperties;
      return this;
    }

    public Builder veniceProperties(VeniceProperties veniceProperties) {
      this.veniceProperties = veniceProperties;
      return this;
    }

    private void addDefaults() {
      if (!isMinActiveReplicaSet) {
        minActiveReplica = replicationFactor - 1;
      }
      if (childControllerProperties == null) {
        childControllerProperties = new Properties();
      }
      if (veniceProperties == null) {
        veniceProperties = new VeniceProperties();
      }
      if (kafkaClusterMap == null) {
        kafkaClusterMap = Collections.emptyMap();
      }
    }

    public VeniceMultiClusterCreateOptions build() {
      addDefaults();
      return new VeniceMultiClusterCreateOptions(this);
    }
  }
}
