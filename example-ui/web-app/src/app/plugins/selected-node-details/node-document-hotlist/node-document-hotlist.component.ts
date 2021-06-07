import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-document-hotlist',
    templateUrl: '../node-document-hotlist/node-document-hotlist.component.html',
    styleUrls: ['../node-document-hotlist/node-document-hotlist.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeDocumentHotlistComponent extends SelectedNodeBaseComponent {

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
