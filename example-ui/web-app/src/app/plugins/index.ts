import {
    CompoundGraphViewModule,
    HistogramViewModule,
    InvestigationFindingsPluginModule,
    InvestigationListPluginModule,
    InvestigationMapViewPluginModule,
    InvestigationNetworkViewPluginModule,
    InvestigationPluginModule,
    InvestigationTaskViewPluginModule,
    InvestigationPrintViewPluginModule,
    ScoringDataPluginModule,
    SearchPluginModule,
    SelectionDetailsPluginModule,
    SimplifiedViewModule,
    SourceViewerPluginModule,
    TasksPluginModule,
    TaskViewSummaryPluginModule} from '@quantexa/explorer-web-core/ui-base-plugins';

import { SearchDetailsModule } from './search-details';
import { SearchResultsModule } from './search-results';
import { SelectedNodeDetailsModule } from './selected-node-details';
import { InvestigationPrintContentPluginModule } from './investigation-print-content';

const basePlugins = [
    CompoundGraphViewModule,
    HistogramViewModule,
    SimplifiedViewModule,
    InvestigationPluginModule,
    InvestigationListPluginModule,
    InvestigationMapViewPluginModule,
    InvestigationNetworkViewPluginModule,
    InvestigationTaskViewPluginModule,
    InvestigationFindingsPluginModule,
    InvestigationPrintViewPluginModule,
    InvestigationPrintContentPluginModule,
    SearchPluginModule,
    TasksPluginModule,
    SelectionDetailsPluginModule,
    ScoringDataPluginModule,
    TaskViewSummaryPluginModule,
    SourceViewerPluginModule
];

export const modules = [
    ...basePlugins,
    SearchResultsModule,
    SearchDetailsModule,
    SelectedNodeDetailsModule
];
