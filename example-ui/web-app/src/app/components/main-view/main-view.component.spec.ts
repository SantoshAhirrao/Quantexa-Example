import { Component, Input } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MainViewComponent } from './main-view.component';
import { TabManagerConfiguration } from '@quantexa/explorer-web-core/ui-navigation';


@Component({
    selector: 'qe-notification-container',
    template: ''
})
class MockNotificationContainerComponent { }

@Component({
    selector: 'qe-tab-manager',
    template: ''
})
class MockTabManagerComponent {
    @Input() config: TabManagerConfiguration;
}

describe('MainViewComponent', () => {
    let component: MainViewComponent;
    let fixture: ComponentFixture<MainViewComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                MainViewComponent,
                MockNotificationContainerComponent,
                MockTabManagerComponent
            ]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(MainViewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
