import { TaskViewLib, ProjectLib } from '@quantexa/explorer-web-core/ui-investigation-task-view';

export const lib: TaskViewLib = {
    ...ProjectLib,
    'bulk-search': {
        tabs: [
            {
                label: 'Bulk Search Summary',
                pluginSlot: 'task-view-summary',
                order: 0
            }
        ]
    }
};
