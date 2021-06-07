import { animate, animateChild, group, query, style, transition, trigger } from '@angular/animations';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DateTimeAdapter } from 'ng-pick-datetime';


@Component({
    selector: 'qe-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    animations: [
        trigger('routeAnimation', [
            transition('true => false', [
                group([
                    query('.app-view:leave', animateChild(), { optional: true }),
                    query('.app-view:leave', style({
                            zIndex: 0
                          }), { optional: true }),
                    query('.app-view:enter', style({
                        transform: 'translateX(100%)',
                        zIndex: 1
                    })),
                    query('.app-view:enter', animate('350ms ease-in', style({
                        transform: 'translateX(0)'
                    })))
                ])
            ]),
            transition('false => true', [
                group([
                    query('.app-view:leave', animateChild(), { optional: true }),
                    query('.app-view:leave', style({
                            transform: 'translateX(0)',
                            zIndex: 100
                    }), { optional: true }),
                    query('.app-view:enter', style({
                        zIndex: 0
                    })),
                    query('.app-view:leave', animate('350ms ease-in-out', style({
                        transform: 'translateX(-100%)'
                    })), { optional: true }),
                    query('router-outlet ~ *', [
                        style({}),
                        animate(1, style({}))
                    ], { optional: true })
                ])
            ])
        ])
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
    title = 'Quantexa Explorer';

    constructor(private dateTimeAdapter: DateTimeAdapter<any>) {
        dateTimeAdapter.setLocale('en-GB');
    }

    isTabView(outlet: RouterOutlet) {
        return outlet.activatedRouteData.isTabView;
    }
}
