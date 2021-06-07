import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultDetailsBaseComponent } from '@quantexa/explorer-web-core/ui-search';


@Component({
    selector: 'qe-search-details-document-hotlist',
    templateUrl: '../document-hotlist/document-hotlist.component.html',
    styleUrls: ['../document-hotlist/document-hotlist.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DocumentHotlistComponent extends ResultDetailsBaseComponent {
    dataSource = 'hotlist';

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }
}
