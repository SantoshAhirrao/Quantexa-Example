
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { PluginConfig, PluginModule, PluginPlacement } from '@quantexa/explorer-web-core/plugin-loader';
import { SharedModule } from '@quantexa/explorer-web-core/shared-module';
import { SpinnerModule } from '@quantexa/explorer-web-core/ui-base-components';
import { GraphNodeSelectorModule } from '@quantexa/explorer-web-core/ui-investigation';
import { OwlDateTimeModule, OwlNativeDateTimeModule } from 'ng-pick-datetime';
import { AccordionModule } from 'primeng/accordion';
import { DropdownModule } from 'primeng/dropdown';
import { DocViewersModule } from '../doc-viewers/doc-viewers.module';
import { NodeDocumentCustomerComponent } from './node-document-customer/node-document-customer.component';
import { NodeDocumentHotlistComponent } from './node-document-hotlist/node-document-hotlist.component';
import { NodeDocumentTransactionComponent } from './node-document-transaction/node-document-transaction.component';
import { NodeEntityAccountComponent } from './node-entity-account/node-entity-account.component';
import { NodeEntityAddressComponent } from './node-entity-address/node-entity-address.component';
import { NodeEntityBusinessComponent } from './node-entity-business/node-entity-business.component';
import { NodeEntityIndividualComponent } from './node-entity-individual/node-entity-individual.component';
import { NodeEntityTelephoneComponent } from './node-entity-telephone/node-entity-telephone.component';


const exportedComponents = [
    NodeEntityAccountComponent,
    NodeEntityTelephoneComponent,
    NodeEntityIndividualComponent,
    NodeEntityBusinessComponent,
    NodeEntityAddressComponent,
    NodeDocumentCustomerComponent,
    NodeDocumentHotlistComponent,
    NodeDocumentTransactionComponent
];

@NgModule({
    imports: [
        SharedModule,
        DropdownModule,
        AccordionModule,
        ReactiveFormsModule,
        OwlDateTimeModule,
        OwlNativeDateTimeModule,
        GraphNodeSelectorModule,
        SpinnerModule,
        DocViewersModule
    ],
    declarations: [
        ...exportedComponents
    ],
    entryComponents: exportedComponents
})
@PluginConfig({
    name: 'Selected Node Details',
    description: 'Custom components for rendering selected node details depending on Document/Entity source specifications',
    placements: [
        new PluginPlacement({
            slot: 'selected-node-details-entity-account',
            component: NodeEntityAccountComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-entity-telephone',
            component: NodeEntityTelephoneComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-entity-business',
            component: NodeEntityBusinessComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-entity-individual',
            component: NodeEntityIndividualComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-entity-address',
            component: NodeEntityAddressComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-document-customer',
            component: NodeDocumentCustomerComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-document-hotlist',
            component: NodeDocumentHotlistComponent
        }),
        new PluginPlacement({
            slot: 'selected-node-details-document-transaction',
            component: NodeDocumentTransactionComponent
        })
    ]
})
export class SelectedNodeDetailsModule extends PluginModule {
}
