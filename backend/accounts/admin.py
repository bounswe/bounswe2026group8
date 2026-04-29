from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from .models import Hub, User, Profile, StaffAuditLog


@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'phone_number', 'preferred_language', 'created_at')
    search_fields = ('user__email', 'user__full_name', 'phone_number')
    readonly_fields = ('created_at', 'updated_at')


@admin.register(Hub)
class HubAdmin(admin.ModelAdmin):
    list_display = ('name', 'slug', 'created_at')
    prepopulated_fields = {'slug': ('name',)}


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ('email', 'full_name', 'role', 'staff_role', 'hub', 'is_active', 'is_staff', 'created_at')
    list_filter = ('role', 'staff_role', 'hub', 'is_staff', 'is_active')
    search_fields = ('email', 'full_name')
    ordering = ('-created_at',)

    fieldsets = (
        (None, {'fields': ('email', 'password')}),
        ('Personal info', {'fields': ('full_name', 'role', 'hub', 'neighborhood_address', 'expertise_field')}),
        ('App permissions', {'fields': ('staff_role',)}),
        ('Permissions', {'fields': ('is_active', 'is_staff', 'is_superuser', 'groups', 'user_permissions')}),
        ('Dates', {'fields': ('created_at',)}),
    )
    readonly_fields = ('created_at',)

    add_fieldsets = (
        (None, {
            'classes': ('wide',),
            'fields': ('email', 'full_name', 'role', 'hub', 'password1', 'password2'),
        }),
    )


@admin.register(StaffAuditLog)
class StaffAuditLogAdmin(admin.ModelAdmin):
    list_display = ('created_at', 'actor', 'action', 'target_type', 'target_id', 'target_user')
    list_filter = ('action', 'target_type')
    search_fields = ('actor__email', 'target_user__email', 'target_id', 'reason')
    readonly_fields = (
        'actor', 'target_user', 'target_type', 'target_id', 'action',
        'previous_state', 'new_state', 'reason', 'created_at',
    )

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False
