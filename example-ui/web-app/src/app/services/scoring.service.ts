import { Injectable } from '@angular/core';
import { ProjectLibraryService } from '@quantexa/explorer-web-core/project-library-loader';
import { InvestigationLib } from '@quantexa/explorer-web-core/ui-investigation';
import { SearchProjectLib } from '@quantexa/explorer-web-core/ui-search';


@Injectable({ providedIn: 'root' })
export class ScoringService {
    searchLib: SearchProjectLib;
    investigationLib: InvestigationLib;

    constructor(projectLib: ProjectLibraryService) {
        this.searchLib = projectLib.get('search');
        this.investigationLib = projectLib.get('investigation');
    }

    getTotalScore(entityData: any) {
        return this.searchLib.searchUtil.getEntityScore(entityData);
    }

    getScoreColor(score: number) {
        const colorTiers = this.investigationLib.investigationUtil.getScoringColors();
        const colorTier = colorTiers.find(tier => tier.interval[0] <= score && score <= tier.interval[1]);
        return colorTier ? colorTier.color : 'white';
    }
}
