from django.db import migrations


def backfill_trust_baseline(apps, schema_editor):
    Profile = apps.get_model('accounts', 'Profile')
    Profile.objects.filter(trust_points__lt=10).update(trust_points=10)


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0010_rewards'),
    ]

    operations = [
        migrations.RunPython(backfill_trust_baseline, migrations.RunPython.noop),
    ]
