package com.linkedin.davinci.replication.merge;

import static com.linkedin.venice.schema.rmd.RmdConstants.TIMESTAMP_FIELD_NAME;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.linkedin.davinci.replication.RmdWithValueSchemaId;
import com.linkedin.venice.meta.ReadOnlySchemaRepository;
import com.linkedin.venice.schema.SchemaEntry;
import com.linkedin.venice.utils.lazy.Lazy;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestMergeDeleteWithFieldLevelTimestamp extends TestMergeConflictResolver {
  @Test
  public void testDeleteIgnored() {
    MergeConflictResolver mergeConflictResolver = MergeConflictResolverFactory.getInstance()
        .createMergeConflictResolver(
            mock(ReadOnlySchemaRepository.class),
            new RmdSerDe(schemaRepository, storeName, RMD_VERSION_ID),
            storeName);

    Map<String, Long> fieldNameToTimestampMap = new HashMap<>();
    fieldNameToTimestampMap.put("id", 10L);
    fieldNameToTimestampMap.put("name", 20L);
    fieldNameToTimestampMap.put("age", 30L);
    GenericRecord rmdRecord = createRmdWithFieldLevelTimestamp(rmdSchemaV1, fieldNameToTimestampMap);
    final int oldValueSchemaID = 1;
    final long deleteOperationTimestamp = 9L; // Strictly smaller than all fields' RMD timestamps.

    MergeConflictResult mergeResult = mergeConflictResolver.delete(
        Lazy.of(() -> null),
        Optional.of(new RmdWithValueSchemaId(oldValueSchemaID, RMD_VERSION_ID, rmdRecord)),
        deleteOperationTimestamp,
        1L, // Same as the old value schema ID.
        0,
        0);
    Assert.assertTrue(mergeResult.isUpdateIgnored());
  }

  @Test
  public void testDeleteWithFieldLevelTimestamp() {
    final int valueSchemaID = 1;
    Map<String, Long> fieldNameToTimestampMap = new HashMap<>();
    fieldNameToTimestampMap.put("id", 10L);
    fieldNameToTimestampMap.put("name", 20L);
    fieldNameToTimestampMap.put("age", 30L);
    GenericRecord oldRmdRecord = createRmdWithFieldLevelTimestamp(rmdSchemaV1, fieldNameToTimestampMap);
    RmdWithValueSchemaId oldRmdWithValueSchemaID =
        new RmdWithValueSchemaId(valueSchemaID, RMD_VERSION_ID, oldRmdRecord);
    GenericRecord oldValueRecord = new GenericData.Record(valueRecordSchemaV1);
    oldValueRecord.put("id", "123");
    oldValueRecord.put("name", "James");
    oldValueRecord.put("age", 32);
    final ByteBuffer oldValueBytes = ByteBuffer.wrap(getSerializer(valueRecordSchemaV1).serialize(oldValueRecord));
    ReadOnlySchemaRepository schemaRepository = mock(ReadOnlySchemaRepository.class);
    doReturn(new SchemaEntry(1, valueRecordSchemaV1)).when(schemaRepository).getValueSchema(storeName, valueSchemaID);

    MergeConflictResolver mergeConflictResolver = MergeConflictResolverFactory.getInstance()
        .createMergeConflictResolver(
            schemaRepository,
            new RmdSerDe(schemaRepository, storeName, RMD_VERSION_ID),
            storeName);

    // Case 1: Delete one field with the same delete timestamp.
    long deleteOperationTimestamp = 10L;
    MergeConflictResult result = mergeConflictResolver
        .delete(Lazy.of(() -> oldValueBytes), Optional.of(oldRmdWithValueSchemaID), deleteOperationTimestamp, 1L, 0, 0);
    Assert.assertFalse(result.isUpdateIgnored());
    GenericRecord updatedRmd = result.getRmdRecord();
    GenericRecord updatedPerFieldTimestampRecord = (GenericRecord) updatedRmd.get(TIMESTAMP_FIELD_NAME);
    Assert.assertEquals(updatedPerFieldTimestampRecord.get("id"), 10L); // Not updated
    Assert.assertEquals(updatedPerFieldTimestampRecord.get("name"), 20L); // Not updated
    Assert.assertEquals(updatedPerFieldTimestampRecord.get("age"), 30L); // Not updated

    Optional<ByteBuffer> updatedValueOptional = result.getNewValue();
    Assert.assertTrue(updatedValueOptional.isPresent());
    GenericRecord updatedValueRecord =
        getDeserializer(valueRecordSchemaV1, valueRecordSchemaV1).deserialize(updatedValueOptional.get());
    Assert.assertEquals(updatedValueRecord.get("id").toString(), "default_id"); // Deleted and has the default value
    Assert.assertEquals(updatedValueRecord.get("name").toString(), "James"); // Not updated
    Assert.assertEquals(updatedValueRecord.get("age"), 32); // Not updated

    // Case 2: Delete two fields with the a higher delete timestamp.
    deleteOperationTimestamp = 25L;
    result = mergeConflictResolver
        .delete(Lazy.of(() -> oldValueBytes), Optional.of(oldRmdWithValueSchemaID), deleteOperationTimestamp, 1L, 0, 0);
    Assert.assertFalse(result.isUpdateIgnored());
    updatedRmd = result.getRmdRecord();
    updatedPerFieldTimestampRecord = (GenericRecord) updatedRmd.get(TIMESTAMP_FIELD_NAME);
    Assert.assertEquals(updatedPerFieldTimestampRecord.get("id"), deleteOperationTimestamp); // Updated
    Assert.assertEquals(updatedPerFieldTimestampRecord.get("name"), deleteOperationTimestamp); // Not updated
    Assert.assertEquals(updatedPerFieldTimestampRecord.get("age"), 30L); // Not updated

    updatedValueOptional = result.getNewValue();
    Assert.assertTrue(updatedValueOptional.isPresent());
    updatedValueRecord =
        getDeserializer(valueRecordSchemaV1, valueRecordSchemaV1).deserialize(updatedValueOptional.get());
    Assert.assertEquals(updatedValueRecord.get("id").toString(), "default_id"); // Not updated. Still the default value.
    Assert.assertEquals(updatedValueRecord.get("name").toString(), "default_name"); // Deleted and has the default value
    Assert.assertEquals(updatedValueRecord.get("age"), 32); // Not updated

    // Case 3: Delete all three fields with the a higher delete timestamp.
    deleteOperationTimestamp = 99L;
    result = mergeConflictResolver
        .delete(Lazy.of(() -> oldValueBytes), Optional.of(oldRmdWithValueSchemaID), deleteOperationTimestamp, 1L, 0, 0);
    Assert.assertFalse(result.isUpdateIgnored());
    updatedRmd = result.getRmdRecord();
    Object timestampObj = updatedRmd.get(TIMESTAMP_FIELD_NAME);
    // Because all fields are deleted. Timestamp should be converted to be a value-level timestamp.
    Assert.assertTrue(timestampObj instanceof Long);
    Assert.assertEquals((long) timestampObj, 99L);

    // Because all fields being deleted is equivalent to the whole value record being deleted.
    updatedValueOptional = result.getNewValue();
    Assert.assertFalse(updatedValueOptional.isPresent());
  }
}
