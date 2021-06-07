import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-document-customer',
    templateUrl: '../node-document-customer/node-document-customer.component.html',
    styleUrls: ['../node-document-customer/node-document-customer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeDocumentCustomerComponent extends SelectedNodeBaseComponent {

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
