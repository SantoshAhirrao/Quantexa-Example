import { ChangeDetectionStrategy, Component, Input } from '@angular/core';


@Component({
    selector: 'qe-hotlist-doc-viewer',
    templateUrl: './hotlist-doc-viewer.component.html',
    styleUrls: ['./hotlist-doc-viewer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class HotlistDocViewerComponent {
    @Input() document: any;
}
