import { Injectable, NgZone, OnDestroy } from '@angular/core';

/**
 * A tiny, reusable scheduler that coalesces many calls into a single callback per animation frame.
 * Useful for throttling MutationObserver, resize/scroll handlers, or any high-frequency DOM work.
 */
@Injectable({ providedIn: 'root' })
export class RafSchedulerService implements OnDestroy {
  /**
   * The active requestAnimationFrame id, or null if no frame is currently scheduled.
   * If non-null, it means we already queued a flush for the current frame.
   */
  private frameId: number | null = null;

  /**
   * A map of scheduled tasks keyed by a string identifier.
   * - If the same key is scheduled multiple times within the same frame,
   *   the last provided function overwrites the previous one (latest-wins).
   * - Different keys will all run (once) during the next frame.
   */
  private queue = new Map<string, () => void>(); // key -> task

  constructor(private zone: NgZone) {}

  /**
   * Schedule a task to run on the next animation frame.
   *
   * @param key   A stable identifier for the work (e.g., 'table-header', 'chart-resize').
   *              Re-using the same key within a frame coalesces work to a single run.
   * @param task  The function to run. It will execute OUTSIDE Angular by default.
   *
   * Behavior:
   * - Adds/overwrites the task in the per-frame queue.
   * - If a frame is not already scheduled, posts one via requestAnimationFrame (rAF).
   * - When the frame fires, all queued tasks (for distinct keys) execute exactly once.
   */
  schedule(key: string, task: () => void): void {
    // Store/replace the task for this key
    this.queue.set(key, task);

    // If there is already a pending frame, do nothing—this task will be picked up then.
    if (this.frameId != null) return;

    // Schedule a new frame outside Angular to avoid triggering change detection on every rAF.
    // Callers can re-enter the Angular zone themselves if they need to update bound state.
    this.zone.runOutsideAngular(() => {
      this.frameId = requestAnimationFrame(() => {
        // Snapshot and clear the queue before executing, so tasks can re-schedule themselves if needed.
        const tasks = Array.from(this.queue.values());
        this.queue.clear();
        this.frameId = null;

        // Execute each queued task once. Still outside Angular.
        for (const t of tasks) t();
      });
    });
  }

  /**
   * Cancel a specific scheduled task by key (optional).
   * Note: If a frame is already scheduled, it will still fire for other keys.
   */
  cancel(key: string): void {
    this.queue.delete(key);
  }

  /**
   * Flush all queued tasks immediately (synchronously), and cancel any pending rAF.
   * Useful in ngOnDestroy to ensure the latest state is applied before teardown,
   * or in tests to deterministically run scheduled work.
   */
  flush(): void {
    // Snapshot current tasks then clear queue/state
    const tasks = Array.from(this.queue.values());
    this.queue.clear();

    // If a frame was pending, cancel it since we are flushing now.
    if (this.frameId != null) {
      cancelAnimationFrame(this.frameId);
      this.frameId = null;
    }

    // Run tasks now (still outside Angular context unless caller wrapped this).
    for (const t of tasks) t();
  }

  /**
   * Angular lifecycle hook.
   * Ensures we don’t leave a pending rAF and clears any queued work on service destroy.
   */
  ngOnDestroy(): void {
    if (this.frameId != null) cancelAnimationFrame(this.frameId);
    this.queue.clear();
  }
}
