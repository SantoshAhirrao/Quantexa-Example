import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ResultDetailsBaseComponent } from '@quantexa/explorer-web-core/ui-search';
import { ScoringService } from '../../../services/scoring.service';


@Component({
    selector: 'qe-search-details-entity-telephone',
    templateUrl: './entity-telephone.component.html',
    styleUrls: ['./entity-telephone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityTelephoneComponent extends ResultDetailsBaseComponent {
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

    get scores() {
        if (!(this.item.scoreSummary)) return null;
        else return this.item.scoreSummary;
    }

    get totalScore() {
        return this.Scores.getTotalScore(this.entity);
    }

    getScoreColor(score: number) {
        return this.Scores.getScoreColor(score);
    }
}
