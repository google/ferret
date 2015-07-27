package com.google.research.ic.ferret.data;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author liyang@google.com (Yang Li)
 */
public class ParallelTaskExecutor {

  private int numProcessors;
  private ExecutorService eservice;
  private boolean lock = false;
  static ParallelTaskExecutor instance;
  
  public static boolean parallelExecution = true;
  
  private int threadCount;
  
  private ParallelTaskExecutor() {
    int numCores = Runtime.getRuntime().availableProcessors();
    numProcessors = numCores;
    eservice = Executors.newFixedThreadPool(numProcessors);
  }
  
  public static void shutdown() {
    if (instance != null) {
      instance.eservice.shutdown();
    }
  }
  
  public static ParallelTaskExecutor getInstance() {
    if (instance == null) {
      instance = new ParallelTaskExecutor();
    }
    return instance;
  }
  
  private static class Task implements Callable<Boolean> {
    private int taskId;
    int unitSize;
    int max;
    ParallelTask parallelTask;
    
    public Task(int taskId, int unitSize, int max,
        ParallelTask parallelTask) { 
      this.taskId = taskId;
      this.unitSize = unitSize;
      this.max = max;
      this.parallelTask = parallelTask;
    }

    @Override
    public Boolean call() {
      parallelTask.init(taskId);
      int startIndex = unitSize * taskId;
      int endIndex = Math.min(max, (taskId + 1) * unitSize);
      for (int i = startIndex; i < endIndex; i++) {
        parallelTask.compute(i);
      }
      return true;
    }
  }
  
  public void compute(int length, ParallelTask parallelTask) {
    if (!parallelExecution || lock || length == 1) {
      for (int i = 0; i < length; i++) {
        parallelTask.compute(i);
      }
    } else {
      parallelCompute(length, parallelTask);
    }
  }

  private synchronized void parallelCompute(int length, ParallelTask parallelTask) {
    lock = true;
    ExecutorCompletionService<Boolean> cservice = 
        new ExecutorCompletionService<Boolean> (eservice);
    int unitSize;
    if (length < numProcessors) {
      unitSize = 1;
    } else {
      unitSize = (int) Math.ceil(length / (float) numProcessors);
    }
    threadCount = Math.min(numProcessors, length);
    for (int index = 0; index < threadCount; index++) {
      cservice.submit(new Task(index, unitSize, length, parallelTask));
    }
    for(int index = 0; index < threadCount; index++) {
      try {
        cservice.take().get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        System.err.println("threadCount: " + threadCount + " for length: " + length);
        e.printStackTrace();
      }
    }
    lock = false;
  }

  public int getNumThreads() {
    return threadCount;
  }
}
