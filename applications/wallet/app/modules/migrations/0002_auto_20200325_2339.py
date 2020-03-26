# flake8: noqa
# Generated by Django 2.2.11 on 2020-03-25 23:39

from django.db import migrations


default_providers = [
    {'name': 'zorroa',
     'description': 'These analysis modules are included in your base package. You can run as many as you’d like, but running more than you need will increase processing time.',  # noqa
     'logo': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDMuMyAzMCI+CiAgPHBhdGggZD0iTTExLjMgMGwxMS45IDQuNy0uOCAxLjd6bS0uMy45TDIzLjUgOGwtNy40IDEyLjctMi40LTEuNiA3LjgtOS42eiIgZmlsbD0iI2ZmZmZmZiIvPgogIDxwYXRoIGQ9Ik05LjQuOGwxMC44IDkuMS05LjcgMTEtMS44LTEuNyA5LjYtOC4zeiIgZmlsbD0iI2I4ZDY1MiIvPgogIDxwYXRoIGQ9Ik0xMy43IDkuNEwxLjEgMTEuOGwuNCAxLjd6bS0uMSAxTDAgMTQuOWw0LjcgMTMuOSAyLjgtMS4xLTUuOC0xMXoiIGZpbGw9IiNmZmZmZmYiLz4KICA8cGF0aCBkPSJNMTUuMSAxMC41TDIuOSAxNy40IDEwLjMgMzBsMi4xLTEuMy03LjktMTB6IiBmaWxsPSIjYjhkNjUyIi8+CiAgPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTk5LjMgMTAuN2gtMmwtMy45IDkuOWgybDEtMi40aDMuOGwxIDIuNGgybC0zLjktOS45em0uMyA1LjhIOTdsMS4zLTMuOSAxLjMgMy45em0tMjIuMy0xLjJjLjQtLjUuNi0xIC42LTEuNyAwLTEtLjUtMS43LTEuMy0yLjMtLjYtLjMtMS41LS41LTIuNi0uNWgtNC4zdjkuOWgxLjl2LTQuMWgyLjZsMiA0LjFoMkw3NiAxNi4yYy41LS4yLjktLjUgMS4zLS45ek03NiAxMy42YzAgLjUtLjEuOC0uNSAxLS4yLjEtLjUuMy0uNy40LS4zLjEtLjYuMS0xIC4xaC0yLjN2LTIuOUg3NGMuNiAwIDEgLjEgMS40LjIuNC40LjYuNy42IDEuMnptLTExIDEuN2MuNC0uNS42LTEgLjYtMS43IDAtMS0uNS0xLjctMS4zLTIuMy0uNi0uNC0xLjUtLjYtMi42LS42aC00LjN2OS45aDEuOXYtNC4xSDYybDIgNC4xaDJsLTIuMi00LjRjLjUtLjIuOS0uNSAxLjItLjl6bS0xLjMtMS43YzAgLjUtLjEuOC0uNSAxLS4yLjEtLjUuMy0uNy40LS4zLjEtLjYuMS0xIC4xaC0yLjN2LTIuOWgyLjVjLjYgMCAxIC4xIDEuNC4yLjUuNC42LjcuNiAxLjJ6bS0yMy0xLjd2LTEuMmgtNy44djEuNWg1LjRsLTUuNiA3djEuNGg4LjF2LTEuNUgzNXptMTAuMi0xYy00LTEuNC03LjkgMi41LTYuNSA2LjUuNSAxLjUgMS43IDIuNiAzLjEgMy4xIDQgMS40IDcuOS0yLjUgNi41LTYuNS0uNS0xLjQtMS43LTIuNi0zLjEtMy4xem0xLjYgNGMuNiAyLjQtMS42IDQuNy00LjEgNC4xLTEuMi0uMy0yLjEtMS4yLTIuNC0yLjQtLjYtMi40IDEuNi00LjcgNC4xLTQuMSAxLjEuMyAyIDEuMiAyLjQgMi40em0zNS40LTRjLTQtMS40LTcuOSAyLjUtNi41IDYuNS41IDEuNSAxLjcgMi42IDMuMSAzLjEgNCAxLjQgNy45LTIuNSA2LjUtNi41LS41LTEuNC0xLjctMi42LTMuMS0zLjF6bTEuNSA0Yy42IDIuNC0xLjYgNC43LTQuMSA0LjEtMS4yLS4zLTIuMS0xLjItMi40LTIuNC0uNi0yLjQgMS42LTQuNyA0LjEtNC4xIDEuMS4zIDIuMSAxLjIgMi40IDIuNHoiLz4KPC9zdmc+Cg=='},
    {'name': 'google',
     'description': 'These analysis modules call directly into the Google Cloud Vision API and can be activated individually. Contact your Account Manager to activate.',
     'logo': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNjggMjciPgogIDxwYXRoIGQ9Ik0xOS43NiA2Ljg4NmguMWwtLjA2LS4wNSAzLjc0MS0zLjczMi4wMDgtLjA2N0ExMi4xOSAxMi4xOSAwIDAwMTIuMDAzLjUxMWExMi4xNjcgMTIuMTY3IDAgMDAtOC4yNDcgOC40NDdjLjA2Ni0uMDI2LjEzMy0uMDQ5LjIwMS0uMDY1YTguODE3IDguODE3IDAgMDE0Ljc4Ny0xLjQ1IDguODQgOC44NCAwIDAxMS43MTYuMTY0Yy4wMjktLjAxOC4wNTYtLjAyNS4wNzktLjAyYTYuNzYxIDYuNzYxIDAgMDE5LjI0LS43aC0uMDE5IiBmaWxsPSIjRUE0MzM1Ii8+CiAgPHBhdGggZD0iTTI3LjI3NSA4Ljk1OGExMi4xOSAxMi4xOSAwIDAwLTMuNjc2LTUuOTFsLTMuOCAzLjc4OWE2LjczMSA2LjczMSAwIDAxMi40OCA1LjM0NHYuNjcxYzQuNTA3IDAgNC41MDcgNi43NDYgMCA2Ljc0NmgtNi43NjNsLS4wMS4wMXY1LjM1NGgtLjAzbC4wNC4wNGg2Ljc2MmE4Ljc5NyA4Ljc5NyAwIDAwOC40Ny02LjE3MyA4Ljc3MiA4Ljc3MiAwIDAwLTMuNDczLTkuODcxIiBmaWxsPSIjNDI4NUY0Ii8+CiAgPHBhdGggZD0iTTguNzQ0IDI0Ljk2Mmg2Ljc2MXYtNS4zOThoLTYuNzZjLS40NzkgMC0uOTU2LS4xMDMtMS4zOS0uMzAzbC0zLjkxIDMuODk5LS4wMS4wNGE4Ljc3IDguNzcgMCAwMDUuMzEgMS43NjIiIGZpbGw9IiMzNEE4NTMiLz4KICA8cGF0aCBkPSJNOC43NDQgNy40NDRhOC44IDguOCAwIDAwLTguMjgyIDUuOTcyIDguNzU2IDguNzU2IDAgMDAyLjk3MyA5Ljc1NGwzLjkyNC0zLjkxMmMtMi4xNTgtLjk3My0yLjY3My0zLjc5LTEtNS40NTkgMS42NzItMS42NjcgNC40OTYtMS4xNTQgNS40NzIuOTk2bDMuOTIzLTMuOTFhOC43OTUgOC43OTUgMCAwMC03LjAxLTMuNDQxIiBmaWxsPSIjRkJCQzA1Ii8+CiAgPHBhdGggZD0iTTQ5LjE1IDIxLjE5MmE4LjY4IDguNjggMCAwMS02LjI3Mi0yLjU2NSA4LjM0NyA4LjM0NyAwIDAxLTIuNjI4LTYuMTggOC4zMyA4LjMzIDAgMDEyLjYyOC02LjE3OCA4LjY1OSA4LjY1OSAwIDAxNi4yNzItMi42MTUgOC4zOTYgOC4zOTYgMCAwMTYuMDEgMi40MThMNTMuNDcgNy43ODVhNi4wOSA2LjA5IDAgMDAtNC4zMTktMS43MDMgNi4wMTQgNi4wMTQgMCAwMC00LjQ2OCAxLjg5NCA2LjE4MyA2LjE4MyAwIDAwLTEuODMgNC41MDEgNi4xNjUgNi4xNjUgMCAwMDEuODMgNC40NDEgNi4zODIgNi4zODIgMCAwMDguODkuMTA0IDUuMDI3IDUuMDI3IDAgMDAxLjMwMS0yLjk0NmgtNS42OTl2LTIuNDI4aDguMDY5Yy4wOC40ODguMTA4Ljk4Ni4wOTUgMS40OGE3LjU5MSA3LjU5MSAwIDAxLTIuMDgzIDUuNTkyIDguMDgyIDguMDgyIDAgMDEtNi4xMDUgMi40NzJ6bTE4Ljc0OC0xLjYxYTUuODI2IDUuODI2IDAgMDEtOC4wNDMgMCA1LjM5OSA1LjM5OSAwIDAxLTEuNjQ3LTQuMDI2IDUuNDAyIDUuNDAyIDAgMDExLjY0Ny00LjAyNyA1LjgyOCA1LjgyOCAwIDAxOC4wNDMgMCA1LjM5NCA1LjM5NCAwIDAxMS42NDMgNC4wMjdjLjA0IDEuNTEtLjU2IDIuOTctMS42NTIgNC4wMjdoLjAwOXptLTYuMjY3LTEuNTg0YTMuMDg0IDMuMDg0IDAgMDA0LjQ5MiAwIDMuMzc0IDMuMzc0IDAgMDAuOTQ5LTIuNDQyIDMuMzk5IDMuMzk5IDAgMDAtLjk1LTIuNDU2IDMuMTMgMy4xMyAwIDAwLTQuNTEgMCAzLjM2MiAzLjM2MiAwIDAwLS45NSAyLjQ1NiAzLjM2IDMuMzYgMCAwMC45NiAyLjQ0MmguMDA5em0xOC42MzUgMS41ODVhNS44MzQgNS44MzQgMCAwMS04LjA0OSAwIDUuNDA0IDUuNDA0IDAgMDEtMS42NDEtNC4wMjcgNS4zOSA1LjM5IDAgMDExLjY0MS00LjAyNyA1LjgyNSA1LjgyNSAwIDAxOC4wNSAwIDUuNCA1LjQgMCAwMTEuNjQxIDQuMDI3IDUuNDE1IDUuNDE1IDAgMDEtMS42NDIgNC4wMjd6bS02LjI3Mi0xLjU4NWEzLjA5IDMuMDkgMCAwMDQuNDk2IDAgMy4zNzkgMy4zNzkgMCAwMC45NS0yLjQ0MiAzLjQwMyAzLjQwMyAwIDAwLS45NS0yLjQ1NiAzLjEzMyAzLjEzMyAwIDAwLTQuNTEyIDAgMy4zNzMgMy4zNzMgMCAwMC0uOTUgMi40NTYgMy4zOCAzLjM4IDAgMDAuOTU1IDIuNDQyaC4wMXptMTQuNCA4LjI1M2E0Ljk2NSA0Ljk2NSAwIDAxLTMuMTYxLTEuMDAyIDUuODM4IDUuODM4IDAgMDEtMS44NDUtMi4zMzVsMi4xNjctLjg5OGEzLjYxNCAzLjYxNCAwIDAwMS4wNzQgMS40MTEgMi42OTIgMi42OTIgMCAwMDEuNzc1LjU5OCAyLjg1IDIuODUgMCAwMDIuMjExLS44NTUgMy40NDYgMy40NDYgMCAwMC43OTItMi40NjJ2LS44MTRoLS4wODlhMy42MiAzLjYyIDAgMDEtMi45NjkgMS4yNzggNS4xNDggNS4xNDggMCAwMS0zLjc5OS0xLjY0OSA1LjQzOCA1LjQzOCAwIDAxLTEuNjI2LTMuOTY3IDUuNDg4IDUuNDg4IDAgMDExLjYyNi00LjAwMiA1LjE3MSA1LjE3MSAwIDAxMy44LTEuNjU4IDQuMTIgNC4xMiAwIDAxMS43NDYuMzljLjQ3LjIwNy44OTUuNTEzIDEuMjMxLjg5OGguMDl2LS44OThoMi4zNTR2MTAuMDlhNS44ODUgNS44ODUgMCAwMS0xLjUwOCA0LjQwOCA1LjMzNCA1LjMzNCAwIDAxLTMuODcgMS40Njd6bS4xNzQtNy4yNzVhMi43OCAyLjc4IDAgMDAyLjE0Ni0uOTQ3Yy41ODQtLjY3MS44OTUtMS41NC44NjItMi40MjhhMy41OSAzLjU5IDAgMDAtLjg2Mi0yLjQ3MyAyLjc1MyAyLjc1MyAwIDAwLTIuMTQ2LS45NDcgMi45NSAyLjk1IDAgMDAtMi4yMjYuOTQ3IDMuNDUgMy40NSAwIDAwLS45NSAyLjQ1MmMtLjAyMS45MDMuMzI2IDEuNzc4Ljk1IDIuNDI4LjU2OC42MjcgMS4zOC45NzkgMi4yMjYuOTY4em02LjkxIDEuODc1aDIuNDg0VjQuMjloLTIuNDgzdjE2LjU2em05LjI4LjM0YTUuNCA1LjQgMCAwMS0zLjk5Ni0xLjYxOCA1LjUwNiA1LjUwNiAwIDAxLTEuNjA5LTQuMDE3IDUuNTU2IDUuNTU2IDAgMDExLjU1LTQuMDU2IDUuMTE0IDUuMTE0IDAgMDEzLjc5OC0xLjU4NCA0LjgyNyA0LjgyNyAwIDAxMS45LjM3IDQuMzk0IDQuMzk0IDAgMDExLjQ1NC45NDdjLjM2MS4zNDYuNjc3LjczMS45NDkgMS4xNDUuMjI0LjM1NS40MS43MjYuNTYxIDEuMTFsLjI1Ny42NDctNy41OTkgMy4xMTVhMi44NDggMi44NDggMCAwMDIuNzM1IDEuNzA2IDMuMjU0IDMuMjU0IDAgMDAyLjc4NS0xLjU2NGwxLjkgMS4yNzhhNi4zNTcgNi4zNTcgMCAwMS0xLjgyNCAxLjcyNyA1LjE3NiA1LjE3NiAwIDAxLTIuODYxLjc5NXptLTMuMTctNS44MDdsNS4wNS0yLjA5MmExLjcxOSAxLjcxOSAwIDAwLS43ODYtLjg2NCAyLjU2NiAyLjU2NiAwIDAwLTEuMzAxLS4zM2MtLjc3My4wMTktMS41MS4zNC0yLjA0My44OThhMy4wMDEgMy4wMDEgMCAwMC0uOTIgMi4zODh6TTEyMi42MDcgMjEuMTkyYTcuODI0IDcuODI0IDAgMDEtNS42ODQtMi4zMDUgNy43OCA3Ljc4IDAgMDEtMi4yNzUtNS42ODQgNy44MiA3LjgyIDAgMDE3Ljk2LTcuOTg1IDcuMDk3IDcuMDk3IDAgMDE1LjYzNCAyLjUwMmwtMS4zNyAxLjMyM2E1LjE3NyA1LjE3NyAwIDAwLTQuMjU2LTEuOTQgNS44MTggNS44MTggMCAwMC00LjIyOCAxLjY4NyA1LjkwMSA1LjkwMSAwIDAwLTEuNzE3IDQuNDEzIDUuOTEgNS45MSAwIDAwMS43MTYgNC40MTYgNS44MTggNS44MTggMCAwMDQuMjMgMS42ODggNi4wNDcgNi4wNDcgMCAwMDQuNzQ4LTIuMjE2bDEuMzY3IDEuMzYyYTcuMzk3IDcuMzk3IDAgMDEtMi42MjkgMS45OTggOC4wNzYgOC4wNzYgMCAwMS0zLjQ5Ni43NE0xMzAuNDQyIDIwLjg1aDEuOTY0VjUuNTQ5aC0xLjk2NHpNMTM1LjYxOCAxMS42NDNhNS41MDcgNS41MDcgMCAwMTcuNzUxIDAgNS41MjMgNS41MjMgMCAwMTEuNTE5IDMuOTc3IDUuNTIgNS41MiAwIDAxLTEuNTE5IDMuOTc4IDUuNTA1IDUuNTA1IDAgMDEtNy43NTEgMCA1LjU0IDUuNTQgMCAwMS0xLjUxOS0zLjk3OCA1LjU0MyA1LjU0MyAwIDAxMS41MTktMy45Nzd6bTEuNDYzIDYuNzI2YTMuMzIxIDMuMzIxIDAgMDA0LjgxNCAwIDMuNzc3IDMuNzc3IDAgMDAxLjAyLTIuNzQ5IDMuNzc4IDMuNzc4IDAgMDAtMS4wMi0yLjc0OCAzLjMyMSAzLjMyMSAwIDAwLTQuODE0IDAgMy43ODQgMy43ODQgMCAwMC0xLjAxMiAyLjc0OCAzLjc2NyAzLjc2NyAwIDAwMS4wMjMgMi43NGwtLjAxLjAwOXpNMTU1Ljc4NiAyMC44NWgtMS45VjE5LjRoLS4wNTlhMy42NDIgMy42NDIgMCAwMS0xLjM4NiAxLjMwOCAzLjkxNSAzLjkxNSAwIDAxLTEuOTQ5LjUyNCAzLjcyIDMuNzIgMCAwMS0yLjk4Mi0xLjE5NCA0LjczOSA0LjczOSAwIDAxLTEuMDE0LTMuMjA0VjEwLjM5aDEuOTYzdjYuMDg0YzAgMS45NDkuODY2IDIuOTI2IDIuNTkzIDIuOTI2YTIuNDI5IDIuNDI5IDAgMDAxLjk5OS0uOTQ3Yy41MS0uNjQ3Ljc3Ni0xLjQ0Ni43NjYtMi4yNjVWMTAuMzloMS45N3YxMC40Nk0xNjIuNDU5IDIxLjE5MmE0LjYwOCA0LjYwOCAwIDAxLTMuNTEyLTEuNjA1IDUuNjc1IDUuNjc1IDAgMDEtMS40NzQtMy45NjcgNS42ODUgNS42ODUgMCAwMTEuNDc0LTMuOTcyIDQuNjI1IDQuNjI1IDAgMDEzLjUxMi0xLjU5OSA0LjQ2NCA0LjQ2NCAwIDAxMi4xNDguNTE0Yy41ODMuMjkgMS4wNzIuNzMgMS40MjQgMS4yNzdoLjA4NGwtLjA4NC0xLjQ1VjUuNTQ4SDE2OHYxNS4zMDNoLTEuOVYxOS40aC0uMDg4Yy0uMzUxLjU0Ny0uODQxLjk4Ni0xLjQyNSAxLjI3OWE0LjQzMyA0LjQzMyAwIDAxLTIuMTI4LjUxMnptLjMyNi0xLjc5MmEzLjE0NiAzLjE0NiAwIDAwMi4zNjUtMS4wMjEgMy44MzggMy44MzggMCAwMC45NS0yLjc1OSAzLjgzOCAzLjgzOCAwIDAwLS45NS0yLjc1NyAzLjIxNSAzLjIxNSAwIDAwLTQuNzQ5IDAgMy43NjYgMy43NjYgMCAwMC0uOTUgMi43NDcgMy43NyAzLjc3IDAgMDAuOTUgMi43NSAzLjEyNiAzLjEyNiAwIDAwMi4zODQgMS4wNHoiIGZpbGw9IiNGRkYiLz4KPC9zdmc+Cg=='}
]


def add_default_providers(apps, schema_editor):
    Provider = apps.get_model('modules', 'Provider')
    for provider in default_providers:
        Provider.objects.create(name=provider['name'], description=provider['description'],
                                logo_data_uri=provider['logo'])


class Migration(migrations.Migration):

    dependencies = [
        ('modules', '0001_initial'),
    ]

    operations = [
        migrations.RunPython(add_default_providers)
    ]
