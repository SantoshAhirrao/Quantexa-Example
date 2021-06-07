import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { KeepHtmlPipe } from '@quantexa/explorer-web-core/pipes';
import { EntityIndividualComponent } from './entity-individual.component';


describe('EntityIndividualComponent', () => {
    let component: EntityIndividualComponent;
    let fixture: ComponentFixture<EntityIndividualComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [EntityIndividualComponent,
                KeepHtmlPipe]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(EntityIndividualComponent);
        component = fixture.componentInstance;
        component.context = { data: {} };
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should return context item for documents', () => {
        component.context = {
            data: {
                index: 5,
                isSelected: true,
                itemType: 'document',
                item: Object({
                    'attributes': {
                        'full_name': 'John DOE',
                        'gender': 'M',
                        'cus_risk_rtng': '2137'
                    },
                    'documents': {},
                    'entityId': 'individual-/sthsthdarkside',
                    'entityType': 'individual',
                    'scoreDetail': {},
                    'scoreSummary': {},
                    'sortFields': {},
                    'template': {}
                })
            }
        };
        const expectation = Object({
            'full_name': 'John DOE',
            'gender': 'M',
            'cus_risk_rtng': '2137',
            'scoreSummary': {},
            'id': 'individual-/sthsthdarkside'
        });
        expect(component.entity).toEqual(expectation);
    });
});

