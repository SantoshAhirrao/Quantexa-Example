import { NgModule } from '@angular/core';
import {
    PluginConfig,
    PluginModule,
    PluginPlacement
} from '@quantexa/explorer-web-core/plugin-loader';
import { PrintExamplePluginComponent } from './example/print-example-plugin.component';
import { CommonModule } from '@angular/common';

@NgModule({
    imports: [CommonModule],
    entryComponents: [PrintExamplePluginComponent],
    declarations: [PrintExamplePluginComponent],
    exports: [PrintExamplePluginComponent]
})
@PluginConfig({
    name: 'Investigation Print Content Plugin',
    description: 'Print Content for an investigation.',
    placements: [
        new PluginPlacement({
            slot: 'investigation-print-content-example',
            component: PrintExamplePluginComponent
        })
    ]
})
export class InvestigationPrintContentPluginModule extends PluginModule {}
