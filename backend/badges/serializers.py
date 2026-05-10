from rest_framework import serializers
from .models import Badge, UserBadge

class UserBadgeSerializer(serializers.ModelSerializer):
    # We "flatten" the badge data so React doesn't have to dig through nested objects
    badge_name = serializers.CharField(source='badge.name', read_only=True)
    badge_icon = serializers.CharField(source='badge.icon', read_only=True)
    badge_description = serializers.CharField(source='badge.description', read_only=True)
    max_level = serializers.IntegerField(source='badge.max_level', read_only=True)
    
    # We pull in those @property methods we wrote in models.py
    next_level_goal = serializers.ReadOnlyField()
    is_max_level = serializers.ReadOnlyField()

    class Meta:
        model = UserBadge
        fields = [
            'id', 
            'badge_name', 
            'badge_icon', 
            'badge_description', 
            'current_level', 
            'current_progress', 
            'max_level', 
            'next_level_goal', 
            'is_max_level'
        ]