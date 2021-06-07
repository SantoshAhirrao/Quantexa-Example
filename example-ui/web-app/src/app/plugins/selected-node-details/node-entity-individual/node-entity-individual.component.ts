import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-entity-individual',
    templateUrl: './node-entity-individual.component.html',
    styleUrls: ['./node-entity-individual.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeEntityIndividualComponent extends SelectedNodeBaseComponent {

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
