import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { KeepHtmlPipe } from '@quantexa/explorer-web-core/pipes';
import { EntityAccountComponent } from './entity-account.component';


describe('EntityAccountComponent', () => {
    let component: EntityAccountComponent;
    let fixture: ComponentFixture<EntityAccountComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [EntityAccountComponent,
                KeepHtmlPipe]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(EntityAccountComponent);
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
                        'bussines_display': 'JOIN EMPIRE',
                        'is_on_offshore': false,
                        'total_documents': 21,
                        'total_records': 37
                    },
                    'documents': {},
                    'entityId': 'account-/sthsthdarkside',
                    'entityType': 'individual',
                    'scoreDetail': {},
                    'scoreSummary': {},
                    'sortFields': {},
                    'template': {}
                })
            }
        };
        const expectation = Object({
            'bussines_display': 'JOIN EMPIRE',
            'is_on_offshore': false,
            'total_documents': 21,
            'total_records': 37,
            'scoreSummary': {},
            'id': 'account-/sthsthdarkside'
        });
        expect(component.entity).toEqual(expectation);
    });
});

