import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultItemBaseComponent } from '@quantexa/explorer-web-core/ui-search';


@Component({
    selector: 'qe-search-item-customer',
    templateUrl: '../datasource-customer/item-customer.component.html',
    styleUrls: ['../datasource-customer/item-customer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ItemCustomerComponent extends ResultItemBaseComponent {
    dataSource = 'customer';

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }
}
