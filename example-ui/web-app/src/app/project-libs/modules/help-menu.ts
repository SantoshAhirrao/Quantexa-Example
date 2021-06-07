import { HelpMenuProjectLib, ProjectLib } from '@quantexa/explorer-web-core/ui-help-menu';

export const lib: HelpMenuProjectLib = {
    ...ProjectLib,
    helpLinksOptions: {
        staticLinks: [
            {
                label: 'Help Centre',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'FAQ',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                separator: true
            },
            {
                label: 'Searching',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'Investigation',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'Bulk search',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'Tasks and task lists',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'Transaction viewer',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'Glossary',
                url: 'https://www.google.com',
                target: '_blank'
            },
            {
                label: 'User-defined inteligence',
                items: [
                    {
                        label: 'Overview',
                        url: 'https://www.google.com',
                        target: '_blank'
                    },
                    {
                        label: 'Creating user-defined document',
                        url: 'https://www.google.com',
                        target: '_blank'
                    },
                    {
                        label: 'Editing user-defined document',
                        url: 'https://www.google.com',
                        target: '_blank'
                    },
                    {
                        label: 'Deleting user-defined document',
                        url: 'https://www.google.com',
                        target: '_blank'
                    },
                    {
                        label: 'Creating user-defined document',
                        url: 'https://www.google.com',
                        target: '_blank'
                    },
                    {
                        label: 'Editing permissions',
                        url: 'https://www.google.com',
                        target: '_blank'
                    }
                ]
            },
        ]
    }
};
