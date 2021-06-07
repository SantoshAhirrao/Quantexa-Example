import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-entity-business',
    templateUrl: './node-entity-business.component.html',
    styleUrls: ['./node-entity-business.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeEntityBusinessComponent extends SelectedNodeBaseComponent {

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
