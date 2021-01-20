import json

from django.conf import settings
from django.db.models import Field
from django.db.models import JSONField as DjangoJSONField


class JSONField(DjangoJSONField):
    pass


if 'sqlite' in settings.DATABASES['default']['ENGINE']:
    # This is a very basic implementation of the JSONField for SQLite so that we
    # can continue to use the SQLite In Memory DB for testing. It doesn't allow for
    # queries or any of the more complicated field interactions.
    # Based on: https://medium.com/@philamersune/using-postgresql-jsonfield-in-sqlite-95ad4ad2e5f1
    class JSONField(Field):
        def db_type(self, connection):
            return 'text'

        def from_db_value(self, value, expression, connection):
            if value is not None:
                return self.to_python(value)
            return value

        def to_python(self, value):
            if value is not None:
                try:
                    return json.loads(value)
                except (TypeError, ValueError):
                    return value
            return value

        def get_prep_value(self, value):
            if value is not None:
                return str(json.dumps(value))
            return value

        def value_to_string(self, obj):
            return self.value_from_object(obj)
