import { ChangeDetectionStrategy, Component, Input } from '@angular/core';


@Component({
    selector: 'qe-customer-doc-viewer',
    templateUrl: './customer-doc-viewer.component.html',
    styleUrls: ['./customer-doc-viewer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerDocViewerComponent {
    @Input() document: any;
}
