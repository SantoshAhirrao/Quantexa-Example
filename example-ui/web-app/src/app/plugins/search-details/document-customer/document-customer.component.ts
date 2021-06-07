import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultDetailsBaseComponent } from '@quantexa/explorer-web-core/ui-search';


@Component({
    selector: 'qe-search-details-document-customer',
    templateUrl: '../document-customer/document-customer.component.html',
    styleUrls: ['../document-customer/document-customer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DocumentCustomerComponent extends ResultDetailsBaseComponent {
    dataSource = 'customer';

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }
}
