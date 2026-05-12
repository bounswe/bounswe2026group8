from decimal import Decimal

from django.core.management.base import BaseCommand
from django.db import transaction
from django.db.models import Count
from django.utils import timezone
from django.utils.text import slugify

from accounts.models import (
    ExpertiseCategory,
    ExpertiseField,
    Hub,
    Profile,
    Resource,
    User,
)
from forum.models import Comment, Post, Vote
from help_requests.models import HelpComment, HelpOffer, HelpRequest


PASSWORD = 'password123'

COUNTRY_TURKEY = 'Turkey'
CITY_ISTANBUL = '\u0130stanbul'
CITY_IZMIR = '\u0130zmir'
CITY_ANKARA = 'Ankara'

HUB_DEFINITIONS = [
    (CITY_ISTANBUL, 'istanbul', [('sariyer', 'Sar\u0131yer')]),
    (CITY_IZMIR, 'izmir', [('konak', 'Konak')]),
    (CITY_ANKARA, 'ankara', [('cankaya', '\u00c7ankaya')]),
]

HUBS = [
    {
        'key': f'{city_key}-{district_key}',
        'country': COUNTRY_TURKEY,
        'city': city,
        'district': district,
        'name': f'{city} / {district}, {COUNTRY_TURKEY}',
    }
    for city, city_key, districts in HUB_DEFINITIONS
    for district_key, district in districts
]

EXPERTISE_CATEGORIES = [
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

USERS = [
    {
        'email': 'admin@example.com',
        'full_name': 'Local App Admin',
        'role': User.Role.STANDARD,
        'staff_role': User.StaffRole.ADMIN,
        'is_staff': True,
        'is_superuser': True,
        'hub': 'istanbul-sariyer',
        'neighborhood_address': 'Platform operations',
        'profile': {
            'phone_number': '+905551000000',
            'bio': 'Seeded local administrator for moderation and staff workflows.',
            'availability_status': Profile.AvailabilityStatus.SAFE,
            'preferred_language': 'English',
        },
    },
    {
        'email': 'standard1@example.com',
        'full_name': 'John Standard',
        'role': User.Role.STANDARD,
        'hub': 'istanbul-sariyer',
        'neighborhood_address': 'Sar\u0131yer, \u0130stanbul',
        'profile': {
            'phone_number': '+905551000001',
            'bio': 'Community member interested in earthquake preparedness.',
            'availability_status': Profile.AvailabilityStatus.SAFE,
            'preferred_language': 'English',
        },
    },
    {
        'email': 'standard2@example.com',
        'full_name': 'Jane Citizen',
        'role': User.Role.STANDARD,
        'hub': 'izmir-konak',
        'neighborhood_address': 'Konak, \u0130zmir',
        'profile': {
            'phone_number': '+905551000002',
            'bio': 'Organizes neighborhood check-ins and supply lists.',
            'availability_status': Profile.AvailabilityStatus.NEEDS_HELP,
            'preferred_language': 'English',
        },
    },
    {
        'email': 'standard3@example.com',
        'full_name': 'Ali Neighbor',
        'role': User.Role.STANDARD,
        'hub': 'ankara-cankaya',
        'neighborhood_address': '\u00c7ankaya, Ankara',
        'profile': {
            'phone_number': '+905551000003',
            'bio': 'Keeps a shared list of local shelters and pharmacies.',
            'availability_status': Profile.AvailabilityStatus.SAFE,
            'preferred_language': 'Turkish',
        },
    },
    {
        'email': 'expert1@example.com',
        'full_name': 'Dr. Sarah Expert',
        'role': User.Role.EXPERT,
        'hub': 'istanbul-sariyer',
        'neighborhood_address': 'Sar\u0131yer, \u0130stanbul',
        'profile': {
            'phone_number': '+905551000004',
            'bio': 'Emergency medicine doctor with field response experience.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'English',
        },
        'expertise': ['First Aid', 'Doctor/Nurse'],
        'resources': [
            {'name': 'First Aid Kit', 'category': 'Medical', 'quantity': 2, 'condition': True},
            {'name': 'Portable AED', 'category': 'Medical', 'quantity': 1, 'condition': True},
        ],
    },
    {
        'email': 'expert2@example.com',
        'full_name': 'Mike Rescue',
        'role': User.Role.EXPERT,
        'hub': 'izmir-konak',
        'neighborhood_address': 'Konak, \u0130zmir',
        'profile': {
            'phone_number': '+905551000005',
            'bio': 'Former firefighter trained in search and rescue.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'English',
        },
        'expertise': ['Search & Rescue', 'Firefighter'],
        'resources': [
            {'name': 'Rope Rescue Kit', 'category': 'Shelter', 'quantity': 1, 'condition': True},
            {'name': 'Thermal Blanket Set', 'category': 'Shelter', 'quantity': 10, 'condition': True},
        ],
    },
    {
        'email': 'expert3@example.com',
        'full_name': 'Lisa Logistics',
        'role': User.Role.EXPERT,
        'hub': 'ankara-cankaya',
        'neighborhood_address': '\u00c7ankaya, Ankara',
        'profile': {
            'phone_number': '+905551000006',
            'bio': 'Supply chain coordinator for emergency resource distribution.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'English',
        },
        'expertise': ['Driver', 'Logistics', 'General Volunteer'],
        'resources': [
            {'name': 'Minivan', 'category': 'Transport', 'quantity': 1, 'condition': True},
            {'name': 'Handheld Radios', 'category': 'Communication', 'quantity': 4, 'condition': True},
        ],
    },
    {
        'email': 'expert4@example.com',
        'full_name': 'Nora Nutrition',
        'role': User.Role.EXPERT,
        'hub': 'istanbul-sariyer',
        'neighborhood_address': 'Sar\u0131yer, \u0130stanbul',
        'profile': {
            'phone_number': '+905551000007',
            'bio': 'Dietitian helping families plan shelf-stable emergency food.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'English',
        },
        'expertise': ['Food Safety', 'Nutrition'],
        'resources': [
            {'name': 'Food Safety Thermometers', 'category': 'Food', 'quantity': 5, 'condition': True},
        ],
    },
    {
        'email': 'standard4@example.com',
        'full_name': 'Derya Sariyer',
        'role': User.Role.STANDARD,
        'hub': 'istanbul-sariyer',
        'neighborhood_address': 'Sar\u0131yer, \u0130stanbul',
        'profile': {
            'phone_number': '+905551000008',
            'bio': 'Coordinates apartment emergency contact trees.',
            'availability_status': Profile.AvailabilityStatus.SAFE,
            'preferred_language': 'Turkish',
        },
    },
    {
        'email': 'standard5@example.com',
        'full_name': 'Ece Konak',
        'role': User.Role.STANDARD,
        'hub': 'izmir-konak',
        'neighborhood_address': 'Konak, \u0130zmir',
        'profile': {
            'phone_number': '+905551000009',
            'bio': 'Tracks open pharmacies and accessible routes.',
            'availability_status': Profile.AvailabilityStatus.SAFE,
            'preferred_language': 'Turkish',
        },
    },
    {
        'email': 'standard6@example.com',
        'full_name': 'Mert Konak',
        'role': User.Role.STANDARD,
        'hub': 'izmir-konak',
        'neighborhood_address': 'Konak, \u0130zmir',
        'profile': {
            'phone_number': '+905551000010',
            'bio': 'Volunteer for supply pickup and neighborhood checks.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'Turkish',
        },
    },
    {
        'email': 'expert5@example.com',
        'full_name': 'Ayse Food Safety',
        'role': User.Role.EXPERT,
        'hub': 'izmir-konak',
        'neighborhood_address': 'Konak, \u0130zmir',
        'profile': {
            'phone_number': '+905551000011',
            'bio': 'Food safety specialist for emergency kitchens.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'Turkish',
        },
        'expertise': ['Food Safety', 'Nutrition'],
        'resources': [
            {'name': 'Emergency Food Checklist', 'category': 'Food', 'quantity': 20, 'condition': True},
        ],
    },
    {
        'email': 'standard7@example.com',
        'full_name': 'Selin Cankaya',
        'role': User.Role.STANDARD,
        'hub': 'ankara-cankaya',
        'neighborhood_address': '\u00c7ankaya, Ankara',
        'profile': {
            'phone_number': '+905551000012',
            'bio': 'Maintains a list of elderly neighbors who may need check-ins.',
            'availability_status': Profile.AvailabilityStatus.SAFE,
            'preferred_language': 'Turkish',
        },
    },
    {
        'email': 'standard8@example.com',
        'full_name': 'Burak Cankaya',
        'role': User.Role.STANDARD,
        'hub': 'ankara-cankaya',
        'neighborhood_address': '\u00c7ankaya, Ankara',
        'profile': {
            'phone_number': '+905551000013',
            'bio': 'Can help with transport and charging stations.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'Turkish',
        },
    },
    {
        'email': 'expert6@example.com',
        'full_name': 'Emre Paramedic',
        'role': User.Role.EXPERT,
        'hub': 'ankara-cankaya',
        'neighborhood_address': '\u00c7ankaya, Ankara',
        'profile': {
            'phone_number': '+905551000014',
            'bio': 'Paramedic available for triage and first aid guidance.',
            'availability_status': Profile.AvailabilityStatus.AVAILABLE_TO_HELP,
            'preferred_language': 'Turkish',
        },
        'expertise': ['First Aid', 'Paramedic'],
        'resources': [
            {'name': 'Triage Kit', 'category': 'Medical', 'quantity': 1, 'condition': True},
        ],
    },
]


class Command(BaseCommand):
    help = 'Populate the database with idempotent sample data for local development.'

    def handle(self, *args, **options):
        self.stdout.write('Populating sample data...')

        with transaction.atomic():
            self.remove_other_hubs()
            hubs = self.ensure_hubs()
            categories = self.ensure_expertise_categories()
            users = self.ensure_users(hubs, categories)
            posts = self.ensure_posts(hubs, users)
            help_requests = self.ensure_help_requests(hubs, users)
            self.ensure_help_offers(hubs, users)
            self.ensure_forum_comments(posts, users)
            self.ensure_help_comments(help_requests, users)
            self.ensure_votes(posts, users)
            self.sync_counts()

        self.stdout.write(self.style.SUCCESS('Sample data populated successfully.'))
        self.stdout.write('Sample login: admin@example.com / password123')
        self.stdout.write('Sample login: standard1@example.com / password123')
        self.stdout.write('Sample login: expert1@example.com / password123')

    def remove_other_hubs(self):
        allowed = {(data['country'], data['city'], data['district']) for data in HUBS}
        removed = 0
        for hub in Hub.objects.all():
            if (hub.country, hub.city, hub.district) in allowed:
                continue
            hub.delete()
            removed += 1
        if removed:
            self.stdout.write(f'Removed {removed} hubs outside the sample set.')

    def ensure_hubs(self):
        hubs = {}
        for data in HUBS:
            hub = self.find_or_create_hub(data)
            hubs[data['key']] = hub
        self.stdout.write(f'Ensured {len(hubs)} hubs.')
        return hubs

    def find_or_create_hub(self, data):
        hub = Hub.objects.filter(country=data['country'], city=data['city'], district=data['district']).first()
        if hub:
            changed = False
            if hub.name != data['name']:
                hub.name = data['name']
                changed = True
            if not hub.slug:
                hub.slug = self.unique_slug(data['name'])
                changed = True
            if changed:
                hub.save(update_fields=['name', 'slug'])
            return hub

        legacy = Hub.objects.filter(name=data['name']).first()
        if legacy:
            legacy.country = data['country']
            legacy.city = data['city']
            legacy.district = data['district']
            if not legacy.slug:
                legacy.slug = self.unique_slug(data['name'], exclude_pk=legacy.pk)
            legacy.save(update_fields=['country', 'city', 'district', 'slug'])
            return legacy

        return Hub.objects.create(
            name=data['name'],
            slug=self.unique_slug(data['name']),
            country=data['country'],
            city=data['city'],
            district=data['district'],
        )

    def unique_slug(self, value, exclude_pk=None):
        base = slugify(value) or 'hub'
        slug = base
        suffix = 2
        qs = Hub.objects.all()
        if exclude_pk is not None:
            qs = qs.exclude(pk=exclude_pk)
        while qs.filter(slug=slug).exists():
            slug = f'{base}-{suffix}'
            suffix += 1
        return slug

    def ensure_expertise_categories(self):
        categories = {}
        for name, help_request_category in EXPERTISE_CATEGORIES:
            category, _ = ExpertiseCategory.objects.update_or_create(
                name=name,
                defaults={
                    'help_request_category': help_request_category,
                    'is_active': True,
                },
            )
            categories[name] = category
        self.stdout.write(f'Ensured {len(categories)} expertise categories.')
        return categories

    def ensure_users(self, hubs, categories):
        users = {}
        for data in USERS:
            user, created = User.objects.update_or_create(
                email=data['email'],
                defaults={
                    'full_name': data['full_name'],
                    'role': data['role'],
                    'staff_role': data.get('staff_role', User.StaffRole.NONE),
                    'hub': hubs[data['hub']],
                    'neighborhood_address': data['neighborhood_address'],
                    'is_active': True,
                    'is_staff': data.get('is_staff', False),
                    'is_superuser': data.get('is_superuser', False),
                },
            )
            user.set_password(PASSWORD)
            user.save(update_fields=['password'])

            profile = self.ensure_profile(user)
            self.update_profile(profile, data['profile'])

            if user.role == User.Role.EXPERT:
                self.ensure_expertise(user, data.get('expertise', []), categories)
                self.ensure_resources(user, data.get('resources', []))

            users[data['email']] = user
            if created:
                self.stdout.write(f'Created user {user.email}.')

        self.stdout.write(f'Ensured {len(users)} users.')
        return users

    def ensure_profile(self, user):
        profile, _ = Profile.objects.get_or_create(user=user)
        return profile

    def update_profile(self, profile, data):
        for field, value in data.items():
            setattr(profile, field, value)
        profile.save(update_fields=list(data.keys()))

    def ensure_expertise(self, user, expertise_names, categories):
        for name in expertise_names:
            category = categories[name]
            ExpertiseField.objects.update_or_create(
                user=user,
                category=category,
                defaults={
                    'is_approved': True,
                    'certification_level': ExpertiseField.CertificationLevel.ADVANCED,
                    'certification_document_url': f'https://example.com/certificates/{slugify(name)}.pdf',
                    'verification_status': ExpertiseField.VerificationStatus.APPROVED,
                    'reviewed_at': timezone.now(),
                    'verification_note': 'Seeded sample expertise.',
                },
            )

    def ensure_resources(self, user, resources):
        for data in resources:
            Resource.objects.update_or_create(
                user=user,
                name=data['name'],
                defaults={
                    'category': data['category'],
                    'quantity': data['quantity'],
                    'condition': data['condition'],
                },
            )

    def ensure_posts(self, hubs, users):
        post_data = [
            {
                'title': 'Earthquake Safety Tips',
                'content': 'During an earthquake: drop, cover, and hold on. Stay away from windows and heavy objects until the shaking stops.',
                'forum_type': Post.ForumType.GLOBAL,
                'hub': None,
                'author': 'expert1@example.com',
            },
            {
                'title': 'Emergency Kit Essentials',
                'content': 'Keep water, non-perishable food, medications, first aid supplies, a flashlight, radio, batteries, and copies of important documents.',
                'forum_type': Post.ForumType.GLOBAL,
                'hub': None,
                'author': 'expert3@example.com',
            },
            {
                'title': 'Basic First Aid Knowledge',
                'content': 'Learn CPR, bleeding control, burn care, and how to recognize shock. Keep your kit somewhere easy to reach.',
                'forum_type': Post.ForumType.GLOBAL,
                'hub': None,
                'author': 'expert1@example.com',
            },
            {
                'title': 'URGENT: Power Outage in Sar\u0131yer, \u0130stanbul',
                'content': 'Power has been out for several hours around Sariyer. Please check on neighbors who rely on medical equipment.',
                'forum_type': Post.ForumType.URGENT,
                'hub': hubs['istanbul-sariyer'],
                'author': 'standard1@example.com',
            },
            {
                'title': 'URGENT: Flash Flood Warning in Konak, \u0130zmir',
                'content': 'Several low roads around Konak are flooded. Avoid driving through standing water and share safe routes here.',
                'forum_type': Post.ForumType.URGENT,
                'hub': hubs['izmir-konak'],
                'author': 'standard2@example.com',
            },
            {
                'title': 'URGENT: Gas Leak Reported in \u00c7ankaya, Ankara',
                'content': 'A residential block near Cankaya is evacuating due to a reported gas leak. Temporary transport and shelter may be needed.',
                'forum_type': Post.ForumType.URGENT,
                'hub': hubs['ankara-cankaya'],
                'author': 'standard3@example.com',
            },
        ]

        for hub in hubs.values():
            post_data.extend(
                [
                    {
                        'title': f'Emergency Preparedness Meeting in {hub.name}',
                        'content': f'We are planning a community preparedness meeting in {hub.name} to review kits, family plans, and neighbor check-ins.',
                        'forum_type': Post.ForumType.STANDARD,
                        'hub': hub,
                        'author': 'expert3@example.com',
                    },
                    {
                        'title': f'First Aid Training Session in {hub.name}',
                        'content': f'A free first aid and CPR practice session is planned for this weekend in {hub.name}.',
                        'forum_type': Post.ForumType.STANDARD,
                        'hub': hub,
                        'author': 'expert1@example.com',
                    },
                    {
                        'title': f'Community Supply Map for {hub.name}',
                        'content': f'Use this thread to share open pharmacies, water points, public shelters, and charging locations around {hub.name}.',
                        'forum_type': Post.ForumType.STANDARD,
                        'hub': hub,
                        'author': 'standard1@example.com',
                    },
                ]
            )

        posts = []
        for data in post_data:
            posts.append(
                self.upsert_post(
                    title=data['title'],
                    defaults={
                        'content': data['content'],
                        'forum_type': data['forum_type'],
                        'hub': data['hub'],
                        'author': users[data['author']],
                        'status': Post.Status.ACTIVE,
                        'image_urls': [],
                    },
                )
            )

        self.stdout.write(f'Ensured {len(posts)} forum posts.')
        return posts

    def upsert_post(self, title, defaults):
        post = Post.objects.filter(title=title, reposted_from__isnull=True).first()
        if post is None:
            return Post.objects.create(title=title, **defaults)

        for field, value in defaults.items():
            setattr(post, field, value)
        post.save(update_fields=list(defaults.keys()))
        return post

    def ensure_help_requests(self, hubs, users):
        request_data = [
            {
                'title': 'Need Medical Assistance for Elderly Neighbor in Sariyer',
                'description': 'My elderly neighbor hurt her ankle and needs help getting checked by a doctor.',
                'category': 'MEDICAL',
                'urgency': HelpRequest.Urgency.HIGH,
                'hub': hubs['istanbul-sariyer'],
                'author': 'standard1@example.com',
                'assigned_expert': 'expert1@example.com',
                'location_text': 'Sar\u0131yer, \u0130stanbul',
                'latitude': Decimal('41.043800'),
                'longitude': Decimal('29.009400'),
            },
            {
                'title': 'Looking for Non-Perishable Food Donations in Sariyer',
                'description': 'A family nearby needs canned goods, pasta, and baby food for the next few days.',
                'category': 'FOOD',
                'urgency': HelpRequest.Urgency.MEDIUM,
                'hub': hubs['istanbul-sariyer'],
                'author': 'standard1@example.com',
                'assigned_expert': 'expert4@example.com',
                'location_text': 'Sar\u0131yer, \u0130stanbul',
                'latitude': Decimal('40.991900'),
                'longitude': Decimal('29.025300'),
            },
            {
                'title': 'Temporary Shelter Needed in Konak',
                'description': 'A small family needs short-term accommodation after water damage at home.',
                'category': 'SHELTER',
                'urgency': HelpRequest.Urgency.HIGH,
                'hub': hubs['izmir-konak'],
                'author': 'standard2@example.com',
                'assigned_expert': 'expert2@example.com',
                'location_text': 'Konak, \u0130zmir',
                'latitude': Decimal('38.418900'),
                'longitude': Decimal('27.128700'),
            },
            {
                'title': 'Transportation to Hospital in Ankara',
                'description': 'Need a ride to a hospital appointment tomorrow morning.',
                'category': 'TRANSPORT',
                'urgency': HelpRequest.Urgency.MEDIUM,
                'hub': hubs['ankara-cankaya'],
                'author': 'standard3@example.com',
                'assigned_expert': 'expert3@example.com',
                'location_text': '\u00c7ankaya, Ankara',
                'latitude': Decimal('39.917900'),
                'longitude': Decimal('32.862700'),
            },
            {
                'title': 'Water Damage Cleanup Help in Konak',
                'description': 'Basement cleanup after flooding. Heavy lifting help would be appreciated.',
                'category': 'SHELTER',
                'urgency': HelpRequest.Urgency.MEDIUM,
                'hub': hubs['izmir-konak'],
                'author': 'standard2@example.com',
                'assigned_expert': None,
                'location_text': 'Konak, \u0130zmir',
                'latitude': Decimal('38.462200'),
                'longitude': Decimal('27.221400'),
            },
            {
                'title': 'Medication Pickup Needed in Ankara',
                'description': 'Could someone pick up prescription medicine from an open pharmacy?',
                'category': 'MEDICAL',
                'urgency': HelpRequest.Urgency.HIGH,
                'hub': hubs['ankara-cankaya'],
                'author': 'standard3@example.com',
                'assigned_expert': None,
                'location_text': 'Kizilay, Ankara',
                'latitude': Decimal('39.920800'),
                'longitude': Decimal('32.854100'),
            },
        ]

        requests = []
        for data in request_data:
            assigned_expert = users[data['assigned_expert']] if data['assigned_expert'] else None
            status = (
                HelpRequest.Status.EXPERT_RESPONDING
                if assigned_expert
                else HelpRequest.Status.OPEN
            )
            defaults = {
                'description': data['description'],
                'category': data['category'],
                'urgency': data['urgency'],
                'hub': data['hub'],
                'author': users[data['author']],
                'assigned_expert': assigned_expert,
                'assigned_at': timezone.now() if assigned_expert else None,
                'status': status,
                'location_text': data['location_text'],
                'latitude': data['latitude'],
                'longitude': data['longitude'],
                'image_urls': [],
            }
            request = self.upsert_help_request(data['title'], defaults)
            requests.append(request)

        self.stdout.write(f'Ensured {len(requests)} help requests.')
        return requests

    def upsert_help_request(self, title, defaults):
        help_request = HelpRequest.objects.filter(title=title).first()
        if help_request is None:
            return HelpRequest.objects.create(title=title, **defaults)

        if help_request.status == HelpRequest.Status.RESOLVED:
            defaults.pop('status', None)
            defaults.pop('assigned_expert', None)
            defaults.pop('assigned_at', None)
        for field, value in defaults.items():
            setattr(help_request, field, value)
        help_request.save(update_fields=list(defaults.keys()))
        return help_request

    def ensure_help_offers(self, hubs, users):
        offer_data = [
            ('Sariyer First Aid Support', 'MEDICAL', 'expert1@example.com', hubs['istanbul-sariyer'], 'Weekends and evenings'),
            ('Sariyer Emergency Food Planning', 'FOOD', 'expert4@example.com', hubs['istanbul-sariyer'], 'Weekdays 10:00-18:00'),
            ('Konak Search and Rescue Help', 'SHELTER', 'expert2@example.com', hubs['izmir-konak'], 'Available on short notice'),
            ('Konak Temporary Shelter Check', 'SHELTER', 'expert2@example.com', hubs['izmir-konak'], 'Evenings and weekends'),
            ('Cankaya Transport Coordination', 'TRANSPORT', 'expert3@example.com', hubs['ankara-cankaya'], 'Weekdays after 17:00'),
            ('Cankaya Logistics Planning', 'OTHER', 'expert3@example.com', hubs['ankara-cankaya'], 'Anytime for urgent coordination'),
        ]

        for skill, category, author_email, hub, availability in offer_data:
            HelpOffer.objects.update_or_create(
                skill_or_resource=skill,
                author=users[author_email],
                defaults={
                    'category': category,
                    'hub': hub,
                    'description': f'{skill} available for neighbors in {hub.name}.',
                    'availability': availability,
                },
            )

        self.stdout.write(f'Ensured {len(offer_data)} help offers.')

    def ensure_forum_comments(self, posts, users):
        commenters = list(users.values())
        for index, post in enumerate(posts):
            for offset in range(2 if post.forum_type == Post.ForumType.URGENT else 1):
                author = commenters[(index + offset + 1) % len(commenters)]
                content = self.comment_text(post, offset)
                Comment.objects.get_or_create(post=post, author=author, content=content)

    def comment_text(self, post, offset):
        if post.forum_type == Post.ForumType.URGENT:
            messages = [
                'I can help coordinate nearby support. Please share any confirmed updates.',
                'I am checking with neighbors now and will report safe routes here.',
            ]
            return messages[offset]
        return 'Thanks for sharing this. I added it to my preparedness checklist.'

    def ensure_help_comments(self, help_requests, users):
        experts = [user for user in users.values() if user.role == User.Role.EXPERT]
        for index, help_request in enumerate(help_requests):
            if help_request.assigned_expert_id:
                author = help_request.assigned_expert
                content = f'I can take responsibility for this request near {help_request.location_text}.'
            else:
                author = experts[index % len(experts)]
                content = 'I may be able to help. Please add any timing or access details.'
            HelpComment.objects.get_or_create(
                request=help_request,
                author=author,
                content=content,
            )

    def ensure_votes(self, posts, users):
        voters = list(users.values())
        for post_index, post in enumerate(posts):
            for voter in voters[:4]:
                if voter.id == post.author_id:
                    continue
                vote_type = Vote.VoteType.DOWN if (post_index + voter.id) % 9 == 0 else Vote.VoteType.UP
                Vote.objects.update_or_create(
                    post=post,
                    user=voter,
                    defaults={'vote_type': vote_type},
                )

    def sync_counts(self):
        post_comment_counts = dict(
            Comment.objects.values('post_id').annotate(total=Count('id')).values_list('post_id', 'total')
        )
        post_upvote_counts = dict(
            Vote.objects.filter(vote_type=Vote.VoteType.UP)
            .values('post_id')
            .annotate(total=Count('id'))
            .values_list('post_id', 'total')
        )
        post_downvote_counts = dict(
            Vote.objects.filter(vote_type=Vote.VoteType.DOWN)
            .values('post_id')
            .annotate(total=Count('id'))
            .values_list('post_id', 'total')
        )
        for post in Post.objects.all():
            post.comment_count = post_comment_counts.get(post.id, 0)
            post.upvote_count = post_upvote_counts.get(post.id, 0)
            post.downvote_count = post_downvote_counts.get(post.id, 0)
            post.save(update_fields=['comment_count', 'upvote_count', 'downvote_count'])

        help_comment_counts = dict(
            HelpComment.objects.values('request_id')
            .annotate(total=Count('id'))
            .values_list('request_id', 'total')
        )
        for help_request in HelpRequest.objects.all():
            help_request.comment_count = help_comment_counts.get(help_request.id, 0)
            help_request.save(update_fields=['comment_count'])
