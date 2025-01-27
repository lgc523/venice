package com.linkedin.venice.schema.rmd.v1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestCollectionRmdTimestamp {
  @Test
  public void testFindIndexOfNextLargerNumber() {
    List<Long> list = Arrays.asList(1L, 2L, 3L, 3L, 4L, 7L, 7L, 100L);

    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 1), 1);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 2), 2);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 3), 4);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 4), 5);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 5), 5);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 6), 5);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 7), 7);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 8), 7);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 9), 7);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 60), 7);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 99), 7);

    // Edge cases
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 0), 0);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, Long.MIN_VALUE), 0);
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, 101), list.size());
    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(list, Long.MAX_VALUE), list.size());

    Assert.assertEquals(CollectionRmdTimestamp.findIndexOfNextLargerNumber(Collections.emptyList(), 5), 0);
  }
}
