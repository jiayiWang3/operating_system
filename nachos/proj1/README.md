## Implementation Description and Test
### Alarm
#### 1. Code Modified
* Set a priority queue(**"blockedThreadQueue"**) to store threads that are waiting until x wall-clock time. Priority between threads are decided by the set wake up time(present time + waitUntil time x).
* Alarm.waitUntil(): store the thread and wake up time in **blockedThreadQueue**, let the thread sleep.
* Alarm.timerInterrupt(): search for all threads in **blockedThreadQueue** whose wake up time is due, let the threads ready.
#### 2. Testing Cases
* waitUntil -100, -10, 0, 1000, 10000, 100000 clock time, check whether waiting periods are approximately same as wakeUntil time x.
### Join
#### 1. Code Modified
* set a hashmap(**"joinedThreads"**) for recording all joined threads' relationship. Key is the child thread, Value is the parent thread.
* KThread.join(): First, do 2 asserts to ensure(1)thread is not joining itself.(2)thread that is going to join has not been joined before. There won't exist more than 2 threads joining and waiting for the same thread. Then, store this new relationship in **joinedThreads**, let current thread sleep.
* KThread.finish(): When a thread finishes, check whether it is in **joinedThreads**. If yes, it means that there exists a parent thread waiting for this child thread to finish. Find the parent thread according to **joinedThreads**, let the parent thread ready, delete this used relationship in **joinedThreads**.
#### 2. Testing Cases
* Test Case 1: child thread is finished before join.
* Test Case 2: child thread is not finished before join.
* Test Case 3: thread call join on itself.
* Test Case 4: join is called more than once on a thread.
* Test Case 5: one thread can join with multiple other threads in succession.
* Test Case 6: same as Homework2.
### Condition
#### 1. Code Modified
* set a queue(**"waitQueue"**) of waiting threads. set a lock(**"conditionLock"**) to protect this condition variable's data.
* Conidition2.sleep(): First release **conditionLock**. Add the thread to **waitQueue**. Let thread sleep. Last reaquire **conditionLock**.
* Condition2.wake(): First acquire **conditionLock**. Pop the first thread in **waitQueue**. If the thread is added in Alarm's waiting queue but hasn't been waked by Alarm, call ThreadedKernel.alarm.cancel() to effectively placing it in the scheduler ready set immediately. Cancel the thread in Alarm's waiting queue at the same time. Otherwise, if the thread hasn't been added in Alarm's waiting queue, just placing it in the scheduler ready set.
* Condition2.wakeAll(): While loop + wake().
* Condition2.sleepFor(): Add the thread to **waitQueue**. Release **conditionLock**. Call ThreadedKernel.alarm.waitUntil(). Last reaquire **conditionLock**. Remove the current thread from **waitQueue** since it has already returned from waitUntil.
* Alarm.cancel(): Cancel any timer set by <i>thread</i>, effectively waking up the thread immediately (placing it in the schedulerready set) and returning true.  If <i>thread</i> has no timer set, return false.
#### 2. Testing Cases
* InterlockTest: ping pong test.
* cvTest1: producer and comsumer test.
* Test Case 1: simple sleepFor test.
* Test Case 2: thread2 cancel thread1's sleep by invoking cancel.
* Test Case 3: thread2 cancel thread1's sleep by invoking wake.
* Test Case 4: thread2 cancel thread1's sleep by invoking wake, and cancel again.
* Test Case 5: thread2 cancel thread1's sleep by invoking cancel, and wake again.
### Rendezvous
#### 1. Code Modified
* set a hashmap(**"first"**) to store the first exchanged value with corresponding tag. set another hashmap(**"second"**) to store the second exchanged value with corresponding tag. set a lock (**"lock"**) and condition variable(**"cv"**) to manage concurrency.
* Rendezvous.exchange(): There are two if statesment for threadA (first occurence with a tag) and threadB (second occurence with same tag). Since the tag is first appeard, ThreadA put the tag and exchanged value in **first** and call cv.sleep(). Suppose threadB with the same tag, threadB put the tag and exchanged value in **second** and call cv.wake(). ThreadB then get the return value through tag-value pair stored in **first** and then remove this tag-value pair from **first**. As threadA is waken up by cv.wake() called in thread B before, threadA then get the tag-value pair stored in **second** and remove this tag-value pair from **second.
#### 2. Testing Cases
* Test Case 1: simple exchange test
* Test Case 2: four threads exchange values on the same tag
* Test Case 3: four threads exchange values on two different tags

