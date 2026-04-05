from django.test import TestCase, override_settings
from rest_framework.test import APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from accounts.models import Hub, User
from .models import Post, Comment, Vote, Report


class ForumTestBase(TestCase):
    """Shared setup: a hub and two authenticated users."""

    def setUp(self):
        self.hub, _ = Hub.objects.get_or_create(name='Istanbul', defaults={'slug': 'istanbul'})
        self.user1 = User.objects.create_user(
            email='alice@example.com', full_name='Alice', password='Pass1234', hub=self.hub,
        )
        self.user2 = User.objects.create_user(
            email='bob@example.com', full_name='Bob', password='Pass1234', hub=self.hub,
        )

        self.client1 = APIClient()
        self.client1.credentials(HTTP_AUTHORIZATION=f'Bearer {str(RefreshToken.for_user(self.user1).access_token)}')
        self.client2 = APIClient()
        self.client2.credentials(HTTP_AUTHORIZATION=f'Bearer {str(RefreshToken.for_user(self.user2).access_token)}')
        self.anon = APIClient()

    def _create_post(self, client=None, **overrides):
        payload = {
            'hub': self.hub.pk,
            'forum_type': 'STANDARD',
            'title': 'Test Post',
            'content': 'Some content.',
        }
        payload.update(overrides)
        return (client or self.client1).post('/forum/posts/', payload, format='json')


# ── Post CRUD Tests ────────────────────────────────────────────────────────────

class PostCRUDTests(ForumTestBase):

    def test_create_post(self):
        res = self._create_post()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['title'], 'Test Post')
        self.assertEqual(res.data['author']['email'], 'alice@example.com')

    def test_create_post_unauthenticated(self):
        res = self._create_post(client=self.anon)
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_list_posts(self):
        self._create_post()
        self._create_post(title='Second Post')
        res = self.anon.get('/forum/posts/')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(len(res.data), 2)

    def test_list_posts_filtered_by_hub(self):
        other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        self._create_post()
        self._create_post(hub=other_hub.pk, title='Ankara Post')
        res = self.anon.get(f'/forum/posts/?hub={self.hub.pk}')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['title'], 'Test Post')

    def test_list_posts_filtered_by_forum_type(self):
        self._create_post()
        self._create_post(forum_type='URGENT', title='Urgent!')
        res = self.anon.get('/forum/posts/?forum_type=urgent')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['title'], 'Urgent!')

    def test_list_posts_filtered_by_author(self):
        self._create_post(client=self.client1, title='Alice post')
        self._create_post(client=self.client2, title='Bob post')
        res = self.anon.get(f'/forum/posts/?author={self.user1.pk}')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['title'], 'Alice post')

    def test_create_global_post_no_hub(self):
        res = self.client1.post(
            '/forum/posts/',
            {'forum_type': 'GLOBAL', 'title': 'World', 'content': 'Hello everyone.'},
            format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['forum_type'], 'GLOBAL')
        self.assertIsNone(res.data['hub'])
        self.assertEqual(res.data['hub_name'], 'Global')

    def test_create_standard_post_requires_hub(self):
        res = self.client1.post(
            '/forum/posts/',
            {'forum_type': 'STANDARD', 'title': 'No hub', 'content': 'x'},
            format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)

    def test_list_global_posts_ignore_hub_filter(self):
        self.client1.post(
            '/forum/posts/',
            {'forum_type': 'GLOBAL', 'title': 'Global news', 'content': 'All hubs'},
            format='json',
        )
        self._create_post(title='Hub local')
        res = self.anon.get('/forum/posts/?forum_type=GLOBAL')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['title'], 'Global news')

    def test_get_post_detail(self):
        create_res = self._create_post()
        pk = create_res.data['id']
        res = self.anon.get(f'/forum/posts/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(res.data['content'], 'Some content.')

    def test_update_post_by_author(self):
        pk = self._create_post().data['id']
        res = self.client1.put(f'/forum/posts/{pk}/', {'title': 'Updated'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(res.data['title'], 'Updated')

    def test_update_post_by_non_author_forbidden(self):
        pk = self._create_post().data['id']
        res = self.client2.put(f'/forum/posts/{pk}/', {'title': 'Nope'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)

    def test_delete_post_by_author(self):
        pk = self._create_post().data['id']
        res = self.client1.delete(f'/forum/posts/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Post.objects.filter(pk=pk).exists())

    def test_delete_post_by_non_author_forbidden(self):
        pk = self._create_post().data['id']
        res = self.client2.delete(f'/forum/posts/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)

    def test_hidden_posts_excluded_from_list(self):
        pk = self._create_post().data['id']
        Post.objects.filter(pk=pk).update(status=Post.Status.HIDDEN)
        res = self.anon.get('/forum/posts/')
        self.assertEqual(len(res.data), 0)


# ── Comment Tests ──────────────────────────────────────────────────────────────

class CommentTests(ForumTestBase):

    def test_create_comment(self):
        pk = self._create_post().data['id']
        res = self.client2.post(f'/forum/posts/{pk}/comments/', {'content': 'Nice!'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Post.objects.get(pk=pk).comment_count, 1)

    def test_list_comments(self):
        pk = self._create_post().data['id']
        self.client1.post(f'/forum/posts/{pk}/comments/', {'content': 'First'}, format='json')
        self.client2.post(f'/forum/posts/{pk}/comments/', {'content': 'Second'}, format='json')
        res = self.anon.get(f'/forum/posts/{pk}/comments/')
        self.assertEqual(len(res.data), 2)

    def test_delete_own_comment(self):
        pk = self._create_post().data['id']
        cres = self.client2.post(f'/forum/posts/{pk}/comments/', {'content': 'Hi'}, format='json')
        cid = cres.data['id']
        res = self.client2.delete(f'/forum/comments/{cid}/')
        self.assertEqual(res.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(Post.objects.get(pk=pk).comment_count, 0)

    def test_delete_other_user_comment_forbidden(self):
        pk = self._create_post().data['id']
        cres = self.client2.post(f'/forum/posts/{pk}/comments/', {'content': 'Hi'}, format='json')
        cid = cres.data['id']
        res = self.client1.delete(f'/forum/comments/{cid}/')
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)

    def test_comment_unauthenticated(self):
        pk = self._create_post().data['id']
        res = self.anon.post(f'/forum/posts/{pk}/comments/', {'content': 'Hi'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)


# ── Vote Tests ─────────────────────────────────────────────────────────────────

class VoteTests(ForumTestBase):

    def test_upvote(self):
        pk = self._create_post().data['id']
        res = self.client2.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'UP'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Post.objects.get(pk=pk).upvote_count, 1)

    def test_toggle_same_vote_removes_it(self):
        pk = self._create_post().data['id']
        self.client2.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'UP'}, format='json')
        res = self.client2.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'UP'}, format='json')
        self.assertEqual(res.data['vote'], None)
        self.assertEqual(Post.objects.get(pk=pk).upvote_count, 0)

    def test_switch_vote(self):
        pk = self._create_post().data['id']
        self.client2.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'UP'}, format='json')
        res = self.client2.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'DOWN'}, format='json')
        self.assertEqual(res.data['vote'], 'DOWN')
        post = Post.objects.get(pk=pk)
        self.assertEqual(post.upvote_count, 0)
        self.assertEqual(post.downvote_count, 1)

    def test_vote_unauthenticated(self):
        pk = self._create_post().data['id']
        res = self.anon.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'UP'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_user_vote_in_detail(self):
        pk = self._create_post().data['id']
        self.client2.post(f'/forum/posts/{pk}/vote/', {'vote_type': 'DOWN'}, format='json')
        res = self.client2.get(f'/forum/posts/{pk}/')
        self.assertEqual(res.data['user_vote'], 'DOWN')

    def test_user_vote_null_when_not_voted(self):
        pk = self._create_post().data['id']
        res = self.client2.get(f'/forum/posts/{pk}/')
        self.assertIsNone(res.data['user_vote'])


# ── Report Tests ───────────────────────────────────────────────────────────────

class ReportTests(ForumTestBase):

    def test_report_post(self):
        pk = self._create_post().data['id']
        res = self.client2.post(f'/forum/posts/{pk}/report/', {'reason': 'SPAM'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Post.objects.get(pk=pk).report_count, 1)

    def test_duplicate_report_rejected(self):
        pk = self._create_post().data['id']
        self.client2.post(f'/forum/posts/{pk}/report/', {'reason': 'SPAM'}, format='json')
        res = self.client2.post(f'/forum/posts/{pk}/report/', {'reason': 'ABUSE'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)

    @override_settings(FORUM_REPORT_HIDE_THRESHOLD=2)
    def test_auto_hide_on_threshold(self):
        pk = self._create_post().data['id']
        self.client1.post(f'/forum/posts/{pk}/report/', {'reason': 'SPAM'}, format='json')
        self.client2.post(f'/forum/posts/{pk}/report/', {'reason': 'ABUSE'}, format='json')
        self.assertEqual(Post.objects.get(pk=pk).status, Post.Status.HIDDEN)

    def test_report_unauthenticated(self):
        pk = self._create_post().data['id']
        res = self.anon.post(f'/forum/posts/{pk}/report/', {'reason': 'SPAM'}, format='json')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)


# ── Repost Tests ───────────────────────────────────────────────────────────────

class RepostTests(ForumTestBase):

    def test_repost_to_another_hub(self):
        other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        pk = self._create_post().data['id']
        res = self.client2.post(f'/forum/posts/{pk}/repost/', {'hub': other_hub.pk}, format='json')
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Post.objects.get(pk=pk).repost_count, 1)

    def test_cannot_repost_own_post(self):
        pk = self._create_post().data['id']
        res = self.client1.post(f'/forum/posts/{pk}/repost/', {}, format='json')
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_repost_same_post_twice(self):
        other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        pk = self._create_post().data['id']
        self.client2.post(f'/forum/posts/{pk}/repost/', {'hub': other_hub.pk}, format='json')
        res = self.client2.post(f'/forum/posts/{pk}/repost/', {'hub': other_hub.pk}, format='json')
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)

    def test_repost_unauthenticated(self):
        pk = self._create_post().data['id']
        res = self.anon.post(f'/forum/posts/{pk}/repost/', {}, format='json')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)
