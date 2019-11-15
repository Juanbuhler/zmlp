import base64

from django.core.management.base import BaseCommand
from django.db import transaction
from pathlib2 import Path

from users.models import User


class Command(BaseCommand):
    help = 'Creates a project user and adds their ZMLP Api Key'

    def add_arguments(self, parser):
        parser.add_argument('username', type=str)
        parser.add_argument('password', type=str)
        parser.add_argument('project', type=str)
        parser.add_argument('key_file', type=str)

    def handle(self, *args, **options):
        key = base64.b64encode(Path(options['key_file']).expanduser().read_bytes()).decode('utf-8')
        with transaction.atomic():
            user = User.objects.create(username=options['username'])
            user.set_password(options['password'])
            user.add_project(key, options['project'])
