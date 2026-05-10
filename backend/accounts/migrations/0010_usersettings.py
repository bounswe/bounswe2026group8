from django.db import migrations, models
import django.db.models.deletion


def create_user_settings(apps, schema_editor):
    User = apps.get_model('accounts', 'User')
    UserSettings = apps.get_model('accounts', 'UserSettings')
    for user in User.objects.all():
        UserSettings.objects.get_or_create(user=user)


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0009_alter_expertisecategory_id'),
    ]

    operations = [
        migrations.CreateModel(
            name='UserSettings',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('notify_help_requests', models.BooleanField(default=True)),
                ('notify_urgent_posts', models.BooleanField(default=True)),
                ('notify_expertise_matches_only', models.BooleanField(default=False, help_text='If true, expert fallback notifications are skipped unless expertise matches.')),
                ('show_phone_number', models.BooleanField(default=False)),
                ('show_emergency_contact', models.BooleanField(default=False)),
                ('show_medical_info', models.BooleanField(default=False)),
                ('show_availability_status', models.BooleanField(default=True)),
                ('show_bio', models.BooleanField(default=True)),
                ('show_location', models.BooleanField(default=True)),
                ('show_resources', models.BooleanField(default=True)),
                ('show_expertise', models.BooleanField(default=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('updated_at', models.DateTimeField(auto_now=True)),
                ('user', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name='settings', to='accounts.user')),
            ],
            options={
                'verbose_name': 'User settings',
                'verbose_name_plural': 'User settings',
            },
        ),
        migrations.RunPython(create_user_settings, migrations.RunPython.noop),
    ]
