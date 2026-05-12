import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import PostCreatePageTutorial from './PostCreatePage_tutorial';
import { getTutorialPosts } from '../utils/tutorialStorage';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

function renderPostCreateTutorial() {
  return render(
    <MemoryRouter>
      <PostCreatePageTutorial />
    </MemoryRouter>
  );
}

describe('PostCreatePageTutorial', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    mockNavigate.mockClear();
    jest.spyOn(Date, 'now').mockReturnValue(1700000000000);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders the guided create-post scenario with default sample content', () => {
    renderPostCreateTutorial();

    expect(screen.getByRole('heading', { name: /new forum post/i })).toBeInTheDocument();
    expect(screen.getByText(/current situation/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue(/charging station open/i)).toBeInTheDocument();
    expect(screen.getByText(/100%/)).toBeInTheDocument();
    expect(screen.getByText(/step 1/i)).toBeInTheDocument();
  });

  it('advances the embedded guide and highlights the matching form field', async () => {
    const user = userEvent.setup();
    renderPostCreateTutorial();

    const typeGroup = screen.getByLabelText(/forum type/i).closest('.form-group');
    const titleGroup = screen.getByLabelText(/^title$/i).closest('.form-group');

    expect(typeGroup).toHaveClass('tutorial-tour-highlight');
    expect(titleGroup).not.toHaveClass('tutorial-tour-highlight');

    await user.click(screen.getByRole('button', { name: /next/i }));

    expect(typeGroup).not.toHaveClass('tutorial-tour-highlight');
    expect(titleGroup).toHaveClass('tutorial-tour-highlight');
  });

  it('validates required title and content before saving', async () => {
    const user = userEvent.setup();
    renderPostCreateTutorial();

    await user.clear(screen.getByLabelText(/^title$/i));
    fireEvent.change(screen.getByLabelText(/content/i), { target: { value: '' } });
    await user.click(screen.getByRole('button', { name: /save/i }));

    expect(screen.getByText(/add a short title before saving/i)).toBeInTheDocument();
    expect(screen.getByText(/add details neighbors can act on/i)).toBeInTheDocument();
    expect(getTutorialPosts()).toEqual([]);
    expect(mockNavigate).not.toHaveBeenCalledWith('/tutorial/forum');
  });

  it('clears field validation as the user fixes input', async () => {
    const user = userEvent.setup();
    renderPostCreateTutorial();

    const title = screen.getByLabelText(/^title$/i);
    await user.clear(title);
    await user.click(screen.getByRole('button', { name: /save/i }));
    expect(screen.getByText(/add a short title before saving/i)).toBeInTheDocument();

    await user.type(title, 'Fresh neighborhood update');

    expect(screen.queryByText(/add a short title before saving/i)).not.toBeInTheDocument();
  });

  it('saves a local tutorial post with trimmed content, images, and forum type', async () => {
    const user = userEvent.setup();
    renderPostCreateTutorial();

    await user.selectOptions(screen.getByLabelText(/forum type/i), 'URGENT');
    await user.clear(screen.getByLabelText(/^title$/i));
    await user.type(screen.getByLabelText(/^title$/i), '  Generator available  ');
    fireEvent.change(screen.getByLabelText(/content/i), { target: { value: '  Bring a cable and power bank.  ' } });
    await user.type(screen.getByLabelText(/image links/i), 'https://example.com/a.jpg\n\n https://example.com/b.jpg ');
    await user.click(screen.getByRole('button', { name: /save/i }));

    expect(getTutorialPosts()).toEqual([
      expect.objectContaining({
        id: 'local-post-1700000000000',
        title: 'Generator available',
        body: 'Bring a cable and power bank.',
        forumType: 'URGENT',
        imageUrls: ['https://example.com/a.jpg', 'https://example.com/b.jpg'],
        author: 'You',
        local: true,
      }),
    ]);
    expect(mockNavigate).toHaveBeenCalledWith('/tutorial/forum');
  });

  it('navigates back to the tutorial forum from the header button', async () => {
    const user = userEvent.setup();
    renderPostCreateTutorial();

    await user.click(screen.getByRole('button', { name: /forum/i }));

    expect(mockNavigate).toHaveBeenCalledWith('/tutorial/forum');
  });
});
