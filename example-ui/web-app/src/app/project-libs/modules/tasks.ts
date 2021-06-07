import { DataField } from '@quantexa/explorer-web-core/ui-base-components';
import {
  COLUMN_DISPLAY_MODE,
  createdDateExtractor,
  nameExtractor,
  ProjectLib,
  sortableScoreExtractor,
  stateExtractor,
  Task,
  TaskListLib,
  TasksLib,
  updatedDateExtractor
} from '@quantexa/explorer-web-core/ui-tasks';

export const lib: TasksLib = {
  ...ProjectLib,
  taskList: {
      ...ProjectLib.taskList,
      default: {
          ...ProjectLib.taskList.default,
          outcomeExtractor: extractOutcome
      }
      // 'bulk-search': {
          // Angular AOT removes values copied via ... syntax in values that are dynamic so we need to set them one by one
          // availablePageSizes: ProjectLib.taskList.default.availablePageSizes,
          // defaults: ProjectLib.taskList.default.defaults,
          // columns: [
              // {
                  // name: 'name',
                  // displayName: 'TASKS.TASK_LIST.TABLE_COLS.NAME',
                  // sortable: true,
                  // hiddenByDefault: false,
                  // alignment: 'left',
                  // fieldExtractor: nameExtractor,
                  // translate: true,
                  // displayMode: COLUMN_DISPLAY_MODE.TEXT
              // },
              // {
                  // name: 'score',
                  // displayName: 'Total Score',
                  // sortable: true,
                  // hiddenByDefault: false,
                  // alignment: 'right',
                  // fieldExtractor: sortableScoreExtractor,
                  // translate: false,
                  // displayMode: COLUMN_DISPLAY_MODE.SCORE
              // },
              // {
                  // name: 'status',
                  // displayName: 'TASKS.TASK_LIST.TABLE_COLS.STATUS',
                  // sortable: false,
                  // hiddenByDefault: false,
                  // alignment: 'left',
                  // fieldExtractor: stateExtractor,
                  // translate: true,
                  // displayMode: COLUMN_DISPLAY_MODE.STATUS
              // },
              // {
                  // name: 'totalMatchScore',
                  // displayName: 'Match Score',
                  // sortable: false,
                  // hiddenByDefault: false,
                  // alignment: 'right',
                  // fieldExtractor: matchScoreExtractor,
                  // translate: false,
                  // displayMode: COLUMN_DISPLAY_MODE.SCORE
              // },
              // {
                  // name: 'riskScore',
                  // displayName: 'Risk Score',
                  // sortable: false,
                  // hiddenByDefault: false,
                  // alignment: 'right',
                  // fieldExtractor: riskScoreExtractor,
                  // translate: false,
                  // displayMode: COLUMN_DISPLAY_MODE.SCORE
              // },
              // {
                  // name: 'createdDate',
                  // displayName: 'TASKS.TASK_LIST.TABLE_COLS.CREATED_DATE',
                  // sortable: true,
                  // hiddenByDefault: false,
                  // alignment: 'left',
                  // fieldExtractor: createdDateExtractor,
                  // translate: true,
                  // displayMode: COLUMN_DISPLAY_MODE.DATE
              // },
              // {
                  // name: 'updatedDate',
                  // displayName: 'TASKS.TASK_LIST.TABLE_COLS.UPDATED_DATE',
                  // sortable: true,
                  // hiddenByDefault: false,
                  // alignment: 'left',
                  // fieldExtractor: updatedDateExtractor,
                  // translate: true,
                  // displayMode: COLUMN_DISPLAY_MODE.DATE
              // }
          // ],
          // outcomeExtractor: ProjectLib.taskList.default.outcomeExtractor,
          // extraDataExtractor: ProjectLib.taskList.default.extraDataExtractor,
          // dateFormat: ProjectLib.taskList.default.dateFormat,
          // enableColumnVisibility: ProjectLib.taskList.default.enableColumnVisibility,
          // Angular AOT compiler removes infinity from reference so we need to re-define our scoring colours
          // scoringColors: [
              // {
                  // interval: [0, 49],
                  // color: '#00C3A3'
              // },
              // {
                  // interval: [50, 99],
                  // color: '#EFE729'
              // },
              // {
                  // interval: [100, 149],
                  // color: '#FFB170'
              // },
              // {
                  // Angular AOT compiler doesnt like infinity or max int so we are using a big number
                  // interval: [150, 999999999],
                  // color: '#F27772'
              // }
          // ]
      // }
  }
  // bulkSearch: {
  //     ...ProjectLib.bulkSearch,
  //     enabled: true
  // }
};

export function matchScoreExtractor(task: Task, l: TaskListLib): DataField {
  return {
      value: task.extraData.scores && task.extraData.scores.totalMatchScore
      ? task.extraData.scores.totalMatchScore
      : 'N/A'
  };
}

export function riskScoreExtractor(task: Task, l: TaskListLib): DataField {
  return {
      value: task.extraData.scores && task.extraData.scores.totalRiskScore
      ? task.extraData.scores.totalRiskScore
      : 'N/A'
  };
}

export function extractOutcome(outcome: any): undefined | any {
  if ('status' in outcome) return outcome.status;
  else return undefined;
}
