import { NgModule, Pipe, PipeTransform } from '@angular/core';
import { SharedModule } from '@quantexa/explorer-web-core/shared-module';
import { AutoheightScrollpanelModule } from '@quantexa/explorer-web-core/ui-base-components';
import * as moment from 'moment';
import { AccordionModule } from 'primeng/accordion';
import { TooltipModule } from 'primeng/tooltip';
import { CustomerDocViewerComponent } from './customer/customer-doc-viewer.component';
import { HotlistDocViewerComponent } from './hotlist/hotlist-doc-viewer.component';
import { TransactionDocViewerComponent } from './transaction/transaction-doc-viewer.component';


@Pipe({ name: 'parseDate' })
export class ParseDate implements PipeTransform {
    transform(dateString: string, format: string) {
        const parsedDate = moment(dateString, format, true);
        if (parsedDate.isValid()) return parsedDate.format('ll');
        else return dateString;
    }
}

const docViewers = [
    CustomerDocViewerComponent,
    HotlistDocViewerComponent,
    TransactionDocViewerComponent
];

const pipes = [
    ParseDate
];


@NgModule({
    imports: [
        SharedModule,
        AutoheightScrollpanelModule,
        TooltipModule,
        AccordionModule
    ],
    declarations: [
        ...docViewers,
        ...pipes
    ],
    exports: [
        ...docViewers,
        ...pipes,
        AutoheightScrollpanelModule
    ]
})
export class DocViewersModule {}
