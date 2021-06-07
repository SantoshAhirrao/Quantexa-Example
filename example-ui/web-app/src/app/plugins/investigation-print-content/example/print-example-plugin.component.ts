import { PluginComponent } from '@quantexa/explorer-web-core/plugin-loader';
import {
    Component,
    ChangeDetectorRef,
    ChangeDetectionStrategy,
    OnInit,
    OnDestroy
} from '@angular/core';
import { Subscription } from 'rxjs';
import {
    StoreManagerService,
    State,
    MODULE_NAME as TRANSACTION_MODULE_NAME
} from '@quantexa/explorer-web-core/ui-transaction-viewer';
import { Store } from '@ngrx/store';
import { CreateContainerAction } from '@quantexa/explorer-web-core/global-store-actions';

@Component({
    selector: 'qe-investigation-view-print-example-plugin',
    templateUrl: './print-example-plugin.component.html',
    styleUrls: ['./print-example-plugin.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PrintExamplePluginComponent extends PluginComponent implements OnInit, OnDestroy {
    hits: any = [];
    sub = new Subscription();

    constructor(protected changeDetector: ChangeDetectorRef,
        private storeManager: StoreManagerService,
        private store: Store<State>) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.store.dispatch(
            new CreateContainerAction({
                feature: TRANSACTION_MODULE_NAME,
                id: this.context.data.containerId
            })
        );
        this.storeManager.init(
            this.context.data.containerId,
            TRANSACTION_MODULE_NAME,
            this.store
        );

        this.sub = this.storeManager.select(state => state).subscribe(
            (state: any) => {
                Object.values(state.configurations).map(
                    (configuration: any) => {
                        this.hits = configuration.transactions.results
                            ? configuration.transactions.results.hits
                            : this.hits;
                        this.changeDetector.markForCheck();
                    }
                );
            }
        );
    }

    ngOnDestroy(): void {
        this.sub.unsubscribe();
    }
}
