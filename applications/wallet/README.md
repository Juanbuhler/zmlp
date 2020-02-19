# Wallet

# Development Quickstart
_NOTE_: Zorroa issues Macbooks to developers so all instructions are for MacOS.

## Prerequisites
- Latest Docker and docker-compose installed.

## Start the local ZMLP Deployment

First off we will use docker-compose to start a local deployment of ZMLP. This will pull all of the 
latest deployed container images and start them up locally. Once you have a complete ZMLP
deployment running based off the main code branch, `development`. Once the deployment is up you 
can access wallet at http://localhost.

### Steps

1. Run `docker-compose up`

## Building & Running your local code.
Once you have made changes to the Wallet code you can build and run those changes in the local 
deployment. This will build all the local wallet code, package it in a docker container and then
run it. Once it's up and running you can test that were changes are working as expected.

### Steps

`docker-compose -f docker-compose.yml -f docker-compose.local-build-wallet.yml up --build`

# Advanced Development Options
There are multiple ways to run this application which may be better suited to the type of development
you are doing. 

1. _Frontend Development_ - Use the Frontend development server. For the
   Backend, you can either use the runserver or the Docker container.
1. _Backend Development_ - Use the Backend Development server.

---

## Frontend Development
The frontend for wallet is written using React and can be found in `wallet/frontend`

### Prerequisites
- Node.js 12.14.0 or greater installed.

See the [frontend README](frontend/README.md) for more info.

---

## Backend Development

### Prerequisites
- [Python](https://www.python.org/downloads/) 3.8.0 or greater installed.
- Latest [Pipenv](https://github.com/pypa/pipenv) installed.
- [Homebrew](https://docs.brew.sh/Installation) installed.


#### Install 

Pipenv is used to manage package dependencies and the python version. Install it
with homebrew.

1. `brew install pipenv`
2. (Optional) Run `echo 'eval "$(pipenv --completion)"' >> ~/.bash_profile` to
   add pipenv completion to your shell.
3. Restart your shell to pickup the changes: `exec "$SHELL"`

#### Install Python dependencies

1. `cd` into the `wallet` base directory (`zmlp/applications/wallet`).
2. Run `pipenv sync`

#### Use your Pipenv

To open a shell with your pipenv activated, run:

- `pipenv shell` If you're using an IDE with built-in Django support, here's
  some helpful commands for setting up the IDE:
- Get the path to your pipenv Python Interpreter: `pipenv --py`
- Get the location of the virtualenv that pipenv is using: `pipenv --venv`
- Install a new python package and add it to your pipenv:
  `pipenv install $PACKAGE`

#### Start Backend Development Server

The Django runserver will serve out the frontend, assuming that a build is
present. More info on the Django runserver can be found
[here](https://docs.djangoproject.com/en/2.2/intro/tutorial01/#the-development-server).

1. CD into the project directory: `cd app`
1. Make sure you've built the Frontend if you expect the backend to serve it
   (instructions above).
1. Make sure your database is up to date:
   `./manage.py migrate --settings=wallet.settings.local`
   - If you receive an error about a Role, User, or DB not existing when running
     migrate, check the "Postgres Setup" section below.
1. `./manage.py runserver --settings=wallet.settings.local`
1. Your server will now be running on `http://localhost:8000`

- _Note:_ You can drop the `--settings=wallet.settings.local` from the previous
  commands if you specify this in the `DJANGO_SETTINGS_MODULE` env variable. For
  example: `export DJANGO_SETTINGS_MODULE=wallet.settings.local`

##### Postgres Setup

The development server has been setup to use Postgres for it's DB rather than
SQLite, due to us using some Postgres specific fields. The first time setting up
Postgres, you'll need to create the wallet DB and User/Role.

1. Make sure the `wallet` db has been created: `$ createdb wallet`
2. Start the PG Console: `$ psql -h localhost`
3. Create Role in the console (replace `$password` with password from settings
   file): `# CREATE ROLE wallet WITH LOGIN PASSWORD '$password';`
4. Set permissions in the console:
   `# GRANT ALL PRIVILEGES ON DATABASE wallet TO wallet;`
5. Give last permission to user in the console: `# ALTER USER wallet CREATEDB;`

---

#### Browsable API

One of the benefits of using the Backend runserver is that it will setup a
browsable API you can access through your web browser. This provides an easy way
to see what endpoints are available, the supported methods, and the expected
arguments for them. To access this:

1.  With the backend server running.
2.  Navigate to http://127.0.0.1:8000/api/v1/
3.  This will drop you in the API Root.

From here, you should be able to follow the links on the available resources to
see what is available.

---

### Style Guide

Unless otherwise noted below this projects adheres to the
[pep8](https://www.python.org/dev/peps/pep-0008/) style guide.

_Exceptions and Extension to the Rules:_

- Docstrings follow the google python style. An excellent example can be found
  [here](https://sphinxcontrib-napoleon.readthedocs.io/en/latest/example_google.html).
- Max line width is 100 characters.
- Lambda functions are avoided at almost all costs. We ride with Guido on this
  one.
- String formatting always uses the `f'{VARIABLE}'` style.
- Variable names are never abbreviated, characters are cheap and readability is
  priceless. Yes: `project` No: `proj`.
- Any block of code that needs to be separated by newlines is preceded by a
  comment.

### Testing

- Unit tests are located in each of the app directories in a file named
  `tests.py`
- There is a suite of smoke tests that run against a live server. These tests
  use the REST api to accomplish the basic functionality of the application.
  These tests are located in `wallet.tests`.
- Code coverage must meet or exceed 98% in order to pass CI.

## Building and Running the Docker Container

The Dockerfile builds a container with the Django project that is capable of
running the backend web server as well as a celery worker for the processing
queue.

- _Build the container._ - `docker build -t wallet .`
- _Run the web server._ -
  `docker run -p 8080:8080 wallet sh /app/start-server.sh`

---

## External Services

### Continuous Integration

CI is handled by Gitlab and configured in the gitlab-ci.yml file. The config
file is documented and the best place to go to understand what happens during
the CI process.

### Error Tracking

This application is configured to send all errors to the Sentry service
(https://sentry.io/organizations/zorroa-eb/projects/). The errors can be viewed
in the wallet app in the #Zorroa organization. The configuration can be found in
the settings file.

### Mail Delivery

Emails is configured to be sent via SMTP by [MailGun](https://app.mailgun.com/).
The credentials to MailGun are stored in 1password. The SMTP_PASSWORD can found
by going to the MailGun console [here](https://app.mailgun.com/) and looking at
mg.zorroa.com under the domains menu.

---

## Deployment Options

Many of the configuration options can be set using environment variables. Below 
are the current options. These environment variables need to be set on the running
wallet container.

| Environment Variable | Effect |
| -------------------- | ------ |
| ENABLE_SENTRY | Enables Sentry error logging if "true". |
| ENVIRONMENT | Name of the environment wallet is running in (i.e. qa, staging). |
| ZMLP_API_URL | FQDN for the ZMLP REST API. |
| PG_HOST | Hostname of the Postgres DB server. |
| PG_PASSWORD | Password to be used by the wallet Postgres user. |
| SMTP_PASSWORD | Password for the MailGun SMTP server used for sending emails. |
| GOOGLE_OAUTH_CLIENT_ID | Client ID for Google OAuth used for Google sign in. |
