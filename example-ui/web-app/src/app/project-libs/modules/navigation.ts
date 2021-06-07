import { NavigationProjectLib, ProjectLib } from '@quantexa/explorer-web-core/ui-navigation';


export const lib: NavigationProjectLib = {
    ...ProjectLib,
    logoutOptions: [
        {
            name: 'SAML logout',
            redirectionLink: '/saml/logout'
        },
    ]
};
