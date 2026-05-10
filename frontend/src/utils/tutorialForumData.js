import { getTutorialPosts } from './tutorialStorage';

export const SAMPLE_TUTORIAL_POSTS = [
  {
    id: 'sample-post-1',
    title: 'Power is out near Barbaros Boulevard',
    author: 'Aylin Neighbor',
    role: 'STANDARD',
    status: 'Safe',
    comments: 4,
    upvotes: 12,
    downvotes: 0,
    forumType: 'GLOBAL',
    createdLabel: 'recent update',
    sampleComments: [
      'I checked Block A. Stairs are clear.',
      'Please avoid using elevators until power is stable.',
    ],
    body: 'Several buildings are affected. Elevators are not working, and residents are checking on older neighbors.',
  },
  {
    id: 'sample-post-2',
    title: 'Volunteer list for charging phones',
    author: 'Can Expert',
    role: 'EXPERT',
    status: 'Available to help',
    comments: 7,
    upvotes: 19,
    downvotes: 0,
    forumType: 'GLOBAL',
    createdLabel: 'recent update',
    sampleComments: [
      'I can bring two extra charging cables.',
      'Community center staff confirmed the station is open.',
    ],
    body: 'A small charging station is being organized at the community center. Bring your own cable if possible.',
  },
  {
    id: 'sample-post-3',
    title: 'Reminder: avoid downed cables',
    author: 'Safety Moderator',
    role: 'EXPERT',
    status: 'Available to help',
    comments: 2,
    upvotes: 24,
    downvotes: 0,
    forumType: 'GLOBAL',
    createdLabel: 'recent update',
    sampleComments: [
      'Reported one cable near the bus stop.',
      'Keeping children away from the sidewalk area.',
    ],
    body: 'If you see damaged electrical lines, keep distance and report the exact location to emergency services.',
  },
];

export function getAllTutorialPosts() {
  return [...getTutorialPosts(), ...SAMPLE_TUTORIAL_POSTS];
}

export function getTutorialPostById(id) {
  return getAllTutorialPosts().find((post) => String(post.id) === String(id));
}
