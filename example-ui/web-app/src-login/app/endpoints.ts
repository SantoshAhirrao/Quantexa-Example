import { EndpointConfiguration, HTTP_VERBS } from '@quantexa/explorer-web-core/api-client';

export function saveSecret(p: {secret: string}) {
    return `saveTotp/${p.secret}`;
}

export const enum ALIASES {
    AUTHENTICATE = 'Login:Authenticate',
    GET_PROPERTY_MAPPINGS = 'Login:GetPropertyMappings',
    GENERATE_SECRET = 'Login:GenerateSecret',
    SAVE_SECRET = 'Login:SaveSecret'
}

export const API_ENDPOINTS: EndpointConfiguration[] = [
    {
        alias: ALIASES.AUTHENTICATE,
        method: HTTP_VERBS.POST,
        path: 'authenticate',
        isForm: true,
        contentType: 'text'
    },
    {
        alias: ALIASES.GET_PROPERTY_MAPPINGS,
        method: HTTP_VERBS.GET,
        path: 'getPropertyMappings',
        prefix: '/api-root'
    },
    {
        alias: ALIASES.GENERATE_SECRET,
        method: HTTP_VERBS.GET,
        path: 'generateSecret',
        prefix: '/api-root'
    },
    {
        alias: ALIASES.SAVE_SECRET,
        method: HTTP_VERBS.POST,
        path: saveSecret,
        prefix: '/api-root'
    }
];
