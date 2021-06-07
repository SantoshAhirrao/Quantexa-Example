import { NgModule, Pipe, PipeTransform } from '@angular/core';
import { PluginConfig, PluginModule, PluginPlacement } from '@quantexa/explorer-web-core/plugin-loader';
import { SharedModule } from '@quantexa/explorer-web-core/shared-module';
import { AutoheightScrollpanelModule, LozengeModule } from '@quantexa/explorer-web-core/ui-base-components';
import * as moment from 'moment';
import { ItemCustomerComponent } from './datasource-customer/item-customer.component';
import { ItemHotlistComponent } from './datasource-hotlist/item-hotlist.component';
import { ItemTransactionComponent } from './datasource-transaction/item-transaction.component';
import { ItemEntityIndividualComponent } from './datasource-entity-individual/item-entity-individual.component';
import { ItemEntityBusinessComponent } from './datasource-entity-business/item-entity-business.component';
import { ItemEntityAccountComponent } from './datasource-entity-account/item-entity-account.component';
import { ItemEntityTelephoneComponent } from './datasource-entity-telephone/item-entity-telephone.component';
import { ItemEntityAddressComponent } from './datasource-entity-address/item-entity-address.component';




@Pipe({ name: 'findLastField' })
export class HighlightFinalFieldPipe implements PipeTransform {
    transform(field: string) {
        const items: string[] = field.split('.');
        return items[items.length - 1].replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ');
    }
}

@Pipe({ name: 'dotsToArrows' })
export class DotsToArrows implements PipeTransform {
    transform(field: string) {
        return field.replace(/\./g, ' -> ');
    }
}

@Pipe({ name: 'removeDuplicates' })
export class RemoveDuplicates implements PipeTransform {
    transform(arr: any) {
        if (typeof arr === 'object' && typeof arr.forEach === 'function') return Array.from(new Set(arr));
        else return arr;
    }
}

@Pipe({ name: 'parseDate' })
export class ParseDate implements PipeTransform {
    transform(dateString: string, format: string) {
        const parsedDate = moment(dateString, format);
        if (parsedDate.isValid()) return parsedDate.format('ll');
        else return dateString;
    }
}

@Pipe({ name: 'getTotalScore' })
export class GetTotalScore implements PipeTransform {
    transform(items: any[]): any {
        if (!items) {
            return items;
        }
        // filter items array, items which match and return true will be
        // kept, false will be filtered out
        return items.filter(item => item.indexOf('Total Score') !== -1);
    }
}

const pipes = [
    HighlightFinalFieldPipe,
    DotsToArrows,
    RemoveDuplicates,
    ParseDate,
    GetTotalScore
];

const exportedComponents = [
        ItemCustomerComponent,
        ItemHotlistComponent,
        ItemTransactionComponent,
        ItemEntityIndividualComponent,
        ItemEntityBusinessComponent,
        ItemEntityAccountComponent,
        ItemEntityTelephoneComponent,
        ItemEntityAddressComponent
];

@NgModule({
    imports: [
        SharedModule,
        AutoheightScrollpanelModule,
        LozengeModule
    ],
    declarations: [
        ...pipes,
        ...exportedComponents
    ],
    entryComponents: [
        ...exportedComponents
    ]
})
@PluginConfig({
    name: 'Search Results',
    description: 'Custom components for rendering search results depending on Document source specifications',
    placements: [
        new PluginPlacement({
            slot: 'search-results-item-document-transaction',
            component: ItemTransactionComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-document-hotlist',
            component: ItemHotlistComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-document-customer',
            component: ItemCustomerComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-entity-individual',
            component: ItemEntityIndividualComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-entity-business',
            component: ItemEntityBusinessComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-entity-account',
            component: ItemEntityAccountComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-entity-telephone',
            component: ItemEntityTelephoneComponent
        }),
        new PluginPlacement({
            slot: 'search-results-item-entity-address',
            component: ItemEntityAddressComponent
        })
    ]
})
export class SearchResultsModule extends PluginModule {
}
