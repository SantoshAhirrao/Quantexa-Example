import { lib as investigationModule } from './modules/investigation';
import { lib as investigationNetworkViewModule } from './modules/investigation-network-view';
import { lib as searchModule } from './modules/search';
import { lib as tasksModule } from './modules/tasks';
import { lib as helpMenuModule } from './modules/help-menu';
import { lib as investigationTaskViewLib } from './modules/investigation-task-view';
import { lib as navigationProjectLib } from './modules/navigation';

export const libs = {
    search: searchModule,
    investigation: investigationModule,
    'investigation-network-view': investigationNetworkViewModule,
    tasks: tasksModule,
    'help-menu': helpMenuModule,
    'investigation-task-view': investigationTaskViewLib,
    navigation: navigationProjectLib
};
