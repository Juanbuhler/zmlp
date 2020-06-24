from django.contrib import admin
from django.contrib.admin import AdminSite, ModelAdmin
from django.contrib.auth import get_user_model
from django.forms import ModelForm

from modules.models import Provider
from projects.admin import ProjectAdmin
from projects.models import Project, Membership
from subscriptions.models import Subscription

User = get_user_model()


class NoDeleteMixin():
    def get_actions(self, request):
        actions = super().get_actions(request)
        if 'delete_selected' in actions:
            del actions['delete_selected']
        return actions


class MembershipInline(admin.TabularInline):
    model = Membership
    extra = 0


class AlwaysChangedModelForm(ModelForm):
    def has_changed(self):
        return True


class SubscriptionInline(admin.StackedInline):
    model = Subscription
    form = AlwaysChangedModelForm


class SupportUserAdmin(NoDeleteMixin, ModelAdmin):
    fieldsets = [
        (None, {'fields': ('email', 'first_name', 'last_name',
                           'is_active', 'is_staff')})
    ]
    list_display = ('email', 'first_name', 'last_name', 'is_active', 'last_login', 'date_joined')
    search_fields = ('email', 'first_name', 'last_name')
    readonly_fields = ['email', 'username']
    list_filter = ('is_active',)
    exclude = ('permissions',)
    inlines = [MembershipInline]

    def save_related(self, request, form, formsets, change):
        user = form.instance
        # client = project.get_zmlp_super_client()

        # If any of the Memberships are going to be deleted remove their API keys.
        if change:
            membership_formset = formsets[0]
            for membership_form in membership_formset:
                if membership_form.cleaned_data['DELETE']:
                    membership = membership_form.instance
                    client = membership.project.get_zmlp_super_client()
                    membership.destroy_zmlp_api_key(client)

        # Save the changes.
        super(SupportUserAdmin, self).save_related(request, form, formsets, change)

        # Sync all of the Project's Memberships with ZMLP.
        for membership in user.memberships.all():
            client = membership.project.get_zmlp_super_client()
            membership.sync_with_zmlp(client)


class SupportProjectAdmin(NoDeleteMixin, ProjectAdmin):
    readonly_fields = ['id']
    inlines = [SubscriptionInline, MembershipInline]

    def save_related(self, request, form, formsets, change):
        project = form.instance
        client = project.get_zmlp_super_client()

        # If any of the Memberships are going to be deleted remove their API keys.
        if change:
            membership_formset = formsets[1]
            for membership_form in membership_formset:
                if membership_form.cleaned_data['DELETE']:
                    membership = membership_form.instance
                    membership.destroy_zmlp_api_key(client)

        # Save the changes.
        super(SupportProjectAdmin, self).save_related(request, form, formsets, change)

        # Sync all of the Project's Memberships with ZMLP.
        for membership in project.membership_set.all():
            membership.sync_with_zmlp(client)


class SupportAdminSite(AdminSite):
    """Stripped down admin site that is safe for the support team to use."""
    site_header = "ZVI Administration Console"
    site_title = "ZVI Administration Console"
    index_title = "Welcome to the ZVI Administration Console."


support_admin_site = SupportAdminSite(name='support_admin')
support_admin_site.register(Project, SupportProjectAdmin)
support_admin_site.register(get_user_model(), SupportUserAdmin)
support_admin_site.register(Provider)
