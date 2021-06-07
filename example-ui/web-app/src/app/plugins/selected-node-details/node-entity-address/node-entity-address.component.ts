import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { SelectedNodeBaseComponent } from '@quantexa/explorer-web-core/ui-investigation-selection-panel';


@Component({
    selector: 'qe-node-entity-address',
    templateUrl: './node-entity-address.component.html',
    styleUrls: ['./node-entity-address.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeEntityAddressComponent extends SelectedNodeBaseComponent implements OnChanges {
    private MAPS_URL = 'https://www.google.com/maps/embed/v1';
    private API_KEY = 'AIzaSyDRQ249ECn1csZUfQW5YyFI0XAVVQNaQDA';

    private currentEntityId = '';
    googleMapsUrl: SafeResourceUrl;
    googleStreetViewUrl: SafeResourceUrl;

    constructor(protected cd: ChangeDetectorRef, protected sanitizer: DomSanitizer) {
        super(cd);
    }

    ngOnChanges() {
        if (!this.model) return;

        if (this.model.id !== this.currentEntityId) {
            setTimeout(() => {
                this.googleMapsUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                    `${ this.MAPS_URL }/place?key=${ this.API_KEY }&q=${ encodeURIComponent(this.model.attributes.addressDisplay) }`
                );
                this.googleStreetViewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                    `${ this.MAPS_URL }/streetview?key=${ this.API_KEY }&location=${ this.item.view.latitude },${ this.item.view.longitude }`
                );
                this.cd.markForCheck();
            }, 10);
        }

        this.currentEntityId = this.model.id;
    }

    get model() {
        if (!this.item) return null;
        return {
            ...this.item.model
        };
    }

    get view() {
        if (!this.item) return null;
        return {
            ...this.item.view
        };
    }
}
