import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultDetailsBaseComponent } from '@quantexa/explorer-web-core/ui-search';


@Component({
    selector: 'qe-search-details-document-transaction',
    templateUrl: '../document-transaction/document-transaction.component.html',
    styleUrls: ['../document-transaction/document-transaction.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DocumentTransactionComponent extends ResultDetailsBaseComponent {
    dataSource = 'transaction';

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }
}
