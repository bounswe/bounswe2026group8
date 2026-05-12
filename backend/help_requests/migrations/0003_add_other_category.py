from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('help_requests', '0002_add_image_urls_to_helprequest'),
    ]

    operations = [
        migrations.AlterField(
            model_name='helprequest',
            name='category',
            field=models.CharField(
                choices=[
                    ('MEDICAL', 'Medical'),
                    ('FOOD', 'Food'),
                    ('SHELTER', 'Shelter'),
                    ('TRANSPORT', 'Transport'),
                    ('OTHER', 'Other'),
                ],
                max_length=10,
            ),
        ),
        migrations.AlterField(
            model_name='helpoffer',
            name='category',
            field=models.CharField(
                choices=[
                    ('MEDICAL', 'Medical'),
                    ('FOOD', 'Food'),
                    ('SHELTER', 'Shelter'),
                    ('TRANSPORT', 'Transport'),
                    ('OTHER', 'Other'),
                ],
                max_length=10,
            ),
        ),
    ]
