import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-document-transaction',
    templateUrl: '../node-document-transaction/node-document-transaction.component.html',
    styleUrls: ['../node-document-transaction/node-document-transaction.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeDocumentTransactionComponent extends SelectedNodeBaseComponent {

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }

    get model() {
        if (!this.document) return null;
        return {
            ...this.document
        };
    }
}
