from django.db import migrations, models


SEEDED_TURKEY_CITIES = {
    'Istanbul', 'Ankara', 'Izmir', 'Bursa', 'Antalya',
    'Adana', 'Konya', 'Gaziantep', 'Mersin', 'Diyarbakir',
    'Kayseri', 'Eskisehir', 'Samsun', 'Trabzon', 'Denizli',
    'Malatya', 'Erzurum', 'Sanliurfa', 'Van', 'Kocaeli',
}


def backfill_hub_locations(apps, schema_editor):
    Hub = apps.get_model('accounts', 'Hub')
    for hub in Hub.objects.all():
        if hub.country or hub.city:
            continue
        if hub.name in SEEDED_TURKEY_CITIES:
            hub.country = 'Turkey'
            hub.city = hub.name
            hub.save(update_fields=['country', 'city'])
        else:
            # Best-effort: park unknown legacy hubs under an "Unknown" country
            # keyed by the existing name to keep the upcoming unique constraint
            # on (country, city, district) satisfiable.
            hub.country = 'Unknown'
            hub.city = hub.name
            hub.save(update_fields=['country', 'city'])


def noop_reverse(apps, schema_editor):
    # Reversing the data migration is safe — the columns get dropped after.
    pass


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0016_merge_20260510_2250'),
    ]

    operations = [
        migrations.AlterField(
            model_name='hub',
            name='name',
            field=models.CharField(max_length=200, unique=True),
        ),
        migrations.AlterField(
            model_name='hub',
            name='slug',
            field=models.SlugField(max_length=200, unique=True),
        ),
        migrations.AddField(
            model_name='hub',
            name='country',
            field=models.CharField(blank=True, default='', max_length=100),
        ),
        migrations.AddField(
            model_name='hub',
            name='city',
            field=models.CharField(blank=True, default='', max_length=120),
        ),
        migrations.AddField(
            model_name='hub',
            name='district',
            field=models.CharField(blank=True, default='', max_length=120),
        ),
        migrations.RunPython(backfill_hub_locations, noop_reverse),
        migrations.AlterModelOptions(
            name='hub',
            options={'ordering': ['country', 'city', 'district']},
        ),
        migrations.AddConstraint(
            model_name='hub',
            constraint=models.UniqueConstraint(
                fields=('country', 'city', 'district'),
                name='unique_hub_location',
            ),
        ),
    ]
