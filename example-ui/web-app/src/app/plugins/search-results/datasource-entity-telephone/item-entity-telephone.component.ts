import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultItemBaseComponent } from '@quantexa/explorer-web-core/ui-search';
import { ScoringService } from '../../../services/scoring.service';


@Component({
    selector: 'qe-search-item-entity-telephone',
    templateUrl: './item-entity-telephone.component.html',
    styleUrls: ['./item-entity-telephone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ItemEntityTelephoneComponent extends ResultItemBaseComponent {
    dataSource = 'telephone';

    constructor(protected changeDetector: ChangeDetectorRef, private Scores: ScoringService) {
        super(changeDetector);
    }

    get entity() {
        if (!this.item) return null;
        return {
            ...this.item.attributes,
            scoreSummary: this.item.scoreSummary,
            id: this.item.entityId
        };
    }

    get totalScore() {
        return this.Scores.getTotalScore(this.item);
    }

    getScoreColor(score: number) {
        return this.Scores.getScoreColor(score);
    }
}
