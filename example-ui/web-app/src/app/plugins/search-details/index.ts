import { NgModule } from '@angular/core';
import { PluginConfig, PluginModule, PluginPlacement } from '@quantexa/explorer-web-core/plugin-loader';
import { SharedModule } from '@quantexa/explorer-web-core/shared-module';
import { LozengeModule } from '@quantexa/explorer-web-core/ui-base-components';
import { DocViewersModule } from '../doc-viewers/doc-viewers.module';
import { DocumentCustomerComponent } from './document-customer/document-customer.component';
import { DocumentHotlistComponent } from './document-hotlist/document-hotlist.component';
import { DocumentTransactionComponent } from './document-transaction/document-transaction.component';
import { EntityBusinessComponent } from './entity-business/entity-business.component';
import { EntityIndividualComponent } from './entity-individual/entity-individual.component';
import { EntityAccountComponent } from './entity-account/entity-account.component';
import { EntityAddressComponent } from './entity-address/entity-address.component';
import { EntityTelephoneComponent } from './entity-telephone/entity-telephone.component';
import { AccordionModule } from 'primeng/accordion';


const exportedComponents = [
    DocumentCustomerComponent,
    DocumentHotlistComponent,
    DocumentTransactionComponent,
    EntityIndividualComponent,
    EntityBusinessComponent,
    EntityAccountComponent,
    EntityAddressComponent,
    EntityTelephoneComponent
];

@NgModule({
    imports: [
        SharedModule,
        AccordionModule,
        DocViewersModule,
        LozengeModule
    ],
    declarations: [
        ...exportedComponents
    ],
    entryComponents: [
        ...exportedComponents
    ]
})
@PluginConfig({
    name: 'Search Result Details',
    description: 'Custom components for rendering search details depending on Document/Entity source specifications',
    placements: [
        new PluginPlacement({
            slot: 'search-result-details-document-customer',
            component: DocumentCustomerComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-document-hotlist',
            component: DocumentHotlistComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-document-transaction',
            component: DocumentTransactionComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-entity-individual',
            component: EntityIndividualComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-entity-business',
            component: EntityBusinessComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-entity-account',
            component: EntityAccountComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-entity-address',
            component: EntityAddressComponent
        }),
        new PluginPlacement({
            slot: 'search-result-details-entity-telephone',
            component: EntityTelephoneComponent
        })
    ]
})
export class SearchDetailsModule extends PluginModule {
}
