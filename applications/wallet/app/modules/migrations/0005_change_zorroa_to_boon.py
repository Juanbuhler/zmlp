# Generated by Django 3.1.5 on 2021-02-17 18:02

from django.db import migrations


def forward(apps, schema_editor):
    Provider = apps.get_model('modules', 'Provider')
    provider = Provider.objects.get(name='zorroa')
    provider.name = 'boon ai'
    provider.logo_data_uri = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNTc2LjEgNDQ4LjUiPgogIDxwYXRoIGZpbGw9IiNmZmZmZmYiIGQ9Ik01ODEuMiAyMTUuOWMyNy40IDQuNSAzNi45IDI1LjYgMzYuOSA0OS45djUxLjdjMCAzOS42LTMwLjYgNjItNzguMiA2MmgtODEuNFY2NC43aDgyLjNjNDcuNiAwIDc1LjUgMjIuNSA3NS41IDYydjM5LjZjLS4xIDIyLjYtOS45IDQ2LjQtMzUuMSA0OS42em0tNzAuNy0yMS4yaDI5LjNjMTQuOSAwIDI0LjMtNy42IDI0LjMtMjQuM1YxMzRjMC0xNi43LTkuNC0yNC4zLTI0LjMtMjQuM2gtMjkuM3Y4NXptMzEuMSA0NC4xaC0zMS4xdjk1LjhoMzEuMWMxNC45IDAgMjQuMy03LjYgMjQuMy0yNC4zVjI2M2MwLTE2LjYtOS40LTI0LjItMjQuMy0yNC4yek03MzQuMSA2MS4xaDMuNmM0Ny42IDAgNzkuMiAxOC45IDc5LjIgNjYuNnYxODguOWMwIDQ3LjYtMzEuNSA2Ni42LTc5LjIgNjYuNmgtMy42Yy00Ny42IDAtNzkuMi0xOC45LTc5LjItNjYuNlYxMjcuN2MwLTQ4LjEgMzEuNS02Ni42IDc5LjItNjYuNnptMS44IDI3Ni4xYzE3LjEgMCAyOC44LTYuMyAyOC44LTI4LjhWMTM1LjhjMC0yMi41LTExLjctMjguOC0yOC44LTI4LjhzLTI4LjggNi4zLTI4LjggMjguOHYxNzIuN2MwIDIyLjUgMTEuNiAyOC43IDI4LjggMjguN3pNOTM3LjQgNjEuMWgzLjZjNDcuNiAwIDc5LjIgMTguOSA3OS4yIDY2LjZ2MTg4LjljMCA0Ny42LTMxLjUgNjYuNi03OS4yIDY2LjZoLTMuNmMtNDcuNiAwLTc5LjItMTguOS03OS4yLTY2LjZWMTI3LjdjMC00OC4xIDMxLjQtNjYuNiA3OS4yLTY2LjZ6bTEuOCAyNzYuMWMxNy4xIDAgMjguOC02LjMgMjguOC0yOC44VjEzNS44YzAtMjIuNS0xMS43LTI4LjgtMjguOC0yOC44cy0yOC44IDYuMy0yOC44IDI4Ljh2MTcyLjdjMCAyMi41IDExLjcgMjguNyAyOC44IDI4Ljd6TTEwNjMuMyAzNzkuNVY2NC43aDUyLjJsNjYuMSAxOTQuN1Y2NC43aDQ3LjZ2MzE0LjhoLTQ5LjRMMTExMSAxNzQuOHYyMDQuN2gtNDcuN3pNMTQ1Ni40IDI5OC42aC04NC42bC0xNy42IDgxaC0yNC4zbDcwLjItMzE0LjhoMjcuOWw3MS4xIDMxNC44aC0yNS4ybC0xNy41LTgxem0tNS0yMi4xbC0zNy4zLTE3My42LTM3LjMgMTczLjZoNzQuNnpNMTU0MS4zIDM3OS41VjY0LjdoMjQuM3YzMTQuOGgtMjQuM3oiLz4KICA8cGF0aCBmaWxsPSIjMjU0Y2ZmIiBkPSJNNzQuOCAxLjV2NjMuNmg2My42djcxLjZINzQuOHY2My42SDMuM1YxLjVoNzEuNXoiLz4KICA8cGF0aCBmaWxsPSIjZjIwMDAwIiBkPSJNNzQuOCAyNDQuMXY2My42aDYzLjZ2NzEuNkgzLjNWMjQ0LjFoNzEuNXoiLz4KICA8cGF0aCBmaWxsPSIjZWY4ZDAwIiBkPSJNMTgyLjIgMzc5LjN2LTcxLjZoNjMuNnYtNjMuNmg3MS42djEzNS4ySDE4Mi4yeiIvPgogIDxwYXRoIGZpbGw9IiMwMGQ2ZjIiIGQ9Ik0zMTcuNCA2NS4xdjEzNS4yaC03MS42di02My42aC02Ny42VjY1LjFoMTM5LjJ6Ii8+Cjwvc3ZnPgo='  # noqa
    provider.save()


def backward(apps, schema_editor):
    Provider = apps.get_model('modules', 'Provider')
    provider = Provider.objects.get(name='boon ai')
    provider.name = 'zorroa'
    provider.logo_data_uri = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDMuMyAzMCI+CiAgPHBhdGggZD0iTTExLjMgMGwxMS45IDQuNy0uOCAxLjd6bS0uMy45TDIzLjUgOGwtNy40IDEyLjctMi40LTEuNiA3LjgtOS42eiIgZmlsbD0iI2ZmZmZmZiIvPgogIDxwYXRoIGQ9Ik05LjQuOGwxMC44IDkuMS05LjcgMTEtMS44LTEuNyA5LjYtOC4zeiIgZmlsbD0iI2I4ZDY1MiIvPgogIDxwYXRoIGQ9Ik0xMy43IDkuNEwxLjEgMTEuOGwuNCAxLjd6bS0uMSAxTDAgMTQuOWw0LjcgMTMuOSAyLjgtMS4xLTUuOC0xMXoiIGZpbGw9IiNmZmZmZmYiLz4KICA8cGF0aCBkPSJNMTUuMSAxMC41TDIuOSAxNy40IDEwLjMgMzBsMi4xLTEuMy03LjktMTB6IiBmaWxsPSIjYjhkNjUyIi8+CiAgPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTk5LjMgMTAuN2gtMmwtMy45IDkuOWgybDEtMi40aDMuOGwxIDIuNGgybC0zLjktOS45em0uMyA1LjhIOTdsMS4zLTMuOSAxLjMgMy45em0tMjIuMy0xLjJjLjQtLjUuNi0xIC42LTEuNyAwLTEtLjUtMS43LTEuMy0yLjMtLjYtLjMtMS41LS41LTIuNi0uNWgtNC4zdjkuOWgxLjl2LTQuMWgyLjZsMiA0LjFoMkw3NiAxNi4yYy41LS4yLjktLjUgMS4zLS45ek03NiAxMy42YzAgLjUtLjEuOC0uNSAxLS4yLjEtLjUuMy0uNy40LS4zLjEtLjYuMS0xIC4xaC0yLjN2LTIuOUg3NGMuNiAwIDEgLjEgMS40LjIuNC40LjYuNy42IDEuMnptLTExIDEuN2MuNC0uNS42LTEgLjYtMS43IDAtMS0uNS0xLjctMS4zLTIuMy0uNi0uNC0xLjUtLjYtMi42LS42aC00LjN2OS45aDEuOXYtNC4xSDYybDIgNC4xaDJsLTIuMi00LjRjLjUtLjIuOS0uNSAxLjItLjl6bS0xLjMtMS43YzAgLjUtLjEuOC0uNSAxLS4yLjEtLjUuMy0uNy40LS4zLjEtLjYuMS0xIC4xaC0yLjN2LTIuOWgyLjVjLjYgMCAxIC4xIDEuNC4yLjUuNC42LjcuNiAxLjJ6bS0yMy0xLjd2LTEuMmgtNy44djEuNWg1LjRsLTUuNiA3djEuNGg4LjF2LTEuNUgzNXptMTAuMi0xYy00LTEuNC03LjkgMi41LTYuNSA2LjUuNSAxLjUgMS43IDIuNiAzLjEgMy4xIDQgMS40IDcuOS0yLjUgNi41LTYuNS0uNS0xLjQtMS43LTIuNi0zLjEtMy4xem0xLjYgNGMuNiAyLjQtMS42IDQuNy00LjEgNC4xLTEuMi0uMy0yLjEtMS4yLTIuNC0yLjQtLjYtMi40IDEuNi00LjcgNC4xLTQuMSAxLjEuMyAyIDEuMiAyLjQgMi40em0zNS40LTRjLTQtMS40LTcuOSAyLjUtNi41IDYuNS41IDEuNSAxLjcgMi42IDMuMSAzLjEgNCAxLjQgNy45LTIuNSA2LjUtNi41LS41LTEuNC0xLjctMi42LTMuMS0zLjF6bTEuNSA0Yy42IDIuNC0xLjYgNC43LTQuMSA0LjEtMS4yLS4zLTIuMS0xLjItMi40LTIuNC0uNi0yLjQgMS42LTQuNyA0LjEtNC4xIDEuMS4zIDIuMSAxLjIgMi40IDIuNHoiLz4KPC9zdmc+Cg=='  # noqa
    provider.save()


class Migration(migrations.Migration):

    dependencies = [
        ('modules', '0004_data_migration_aws_azure_providers'),
    ]

    operations = [
        migrations.RunPython(forward, reverse_code=backward)
    ]
