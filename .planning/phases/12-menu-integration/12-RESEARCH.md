# Phase 12 Research: Menu Integration

## Summary

Phase 12 is minimal. The profile route already exists and is properly configured. Only the menu item needs to be added.

## Findings

### Router Configuration

**File:** `src/frontend/src/router/routes.js`

Profile route already exists (lines 52-56):
```js
{
  path: 'profile',
  name: 'profile',
  component: () => import('pages/ProfilePage.vue'),
  meta: { requiresAuth: true },
},
```

**Status:** ALREADY DONE - No changes needed.

### Navigation Menu

**File:** `src/frontend/src/layouts/MainLayout.vue`

Current drawer navigation (lines 82-93):
```vue
<q-list>
  <q-item-label header>{{ t('common.menu') }}</q-item-label>

  <q-item clickable to="/dashboard">
    <q-item-section avatar>
      <q-icon name="dashboard" />
    </q-item-section>
    <q-item-section>
      <q-item-label>Dashboard</q-item-label>
    </q-item-section>
  </q-item>
</q-list>
```

**Required:** Add Profile menu item following same pattern:
- Icon: `person` (matches user menu icon)
- Route: `/profile`
- Label: `{{ t('profile.title') }}`

### i18n Keys

`profile.title` already exists:
- **en-US:** "My Profile"
- **fr-FR:** "Mon profil" (confirmed exists)

**Status:** ALREADY DONE - No changes needed.

### User Menu Enhancement (Optional)

The user dropdown menu (lines 48-67) currently only has "Logout". Could optionally add "Profile" link there too for better UX (users expect profile in user menu).

## Scope

**Required (PROF-02):**
1. Add Profile item to q-drawer navigation

**Optional enhancement:**
2. Add Profile link to user dropdown menu (better UX)

## Implementation

Single file change: `src/frontend/src/layouts/MainLayout.vue`

Add after Dashboard q-item (around line 92):
```vue
<q-item clickable to="/profile">
  <q-item-section avatar>
    <q-icon name="person" />
  </q-item-section>
  <q-item-section>
    <q-item-label>{{ t('profile.title') }}</q-item-label>
  </q-item-section>
</q-item>
```

---

*Research completed: 2026-01-29*
