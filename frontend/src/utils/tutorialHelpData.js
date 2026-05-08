import { getTutorialHelpRequests } from './tutorialStorage';

export const SAMPLE_HELP_REQUESTS = [
  {
    id: 'sample-help-1',
    title: 'Need drinking water for an elderly neighbor',
    description: 'A neighbor cannot walk to the distribution point after the outage.',
    category: 'FOOD',
    urgency: 'MEDIUM',
    location_text: 'Besiktas community center, Block B entrance',
    author: 'Aylin Neighbor',
    status: 'Open',
    createdLabel: 'recent update',
    comments: ['I can bring two bottles in 20 minutes.'],
  },
  {
    id: 'sample-help-2',
    title: 'Ride needed to pharmacy',
    description: 'One resident needs transport to pick up medication before evening.',
    category: 'TRANSPORT',
    urgency: 'HIGH',
    location_text: 'Near Barbaros Boulevard',
    author: 'Mert Neighbor',
    status: 'Open',
    createdLabel: 'recent update',
    comments: ['I am nearby with a car. Please share the pickup point.'],
  },
  {
    id: 'sample-help-3',
    title: 'Blankets for temporary shelter',
    description: 'A small group at the community hall needs clean blankets tonight.',
    category: 'SHELTER',
    urgency: 'LOW',
    location_text: 'Community hall entrance desk',
    author: 'Shelter Volunteer',
    status: 'Open',
    createdLabel: 'recent update',
    comments: [],
  },
];

export function getAllTutorialHelpRequests() {
  return [...getTutorialHelpRequests(), ...SAMPLE_HELP_REQUESTS];
}

export function getTutorialHelpRequestById(id) {
  return getAllTutorialHelpRequests().find((request) => String(request.id) === String(id));
}
