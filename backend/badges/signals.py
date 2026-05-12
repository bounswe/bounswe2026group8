from django.db.models.signals import post_save
from django.dispatch import receiver
from django.db import transaction

# Import the Comment model from your forum app
from forum.models import Comment 
from .models import Badge, UserBadge
from forum.models import Vote
from forum.models import Post
from help_requests.models import HelpComment
from help_requests.models import HelpRequest

@receiver(post_save, sender=Comment)
@receiver(post_save, sender=HelpComment)
def update_comment_badge_progress(sender, instance, created, **kwargs):
    """
    Updates the Conversationalist badge progress for both Forum 
    and Help Request comments.
    """
    if created:
        with transaction.atomic():
            try:
                # Use the CriteriaType to find the badge blueprint
                comment_badge = Badge.objects.get(criteria_type=Badge.CriteriaType.COMMENTS)
            except Badge.DoesNotExist:
                # Log a warning or just return if the admin hasn't set this up
                print(f"Badge with criteria_type {Badge.CriteriaType.COMMENTS} not found.")
                return 

            # Get or Create the specific UserBadge for this author
            user_badge, _ = UserBadge.objects.select_for_update().get_or_create(
                user=instance.author,
                badge=comment_badge,
                defaults={'current_progress': 0, 'current_level': 0}
            )

            # Increment progress
            user_badge.current_progress += 1

            # Check for Level Up logic
            if not user_badge.is_max_level:
                # We use the current level as the index to find the 'next' milestone
                # e.g. Level 0 looks at milestones[0] to get to Level 1
                milestones = comment_badge.milestones # Assuming this is your JSON list
                
                if user_badge.current_level < len(milestones):
                    next_goal = milestones[user_badge.current_level]
                    
                    if user_badge.current_progress >= next_goal:
                        user_badge.current_level += 1
                        print(f"🎉 LEVEL UP! {instance.author.email} reached Level {user_badge.current_level} for {comment_badge.name}!")

            user_badge.save()

@receiver(post_save, sender=Vote)
def update_vote_badge_progress(sender, instance, created, **kwargs):
    # Only award points for a BRAND NEW vote. 
    # If a user changes their upvote to a downvote, we don't want to give them double points!
    if created:
        try:
            # MUST exactly match the name you typed in the Django Admin
            badge = Badge.objects.get(name="Voter") 
            
            # Assuming your Vote model uses 'user' to track who voted. 
            # If your model uses 'author' instead, change instance.user to instance.author!
            user_badge, _ = UserBadge.objects.get_or_create(
                user=instance.user, 
                badge=badge
            )
            
            if not user_badge.is_max_level:
                user_badge.current_progress += 1
                
                # Inline logic: If progress hits the goal, increase the level!
                if user_badge.current_progress >= user_badge.next_level_goal:
                    user_badge.current_level += 1
                    print(f"🎉 LEVEL UP! {instance.user.email} reached Voter Level {user_badge.current_level}!")
                
                user_badge.save()
                
        except Badge.DoesNotExist:
            print("Warning: 'Voter' badge not found in database.")

@receiver(post_save, sender=Post)
def update_post_badge_progress(sender, instance, created, **kwargs):
    # Only trigger when a BRAND NEW post is created, not when it's edited
    if created:
        try:
            badge = Badge.objects.get(name="Forum Active") 
            
            # Using instance.author (change to instance.user if your Post model requires it)
            user_badge, _ = UserBadge.objects.get_or_create(
                user=instance.author, 
                badge=badge
            )
            
            if not user_badge.is_max_level:
                user_badge.current_progress += 1
                
                # Inline logic: If progress hits the goal, increase the level!
                if user_badge.current_progress >= user_badge.next_level_goal:
                    user_badge.current_level += 1
                    print(f"🎉 LEVEL UP! {instance.author.email} reached Forum Active Level {user_badge.current_level}!")
                
                user_badge.save()
                
        except Badge.DoesNotExist:
            print("Warning: 'Forum Active' badge not found in database.")

@receiver(post_save, sender=HelpRequest)
def update_responder_badge_progress(sender, instance, created, **kwargs):
    """
    Awards progress for the Responder badge ONLY when a Help Request
    is officially marked as RESOLVED and has an assigned expert.
    """
    # 1. We don't care about brand new requests (they aren't resolved yet)
    if created:
        return

    # 2. Only proceed if status is RESOLVED and someone was actually assigned to help
    if instance.status == HelpRequest.Status.RESOLVED and instance.assigned_expert:
        with transaction.atomic():
            try:
                # Find the 'Responder' badge blueprint
                responder_badge = Badge.objects.get(criteria_type=Badge.CriteriaType.HELP_RESPONSES)
            except Badge.DoesNotExist:
                return

            # 3. Get or Create the specific UserBadge for the ASSIGNED EXPERT
            # We use select_for_update to prevent race conditions
            user_badge, _ = UserBadge.objects.select_for_update().get_or_create(
                user=instance.assigned_expert,
                badge=responder_badge,
                defaults={'current_progress': 0, 'current_level': 0}
            )

            # 4. Increment progress
            user_badge.current_progress += 1

            # 5. Check for Level Up
            if not user_badge.is_max_level:
                milestones = responder_badge.milestones
                if user_badge.current_level < len(milestones):
                    next_goal = milestones[user_badge.current_level]
                    
                    if user_badge.current_progress >= next_goal:
                        user_badge.current_level += 1
                        print(f"🎉 MISSION ACCOMPLISHED! {instance.assigned_expert.email} resolved a request and reached Level {user_badge.current_level} Responder!")

            user_badge.save()