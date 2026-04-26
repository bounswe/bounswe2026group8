from django.db import migrations, models


TRANSLATIONS = {
    'First Aid':         {'tr': 'İlk Yardım',          'es': 'Primeros Auxilios',    'zh': '急救'},
    'Doctor/Nurse':      {'tr': 'Doktor/Hemşire',       'es': 'Médico/Enfermero',     'zh': '医生/护士'},
    'Paramedic':         {'tr': 'Paramedik',            'es': 'Paramédico',           'zh': '急救医生'},
    'Psychologist':      {'tr': 'Psikolog',             'es': 'Psicólogo',            'zh': '心理学家'},
    'Pharmacist':        {'tr': 'Eczacı',               'es': 'Farmacéutico',         'zh': '药剂师'},
    'Search & Rescue':   {'tr': 'Arama & Kurtarma',     'es': 'Búsqueda y Rescate',  'zh': '搜救'},
    'Civil Engineer':    {'tr': 'İnşaat Mühendisi',     'es': 'Ingeniero Civil',      'zh': '土木工程师'},
    'Firefighter':       {'tr': 'İtfaiyeci',            'es': 'Bombero',              'zh': '消防员'},
    'Driver':            {'tr': 'Sürücü',               'es': 'Conductor',            'zh': '驾驶员'},
    'Logistics':         {'tr': 'Lojistik',             'es': 'Logística',            'zh': '物流'},
    'Food Safety':       {'tr': 'Gıda Güvenliği',       'es': 'Seguridad Alimentaria','zh': '食品安全'},
    'Nutrition':         {'tr': 'Beslenme',             'es': 'Nutrición',            'zh': '营养'},
    'General Volunteer': {'tr': 'Genel Gönüllü',        'es': 'Voluntario General',   'zh': '普通志愿者'},
}


def backfill_translations(apps, schema_editor):
    ExpertiseCategory = apps.get_model('accounts', 'ExpertiseCategory')
    for category in ExpertiseCategory.objects.all():
        category.translations = TRANSLATIONS.get(category.name, {})
        category.save(update_fields=['translations'])


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0007_remove_user_expertise_field'),
    ]

    operations = [
        migrations.AddField(
            model_name='expertisecategory',
            name='translations',
            field=models.JSONField(blank=True, default=dict),
        ),
        migrations.RunPython(backfill_translations, migrations.RunPython.noop),
    ]
