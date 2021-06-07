import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { EffectsModule } from '@ngrx/effects';
import { StoreRouterConnectingModule } from '@ngrx/router-store';
import { StoreModule } from '@ngrx/store';
import { MissingTranslationHandler, TranslateModule } from '@ngx-translate/core';
import { AuthenticationModule } from '@quantexa/explorer-web-core/authentication';
import { PluginLoaderModule } from '@quantexa/explorer-web-core/plugin-loader';
import { ProjectLibraryLoaderModule } from '@quantexa/explorer-web-core/project-library-loader';
import { SharedModule } from '@quantexa/explorer-web-core/shared-module';
import { DefaultTranslationHandler, translationInitialiser, translationLoader } from '@quantexa/explorer-web-core/translations';
import { NavigationModule } from '@quantexa/explorer-web-core/ui-navigation';
import { NotificationsModule } from '@quantexa/explorer-web-core/ui-notifications';
import { PermissionsModule } from '@quantexa/explorer-web-core/ui-permissions';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { MainViewComponent } from './components/main-view/main-view.component';
import { modules as pluginModules } from './plugins';
import { libs as projectLibOverrides } from './project-libs';
import { ScoringService } from './services/scoring.service';

const ngModules = [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule
];

const qeModules: any[] = [
    NotificationsModule,
    NavigationModule,
    PermissionsModule.forRoot(),
    AuthenticationModule.forRoot(),
    PluginLoaderModule.forRoot(pluginModules),
    ProjectLibraryLoaderModule.forRoot(projectLibOverrides)
];

const primeNgModules = [
    InputTextModule,
    ButtonModule
];

const providers: any[] = [
    translationInitialiser,
    ScoringService
];

@NgModule({
    declarations: [
        AppComponent,
        MainViewComponent
    ],
    imports: [
        SharedModule,
        ...ngModules,
        ...qeModules,
        ...pluginModules,
        ...primeNgModules,
        StoreModule.forRoot({}, {
            metaReducers: []
        }),
        StoreRouterConnectingModule.forRoot({
            stateKey: 'router'
        }),
        EffectsModule.forRoot([]),
        TranslateModule.forRoot({
            loader: translationLoader,
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: DefaultTranslationHandler
            }
        }),
        environment.imports
    ],
    providers: providers,
    bootstrap: [AppComponent]
})
export class AppModule {
}
