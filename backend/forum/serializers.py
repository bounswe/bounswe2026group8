from rest_framework import serializers

from accounts.serializers import UserSerializer
from .models import Post, Comment, Vote, Report


class RepostOriginSerializer(serializers.ModelSerializer):
    """Minimal info about the original post for repost display."""
    author = UserSerializer(read_only=True)

    class Meta:
        model = Post
        fields = ['id', 'title', 'author']
        read_only_fields = fields


class PostListSerializer(serializers.ModelSerializer):
    """Compact representation for list views."""
    author = UserSerializer(read_only=True)
    hub_name = serializers.SerializerMethodField()
    user_vote = serializers.SerializerMethodField()
    user_has_reposted = serializers.SerializerMethodField()
    reposted_from = RepostOriginSerializer(read_only=True)

    class Meta:
        model = Post
        fields = [
            'id', 'hub', 'hub_name', 'forum_type', 'author',
            'title', 'image_urls', 'status',
            'upvote_count', 'downvote_count', 'comment_count', 'repost_count',
            'user_vote', 'user_has_reposted', 'reposted_from',
            'created_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else 'Global'

    def get_user_vote(self, obj):
        request = self.context.get('request')
        if not request or not request.user.is_authenticated:
            return None
        vote = obj.votes.filter(user=request.user).first()
        return vote.vote_type if vote else None

    def get_user_has_reposted(self, obj):
        request = self.context.get('request')
        if not request or not request.user.is_authenticated:
            return False
        return Post.objects.filter(author=request.user, reposted_from=obj).exists()


class PostDetailSerializer(serializers.ModelSerializer):
    """Full representation with content and images."""
    author = UserSerializer(read_only=True)
    hub_name = serializers.SerializerMethodField()
    user_vote = serializers.SerializerMethodField()
    user_has_reposted = serializers.SerializerMethodField()
    reposted_from = RepostOriginSerializer(read_only=True)

    class Meta:
        model = Post
        fields = [
            'id', 'hub', 'hub_name', 'forum_type', 'author',
            'title', 'content', 'image_urls', 'status',
            'upvote_count', 'downvote_count', 'comment_count', 'report_count', 'repost_count',
            'user_vote', 'user_has_reposted', 'reposted_from',
            'created_at', 'updated_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else 'Global'

    def get_user_vote(self, obj):
        request = self.context.get('request')
        if not request or not request.user.is_authenticated:
            return None
        vote = obj.votes.filter(user=request.user).first()
        return vote.vote_type if vote else None

    def get_user_has_reposted(self, obj):
        request = self.context.get('request')
        if not request or not request.user.is_authenticated:
            return False
        return Post.objects.filter(author=request.user, reposted_from=obj).exists()


class PostCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ['hub', 'forum_type', 'title', 'content', 'image_urls']
        extra_kwargs = {'hub': {'required': False, 'allow_null': True}}

    def validate(self, attrs):
        forum_type = attrs.get('forum_type', Post.ForumType.STANDARD)
        if forum_type == Post.ForumType.GLOBAL:
            attrs['hub'] = None
        elif not attrs.get('hub'):
            raise serializers.ValidationError({'hub': 'Hub is required for standard and urgent posts.'})
        return attrs

    def validate_image_urls(self, value):
        if not isinstance(value, list):
            raise serializers.ValidationError('image_urls must be a list.')
        return value


class PostUpdateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ['title', 'content', 'image_urls']

    def validate_image_urls(self, value):
        if not isinstance(value, list):
            raise serializers.ValidationError('image_urls must be a list.')
        return value


class CommentSerializer(serializers.ModelSerializer):
    author = UserSerializer(read_only=True)

    class Meta:
        model = Comment
        fields = ['id', 'post', 'author', 'content', 'created_at']
        read_only_fields = ['id', 'post', 'author', 'created_at']


class VoteSerializer(serializers.Serializer):
    vote_type = serializers.ChoiceField(choices=Vote.VoteType.choices)


class ReportSerializer(serializers.Serializer):
    reason = serializers.ChoiceField(choices=Report.Reason.choices)
