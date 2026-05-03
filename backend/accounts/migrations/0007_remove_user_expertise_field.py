from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0006_expertise_category_and_update_expertise_field'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='user',
            name='expertise_field',
        ),
    ]
