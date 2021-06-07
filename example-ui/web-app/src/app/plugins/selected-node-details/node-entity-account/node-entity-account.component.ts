import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-entity-account',
    templateUrl: './node-entity-account.component.html',
    styleUrls: ['./node-entity-account.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeEntityAccountComponent extends SelectedNodeBaseComponent {

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
