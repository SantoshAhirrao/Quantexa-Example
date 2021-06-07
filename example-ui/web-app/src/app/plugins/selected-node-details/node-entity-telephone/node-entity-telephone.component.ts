import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-entity-telephone',
    templateUrl: './node-entity-telephone.component.html',
    styleUrls: ['./node-entity-telephone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeEntityTelephoneComponent extends SelectedNodeBaseComponent {

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }

    get model() {
        if (!this.item) return null;

        return {
            ...this.item.model
        };
    }

}
