package com.linkedin.venice.stats;

import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.meta.ReadOnlyStoreRepository;
import com.linkedin.venice.meta.Store;
import com.linkedin.venice.meta.StoreDataChangedListener;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.meta.VersionStatus;
import io.tehuti.metrics.MetricsRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

import static com.linkedin.venice.meta.Store.NON_EXISTING_VERSION;


public abstract class AbstractVeniceAggVersionedStats<STATS, STATS_REPORTER extends AbstractVeniceStatsReporter<STATS>>
    implements StoreDataChangedListener {
  private static final Logger logger = Logger.getLogger(AbstractVeniceAggVersionedStats.class);

  private final Supplier<STATS> statsInitiator;
  private final StatsSupplier<STATS_REPORTER> reporterSupplier;

  private final ReadOnlyStoreRepository metadataRepository;
  private final MetricsRepository metricsRepository;

  private final Map<String, VeniceVersionedStats<STATS, STATS_REPORTER>> aggStats;

  public AbstractVeniceAggVersionedStats(MetricsRepository metricsRepository, ReadOnlyStoreRepository metadataRepository,
      Supplier<STATS> statsInitiator, StatsSupplier<STATS_REPORTER> reporterSupplier) {
    this.metadataRepository = metadataRepository;
    this.metricsRepository = metricsRepository;
    this.statsInitiator = statsInitiator;
    this.reporterSupplier = reporterSupplier;

    this.aggStats = new HashMap<>();
    metadataRepository.registerStoreDataChangedListener(this);
    loadAllStats();
  }

  synchronized void loadAllStats() {
    metadataRepository.getAllStores().forEach(store -> {
      addStore(store.getName());
      updateStatsVersionInfo(store);
    });
  }

  protected STATS getTotalStats(String storeName) {
    return getVersionedStats(storeName).getTotalStats();
  }

  protected STATS getStats(String storeName, int version) {
    return getVersionedStats(storeName).getStats(version);
  }

  private VeniceVersionedStats<STATS, STATS_REPORTER> getVersionedStats(String storeName) {
    if (!aggStats.containsKey(storeName)) {
      addStore(storeName);
      Store store = metadataRepository.getStore(storeName);
      if (null == store) {
        throw new VeniceException("Unknown store: " + storeName);
      }
      updateStatsVersionInfo(store);
    }

    return aggStats.get(storeName);
  }

  private synchronized void addStore(String storeName) {
    if (!aggStats.containsKey(storeName)) {
      aggStats.put(storeName, new VeniceVersionedStats<>(metricsRepository, storeName, statsInitiator, reporterSupplier));
    } else {
      logger.warn("VersionedStats has already been created. Something might be wrong. "
          + "Store: " + storeName);
    }
  }

  private void updateStatsVersionInfo(Store store) {
    VeniceVersionedStats<STATS, STATS_REPORTER> versionedDIVStats = getVersionedStats(store.getName());

    int newCurrentVersion = store.getCurrentVersion();
    if (newCurrentVersion != versionedDIVStats.getCurrentVersion()) {
      versionedDIVStats.setCurrentVersion(newCurrentVersion);
    }

    List<Version> existingVersions = store.getVersions();
    List<Integer> existingVersionNumbers =
        existingVersions.stream().map(Version::getNumber).collect(Collectors.toList());

    //remove old versions except version 0. Version 0 is the default version when a store is created. Since no one will
    //report to it, it is always "empty". We use it to reset reporters. eg. when a topic goes from in-flight to current,
    //we reset in-flight reporter to version 0.
    versionedDIVStats.getAllVersionNumbers().stream()
        .filter(versionNum -> !existingVersionNumbers.contains(versionNum) && versionNum != NON_EXISTING_VERSION)
        .forEach(versionNum -> versionedDIVStats.removeVersion(versionNum));

    int futureVersion = NON_EXISTING_VERSION;
    int backupVersion = NON_EXISTING_VERSION;
    for (Version version : existingVersions) {
      int versionNum = version.getNumber();

      //add this version to stats if it is absent
      if (!versionedDIVStats.containsVersion(versionNum)) {
        versionedDIVStats.addVersion(versionNum);
      }

      VersionStatus status = version.getStatus();
      if (status == VersionStatus.STARTED || status == VersionStatus.PUSHED) {
        if (futureVersion != NON_EXISTING_VERSION) {
          logger.warn(
              "Multiple versions have been marked as STARTED PUSHING. " + "There might be a parallel push. Store: " + store.getName());
        }

        //in case there is a parallel push, record the largest version as future version
        if (futureVersion < versionNum) {
          futureVersion = versionNum;
        }
      } else {
        //check past version
        if (status == VersionStatus.ONLINE && versionNum != newCurrentVersion) {
          if (backupVersion != 0) {
            logger.warn("There are more than 1 backup versions. Something might be wrong." + "Store: " + store.getName());
          }

          backupVersion = versionNum;
        }
      }
    }

    if (futureVersion != versionedDIVStats.getFutureVersion()) {
      versionedDIVStats.setFutureVersion(futureVersion);
    }
    if (backupVersion != versionedDIVStats.getBackupVersion()) {
      versionedDIVStats.setBackupVersion(backupVersion);
    }
  }

  @Override
  public void handleStoreCreated(Store store) {
    addStore(store.getName());
  }

  @Override
  public void handleStoreDeleted(String storeName) {
    if (!aggStats.containsKey(storeName)) {
      logger.warn("Trying to delete stats but store: " + storeName + "is not in the metric list. Something might be wrong.");
    }

    //aggStats.remove(storeName); //TODO: sdwu to make a more permanent solution
  }

  @Override
  public void handleStoreChanged(Store store) {
    updateStatsVersionInfo(store);
  }
}