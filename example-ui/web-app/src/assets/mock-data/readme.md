# List of mocked operations

## Search

### Endpoint: /search/configuration

```
search-configuration.json
```

### Endpoint: /search/search

Query string "michael greene", single data source (aggregatedwire), no parameter filters, first page.
```
search-ds-aggregatedwire-request.json
search-ds-aggregatedwire-response.json
```

Query string "michael greene", single data source (customerbusiness), no parameter filters, first page.
```
search-ds-customerbusiness-request.json
search-ds-customerbusiness-response.json
```

Query string "michael greene", single data source (customerindividual), no parameter filters, first page.
```
search-ds-customerindividual-request.json
search-ds-customerindividual-response.json
```

### Endpoint: /entity-search/search

Query string "michael greene", entity resolution (individual+business), single data source (customerindividual), no parameter filters, first page.
Results just for individual.
```
search-entity-individual-business-request.json
search-entity-individual-business-response.json
```

## Investigations list

### Endpoint: explorer/investigations GET

Request all the investigations visible to the user.
```
explorer/investigations?sortFieldOption=createdDate&sortAsc=false&filterOption=&page=0&pageSize=25
investigation-list-response.json
```

## Investigation

### Endpoint: /explorer/investigation POST

Request a new investigation for an entity (Individual, Michael Greene).
```
investigation-new-request.json
investigation-new-response.json
```

### Endpoint: /explorer/investigation-config GET

Request the investigation configuration.
```
investigation-config-response.json
```

### Endpoint: /explorer/investigation/{id} POST

Request an investigation by id (just an entity, Michael Greene).
```
investigation-id-response.json
```

### Endpoint: /explorer/investigation/{id}/privileges GET

Request all available privileges for the investigation.
```
investigation-privileges-response.json
```

### Endpoint: /explorer/investigation/{id}/documents?investigationTag=0 POST

Request the details of a document from an investigation.
```
investigation-document-request.json
investigation-document-response.json
```
