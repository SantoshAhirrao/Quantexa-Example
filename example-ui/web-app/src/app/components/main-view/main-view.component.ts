import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MODULE_NAME as INVESTIGATION_TYPE } from '@quantexa/explorer-web-core/ui-investigation';
import { MODULE_NAME as EXPLORATION_LIST_TYPE } from '@quantexa/explorer-web-core/ui-exploration-list';
import { TabManagerConfiguration } from '@quantexa/explorer-web-core/ui-navigation';
import { GLOBAL_PRIVILEGES } from '@quantexa/explorer-web-core/ui-permissions';


@Component({
    selector: 'qe-main-view',
    templateUrl: './main-view.component.html',
    styleUrls: ['./main-view.component.scss'],
    host: {
        'class': 'app-view'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MainViewComponent {
    tabManagerConfig: TabManagerConfiguration = {
        types: [
            {
                id: 'search',
                title: 'SEARCH.TAB_TITLE',
                entry: 'SEARCH.NEW_TAB_MENU_ENTRY',
                icon: 'qe-icon-search',
                hidden: false,
                requiredPrivileges: [GLOBAL_PRIVILEGES.SEARCH, GLOBAL_PRIVILEGES.SEARCH_CONFIG]
            },
            {
                id: 'investigation-list',
                title: 'INVESTIGATION_LIST.TAB_TITLE',
                entry: 'INVESTIGATION_LIST.NEW_TAB_MENU_ENTRY',
                icon: 'qe-icon-investigation-list',
                uniqueResourcesOnly: true,
                usesResourceId: false,
                hidden: false
            },
            {
                id: INVESTIGATION_TYPE,
                uniqueResourcesOnly: true,
                usesResourceId: true,
                title: 'INVESTIGATION.TAB_TITLE',
                entry: 'INVESTIGATION.NEW_TAB_MENU_ENTRY',
                icon: 'qe-icon-investigation',
                hidden: false,
                requiredPrivileges: [GLOBAL_PRIVILEGES.CREATE_INVESTIGATION]
            },
            {
                id: 'tasks',
                title: 'TASKS.TAB_TITLE',
                entry: 'TASKS.NEW_TAB_MENU_ENTRY',
                icon: 'qe-icon-task-list-list',
                uniqueResourcesOnly: true,
                usesResourceId: false,
                hidden: false
            }        ]
    };
}
