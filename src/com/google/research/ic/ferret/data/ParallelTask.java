package com.google.research.ic.ferret.data;

/**
 * @author liyang@google.com (Yang Li)
 */
public interface ParallelTask {
  public void init(int taskId);
  public Boolean compute(int index);
}
