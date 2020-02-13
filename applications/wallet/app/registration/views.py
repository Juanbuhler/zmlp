from datetime import timedelta

from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ObjectDoesNotExist, ValidationError
from django.core.mail import send_mail
from django.db import transaction
from django.http import Http404
from django.template.loader import render_to_string
from django.utils.timezone import now
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from registration.models import UserRegistrationToken

User = get_user_model()


class UserRegistrationView(APIView):
    """Allows anyone to sign up for a new account. The user is created and email
is sent with a link that will activate the account.

Example POST json body:

    {
        "email": "fake@gmail.com",
        "firstName": "Fakey",
        "lastName": "Fakerson",
        "password": "sjlhdffiuhdaifuh"
    }

Response Codes:

- 200 - User was registered and activation email was sent.
- 400 - Bad params in the request.
- 409 - Email address is already registered to an active user.
- 422 - The password given was not strong enough.

"""
    authentication_classes = []
    permission_classes = []

    def post(self, request, *args, **kwargs):
        try:
            email = request.data['email']
            first = request.data['first_name']
            last = request.data['last_name']
            password = request.data['password']
        except KeyError:
            msg = 'Request must contain email, firstName, lastName, and password'
            return Response(data={'detail': msg}, status=status.HTTP_400_BAD_REQUEST)

        try:
            validate_password(password)
        except ValidationError as e:
            return Response({'detail': 'Password not strong enough',
                             'errors': e.messages}, status=422)

        with transaction.atomic():
            # If a registration token already exists for the user delete it so a new on
            # can be issued.
            if UserRegistrationToken.objects.filter(user__username=email).exists():
                token = UserRegistrationToken.objects.get(user__username=email)
                user = token.user
                user.set_password(password)
                user.save()
                token.delete()

            # If the user exists and there is no registration token then the user was already
            # activated. Exit with generic success message to prevent phishing.
            elif User.objects.filter(username=email).exists():
                msg = 'A user with this email address already exists.'
                return Response(data={'detail': msg}, status=status.HTTP_409_CONFLICT)

            # If the user does not exist yet then create it.
            else:
                user = User.objects.create(username=email, email=email,
                                           first_name=first, last_name=last, is_active=False)
                user.set_password(password)

            # Issue a new registration token.
            token = UserRegistrationToken.objects.create(user=user)

        # Email the user a link to activate their account.
        subject = 'Welcome To ZVI - Please Activate Your Account.'
        html = render_to_string('registration/activation-email.html',
                                context={'hostname': settings.HOSTNAME,
                                         'token': token.token,
                                         'user_id': user.id})
        body = (f'Click this link to confirm your email address and activate your account.\n'
                f'https://{settings.HOSTNAME}/accounts/confirm?'
                f'token={token.token}&userId={user.id}')
        send_mail(subject=subject, message=body, html_message=html, fail_silently=False,
                  from_email='do_not_reply@zorroa.com', recipient_list=[user.username])

        return Response(data={'detail': 'Success, confirmation email has been sent.'})


class UserConfirmationView(APIView):
    """Activates a newly created account. Requires a user id and registration token that
are sent in an email to the user on registration.

Example POST json body:

    {
        "userId": 7,
        "token": "20938092384-30948-9384-304984390"
    }

Response Codes:

- 200 - User was activated successfully.
- 400 - Bad params in request body.
- 404 - The token/userId is incorrect or the user was never actually registered.

"""
    authentication_classes = []
    permission_classes = []

    def post(self, request, *args, **kwargs):
        try:
            token = request.data['token']
            user_id = request.data['user_id']
        except KeyError:
            msg = 'Confirming an email address requires sending the "token" and "userId" params.'
            return Response(data={'detail': msg}, status=status.HTTP_400_BAD_REQUEST)
        try:
            token = UserRegistrationToken.objects.get(token=token, user=user_id)
        except ObjectDoesNotExist:
            raise Http404('User ID and/or token does not exist.')
        if now() - token.created_at > timedelta(days=settings.REGISTRATION_TIMEOUT_DAYS):
            msg = 'The activation link has expired. Please sign up again.'
            return Response(data={'detail': msg}, status=status.HTTP_403_FORBIDDEN)
        user = token.user
        user.is_active = True
        with transaction.atomic():
            user.save()
            token.delete()
        return Response(data={'detail': 'Success. User has been activated.'})
