# Generated by Django 3.0.8 on 2020-08-10 23:20

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('subscriptions', '0003_auto_20200718_0134'),
    ]

    operations = [
        migrations.RenameField(
            model_name='subscription',
            old_name='created_date',
            new_name='createdDate',
        ),
        migrations.RenameField(
            model_name='subscription',
            old_name='modified_date',
            new_name='modifiedDate',
        ),
    ]