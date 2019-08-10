package com.linkedin.venice.controller.kafka.protocol.serializer;

import com.linkedin.venice.controller.kafka.protocol.admin.AdminOperation;
import com.linkedin.venice.exceptions.VeniceMessageException;
import com.linkedin.venice.utils.Utils;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.LinkedinAvroMigrationHelper;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminOperationSerializer {
  // Latest schema id, and it needs to be updated whenever we add a new version
  public static int LATEST_SCHEMA_ID_FOR_ADMIN_OPERATION = 29;

  private static SpecificDatumWriter<AdminOperation> SPECIFIC_DATUM_WRITER = new SpecificDatumWriter<>(AdminOperation.SCHEMA$);
  /** Used to generate decoders. */
  private static final DecoderFactory DECODER_FACTORY = new DecoderFactory();

  private static final Map<Integer, Schema> PROTOCOL_MAP = initProtocolMap();

  public byte[] serialize(AdminOperation object) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      Encoder encoder = LinkedinAvroMigrationHelper.newBinaryEncoder(byteArrayOutputStream);
      SPECIFIC_DATUM_WRITER.write(object, encoder);
      encoder.flush();

      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      throw new VeniceMessageException("Failed to encode message: " + object.toString(), e);
    }
  }

  public AdminOperation deserialize(byte[] bytes, int writerSchemaId) {
    if (!PROTOCOL_MAP.containsKey(writerSchemaId)) {
      throw new VeniceMessageException("Writer schema: " + writerSchemaId + " doesn't exist");
    }
    SpecificDatumReader<AdminOperation> reader = new SpecificDatumReader<>(
        PROTOCOL_MAP.get(writerSchemaId), AdminOperation.SCHEMA$);
    Decoder decoder = DECODER_FACTORY.createBinaryDecoder(bytes, null);
    try {
      return reader.read(null, decoder);
    } catch(IOException e) {
      throw new VeniceMessageException("Could not deserialize bytes back into AdminOperation object" + e);
    }
  }

  public static Map<Integer, Schema> initProtocolMap() {
    try {
      Map<Integer, Schema> protocolSchemaMap = new HashMap<>();
      for (int i=1; i<= LATEST_SCHEMA_ID_FOR_ADMIN_OPERATION; i++){
        protocolSchemaMap.put(i, Utils.getSchemaFromResource("avro/AdminOperation/v"+i+"/AdminOperation.avsc"));
      }
      return protocolSchemaMap;
    } catch (IOException e) {
      throw new VeniceMessageException("Could not initialize " + AdminOperationSerializer.class.getSimpleName(), e);
    }
  }
}
