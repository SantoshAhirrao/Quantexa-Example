import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultItemBaseComponent } from '@quantexa/explorer-web-core/ui-search';


@Component({
    selector: 'qe-search-item-transaction',
    templateUrl: '../datasource-transaction/item-transaction.component.html',
    styleUrls: ['../datasource-transaction/item-transaction.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ItemTransactionComponent extends ResultItemBaseComponent {
    dataSource = 'transaction';

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }
}
