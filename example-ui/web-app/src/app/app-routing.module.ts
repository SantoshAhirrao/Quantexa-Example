import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MainViewComponent } from './components/main-view/main-view.component';
import { AdminRoleGuard } from '@quantexa/explorer-web-core/ui-permissions';

const routes: Routes = [
    {
        path: 'admin',
        loadChildren: './lazy-load/admin.module#AdminModule',
        canActivate: [AdminRoleGuard],
        data: {
            isTabView: false
        }
    },
    {
        path: 'share',
        loadChildren: './lazy-load/link-parser.module#LinkParserModule',
        data: {
            isTabView: false
        }
    },
    {
        path: '',
        component: MainViewComponent,
        pathMatch: 'full',
        data: {
            isTabView: true
        }
    },
    {
        path: '**',
        redirectTo: '',
        pathMatch: 'full'
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule],
    providers: [AdminRoleGuard]
})
export class AppRoutingModule {
}
