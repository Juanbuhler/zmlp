# Generated by Django 3.2.5 on 2021-07-27 00:44

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('records', '0006_rename_video_minutes_apicall_video_seconds'),
    ]

    operations = [
        migrations.AlterUniqueTogether(
            name='apicall',
            unique_together=set(),
        ),
    ]