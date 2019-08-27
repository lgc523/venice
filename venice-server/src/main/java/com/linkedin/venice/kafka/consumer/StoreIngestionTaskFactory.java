package com.linkedin.venice.kafka.consumer;

import com.linkedin.venice.config.VeniceServerConfig;
import com.linkedin.venice.config.VeniceStoreConfig;
import com.linkedin.venice.kafka.TopicManager;
import com.linkedin.venice.meta.HybridStoreConfig;
import com.linkedin.venice.meta.ReadOnlySchemaRepository;
import com.linkedin.venice.notifier.VeniceNotifier;
import com.linkedin.venice.server.StoreRepository;
import com.linkedin.venice.stats.AggStoreIngestionStats;
import com.linkedin.venice.stats.AggVersionedDIVStats;
import com.linkedin.venice.storage.StorageMetadataService;
import com.linkedin.venice.throttle.EventThrottler;
import com.linkedin.venice.utils.DiskUsage;
import com.linkedin.venice.writer.VeniceWriterFactory;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import javax.validation.constraints.NotNull;


public class StoreIngestionTaskFactory {
  private final Builder builder;

  /**
   * Make constructor as private on purpose to force user build the factory
   * using the builder.
   */
  private StoreIngestionTaskFactory(Builder builder) {
    this.builder = builder;
  }

  public StoreIngestionTask getNewIngestionTask(
      boolean isLeaderFollowerModelEnabled,
      @NotNull Properties kafkaConsumerProperties,
      @NotNull BooleanSupplier isCurrentVersion,
      @NotNull Optional<HybridStoreConfig> hybridStoreConfig,
      boolean isIncrementalPushEnabled,
      @NotNull VeniceStoreConfig storeConfig,
      boolean bufferReplayEnabledForHybrid
  ) {
    if (isLeaderFollowerModelEnabled) {
      return new LeaderFollowerStoreIngestionTask(builder.veniceWriterFactory, builder.veniceConsumerFactory, kafkaConsumerProperties,
          builder.storeRepository, builder.storageMetadataService, builder.notifiers, builder.bandwidthThrottler, builder.recordsThrottler,
          builder.schemaRepo, builder.topicManager, builder.ingestionStats, builder.versionedDIVStats, builder.storeBufferService, isCurrentVersion,
          hybridStoreConfig, isIncrementalPushEnabled, storeConfig, builder.diskUsage, bufferReplayEnabledForHybrid, builder.serverConfig);
    } else {
      return new OnlineOfflineStoreIngestionTask(builder.veniceWriterFactory, builder.veniceConsumerFactory, kafkaConsumerProperties,
          builder.storeRepository, builder.storageMetadataService, builder.notifiers, builder.bandwidthThrottler, builder.recordsThrottler,
          builder.schemaRepo, builder.topicManager, builder.ingestionStats, builder.versionedDIVStats, builder.storeBufferService, isCurrentVersion,
          hybridStoreConfig, isIncrementalPushEnabled, storeConfig, builder.diskUsage, bufferReplayEnabledForHybrid, builder.serverConfig);
    }
  }

  /**
   * @return a new builder for the {@link StoreIngestionTaskFactory}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for {@link StoreIngestionTaskFactory}; once the build() function is invoked,
   * no modification is allowed on any parameters.
   */
  public static class Builder {
    private volatile boolean built = false;

    private VeniceWriterFactory veniceWriterFactory;
    private VeniceConsumerFactory veniceConsumerFactory;
    private StoreRepository storeRepository;
    private StorageMetadataService storageMetadataService;
    private Queue<VeniceNotifier> notifiers;
    private EventThrottler bandwidthThrottler;
    private EventThrottler recordsThrottler;
    private ReadOnlySchemaRepository schemaRepo;
    private TopicManager topicManager;
    private AggStoreIngestionStats ingestionStats;
    private AggVersionedDIVStats versionedDIVStats;
    private StoreBufferService storeBufferService;
    private VeniceServerConfig serverConfig;
    private DiskUsage diskUsage;

    public StoreIngestionTaskFactory build() {
      // flip the build flag to true
      this.built = true;
      return new StoreIngestionTaskFactory(this);
    }

    public Builder setVeniceWriterFactory(VeniceWriterFactory writerFactory) {
      if (!built) {
        this.veniceWriterFactory = writerFactory;
      }
      return this;
    }

    public Builder setVeniceConsumerFactory(VeniceConsumerFactory consumerFactory) {
      if (!built) {
        this.veniceConsumerFactory = consumerFactory;
      }
      return this;
    }

    public Builder setStoreRepository(StoreRepository storeRepository) {
      if (!built) {
        this.storeRepository = storeRepository;
      }
      return this;
    }

    public Builder setStorageMetadataService(StorageMetadataService storageMetadataService) {
      if (!built) {
        this.storageMetadataService = storageMetadataService;
      }
      return this;
    }

    public Builder setNotifiersQueue(Queue<VeniceNotifier> notifiers) {
      if (!built) {
        this.notifiers = notifiers;
      }
      return this;
    }

    public Builder setBandwidthThrottler(EventThrottler bandwidthThrottler) {
      if (!built) {
        this.bandwidthThrottler = bandwidthThrottler;
      }
      return this;
    }

    public Builder setRecordsThrottler(EventThrottler recordsThrottler) {
      if (!built) {
        this.recordsThrottler = recordsThrottler;
      }
      return this;
    }

    public Builder setSchemaRepository(ReadOnlySchemaRepository schemaRepo) {
      if (!built) {
        this.schemaRepo = schemaRepo;
      }
      return this;
    }

    public Builder setTopicManager(TopicManager topicManager) {
      if (!built) {
        this.topicManager = topicManager;
      }
      return this;
    }

    public Builder setStoreIngestionStats(AggStoreIngestionStats storeIngestionStats) {
      if (!built) {
        this.ingestionStats = storeIngestionStats;
      }
      return this;
    }

    public Builder setVersionedDIVStats(AggVersionedDIVStats versionedDIVStats) {
      if (!built) {
        this.versionedDIVStats = versionedDIVStats;
      }
      return this;
    }

    public Builder setStoreBufferService(StoreBufferService storeBufferService) {
      if (!built) {
        this.storeBufferService = storeBufferService;
      }
      return this;
    }

    public Builder setServerConfig(VeniceServerConfig serverConfig) {
      if (!built) {
        this.serverConfig = serverConfig;
      }
      return this;
    }

    public Builder setDiskUsage(DiskUsage diskUsage) {
      if (!built) {
        this.diskUsage = diskUsage;
      }
      return this;
    }
  }
}