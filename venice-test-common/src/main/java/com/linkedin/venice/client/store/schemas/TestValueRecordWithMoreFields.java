/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.linkedin.venice.client.store.schemas;

@SuppressWarnings("all")
public class TestValueRecordWithMoreFields extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"TestValueRecordWithMoreFields\",\"namespace\":\"com.linkedin.venice.client.store.schemas\",\"fields\":[{\"name\":\"long_field\",\"type\":\"long\"},{\"name\":\"string_field\",\"type\":\"string\"},{\"name\":\"int_field\",\"type\":\"int\",\"default\":10}]}");
  public long long_field;
  public java.lang.CharSequence string_field;
  public int int_field;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return long_field;
    case 1: return string_field;
    case 2: return int_field;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: long_field = (java.lang.Long)value$; break;
    case 1: string_field = (java.lang.CharSequence)value$; break;
    case 2: int_field = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}