package com.linkedin.venice.compute;

import com.linkedin.avro.fastserde.PrimitiveFloatList;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.schema.avro.ComputablePrimitiveFloatList;
import java.util.List;


/**
 * This class provides utilities for float-vector operations, and it also handles {@link ComputablePrimitiveFloatList}
 * and {@link PrimitiveFloatList} transparently to the user of this class.
 */
public class ComputeOperationUtils {
  public static final String CACHED_SQUARED_L2_NORM_KEY = "CACHED_SQUARED_L2_NORM_KEY";

  public static double dotProduct(List<Float> list1, List<Float> list2) {
    if (list1.size() != list2.size()) {
      throw new VeniceException("Two lists are with different dimensions: " + list1.size() + ", and " + list2.size());
    }
    if (list1 instanceof ComputablePrimitiveFloatList && list2 instanceof ComputablePrimitiveFloatList) {
      ComputablePrimitiveFloatList computablePrimitiveFloatList1 = (ComputablePrimitiveFloatList)list1;
      ComputablePrimitiveFloatList computablePrimitiveFloatList2 = (ComputablePrimitiveFloatList)list2;
      return dotProduct(list1.size(), (i) -> computablePrimitiveFloatList1.getPrimitive(i), (i) -> computablePrimitiveFloatList2.getPrimitive(i));
    } else if (list1 instanceof PrimitiveFloatList && list2 instanceof PrimitiveFloatList) {
      PrimitiveFloatList primitiveFloatList1 = (PrimitiveFloatList)list1;
      PrimitiveFloatList primitiveFloatList2 = (PrimitiveFloatList)list2;
      return dotProduct(list1.size(), (i) -> primitiveFloatList1.getPrimitive(i), (i) -> primitiveFloatList2.getPrimitive(i));
    } else {
      return dotProduct(list1.size(), (i) -> list1.get(i), (i) -> list2.get(i));
    }
  }

  private interface FloatSupplierByIndex {
    float get(int index);
  }

  private static double dotProduct(int size, FloatSupplierByIndex floatSupplier1, FloatSupplierByIndex floatSupplier2) {
    double dotProductResult = 0.0;
    for (int i = 0; i < size; i++) {
      dotProductResult += floatSupplier1.get(i) * floatSupplier2.get(i);
    }
    return dotProductResult;
  }

  public static double squaredL2Norm(List<Float> list) {
    if (list instanceof ComputablePrimitiveFloatList) {
      return ((ComputablePrimitiveFloatList) list).squaredL2Norm();
    } else if (list instanceof PrimitiveFloatList) {
      PrimitiveFloatList primitiveFloatList = (PrimitiveFloatList)list;
      int size = primitiveFloatList.size();
      FloatSupplierByIndex floatSupplierByIndex = (i) -> primitiveFloatList.getPrimitive(i);
      return dotProduct(size, floatSupplierByIndex, floatSupplierByIndex);
    } else {
      int size = list.size();
      FloatSupplierByIndex floatSupplierByIndex = (i) -> list.get(i);
      return dotProduct(size, floatSupplierByIndex, floatSupplierByIndex);
    }
  }
}