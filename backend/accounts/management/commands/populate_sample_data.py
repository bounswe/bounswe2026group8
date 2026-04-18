from django.core.management.base import BaseCommand
from django.utils import timezone
from accounts.models import User, Hub, Profile, Resource, ExpertiseField
from forum.models import Post, Comment, Vote
from help_requests.models import HelpRequest, HelpComment, HelpOffer
import random


class Command(BaseCommand):
    help = 'Populate the database with sample data for local development'

    def handle(self, *args, **options):
        self.stdout.write('Populating sample data...')

        # Get specific hubs: Istanbul, Izmir, and Ankara
        target_hubs = ['Istanbul', 'Izmir', 'Ankara']
        hubs = list(Hub.objects.filter(name__in=target_hubs))
        if len(hubs) < 3:
            self.stdout.write(self.style.ERROR(f'Expected 3 hubs ({target_hubs}), found {len(hubs)}. Run migrations first.'))
            return

        self.stdout.write(f'Using hubs: {[h.name for h in hubs]}')

        # Create sample users if they don't exist
        if not User.objects.filter(email__in=['standard1@example.com', 'expert1@example.com']).exists():
            self.create_users(hubs)
        else:
            self.stdout.write('Users already exist, skipping user creation.')

        # Create sample posts if they don't exist
        # Check if we have posts for our target hubs
        target_hub_posts = Post.objects.filter(hub__name__in=target_hubs)
        if target_hub_posts.count() < 10:  # We expect at least 10 posts for target hubs
            # Clear existing posts to recreate with new structure
            Post.objects.all().delete()
            Vote.objects.all().delete()  # Clear votes too
            Comment.objects.all().delete()  # Clear comments too
            self.create_posts(hubs)
        else:
            self.stdout.write('Target hub posts already exist, skipping post creation.')

        # Create sample help requests if they don't exist
        target_hub_requests = HelpRequest.objects.filter(hub__name__in=target_hubs)
        if target_hub_requests.count() < 10:  # We expect at least 10 requests for target hubs
            HelpRequest.objects.all().delete()
            HelpComment.objects.all().delete()
            self.create_help_requests(hubs)
        else:
            self.stdout.write('Target hub help requests already exist, skipping help request creation.')

        # Create sample help offers if they don't exist
        target_hub_offers = HelpOffer.objects.filter(hub__name__in=target_hubs)
        if target_hub_offers.count() < 10:  # We expect at least 10 offers for target hubs
            HelpOffer.objects.all().delete()
            self.create_help_offers(hubs)
        else:
            self.stdout.write('Target hub help offers already exist, skipping help offer creation.')

        self.stdout.write(self.style.SUCCESS('Sample data populated successfully!'))

    def create_users(self, hubs):
        users_data = [
            {
                'email': 'standard1@example.com',
                'full_name': 'John Standard',
                'role': 'STANDARD',
                'hub': random.choice(hubs),
                'neighborhood_address': '123 Main St',
                'profile': {
                    'phone_number': '+1234567890',
                    'bio': 'Regular community member interested in emergency preparedness.',
                    'availability_status': 'SAFE',
                }
            },
            {
                'email': 'standard2@example.com',
                'full_name': 'Jane Citizen',
                'role': 'STANDARD',
                'hub': random.choice(hubs),
                'neighborhood_address': '456 Oak Ave',
                'profile': {
                    'phone_number': '+1234567891',
                    'bio': 'Mother of two, concerned about neighborhood safety.',
                    'availability_status': 'SAFE',
                }
            },
            {
                'email': 'expert1@example.com',
                'full_name': 'Dr. Sarah Expert',
                'role': 'EXPERT',
                'hub': random.choice(hubs),
                'neighborhood_address': '789 Pine Rd',
                'expertise_field': 'First Aid',
                'profile': {
                    'phone_number': '+1234567892',
                    'bio': 'Medical professional with 10 years experience in emergency response.',
                    'availability_status': 'AVAILABLE_TO_HELP',
                },
                'expertise': {
                    'field': 'Emergency Medicine',
                    'certification_level': 'ADVANCED',
                    'certification_document_url': 'https://example.com/cert1.pdf'
                }
            },
            {
                'email': 'expert2@example.com',
                'full_name': 'Mike Rescue',
                'role': 'EXPERT',
                'hub': random.choice(hubs),
                'neighborhood_address': '321 Elm St',
                'expertise_field': 'Search and Rescue',
                'profile': {
                    'phone_number': '+1234567893',
                    'bio': 'Former firefighter, certified in urban search and rescue operations.',
                    'availability_status': 'AVAILABLE_TO_HELP',
                },
                'expertise': {
                    'field': 'Search and Rescue',
                    'certification_level': 'ADVANCED',
                    'certification_document_url': 'https://example.com/cert2.pdf'
                }
            },
            {
                'email': 'expert3@example.com',
                'full_name': 'Lisa Logistics',
                'role': 'EXPERT',
                'hub': random.choice(hubs),
                'neighborhood_address': '654 Maple Dr',
                'expertise_field': 'Logistics',
                'profile': {
                    'phone_number': '+1234567894',
                    'bio': 'Supply chain manager experienced in emergency resource distribution.',
                    'availability_status': 'SAFE',
                },
                'expertise': {
                    'field': 'Emergency Logistics',
                    'certification_level': 'BEGINNER',
                }
            }
        ]

        for user_data in users_data:
            user, created = User.objects.get_or_create(
                email=user_data['email'],
                defaults={
                    'full_name': user_data['full_name'],
                    'role': user_data['role'],
                    'hub': user_data['hub'],
                    'neighborhood_address': user_data['neighborhood_address'],
                    'expertise_field': user_data.get('expertise_field', ''),
                }
            )
            if created:
                user.set_password('password123')
                user.save()

                # Update profile (it should already exist due to signal)
                profile_data = user_data['profile']
                profile = user.profile
                profile.phone_number = profile_data.get('phone_number')
                profile.bio = profile_data.get('bio')
                profile.availability_status = profile_data.get('availability_status')
                profile.save()

                # Create expertise if expert
                if user.role == 'EXPERT' and 'expertise' in user_data:
                    ExpertiseField.objects.create(
                        user=user,
                        field=user_data['expertise']['field'],
                        certification_level=user_data['expertise']['certification_level'],
                        certification_document_url=user_data['expertise'].get('certification_document_url', ''),
                    )

                # Create some resources for experts
                if user.role == 'EXPERT':
                    Resource.objects.create(
                        user=user,
                        name='First Aid Kit',
                        category='Medical',
                        quantity=1,
                        condition=True,
                    )

        self.stdout.write(f'Created {len(users_data)} sample users')

    def create_posts(self, hubs):
        users = list(User.objects.all())
        if not users:
            return

        # Global posts (at least 10)
        global_posts_data = [
            {
                'title': 'Earthquake Safety Tips',
                'content': 'During an earthquake: DROP, COVER, HOLD ON. Drop to the ground, take cover under a sturdy table or desk, and hold on until the shaking stops. Remember to stay away from windows and heavy objects.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Flood Preparedness Guide',
                'content': 'Prepare for floods by knowing your flood risk, having an emergency kit, and creating an evacuation plan. Move to higher ground immediately if flooding occurs.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Fire Safety in the Home',
                'content': 'Install smoke detectors on every level of your home. Create and practice a fire escape plan. Never leave cooking unattended.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Emergency Communication Plan',
                'content': 'Establish a family communication plan. Designate an out-of-area contact. Know how to text emergency services if you can\'t speak.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Basic First Aid Knowledge',
                'content': 'Learn CPR and the Heimlich maneuver. Know how to treat burns, cuts, and sprains. Keep a well-stocked first aid kit.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Power Outage Survival Tips',
                'content': 'Keep flashlights and batteries ready. Use generators safely outdoors only. Conserve phone battery for emergencies.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Winter Storm Preparation',
                'content': 'Stock up on food, water, and warm clothing. Have alternative heat sources. Stay informed about weather conditions.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Hurricane Preparedness',
                'content': 'Board up windows, secure outdoor items, and have plenty of supplies. Know your evacuation routes and have a safe room.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Emergency Kit Essentials',
                'content': 'Water (1 gallon per person per day), non-perishable food, medications, first aid supplies, flashlight, radio, and important documents.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
            {
                'title': 'Neighborhood Watch Programs',
                'content': 'Organize community watch programs. Know your neighbors and their emergency contact information. Report suspicious activities.',
                'forum_type': 'GLOBAL',
                'hub': None,
            },
        ]

        # Urgent posts (3)
        urgent_posts_data = [
            {
                'title': 'URGENT: Major Power Outage in Downtown Istanbul',
                'content': 'Power has been out for 3 hours in the downtown Istanbul area. Multiple blocks affected. If anyone needs assistance with generators, medical equipment, or checking on elderly neighbors, please respond immediately.',
                'forum_type': 'URGENT',
                'hub': Hub.objects.filter(name='Istanbul').first(),
            },
            {
                'title': 'URGENT: Flash Flood Warning in Izmir Coastal Areas',
                'content': 'Heavy rainfall has caused flash flooding in coastal districts of Izmir. Several roads are impassable. People may need immediate evacuation assistance. Emergency services are overwhelmed.',
                'forum_type': 'URGENT',
                'hub': Hub.objects.filter(name='Izmir').first(),
            },
            {
                'title': 'URGENT: Gas Leak Reported in Ankara Residential Area',
                'content': 'Gas leak detected in a residential neighborhood in Ankara. Evacuation in progress. Multiple families displaced. Need temporary shelter and transportation assistance urgently.',
                'forum_type': 'URGENT',
                'hub': Hub.objects.filter(name='Ankara').first(),
            },
        ]

        # Standard posts for each hub
        standard_posts_data = []
        for hub in hubs:
            hub_posts = [
                {
                    'title': f'Emergency Preparedness Meeting in {hub.name}',
                    'content': f'Hi everyone in {hub.name}! We\'re organizing a community meeting to discuss emergency preparedness. Topics include creating emergency kits, family communication plans, and neighborhood watch programs. All are welcome!',
                    'forum_type': 'STANDARD',
                    'hub': hub,
                },
                {
                    'title': f'Community Garden Initiative in {hub.name}',
                    'content': f'Let\'s start a community garden in {hub.name} for sustainable food sources during emergencies. We can grow vegetables and herbs that store well. Who\'s interested in participating?',
                    'forum_type': 'STANDARD',
                    'hub': hub,
                },
                {
                    'title': f'First Aid Training Session in {hub.name}',
                    'content': f'I\'m organizing a free first aid training session next Saturday at the {hub.name} community center. Learn CPR, wound care, and basic emergency response. Limited spots available!',
                    'forum_type': 'STANDARD',
                    'hub': hub,
                },
                {
                    'title': f'Volunteer Recruitment in {hub.name}',
                    'content': f'{hub.name} Emergency Response Team is looking for volunteers. Training provided. Help your community be prepared for emergencies.',
                    'forum_type': 'STANDARD',
                    'hub': hub,
                },
            ]
            standard_posts_data.extend(hub_posts)

        # Combine all posts
        all_posts_data = global_posts_data + urgent_posts_data + standard_posts_data

        for post_data in all_posts_data:
            author = random.choice(users)
            post = Post.objects.create(
                title=post_data['title'],
                content=post_data['content'],
                forum_type=post_data['forum_type'],
                hub=post_data['hub'],
                author=author,
            )

            # Add some comments (more for urgent posts)
            num_comments = random.randint(2, 5) if post_data['forum_type'] == 'URGENT' else random.randint(0, 3)
            for i in range(num_comments):
                Comment.objects.create(
                    post=post,
                    author=random.choice(users),
                    content=f'This is comment {i+1} on the post. {"Very urgent - please help!" if post_data["forum_type"] == "URGENT" else "Very helpful information!"}',
                )
                post.comment_count += 1
            post.save()

            # Add some votes (more engagement for urgent posts)
            num_votes = random.randint(5, 15) if post_data['forum_type'] == 'URGENT' else random.randint(0, 5)
            for i in range(num_votes):
                voter = random.choice(users)
                if not Vote.objects.filter(user=voter, post=post).exists():
                    Vote.objects.create(
                        post=post,
                        user=voter,
                        vote_type=random.choice(['UP', 'DOWN']),
                    )
                    if random.choice([True, False]):
                        post.upvote_count += 1
                    else:
                        post.downvote_count += 1
            post.save()

        self.stdout.write(f'Created {len(all_posts_data)} sample posts: {len(global_posts_data)} global, {len(urgent_posts_data)} urgent, {len(standard_posts_data)} standard')

    def create_help_requests(self, hubs):
        users = list(User.objects.filter(role='STANDARD'))
        if not users:
            users = list(User.objects.all())

        # Create multiple help requests for each hub
        requests_data = []
        for hub in hubs:
            hub_requests = [
                {
                    'title': f'Need Medical Assistance for Elderly Neighbor in {hub.name}',
                    'description': f'My 85-year-old neighbor in {hub.name} fell and hurt her ankle. She needs help getting to the doctor. Does anyone have a car available?',
                    'category': 'MEDICAL',
                    'urgency': 'HIGH',
                    'hub': hub,
                },
                {
                    'title': f'Looking for Non-Perishable Food Donations in {hub.name}',
                    'description': f'Our family of 4 in {hub.name} is going through a difficult time and could use some canned goods, pasta, and other non-perishable food items.',
                    'category': 'FOOD',
                    'urgency': 'MEDIUM',
                    'hub': hub,
                },
                {
                    'title': f'Temporary Shelter Needed in {hub.name}',
                    'description': f'Due to a house fire, my family in {hub.name} needs temporary accommodation for 2-3 nights while repairs are made. We have pets.',
                    'category': 'SHELTER',
                    'urgency': 'HIGH',
                    'hub': hub,
                },
                {
                    'title': f'Transportation to Hospital in {hub.name}',
                    'description': f'I need to get to the hospital in {hub.name} for a medical appointment but don\'t have transportation. Appointment is tomorrow morning.',
                    'category': 'TRANSPORT',
                    'urgency': 'MEDIUM',
                    'hub': hub,
                },
                {
                    'title': f'Water Damage Cleanup Help in {hub.name}',
                    'description': f'Our basement flooded in {hub.name} and we need help with cleanup and drying. We have elderly family members who can\'t do heavy lifting.',
                    'category': 'SHELTER',
                    'urgency': 'MEDIUM',
                    'hub': hub,
                },
            ]
            requests_data.extend(hub_requests)

        for request_data in requests_data:
            author = random.choice(users)
            request = HelpRequest.objects.create(
                title=request_data['title'],
                description=request_data['description'],
                category=request_data['category'],
                urgency=request_data['urgency'],
                hub=request_data['hub'],
                author=author,
            )

            # Add some comments (responses from experts/helpers)
            experts = list(User.objects.filter(role='EXPERT'))
            if experts:
                num_responses = random.randint(1, 3)
                for i in range(num_responses):
                    expert = random.choice(experts)
                    HelpComment.objects.create(
                        request=request,
                        author=expert,
                        content=f'I can help with this in {request_data["hub"].name}. Please contact me at {expert.profile.phone_number} to coordinate.',
                    )
                    request.comment_count += 1
                request.status = 'EXPERT_RESPONDING'
                request.save()

        self.stdout.write(f'Created {len(requests_data)} sample help requests across {len(hubs)} hubs')

    def create_help_offers(self, hubs):
        experts = list(User.objects.filter(role='EXPERT'))
        if not experts:
            return

        # Create help offers for each hub
        offers_data = []
        for hub in hubs:
            hub_offers = [
                {
                    'skill_or_resource': f'First Aid Training in {hub.name}',
                    'description': f'Certified first aid instructor available in {hub.name} to teach CPR and basic emergency response to community members.',
                    'category': 'MEDICAL',
                    'availability': 'Weekends, 9 AM - 5 PM',
                    'hub': hub,
                },
                {
                    'skill_or_resource': f'Vehicle for Transport in {hub.name}',
                    'description': f'Have access to a minivan in {hub.name} that can transport up to 6 people. Available for medical appointments and emergency evacuations.',
                    'category': 'TRANSPORT',
                    'availability': 'Weekdays after 5 PM, Weekends anytime',
                    'hub': hub,
                },
                {
                    'skill_or_resource': f'Emergency Food Supplies in {hub.name}',
                    'description': f'Stockpiled non-perishable food items in {hub.name} for 4 people for 2 weeks. Can share with families in need during emergencies.',
                    'category': 'FOOD',
                    'availability': 'Available immediately in case of emergency',
                    'hub': hub,
                },
                {
                    'skill_or_resource': f'Guest Room in {hub.name}',
                    'description': f'Spare bedroom available in {hub.name} for temporary shelter during emergencies. Includes basic amenities.',
                    'category': 'SHELTER',
                    'availability': 'Available on short notice, up to 1 week stays',
                    'hub': hub,
                },
            ]
            offers_data.extend(hub_offers)

        for offer_data in offers_data:
            author = random.choice(experts)
            HelpOffer.objects.create(
                skill_or_resource=offer_data['skill_or_resource'],
                description=offer_data['description'],
                category=offer_data['category'],
                availability=offer_data['availability'],
                hub=offer_data['hub'],
                author=author,
            )

        self.stdout.write(f'Created {len(offers_data)} sample help offers across {len(hubs)} hubs')