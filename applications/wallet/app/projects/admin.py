from django.contrib import admin
from django.contrib.admin import ModelAdmin

from projects.models import Project, Membership
from wallet.utils import get_zmlp_superuser_client


def sync_project_with_zmlp(modeladmin, request, queryset):
    """Admin action that syncs a Wallet project with ZMLP."""
    for project in queryset:
        client = get_zmlp_superuser_client(project_id=str(project.id))
        project.sync_with_zmlp()
        for membership in project.membership_set.all():
            membership.sync_with_zmlp(client)


@admin.register(Project)
class ProjectAdmin(ModelAdmin):
    actions = [sync_project_with_zmlp]
    list_display = ('name', 'id', 'tier', 'usage', 'is_active')
    list_filter = ('is_active',)
    search_fields = ('name', 'id')

    def tier(self, project):
        if project.subscription:
            return project.subscription.tier
        return None

    def usage(self, project):
        if project.subscription:
            usage = project.subscription.usage()
            return f'{usage["video_hours"]} video hours/{usage["image_count"]} images'
        return None

    def save_model(self, request, obj, form, change):
        """Creates a new project in the database as well as ZMLP."""
        obj.save()
        obj.sync_with_zmlp()


@admin.register(Membership)
class MembershipAdmin(ModelAdmin):
    list_display = ('user', 'project', 'roles')
    list_display_links = ('user', 'project')
    list_filter = ('project',)
    search_fields = ('user__email', 'project__name')

    def save_model(self, request, obj, form, change):
        """When adding a membership if no api key is given then a new one is created."""
        obj.sync_with_zmlp(self.project.get_zmlp_super_client())
        obj.save()

    def delete_model(self, request, obj):
        """When deleting a Membership the ZMLP API key associated with it is deleted too."""
        client = get_zmlp_superuser_client(project_id=str(obj.project.id))
        obj.delete_and_sync_with_zmlp(client)
