# Generated by Django 2.2.12 on 2020-04-29 23:18

from django.db import migrations
import django.db.models.manager


class Migration(migrations.Migration):

    dependencies = [
        ('projects', '0007_project_is_active'),
    ]

    operations = [
        migrations.AlterModelManagers(
            name='project',
            managers=[
                ('all_objects', django.db.models.manager.Manager()),
            ],
        ),
    ]
