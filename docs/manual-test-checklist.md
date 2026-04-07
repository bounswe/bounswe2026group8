# Manual Test Checklist

This checklist covers UI flows, edge cases, and scenarios that are not feasible
to automate. Run through the full checklist before each release candidate.

Mark each item ✅ (pass), ❌ (fail), or ⚠️ (partial / known issue).

---

## 1. Navigation

### Android
- [ ] All bottom navigation tabs switch screens without crashing
- [ ] Back button behaves correctly throughout all flows
- [ ] Back button on offline information screen returns to previous screen
- [ ] App does not crash when navigating quickly between screens

### Web
- [ ] Browser back/forward navigation works in the forum and help requests pages
- [ ] Direct URL access to authenticated routes redirects to sign-in if not logged in
- [ ] Deep links (e.g., a shared post URL) open the correct screen after sign-in

---

## 2. Authentication Flow

- [ ] Register → hub selection → dashboard works end-to-end
- [ ] Registering with an already-used email shows a clear error
- [ ] Registering as EXPERT without expertise_field shows a validation error
- [ ] Login with wrong password shows a clear error (no crash)
- [ ] Login with correct credentials lands on dashboard
- [ ] Logout clears the session and redirects to sign-in
- [ ] Refreshing the page / reopening the app while logged in restores the session
- [ ] Expired or invalid tokens redirect to sign-in without crashing

---

## 3. Forum

### Post Listing
- [ ] Posts appear immediately after loading
- [ ] GLOBAL, STANDARD, and URGENT tabs filter correctly
- [ ] Sorting by newest / hot / comments / votes works visibly
- [ ] Empty tab shows an empty state (no error, no spinner stuck)

### Post Creation
- [ ] Creating a post with title and content submits successfully
- [ ] Creating a post without a title shows a validation error
- [ ] Uploading images attaches them to the post
- [ ] Double-tapping submit does not create duplicate posts

### Post Interaction
- [ ] Opening a post loads comments
- [ ] Adding a comment appears immediately in the list
- [ ] Upvoting a post increments the upvote count visually
- [ ] Downvoting a post increments the downvote count visually
- [ ] Clicking an active vote again removes the vote (toggle off)
- [ ] Switching from upvote to downvote works correctly
- [ ] Reporting a post submits without crashing

### Repost
- [ ] Reposting a post to a different hub succeeds
- [ ] Trying to repost a post you already reposted shows a block / error

### Authorization
- [ ] Edit/delete options only appear on your own posts
- [ ] Attempting to edit another user's post is blocked

---

## 4. Help Requests

### Listing & Filtering
- [ ] All help requests load on the Requests tab
- [ ] Category filter (MEDICAL, FOOD, SHELTER, TRANSPORT) narrows the list
- [ ] Requests show urgency badge (LOW / MEDIUM / HIGH) with correct color
- [ ] Requests show status badge (OPEN / EXPERT RESPONDING / RESOLVED)
- [ ] Empty state appears when no requests match the filter

### Create Help Request
- [ ] Submitting with all required fields creates the request
- [ ] Submitting without a title or description shows a validation error
- [ ] Optional location fields (lat/lon/text) can be left blank
- [ ] Optional medical information can be left blank
- [ ] Image upload attaches images to the request
- [ ] Double-tapping submit does not create duplicate requests

### Help Request Detail
- [ ] Viewing a request shows full description, category, urgency, and comments
- [ ] Adding a comment on a request appears immediately
- [ ] Expert comment changes status to EXPERT RESPONDING
- [ ] Author can mark request as RESOLVED
- [ ] Status does not revert after being RESOLVED
- [ ] Author can delete their own request

### Help Offers
- [ ] Offers tab loads correctly
- [ ] Creating an offer with valid fields succeeds
- [ ] Author can delete their own offer

### Authorization
- [ ] Non-author cannot delete/edit a request
- [ ] Only the author sees the RESOLVED button

---

## 5. Profile Page

- [ ] Profile page loads user information correctly
- [ ] Editing bio and saving reflects the change immediately
- [ ] Editing phone number with invalid format shows a validation error
- [ ] Adding a resource (name, category, quantity) saves and appears in the list
- [ ] Deleting a resource removes it from the list
- [ ] Expert user can add an expertise field (field + certification level)
- [ ] Standard user does NOT see the expertise field section
- [ ] Changing availability status (SAFE / NEEDS HELP / AVAILABLE) saves correctly

---

## 6. Notifications

- [ ] Creating a help request sends a push notification to expert users in the same hub (verify on a second test device)
- [ ] Tapping the notification while app is in background opens the correct help request detail screen
- [ ] Tapping the notification while app is killed opens the app and navigates to the correct screen
- [ ] Notifications are NOT sent when the help request has no hub

---

## 7. Offline Access (Android only)

- [ ] Switch device to airplane mode
- [ ] Emergency phone numbers screen is accessible and displays data
- [ ] First aid information screen is accessible and displays data
- [ ] Map shows gathering points from the last cached location
- [ ] No crash or blank screen in offline mode

---

## 8. Error Handling & Edge Cases

- [ ] API failure (server down) shows a user-friendly error, does not crash
- [ ] Form with all optional fields left blank still submits if required fields are filled
- [ ] Viewing a post or request that has been deleted shows a 404-friendly message
- [ ] Submitting a form twice quickly does not create duplicates (double submit guard)
- [ ] Forum with zero posts shows an empty state, not a blank/broken screen
- [ ] Help requests list with zero items shows an empty state
- [ ] Profile with no resources shows empty state, not a broken list

---

## 9. UI / Layout

- [ ] All text is readable on a small-screen Android device (5")
- [ ] All text is readable on a large-screen Android device (6.7")
- [ ] Web layout renders correctly at 1280px viewport width
- [ ] Web layout renders correctly at 375px (mobile-sized browser window)
- [ ] No overlapping elements or cut-off text on any screen
- [ ] Loading spinners appear while data is fetching and disappear after

---

## 10. Slow / Unstable Network (Android)

- [ ] Enable network throttling (Android emulator: ~3G speed)
- [ ] App shows a loading indicator while fetching posts / help requests
- [ ] App does not time out or crash on slow connections
- [ ] Retry works after a temporary network failure
- [ ] Switching to offline after loading data still shows cached content where applicable

---

## 11. First-Time User

- [ ] A new user can open the app, register, select a hub, and reach the forum without any help
- [ ] The sign-up form clearly explains what fields are required
- [ ] Hub selection is clear and provides visible options
- [ ] First post creation is self-explanatory (no training needed)
- [ ] Error messages use plain language and suggest what to fix
