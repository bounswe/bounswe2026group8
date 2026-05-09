from django.db.models.signals import post_save
from django.dispatch import receiver
from django.db import transaction

# Import the Comment model from your forum app
from forum.models import Comment 
from .models import Badge, UserBadge
from forum.models import Vote
from forum.models import Post

@receiver(post_save, sender=Comment)
def update_comment_badge_progress(sender, instance, created, **kwargs):
    """
    This signal runs every time a Comment is saved to the database.
    If it's a NEW comment (created=True), we update the author's badge progress.
    """
    if created:
        with transaction.atomic():
            # 1. Find the specific Badge Blueprint for Comments
            # Use try/except just in case the admin hasn't created the badge yet
            try:
                comment_badge = Badge.objects.get(criteria_type=Badge.CriteriaType.COMMENTS)
            except Badge.DoesNotExist:
                return # If the badge doesn't exist, just do nothing

            # 2. Get or Create the specific UserBadge for this author
            user_badge, was_created = UserBadge.objects.get_or_create(
                user=instance.author,
                badge=comment_badge,
                defaults={'current_progress': 0, 'current_level': 0}
            )

            # 3. Add 1 to their progress
            user_badge.current_progress += 1

            # 4. Check if they leveled up!
            # Compare their progress to the next milestone in the JSON array
            if not user_badge.is_max_level:
                # Need to use user_badge.current_level (not next_level_goal directly) 
                # as an index to access the JSON array safely
                next_milestone_index = user_badge.current_level
                
                # Double check the array isn't empty to prevent errors
                if next_milestone_index < len(comment_badge.milestones):
                    next_goal = comment_badge.milestones[next_milestone_index]
                    
                    if user_badge.current_progress >= next_goal:
                        user_badge.current_level += 1
                        print(f"🎉 LEVEL UP! {instance.author.email} reached Level {user_badge.current_level} for {comment_badge.name}!")

            # 5. Save the progress
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