# Generated by Django 3.1.3 on 2020-11-23 22:27

from django.contrib.auth import get_user_model
from django.db import migrations
from django.conf import settings


User = get_user_model()


def forwards(apps, schema_editor):
    User.objects.create_superuser(username=settings.SUPERUSER_EMAIL,
                                  email=settings.SUPERUSER_EMAIL,
                                  password=settings.SUPERUSER_PASSWORD,
                                  first_name=settings.SUPERUSER_FIRST_NAME,
                                  last_name=settings.SUPERUSER_LAST_NAME)


def reverse(apps, schema_editor):
    User.objects.get(username=settings.SUPERUSER_EMAIL).delete()


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.RunPython(forwards, reverse)
    ]