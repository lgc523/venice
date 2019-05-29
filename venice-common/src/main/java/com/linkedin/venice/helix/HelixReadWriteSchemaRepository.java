package com.linkedin.venice.helix;

import com.linkedin.venice.schema.avro.DirectionalSchemaCompatibilityType;
import com.linkedin.venice.utils.AvroSchemaUtils;
import com.linkedin.venice.exceptions.StoreKeySchemaExistException;
import com.linkedin.venice.exceptions.SchemaIncompatibilityException;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.exceptions.VeniceNoStoreException;
import com.linkedin.venice.meta.ReadWriteSchemaRepository;
import com.linkedin.venice.meta.ReadWriteStoreRepository;
import com.linkedin.venice.meta.Store;
import com.linkedin.venice.schema.SchemaData;
import com.linkedin.venice.schema.SchemaEntry;
import java.util.List;
import org.apache.helix.AccessOption;
import org.apache.helix.manager.zk.ZkBaseDataAccessor;
import org.apache.helix.manager.zk.ZkClient;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * This class is used to add schema entries for stores.
 */
public class HelixReadWriteSchemaRepository implements ReadWriteSchemaRepository {
  private Logger logger = Logger.getLogger(HelixReadWriteSchemaRepository.class);

  private ZkBaseDataAccessor<SchemaEntry> dataAccessor;

  // Store repository to check store related info
  private ReadWriteStoreRepository storeRepository;

  // Venice cluster name
  private String clusterName;

  public HelixReadWriteSchemaRepository(@NotNull ReadWriteStoreRepository storeRepository,
                                        @NotNull ZkClient zkClient,
                                        @NotNull HelixAdapterSerializer adapter,
                                        @NotNull String clusterName) {
    // Comment out store repo refresh for now, need to revisit it later.
    //storeRepository.refresh();
    this.storeRepository = storeRepository;
    this.clusterName = clusterName;
    // Register schema serializer
    HelixReadOnlySchemaRepository.registerSerializerForSchema(clusterName, zkClient, adapter);
    this.dataAccessor = new ZkBaseDataAccessor<>(zkClient);

    // Register store data change listener
    storeRepository.registerStoreDataChangedListener(this);
  }

  /**
   * Get key schema for the given store.
   * Fetch from zookeeper directly.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException} if the store doesn't exist;
   *
   * @param storeName
   * @return
   *    null if key schema doesn't exist;
   *    schema entry if exists;
   */
  @Override
  public SchemaEntry getKeySchema(String storeName) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName, clusterName);
    }
    String keySchemaPath = HelixReadOnlySchemaRepository.getKeySchemaPath(clusterName, storeName);
    return dataAccessor.get(keySchemaPath, null, AccessOption.PERSISTENT);
  }

  /**
   * Get value schema for the given store and schema id.
   * Fetch from zookeeper directly.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException} if the store doesn't exist;
   *
   * @param storeName
   * @param id
   * @return
   *    null if the schema doesn't exist;
   *    schema entry if exists;
   */
  @Override
  public SchemaEntry getValueSchema(String storeName, int id) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName);
    }
    String valueSchemaPath = HelixReadOnlySchemaRepository.getValueSchemaPath(clusterName,
        storeName, Integer.toString(id));
    return dataAccessor.get(valueSchemaPath, null, AccessOption.PERSISTENT);
  }

  /**
   * Check whether the given value schema id exists in the given store or not.
   * Fetch from zookeeper directly.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException} if the store doesn't exist;
   *
   * @param storeName
   * @param id
   * @return
   *    null if the schema doesn't exist;
   *    schema entry if exists;
   */
  @Override
  public boolean hasValueSchema(String storeName, int id) {
    return null != getValueSchema(storeName, id);
  }

  /**
   * This function is used to retrieve value schema id for the given store and schema. Attempts to get the schema that
   * matches exactly. If multiple matching schemas are found then the id of the latest added schema is returned.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException} if the store doesn't exist;
   * @throws {@link org.apache.avro.SchemaParseException} if the schema is invalid;
   *
   * @param storeName
   * @param valueSchemaStr
   * @return
   *    {@link com.linkedin.venice.schema.SchemaData#INVALID_VALUE_SCHEMA_ID}, if the schema doesn't exist in the given store;
   *    schema id (int), if the schema exists in the given store;
   */
  @Override
  public int getValueSchemaId(String storeName, String valueSchemaStr) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName);
    }
    Collection<SchemaEntry> valueSchemas = getValueSchemas(storeName);
    SchemaEntry valueSchemaEntry = new SchemaEntry(SchemaData.INVALID_VALUE_SCHEMA_ID, valueSchemaStr);
    List<SchemaEntry> canonicalizedMatches = AvroSchemaUtils.filterCanonicalizedSchemas(valueSchemaEntry, valueSchemas);
    int schemaId = SchemaData.INVALID_VALUE_SCHEMA_ID;
    if (!canonicalizedMatches.isEmpty()) {
      if (canonicalizedMatches.size() == 1) {
        schemaId = canonicalizedMatches.iterator().next().getId();
      } else {
        List<SchemaEntry> exactMatches = AvroSchemaUtils.filterSchemas(valueSchemaEntry, canonicalizedMatches);
        if (exactMatches.isEmpty()) {
          schemaId = getSchemaEntryWithLargestId(canonicalizedMatches).getId();
        } else {
          schemaId = getSchemaEntryWithLargestId(exactMatches).getId();
        }
      }
    }
    return schemaId;
  }

  private SchemaEntry getSchemaEntryWithLargestId(Collection<SchemaEntry> schemas) {
    SchemaEntry largestIdSchema = schemas.iterator().next();
    for (SchemaEntry schema : schemas) {
      if (schema.getId() > largestIdSchema.getId()) {
        largestIdSchema = schema;
      }
    }
    return largestIdSchema;
  }

  /**
   * This function is used to retrieve all the value schemas for the given store.
   * Fetch from zookeeper directly.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException} if the store doesn't exist;
   *
   * @param storeName
   * @return
   */
  @Override
  public Collection<SchemaEntry> getValueSchemas(String storeName) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName);
    }
    String valueSchemaParentPath = HelixReadOnlySchemaRepository.getValueSchemaParentPath(clusterName, storeName);
    return dataAccessor.getChildren(valueSchemaParentPath, null, AccessOption.PERSISTENT);
  }

  @Override
  public SchemaEntry getLatestValueSchema(String storeName) {
    Collection<SchemaEntry>  valueSchemas = getValueSchemas(storeName);
    int maxValueSchemaId = -1;
    SchemaEntry latestSchema = null;
    for (SchemaEntry schema : valueSchemas) {
      if (schema.getId() > maxValueSchemaId) {
        maxValueSchemaId = schema.getId();
        latestSchema = schema;
      }
    }
    return latestSchema;
  }

  /**
   * Set up key schema for the given store.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException}, if store doesn't exist;
   * @throws {@link com.linkedin.venice.exceptions.StoreKeySchemaExistException}, if key schema already exists;
   * @throws {@link org.apache.avro.SchemaParseException}, if key schema is invalid;
   * @throws {@link com.linkedin.venice.exceptions.VeniceException}, if zookeeper update fails;
   *
   * @param storeName
   * @param schemaStr
   * @return
   */
  @Override
  public SchemaEntry initKeySchema(String storeName, String schemaStr) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName);
    }
    SchemaEntry keySchema = new SchemaEntry(Integer.parseInt(HelixReadOnlySchemaRepository.KEY_SCHEMA_ID), schemaStr);
    SchemaEntry existingKeySchema = getKeySchema(storeName);
    if (null != existingKeySchema) {
      if (existingKeySchema.equals(keySchema)) {
        return existingKeySchema;
      } else {
        throw StoreKeySchemaExistException.newExceptionForStore(storeName);
      }
    }
    boolean ret = dataAccessor.create(HelixReadOnlySchemaRepository.getKeySchemaPath(clusterName, storeName),
        keySchema, AccessOption.PERSISTENT);
    if (!ret) {
      throw new VeniceException("Failed to set key schema: " + keySchema + " for store: " + storeName);
    }
    logger.info("Setup key schema: " + schemaStr + " for store: " + storeName);

    return keySchema;
  }

  /**
   * Add new value schema for the given store.
   *
   * @throws {@link com.linkedin.venice.exceptions.VeniceNoStoreException} if the store doesn't exist;
   * @throws {@link org.apache.avro.SchemaParseException}, if key schema is invalid;
   * @throws {@link SchemaIncompatibilityException}, if the new schema is
   *  incompatible with the previous value schemas;
   * @throws {@link com.linkedin.venice.exceptions.VeniceException}, if updating zookeeper fails;
   *
   * @param storeName
   * @param schemaStr
   * @return
   *    schema entry if the schema is successfully added or already exists.
   */
  @Override
  public SchemaEntry addValueSchema(String storeName, String schemaStr, DirectionalSchemaCompatibilityType expectedCompatibilityType) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName);
    }
    SchemaEntry newValueSchemaWithInvalidId = new SchemaEntry(SchemaData.INVALID_VALUE_SCHEMA_ID, schemaStr);
    Collection<SchemaEntry> valueSchemas = getValueSchemas(storeName);
    int maxValueSchemaId = SchemaData.INVALID_VALUE_SCHEMA_ID;
    for (SchemaEntry entry : valueSchemas) {
      if (entry.equals(newValueSchemaWithInvalidId)) {
        return entry;
      }
      if (!entry.isNewSchemaCompatible(newValueSchemaWithInvalidId, expectedCompatibilityType)) {
        throw new SchemaIncompatibilityException(entry, newValueSchemaWithInvalidId);
      }
      int entryId = entry.getId();
      if (entryId > maxValueSchemaId) {
        maxValueSchemaId = entryId;
      }
    }
    int newValueSchemaId = HelixReadOnlySchemaRepository.VALUE_SCHEMA_STARTING_ID;
    if (SchemaData.INVALID_VALUE_SCHEMA_ID != maxValueSchemaId) {
      newValueSchemaId = maxValueSchemaId + 1;
    }
    SchemaEntry newValueSchema = new SchemaEntry(newValueSchemaId, newValueSchemaWithInvalidId.getSchema());
    String newValueSchemaPath = HelixReadOnlySchemaRepository.getValueSchemaPath(clusterName,
        storeName, Integer.toString(newValueSchemaId));
    boolean ret = dataAccessor.create(newValueSchemaPath, newValueSchema, AccessOption.PERSISTENT);
    if (!ret) {
      throw new VeniceException("Failed to create value schema: " + schemaStr + " for store: " + storeName);
    }
    logger.info("Add value schema: " + newValueSchema.toString() + " for store: " + storeName);

    return newValueSchema;
  }

  @Override
  public SchemaEntry addValueSchema(String storeName, String schemaStr, int schemaId, DirectionalSchemaCompatibilityType expectedCompatibilityType) {
    if (!storeRepository.hasStore(storeName)) {
      throw new VeniceNoStoreException(storeName);
    }
    SchemaEntry newValueSchemaEntry = new SchemaEntry(schemaId, schemaStr);
    Collection<SchemaEntry> valueSchemas = getValueSchemas(storeName);
    for (SchemaEntry entry : valueSchemas) {
      // If the new schema is not compatible with existing schema, here still throw an exception
      /**
       * TODO: Consider removing this check, since it is already performed prior to calling this in
       * {@link com.linkedin.venice.controller.VeniceHelixAdmin#checkPreConditionForAddValueSchemaAndGetNewSchemaId()}
       */
      if (!entry.isNewSchemaCompatible(newValueSchemaEntry, expectedCompatibilityType)) {
        throw new SchemaIncompatibilityException(entry, newValueSchemaEntry);
      }
    }
    String newValueSchemaPath = HelixReadOnlySchemaRepository.getValueSchemaPath(clusterName,
        storeName, Integer.toString(schemaId));
    boolean ret = dataAccessor.set(newValueSchemaPath, newValueSchemaEntry, AccessOption.PERSISTENT);
    if (!ret) {
      throw new VeniceException("Failed to set value schema: " + schemaStr + " for store: " + storeName);
    }
    logger.info("Setup value schema: " + newValueSchemaEntry.toString() + " with schema id: "
        + schemaId + " for store: " + storeName);

    return newValueSchemaEntry;
  }

  /**
   * Create schema parent node when store node is firstly created in Zookeeper.
   *
   * @param store
   */
  @Override
  public void handleStoreCreated(Store store) {
    // Create key-schema/value-schema folder
    String storeName = store.getName();
    dataAccessor.create(HelixReadOnlySchemaRepository.getKeySchemaParentPath(clusterName, storeName),
        null, AccessOption.PERSISTENT);
    dataAccessor.create(HelixReadOnlySchemaRepository.getValueSchemaParentPath(clusterName, storeName),
        null, AccessOption.PERSISTENT);
    logger.info("Create key-schema/value-schema folder for store: " + storeName);
  }

  /**
   * Do nothing for store deletion, since it will recursively remove all the related schema.
   *
   * @param storeName
   */
  @Override
  public void handleStoreDeleted(String storeName) {
  }

  @Override
  public void handleStoreChanged(Store store) {
  }

  @Override
  public void refresh() {

  }

  @Override
  public void clear() {

  }
}