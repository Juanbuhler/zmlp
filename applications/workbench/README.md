# Workbench

Workbench is a Jupyter notebook server that runs concurrently with the rest of ZMLP and allows notebooks to be run against the server.

## Getting Started

### Prerequisites

- Make sure you can run `docker-compose up` on the main zmlp repo directory

### Build the docker container

```
docker build . -t 'zmlp/workbench'
```

or two run the server, Workbench and Sandbox all at once, run

```
docker-compose -f docker-compose.yml -f applications/workbench/docker-compose.yml -f applications/sandbox/docker-compose.yml up --force-recreate -d
```

### Run

From the main zmlp repo folder:

```
docker-compose up -d
docker-compose -f docker-compose.yml -f applications/workbench/docker-compose.yml up workbench
```

Then navigate to http://localhost:8888/ . Use the password "admin" to log in.
