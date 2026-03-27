import { Injectable } from '@angular/core';
import { ParsedValue, StorageType } from '@model/storage.model';

/**
 * Service for managing local and session storage.
 * Provides methods to get, set, remove, and clear items in storage.
 * Supports both local and session storage, as well as combined operations.
 * Automatically detects and parses JSON values.
 */
@Injectable({ providedIn: 'root' })
export class StorageService {
  // _type parameter is intentionally unused because storage is forced to localStorage.
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private getStorage(_type: Exclude<StorageType, 'all'>): Storage {
    // Force use of localStorage for both 'local' and 'session' to
    // ensure login information (and other important data) is persisted
    // across browser sessions. Calls that explicitly passed 'session'
    // will now be stored in localStorage as requested.
    return localStorage;
  }

  /**
   * Retrieves an item from storage by key.
   * @param key The key of the item to retrieve.
   * @param type The type of storage to use ('local', 'session', or 'all').
   * @returns The parsed value or null if not found.
   */
  get<T = unknown>(key: string, type: StorageType = 'local'): T | null {
    try {
      if (type === 'all') {
        const localValue = this.getStorage('local').getItem(key);
        if (localValue) return this.detectValueType(localValue).parsed as T;
        const sessionValue = this.getStorage('session').getItem(key);
        if (sessionValue) return this.detectValueType(sessionValue).parsed as T;
        return null;
      }
      const raw = this.getStorage(type).getItem(key);
      if (!raw || raw === 'null') return null;
      return this.detectValueType(raw).parsed as T;
    } catch (e) {
      console.error(`[StorageService] Error parsing key "${key}"`, e);
      return null;
    }
  }

  /**
   * Stores an item in storage.
   * Automatically serializes objects to JSON.
   * @param key The key under which to store the item.
   * @param value The value to store.
   * @param type The type of storage to use ('local', 'session', or 'all').
   */
  set<T>(key: string, value: T, type: StorageType = 'local'): void {
    try {
      const data =
        value !== null && typeof value === 'object'
          ? JSON.stringify(value)
          : String(value);
      if (type === 'all') {
        this.getStorage('local').setItem(key, data);
        this.getStorage('session').setItem(key, data);
      } else {
        this.getStorage(type).setItem(key, data);
      }
    } catch (e) {
      console.error(`[StorageService] Error storing key "${key}"`, e);
    }
  }

  /**
   * Removes an item from storage by key.
   * @param key The key of the item to remove.
   * @param type The type of storage to use ('local', 'session', or 'all').
   */
  remove(key: string, type: StorageType = 'local'): void {
    if (type === 'all') {
      this.getStorage('local').removeItem(key);
      this.getStorage('session').removeItem(key);
      return;
    }
    this.getStorage(type).removeItem(key);
  }

  /**
   * Clears all items from storage.
   * @param type The type of storage to clear ('local', 'session', or 'all').
   */
  clear(type: StorageType = 'local'): void {
    if (type === 'all') {
      this.getStorage('local').clear();
      this.getStorage('session').clear();
      return;
    }
    this.getStorage(type).clear();
  }

  /**
   * Clears all items from both local and session storage.
   */
  clearAll(): void {
    this.clear('all');
  }

  /**
   * Checks if an item exists in storage.
   * @param key The key of the item to check.
   * @param type The type of storage to use ('local', 'session', or 'all').
   * @returns True if the item exists, false otherwise.
   */
  has(key: string, type: StorageType = 'local'): boolean {
    if (type === 'all')
      return this.has(key, 'local') || this.has(key, 'session');

    return this.getStorage(type).getItem(key) !== null;
  }

  /**
   * Detects the type of a stored value and parses it accordingly.
   * @param value The raw value from storage.
   * @returns An object containing the type and parsed value.
   */
  detectValueType(value: string | null): ParsedValue {
    if (value === null) {
      return { type: 'null', parsed: null };
    }

    try {
      const parsed = JSON.parse(value);

      if (Array.isArray(parsed)) {
        return { type: 'array', parsed };
      }

      switch (typeof parsed) {
        case 'object':
          return { type: 'object', parsed };
        case 'string':
          return { type: 'json-string', parsed };
        case 'number':
          return { type: 'json-number', parsed };
        case 'boolean':
          return { type: 'json-boolean', parsed };
        default:
          return { type: 'invalid', parsed };
      }
    } catch {
      // Not JSON: treat as raw string
      return { type: 'raw-string', parsed: value };
    }
  }
}
