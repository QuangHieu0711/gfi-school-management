import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { marker } from '@colsen1991/ngx-translate-extract-marker';
import { FALLBACK_LANGUAGE, LANGUAGE_KEY, SUPPORT_LANGUAGE } from '@constant/constant';
import { StorageService } from './storage.service';

/**
 * LanguageService is responsible for managing the application's language settings.
 * It allows setting the default language, switching languages, and loading translation files.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly loadedMap = new Map<string, Set<string>>();
  get currentLang(): string {
    return this.translateService.currentLang || FALLBACK_LANGUAGE;
  }
  constructor(
    private readonly http: HttpClient,
    private readonly translateService: TranslateService,
    private readonly storageService: StorageService
  ) {
    this.setDefaultLanguage();
  }

  /**
   * Sets the default language for the application.
   * It checks localStorage for a saved language preference,
   * falls back to the browser's language, or uses a predefined fallback language.
   */
  public setDefaultLanguage(): void {
    this.setLang('vi');
  }

  /**
   * Sets the application's language to the specified language.
   * It resolves the language against supported languages and updates the translation service.
   * The selected language is also saved in localStorage.
   * @param language The language code to set (e.g., 'en', 'fr').
   */
  public setLang(language: string): void {
    const resolved = this.resolveLanguage(language);
    this.translateService.use(resolved);
    this.storageService.set<string>(LANGUAGE_KEY, resolved);
  }

  /**
   * Resolves the provided language code against supported languages.
   * If the language is not supported, it returns the fallback language.
   * @param lang The language code to resolve.
   * @returns The resolved language code.
   */
  private resolveLanguage(lang: string | null | undefined): string {
    return SUPPORT_LANGUAGE.includes(lang || '') ? lang! : FALLBACK_LANGUAGE;
  }

  /**
   * Switches the application's language to the specified language.
   * This method is a convenience wrapper around setLang.
   * @param language The language code to switch to.
   */
  public switchLang(language: string): void {
    this.setLang(language);
  }

  /**
   * Returns the current language of the application.
   * @returns The current language code.
   */
  public instant(key: string | string[], interpolateParams?: object): string {
    if (typeof key === 'string') marker(key);
    else key.forEach((k) => marker(k));
    return this.translateService.instant(key, interpolateParams);
  }

  /**
   * Retrieves the translation for the specified key or keys.
   * If the key is a string, it marks it for extraction.
   * If it's an array, it marks each key in the array.
   * @param key The translation key or keys to retrieve.
   * @param interpolateParams Optional parameters for interpolation in the translation.
   * @returns An observable that emits the translation(s).
   */
  public get(key: string | string[], interpolateParams?: object) {
    if (typeof key === 'string') marker(key);
    else key.forEach((k) => marker(k));

    return this.translateService.get(key, interpolateParams);
  }

  /**
   * Load and merge a namespace file (if not already loaded).
   * This method fetches the translation file for the specified namespace and language,
   * and merges it into the translation service.
   */
  async loadLanguage(namespace: string): Promise<void> {
    if (this.isNamespaceLoaded(this.currentLang, namespace)) return;
    try {
      const url = `assets/i18n/${namespace}/${this.currentLang}.json`;
      const data: Record<string, string> = await firstValueFrom(this.http.get<Record<string, string>>(url));
      this.translateService.setTranslation(this.currentLang, data, true);
      this.markNamespaceAsLoaded(this.currentLang, namespace);
    } catch (error) {
      console.warn(`[i18n] Failed to load namespace "${namespace}" for language "${this.currentLang}"`, error);
      /**
       * If loading the namespace for the current language fails,
       * attempt to load the fallback language.
       * This ensures that the application can still function with a fallback language.
       */
      if (this.currentLang !== FALLBACK_LANGUAGE) {
        try {
          const fallbackUrl = `assets/i18n/${namespace}/${FALLBACK_LANGUAGE}.json`;
          const fallbackData = await firstValueFrom(this.http.get<Record<string, string>>(fallbackUrl));
          this.translateService.setTranslation(FALLBACK_LANGUAGE, fallbackData, true);
          this.markNamespaceAsLoaded(FALLBACK_LANGUAGE, namespace);
          this.storageService.set<string>(LANGUAGE_KEY, FALLBACK_LANGUAGE);
        } catch (fallbackError) {
          console.error(`[i18n] Fallback failed for namespace "${namespace}"`, fallbackError);
        }
      }
    }
  }

  /**
   * Checks if a namespace for a specific language has already been loaded.
   * @param lang The language code to check.
   * @param namespace The namespace to check.
   * @returns True if the namespace is loaded, false otherwise.
   */
  private isNamespaceLoaded(lang: string, namespace: string): boolean {
    return this.loadedMap.get(lang)?.has(namespace) ?? false;
  }

  /**
   * Marks a namespace as loaded for a specific language.
   * This prevents reloading the same namespace multiple times.
   * @param lang The language code.
   * @param namespace The namespace to mark as loaded.
   */
  private markNamespaceAsLoaded(lang: string, namespace: string): void {
    if (!this.loadedMap.has(lang)) this.loadedMap.set(lang, new Set());
    this.loadedMap.get(lang)!.add(namespace);
  }
}
