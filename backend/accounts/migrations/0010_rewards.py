from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('accounts', '0009_alter_expertisecategory_id'),
    ]

    operations = [
        migrations.AddField(
            model_name='profile',
            name='completed_help_count',
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.AddField(
            model_name='profile',
            name='contribution_count',
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.AddField(
            model_name='profile',
            name='trust_points',
            field=models.PositiveIntegerField(default=10),
        ),
        migrations.CreateModel(
            name='RewardEvent',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('event_type', models.CharField(choices=[('HELP_COMMENT_CREATED', 'Help comment created'), ('HELP_OFFER_CREATED', 'Help offer created'), ('HELP_REQUEST_RESOLVED', 'Help request resolved')], max_length=32)),
                ('points', models.PositiveIntegerField()),
                ('source_key', models.CharField(max_length=120)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('user', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='reward_events', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'ordering': ['-created_at'],
            },
        ),
        migrations.AddConstraint(
            model_name='rewardevent',
            constraint=models.UniqueConstraint(fields=('user', 'source_key'), name='unique_reward_source_per_user'),
        ),
    ]
