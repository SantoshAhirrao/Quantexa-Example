import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultItemBaseComponent } from '@quantexa/explorer-web-core/ui-search';


@Component({
    selector: 'qe-search-item-hotlist',
    templateUrl: '../datasource-hotlist/item-hotlist.component.html',
    styleUrls: ['../datasource-hotlist/item-hotlist.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ItemHotlistComponent extends ResultItemBaseComponent {
    dataSource = 'hotlist';

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }
}
