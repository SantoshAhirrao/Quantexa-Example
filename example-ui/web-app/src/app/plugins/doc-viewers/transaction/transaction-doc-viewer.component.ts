import { ChangeDetectionStrategy, Component, Input } from '@angular/core';


@Component({
    selector: 'qe-transaction-doc-viewer',
    templateUrl: './transaction-doc-viewer.component.html',
    styleUrls: ['./transaction-doc-viewer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TransactionDocViewerComponent {
    @Input() document: any;
}
