import { NgModule } from '@angular/core';
import { MissingTranslationHandler, TranslateModule } from '@ngx-translate/core';
import {
    DefaultTranslationHandler,
    translationInitialiser,
    translationLoader
} from '@quantexa/explorer-web-core/translations';
import { AppComponent } from './app.component';
import { LoginModule } from '@quantexa/explorer-web-core/ui-login';
import { AppRoutingModule } from './app-routing.module';
import { ProjectLibraryLoaderModule } from '@quantexa/explorer-web-core/project-library-loader';
import { libs as projectLibOverrides } from './project-libs';

@NgModule({
    declarations: [
        AppComponent
    ],
    imports: [
        LoginModule,
        TranslateModule.forRoot({
            loader: translationLoader,
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: DefaultTranslationHandler
            }
        }),
        ProjectLibraryLoaderModule.forRoot(projectLibOverrides),
        AppRoutingModule
    ],
    providers: [
        translationInitialiser
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
