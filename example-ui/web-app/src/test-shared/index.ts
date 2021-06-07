import { EventEmitter, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { EMPTY, Observable, of } from 'rxjs';


@Injectable()
export class MockStoreManagerService {
    init() {
    }

    dispatch() {
    }

    select(): Observable<any> {
        return EMPTY;
    }

    selectGlobal() {
        return this.select();
    }
}

@Injectable()
export class MockApiClientService {
    registerEndpoints(configs: any[]) {
    }

    request(endpointAlias: any, payload?: any) {
    }
}

@Injectable()
export class MockPinnedItemsService {
    get pinnedItemsObservable() {
        return of(null);
    }

    get pinnedItems() {
        return [];
    }

    init() {
    }

    isPinned(item) {
        return true;
    }

    pinItems(items) {
    }

    unpinItems(items) {
    }
}

@Injectable()
export class MockSearchService {
    fetchConfiguration() {
    }

    searchDocuments(...args: any[]) {
    }

    searchEntities(...args: any[]) {
    }

    createInvestigation(items: any[]) {
    }
}

@Injectable()
export class MockSelectionService {
    selectItems(payload: any) {

    }

    sortItems(payload: any) {

    }
}

@Injectable()
export class MockTranslateService extends TranslateService {
    store: any;
    currentLoader: any;
    compiler: any;
    parser: any;
    missingTranslationHandler: any = {
        load: () => {}
    };
    readonly onTranslationChange: EventEmitter<any>;
    readonly onLangChange: EventEmitter<any>;
    readonly onDefaultLangChange: EventEmitter<any>;
    defaultLang: string;
    currentLang: string;
    langs: string[];
    translations: any;

    constructor() {
        super(null, null, null, null, null);
    }

    setDefaultLang(lang: string) {
    }

    getDefaultLang(): string {
        return '';
    }

    use(lang: string): Observable<any> {
        return EMPTY;
    }

    getTranslation(lang: string): Observable<any> {
        return EMPTY;
    }

    setTranslation(lang: string, translations: Object, shouldMerge?: boolean): void {
    }

    getLangs(): Array<string> {
        return [];
    }

    addLangs(langs: Array<string>): void {
    }

    getParsedResult(translations: any, key: any, interpolateParams?: Object): any {
        return null;
    }

    get(key: string | Array<string>, interpolateParams?: Object): Observable<string | any> {
        return EMPTY;
    }

    stream(key: string | Array<string>, interpolateParams?: Object): Observable<string | any> {
        return EMPTY;
    }

    instant(key: string | Array<string>, interpolateParams?: Object): string | any {
        return '';
    }

    set(key: string, value: string, lang?: string): void {
    }

    reloadLang(lang: string): Observable<any> {
        return EMPTY;
    }

    resetLang(lang: string): void {
    }

    getBrowserLang(): string {
        return '';
    }

    getBrowserCultureLang(): string {
        return '';
    }
}

@Injectable()
export class MockAuthenticationService {
    createToken(value: string) {
    }

    logout() {
    }

    readLocalToken() {
    }
}

@Injectable()
export class MockInvestigationSelectionPanelService {
    loadDocuments(payload: any[]) {
    }
}
