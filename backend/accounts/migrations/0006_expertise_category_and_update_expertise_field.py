from django.db import migrations, models
import django.db.models.deletion


PREDEFINED_CATEGORIES = [
    ('First Aid', 'MEDICAL'),
    ('Doctor/Nurse', 'MEDICAL'),
    ('Paramedic', 'MEDICAL'),
    ('Psychologist', 'MEDICAL'),
    ('Pharmacist', 'MEDICAL'),
    ('Search & Rescue', 'SHELTER'),
    ('Civil Engineer', 'SHELTER'),
    ('Firefighter', 'SHELTER'),
    ('Driver', 'TRANSPORT'),
    ('Logistics', 'TRANSPORT'),
    ('Food Safety', 'FOOD'),
    ('Nutrition', 'FOOD'),
    ('General Volunteer', 'OTHER'),
]


def seed_expertise_categories(apps, schema_editor):
    ExpertiseCategory = apps.get_model('accounts', 'ExpertiseCategory')
    for name, help_request_category in PREDEFINED_CATEGORIES:
        ExpertiseCategory.objects.create(
            name=name,
            help_request_category=help_request_category,
            is_active=True,
        )


def clear_expertise_fields(apps, schema_editor):
    """Delete existing ExpertiseField rows before making category FK non-nullable."""
    ExpertiseField = apps.get_model('accounts', 'ExpertiseField')
    ExpertiseField.objects.all().delete()


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0005_change_cert_url_to_charfield'),
    ]

    operations = [
        # 1. Create ExpertiseCategory table
        migrations.CreateModel(
            name='ExpertiseCategory',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=100, unique=True)),
                ('help_request_category', models.CharField(
                    choices=[
                        ('MEDICAL', 'Medical'),
                        ('FOOD', 'Food'),
                        ('SHELTER', 'Shelter'),
                        ('TRANSPORT', 'Transport'),
                        ('OTHER', 'Other'),
                    ],
                    max_length=20,
                )),
                ('is_active', models.BooleanField(default=True)),
            ],
            options={
                'verbose_name_plural': 'expertise categories',
                'ordering': ['help_request_category', 'name'],
            },
        ),
        # 2. Seed the 13 predefined expertise categories
        migrations.RunPython(seed_expertise_categories, migrations.RunPython.noop),
        # 3. Add is_approved to ExpertiseField
        migrations.AddField(
            model_name='expertisefield',
            name='is_approved',
            field=models.BooleanField(default=True),
        ),
        # 4. Add nullable category FK (nullable so existing rows don't violate constraint)
        migrations.AddField(
            model_name='expertisefield',
            name='category',
            field=models.ForeignKey(
                null=True,
                blank=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name='expertise_fields',
                to='accounts.ExpertiseCategory',
            ),
        ),
        # 5. Clear stale ExpertiseField rows that can't map to the new FK
        migrations.RunPython(clear_expertise_fields, migrations.RunPython.noop),
        # 6. Make category non-nullable now that all rows are gone
        migrations.AlterField(
            model_name='expertisefield',
            name='category',
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.PROTECT,
                related_name='expertise_fields',
                to='accounts.ExpertiseCategory',
            ),
        ),
        # 7. Remove the old free-text field
        migrations.RemoveField(
            model_name='expertisefield',
            name='field',
        ),
    ]
